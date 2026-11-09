package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class VoiceIaExternalAudio240Test {
    @Test fun importedTrackIsDistinctFromGeminiAndLocalOnly() {
        val cue = SubtitleCue(id = "fala-9", text = "[Gargalhada]", startMs = 0, endMs = 2500)
        val audioFile = java.io.File.createTempFile("uai-import-", ".m4a")
        try {
        val external = AudioTrack(uri = audioFile.toURI().toASCIIString(),
            title = "Áudio externo", durationMs = 2000L, kind = VoiceIaPolicy.KIND,
            subtitleId = cue.id, ttsModel = VoiceIaExternalAudio.MODEL,
            subtitleFingerprint = VoiceIaPolicy.fingerprint(cue))
        val project = VideoProject(subtitles = listOf(cue), audioTracks = listOf(external))
        assertTrue(VoiceIaExternalAudio.isImported(external))
        assertNotNull(VoiceIaExternalAudio.importedFor(project, cue))
        assertNull(VoiceIaExternalAudio.importedFor(project, cue.copy(text = "Mudou")))
        assertFalse(VoiceIaExternalAudio.isImported(external.copy(ttsModel = "gemini")))
        assertEquals(VoiceIaExternalAudio.MODEL, VideoProject.read(project.json()).audioTracks[0].ttsModel)
        } finally { audioFile.delete() }
    }

    @Test fun changingCharacterVoiceKeepsIndividualMood() {
        val cue = SubtitleCue(id = "fala", text = "Conseguimos!", startMs = 0, endMs = 3000,
            speakerId = "et")
        val base = VideoProject(subtitles = listOf(cue), subtitleSpeakers = listOf(
            SubtitleSpeaker("et", "ET da direita", 0xFF0088AA.toInt())))
        val withMood = VoiceIaStylePolicy.cueStyle(base, cue.id, "Animada")
            ?: error("Tom não disponível")
        val fixed = VoiceIaSpeakerVoicePolicy.speakerVoice(withMood, "et", "voice-1")!!
        assertEquals("voice-1", VoiceIaSpeakerVoicePolicy.preferred(cue,
            fixed.subtitleSpeakers, fixed.voiceIaVoices))
        assertEquals("Animada", VoiceIaStylePolicy.preferred(cue,
            fixed.subtitleSpeakers, fixed.voiceIaVoices))
        assertNull(VoiceIaSpeakerVoicePolicy.cueVoice(fixed, cue.id, "voice-2"))
    }
}
