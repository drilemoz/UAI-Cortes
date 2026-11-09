package br.com.uaicortes

/** Janela mínima necessária para renderizar um quadro pelo compositor do MP4.
 * O PNG vem do vídeo renderizado, nunca de uma região da tela Android.
 */
internal object FrameSnapshotTiming {
    const val LEAD_MS = 70L
    const val WINDOW_MS = 260L

    data class Slice(val startMs: Long, val endMs: Long, val frameAtMs: Long) {
        val durationMs: Long get() = endMs - startMs
    }

    fun slice(startMs: Long, endMs: Long, requestedMs: Long): Slice? {
        if (startMs < 0L || endMs <= startMs) return null
        val capture = requestedMs.coerceIn(startMs, endMs - 1L)
        val start = (capture - LEAD_MS).coerceAtLeast(startMs)
        val end = (start + WINDOW_MS).coerceAtMost(endMs)
        if (end <= start) return null
        return Slice(start, end, capture - start)
    }

    /** Recorte de um PiP dentro do intervalo global [início,fim), sem extrapolar a mídia. */
    fun overlap(startMs: Long, lengthMs: Long, windowStart: Long, windowEnd: Long): LongRange? {
        if (lengthMs <= 0 || windowEnd <= windowStart) return null
        val from = maxOf(startMs, windowStart)
        val to = minOf(startMs + lengthMs, windowEnd)
        return if (to > from) from until to else null
    }
}

