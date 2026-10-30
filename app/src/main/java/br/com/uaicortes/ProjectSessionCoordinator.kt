package br.com.uaicortes

/**
 * Fonte única dos snapshots de projeto; testável sem Android/Compose.
 * Atualiza o ID persistível somente após obter snapshot completo do repositório.
 */
internal class ProjectSessionCoordinator(
    private val controller: ProjectController,
    restoredId: String = "",
    private val onActiveIdChanged: (String) -> Unit = {}
) {
    var snapshot: ProjectSnapshot = controller.load(restoredId)
        private set

    init { onActiveIdChanged(snapshot.activeId) }

    private fun accept(next: ProjectSnapshot): ProjectSnapshot {
        snapshot = next
        onActiveIdChanged(next.activeId)
        return next
    }

    fun save(project: VideoProject): ProjectSnapshot = accept(controller.save(project))
    fun open(id: String): ProjectSnapshot = accept(controller.open(id))
    fun delete(id: String): ProjectSnapshot = accept(controller.delete(id, snapshot.activeId))
    fun append(base: VideoProject?, clips: List<Clip>, createNew: Boolean): VideoProject =
        controller.append(base, clips, createNew)
}
