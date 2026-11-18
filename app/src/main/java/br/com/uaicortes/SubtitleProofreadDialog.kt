package br.com.uaicortes

import android.content.ClipData
import android.content.ClipboardManager

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
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
    var help by remember { mutableStateOf(false) }
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
        title = {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Corrigir português", Modifier.weight(1f))
                IconButton(onClick = { help = !help }) { Icon(Icons.Default.Info, "Sobre a revisão") }
            }
        },
        text = { Column(Modifier.heightIn(max = 430.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(9.dp)) {
            val suggested = changes
            if (help) Text("O Gemini não recebe vídeo nem áudio. Você confere as sugestões antes de aplicar. " +
                "Os horários de início e fim das legendas são mantidos.",
                style = MaterialTheme.typography.bodySmall)
            when {
                running -> {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text("Revisando o texto das legendas…")
                }
                suggested == null -> {
                    Text("Só o texto de ${cues.size} legenda(s) vai ao Gemini. Pode consumir cota.")
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
            issue?.let { message ->
                Surface(shape = UaiShapes.dialog, color = MaterialTheme.colorScheme.errorContainer) {
                    Column(Modifier.fillMaxWidth().padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(message, color = MaterialTheme.colorScheme.onErrorContainer)
                        if (suggested == null) Text("Gemini ${model.label} · Nenhuma legenda foi alterada.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
            if (!running && suggested == null && failureHttp == 503) {
                OutlinedButton(onClick = { request(model.alternative) }, enabled = hasKey,
                    modifier = Modifier.fillMaxWidth()) {
                    Text("Tentar com ${model.alternative.label}")
                }
            }
            if (!running && suggested == null && issue != null && cues.isNotEmpty()) {
                OutlinedButton(onClick = {
                    val clipboard = context.getSystemService(ClipboardManager::class.java)
                    clipboard?.setPrimaryClip(ClipData.newPlainText("Legendas com horários", SubtitleFileExport.render(cues)))
                    copied = clipboard != null
                }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (copied) "Copiado com horários" else "Copiar texto com horários")
                }
            }
        } },
        confirmButton = {
            when {
                changes == null -> Button(onClick = { request(model) }, enabled = !running && hasKey && cues.isNotEmpty()) {
                    Text(if (issue == null) "Revisar com Gemini" else "Tentar novamente")
                }
                changes.orEmpty().isEmpty() -> Button(onClick = close) { Text("Entendi") }
                else -> Button(onClick = {
                    if (apply(changes.orEmpty().filter { it.before.id in selected })) close()
                    else issue = "Não há revisão aplicável. As legendas podem ter mudado; revise novamente."
                }, enabled = selected.isNotEmpty()) { Text("Aplicar selecionadas") }
            }
        },
        dismissButton = {
            if (changes?.isEmpty() != true)
                OutlinedButton(onClick = close) { Text(if (running) "Cancelar" else "Fechar") }
        })
}
