package br.com.uaicortes

/** Camada de acesso ao cadastro, sem dependência de UI. */
internal interface ProjectRepository {
    fun load(): List<VideoProject>
    fun save(project: VideoProject)
    fun delete(id: String)
}

internal class StoredProjectRepository(private val store: ProjectStore) : ProjectRepository {
    override fun load(): List<VideoProject> = store.load()
    override fun save(project: VideoProject) = store.save(project)
    override fun delete(id: String) = store.delete(id)
}

internal data class ProjectSnapshot(
    val projects: List<VideoProject>,
    val active: VideoProject?,
    val activeId: String = active?.id.orEmpty()
)

/** Uma atualização só chega à UI após o armazenamento aceitar a alteração.
 * Não substitui o ProjectStore nem seu esquema JSON.
 */
internal class ProjectController(private val repository: ProjectRepository) {
    fun load(activeId: String = ""): ProjectSnapshot {
        val all = repository.load()
        return ProjectSnapshot(all, all.firstOrNull { it.id == activeId })
    }

    fun save(project: VideoProject): ProjectSnapshot {
        repository.save(project) // exceção interrompe antes de atualizar a UI
        val all = repository.load()
        // Preserva os valores de edição em memória, assim como a tela antiga fazia.
        return ProjectSnapshot(all, project)
    }

    fun delete(id: String, activeId: String): ProjectSnapshot {
        repository.delete(id)
        return load(activeId.takeUnless { it == id }.orEmpty())
    }

    fun open(id: String): ProjectSnapshot = load(id)

    fun append(base: VideoProject?, clips: List<Clip>, createNew: Boolean): VideoProject {
        require(clips.isNotEmpty()) { "Nenhum clipe para inserir" }
        val target = if (createNew) VideoProject() else base ?: VideoProject()
        return target.copy(clips = target.clips + clips)
    }
}
