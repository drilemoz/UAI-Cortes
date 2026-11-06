package br.com.uaicortes

/** Regras de navegação da pista única de legendas, independentes da UI Android. */
internal object SubtitleCompactLayout {
    const val WINDOW_MS = 10_000L

    fun windowCount(totalMs: Long, windowMs: Long = WINDOW_MS): Int =
        (((totalMs.coerceAtLeast(1L) - 1L) / windowMs.coerceAtLeast(1L)) + 1L)
            .coerceAtMost(Int.MAX_VALUE.toLong()).toInt()

    fun windowFor(timeMs: Long, totalMs: Long, windowMs: Long = WINDOW_MS): Int =
        (timeMs.coerceAtLeast(0L) / windowMs.coerceAtLeast(1L)).coerceAtMost(
            (windowCount(totalMs, windowMs) - 1).toLong()
        ).toInt()

    /** Segmento de um clipe principal dentro de uma janela de tempo absoluto.
     * A duração dos recortes é usada, não o horário original do arquivo.
     * Fotos contam como clipes e não exigem legendas para serem desenhadas.
     */
    data class VisibleClip(val index: Int, val startMs: Long, val endMs: Long)

    fun clipsInWindow(durationsMs: List<Long>, window: Int, totalMs: Long,
        windowMs: Long = WINDOW_MS): List<VisibleClip> {
        if (window !in 0 until windowCount(totalMs, windowMs) || totalMs <= 0) return emptyList()
        val start = window.toLong() * windowMs
        val finish = minOf(start + windowMs, totalMs)
        var position = 0L
        val result = ArrayList<VisibleClip>()
        durationsMs.forEachIndexed { index, raw ->
            val length = raw.coerceAtLeast(0L)
            val end = if (length > Long.MAX_VALUE - position) Long.MAX_VALUE else position + length
            val first = maxOf(position, start)
            val last = minOf(end, finish)
            if (first < last) result.add(VisibleClip(index, first, last))
            position = end
        }
        return result
    }

    /** Intervalo absoluto da legenda visível na janela, ou null se não cruza a janela. */
    fun visibleRange(window: Int, startMs: Long, endMs: Long,
                     totalMs: Long, windowMs: Long = WINDOW_MS): Pair<Long, Long>? {
        if (window !in 0 until windowCount(totalMs, windowMs) || startMs >= endMs) return null
        val begin = window.toLong() * windowMs
        val finish = minOf(begin + windowMs, totalMs.coerceAtLeast(0L))
        val first = maxOf(begin, startMs)
        val last = minOf(finish, endMs)
        return if (first < last) first to last else null
    }
}
