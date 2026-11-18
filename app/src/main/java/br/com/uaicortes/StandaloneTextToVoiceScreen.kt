package br.com.uaicortes

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.text.DateFormat
import java.util.Date

/** Rascunho apenas em memória, preservado ao visitar Ajustes sem gravar texto em disco. */
internal class StandaloneVoiceDraft {
    val text = mutableStateOf("")
    val model = mutableStateOf(GeminiModels.LITE)
    val voice = mutableStateOf<GeminiVoice?>(null)
    val style = mutableStateOf(VoiceStudioPlan.AUTO)
    val mode = mutableStateOf(VoiceStudioMode.CONTINUOUS)
    val tone = mutableStateOf<Pair<String, String>?>(null)
    val frozenWindows = mutableStateOf<List<Long>>(emptyList())
    val language = mutableStateOf(GeminiSpeechLanguage.PORTUGUESE_BR)
    val generated = mutableStateOf<File?>(null)
    val feedback = mutableStateOf("")
    val apiUsage = mutableStateOf("")
}

/** Estúdio autônomo: só insere na timeline após confirmação explícita do usuário. */
@Composable
internal fun StandaloneTextToVoiceScreen(onBack: () -> Unit, onOpenGeminiSettings: () -> Unit,
    draft: StandaloneVoiceDraft, projectClipWindows: List<Long> = emptyList(), onAddToEditor: (suspend (File) -> Boolean)? = null) {
    val ctx = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val client = remember(ctx) { GeminiTtsClient(ctx) }
    val textClient = remember(ctx) { GeminiVideoCaptionClient(ctx) }
    val recovery = rememberGeminiRecovery()
    val batchDraft = remember(ctx) { StandaloneVoiceDraftStore(ctx, batch = true) }
    val textPrefs = remember(ctx) { ctx.getSharedPreferences("voice_studio_text", android.content.Context.MODE_PRIVATE) }
    var textModel by remember {
        mutableStateOf(textPrefs.getString("model", null)?.takeIf(GeminiCaptionModelPolicy::validId).orEmpty())
    }
    var textModels by remember { mutableStateOf<List<GeminiCaptionModel>>(emptyList()) }
    var modelChooser by remember { mutableStateOf(false) }
    var scriptAction by remember { mutableStateOf<VoiceStudioTextAction?>(null) }
    var proposedText by remember { mutableStateOf<String?>(null) }
    var proposedTone by remember { mutableStateOf<VoiceStudioTone?>(null) }
    var configured by remember { mutableStateOf(false) }
    var storedBatchAvailable by remember { mutableStateOf(false) }
    var restoreBatch by remember { mutableStateOf(false) }
    var sceneWork by remember { mutableStateOf(false) }
    var sceneReview by remember { mutableStateOf(false) }
    var previewRequested by remember { mutableStateOf(false) }
    var retryScene by remember { mutableStateOf<Int?>(null) }
    var playedFile by remember { mutableStateOf<File?>(null) }
    var shortPreview by remember { mutableStateOf<File?>(null) }
    val credentials = remember(ctx) { GeminiCredentialStore(ctx) }
    val pacing = remember(ctx) { GeminiLocalPacing(ctx) }
    val library = remember(ctx) { StandaloneVoiceLibrary(ctx) }
    val draftStore = remember(ctx) { StandaloneVoiceDraftStore(ctx) }
    val ledger = remember(ctx) { GeminiUsageLedger(ctx) }
    val player = remember(ctx) { diagnosticPlayer(ctx) }
    var playing by remember { mutableStateOf(false) }
    var job by remember { mutableStateOf<Job?>(null) }
    var busy by remember { mutableStateOf(false) }
    var stage by remember { mutableStateOf("") }
    var text by draft.text
    var model by draft.model
    var modelMenu by remember { mutableStateOf(false) }
    var voice by draft.voice
    var chooseVoice by remember { mutableStateOf(false) }
    var localVoiceAliases by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    LaunchedEffect(chooseVoice, voice?.id) {
        if (!chooseVoice) localVoiceAliases = withContext(Dispatchers.IO) { GeminiVoiceAliases(ctx).load() }
    }
    var style by draft.style
    var mode by draft.mode
    var selectedTone by draft.tone
    var frozenWindows by draft.frozenWindows
    var language by draft.language
    var styleMenu by remember { mutableStateOf(false) }
    var technicalExpanded by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf(false) }
    var confirmExit by remember { mutableStateOf(false) }
    var generated by draft.generated
    var savedFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var audioInfo by remember { mutableStateOf<Map<String, StandaloneVoiceLibrary.Info>>(emptyMap()) }
    var exportFile by remember { mutableStateOf<File?>(null) }
    var deleteFile by remember { mutableStateOf<File?>(null) }
    var renameFile by remember { mutableStateOf<File?>(null) }
    var audioMenuFile by remember { mutableStateOf<File?>(null) }
    var renameText by remember { mutableStateOf("") }
    var addToEditorFile by remember { mutableStateOf<File?>(null) }
    var showAllSaved by remember { mutableStateOf(false) }
    var favoritesOnly by remember { mutableStateOf(false) }
    var storedDraftAvailable by remember { mutableStateOf(false) }
    var feedback by draft.feedback
    var apiUsage by draft.apiUsage

    suspend fun reloadLibrary() {
        val pair = withContext(Dispatchers.IO) {
            val files = library.list()
            files to files.mapNotNull { file -> library.info(file)?.let { file.absolutePath to it } }.toMap()
        }
        savedFiles = pair.first
        audioInfo = pair.second
    }
    LaunchedEffect(library) {
        reloadLibrary()
        storedDraftAvailable = withContext(Dispatchers.IO) { draftStore.exists() }
        storedBatchAvailable = withContext(Dispatchers.IO) { batchDraft.exists() }
        configured = withContext(Dispatchers.IO) { !credentials.read().isNullOrBlank() }
    }
    DisposableEffect(client, player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) { playing = isPlaying }
        }
        player.addListener(listener)
        onDispose {
            job?.cancel(); client.cancel(); textClient.cancel(); player.removeListener(listener); player.release()
            shortPreview?.delete()
        }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("audio/wav")) { uri ->
        val file = exportFile
        exportFile = null
        if (uri != null && file != null) scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    if (!file.isFile) throw IOException("Áudio não encontrado.")
                    ctx.contentResolver.openOutputStream(uri, "wt")?.use { out ->
                        file.inputStream().use { it.copyTo(out) }
                        out.flush()
                    } ?: throw IOException("Destino indisponível.")
                }
                feedback = "Áudio WAV salvo no local escolhido."
            } catch (_: Exception) { feedback = "Falha ao exportar WAV. O original continua salvo no aplicativo." }
        }
    }
    fun play(file: File, select: Boolean) {
        if (!file.isFile) { feedback = "Áudio não encontrado no aparelho."; return }
        if (playing && playedFile == file) { player.pause(); return }
        player.setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
        player.prepare()
        player.play()
        playedFile = file
        if (select) generated = file
    }
    fun playFile(file: File) = play(file, true)
    fun saveExternally(file: File) {
        exportFile = file
        val title = audioInfo[file.absolutePath]?.title.orEmpty()
            .replace(Regex("[^\\p{L}\\p{N} _-]"), "").trim().take(50)
            .ifBlank { "UaiCortes-voz" }
        picker.launch("$title.wav")
    }

    val tone = remember(text, selectedTone) {
        VoiceStudioPlan.tones.firstOrNull { selectedTone?.first == text && it.preset == selectedTone?.second }
            ?: VoiceStudioPlan.suggest(text)
    }
    val interpretation = if (style == VoiceStudioPlan.AUTO)
        GeminiNarrationStyling.instruction(tone.preset) + ". " + tone.guidance
        else GeminiNarrationStyling.instruction(style)
    val projectWindows = frozenWindows.ifEmpty { projectClipWindows }
    val scenes = remember(text, mode, projectWindows, style, tone) {
        VoiceStudioPlan.scenes(text, mode, projectWindows, style, tone)
    }
    val batch = remember(text, model, voice?.id, interpretation, language, scenes) {
        VoiceStudioBatch(ctx.filesDir, VoiceStudioBatch.identity(text, model, voice?.id.orEmpty(),
            interpretation, language, scenes))
    }
    var ready by remember(batch) { mutableStateOf<List<Pair<File, Long>?>>(emptyList()) }
    LaunchedEffect(batch, busy) {
        if (!busy && scenes.isNotEmpty()) ready = withContext(Dispatchers.IO) { batch.completed(scenes) }
    }

    fun snapshot() = StandaloneVoiceDraftStore.Snapshot(text, model, voice, style, language, mode,
        tone.preset, if (mode == VoiceStudioMode.PROJECT) projectWindows.take(VoiceStudioPlan.MAX_SCENES) else emptyList())

    fun restore(value: StandaloneVoiceDraftStore.Snapshot) {
        text = value.text; model = value.model; voice = value.voice
        style = value.style; language = value.language; mode = value.mode
        selectedTone = value.text to value.tonePreset
        frozenWindows = value.windows
    }

    fun openTextModels() {
        modelChooser = true
        busy = true
        feedback = ""
        stage = "Consultando modelos de texto..."
        job = scope.launch {
            try {
                val key = withContext(Dispatchers.IO) { credentials.read() } ?: throw IOException("Sem chave.")
                textModels = textClient.listModels(key)
                if (textModels.none { it.id == textModel }) {
                    textModel = textModels.first().id
                    textPrefs.edit().putString("model", textModel).apply()
                }
            } catch (cancel: CancellationException) { throw cancel }
            catch (_: Exception) { feedback = "Não foi possível consultar os modelos Gemini. Verifique a API." }
            finally { busy = false; stage = "" }
        }
    }

    fun runScriptAction(action: VoiceStudioTextAction, brief: String) {
        val original = text
        val input = if (action == VoiceStudioTextAction.WRITE) brief else original
        val requested = textModel
        scriptAction = null
        busy = true
        stage = "Consultando IA do roteiro..."
        feedback = ""
        job = scope.launch {
            try {
                val key = withContext(Dispatchers.IO) { credentials.read() } ?: throw IOException("Sem chave.")
                val resolvedModel = requested.ifBlank {
                    textClient.listModels(key).also { textModels = it }.first().id.also {
                        textModel = it
                        textPrefs.edit().putString("model", it).apply()
                    }
                }
                val prompt = when (action) {
                    VoiceStudioTextAction.TONE -> "Classifique o texto para interpretação em voz alta. " +
                        "Retorne apenas um JSON com o campo tone igual a uma destas opções: " +
                        VoiceStudioPlan.tones.joinToString(" | ") { it.preset } + "."
                    VoiceStudioTextAction.WRITE -> "Escreva um roteiro pronto para narrar, em português brasileiro, " +
                        "a partir do pedido fornecido. Não adicione informações factuais não fornecidas. " +
                        "Retorne apenas JSON com o campo text, até 4000 caracteres, sem instruções de leitura."
                    VoiceStudioTextAction.REVISE -> "Revise o texto em português brasileiro para leitura em voz alta. " +
                        "Preserve história, personagens e sentido; ajuste fluidez, pontuação e pausas. " +
                        "Retorne apenas JSON com o campo text, até 4000 caracteres, sem instruções de leitura."
                }
                val answer = recovery.track {
                    textClient.generateStudioValidated(key, "$prompt\nConteúdo de referência:\n$input", resolvedModel,
                        if (action == VoiceStudioTextAction.TONE) "voice_studio_tone" else "voice_studio_text") { json ->
                        val valid = if (action == VoiceStudioTextAction.TONE)
                            VoiceStudioPlan.tones.any { it.preset.equals(json.optString("tone").trim(), true) }
                        else json.optString("text").trim().length in 1..StandaloneVoiceTextPolicy.MAX_CHARS
                        if (!valid) throw GeminiInvalidResult("Sugestão incompatível com o roteiro.")
                        json
                    }
                }
                if (action == VoiceStudioTextAction.TONE) {
                    proposedTone = VoiceStudioPlan.tones.firstOrNull { it.preset.equals(answer.optString("tone").trim(), true) }
                        ?: throw IOException("Tom não reconhecido.")
                } else {
                    proposedText = answer.optString("text").trim().takeIf { it.isNotBlank() &&
                        it.length <= StandaloneVoiceTextPolicy.MAX_CHARS } ?: throw IOException("Roteiro vazio ou longo.")
                }
            } catch (cancel: CancellationException) { throw cancel }
            catch (e: Exception) {
                feedback = if (e is GeminiHttpException && e.statusCode == 429)
                    "Limite da IA de roteiro atingido. O texto original foi preservado."
                else "Não foi possível obter uma sugestão. O texto original foi preservado."
            } finally { busy = false; stage = "" }
        }
    }

    fun infoFor(wave: GeminiWave, startsAtZero: Boolean): StandaloneVoiceLibrary.Info =
        StandaloneVoiceLibrary.Info(
            title = "Narração ${DateFormat.getDateTimeInstance(DateFormat.SHORT,
                DateFormat.SHORT).format(Date())}",
            voiceName = voice?.let { GeminiVoiceAliases.title(it, localVoiceAliases) }.orEmpty(),
            excerpt = text.trim().replace(Regex("\\s+"), " ").take(120),
            style = if (style == VoiceStudioPlan.AUTO) tone.label else style,
            durationMs = wave.durationMs, startsAtZero = startsAtZero)

    fun errorFor(error: Exception): String = when (error) {
        is GeminiConfiguredDailyLimit -> error.message.orEmpty()
        is GeminiApiError -> if (error.http == 429 && error.dailyQuota)
            "Cota diária Gemini esgotada para este projeto/modelo."
        else if (error.http == 429) "O Gemini limitou as solicitações. A resposta não confirmou cota diária."
        else GeminiVoiceCatalog.safeMessage(error)
        else -> GeminiVoiceCatalog.safeMessage(error)
    }

    fun runScenes(regenerate: Int? = null, assemble: Boolean = false) {
        if (busy || scenes.isEmpty() || (!assemble && (!configured || voice == null))) return
        val request = snapshot()
        val plan = scenes
        busy = true
        feedback = ""
        apiUsage = ""
        sceneWork = true
        stage = if (assemble) "Montando WAV local..." else "Preparando trechos..."
        job = scope.launch {
            var current = ready
            try {
                current = withContext(Dispatchers.IO) { batch.completed(plan) }
                if (assemble) {
                    if (current.any { it == null }) throw IOException("Ainda há trechos pendentes.")
                    if (current.indices.any { current[it]!!.second > plan[it].windowMs })
                        throw IOException("Ajuste os trechos que passam da janela antes de montar.")
                    val wave = withContext(Dispatchers.IO) { batch.assemble(plan, current) }
                    val info = infoFor(wave, request.mode == VoiceStudioMode.PROJECT)
                    val file = withContext(Dispatchers.IO) { library.save(wave, info) }
                    reloadLibrary()
                    sceneReview = false
                    feedback = "WAV montado localmente, sem nova chamada à API."
                    playFile(file)
                } else {
                    val pending = regenerate?.let { listOf(it) } ?: plan.indices.filter { current[it] == null }
                    if (pending.isEmpty()) return@launch
                    val key = withContext(Dispatchers.IO) { credentials.read() }
                        ?: throw IOException("Configure a API Gemini.")
                    if (!withContext(Dispatchers.IO) { batchDraft.save(request) })
                        throw IOException("Não foi possível salvar o plano no aparelho.")
                    storedBatchAvailable = true
                    for (i in pending) {
                        val scene = plan[i]
                        val previous = current[i]?.second
                        pacing.beforeRequest(request.model) { wait ->
                            stage = "Cena ${i + 1} · aguardando limite local (${wait / 1000}s)."
                        }
                        stage = "Gerando cena ${i + 1} de ${plan.size}..."
                        val goal = scene.windowMs.coerceIn(NarrationDurationAssist.MIN_TARGET_MS,
                            NarrationDurationAssist.MAX_TARGET_MS)
                        val directions = "$interpretation. ${NarrationDurationAssist.instruction(goal)}" +
                            if ((previous ?: 0L) > scene.windowMs)
                                " A tentativa anterior ficou longa. Fale um pouco mais rápido, mantendo a clareza."
                            else ""
                        val wave = client.synthesize(key, request.model, request.voice!!.id, directions,
                            scene.text, request.language, useFailover = false)
                        if (regenerate != null && previous != null && previous > scene.windowMs &&
                            wave.durationMs > scene.windowMs && wave.durationMs >= previous) {
                            feedback = "A nova tentativa não encurtou a fala. O melhor trecho foi mantido."
                            return@launch
                        }
                        // Uma resposta recebida deve ser gravada antes de iniciar outra solicitação.
                        val written = withContext(kotlinx.coroutines.NonCancellable + Dispatchers.IO) {
                            batch.save(i, wave)
                            batch.existing(i)
                        }
                        current = current.mapIndexed { index, old -> if (i == index) written else old }
                        ready = current
                    }
                    feedback = "${current.count { it != null }} de ${plan.size} trechos prontos. Confira as durações."
                }
            } catch (cancel: CancellationException) {
                feedback = "Geração interrompida. Os trechos gravados ficaram salvos."
                throw cancel
            } catch (error: Exception) {
                feedback = "${errorFor(error)} ${current.count { it != null }} de ${plan.size} trechos salvos."
            } finally { busy = false; stage = ""; sceneWork = false }
        }
    }

    BackHandler(enabled = busy && !chooseVoice) { confirmExit = true }
    BackHandler(enabled = chooseVoice) { chooseVoice = false }
    if (chooseVoice) {
        GeminiVoiceCenterScreen(
            onBack = { chooseVoice = false },
            selectedVoiceId = voice?.id,
            initialModel = model,
            onModelChange = { model = it },
            styleInstruction = interpretation,
            onChoose = { selected -> voice = selected; chooseVoice = false; feedback = "" }
        )
        return
    }

    val durationEstimate = remember(text, style, tone) { VoiceStudioPlan.plannedSeconds(text, style, tone) }
    val visibleSaved = remember(savedFiles, audioInfo, favoritesOnly, showAllSaved) {
        savedFiles.filter { !favoritesOnly || audioInfo[it.absolutePath]?.favorite == true }
            .take(if (showAllSaved) 100 else 5)
    }
    Column(Modifier.fillMaxSize().imePadding()) {
        Heading("Estúdio de Voz", { if (busy) confirmExit = true else onBack() })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Texto para Voz", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold)
                    Text("Narração Gemini · biblioteca local", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onOpenGeminiSettings, enabled = !busy) {
                    Icon(Icons.Default.Settings, "Configurar chave Gemini")
                }
            }
            Surface(shape = UaiShapes.card,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .48f)) {
                Column(Modifier.fillMaxWidth().padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Seu texto", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Text("${StandaloneVoiceTextPolicy.length(text)}/4.000 caracteres",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    OutlinedTextField(
                        value = text,
                        onValueChange = { value ->
                            if (value.length <= StandaloneVoiceTextPolicy.MAX_CHARS &&
                                StandaloneVoiceTextPolicy.length(value) <= StandaloneVoiceTextPolicy.MAX_CHARS) text = value
                        },
                        placeholder = { Text("Digite ou cole o texto que será narrado...") },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 112.dp),
                        minLines = 3, maxLines = 8, enabled = !busy,
                        shape = UaiShapes.card
                    )
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TextButton(onClick = {
                            clipboard.getText()?.text?.let { pasted ->
                                val candidate = text + pasted
                                if (StandaloneVoiceTextPolicy.length(candidate) <= StandaloneVoiceTextPolicy.MAX_CHARS &&
                                    candidate.length <= StandaloneVoiceTextPolicy.MAX_CHARS) text = candidate
                                else feedback = "Limite de 4.000 caracteres atingido."
                            }
                        }, enabled = !busy) {
                            Icon(Icons.Default.ContentPaste, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp)); Text("Colar")
                        }
                        VoiceStudioScriptMenu(configured, textModel, !busy, text.isNotBlank(),
                            onOpenGeminiSettings, ::openTextModels) { scriptAction = it }
                    }
                    if (durationEstimate > 0) {
                        Text("Planeje ${VoiceStudioPlan.time(durationEstimate)} · estimativa",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            val current = snapshot()
                            scope.launch {
                                val ok = withContext(Dispatchers.IO) { draftStore.save(current) }
                                storedDraftAvailable = ok || storedDraftAvailable
                                feedback = if (ok) "Rascunho privado salvo no aparelho." else "Não foi possível salvar o rascunho."
                            }
                        }, enabled = !busy && text.isNotBlank(), shape = UaiShapes.button,
                            modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Save, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp)); Text("Salvar rascunho", maxLines = 1)
                        }
                        if (storedDraftAvailable) OutlinedButton(onClick = {
                            scope.launch {
                                val loaded = withContext(Dispatchers.IO) { draftStore.load() }
                                if (loaded != null) {
                                    restore(loaded)
                                    feedback = "Rascunho restaurado."
                                } else feedback = "Rascunho indisponível."
                            }
                        }, enabled = !busy, shape = UaiShapes.button) { Text("Restaurar") }
                    }
                    if (storedDraftAvailable) TextButton(onClick = {
                        scope.launch {
                            val ok = withContext(Dispatchers.IO) { draftStore.clear() }
                            if (ok) storedDraftAvailable = false
                            feedback = if (ok) "Rascunho salvo apagado; texto atual preservado." else
                                "Falha ao apagar rascunho local."
                        }
                    }, enabled = !busy) { Text("Apagar rascunho salvo") }
                }
            }

            Surface(shape = UaiShapes.card, tonalElevation = 1.dp) {
                Column(Modifier.fillMaxWidth().padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Voz selecionada", fontWeight = FontWeight.SemiBold)
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Default.GraphicEq, null,
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
                        Column(Modifier.weight(1f)) {
                            Text(voice?.let { GeminiVoiceAliases.title(it, localVoiceAliases) }
                                    ?: "Nenhuma voz selecionada", maxLines = 1,
                                overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
                            Text(voice?.let { selected ->
                                listOf(selected.gender, selected.languageCode, selected.pitch)
                                    .filter(String::isNotBlank).joinToString(" · ").ifBlank { "Voz Gemini" }
                            } ?: "Escolha uma voz na Central", maxLines = 2,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(onClick = { chooseVoice = true }, enabled = !busy) {
                            Text(if (voice == null) "Escolher" else "Trocar")
                        }
                    }
                    if (voice == null) OutlinedButton(onClick = { chooseVoice = true }, enabled = !busy,
                        modifier = Modifier.fillMaxWidth(), shape = UaiShapes.button) {
                        Icon(Icons.Default.RecordVoiceOver, null)
                        Spacer(Modifier.width(6.dp)); Text("Selecionar na Central de Vozes")
                    }
                    HorizontalDivider()
                    Text("Interpretação", style = MaterialTheme.typography.labelMedium)
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        listOf(VoiceStudioPlan.AUTO, "Neutra", "Animada", "Dramática").forEach { preset ->
                            FilterChip(selected = style == preset, onClick = { style = preset },
                                enabled = !busy,
                                label = { Text(if (preset == "Narrador profissional") "Narrador" else preset) })
                        }
                        Box {
                            FilterChip(selected = style !in listOf(VoiceStudioPlan.AUTO, "Neutra", "Animada", "Dramática"),
                                onClick = { styleMenu = true }, enabled = !busy,
                                label = { Text("Mais estilos") })
                            DropdownMenu(expanded = styleMenu, onDismissRequest = { styleMenu = false }) {
                                GeminiNarrationStyling.presets.forEach { preset ->
                                    DropdownMenuItem(text = { Text(preset.label) }, onClick = {
                                        style = preset.label; styleMenu = false
                                    })
                                }
                            }
                        }
                    }
                    if (style == VoiceStudioPlan.AUTO && text.isNotBlank())
                        Text("${tone.label} · ${tone.guidance}", style = MaterialTheme.typography.bodySmall)
                    if (configured && StandaloneVoiceTextPolicy.accepted(text, voice?.id.orEmpty()))
                        TextButton(onClick = {
                            if (shortPreview != null && playedFile == shortPreview && playing) player.pause()
                            else { previewRequested = true; confirm = true }
                        }, enabled = !busy) {
                            Text(if (shortPreview != null && playedFile == shortPreview && playing) "Pausar amostra"
                                else "Ouvir amostra curta")
                        }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Idioma da fala", style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.weight(1f))
                    }
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        GeminiSpeechLanguage.choices.forEach { mode ->
                            FilterChip(selected = language == mode, enabled = !busy,
                                onClick = { language = mode },
                                label = { Text(GeminiSpeechLanguage.label(mode)) })
                        }
                    }
                    Text("A orientação pt-BR ajuda na pronúncia, mas não garante sotaque brasileiro em todas as vozes.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = { technicalExpanded = !technicalExpanded }) {
                        Icon(if (technicalExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            null, Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp)); Text("Opções técnicas")
                    }
                    if (technicalExpanded) {
                        Box {
                            OutlinedButton(onClick = { modelMenu = true }, enabled = !busy,
                                modifier = Modifier.fillMaxWidth(), shape = UaiShapes.button) {
                                Text(if (model == GeminiModels.LITE) "Gemini Lite" else "Gemini Flash")
                                Spacer(Modifier.weight(1f)); Icon(Icons.Default.ArrowDropDown, null)
                            }
                            DropdownMenu(expanded = modelMenu, onDismissRequest = { modelMenu = false }) {
                                GeminiModels.supported.forEach { candidate ->
                                    DropdownMenuItem(text = {
                                        Text(if (candidate == GeminiModels.LITE) "Gemini Lite" else "Gemini Flash")
                                    }, onClick = { model = candidate; modelMenu = false })
                                }
                            }
                        }
                        Text("~${StandaloneVoiceTextPolicy.tokenEstimate(text)} tokens (estimativa)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("A geração envia o texto à API Google e pode consumir cota. Nada é enviado antes de você confirmar.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Formato da narração", style = MaterialTheme.typography.titleSmall)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    VoiceStudioMode.entries.forEach { option ->
                        FilterChip(selected = mode == option, enabled = !busy &&
                            (option != VoiceStudioMode.PROJECT || projectClipWindows.isNotEmpty() || frozenWindows.isNotEmpty()),
                            onClick = {
                                if (option != mode) frozenWindows = emptyList()
                                mode = option
                            }, label = { Text(option.label) })
                    }
                }
                if (mode == VoiceStudioMode.PROJECT && frozenWindows.isNotEmpty() && frozenWindows != projectClipWindows) {
                    Text("Este lote usa as janelas salvas. Confira a montagem se os clipes mudaram.",
                        style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { frozenWindows = emptyList() },
                        enabled = !busy && projectClipWindows.isNotEmpty()) { Text("Usar clipes atuais") }
                }
                if (scenes.isNotEmpty()) TextButton(onClick = { sceneReview = true }) {
                    Text("Ver cenas · ${ready.count { it != null }} de ${scenes.size} prontas")
                }
                if (storedBatchAvailable) TextButton(onClick = { restoreBatch = true }, enabled = !busy) {
                    Text("Restaurar último lote salvo")
                }
            }
            if (feedback.isNotEmpty()) Text(feedback, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary)
            GeminiRecoveryFeedback(recovery)
            if (apiUsage.isNotEmpty()) Text(apiUsage, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (busy) {
                Surface(shape = UaiShapes.card,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .5f)) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Preparando narração", fontWeight = FontWeight.SemiBold)
                        if (scenes.isNotEmpty() && sceneWork)
                            LinearProgressIndicator(progress = { ready.count { it != null }.toFloat() / scenes.size },
                                modifier = Modifier.fillMaxWidth())
                        else LinearProgressIndicator(Modifier.fillMaxWidth())
                        Text(stage.ifBlank { "Processando..." }, style = MaterialTheme.typography.bodySmall)

                    }
                }
            }
            if (generated?.isFile == true) {
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = UaiShapes.card) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Áudio pronto", fontWeight = FontWeight.SemiBold)
                        audioInfo[generated?.absolutePath]?.durationMs?.takeIf { it > 0 }?.let {
                            Text("Duração real · ${VoiceStudioPlan.measured(it)}", style = MaterialTheme.typography.bodySmall)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { generated?.let(::playFile) },
                                modifier = Modifier.weight(1f), shape = UaiShapes.button) {
                                Icon(if (playing && playedFile == generated) Icons.Default.Pause else Icons.Default.PlayArrow, null)
                                Spacer(Modifier.width(4.dp)); Text(if (playing && playedFile == generated) "Pausar" else "Ouvir")
                            }
                            OutlinedButton(onClick = { generated?.let(::saveExternally) },
                                modifier = Modifier.weight(1f), shape = UaiShapes.button) {
                                Icon(Icons.Default.SaveAlt, null)
                                Spacer(Modifier.width(4.dp)); Text("Salvar WAV")
                            }
                        }
                        if (onAddToEditor != null) OutlinedButton(
                            onClick = { addToEditorFile = generated },
                            modifier = Modifier.fillMaxWidth(), shape = UaiShapes.button) {
                            Icon(Icons.Default.Add, null)
                            Spacer(Modifier.width(4.dp)); Text("Adicionar à linha do tempo")
                        }
                    }
                }
            }
            if (savedFiles.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Biblioteca de áudios (${savedFiles.size})", fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f))
                    if (savedFiles.size > 5) TextButton(onClick = { showAllSaved = !showAllSaved }) {
                        Text(if (showAllSaved) "Recolher" else "Ver todos")
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !favoritesOnly, onClick = { favoritesOnly = false },
                        label = { Text("Recentes") })
                    FilterChip(selected = favoritesOnly, onClick = { favoritesOnly = true },
                        label = { Text("Favoritos") })
                }
                if (visibleSaved.isEmpty()) Text("Nenhum áudio neste filtro.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                visibleSaved.forEach { file ->
                    val meta = audioInfo[file.absolutePath]
                    Surface(shape = UaiShapes.card, color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp) {
                        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { playFile(file) }) {
                                Icon(if (playing && playedFile == file) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    "Ouvir áudio")
                            }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(meta?.title?.takeIf(String::isNotBlank)
                                        ?: "Narração ${DateFormat.getDateTimeInstance(DateFormat.SHORT,
                                            DateFormat.SHORT).format(Date(file.lastModified()))}",
                                    style = MaterialTheme.typography.bodyMedium, maxLines = 1,
                                    overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
                                if (!meta?.excerpt.isNullOrBlank()) Text(meta!!.excerpt,
                                    style = MaterialTheme.typography.labelSmall, maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(buildString {
                                    append("WAV · ${file.length() / 1024} KB")
                                    if ((meta?.durationMs ?: 0) > 0) append(" · ${formatMs(meta!!.durationMs)}")
                                    if (!meta?.voiceName.isNullOrBlank()) append(" · ${meta!!.voiceName}")
                                }, style = MaterialTheme.typography.labelSmall, maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = {
                                scope.launch {
                                    val ok = withContext(Dispatchers.IO) { library.toggleFavorite(file) }
                                    if (ok) reloadLibrary() else feedback = "Falha ao alterar favorito."
                                }
                            }) { Icon(if (meta?.favorite == true) Icons.Default.Star else Icons.Default.StarBorder,
                                    "Favoritar áudio") }
                            Box {
                                IconButton(onClick = { audioMenuFile = file }) {
                                    Icon(Icons.Default.MoreVert, "Opções do áudio")
                                }
                                DropdownMenu(expanded = audioMenuFile == file,
                                    onDismissRequest = { audioMenuFile = null }) {
                                    DropdownMenuItem(text = { Text("Renomear") }, onClick = {
                                        audioMenuFile = null; renameFile = file
                                        renameText = meta?.title.orEmpty().ifBlank { "Minha narração" }
                                    }, leadingIcon = { Icon(Icons.Default.Edit, null) })
                                    DropdownMenuItem(text = { Text("Exportar WAV") }, onClick = {
                                        audioMenuFile = null; saveExternally(file)
                                    }, leadingIcon = { Icon(Icons.Default.SaveAlt, null) })
                                    if (onAddToEditor != null) DropdownMenuItem(
                                        text = { Text("Adicionar à linha do tempo") }, onClick = {
                                            audioMenuFile = null; addToEditorFile = file
                                        }, leadingIcon = { Icon(Icons.Default.Add, null) })
                                    DropdownMenuItem(text = { Text("Excluir áudio") }, onClick = {
                                        audioMenuFile = null; deleteFile = file
                                    }, leadingIcon = { Icon(Icons.Default.DeleteOutline, null) })
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        Surface(tonalElevation = 2.dp) {
            Column(Modifier.fillMaxWidth().navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)) {
                if (busy) OutlinedButton(onClick = {
                    job?.cancel(); client.cancel(); textClient.cancel()
                    feedback = "Cancelamento solicitado. Uma chamada já enviada pode ter consumido cota."
                }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = UaiShapes.button) {
                    Icon(Icons.Default.Close, null)
                    Spacer(Modifier.width(8.dp)); Text("Cancelar solicitação")
                } else BlueButton(if (mode == VoiceStudioMode.CONTINUOUS) "Gerar áudio" else "Revisar cenas",
                    onClick = { if (mode == VoiceStudioMode.CONTINUOUS) confirm = true else sceneReview = true },
                    enabled = StandaloneVoiceTextPolicy.accepted(text, voice?.id.orEmpty()) &&
                        (if (mode == VoiceStudioMode.CONTINUOUS) configured else scenes.isNotEmpty()),
                    modifier = Modifier.fillMaxWidth())
            }
        }
    }
    if (confirmExit) AlertDialog(onDismissRequest = { confirmExit = false },
        title = { Text("Sair da geração?") },
        text = { Text("A solicitação será interrompida no aplicativo, mas se o Google já a recebeu, " +
            "pode consumir cota. Nenhuma nova chamada será feita automaticamente.") },
        confirmButton = { TextButton(onClick = {
            confirmExit = false
            job?.cancel(); client.cancel(); textClient.cancel(); onBack()
        }) { Text("Sair") } },
        dismissButton = { TextButton(onClick = { confirmExit = false }) { Text("Continuar aqui") } })
    if (modelChooser) AlertDialog(onDismissRequest = { modelChooser = false },
        title = { Text("Gemini · modelo de texto") },
        text = {
            Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                if (busy) Text(stage)
                else if (textModels.isEmpty()) Text(feedback.ifBlank { "Nenhum modelo listado." })
                textModels.forEach { item ->
                    TextButton(onClick = {
                        textModel = item.id; textPrefs.edit().putString("model", item.id).apply()
                        modelChooser = false
                    }, enabled = !busy) {
                        Text(item.label, color = if (textModel == item.id) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { modelChooser = false }) { Text("Fechar") } })
    scriptAction?.let { action ->
        VoiceStudioTextRequest(action, textModel, { scriptAction = null }) { brief -> runScriptAction(action, brief) }
    }
    proposedText?.let { proposal ->
        AlertDialog(onDismissRequest = { proposedText = null }, title = { Text("Sugestão de roteiro") },
            text = { Text(proposal, Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState())) },
            confirmButton = { TextButton(onClick = { text = proposal; proposedText = null }) { Text("Usar texto") } },
            dismissButton = { TextButton(onClick = { proposedText = null }) { Text("Manter original") } })
    }
    proposedTone?.let { suggestion ->
        AlertDialog(onDismissRequest = { proposedTone = null }, title = { Text(suggestion.label) },
            text = { Text(suggestion.guidance) },
            confirmButton = { TextButton(onClick = {
                selectedTone = text to suggestion.preset; style = VoiceStudioPlan.AUTO; proposedTone = null
            }) { Text("Aplicar interpretação") } },
            dismissButton = { TextButton(onClick = { proposedTone = null }) { Text("Voltar") } })
    }
    if (restoreBatch) AlertDialog(onDismissRequest = { restoreBatch = false },
        title = { Text("Restaurar lote de narração?") },
        text = { Text("O roteiro e as opções atuais serão substituídos pelo último lote salvo no aparelho.") },
        confirmButton = { TextButton(onClick = {
            restoreBatch = false
            scope.launch {
                val previous = withContext(Dispatchers.IO) { batchDraft.load() }
                if (previous != null) {
                    restore(previous); feedback = "Lote restaurado."; sceneReview = true
                } else feedback = "Lote salvo indisponível."
            }
        }) { Text("Restaurar") } },
        dismissButton = { TextButton(onClick = { restoreBatch = false }) { Text("Voltar") } })
    if (sceneReview && scenes.isNotEmpty()) VoiceStudioSceneReview(scenes, ready, style, tone, busy,
        if (busy) stage else feedback, configured && voice != null, { sceneReview = false },
        { job?.cancel(); client.cancel() },
        {
            if (ready.count { it != null } == scenes.size) runScenes(assemble = true)
            else { retryScene = null; confirm = true; sceneReview = false }
        },
        { index -> retryScene = index; confirm = true; sceneReview = false },
        { play(it, false) })
    if (confirm) AlertDialog(onDismissRequest = { confirm = false; previewRequested = false; retryScene = null },
        title = { Text(if (previewRequested) "Gerar amostra no Gemini?" else "Gerar áudio no Gemini?") },
        text = {
            val details = if (previewRequested) "Apenas um trecho curto será enviado ao Google. "
                else if (mode == VoiceStudioMode.CONTINUOUS) "O texto será enviado ao Google para gerar um WAV. "
                else if (retryScene != null) "Somente a cena ${retryScene!! + 1} será solicitada novamente. "
                else "Serão enviados apenas os trechos pendentes. Cada trecho pronto fica salvo. "
            val modelNotice = if (!previewRequested && mode == VoiceStudioMode.CONTINUOUS)
                "Se o modelo estiver indisponível, o app pode tentar outro mantendo a voz. " else ""
            Text(details + modelNotice + "A solicitação poderá consumir cota ou gerar cobrança. " +
                "Nada é enviado antes de você confirmar.")
        },
        confirmButton = { TextButton(onClick = {
            confirm = false
            if (busy || !configured || !StandaloneVoiceTextPolicy.accepted(text, voice?.id.orEmpty())) return@TextButton
            val sample = previewRequested
            previewRequested = false
            if (!sample && mode != VoiceStudioMode.CONTINUOUS) {
                val retry = retryScene
                retryScene = null
                sceneReview = true
                runScenes(retry)
                return@TextButton
            }
            val requestedText = if (sample)
                VoiceStudioPlan.scenes(text, VoiceStudioMode.SEVEN, emptyList(), style, tone).first().text
                else text
            val requestedVoice = voice!!.id
            val requestedModel = model
            val requestedStyle = interpretation
            val requestedLanguage = language
            busy = true
            feedback = if (sample) "Gerando amostra..." else "Gerando áudio..."
            apiUsage = ""
            job = scope.launch {
                try {
                    val key = withContext(Dispatchers.IO) { credentials.read() }
                        ?: throw IOException("Configure uma chave Gemini em Serviços e APIs.")
                    pacing.beforeRequest(requestedModel) { wait ->
                        stage = "Aguardando limite local configurado (${wait / 1000}s)."
                    }
                    val started = System.currentTimeMillis()
                    stage = "Aguardando resposta do Gemini..."
                    val wave = recovery.track {
                        client.synthesize(key, requestedModel, requestedVoice,
                            requestedStyle, requestedText, requestedLanguage, useFailover = false,
                            allowModelRecovery = !sample)
                    }
                    if (sample) {
                        val old = shortPreview
                        val file = withContext(Dispatchers.IO) {
                            File.createTempFile("voice-sample-", ".wav", ctx.cacheDir).apply { writeBytes(wave.bytes) }
                        }
                        shortPreview = file
                        play(file, false)
                        old?.delete()
                        feedback = "Amostra pronta. Duração real · ${VoiceStudioPlan.measured(wave.durationMs)}."
                    } else {
                        stage = "Salvando WAV no armazenamento privado..."
                        val info = infoFor(wave, false)
                        val file = withContext(Dispatchers.IO) { library.save(wave, info) }
                        reloadLibrary()
                        val recorded = withContext(Dispatchers.IO) {
                            ledger.events().lastOrNull { it.at >= started && it.operation == "narracao" &&
                                it.model == (wave.modelUsed ?: requestedModel) && it.status == "ok" }
                        }
                        apiUsage = recorded?.tokens?.total?.let { "$it tokens informados pelo Gemini." }
                            ?: "Consumo real de tokens não informado pela API."
                        feedback = "Áudio gerado e salvo no aplicativo."
                        playFile(file)
                    }
                } catch (e: CancellationException) {
                    feedback = "Solicitação interrompida. Uma chamada já enviada pode consumir cota."
                    throw e
                } catch (e: Exception) {
                    feedback = "Não foi possível gerar o áudio. ${errorFor(e)}"
                } finally { busy = false; stage = "" }
            }
        }) { Text("Confirmar e gerar") } },
        dismissButton = { TextButton(onClick = { confirm = false; previewRequested = false; retryScene = null }) { Text("Voltar") } })
    if (renameFile != null) AlertDialog(onDismissRequest = { renameFile = null },
        title = { Text("Renomear áudio") },
        text = { OutlinedTextField(value = renameText,
            onValueChange = { if (it.length <= 80) renameText = it },
            singleLine = true, label = { Text("Nome da narração") }) },
        confirmButton = { TextButton(enabled = renameText.isNotBlank(), onClick = {
            val file = renameFile
            val title = renameText
            renameFile = null
            if (file != null) scope.launch {
                val ok = withContext(Dispatchers.IO) { library.rename(file, title) }
                reloadLibrary()
                feedback = if (ok) "Narração renomeada." else "Não foi possível renomear o áudio."
            }
        }) { Text("Salvar") } },
        dismissButton = { TextButton(onClick = { renameFile = null }) { Text("Cancelar") } })
    if (addToEditorFile != null && onAddToEditor != null) AlertDialog(
        onDismissRequest = { addToEditorFile = null },
        title = { Text("Adicionar ao projeto aberto?") },
        text = {
            val place = if (audioInfo[addToEditorFile?.absolutePath]?.startsAtZero == true)
                "no início da linha do tempo para acompanhar os clipes"
                else "na posição atual da linha do tempo"
            Text("O WAV será copiado para o projeto e adicionado $place. O original continua salvo.")
        },
        confirmButton = { TextButton(onClick = {
            val file = addToEditorFile
            addToEditorFile = null
            if (file != null) scope.launch {
                val ok = try { onAddToEditor(file) }
                    catch (cancel: CancellationException) { throw cancel }
                    catch (_: Exception) { false }
                feedback = if (ok) "Narração adicionada ao projeto." else
                    "Não foi possível adicionar. Confira o projeto e a duração disponível."
            }
        }) { Text("Adicionar") } },
        dismissButton = { TextButton(onClick = { addToEditorFile = null }) { Text("Cancelar") } })
    if (deleteFile != null) AlertDialog(onDismissRequest = { deleteFile = null },
        title = { Text("Excluir este áudio local?") },
        text = { Text("O WAV será excluído apenas deste aplicativo. Cópias exportadas não serão afetadas.") },
        confirmButton = { TextButton(onClick = {
            val file = deleteFile
            deleteFile = null
            scope.launch {
                val success = withContext(Dispatchers.IO) { file != null && library.delete(file) }
                reloadLibrary()
                if (generated == file) { player.stop(); generated = null }
                feedback = if (success) "Áudio excluído." else "Não foi possível excluir o áudio."
            }
        }) { Text("Excluir") } },
        dismissButton = { TextButton(onClick = { deleteFile = null }) { Text("Cancelar") } })
}
