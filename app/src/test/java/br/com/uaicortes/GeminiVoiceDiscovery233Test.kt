package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

/** O registro local não é prova de identidade acústica. */
class GeminiVoiceDiscovery233Test {
    private fun voice(id: String, lang: String = "pt-BR") = GeminiVoice(
        id, id, "", lang, "", "", "", "", "")

    @Test fun `pair order is stable and repeated voice invalid`() {
        val a = GeminiVoiceDiscovery.pairKey("b", "a")
        assertEquals(a, GeminiVoiceDiscovery.pairKey("a", "b"))
        assertNull(GeminiVoiceDiscovery.pairKey("a", "a"))
    }

    @Test fun `manual similarities preserve a favorite and are optional`() {
        val list = listOf(voice("a"), voice("b"), voice("c"), voice("d"))
        val reviews = mapOf(
            GeminiVoiceDiscovery.pairKey("a", "b")!! to GeminiVoiceDiscovery.SIMILAR,
            GeminiVoiceDiscovery.pairKey("b", "c")!! to GeminiVoiceDiscovery.SIMILAR,
            GeminiVoiceDiscovery.pairKey("c", "d")!! to GeminiVoiceDiscovery.DIFFERENT)
        assertEquals(setOf("a", "b"), GeminiVoiceDiscovery.hiddenFromReview(list, reviews, setOf("c"), emptySet()))
        assertTrue(GeminiVoiceDiscovery.hiddenFromReview(list, emptyMap(), emptySet(), emptySet()).isEmpty())
    }

    @Test fun `merge keeps previous voices and adds others without duplication`() {
        val local = listOf(voice("br"))
        val incoming = listOf(voice("en", "en-US"), voice("br", "pt-BR"))
        val merged = GeminiVoiceDiscovery.merge(local, incoming, limit = 2)
        assertEquals(listOf("br", "en"), merged.map { it.id })
        assertFalse(GeminiVoiceDiscovery.isPortuguese(merged[1]))
        assertTrue(GeminiVoiceDiscovery.isPortuguese(merged[0]))
    }
}
