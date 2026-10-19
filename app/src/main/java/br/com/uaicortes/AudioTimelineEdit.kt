package br.com.uaicortes

/** Edição visual não destrutiva de áudio. Todas as coordenadas são milissegundos.
 * sourceStartMs aponta para a origem; timelineStartMs aponta para o projeto.
 */
data class AudioTimelineDraft(
    val timelineStartMs: Long,
    val sourceStartMs: Long,
    val sourceEndMs: Long
) {
    val durationMs: Long get() = sourceEndMs - sourceStartMs
    val timelineEndMs: Long get() = timelineStartMs + durationMs
}

object AudioTimelineEdit {
    /** Desloca a faixa inteira sem mudar o trecho de origem. */
    fun move(draft: AudioTimelineDraft, deltaMs: Long, projectDurationMs: Long): AudioTimelineDraft {
        if (draft.durationMs < AudioTrackTiming.MIN_TRIM_MS || projectDurationMs < draft.durationMs) return draft
        val lastStart = projectDurationMs - draft.durationMs
        return draft.copy(timelineStartMs = safeAdd(draft.timelineStartMs, deltaMs).coerceIn(0L, lastStart))
    }

    /** Borda esquerda: altera ponto de entrada na fonte e posição no projeto;
     * o fim do segmento continua no mesmo instante da timeline.
     */
    fun left(draft: AudioTimelineDraft, deltaMs: Long): AudioTimelineDraft {
        if (draft.durationMs < AudioTrackTiming.MIN_TRIM_MS || draft.sourceStartMs < 0 || draft.timelineStartMs < 0) return draft
        val minimumDelta = maxOf(-draft.sourceStartMs, -draft.timelineStartMs)
        val maximumDelta = draft.durationMs - AudioTrackTiming.MIN_TRIM_MS
        val applied = deltaMs.coerceIn(minimumDelta, maximumDelta)
        return draft.copy(
            timelineStartMs = draft.timelineStartMs + applied,
            sourceStartMs = draft.sourceStartMs + applied
        )
    }

    /** Borda direita: altera apenas o fim do trecho no arquivo de origem. */
    fun right(draft: AudioTimelineDraft, deltaMs: Long, sourceDurationMs: Long,
              projectDurationMs: Long): AudioTimelineDraft {
        if (draft.durationMs < AudioTrackTiming.MIN_TRIM_MS || sourceDurationMs < draft.sourceEndMs ||
            projectDurationMs < draft.timelineEndMs) return draft
        val minimumEnd = draft.sourceStartMs + AudioTrackTiming.MIN_TRIM_MS
        val maximumEnd = minOf(sourceDurationMs, safeAdd(draft.sourceEndMs, projectDurationMs - draft.timelineEndMs))
        if (maximumEnd < minimumEnd) return draft
        return draft.copy(sourceEndMs = safeAdd(draft.sourceEndMs, deltaMs).coerceIn(minimumEnd, maximumEnd))
    }

    /** Mantém a prévia na mesma escala do projeto e evita divisões por zero. */
    fun pixelsToMs(totalDeltaPx: Float, timelineWidthPx: Float, projectDurationMs: Long): Long {
        if (!totalDeltaPx.isFinite() || !timelineWidthPx.isFinite() || timelineWidthPx <= 0f || projectDurationMs <= 0L) return 0L
        return (totalDeltaPx.toDouble() / timelineWidthPx.toDouble() * projectDurationMs)
            .coerceIn(Long.MIN_VALUE.toDouble(), Long.MAX_VALUE.toDouble()).toLong()
    }

    private fun safeAdd(base: Long, delta: Long): Long = when {
        delta > 0 && base > Long.MAX_VALUE - delta -> Long.MAX_VALUE
        delta < 0 && base < Long.MIN_VALUE - delta -> Long.MIN_VALUE
        else -> base + delta
    }
}
