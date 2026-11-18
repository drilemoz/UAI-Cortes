package br.com.uaicortes

import kotlin.math.min

internal object SubtitleSingleLinePolicy {
    /** Keep UTF-16 indices, including both CRLF characters, for timed word highlights. */
    fun display(text: String): String {
        if (text.none(::breaksLine)) return text
        return buildString(text.length) {
            text.forEach { append(if (breaksLine(it)) ' ' else it) }
        }
    }

    private fun breaksLine(char: Char): Boolean = when (char) {
        '\n', '\r', '\t', '\u000b', '\u000c', '\u0085', '\u2028', '\u2029' -> true
        else -> false
    }

    fun fontFit(widthAtNominalSize: Float, availableWidth: Float): Float =
        if (widthAtNominalSize > 0f) min(1f, availableWidth.coerceAtLeast(1f) / widthAtNominalSize)
        else 1f

    data class Placement(val centerX: Float, val bottomY: Float, val scale: Float)

    /** Normalized video coordinates: x is center, y is the bottom of the rendered text. */
    fun place(x: Float, y: Float, slideX: Float, slideY: Float, scale: Float,
              width: Int, height: Int, frameWidth: Int, frameHeight: Int): Placement {
        val baseWidth = width.coerceAtLeast(1) / frameWidth.coerceAtLeast(1).toFloat()
        val baseHeight = height.coerceAtLeast(1) / frameHeight.coerceAtLeast(1).toFloat()
        val fitScale = minOf(scale.coerceAtLeast(0f), 1f / baseWidth, 1f / baseHeight)
        val halfWidth = (baseWidth * fitScale * .5f).coerceIn(0f, .5f)
        val relativeHeight = (baseHeight * fitScale).coerceIn(0f, 1f)
        return Placement((x + slideX).coerceIn(halfWidth, 1f - halfWidth),
            (y + slideY).coerceIn(relativeHeight, 1f), fitScale)
    }
}
