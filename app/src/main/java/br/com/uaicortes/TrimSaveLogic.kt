package br.com.uaicortes

/** Aplica o rascunho do corte apenas quando válido. Nulo impede sair sem salvar o ajuste. */
object TrimSaveLogic {
    fun applyPending(project: VideoProject, clipId: String, begin: Long, finish: Long): VideoProject? {
        val clip = project.clips.firstOrNull { it.id == clipId } ?: return null
        if (!TrimLogic.valid(begin, finish, clip.sourceDurationMs)) return null
        if (begin == clip.startMs && finish == clip.endMs) return project
        return project.copy(clips = project.clips.map { existing ->
            if (existing.id == clipId) existing.copy(startMs = begin, endMs = finish) else existing
        })
    }
}
