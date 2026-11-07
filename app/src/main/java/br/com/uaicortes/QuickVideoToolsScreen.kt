package br.com.uaicortes

import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

internal enum class QuickToolMode { SPLIT_VIDEO, EXTRACT_AUDIO }

/** Tela independente: arquivos originais e projetos nunca são modificados. */
@Composable
internal fun QuickVideoToolsScreen(mode: QuickToolMode, onBack: () -> Unit) {
    val context = LocalContext.current
    val splitEngine = remember(context) { QuickVideoSplitEngine(context.applicationContext) }
    val audioEngine = remember(context) { AudioExtractionEngine(context.applicationContext) }
    DisposableEffect(splitEngine, audioEngine) {
        onDispose { splitEngine.cancel(); audioEngine.cancel() }
    }
    // A galeria integrada é o seletor principal, assim como na importação de imagens.
    // O OpenDocument continua disponível como alternativa pelo botão de arquivos.
    var galleryOpen by rememberSaveable(mode) { mutableStateOf(false) }
    var sourceText by rememberSaveable(mode) { mutableStateOf("") }
    var sourceName by rememberSaveable(mode) { mutableStateOf("") }
    var sourceDurationMs by rememberSaveable(mode) { mutableLongStateOf(0L) }
    var hasAudio by rememberSaveable(mode) { mutableStateOf(false) }
    var partDuration by rememberSaveable { mutableLongStateOf(QuickVideoSplitPolicy.SHORT_MS) }
    var audioMode by rememberSaveable { mutableStateOf("keep") }
    var processing by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var progressIndex by remember { mutableIntStateOf(0) }
    var progressTotal by remember { mutableIntStateOf(0) }
    var completed by remember { mutableIntStateOf(0) }
    var phase by remember { mutableStateOf("") }
    var confirmCancel by remember { mutableStateOf(false) }
    val partsResult = remember(sourceDurationMs, partDuration) {
        runCatching { QuickVideoSplitPolicy.parts(sourceDurationMs, partDuration) }
    }
    val parts = partsResult.getOrNull().orEmpty()
    fun useVideo(uri: Uri, persistDocumentAccess: Boolean) {
        if (processing) return
        // A galeria entrega URIs MediaStore; apenas OpenDocument concede permissão persistível.
        if (persistDocumentAccess) runCatching {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val duration = verifiedMediaDuration(context, uri)
        val name = runCatching { mediaName(context, uri) }.getOrDefault("Vídeo")
        val containsAudio = runCatching {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO) == "yes"
            } finally { retriever.release() }
        }.getOrDefault(false)
        if (duration <= 0L) {
            error = "Não foi possível ler a duração do vídeo selecionado. Escolha outro arquivo."
        } else {
            sourceText = uri.toString()
            sourceName = name
            sourceDurationMs = duration
            hasAudio = containsAudio
            finished = false; error = ""; completed = 0
            if (!containsAudio && mode == QuickToolMode.SPLIT_VIDEO && audioMode == "separate")
                audioMode = "keep"
        }
        galleryOpen = false
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) useVideo(uri, persistDocumentAccess = true)
    }
    val audioDocument = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("audio/mp4")) { uri ->
        if (uri != null && sourceText.isNotBlank() && !processing) {
            processing = true; error = ""; finished = false; phase = "Extraindo áudio"
            val clip = Clip(uri = sourceText, title = sourceName, sourceDurationMs = sourceDurationMs)
            runCatching {
                audioEngine.start(listOf(clip), uri,
                    onSuccess = { processing = false; finished = true; phase = "M4A salvo no destino escolhido" },
                    onError = { reason -> processing = false; error = reason })
            }.onFailure { processing = false; error = it.message ?: "Não foi possível extrair o áudio." }
        }
    }
    fun leave() {
        if (processing) confirmCancel = true else onBack()
    }
    BackHandler(galleryOpen || processing) {
        if (galleryOpen) galleryOpen = false else confirmCancel = true
    }
    if (confirmCancel) AlertDialog(
        onDismissRequest = { confirmCancel = false },
        title = { Text("Cancelar processamento?") },
        text = { Text("Os arquivos já salvos continuarão na galeria. A parte atual pode ser interrompida.") },
        confirmButton = { TextButton(onClick = {
            splitEngine.cancel(); audioEngine.cancel()
            processing = false; confirmCancel = false
            phase = "Cancelado. Arquivos concluídos foram preservados."
        }) { Text("Cancelar tarefa") } },
        dismissButton = { TextButton(onClick = { confirmCancel = false }) { Text("Continuar") } }
    )
    if (galleryOpen) {
        DeviceMediaBrowser(
            onBack = { galleryOpen = false },
            onSelect = { chosen ->
                chosen.firstOrNull()?.takeUnless { it.isImage }?.let { media ->
                    useVideo(media.uri, persistDocumentAccess = false)
                }
            },
            openDocuments = { picker.launch(arrayOf("video/*")) },
            scope = GalleryScope.VIDEOS,
            allowMultiple = false
        )
    } else Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Heading(if (mode == QuickToolMode.SPLIT_VIDEO) "Dividir vídeo" else "Extrair áudio", ::leave)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(if (mode == QuickToolMode.SPLIT_VIDEO)
                "Crie partes MP4 independentes, sem abrir um projeto."
                else "Salve o áudio de um vídeo em M4A, sem abrir o editor.",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(onClick = { galleryOpen = true },
                enabled = !processing, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp), shape = UaiShapes.button) {
                Icon(Icons.Default.VideoFile, null)
                Spacer(Modifier.width(10.dp))
                Text(if (sourceText.isBlank()) "Selecionar vídeo" else "Trocar vídeo")
            }
            if (sourceText.isNotBlank()) {
                Text(sourceName, fontWeight = FontWeight.SemiBold)
                Text("Duração: ${formatMs(sourceDurationMs)} · ${if (hasAudio) "Com áudio" else "Sem áudio identificado"}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (mode == QuickToolMode.SPLIT_VIDEO) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
                    Text("A divisão rápida com salvamento automático na galeria exige Android 10 ou superior.",
                        color = MaterialTheme.colorScheme.error)
                Text("Duração de cada parte", fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf(QuickVideoSplitPolicy.SHORT_MS to "9,5 s",
                        QuickVideoSplitPolicy.TEN_SECONDS_MS to "10 s").forEach { (value, label) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = partDuration == value,
                                onClick = { if (!processing) partDuration = value }, enabled = !processing)
                            Text(label)
                        }
                    }
                }
                Text("Áudio", fontWeight = FontWeight.SemiBold)
                listOf("keep" to "Manter áudio original",
                    "mute" to "Vídeos sem áudio",
                    "separate" to "MP4 com áudio + M4A de cada parte").forEach { (value, label) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = audioMode == value,
                            onClick = { if (!processing) audioMode = value },
                            enabled = !processing && (value != "separate" ||
                                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && hasAudio)))
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                if (sourceText.isNotBlank()) {
                    if (parts.isNotEmpty()) {
                        Text("${parts.size} parte(s) prevista(s)", fontWeight = FontWeight.SemiBold)
                        parts.take(4).forEach { part ->
                            Text("Parte ${part.index.toString().padStart(3, '0')}: " +
                                "${"%.1f".format(java.util.Locale.ROOT, part.startMs / 1000.0)} a " +
                                "${"%.1f".format(java.util.Locale.ROOT, part.endMs / 1000.0)} s",
                                style = MaterialTheme.typography.bodySmall)
                        }
                        if (parts.size > 4) Text("… até a parte ${parts.size}",
                            style = MaterialTheme.typography.bodySmall)
                    } else Text(partsResult.exceptionOrNull()?.message.orEmpty(),
                        color = MaterialTheme.colorScheme.error)
                }
                Button(onClick = {
                    if (sourceText.isBlank() || parts.isEmpty()) return@Button
                    error = ""; finished = false; completed = 0; progressIndex = 0
                    processing = true
                    runCatching {
                        splitEngine.start(Uri.parse(sourceText), sourceName, parts, audioMode,
                            onProgress = { index, total, step ->
                                progressIndex = index; progressTotal = total; phase = step
                            },
                            onDone = { count ->
                                processing = false; finished = true; completed = count
                                phase = "Partes concluídas e salvas na galeria"
                            },
                            onError = { reason, count ->
                                processing = false; completed = count; error = reason
                            })
                    }.onFailure {
                        processing = false; error = it.message ?: "Não foi possível iniciar a divisão."
                    }
                }, enabled = !processing && sourceText.isNotBlank() && parts.isNotEmpty() &&
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                    (audioMode != "separate" || hasAudio),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp), shape = UaiShapes.button) {
                    Icon(Icons.Default.ContentCut, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Dividir em ${parts.size} partes")
                }
                Text("Destino: Filmes/UaiCortes/Divididos. A opção M4A usa Música/UaiCortes/Divididos.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Button(onClick = {
                    if (sourceText.isNotBlank() && hasAudio) {
                        audioDocument.launch(QuickVideoSplitPolicy.fileBase(sourceName) + ".m4a")
                    }
                }, enabled = !processing && sourceText.isNotBlank() && hasAudio,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp), shape = UaiShapes.button) {
                    Icon(Icons.Default.AudioFile, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Extrair e salvar M4A")
                }
                if (sourceText.isNotBlank() && !hasAudio)
                    Text("O vídeo selecionado não possui faixa de áudio detectável.",
                        color = MaterialTheme.colorScheme.error)
            }
            if (processing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(if (mode == QuickToolMode.SPLIT_VIDEO) "$phase · Parte $progressIndex/$progressTotal" else phase)
                OutlinedButton(onClick = { confirmCancel = true }, shape = UaiShapes.button) {
                    Icon(Icons.Default.Close, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Cancelar")
                }
            }
            if (error.isNotBlank()) Text("Erro: $error" +
                (if (completed > 0) " · $completed parte(s) já concluída(s)." else ""),
                color = MaterialTheme.colorScheme.error)
            if (finished) {
                Text("Concluído", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(if (mode == QuickToolMode.SPLIT_VIDEO)
                    "$completed arquivo(s) MP4 salvos. O vídeo original não foi alterado."
                    else "Áudio M4A extraído no local escolhido.")
            }
        }
    }
}
