package br.com.uaicortes

internal object EditorTimelineHeaderPolicy {
    val zoomLevels = listOf(1, 2, 4, 8)

    /** More space for the time at narrow widths. Undo and redo are always direct actions. */
    enum class Layout { DIRECT, COMPACT, MENU }
    fun layout(widthDp: Float, fontScale: Float): Layout {
        val extra = ((fontScale - 1f).coerceAtLeast(0f)) * 108f
        return when {
            widthDp >= 344f + extra -> Layout.DIRECT
            widthDp >= 288f + extra -> Layout.COMPACT
            else -> Layout.MENU
        }
    }

    fun position(digits: String, durationMs: Long): Long? =
        NumericTime.toMs(digits)?.takeIf { durationMs > 0L && it in 0L..durationMs }

    /** At least a 48 dp target. If the tools overflow, show part of the next one. */
    fun toolbarItemWidthDp(available: Float, count: Int): Float {
        if (count <= 1) return 48f
        val inner = (available - 12f).coerceAtLeast(0f)
        val gap = 2f
        val all = (inner - gap * (count - 1)) / count
        if (all >= 48f) return all.coerceAtMost(64f)
        val visible = ((inner - 16f + gap) / (48f + gap)).toInt().coerceIn(1, count - 1)
        return ((inner - 16f - gap * visible) / visible).coerceIn(48f, 64f)
    }
}
