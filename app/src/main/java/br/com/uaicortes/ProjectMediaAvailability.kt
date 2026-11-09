package br.com.uaicortes

import android.content.Context
import android.net.Uri
import java.io.File
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Diagnóstico local. Uma URI sem permissão não equivale necessariamente a arquivo apagado. */
internal object ProjectMediaAvailability {
    fun canOpen(context: Context, raw: String): Boolean = runCatching {
        val uri = Uri.parse(raw)
        when (uri.scheme) {
            "file" -> File(uri.path.orEmpty()).canRead()
            "content" -> {
                val descriptorReady = runCatching {
                    context.contentResolver.openFileDescriptor(uri, "r")?.use { true } == true
                }.getOrDefault(false)
                // Alguns provedores virtuais permitem stream, mas não FileDescriptor.
                descriptorReady || runCatching {
                    context.contentResolver.openInputStream(uri)?.use { true } == true
                }.getOrDefault(false)
            }
            else -> false
        }
    }.getOrDefault(false)

    fun inaccessibleCount(context: Context, project: VideoProject): Int {
        val sources = buildList {
            addAll(project.clips.map { it.uri })
            addAll(project.overlays.map { it.uri })
            project.music?.let { add(it.uri) }
            addAll(project.audioTracks.map { it.uri })
        }.filter { it.isNotBlank() }.distinct().take(100)
        return sources.count { !canOpen(context, it) }
    }
}

/** Aviso acionável uma vez por projeto/sessão, sem bloquear leituras na UI. */
@Composable
internal fun ProjectMediaAlert(project: VideoProject?, onRepair: () -> Unit) {
    val context = LocalContext.current
    var missing by remember(project?.id) { mutableIntStateOf(0) }
    var checked by remember(project?.id) { mutableStateOf(false) }
    var ignoredIds by remember { mutableStateOf(setOf<String>()) }
    LaunchedEffect(project) {
        checked = false
        missing = if (project == null) 0 else withContext(Dispatchers.IO) {
            ProjectMediaAvailability.inaccessibleCount(context.applicationContext, project)
        }
        checked = true
        if (project != null && missing > 0) {
            DiagnosticRecorder.action("editor", "media_access_missing")
            DiagnosticRecorder.mediaAvailability(missing)
        }
    }
    if (project != null && checked && missing > 0 && project.id !in ignoredIds) {
        AlertDialog(
            onDismissRequest = { ignoredIds = ignoredIds + project.id },
            title = { Text("Mídias inacessíveis") },
            text = { Text("$missing arquivo(s) do projeto não estão acessíveis. " +
                "As edições foram preservadas. Você pode localizar as mídias sem perder os cortes ou legendas.") },
            confirmButton = { Button(onClick = {
                ignoredIds = ignoredIds + project.id
                onRepair()
            }) { Text("Reparar mídias") } },
            dismissButton = { TextButton(onClick = {
                ignoredIds = ignoredIds + project.id
            }) { Text("Agora não") } }
        )
    }
}
