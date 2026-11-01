package br.com.uaicortes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiVoiceSearchTest {
    private val voices = listOf(
        GeminiVoice("voices/kore", "Kore", "Firm"),
        GeminiVoice("custom-ptbr-2", "Conselheiro São Paulo", ""),
        GeminiVoice("voices/puck", "Puck", "Upbeat")
    )

    @Test fun searchesRealIdAndKeepsItUnchanged() {
        assertEquals("custom-ptbr-2", GeminiVoiceSearch.filter(voices, "ptbr-2").single().id)
    }
    @Test fun searchIgnoresCaseAndAccents() {
        assertEquals("custom-ptbr-2", GeminiVoiceSearch.filter(voices, "sAo paUlO").single().id)
    }
    @Test fun blankSearchReturnsAllInOriginalOrder() {
        assertEquals(voices, GeminiVoiceSearch.filter(voices, "   "))
    }
    @Test fun unknownSearchReturnsEmpty() {
        assertTrue(GeminiVoiceSearch.filter(voices, "inexistente").isEmpty())
    }
}
