package br.com.uaicortes

import android.content.ClipData
import android.content.ClipboardManager

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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

/** Uma chamada textual explícita, seguida de revisão. Nunca grava a resposta automaticamente. */
@Composable
internal fun SubtitleProofreadDialog(cues: List<SubtitleCue>, total: Int,
    close: () -> Unit, apply: (List<SubtitleProofreadPolicy.Change>) -> Boolean) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val client = remember(context) { GeminiVideoCaptionClient(context) }
    val credentials = remember(context) { GeminiCredentialStore(context) }
    val hasKey = remember(credentials) { credentials.hasKey() }
    var running by remember { mutableStateOf(false) }
    var issue by remember { mutableStateOf<String?>(null) }
    var failureHttp by remember { mutableStateOf<Int?>(null) }
    var model by remember { mutableStateOf(SubtitleProofreadPolicy.Model.FLASH) }
    var copied by remember { mutableStateOf(false) }
    var changes by remember { mutableStateOf<List<SubtitleProofreadPolicy.Change>?>(null) }
    var selected by remember { mutableStateOf(emptySet<String>()) }
    DisposableEffect(client) { onDispose { client.cancel() } }

    fun request(using: SubtitleProofreadPolicy.Model) {
        if (running || !hasKey || cues.isEmpty()) return
        issue = null
        failureHttp = null
        copied = false
        model = using
        running = true
        scope.launch {
            var phase = SubtitleCloudPhase.KEY
            try {
                val key = withContext(Dispatchers.IO) { credentials.read() } ?: error("Chave ausente")
                phase = SubtitleCloudPhase.PROOFREAD
                val answer = client.generateStudioJson(key, SubtitleProofreadPolicy.prompt(cues),
                    using.id, "subtitle_proofread")
                val found = try { SubtitleProofreadPolicy.parse(cues, answer) }
                catch (_: Exception) { throw SubtitleCloudException(
                    SubtitleCloudIssue(phase, SubtitleCloudReason.INVALID_RESULT)) }
                changes = found
                selected = found.map { it.before.id }.toSet()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                val info = SubtitleCloudIssue.from(phase, failure)
                DiagnosticRecorder.subtitleCloudFailure("proofread", info)
                failureHttp = info.http
                issue = (info.http?.let { "HTTP $it · " }.orEmpty()) + info.message()
            } finally { running = false }
        }
    }
    AlertDialog(onDismissRequest = close, shape = UaiShapes.dialog,
        title = { Text("Corrigir português") },
        text = { Column(Modifier.heightIn(max = 430.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(9.dp)) {
            val suggested = changes
            when {
                running -> {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text("Revisando o texto das legendas…")
                }
                suggested == null -> {
                    Text("Envia somente o texto de ${cues.size} legenda(s) ao Gemini. Não envia o áudio nem o vídeo. Pode consumir cota.")
                    if (cues.size < total) Text("Revise este bloco e selecione outra legenda para continuar.",
                        style = MaterialTheme.typography.bodySmall)
                    if (!hasKey) Text("Configure a chave Gemini em Integrações e APIs.",
                        color = MaterialTheme.colorScheme.error)
                }
                suggested.isEmpty() -> Text("Nenhuma correção encontrada para este bloco.")
                else -> {
                    Text("Escolha o que aplicar. Os tempos das legendas permanecem.")
                    suggested.forEach { change ->
                        Row(verticalAlignment = Alignment.Top) {
                            val id = change.before.id
                            Checkbox(id in selected, { checked ->
                                selected = if (checked) selected + id else selected - id
                            })
                            Column(Modifier.weight(1f)) {
                                Text("Antes · ${change.before.text}", style = MaterialTheme.typography.bodySmall)
                                Text(change.after.text, style = MaterialTheme.typography.bodyMedium)
                                if (change.before.wordTimings.isNotEmpty() &&
                                    change.before.wordTimings != change.after.wordTimings)
                                    Text("O tempo das palavras modificadas foi reestimado.",
                                        style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
            issue?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (!running && suggested == null && failureHttp == 503) {
                Text("Modelo usado: Gemini ${model.label}. Nenhuma legenda foi alterada.",
                    style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = { request(model.alternative) }, enabled = hasKey) {
                    Text("Tentar com ${model.alternative.label}")
                }
            }
            if (!running && suggested == null && issue != null && cues.isNotEmpty()) {
                TextButton(onClick = {
                    val clipboard = context.getSystemService(ClipboardManager::class.java)
                    clipboard?.setPrimaryClip(ClipData.newPlainText("Legendas com horários", SubtitleFileExport.render(cues)))
                    copied = clipboard != null
                }) { Text(if (copied) "Copiado com horários" else "Copiar texto com horários") }
            }
        } },
        confirmButton = {
            when {
                changes == null -> TextButton(onClick = { request(model) }, enabled = !running && hasKey && cues.isNotEmpty()) {
                    Text(if (issue == null) "Revisar com Gemini" else "Tentar novamente")
                }
                changes.orEmpty().isEmpty() -> TextButton(onClick = close) { Text("Entendi") }
                else -> TextButton(onClick = {
                    if (apply(changes.orEmpty().filter { it.before.id in selected })) close()
                    else issue = "Não há revisão aplicável. As legendas podem ter mudado; revise novamente."
                }, enabled = selected.isNotEmpty()) { Text("Aplicar selecionadas") }
            }
        },
        dismissButton = { TextButton(onClick = close) { Text(if (running) "Cancelar" else "Fechar") } })
}
