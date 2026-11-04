package br.com.uaicortes

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class GeminiUsagePolicyTest {
    @Test fun generateContentKeepsAuthoritativeTotalWithoutAddingThoughtsTwice() {
        val response = JSONObject().put("usageMetadata", JSONObject()
            .put("promptTokenCount", 100).put("candidatesTokenCount", 40)
            .put("thoughtsTokenCount", 60).put("cachedContentTokenCount", 15)
            .put("totalTokenCount", 200))
        val usage = GeminiUsagePolicy.parse(response)!!
        assertEquals(200L, usage.total)
        assertEquals(100L, usage.input)
        assertEquals(40L, usage.output)
        assertEquals(60L, usage.thoughts)
        assertEquals(15L, usage.cached)
    }

    @Test fun interactionsCountsAudioFromProviderWithoutInventingTotals() {
        val usage = GeminiUsagePolicy.parse(JSONObject().put("usage", JSONObject()
            .put("total_input_tokens", 950).put("total_output_tokens", 310)
            .put("total_thought_tokens", 18).put("total_tokens", 1278)))!!
        assertEquals(1278L, usage.total)
        assertNull(usage.cached)
        assertEquals(310L, usage.output)
    }

    @Test fun missingOrIncompleteUsageIsNotReportedAsZero() {
        assertNull(GeminiUsagePolicy.parse(JSONObject().put("candidates", org.json.JSONArray())))
        val partial = GeminiUsagePolicy.parse(JSONObject().put("usageMetadata", JSONObject()
            .put("promptTokenCount", 15)))!!
        assertNull(partial.total)
        assertNull(partial.output)
        assertEquals(15L, partial.input)
    }

    @Test fun localEstimateIsMarkedAsEstimatedNotAsCredits() {
        val notice = GeminiUsagePolicy.estimateText("Um jovem joga videogame no quarto".length)
        assertTrue(notice.contains("estimativa local"))
        assertFalse(notice.contains("créditos restantes"))
    }
}
