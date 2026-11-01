package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class GeminiVoicePresentationTest {
    @Test fun knownRoleTranslatedWithoutChangingApiId() {
        val voice = GeminiVoice("pt-br-advisor-4", "Authoritative Advisor 4", "")
        assertEquals("Conselheiro de tom firme 4", GeminiVoicePresentation.label(voice))
        assertEquals("pt-br-advisor-4", voice.id)
        assertEquals("Authoritative Advisor 4", voice.name)
    }
    @Test fun unknownNamesAreNotInvented() {
        val custom = GeminiVoice("voice_custom", "Mara da Serra", "")
        assertEquals("Mara da Serra", GeminiVoicePresentation.label(custom))
    }
    @Test fun studioDescriptorIsDescriptiveNotRenamedId() {
        val studio = GeminiVoice("Zephyr", "Zephyr", "")
        assertEquals("Zephyr · Brilhante", GeminiVoicePresentation.label(studio))
        assertEquals("Zephyr", studio.id)
    }
    @Test fun metadataOnlyWhenSupplied() {
        val v = GeminiVoice("a", "Example", "", languageCode = "pt-BR", gender = "female",
            pitch = "low", accent = "Mineiro", context = "News")
        val details = GeminiVoicePresentation.details(v)
        assertTrue(details.contains("Português (Brasil)"))
        assertTrue(details.contains("Voz feminina"))
        assertTrue(details.contains("Grave"))
        assertTrue(details.contains("Mineiro"))
        assertTrue(details.contains("News"))
        assertEquals("", GeminiVoicePresentation.details(GeminiVoice("b", "Name", "")))
    }
    @Test fun searchIncludesTranslatedLabelAndMetadata() {
        val voice = GeminiVoice("pt-br-advisor-4", "Authoritative Advisor 4", "", gender = "female")
        assertEquals(listOf(voice), GeminiVoiceSearch.filter(listOf(voice), "conselheiro"))
        assertEquals(listOf(voice), GeminiVoiceSearch.filter(listOf(voice), "voz feminina"))
        assertEquals(listOf(voice), GeminiVoiceSearch.filter(listOf(voice), "ADVISOR"))
    }
    @Test fun previewIdentityChangesWithAccountModelVoiceOrStyle() {
        fun id(key: String = "Key_for_test_only", model: String = "lite", voice: String = "Zephyr",
            style: String = "calm") = GeminiPreviewIdentity.id(key, model, voice, style)
        val first = id()
        assertEquals(first, id())
        assertTrue(first.matches(Regex("[a-f0-9]{64}")))
        assertNotEquals(first, id(key = "Another_test_key"))
        assertNotEquals(first, id(model = "flash"))
        assertNotEquals(first, id(voice = "Puck"))
        assertNotEquals(first, id(style = "cheerful"))
        assertFalse(first.contains("Key_for_test_only"))
    }
}
