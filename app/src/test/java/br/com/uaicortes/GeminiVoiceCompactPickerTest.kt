package br.com.uaicortes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiVoiceCompactPickerTest {
    private val voices = listOf(
        GeminiVoice("pt-br-advisor-1", "Authoritative Advisor 1", "A long English description",
            languageCode = "pt-BR", gender = "female", pitch = "medium"),
        GeminiVoice("pt-br-advisor-10", "Authoritative Advisor 10", "Another long description",
            languageCode = "pt-BR", gender = "male"),
        GeminiVoice("voice-neutral", "Custom Name", "", gender = "neutral"),
        GeminiVoice("voice-undisclosed", "Undisclosed", "")
    )

    @Test fun compactLabelsUseOnlyProvidedMetadata() {
        assertEquals("Feminina · Português (Brasil) · Média",
            GeminiVoicePresentation.shortDetails(voices[0]))
        assertEquals("Masculina · Português (Brasil)",
            GeminiVoicePresentation.shortDetails(voices[1]))
        assertEquals("Neutra", GeminiVoicePresentation.shortDetails(voices[2]))
        assertEquals("Características não informadas", GeminiVoicePresentation.shortDetails(voices[3]))
    }

    @Test fun genderFiltersNeverGuessMissingMetadata() {
        assertEquals(listOf(voices[0]), GeminiVoiceSearch.byGender(voices, "Femininas"))
        assertEquals(listOf(voices[1]), GeminiVoiceSearch.byGender(voices, "Masculinas"))
        assertEquals(voices, GeminiVoiceSearch.byGender(voices, "Todas"))
    }

    @Test fun filterAndSearchPreserveOriginalVoiceObjectsAndIdentifiers() {
        val result = GeminiVoiceSearch.byGender(
            GeminiVoiceSearch.filter(voices, "CONSELHEIRO"), "Masculinas")
        assertEquals(listOf(voices[1]), result)
        assertEquals("pt-br-advisor-10", result.single().id)
        assertEquals("Authoritative Advisor 10", result.single().name)
    }

    @Test fun femaleFilterNeverConfirmsPreviouslySelectedMaleVoice() {
        val visibleFemale = GeminiVoiceSearch.byGender(voices, "Femininas")
        assertEquals(1, visibleFemale.size)
        assertEquals(null, GeminiVoiceChoice.visibleChoice(visibleFemale, "pt-br-advisor-10"))
        assertEquals(null, GeminiVoiceChoice.visibleIdOrNull(visibleFemale, "pt-br-advisor-10"))
        assertEquals("pt-br-advisor-1", GeminiVoiceChoice.visibleIdOrNull(visibleFemale, "pt-br-advisor-1"))
        assertEquals("female", GeminiVoiceChoice.visibleChoice(visibleFemale, "pt-br-advisor-1")?.gender)
        assertEquals("pt-br-advisor-1", GeminiVoiceChoice.visibleChoice(visibleFemale, "pt-br-advisor-1")?.id)
    }

    @Test fun hiddenVoiceAfterSearchCannotBeConfirmed() {
        val visibleFemale = GeminiVoiceSearch.byGender(GeminiVoiceSearch.filter(voices, "Conselheiro de tom firme 1"), "Femininas")
        assertEquals(null, GeminiVoiceChoice.visibleChoice(visibleFemale, "pt-br-advisor-10"))
    }

    @Test fun confirmationIsExplicitAndDoesNotGuessFirstVoice() {
        assertEquals(null, GeminiVoiceChoice.confirmed(voices, null))
        assertEquals(null, GeminiVoiceChoice.confirmed(voices, "unlisted"))
        assertEquals(voices[1], GeminiVoiceChoice.confirmed(voices, "pt-br-advisor-10"))
    }
}
