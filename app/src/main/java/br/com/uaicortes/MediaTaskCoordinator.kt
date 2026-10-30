package br.com.uaicortes

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Coordena exclusivamente leituras/importações de mídia. O escopo pertence à UI e é
 * encerrado quando ela sai da composição. Não retém Context nem persiste trabalhos.
 * Todas as chamadas públicas e callbacks são executados na thread da interface.
 */
internal class MediaTaskCoordinator(
    private val scope: CoroutineScope,
    private val onActiveChange: (MediaTaskKind?) -> Unit,
    private val onError: (Exception) -> Unit
) {
    private val gate = MediaTaskGate()
    private var job: Job? = null
    private var origin: MediaTaskOrigin? = null

    val active: MediaTaskKind? get() = gate.active?.kind

    fun start(kind: MediaTaskKind, taskOrigin: MediaTaskOrigin, operation: suspend () -> Unit): Boolean {
        val ticket = gate.tryStart(kind) ?: return false
        origin = taskOrigin
        onActiveChange(kind)
        val launched = scope.launch(start = CoroutineStart.LAZY) {
            try {
                operation()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (gate.isActive(ticket)) onError(error)
            } finally {
                // A conclusão de uma tarefa antiga não limpa o estado de uma nova.
                if (gate.finish(ticket)) {
                    job = null
                    origin = null
                    onActiveChange(null)
                }
            }
        }
        job = launched
        launched.start()
        return true
    }

    /** Libera a interface imediatamente e cancela a leitura em andamento. */
    fun cancel(): Boolean {
        val ticket = gate.active ?: return false
        gate.finish(ticket)
        val previous = job
        job = null
        origin = null
        onActiveChange(null)
        previous?.cancel()
        return true
    }

    /** Evita aplicar respostas em outra rota/projeto/ferramenta. */
    fun cancelIfDestinationChanged(projectId: String, screen: String, action: String): Boolean {
        val previous = origin ?: return false
        if (previous.matches(projectId, screen, action)) return false
        return cancel()
    }
}
