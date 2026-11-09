package br.com.uaicortes

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.combinedClickable
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
@OptIn(ExperimentalFoundationApi::class)
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
    val aliasStore = remember(context) { GeminiVoiceAliases(context) }
    var voiceAliases by remember(context) { mutableStateOf<Map<String, String>>(emptyMap()) }
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
    val voiceLibrary = remember(context) { GeminiVoiceLibrary(context) }
    LaunchedEffect(project.id) {
        catalog = withContext(Dispatchers.IO) { voiceLibrary.load().voices }
    }
    var busy by remember { mutableStateOf(false) }
    var completed by remember { mutableIntStateOf(0) }
    var total by remember { mutableIntStateOf(0) }
    var generatingCueId by remember { mutableStateOf<String?>(null) }
    var generationJob by remember { mutableStateOf<Job?>(null) }
    var voicePickerCueId by remember { mutableStateOf<String?>(null) }
    var voicePickerSpeakerId by remember { mutableStateOf<String?>(null) }
    var pendingSpeakerVoice by remember { mutableStateOf<Pair<String, String>?>(null) }
    var pendingSpeakerStyle by remember { mutableStateOf<Pair<String, String>?>(null) }
    var pendingPreviewSpeakerId by remember { mutableStateOf<String?>(null) }
    var previewFile by remember { mutableStateOf<File?>(null) }
    var batchReviewExpanded by remember { mutableStateOf(false) }
    var showSpeakerSettings by remember(project.id) { mutableStateOf(false) }
    var showTechnicalDetails by remember(project.id) { mutableStateOf(false) }
    var showPendingReminder by remember(project.id) { mutableStateOf(false) }
    var reminderChecked by remember(project.id) { mutableStateOf(false) }
    var advancedCueId by remember { mutableStateOf<String?>(null) }
    var showVoiceCloneStudio by remember { mutableStateOf(false) }
    val cloneLibrary = remember(context) { VoiceCloneLibrary(context) }
    var cloneProfiles by remember(context) { mutableStateOf(cloneLibrary.load()) }
    var importCueId by remember { mutableStateOf<String?>(null) }
    var importedUri by remember { mutableStateOf<Uri?>(null) }
    val externalPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) importCueId = null else importedUri = uri
    }
    var speakerFilter by remember(project.id) { mutableStateOf<String?>(null) }
    var speakerFilterOpen by remember { mutableStateOf(false) }
    var cueSearch by remember(project.id) { mutableStateOf("") }
    var searchExpanded by remember { mutableStateOf(false) }
    var voicePickerForAll by remember { mutableStateOf(false) }
    LaunchedEffect(voicePickerCueId, voicePickerForAll, voicePickerSpeakerId) {
        if (voicePickerCueId == null && !voicePickerForAll && voicePickerSpeakerId == null) {
            voiceAliases = withContext(Dispatchers.IO) { aliasStore.load() }
        }
    }
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
    val voiceOverrides = remember(project.id) { mutableStateMapOf<String, String>().apply {
        putAll(project.voiceIaVoices.cueVoices)
    } }
    val speakerVoiceOverrides = remember(project.id) { mutableStateMapOf<String, String>().apply {
        putAll(project.voiceIaVoices.speakerVoices)
    } }
    var model by remember(project.id) { mutableStateOf(GeminiModels.LITE) }
    var modelOpen by remember { mutableStateOf(false) }
    var defaultStyle by remember(project.id) {
        mutableStateOf(project.voiceIaVoices.defaultStyle ?: VoiceIaStylePolicy.DEFAULT)
    }
    var hasExplicitDefaultStyle by remember(project.id) {
        mutableStateOf(project.voiceIaVoices.defaultStyle != null)
    }
    val styleOverrides = remember(project.id) { mutableStateMapOf<String, String>().apply {
        putAll(project.voiceIaVoices.cueStyles)
    } }
    val speakerStyleOverrides = remember(project.id) { mutableStateMapOf<String, String>().apply {
        putAll(project.voiceIaVoices.speakerStyles)
    } }
    var stylePickerCueId by remember { mutableStateOf<String?>(null) }
    var stylePickerSpeakerId by remember { mutableStateOf<String?>(null) }
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
            // Prévia durável pertence à Central; não excluir ao sair.
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
                        val currentVoices = cues.associate { cue ->
                            cue.id to (VoiceIaSpeakerVoicePolicy.preferred(cue,
                                project.subtitleSpeakers, project.voiceIaVoices)
                                ?: recoveredTakes.firstOrNull { it.cueId == cue.id }?.voiceId
                                ?: project.audioTracks.firstOrNull {
                                    it.kind == VoiceIaPolicy.KIND && it.subtitleId == cue.id
                                }?.voiceId)
                        }
                        val currentStyles = cues.associate { cue ->
                            val chosenStyle = VoiceIaStylePolicy.preferred(cue,
                                project.subtitleSpeakers, project.voiceIaVoices)
                                ?: recoveredTakes.firstOrNull { it.cueId == cue.id }?.style?.let {
                                    direction -> GeminiNarrationStyling.presets
                                        .firstOrNull { it.direction == direction }?.label
                                } ?: VoiceIaStylePolicy.DEFAULT
                            cue.id to GeminiNarrationStyling.instruction(chosenStyle)
                        }
                        val safe = VoiceIaQueuePolicy.reconcileSelections(repaired,
                            currentVoices, currentStyles)
                        if (safe != old) queueStore.save(safe)
                        if (safe.unfinished) safe else {
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
                ?: project.audioTracks.firstOrNull { it.kind == VoiceIaPolicy.KIND &&
                    !VoiceIaExternalAudio.isImported(it) }?.ttsModel
                ?: GeminiModels.LITE
            defaultVoice = project.voiceIaVoices.defaultVoiceId ?: takes.firstOrNull()?.voiceId
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
        val custom = voiceAliases[id]?.takeIf { GeminiVoiceAliases.validAlias(it) }
        val current = catalog.firstOrNull { it.id == id }
        custom ?: if (current != null) VoiceIaShortLabels.voice(current, id)
            else cachedVoiceNames[id] ?: VoiceIaShortLabels.voice(null, id)
    }
    fun speakerFor(cue: SubtitleCue): SubtitleSpeaker? =
        project.subtitleSpeakers.firstOrNull { it.id == cue.speakerId }
    fun cloneIdFor(cue: SubtitleCue): String? {
        val speaker = speakerFor(cue)
        return if (speaker != null) project.voiceIaVoices.speakerCloneProfiles[speaker.id]
            else project.voiceIaVoices.cueCloneProfiles[cue.id]
    }
    fun cloneFor(cue: SubtitleCue): VoiceCloneProfile? =
        cloneIdFor(cue)?.let { id -> cloneProfiles.firstOrNull { it.id == id } }
    fun voiceFor(cue: SubtitleCue): String? {
        // Uma fala com personagem exige voz definida nele: não herdar exceções
        // legadas de fala ou voz geral que poderiam trocar a identidade sem aviso.
        val speaker = speakerFor(cue)
        if (speaker != null) return speakerVoiceOverrides[speaker.id]
        return voiceOverrides[cue.id]
            ?: project.voiceIaVoices.defaultVoiceId
            ?: takes.firstOrNull { it.cueId == cue.id }?.voiceId
            ?: project.audioTracks.firstOrNull {
                it.kind == VoiceIaPolicy.KIND && it.subtitleId == cue.id &&
                    !VoiceIaExternalAudio.isImported(it)
            }?.voiceId
            ?: defaultVoice
    }
    fun styleFor(cue: SubtitleCue): String = styleOverrides[cue.id]
        ?: cue.speakerId?.takeIf { id -> project.subtitleSpeakers.any { it.id == id } }
            ?.let(speakerStyleOverrides::get)
        ?: defaultStyle.takeIf { hasExplicitDefaultStyle }
        ?: takes.firstOrNull { it.cueId == cue.id }?.style
            ?.let { instruction -> GeminiNarrationStyling.presets.firstOrNull { it.direction == instruction }?.label }
        ?: project.audioTracks.firstOrNull { it.kind == VoiceIaPolicy.KIND && it.subtitleId == cue.id }?.ttsStyle
            ?.let { instruction -> GeminiNarrationStyling.presets.firstOrNull { it.direction == instruction }?.label }
        ?: defaultStyle
    fun importedFor(cue: SubtitleCue): AudioTrack? =
        VoiceIaExternalAudio.importedFor(project, cue)
    fun readyForFile(cue: SubtitleCue): Boolean {
        if (cloneIdFor(cue) != null) return false
        val id = voiceFor(cue) ?: return false
        return takes.any { VoiceIaPolicy.matches(it, cue, id, model,
            GeminiNarrationStyling.instruction(styleFor(cue))) && File(it.filePath).isFile }
    }
    fun hasPreparedAudio(cue: SubtitleCue): Boolean =
        importedFor(cue) != null || readyForFile(cue)
    // O filtro atua apenas na lista; geração, fila e timeline continuam com todas as falas.
    val filteredCues = remember(cues, speakerFilter, cueSearch, project.subtitleSpeakers) {
        SubtitleSpeakerPolicy.filter(cues, speakerFilter, project.subtitleSpeakers).filter { cue ->
            cueSearch.isBlank() || java.text.Normalizer.normalize(cue.text, java.text.Normalizer.Form.NFD)
                .replace(Regex("\\p{Mn}+"), "").contains(
                    java.text.Normalizer.normalize(cueSearch, java.text.Normalizer.Form.NFD)
                        .replace(Regex("\\p{Mn}+"), ""), ignoreCase = true)
        }
    }
    LaunchedEffect(project.subtitleSpeakers.map { it.id }) {
        if (speakerFilter != null && speakerFilter != "__unassigned__" &&
            project.subtitleSpeakers.none { it.id == speakerFilter }) speakerFilter = null
    }
    LaunchedEffect(speakerFilter, cueSearch) {
        if (filteredCues.isNotEmpty() && filteredCues.none { it.id == selectedCueId })
            selectedCueId = filteredCues.first().id
    }
    // Voz e personagem selecionados são condições de validade da fila, não apenas seu texto.
    LaunchedEffect(project.voiceIaVoices, loaded, defaultStyle, model,
        styleOverrides.toMap(), speakerStyleOverrides.toMap()) {
        val previous = savedQueue
        if (loaded && previous != null && !busy) {
            val updated = VoiceIaQueuePolicy.reconcileSelections(previous,
                cues.associate { it.id to voiceFor(it) },
                cues.associate { it.id to GeminiNarrationStyling.instruction(styleFor(it)) }, model)
            if (updated != previous) {
                withContext(Dispatchers.IO) { queueStore.save(updated) }
                savedQueue = updated
                status = "Voz, interpretação ou modelo alterado: itens antigos foram marcados " +
                    "como desatualizados. Nenhuma API foi chamada."
            }
        }
    }
    fun readyFor(cue: SubtitleCue): VoiceIaTake? {
        val id = voiceFor(cue) ?: return null
        val direction = GeminiNarrationStyling.instruction(styleFor(cue))
        return takes.firstOrNull { VoiceIaPolicy.matches(it, cue, id, model, direction) }
    }
    // Só perguntar ao reabrir um trabalho já iniciado. Uma tela nova sem geração
    // não deve mostrar aviso; carregar WAVs/fila é pré-requisito para decidir.
    LaunchedEffect(loaded, project.id) {
        if (loaded && !reminderChecked) {
            reminderChecked = true
            val ready = cues.count(::hasPreparedAudio)
            val previouslyStarted = takes.isNotEmpty() || savedQueue != null ||
                project.audioTracks.any { it.kind == VoiceIaPolicy.KIND }
            showPendingReminder = VoiceIaCompactPolicy.shouldRemind(
                total = cues.size, ready = ready, previouslyStarted = previouslyStarted)
        }
    }
    fun requestPendingGeneration() {
        val clonePending = cues.firstOrNull { !hasPreparedAudio(it) && cloneIdFor(it) != null }
        if (clonePending != null) {
            selectedCueId = clonePending.id
            showVoiceCloneStudio = true
            status = "Este personagem usa uma referência de voz clonada. Gere pelo Coqui/Chatterbox, sem substituir por Gemini."
            return
        }
        val pendingIds = cues.filterNot(::hasPreparedAudio).map { it.id }
        if (pendingIds.isEmpty()) return
        if (savedQueue?.unfinished == true) {
            // Não sobrescrever uma fila anterior, sobretudo chamada incerta.
            showTechnicalDetails = true
            status = "Revise ou retome a fila salva antes de iniciar novas gerações."
        } else if (cues.any { cue -> cue.id in pendingIds && voiceFor(cue).isNullOrBlank() }) {
            val missing = cues.first { it.id in pendingIds && voiceFor(it).isNullOrBlank() }
            val speaker = speakerFor(missing)
            status = if (speaker == null) "Escolha a voz para a fala sem personagem."
                else "Escolha a voz do personagem ${speaker.name} antes de gerar."
            if (speaker == null) voicePickerCueId = missing.id else {
                showSpeakerSettings = true
                voicePickerSpeakerId = speaker.id
            }
        } else {
            forceCueId = null
            promptGenerate = pendingIds
        }
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
        val chosen = cues.filter { it.id in selectedIds &&
            (it.id == regenerateId || importedFor(it) == null) && cloneIdFor(it) == null }
        if (chosen.isEmpty()) return
        if (!resumeQueued && chosen.any { voiceFor(it).isNullOrBlank() }) {
            status = "Faltam vozes em ${chosen.count { voiceFor(it).isNullOrBlank() }} fala(s) deste lote. " +
                "Configure uma voz geral ou por personagem antes de gerar."
            return
        }
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
                    val safe = VoiceIaQueuePolicy.reconcileSelections(reconciled,
                        cues.associate { it.id to voiceFor(it) },
                        cues.associate { it.id to GeminiNarrationStyling.instruction(styleFor(it)) }, model)
                    saveQueue(safe)
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
                if (resumeQueued && journal?.pending == 0) {
                    status = "A fila não contém solicitações pendentes válidas para a voz atual. " +
                        "Descarte a fila anterior para gerar com as novas escolhas."
                    return@launch
                }
                val key = withContext(Dispatchers.IO) { credentials.read() }
                    ?: error("Configure uma chave Gemini em Serviços e APIs.")
                for (group in chosen.chunked(SubtitleNarrationTimeline.MAX_SEGMENTS)) {
                    for (cue in group) {
                        coroutineContext.ensureActive()
                        val task = journal?.items?.firstOrNull { it.cueId == cue.id }
                        if (resumeQueued && task?.stage != VoiceIaQueueStage.PENDING) continue
                        if (task != null && task.voiceId != voiceFor(cue)) {
                            journal = saveQueue(VoiceIaQueuePolicy.transition(journal!!,
                                cue.id, VoiceIaQueueStage.STALE))
                            continue
                        }
                        if (task != null && (task.style != GeminiNarrationStyling.instruction(styleFor(cue)) ||
                            task.model != model)) {
                            journal = saveQueue(VoiceIaQueuePolicy.transition(journal!!,
                                cue.id, VoiceIaQueueStage.STALE))
                            continue
                        }
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
    fun requestSpeakerBatch(speakerId: String) {
        val chosen = cues.filter { it.speakerId == speakerId && !hasPreparedAudio(it) }
        if (chosen.isEmpty()) {
            status = "Todas as falas deste personagem já têm WAV compatível."
        } else if (chosen.any { voiceFor(it).isNullOrBlank() }) {
            status = "Configure a voz deste personagem antes de gerar ${chosen.size} fala(s)."
            voicePickerSpeakerId = speakerId
        } else {
            promptGenerate = chosen.map { it.id }
        }
    }
    fun previewSpeaker(speakerId: String) {
        val speaker = project.subtitleSpeakers.firstOrNull { it.id == speakerId } ?: return
        if (cues.none { it.speakerId == speakerId }) return
        // A amostra representa a preferência do personagem, não uma exceção de fala.
        val voice = speakerVoiceOverrides[speakerId]
        val chosenStyle = speakerStyleOverrides[speakerId] ?: defaultStyle
        if (voice.isNullOrBlank()) {
            status = "Escolha uma voz para ${speaker.name} antes de ouvir o teste."
            return
        }
        scope.launch {
            busy = true
            try {
                wavePlayer.stop(); playingFile = null
                val direction = GeminiNarrationStyling.instruction(chosenStyle)
                val identity = GeminiPreviewIdentity.id("", model, voice, direction)
                val cache = GeminiVoicePreviewCache(context)
                val previous = withContext(Dispatchers.IO) { cache.find(identity) }
                val file = if (previous != null) previous else {
                    val key = withContext(Dispatchers.IO) { credentials.read() }
                        ?: error("Configure a chave Gemini em Serviços e APIs.")
                    withContext(Dispatchers.IO) { pacing.beforeRequest(model) { } }
                    val sample = api.synthesize(key, model, voice, direction, GeminiPreviewIdentity.TEXT)
                    withContext(Dispatchers.IO) { cache.save(identity, sample.bytes, voice) }
                }
                previewFile = file
                wavePlayer.setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
                wavePlayer.prepare(); wavePlayer.play()
                playingFile = file.absolutePath
                status = if (previous == null) "Teste salvo na Central de Vozes e reproduzido."
                    else "Teste de ${speaker.name} reproduzido do armazenamento local, sem API."
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                status = "Teste de voz não concluído: ${GeminiVoiceCatalog.safeMessage(e)}. " +
                    "Se houve chamada Gemini, ela pode ter consumido cota."
            } finally { busy = false }
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
            val navigationCues = filteredCues.ifEmpty { cues }
            VoiceIaVideoPreview(project, selectedCue,
                onPrevious = { indexOfCue(navigationCues, selectedCue.id).takeIf { it > 0 }
                    ?.let { selectedCueId = navigationCues[it - 1].id } },
                onNext = { indexOfCue(navigationCues, selectedCue.id)
                    .takeIf { it in 0 until navigationCues.lastIndex }
                    ?.let { selectedCueId = navigationCues[it + 1].id } })
            VoiceIaCueTimeline(cues, project.durationMs, selectedCue.id) { id ->
                if (filteredCues.none { it.id == id }) {
                    speakerFilter = null; cueSearch = ""
                }
                selectedCueId = id
            }
        }
        // Visão principal: apenas progresso, três ações e lista de falas.
        val batchPlan = VoiceIaBatchPlanner.plan(cues.map { cue ->
            VoiceIaBatchItem(cue.id, hasPreparedAudio(cue))
        })
        val pending = batchPlan.toGenerate
        val importedCount = cues.count { importedFor(it) != null }
        val preparedCount = cues.count { importedFor(it) == null && readyForFile(it) }
        val readyCount = batchPlan.reusable
        val reviewItems = cues.map { cue ->
            val wav = readyFor(cue)?.takeIf { File(it.filePath).isFile }
            val inserted = project.audioTracks.firstOrNull {
                it.kind == VoiceIaPolicy.KIND && it.subtitleId == cue.id
            }
            val installedOutdated = inserted != null && (
                (voiceFor(cue) != null && inserted.voiceId != voiceFor(cue)) ||
                inserted.ttsStyle != GeminiNarrationStyling.instruction(styleFor(cue)) ||
                inserted.ttsModel != model)
            val imported = importedFor(cue) != null
            val prior = takes.any { it.cueId == cue.id } || inserted != null
            val queueUncertain = savedQueue?.items?.any { it.cueId == cue.id &&
                it.stage == VoiceIaQueueStage.REVIEW } == true
            VoiceIaReviewItem(cue.id, cue.speakerId,
                imported || cloneIdFor(cue) != null || !voiceFor(cue).isNullOrBlank(),
                wav != null || imported,
                (prior && wav == null && !imported) || (installedOutdated && !imported),
                wav?.let { it.durationMs > cue.endMs - cue.startMs } == true,
                queueUncertain)
        }
        val review = VoiceIaBatchReviewPolicy.review(reviewItems)
        Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 3.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("$readyCount/${cues.size} prontas", fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text("$pending faltando", fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                IconButton(onClick = { showTechnicalDetails = !showTechnicalDetails },
                    modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.Info, "${if (showTechnicalDetails) "Ocultar" else "Exibir"} fila e detalhes",
                        Modifier.size(19.dp))
                }
            }
            LinearProgressIndicator(progress = { if (batchPlan.total == 0) 0f
                else readyCount.toFloat() / batchPlan.total },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(3.dp)))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                VoiceIaToolbarAction(label = if (project.subtitleSpeakers.isEmpty()) "Vozes" else "Personagens",
                    accessibilityLabel = if (project.subtitleSpeakers.isEmpty()) "Central de Vozes Google"
                        else "Configurar as vozes dos personagens",
                    icon = Icons.Default.RecordVoiceOver, modifier = Modifier.weight(.92f),
                    enabled = !busy, onClick = {
                        if (project.subtitleSpeakers.isEmpty()) voicePickerForAll = true
                        else showSpeakerSettings = !showSpeakerSettings
                    })
                VoiceIaToolbarAction(label = "Gerar $pending", accessibilityLabel =
                    "Gerar vozes pendentes: $pending", accent = true,
                    modifier = Modifier.weight(1.15f), enabled = !busy && loaded && pending > 0,
                    onClick = { requestPendingGeneration() })
                VoiceIaToolbarAction(label = "Inserir $preparedCount", accessibilityLabel =
                    "Inserir vozes prontas: $preparedCount", modifier = Modifier.weight(1.12f),
                    enabled = !busy && loaded && preparedCount > 0,
                    onClick = { pendingBatchInsert = true })
            }
            TextButton(onClick = { showVoiceCloneStudio = true }, enabled = !busy,
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)) {
                Icon(Icons.Default.GraphicEq, null, Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Clonar voz · Coqui / Chatterbox", fontSize = 11.sp)
            }
            if (importedCount > 0) Text("$importedCount áudio(s) externo(s) ou clonado(s) inserido(s) · " +
                "não serão regenerados pelo lote Gemini.", fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (status.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(status, fontSize = 10.sp, maxLines = if (showTechnicalDetails) 6 else 1,
                        overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (generationJob != null && busy) {
                val batchPercent = VoiceIaBatchProgress.percent(completed, total)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(15.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(6.dp))
                    Text("Gerando $completed/$total ($batchPercent%)", fontSize = 11.sp,
                        modifier = Modifier.weight(1f))
                    TextButton(onClick = { generationJob?.cancel(); api.cancel() }) {
                        Text("Parar", fontSize = 11.sp)
                    }
                }
            }
            if (project.subtitleSpeakers.isNotEmpty()) {
                TextButton(onClick = { showSpeakerSettings = !showSpeakerSettings },
                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
                    modifier = Modifier.height(28.dp)) {
                    Icon(Icons.Default.People, null, Modifier.size(17.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Personagens (${project.subtitleSpeakers.size})", fontSize = 11.sp)
                    Icon(if (showSpeakerSettings) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        "Expandir vozes dos personagens", Modifier.size(17.dp))
                }
                if (showSpeakerSettings) LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    itemsIndexed(project.subtitleSpeakers, key = { _, speaker -> speaker.id }) { _, speaker ->
                        val voiceId = speakerVoiceOverrides[speaker.id]
                        Surface(color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(9.dp)) {
                            Column(Modifier.width(188.dp).padding(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(10.dp).background(Color(speaker.color), RoundedCornerShape(2.dp)))
                                    Spacer(Modifier.width(4.dp))
                                    Text(speaker.name, fontSize = 11.sp, maxLines = 1,
                                        modifier = Modifier.weight(1f), overflow = TextOverflow.Ellipsis)
                                    Text("${cues.count { it.speakerId == speaker.id }}", fontSize = 10.sp)
                                }
                                Text(if (project.voiceIaVoices.speakerCloneProfiles[speaker.id] != null)
                                    "Clonada · ${cloneProfiles.firstOrNull {
                                        it.id == project.voiceIaVoices.speakerCloneProfiles[speaker.id]
                                    }?.name ?: "referência ausente"}"
                                    else displayVoiceName(voiceId), fontSize = 10.sp, maxLines = 1,
                                    overflow = TextOverflow.Ellipsis)
                                Row(horizontalArrangement = Arrangement.SpaceEvenly,
                                    modifier = Modifier.fillMaxWidth()) {
                                    IconButton(onClick = { voicePickerSpeakerId = speaker.id },
                                        enabled = !busy, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Default.RecordVoiceOver, "Voz de ${speaker.name}", Modifier.size(19.dp))
                                    }
                                    IconButton(onClick = { stylePickerSpeakerId = speaker.id },
                                        enabled = !busy, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Default.Mood, "Tom de ${speaker.name}", Modifier.size(19.dp))
                                    }
                                    IconButton(onClick = {
                                        scope.launch {
                                            if (voiceId.isNullOrBlank()) voicePickerSpeakerId = speaker.id
                                            else {
                                                val direction = GeminiNarrationStyling.instruction(
                                                    speakerStyleOverrides[speaker.id] ?: defaultStyle)
                                                val cacheId = GeminiPreviewIdentity.id("", model, voiceId, direction)
                                                val cached = withContext(Dispatchers.IO) {
                                                    GeminiVoicePreviewCache(context).find(cacheId)
                                                }
                                                if (cached != null) previewSpeaker(speaker.id)
                                                else pendingPreviewSpeakerId = speaker.id
                                            }
                                        }
                                    }, enabled = !busy && !voiceId.isNullOrBlank(),
                                        modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Default.PlayCircleOutline, "Testar voz de ${speaker.name}", Modifier.size(19.dp))
                                    }
                                    IconButton(onClick = { requestSpeakerBatch(speaker.id) },
                                        enabled = !busy && loaded, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Default.AutoAwesome, "Gerar falas de ${speaker.name}", Modifier.size(19.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (showTechnicalDetails) {
                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(9.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant) {
                    Column(Modifier.padding(9.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("Revisão · ${review.total} falas · ${review.pending} pendentes · " +
                            "${review.missingVoice} sem voz · ${review.outdated} desatualizadas",
                            fontSize = 11.sp)
                        if (review.syncReview > 0 || review.uncertain > 0) Text(
                            "Sincronização: ${review.syncReview} · Chamada incerta: ${review.uncertain}",
                            fontSize = 11.sp)
                        savedQueue?.takeIf { it.unfinished }?.let { queue ->
                            Text("Fila salva: ${queue.pending} pendentes, ${queue.review} em revisão, " +
                                "${queue.stale} alteradas", fontSize = 11.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                TextButton(onClick = { resumeQueueConfirmation = true },
                                    enabled = !busy && loaded && queue.pending > 0) { Text("Retomar") }
                                TextButton(onClick = { retryQueueConfirmation = true },
                                    enabled = !busy && queue.review > 0) { Text("Revisar") }
                                TextButton(onClick = { discardQueueConfirmation = true },
                                    enabled = !busy) { Text("Descartar fila") }
                            }
                        }
                        val subset = filteredCues.filter { cue ->
                            reviewItems.any { it.cueId == cue.id && it.hasVoice && !it.wavReady }
                        }
                        if (subset.isNotEmpty()) TextButton(onClick = {
                            if (savedQueue?.unfinished == true) status = "Revise a fila antiga primeiro."
                            else promptGenerate = subset.map { it.id }
                        }, enabled = !busy && loaded) {
                            Text("Gerar pendentes do filtro (${subset.size})", fontSize = 11.sp)
                        }
                        Text("Toque numa fala para escolher humor, importar áudio ou inserir na timeline.",
                            fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 9.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text("Falas (${filteredCues.size}/${cues.size})", fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Box {
                TextButton(onClick = { speakerFilterOpen = true }, enabled = !busy) {
                    Icon(Icons.Default.People, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(3.dp))
                    Text(when (speakerFilter) {
                        null -> "Todas"
                        "__unassigned__" -> "Sem personagem"
                        else -> project.subtitleSpeakers.firstOrNull { it.id == speakerFilter }?.name ?: "Todas"
                    }, fontSize = 11.sp, maxLines = 1)
                    Icon(Icons.Default.ArrowDropDown, null, Modifier.size(17.dp))
                }
                DropdownMenu(expanded = speakerFilterOpen,
                    onDismissRequest = { speakerFilterOpen = false }) {
                    (listOf(null, "__unassigned__") + project.subtitleSpeakers.map { it.id }).forEach { id ->
                        DropdownMenuItem(text = { Text(when (id) {
                            null -> "Todas"
                            "__unassigned__" -> "Sem personagem"
                            else -> project.subtitleSpeakers.firstOrNull { it.id == id }?.name ?: "Todas"
                        }) }, onClick = { speakerFilter = id; speakerFilterOpen = false })
                    }
                }
            }
            IconButton(onClick = { searchExpanded = !searchExpanded; if (!searchExpanded) cueSearch = "" },
                modifier = Modifier.size(40.dp)) {
                Icon(if (searchExpanded) Icons.Default.Close else Icons.Default.Search,
                    "Buscar fala", Modifier.size(19.dp))
            }
        }
        if (searchExpanded) OutlinedTextField(value = cueSearch, onValueChange = { cueSearch = it },
            label = { Text("Pesquisar texto da legenda") }, singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp))
        if (cues.isEmpty()) {
            Text("Este projeto não possui legendas com texto. Importe ou crie legendas na ferramenta Legendas; " +
                "elas aparecerão aqui automaticamente.", Modifier.padding(20.dp))
        } else {
            HorizontalDivider()
            if (filteredCues.isEmpty()) Text("Nenhuma fala para o filtro selecionado.",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp)
            LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(filteredCues, key = { _, cue -> cue.id }) { _, cue ->
                    val index = cues.indexOfFirst { it.id == cue.id }
                    val active = selectedCue?.id == cue.id
                    val take = readyFor(cue)
                    val inserted = project.audioTracks.firstOrNull {
                        it.kind == VoiceIaPolicy.KIND && it.subtitleId == cue.id
                    }
                    val wav = take?.takeIf { File(it.filePath).isFile }
                    val character = speakerFor(cue)
                    val imported = importedFor(cue)
                    val isGenerating = generatingCueId == cue.id && busy
                    val insertedCompatible = wav != null && inserted != null &&
                        inserted.voiceId == wav.voiceId && inserted.ttsModel == wav.model &&
                        inserted.ttsStyle == wav.style
                    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(9.dp),
                        tonalElevation = if (active) 2.dp else 0.dp,
                        color = if (active) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant) {
                        Row(Modifier.fillMaxWidth().combinedClickable(
                            onClick = { selectedCueId = cue.id; advancedCueId = cue.id },
                            onLongClickLabel = "Mais ações desta fala",
                            onLongClick = { selectedCueId = cue.id; advancedCueId = cue.id })
                            .padding(horizontal = 7.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("${index + 1}", fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Box(Modifier.size(10.dp).background(
                                character?.let { Color(it.color) }
                                    ?: MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(3.dp)))
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                Text(cue.text, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    fontSize = 12.sp, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal)
                                Text("${character?.name ?: "Sem personagem"} · " + when {
                                    isGenerating -> "Gerando"
                                    imported != null -> if (VoiceCloneAudio.isClone(imported)) "Voz clonada inserida" else "Áudio externo inserido"
                                    insertedCompatible -> "Inserida"
                                    wav != null && inserted != null -> "Atualizar"
                                    wav != null -> "Pronta"
                                    cloneIdFor(cue) != null -> "Clonagem pendente"
                                    voiceFor(cue).isNullOrBlank() -> "Sem voz"
                                    else -> "Pendente"
                                }, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = {
                                selectedCueId = cue.id
                                if (imported != null) {
                                    val source = Uri.parse(imported.uri).path?.let(::File)
                                    if (source != null && source.isFile) {
                                        if (playingFile == source.absolutePath) {
                                            wavePlayer.pause(); playingFile = null
                                        } else {
                                            wavePlayer.setMediaItem(MediaItem.fromUri(Uri.fromFile(source)))
                                            wavePlayer.prepare(); wavePlayer.play()
                                            playingFile = source.absolutePath
                                        }
                                    } else status = "Áudio externo não encontrado. Importe novamente."
                                } else if (wav == null) {
                                    if (cloneIdFor(cue) != null) showVoiceCloneStudio = true
                                    else if (voiceFor(cue).isNullOrBlank()) {
                                        if (character == null) voicePickerCueId = cue.id
                                        else { showSpeakerSettings = true; voicePickerSpeakerId = character.id }
                                    } else { forceCueId = null; promptGenerate = listOf(cue.id) }
                                } else if (playingFile == wav.filePath) {
                                    wavePlayer.pause(); playingFile = null
                                } else {
                                    wavePlayer.setMediaItem(MediaItem.fromUri(Uri.fromFile(File(wav.filePath))))
                                    wavePlayer.prepare(); wavePlayer.play(); playingFile = wav.filePath
                                }
                            }, enabled = !busy && loaded, modifier = Modifier.size(40.dp)) {
                                Icon(when {
                                    isGenerating -> Icons.Default.HourglassEmpty
                                    imported != null -> Icons.Default.PlayArrow
                                    wav == null -> Icons.Default.AutoAwesome
                                    playingFile == wav.filePath -> Icons.Default.Pause
                                    else -> Icons.Default.PlayArrow
                                }, if (wav == null) "Gerar fala ${index + 1}"
                                else if (playingFile == wav.filePath) "Pausar fala ${index + 1}"
                                else "Ouvir fala ${index + 1}", Modifier.size(21.dp))
                            }
                            IconButton(onClick = { selectedCueId = cue.id; stylePickerCueId = cue.id },
                                enabled = !busy, modifier = Modifier.size(40.dp)) {
                                Icon(Icons.Default.Mood, "Alterar tom da fala ${index + 1}: ${styleFor(cue)}",
                                    Modifier.size(21.dp))
                            }
                            if (character == null) IconButton(onClick = {
                                selectedCueId = cue.id; voicePickerCueId = cue.id
                            }, enabled = !busy, modifier = Modifier.size(40.dp)) {
                                Icon(Icons.Default.RecordVoiceOver, "Trocar voz da fala ${index + 1}",
                                    Modifier.size(21.dp))
                            } else IconButton(onClick = {
                                selectedCueId = cue.id; advancedCueId = cue.id
                            }, enabled = !busy, modifier = Modifier.size(40.dp)) {
                                Icon(Icons.Default.MoreVert,
                                    "Opções da fala ${index + 1}, voz do personagem bloqueada",
                                    Modifier.size(21.dp))
                            }
                        }
                    }
                }
            }
        }
    }
    if (showVoiceCloneStudio) VoiceCloneStudioDialog(
        project = project, cue = selectedCue,
        onSave = onSave, onClose = {
            showVoiceCloneStudio = false
            cloneProfiles = cloneLibrary.load()
        })
    advancedCueId?.let { id ->
        val cue = cues.firstOrNull { it.id == id }
        if (cue == null) LaunchedEffect(id) { advancedCueId = null }
        else {
            val take = readyFor(cue)?.takeIf { File(it.filePath).isFile }
            val inserted = project.audioTracks.firstOrNull {
                it.kind == VoiceIaPolicy.KIND && it.subtitleId == cue.id
            }
            val nextCue = cues.firstOrNull { it.startMs > cue.startMs }
            val review = take?.let { VoiceIaSync.review(inserted?.startMs ?: cue.startMs,
                cue.endMs, nextCue?.startMs, project.durationMs, it.durationMs) }
            AlertDialog(onDismissRequest = { advancedCueId = null },
                title = { Text("Fala ${cues.indexOf(cue) + 1} · Opções") },
                text = { Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(cue.text, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    Text("${formatPipMs(cue.startMs)} – ${formatPipMs(cue.endMs)} · " +
                        displayVoiceName(voiceFor(cue)) + " · ${styleFor(cue)}",
                        fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (speakerFor(cue) != null) Text(
                        "Voz definida no personagem. Nesta fala, personalize somente o humor.",
                        fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (importedFor(cue) != null) Text("Áudio importado ou clonado já inserido nesta fala.",
                        fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                    TextButton(onClick = {
                        selectedCueId = cue.id
                        advancedCueId = null
                        showVoiceCloneStudio = true
                    }, enabled = !busy) {
                        Icon(Icons.Default.GraphicEq, null, Modifier.size(18.dp))
                        Text("Clonar voz / gerar com Coqui ou Chatterbox")
                    }
                    TextButton(onClick = { advancedCueId = null; stylePickerCueId = cue.id },
                        enabled = !busy) { Icon(Icons.Default.Mood, null, Modifier.size(18.dp))
                        Text("Alterar humor desta fala") }
                    TextButton(onClick = {
                        advancedCueId = null
                        importCueId = cue.id
                        externalPicker.launch(arrayOf("audio/*"))
                    }, enabled = !busy) {
                        Icon(Icons.Default.FileUpload, null, Modifier.size(18.dp))
                        Text(if (importedFor(cue) != null) "Substituir áudio externo"
                            else "Importar áudio externo")
                    }
                    if (take != null) {
                        Text("WAV ${formatPipMs(take.durationMs)}" +
                            if ((review?.excessMs ?: 0L) > 0L) " · Revisar sincronização" else " · Pronto",
                            fontSize = 11.sp)
                        TextButton(onClick = { advancedCueId = null; pendingInsertId = cue.id },
                            enabled = !busy) {
                            Icon(if (inserted == null) Icons.Default.AddCircleOutline else Icons.Default.SwapHoriz,
                                null, Modifier.size(18.dp))
                            Text(if (inserted == null) "Inserir na timeline" else "Substituir na timeline")
                        }
                        if ((review?.excessMs ?: 0L) > 0L) TextButton(onClick = {
                            advancedCueId = null; pendingSyncId = cue.id
                        }, enabled = !busy) { Text("Ajustar sincronização") }
                        if (take.sourceFilePath != null) TextButton(onClick = {
                            advancedCueId = null; restoreOriginal(cue, take)
                        }, enabled = !busy) { Text("Restaurar WAV original") }
                    }
                    TextButton(onClick = {
                        advancedCueId = null
                        if (cloneIdFor(cue) != null) {
                            selectedCueId = cue.id
                            showVoiceCloneStudio = true
                        } else if (voiceFor(cue).isNullOrBlank()) {
                            val speaker = speakerFor(cue)
                            if (speaker != null) { showSpeakerSettings = true; voicePickerSpeakerId = speaker.id }
                            else voicePickerCueId = cue.id
                        } else { forceCueId = cue.id; promptGenerate = listOf(cue.id) }
                    }, enabled = !busy) { Text(when {
                        importedFor(cue) != null -> "Substituir áudio externo por voz Gemini"
                        take == null -> "Gerar fala"
                        else -> "Refazer voz"
                    }) }
                    if (inserted != null) TextButton(onClick = {
                        advancedCueId = null; pendingDeleteId = cue.id
                    }, enabled = !busy) { Text("Remover faixa da timeline") }
                } },
                confirmButton = { TextButton(onClick = { advancedCueId = null }) { Text("Fechar") } })
        }
    }
    if (voicePickerCueId != null || voicePickerForAll || voicePickerSpeakerId != null) {
        val targetId = voicePickerCueId
        val speakerId = voicePickerSpeakerId
        val forAll = voicePickerForAll
        fun closePicker() {
            voicePickerCueId = null; voicePickerForAll = false; voicePickerSpeakerId = null
        }
        val currentId = if (forAll) defaultVoice else if (speakerId != null)
            speakerVoiceOverrides[speakerId] else cues.firstOrNull { it.id == targetId }?.let { voiceFor(it) }
        Dialog(onDismissRequest = { closePicker() },
            properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(Modifier.fillMaxWidth(.97f).fillMaxHeight(.91f),
                shape = UaiShapes.card, color = MaterialTheme.colorScheme.surface) {
                GeminiVoiceCenterScreen(onBack = { closePicker() }, selectedVoiceId = currentId,
                    initialModel = model, onModelChange = { model = it },
                    styleInstruction = GeminiNarrationStyling.instruction(
                        if (speakerId != null) speakerStyleOverrides[speakerId] ?: defaultStyle
                        else if (targetId != null) cues.firstOrNull { it.id == targetId }
                            ?.let { styleFor(it) } ?: defaultStyle else defaultStyle),
                    onChoose = { voice ->
                        catalog = (catalog + voice).distinctBy { it.id }
                        if (forAll) pendingVoiceForAll = voice.id
                        else if (speakerId != null) pendingSpeakerVoice = speakerId to voice.id
                        else if (targetId != null) {
                            val changed = VoiceIaSpeakerVoicePolicy.cueVoice(latestProject,
                                targetId, voice.id)
                            if (changed != null && onSave(changed)) {
                                voiceOverrides[targetId] = voice.id
                                status = "Voz desta fala aplicada ao rascunho. Salve o projeto ao sair."
                            } else status = "Não foi possível salvar a voz desta fala."
                        }
                        closePicker()
                    })
            }
        }
    }
    if (stylePickerCueId != null || stylePickerForAll || stylePickerSpeakerId != null) {
        val targetId = stylePickerCueId
        val speakerId = stylePickerSpeakerId
        val forAll = stylePickerForAll
        val currentStyle = when {
            forAll -> defaultStyle
            speakerId != null -> speakerStyleOverrides[speakerId] ?: defaultStyle
            else -> cues.firstOrNull { it.id == targetId }?.let { styleFor(it) } ?: defaultStyle
        }
        fun closeStylePicker() {
            stylePickerCueId = null; stylePickerForAll = false; stylePickerSpeakerId = null
        }
        val comparator = Collator.getInstance(Locale.forLanguageTag("pt-BR"))
        val sortedStyles = GeminiNarrationStyling.presets.sortedWith { a, b ->
            comparator.compare(a.label, b.label)
        }
        AlertDialog(onDismissRequest = { closeStylePicker() },
            modifier = Modifier.padding(horizontal = 10.dp), shape = UaiShapes.dialog,
            title = { Text(when {
                forAll -> "Interpretação geral"
                speakerId != null -> "Interpretação do personagem"
                else -> "Interpretação da fala"
            }, fontSize = 18.sp) },
            text = { Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Atual: $currentStyle", style = MaterialTheme.typography.labelSmall)
                if (!forAll && (speakerId != null || targetId != null)) {
                    TextButton(onClick = {
                        val changed = if (speakerId != null)
                            VoiceIaStylePolicy.speakerStyle(latestProject, speakerId, null)
                        else VoiceIaStylePolicy.cueStyle(latestProject, targetId!!, null)
                        if (changed != null && onSave(changed)) {
                            if (speakerId != null) speakerStyleOverrides.remove(speakerId)
                            else if (targetId != null) styleOverrides.remove(targetId)
                            status = "Interpretação individual removida; configuração herdada restaurada."
                        } else status = "Falha ao remover configuração de interpretação."
                        closeStylePicker()
                    }) { Text(if (speakerId != null) "Herdar interpretação geral"
                        else "Herdar interpretação do personagem") }
                }
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 310.dp)) {
                    itemsIndexed(sortedStyles, key = { _, preset -> preset.label }) { _, preset ->
                        Row(Modifier.fillMaxWidth().clickable {
                            if (forAll) pendingStyleForAll = preset.label
                            else if (speakerId != null) pendingSpeakerStyle = speakerId to preset.label
                            else if (targetId != null) {
                                val changed = VoiceIaStylePolicy.cueStyle(latestProject,
                                    targetId, preset.label)
                                if (changed != null && onSave(changed)) {
                                    styleOverrides[targetId] = preset.label
                                    status = "Interpretação desta fala salva no rascunho."
                                } else status = "Falha ao salvar interpretação desta fala."
                            }
                            closeStylePicker()
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
                closeStylePicker()
            }) { Text("Fechar") } })
    }
    pendingSpeakerStyle?.let { (speakerId, style) ->
        val speaker = project.subtitleSpeakers.firstOrNull { it.id == speakerId }
        val affected = cues.count { it.speakerId == speakerId && it.id !in styleOverrides }
        AlertDialog(onDismissRequest = { pendingSpeakerStyle = null },
            title = { Text("Interpretação para ${speaker?.name ?: "personagem"}?") },
            text = { Text("$affected fala(s) passarão a herdar $style. Exceções individuais são " +
                "preservadas e WAVs anteriores não são apagados. Uma nova geração pode consumir cota Gemini. " +
                "Nenhuma chamada será feita ao aplicar.") },
            confirmButton = { TextButton(onClick = {
                val changed = VoiceIaStylePolicy.speakerStyle(latestProject, speakerId, style)
                if (changed != null && onSave(changed)) {
                    speakerStyleOverrides[speakerId] = style
                    status = "Interpretação $style configurada para ${speaker?.name ?: "personagem"}."
                } else status = "Falha ao salvar interpretação do personagem."
                pendingSpeakerStyle = null
            }) { Text("Aplicar") } },
            dismissButton = { TextButton(onClick = { pendingSpeakerStyle = null }) { Text("Cancelar") } })
    }
    pendingPreviewSpeakerId?.let { speakerId ->
        val speaker = project.subtitleSpeakers.firstOrNull { it.id == speakerId }
        val chosenVoice = speakerVoiceOverrides[speakerId]
        val chosenStyle = speakerStyleOverrides[speakerId] ?: defaultStyle
        AlertDialog(onDismissRequest = { pendingPreviewSpeakerId = null },
            title = { Text("Testar voz de ${speaker?.name ?: "personagem"}?") },
            text = { Text("O Gemini produzirá uma amostra curta com a voz " +
                "${displayVoiceName(chosenVoice)} e interpretação $chosenStyle. " +
                "Isso faz UMA chamada de API e pode consumir cota ou gerar cobrança. " +
                "A amostra ficará armazenada na Central de Vozes e não será inserida no projeto.") },
            confirmButton = { TextButton(onClick = {
                pendingPreviewSpeakerId = null
                previewSpeaker(speakerId)
            }, enabled = chosenVoice != null && !busy) { Text("Gerar e ouvir") } },
            dismissButton = { TextButton(onClick = { pendingPreviewSpeakerId = null }) {
                Text("Cancelar")
            } })
    }
    pendingSpeakerVoice?.let { (speakerId, chosenVoice) ->
        val speaker = project.subtitleSpeakers.firstOrNull { it.id == speakerId }
        val affected = cues.count { it.speakerId == speakerId }
        AlertDialog(onDismissRequest = { pendingSpeakerVoice = null },
            title = { Text("Aplicar voz a ${speaker?.name ?: "personagem"}?") },
            text = { Text("$affected fala(s) usarão ${displayVoiceName(chosenVoice)}. " +
                "Escolhas antigas por fala serão removidas, mas os arquivos de áudio não serão apagados. " +
                "O humor individual permanece. Gerar novamente pode consumir cota Gemini.") },
            confirmButton = { TextButton(onClick = {
                val changed = VoiceIaSpeakerVoicePolicy.speakerVoice(latestProject,
                    speakerId, chosenVoice)
                if (changed != null && onSave(changed)) {
                    speakerVoiceOverrides[speakerId] = chosenVoice
                    cues.filter { it.speakerId == speakerId }.forEach { voiceOverrides.remove(it.id) }
                    status = "Voz fixa de ${speaker?.name ?: "personagem"} aplicada a $affected fala(s)."
                } else status = "Não foi possível salvar a voz deste personagem."
                pendingSpeakerVoice = null
            }) { Text("Aplicar voz") } },
            dismissButton = { TextButton(onClick = { pendingSpeakerVoice = null }) {
                Text("Cancelar")
            } })
    }
    pendingVoiceForAll?.let { selected ->
        AlertDialog(onDismissRequest = { pendingVoiceForAll = null },
            title = { Text("Usar esta voz em todas as falas?") },
            text = { Text("A seleção substitui as escolhas individuais de voz deste rascunho. " +
                "A preferência será aplicada ao rascunho do projeto. WAVs antigos permanecem intactos, " +
                "mas novas escolhas podem exigir geração Gemini. Nenhuma API será chamada agora.") },
            confirmButton = { TextButton(onClick = {
                val changed = VoiceIaSpeakerVoicePolicy.all(latestProject, selected)
                if (changed != null && onSave(changed)) {
                    defaultVoice = selected
                    voiceOverrides.clear()
                    speakerVoiceOverrides.clear()
                    pendingVoiceForAll = null
                    status = "Voz geral aplicada às ${cues.size} falas. Salve o projeto. Nenhuma chamada Gemini foi feita."
                } else {
                    pendingVoiceForAll = null
                    status = "Falha ao salvar a escolha de voz geral."
                }
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
                val changed = VoiceIaStylePolicy.all(latestProject, selected)
                if (changed != null && onSave(changed)) {
                    defaultStyle = selected
                    hasExplicitDefaultStyle = true
                    styleOverrides.clear()
                    speakerStyleOverrides.clear()
                    status = "Interpretação $selected aplicada e persistida no rascunho de ${cues.size} falas."
                } else status = "Falha ao salvar interpretação geral."
                pendingStyleForAll = null
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
    if (showPendingReminder && !busy) {
        val missing = cues.count { readyFor(it)?.let { take -> File(take.filePath).isFile } != true }
        if (missing > 0) AlertDialog(onDismissRequest = { showPendingReminder = false },
            title = { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Faltam $missing falas", modifier = Modifier.weight(1f))
                IconButton(onClick = { showPendingReminder = false }) {
                    Icon(Icons.Default.Close, "Fechar lembrete de pendências")
                }
            } },
            text = { Text(if (savedQueue?.unfinished == true)
                "Há uma fila salva. Deseja revisar as pendências antes de continuar?"
                else "Deseja gerar as vozes restantes? Os áudios prontos serão reutilizados.") },
            confirmButton = { TextButton(onClick = {
                showPendingReminder = false
                requestPendingGeneration()
            }) { Text(if (savedQueue?.unfinished == true) "Ver fila" else "Gerar $missing") } },
            dismissButton = { TextButton(onClick = { showPendingReminder = false }) { Text("Depois") } })
        else LaunchedEffect(missing) { showPendingReminder = false }
    }
    importedUri?.let { uri ->
        val cueId = importCueId
        val cue = cues.firstOrNull { it.id == cueId }
        AlertDialog(onDismissRequest = { importedUri = null; importCueId = null },
            shape = UaiShapes.dialog,
            title = { Text("Importar áudio para esta fala?") },
            text = { Text("O Uai Cortes copiará o arquivo para uma pasta privada, confirmará " +
                "o formato e a duração e substituirá somente a faixa de áudio desta fala. " +
                "Aceita WAV, MP3, M4A, AAC ou OGG até 64 MiB. " +
                "Não altera a legenda, as outras falas, nem consome API Gemini. " +
                "O arquivo anterior não será apagado automaticamente.") },
            confirmButton = { TextButton(enabled = !busy && cue != null, onClick = {
                importedUri = null; importCueId = null
                if (cue != null) scope.launch {
                    busy = true
                    try {
                        val current = latestProject
                        require(current.id == project.id) { "O projeto foi alterado." }
                        val (track, file) = VoiceIaExternalAudio.prepare(context, uri, current, cue)
                        val updated = current.copy(audioTracks = current.audioTracks.filterNot {
                            it.kind == VoiceIaPolicy.KIND && it.subtitleId == cue.id
                        } + track)
                        if (!runCatching { onSave(updated) }.getOrDefault(false)) {
                            file.delete()
                            status = "Não foi possível salvar a faixa. Nenhum áudio foi substituído."
                        } else status = "Áudio externo inserido somente na fala ${cues.indexOf(cue) + 1}."
                    } catch (e: Exception) {
                        status = "Importação não concluída: ${e.message?.take(140) ?: "arquivo inválido"}"
                    } finally { busy = false }
                }
            }) { Text("Importar") } },
            dismissButton = { TextButton(onClick = { importedUri = null; importCueId = null }) {
                Text("Cancelar")
            } })
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
                    Text("Planejamento: até $count chamada(s) Gemini, $reusable WAV(s) já reutilizáveis " +
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
                val cloned = cues.firstOrNull { it.id in ids && cloneIdFor(it) != null }
                if (cloned != null) {
                    selectedCueId = cloned.id
                    showVoiceCloneStudio = true
                    status = "Gere esta voz vinculada usando Coqui ou Chatterbox."
                } else generate(ids, regenerate, quick)
            }) { Text("Gerar") } },
            dismissButton = { TextButton(onClick = { promptGenerate = null; quickGenerateId = null }) { Text("Cancelar") } })
    }
    if (pendingBatchInsert) {
        val validCues = cues.mapNotNull { cue ->
            if (importedFor(cue) != null) return@mapNotNull null
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
                        }, modifier = Modifier.weight(1f), enabled = !busy, shape = UaiShapes.button) {
                            Text("Ajustar WAV", fontSize = 12.sp)
                        }
                        OutlinedButton(onClick = {
                            pendingSyncId = null
                            quickGenerateId = cue.id
                            forceCueId = cue.id
                            promptGenerate = listOf(cue.id)
                        }, modifier = Modifier.weight(1f), enabled = !busy, shape = UaiShapes.button) {
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
    var previewExpanded by remember(p.id) { mutableStateOf(false) }
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
        InlineVideoPreview(aspect, maxHeightDp = if (previewExpanded) null else 210f) {
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
            IconButton(onClick = { previewExpanded = !previewExpanded }) {
                Icon(if (previewExpanded) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                    if (previewExpanded) "Recolher prévia" else "Ampliar prévia")
            }
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
