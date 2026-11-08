package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

/** Prévia offline e julgamento auditivo manual; sem biometria nem chamadas de rede. */
class GeminiVoiceBlindCompare234Test {
    private fun voice(id: String) = GeminiVoice(id, id, "", "pt-BR", "", "", "", "", "")

    @Test fun `offline pairs need two saved distinct voices`() {
        val ids = listOf("a", "b", "b", "c")
        assertNull(GeminiVoiceDiscovery.nextOfflinePair(ids, setOf("a"), emptyMap()))
        assertEquals("a" to "c", GeminiVoiceDiscovery.nextOfflinePair(ids, setOf("a", "c"), emptyMap()))
    }

    @Test fun `unreviewed pair is preferred and last pair avoided`() {
        val ab = GeminiVoiceDiscovery.pairKey("a", "b")!!
        val reviews = mapOf(ab to GeminiVoiceDiscovery.SIMILAR)
        assertEquals("a" to "c", GeminiVoiceDiscovery.nextOfflinePair(
            listOf("a", "b", "c"), setOf("a", "b", "c"), reviews))
        assertEquals("a" to "b", GeminiVoiceDiscovery.nextOfflinePair(
            listOf("a", "b"), setOf("a", "b"), reviews, ab))
    }

    @Test fun `different pair prevents transitive hiding`() {
        val voices = listOf(voice("a"), voice("b"), voice("c"))
        val reviews = mapOf(
            GeminiVoiceDiscovery.pairKey("a", "b")!! to GeminiVoiceDiscovery.SIMILAR,
            GeminiVoiceDiscovery.pairKey("b", "c")!! to GeminiVoiceDiscovery.SIMILAR,
            GeminiVoiceDiscovery.pairKey("a", "c")!! to GeminiVoiceDiscovery.DIFFERENT)
        val hidden = GeminiVoiceDiscovery.hiddenFromReview(voices, reviews, emptySet(), emptySet())
        assertEquals(setOf("b"), hidden)
        assertTrue("a" !in hidden && "c" !in hidden)
    }
}
