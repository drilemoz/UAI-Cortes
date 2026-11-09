package br.com.uaicortes

import kotlin.math.roundToLong

/** Regras puras, sem Media3 ou Compose; o áudio original nunca é modificado. */
internal object AudioClipSelectionPolicy {
    const val MIN_SELECTION_MS = 100L
    const val MAX_DURATION_MS = 6L * 60L * 60L * 1_000L
    const val WAVEFORM_BARS = 160

    fun valid(durationMs: Long, startMs: Long, endMs: Long): Boolean =
        durationMs in MIN_SELECTION_MS..MAX_DURATION_MS &&
            startMs >= 0L && endMs <= durationMs && endMs - startMs >= MIN_SELECTION_MS

    fun start(durationMs: Long, requested: Long, endMs: Long): Long =
        requested.coerceIn(0L, (minOf(endMs, durationMs) - MIN_SELECTION_MS).coerceAtLeast(0L))

    fun end(durationMs: Long, startMs: Long, requested: Long): Long =
        requested.coerceIn((startMs + MIN_SELECTION_MS).coerceAtMost(durationMs), durationMs)

    /** Posição do cursor na onda, com limites seguros mesmo quando a duração não está pronta. */
    fun playheadFraction(durationMs: Long, currentMs: Long): Float =
        if (durationMs <= 0L) 0f
        else (currentMs.coerceIn(0L, durationMs).toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)

    /** Tocar na onda posiciona o cursor somente dentro do trecho recortado. */
    fun seekFromFraction(durationMs: Long, startMs: Long, endMs: Long, fraction: Float): Long {
        if (!valid(durationMs, startMs, endMs)) return 0L
        val safe = fraction.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0f
        return (safe * durationMs).roundToLong().coerceIn(startMs, endMs)
    }

    fun time(ms: Long): String {
        val safe = ms.coerceAtLeast(0L)
        val hours = safe / 3_600_000L
        val minutes = (safe / 60_000L) % 60L
        val seconds = (safe / 1_000L) % 60L
        val tenths = (safe % 1_000L) / 100L
        return if (hours > 0L) "%d:%02d:%02d.%d".format(java.util.Locale.ROOT,hours,minutes,seconds,tenths)
        else "%02d:%02d.%d".format(java.util.Locale.ROOT,minutes,seconds,tenths)
    }

    fun exportBase(name: String): String {
        val stem = name.substringBeforeLast('.', name).trim()
        val cleaned = stem.replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"), "_")
            .trim(' ', '.')
        return cleaned.take(72).ifBlank { "Trecho de áudio" }
    }
}
