package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.net.URI

class VoiceIaPolicyTest {
    private val cue = SubtitleCue(id = "fala1", text = "Olá!", startMs = 1_000, endMs = 2_000)

    @Test fun fingerprintRejectsSubtitleChanges() {
        val prepared = VoiceIaTake(cue.id, VoiceIaPolicy.fingerprint(cue), "voz", GeminiModels.LITE,
            "estilo", "/data/voice.wav", 900L, "a".repeat(64))
        assertTrue(VoiceIaPolicy.matches(prepared, cue, "voz", GeminiModels.LITE, "estilo"))
        assertFalse(VoiceIaPolicy.matches(prepared, cue.copy(text = "Outra!"), "voz", GeminiModels.LITE, "estilo"))
        assertFalse(VoiceIaPolicy.matches(prepared, cue.copy(startMs = 900), "voz", GeminiModels.LITE, "estilo"))
        assertFalse(VoiceIaPolicy.matches(prepared, cue, "outra voz", GeminiModels.LITE, "estilo"))
    }

    /** Regressão CI118: o mockable android.jar de JVM não executa Uri.fromFile(). */
    @Test fun geminiFileUriCanBeBuiltWithoutAndroidAndEscapesUnicode() {
        val path = "/tmp/voz com acento á.wav"
        val result = GeminiAudioFiles.uri(File(path))
        assertEquals("file:///tmp/voz%20com%20acento%20%C3%A1.wav", result)
        assertEquals(path, URI(result).path)
        assertEquals("file:///tmp/a.wav", GeminiAudioFiles.uri(File("/tmp/a.wav")))
    }

    @Test fun insertAndReplaceKeepOnlyOneVoiceWithoutTouchingMusic() {
        val music = AudioTrack("file:///music.wav", "Música", startMs = 0, durationMs = 3_000)
        val project = VideoProject(clips = listOf(Clip(uri = "file:///video.mp4", title = "Vídeo",
            sourceDurationMs = 5_000)), subtitles = listOf(cue), audioTracks = listOf(music))
        val first = VoiceIaTake(cue.id, VoiceIaPolicy.fingerprint(cue), "voz A", GeminiModels.LITE,
            "neutra", "/tmp/a.wav", 900L, "a".repeat(64))
        val inserted = VoiceIaPolicy.merge(project, cue, first)
        assertEquals(2, inserted.audioTracks.size)
        assertEquals(music, inserted.audioTracks.first())
        val voice = inserted.audioTracks.last()
        assertEquals(VoiceIaPolicy.KIND, voice.kind)
        assertEquals(cue.id, voice.subtitleId)
        assertEquals(1_000L, voice.startMs)
        val moved = inserted.copy(audioTracks = inserted.audioTracks.map {
            if (it.id == voice.id) it.copy(startMs = 1_300L, volume = .6f) else it
        })
        val updated = VoiceIaPolicy.merge(moved, cue, first.copy(filePath = "/tmp/b.wav"))
        assertEquals(2, updated.audioTracks.size)
        assertEquals(music, updated.audioTracks.first())
        assertEquals(voice.id, updated.audioTracks.last().id)
        assertEquals(1_300L, updated.audioTracks.last().startMs)
        assertEquals(.6f, updated.audioTracks.last().volume, 0.0001f)
    }

    @Test fun neverTruncateGeneratedWavAtProjectEnd() {
        assertFalse(VoiceIaPolicy.fits(4_700, 600, 5_000))
        assertTrue(VoiceIaPolicy.fits(4_300, 600, 5_000))
        assertFalse(VoiceIaPolicy.fits(4_300, 150, 5_000))
    }
}
