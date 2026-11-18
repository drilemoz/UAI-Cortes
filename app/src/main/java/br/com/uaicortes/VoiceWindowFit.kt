package br.com.uaicortes

/** Limites em tempo de montagem; não deduz fonemas nem altera legendas. */
internal object VoiceWindowFit {
    fun targetMs(project: VideoProject, cue: SubtitleCue): Long {
        val next = project.subtitles.asSequence().filter { it.startMs > cue.startMs }
            .minOfOrNull { it.startMs }
        return (minOf(cue.endMs, next ?: Long.MAX_VALUE, project.durationMs) - cue.startMs)
            .coerceAtLeast(0L)
    }

    // Sem resultado seguro, manter o áudio original e a possibilidade de ajuste manual.
    fun adjust(wave: GeminiWave, targetMs: Long): GeminiWave? {
        if (targetMs < 250L || wave.bytes.size > 4 * 1024 * 1024 ||
            wave.durationMs !in 250L..25_000L) return null
        val trimmed = VoiceIaWavStretch.trimSilence(wave.bytes)
        val candidate = trimmed ?: wave
        return when {
            candidate.durationMs <= targetMs -> trimmed
            candidate.durationMs.toDouble() / targetMs <= VoiceIaSync.MAX_LOCAL_SPEED ->
                VoiceIaWavStretch.shorten(candidate.bytes, targetMs)
            else -> trimmed // preserva todas as palavras, mesmo quando a fala não cabe
        }
    }
}
