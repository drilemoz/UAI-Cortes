package br.com.uaicortes

/** X is in the shared, already scrolled track coordinates. The extension is editable, not seekable. */
internal object TimelinePlayheadPosition {
    fun fromX(x: Float, widthPx: Int, displayMs: Long, projectMs: Long): Long {
        if (widthPx <= 0 || !x.isFinite()) return 0L
        val fraction = (x.toDouble() / widthPx).coerceIn(0.0, 1.0)
        return (fraction * displayMs.coerceAtLeast(0L)).toLong()
            .coerceIn(0L, projectMs.coerceAtLeast(0L))
    }
}
