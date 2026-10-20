package br.com.uaicortes

/** Geometria única usada pelas prévias Media3; não altera pixels da exportação. */
internal object AdaptiveVideoLayout {
    data class Size(val width: Float, val height: Float)
    private const val DEFAULT_ASPECT = 16f / 9f

    fun safeAspect(aspect: Float): Float =
        if (aspect.isFinite() && aspect in 0.1f..10f) aspect else DEFAULT_ASPECT

    fun fit(width: Float, height: Float, aspect: Float): Size {
        if (!width.isFinite() || !height.isFinite() || width <= 0f || height <= 0f)
            return Size(0f, 0f)
        val ratio = safeAspect(aspect)
        val fitWidth = minOf(width, height * ratio)
        return Size(fitWidth, fitWidth / ratio)
    }

    /** Limita a prévia a 48% da altura da tela, evitando esconder as ferramentas.
     * Paisagem ocupa toda a largura; retrato cresce até o limite vertical. */
    fun inline(width: Float, screenHeight: Float, aspect: Float): Size {
        val heightBudget = minOf(380f, (screenHeight * .48f).coerceAtLeast(160f))
        return fit(width, heightBudget, aspect)
    }

    /** A seleção pode passar a outro arquivo com proporção diferente. */
    fun activeClipIndex(durations: List<Long>, timeMs: Long): Int {
        if (durations.isEmpty()) return -1
        var t = timeMs.coerceAtLeast(0L)
        durations.forEachIndexed { index, duration ->
            val length = duration.coerceAtLeast(0L)
            if (t < length || index == durations.lastIndex) return index
            t -= length
        }
        return durations.lastIndex
    }
}
