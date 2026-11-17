package br.com.uaicortes

/** Métricas de karaokê proporcionais ao glifo, independentes da densidade Android. */
internal object SubtitleCaptionInk {
    fun outlineRadius(level: Int, fontPx: Float): Float =
        fontPx.coerceAtLeast(0f) * (level.coerceAtLeast(0) * .022f).coerceAtMost(.075f)

    fun shadowOffset(level: Int, fontPx: Float): Float =
        fontPx.coerceAtLeast(0f) * (level.coerceAtLeast(0) * .025f).coerceAtMost(.12f)
}
