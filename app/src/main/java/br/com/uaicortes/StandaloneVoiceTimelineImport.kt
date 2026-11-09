package br.com.uaicortes

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Importação explícita para projeto aberto, sem reutilizar o WAV removível da biblioteca. */
internal object StandaloneVoiceTimelineImport {
    fun callback(context: Context, active: VideoProject?, cursor: Long,
        latest: () -> VideoProject?, persist: (VideoProject) -> Boolean,
        onSuccess: () -> Unit): (suspend (File) -> Boolean)? {
        if (active?.clips?.isNotEmpty() != true) return null
        return { file ->
            val ok = add(context, file, active, cursor, { latest() === active }, persist)
            if (ok) onSuccess()
            ok
        }
    }
    suspend fun add(context: Context, source: File, project: VideoProject, cursorMs: Long,
        stillCurrent: () -> Boolean, persist: (VideoProject) -> Boolean): Boolean {
        if (!source.isFile || project.clips.isEmpty() || project.durationMs <= 0L) return false
        val cursor = cursorMs.coerceIn(0L, project.durationMs)
        if (!stillCurrent()) return false
        val copy = ProjectMediaCopies.copy(context, Uri.fromFile(source), project.id,
            ProjectMediaCopies.Kind.AUDIO)
        val duration = withContext(Dispatchers.IO) { verifiedMediaDuration(context, copy) }
        if (duration < 250L || duration > project.durationMs - cursor || !stillCurrent() ||
            project.audioTracks.any { it.uri == copy.toString() }) return false
        return persist(project.copy(audioTracks = project.audioTracks + AudioTrack(
            uri = copy.toString(), title = "Narração Gemini",
            startMs = cursor, durationMs = duration, volume = 1f)))
    }
}
