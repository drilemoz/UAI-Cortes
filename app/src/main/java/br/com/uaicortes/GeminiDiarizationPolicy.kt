package br.com.uaicortes

/** Associa os rótulos anônimos do Gemini a personagens SOMENTE após revisão explícita.
 * Nunca deduz nomes nem relaciona duas gravações diferentes pelo número da voz.
 * Não faz rede e não altera a mídia original.
 */
internal object GeminiDiarizationPolicy {
    const val CREATE = "__create__"
    const val IGNORE = "__ignore__"
    private val idPattern = Regex("^gemini_([0-9a-f]{32})_(spk_[1-8])$")
    private val labelPattern = Regex("^spk_[1-8]$")

    fun provisionalId(session: String, raw: String): String? {
        if (!session.matches(Regex("[0-9a-f]{32}")) || !labelPattern.matches(raw)) return null
        return "gemini_${session}_${raw}"
    }

    fun label(id: String): String? {
        val match = idPattern.matchEntire(id) ?: return null
        return "Voz ${match.groupValues[2].removePrefix("spk_")}"
    }

    fun suggestions(cues: List<SubtitleCue>): List<String> =
        cues.mapNotNull { it.speakerId?.takeIf { id -> label(id) != null } }.distinct()

    /** Retorna null se seleção/nome inválido ou capacidade excedida: nenhuma edição parcial. */
    fun prepare(project: VideoProject, generated: List<SubtitleCue>, replace: Boolean,
        decisions: Map<String,String>, names: Map<String,String>): VideoProject? {
        val speakers = project.subtitleSpeakers.toMutableList()
        val mapped = HashMap<String,String?>()
        for (id in suggestions(generated)) {
            val selected = decisions[id] ?: CREATE
            when (selected) {
                IGNORE -> mapped[id] = null
                CREATE -> {
                    val name = (names[id] ?: label(id).orEmpty()).trim()
                    if (name.isEmpty() || name.length > 40 || speakers.size >= SubtitleSpeakerPolicy.MAX_SPEAKERS ||
                        speakers.any { it.name.equals(name, ignoreCase = true) || it.id == id }) return null
                    speakers += SubtitleSpeaker(id,name,SubtitleSpeakerPolicy.COLORS[
                        speakers.size % SubtitleSpeakerPolicy.COLORS.size])
                    mapped[id] = id
                }
                else -> {
                    if (project.subtitleSpeakers.none { it.id == selected }) return null
                    mapped[id] = selected
                }
            }
        }
        val updated = generated.map { cue ->
            val resolved = cue.speakerId?.let { mapped[it] }
            cue.copy(speakerId = resolved)
        }
        return project.copy(subtitles = (if (replace) emptyList() else project.subtitles) + updated,
            subtitleSpeakers = speakers)
    }
}
