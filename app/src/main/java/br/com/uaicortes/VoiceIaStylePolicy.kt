package br.com.uaicortes

/** Interpretação por legenda > personagem conhecido > geral. Sem chamada remota. */
internal object VoiceIaStylePolicy {
    const val DEFAULT = "Neutra"
    fun valid(style: String?): Boolean = style != null &&
        GeminiNarrationStyling.presets.any { it.label == style }

    fun preferred(cue: SubtitleCue, speakers: List<SubtitleSpeaker>,
        saved: VoiceIaVoiceAssignments): String? {
        saved.cueStyles[cue.id]?.takeIf(::valid)?.let { return it }
        val speaker = cue.speakerId?.takeIf { id -> speakers.any { it.id == id } }
        speaker?.let(saved.speakerStyles::get)?.takeIf(::valid)?.let { return it }
        return saved.defaultStyle?.takeIf(::valid)
    }

    fun speakerStyle(project: VideoProject, speakerId: String, style: String?): VideoProject? {
        if (project.subtitleSpeakers.none { it.id == speakerId } ||
            (style != null && !valid(style))) return null
        val next = project.voiceIaVoices.speakerStyles.toMutableMap().apply {
            if (style == null) remove(speakerId) else put(speakerId, style)
        }
        return project.copy(voiceIaVoices = project.voiceIaVoices.copy(speakerStyles = next))
    }

    fun cueStyle(project: VideoProject, cueId: String, style: String?): VideoProject? {
        if (project.subtitles.none { it.id == cueId } ||
            (style != null && !valid(style))) return null
        val next = project.voiceIaVoices.cueStyles.toMutableMap().apply {
            if (style == null) remove(cueId) else put(cueId, style)
        }
        if (next.size > VoiceIaSpeakerVoicePolicy.MAX_CUE_OVERRIDES) return null
        return project.copy(voiceIaVoices = project.voiceIaVoices.copy(cueStyles = next))
    }

    /** Aplicação geral substitui interpretações específicas, sem remover escolhas de voz. */
    fun all(project: VideoProject, style: String): VideoProject? =
        style.takeIf(::valid)?.let { project.copy(voiceIaVoices = project.voiceIaVoices.copy(
            defaultStyle = it, speakerStyles = emptyMap(), cueStyles = emptyMap())) }
}
