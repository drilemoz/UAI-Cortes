package br.com.uaicortes

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceIaCompactPolicyTest {
    @Test fun emptyOrNewProjectNeverPrompts() {
        assertFalse(VoiceIaCompactPolicy.shouldRemind(0, 0, true))
        assertFalse(VoiceIaCompactPolicy.shouldRemind(26, 0, false))
        assertFalse(VoiceIaCompactPolicy.shouldRemind(26, 26, false))
    }
    @Test fun unfinishedEarlierGenerationPromptsOnceOnScreenEntry() {
        assertTrue(VoiceIaCompactPolicy.shouldRemind(26, 11, true))
        assertTrue(VoiceIaCompactPolicy.shouldRemind(26, 0, true))
        assertFalse(VoiceIaCompactPolicy.shouldRemind(26, 26, true))
    }
}
