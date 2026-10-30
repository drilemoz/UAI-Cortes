package br.com.uaicortes

import android.app.Application
import android.net.Uri
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Mantém o Transformer durante recriações da composição/rotação, sem colocá-lo no Bundle.
 * Exportações não são recomeçadas automaticamente após morte do processo Android.
 */
internal class ExportTaskViewModel(application: Application) : AndroidViewModel(application) {
    private val engine = ExportEngine(application.applicationContext)
    private val coordinator = ExportTaskCoordinator()
    private var polling: Job? = null
    private var projectId: String? = null

    var task by mutableStateOf(coordinator.snapshot)
        private set
    var savedVideo by mutableStateOf<Uri?>(null)
        private set
    var savedName by mutableStateOf("")
        private set

    /** Se o usuário selecionar outro projeto, impede aproveitar confirmação da exportação anterior. */
    fun setProject(id: String) {
        if (projectId == id) return
        if (task.working) {
            if (!ExportProgressUi.canCancel(task.phase)) return // conclusão MediaStore é indivisível
            cancel()
        }
        projectId = id
        savedVideo = null
        savedName = ""
        coordinator.reset()
        publish()
    }

    fun start(project: VideoProject, quality: Int, name: String) {
        if (task.working) return
        setProject(project.id)
        if (name.isBlank() || project.clips.isEmpty()) return
        val requestedName = name.trim().let { if (it.endsWith(".mp4", true)) it else "$it.mp4" }
        val id = coordinator.start(requestedName, quality, SystemClock.elapsedRealtime()) ?: return
        savedVideo = null
        savedName = ""
        publish()
        DiagnosticRecorder.action("export", "export_start_${quality}p")
        try {
            engine.start(project, quality, requestedName,
                onSaving = {
                    if (coordinator.saving(id)) publish()
                },
                onSuccess = { uri ->
                    if (coordinator.success(id)) {
                        polling?.cancel()
                        savedVideo = uri
                        savedName = requestedName
                        DiagnosticRecorder.action("export", "export_success")
                        publish()
                    }
                },
                onError = { error ->
                    if (coordinator.failure(id, error)) {
                        polling?.cancel()
                        DiagnosticRecorder.failure("export", error)
                        publish()
                    }
                })
            if (coordinator.rendering(id)) publish()
            polling?.cancel()
            polling = viewModelScope.launch {
                while (task.working) {
                    val percent = if (task.phase == ExportPhase.RENDERING)
                        engine.progressOrNull() else null
                    if (coordinator.tick(id, percent, SystemClock.elapsedRealtime())) publish()
                    delay(400)
                }
            }
        } catch (error: Exception) {
            engine.cancel() // libera arquivo temporário também se o início falhar parcialmente
            if (coordinator.failure(id, error.message ?: "Falha ao iniciar a exportação")) {
                DiagnosticRecorder.exception("export_start", error)
                publish()
            }
        }
    }

    fun cancel(): Boolean {
        if (!task.working || !ExportProgressUi.canCancel(task.phase)) return false
        val id = task.id
        val cancelled = coordinator.cancel(id)
        if (cancelled) {
            engine.cancel() // invalida o ticket antes do cancelamento assíncrono do Media3
            polling?.cancel()
            DiagnosticRecorder.action("export", "export_cancelled")
            publish()
        }
        return cancelled
    }

    fun dismissSaved() { savedVideo = null }

    private fun publish() { task = coordinator.snapshot }

    override fun onCleared() {
        polling?.cancel()
        if (task.working && ExportProgressUi.canCancel(task.phase)) engine.cancel()
        // Durante a cópia MediaStore, o engine mantém ApplicationContext até finalizar.
        super.onCleared()
    }
}
