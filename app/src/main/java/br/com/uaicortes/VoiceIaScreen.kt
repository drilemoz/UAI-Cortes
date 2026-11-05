package br.com.uaicortes

import android.net.Uri
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import java.io.File
import java.text.Collator
import java.util.Locale

/** Vozes IA: as legendas são fonte somente leitura. Áudio tradicional continua separado. */
@Composable
internal fun VoiceIaScreen(project: VideoProject, back: () -> Unit,
    onSave: (VideoProject) -> Boolean, openGeminiSettings: () -> Unit,
    initialCueId: String? = null) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember(project.id) { VoiceIaDraftStore(context, project.id) }
    val queueStore = remember(project.id) { VoiceIaQueueStore(context, project.id) }
    val credentials = remember(context) { GeminiCredentialStore(context) }
    val api = remember(context) { GeminiTtsClient(context) }
    val pacing = remember(context) { GeminiLocalPacing(context) }
    val favoritesStore = remember(context) { GeminiVoiceFavorites(context) }
    var favoriteVoiceIds by remember(context) { mutableStateOf(favoritesStore.load()) }
    var cachedVoiceNames by remember(context) { mutableStateOf(favoritesStore.names()) }
    val latestProject by rememberUpdatedState(project)
    val cues = remember(project.subtitles) {
        project.subtitles.filter { it.text.isNotBlank() && it.startMs >= 0L &&
            it.startMs < project.durationMs && it.endMs > it.startMs }
            .sortedWith(compareBy(SubtitleCue::startMs, SubtitleCue::endMs))
    }
    var selectedCueId by remember(project.id, initialCueId) {
        mutableStateOf(initialCueId?.takeIf { id -> cues.any { it.id == id } } ?: cues.firstOrNull()?.id)
    }
    val selectedCue = cues.firstOrNull { it.id == selectedCueId } ?: cues.firstOrNull()
    var takes by remember(project.id) { mutableStateOf<List<VoiceIaTake>>(emptyList()) }
    var loaded by remember(project.id) { mutableStateOf(false) }
    var savedQueue by remember(project.id) { mutableStateOf<VoiceIaQueueSnapshot?>(null) }
    var resumeQueueConfirmation by remember { mutableStateOf(false) }
    var retryQueueConfirmation by remember { mutableStateOf(false) }
    var discardQueueConfirmation by remember { mutableStateOf(false) }
    var status by remember(project.id) { mutableStateOf("") }
    var catalog by remember(project.id) { mutableStateOf<List<GeminiVoice>>(emptyList()) }
    var catalogBusy by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var completed by remember { mutableIntStateOf(0) }
    var total by remember { mutableIntStateOf(0) }
    var generatingCueId by remember { mutableStateOf<String?>(null) }
    var generationJob by remember { mutableStateOf<Job?>(null) }
    var voicePickerCueId by remember { mutableStateOf<String?>(null) }
    var voicePickerForAll by remember { mutableStateOf(false) }
    var pendingVoiceForAll by remember { mutableStateOf<String?>(null) }
    var pendingStyleForAll by remember { mutableStateOf<String?>(null) }
    var promptGenerate by remember { mutableStateOf<List<String>?>(null) }
    var forceCueId by remember { mutableStateOf<String?>(null) }
    var pendingInsertId by remember { mutableStateOf<String?>(null) }
    var pendingBatchInsert by remember { mutableStateOf(false) }
    var pendingDeleteId by remember { mutableStateOf<String?>(null) }
    var pendingSyncId by remember { mutableStateOf<String?>(null) }
    var quickGenerateId by remember { mutableStateOf<String?>(null) }
    var defaultVoice by remember(project.id) { mutableStateOf<String?>(null) }
    val voiceOverrides = remember(project.id) { mutableStateMapOf<String, String>() }
    var model by remember(project.id) { mutableStateOf(GeminiModels.LITE) }
    var modelOpen by remember { mutableStateOf(false) }
    var defaultStyle by remember(project.id) { mutableStateOf("Neutra") }
    val styleOverrides = remember(project.id) { mutableStateMapOf<String, String>() }
    var stylePickerCueId by remember { mutableStateOf<String?>(null) }
    var stylePickerForAll by remember { mutableStateOf(false) }
    val wavePlayer = remember(context) { diagnosticPlayer(context) }
    var playingFile by remember { mutableStateOf<String?>(null) }
    DisposableEffect(api, wavePlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (!isPlaying) playingFile = null
            }
        }
        wavePlayer.addListener(listener)
        onDispose {
            generationJob?.cancel()
            api.cancel()
            wavePlayer.removeListener(listener)
            wavePlayer.release()
        }
    }
    LaunchedEffect(store, queueStore) {
        try {
            val recovery = withContext(Dispatchers.IO) {
                val recoveredTakes = store.load()
                var queueWarning = ""
                val restoredQueue = try {
                    queueStore.load()?.let { old ->
                        val repaired = VoiceIaQueuePolicy.recover(old,
                            cues.associate { it.id to VoiceIaPolicy.fingerprint(it) }) { item ->
                            val cue = cues.firstOrNull { it.id == item.cueId }
                            cue != null && recoveredTakes.any { take ->
                                VoiceIaPolicy.matches(take, cue, item.voiceId, item.model, item.style)
                            }
                        }
                        if (repaired != old) queueStore.save(repaired)
                        if (repaired.unfinished) repaired else {
                            queueStore.clear(); null
                        }
                    }
                } catch (_: Exception) {
                    queueWarning = "A fila antiga não pôde ser verificada. Nenhuma chamada foi reiniciada. " +
                        "Os WAVs válidos continuam disponíveis."
                    null
                }
                Triple(recoveredTakes, restoredQueue, queueWarning)
            }
            takes = recovery.first
            savedQueue = recovery.second
            model = takes.firstOrNull()?.model
                ?: project.audioTracks.firstOrNull { it.kind == VoiceIaPolicy.KIND }?.ttsModel
                ?: GeminiModels.LITE
            defaultVoice = takes.firstOrNull()?.voiceId
                ?: project.audioTracks.firstOrNull { it.kind == VoiceIaPolicy.KIND }?.voiceId
            if (recovery.third.isNotBlank()) status = recovery.third
            else if (recovery.second != null) status = "Fila recuperada: " +
                "${recovery.second!!.pending} pendente(s), ${recovery.second!!.review} para revisar. " +
                "Nenhuma chamada Gemini foi iniciada automaticamente."
            loaded = true
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) {
            loaded = true
            status = "Não foi possível ler os rascunhos. Os áudios já inseridos continuam no projeto."
        }
    }
    fun displayVoiceName(id: String?): String = if (id.isNullOrBlank()) "Escolher voz" else {
        val current = catalog.firstOrNull { it.id == id }
        if (current != null) VoiceIaShortLabels.voice(current, id)
        else cachedVoiceNames[id] ?: VoiceIaShortLabels.voice(null, id)
    }
    fun voiceFor(cue: SubtitleCue): String? = voiceOverrides[cue.id]
        ?: takes.firstOrNull { it.cueId == cue.id }?.voiceId
        ?: project.audioTracks.firstOrNull { it.kind == VoiceIaPolicy.KIND && it.subtitleId == cue.id }?.voiceId
        ?: defaultVoice
    fun styleFor(cue: SubtitleCue): String = styleOverrides[cue.id]
        ?: takes.firstOrNull { it.cueId == cue.id }?.style
            ?.let { instruction -> GeminiNarrationStyling.presets.firstOrNull { it.direction == instruction }?.label }
        ?: project.audioTracks.firstOrNull { it.kind == VoiceIaPolicy.KIND && it.subtitleId == cue.id }?.ttsStyle
            ?.let { instruction -> GeminiNarrationStyling.presets.firstOrNull { it.direction == instruction }?.label }
        ?: defaultStyle
    fun readyFor(cue: SubtitleCue): VoiceIaTake? {
        val id = voiceFor(cue) ?: return null
        val direction = GeminiNarrationStyling.instruction(styleFor(cue))
        return takes.firstOrNull { VoiceIaPolicy.matches(it, cue, id, model, direction) }
    }
    fun insert(cue: SubtitleCue, take: VoiceIaTake) {
        val current = latestProject
        if (current.id != project.id || current.subtitles.none {
            it.id == cue.id && VoiceIaPolicy.fingerprint(it) == take.cueFingerprint
        }) { status = "A legenda foi alterada. Revise a geração antes de inserir."; return }
        if (!File(take.filePath).isFile) { status = "Arquivo de voz não encontrado. Gere a fala novamente."; return }
        val changed = runCatching { VoiceIaPolicy.merge(current, cue, take) }.getOrElse {
            status = it.message ?: "A narração ultrapassa a duração do vídeo."; return
        }
        if (onSave(changed)) status = "Voz salva na linha Vozes. Toque na faixa no editor para ajustar ou substituir."
        else status = "Falha ao salvar o projeto. O WAV preparado continua disponível."
    }
    /** Registra cada transição antes da chamada de rede e após salvar o WAV. */
    suspend fun saveQueue(snapshot: VoiceIaQueueSnapshot): VoiceIaQueueSnapshot {
        withContext(Dispatchers.IO) { queueStore.save(snapshot) }
        savedQueue = snapshot
        return snapshot
    }
    fun generate(ids: List<String>, regenerateId: String? = null, quickId: String? = null,
        resumeQueued: Boolean = false) {
        if (busy || !loaded || catalogBusy) return
        val source = savedQueue
        if (!resumeQueued && ids.size > 1 && source?.unfinished == true) {
            status = "Há uma fila anterior. Retome ou descarte a fila antes de iniciar outro lote."
            return
        }
        if (resumeQueued && source == null) return
        val selectedIds = if (resumeQueued) source!!.items.filter {
            it.stage == VoiceIaQueueStage.PENDING
        }.map { it.cueId } else ids
        val chosen = cues.filter { it.id in selectedIds }
        if (chosen.isEmpty()) return
        generationJob = scope.launch {
            busy = true; completed = 0; total = chosen.size
            var journal: VoiceIaQueueSnapshot? = null
            var inflightId: String? = null
            try {
                journal = if (resumeQueued) {
                    val current = source!!
                    val reconciled = VoiceIaQueuePolicy.recover(current,
                        cues.associate { it.id to VoiceIaPolicy.fingerprint(it) }) { item ->
                        val cue = cues.firstOrNull { it.id == item.cueId }
                        cue != null && takes.any { take ->
                            VoiceIaPolicy.matches(take, cue, item.voiceId, item.model, item.style)
                        }
                    }
                    saveQueue(reconciled)
                } else if (chosen.size > 1) {
                    val planned = chosen.map { cue ->
                        val voiceId = voiceFor(cue) ?: error("Selecione a voz de todas as legendas antes do lote.")
                        val direction = GeminiNarrationStyling.instruction(styleFor(cue))
                        val nextStart = cues.firstOrNull { it.startMs > cue.startMs }?.startMs
                        val available = (minOf(cue.endMs, nextStart ?: Long.MAX_VALUE,
                            project.durationMs) - cue.startMs).coerceAtLeast(250L)
                        VoiceIaQueueItem(cue.id, VoiceIaPolicy.fingerprint(cue), voiceId,
                            model, direction, VoiceIaSync.guidance(direction, available, false))
                    }
                    saveQueue(VoiceIaQueuePolicy.create(project.id, planned))
                } else null
                val key = withContext(Dispatchers.IO) { credentials.read() }
                    ?: error("Configure uma chave Gemini em Serviços e APIs.")
                for (group in chosen.chunked(SubtitleNarrationTimeline.MAX_SEGMENTS)) {
                    for (cue in group) {
                        coroutineContext.ensureActive()
                        val task = journal?.items?.firstOrNull { it.cueId == cue.id }
                        if (resumeQueued && task?.stage != VoiceIaQueueStage.PENDING) continue
                        if (task != null && task.cueFingerprint != VoiceIaPolicy.fingerprint(cue)) {
                            journal = saveQueue(VoiceIaQueuePolicy.transition(journal!!, cue.id,
                                VoiceIaQueueStage.STALE))
                            continue
                        }
                        generatingCueId = cue.id
                        val voiceId = task?.voiceId ?: voiceFor(cue)
                            ?: error("Selecione a voz da legenda ${completed + 1}.")
                        val usedModel = task?.model ?: model
                        val direction = task?.style ?: GeminiNarrationStyling.instruction(styleFor(cue))
                        val existing = takes.firstOrNull {
                            cue.id != regenerateId && VoiceIaPolicy.matches(it, cue, voiceId, usedModel, direction)
                        }
                        if (existing == null) {
                            withContext(Dispatchers.IO) {
                                pacing.beforeRequest(usedModel) { waitMs ->
                                    withContext(Dispatchers.Main) {
                                        status = "Aguardando ${((waitMs + 999L) / 1000L)} s: " +
                                            "ritmo RPM configurado. $completed/${chosen.size} concluídas."
                                    }
                                }
                            }
                            coroutineContext.ensureActive()
                            if (journal != null) {
                                journal = saveQueue(VoiceIaQueuePolicy.transition(journal!!,
                                    cue.id, VoiceIaQueueStage.IN_FLIGHT))
                                inflightId = cue.id
                            }
                            status = "Gerando ${completed + 1}/${chosen.size}: ${cue.text.take(50)}"
                            val nextStart = cues.firstOrNull { it.startMs > cue.startMs }?.startMs
                            val available = (minOf(cue.endMs, nextStart ?: Long.MAX_VALUE,
                                project.durationMs) - cue.startMs).coerceAtLeast(250L)
                            val requestStyle = task?.requestStyle
                                ?: VoiceIaSync.guidance(direction, available, quickId == cue.id)
                            val response = api.synthesize(key, usedModel, voiceId, requestStyle, cue.text)
                            val savedFile = withContext(Dispatchers.IO) {
                                coroutineContext.ensureActive()
                                GeminiAudioFiles.save(context, response)
                            }
                            val take = VoiceIaTake(cue.id, VoiceIaPolicy.fingerprint(cue), voiceId,
                                usedModel, direction, savedFile.absolutePath, response.durationMs,
                                GeminiNarrationCheckpoint.hash(response.bytes), requestStyle = requestStyle)
                            val updated = takes.filterNot { it.cueId == cue.id } + take
                            withContext(Dispatchers.IO) { store.save(updated) }
                            takes = updated
                        }
                        if (journal != null) {
                            // O WAV já está íntegro no rascunho antes de concluir a fila.
                            if (journal!!.items.firstOrNull { it.cueId == cue.id }?.stage ==
                                VoiceIaQueueStage.PENDING)
                                journal = saveQueue(VoiceIaQueuePolicy.transition(journal!!,
                                    cue.id, VoiceIaQueueStage.IN_FLIGHT))
                            journal = saveQueue(VoiceIaQueuePolicy.transition(journal!!,
                                cue.id, VoiceIaQueueStage.READY))
                            inflightId = null
                        }
                        completed++
                        generatingCueId = null
                    }
                }
                if (journal != null && !journal!!.unfinished) {
                    withContext(Dispatchers.IO) { queueStore.clear() }
                    savedQueue = null
                }
                status = "${completed} fala(s) preparada(s). Ouça e insira as faixas na timeline." +
                    if (journal?.unfinished == true) " Revise as pendências da fila." else ""
            } catch (e: CancellationException) {
                status = "Geração interrompida. WAVs concluídos preservados. " +
                    "Chamadas em andamento podem ter consumido cota."
                inflightId?.let { id ->
                    journal?.let { old ->
                        withContext(NonCancellable + Dispatchers.IO) {
                            runCatching { queueStore.save(VoiceIaQueuePolicy.transition(old,
                                id, VoiceIaQueueStage.REVIEW)) }
                        }
                        savedQueue = VoiceIaQueuePolicy.transition(old, id, VoiceIaQueueStage.REVIEW)
                    }
                }
            } catch (e: Exception) {
                val id = inflightId
                if (id != null && journal != null) {
                    // 429 é rejeição explícita; outras falhas podem ter consumido cota.
                    val next = if (e is GeminiApiError && e.http == 429)
                        VoiceIaQueueStage.PENDING else VoiceIaQueueStage.REVIEW
                    val updated = VoiceIaQueuePolicy.transition(journal!!, id, next)
                    withContext(Dispatchers.IO) { queueStore.save(updated) }
                    savedQueue = updated
                }
                status = "Parou em ${completed + 1}/${chosen.size}: " +
                    "${e.message?.take(130) ?: "falha local"}. Sem novas tentativas automáticas."
            } finally { busy = false; generationJob = null; generatingCueId = null }
        }
    }
    fun adjustWav(cue: SubtitleCue, take: VoiceIaTake, targetMs: Long) {
        if (busy) return
        scope.launch {
            busy = true
            try {
                val updated = withContext(Dispatchers.IO) {
                    val source = File(take.sourceFilePath ?: take.filePath)
                    val wave = VoiceIaWavStretch.shorten(source.readBytes(), targetMs)
                    val saved = GeminiAudioFiles.save(context, wave)
                    take.copy(filePath = saved.absolutePath, durationMs = wave.durationMs,
                        sha256 = GeminiNarrationCheckpoint.hash(wave.bytes),
                        sourceFilePath = source.absolutePath,
                        sourceDurationMs = take.sourceDurationMs ?: take.durationMs)
                }
                if (takes.firstOrNull { it.cueId == cue.id } != take) {
                    status = "A fala mudou durante o ajuste. O WAV original continua preservado."
                } else {
                    val copy = takes.filterNot { it.cueId == cue.id } + updated
                    withContext(Dispatchers.IO) { store.save(copy) }
                    takes = copy
                    status = "Ajuste salvo em novo WAV (${formatPipMs(updated.durationMs)}). " +
                        "Ouça antes de inserir ou substituir a faixa existente."
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { status = "Não foi possível ajustar localmente: ${e.message?.take(120)}" }
            finally { busy = false }
        }
    }
    fun restoreOriginal(cue: SubtitleCue, take: VoiceIaTake) {
        if (busy || take.sourceFilePath == null) return
        scope.launch {
            busy = true
            try {
                val original = withContext(Dispatchers.IO) {
                    val file = File(take.sourceFilePath)
                    val bytes = file.readBytes()
                    val duration = GeminiWaveParser.duration(bytes, "audio/wav")
                    take.copy(filePath = file.absolutePath, durationMs = duration,
                        sha256 = GeminiNarrationCheckpoint.hash(bytes),
                        sourceFilePath = null, sourceDurationMs = null)
                }
                val updated = takes.filterNot { it.cueId == cue.id } + original
                withContext(Dispatchers.IO) { store.save(updated) }
                takes = updated
                status = "WAV original recuperado. Ouça e toque em Trocar para atualizar a timeline."
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { status = "Não foi possível restaurar o WAV: ${e.message?.take(120)}" }
            finally { busy = false }
        }
    }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = back, enabled = !busy && !catalogBusy) {
                Icon(Icons.Default.ArrowBack, "Voltar")
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Vozes IA", style = MaterialTheme.typography.titleLarge)
                Text("Legendas do projeto · somente leitura", style = MaterialTheme.typography.labelSmall)
            }
            IconButton(onClick = openGeminiSettings, enabled = !busy) {
                Icon(Icons.Default.Settings, "Configurar Gemini")
            }
        }
        if (selectedCue != null) {
            VoiceIaVideoPreview(project, selectedCue,
                onPrevious = { indexOfCue(cues, selectedCue.id).takeIf { it > 0 }
                    ?.let { selectedCueId = cues[it - 1].id } },
                onNext = { indexOfCue(cues, selectedCue.id).takeIf { it in 0 until cues.lastIndex }
                    ?.let { selectedCueId = cues[it + 1].id } })
            VoiceIaCueTimeline(cues, project.durationMs, selectedCue.id) { selectedCueId = it }
        }
        // Uma única barra compacta: mantém as seis ações visíveis sem rolagem horizontal.
        // Surface evita as larguras mínimas dos botões Material3 em telas estreitas.
        val batchPlan = VoiceIaBatchPlanner.plan(cues.map { cue ->
            VoiceIaBatchItem(cue.id, readyFor(cue)?.let { File(it.filePath).isFile } == true)
        })
        val pending = batchPlan.toGenerate
        val preparedCount = batchPlan.reusable
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            VoiceIaToolbarAction(label = "", accessibilityLabel = if (catalogBusy)
                    "Consultando catálogo Gemini" else "Buscar vozes Gemini",
                icon = Icons.Default.AutoAwesome, modifier = Modifier.weight(.64f),
                enabled = !busy && !catalogBusy, onClick = {
                    scope.launch {
                        catalogBusy = true
                        try {
                            val key = withContext(Dispatchers.IO) { credentials.read() }
                                ?: error("Configure uma chave Gemini primeiro.")
                            val result = api.voicesWithDiagnosis(key)
                            catalog = result.voices
                            val names = catalog.associate { it.id to VoiceIaShortLabels.voice(it, it.id) }
                            if (names.isNotEmpty()) {
                                val updatedNames = cachedVoiceNames + names
                                withContext(Dispatchers.IO) { favoritesStore.saveNames(updatedNames) }
                                cachedVoiceNames = updatedNames
                            }
                            status = if (catalog.isEmpty()) "Catálogo sem vozes disponíveis." else
                                "Catálogo Gemini: ${catalog.size} voz(es). Escolha uma para gerar."
                        } catch (e: CancellationException) { throw e }
                        catch (e: Exception) { status = "Consulta de vozes: ${GeminiVoiceCatalog.safeMessage(e)}" }
                        finally { catalogBusy = false }
                    }
                })
            Box(Modifier.weight(.75f)) {
                VoiceIaToolbarAction(label = if (model == GeminiModels.LITE) "Lite" else "Flash",
                    accessibilityLabel = "Modelo de geração: ${if (model == GeminiModels.LITE) "Lite" else "Flash"}",
                    arrow = true, modifier = Modifier.fillMaxWidth(), enabled = !busy,
                    onClick = { modelOpen = true })
                DropdownMenu(expanded = modelOpen, onDismissRequest = { modelOpen = false }) {
                    GeminiModels.supported.forEach { candidate ->
                        DropdownMenuItem(text = { Text(candidate) }, onClick = {
                            model = candidate; modelOpen = false
                        })
                    }
                }
            }
            val allSameVoice = defaultVoice != null && cues.isNotEmpty() &&
                cues.all { voiceFor(it) == defaultVoice }
            val globalVoiceLabel = displayVoiceName(defaultVoice)
            VoiceIaToolbarAction(label = if (allSameVoice) globalVoiceLabel else "Voz", accessibilityLabel =
                    if (allSameVoice) "Voz geral selecionada. Alterar voz para todas as falas"
                    else "Escolher voz geral para todas as falas",
                selected = allSameVoice, modifier = Modifier.weight(1.50f),
                enabled = !busy && !catalogBusy, onClick = { voicePickerForAll = true })
            VoiceIaToolbarAction(label = VoiceIaShortLabels.style(defaultStyle),
                accessibilityLabel = "Interpretação geral: $defaultStyle",
                arrow = true, modifier = Modifier.weight(.85f), enabled = !busy,
                onClick = { stylePickerForAll = true })
            VoiceIaToolbarAction(label = "Gerar $pending", accessibilityLabel =
                    "Gerar vozes pendentes: $pending",
                accent = true, modifier = Modifier.weight(1.02f),
                enabled = !busy && loaded && pending > 0, onClick = {
                    val missingVoice = cues.firstOrNull { voiceFor(it).isNullOrBlank() }
                    if (missingVoice != null) {
                        status = "Escolha Voz geral ou selecione uma voz em cada fala antes de gerar."
                        voicePickerForAll = true
                    } else {
                        forceCueId = null
                        promptGenerate = cues.filter { cue ->
                            readyFor(cue)?.let { File(it.filePath).isFile } != true
                        }.map { it.id }
                    }
                })
            VoiceIaToolbarAction(label = "Inserir $preparedCount", accessibilityLabel =
                    "Inserir todas as vozes prontas: $preparedCount",
                modifier = Modifier.weight(.95f), enabled = !busy && loaded && preparedCount > 0,
                onClick = { pendingBatchInsert = true })
        }
        savedQueue?.takeIf { it.unfinished }?.let { queue ->
            Surface(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 3.dp),
                shape = RoundedCornerShape(9.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                Column(Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Fila salva: ${queue.pending} pendente(s) · ${queue.ready} pronta(s) · " +
                        "${queue.review} a revisar" + if (queue.stale > 0) " · ${queue.stale} alterada(s)" else "",
                        fontSize = 11.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { resumeQueueConfirmation = true },
                            enabled = !busy && loaded && queue.pending > 0) { Text("Retomar") }
                        TextButton(onClick = { retryQueueConfirmation = true },
                            enabled = !busy && queue.review > 0) { Text("Revisar") }
                        TextButton(onClick = { discardQueueConfirmation = true }, enabled = !busy) {
                            Text("Descartar fila")
                        }
                    }
                }
            }
        }
        if (status.isNotBlank()) Text(status, Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        if (generationJob != null && busy) {
            val batchPercent = VoiceIaBatchProgress.percent(completed, total)
            LinearProgressIndicator(progress = { batchPercent / 100f },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("$batchPercent% concluído · $completed/$total falas", modifier = Modifier.weight(1f), fontSize = 12.sp)
                TextButton(onClick = { generationJob?.cancel(); api.cancel() }) { Text("Cancelar") }
            }
        }
        if (busy && generatingCueId != null) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 1.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
                Text("Gerando fala ${completed + 1}/$total · progresso desta chamada indisponível pela API",
                    fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (cues.isEmpty()) {
            Text("Este projeto não possui legendas com texto. Importe ou crie legendas na ferramenta Legendas; " +
                "elas aparecerão aqui automaticamente.", Modifier.padding(20.dp))
        } else {
            HorizontalDivider()
            LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(cues, key = { _, cue -> cue.id }) { index, cue ->
                    val active = selectedCue?.id == cue.id
                    val take = readyFor(cue)
                    val inserted = project.audioTracks.firstOrNull {
                        it.kind == VoiceIaPolicy.KIND && it.subtitleId == cue.id
                    }
                    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                        tonalElevation = if (active) 3.dp else 1.dp,
                        color = if (active) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant) {
                        Column(Modifier.fillMaxWidth().clickable { selectedCueId = cue.id }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("${index + 1}. ${cue.text}", modifier = Modifier.weight(1f),
                                    maxLines = if (active) 3 else 1, overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodyMedium)
                                Icon(if (active) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    "Abrir configurações da fala")
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("${formatPipMs(cue.startMs)} – ${formatPipMs(cue.endMs)}", fontSize = 11.sp)
                                Text(when {
                                    inserted != null -> "Na timeline"
                                    take != null -> "WAV pronto"
                                    else -> "Pendente"
                                }, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                            if (active) {
                                if (busy && generatingCueId == cue.id) {
                                    Row(verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        CircularProgressIndicator(Modifier.size(13.dp), strokeWidth = 2.dp)
                                        Text("Gerando voz...", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                                HorizontalDivider()
                                Row(verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = { voicePickerCueId = cue.id },
                                        enabled = !busy, shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1.45f),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)) {
                                        Text(displayVoiceName(voiceFor(cue)), maxLines = 1,
                                            overflow = TextOverflow.Ellipsis, fontSize = 12.sp)
                                    }
                                    OutlinedButton(onClick = { stylePickerCueId = cue.id }, enabled = !busy,
                                        shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)) {
                                        Text(styleFor(cue), fontSize = 11.sp, maxLines = 1,
                                            overflow = TextOverflow.Ellipsis)
                                    }
                                }
                                if (catalog.isEmpty() && voiceFor(cue).isNullOrBlank())
                                    Text("Busque as vozes Gemini acima.",
                                        style = MaterialTheme.typography.labelSmall)
                                if (take != null) {
                                    val overrunMs = VoiceIaDisplayPolicy.overrunMs(
                                        cue.startMs, cue.endMs, take.durationMs)
                                    val nextCue = cues.firstOrNull { it.startMs > cue.startMs }
                                    val voiceStart = inserted?.startMs ?: cue.startMs
                                    val review = VoiceIaSync.review(voiceStart, cue.endMs,
                                        nextCue?.startMs, project.durationMs, take.durationMs)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(if (review.excessMs > 0)
                                            "WAV ${formatPipMs(take.durationMs)} · +${VoiceIaDisplayPolicy.seconds(review.excessMs)} s fora da janela"
                                        else "WAV ${formatPipMs(take.durationMs)} · sincronizado",
                                            modifier = Modifier.weight(1f),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (review.excessMs > 0L) MaterialTheme.colorScheme.error
                                            else MaterialTheme.colorScheme.onSurfaceVariant)
                                        if (take.sourceFilePath != null) TextButton(onClick = {
                                            restoreOriginal(cue, take)
                                        }, enabled = !busy, contentPadding = PaddingValues(horizontal = 4.dp)) {
                                            Text("Original", fontSize = 11.sp)
                                        }
                                        if (review.excessMs > 0L) TextButton(onClick = {
                                            pendingSyncId = cue.id
                                        }, enabled = !busy, contentPadding = PaddingValues(horizontal = 5.dp)) {
                                            Icon(Icons.Default.Tune, "Ajustar sincronização", Modifier.size(15.dp))
                                            Text("Ajustar", fontSize = 11.sp)
                                        }
                                    }
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Button(onClick = {
                                        if (voiceFor(cue).isNullOrBlank()) voicePickerCueId = cue.id
                                        else {
                                            forceCueId = if (take != null) cue.id else null
                                            promptGenerate = listOf(cue.id)
                                        }
                                    }, enabled = !busy && loaded, shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f), contentPadding = PaddingValues(6.dp)) {
                                        Icon(if (take == null) Icons.Default.AutoAwesome else Icons.Default.Refresh,
                                            contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(Modifier.width(2.dp))
                                        Text(if (take == null) "Gerar" else "Refazer", fontSize = 11.sp)
                                    }
                                    OutlinedButton(onClick = {
                                        if (take == null) return@OutlinedButton
                                        if (playingFile == take.filePath) {
                                            wavePlayer.pause(); playingFile = null
                                        } else {
                                            wavePlayer.setMediaItem(MediaItem.fromUri(Uri.fromFile(File(take.filePath))))
                                            wavePlayer.prepare(); wavePlayer.play()
                                            playingFile = take.filePath
                                        }
                                    }, enabled = take != null && !busy, shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f), contentPadding = PaddingValues(6.dp)) {
                                        Icon(if (playingFile == take?.filePath) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = null, modifier = Modifier.size(15.dp))
                                        Text(if (playingFile == take?.filePath) "Pausa" else "Ouvir", fontSize = 11.sp)
                                    }
                                    Button(onClick = { pendingInsertId = cue.id },
                                        enabled = take != null && !busy, shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1.3f), contentPadding = PaddingValues(6.dp)) {
                                        Text(if (inserted == null) "Inserir" else "Trocar", fontSize = 11.sp)
                                    }
                                }
                                if (inserted != null) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Faixa: ${formatPipMs(inserted.startMs)} · volume ${(inserted.volume * 100).toInt()}%",
                                            style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                                        TextButton(onClick = { pendingDeleteId = cue.id }, enabled = !busy) {
                                            Icon(Icons.Default.DeleteOutline, null, Modifier.size(17.dp))
                                            Text("Excluir")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    if (voicePickerCueId != null || voicePickerForAll) {
        val targetId = voicePickerCueId
        val forAll = voicePickerForAll
        val comparator = Collator.getInstance(Locale.forLanguageTag("pt-BR"))
        // Ordem natural: Atendente 2 vem antes de Atendente 10, sem alterar IDs da API.
        val suffix = Regex("""^(.*?)(\d+)$""")
        val sortedVoices = catalog.sortedWith { a, b ->
            val first = GeminiVoicePresentation.label(a)
            val second = GeminiVoicePresentation.label(b)
            val firstParts = suffix.matchEntire(first)
            val secondParts = suffix.matchEntire(second)
            if (firstParts != null && secondParts != null &&
                comparator.compare(firstParts.groupValues[1], secondParts.groupValues[1]) == 0) {
                (firstParts.groupValues[2].toIntOrNull() ?: 0).compareTo(
                    secondParts.groupValues[2].toIntOrNull() ?: 0)
            } else comparator.compare(first, second)
        }
        var voiceQuery by remember(targetId, forAll) { mutableStateOf("") }
        var genderFilter by remember(targetId, forAll) { mutableStateOf("Todas") }
        var pitchFilter by remember(targetId, forAll) { mutableStateOf("Todos") }
        var favoritesOnly by remember(targetId, forAll) { mutableStateOf(false) }
        val filteredVoices = GeminiVoiceSearch.byPitch(
            GeminiVoiceSearch.byGender(GeminiVoiceSearch.filter(sortedVoices, voiceQuery), genderFilter),
            pitchFilter).filter { !favoritesOnly || it.id in favoriteVoiceIds }
            .sortedWith(compareByDescending<GeminiVoice> { it.id in favoriteVoiceIds })
        val currentId = if (forAll) defaultVoice else
            cues.firstOrNull { it.id == targetId }?.let { voiceFor(it) }
        val closePicker = { voicePickerCueId = null; voicePickerForAll = false }
        Dialog(onDismissRequest = closePicker,
            properties = DialogProperties(usePlatformDefaultWidth = false)) {
            // A largura usa quase toda a tela: nenhuma pílula pode sair pela lateral.
            Surface(modifier = Modifier.fillMaxWidth(0.96f).widthIn(max = 600.dp)
                    .fillMaxHeight(0.83f),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp) {
                Column(Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.RecordVoiceOver, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Escolher voz Gemini", fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.titleMedium)
                            Text(if (forAll) "Aplicar a todas as legendas" else "Voz desta legenda",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp)
                        }
                        IconButton(onClick = closePicker, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Fechar seleção de voz")
                        }
                    }
                    OutlinedTextField(value = voiceQuery, onValueChange = { voiceQuery = it },
                        placeholder = { Text("Buscar voz ou característica", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null,
                            modifier = Modifier.size(19.dp)) },
                        trailingIcon = {
                            if (voiceQuery.isNotBlank()) IconButton(onClick = { voiceQuery = "" }) {
                                Icon(Icons.Default.Close, "Limpar pesquisa", Modifier.size(18.dp))
                            }
                        },
                        singleLine = true, modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        textStyle = MaterialTheme.typography.bodyMedium)
                    Text("GÊNERO", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        listOf("Todas", "Femininas", "Masculinas").forEach { option ->
                            VoiceIaPickerFilter(option, genderFilter == option,
                                Modifier.weight(1f), onClick = { genderFilter = option })
                        }
                    }
                    Text("ALTURA DA VOZ", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("Todos" to "Todas", "Graves" to "Grave",
                            "Médias" to "Média", "Agudas" to "Aguda",
                            "Não informado" to "N/D").forEach { (value, label) ->
                            VoiceIaPickerFilter(label, pitchFilter == value,
                                Modifier.weight(1f), accessibilityLabel = value,
                                onClick = { pitchFilter = value })
                        }
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("${filteredVoices.size} de ${sortedVoices.size} vozes",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(onClick = { favoritesOnly = !favoritesOnly },
                            contentPadding = PaddingValues(horizontal = 5.dp, vertical = 0.dp)) {
                            Icon(Icons.Default.Star, contentDescription = null, Modifier.size(15.dp),
                                tint = if (favoritesOnly) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Favoritas (${favoriteVoiceIds.size})", fontSize = 11.sp)
                        }
                        if (voiceQuery.isNotBlank() || genderFilter != "Todas" || pitchFilter != "Todos" || favoritesOnly) {
                            TextButton(onClick = {
                                voiceQuery = ""; genderFilter = "Todas"; pitchFilter = "Todos"; favoritesOnly = false
                            }, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)) {
                                Text("Limpar filtros", fontSize = 12.sp)
                            }
                        }
                    }
                    HorizontalDivider()
                    if (sortedVoices.isEmpty()) {
                        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("Carregue o catálogo no botão ✨ da tela Vozes IA.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else if (filteredVoices.isEmpty()) {
                        Column(Modifier.weight(1f).fillMaxWidth(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.SearchOff, contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(8.dp))
                            Text("Nenhuma voz encontrada", fontWeight = FontWeight.Medium)
                            Text("Experimente remover um filtro.", fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(),
                            contentPadding = PaddingValues(vertical = 3.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            itemsIndexed(filteredVoices, key = { _, voice -> voice.id }) { _, voice ->
                                val selected = currentId == voice.id
                                Surface(onClick = {
                                    if (forAll) pendingVoiceForAll = voice.id
                                    else if (targetId != null) voiceOverrides[targetId] = voice.id
                                    closePicker()
                                }, modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (selected) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)) {
                                    Row(Modifier.fillMaxWidth().padding(horizontal = 9.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically) {
                                        Box(Modifier.size(34.dp).clip(RoundedCornerShape(9.dp))
                                                .background(MaterialTheme.colorScheme.surface),
                                            contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.GraphicEq, contentDescription = null,
                                                modifier = Modifier.size(20.dp),
                                                tint = MaterialTheme.colorScheme.primary)
                                        }
                                        Spacer(Modifier.width(9.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(VoiceIaShortLabels.voice(voice, voice.id),
                                                fontSize = 14.sp, fontWeight = FontWeight.Medium,
                                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text(GeminiVoicePresentation.shortDetails(voice),
                                                fontSize = 11.sp, lineHeight = 14.sp, maxLines = 2,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                overflow = TextOverflow.Ellipsis)
                                        }
                                        IconButton(onClick = {
                                            val next = GeminiVoiceFavorites.toggle(favoriteVoiceIds, voice.id)
                                            if (next == favoriteVoiceIds) {
                                                status = "Máximo de ${GeminiVoiceFavorites.MAX_FAVORITES} vozes favoritas."
                                            } else if (favoritesStore.save(next)) {
                                                favoriteVoiceIds = next
                                            } else status = "Não foi possível salvar favoritos neste aparelho."
                                        }, modifier = Modifier.size(40.dp)) {
                                            Icon(if (voice.id in favoriteVoiceIds) Icons.Default.Star
                                                else Icons.Default.StarBorder,
                                                contentDescription = if (voice.id in favoriteVoiceIds)
                                                    "Remover ${voice.name} das favoritas"
                                                    else "Adicionar ${voice.name} às favoritas",
                                                tint = if (voice.id in favoriteVoiceIds)
                                                    MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        if (selected) {
                                            Spacer(Modifier.width(4.dp))
                                            Icon(Icons.Default.CheckCircle, "Voz selecionada",
                                                modifier = Modifier.size(19.dp),
                                                tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    HorizontalDivider()
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(if (forAll) "A alteração geral pede confirmação."
                             else "Toque em uma voz para escolher.",
                            fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f))
                        TextButton(onClick = closePicker) { Text("Fechar") }
                    }
                }
            }
        }
    }
    if (stylePickerCueId != null || stylePickerForAll) {
        val targetId = stylePickerCueId
        val forAll = stylePickerForAll
        val currentStyle = if (forAll) defaultStyle else
            cues.firstOrNull { it.id == targetId }?.let { styleFor(it) } ?: defaultStyle
        val comparator = Collator.getInstance(Locale.forLanguageTag("pt-BR"))
        val sortedStyles = GeminiNarrationStyling.presets.sortedWith { a, b ->
            comparator.compare(a.label, b.label)
        }
        AlertDialog(onDismissRequest = { stylePickerCueId = null; stylePickerForAll = false },
            modifier = Modifier.padding(horizontal = 10.dp), shape = RoundedCornerShape(14.dp),
            title = { Text(if (forAll) "Estilo geral" else "Interpretação da fala", fontSize = 18.sp) },
            text = { Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Atual: $currentStyle", style = MaterialTheme.typography.labelSmall)
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 310.dp)) {
                    itemsIndexed(sortedStyles, key = { _, preset -> preset.label }) { _, preset ->
                        Row(Modifier.fillMaxWidth().clickable {
                            if (forAll) pendingStyleForAll = preset.label
                            else if (targetId != null) styleOverrides[targetId] = preset.label
                            stylePickerCueId = null
                            stylePickerForAll = false
                        }.heightIn(min = 42.dp).padding(horizontal = 6.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Text(preset.label, modifier = Modifier.weight(1f), fontSize = 14.sp)
                            if (currentStyle == preset.label)
                                Icon(Icons.Default.Check, "Selecionado", Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.primary)
                        }
                        HorizontalDivider()
                    }
                }
            } }, confirmButton = { TextButton(onClick = {
                stylePickerCueId = null; stylePickerForAll = false
            }) { Text("Fechar") } })
    }
    pendingVoiceForAll?.let { selected ->
        AlertDialog(onDismissRequest = { pendingVoiceForAll = null },
            title = { Text("Usar esta voz em todas as falas?") },
            text = { Text("A seleção substitui as escolhas individuais de voz deste rascunho. " +
                "Legendas e WAVs anteriores não são alterados, mas mudar a voz pode exigir nova geração Gemini.") },
            confirmButton = { TextButton(onClick = {
                defaultVoice = selected
                voiceOverrides.clear()
                cues.forEach { voiceOverrides[it.id] = selected }
                pendingVoiceForAll = null
                status = "Voz geral aplicada às ${cues.size} falas. Nenhuma chamada Gemini foi feita."
            }) { Text("Aplicar a todas") } },
            dismissButton = { TextButton(onClick = { pendingVoiceForAll = null }) { Text("Cancelar") } })
    }
    pendingStyleForAll?.let { selected ->
        AlertDialog(onDismissRequest = { pendingStyleForAll = null },
            title = { Text("Aplicar ${selected} a todas?") },
            text = { Text("Isso substitui os estilos individuais deste rascunho. " +
                "Vozes já geradas permanecem salvas, mas podem precisar de nova geração para usar o estilo escolhido. " +
                "Nenhuma chamada Gemini acontece agora.") },
            confirmButton = { TextButton(onClick = {
                defaultStyle = selected
                styleOverrides.clear()
                cues.forEach { styleOverrides[it.id] = selected }
                pendingStyleForAll = null
                status = "Estilo ${selected} aplicado às ${cues.size} falas. Gere para ouvir o resultado."
            }) { Text("Aplicar a todas") } },
            dismissButton = { TextButton(onClick = { pendingStyleForAll = null }) { Text("Cancelar") } })
    }
    if (resumeQueueConfirmation) {
        val pendingCount = savedQueue?.pending ?: 0
        AlertDialog(onDismissRequest = { resumeQueueConfirmation = false },
            title = { Text("Retomar $pendingCount fala(s) pendente(s)?") },
            text = { Text("Somente solicitações que ainda não foram iniciadas serão enviadas. " +
                "Chamadas de resultado incerto não serão repetidas. A retomada pode consumir cota Gemini.") },
            confirmButton = { TextButton(onClick = {
                resumeQueueConfirmation = false
                generate(savedQueue?.items?.filter { it.stage == VoiceIaQueueStage.PENDING }
                    ?.map { it.cueId }.orEmpty(), resumeQueued = true)
            }) { Text("Retomar") } },
            dismissButton = { TextButton(onClick = { resumeQueueConfirmation = false }) { Text("Cancelar") } })
    }
    if (retryQueueConfirmation) {
        AlertDialog(onDismissRequest = { retryQueueConfirmation = false },
            title = { Text("Liberar chamadas de resultado incerto?") },
            text = { Text("A chamada pode ter sido processada pelo Gemini antes da interrupção. " +
                "Repeti-la pode consumir cota novamente. Esta ação apenas coloca as falas na fila; " +
                "você precisará tocar em Retomar para gerar.") },
            confirmButton = { TextButton(onClick = {
                retryQueueConfirmation = false
                savedQueue?.let { current -> scope.launch {
                    try {
                        saveQueue(VoiceIaQueuePolicy.retryReviewed(current))
                        status = "Chamadas liberadas. Toque em Retomar quando quiser."
                    } catch (_: Exception) { status = "Falha ao salvar revisão da fila." }
                } }
            }) { Text("Liberar para tentativa") } },
            dismissButton = { TextButton(onClick = { retryQueueConfirmation = false }) { Text("Cancelar") } })
    }
    if (discardQueueConfirmation) {
        AlertDialog(onDismissRequest = { discardQueueConfirmation = false },
            title = { Text("Descartar o planejamento da fila?") },
            text = { Text("O planejamento será removido. WAVs já concluídos e a timeline permanecerão intactos. " +
                "Nenhuma geração será iniciada.") },
            confirmButton = { TextButton(onClick = {
                discardQueueConfirmation = false
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) { queueStore.clear() }
                        savedQueue = null
                        status = "Fila descartada; WAVs e legendas preservados."
                    } catch (_: Exception) { status = "Falha ao descartar fila." }
                }
            }) { Text("Descartar") } },
            dismissButton = { TextButton(onClick = { discardQueueConfirmation = false }) { Text("Cancelar") } })
    }
    promptGenerate?.let { ids ->
        val count = ids.size
        AlertDialog(onDismissRequest = { promptGenerate = null },
            title = { Text("Gerar ${if (count == 1) "esta voz" else "$count vozes"}?") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Cada fala exige uma solicitação à API Gemini, que pode consumir cota ou gerar cobrança. " +
                    "Os arquivos são salvos individualmente; falhas não geram novas tentativas automáticas. " +
                    "Nenhuma voz entra na timeline sem a sua confirmação.")
                if (count > 1) {
                    val reusable = cues.count { cue ->
                        cue.id !in ids && readyFor(cue)?.let { File(it.filePath).isFile } == true
                    }
                    Text("Planejamento: $count chamada(s) pendente(s), $reusable WAV(s) já reutilizáveis " +
                        "com as configurações atuais. Cada WAV concluído fica salvo para retomar depois.",
                        style = MaterialTheme.typography.bodySmall)
                }
                Text("O pedido inclui a duração aproximada da legenda; o Gemini pode devolver um WAV mais longo. " +
                    "Nenhuma palavra será removida pelo aplicativo.", style = MaterialTheme.typography.labelSmall)
                GeminiUsageHint("narração", inputChars = cues.filter { it.id in ids }.sumOf { it.text.length },
                    model = model, requestedCount = count)
            } },
            confirmButton = { TextButton(onClick = {
                promptGenerate = null
                val regenerate = forceCueId
                forceCueId = null
                val quick = quickGenerateId
                quickGenerateId = null
                generate(ids, regenerate, quick)
            }) { Text("Gerar") } },
            dismissButton = { TextButton(onClick = { promptGenerate = null; quickGenerateId = null }) { Text("Cancelar") } })
    }
    if (pendingBatchInsert) {
        val validCues = cues.mapNotNull { cue ->
            readyFor(cue)?.takeIf { take ->
                latestProject.subtitles.any { it.id == cue.id && VoiceIaPolicy.fingerprint(it) == take.cueFingerprint } &&
                    File(take.filePath).isFile &&
                    VoiceIaPolicy.fits(latestProject.audioTracks.firstOrNull {
                        it.kind == VoiceIaPolicy.KIND && it.subtitleId == cue.id
                    }?.startMs ?: cue.startMs, take.durationMs, latestProject.durationMs)
            }?.let { cue to it }
        }
        val skipped = cues.size - validCues.size
        val candidateIds = validCues.mapTo(mutableSetOf()) { it.first.id }
        val intervals = validCues.map { (cue, take) ->
            val start = latestProject.audioTracks.firstOrNull {
                it.kind == VoiceIaPolicy.KIND && it.subtitleId == cue.id
            }?.startMs ?: cue.startMs
            start to (start + take.durationMs)
        }
        val otherIntervals = latestProject.audioTracks.filter {
            it.kind == VoiceIaPolicy.KIND && it.subtitleId !in candidateIds
        }.map { it.startMs to (it.startMs + it.durationMs) }
        val overlapCount = intervals.count { (start, end) ->
            (intervals + otherIntervals).count { (secondStart, secondEnd) ->
                secondStart < end && secondEnd > start
            } > 1
        }
        val late = validCues.count { (cue, take) ->
            val start = latestProject.audioTracks.firstOrNull { it.kind == VoiceIaPolicy.KIND &&
                it.subtitleId == cue.id }?.startMs ?: cue.startMs
            start + take.durationMs > cue.endMs
        }
        AlertDialog(onDismissRequest = { pendingBatchInsert = false },
            title = { Text("Inserir ${validCues.size} vozes na timeline?") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("As vozes serão inseridas ou substituídas em uma única gravação do projeto, " +
                    "mantendo as legendas e músicas intactas. Arquivos WAV não serão cortados.")
                if (late > 0) Text("$late fala(s) ultrapassam seu intervalo de legenda.",
                    color = MaterialTheme.colorScheme.error)
                if (overlapCount > 0) Text("$overlapCount voz(es) terão sobreposição com outras vozes. " +
                    "Elas poderão tocar ao mesmo tempo.", color = MaterialTheme.colorScheme.error)
                if (skipped > 0) Text("$skipped legenda(s) não serão inseridas: não foram geradas " +
                    "com as configurações atuais ou ultrapassariam o final do vídeo.",
                    color = MaterialTheme.colorScheme.error)
            } },
            confirmButton = { TextButton(onClick = {
                pendingBatchInsert = false
                var changed = latestProject
                var count = 0
                for ((cue, take) in validCues) {
                    val next = runCatching { VoiceIaPolicy.merge(changed, cue, take) }.getOrNull()
                    if (next != null) { changed = next; count++ }
                }
                status = if (count > 0 && onSave(changed))
                    "$count faixa(s) de voz salvas. Revise a sincronização no editor."
                else "Falha ao salvar: os WAVs preparados continuam disponíveis."
            }, enabled = validCues.isNotEmpty() && !busy) { Text("Inserir faixas") } },
            dismissButton = { TextButton(onClick = { pendingBatchInsert = false }) { Text("Cancelar") } })
    }
    pendingSyncId?.let { id ->
        val cue = cues.firstOrNull { it.id == id }
        val take = cue?.let(::readyFor)
        if (cue == null || take == null) LaunchedEffect(id) { pendingSyncId = null }
        else {
            val installed = project.audioTracks.firstOrNull { it.kind == VoiceIaPolicy.KIND && it.subtitleId == cue.id }
            val startMs = installed?.startMs ?: cue.startMs
            val nextStart = cues.firstOrNull { it.startMs > cue.startMs }?.startMs
            val review = VoiceIaSync.review(startMs, cue.endMs, nextStart,
                project.durationMs, take.durationMs)
            AlertDialog(onDismissRequest = { pendingSyncId = null },
                title = { Text("Sincronizar fala") },
                text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Legenda: ${formatPipMs(review.availableMs)} · WAV: ${formatPipMs(take.durationMs)}")
                    if (review.nextOverlapMs > 0) Text(
                        "A voz invade a próxima legenda em ${formatPipMs(review.nextOverlapMs)}.",
                        color = MaterialTheme.colorScheme.error)
                    if (review.canStretch) Text(
                        "Ajuste offline ${String.format(Locale.ROOT, "%.2f", review.requiredSpeed)}×: " +
                        "preserva a altura aproximada da voz, mas pode criar artefatos. " +
                        "O WAV original fica guardado; ouça antes de inserir.")
                    else Text("Aceleração excessiva prejudicaria a naturalidade. " +
                        "Prefira regenerar com ritmo mais rápido ou editar os horários na tela Legendas.")
                    Text("Os horários e o texto das legendas não serão alterados.",
                        style = MaterialTheme.typography.labelSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (review.canStretch) Button(onClick = {
                            pendingSyncId = null
                            adjustWav(cue, take, review.availableMs)
                        }, modifier = Modifier.weight(1f), enabled = !busy) {
                            Text("Ajustar WAV", fontSize = 12.sp)
                        }
                        OutlinedButton(onClick = {
                            pendingSyncId = null
                            quickGenerateId = cue.id
                            forceCueId = cue.id
                            promptGenerate = listOf(cue.id)
                        }, modifier = Modifier.weight(1f), enabled = !busy) {
                            Text("Regenerar breve", fontSize = 12.sp)
                        }
                    }
                } },
                confirmButton = { TextButton(onClick = { pendingSyncId = null }) { Text("Fechar") } })
        }
    }
    pendingInsertId?.let { id ->
        val cue = cues.firstOrNull { it.id == id }
        val take = cue?.let(::readyFor)
        if (cue == null || take == null) LaunchedEffect(id) { pendingInsertId = null }
        else {
            val installed = project.audioTracks.firstOrNull {
                it.kind == VoiceIaPolicy.KIND && it.subtitleId == cue.id }
            val start = installed?.startMs ?: cue.startMs
            val end = start + take.durationMs
            val clashes = project.audioTracks.any { it.kind == VoiceIaPolicy.KIND &&
                it.subtitleId != cue.id && it.startMs < end && it.startMs + it.durationMs > start }
            val overshoot = end > cue.endMs
            val fits = VoiceIaPolicy.fits(start, take.durationMs, project.durationMs)
            AlertDialog(onDismissRequest = { pendingInsertId = null },
                title = { Text(if (installed == null) "Inserir voz na timeline?" else "Substituir esta voz?") },
                text = { Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("Início: ${formatPipMs(start)} · duração: ${formatPipMs(take.durationMs)}. " +
                        "O áudio original e a legenda permanecem intactos.")
                    if (!fits) Text("O áudio ultrapassa o final do projeto. Ajuste a posição ou gere outra fala.",
                        color = MaterialTheme.colorScheme.error)
                    if (overshoot) Text("A fala ultrapassa o horário final da legenda. A sincronização pode ficar diferente.",
                        color = MaterialTheme.colorScheme.error)
                    if (clashes) Text("Há sobreposição com outra voz. As duas faixas poderão tocar juntas.",
                        color = MaterialTheme.colorScheme.error)
                    if (installed != null) Text("O WAV anterior continuará armazenado; somente a referência da faixa será substituída.")
                } },
                confirmButton = { TextButton(onClick = {
                    pendingInsertId = null
                    insert(cue, take)
                }, enabled = fits) { Text(if (installed == null) "Inserir" else "Substituir") } },
                dismissButton = { TextButton(onClick = { pendingInsertId = null }) { Text("Cancelar") } })
        }
    }
    pendingDeleteId?.let { id ->
        AlertDialog(onDismissRequest = { pendingDeleteId = null },
            title = { Text("Remover voz da timeline?") },
            text = { Text("A faixa será retirada do projeto, sem apagar legendas nem outros áudios. " +
                "O arquivo gerado continuará disponível para inserir novamente.") },
            confirmButton = { TextButton(onClick = {
                pendingDeleteId = null
                val current = latestProject
                if (onSave(current.copy(audioTracks = current.audioTracks.filterNot {
                    it.kind == VoiceIaPolicy.KIND && it.subtitleId == id
                }))) status = "Faixa removida da timeline."
                else status = "Não foi possível salvar a exclusão."
            }) { Text("Excluir faixa") } },
            dismissButton = { TextButton(onClick = { pendingDeleteId = null }) { Text("Cancelar") } })
    }
}

private fun indexOfCue(cues: List<SubtitleCue>, id: String): Int = cues.indexOfFirst { it.id == id }

/** Player não editável: a prévia espelha a legenda sem liberar arrasto nem alteração de texto. */
@Composable
private fun VoiceIaVideoPreview(p: VideoProject, cue: SubtitleCue,
    onPrevious: () -> Unit, onNext: () -> Unit) {
    val context = LocalContext.current
    val player = remember(p.id) { diagnosticPlayer(context) }
    var playing by remember { mutableStateOf(false) }
    var playbackMs by remember { mutableLongStateOf(cue.startMs) }
    DisposableEffect(player) { onDispose { player.release() } }
    LaunchedEffect(player, p.clips.map { Triple(it.uri, it.startMs, it.endMs) }) {
        player.setMediaItems(p.clips.map { clip ->
            ClipMediaItems.playback(clip)
        })
        player.prepare()
    }
    LaunchedEffect(cue.id, cue.startMs) {
        player.pause(); playing = false; playbackMs = cue.startMs
        seekProjectPosition(player, p.clips, cue.startMs)
    }
    LaunchedEffect(playing, player) {
        while (playing) {
            playbackMs = projectPosition(player, p.clips, p.durationMs)
            if (player.playbackState == Player.STATE_ENDED) { playing = false; break }
            delay(120)
        }
    }
    val source = remember(p.clips, playbackMs) {
        TimelinePrecision.sourceAt(p.clips.map { it.uri to it.durationMs }, playbackMs)
    }
    val clip = p.clips.getOrNull(source?.first ?: -1)
    val aspect = remember(clip?.uri) { clip?.let { mediaAspect(context, it.uri, false) } ?: 9f / 16f }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        // Mesma geometria de prévia da ferramenta Legendas: fonte/posição ficam
        // proporcionais ao vídeo, sem alterar os dados originais da legenda.
        InlineVideoPreview(aspect) {
            AndroidView(factory = { ctx -> previewPlayer(ctx).apply {
                this.player = player; useController = false
            } }, update = { it.player = player }, modifier = Modifier.fillMaxSize())
            val visible = if (playing) SubtitlePreviewPolicy.activeAt(p.subtitles, playbackMs) else listOf(cue)
            visible.forEach { selected ->
                key(selected.id) { SubtitleOverlayOnFrame(selected, editable = false, playbackMs = playbackMs) }
            }
        }
        Row(Modifier.height(42.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrevious) { Icon(Icons.Default.ChevronLeft, "Legenda anterior") }
            IconButton(onClick = {
                player.pause(); playing = false; playbackMs = cue.startMs
                seekProjectPosition(player, p.clips, cue.startMs)
            }) { Icon(Icons.Default.SkipPrevious, "Voltar ao início da fala") }
            IconButton(onClick = {
                if (playing) { player.pause(); playing = false }
                else { player.play(); playing = true }
            }, enabled = p.clips.isNotEmpty()) {
                Icon(if (playing) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                    "Reproduzir o vídeo", Modifier.size(29.dp))
            }
            IconButton(onClick = onNext) { Icon(Icons.Default.ChevronRight, "Próxima legenda") }
        }
    }
}

@Composable
private fun VoiceIaCueTimeline(cues: List<SubtitleCue>, durationMs: Long,
    selectedId: String, onSelect: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().height(42.dp).padding(horizontal = 12.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(60.dp).fillMaxHeight().background(Purple, RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center) {
            Text("Vozes IA", color = Color.White, fontSize = 10.sp)
        }
        Spacer(Modifier.width(6.dp))
        BoxWithConstraints(Modifier.weight(1f).fillMaxHeight().background(Purple.copy(alpha = .12f))) {
            val trackWidth = maxWidth
            cues.forEach { cue ->
                val startFraction = (cue.startMs.toFloat() / durationMs.coerceAtLeast(1L)).coerceIn(0f, 1f)
                val endFraction = (cue.endMs.toFloat() / durationMs.coerceAtLeast(1L)).coerceIn(startFraction, 1f)
                val width = (trackWidth * (endFraction - startFraction)).coerceAtLeast(4.dp)
                Box(Modifier.offset(x = trackWidth * startFraction).width(width).fillMaxHeight()
                    .padding(end = 1.dp).clip(RoundedCornerShape(3.dp))
                    .background(Purple.copy(alpha = if (cue.id == selectedId) .95f else .48f))
                    .clickable { onSelect(cue.id) }, contentAlignment = Alignment.Center) {
                    if (width > 28.dp) Text(cue.text, color = Color.White, fontSize = 9.sp,
                        maxLines = 1, overflow = TextOverflow.Clip)
                }
            }
        }
    }
}

/** Controles compactos da barra de Vozes IA: mesma altura e raios discretos. */
@Composable
private fun VoiceIaToolbarAction(label: String, accessibilityLabel: String,
    modifier: Modifier = Modifier, enabled: Boolean = true, accent: Boolean = false,
    icon: ImageVector? = null, arrow: Boolean = false, selected: Boolean = false,
    onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val foreground = when {
        !enabled -> colors.onSurface.copy(alpha = .38f)
        accent -> colors.onPrimary
        else -> colors.primary
    }
    Surface(onClick = onClick, enabled = enabled,
        modifier = modifier.height(40.dp).semantics { contentDescription = accessibilityLabel },
        shape = RoundedCornerShape(8.dp), color = if (accent) colors.primary else colors.surface,
        contentColor = foreground,
        border = if (accent) null else BorderStroke(1.dp, colors.outlineVariant)) {
        Row(Modifier.fillMaxSize().padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) Icon(icon, contentDescription = null, modifier = Modifier.size(17.dp))
            if (label.isNotEmpty()) Text(label, fontSize = 11.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
            if (arrow) Icon(Icons.Default.ArrowDropDown, contentDescription = null,
                modifier = Modifier.size(13.dp))
            if (selected) Icon(Icons.Default.Check, contentDescription = null,
                modifier = Modifier.size(12.dp))
        }
    }
}

/** Rótulos curtos somente na barra; nunca modificam estilo salvo nem prompt Gemini. */
internal object VoiceIaShortLabels {
    fun voice(voice: GeminiVoice?, voiceId: String?): String {
        if (voice == null && voiceId.isNullOrBlank()) return "Escolher voz"
        val raw = voice?.name?.ifBlank { voice.id } ?: voiceId.orEmpty()
        val call = Regex("(?i)^(?:pt[-_]br[-_])?(?:csagent|call[ -]?center[ -]?agent)[ -]?(\\d+)$")
            .matchEntire(raw)
        if (call != null) return "Atendente ${call.groupValues[1]}"
        val podcast = Regex("(?i)^(?:pt[-_]br[-_])?(?:podcaster|podcast[ -]?host)[ -]?(\\d+)$")
            .matchEntire(raw)
        if (podcast != null) return "Podcast ${podcast.groupValues[1]}"
        return voice?.let(GeminiVoicePresentation::label) ?: raw
    }
    fun style(name: String): String = when (name) {
        "Humorística" -> "Humor"
        "Narrador profissional" -> "Narrador"
        "Contadora de histórias" -> "Histórias"
        "Jornalística" -> "Jornal"
        "Conversacional" -> "Conversa"
        "Sussurrada" -> "Sussurro"
        "Publicitária" -> "Anúncio"
        "Sarcástica" -> "Sarcasmo"
        else -> name
    }
}

/** Segmento de filtro: largura dividida igualmente, sem scroll ou corte lateral. */
@Composable
private fun VoiceIaPickerFilter(label: String, selected: Boolean, modifier: Modifier = Modifier,
    accessibilityLabel: String = label, onClick: () -> Unit) {
    val color = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surface
    Surface(onClick = onClick, modifier = modifier.height(40.dp)
            .semantics { contentDescription = "Filtro $accessibilityLabel" },
        shape = RoundedCornerShape(9.dp), color = color,
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outlineVariant)) {
        Box(Modifier.fillMaxSize().padding(horizontal = 2.dp), contentAlignment = Alignment.Center) {
            Text(label, fontSize = 11.sp, maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
        }
    }
}
