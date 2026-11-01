package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class GeminiNarrationStylingTest {
    @Test fun allPresetsHaveUniqueNamesAndRealDirections() {
        val presets = GeminiNarrationStyling.presets
        assertTrue(presets.size >= 14)
        assertEquals(presets.size, presets.map { it.label }.toSet().size)
        assertTrue(presets.all { it.direction.isNotBlank() })
        listOf("Neutra", "Sussurrada", "Sarcástica", "Dramática", "Conversacional")
            .forEach { assertTrue(presets.any { p -> p.label == it }) }
    }
    @Test fun oldDefaultDirectionsRemainUnchangedForCachedSamples() {
        assertEquals("clear, neutral and natural Brazilian Portuguese narration",
            GeminiNarrationStyling.instruction("Neutra"))
        assertEquals("cheerful, upbeat and expressive", GeminiNarrationStyling.instruction("Animada"))
        assertEquals("softly whispered, intimate", GeminiNarrationStyling.instruction("Sussurrada"))
    }
    @Test fun independentAdjustmentsComposeOnlyMetadataInstruction() {
        val basic = GeminiNarrationStyling.instruction("Jornalística")
        val adjusted = GeminiNarrationStyling.instruction("Jornalística", 4, 0, 3)
        assertTrue(adjusted.startsWith(basic))
        assertTrue(adjusted.contains("rapidly"))
        assertTrue(adjusted.contains("restrained"))
        assertTrue(adjusted.contains("higher vocal pitch"))
        assertTrue(adjusted.contains("informative broadcast newsreader"))
        assertNotEquals(adjusted, GeminiNarrationStyling.instruction("Jornalística", 4, 0, 4))
    }
    @Test fun adjustsEachParameterWithoutChangingPreset() {
        val basic = GeminiNarrationStyling.instruction("Neutra")
        assertNotEquals(basic, GeminiNarrationStyling.instruction("Neutra", pace = 1))
        assertNotEquals(basic, GeminiNarrationStyling.instruction("Neutra", expressiveness = 3))
        assertNotEquals(basic, GeminiNarrationStyling.instruction("Neutra", pitch = 0))
        assertEquals(basic, GeminiNarrationStyling.instruction("Neutra", 2, 2, 2))
    }
    @Test fun allCombinationsHaveDistinctInstructionsAndCacheKeys() {
        val key = "fake-key-used-only-for-test"
        val unique = mutableSetOf<String>()
        for (preset in GeminiNarrationStyling.presets) {
            for (pace in 0..4) for (emotion in 0..4) for (pitch in 0..4) {
                val direction = GeminiNarrationStyling.instruction(preset.label, pace, emotion, pitch)
                val id = GeminiPreviewIdentity.id(key, "gemini-3.8-flash-lite-tts", "test-voice", direction)
                assertEquals(64, id.length)
                assertTrue(unique.add(id))
                assertEquals(id, GeminiPreviewIdentity.id(key, "gemini-3.8-flash-lite-tts", "test-voice", direction))
            }
        }
        assertEquals(16 * 5 * 5 * 5, unique.size)
    }
    @Test fun invalidValuesAreRejectedInsteadOfInventingSettings() {
        for (args in listOf(listOf(-1, 2, 2), listOf(2, 5, 2), listOf(2, 2, 7))) {
            try {
                GeminiNarrationStyling.instruction("Neutra", args[0], args[1], args[2])
                fail("Parâmetro fora da faixa foi aceito")
            } catch (_: IllegalArgumentException) { }
        }
        try { GeminiNarrationStyling.instruction("Inventado"); fail() }
        catch (_: IllegalArgumentException) { }
    }
}
