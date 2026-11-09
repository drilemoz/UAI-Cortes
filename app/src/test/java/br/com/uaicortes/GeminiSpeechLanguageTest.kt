package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class GeminiSpeechLanguageTest {
    @Test fun portugueseGuidancePreservesOriginalStyle() {
        val prepared = GeminiSpeechLanguage.style("warm narration", GeminiSpeechLanguage.PORTUGUESE_BR)
        assertTrue(prepared.startsWith("warm narration"))
        assertTrue(prepared.contains("Brazilian Portuguese (pt-BR)"))
        assertTrue(prepared.contains("Do not translate"))
    }

    @Test fun automaticModeDoesNotForcePortuguese() {
        assertEquals("neutral", GeminiSpeechLanguage.style(" neutral ", GeminiSpeechLanguage.AUTOMATIC))
        assertEquals("", GeminiSpeechLanguage.style(" ", GeminiSpeechLanguage.AUTOMATIC))
    }

    @Test fun rejectsUnknownLanguageWithoutNetwork() {
        try {
            GeminiSpeechLanguage.style("neutral", "en-UK")
            fail("Unknown mode should not be sent to Gemini")
        } catch (_: IllegalArgumentException) { }
    }
}
