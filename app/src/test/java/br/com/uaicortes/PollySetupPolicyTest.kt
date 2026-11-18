package br.com.uaicortes

import br.com.uaicortes.PollySetupPolicy.Source
import org.junit.Assert.*
import org.junit.Test

class PollySetupPolicyTest {
    @Test fun testingWorksBeforeSavingWithBothFields() {
        assertEquals(Source.FORM, PollySetupPolicy.source(true, false, "id", "segredo"))
        assertEquals(Source.FORM, PollySetupPolicy.source(true, true, "novo", "segredo"))
    }

    @Test fun editingNeverUsesHalfOfThePreviouslySavedCredentials() {
        assertEquals(Source.NONE, PollySetupPolicy.source(true, true, "novo", " "))
        assertEquals(Source.NONE, PollySetupPolicy.source(true, true, "", "novo"))
        assertEquals(Source.SAVED, PollySetupPolicy.source(false, true, "", ""))
        assertEquals(Source.NONE, PollySetupPolicy.source(false, false, "", ""))
    }

    @Test fun defaultVoiceCanBeUsedBeforeListingAndTypeLabelsAreLocalized() {
        assertEquals("pt-BR", PollySetupPolicy.fallbackVoice.language)
        assertEquals("standard", PollySetupPolicy.preferredEngine(PollySetupPolicy.fallbackVoice))
        assertEquals("neural", PollySetupPolicy.preferredEngine(
            PollyVoice("teste", "Teste", "pt-BR", listOf("standard", "neural"))))
        assertEquals("Padrão", PollySetupPolicy.engineName("standard"))
        assertEquals("Longa duração", PollySetupPolicy.engineName("long-form"))
        assertTrue(PollySetupPolicy.SAMPLE.length < 100)
    }
}
