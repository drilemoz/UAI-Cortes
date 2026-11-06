package br.com.uaicortes

/**
 * Separa alterações não confirmadas do catálogo de projetos.
 * Nenhum método desta classe faz I/O. O catálogo só é alterado após commit explícito.
 */
internal class ProjectDraftSession {
    var baseline: VideoProject? = null
        private set
    var current: VideoProject? = null
        private set

    val active: Boolean get() = current != null
    val isNew: Boolean get() = active && baseline == null
    val dirty: Boolean get() = current?.let { candidate ->
        val saved = baseline
        saved == null || candidate.copy(updated = 0L) != saved.copy(updated = 0L)
    } ?: false

    fun begin(project: VideoProject, saved: VideoProject?) {
        require(saved == null || saved.id == project.id) { "Projeto e versão salva não correspondem" }
        baseline = saved
        current = project
    }

    fun stage(project: VideoProject) {
        val active = current
        if (active == null) begin(project, null)
        else {
            require(active.id == project.id) { "Alteração pertence a outro projeto" }
            current = project
        }
    }

    fun committed(saved: VideoProject) {
        require(current?.id == saved.id) { "Confirmação de outro projeto" }
        baseline = saved
        current = saved
    }

    fun discard(): VideoProject? {
        val saved = baseline
        baseline = null
        current = null
        return saved
    }
}
