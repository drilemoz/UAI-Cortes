package br.com.uaicortes

/** Limites de edição de faixas extras, independentes da interface Android. */
object AudioTrackTiming {
    const val MIN_TRIM_MS = 250L

    fun valid(sourceStartMs: Long, sourceEndMs: Long, sourceDurationMs: Long,
              timelineStartMs: Long, projectDurationMs: Long): Boolean {
        if (sourceDurationMs < MIN_TRIM_MS || projectDurationMs < MIN_TRIM_MS) return false
        if (sourceStartMs < 0L || sourceEndMs > sourceDurationMs || sourceEndMs < sourceStartMs) return false
        if (timelineStartMs < 0L || timelineStartMs > projectDurationMs) return false
        val length = sourceEndMs - sourceStartMs
        return length >= MIN_TRIM_MS && length <= projectDurationMs - timelineStartMs
    }

    /** Resolve limites de um projeto antigo sem mutar seus dados persistidos. */
    fun initialRange(sourceStartMs: Long, durationMs: Long, sourceDurationMs: Long,
                     timelineStartMs: Long, projectDurationMs: Long): LongRange? {
        if (sourceDurationMs < MIN_TRIM_MS || projectDurationMs < MIN_TRIM_MS) return null
        val start = sourceStartMs.coerceIn(0L, sourceDurationMs - MIN_TRIM_MS)
        val placement = timelineStartMs.coerceIn(0L, projectDurationMs - MIN_TRIM_MS)
        val maxLength = minOf(sourceDurationMs - start, projectDurationMs - placement)
        if (maxLength < MIN_TRIM_MS) return null
        val length = if (durationMs > 0L) durationMs.coerceIn(MIN_TRIM_MS, maxLength) else maxLength
        return start..(start + length)
    }
}
