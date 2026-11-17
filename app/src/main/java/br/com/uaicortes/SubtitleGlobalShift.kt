package br.com.uaicortes

/** Desloca todas as legendas por um mesmo valor, preservando durações, textos e tempos de palavras. */
internal object SubtitleGlobalShift {
    const val MAX_MS = 5000L

    /** Positivo = legendas mais tarde. Limitado para nenhuma legenda sair do vídeo. */
    fun allowed(cues: List<SubtitleCue>, deltaMs: Long, durationMs: Long): Long {
        if (cues.isEmpty()) return 0L
        val lower = minOf(0L, -cues.minOf { it.startMs })
        val upper = maxOf(0L, durationMs - cues.maxOf { it.endMs })
        return deltaMs.coerceIn(-MAX_MS, MAX_MS).coerceIn(lower, upper)
    }

    fun apply(cues: List<SubtitleCue>, deltaMs: Long, durationMs: Long): Pair<List<SubtitleCue>, Long> {
        val applied = allowed(cues, deltaMs, durationMs)
        if (applied == 0L) return cues to 0L
        return cues.map { cue ->
            cue.copy(startMs = cue.startMs + applied, endMs = cue.endMs + applied,
                wordTimings = cue.wordTimings.map {
                    SubtitleWordTiming(it.startMs + applied, it.endMs + applied)
                })
        } to applied
    }
}
