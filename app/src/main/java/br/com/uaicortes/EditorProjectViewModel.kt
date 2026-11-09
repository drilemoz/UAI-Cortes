package br.com.uaicortes

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import java.util.UUID

/**
 * Catálogo persistente e sessão temporária separados.
 * Edições só chegam ao ProjectStore quando o usuário confirma Salvar.
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
    private val draft = ProjectDraftSession()

    var storageWarning by mutableStateOf(store.lastReadIssue)
        private set
    var projects by mutableStateOf(session.snapshot.projects)
        private set
    var project by mutableStateOf(session.snapshot.active)
        private set
    var activeProjectId by mutableStateOf(session.snapshot.activeId)
        private set
    var hasPendingChanges by mutableStateOf(false)
        private set
    var isEditingDraft by mutableStateOf(false)
        private set
    val hasSavedOriginal: Boolean get() = draft.baseline != null
    val draftBaseline: VideoProject? get() = draft.baseline

    init {
        // Rotação/recriação com ID salvo: nunca tratar projeto existente como novo.
        session.snapshot.active?.let { existing ->
            draft.begin(existing, existing)
            isEditingDraft = true
        }
    }

    private fun publish(snapshot: ProjectSnapshot) {
        projects = snapshot.projects
        project = snapshot.active
        activeProjectId = snapshot.activeId
        storageWarning = store.lastReadIssue
    }

    /** Novo projeto ou animação importada: ainda não consta em Projetos. */
    fun beginNew(edited: VideoProject) {
        draft.begin(edited, null)
        project = edited
        activeProjectId = edited.id
        isEditingDraft = true
        hasPendingChanges = true
    }

    /** Aplicação de gesto/ferramenta; não grava prefs, catálogo ou backup. */
    fun stage(edited: VideoProject) {
        draft.stage(edited)
        project = edited
        activeProjectId = edited.id
        isEditingDraft = true
        hasPendingChanges = draft.dirty
    }

    fun open(id: String): VideoProject? {
        val snapshot = session.open(id)
        publish(snapshot)
        draft.discard()
        snapshot.active?.let { draft.begin(it, it) }
        isEditingDraft = snapshot.active != null
        hasPendingChanges = false
        return project
    }

    fun restoreDraft(recovered: DraftRecovery): Boolean {
        val baseline = projects.firstOrNull { it.id == recovered.project.id }
        val hash = baseline?.json()?.toString()?.let(ProjectBackupPolicy::hash)
        if (hash != recovered.baselineHash) return false
        draft.begin(recovered.project, baseline)
        project = recovered.project
        activeProjectId = recovered.project.id
        isEditingDraft = true
        hasPendingChanges = draft.dirty
        return true
    }

    /** Salva a versão de trabalho; se falhar, o rascunho fica aberto e intacto. */
    fun commit(): VideoProject {
        val edited = requireNotNull(project) { "Nenhum projeto para salvar" }
        publish(session.save(edited)) // só publica se o ProjectStore realmente gravar
        val persisted = projects.firstOrNull { it.id == edited.id }
            ?: error("Projeto confirmado não foi encontrado no catálogo")
        draft.committed(persisted)
        project = persisted
        isEditingDraft = true
        hasPendingChanges = false
        return persisted
    }

    /** Cópia independente: não modifica o projeto original. */
    fun commitCopy(): VideoProject {
        val edited = requireNotNull(project)
        check(draft.baseline != null) { "Salvar cópia exige um projeto original" }
        val copy = edited.copy(id = UUID.randomUUID().toString(),
            title = "${edited.title} (cópia)", updated = System.currentTimeMillis())
        publish(session.save(copy))
        val persisted = projects.firstOrNull { it.id == copy.id }
            ?: error("Cópia confirmada não foi encontrada no catálogo")
        draft.begin(persisted, persisted)
        project = persisted
        isEditingDraft = true
        hasPendingChanges = false
        return persisted
    }

    fun finishWithoutSaving() {
        draft.discard()
        isEditingDraft = false
        hasPendingChanges = false
        publish(session.reload())
    }

    /** Restauração de backups é uma ação explícita, fora do rascunho do editor. */
    fun persistRestored(restored: VideoProject) {
        publish(session.save(restored))
        draft.discard()
        isEditingDraft = false
        hasPendingChanges = false
    }

    /** Renomeia projeto salvo sem comitar ou descartar alterações de um editor ativo. */
    fun rename(id: String, title: String) {
        check(!hasPendingChanges) { "Há uma edição não salva; conclua-a antes de renomear." }
        publish(session.rename(id, title))
        if (draft.current?.id == id) {
            // A sessão aberta também passa a refletir o nome persistido.
            project?.let { draft.begin(it, it) }
            hasPendingChanges = false
        }
    }

    fun delete(id: String) {
        if (draft.current?.id == id) draft.discard()
        isEditingDraft = false
        hasPendingChanges = false
        publish(session.delete(id))
    }

    fun deleteAll() {
        store.deleteAll()
        draft.discard()
        isEditingDraft = false
        hasPendingChanges = false
        publish(session.reload())
    }

    fun append(base: VideoProject?, clips: List<Clip>, createNew: Boolean): VideoProject =
        session.append(base, clips, createNew)

    companion object { private const val ACTIVE_ID = "editor_active_project_id" }
}
