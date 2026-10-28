package br.com.uaicortes

/** Operações de salvar escolhas da tela Transições sem mutar o projeto original.
 * A opção "todos" afeta todas as junções entre vídeos, nunca cada arquivo de origem.
 */
object TransitionBatch {
    fun apply(
        project: VideoProject, boundary: Int,
        kind: String, durationMs: Long, applyAll: Boolean
    ): VideoProject {
        if (project.clips.size < 2 || !TransitionEffects.supported(kind)) return project
        if (!applyAll && boundary !in 0 until project.clips.lastIndex) return project
        val joins = project.clips.zipWithNext().map { (a,b) -> a.id to b.id }
        val selected = if (applyAll) joins else listOf(joins[boundary])
        val ids = selected.toSet()
        val remaining = project.transitions.filterNot { (it.fromId to it.toId) in ids }
        if (kind == TransitionEffects.CUT) return project.copy(transitions = remaining)
        val ms = durationMs.coerceIn(250L, 1000L)
        return project.copy(transitions = remaining + selected.map { (a,b) ->
            ClipTransition(a,b,kind,ms)
        })
    }
}
