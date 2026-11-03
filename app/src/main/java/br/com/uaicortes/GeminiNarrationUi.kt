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
    val durationGoalMs: Long? = null
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
    val creds = remember(ctx) { GeminiCredentialStore(ctx) }
    val api = remember { GeminiTtsClient() }
    val previewCache = remember(ctx) { GeminiVoicePreviewCache(ctx) }
    val scope = rememberCoroutineScope()
    var expanded by remember(project.id) { mutableStateOf(false) }
    var reuseDialogOpen by remember(project.id) { mutableStateOf(false) }
    var reuseSnapshot by remember(project.id) { mutableStateOf<List<NarrationSubtitleLine>>(emptyList()) }
    var reuseSelectedIds by remember(project.id) { mutableStateOf<Set<String>>(emptySet()) }
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
    var compareCueId by remember(project.id) { mutableStateOf<String?>(null) }
    var regenerateCueId by remember(project.id) { mutableStateOf<String?>(null) }
    var referenceDialog by remember(project.id) { mutableStateOf(false) }
    var timedReview by remember(project.id) { mutableStateOf(false) }
    val styleInstruction = GeminiNarrationStyling.instruction(style, pace, expressiveness, pitch)
    val timedSettingsKey = "$model|${selectedVoice?.id.orEmpty()}|$styleInstruction"
    val timedReady = timedLines.mapNotNull { line ->
        timedFiles.firstOrNull { it.segment == line && it.settingsKey == timedSettingsKey && it.file.exists() }
    }
    var advancedStylesOpen by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var working by remember { mutableStateOf(false) }
    var generated by remember(project.id) { mutableStateOf<File?>(null) }
    var duration by remember(project.id) { mutableLongStateOf(0L) }
    var addedUri by remember { mutableStateOf<String?>(null) }
    val latestProject by rememberUpdatedState(project)
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
                voices = result.voices
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
            shape = RoundedCornerShape(24.dp),
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
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = {
                            reuseSnapshot = availableSubtitleLines.toList()
                            reuseSelectedIds = reuseSnapshot.map { it.id }.toSet()
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
                    OutlinedTextField(value = text, onValueChange = {
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
                                "Horários originais das legendas. Referência de áudio é opcional e não é enviada ao Gemini."
                            else "${referenceStarts.size} falas com início ajustado pela referência sonora.",
                                style = MaterialTheme.typography.bodySmall)
                            timedLines.forEach { line ->
                                OutlinedTextField(value = line.text, onValueChange = { newText ->
                                    timedLines = timedLines.map { if (it.cueId == line.cueId)
                                        it.copy(text = newText.take(SubtitleNarrationReuse.MAX_CHARACTERS)) else it }
                                    text = timedLines.joinToString("\n") { it.text.trim() }
                                    timedFiles = timedFiles.filterNot { it.segment.cueId == line.cueId }
                                    durationGoals = durationGoals - line.cueId
                                }, label = { Text("${formatMs(line.startMs)} – ${formatMs(line.subtitleEndMs)}") },
                                    modifier = Modifier.fillMaxWidth(), maxLines = 3, enabled = !working)
                            }
                            val check = SubtitleNarrationTimeline.validate(timedLines, project.durationMs)
                            if (check != null) Text(check, color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall)
                            else Text("${timedLines.size} solicitações de síntese, uma por fala. " +
                                "Arquivos concluídos e sem mudanças de voz/texto serão reaproveitados em caso de falha.",
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
                                    "${voices.size} vozes da API · escolha manualmente", style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                TextButton(onClick = { loadVoices() }, enabled = !working && !loading) {
                                    Icon(Icons.Default.Refresh, null, Modifier.size(17.dp))
                                    Spacer(Modifier.width(4.dp)); Text(if (voices.isEmpty()) "Consultar" else "Atualizar")
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
                            selectedVoice?.let { voice ->
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
                            Button(onClick = {
                                confirmation = if (useTimedSubtitles && timedLines.isNotEmpty())
                                    "generate_sync" else "generate"
                            }, modifier = Modifier.weight(1f),
                                enabled = !working && !loading && text.isNotBlank() && selectedVoice != null &&
                                    (if (useTimedSubtitles && timedLines.isNotEmpty())
                                        SubtitleNarrationTimeline.validate(timedLines, project.durationMs) == null
                                     else (project.durationMs - timelinePositionMs.coerceIn(0L, project.durationMs)) >= 250L),
                                contentPadding = PaddingValues(horizontal = 6.dp)) {
                                Icon(Icons.Default.GraphicEq, null, Modifier.size(17.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(if (useTimedSubtitles && timedLines.isNotEmpty()) "Gerar falas"
                                    else "Gerar e usar", fontSize = 12.sp, maxLines = 1)
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
                        if (status.isNotBlank()) Surface(color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                            Text(status, Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
                        }
                        Text("A primeira amostra de cada voz/modelo/estilo é gerada pela API e pode consumir cota. " +
                            "Repetições com a mesma voz, modelo, estilo e ajustes usam cache local sem internet " +
                            "enquanto o Android mantiver o arquivo. " +
                            "O catálogo Gemini não fornece amostras oficiais para vozes predefinidas.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
    // Seletor único, sem a janela "Narração" aberta atrás e sem ações de rede.
    if (voicePicker && expanded) Dialog(onDismissRequest = { if (!working) { player.pause(); voicePicker = false } },
        properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxWidth(0.96f).fillMaxHeight(0.90f),
            shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.RecordVoiceOver, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Escolher voz", style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f))
                    IconButton(onClick = { player.pause(); voicePicker = false }, enabled = !working) {
                        Icon(Icons.Default.Close, "Voltar à narração")
                    }
                }
                OutlinedTextField(value = voiceSearch, onValueChange = { query ->
                    voiceSearch = query
                    val visible = GeminiVoiceSearch.byGender(GeminiVoiceSearch.filter(voices, query), voiceGenderFilter)
                    draftVoiceId = GeminiVoiceChoice.visibleIdOrNull(visible, draftVoiceId)
                    player.pause()
                    voiceSampleStatus = ""
                },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    placeholder = { Text("Pesquisar vozes...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        if (voiceSearch.isNotBlank()) IconButton(onClick = { voiceSearch = "" }) {
                            Icon(Icons.Default.Close, "Limpar busca")
                        }
                    })
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Todas", "Femininas", "Masculinas").forEach { option ->
                        FilterChip(selected = voiceGenderFilter == option,
                            onClick = {
                                voiceGenderFilter = option
                                val visible = GeminiVoiceSearch.byGender(
                                    GeminiVoiceSearch.filter(voices, voiceSearch), option)
                                draftVoiceId = GeminiVoiceChoice.visibleIdOrNull(visible, draftVoiceId)
                                player.pause()
                                voiceSampleStatus = ""
                            }, label = { Text(option) })
                    }
                }
                val searched = GeminiVoiceSearch.filter(voices, voiceSearch)
                val filtered = GeminiVoiceSearch.byGender(searched, voiceGenderFilter)
                val visibleChoice = GeminiVoiceChoice.visibleChoice(filtered, draftVoiceId)
                Text("${filtered.size} de ${voices.size} vozes · Selecione uma para confirmar",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (filtered.isEmpty()) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("Nenhuma voz encontrada. Altere a busca ou o filtro.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else LazyColumn(Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)) {
                    items(filtered, key = { it.id }) { voice ->
                        val selected = draftVoiceId == voice.id
                        val detailsOpen = expandedVoiceId == voice.id
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(if (selected) 2.dp else 1.dp,
                                if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant),
                            color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                else MaterialTheme.colorScheme.surface
                        ) {
                            Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp)) {
                                Row(Modifier.fillMaxWidth().clickable {
                                    draftVoiceId = voice.id
                                    player.pause()
                                    voiceSampleStatus = ""
                                },
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Mic, null, tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(GeminiVoicePresentation.label(voice),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 2, overflow = TextOverflow.Ellipsis)
                                        Text(GeminiVoicePresentation.shortDetails(voice),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        if (selected) Text("Voz marcada para confirmação",
                                            color = MaterialTheme.colorScheme.primary,
                                            style = MaterialTheme.typography.labelSmall)
                                    }
                                    RadioButton(selected = draftVoiceId == voice.id,
                                        onClick = {
                                            draftVoiceId = voice.id
                                            player.pause()
                                            voiceSampleStatus = ""
                                        }, modifier = Modifier.size(42.dp))
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    TextButton(onClick = {
                                        expandedVoiceId = if (detailsOpen) null else voice.id
                                    }) {
                                        Text(if (detailsOpen) "Ocultar detalhes" else "Detalhes")
                                        Spacer(Modifier.width(4.dp))
                                        Icon(if (detailsOpen) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            null, Modifier.size(18.dp))
                                    }
                                }
                                if (detailsOpen) {
                                    HorizontalDivider()
                                    Spacer(Modifier.height(6.dp))
                                    Text("Nome original: ${voice.name}",
                                        style = MaterialTheme.typography.bodySmall)
                                    Text("Código da API: ${voice.id}",
                                        style = MaterialTheme.typography.bodySmall)
                                    val allDetails = GeminiVoicePresentation.details(voice)
                                    if (allDetails.isNotBlank()) Text(allDetails,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    if (voice.description.isNotBlank()) Text(
                                        "Descrição original da API: ${voice.description}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
                // Reproduzir amostra é opcional e não altera a escolha confirmada.
                // Somente a primeira amostra de cada voz/modelo/estilo pode usar cota.
                OutlinedButton(onClick = {
                    val candidate = GeminiVoiceChoice.visibleChoice(filtered, draftVoiceId)
                    if (candidate != null && !working && !loading) {
                        pendingSampleVoiceId = candidate.id
                        player.pause()
                        voiceSampleStatus = "Verificando amostra de ${GeminiVoicePresentation.label(candidate)}..."
                        job = scope.launch {
                            working = true
                            try {
                                val key = withContext(Dispatchers.IO) { creds.read() }
                                    ?: throw IllegalStateException("Chave Gemini ausente")
                                val id = GeminiPreviewIdentity.id(key, model, candidate.id, styleInstruction)
                                val cached = withContext(Dispatchers.IO) { previewCache.find(id) }
                                if (cached == null) {
                                    confirmation = "sample"
                                    voiceSampleStatus = "Amostra não armazenada: confirme a consulta à API."
                                } else {
                                    player.setMediaItem(MediaItem.fromUri(Uri.fromFile(cached)))
                                    player.prepare(); player.play()
                                    voiceSampleStatus = "Ouvindo ${GeminiVoicePresentation.label(candidate)} do cache local."
                                }
                            } catch (e: CancellationException) { throw e }
                            catch (_: Exception) {
                                voiceSampleStatus = "Não foi possível acessar a amostra. Verifique a chave e a conexão."
                                pendingSampleVoiceId = null
                            } finally { working = false }
                        }
                    }
                }, modifier = Modifier.fillMaxWidth(),
                    enabled = visibleChoice != null && !loading && !working) {
                    Icon(Icons.Default.Headphones, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (playing) "Reproduzindo amostra" else "Ouvir voz marcada")
                }
                Text("Ouvir uma voz pode consumir cota na primeira vez; o aplicativo pede autorização. " +
                    "Confirmar a voz não gera narração automaticamente.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (voiceSampleStatus.isNotBlank()) Text(voiceSampleStatus,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                HorizontalDivider()
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { player.pause(); voicePicker = false },
                        modifier = Modifier.weight(1f), enabled = !working) {
                        Text("Cancelar")
                    }
                    Button(onClick = {
                        val chosen = GeminiVoiceChoice.visibleChoice(filtered, draftVoiceId)
                        if (chosen != null) {
                            selectedVoice = chosen
                            generated = null; duration = 0L; addedUri = null
                            voicePicker = false
                            player.pause()
                            // Volta à preparação da narração; a geração exige confirmação própria.
                            status = "Voz escolhida: ${GeminiVoicePresentation.label(chosen)}. " +
                                "Revise o texto e confirme a geração para inserir na mixagem."
                        }
                    }, modifier = Modifier.weight(1f), enabled = visibleChoice != null && !working && !loading) {
                        Text("Confirmar voz")
                    }
                }
            }
        }
    }
    if (reuseDialogOpen && expanded) {
        val selectedText = SubtitleNarrationReuse.assembled(reuseSnapshot, reuseSelectedIds)
        val selectedCount = reuseSnapshot.count { it.id in reuseSelectedIds }
        val limitExceeded = selectedText.length > SubtitleNarrationReuse.MAX_CHARACTERS
        val sourceChanged = availableSubtitleLines != reuseSnapshot
        Dialog(onDismissRequest = { reuseDialogOpen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(modifier = Modifier.fillMaxWidth(0.94f).fillMaxHeight(0.82f),
                shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Reutilizar legendas do projeto", style = MaterialTheme.typography.titleMedium)
                    Text("Revise os trechos. A importação apenas copia o texto para um rascunho de narração: " +
                        "não altera legendas, não escolhe voz e não utiliza a API.",
                        style = MaterialTheme.typography.bodySmall)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { reuseSelectedIds = reuseSnapshot.map { it.id }.toSet() },
                            modifier = Modifier.weight(1f)) { Text("Marcar todas") }
                        OutlinedButton(onClick = { reuseSelectedIds = emptySet() },
                            modifier = Modifier.weight(1f)) { Text("Desmarcar") }
                    }
                    Text("$selectedCount de ${reuseSnapshot.size} trechos · " +
                        "${selectedText.length}/${SubtitleNarrationReuse.MAX_CHARACTERS} caracteres",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (limitExceeded) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurface)
                    if (limitExceeded) Text("Limite excedido. Desmarque trechos para continuar; " +
                        "nenhuma frase será cortada automaticamente.",
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
                        items(reuseSnapshot, key = { it.id }) { line ->
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = line.id in reuseSelectedIds,
                                    onCheckedChange = { checked ->
                                        reuseSelectedIds = if (checked) reuseSelectedIds + line.id
                                            else reuseSelectedIds - line.id
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
                                timedFiles = emptyList(); referenceStarts = emptyMap(); durationGoals = emptyMap()
                                generated = null; duration = 0L; addedUri = null
                                status = "$selectedCount trechos reutilizados localmente. " +
                                    "Agora consulte as vozes, escolha uma e confirme a geração quando quiser."
                            }
                            reuseDialogOpen = false
                        }, modifier = Modifier.weight(1f),
                            enabled = SubtitleNarrationReuse.canImport(selectedText) && !sourceChanged &&
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
        text = { Text(if (confirmation == "regenerate_one") {
            val line = timedLines.firstOrNull { it.cueId == regenerateCueId }
            val start = line?.let { NarrationReferenceAlignment.start(it, referenceStarts) } ?: 0L
            val target = line?.let { durationGoals[it.cueId] ?:
                NarrationDurationAssist.defaultTarget(start, it.subtitleEndMs, project.durationMs) } ?: 0L
            "Refazer apenas esta fala usando a voz selecionada e meta aproximada de " +
                "${NarrationReferenceAlignment.timeLabel(target)}? " +
                "Esta ação faz uma nova chamada Gemini e pode consumir cota. O WAV anterior " +
                "será mantido se a solicitação falhar; não existe duração exata garantida."
        } else if (confirmation == "generate_sync")
            "Gerar ${timedLines.size} falas separadas usando os horários das legendas? " +
                "Cada fala ainda não gerada exige uma chamada à API e pode consumir cota. " +
                "O áudio será revisado antes de inserir na timeline; não haverá corte automático."
            else if (confirmation == "sample")
            "Não existe amostra oficial disponível nesta consulta. Gerar uma amostra curta pela API " +
                "pode consumir cota ou gerar cobrança. Ela ficará no cache local para próximas reproduções " +
                "com a mesma voz, modelo e estilo. A escolha da voz não será alterada. Continuar?"
            else "Gerar a narração pela API pode consumir cota ou gerar cobrança. " +
                "Se a geração e o salvamento forem bem-sucedidos, o áudio será incluído na " +
                "mixagem do projeto e ficará visível na tela Áudio. O arquivo original não será alterado. " +
                "Não haverá repetição automática em caso de falha.") },
        confirmButton = { TextButton(onClick = {
            val action = confirmation; confirmation = ""
            if (working || loading) return@TextButton
            job = scope.launch {
                working = true
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
                        val saved = withContext(Dispatchers.IO) {
                            coroutineContext.ensureActive()
                            GeminiAudioFiles.save(ctx, wave)
                        }
                        // Somente depois de um WAV válido e salvo substituir a referência do trecho.
                        if (latestProject.id != current.id || timedLines.none { it == segment }) {
                            status = "O projeto mudou. WAV salvo, mas a seleção não foi substituída."
                        } else {
                            timedFiles = timedFiles.filterNot { it.segment.cueId == segment.cueId } +
                                GeminiTimedFile(segment, saved, wave.durationMs, settings, target)
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
                                it.settingsKey == settings && it.file.exists() }
                            if (previous == null) {
                                status = "Gerando trecho ${completed + 1}/${segments.size}. " +
                                    "Falhas não repetem a chamada automaticamente."
                                val wave = api.synthesize(key, model, chosenVoice.id,
                                    styleInstruction, line.text)
                                val saved = withContext(Dispatchers.IO) {
                                    coroutineContext.ensureActive()
                                    GeminiAudioFiles.save(ctx, wave)
                                }
                                timedFiles = timedFiles.filterNot { it.segment.cueId == line.cueId } +
                                    GeminiTimedFile(line, saved, wave.durationMs, settings)
                            }
                            completed++
                        }
                        status = "${segments.size} trechos disponíveis. Revise durações e conflitos antes de inserir."
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
                        file = withContext(Dispatchers.IO) {
                            coroutineContext.ensureActive()
                            if (action == "sample") {
                                // Cancelamento durante síntese não grava amostra incompleta.
                                val saved = previewCache.save(sampleId, wave.bytes)
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
