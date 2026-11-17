package br.com.uaicortes

import androidx.annotation.OptIn
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TextWatermarkScreen(p: VideoProject, back: () -> Unit, atMs: Long,
    tabs: @Composable () -> Unit = {}, onApply: (VideoProject) -> Unit) {
    val context = LocalContext.current
    val savedPresets = remember(context) { WatermarkDefaultsStore(context) }
    var draft by remember(p.id) { mutableStateOf(p.textWatermark ?: TextWatermark()) }
    var panel by remember(p.id) { mutableStateOf<String?>(null) }
    var previewExpanded by remember(p.id) { mutableStateOf(false) }
    var guides by remember(p.id) { mutableStateOf(false) }
    var removeConfirm by remember(p.id) { mutableStateOf(false) }
    val player = remember(p.id) { diagnosticPlayer(context) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val total = p.durationMs
    val lengths = remember(p.clips) { p.clips.map(Clip::durationMs) }
    val periods = remember(lengths) { PipTimePresets.clipPeriods(lengths) }
    var previewMs by remember(p.id) { mutableLongStateOf(atMs.coerceIn(0L, total)) }
    var playing by remember(player) { mutableStateOf(false) }
    val frameMs = previewMs.coerceIn(0L, (total - 1L).coerceAtLeast(0L))
    val index = AdaptiveVideoLayout.activeClipIndex(lengths, frameMs)
    val clip = p.clips.getOrNull(index)
    val aspect = remember(clip?.uri, clip?.isImage) {
        clip?.let { mediaAspect(context, it.uri, it.isImage) } ?: 16f / 9f
    }
    val cue = remember(draft, p.clips) { draft.toSubtitle(p.clips) }
    val scopeIndex = p.clips.indexOfFirst { it.id == draft.clipId }
    val validScope = draft.clipId == null || scopeIndex >= 0
    val scopeCaption = when {
        draft.clipId == null -> "Projeto todo"
        scopeIndex >= 0 -> "Clipe ${scopeIndex + 1}"
        else -> "Revisar"
    }
    fun seek(value: Long) {
        val target = value.coerceIn(0L, (total - 1L).coerceAtLeast(0L))
        val clipIndex = AdaptiveVideoLayout.activeClipIndex(lengths, target)
        val period = periods.getOrNull(clipIndex) ?: return
        previewMs = target
        player.seekTo(clipIndex, target - period.startMs)
    }
    DisposableEffect(player, lifecycleOwner) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) { playing = isPlaying }
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) previewMs = total
            }
        }
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) player.pause()
        }
        player.addListener(listener)
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            player.removeListener(listener)
        }
    }
    DisposableEffect(player) { onDispose { player.release() } }
    LaunchedEffect(p.clips, player) {
        if (p.clips.isNotEmpty()) {
            player.setMediaItems(p.clips.map(ClipMediaItems::playback))
            player.prepare()
            seek(previewMs)
        }
    }
    LaunchedEffect(p.clips, player, playing) {
        while (playing && player.isPlaying) {
            previewMs = projectPosition(player, p.clips, total)
            delay(100L)
        }
    }
    if (removeConfirm) AlertDialog(onDismissRequest = { removeConfirm = false },
        title = { Text("Remover marca d'água?") },
        text = { Text("Somente o texto deste projeto será removido. O vídeo original será preservado.") },
        confirmButton = { TextButton(onClick = {
            removeConfirm = false
            onApply(p.copy(textWatermark = null))
        }) { Text("Remover") } },
        dismissButton = { TextButton(onClick = { removeConfirm = false }) { Text("Cancelar") } })
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        if (!previewExpanded) {
            Heading("Camadas", back)
            tabs()
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Prévia", fontWeight = FontWeight.SemiBold)
            TextButton(onClick = { previewExpanded = !previewExpanded },
                contentPadding = PaddingValues(horizontal = 5.dp, vertical = 0.dp)) {
                Icon(if (previewExpanded) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                    contentDescription = null, modifier = Modifier.size(19.dp))
                Text(if (previewExpanded) "Reduzir" else "Ampliar", fontSize = 12.sp)
            }
        }
        LayerPreviewFrame(aspect, Modifier.weight(1f).padding(horizontal = 14.dp)) {
            AndroidView(factory = { ctx -> previewPlayer(ctx).apply {
                this.player = player
                useController = false
            } }, update = { it.player = player }, modifier = Modifier.fillMaxSize())
            p.overlays.forEach { layer ->
                if (frameMs >= layer.startAtMs && frameMs - layer.startAtMs < layer.durationMs) {
                    key(layer.id) {
                        PipLayer(layer, frameMs - layer.startAtMs, playing, editable = false,
                            onUpdate = {})
                    }
                }
            }
            if (guides) Box(Modifier.fillMaxSize().padding(12.dp)
                .border(1.dp, Color.White.copy(alpha = .70f), RoundedCornerShape(5.dp)))
            cue?.takeIf { frameMs >= it.startMs && frameMs < it.endMs }?.let { watermark ->
                SubtitleOverlayOnFrame(watermark, editable = true, playbackMs = frameMs,
                    onPositionCommit = { x, y -> draft = draft.copy(x = x, y = y) })
            }
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${displayEditableTime(previewMs)} / ${displayEditableTime(total)}",
                    style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = {
                    if (player.isPlaying || player.playWhenReady) player.pause()
                    else {
                        if (player.playbackState == Player.STATE_ENDED) seek(0L)
                        player.play()
                    }
                }, enabled = total > 0L) {
                    Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, null,
                        modifier = Modifier.size(20.dp))
                    Text(if (playing) "Pausar" else "Reproduzir")
                }
            }
            if (total > 0L) Slider(
                value = previewMs.coerceIn(0L, total).toFloat(),
                onValueChange = { player.pause(); seek(it.toLong()) },
                valueRange = 0f..total.toFloat(),
                modifier = Modifier.fillMaxWidth().height(36.dp)
                    .semantics { contentDescription = "Posição da prévia" })
            if (!previewExpanded) {
                if (!validScope) Text("O clipe da marca foi removido. Escolha outra duração.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                else if (draft.text.isNotBlank() && cue != null && (frameMs < cue.startMs || frameMs >= cue.endMs))
                    Text("Neste trecho a marca d'água não aparece.", style = MaterialTheme.typography.bodySmall)
                Row(Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    listOf(
                        Triple("text", "Texto", Icons.Default.Edit),
                        Triple("position", "Posição", Icons.Default.OpenWith),
                        Triple("duration", "Duração", Icons.Default.Schedule),
                        Triple("style", "Estilo", Icons.Default.Palette)
                    ).forEach { (id, label, icon) ->
                        LayerTool(label, icon, Modifier.weight(1f),
                            value = if (id == "duration") scopeCaption else null) {
                            player.pause()
                            panel = id
                        }
                    }
                }
            }
        }
        if (!previewExpanded) Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = { removeConfirm = true }, enabled = p.textWatermark != null,
                modifier = Modifier.weight(1f).height(48.dp), shape = UaiShapes.button) {
                Icon(Icons.Default.DeleteOutline, null, Modifier.size(18.dp)); Text(" Remover")
            }
            Button(onClick = {
                val safe = draft.normalized()
                onApply(p.copy(textWatermark = safe.takeIf { it.text.isNotBlank() }))
            }, enabled = validScope, modifier = Modifier.weight(1f).height(48.dp), shape = UaiShapes.button) {
                Icon(Icons.Default.Check, null, Modifier.size(18.dp)); Text(" Aplicar")
            }
        }
    }
    androidx.activity.compose.BackHandler(enabled = previewExpanded && panel == null) { previewExpanded = false }
    panel?.let { current ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val height = (LocalConfiguration.current.screenHeightDp * .64f).dp
        ModalBottomSheet(onDismissRequest = { panel = null }, sheetState = sheetState,
            shape = UaiShapes.sheet, containerColor = MaterialTheme.colorScheme.surface,
            scrimColor = Color.Black.copy(alpha = .12f), dragHandle = null) {
            Column(Modifier.fillMaxWidth().heightIn(max = height).verticalScroll(rememberScrollState())
                .imePadding().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(when (current) {
                        "text" -> "Texto da marca d'água"
                        "position" -> "Alinhar posição"
                        "duration" -> "Duração da marca"
                        else -> "Estilo da marca"
                    }, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = { panel = null }) { Icon(Icons.Default.Close, "Fechar painel") }
                }
                when (current) {
                    "text" -> OutlinedTextField(value = draft.text,
                        onValueChange = { draft = draft.copy(text = it.take(80)) },
                        label = { Text("Texto da marca d'água") }, modifier = Modifier.fillMaxWidth(),
                        maxLines = 2, supportingText = { Text("${draft.text.length}/80") })
                    "position" -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Guias de segurança (apenas prévia)", Modifier.weight(1f),
                                style = MaterialTheme.typography.bodySmall)
                            Switch(checked = guides, onCheckedChange = { guides = it })
                        }
                        Text("Posição rápida", style = MaterialTheme.typography.titleSmall)
                        val positions = listOf(
                            Triple("↖", .15f, .18f), Triple("↑", .50f, .18f), Triple("↗", .85f, .18f),
                            Triple("←", .15f, .52f), Triple("•", .50f, .52f), Triple("→", .85f, .52f),
                            Triple("↙", .15f, .90f), Triple("↓", .50f, .90f), Triple("↘", .85f, .90f))
                        val names = listOf("Superior esquerdo", "Superior central", "Superior direito",
                                        "Meio esquerdo", "Centro", "Meio direito",
                                        "Inferior esquerdo", "Inferior central", "Inferior direito")
                        positions.chunked(3).forEachIndexed { row, line ->
                            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                line.forEachIndexed { col, (symbol, x, y) ->
                                    val selected = kotlin.math.abs(draft.x - x) < .01f && kotlin.math.abs(draft.y - y) < .01f
                                    OutlinedButton(onClick = { draft = draft.copy(x = x, y = y) },
                                        modifier = Modifier.weight(1f).height(44.dp)
                                            .semantics { contentDescription = names[row * 3 + col] },
                                        contentPadding = PaddingValues(2.dp),
                                        border = BorderStroke(if (selected) 2.dp else 1.dp,
                                            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)) {
                                        Text(symbol)
                                    }
                                }
                            }
                        }
                    }
                    "duration" -> {
                        WatermarkDurationChoice("Projeto inteiro", draft.clipId == null,
                            displayEditableTime(total)) {
                            draft = draft.copy(clipId = null)
                            panel = null
                        }
                        p.clips.forEachIndexed { i, item ->
                            val name = if (!item.isImage && i == 0) "Só no 1º vídeo"
                                else "${i + 1}º clipe · ${if (item.isImage) "foto" else "vídeo"}"
                            WatermarkDurationChoice(name, draft.clipId == item.id,
                                "${displayEditableTime(item.durationMs)} · ${item.title}") {
                                draft = draft.copy(clipId = item.id)
                                periods.getOrNull(i)?.let { seek(it.startMs) }
                                panel = null
                            }
                        }
                    }
                    else -> WatermarkStylePanel(draft, { draft = it }, savedPresets)
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun WatermarkDurationChoice(label: String, selected: Boolean, detail: String, onSelect: () -> Unit) {
    Row(Modifier.fillMaxWidth().selectable(selected, role = Role.RadioButton, onClick = onSelect)
        .heightIn(min = 48.dp).padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selected, onClick = null)
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleSmall)
            Text(detail, style = MaterialTheme.typography.bodySmall, maxLines = 1)
        }
    }
}

@Composable
internal fun WatermarkStepSlider(title: String, value: Int, min: Int, max: Int,
    onChange: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, Modifier.weight(1f))
            Text(value.toString())
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onChange((value - 1).coerceAtLeast(min)) }, enabled = value > min,
                modifier = Modifier.size(40.dp)) { Icon(Icons.Default.Remove, "Diminuir $title") }
            Slider(value = value.toFloat(), valueRange = min.toFloat()..max.toFloat(),
                steps = if (max - min <= 30) (max - min - 1).coerceAtLeast(0) else 0,
                onValueChange = { onChange(it.toInt().coerceIn(min, max)) },
                modifier = Modifier.weight(1f))
            IconButton(onClick = { onChange((value + 1).coerceAtMost(max)) }, enabled = value < max,
                modifier = Modifier.size(40.dp)) { Icon(Icons.Default.Add, "Aumentar $title") }
        }
    }
}
