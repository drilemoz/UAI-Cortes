package br.com.uaicortes

import android.os.SystemClock
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class SubtitleEngine(val title: String) { VOSK("Offline · Vosk"), GEMINI("Online · Gemini") }
private enum class AutoSubtitleRoute { SETUP, GENERATING, REVIEW, CUE, LIPS, REDO }

private const val SUBTITLE_GEMINI_MODEL = "gemini-3.5-transcribe"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoSubtitleScreen(project: VideoProject, back: () -> Unit,
    onManageModel: () -> Unit, onManageGemini: () -> Unit, onApply: (VideoProject) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val geminiStore = remember(context) { GeminiCredentialStore(context) }
    val geminiClient = remember(context) { GeminiSubtitleClient(context) }
    val installed = AutoSubtitleEngine.installed(context)
    val geminiConfigured = geminiStore.hasKey()
    val full = SubtitleSpeechRange(0L, project.durationMs)
    val sources = remember(project.audioTracks, project.clips) { ClipAudioBindings.resolved(project) }
    var open by remember(project.id) { mutableStateOf<SubtitleEngine?>(null) }
    var page by remember(project.id) { mutableStateOf(AutoSubtitleRoute.SETUP) }
    var referenceId by remember(project.id) { mutableStateOf<String?>(null) }
    var requestedRange by remember(project.id, full) { mutableStateOf(full) }
    var customRange by remember(project.id) { mutableStateOf(false) }
    var voiceDecisions by remember(project.id) { mutableStateOf<Map<String,String>>(emptyMap()) }
    var voiceNames by remember(project.id) { mutableStateOf<Map<String,String>>(emptyMap()) }
    var lastEngine by remember(project.id) { mutableStateOf<SubtitleEngine?>(null) }
    var draftRange by remember(project.id, full) { mutableStateOf(full) }
    var draftTrack by remember(project.id) { mutableStateOf<String?>(null) }
    var results by remember(project.id) { mutableStateOf<List<SubtitleCue>>(emptyList()) }
    var needsReview by remember(project.id) { mutableStateOf(emptySet<String>()) }
    var replace by remember(project.id) { mutableStateOf(false) }
    var detectSpeakers by remember(project.id) { mutableStateOf(true) }
    var showDetails by remember { mutableStateOf(false) }
    var cloudIssue by remember(project.id) { mutableStateOf<SubtitleCloudIssue?>(null) }
    var appearanceMode by remember(project.id) { mutableStateOf(SubtitleAppearanceMode.KARAOKE_ROSA) }
    var generatedSize by remember(project.id) { mutableIntStateOf(SubtitleAppearancePolicy.ROSA_SIZE_SP) }
    var selectedCueId by remember(project.id) { mutableStateOf<String?>(null) }
    var cueText by remember(project.id) { mutableStateOf("") }
    var lipsReturn by remember(project.id) { mutableStateOf(AutoSubtitleRoute.REVIEW) }
    var busy by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf<Int?>(null) }
    var message by remember { mutableStateOf("") }
    var task by remember { mutableStateOf<Job?>(null) }
    var elapsed by remember { mutableLongStateOf(0L) }
    var runningCloud by remember { mutableStateOf(false) }
    var processingStarted by remember { mutableLongStateOf(0L) }
    LaunchedEffect(busy) {
        while (busy) {
            elapsed = (SystemClock.elapsedRealtime() - processingStarted).coerceAtLeast(0L)
            delay(1000)
        }
    }
    DisposableEffect(geminiClient) {
        onDispose { task?.cancel(); geminiClient.cancel() }
    }

    val geminiCheckpoint = remember(context, project.id) {
        GeminiTaskCheckpointStore(context, GeminiTaskKind.LEGENDAS, project.id)
    }
    val mediaFingerprint = remember(project.clips, project.overlays, project.audioTracks, project.music) {
        GeminiTaskRecoveryPolicy.digest(buildString {
            project.clips.forEach { append(it.json().toString()) }
            project.overlays.forEach { append(it.json().toString()) }
            project.audioTracks.forEach { append(it.json().toString()) }
            append(project.music?.json()?.toString().orEmpty())
            append(project.durationMs)
        })
    }
    var savedGeminiTask by remember(project.id, mediaFingerprint) { mutableStateOf<GeminiTaskCheckpoint?>(null) }
    LaunchedEffect(geminiCheckpoint, mediaFingerprint) {
        savedGeminiTask = withContext(Dispatchers.IO) {
            runCatching { geminiCheckpoint.load(mediaFingerprint) }.getOrNull()
        }
    }

    val activeCue = results.firstOrNull { it.id == selectedCueId }
    val effectiveRange = if (customRange) requestedRange else full
    fun stop() { task?.cancel(); geminiClient.cancel() }

    fun openEngine(engine: SubtitleEngine) {
        if (busy) return
        message = ""
        open = engine
        showDetails = false
        page = if (lastEngine == engine && results.isNotEmpty()) AutoSubtitleRoute.REVIEW else AutoSubtitleRoute.SETUP
    }

    fun restoreCloud() {
        if (busy) return
        openEngine(SubtitleEngine.GEMINI)
        if (lastEngine == SubtitleEngine.GEMINI && results.isNotEmpty()) return
        busy = true
        page = AutoSubtitleRoute.GENERATING
        runningCloud = false
        processingStarted = SystemClock.elapsedRealtime()
        progress = null
        message = "Recuperando transcrição no aparelho…"
        task = scope.launch {
            try {
                val (saved, subtitles) = withContext(Dispatchers.IO) {
                    val persisted = geminiCheckpoint.load(mediaFingerprint) ?: error("Registro removido.")
                    persisted to geminiCheckpoint.subtitles(persisted)
                }
                val meta = saved.subtitleSource
                results = subtitles
                draftTrack = meta?.trackId?.takeIf { id -> sources.any { it.id == id } }
                draftRange = meta?.let { SubtitleSpeechRange(it.startMs, it.endMs) }
                    ?.takeIf { it.startMs >= 0 && it.endMs <= full.endMs && it.durationMs > 0 } ?: full
                needsReview = meta?.needsReview.orEmpty()
                lastEngine = SubtitleEngine.GEMINI
                replace = false
                voiceDecisions = emptyMap(); voiceNames = emptyMap()
                page = if (subtitles.isEmpty()) AutoSubtitleRoute.SETUP else AutoSubtitleRoute.REVIEW
                message = if (subtitles.isEmpty()) "A transcrição salva não encontrou fala."
                    else "Transcrição recuperada sem nova chamada."
            } catch (cancel: CancellationException) {
                page = AutoSubtitleRoute.SETUP
                throw cancel
            } catch (_: Exception) {
                page = AutoSubtitleRoute.SETUP
                message = "Não foi possível recuperar a transcrição."
            } finally { busy = false; task = null }
        }
    }

    fun generate(engine: SubtitleEngine, range: SubtitleSpeechRange, cue: SubtitleCue? = null,
        align: Boolean = false, retry: Boolean = false) {
        if (busy || range.durationMs <= 0L) return
        val partial = cue != null || retry
        val trackId = if (partial) draftTrack else referenceId
        val track = sources.firstOrNull { it.id == trackId }
        val detect = detectSpeakers && engine == SubtitleEngine.GEMINI && !partial
        val cloud = engine == SubtitleEngine.GEMINI
        val previousReady = savedGeminiTask?.stage == GeminiTaskStage.READY ||
            (lastEngine == SubtitleEngine.GEMINI && results.isNotEmpty())
        val original = results
        val priorReview = needsReview
        if (cloud) cloudIssue = null
        page = AutoSubtitleRoute.GENERATING
        busy = true
        runningCloud = cloud
        progress = null
        elapsed = 0L
        processingStarted = SystemClock.elapsedRealtime()
        message = "Preparando áudio…"
        task = scope.launch {
            val sourceEngine = if (cloud) "gemini" else "vosk"
            DiagnosticRecorder.subtitleGeneration(sourceEngine, "start")
            var recorded = false
            var requestingCloud = false
            var preflight = SubtitleCloudPhase.AUDIO
            try {
                if (trackId != null && track == null) error("A faixa escolhida não existe mais no projeto.")
                val source = prepareSubtitleSpeechSource(context, project, track, range)
                require(source.project.durationMs in 1L..SubtitleSpeechDraft.MAX_OPERATION_MS) {
                    "Escolha um trecho de até 10 minutos."
                }
                val report: suspend (Int?, String) -> Unit = { pct, step ->
                    withContext(Dispatchers.Main.immediate) { progress = pct; message = step }
                }
                val generated = if (cloud) {
                    preflight = SubtitleCloudPhase.KEY
                    val apiKey = withContext(Dispatchers.IO) {
                        geminiStore.read() ?: error("Configure a chave Gemini.")
                    }
                    if (!previousReady) {
                        withContext(Dispatchers.IO) { geminiCheckpoint.start(mediaFingerprint, SUBTITLE_GEMINI_MODEL) }
                        recorded = true
                    }
                    requestingCloud = true
                    geminiClient.transcribe(context, source.project, apiKey, detect, report)
                } else AutoSubtitleEngine.transcribe(context, source.project, report)
                val actual = SubtitleSpeechRange(source.offsetMs, source.offsetMs + source.project.durationMs)
                val placed = SubtitleSpeechDraft.place(generated, source.offsetMs, actual)
                var verifiedAlignment = false
                val combined = when {
                    align && cue != null -> {
                        val revised = SrtSpeechAlignment.align(listOf(cue), placed, project.durationMs)
                        val aligned = revised.cues.singleOrNull()?.takeIf { revised.matched == 1 && actual.contains(it) }
                            ?.takeUnless { candidate -> original.any { other ->
                                other.id != cue.id && other.startMs < candidate.endMs && other.endMs > candidate.startMs
                            } }
                        verifiedAlignment = aligned != null
                        if (aligned == null) original else original.map { if (it.id == cue.id) aligned else it }
                    }
                    partial -> SubtitleSpeechDraft.redo(original, placed, range)
                    else -> placed
                }
                if (partial && combined == original && !verifiedAlignment) {
                    message = if (align) "Nenhuma correção segura pelo áudio. A legenda foi mantida."
                        else "Nenhuma fala nova coube no trecho. As legendas anteriores foram mantidas."
                } else if (!partial && combined.isEmpty()) {
                    message = "Não encontrei fala nesse trecho. Você pode escolher outra fonte ou refazer só um intervalo."
                    if (recorded) {
                        withContext(Dispatchers.IO) { geminiCheckpoint.clear() }
                        savedGeminiTask = null
                    }
                } else {
                    val retained = priorReview.intersect(combined.map { it.id }.toSet())
                    needsReview = when {
                        !partial -> emptySet()
                        align -> retained - cue?.id.orEmpty()
                        else -> retained
                    }
                    results = combined
                    if (!partial) {
                        draftRange = actual; draftTrack = trackId; lastEngine = engine
                        replace = false; voiceDecisions = emptyMap(); voiceNames = emptyMap()
                    }
                    message = if (align) "Sincronia conferida pelo áudio."
                        else if (partial) "Trecho atualizado. As outras legendas foram mantidas."
                        else "${combined.size} legendas geradas. Revise antes de aplicar."
                    if (cloud) {
                        val state = SubtitleCheckpointSource(draftTrack, draftRange.startMs, draftRange.endMs, needsReview)
                        val stored = withContext(Dispatchers.IO) {
                            runCatching { geminiCheckpoint.saveSubtitles(mediaFingerprint, SUBTITLE_GEMINI_MODEL,
                                combined, state) }.isSuccess
                        }
                        if (stored) savedGeminiTask = GeminiTaskCheckpoint(mediaFingerprint, SUBTITLE_GEMINI_MODEL,
                            GeminiTaskStage.READY, System.currentTimeMillis(), subtitleSource = state)
                        else message += " Não foi possível salvar uma cópia de recuperação."
                    }
                }
                DiagnosticRecorder.subtitleGeneration(sourceEngine, "ready", placed.size)
            } catch (cancel: CancellationException) {
                DiagnosticRecorder.subtitleGeneration(sourceEngine, "cancelled")
                if (recorded) {
                    withContext(NonCancellable + Dispatchers.IO) {
                        runCatching { geminiCheckpoint.review(mediaFingerprint, SUBTITLE_GEMINI_MODEL) }
                    }
                    savedGeminiTask = GeminiTaskCheckpoint(mediaFingerprint, SUBTITLE_GEMINI_MODEL,
                        GeminiTaskStage.REVIEW, System.currentTimeMillis())
                }
                message = "Transcrição cancelada. Nada foi aplicado ao projeto."
                throw cancel
            } catch (error: Exception) {
                DiagnosticRecorder.subtitleGeneration(sourceEngine, "failed")
                val issue = if (cloud) SubtitleCloudIssue.from(
                    if (requestingCloud) SubtitleCloudPhase.TRANSCRIBE else preflight, error) else null
                cloudIssue = issue
                if (issue != null && error !is SubtitleCloudException)
                    DiagnosticRecorder.subtitleCloudFailure("transcribe", issue)
                if (recorded) {
                    val review = issue?.needsRecoveryReview == true
                    withContext(Dispatchers.IO) {
                        runCatching {
                            if (review) geminiCheckpoint.review(mediaFingerprint, SUBTITLE_GEMINI_MODEL)
                            else geminiCheckpoint.clear()
                        }
                    }
                    savedGeminiTask = if (review) GeminiTaskCheckpoint(mediaFingerprint, SUBTITLE_GEMINI_MODEL,
                        GeminiTaskStage.REVIEW, System.currentTimeMillis()) else null
                }
                message = if (issue != null) {
                    issue.phase.label + (issue.http?.let { " · HTTP $it" }.orEmpty()) + ". " +
                        issue.message() + (if (issue.needsRecoveryReview) " Pode ter consumido cota." else "")
                } else "Não foi possível transcrever este áudio no aparelho."
            } finally {
                busy = false; task = null
                page = if (results.isEmpty() || lastEngine != engine) AutoSubtitleRoute.SETUP else AutoSubtitleRoute.REVIEW
            }
        }
    }

    fun updateText() {
        val cue = activeCue ?: return
        val corrected = cueText.trim()
        if (corrected.isBlank()) return
        val edit = SubtitleTextRetiming.edit(cue, corrected)
        results = results.map { if (it.id == cue.id) edit.cue else it }
        if (edit.needsAlignment) needsReview = needsReview + cue.id
    }

    fun apply() {
        val replacing = replace && project.subtitles.any(draftRange::contains)
        val retained = if (replacing) project.subtitles.filterNot(draftRange::contains) else project.subtitles
        val candidates = if (replacing) results.filter { cue -> retained.none { other ->
            other.startMs < cue.endMs && other.endMs > cue.startMs
        } } else results
        if (candidates.isEmpty()) {
            message = "As sugestões coincidem com uma legenda que cruza a borda do trecho. Revise antes de aplicar."
            return
        }
        val base = project.copy(subtitles = retained)
        val proposal = GeminiDiarizationPolicy.prepare(base,
            candidates.map { SubtitleAppearancePolicy.generated(it, appearanceMode, generatedSize) },
            false, voiceDecisions, voiceNames)
        if (proposal == null) message = "Revise os personagens antes de aplicar."
        else onApply(proposal.copy(subtitles = proposal.subtitles.sortedBy { it.startMs }))
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Heading("Legendas automáticas", back)
        Text("Como você quer gerar as legendas?", style = MaterialTheme.typography.titleMedium)
        AutoSubtitleEngineCard(SubtitleEngine.VOSK.title,
            if (installed) "Modelo offline PT-BR pronto" else "Modelo PT-BR não instalado",
            Icons.Default.RecordVoiceOver,
            if (lastEngine == SubtitleEngine.VOSK && results.isNotEmpty()) ({ openEngine(SubtitleEngine.VOSK) }) else null) {
            openEngine(SubtitleEngine.VOSK)
        }
        AutoSubtitleEngineCard(SubtitleEngine.GEMINI.title,
            if (geminiConfigured) "Envia somente áudio · API configurada" else "API Gemini não configurada",
            Icons.Default.AutoAwesome,
            if ((lastEngine == SubtitleEngine.GEMINI && results.isNotEmpty()) ||
                savedGeminiTask?.stage == GeminiTaskStage.READY) ::restoreCloud else null) {
            openEngine(SubtitleEngine.GEMINI)
        }
    }

    open?.let { engine ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true,
            confirmValueChange = { value -> value != SheetValue.Hidden || !busy })
        val height = (LocalConfiguration.current.screenHeightDp *
            (if (page == AutoSubtitleRoute.SETUP) .73f else .90f)).dp
        ModalBottomSheet(onDismissRequest = { if (busy) stop() else open = null },
            sheetState = sheetState, shape = UaiShapes.sheet, dragHandle = null) {
            Column(Modifier.fillMaxWidth().heightIn(max = height).imePadding().navigationBarsPadding()
                .padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(engine.title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    if (engine == SubtitleEngine.GEMINI && page == AutoSubtitleRoute.SETUP) {
                        IconButton(onClick = { showDetails = true }) {
                            Icon(Icons.Default.HelpOutline, "Ajuda sobre transcrição Gemini")
                        }
                    }
                    IconButton(onClick = { if (busy) stop() else open = null }) {
                        Icon(Icons.Default.Close, if (busy) "Cancelar" else "Fechar painel")
                    }
                }
                Box(Modifier.weight(1f, fill = false)) {
                    when (page) {
                        AutoSubtitleRoute.SETUP -> Column(Modifier.verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            AutoSubtitleSetup(sources, referenceId, { referenceId = it }, full, effectiveRange,
                                customRange, { customRange = it }, { requestedRange = it })
                            if (engine == SubtitleEngine.VOSK && !installed) {
                                OutlinedButton(onClick = onManageModel) { Text("Baixar modelo PT-BR") }
                            }
                            if (engine == SubtitleEngine.GEMINI) {
                                if (!geminiConfigured) OutlinedButton(onClick = onManageGemini) { Text("Configurar API Gemini") }
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Text("Identificar vozes", modifier = Modifier.weight(1f))
                                    Switch(checked = detectSpeakers, onCheckedChange = { detectSpeakers = it })
                                }
                                if (savedGeminiTask?.stage == GeminiTaskStage.READY && lastEngine != engine)
                                    TextButton(onClick = ::restoreCloud) { Text("Continuar revisão") }
                            }
                            if (message.isNotBlank() && (engine != SubtitleEngine.GEMINI || cloudIssue == null))
                                Text(message, style = MaterialTheme.typography.bodySmall)
                        }
                        AutoSubtitleRoute.GENERATING -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(message)
                            if (runningCloud || progress == null) LinearProgressIndicator(Modifier.fillMaxWidth())
                            else LinearProgressIndicator(progress = { (progress ?: 0).coerceIn(0, 100) / 100f },
                                modifier = Modifier.fillMaxWidth())
                            Text("Tempo decorrido · ${formatMs(elapsed)}", style = MaterialTheme.typography.bodySmall)
                            OutlinedButton(onClick = ::stop) { Text("Cancelar transcrição") }
                        }
                        AutoSubtitleRoute.REVIEW -> AutoSubtitleReviewList(results, needsReview, message,
                            appearanceMode, { next ->
                                val oldDefault = if (appearanceMode == SubtitleAppearanceMode.KARAOKE_ROSA)
                                    SubtitleAppearancePolicy.ROSA_SIZE_SP else SubtitleAppearancePolicy.defaultSize()
                                if (generatedSize == oldDefault)
                                    generatedSize = if (next == SubtitleAppearanceMode.KARAOKE_ROSA)
                                        SubtitleAppearancePolicy.ROSA_SIZE_SP else SubtitleAppearancePolicy.defaultSize()
                                appearanceMode = next
                            }, generatedSize, { generatedSize = it },
                            replace, { replace = it }, project, draftRange, voiceDecisions, voiceNames,
                            onDecision = { id, decision -> voiceDecisions = voiceDecisions + (id to decision) },
                            onName = { id, name -> voiceNames = voiceNames + (id to name) },
                            onNew = { page = AutoSubtitleRoute.SETUP; message = ""; showDetails = false },
                            onRedo = { page = AutoSubtitleRoute.REDO },
                            onEdit = { cue -> selectedCueId = cue.id; cueText = cue.text; page = AutoSubtitleRoute.CUE })
                        AutoSubtitleRoute.CUE -> activeCue?.let { cue ->
                            AutoSubtitleCueReview(project, cue, cueText, { cueText = it },
                                sources.firstOrNull { it.id == draftTrack }, draftRange,
                                engine == SubtitleEngine.GEMINI, cue.id in needsReview, geminiConfigured,
                                onSave = { updateText(); page = AutoSubtitleRoute.REVIEW },
                                onBack = { page = AutoSubtitleRoute.REVIEW },
                                onLips = { updateText(); lipsReturn = AutoSubtitleRoute.CUE; page = AutoSubtitleRoute.LIPS },
                                onRun = { window, align ->
                                    updateText()
                                    val fresh = results.firstOrNull { it.id == cue.id } ?: cue
                                    generate(engine, window, fresh, align)
                                })
                        }
                        AutoSubtitleRoute.REDO -> AutoSubtitleRedoReview(draftRange,
                            engine == SubtitleEngine.GEMINI, { page = AutoSubtitleRoute.REVIEW },
                            { range -> generate(engine, range, retry = true) })
                        AutoSubtitleRoute.LIPS -> activeCue?.let { cue ->
                            SubtitleLipSyncDialog(project.copy(subtitles = results), listOf(cue),
                                { page = lipsReturn }, embedded = true) { changes ->
                                val updates = changes.associateBy { it.before.id }
                                results = results.map { item -> updates[item.id]?.takeIf { it.before == item }?.after ?: item }
                                message = "${changes.size} sugestão aplicada à revisão."
                                page = lipsReturn
                            }
                        }
                    }
                }
                when (page) {
                    AutoSubtitleRoute.SETUP -> {
                        val ready = if (engine == SubtitleEngine.GEMINI) geminiConfigured else installed
                        val candidate = effectiveRange
                        if (engine == SubtitleEngine.GEMINI) {
                            cloudIssue?.let { issue ->
                                Surface(Modifier.fillMaxWidth(), shape = UaiShapes.card,
                                    color = MaterialTheme.colorScheme.errorContainer) {
                                    Column(Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(issue.phase.label + (issue.http?.let { " · HTTP $it" }.orEmpty()),
                                            style = MaterialTheme.typography.labelLarge)
                                        Text(issue.message(), style = MaterialTheme.typography.bodySmall)
                                        Text("Nenhuma legenda alterada.", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                            if (savedGeminiTask?.stage == GeminiTaskStage.REVIEW)
                                Text("A tentativa anterior pode ter consumido cota. Nada foi aplicado.",
                                    style = MaterialTheme.typography.bodySmall)
                            Text("Só envia áudio ao Gemini. Pode consumir cota.",
                                style = MaterialTheme.typography.labelSmall)
                        }
                        Button(onClick = { generate(engine, candidate) }, enabled = ready && !busy &&
                            candidate.durationMs > 0 && (referenceId != null ||
                            candidate.durationMs <= SubtitleSpeechDraft.MAX_OPERATION_MS), modifier = Modifier.fillMaxWidth()) {
                            Text(if (engine == SubtitleEngine.GEMINI) "Enviar e gerar" else "Gerar offline")
                        }
                    }
                    AutoSubtitleRoute.REVIEW -> Button(onClick = ::apply, enabled = results.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth()) { Text("Aplicar ${results.size} legendas") }
                    else -> Unit
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
    if (showDetails && open == SubtitleEngine.GEMINI) AlertDialog(
        onDismissRequest = { showDetails = false },
        title = { Text("Sobre a transcrição com Gemini") },
        text = { Column(Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("O app envia apenas o áudio da fonte e do trecho selecionados. O vídeo não é enviado.")
            Text("Identificar vozes sugere quem falou cada trecho. Revise os personagens antes de aplicar.")
            Text("A consulta pode consumir cota. Tentativas com falha de conexão não serão repetidas automaticamente.")
            GeminiUsageHint("legendas automáticas", durationMs = effectiveRange.durationMs,
                model = SUBTITLE_GEMINI_MODEL)
            if (savedGeminiTask != null) TextButton(onClick = {
                scope.launch {
                    withContext(Dispatchers.IO) { geminiCheckpoint.clear() }
                    savedGeminiTask = null
                    showDetails = false
                }
            }) { Text("Apagar cópia de recuperação") }
        } },
        confirmButton = { TextButton(onClick = { showDetails = false }) { Text("Entendi") } })

}
