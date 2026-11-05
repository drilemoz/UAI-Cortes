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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import java.io.File

/** Vozes IA: as legendas são fonte somente leitura. Áudio tradicional continua separado. */
@Composable
internal fun VoiceIaScreen(project: VideoProject, back: () -> Unit,
    onSave: (VideoProject) -> Boolean, openGeminiSettings: () -> Unit,
    initialCueId: String? = null) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember(project.id) { VoiceIaDraftStore(context, project.id) }
    val credentials = remember(context) { GeminiCredentialStore(context) }
    val api = remember(context) { GeminiTtsClient(context) }
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
    var status by remember(project.id) { mutableStateOf("") }
    var catalog by remember(project.id) { mutableStateOf<List<GeminiVoice>>(emptyList()) }
    var catalogBusy by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var completed by remember { mutableIntStateOf(0) }
    var total by remember { mutableIntStateOf(0) }
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
    LaunchedEffect(store) {
        try {
            takes = withContext(Dispatchers.IO) { store.load() }
            model = takes.firstOrNull()?.model
                ?: project.audioTracks.firstOrNull { it.kind == VoiceIaPolicy.KIND }?.ttsModel
                ?: GeminiModels.LITE
            defaultVoice = takes.firstOrNull()?.voiceId
                ?: project.audioTracks.firstOrNull { it.kind == VoiceIaPolicy.KIND }?.voiceId
            loaded = true
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) {
            loaded = true
            status = "Não foi possível ler o rascunho de vozes. Os áudios já inseridos continuam no projeto."
        }
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
    fun generate(ids: List<String>, regenerateId: String? = null) {
        if (busy || !loaded || catalogBusy) return
        val chosen = cues.filter { it.id in ids }
        if (chosen.isEmpty()) return
        generationJob = scope.launch {
            busy = true; completed = 0; total = chosen.size
            try {
                val key = withContext(Dispatchers.IO) { credentials.read() }
                    ?: error("Configure uma chave Gemini em Serviços e APIs.")
                for (group in chosen.chunked(SubtitleNarrationTimeline.MAX_SEGMENTS)) {
                    // Lotes limitados a 12, com WAV salvo antes de passar à próxima chamada.
                    for (cue in group) {
                        coroutineContext.ensureActive()
                        val voiceId = voiceFor(cue) ?: error("Selecione a voz da legenda ${completed + 1}.")
                        val direction = GeminiNarrationStyling.instruction(styleFor(cue))
                        val existing = takes.firstOrNull {
                            cue.id != regenerateId && VoiceIaPolicy.matches(it, cue, voiceId, model, direction)
                        }
                        if (existing == null) {
                            status = "Gerando ${completed + 1}/${chosen.size}: ${cue.text.take(50)}"
                            val response = api.synthesize(key, model, voiceId, direction, cue.text)
                            val savedFile = withContext(Dispatchers.IO) {
                                coroutineContext.ensureActive()
                                GeminiAudioFiles.save(context, response)
                            }
                            val take = VoiceIaTake(cue.id, VoiceIaPolicy.fingerprint(cue), voiceId,
                                model, direction, savedFile.absolutePath, response.durationMs,
                                GeminiNarrationCheckpoint.hash(response.bytes))
                            val updated = takes.filterNot { it.cueId == cue.id } + take
                            withContext(Dispatchers.IO) { store.save(updated) }
                            takes = updated
                        }
                        completed++
                    }
                }
                status = "${chosen.size} fala(s) preparada(s). Ouça e insira as faixas desejadas na timeline."
            } catch (e: CancellationException) {
                status = "Geração interrompida. Os WAVs concluídos permanecem salvos; chamadas em andamento podem consumir cota."
            } catch (e: GeminiApiError) {
                status = "Falha na chamada ${completed + 1}: ${e.message}. Arquivos concluídos foram mantidos."
            } catch (e: Exception) {
                status = "Interrompido em ${completed + 1}/${chosen.size}: ${e.message?.take(130) ?: "falha local"}. Não haverá repetição automática."
            } finally { busy = false; generationJob = null }
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
        // Ferramentas gerais próximas: catálogo, modelo e voz comum.
        // A escolha geral só altera o rascunho, sem enviar texto ao Gemini.
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            OutlinedButton(onClick = {
                if (catalogBusy || busy) return@OutlinedButton
                scope.launch {
                    catalogBusy = true
                    try {
                        val key = withContext(Dispatchers.IO) { credentials.read() }
                            ?: error("Configure uma chave Gemini primeiro.")
                        val result = api.voicesWithDiagnosis(key)
                        catalog = result.voices
                        status = if (catalog.isEmpty()) "Catálogo sem vozes disponíveis." else
                            "Catálogo Gemini: ${catalog.size} voz(es). Escolha uma para gerar."
                    } catch (e: CancellationException) { throw e }
                    catch (e: Exception) { status = "Consulta de vozes: ${GeminiVoiceCatalog.safeMessage(e)}" }
                    finally { catalogBusy = false }
                }
            }, enabled = !busy && !catalogBusy, modifier = Modifier.weight(1.35f),
                contentPadding = PaddingValues(horizontal = 5.dp, vertical = 0.dp)) {
                Icon(Icons.Default.AutoAwesome, null, Modifier.size(15.dp))
                Spacer(Modifier.width(3.dp))
                Text(if (catalogBusy) "Buscando" else "Buscar vozes", fontSize = 11.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Box {
                TextButton(onClick = { modelOpen = true }, enabled = !busy,
                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 0.dp)) {
                    Text(if (model == GeminiModels.LITE) "Lite" else "Flash", fontSize = 12.sp)
                    Icon(Icons.Default.ArrowDropDown, null, Modifier.size(17.dp))
                }
                DropdownMenu(expanded = modelOpen, onDismissRequest = { modelOpen = false }) {
                    GeminiModels.supported.forEach { candidate ->
                        DropdownMenuItem(text = { Text(candidate) }, onClick = {
                            model = candidate; modelOpen = false
                        })
                    }
                }
            }
            OutlinedButton(onClick = { voicePickerForAll = true },
                enabled = !busy && !catalogBusy, modifier = Modifier.weight(1.1f),
                contentPadding = PaddingValues(horizontal = 5.dp, vertical = 0.dp)) {
                Text("Voz geral", fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (defaultVoice != null && cues.isNotEmpty() && cues.all { voiceFor(it) == defaultVoice }) {
                    Spacer(Modifier.width(2.dp))
                    Icon(Icons.Default.Check, "Voz geral selecionada", Modifier.size(14.dp))
                }
            }
        }
        if (status.isNotBlank()) Text(status, Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        if (busy) {
            LinearProgressIndicator(progress = { if (total > 0) completed.toFloat() / total else 0f },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${completed}/${total} concluídas", modifier = Modifier.weight(1f), fontSize = 12.sp)
                TextButton(onClick = { generationJob?.cancel(); api.cancel() }) { Text("Cancelar") }
            }
        }
        if (cues.isEmpty()) {
            Text("Este projeto não possui legendas com texto. Importe ou crie legendas na ferramenta Legendas; " +
                "elas aparecerão aqui automaticamente.", Modifier.padding(20.dp))
        } else {
            val pending = cues.count { readyFor(it) == null }
            val preparedCount = cues.size - pending
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                OutlinedButton(onClick = { stylePickerForAll = true }, enabled = !busy,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)) {
                    Icon(Icons.Default.TheaterComedy, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (defaultStyle == "Neutra") "Humor" else defaultStyle, fontSize = 12.sp, maxLines = 1,
                        overflow = TextOverflow.Ellipsis)
                    Icon(Icons.Default.ArrowDropDown, null, Modifier.size(17.dp))
                }
                Button(onClick = {
                    val missingVoice = cues.firstOrNull { voiceFor(it).isNullOrBlank() }
                    if (missingVoice != null) {
                        status = "Escolha Voz geral ou selecione uma voz em cada fala antes de gerar."
                        voicePickerForAll = true
                    } else {
                        forceCueId = null
                        promptGenerate = cues.filter { readyFor(it) == null }.map { it.id }
                    }
                }, enabled = !busy && loaded && pending > 0,
                    modifier = Modifier.weight(1.55f),
                    contentPadding = PaddingValues(horizontal = 5.dp, vertical = 0.dp)) {
                    Text("Gerar todas ($pending)", maxLines = 1, fontSize = 12.sp)
                }
            }
            if (preparedCount > 0) {
                OutlinedButton(onClick = { pendingBatchInsert = true },
                    enabled = !busy && loaded,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 3.dp)) {
                    Icon(Icons.Default.LibraryAdd, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(7.dp))
                    Text("Inserir todas as vozes prontas ($preparedCount)")
                }
            }
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
                                HorizontalDivider()
                                Row(verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = { voicePickerCueId = cue.id },
                                        enabled = !busy, modifier = Modifier.weight(1.45f),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)) {
                                        Text(catalog.firstOrNull { it.id == voiceFor(cue) }?.let(GeminiVoicePresentation::label)
                                            ?: voiceFor(cue) ?: "Escolher voz", maxLines = 1,
                                            overflow = TextOverflow.Ellipsis, fontSize = 12.sp)
                                    }
                                    OutlinedButton(onClick = { stylePickerCueId = cue.id }, enabled = !busy,
                                        modifier = Modifier.weight(1f),
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
                                    Text(if (overrunMs > 0L)
                                        "WAV ${formatPipMs(take.durationMs)} · +${VoiceIaDisplayPolicy.seconds(overrunMs)} s além da legenda"
                                    else "WAV ${formatPipMs(take.durationMs)} · dentro da legenda",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (overrunMs > 0L) MaterialTheme.colorScheme.error
                                        else MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Button(onClick = {
                                        if (voiceFor(cue).isNullOrBlank()) voicePickerCueId = cue.id
                                        else {
                                            forceCueId = if (take != null) cue.id else null
                                            promptGenerate = listOf(cue.id)
                                        }
                                    }, enabled = !busy && loaded,
                                        modifier = Modifier.weight(1f), contentPadding = PaddingValues(6.dp)) {
                                        Text(if (take == null) "Gerar" else "Refazer", fontSize = 12.sp)
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
                                    }, enabled = take != null && !busy, modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(6.dp)) {
                                        Text(if (playingFile == take?.filePath) "Pausar" else "Ouvir", fontSize = 12.sp)
                                    }
                                    Button(onClick = { pendingInsertId = cue.id },
                                        enabled = take != null && !busy, modifier = Modifier.weight(1.3f),
                                        contentPadding = PaddingValues(6.dp)) {
                                        Text(if (inserted == null) "Inserir" else "Substituir", fontSize = 12.sp)
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
        AlertDialog(onDismissRequest = { voicePickerCueId = null; voicePickerForAll = false },
            title = { Text(if (forAll) "Voz para todas as falas" else "Escolher voz Gemini") },
            text = { Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                if (catalog.isEmpty()) Text("Consulte o catálogo em Buscar vozes antes de selecionar.")
                catalog.forEach { voice ->
                    Column(Modifier.fillMaxWidth().clickable {
                        if (forAll) pendingVoiceForAll = voice.id
                        else if (targetId != null) voiceOverrides[targetId] = voice.id
                        voicePickerCueId = null
                        voicePickerForAll = false
                    }.padding(vertical = 9.dp)) {
                        Text(GeminiVoicePresentation.label(voice), fontWeight = FontWeight.Medium)
                        Text(GeminiVoicePresentation.shortDetails(voice),
                            style = MaterialTheme.typography.labelSmall)
                    }
                    HorizontalDivider()
                }
            } }, confirmButton = { TextButton(onClick = {
                voicePickerCueId = null; voicePickerForAll = false
            }) { Text("Fechar") } })
    }
    if (stylePickerCueId != null || stylePickerForAll) {
        val targetId = stylePickerCueId
        val forAll = stylePickerForAll
        AlertDialog(onDismissRequest = { stylePickerCueId = null; stylePickerForAll = false },
            title = { Text(if (forAll) "Estilo para todas as falas" else "Interpretação da fala") },
            text = { Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                GeminiNarrationStyling.presets.forEach { preset ->
                    Text(preset.label, Modifier.fillMaxWidth().clickable {
                        if (forAll) pendingStyleForAll = preset.label
                        else if (targetId != null) styleOverrides[targetId] = preset.label
                        stylePickerCueId = null
                        stylePickerForAll = false
                    }.padding(12.dp))
                }
            } }, confirmButton = { TextButton(onClick = {
                stylePickerCueId = null; stylePickerForAll = false
            }) { Text("Cancelar") } })
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
    promptGenerate?.let { ids ->
        val count = ids.size
        AlertDialog(onDismissRequest = { promptGenerate = null },
            title = { Text("Gerar ${if (count == 1) "esta voz" else "$count vozes"}?") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Cada fala exige uma solicitação à API Gemini, que pode consumir cota ou gerar cobrança. " +
                    "Os arquivos são salvos individualmente; falhas não geram novas tentativas automáticas. " +
                    "Nenhuma voz entra na timeline sem a sua confirmação.")
                GeminiUsageHint("narração", inputChars = cues.filter { it.id in ids }.sumOf { it.text.length },
                    model = model)
            } },
            confirmButton = { TextButton(onClick = {
                promptGenerate = null
                val regenerate = forceCueId
                forceCueId = null
                generate(ids, regenerate)
            }) { Text("Gerar") } },
            dismissButton = { TextButton(onClick = { promptGenerate = null }) { Text("Cancelar") } })
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
            MediaItem.Builder().setUri(clip.uri).setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder().setStartPositionMs(clip.startMs)
                    .setEndPositionMs(clip.endMs).build()).build()
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
