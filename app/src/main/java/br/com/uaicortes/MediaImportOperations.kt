package br.com.uaicortes

import android.content.Context
import android.content.Intent
import android.net.Uri

/** Metadados lidos de conteúdo local. Não cria um clipe para mídia inválida. */
internal data class ClipSource(val uri: String, val title: String, val durationMs: Long)
internal data class ClipImportBatch(val clips: List<Clip>, val rejected: Int)

internal object MediaImportRules {
    fun clips(sources: List<ClipSource>): ClipImportBatch {
        val valid = sources.filter { it.uri.isNotBlank() && it.durationMs > 0L }
        return ClipImportBatch(valid.map { Clip(uri = it.uri, title = it.title, sourceDurationMs = it.durationMs) },
            sources.size - valid.size)
    }

    /** Imagens duplicadas e itens acima do limite não entram na seleção. */
    fun images(current: List<String>, incoming: List<String>, max: Int = ImageAnimationPlan.MAX_IMAGES): List<String> =
        (current + incoming).filter(String::isNotBlank).distinct().take(max)

    /** Áudio usa início no projeto, mas mantém offset de origem independente. */
    fun audio(uri: String, title: String, sourceDurationMs: Long,
              projectDurationMs: Long, cursorMs: Long): AudioTrack? {
        if (sourceDurationMs < AudioTrackTiming.MIN_TRIM_MS || projectDurationMs < AudioTrackTiming.MIN_TRIM_MS)
            return null
        val lastStart = (projectDurationMs - AudioTrackTiming.MIN_TRIM_MS).coerceAtLeast(0L)
        val start = cursorMs.coerceIn(0L, lastStart)
        val length = minOf(sourceDurationMs, projectDurationMs - start)
        return if (length >= AudioTrackTiming.MIN_TRIM_MS)
            AudioTrack(uri, title, startMs = start, durationMs = length) else null
    }
}

/** Adaptador Android para permissões e metadados, reutilizado por galeria e SAF. */
internal class MediaImportController(private val context: Context) {
    fun persistReadAccess(uri: Uri) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } // Galeria/MediaStore não oferecem concessões persistíveis; esse caso é esperado.
    }

    fun videoClips(uris: List<Uri>, persistDocumentAccess: Boolean): ClipImportBatch {
        val sources = uris.map { uri ->
            if (persistDocumentAccess) persistReadAccess(uri)
            ClipSource(uri.toString(), runCatching { mediaName(context, uri) }.getOrDefault("Vídeo"),
                runCatching { verifiedMediaDuration(context, uri) }.getOrDefault(0L))
        }
        return MediaImportRules.clips(sources)
    }

    fun isImage(uri: Uri): Boolean = context.contentResolver.getType(uri)?.startsWith("image/") == true
    fun duration(uri: Uri): Long = verifiedMediaDuration(context, uri)
    fun title(uri: Uri): String = mediaName(context, uri)
}
