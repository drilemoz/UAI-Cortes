package br.com.uaicortes

/** Horários marcados manualmente ouvindo o áudio de referência, sem alterar as legendas. */
internal object NarrationReferenceAlignment {
    /** Retém somente deslocamentos reais de legendas presentes; o original é o padrão. */
    fun cleaned(
        segments: List<SubtitleNarrationTimeline.Segment>,
        markedStarts: Map<String, Long>
    ): Map<String, Long> = segments.mapNotNull { segment ->
        markedStarts[segment.cueId]?.takeIf { it != segment.startMs }?.let { segment.cueId to it }
    }.toMap()

    fun start(segment: SubtitleNarrationTimeline.Segment, markedStarts: Map<String, Long>): Long =
        markedStarts[segment.cueId] ?: segment.startMs

    /** Uma faixa pode começar antes/depois de sua legenda; a legenda original não é reescrita. */
    fun placed(
        segment: SubtitleNarrationTimeline.Segment,
        markedStarts: Map<String, Long>
    ): SubtitleNarrationTimeline.Segment = segment.copy(startMs = start(segment, markedStarts))

    fun validate(
        segments: List<SubtitleNarrationTimeline.Segment>,
        markedStarts: Map<String, Long>,
        durationMs: Long
    ): String? {
        if (durationMs <= 0L) return "Duração do projeto inválida."
        val starts = segments.map { start(it, markedStarts) }
        if (starts.any { it < 0L || it >= durationMs })
            return "Existe uma fala posicionada fora da duração do vídeo."
        if (starts.zipWithNext().any { (previous, next) -> previous > next })
            return "Os inícios das falas ficaram fora de ordem. Ajuste ou restaure as marcações."
        return null
    }

    /** Exibe precisão subsegundo para o ajuste fino (formatMs omite milissegundos). */
    fun timeLabel(milliseconds: Long): String {
        val ms = milliseconds.coerceAtLeast(0L)
        val minutes = ms / 60000L
        val seconds = (ms / 1000L) % 60L
        val millis = ms % 1000L
        return "%02d:%02d.%03d".format(java.util.Locale.ROOT, minutes, seconds, millis)
    }

    /** Traduz o tempo da referência para o tempo absoluto da timeline. */
    fun sourceToProject(sourcePositionMs: Long, offsetMs: Long, durationMs: Long): Long =
        (sourcePositionMs + offsetMs).coerceIn(0L, (durationMs - 1L).coerceAtLeast(0L))
}
