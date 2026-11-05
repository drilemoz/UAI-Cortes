package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class GeminiQuotaPolicyTest {
    private val now = Instant.parse("2026-11-05T20:00:00Z").toEpochMilli()
    private fun event(project: String = "project-a", model: String = "model-tts", op: String = "narracao",
        at: Long = now - 120_000, input: Long? = 80, total: Long? = 300,
        status: String = "ok") = GeminiUsageEvent(at, project, model, op, status,
            if (input == null && total == null) null else GeminiTokenUsage(input, null, null, null, total))

    @Test fun fiveMeasuredSamplesProduceAverageAndLocalRpdProjection() {
        val events = (1..5).map { event() }
        val forecast = GeminiQuotaPolicy.compute(events, "project-a", "model-tts", "narração",
            GeminiManualLimits(null, null, 20), now)
        assertEquals(5, forecast.samples)
        assertEquals(300L, forecast.averageTotal)
        assertEquals(80L, forecast.averageInput)
        assertEquals(15, forecast.remainingByLocalLimits)
    }

    @Test fun neverMixFunctionsModelsOrProjects() {
        val events = listOf(event(),event(),event("project-b"),event(model="asr"),event(op="legendas"))
        val forecast = GeminiQuotaPolicy.compute(events, "project-a", "model-tts", "narracao",
            GeminiManualLimits(null, null, 15), now)
        assertEquals(2, forecast.samples)
        assertNull(forecast.averageTotal)
        assertEquals(12, forecast.remainingByLocalLimits) // três operações desse projeto e modelo
    }

    @Test fun noConfiguredLimitNeverInventsRemainingCredit() {
        val result = GeminiQuotaPolicy.compute(List(10) { event() }, "project-a", "model-tts",
            "narracao", GeminiManualLimits(null, null, null), now)
        assertEquals(300L, result.averageTotal)
        assertNull(result.remainingByLocalLimits)
        assertTrue(result.label.contains("Sem limites"))
    }

    @Test fun tpmCountsInputNotOutputOrTotal() {
        val events = List(5) { event(at = now - 5_000, input=80,total=800) }
        val result = GeminiQuotaPolicy.compute(events, "project-a", "model-tts", "narracao",
            GeminiManualLimits(null, 1000, null), now)
        assertEquals(7, result.remainingByLocalLimits) // (1000 - 5*80) / 80
    }

    @Test fun missingInputMeansUnknownTpmCapacity() {
        val events = List(5) { event(input = null, total=350) }
        val result = GeminiQuotaPolicy.compute(events, "project-a", "model-tts", "narracao",
            GeminiManualLimits(null, 1000, null), now)
        assertNull(result.remainingByLocalLimits)
        assertNull(result.averageInput)
    }

    @Test fun rpmAndDailyLimitsUseMostRestrictiveDimension() {
        val events = List(5) { event(at = now - 300) }
        val result = GeminiQuotaPolicy.compute(events, "project-a", "model-tts", "narracao",
            GeminiManualLimits(6, null, 100), now)
        assertEquals(1, result.remainingByLocalLimits)
    }

    @Test fun unknownProjectNeverInheritsHistoricalEvents() {
        val events = List(5) { event(project = "Projeto não informado") }
        val result = GeminiQuotaPolicy.compute(events, "real-new-id", "model-tts", "narracao",
            GeminiManualLimits(null, null, 20), now)
        assertNull(result.averageTotal)
        assertEquals(20, result.remainingByLocalLimits)
    }

    @Test fun pacificDateControlsDailyCounter() {
        val early = Instant.parse("2026-11-05T06:00:00Z").toEpochMilli() // dia 04 no Pacífico
        val result = GeminiQuotaPolicy.compute(listOf(event(at=early)), "project-a", "model-tts",
            "narracao", GeminiManualLimits(null, null, 10), now)
        assertEquals(0, result.usedToday)
        assertEquals(10, result.remainingByLocalLimits)
    }
}
