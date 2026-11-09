package br.com.uaicortes

/** Tamanho associado à largura REAL do vídeo, não à largura da tela ou densidade.
 * 280 dp de quadro na prévia equivalem a 280 px na regra proporcional de saída.
 * A prévia converte px para sp considerando fontScale, pois o MP4 não depende
 * das configurações de fonte do Android. */
internal object SubtitleFrameTypography {
    const val REFERENCE_WIDTH = 280f
    const val SIDE_PADDING = .90f

    fun fontPixelsForFrame(sizeSp: Int, frameWidthPx: Int): Float =
        sizeSp.coerceIn(12, 48) * frameWidthPx.coerceAtLeast(16) / REFERENCE_WIDTH

    fun previewSizeSp(sizeSp: Int, frameWidthDp: Float, fontScale: Float): Float =
        (sizeSp.coerceIn(12, 48) * frameWidthDp.coerceAtLeast(1f) /
            REFERENCE_WIDTH / fontScale.coerceAtLeast(.5f)).coerceAtLeast(.01f)

    fun allowedTextWidth(frameWidthPx: Int): Int =
        (frameWidthPx.coerceAtLeast(16) * SIDE_PADDING).toInt().coerceAtLeast(2)
}
