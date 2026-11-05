package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class GeminiQuotaWindowTest {
    private val now = Instant.parse("2026-11-05T20:00:00Z").toEpochMilli()
    private fun event(at: Long = now - 5_000L, project: String = "cloud-a",
        model: String = "tts", input: Long? = 60L) = GeminiUsageEvent(at, project,
        model, "narracao", "ok", if (input == null) null
            else GeminiTokenUsage(input, null, null, null, input * 2))

    @Test fun rpmAndRpdAreSeparateWindows() {
        val result = GeminiQuotaPolicy.compute(List(5) { event() }, "cloud-a", "tts",
            "narração", GeminiManualLimits(6, null, 100), now)
        assertEquals(1, result.remainingNow)
        assertEquals(1, result.remainingByRpm)
        assertEquals(95, result.remainingToday)
        assertEquals(5, result.usedLastMinute)
    }

    @Test fun nextWindowAvailableWithoutInventingDailyReset() {
        val samples = List(6) { event() }
        val result = GeminiQuotaPolicy.compute(samples, "cloud-a", "tts", "narracao",
            GeminiManualLimits(6, null, 200), now)
        assertEquals(0, result.remainingNow)
        assertEquals(194, result.remainingToday)
        assertEquals(55_500L, result.nextRpmWindowMs)
        assertEquals(55_500L, GeminiQuotaPolicy.rpmWaitMs(samples, "cloud-a", "tts", 6, now))
    }

    @Test fun ignoresOtherModelAndProjectWhenPacing() {
        val events = listOf(event(), event(project = "cloud-b"), event(model = "asr"))
        assertNull(GeminiQuotaPolicy.rpmWaitMs(events, "cloud-a", "tts", 2, now))
    }

    @Test fun anExcessOfLocalCallsWaitsUntilEnoughExpire() {
        val events = listOf(event(at = now - 59_000), event(at = now - 40_000),
            event(at = now - 10_000))
        assertEquals(20_500L, GeminiQuotaPolicy.rpmWaitMs(events, "cloud-a", "tts", 2, now))
    }

    @Test fun dailyLimitExhaustionIsNotMistakenForRpmAvailability() {
        val result = GeminiQuotaPolicy.compute(List(5) { event(at = now - 120_000) },
            "cloud-a", "tts", "narracao", GeminiManualLimits(10, null, 5), now)
        assertEquals(10, result.remainingByRpm)
        assertEquals(0, result.remainingToday)
        assertEquals(0, result.remainingNow)
    }

    @Test fun unknownInputCannotProduceFakeTpmCapacity() {
        val events = List(5) { event(input = null) }
        val result = GeminiQuotaPolicy.compute(events, "cloud-a", "tts",
            "narracao", GeminiManualLimits(null, 1000, null), now)
        assertNull(result.remainingByTpm)
        assertNull(result.remainingNow)
    }

    @Test fun dailyPacificResetIsIndependentOfUtcDate() {
        val yesterdayPacific = Instant.parse("2026-11-05T06:00:00Z").toEpochMilli()
        val result = GeminiQuotaPolicy.compute(listOf(event(at = yesterdayPacific)),
            "cloud-a", "tts", "narracao", GeminiManualLimits(null, null, 12), now)
        assertEquals(12, result.remainingToday)
    }

    @Test fun planCountsOnlyReusableCuesOnce() {
        val plan = VoiceIaBatchPlanner.plan(listOf(VoiceIaBatchItem("1", true),
            VoiceIaBatchItem("2", false), VoiceIaBatchItem("1", true),
            VoiceIaBatchItem("3", true)))
        assertEquals(3, plan.total)
        assertEquals(2, plan.savedCalls)
        assertEquals(1, plan.toGenerate)
    }
}
