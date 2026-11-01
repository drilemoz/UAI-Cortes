package br.com.uaicortes

import android.content.Context
import android.net.Uri
import java.io.File
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Diagnóstico local. Uma URI sem permissão não equivale necessariamente a arquivo apagado. */
internal object ProjectMediaAvailability {
    fun inaccessibleCount(context: Context, project: VideoProject): Int {
        val sources = buildList {
            addAll(project.clips.map { it.uri })
            addAll(project.overlays.map { it.uri })
            project.music?.let { add(it.uri) }
            addAll(project.audioTracks.map { it.uri })
        }.filter { it.isNotBlank() }.distinct().take(100)
        return sources.count { raw ->
            runCatching {
                val uri = Uri.parse(raw)
                when (uri.scheme) {
                    "file" -> File(uri.path.orEmpty()).canRead()
                    "content" -> context.contentResolver.openFileDescriptor(uri, "r")?.use { true } ?: false
                    else -> false
                }
            }.getOrDefault(false).not()
        }
    }
}

@Composable
internal fun ProjectMediaAlert(project: VideoProject?) {
    val context = LocalContext.current
    LaunchedEffect(project?.id) {
        if (project == null) return@LaunchedEffect
        val missing = withContext(Dispatchers.IO) {
            ProjectMediaAvailability.inaccessibleCount(context.applicationContext, project)
        }
        if (missing > 0) Toast.makeText(context,
            "$missing mídia(s) não acessível(is). Confira arquivos e permissões de leitura.",
            Toast.LENGTH_LONG).show()
    }
}
