package br.com.uaicortes

/** Janela mínima necessária para renderizar um quadro pelo compositor do MP4.
 * O PNG vem do vídeo renderizado, nunca de uma região da tela Android.
 */
internal object FrameSnapshotTiming {
    // A janela anterior de 260 ms podia produzir poucas amostras para um muxer
    // Media3 no Xiaomi: o diagnóstico real registrou ERROR_CODE_MUXING_TIMEOUT.
    const val LEAD_MS = 500L
    const val WINDOW_MS = 1_600L

    data class Slice(val startMs: Long, val endMs: Long, val frameAtMs: Long) {
        val durationMs: Long get() = endMs - startMs
    }

    fun slice(startMs: Long, endMs: Long, requestedMs: Long): Slice? {
        if (startMs < 0L || endMs <= startMs) return null
        val capture = requestedMs.coerceIn(startMs, endMs - 1L)
        val end = ((capture - LEAD_MS).coerceAtLeast(startMs) + WINDOW_MS)
            .coerceAtMost(endMs)
        // Próximo do fim da origem, deslocar a janela para trás para obter
        // quadros suficientes sem solicitar posições fora do clipe.
        val start = (end - WINDOW_MS).coerceAtLeast(startMs)
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

