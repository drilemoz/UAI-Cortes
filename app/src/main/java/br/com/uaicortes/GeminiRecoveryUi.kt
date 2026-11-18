package br.com.uaicortes

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class GeminiRecoveryState {
    var trace by mutableStateOf<GeminiRecoveryTrace?>(null)
        private set

    suspend fun <T> track(action: suspend () -> T): T {
        withContext(Dispatchers.Main.immediate) { trace = null }
        return try {
            withContext(GeminiRecoveryChannel { event ->
                withContext(Dispatchers.Main.immediate) { trace = event }
            }) { action() }
        } catch (cancel: CancellationException) {
            if (trace?.complete != true) trace = null
            throw cancel
        }
    }
}

@Composable
internal fun rememberGeminiRecovery(): GeminiRecoveryState = remember { GeminiRecoveryState() }

@Composable
internal fun GeminiRecoveryFeedback(state: GeminiRecoveryState) {
    val trace = state.trace
    trace?.status?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
    if (trace?.failed == true) {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        var pending by remember { mutableStateOf<String?>(null) }
        val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
            val report = pending
            pending = null
            if (uri != null && report != null) scope.launch {
                val saved = withContext(Dispatchers.IO) {
                    runCatching {
                        checkNotNull(context.contentResolver.openOutputStream(uri, "wt")).use { stream ->
                            stream.write(report.toByteArray(Charsets.UTF_8))
                        }
                    }.isSuccess
                }
                Toast.makeText(context, if (saved) "Relatório do Gemini salvo."
                    else "Não foi possível salvar o relatório.", Toast.LENGTH_SHORT).show()
            }
        }
        OutlinedButton(onClick = {
            pending = trace.report(AppReleaseNotes.VERSION_NAME)
            export.launch("UaiCortes-diagnostico-gemini.txt")
        }) { Text("Exportar relatório") }
    }
}
