package br.com.uaicortes

import android.net.Uri
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Tipo da tarefa em curso. Uma importação não deve sobrescrever o resultado de outra. */
internal enum class MediaTaskKind { VIDEO, AUDIO, OVERLAY }

/**
 * Exclusão mútua sem estado Android. A UI observa o estado Compose separadamente.
 * O término de uma tarefa antiga não libera uma tarefa diferente.
 */
internal data class MediaTaskTicket(val serial: Long, val kind: MediaTaskKind)

/** Tickets impedem que o finally de uma leitura cancelada solte outra leitura do mesmo tipo. */
internal class MediaTaskGate {
    private var nextSerial = 0L
    var active: MediaTaskTicket? = null
        private set

    fun tryStart(kind: MediaTaskKind): MediaTaskTicket? {
        if (active != null) return null
        return MediaTaskTicket(++nextSerial, kind).also { active = it }
    }

    fun isActive(ticket: MediaTaskTicket): Boolean = active == ticket

    fun finish(ticket: MediaTaskTicket): Boolean {
        if (!isActive(ticket)) return false
        active = null
        return true
    }
}

/** Desconsidera respostas tardias depois de mudar o projeto, a tela ou a ferramenta. */
internal data class MediaTaskOrigin(val projectId: String, val screen: String, val action: String) {
    fun matches(projectIdNow: String, screenNow: String, actionNow: String): Boolean =
        projectId == projectIdNow && screen == screenNow && action == actionNow
}

internal data class OverlayImportSource(val isImage: Boolean, val durationMs: Long)

/**
 * Limite entre operações de conteúdo (ContentResolver/metadata) e estados da interface.
 * Nenhuma escrita de projeto ou atualização Compose ocorre no dispatcher de IO.
 */
internal class MediaImportTasks(
    private val media: MediaImportController,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    suspend fun videos(uris: List<Uri>, persistDocuments: Boolean): ClipImportBatch =
        withContext(ioDispatcher) { media.videoClips(uris, persistDocuments) }

    suspend fun audio(uri: Uri, projectDurationMs: Long, cursorMs: Long): AudioTrack? =
        withContext(ioDispatcher) {
            media.persistReadAccess(uri)
            val name = media.title(uri)
            val duration = media.duration(uri)
            MediaImportRules.audio(uri.toString(), name, duration, projectDurationMs, cursorMs)
        }

    suspend fun overlay(uri: Uri, availableMs: Long): OverlayImportSource? =
        withContext(ioDispatcher) {
            media.persistReadAccess(uri)
            if (availableMs <= 0L) return@withContext null
            val isImage = media.isImage(uri)
            val duration = if (isImage) 5_000L else media.duration(uri)
            if (duration <= 0L) null
            else OverlayImportSource(isImage, minOf(duration, availableMs))
        }
}
