package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class AudioClipSelectionPolicyTest {
    @Test fun validRangeRequiresAudioAndOneTenthSecond() {
        assertTrue(AudioClipSelectionPolicy.valid(10_000, 1_000, 5_000))
        assertTrue(AudioClipSelectionPolicy.valid(10_000, 0, 10_000))
        assertFalse(AudioClipSelectionPolicy.valid(10_000, 0, 99))
        assertFalse(AudioClipSelectionPolicy.valid(10_000, -1, 9_000))
        assertFalse(AudioClipSelectionPolicy.valid(10_000, 8_000, 10_001))
        assertFalse(AudioClipSelectionPolicy.valid(0, 0, 0))
    }
    @Test fun handlesCannotCross() {
        assertEquals(4_900, AudioClipSelectionPolicy.start(10_000, 9_500, 5_000))
        assertEquals(5_100, AudioClipSelectionPolicy.end(10_000, 5_000, 1_000))
        assertEquals(10_000, AudioClipSelectionPolicy.end(10_000, 5_000, 20_000))
    }
    @Test fun cursorTracksPlayerPositionAndClampsToSelection() {
        assertEquals(0f, AudioClipSelectionPolicy.playheadFraction(0L, 300L), 0.0001f)
        assertEquals(0.5f, AudioClipSelectionPolicy.playheadFraction(10_000L, 5_000L), 0.0001f)
        assertEquals(1f, AudioClipSelectionPolicy.playheadFraction(10_000L, 40_000L), 0.0001f)
        assertEquals(0f, AudioClipSelectionPolicy.playheadFraction(10_000L, -5L), 0.0001f)
        assertEquals(2_000L, AudioClipSelectionPolicy.seekFromFraction(10_000L, 2_000L, 8_000L, 0f))
        assertEquals(5_000L, AudioClipSelectionPolicy.seekFromFraction(10_000L, 2_000L, 8_000L, 0.5f))
        assertEquals(8_000L, AudioClipSelectionPolicy.seekFromFraction(10_000L, 2_000L, 8_000L, 1f))
        assertEquals(0L, AudioClipSelectionPolicy.seekFromFraction(0L, 0L, 0L, 1f))
        assertEquals(2_000L, AudioClipSelectionPolicy.seekFromFraction(10_000L, 2_000L, 8_000L, Float.NaN))
    }
    @Test fun labelsAndFilename() {
        assertEquals("01:02.3", AudioClipSelectionPolicy.time(62_340))
        assertEquals("1:01:02.3", AudioClipSelectionPolicy.time(3_662_399))
        assertEquals("Narração dos ETs", AudioClipSelectionPolicy.exportBase("Narração dos ETs.mp3"))
        assertEquals("Trecho de áudio", AudioClipSelectionPolicy.exportBase("...."))
        assertFalse(AudioClipSelectionPolicy.exportBase("../../arquivo?.wav").contains('/'))
    }
}
