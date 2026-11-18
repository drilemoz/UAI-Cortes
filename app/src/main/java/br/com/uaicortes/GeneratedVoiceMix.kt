package br.com.uaicortes

/** Ajusta o volume salvo dos vídeos atingidos pela fala. Nunca modifica a fonte de mídia. */
internal object GeneratedVoiceMix {
    const val ORIGINAL_GAIN_LIMIT = .10f

    fun lowerOriginal(clips: List<Clip>, voiceStartMs: Long, voiceDurationMs: Long): List<Clip> {
        if (voiceStartMs < 0L || voiceDurationMs <= 0L) return clips
        val voiceEndMs = voiceStartMs + voiceDurationMs
        var cursorMs = 0L
        return clips.map { clip ->
            val start = cursorMs
            cursorMs += clip.durationMs
            if (!clip.isImage && voiceStartMs < cursorMs && voiceEndMs > start &&
                clip.volume > ORIGINAL_GAIN_LIMIT) clip.copy(volume = ORIGINAL_GAIN_LIMIT)
            else clip
        }
    }
}
