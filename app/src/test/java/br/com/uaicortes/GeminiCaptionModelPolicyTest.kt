package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class GeminiCaptionModelPolicyTest {
    @Test fun acceptsOnlyTextGenerationModelIds() {
        assertEquals("gemini-2.5-flash", GeminiCaptionModelPolicy.eligible(
            "models/gemini-2.5-flash", listOf("generateContent", "countTokens")))
        assertNull(GeminiCaptionModelPolicy.eligible("models/gemini-2.5-flash",
            listOf("countTokens")))
        assertNull(GeminiCaptionModelPolicy.eligible("models/gemini-2.5-flash-tts",
            listOf("generateContent")))
        assertNull(GeminiCaptionModelPolicy.eligible("models/gemini-3.0-flash-image",
            listOf("generateContent")))
        assertNull(GeminiCaptionModelPolicy.eligible("https://other-host/models/gemini-2.5-flash",
            listOf("generateContent")))
        assertFalse(GeminiCaptionModelPolicy.validId("gemini-2.5-flash/../other"))
    }

    @Test fun invalidSavedModelRestoresSafeDefault() {
        assertEquals(GeminiCaptionModelPolicy.DEFAULT_MODEL,
            GeminiCaptionModelPolicy.selectedOrDefault("../../fake:generateContent"))
        assertEquals("gemini-3.1-flash-lite",
            GeminiCaptionModelPolicy.selectedOrDefault("gemini-3.1-flash-lite"))
    }

    @Test fun sortsAndDeduplicatesModelsWithoutInventingAvailability() {
        val models = listOf(
            GeminiCaptionModel("gemini-2.5-pro", "Pro"),
            GeminiCaptionModel("gemini-2.5-flash-lite", "Flash Lite"),
            GeminiCaptionModel("gemini-2.5-flash-lite", "Flash Lite again"),
            GeminiCaptionModel("gemini-2.5-flash", "Flash"))
        assertEquals(listOf("gemini-2.5-flash-lite", "gemini-2.5-flash", "gemini-2.5-pro"),
            GeminiCaptionModelPolicy.order(models).map { it.id })
    }
}
