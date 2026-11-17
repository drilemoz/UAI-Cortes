package br.com.uaicortes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
internal fun AutoSubtitleEngineCard(title: String, detail: String, icon: ImageVector,
    onResume: (() -> Unit)?, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = UaiShapes.card,
        color = MaterialTheme.colorScheme.surfaceVariant) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(detail, style = MaterialTheme.typography.bodySmall)
                if (onResume != null) TextButton(onClick = onResume) { Text("Continuar revisão") }
            }
        }
    }
}

@Composable
internal fun AutoSubtitleSetup(sources: List<AudioTrack>, selected: String?, onSource: (String?) -> Unit,
    full: SubtitleSpeechRange, range: SubtitleSpeechRange, custom: Boolean,
    onCustom: (Boolean) -> Unit, onRange: (SubtitleSpeechRange) -> Unit) {
    var showSources by remember { mutableStateOf(false) }
    Text("Fonte da fala", style = MaterialTheme.typography.titleSmall)
    Box {
        OutlinedButton(onClick = { showSources = true }, enabled = sources.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()) {
            Text(sources.firstOrNull { it.id == selected }?.title?.ifBlank { "Narração escolhida" }
                ?: "Áudio dos clipes", maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        DropdownMenu(expanded = showSources, onDismissRequest = { showSources = false },
            modifier = Modifier.heightIn(max = 260.dp)) {
            DropdownMenuItem(text = { Text("Áudio dos clipes") }, onClick = {
                onSource(null); showSources = false
            })
            sources.forEach { audio ->
                DropdownMenuItem(text = {
                    Text(audio.title.ifBlank { "Narração escolhida" }, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }, onClick = { onSource(audio.id); showSources = false })
            }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(!custom, { onCustom(false) }, label = { Text("Projeto inteiro") })
        FilterChip(custom, {
            if (!custom && range.durationMs > SubtitleSpeechDraft.MAX_OPERATION_MS)
                onRange(SubtitleSpeechRange(0L, SubtitleSpeechDraft.MAX_OPERATION_MS))
            onCustom(true)
        }, label = { Text("Escolher trecho") }, enabled = full.durationMs > 0L)
    }
    if (custom) SubtitleSpeechRangeFields(range, full, onRange)
    if (selected == null && range.durationMs > SubtitleSpeechDraft.MAX_OPERATION_MS)
        Text("Escolha um trecho de até 10 minutos.", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error)
}

@Composable
private fun SubtitleSpeechRangeFields(range: SubtitleSpeechRange, limit: SubtitleSpeechRange,
    onRange: (SubtitleSpeechRange) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TimeDigitsField("Início", range.startMs, limit.endMs,
            valid = { it >= limit.startMs && it < range.endMs },
            onCommit = { onRange(range.copy(startMs = it)) }, modifier = Modifier.weight(1f), compact = true)
        TimeDigitsField("Fim", range.endMs, limit.endMs,
            valid = { it > range.startMs && it <= limit.endMs },
            onCommit = { onRange(range.copy(endMs = it)) }, modifier = Modifier.weight(1f), compact = true)
    }
}

@Composable
internal fun AutoSubtitleReviewList(results: List<SubtitleCue>, review: Set<String>, message: String,
    appearance: SubtitleAppearanceMode, onAppearance: (SubtitleAppearanceMode) -> Unit,
    size: Int, onSize: (Int) -> Unit, replace: Boolean, onReplace: (Boolean) -> Unit,
    project: VideoProject, range: SubtitleSpeechRange, decisions: Map<String, String>, names: Map<String, String>,
    onDecision: (String, String) -> Unit, onName: (String, String) -> Unit,
    onNew: () -> Unit, onRedo: () -> Unit, onEdit: (SubtitleCue) -> Unit) {
    var appearanceOpen by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Text("Revisar ${results.size} legendas", style = MaterialTheme.typography.titleMedium)
            if (message.isNotBlank()) Text(message, style = MaterialTheme.typography.bodySmall)
            if (review.isNotEmpty()) Text("${review.size} com palavra alterada · confira o tempo pelo áudio",
                color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(onClick = onNew, modifier = Modifier.weight(1f)) { Text("Nova transcrição") }
                TextButton(onClick = onRedo, modifier = Modifier.weight(1f)) { Text("Refazer intervalo") }
            }
            GeminiVoiceReviewUi(results, project.subtitleSpeakers, decisions, names, onDecision, onName)
            TextButton(onClick = { appearanceOpen = !appearanceOpen }) {
                Text(if (appearanceOpen) "Ocultar aparência" else "Aparência · $size sp")
            }
            if (appearanceOpen) {
                listOf(SubtitleAppearanceMode.KARAOKE_ROSA to "Rosa do vídeo",
                    SubtitleAppearanceMode.CLASSIC_SRT to "Clássica (SRT)",
                    SubtitleAppearanceMode.KARAOKE_IA to "Karaokê IA").forEach { (mode, label) ->
                    FilterChip(appearance == mode, { onAppearance(mode) }, label = { Text(label) })
                }
                Text("Tamanho · $size sp")
                Slider(value = size.toFloat(), onValueChange = { onSize(it.toInt().coerceIn(12, 30)) },
                    valueRange = 12f..30f)
            }
            if (project.subtitles.any(range::contains)) Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(replace, onReplace)
                Text("Substituir só legendas inteiras dentro do trecho",
                    style = MaterialTheme.typography.bodySmall)
            }
        }
        items(results, key = { it.id }) { cue ->
            Surface(onClick = { onEdit(cue) }, shape = UaiShapes.card,
                color = MaterialTheme.colorScheme.surfaceVariant) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(cue.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text("${formatPipMs(cue.startMs)} – ${formatPipMs(cue.endMs)}" +
                        if (cue.id in review) " · Confira o karaokê" else "",
                        style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
internal fun AutoSubtitleCueReview(project: VideoProject, cue: SubtitleCue, text: String, onText: (String) -> Unit,
    track: AudioTrack?, limits: SubtitleSpeechRange, cloud: Boolean, marked: Boolean, canUseGemini: Boolean,
    onSave: () -> Unit, onBack: () -> Unit, onLips: () -> Unit, onRun: (SubtitleSpeechRange, Boolean) -> Unit) {
    var playback by remember(cue.id) { mutableStateOf<Boolean?>(null) }
    val baseRange = SubtitleSpeechRange(cue.startMs, cue.endMs)
    val alignedWindow = SubtitleSpeechRange(maxOf(limits.startMs, cue.startMs - 450),
        minOf(limits.endMs, cue.endMs + 450))
    var range by remember(cue.id) { mutableStateOf(baseRange) }
    var showRedo by remember(cue.id) { mutableStateOf(false) }
    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Revisar trecho", style = MaterialTheme.typography.titleMedium)
        Text("${formatPipMs(cue.startMs)} – ${formatPipMs(cue.endMs)}")
        OutlinedTextField(value = text, onValueChange = { onText(it.take(500)) }, label = { Text("Texto") },
            modifier = Modifier.fillMaxWidth(), maxLines = 5)
        if (marked || SubtitleTextRetiming.edit(cue, text).needsAlignment)
            Text("Palavras alteradas. Os tempos dessas palavras são aproximados até conferir pelo áudio.",
                style = MaterialTheme.typography.bodySmall)
        playback?.let { video ->
            key(video, cue.id) { AutoSubtitlePlayback(project, cue, text, baseRange, track, video) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(onClick = { playback = if (playback == false) null else false },
                modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 5.dp)) {
                Icon(Icons.Default.PlayArrow, null, Modifier.size(16.dp))
                Text(if (playback == false) "Parar áudio" else "Ouvir trecho", maxLines = 1)
            }
            OutlinedButton(onClick = { playback = if (playback == true) null else true },
                modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 5.dp)) {
                Text(if (playback == true) "Fechar vídeo" else "Ver trecho", maxLines = 1)
            }
        }
        Text(if (track != null) "Ouvir trecho usa a narração selecionada. Ver trecho usa o som dos clipes."
            else "Ouça o trecho ou veja a legenda sobre os clipes.", style = MaterialTheme.typography.labelSmall)
        if (cloud) Text("Os ajustes pelo áudio abaixo enviam somente o trecho escolhido ao Gemini.",
            style = MaterialTheme.typography.bodySmall)
        OutlinedButton(onClick = { onRun(alignedWindow, true) },
            enabled = alignedWindow.durationMs > 0 && text.isNotBlank(),
            modifier = Modifier.fillMaxWidth()) {
            Text(if (cloud) "Enviar trecho e ajustar pelo áudio" else "Ajustar pelo áudio")
        }
        OutlinedButton(onClick = onLips, enabled = canUseGemini && text.isNotBlank(),
            modifier = Modifier.fillMaxWidth()) { Text("Conferir pela boca · Gemini") }
        TextButton(onClick = { showRedo = !showRedo }) { Text(if (showRedo) "Ocultar intervalo" else "Refazer trecho") }
        if (showRedo) {
            SubtitleSpeechRangeFields(range, limits) { range = it }
            Text("As legendas fora deste intervalo não mudam.", style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = { onRun(range, false) }, enabled = range.durationMs in
                1L..SubtitleSpeechDraft.MAX_OPERATION_MS, modifier = Modifier.fillMaxWidth()) {
                Text(if (cloud) "Enviar e refazer este trecho" else "Refazer este trecho offline")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onBack) { Text("Voltar") }
            Button(onClick = onSave, enabled = text.isNotBlank()) { Text("Salvar texto") }
        }
    }
}

@Composable
internal fun AutoSubtitleRedoReview(limit: SubtitleSpeechRange, cloud: Boolean,
    onBack: () -> Unit, onRedo: (SubtitleSpeechRange) -> Unit) {
    var range by remember(limit) { mutableStateOf(
        SubtitleSpeechRange(limit.startMs, minOf(limit.endMs, limit.startMs + 5000))) }
    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Refazer intervalo", style = MaterialTheme.typography.titleMedium)
        Text("Use este ajuste inclusive se faltou uma fala. As legendas fora da janela ficam intactas.")
        SubtitleSpeechRangeFields(range, limit) { range = it }
        if (cloud) Text("Envia somente o áudio deste intervalo ao Gemini e pode consumir cota.",
            style = MaterialTheme.typography.bodySmall)
        Button(onClick = { onRedo(range) }, enabled = range.durationMs in 1L..SubtitleSpeechDraft.MAX_OPERATION_MS,
            modifier = Modifier.fillMaxWidth()) {
            Text(if (cloud) "Enviar e refazer intervalo" else "Refazer intervalo offline")
        }
        TextButton(onClick = onBack) { Text("Voltar à revisão") }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun AutoSubtitlePlayback(project: VideoProject, cue: SubtitleCue, text: String,
    range: SubtitleSpeechRange, track: AudioTrack?, video: Boolean) {
    val context = LocalContext.current
    val engine = remember { diagnosticPlayer(context) }
    var playAt by remember { mutableLongStateOf(range.startMs) }
    var problem by remember { mutableStateOf("") }
    val aspect by produceState(16f / 9f, range, video) {
        if (video) {
            val clip = subtitleSpeechSlice(project, range).clips.firstOrNull()
            if (clip != null) value = withContext(Dispatchers.IO) { mediaAspect(context, clip.uri, clip.isImage) }
        }
    }
    DisposableEffect(engine) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                problem = "Não foi possível reproduzir a fonte escolhida."
            }
        }
        engine.addListener(listener)
        onDispose { engine.removeListener(listener); engine.release() }
    }
    LaunchedEffect(engine, range, track, video) {
        try {
            val media = prepareSubtitleSpeechSource(context, project, if (video) null else track, range)
            engine.setMediaItems(media.project.clips.map(ClipMediaItems::playback))
            engine.prepare()
            engine.play()
            while (video) {
                playAt = media.offsetMs + projectPosition(engine, media.project.clips, media.project.durationMs)
                delay(40)
            }
        } catch (cancel: CancellationException) { throw cancel }
        catch (_: Exception) { problem = "Não foi possível reproduzir a fonte escolhida." }
    }
    if (problem.isNotBlank()) Text(problem, color = MaterialTheme.colorScheme.error)
    else if (video) InlineVideoPreview(aspect, maxHeightDp = 220f) {
        AndroidView(factory = { host -> previewPlayer(host).apply { useController = false; player = engine } },
            modifier = Modifier.fillMaxSize(), update = { it.player = engine })
        if (playAt < cue.endMs) SubtitleOverlayOnFrame(cue.copy(text = text), editable = false, playbackMs = playAt)
    } else Text("Reproduzindo a fonte escolhida…", style = MaterialTheme.typography.labelSmall)
}
