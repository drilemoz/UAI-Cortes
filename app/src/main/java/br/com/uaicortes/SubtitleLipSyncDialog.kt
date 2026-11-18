package br.com.uaicortes

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun SubtitleLipSyncDialog(project: VideoProject, candidates: List<SubtitleCue>,
    onDismiss: () -> Unit, embedded: Boolean = false, onApply: (List<SubtitleLipSyncPolicy.Change>) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentials = remember(context) { GeminiCredentialStore(context) }
    val client = remember(context) { GeminiSubtitleLipSync(context) }
    val recovery = rememberGeminiRecovery()
    val modelLookup = remember { GeminiVideoCaptionClient() }
    val tasks = remember(project, candidates) { SubtitleLipSyncPolicy.tasks(project, candidates) }
    var configured by remember { mutableStateOf<Boolean?>(null) }
    var picked by remember(tasks) { mutableStateOf(tasks.firstOrNull()?.let { setOf(it.id) }.orEmpty()) }
    var accepted by remember { mutableStateOf(emptySet<String>()) }
    var changes by remember { mutableStateOf<List<SubtitleLipSyncPolicy.Change>?>(null) }
    var images by remember { mutableStateOf<List<SubtitleLipFrames>>(emptyList()) }
    var compareId by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var job by remember { mutableStateOf<Job?>(null) }
    var message by remember { mutableStateOf("") }
    LaunchedEffect(credentials) {
        configured = withContext(Dispatchers.IO) { runCatching { credentials.read() != null }.getOrDefault(false) }
    }
    DisposableEffect(client, modelLookup) {
        onDispose { job?.cancel(); client.cancel(); modelLookup.cancel() }
    }
    fun cancelOrClose() {
        if (busy) {
            job?.cancel()
            client.cancel()
            modelLookup.cancel()
        } else onDismiss()
    }
    fun analyze() {
        if (busy || configured != true || picked.isEmpty()) return
        val selected = tasks.filter { it.id in picked }
        busy = true
        message = "Preparando análise visual…"
        job = scope.launch {
            try {
                val key = withContext(Dispatchers.IO) { credentials.read() }
                if (key == null) {
                    configured = false
                    message = "Configure a chave Gemini para usar a análise visual."
                    return@launch
                }
                val decoded = SubtitleLipSyncFrames.extract(context, selected) { i, total ->
                    withContext(Dispatchers.Main.immediate) { message = "Extraindo quadros · $i de $total" }
                }
                if (decoded.isEmpty()) {
                    changes = emptyList()
                    message = "Não encontrei quadros utilizáveis. Nenhuma imagem foi enviada."
                    return@launch
                }
                message = "Consultando Gemini…"
                val preferred = context.getSharedPreferences("caption_model_preferences", Context.MODE_PRIVATE)
                    .getString("model", null)?.takeIf(GeminiCaptionModelPolicy::validId)
                val model = preferred ?: modelLookup.listModels(key).let { available ->
                    (available.lastOrNull { it.id.endsWith("-flash") } ?: available.first()).id
                }
                val report = recovery.track { client.analyze(key, model, decoded) }
                val revisions = SubtitleLipSyncPolicy.proposals(decoded.map { it.task }, report, project.subtitles,
                    project.durationMs)
                images = if (revisions.isEmpty()) emptyList() else decoded
                changes = revisions
                accepted = revisions.map { it.before.id }.toSet()
                val untouched = selected.size - revisions.size
                message = if (revisions.isEmpty()) "Nenhum ajuste confiável. Os horários atuais foram mantidos."
                    else "${revisions.size} com sugestão · $untouched sem alteração"
            } catch (cancel: CancellationException) {
                message = "Análise cancelada. Nenhuma legenda foi alterada."
                throw cancel
            } catch (error: GeminiHttpException) {
                message = error.message.orEmpty() + " Os horários atuais foram mantidos."
            } catch (_: Exception) {
                message = "Não foi possível concluir a análise visual. Os horários atuais foram mantidos."
            } finally { busy = false }
        }
    }

    val body: @Composable () -> Unit = {
            Column(Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (changes == null) {
                    Text("Compara a boca nos quadros próximos ao início e ao fim da legenda. " +
                        "Os horários de cada palavra do karaokê ficam intactos.")
                    Text("Ao analisar, somente esses quadros serão enviados ao Gemini. Sem áudio, " +
                        "texto da legenda ou vídeo completo. Se o modelo estiver indisponível, " +
                        "o app pode tentar outro compatível. Pode usar cota ou gerar cobrança.",
                        style = MaterialTheme.typography.bodySmall)
                    if (configured == false) Text("Sem chave Gemini. Abra Ajustes e configure Gemini em Integrações e APIs.",
                        color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    if (tasks.isEmpty()) Text("Não há janelas visuais suficientes nestas legendas. " +
                        "O recurso usa a faixa principal e evita imagens fixas, PiP e transições.")
                    tasks.forEach { task ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(task.id in picked, onCheckedChange = { checked ->
                                picked = if (checked) picked + task.id else picked - task.id
                            }, enabled = !busy)
                            Column {
                                Text(task.cue.text.take(96), maxLines = 2)
                                Text(formatPipMs(task.cue.startMs), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                if (message.isNotBlank()) Text(message, style = MaterialTheme.typography.bodySmall)
                GeminiRecoveryFeedback(recovery)
                changes.orEmpty().forEach { item ->
                    val id = item.before.id
                    HorizontalDivider()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(id in accepted, onCheckedChange = { checked ->
                            accepted = if (checked) accepted + id else accepted - id
                        })
                        Text(item.before.text.take(96), maxLines = 2)
                    }
                    Text("${lipInterval(item.before)} → ${lipInterval(item.after)}",
                        style = MaterialTheme.typography.labelMedium)
                    TextButton(onClick = { compareId = if (compareId == id) null else id }) {
                        Text(if (compareId == id) "Ocultar quadros" else "Comparar quadros")
                    }
                    if (compareId == id) {
                        val frames = images.firstOrNull { it.task.cue.id == id }
                        if (item.before.startMs != item.after.startMs) LipBoundaryFrames("Início",
                            frames?.task?.start, frames?.start, item.before.startMs, item.after.startMs)
                        if (item.before.endMs != item.after.endMs) LipBoundaryFrames("Fim",
                            frames?.task?.end, frames?.end, item.before.endMs, item.after.endMs)
                    }
                }
            }
    }
    val confirm: @Composable () -> Unit = {
        if (changes == null) TextButton(onClick = ::analyze,
            enabled = !busy && configured == true && picked.isNotEmpty()) { Text("Analisar com Gemini") }
        else TextButton(onClick = { onApply(changes.orEmpty().filter { it.before.id in accepted }) },
            enabled = accepted.isNotEmpty()) { Text("Aplicar selecionadas") }
    }
    val dismiss: @Composable () -> Unit = {
        TextButton(onClick = ::cancelOrClose) { Text(if (busy) "Interromper" else "Voltar à revisão") }
    }
    if (embedded) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Sincronia labial", style = MaterialTheme.typography.titleMedium)
            body()
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { dismiss(); confirm() }
        }
    } else AlertDialog(onDismissRequest = ::cancelOrClose,
        title = { Text("Sincronia labial") }, text = body, confirmButton = confirm, dismissButton = dismiss)

}

private fun lipInterval(cue: SubtitleCue) = "${formatPipMs(cue.startMs)} – ${formatPipMs(cue.endMs)}"

@Composable
private fun LipBoundaryFrames(label: String, window: SubtitleLipSyncPolicy.Window?,
    images: List<ByteArray>?, before: Long, after: Long) {
    if (window == null || images == null) return
    Text(label, style = MaterialTheme.typography.labelLarge)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("Antes" to before, "Sugerido" to after).forEach { (title, at) ->
            val data = images.getOrNull(window.timelineMs.indexOf(at))
            val picture = remember(data) { data?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() } }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$title · ${formatPipMs(at)}", style = MaterialTheme.typography.labelSmall)
                if (picture != null) Image(picture, "$label $title", Modifier.fillMaxWidth().height(96.dp),
                    contentScale = ContentScale.Fit)
            }
        }
    }
}
