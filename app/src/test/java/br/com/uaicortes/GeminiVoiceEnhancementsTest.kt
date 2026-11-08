package br.com.uaicortes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** A API não deve ser chamada e o catálogo não deve ser inferido durante os filtros. */
class GeminiVoiceEnhancementsTest {
    private val catalog = listOf(
        GeminiVoice("voices/1", "Voz 1", "", "pt-BR", "Brasil", "calm", "narration", "male", "low"),
        GeminiVoice("voices/2", "Voz 2", "", "pt-BR", "Portugal", "energetic", "news", "female", "high"),
        GeminiVoice("voices/3", "Voz 3", "", "pt-BR", "", "", "", "", "")
    )
    @Test fun filtersUseOnlyActualMetadata() {
        assertEquals(listOf("Brasil", "Portugal"), GeminiVoiceSearch.values(catalog, "accent"))
        assertEquals("voices/1", GeminiVoiceSearch.byMetadata(catalog, "context", "narration").single().id)
        assertEquals(3, GeminiVoiceSearch.byMetadata(catalog, "accent", "").size)
        assertEquals(0, GeminiVoiceSearch.byMetadata(catalog, "accent", "mineiro").size)
    }
    @Test fun aliasesAreLocalAndNeverChangeApiIdentity() {
        val alias = mapOf("voices/1" to "Meu narrador")
        assertEquals("Meu narrador", GeminiVoiceAliases.title(catalog[0], alias))
        assertEquals("voices/1", GeminiVoiceSearch.filterWithAliases(catalog, "narrador", alias).single().id)
        assertEquals("Voz 1", catalog[0].name)
        assertFalse(GeminiVoiceAliases.validAlias("a".repeat(51)))
        assertTrue(GeminiVoiceAliases.validAlias("Voz documental"))
    }
    @Test fun previewPresetAndCacheIdentityStaySafe() {
        assertEquals(GeminiPreviewIdentity.TEXT,
            GeminiVoiceSampleText.content(GeminiVoiceSampleText.STANDARD, ""))
        assertTrue(GeminiVoiceSampleText.content(GeminiVoiceSampleText.SHORT, "").length <
            GeminiPreviewIdentity.TEXT.length)
        assertFalse(GeminiVoiceSampleText.customValid("  "))
        assertTrue(GeminiVoiceSampleText.customValid("Olá, esta é minha voz."))
        val old = GeminiPreviewIdentity.id("ignored", "flash", "voices/1", "neutra")
        val same = GeminiPreviewIdentity.idForText("other", "flash", "voices/1", "neutra", GeminiPreviewIdentity.TEXT)
        val short = GeminiPreviewIdentity.idForText("ignored", "flash", "voices/1", "neutra", GeminiVoiceSampleText.SHORT_TEXT)
        assertEquals(old, same)
        assertFalse(old == short)
    }
}
