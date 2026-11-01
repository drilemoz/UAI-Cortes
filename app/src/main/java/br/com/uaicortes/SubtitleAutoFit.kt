package br.com.uaicortes

/** Reduz tipografia somente quando texto total não cabe em até ~3 linhas.
 * Não trunca a frase: se for muito grande, a quebra continua livre para mais linhas.
 * Recebe medidas reais (px) produzidas pelas fontes de cada renderer. */
internal object SubtitleAutoFit {
    fun factor(longestWordPx: Float, fullTextPx: Float, availableWidthPx: Float,
               targetLines: Int = 3): Float {
        if (!availableWidthPx.isFinite() || availableWidthPx <= 0f) return 1f
        val safeWord = longestWordPx.takeIf { it.isFinite() && it >= 0f } ?: 0f
        val safeFull = fullTextPx.takeIf { it.isFinite() && it >= 0f } ?: 0f
        val wordFactor = if (safeWord == 0f) 1f else availableWidthPx / safeWord
        // Margem de 10% para que a divisão por palavras não produza uma quarta linha.
        val linesFactor = if (safeFull == 0f) 1f else
            (availableWidthPx * targetLines.coerceIn(1, 5) * .90f) / safeFull
        return minOf(1f, wordFactor, linesFactor).coerceIn(.48f, 1f)
    }
}
