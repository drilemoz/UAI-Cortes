package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class NarrationGenerationEligibilityTest {
    private fun reasons(
        busy: Boolean = false, voice: Boolean = true, catalogEmpty: Boolean = false,
        sync: Boolean = true, validation: String? = null, blank: Boolean = false,
        text: Boolean = true, remaining: Long = 9000L
    ) = NarrationGenerationEligibility.blockingReasons(busy, voice, catalogEmpty,
        sync, validation, blank, text, remaining)

    @Test fun allowsValidTimedGeneration() = assertTrue(reasons().isEmpty())
    @Test fun explainsMissingVoiceCatalog() = assertTrue(reasons(voice=false, catalogEmpty=true).single().contains("Consulte"))
    @Test fun explainsVoiceSelection() = assertTrue(reasons(voice=false).single().contains("Escolha"))
    @Test fun explainsOversizedBatchEvenWithoutVoice() {
        val issues = reasons(voice=false, catalogEmpty=true, validation="Escolha até 12 falas")
        assertEquals(2, issues.size)
        assertTrue(issues.any { it.contains("12") })
    }
    @Test fun explainsInvalidSubtitleTiming() = assertTrue(reasons(validation="Legenda fora do vídeo").first().contains("fora"))
    @Test fun blankSegmentNeedsReview() = assertTrue(reasons(blank=true).first().contains("sem texto"))
    @Test fun continuousTextRequired() = assertTrue(reasons(sync=false,text=false).first().contains("texto"))
    @Test fun continuousRequiresRemainingTime() = assertTrue(reasons(sync=false,remaining=100).single().contains("cursor"))
    @Test fun busyBlocksWithoutAdditionalMessages() = assertEquals(1, reasons(busy=true,voice=false,validation="Inválido").size)
}
