package br.com.uaicortes

/** Faixas de tempo globais (respeitando os cortes) para encaixe do PiP. */
internal object PipTimePresets {
    data class Period(val startMs: Long, val endMs: Long) {
        val durationMs: Long get() = endMs - startMs
    }
    fun clipPeriods(durationsMs: List<Long>): List<Period> {
        var start = 0L
        return durationsMs.map { duration ->
            val end = start + duration.coerceAtLeast(0L)
            Period(start, end).also { start = end }
        }
    }
    fun limited(period: Period, isImage: Boolean, sourceDurationMs: Long?): Period? {
        val maxDuration = if (isImage) period.durationMs else
            minOf(period.durationMs, sourceDurationMs?.takeIf { it > 0 } ?: return null)
        return if (maxDuration >= 500L) Period(period.startMs, period.startMs + maxDuration)
            else null
    }
}
