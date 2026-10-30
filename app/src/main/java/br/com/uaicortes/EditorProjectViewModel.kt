package br.com.uaicortes

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle

/**
 * O ViewModel é o proprietário do cadastro em memória durante rotações/recriações da UI.
 * Só o ID ativo vai ao SavedStateHandle; clipes, URIs, efeitos e JSON permanecem no ProjectStore.
 * Operações que falham não publicam estados parciais na UI.
 */
internal class EditorProjectViewModel(
    application: Application,
    private val state: SavedStateHandle
) : AndroidViewModel(application) {
    val store = ProjectStore(application.applicationContext)
    private val session = ProjectSessionCoordinator(
        ProjectController(StoredProjectRepository(store)),
        state[ACTIVE_ID] ?: "",
        onActiveIdChanged = { state[ACTIVE_ID] = it }
    )

    var projects by mutableStateOf(session.snapshot.projects)
        private set
    var project by mutableStateOf(session.snapshot.active)
        private set
    var activeProjectId by mutableStateOf(session.snapshot.activeId)
        private set

    private fun publish(snapshot: ProjectSnapshot) {
        projects = snapshot.projects
        project = snapshot.active
        activeProjectId = snapshot.activeId
    }

    fun save(edited: VideoProject) = publish(session.save(edited))

    fun open(id: String): VideoProject? {
        publish(session.open(id))
        return project
    }

    fun delete(id: String) = publish(session.delete(id))

    fun append(base: VideoProject?, clips: List<Clip>, createNew: Boolean): VideoProject =
        session.append(base, clips, createNew)

    companion object { private const val ACTIVE_ID = "editor_active_project_id" }
}
