package br.com.uaicortes

/** Edição temporal não destrutiva de legendas, expressa em milissegundos absolutos.
 * O projeto mantém o texto, cor, posição e ID; apenas início/fim são alterados.
 * Nenhum gesto grava até o usuário soltar o controle.
 */
object SubtitleTimelineEdit {
    const val MIN_DURATION_MS = 100L

    private fun bounded(startMs: Long, endMs: Long, totalMs: Long): Pair<Long, Long> {
        val limit = totalMs.coerceAtLeast(1L)
        val start = startMs.coerceIn(0L, limit - 1L)
        return start to endMs.coerceIn(start + 1L, limit)
    }

    /** Desloca a legenda sem alterar sua duração, preservando os limites do vídeo. */
    fun move(startMs: Long, endMs: Long, deltaMs: Long, totalMs: Long): Pair<Long, Long> {
        val (start, end) = bounded(startMs, endMs, totalMs)
        val length = end - start
        val moved = TimelinePrecision.step(start, deltaMs, totalMs.coerceAtLeast(1L) - length)
        return moved to moved + length
    }

    /** A alça esquerda muda somente o começo (e portanto a duração). */
    fun trimLeft(startMs: Long, endMs: Long, deltaMs: Long, totalMs: Long): Pair<Long, Long> {
        val (start, end) = bounded(startMs, endMs, totalMs)
        val minimum = if (end - start < MIN_DURATION_MS) 1L else MIN_DURATION_MS
        val next = TimelinePrecision.step(start, deltaMs, end - minimum)
        return next to end
    }

    /** A alça direita muda somente o término (e portanto a duração). */
    fun trimRight(startMs: Long, endMs: Long, deltaMs: Long, totalMs: Long): Pair<Long, Long> {
        val (start, end) = bounded(startMs, endMs, totalMs)
        val minimum = if (end - start < MIN_DURATION_MS) 1L else MIN_DURATION_MS
        val next = TimelinePrecision.step(end, deltaMs, totalMs.coerceAtLeast(1L))
            .coerceAtLeast(start + minimum)
        return start to next
    }
}
