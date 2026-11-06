package br.com.uaicortes

import org.json.JSONObject

/** Estilos materializados nos SubtitleCue para a prévia e a exportação usarem a mesma
 * fonte de verdade. A exceção individual é um ID estável; texto/tempos não são copiados.
 * Perfis futuros se aplicam quando uma fala recebe o personagem, sem consulta à IA.
 */
internal object SubtitleSpeakerStyles {
    private val fullStyle = SubtitleStyles.CopyOptions(
        appearance = true, position = true, animation = true, wordColors = false)

    fun template(cue: SubtitleCue): SubtitleCue = cue.copy(
        id = "subtitle-style-template", text = "Exemplo", startMs = 0, endMs = 3000,
        speakerId = null, wordTimings = emptyList(), wordColors = emptyList())

    fun json(styles: Map<String, SubtitleCue>): JSONObject = JSONObject().apply {
        styles.entries.take(SubtitleSpeakerPolicy.MAX_SPEAKERS).forEach { (id, cue) ->
            if (id.isNotBlank() && id.length <= 80) put(id, template(cue).json())
        }
    }

    fun read(json: JSONObject?): Map<String, SubtitleCue> {
        if (json == null) return emptyMap()
        val result = linkedMapOf<String, SubtitleCue>()
        val keys = json.keys()
        while (keys.hasNext() && result.size < SubtitleSpeakerPolicy.MAX_SPEAKERS) {
            val id = keys.next()
            if (id.isNotBlank() && id.length <= 80) {
                val style = runCatching { json.optJSONObject(id)?.let(SubtitleCue::read) }.getOrNull()
                if (style != null) result[id] = template(style)
            }
        }
        return result
    }

    /** Alterar o estilo de um personagem afeta só suas falas sem exceção individual. */
    fun apply(project: VideoProject, speakerId: String, proposed: SubtitleCue,
        options: SubtitleStyles.CopyOptions): VideoProject? {
        if (project.subtitleSpeakers.none { it.id == speakerId }) return null
        if (!options.appearance && !options.position && !options.animation) return null
        val oldStyle = project.subtitleSpeakerStyles[speakerId]
            ?: project.subtitles.firstOrNull { it.speakerId == speakerId }
            ?: SubtitleCue(text = "Exemplo", startMs = 0, endMs = 3000)
        val nextStyle = template(SubtitleStyles.applySelectedToAll(
            listOf(oldStyle), proposed, options).single())
        val next = project.subtitles.map { cue ->
            if (cue.speakerId != speakerId || cue.id in project.subtitleStyleOverrides) cue
            else SubtitleStyles.applySelectedToAll(listOf(cue), proposed, options).single()
        }
        return project.copy(subtitles = next,
            subtitleSpeakerStyles = project.subtitleSpeakerStyles + (speakerId to nextStyle))
    }

    /** Aplica o padrão a uma fala quando ela é atribuída ao personagem. */
    fun inherit(project: VideoProject, cue: SubtitleCue, speakerId: String?): SubtitleCue {
        val base = cue.copy(speakerId = speakerId)
        val style = speakerId?.let(project.subtitleSpeakerStyles::get) ?: return base
        return if (cue.id in project.subtitleStyleOverrides) base
            else SubtitleStyles.applySelectedToAll(listOf(base), style, fullStyle).single()
    }

    /** Uma edição de texto/tempo não bloqueia a herança. Só mudanças visuais criam exceção. */
    fun manualEdit(project: VideoProject, before: SubtitleCue, after: SubtitleCue): VideoProject {
        val styleChanged = !sameAppearance(before, after)
        val styledSpeaker = after.speakerId?.let(project.subtitleSpeakerStyles::containsKey) == true
        val overrides = if (styleChanged && styledSpeaker)
            project.subtitleStyleOverrides + after.id else project.subtitleStyleOverrides
        return project.copy(subtitles = project.subtitles.map { if (it.id == before.id) after else it },
            subtitleStyleOverrides = overrides)
    }

    fun reset(project: VideoProject, cueId: String): VideoProject? {
        val cue = project.subtitles.firstOrNull { it.id == cueId } ?: return null
        val style = cue.speakerId?.let(project.subtitleSpeakerStyles::get) ?: return null
        val reset = SubtitleStyles.applySelectedToAll(listOf(cue), style, fullStyle).single()
        return project.copy(subtitles = project.subtitles.map { if (it.id == cueId) reset else it },
            subtitleStyleOverrides = project.subtitleStyleOverrides - cueId)
    }

    private fun sameAppearance(a: SubtitleCue, b: SubtitleCue): Boolean {
        val visual = SubtitleStyles.applySelectedToAll(listOf(a), b, fullStyle).single()
        return visual == a && (a.text != b.text || a.wordColors == b.wordColors)
    }
}
