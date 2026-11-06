package br.com.uaicortes

/** Regras locais e determinísticas. Sem reconhecimento de identidade ou requisição a APIs. */
internal object SubtitleSpeakerPolicy {
    const val MAX_SPEAKERS = 16
    val COLORS = listOf(0xFF237FAD.toInt(), 0xFF9963B2.toInt(),
        0xFFB77522.toInt(), 0xFF249779.toInt(), 0xFFC44D76.toInt(), 0xFF6E80B1.toInt())

    fun add(project: VideoProject, name: String, id: String): VideoProject? {
        val clean = name.trim().take(40)
        if (clean.isBlank() || id.isBlank() || project.subtitleSpeakers.size >= MAX_SPEAKERS ||
            project.subtitleSpeakers.any { it.id == id || it.name.equals(clean, ignoreCase = true) }) return null
        val color = COLORS[project.subtitleSpeakers.size % COLORS.size]
        return project.copy(subtitleSpeakers = project.subtitleSpeakers + SubtitleSpeaker(id, clean, color))
    }

    fun rename(project: VideoProject, speakerId: String, name: String, color: Int): VideoProject? {
        val clean = name.trim().take(40)
        if (clean.isBlank() || project.subtitleSpeakers.none { it.id == speakerId } ||
            project.subtitleSpeakers.any { it.id != speakerId && it.name.equals(clean, ignoreCase = true) }) return null
        return project.copy(subtitleSpeakers = project.subtitleSpeakers.map {
            if (it.id == speakerId) it.copy(name = clean, color = color) else it
        })
    }

    fun assign(project: VideoProject, cueId: String, speakerId: String?): VideoProject? {
        if (project.subtitles.none { it.id == cueId }) return null
        if (speakerId != null && project.subtitleSpeakers.none { it.id == speakerId }) return null
        return project.copy(subtitles = project.subtitles.map {
            if (it.id == cueId) it.copy(speakerId = speakerId) else it
        })
    }

    fun remove(project: VideoProject, speakerId: String): VideoProject {
        return project.copy(
            subtitleSpeakers = project.subtitleSpeakers.filterNot { it.id == speakerId },
            voiceIaVoices = project.voiceIaVoices.copy(
                speakerVoices = project.voiceIaVoices.speakerVoices - speakerId),
            subtitles = project.subtitles.map { cue ->
                if (cue.speakerId == speakerId) cue.copy(speakerId = null) else cue
            })
    }

    /** Alteração visual OPT-IN, não toca textos, tempos, IDs, fontes ou posição. */
    fun applyColors(project: VideoProject): VideoProject {
        val colors = project.subtitleSpeakers.associate { it.id to it.color }
        return project.copy(subtitles = project.subtitles.map { cue ->
            val tint = cue.speakerId?.let(colors::get)
            if (tint == null) cue else cue.copy(color = tint)
        })
    }

    /** Cues sem identidade permanecem acessíveis (null); um vínculo órfão também. */
    fun filter(cues: List<SubtitleCue>, id: String?, speakers: List<SubtitleSpeaker>): List<SubtitleCue> {
        if (id == null) return cues
        val known = speakers.mapTo(HashSet()) { it.id }
        return if (id == "__unassigned__") cues.filter { it.speakerId !in known }
            else cues.filter { it.speakerId == id }
    }
}
