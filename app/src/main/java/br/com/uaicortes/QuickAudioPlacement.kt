package br.com.uaicortes

/** Intervalo contínuo do áudio no projeto e posição local no primeiro/último clipe. */
internal object QuickAudioPlacement {
    data class Point(val clipIndex: Int, val localMs: Long)
    data class Window(
        val startMs: Long,
        val endMs: Long,
        val first: Point,
        val last: Point,
        val omittedMs: Long
    ) {
        val durationMs: Long get() = endMs - startMs
    }

    fun resolve(clipDurations: List<Long>, startMs: Long, audioMs: Long): Window? {
        val total = clipDurations.sumOf { it.coerceAtLeast(0) }
        if (startMs < 0L || startMs >= total ||
            minOf(audioMs, total - startMs) < AudioTrackTiming.MIN_TRIM_MS) return null
        val endMs = startMs + minOf(audioMs, total - startMs)
        var offset = 0L
        var first: Point? = null
        for ((index, duration) in clipDurations.withIndex()) {
            if (duration <= 0L) continue
            val next = offset + duration
            if (first == null && startMs < next) first = Point(index, startMs - offset)
            // O fim é exclusivo: num corte exato, pertence ao fim do clipe anterior.
            if (first != null && endMs <= next) {
                return Window(startMs, endMs, first, Point(index, endMs - offset),
                    audioMs - (endMs - startMs))
            }
            offset = next
        }
        return null
    }
}
