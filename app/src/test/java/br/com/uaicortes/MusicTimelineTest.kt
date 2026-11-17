package br.com.uaicortes

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class MusicTimelineTest {
    private val music = Music("content://music", "Tema", .25f,
        startMs = 2_000L, sourceDurationMs = 3_000L, repeat = false)

    @Test fun newMusicDoesNotLoopOrExtendToProjectEnd() {
        val window = requireNotNull(ExportAudioPolicy.musicWindow(music, 9_000L))
        assertEquals(2_000L, window.timelineStartMs)
        assertEquals(0L, window.sourceStartMs)
        assertEquals(3_000L, window.playableMs)
        assertEquals(4_000L, window.trailingMs)
        assertFalse(music.loops)
    }

    @Test fun shorterProjectOrMediaDoesNotExtendMusic() {
        assertEquals(1_000L, requireNotNull(ExportAudioPolicy.musicWindow(music, 3_000L)).playableMs)
        assertEquals(1_000L, requireNotNull(ExportAudioPolicy.musicWindow(music, 9_000L, 1_000L)).playableMs)
        assertEquals(3_000L, requireNotNull(ExportAudioPolicy.musicWindow(music, 9_000L, 4_000L)).playableMs)
        assertNull(ExportAudioPolicy.musicWindow(music, 2_000L))
    }

    @Test fun saveRoundTripKeepsPositionAndOlderMusicStillRepeatsFromZero() {
        assertEquals(music, Music.read(JSONObject(music.json().toString())))
        val old = Music.read(JSONObject().put("uri", "content://music").put("title", "Antiga"))
        assertTrue(old.loops)
        assertEquals(0L, old.startMs)
        assertEquals(12_000L, requireNotNull(ExportAudioPolicy.musicWindow(old, 12_000L)).playableMs)
    }

    @Test fun excludedOrMutedMusicDoesNotRequireAudioInAnOtherwiseSilentProject() {
        val photo = Clip(uri = "content://foto", title = "Foto", sourceDurationMs = 2_000L, isImage = true)
        val project = VideoProject(clips = listOf(photo), music = music)
        assertFalse(ExportAudioPolicy.hasRequiredAudio(project))
        assertTrue(ExportAudioPolicy.hasRequiredAudio(project.copy(music = music.copy(startMs = 0L))))
        assertFalse(ExportAudioPolicy.hasRequiredAudio(project.copy(music = music.copy(startMs = 0L, volume = 0f))))
    }
}
