package br.com.uaicortes

/** Regras independentes do player para não exportar cortes fora do arquivo. */
object ExportTimingPolicy {
    fun validClip(startMs: Long, endMs: Long, sourceDurationMs: Long): Boolean =
        sourceDurationMs > 0L && startMs >= 0L && endMs > startMs && endMs <= sourceDurationMs

    fun projectTime(clips: List<Clip>, index: Int, localMs: Long): Long {
        if (clips.isEmpty()) return 0L
        val safeIndex = index.coerceIn(0, clips.lastIndex)
        return (clips.take(safeIndex).sumOf { it.durationMs } +
            localMs.coerceIn(0L, clips[safeIndex].durationMs))
            .coerceIn(0L, clips.sumOf { it.durationMs })
    }
}
