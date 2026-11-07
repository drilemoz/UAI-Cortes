package br.com.uaicortes

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Protege projetos legados sem modificar alterações que ocorreram durante a cópia. */
@Composable
internal fun ProjectMediaProtectionLifecycle(
    project: VideoProject?, screen: String, isImporting: Boolean,
    onBusyChanged: (Boolean) -> Unit, onProtected: (VideoProject) -> Boolean
) {
    val context = LocalContext.current
    val latestProject by rememberUpdatedState(project)
    val latestScreen by rememberUpdatedState(screen)
    val currentBusy by rememberUpdatedState(onBusyChanged)
    val currentSave by rememberUpdatedState(onProtected)
    LaunchedEffect(screen, project?.id, isImporting) {
        val opened = project ?: return@LaunchedEffect
        if (screen != "editor" || isImporting) return@LaunchedEffect
        currentBusy(true)
        try {
            val updated = withContext(Dispatchers.IO) {
                ProjectMediaCopies.protectProject(context.applicationContext, opened)
            }
            if (latestScreen == "editor" && latestProject === opened && updated != opened &&
                currentSave(updated)) {
                Toast.makeText(context,
                    "Mídias protegidas. Salve a edição para manter as novas referências.",
                    Toast.LENGTH_LONG).show()
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            DiagnosticRecorder.exception("media_protection", failure)
            if (latestScreen == "editor" && latestProject === opened) {
                Toast.makeText(context,
                    "Cópia incompleta. Os originais foram preservados; confira o espaço livre.",
                    Toast.LENGTH_LONG).show()
            }
        } finally {
            currentBusy(false)
        }
    }
}
