package br.com.uaicortes

import android.app.Application
import android.net.Uri
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Ciclo de vida da renderização de imagens, separado da composição Compose.
 * Reutiliza os tickets/estágios do exportador de vídeos e mantém o Transformer
 * ao recriar a Activity. Não retoma trabalhos após morte do processo.
 */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal class ImageAnimationTaskViewModel(application: Application) : AndroidViewModel(application) {
    // Um motor por tentativa: IO de uma tentativa cancelada não pode apagar arquivos da próxima.
    private var activeEngine: ImageAnimationEngine? = null
    private val tasks = ExportTaskCoordinator()
    private var preparation: Job? = null
    private var polling: Job? = null

    var task by mutableStateOf(tasks.snapshot)
        private set
    var savedVideo by mutableStateOf<Uri?>(null)
        private set

    fun start(images: List<Uri>, options: ImageAnimationOptions): Boolean {
        if (task.working || images.isEmpty() || images.size > ImageAnimationPlan.MAX_IMAGES) return false
        val ticket = tasks.start("UaiCortes_Animado.mp4", options.quality,
            SystemClock.elapsedRealtime()) ?: return false
        val sources = images.toList()
        val selectedOptions = options.copy()
        val engine = ImageAnimationEngine(getApplication<Application>().applicationContext)
        activeEngine = engine
        savedVideo = null
        publish()
        preparation = viewModelScope.launch {
            try {
                engine.start(sources, selectedOptions,
                    onStage = { stage ->
                        val changed = when (stage) {
                            "Exportando vídeo" -> tasks.rendering(ticket)
                            "Salvando na galeria" -> tasks.saving(ticket)
                            else -> false
                        }
                        if (changed) publish()
                    },
                    onSaved = { uri ->
                        if (tasks.success(ticket)) {
                            savedVideo = uri
                            polling?.cancel()
                            publish()
                        }
                    },
                    onError = { message -> fail(ticket, message) })
            } catch (_: CancellationException) {
                // Cancelamento é esperado, não um erro exibido ao usuário.
                engine.cancel()
                if (tasks.cancel(ticket)) publish()
            } catch (error: Exception) {
                DiagnosticRecorder.exception("image_animation", error)
                engine.cancel()
                fail(ticket, error.message ?: "Erro ao gerar animação")
            }
        }
        polling?.cancel()
        polling = viewModelScope.launch {
            while (task.working && task.id == ticket) {
                val measured = if (task.phase == ExportPhase.RENDERING)
                    runCatching { engine.progress() }.getOrNull() else null
                if (tasks.tick(ticket, measured, SystemClock.elapsedRealtime())) publish()
                delay(300)
            }
        }
        return true
    }

    /** Nunca interrompe a etapa indivisível de cópia ao MediaStore. */
    fun cancel(): Boolean {
        if (!task.working || !ExportProgressUi.canCancel(task.phase) || activeEngine?.isSaving == true) return false
        val ticket = task.id
        if (!tasks.cancel(ticket)) return false
        preparation?.cancel()
        activeEngine?.cancel()
        polling?.cancel()
        publish()
        return true
    }

    fun canCancel(): Boolean = task.working &&
        ExportProgressUi.canCancel(task.phase) && activeEngine?.isSaving != true

    fun dismissSaved() { savedVideo = null }

    private fun fail(ticket: Long, message: String) {
        if (tasks.failure(ticket, message)) {
            polling?.cancel()
            publish()
        }
    }

    private fun publish() { task = tasks.snapshot }

    override fun onCleared() {
        polling?.cancel()
        if (task.working && canCancel()) {
            preparation?.cancel()
            activeEngine?.cancel()
        }
        // MediaStore finaliza com ApplicationContext, mesmo se a Activity foi fechada.
        super.onCleared()
    }
}
