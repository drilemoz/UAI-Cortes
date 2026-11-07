package br.com.uaicortes

import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.style.TextOverflow
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

        // Opções roláveis, execução sempre visível. Não criar nem modificar um projeto.
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Surface(
                shape = UaiShapes.card,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.48f)
            ) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = UaiShapes.card, color = MaterialTheme.colorScheme.primary) {
                        Icon(
                            if (mode == QuickToolMode.SPLIT_VIDEO) Icons.Default.ContentCut else Icons.Default.GraphicEq,
                            contentDescription = null, modifier = Modifier.padding(12.dp).size(24.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            if (mode == QuickToolMode.SPLIT_VIDEO) "Cortes rápidos em MP4" else "Áudio direto do vídeo",
                            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            if (mode == QuickToolMode.SPLIT_VIDEO)
                                "Divida em partes independentes, sem abrir o editor."
                            else "Extraia a faixa de áudio em M4A, sem abrir um projeto.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                QuickToolStep(1, "Vídeo de origem")
                Surface(
                    onClick = { galleryOpen = true }, enabled = !processing,
                    modifier = Modifier.fillMaxWidth(), shape = UaiShapes.card,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 76.dp).padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(shape = UaiShapes.item, color = MaterialTheme.colorScheme.secondaryContainer) {
                            Icon(Icons.Default.VideoFile, contentDescription = null,
                                modifier = Modifier.padding(10.dp).size(24.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(if (sourceText.isBlank()) "Selecionar vídeo" else sourceName,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(
                                if (sourceText.isBlank()) "Toque para escolher na galeria"
                                else "${formatMs(sourceDurationMs)} · ${if (hasAudio) "Com áudio" else "Sem áudio detectado"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                        Icon(if (sourceText.isBlank()) Icons.Default.AddCircleOutline else Icons.Default.SwapHoriz,
                            contentDescription = if (sourceText.isBlank()) "Selecionar vídeo" else "Trocar vídeo",
                            tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            if (mode == QuickToolMode.SPLIT_VIDEO) {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    QuickToolStep(2, "Duração das partes")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf(QuickVideoSplitPolicy.SHORT_MS to "9,5 s",
                            QuickVideoSplitPolicy.TEN_SECONDS_MS to "10 s").forEach { (value, label) ->
                            val selected = partDuration == value
                            Surface(
                                onClick = { partDuration = value }, enabled = !processing,
                                modifier = Modifier.weight(1f).heightIn(min = 64.dp),
                                shape = UaiShapes.card,
                                color = if (selected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(if (selected) 2.dp else 1.dp,
                                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 14.dp),
                                    horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                    Icon(if (selected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = null,
                                        tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(7.dp))
                                    Text(label, style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                        maxLines = 1)
                                }
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    QuickToolStep(3, "Como salvar o áudio")
                    QuickToolChoice(
                        icon = Icons.Default.VolumeUp, title = "Manter áudio original",
                        subtitle = "Os MP4 mantêm o som do vídeo",
                        selected = audioMode == "keep", enabled = !processing
                    ) { audioMode = "keep" }
                    QuickToolChoice(
                        icon = Icons.Default.VolumeOff, title = "Vídeos sem áudio",
                        subtitle = "Gera somente as imagens dos cortes",
                        selected = audioMode == "mute", enabled = !processing
                    ) { audioMode = "mute" }
                    QuickToolChoice(
                        icon = Icons.Default.LibraryMusic, title = "MP4 + áudio separado",
                        subtitle = if (sourceText.isBlank()) "Selecione um vídeo com áudio para habilitar"
                            else if (!hasAudio) "Vídeo sem faixa de áudio detectada"
                            else "Cria também um M4A para cada parte",
                        selected = audioMode == "separate",
                        enabled = !processing && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && hasAudio
                    ) { audioMode = "separate" }
                }

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    QuickToolNotice("O salvamento automático dos cortes requer Android 10 ou superior.", isError = true)
                }
                if (sourceText.isNotBlank()) {
                    val previewExpanded = rememberSaveable(sourceText, partDuration) { mutableStateOf(false) }
                    Surface(shape = UaiShapes.card, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)) {
                        Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.FormatListNumbered, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(9.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("Prévia da divisão", fontWeight = FontWeight.SemiBold)
                                    Text(if (parts.isNotEmpty()) "${parts.size} partes previstas · original preservado"
                                        else "Não foi possível calcular os cortes",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodySmall)
                                }
                                if (parts.isNotEmpty()) {
                                    IconButton(onClick = { previewExpanded.value = !previewExpanded.value },
                                        enabled = !processing) {
                                        Icon(if (previewExpanded.value) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = if (previewExpanded.value) "Recolher prévia" else "Ver prévia")
                                    }
                                }
                            }
                            if (parts.isEmpty()) {
                                Text(partsResult.exceptionOrNull()?.message.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error)
                            } else if (previewExpanded.value) {
                                parts.take(5).forEach { part ->
                                    Text("Parte ${part.index.toString().padStart(3, '0')}  ·  " +
                                        "${"%.1f".format(java.util.Locale.ROOT, part.startMs / 1000.0)}–" +
                                        "${"%.1f".format(java.util.Locale.ROOT, part.endMs / 1000.0)} s",
                                        style = MaterialTheme.typography.bodySmall)
                                }
                                if (parts.size > 5) Text("… e mais ${parts.size - 5} partes",
                                    style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            } else if (sourceText.isNotBlank() && !hasAudio) {
                QuickToolNotice("O vídeo selecionado não possui faixa de áudio detectável.", isError = true)
            }
        }

        Surface(tonalElevation = 3.dp, color = MaterialTheme.colorScheme.surface) {
            Column(
                Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 18.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (processing) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text(if (mode == QuickToolMode.SPLIT_VIDEO) "$phase · Parte $progressIndex/$progressTotal" else phase,
                        style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(onClick = { confirmCancel = true }, modifier = Modifier.fillMaxWidth(),
                        shape = UaiShapes.button) {
                        Icon(Icons.Default.Close, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Cancelar tarefa")
                    }
                }
                if (error.isNotBlank()) {
                    QuickToolNotice("Erro: $error" +
                        (if (completed > 0) " · $completed parte(s) concluída(s)." else ""), isError = true)
                }
                if (finished) {
                    QuickToolNotice(if (mode == QuickToolMode.SPLIT_VIDEO)
                        "$completed MP4 salvo(s). O vídeo original não foi alterado."
                        else "Áudio M4A salvo no local escolhido.", isError = false)
                }
                if (mode == QuickToolMode.SPLIT_VIDEO) {
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
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp), shape = UaiShapes.button) {
                        Icon(Icons.Default.ContentCut, contentDescription = null)
                        Spacer(Modifier.width(9.dp))
                        Text(if (sourceText.isBlank()) "Selecione um vídeo para dividir"
                            else "Dividir em ${parts.size} partes", fontWeight = FontWeight.SemiBold)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FolderOpen, null, modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(6.dp))
                        Text("MP4: Filmes/UaiCortes/Divididos · M4A: Música/UaiCortes/Divididos",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Button(onClick = {
                        if (sourceText.isNotBlank() && hasAudio) {
                            audioDocument.launch(QuickVideoSplitPolicy.fileBase(sourceName) + ".m4a")
                        }
                    }, enabled = !processing && sourceText.isNotBlank() && hasAudio,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp), shape = UaiShapes.button) {
                        Icon(Icons.Default.AudioFile, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Extrair e salvar M4A", fontWeight = FontWeight.SemiBold)
                    }
                    Text("Escolha onde salvar o arquivo M4A após selecionar o vídeo.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

/** Numeração visual de etapas, sem alterar o fluxo do processamento. */
@Composable
private fun QuickToolStep(number: Int, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = UaiShapes.chip, color = MaterialTheme.colorScheme.primaryContainer) {
            Text(number.toString(), modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Spacer(Modifier.width(9.dp))
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    }
}

/** O cartão inteiro é acionável; não há RadioButton com área de toque pequena. */
@Composable
private fun QuickToolChoice(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth(),
        shape = UaiShapes.card,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
            else MaterialTheme.colorScheme.surface,
        border = BorderStroke(if (selected) 2.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(Modifier.fillMaxWidth().heightIn(min = 63.dp).padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp),
                tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(8.dp))
            Icon(if (selected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                contentDescription = null, modifier = Modifier.size(22.dp),
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun QuickToolNotice(message: String, isError: Boolean) {
    val colors = MaterialTheme.colorScheme
    Surface(shape = UaiShapes.item,
        color = if (isError) colors.errorContainer else colors.primaryContainer) {
        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (isError) Icons.Default.ErrorOutline else Icons.Default.CheckCircle,
                contentDescription = null, modifier = Modifier.size(20.dp),
                tint = if (isError) colors.onErrorContainer else colors.onPrimaryContainer)
            Spacer(Modifier.width(9.dp))
            Text(message, style = MaterialTheme.typography.bodySmall,
                color = if (isError) colors.onErrorContainer else colors.onPrimaryContainer)
        }
    }
}
