package br.com.uaicortes

import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

internal enum class QuickToolMode { SPLIT_VIDEO, EXTRACT_AUDIO }
private enum class SplitView { SETUP, RUNNING, RESULT }
private enum class DurationGroup { SECONDS, MINUTES, CUSTOM }
private data class QuickDurationPreset(val label: String, val hint: String, val milliseconds: Long)

private val secondsPresets = listOf(
    QuickDurationPreset("9,5 s", "Clipes rápidos", QuickVideoSplitPolicy.SHORT_MS),
    QuickDurationPreset("10 s", "Clipes rápidos", QuickVideoSplitPolicy.TEN_SECONDS_MS),
    QuickDurationPreset("30 s", "Reels e Shorts", QuickVideoSplitPolicy.THIRTY_SECONDS_MS),
    QuickDurationPreset("60 s", "Vídeos curtos", QuickVideoSplitPolicy.ONE_MINUTE_MS)
)
private val minutesPresets = listOf(
    QuickDurationPreset("1 min", "Clipes de um minuto", QuickVideoSplitPolicy.ONE_MINUTE_MS),
    QuickDurationPreset("1 min 05 s", "TikTok: acima de 1 min", QuickVideoSplitPolicy.ONE_MINUTE_FIVE_MS),
    QuickDurationPreset("2 min", "Tutoriais curtos", QuickVideoSplitPolicy.TWO_MINUTES_MS),
    QuickDurationPreset("2 min 59 s", "Shorts: margem até 3 min", QuickVideoSplitPolicy.SHORTS_MARGIN_MS),
    QuickDurationPreset("3 min", "Vídeos de até 3 minutos", QuickVideoSplitPolicy.THREE_MINUTES_MS),
    QuickDurationPreset("4 min", "YouTube tradicional", QuickVideoSplitPolicy.FOUR_MINUTES_MS),
    QuickDurationPreset("8 min", "YouTube: anúncios intermediários*", QuickVideoSplitPolicy.EIGHT_MINUTES_MS)
)

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
    var lastSecondsDuration by rememberSaveable { mutableLongStateOf(QuickVideoSplitPolicy.SHORT_MS) }
    var lastMinutesDuration by rememberSaveable { mutableLongStateOf(QuickVideoSplitPolicy.ONE_MINUTE_MS) }
    var durationGroup by rememberSaveable { mutableStateOf(DurationGroup.SECONDS) }
    var customHours by rememberSaveable { mutableStateOf("0") }
    var customMinutes by rememberSaveable { mutableStateOf("1") }
    var customSeconds by rememberSaveable { mutableStateOf("0") }
    val customMs = QuickVideoSplitPolicy.customDurationMs(
        customHours.toIntOrNull(), customMinutes.toIntOrNull(), customSeconds.toIntOrNull()
    )
    val effectivePartDurationMs = if (durationGroup == DurationGroup.CUSTOM) customMs ?: 0L else partDuration
    var audioMode by rememberSaveable { mutableStateOf("keep") }
    var processing by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var progressIndex by remember { mutableIntStateOf(0) }
    var progressTotal by remember { mutableIntStateOf(0) }
    var completed by remember { mutableIntStateOf(0) }
    var phase by remember { mutableStateOf("") }
    var confirmCancel by remember { mutableStateOf(false) }
    var splitView by remember { mutableStateOf(SplitView.SETUP) }
    val savedSplitFiles = remember { mutableStateListOf<QuickSplitSavedFile>() }
    var openFileError by remember { mutableStateOf("") }
    var extractedAudioUri by remember { mutableStateOf<Uri?>(null) }
    var extractedAudioName by remember { mutableStateOf("") }
    var incompleteAudioUri by remember { mutableStateOf<Uri?>(null) }
    var deleteIncompleteConfirm by remember { mutableStateOf(false) }
    var deletingIncomplete by remember { mutableStateOf(false) }
    val partsResult = remember(sourceDurationMs, effectivePartDurationMs, durationGroup) {
        runCatching {
            if (durationGroup == DurationGroup.CUSTOM && customMs == null) {
                throw IllegalArgumentException("Use de 00:00:01 até 23:59:59, com minutos e segundos entre 0 e 59.")
            }
            QuickVideoSplitPolicy.parts(sourceDurationMs, effectivePartDurationMs)
        }
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
            splitView = SplitView.SETUP
            savedSplitFiles.clear()
            extractedAudioUri = null; extractedAudioName = ""; openFileError = ""
            incompleteAudioUri = null; deleteIncompleteConfirm = false; deletingIncomplete = false
            if (!containsAudio && mode == QuickToolMode.SPLIT_VIDEO && audioMode == "separate")
                audioMode = "keep"
        }
        galleryOpen = false
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) useVideo(uri, persistDocumentAccess = true)
    }
    val audioDocument = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("audio/mp4")) { uri ->
        // O SAF pode ser cancelado pelo usuário: só entrar no fluxo após a URI existir.
        if (uri != null && sourceText.isNotBlank() && hasAudio && !processing) {
            processing = true; error = ""; finished = false; phase = "Preparando extração"
            extractedAudioUri = null; openFileError = ""
            incompleteAudioUri = uri; deleteIncompleteConfirm = false; deletingIncomplete = false
            extractedAudioName = runCatching { mediaName(context, uri) }
                .getOrDefault(QuickVideoSplitPolicy.fileBase(sourceName) + ".m4a")
            splitView = SplitView.RUNNING
            val clip = Clip(uri = sourceText, title = sourceName, sourceDurationMs = sourceDurationMs)
            runCatching {
                audioEngine.start(listOf(clip), uri,
                    onSuccess = {
                        if (processing && splitView == SplitView.RUNNING) {
                            processing = false; finished = true
                            extractedAudioUri = uri; incompleteAudioUri = null
                            phase = "M4A salvo no destino escolhido"
                            splitView = SplitView.RESULT
                        }
                    },
                    onError = { reason ->
                        if (processing && splitView == SplitView.RUNNING) {
                            processing = false; error = reason; splitView = SplitView.RESULT
                        }
                    },
                    onPhase = { step ->
                        if (processing && splitView == SplitView.RUNNING) phase = step
                    })
            }.onFailure {
                audioEngine.cancel()
                processing = false; error = it.message ?: "Não foi possível extrair o áudio."
                splitView = SplitView.RESULT
            }
        }
    }
    fun leave() {
        if (processing) confirmCancel = true else onBack()
    }
    BackHandler(galleryOpen || processing || splitView == SplitView.RESULT) {
        when {
            galleryOpen -> galleryOpen = false
            processing -> confirmCancel = true
            deletingIncomplete -> Unit
            splitView == SplitView.RESULT -> splitView = SplitView.SETUP
        }
    }
    if (confirmCancel) AlertDialog(
        onDismissRequest = { confirmCancel = false },
        title = { Text("Cancelar processamento?") },
        text = { Text(if (mode == QuickToolMode.SPLIT_VIDEO)
            "Os arquivos já salvos continuarão na galeria. A parte atual pode ser interrompida."
            else "A extração será interrompida. Se o cancelamento ocorrer durante o salvamento, pode permanecer um M4A incompleto no destino escolhido.") },
        confirmButton = { TextButton(onClick = {
            splitEngine.cancel(); audioEngine.cancel()
            processing = false; confirmCancel = false
            phase = "Cancelado. Arquivos já publicados continuam no armazenamento."
            error = if (mode == QuickToolMode.SPLIT_VIDEO) "Divisão cancelada pelo usuário."
                else "Extração cancelada pelo usuário."
            splitView = SplitView.RESULT
        }) { Text("Cancelar tarefa") } },
        dismissButton = { TextButton(onClick = { confirmCancel = false }) { Text("Continuar") } }
    )
    if (deleteIncompleteConfirm) AlertDialog(
        onDismissRequest = { if (!deletingIncomplete) deleteIncompleteConfirm = false },
        title = { Text("Excluir M4A incompleto?") },
        text = { Text("O arquivo criado para esta extração será excluído do local escolhido. Essa ação não afeta o vídeo original nem outros áudios.") },
        confirmButton = { TextButton(enabled = !deletingIncomplete, onClick = {
            val candidate = incompleteAudioUri
            if (candidate != null) {
                deletingIncomplete = true
                audioEngine.removeIncompleteDestination(candidate) { removed, detail ->
                    deletingIncomplete = false; deleteIncompleteConfirm = false
                    if (removed) {
                        incompleteAudioUri = null
                        openFileError = "O M4A incompleto foi excluído."
                    } else {
                        openFileError = if (detail?.startsWith("A gravação ainda") == true) detail
                            else "O Android não permitiu excluir o M4A. Remova-o no app Arquivos."
                    }
                }
            }
        }) { Text(if (deletingIncomplete) "Excluindo…" else "Excluir arquivo") } },
        dismissButton = { TextButton(enabled = !deletingIncomplete,
            onClick = { deleteIncompleteConfirm = false }) { Text("Manter arquivo") } }
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
    } else if (mode == QuickToolMode.EXTRACT_AUDIO && splitView == SplitView.RUNNING) {
        QuickAudioProgressScreen(
            sourceName = sourceName,
            durationMs = sourceDurationMs,
            phase = phase,
            onCancel = { confirmCancel = true }
        )
    } else if (mode == QuickToolMode.EXTRACT_AUDIO && splitView == SplitView.RESULT) {
        QuickAudioResultScreen(
            sourceName = sourceName,
            audioName = extractedAudioName,
            savedUri = extractedAudioUri,
            issue = error,
            fileError = openFileError,
            incompleteUri = incompleteAudioUri,
            deletingIncomplete = deletingIncomplete,
            onRemoveIncomplete = { deleteIncompleteConfirm = true },
            onOpen = { uri ->
                openFileError = ""
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "audio/mp4")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    })
                }.onFailure { openFileError = "Não foi possível abrir o M4A com os aplicativos instalados." }
            },
            onNew = {
                splitView = SplitView.SETUP; error = ""; finished = false
                phase = ""; extractedAudioUri = null; extractedAudioName = ""; openFileError = ""
                incompleteAudioUri = null; deleteIncompleteConfirm = false; deletingIncomplete = false
            },
            onFinish = { if (!deletingIncomplete) onBack() }
        )
    } else if (mode == QuickToolMode.SPLIT_VIDEO && splitView == SplitView.RUNNING) {
        QuickSplitProgressScreen(
            sourceName = sourceName,
            partIndex = progressIndex,
            totalParts = progressTotal,
            completedParts = completed,
            phase = phase,
            onCancel = { confirmCancel = true }
        )
    } else if (mode == QuickToolMode.SPLIT_VIDEO && splitView == SplitView.RESULT) {
        QuickSplitResultScreen(
            totalParts = parts.size,
            files = savedSplitFiles.toList(),
            issue = error,
            fileError = openFileError,
            onOpen = { file ->
                openFileError = ""
                runCatching {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(file.uri, if (file.isAudio) "audio/mp4" else "video/mp4")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(intent)
                }.onFailure { openFileError = "Não foi possível abrir este arquivo com os aplicativos instalados." }
            },
            onNew = {
                splitView = SplitView.SETUP; error = ""; finished = false
                completed = 0; progressIndex = 0; progressTotal = 0; phase = ""
                openFileError = ""; savedSplitFiles.clear()
            },
            onFinish = onBack
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
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            if (mode == QuickToolMode.SPLIT_VIDEO) "Divisão flexível em MP4" else "Áudio direto do vídeo",
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
                    // Três grupos, sem esconder as opções de 9,5/10 segundos anteriores.
                    BoxWithConstraints(Modifier.fillMaxWidth()) {
                        val stackChoices = maxWidth < 330.dp || LocalDensity.current.fontScale >= 1.3f
                        if (stackChoices) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                QuickDurationGroupChip("Segundos", durationGroup == DurationGroup.SECONDS, !processing,
                                    Modifier.fillMaxWidth()) {
                                    durationGroup = DurationGroup.SECONDS; partDuration = lastSecondsDuration
                                }
                                QuickDurationGroupChip("Minutos", durationGroup == DurationGroup.MINUTES, !processing,
                                    Modifier.fillMaxWidth()) {
                                    durationGroup = DurationGroup.MINUTES; partDuration = lastMinutesDuration
                                }
                                QuickDurationGroupChip("Personalizado", durationGroup == DurationGroup.CUSTOM, !processing,
                                    Modifier.fillMaxWidth()) { durationGroup = DurationGroup.CUSTOM }
                            }
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                QuickDurationGroupChip("Segundos", durationGroup == DurationGroup.SECONDS, !processing,
                                    Modifier.weight(1f)) {
                                    durationGroup = DurationGroup.SECONDS; partDuration = lastSecondsDuration
                                }
                                QuickDurationGroupChip("Minutos", durationGroup == DurationGroup.MINUTES, !processing,
                                    Modifier.weight(1f)) {
                                    durationGroup = DurationGroup.MINUTES; partDuration = lastMinutesDuration
                                }
                                QuickDurationGroupChip("Personalizado", durationGroup == DurationGroup.CUSTOM, !processing,
                                    Modifier.weight(1f)) { durationGroup = DurationGroup.CUSTOM }
                            }
                        }
                    }
                    if (durationGroup == DurationGroup.CUSTOM) {
                        Text("Defina horas, minutos e segundos", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        BoxWithConstraints(Modifier.fillMaxWidth()) {
                            val stackCustom = maxWidth < 330.dp || LocalDensity.current.fontScale >= 1.3f
                            if (stackCustom) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    QuickCustomTimeField("Horas", customHours, !processing,
                                        Modifier.fillMaxWidth()) { customHours = it }
                                    QuickCustomTimeField("Min", customMinutes, !processing,
                                        Modifier.fillMaxWidth()) { customMinutes = it }
                                    QuickCustomTimeField("Seg", customSeconds, !processing,
                                        Modifier.fillMaxWidth()) { customSeconds = it }
                                }
                            } else {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    QuickCustomTimeField("Horas", customHours, !processing,
                                        Modifier.weight(1f)) { customHours = it }
                                    QuickCustomTimeField("Min", customMinutes, !processing,
                                        Modifier.weight(1f)) { customMinutes = it }
                                    QuickCustomTimeField("Seg", customSeconds, !processing,
                                        Modifier.weight(1f)) { customSeconds = it }
                                }
                            }
                        }
                        if (customMs == null) {
                            QuickToolNotice("Use 00:00:01 a 23:59:59; minutos e segundos de 0 a 59.", isError = true)
                        } else {
                            Text("Cada parte: ${QuickVideoSplitPolicy.timeLabel(customMs)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        val options = if (durationGroup == DurationGroup.SECONDS) secondsPresets else minutesPresets
                        BoxWithConstraints(Modifier.fillMaxWidth()) {
                            val stackChoices = maxWidth < 330.dp || LocalDensity.current.fontScale >= 1.3f
                            if (stackChoices) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    options.forEach { option ->
                                        QuickToolDurationChoice(option.label, partDuration == option.milliseconds,
                                            !processing, modifier = Modifier.fillMaxWidth(), subtitle = option.hint) {
                                            partDuration = option.milliseconds
                                            if (durationGroup == DurationGroup.SECONDS) lastSecondsDuration = partDuration
                                            else lastMinutesDuration = partDuration
                                        }
                                    }
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    options.chunked(2).forEach { pair ->
                                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            pair.forEach { option ->
                                                QuickToolDurationChoice(option.label, partDuration == option.milliseconds,
                                                    !processing, modifier = Modifier.weight(1f), subtitle = option.hint) {
                                                    partDuration = option.milliseconds
                                                    if (durationGroup == DurationGroup.SECONDS) lastSecondsDuration = partDuration
                                                    else lastMinutesDuration = partDuration
                                                }
                                            }
                                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Text("*8 minutos permite anúncios intermediários no YouTube para canais/vídeos elegíveis; " +
                        "a duração, sozinha, não garante monetização ou alcance.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)

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
                    val previewExpanded = rememberSaveable(sourceText, effectivePartDurationMs) { mutableStateOf(false) }
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
                                        "${QuickVideoSplitPolicy.timeLabel(part.startMs)}–" +
                                        QuickVideoSplitPolicy.timeLabel(part.endMs),
                                        style = MaterialTheme.typography.bodySmall)
                                }
                                if (parts.size > 5) Text("… e mais ${parts.size - 5} partes",
                                    style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    if (parts.size >= 30) {
                        QuickToolNotice("Esta divisão gerará ${parts.size} arquivos de vídeo" +
                            (if (audioMode == "separate") " e ${parts.size} arquivos de áudio" else "") +
                            ". Confira espaço livre, bateria e aquecimento antes de começar.", isError = false)
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
                    Text(if (mode == QuickToolMode.SPLIT_VIDEO) "$phase · Parte $progressIndex de $progressTotal" else phase,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
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
                        progressTotal = parts.size; phase = "Preparando cortes"
                        openFileError = ""; savedSplitFiles.clear()
                        processing = true; splitView = SplitView.RUNNING
                        runCatching {
                            splitEngine.start(Uri.parse(sourceText), sourceName, parts, audioMode,
                                onProgress = { index, total, step ->
                                    progressIndex = index; progressTotal = total; phase = step
                                },
                                onDone = { count ->
                                    processing = false; finished = true; completed = count
                                    phase = "Partes concluídas e salvas na galeria"
                                    splitView = SplitView.RESULT
                                },
                                onError = { reason, count ->
                                    processing = false; completed = count; error = reason
                                    splitView = SplitView.RESULT
                                },
                                onSaved = { saved -> savedSplitFiles.add(saved) })
                        }.onFailure {
                            processing = false; error = it.message ?: "Não foi possível iniciar a divisão."
                            splitView = SplitView.RESULT
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

/** Evita sobrepor campos numéricos em telas estreitas e com fontes ampliadas. */
@Composable
private fun QuickCustomTimeField(
    label: String, value: String, enabled: Boolean, modifier: Modifier,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.filter { c -> c in '0'..'9' }.take(2)) },
        label = { Text(label) }, enabled = enabled, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier
    )
}

/** Alterna entre presets rápidos, em minutos e tempo manual. */
@Composable
private fun QuickDurationGroupChip(
    label: String, selected: Boolean, enabled: Boolean, modifier: Modifier, onClick: () -> Unit
) {
    FilterChip(selected = selected, onClick = onClick, enabled = enabled,
        label = { Text(label, maxLines = 2) }, modifier = modifier,
        leadingIcon = if (selected) {{ Icon(Icons.Default.Check, contentDescription = null,
            modifier = Modifier.size(16.dp)) }} else null)
}

/** A escolha de duração se adapta ao espaço e expõe a seleção para leitores de tela. */
@Composable
private fun QuickToolDurationChoice(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    modifier: Modifier,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick, enabled = enabled,
        modifier = modifier.heightIn(min = 64.dp).semantics {
            role = Role.RadioButton
            stateDescription = if (selected) "Selecionado" else "Não selecionado"
        },
        shape = UaiShapes.card,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
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
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(label, style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 2)
                if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3)
            }
        }
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
        onClick = onClick, enabled = enabled,
        modifier = Modifier.fillMaxWidth().semantics {
            role = Role.RadioButton
            stateDescription = if (selected) "Selecionado" else "Não selecionado"
        },
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
    Surface(shape = UaiShapes.item, modifier = Modifier.semantics {
        liveRegion = LiveRegionMode.Polite
    }, color = if (isError) colors.errorContainer else colors.primaryContainer) {
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
