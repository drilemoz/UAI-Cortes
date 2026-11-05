package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class VoiceIaDisplayPolicyTest {
    @Test fun warnsOnlyWhenAudioExceedsOriginalCueDuration() {
        assertEquals(0L, VoiceIaDisplayPolicy.overrunMs(500, 2_800, 2_300))
        assertEquals(0L, VoiceIaDisplayPolicy.overrunMs(500, 2_800, 2_000))
        assertEquals(750L, VoiceIaDisplayPolicy.overrunMs(500, 2_800, 3_050))
    }

    @Test fun excessSecondsNeverHideShortOverrun() {
        assertEquals("0.1", VoiceIaDisplayPolicy.seconds(1))
        assertEquals("0.7", VoiceIaDisplayPolicy.seconds(700))
        assertEquals("0.8", VoiceIaDisplayPolicy.seconds(750))
        assertEquals("1.2", VoiceIaDisplayPolicy.seconds(1_200))
    }
}
