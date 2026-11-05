package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class GeminiUsageAnalyticsTest {
    private val price = GeminiManualPrice(0.5, 6.0)
    private fun item(at: Long = 1000L, op: String = "narracao", status: String = "ok",
        input: Long? = 1000L, output: Long? = 10000L, duration: Long? = 1200L): GeminiUsageEvent =
        GeminiUsageEvent(at, "projeto", "modelo", op, status,
            if (input != null || output != null) GeminiTokenUsage(input, output, null, null, null) else null, duration)

    @Test fun costRequiresBothMeasuredTokenDirectionsAndPrice() {
        val measured = item()
        assertEquals(0.0605, GeminiAnalyticsPolicy.cost(measured, price)!!, 1e-9)
        assertNull(GeminiAnalyticsPolicy.cost(item(output = null), price))
        assertNull(GeminiAnalyticsPolicy.cost(item(status = "429"), price))
        assertNull(GeminiAnalyticsPolicy.cost(measured, GeminiManualPrice(null, 6.0)))
        assertEquals(0.0, GeminiAnalyticsPolicy.cost(measured, GeminiManualPrice(0.0, 0.0))!!, 0.0)
    }

    @Test fun aggregatesSeparatelyWithMissingAndLatency() {
        val events = listOf(item(duration = 1000), item(duration = 3000),
            item(output = null), item(status = "429", input = null, output = null),
            item(op = "descricao", input = 250, output = 400))
        val stats = GeminiAnalyticsPolicy.aggregate(events) { _, _ -> price }
        val voice = stats.first { it.operation == "narracao" }
        assertEquals(4, voice.attempts)
        assertEquals(3, voice.success)
        assertEquals(1, voice.errors)
        assertEquals(3, voice.measuredDuration)
        assertEquals(1733L, voice.averageLatencyMs)
        assertEquals(2, voice.priced)
        assertEquals(1, voice.unpriced)
        assertEquals(0.121, voice.knownCostUsd, 1e-9)
        assertEquals(1, stats.first { it.operation == "descricao" }.priced)
    }

    @Test fun batchEstimateRequiresFiveComparablePricedSuccesses() {
        val sample = List(5) { i -> item(at = i.toLong() + 1) }
        assertNull(GeminiAnalyticsPolicy.estimatedBatchCost(sample.take(4), "projeto", "modelo", "narracao", 20, price))
        assertNull(GeminiAnalyticsPolicy.estimatedBatchCost(sample, "outro", "modelo", "narracao", 20, price))
        assertNull(GeminiAnalyticsPolicy.estimatedBatchCost(sample, "projeto", "modelo", "legendas", 20, price))
        assertEquals(1.21, GeminiAnalyticsPolicy.estimatedBatchCost(sample, "projeto", "modelo", "narracao", 20, price)!!, 1e-9)
    }

    @Test fun dailyCountsUseLocalCalendarAndExcludeFuture() {
        val zone = ZoneId.of("America/Sao_Paulo")
        val now = ZonedDateTime.of(2026, 11, 5, 12, 0, 0, 0, zone).toInstant().toEpochMilli()
        val yesterday = now - 24L * 3600 * 1000
        val counts = GeminiAnalyticsPolicy.dailyCounts(listOf(item(at = yesterday), item(at = now), item(at = now + 10)), 2, now, zone)
        assertEquals(listOf(1, 1), counts.map { it.second })
    }
}
