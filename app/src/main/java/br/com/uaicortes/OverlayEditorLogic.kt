package br.com.uaicortes

import kotlin.math.min
import kotlin.math.max

/** Geometria normalizada da sobreposição: independente de resolução e densidade. */
object OverlayEditorLogic {
    const val MIN_SCALE = 0.05f
    const val MAX_SCALE = 1.50f
    const val FIT = "fit"
    const val FILL = "fill"
    fun fitMode(value: String?): String = if (value == FILL) FILL else FIT

    /** Largura e altura ocupadas em frações da área real do vídeo. */
    fun dimensions(scale: Float, contentAspect: Float, canvasAspect: Float,
                   fitMode: String = FIT): Pair<Float, Float> {
        val ratio = contentAspect.coerceAtLeast(0.05f)
        val canvas = canvasAspect.coerceAtLeast(0.05f)
        val target = scale.coerceIn(MIN_SCALE, MAX_SCALE)
        val candidate = target * ratio / canvas
        val width = if (fitMode == FILL) max(target,candidate) else min(target,candidate)
        return width to (width * canvas / ratio)
    }

    /** Sem margem obrigatória: um logo pode tocar a borda do quadro.
     * Em zoom >100%, prende o centro do conteúdo que ultrapassa o canvas. */
    fun clampCenter(center: Float, occupied: Float): Float {
        val inset = occupied / 2f
        if (inset >= .5f) return .5f
        return center.coerceIn(inset,1f-inset)
    }

    fun preset(row: Int, col: Int, widthFraction: Float, heightFraction: Float): Pair<Float, Float> {
        require(row in 0..2 && col in 0..2)
        val x = when(col) { 0 -> 0f; 1 -> 0.5f; else -> 1f }
        val y = when(row) { 0 -> 0f; 1 -> 0.5f; else -> 1f }
        return clampCenter(x, widthFraction) to clampCenter(y, heightFraction)
    }

    /** Percentual pode preencher o quadro ou usar zoom com recorte até 150%. */
    fun parsePercentage(input: String): Float? = input.trim().removeSuffix("%").trim()
        .toIntOrNull()?.takeIf { it in 5..150 }?.div(100f)

    fun startChanged(start: Long, oldStart: Long, duration: Long, total: Long): Pair<Long, Long> {
        val end = (oldStart + duration).coerceIn(0L, total)
        val minLength = min(500L, end.coerceAtLeast(1L))
        val newStart = start.coerceIn(0L, (end - minLength).coerceAtLeast(0L))
        return newStart to (end - newStart).coerceAtLeast(1L)
    }

    fun endChanged(end: Long, start: Long, total: Long): Long {
        val minLength = min(500L, (total - start).coerceAtLeast(1L))
        return (end.coerceIn((start + minLength).coerceAtMost(total), total) - start).coerceAtLeast(1L)
    }
}
