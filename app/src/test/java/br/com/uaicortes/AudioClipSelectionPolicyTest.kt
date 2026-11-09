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
    @Test fun labelsAndFilename() {
        assertEquals("01:02.3", AudioClipSelectionPolicy.time(62_340))
        assertEquals("1:01:02.3", AudioClipSelectionPolicy.time(3_662_399))
        assertEquals("Narração dos ETs", AudioClipSelectionPolicy.exportBase("Narração dos ETs.mp3"))
        assertEquals("Trecho de áudio", AudioClipSelectionPolicy.exportBase("...."))
        assertFalse(AudioClipSelectionPolicy.exportBase("../../arquivo?.wav").contains('/'))
    }
}
