package br.com.uaicortes

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.coroutines.coroutineContext
import java.io.File
import kotlin.math.roundToInt


/** Rascunho de narração em memória, mantido pelo editor ao alternar telas.
 * Não persiste credenciais nem inicia qualquer chamada à API.
 */
internal class GeminiNarrationDraft {
    val text = mutableStateOf("")
    val model = mutableStateOf(GeminiModels.LITE)
    val selectedVoice = mutableStateOf<GeminiVoice?>(null)
    val voices = mutableStateOf<List<GeminiVoice>>(emptyList())
    val style = mutableStateOf("Neutra")
    val pace = mutableIntStateOf(2)
    val expressiveness = mutableIntStateOf(2)
    val pitch = mutableIntStateOf(2)
    /** Rascunho temporal independente da legenda original. */
    val timedLines = mutableStateOf<List<SubtitleNarrationTimeline.Segment>>(emptyList())
    val useTimedSubtitles = mutableStateOf(false)
    val timedFiles = mutableStateOf<List<GeminiTimedFile>>(emptyList())
    /** O carregamento do checkpoint ocorre uma única vez por projeto/sessão. */
    val recovered = mutableStateOf(false)
    val recoveryNotice = mutableStateOf("")
    /** Marcação manual com referência sonora; não modifica as legendas. */
    val referenceStarts = mutableStateOf<Map<String, Long>>(emptyMap())
    /** Metas opcionais por fala; não alteram a duração do WAV nem a timeline. */
    val durationGoals = mutableStateOf<Map<String, Long>>(emptyMap())
}

internal data class GeminiTimedFile(
    val segment: SubtitleNarrationTimeline.Segment,
    val file: File,
    val durationMs: Long,
    val settingsKey: String,
    val durationGoalMs: Long? = null,
    /** Digest do WAV: impede reutilização de arquivos incompletos ou modificados. */
    val sha256: String = ""
)


@Composable
internal fun GeminiNarrationSection(project: VideoProject, timelinePositionMs: Long,
    update: (VideoProject) -> Unit, openSettings: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    draft: GeminiNarrationDraft? = null,
    onSaveToAudio: (VideoProject) -> Boolean = { false },
    onOpenGenerator: () -> Unit = {}) {
    val currentDraft = draft ?: remember(project.id) { GeminiNarrationDraft() }
    val ctx = LocalContext.current
    val checkpoint = remember(ctx, project.id) { GeminiNarrationCheckpoint(ctx, project.id) }
    val creds = remember(ctx) { GeminiCredentialStore(ctx) }
    val api = remember(ctx) { GeminiTtsClient(ctx) }
    val previewCache = remember(ctx) { GeminiVoicePreviewCache(ctx) }
    val voiceLibrary = remember(ctx) { GeminiVoiceLibrary(ctx) }
    val scope = rememberCoroutineScope()
    var expanded by remember(project.id) { mutableStateOf(false) }
    var reuseDialogOpen by remember(project.id) { mutableStateOf(false) }
    var reuseSnapshot by remember(project.id) { mutableStateOf<List<NarrationSubtitleLine>>(emptyList()) }
    var reuseSelectedIds by remember(project.id) { mutableStateOf<Set<String>>(emptySet()) }
    var reusePage by remember(project.id) { mutableIntStateOf(0) }
    var activeTimedCueId by remember(project.id) { mutableStateOf<String?>(null) }
    var addTimelineConfirm by remember(project.id) { mutableStateOf(false) }
    val availableSubtitleLines = remember(project.subtitles) {
        SubtitleNarrationReuse.eligible(project.subtitles.map {
            NarrationSubtitleLine(it.id, it.text, it.startMs, it.endMs)
        })
    }
    var text by currentDraft.text
    var model by currentDraft.model
    var selectedVoice by currentDraft.selectedVoice
    var voices by currentDraft.voices
    LaunchedEffect(currentDraft.recovered.value) {
        if (currentDraft.recovered.value) {
            val local = withContext(Dispatchers.IO) { voiceLibrary.load().voices }
            if (local.isNotEmpty()) voices = (local + voices).distinctBy { it.id }
        }
    }
    var voiceDiagnostic by remember { mutableStateOf<List<String>>(emptyList()) }
    var showVoiceDiagnostic by remember { mutableStateOf(false) }
    var style by currentDraft.style
    var pace by currentDraft.pace
    var expressiveness by currentDraft.expressiveness
    var pitch by currentDraft.pitch
    var timedLines by currentDraft.timedLines
    var useTimedSubtitles by currentDraft.useTimedSubtitles
    var timedFiles by currentDraft.timedFiles
    var referenceStarts by currentDraft.referenceStarts
    var durationGoals by currentDraft.durationGoals
    val recovered by currentDraft.recovered
    val recoveryNotice by currentDraft.recoveryNotice
    val latestProject by rememberUpdatedState(project)
    var compareCueId by remember(project.id) { mutableStateOf<String?>(null) }
    var regenerateCueId by remember(project.id) { mutableStateOf<String?>(null) }
    var referenceDialog by remember(project.id) { mutableStateOf(false) }
    var timedReview by remember(project.id) { mutableStateOf(false) }
    // Recuperação privada e OFFLINE. Nunca dispara TTS, catálogo ou transcrição.
    LaunchedEffect(checkpoint, currentDraft) {
        if (!currentDraft.recovered.value) {
            val result = withContext(Dispatchers.IO) { checkpoint.recover(project) }
            result.snapshot?.let { saved ->
                currentDraft.text.value = saved.text
                currentDraft.model.value = saved.model
                currentDraft.selectedVoice.value = saved.voice
                currentDraft.voices.value = saved.voice?.let { listOf(it) } ?: emptyList()
                currentDraft.style.value = saved.style
                currentDraft.pace.intValue = saved.pace
                currentDraft.expressiveness.intValue = saved.expressiveness
                currentDraft.pitch.intValue = saved.pitch
                currentDraft.timedLines.value = saved.segments
                currentDraft.timedFiles.value = saved.files
                currentDraft.useTimedSubtitles.value = saved.enabled
                currentDraft.referenceStarts.value = saved.starts
                currentDraft.durationGoals.value = saved.goals
            }
            currentDraft.recoveryNotice.value = result.message
            currentDraft.recovered.value = true
        }
    }
    // Grava mudanças de rascunho/horários/voz, serializadas no armazenamento interno.
    // Cada WAV novo recebe um checkpoint síncrono ANTES da próxima chamada à API.
    LaunchedEffect(checkpoint, recovered) {
        if (recovered) snapshotFlow { checkpoint.capture(currentDraft, latestProject.durationMs) }
            .distinctUntilChanged()
            .collect { snapshot ->
                try {
                    withContext(Dispatchers.IO) { checkpoint.save(snapshot) }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    currentDraft.recoveryNotice.value =
                        "Falha ao guardar a sessão local. Confira o armazenamento antes de fechar o aplicativo."
                }
            }
    }
    val styleInstruction = GeminiNarrationStyling.instruction(style, pace, expressiveness, pitch)
    val timedSettingsKey = "$model|${selectedVoice?.id.orEmpty()}|$styleInstruction"
    val timedReady = timedLines.mapNotNull { line ->
        timedFiles.firstOrNull { it.segment == line && it.settingsKey == timedSettingsKey &&
            it.file.exists() && it.sha256.isNotBlank() }
    }
    val missingTimedCount = (timedLines.size - timedReady.size).coerceAtLeast(0)
    val activeTimedIndex = timedLines.indexOfFirst { it.cueId == activeTimedCueId }.let { if (it < 0) 0 else it }
    val activeTimedLine = timedLines.getOrNull(activeTimedIndex)
    val syncedMode = useTimedSubtitles && timedLines.isNotEmpty()
    val syncedIssue = if (syncedMode) SubtitleNarrationTimeline.validate(timedLines, project.durationMs) else null
    var advancedStylesOpen by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    var usageResult by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var working by remember { mutableStateOf(false) }
    val generationIssues = NarrationGenerationEligibility.blockingReasons(
        busy = working || loading,
        hasVoice = selectedVoice != null,
        catalogEmpty = voices.isEmpty(),
        syncedMode = syncedMode,
        syncValidation = syncedIssue,
        hasBlankSegment = timedLines.any { it.text.isBlank() },
        hasContinuousText = text.isNotBlank(),
        remainingMs = project.durationMs - timelinePositionMs.coerceIn(0L, project.durationMs)
    )
    var generated by remember(project.id) { mutableStateOf<File?>(null) }
    var duration by remember(project.id) { mutableLongStateOf(0L) }
    var addedUri by remember { mutableStateOf<String?>(null) }
    val latestTimelinePosition by rememberUpdatedState(timelinePositionMs)
    var confirmation by remember { mutableStateOf("") }
    var voicePicker by remember { mutableStateOf(false) }
    var draftVoiceId by remember { mutableStateOf<String?>(null) }
    var pendingSampleVoiceId by remember { mutableStateOf<String?>(null) }
    var voiceSampleStatus by remember { mutableStateOf("") }
    var voiceSearch by remember { mutableStateOf("") }
    var voiceGenderFilter by remember { mutableStateOf("Todas") }
    var expandedVoiceId by remember { mutableStateOf<String?>(null) }
    var modelMenu by remember { mutableStateOf(false) }
    var styleMenu by remember { mutableStateOf(false) }
    var job by remember { mutableStateOf<Job?>(null) }
    val lastJob by rememberUpdatedState(job)
    val narrationScrollState = rememberScrollState()
    val player = remember(ctx) { diagnosticPlayer(ctx) }
    var playing by remember { mutableStateOf(false) }
    DisposableEffect(player, api) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) { playing = isPlaying }
        }
        player.addListener(listener)
        onDispose {
            api.cancel(); lastJob?.cancel()
            player.removeListener(listener); player.release()
        }
    }
    val savePicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("audio/wav")) { uri ->
        val localFile = generated
        if (uri != null && localFile != null) scope.launch {
            working = true
            try {
                withContext(Dispatchers.IO) {
                    ctx.contentResolver.openOutputStream(uri, "wt")?.use { dest ->
                        localFile.inputStream().use { it.copyTo(dest) }
                        dest.flush()
                    } ?: throw java.io.IOException("Destino indisponível")
                }
                status = "Narração salva no local selecionado."
            } catch (_: Exception) { status = "Não foi possível salvar o arquivo de áudio." }
            finally { working = false }
        }
    }
    fun loadVoices() {
        if (loading || working) return
        // O toque do usuário inicia a consulta. Nenhum áudio/texto é transmitido e nada é selecionado.
        loading = true
        // Preservar a seleção até a nova resposta. Nunca selecionar outra voz automaticamente.
        // Uma consulta falha não deve apagar o rascunho confirmado.
        status = "Consulta de vozes iniciada. Aguardando resposta do Gemini..."
        voiceDiagnostic = listOf("Solicitação manual iniciada: GET /v1beta/voices (sem síntese de áudio).")
        job = scope.launch {
            try {
                val key = withContext(Dispatchers.IO) { creds.read() }
                    ?: throw IllegalStateException("Chave não configurada.")
                val result = api.voicesWithDiagnosis(key)
                if (result.voices.isNotEmpty()) {
                    withContext(Dispatchers.IO) { voiceLibrary.save(result.voices) }
                    voices = result.voices
                }
                selectedVoice = result.voices.firstOrNull { it.id == selectedVoice?.id }
                    ?: selectedVoice?.takeIf { current -> result.voices.isEmpty() && current.id.isNotBlank() }
                voiceDiagnostic = result.report
                status = if (result.voices.isEmpty())
                    "A consulta terminou sem vozes disponíveis. Abra Diagnóstico do catálogo para ver as etapas."
                else "${result.voices.size} vozes disponíveis. Toque na lista e confirme sua escolha."
            } catch (e: CancellationException) {
                voiceDiagnostic = voiceDiagnostic + "Consulta cancelada."
                status = "Consulta cancelada."
                throw e
            } catch (e: GeminiVoiceCatalogFailure) {
                voiceDiagnostic = e.report()
                showVoiceDiagnostic = true
                status = "Não foi possível carregar as vozes: ${GeminiVoiceCatalog.safeMessage(e.causeError)}"
            } catch (e: Exception) {
                voiceDiagnostic = listOf("Consulta interrompida antes de receber o catálogo.",
                    GeminiVoiceCatalog.safeMessage(e))
                showVoiceDiagnostic = true
                status = "Não foi possível consultar as vozes: ${GeminiVoiceCatalog.safeMessage(e)}"
            } finally { loading = false }
        }
    }
    // O gerador fica fora da rolagem do editor: somente o botão de abertura aparece na tela Áudio.
    AudioQuickAction("Gerar por IA",Icons.Default.AutoAwesome,onClick={
        if (!recovered) return@AudioQuickAction // Recuperação ainda lendo o checkpoint local.
        onOpenGenerator() // Não misturar reprodução da timeline com amostra do Gemini.
        expanded = true
        // Abrir a janela nunca faz chamadas de rede ou escolhe uma voz automaticamente.
    },modifier=modifier)
    // Apenas uma janela visível: ao escolher voz, oculta a narração sob o seletor.
    if (expanded && !voicePicker && !referenceDialog && compareCueId == null) Dialog(
        onDismissRequest = {
            if (!working) {
                if (loading) { job?.cancel(); api.cancel() }
                player.pause(); expanded = false
            }
        }, properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.94f).fillMaxHeight(0.88f),
            shape = UaiShapes.card,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column {
                Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
                    Text("Narração com IA", modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = {
                        if (loading) { job?.cancel(); api.cancel() }
                        player.pause(); expanded = false
                    }, enabled = !working) {
                        Icon(Icons.Default.Close, "Fechar janela de narração")
                    }
                }
                HorizontalDivider()
                Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(narrationScrollState)
                    .padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (recoveryNotice.isNotBlank()) {
                        Text(recoveryNotice, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = {
                            reuseSnapshot = availableSubtitleLines.toList()
                            reusePage = 0
                            reuseSelectedIds = reuseSnapshot.take(SubtitleNarrationTimeline.MAX_SEGMENTS)
                                .map { it.id }.toSet()
                            reuseDialogOpen = true
                        }, modifier = Modifier.fillMaxWidth(),
                            enabled = availableSubtitleLines.isNotEmpty() && !working && !loading) {
                            Icon(Icons.Default.ClosedCaption, contentDescription = null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Reutilizar legendas")
                        }
                        Text(if (availableSubtitleLines.isEmpty())
                            "Nenhuma legenda com texto está salva neste projeto. Gere ou edite as legendas primeiro."
                            else "${availableSubtitleLines.size} legendas disponíveis no projeto (offline, online ou manuais). " +
                                "Toque para revisar e confirmar os trechos; nenhuma nova transcrição será feita.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (!useTimedSubtitles || timedLines.isEmpty()) OutlinedTextField(value = text, onValueChange = {
                        text = it.take(SubtitleNarrationReuse.MAX_CHARACTERS)
                        // Texto digitado livremente não conserva alinhamento com as legendas.
                        timedLines = emptyList(); useTimedSubtitles = false
                        timedFiles = emptyList(); referenceStarts = emptyMap(); durationGoals = emptyMap()
                        generated = null; duration = 0L; addedUri = null
                    }, modifier = Modifier.fillMaxWidth(), minLines = 3, maxLines = 7,
                        readOnly = useTimedSubtitles, enabled = !working,
                        label = { Text("Texto da narração") },
                        supportingText = {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${text.length}/${SubtitleNarrationReuse.MAX_CHARACTERS} caracteres")
                                Text("Português (Brasil)")
                            }
                        })
                    if (timedLines.isNotEmpty()) {
                        Text("Tipo de narração", style = MaterialTheme.typography.labelMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = useTimedSubtitles, onClick = { useTimedSubtitles = true },
                                enabled = !working, label = { Text("Por trechos") })
                            FilterChip(selected = !useTimedSubtitles, onClick = { useTimedSubtitles = false },
                                enabled = !working, label = { Text("Contínua") })
                        }
                        if (useTimedSubtitles) {
                            Text("Cada fala será gerada separadamente no horário da legenda. " +
                                "A duração real será revisada antes de inserir qualquer faixa. " +
                                "Editar o texto abaixo não modifica as legendas originais.",
                                style = MaterialTheme.typography.bodySmall)
                            OutlinedButton(onClick = { referenceDialog = true },
                                enabled = !working && !loading && timedLines.isNotEmpty(),
                                modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.Headphones, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Usar áudio como referência de tempo")
                            }
                            Text(if (referenceStarts.isEmpty())
                                "Os horários das legendas serão usados. Referência sonora é opcional e local."
                            else "${referenceStarts.size} início(s) revisado(s) com áudio de referência.",
                                style = MaterialTheme.typography.bodySmall)
                            if (timedLines.size > SubtitleNarrationTimeline.MAX_SEGMENTS) {
                                Text("Há ${timedLines.size} falas neste rascunho; o máximo é " +
                                    "${SubtitleNarrationTimeline.MAX_SEGMENTS} por lote. Escolha um lote menor.",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall)
                                OutlinedButton(onClick = {
                                    reuseSnapshot = availableSubtitleLines.toList()
                                    reusePage = 0
                                    reuseSelectedIds = reuseSnapshot.take(SubtitleNarrationTimeline.MAX_SEGMENTS)
                                        .map { it.id }.toSet()
                                    reuseDialogOpen = true
                                }, enabled = !working && !loading, modifier = Modifier.fillMaxWidth()) {
                                    Text("Selecionar lote de até 12 falas")
                                }
                            }
                            activeTimedLine?.let { line ->
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    IconButton(onClick = {
                                        activeTimedCueId = timedLines[activeTimedIndex - 1].cueId
                                    }, enabled = activeTimedIndex > 0 && !working) {
                                        Icon(Icons.Default.ChevronLeft, "Fala anterior")
                                    }
                                    Text("Fala ${activeTimedIndex + 1} de ${timedLines.size}",
                                        modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                                    IconButton(onClick = {
                                        activeTimedCueId = timedLines[activeTimedIndex + 1].cueId
                                    }, enabled = activeTimedIndex < timedLines.lastIndex && !working) {
                                        Icon(Icons.Default.ChevronRight, "Próxima fala")
                                    }
                                }
                                OutlinedTextField(value = line.text, onValueChange = { newText ->
                                    timedLines = timedLines.map { if (it.cueId == line.cueId)
                                        it.copy(text = newText.take(SubtitleNarrationReuse.MAX_CHARACTERS)) else it }
                                    text = timedLines.joinToString("\n") { it.text.trim() }
                                    timedFiles = timedFiles.filterNot { it.segment.cueId == line.cueId }
                                    durationGoals = durationGoals - line.cueId
                                }, label = { Text("${formatMs(line.startMs)} – ${formatMs(line.subtitleEndMs)}") },
                                    modifier = Modifier.fillMaxWidth(), minLines = 1, maxLines = 3,
                                    enabled = !working)
                                Text("Edite uma fala por vez; as legendas originais não mudam.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            val check = SubtitleNarrationTimeline.validate(timedLines, project.durationMs)
                            if (check != null) Text(check, color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall)
                            else Text("Lote: ${timedReady.size}/${timedLines.size} WAVs prontos. " +
                                "$missingTimedCount chamada(s) pendente(s); até " +
                                "${SubtitleNarrationTimeline.MAX_SEGMENTS} falas por lote. " +
                                "O progresso é guardado no dispositivo e não dispara solicitações automaticamente.",
                                style = MaterialTheme.typography.bodySmall)
                            if (timedReady.isNotEmpty()) {
                                Text("Áudios preparados: ${timedReady.size}/${timedLines.size}",
                                    style = MaterialTheme.typography.labelMedium)
                                timedReady.forEach { result ->
                                    val start = NarrationReferenceAlignment.start(result.segment, referenceStarts)
                                    val report = SubtitleNarrationTimeline.inspect(
                                        listOf(SubtitleNarrationTimeline.Recorded(
                                            NarrationReferenceAlignment.placed(result.segment, referenceStarts), result.durationMs)),
                                        project.durationMs).single()
                                    val defaultGoal = NarrationDurationAssist.defaultTarget(start,
                                        result.segment.subtitleEndMs, project.durationMs)
                                    val goal = durationGoals[result.segment.cueId] ?: defaultGoal
                                    Surface(shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.fillMaxWidth()) {
                                        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                            Text("${NarrationReferenceAlignment.timeLabel(start)} · " +
                                                "duração real ${NarrationReferenceAlignment.timeLabel(result.durationMs)}" +
                                                if (report.needsApproval) " · revisar" else " · ok",
                                                style = MaterialTheme.typography.bodySmall)
                                            Text(result.segment.text, maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                style = MaterialTheme.typography.bodySmall)
                                            Row(verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Text("Meta: ${NarrationReferenceAlignment.timeLabel(goal)}",
                                                    modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                                                TextButton(onClick = {
                                                    val adjusted = NarrationDurationAssist.adjust(goal,
                                                        -NarrationDurationAssist.STEP_MS, start, project.durationMs)
                                                    if (adjusted > 0L) durationGoals = durationGoals +
                                                        (result.segment.cueId to adjusted)
                                                }, enabled = !working && !loading && goal > NarrationDurationAssist.MIN_TARGET_MS) {
                                                    Text("−0,25 s")
                                                }
                                                TextButton(onClick = {
                                                    val adjusted = NarrationDurationAssist.adjust(goal,
                                                        NarrationDurationAssist.STEP_MS, start, project.durationMs)
                                                    if (adjusted > 0L) durationGoals = durationGoals +
                                                        (result.segment.cueId to adjusted)
                                                }, enabled = !working && !loading &&
                                                    goal < (project.durationMs - start).coerceAtMost(NarrationDurationAssist.MAX_TARGET_MS)) {
                                                    Text("+0,25 s")
                                                }
                                            }
                                            val deviation = NarrationDurationAssist.difference(result.durationMs, goal)
                                            Text("Diferença da meta: ${if (deviation > 0L) "+" else ""}${deviation} ms. " +
                                                "A meta só orienta a próxima síntese; o WAV atual não é modificado.",
                                                style = MaterialTheme.typography.bodySmall)
                                            Row(Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                OutlinedButton(onClick = {
                                                    player.pause(); compareCueId = result.segment.cueId
                                                }, enabled = !working && !loading) {
                                                    Text("Comparar A/B")
                                                }
                                                OutlinedButton(onClick = {
                                                    regenerateCueId = result.segment.cueId
                                                    confirmation = "regenerate_one"
                                                }, enabled = !working && !loading && selectedVoice != null) {
                                                    Text("Refazer fala")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            if (timedReady.size == timedLines.size && timedReady.isNotEmpty()) {
                                OutlinedButton(onClick = { timedReview = true },
                                    enabled = !working && !loading, modifier = Modifier.fillMaxWidth()) {
                                    Text("Revisar e inserir ${timedReady.size} falas na timeline")
                                }
                            }
                        }
                    }
                    if (!creds.hasKey()) {
                        Text("O texto pode ser preparado offline. Para consultar vozes e gerar a narração, " +
                            "configure uma chave Gemini.", style = MaterialTheme.typography.bodyMedium)
                        Button(onClick = { player.pause(); expanded = false; openSettings() }) {
                            Text("Configurar Google Gemini")
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Modelo de IA", style = MaterialTheme.typography.labelMedium)
                            Box {
                                OutlinedButton(onClick = { modelMenu = true },
                                    modifier = Modifier.fillMaxWidth(), enabled = !working && !loading) {
                                    Text(if (model == GeminiModels.LITE) "Gemini 3.8 Flash-Lite TTS"
                                        else "Gemini 3.8 Flash TTS", modifier = Modifier.weight(1f),
                                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Icon(Icons.Default.ArrowDropDown, null)
                                }
                                DropdownMenu(expanded = modelMenu, onDismissRequest = { modelMenu = false }) {
                                    GeminiModels.supported.forEach { option ->
                                        DropdownMenuItem(text = { Text(if (option == GeminiModels.LITE)
                                            "Gemini 3.8 Flash-Lite TTS" else "Gemini 3.8 Flash TTS") },
                                            onClick = { model = option; modelMenu = false })
                                    }
                                }
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Voz", style = MaterialTheme.typography.labelMedium)
                            OutlinedButton(onClick = { voiceSearch = ""; voiceGenderFilter = "Todas"; expandedVoiceId = null; draftVoiceId = selectedVoice?.id
                                voiceSampleStatus = ""; pendingSampleVoiceId = null; voicePicker = true },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !working && !loading && voices.isNotEmpty()) {
                                Icon(Icons.Default.RecordVoiceOver, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(selectedVoice?.let(GeminiVoicePresentation::label) ?: "Nenhuma voz disponível",
                                    modifier = Modifier.weight(1f), maxLines = 1,
                                    overflow = TextOverflow.Ellipsis)
                                Icon(Icons.Default.ArrowDropDown, null)
                            }
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(if (voices.isEmpty()) "Consulte o catálogo e escolha uma voz" else
                                    "${voices.size} vozes do catálogo · escolha manualmente", style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                TextButton(onClick = { voicePicker = true }, enabled = !working && !loading) {
                                    Icon(Icons.Default.RecordVoiceOver, null, Modifier.size(17.dp))
                                    Spacer(Modifier.width(4.dp)); Text("Central")
                                }
                            }
                            TextButton(onClick = { showVoiceDiagnostic = !showVoiceDiagnostic },
                                enabled = !working, contentPadding = PaddingValues(horizontal = 0.dp)) {
                                Icon(Icons.Default.BugReport, contentDescription = null, modifier = Modifier.size(17.dp))
                                Spacer(Modifier.width(5.dp))
                                Text(if (showVoiceDiagnostic) "Ocultar diagnóstico do catálogo" else "Diagnóstico do catálogo")
                            }
                            if (showVoiceDiagnostic) {
                                Surface(color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                        Text("Teste do catálogo Gemini", style = MaterialTheme.typography.labelMedium)
                                        Text("Consulta sem síntese de áudio, sem enviar legendas e sem seleção automática. " +
                                            "A chave nunca aparece neste relatório.", style = MaterialTheme.typography.bodySmall)
                                        (if (voiceDiagnostic.isEmpty())
                                            listOf("Nenhuma consulta registrada nesta janela.") else voiceDiagnostic).forEach { entry ->
                                            Text("• $entry", style = MaterialTheme.typography.bodySmall)
                                        }
                                        OutlinedButton(onClick = { loadVoices() },
                                            enabled = !loading && !working) {
                                            Icon(Icons.Default.Refresh, null, Modifier.size(16.dp))
                                            Spacer(Modifier.width(5.dp))
                                            Text("Executar teste novamente")
                                        }
                                    }
                                }
                            }
                            if (showVoiceDiagnostic) selectedVoice?.let { voice ->
                                val info = GeminiVoicePresentation.details(voice)
                                if (GeminiVoicePresentation.label(voice) != voice.name) {
                                    Text("Nome original: ${voice.name}", style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (info.isNotBlank()) Text(info, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Código da API: ${voice.id}", style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Estilo de narração", style = MaterialTheme.typography.labelMedium)
                            Box {
                                OutlinedButton(onClick = { styleMenu = true }, modifier = Modifier.fillMaxWidth(),
                                    enabled = !working) {
                                    Text(style, modifier = Modifier.weight(1f))
                                    Icon(Icons.Default.ArrowDropDown, null)
                                }
                                DropdownMenu(expanded = styleMenu, onDismissRequest = { styleMenu = false },
                                    modifier = Modifier.heightIn(max = 380.dp)) {
                                    GeminiNarrationStyling.presets.forEach { preset ->
                                        val label = preset.label
                                        DropdownMenuItem(text = { Text(label) }, onClick = {
                                            style = label; styleMenu = false
                                        })
                                    }
                                }
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            TextButton(onClick = { advancedStylesOpen = !advancedStylesOpen },
                                modifier = Modifier.fillMaxWidth(), enabled = !working) {
                                Text(if (advancedStylesOpen) "Ocultar ajustes da voz" else "Ajustes avançados da voz",
                                    modifier = Modifier.weight(1f))
                                Icon(if (advancedStylesOpen) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null)
                            }
                            if (advancedStylesOpen) {
                                Text("Ritmo · ${GeminiNarrationStyling.paceLabels[pace]}",
                                    style = MaterialTheme.typography.labelMedium)
                                Slider(value = pace.toFloat(), onValueChange = { pace = it.roundToInt().coerceIn(0, 4) },
                                    valueRange = 0f..4f, steps = 3, enabled = !working && !loading)
                                Text("Emoção · ${GeminiNarrationStyling.expressivenessLabels[expressiveness]}",
                                    style = MaterialTheme.typography.labelMedium)
                                Slider(value = expressiveness.toFloat(),
                                    onValueChange = { expressiveness = it.roundToInt().coerceIn(0, 4) },
                                    valueRange = 0f..4f, steps = 3, enabled = !working && !loading)
                                Text("Entonação / altura · ${GeminiNarrationStyling.pitchLabels[pitch]}",
                                    style = MaterialTheme.typography.labelMedium)
                                Slider(value = pitch.toFloat(), onValueChange = { pitch = it.roundToInt().coerceIn(0, 4) },
                                    valueRange = 0f..4f, steps = 3, enabled = !working && !loading)
                                TextButton(onClick = { pace = 2; expressiveness = 2; pitch = 2 },
                                    enabled = !working && !loading && (pace != 2 || expressiveness != 2 || pitch != 2)) {
                                    Text("Restaurar ajustes naturais")
                                }
                                Text("São orientações para a IA, não controles exatos de velocidade, " +
                                    "frequência ou intensidade. O resultado depende da voz e do modelo.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        if (loading || working) {
                            LinearProgressIndicator(Modifier.fillMaxWidth())
                            Text(if (loading) "Consultando vozes..." else "Processando solicitação...",
                                style = MaterialTheme.typography.bodySmall)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = {
                                if (working || loading) return@OutlinedButton
                                job = scope.launch {
                                    working = true
                                    try {
                                        val key = withContext(Dispatchers.IO) { creds.read() }
                                            ?: throw IllegalStateException("Chave ausente")
                                        val voice = selectedVoice ?: throw IllegalStateException("Nenhuma voz")
                                        val id = GeminiPreviewIdentity.id(key, model, voice.id, styleInstruction)
                                        val saved = withContext(Dispatchers.IO) { previewCache.find(id) }
                                        if (saved == null) confirmation = "sample"
                                        else {
                                            player.setMediaItem(MediaItem.fromUri(Uri.fromFile(saved)))
                                            player.prepare(); player.play()
                                            status = "Amostra reproduzida do cache local, sem chamada à API."
                                        }
                                    } catch (e: CancellationException) { throw e }
                                    catch (_: Exception) { status = "Não foi possível acessar a amostra local." }
                                    finally { working = false }
                                }
                            },
                                modifier = Modifier.weight(1f),
                                enabled = !working && !loading && selectedVoice != null,
                                contentPadding = PaddingValues(horizontal = 6.dp)) {
                                Icon(Icons.Default.Headphones, null, Modifier.size(17.dp))
                                Spacer(Modifier.width(4.dp)); Text("Ouvir amostra", fontSize = 12.sp, maxLines = 1)
                            }
                        }
                        if (working) OutlinedButton(onClick = {
                            job?.cancel(); api.cancel()
                            status = "Operação cancelada. A solicitação pode já ter consumido cota."
                        }) { Text("Cancelar geração") }
                        generated?.let { file ->
                            if (file.exists()) {
                                HorizontalDivider()
                                Text("Áudio gerado · ${duration / 1000}.${(duration % 1000) / 100} s",
                                    style = MaterialTheme.typography.titleSmall)
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = {
                                        val currentUri = Uri.fromFile(file)
                                        val same = player.currentMediaItem?.localConfiguration?.uri == currentUri
                                        if (same && playing) player.pause() else {
                                            if (!same || player.playbackState == Player.STATE_ENDED) {
                                                player.setMediaItem(MediaItem.fromUri(currentUri))
                                                player.prepare()
                                            }
                                            player.play()
                                        }
                                    }, modifier = Modifier.weight(1f), enabled = !working) {
                                        Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            null, Modifier.size(18.dp))
                                        Text(if (playing) "Pausar" else "Reproduzir", fontSize = 12.sp)
                                    }
                                    OutlinedButton(onClick = { savePicker.launch("UaiCortes_narracao.wav") },
                                        modifier = Modifier.weight(1f), enabled = !working) {
                                        Icon(Icons.Default.SaveAlt, null, Modifier.size(18.dp))
                                        Text("Salvar áudio", fontSize = 12.sp)
                                    }
                                }
                                Button(onClick = { addTimelineConfirm = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !working && addedUri != GeminiAudioFiles.uri(file) &&
                                        (project.durationMs - timelinePositionMs.coerceIn(0L, project.durationMs)) >= duration) {
                                    Text("Adicionar à mixagem")
                                }
                            }
                        }
                        if (usageResult.isNotBlank()) Text("Última geração: $usageResult",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (status.isNotBlank()) Surface(color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                            Text(status, Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
                        }
                        Text("Amostras podem consumir cota na primeira geração; repetidas usam o cache local.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                HorizontalDivider()
                Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (generationIssues.isNotEmpty()) {
                        generationIssues.forEach { reason ->
                            Text("Para gerar: $reason", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error)
                        }
                        if (selectedVoice == null && !working && !loading) {
                            TextButton(onClick = {
                                draftVoiceId = null; voicePicker = true
                            }) { Text("Central de Vozes") }
                        }
                    } else if (syncedMode) {
                        Text("${timedLines.size} falas no lote · ${timedReady.size} pronta(s) · " +
                            "$missingTimedCount pendente(s)", style = MaterialTheme.typography.labelMedium)
                    }
                    Button(onClick = {
                        if (useTimedSubtitles && timedLines.isNotEmpty() && missingTimedCount == 0) {
                            timedReview = true
                        } else confirmation = if (useTimedSubtitles && timedLines.isNotEmpty())
                            "generate_sync" else "generate"
                    }, modifier = Modifier.fillMaxWidth(),
                        enabled = generationIssues.isEmpty()) {
                        Icon(Icons.Default.GraphicEq, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(7.dp))
                        Text(if (syncedMode) {
                            if (missingTimedCount == 0) "Revisar falas"
                            else if (missingTimedCount < timedLines.size) "Retomar ($missingTimedCount)"
                            else "Gerar falas"
                        } else "Gerar e usar")
                    }
                }
            }
        }
    }
    // Catálogo e prévias centralizados: o seletor usa a mesma Central da Home e Vozes IA.
    // Escolher uma voz modifica apenas o rascunho; não gera narração nem altera timeline.
    if (voicePicker && expanded) Dialog(onDismissRequest = {
        if (!working) { player.pause(); voicePicker = false }
    }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxWidth(.97f).fillMaxHeight(.91f),
            shape = UaiShapes.card, color = MaterialTheme.colorScheme.surface) {
            GeminiVoiceCenterScreen(onBack = { player.pause(); voicePicker = false },
                selectedVoiceId = selectedVoice?.id, initialModel = model,
                onModelChange = { model = it },
                styleInstruction = styleInstruction,
                onChoose = { chosen ->
                    selectedVoice = chosen
                    voices = (voices + chosen).distinctBy { it.id }
                    generated = null; duration = 0L; addedUri = null
                    voicePicker = false
                    player.pause()
                    status = "Voz escolhida: ${GeminiVoicePresentation.label(chosen)}. " +
                        "Revise e confirme a geração para inserir na mixagem."
                })
        }
    }
    if (reuseDialogOpen && expanded) {
        val selectedText = SubtitleNarrationReuse.assembled(reuseSnapshot, reuseSelectedIds)
        val selectedCount = reuseSnapshot.count { it.id in reuseSelectedIds }
        val reusePageSize = SubtitleNarrationTimeline.MAX_SEGMENTS
        val reusePageCount = ((reuseSnapshot.size + reusePageSize - 1) / reusePageSize).coerceAtLeast(1)
        val visibleReusePage = reusePage.coerceIn(0, reusePageCount - 1)
        val visibleReuseLines = reuseSnapshot.drop(visibleReusePage * reusePageSize).take(reusePageSize)
        val limitExceeded = selectedText.length > SubtitleNarrationReuse.MAX_CHARACTERS ||
            selectedCount > SubtitleNarrationTimeline.MAX_SEGMENTS
        val sourceChanged = availableSubtitleLines != reuseSnapshot
        Dialog(onDismissRequest = { reuseDialogOpen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(modifier = Modifier.fillMaxWidth(0.94f).fillMaxHeight(0.82f),
                shape = UaiShapes.card, color = MaterialTheme.colorScheme.surface) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Reutilizar legendas do projeto", style = MaterialTheme.typography.titleMedium)
                    Text("Revise os trechos. A importação apenas copia o texto para um rascunho de narração: " +
                        "não altera legendas, não escolhe voz e não utiliza a API.",
                        style = MaterialTheme.typography.bodySmall)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        IconButton(onClick = { reusePage = (visibleReusePage - 1).coerceAtLeast(0) },
                            enabled = visibleReusePage > 0) {
                            Icon(Icons.Default.ChevronLeft, "Página anterior")
                        }
                        Text("Página ${visibleReusePage + 1} de $reusePageCount",
                            style = MaterialTheme.typography.labelMedium)
                        IconButton(onClick = {
                            reusePage = (visibleReusePage + 1).coerceAtMost(reusePageCount - 1)
                        }, enabled = visibleReusePage < reusePageCount - 1) {
                            Icon(Icons.Default.ChevronRight, "Próxima página")
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            reuseSelectedIds = visibleReuseLines.map { it.id }.toSet()
                        }, modifier = Modifier.weight(1f)) { Text("Selecionar esta página") }
                        OutlinedButton(onClick = {
                            reusePage = if (visibleReusePage + 1 < reusePageCount) visibleReusePage + 1 else 0
                            reuseSelectedIds = reuseSnapshot.drop(reusePage * reusePageSize)
                                .take(reusePageSize).map { it.id }.toSet()
                        }, modifier = Modifier.weight(1f)) { Text("Próximas 12") }
                    }
                    Text("Lote de até 12 · $selectedCount de ${reuseSnapshot.size} trechos · " +
                        "${selectedText.length}/${SubtitleNarrationReuse.MAX_CHARACTERS} caracteres",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (limitExceeded) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurface)
                    if (limitExceeded) Text("Limite de 12 falas ou 4.000 caracteres excedido. " +
                        "Desmarque trechos; nenhuma frase será cortada automaticamente.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                    if (sourceChanged) Text("As legendas mudaram enquanto esta janela estava aberta. " +
                        "Feche e reabra para atualizar a seleção.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                    if (text.isNotBlank()) Text("Atenção: ao confirmar, o rascunho atual será substituído. " +
                        "As legendas originais permanecerão intactas.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    HorizontalDivider()
                    LazyColumn(Modifier.weight(1f)) {
                        items(visibleReuseLines, key = { it.id }) { line ->
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = line.id in reuseSelectedIds,
                                    onCheckedChange = { checked ->
                                        reuseSelectedIds = if (checked && reuseSelectedIds.size <
                                                SubtitleNarrationTimeline.MAX_SEGMENTS) reuseSelectedIds + line.id
                                            else if (!checked) reuseSelectedIds - line.id else reuseSelectedIds
                                    })
                                Column(Modifier.weight(1f)) {
                                    Text("${formatMs(line.startMs)} – ${formatMs(line.endMs)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(line.text, style = MaterialTheme.typography.bodySmall,
                                        maxLines = 3, overflow = TextOverflow.Ellipsis)
                                }
                            }
                            HorizontalDivider()
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { reuseDialogOpen = false }, modifier = Modifier.weight(1f)) {
                            Text("Cancelar")
                        }
                        Button(onClick = {
                            // Fonte e seleção só são lidas novamente após confirmação expressa.
                            if (availableSubtitleLines != reuseSnapshot) {
                                status = "Legendas alteradas. Reabra a importação para revisar."
                            } else if (SubtitleNarrationReuse.canImport(selectedText)) {
                                player.pause()
                                text = selectedText
                                timedLines = SubtitleNarrationReuse.eligible(reuseSnapshot)
                                    .filter { it.id in reuseSelectedIds }
                                    .map { SubtitleNarrationTimeline.Segment(it.id,
                                        it.text.trim().replace(Regex("\\s+"), " "), it.startMs, it.endMs) }
                                useTimedSubtitles = true
                                activeTimedCueId = timedLines.firstOrNull()?.cueId
                                timedFiles = emptyList(); referenceStarts = emptyMap(); durationGoals = emptyMap()
                                generated = null; duration = 0L; addedUri = null
                                status = "$selectedCount trechos reutilizados localmente. " +
                                    "Agora consulte as vozes, escolha uma e confirme a geração quando quiser."
                            }
                            reuseDialogOpen = false
                        }, modifier = Modifier.weight(1f),
                            enabled = SubtitleNarrationReuse.canImport(selectedText) &&
                                selectedCount <= SubtitleNarrationTimeline.MAX_SEGMENTS && !sourceChanged &&
                                !working && !loading) {
                            Text("Confirmar uso")
                        }
                    }
                }
            }
        }
    }
    if (addTimelineConfirm) {
        val available = (project.durationMs -
            timelinePositionMs.coerceIn(0L, project.durationMs)).coerceAtLeast(0L)
        val playable = minOf(duration, available)
        AlertDialog(onDismissRequest = { addTimelineConfirm = false },
            title = { Text("Adicionar narração à mixagem?") },
            text = { Text("O áudio gerado será adicionado como nova faixa em " +
                "${formatMs(timelinePositionMs.coerceIn(0L, project.durationMs))}. " +
                "Duração disponível: ${formatMs(playable)}. " +
                "O áudio original e as legendas não serão apagados. " +
                "Após salvar, a tela Áudio continuará aberta para conferir a mixagem." +
                (if (playable < duration) " Sem espaço para a narração completa: mova o cursor e tente novamente; não haverá corte automático." else "")) },
            confirmButton = { TextButton(onClick = {
                addTimelineConfirm = false
                val file = generated
                if (file != null && file.exists() && duration >= 250L && playable == duration) {
                    val uri = GeminiAudioFiles.uri(file)
                    if (addedUri != uri && project.audioTracks.none { it.uri == uri }) {
                        val track = AudioTrack(uri = uri, title = "Narração Gemini",
                            startMs = timelinePositionMs.coerceIn(0L, project.durationMs),
                            durationMs = playable, volume = 1f)
                        val changed = project.copy(audioTracks = project.audioTracks + track)
                        // A gravação precisa terminar com sucesso antes de navegar.
                        if (onSaveToAudio(changed)) {
                            addedUri = uri
                            player.pause()
                            expanded = false
                        } else status = "Não foi possível salvar o projeto. A narração foi mantida para nova tentativa."
                    } else status = "Esta narração já foi adicionada ao projeto."
                } else status = "Áudio indisponível ou cursor no final do projeto."
            }) { Text("Adicionar à mixagem") } },
            dismissButton = { TextButton(onClick = { addTimelineConfirm = false }) {
                Text("Cancelar")
            } })
    }
    if (referenceDialog && timedLines.isNotEmpty()) {
        AudioReferenceAlignmentDialog(project, timedLines, referenceStarts,
            onApply = { marked ->
                referenceStarts = marked
                referenceDialog = false
                status = if (marked.isEmpty()) "Horários originais restaurados." else
                    "${marked.size} inícios ajustados ouvindo a referência. " +
                    "A próxima geração usará esses horários; o áudio de referência não é enviado à API."
            }, onDismiss = { referenceDialog = false })
    }
    compareCueId?.let { cueId ->
        val seg = timedLines.firstOrNull { it.cueId == cueId }
        val result = timedReady.firstOrNull { it.segment.cueId == cueId }
        if (seg != null && result != null) {
            GeminiNarrationComparisonDialog(latestProject, seg,
                NarrationReferenceAlignment.start(seg, referenceStarts),
                result.file, result.durationMs, onDismiss = { compareCueId = null })
        } else {
            LaunchedEffect(cueId) { compareCueId = null }
        }
    }
    if (timedReview) {
        val current = latestProject
        val ready = timedLines.mapNotNull { seg ->
            timedFiles.firstOrNull { it.segment == seg && it.settingsKey == timedSettingsKey && it.file.exists() }
        }
        val records = ready.map { SubtitleNarrationTimeline.Recorded(
            NarrationReferenceAlignment.placed(it.segment, referenceStarts), it.durationMs) }
        val inspections = SubtitleNarrationTimeline.inspect(records, current.durationMs)
        val referenceIssue = NarrationReferenceAlignment.validate(timedLines, referenceStarts, current.durationMs)
        val staleSource = timedLines.any { seg -> current.subtitles.none { cue ->
            cue.id == seg.cueId && cue.startMs == seg.startMs && cue.endMs == seg.subtitleEndMs
        } }
        val invalid = SubtitleNarrationTimeline.validate(timedLines, current.durationMs) != null ||
            referenceIssue != null || ready.size != timedLines.size || staleSource || inspections.any { it.blocking } ||
            ready.any { it.file.length() == 0L || current.audioTracks.any { track ->
                track.uri == GeminiAudioFiles.uri(it.file) } }
        var approveOverrun by remember(timedReview) { mutableStateOf(false) }
        val overruns = inspections.count { it.needsApproval }
        AlertDialog(onDismissRequest = { if (!working) timedReview = false },
            title = { Text("Revisar narração sincronizada") },
            text = { Column(Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("${ready.size} falas geradas. Cada áudio começará no horário confirmado " +
                    "(original da legenda ou ajustado pela referência sonora). Não haverá corte automático.")
                inspections.forEach { item ->
                    val reason = buildList {
                        if (SubtitleNarrationTimeline.Issue.SUBTITLE_TOO_SHORT in item.issues)
                            add("ultrapassa a legenda")
                        if (SubtitleNarrationTimeline.Issue.NEXT_SPEECH_OVERLAP in item.issues)
                            add("alcança a próxima fala")
                        if (SubtitleNarrationTimeline.Issue.PROJECT_OVERFLOW in item.issues)
                            add("ultrapassa o fim do vídeo")
                        if (SubtitleNarrationTimeline.Issue.AUDIO_TOO_SHORT in item.issues)
                            add("áudio curto demais")
                    }
                    Text("${formatMs(item.segment.startMs)} · ${formatMs(item.audioDurationMs)}" +
                        if (reason.isEmpty()) " · dentro do intervalo" else " · ${reason.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (reason.isEmpty()) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.error)
                }
                if (referenceIssue != null) Text(referenceIssue,
                    color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                if (invalid) Text("Não é possível inserir: projeto, legendas ou arquivos mudaram, " +
                    "ou uma fala ultrapassa o final. Corrija e regenere somente as falas alteradas.",
                    color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                if (overruns > 0 && !invalid) Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = approveOverrun, onCheckedChange = { approveOverrun = it })
                    Text("Aceito as $overruns falas que ultrapassam seus intervalos. " +
                        "Podem se sobrepor a outras faixas.", style = MaterialTheme.typography.bodySmall)
                }
                Text("As legendas e o áudio original não serão substituídos.",
                    style = MaterialTheme.typography.bodySmall)
            } },
            confirmButton = { TextButton(onClick = {
                if (invalid || (overruns > 0 && !approveOverrun)) return@TextButton
                val tracks = ready.map { result -> AudioTrack(
                    uri = GeminiAudioFiles.uri(result.file),
                    title = "Gemini · ${formatMs(NarrationReferenceAlignment.start(result.segment, referenceStarts))}",
                    startMs = NarrationReferenceAlignment.start(result.segment, referenceStarts),
                    durationMs = result.durationMs, volume = 1f) }
                val changed = current.copy(audioTracks = current.audioTracks + tracks)
                if (runCatching { onSaveToAudio(changed) }.getOrDefault(false)) {
                    timedReview = false; expanded = false
                    status = "${tracks.size} narrações adicionadas com horários confirmados."
                } else {
                    timedReview = false
                    status = "Falha ao salvar. Áudios preparados foram mantidos; tente inserir novamente."
                }
            }, enabled = !invalid && !working && (overruns == 0 || approveOverrun)) {
                Text("Inserir faixas")
            } },
            dismissButton = { TextButton(onClick = { timedReview = false }) { Text("Voltar") } })
    }
    if (confirmation.isNotBlank()) AlertDialog(onDismissRequest = {
        confirmation = ""; pendingSampleVoiceId = null
    },
        title = { Text("Confirmar solicitação Gemini") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(if (confirmation == "regenerate_one") {
            val line = timedLines.firstOrNull { it.cueId == regenerateCueId }
            val start = line?.let { NarrationReferenceAlignment.start(it, referenceStarts) } ?: 0L
            val target = line?.let { durationGoals[it.cueId] ?:
                NarrationDurationAssist.defaultTarget(start, it.subtitleEndMs, project.durationMs) } ?: 0L
            "Refazer apenas esta fala usando a voz selecionada e meta aproximada de " +
                "${NarrationReferenceAlignment.timeLabel(target)}? " +
                "Esta ação faz uma nova chamada Gemini e pode consumir cota. O WAV anterior " +
                "será mantido se a solicitação falhar; não existe duração exata garantida."
        } else if (confirmation == "generate_sync")
            "Retomar este lote com $missingTimedCount solicitação(ões) pendente(s) e " +
                "${timedReady.size} WAV(s) já preparado(s)? " +
                "Somente falas ainda não disponíveis com a mesma voz/texto/estilo receberão " +
                "nova chamada à API, podendo consumir cota. Nada será gerado automaticamente."
            else if (confirmation == "sample")
            "Não existe amostra oficial disponível nesta consulta. Gerar uma amostra curta pela API " +
                "pode consumir cota ou gerar cobrança. Ela ficará no cache local para próximas reproduções " +
                "com a mesma voz, modelo e estilo. A escolha da voz não será alterada. Continuar?"
            else "Gerar a narração pela API pode consumir cota ou gerar cobrança. " +
                "Se a geração e o salvamento forem bem-sucedidos, o áudio será incluído na " +
                "mixagem do projeto e ficará visível na tela Áudio. O arquivo original não será alterado. " +
                "Não haverá repetição automática em caso de falha.")
            GeminiUsageHint("narração", inputChars = if (confirmation == "sample")
                GeminiPreviewIdentity.TEXT.length else text.length, model = model)
        } },
        confirmButton = { TextButton(onClick = {
            val action = confirmation; confirmation = ""
            if (working || loading) return@TextButton
            job = scope.launch {
                working = true
                usageResult = ""
                status = "Solicitando áudio ao Gemini..."
                try {
                    val key = withContext(Dispatchers.IO) { creds.read() }
                        ?: throw IllegalStateException("Chave ausente")
                    if (action == "regenerate_one") {
                        val requestedId = regenerateCueId
                        val segment = timedLines.firstOrNull { it.cueId == requestedId }
                            ?: throw IllegalStateException("Selecione a fala novamente.")
                        val current = latestProject
                        if (current.id != project.id || current.subtitles.none { cue ->
                                cue.id == segment.cueId && cue.startMs == segment.startMs &&
                                    cue.endMs == segment.subtitleEndMs
                            }) throw IllegalStateException("Legenda alterada. Importe a versão atual antes de refazer.")
                        val voice = selectedVoice ?: throw IllegalStateException("Escolha a voz antes de refazer.")
                        val start = NarrationReferenceAlignment.start(segment, referenceStarts)
                        val target = durationGoals[segment.cueId] ?:
                            NarrationDurationAssist.defaultTarget(start, segment.subtitleEndMs, current.durationMs)
                        if (NarrationReferenceAlignment.validate(timedLines, referenceStarts, current.durationMs) != null ||
                            target !in NarrationDurationAssist.MIN_TARGET_MS..NarrationDurationAssist.MAX_TARGET_MS ||
                            target > current.durationMs - start)
                            throw IllegalStateException("Horário ou meta de duração inválidos.")
                        val settings = "$model|${voice.id}|$styleInstruction"
                        status = "Refazendo somente a fala marcada. O áudio anterior permanece até o sucesso."
                        val wave = api.synthesize(key, model, voice.id,
                            styleInstruction + "\n" + NarrationDurationAssist.instruction(target),
                            segment.text)
                        usageResult = GeminiUsageLedger(ctx).lastMessage("narracao")
                        val saved = withContext(Dispatchers.IO) {
                            coroutineContext.ensureActive()
                            GeminiAudioFiles.save(ctx, wave)
                        }
                        // Somente depois de um WAV válido e salvo substituir a referência do trecho.
                        if (latestProject.id != current.id || timedLines.none { it == segment }) {
                            status = "O projeto mudou. WAV salvo, mas a seleção não foi substituída."
                        } else {
                            timedFiles = timedFiles.filterNot { it.segment.cueId == segment.cueId } +
                                GeminiTimedFile(segment, saved, wave.durationMs, settings, target,
                                    GeminiNarrationCheckpoint.hash(wave.bytes))
                            withContext(Dispatchers.IO) {
                                checkpoint.save(checkpoint.capture(currentDraft, current.durationMs))
                            }
                            val difference = NarrationDurationAssist.difference(wave.durationMs, target)
                            status = "Fala refeita; duração real ${NarrationReferenceAlignment.timeLabel(wave.durationMs)} " +
                                "(${if (difference > 0) "+" else ""}$difference ms da meta). " +
                                "Compare e revise a sincronização antes de inserir."
                        }
                        regenerateCueId = null
                    } else if (action == "generate_sync") {
                        val segments = timedLines.toList()
                        val current = latestProject
                        val failure = SubtitleNarrationTimeline.validate(segments, current.durationMs)
                        if (failure != null) throw IllegalStateException(failure)
                        NarrationReferenceAlignment.validate(segments, referenceStarts, current.durationMs)
                            ?.let { throw IllegalStateException(it) }
                        if (current.id != project.id || segments.any { seg -> current.subtitles.none { cue ->
                            cue.id == seg.cueId && cue.startMs == seg.startMs &&
                                cue.endMs == seg.subtitleEndMs
                        } }) throw IllegalStateException("Projeto ou horários mudaram. Importe as legendas novamente.")
                        val chosenVoice = selectedVoice ?: throw IllegalStateException("Escolha uma voz.")
                        val settings = "$model|${chosenVoice.id}|$styleInstruction"
                        var completed = 0
                        for (line in segments) {
                            coroutineContext.ensureActive()
                            val previous = timedFiles.firstOrNull { it.segment == line &&
                                it.settingsKey == settings && it.file.exists() && it.sha256.isNotBlank() }
                            if (previous == null) {
                                status = "Gerando trecho ${completed + 1}/${segments.size}. " +
                                    "Falhas não repetem a chamada automaticamente."
                                val wave = api.synthesize(key, model, chosenVoice.id,
                                    styleInstruction, line.text)
                                usageResult = GeminiUsageLedger(ctx).lastMessage("narracao")
                                val saved = withContext(Dispatchers.IO) {
                                    coroutineContext.ensureActive()
                                    GeminiAudioFiles.save(ctx, wave)
                                }
                                timedFiles = timedFiles.filterNot { it.segment.cueId == line.cueId } +
                                    GeminiTimedFile(line, saved, wave.durationMs, settings,
                                        sha256 = GeminiNarrationCheckpoint.hash(wave.bytes))
                                // Registrar a conclusão ANTES de solicitar o próximo WAV.
                                withContext(Dispatchers.IO) {
                                    checkpoint.save(checkpoint.capture(currentDraft, current.durationMs))
                                }
                            }
                            completed++
                        }
                        status = "${segments.size} trechos disponíveis. Progresso guardado no aparelho; " +
                            "revise durações e conflitos antes de inserir."
                        timedReview = true
                    } else {
                    val voice = if (action == "sample" && pendingSampleVoiceId != null)
                        GeminiVoiceChoice.confirmed(voices, pendingSampleVoiceId)
                    else selectedVoice
                    if (voice == null) throw IllegalStateException("Nenhuma voz disponível")
                    val instruction = styleInstruction
                    val sampleId = if (action == "sample")
                        GeminiPreviewIdentity.id(key, model, voice.id, instruction) else ""
                    val cached = if (action == "sample") withContext(Dispatchers.IO) {
                        previewCache.find(sampleId)
                    } else null
                    val file: File
                    val reused = cached != null
                    val requestProjectId = project.id
                    if (cached != null) file = cached
                    else {
                        // Não fazer preflight GET /models: além de uma chamada extra,
                        // pode ser rejeitado por permissão mesmo quando a síntese funciona.
                        val input = if (action == "sample") GeminiPreviewIdentity.TEXT else text.trim()
                        val wave = api.synthesize(key, model, voice.id, instruction, input)
                        usageResult = GeminiUsageLedger(ctx).lastMessage("narracao")
                        file = withContext(Dispatchers.IO) {
                            coroutineContext.ensureActive()
                            if (action == "sample") {
                                // Cancelamento durante síntese não grava amostra incompleta.
                                val saved = previewCache.save(sampleId, wave.bytes, voice.id)
                                coroutineContext.ensureActive()
                                saved
                            } else {
                                val saved = GeminiAudioFiles.save(ctx, wave)
                                try { coroutineContext.ensureActive(); saved }
                                catch (e: CancellationException) { saved.delete(); throw e }
                            }
                        }
                        if (action != "sample") {
                            generated = file; duration = wave.durationMs; addedUri = null
                        }
                    }
                    player.setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
                    player.prepare()
                    if (action == "sample") {
                        player.play()
                        voiceSampleStatus = "Ouvindo amostra de ${GeminiVoicePresentation.label(voice)}."
                        pendingSampleVoiceId = null
                        status = if (reused) "Amostra local: nenhuma chamada à API."
                            else "Amostra gerada pela API e guardada no cache local."
                    } else {
                        // A confirmação cobriu geração E inserção, mas só atualizar a
                        // timeline após o WAV válido ter sido gravado com sucesso.
                        val fresh = latestProject
                        if (fresh.id != requestProjectId) {
                            status = "Projeto alterado durante a geração. Áudio guardado, não inserido."
                        } else {
                            val position = latestTimelinePosition.coerceIn(0L, fresh.durationMs)
                            val playable = minOf(duration, (fresh.durationMs - position).coerceAtLeast(0L))
                            val uri = GeminiAudioFiles.uri(file)
                            if (playable < 250L || playable < duration) {
                                status = "Áudio gerado, mas não cabe inteiro a partir do cursor. " +
                                    "Mova o cursor e use Adicionar à mixagem; o WAV não será cortado."
                            } else if (fresh.audioTracks.any { it.uri == uri }) {
                                status = "Esta narração já consta na mixagem."
                            } else {
                                val changed = fresh.copy(audioTracks = fresh.audioTracks +
                                    AudioTrack(uri = uri, title = "Narração Gemini",
                                        startMs = position, durationMs = playable, volume = 1f))
                                if (onSaveToAudio(changed)) {
                                    addedUri = uri
                                    player.pause()
                                    expanded = false // Exibir a tela Áudio e a nova faixa.
                                } else {
                                    status = "Narração gerada, mas falhou ao salvar o projeto. " +
                                        "O WAV está guardado para adicionar sem gerar de novo."
                                }
                            }
                        }
                    }
                    } // somente amostra ou geração contínua
                } catch (e: CancellationException) {
                    status = "Solicitação cancelada. A API pode já ter consumido cota."
                    throw e
                }
                catch (e: GeminiApiError) {
                    status = e.message ?: "Falha na API Gemini."
                    if (action == "sample") voiceSampleStatus = status
                }
                catch (e: java.io.IOException) {
                    status = e.message ?: "Falha de conexão ou armazenamento."
                    if (action == "sample") voiceSampleStatus = status
                }
                catch (e: Exception) {
                    status = when (e) {
                        is IllegalStateException -> e.message?.take(160)
                        else -> null
                    } ?: "Não foi possível gerar áudio ou acessar a chave."
                    if (action == "sample") voiceSampleStatus = status
                }
                finally { working = false }
            }
        }) { Text("Continuar") } },
        dismissButton = { TextButton(onClick = {
            confirmation = ""; pendingSampleVoiceId = null
        }) { Text("Voltar") } })
}
