package br.com.uaicortes

/** Proporções relativas ao QUADRO DE VÍDEO, nunca à tela Android.
 * TextOverlay usa fonte base de 100 px; a escala antiga deixava frases longas
 * maiores que o quadro, embora a prévia Compose quebrasse as linhas.
 */
object SubtitleExportSizing {
    fun safeFrameWidth(width: Int): Int = width.takeIf { it >= 16 } ?: 720

    /** Área horizontal com 5% de margem de cada lado. */
    fun textBoxWidth(width: Int): Int = (safeFrameWidth(width) * 0.90f).toInt().coerceAtLeast(2)

    /** O valor em sp controla o tamanho visual proporcional à largura do vídeo.
     * Na prévia típica de 380 dp: 12 sp -> 12 dp, 22 sp -> 22 dp.
     * TextOverlay.TEXT_SIZE_PIXELS = 100, portanto convertemos para RelativeSizeSpan.
     */
    /** Evita que uma legenda deslocada pelo usuário seja cortada nas bordas. */
    fun centerX(x: Float, renderedWidth: Int, frameWidth: Int, animationScale: Float): Float {
        val normalizedHalf = (renderedWidth.toFloat() * animationScale.coerceAtLeast(0f) /
            (2f * safeFrameWidth(frameWidth))).coerceIn(0f, .5f)
        return x.coerceIn(normalizedHalf, 1f - normalizedHalf)
    }

    fun relativeFontSize(sizeSp: Int, frameWidth: Int, longestWordPxAt100: Float): Float {
        val base = (sizeSp.coerceIn(12, 48) / 380f) * safeFrameWidth(frameWidth) / 100f
        val longest = (longestWordPxAt100 * base).coerceAtLeast(0f)
        val shrink = if (longest > textBoxWidth(frameWidth)) textBoxWidth(frameWidth) / longest else 1f
        return (base * shrink).coerceAtLeast(0.01f)
    }
}
