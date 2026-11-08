package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class StandaloneVoiceTextPolicyTest {
    @Test fun requiresTextAndVoice() {
        assertFalse(StandaloneVoiceTextPolicy.accepted("", "Kore"))
        assertFalse(StandaloneVoiceTextPolicy.accepted("Teste", ""))
        assertTrue(StandaloneVoiceTextPolicy.accepted("Olá mundo", "Kore"))
    }
    @Test fun enforcesRealClientCharacterLimit() {
        assertTrue(StandaloneVoiceTextPolicy.accepted("A".repeat(4000), "Kore"))
        assertFalse(StandaloneVoiceTextPolicy.accepted("A".repeat(4001), "Kore"))
        assertFalse(StandaloneVoiceTextPolicy.accepted("😀".repeat(2001), "Kore"))
    }
    @Test fun estimateIsOnlyIndicative() {
        assertEquals(0, StandaloneVoiceTextPolicy.tokenEstimate(""))
        assertEquals(2, StandaloneVoiceTextPolicy.tokenEstimate("abcdef"))
        assertEquals(1, StandaloneVoiceTextPolicy.length("😀"))
    }
}
