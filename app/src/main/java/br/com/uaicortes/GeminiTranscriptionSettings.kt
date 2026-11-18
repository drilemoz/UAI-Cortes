package br.com.uaicortes

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class GeminiTranscriptionPreferences(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "gemini_transcription_preferences", Context.MODE_PRIVATE)

    fun isDefault(): Boolean = preferences.getBoolean("default_automatic_subtitles", false)
    fun setDefault(enabled: Boolean) {
        preferences.edit().putBoolean("default_automatic_subtitles", enabled).apply()
    }
}

@Composable
internal fun GeminiTranscriptionSettings(store: GeminiCredentialStore, hasKey: Boolean) {
    val context = LocalContext.current
    val preferences = remember(context) { GeminiTranscriptionPreferences(context) }
    val client = remember(context) { GeminiSubtitleClient(context) }
    val recovery = rememberGeminiRecovery()
    val scope = rememberCoroutineScope()
    var enabled by remember(preferences) { mutableStateOf(preferences.isDefault()) }
    var confirm by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var result by remember(hasKey) { mutableStateOf("") }
    var failed by remember(hasKey) { mutableStateOf(false) }
    DisposableEffect(client) { onDispose { client.cancel() } }

    Surface(shape = UaiShapes.card, color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Transcrição e legendas automáticas", style = MaterialTheme.typography.titleSmall)
            Text(GeminiTranscriptionRequest.LABEL, style = MaterialTheme.typography.bodyLarge)
            Text("Padrão com horários por palavra. Se o modelo estiver indisponível, sobrecarregado ou sem cota, " +
                "tenta Flash-Lite uma vez com a mesma chave.",
                style = MaterialTheme.typography.bodySmall)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Usar por padrão em Legendas automáticas", Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium)
                Switch(checked = enabled, onCheckedChange = { enabled = it; preferences.setDefault(it) })
            }
            if (enabled) Text("O app abre Gemini primeiro. O áudio só é enviado quando você toca em Enviar e gerar.",
                style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = { confirm = true }, enabled = hasKey && !busy) {
                Text(if (busy) "Testando…" else "Testar transcrição")
            }
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (result.isNotBlank()) Text(result, style = MaterialTheme.typography.bodySmall,
                color = if (failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
            GeminiRecoveryFeedback(recovery)
        }
    }
    if (confirm) AlertDialog(onDismissRequest = { confirm = false }, shape = UaiShapes.dialog,
        title = { Text("Testar transcrição com Gemini?") },
        text = { Text("Envia só uma amostra de voz sintética de menos de 2 segundos, incluída no app. " +
            "Não usa seu microfone nem seus projetos. O teste também pode tentar Flash-Lite e usa cota da API.") },
        confirmButton = { TextButton(onClick = {
            confirm = false
            busy = true
            failed = false
            result = ""
            scope.launch {
                var phase = SubtitleCloudPhase.KEY
                try {
                    val key = withContext(Dispatchers.IO) { store.read() } ?: error("Chave ausente")
                    phase = SubtitleCloudPhase.TRANSCRIBE
                    val probe = recovery.track {
                        client.probe(key) { context.resources.openRawResource(R.raw.gemini_transcribe_probe) }
                    }
                    result = if (probe.model == GeminiLiteTranscription.MODEL)
                        "Teste aprovado com Flash-Lite · ${probe.count} frases com horários estimados."
                    else "Teste aprovado com Transcribe · ${probe.count} palavras com horários."
                } catch (cancel: CancellationException) {
                    throw cancel
                } catch (error: Exception) {
                    failed = true
                    val issue = SubtitleCloudIssue.from(phase, error)
                    result = issue.phase.label + (issue.http?.let { " · HTTP $it" }.orEmpty()) +
                        ". " + issue.message()
                } finally { busy = false }
            }
        }) { Text("Enviar amostra") } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancelar") } })
}
