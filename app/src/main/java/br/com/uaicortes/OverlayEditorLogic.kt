package br.com.uaicortes

import kotlin.math.min

/** Geometria normalizada da sobreposição: independente de resolução e densidade. */
object OverlayEditorLogic {
    private const val MARGIN = 0.025f

    /** Largura e altura ocupadas em frações da área real do vídeo. */
    fun dimensions(scale: Float, contentAspect: Float, canvasAspect: Float): Pair<Float, Float> {
        val ratio = contentAspect.coerceAtLeast(0.05f)
        val canvas = canvasAspect.coerceAtLeast(0.05f)
        val w = min(scale.coerceIn(0.05f, 0.90f), 0.90f * ratio / canvas)
        return w to (w * canvas / ratio)
    }

    fun clampCenter(center: Float, occupied: Float): Float {
        val inset = (occupied / 2f + MARGIN).coerceAtMost(0.5f)
        return center.coerceIn(inset, 1f - inset)
    }

    fun preset(row: Int, col: Int, widthFraction: Float, heightFraction: Float): Pair<Float, Float> {
        require(row in 0..2 && col in 0..2)
        val x = when(col) { 0 -> 0f; 1 -> 0.5f; else -> 1f }
        val y = when(row) { 0 -> 0f; 1 -> 0.5f; else -> 1f }
        return clampCenter(x, widthFraction) to clampCenter(y, heightFraction)
    }

    /** A entrada manual precisa estar entre 5% e 90% da largura do quadro. */
    fun parsePercentage(input: String): Float? = input.trim().removeSuffix("%").trim()
        .toIntOrNull()?.takeIf { it in 5..90 }?.div(100f)

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
