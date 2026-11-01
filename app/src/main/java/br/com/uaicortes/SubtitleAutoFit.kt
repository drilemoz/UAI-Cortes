package br.com.uaicortes

/** Ajuste visual compartilhado entre prévia e exportação.
 * Frases extensas param de encolher a partir de 48% e podem ocupar mais linhas.
 * PALAVRAS isoladas são uma exceção: precisam caber na largura disponível, mesmo
 * quando isso requer fator menor que 48%, para não cortar bordas no vídeo.
 * As medidas são de fonte/renderizador e não alteram texto nem tempos da legenda.
 */
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
        // O limite de legibilidade (.48f) aplica-se APENAS ao comprimento da frase.
        // Aplicá-lo à largura de uma palavra isolada causa corte no MP4 e falha
        // SubtitleExportSizingTest.longWordNeverExceedsFrameAndNoNaN.
        return minOf(1f, wordFactor, linesFactor.coerceAtLeast(.48f))
            .coerceAtLeast(Float.MIN_VALUE)
    }
}
