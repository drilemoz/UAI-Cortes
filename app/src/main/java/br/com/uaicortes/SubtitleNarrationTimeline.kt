package br.com.uaicortes

/** Planejamento puro da redublagem; jamais altera legendas nem corta áudio silenciosamente. */
internal object SubtitleNarrationTimeline {
    const val MAX_SEGMENTS = 12
    const val MIN_AUDIO_MS = 250L

    data class Segment(
        val cueId: String,
        val text: String,
        val startMs: Long,
        val subtitleEndMs: Long
    )

    data class Recorded(val segment: Segment, val audioDurationMs: Long)

    enum class Issue { SUBTITLE_TOO_SHORT, NEXT_SPEECH_OVERLAP, PROJECT_OVERFLOW }
    data class Inspection(
        val segment: Segment,
        val audioDurationMs: Long,
        val issues: Set<Issue>
    ) {
        val blocking get() = Issue.PROJECT_OVERFLOW in issues
        val needsApproval get() = issues.isNotEmpty()
    }

    /** Limites anteriores à requisição: exigem tempos reais da timeline, não a posição do cursor. */
    fun validate(segments: List<Segment>, projectDurationMs: Long): String? {
        if (segments.isEmpty()) return "Nenhuma fala selecionada."
        if (segments.size > MAX_SEGMENTS) return "Escolha até $MAX_SEGMENTS falas por geração para controlar a cota."
        if (segments.map { it.cueId }.toSet().size != segments.size) return "Há legendas duplicadas."
        if (projectDurationMs <= 0L) return "O vídeo não tem duração válida."
        if (segments.any { it.cueId.isBlank() || it.text.isBlank() ||
                it.startMs < 0L || it.startMs >= projectDurationMs ||
                it.subtitleEndMs <= it.startMs || it.subtitleEndMs > projectDurationMs }) {
            return "Uma legenda está fora do vídeo ou não possui texto e horários válidos."
        }
        if (segments != segments.sortedWith(compareBy<Segment> { it.startMs }.thenBy { it.subtitleEndMs }.thenBy { it.cueId })) {
            return "As falas precisam estar em ordem cronológica."
        }
        if (segments.joinToString("\n") { it.text }.length > SubtitleNarrationReuse.MAX_CHARACTERS) {
            return "O texto total excede 4.000 caracteres."
        }
        return null
    }

    /** Verifica duração REAL do WAV, preservando início e sem truncar fim. */
    fun inspect(recorded: List<Recorded>, projectDurationMs: Long): List<Inspection> =
        recorded.mapIndexed { index, item ->
            val s = item.segment
            val issues = mutableSetOf<Issue>()
            val remaining = (projectDurationMs - s.startMs).coerceAtLeast(0L)
            if (item.audioDurationMs > s.subtitleEndMs - s.startMs) issues += Issue.SUBTITLE_TOO_SHORT
            if (index + 1 < recorded.size && item.audioDurationMs > recorded[index + 1].segment.startMs - s.startMs)
                issues += Issue.NEXT_SPEECH_OVERLAP
            if (item.audioDurationMs > remaining || item.audioDurationMs < MIN_AUDIO_MS)
                issues += Issue.PROJECT_OVERFLOW
            Inspection(s, item.audioDurationMs, issues)
        }
}
