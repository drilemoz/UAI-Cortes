package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class GeminiVoiceChoiceTest {
    private val catalog = listOf(
        GeminiVoice("pt-br-advisor-1", "Advisor 1", ""),
        GeminiVoice("pt-br-advisor-2", "Advisor 2", "")
    )

    @Test fun confirmsOnlyExistingChoice() {
        assertEquals("pt-br-advisor-2", GeminiVoiceChoice.confirmed(catalog, "pt-br-advisor-2")?.id)
        assertNull(GeminiVoiceChoice.confirmed(catalog, "outro-id"))
    }

    @Test fun canceledChoiceDoesNotSelectAutomatically() {
        assertNull(GeminiVoiceChoice.confirmed(catalog, null))
        assertNull(GeminiVoiceChoice.confirmed(emptyList(), "pt-br-advisor-1"))
    }

    @Test fun selectionDoesNotModifyCatalog() {
        GeminiVoiceChoice.confirmed(catalog, "pt-br-advisor-1")
        assertEquals(listOf("pt-br-advisor-1", "pt-br-advisor-2"), catalog.map { it.id })
    }
}
