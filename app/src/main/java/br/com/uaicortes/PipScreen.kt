package br.com.uaicortes

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay

// Extraído da MainActivity na v0.1.53 para reutilização de componentes.

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PipScreen(p: VideoProject, selectedId: String, onSelect: (String) -> Unit,
                      back: () -> Unit, onChange: (Overlay) -> Unit,
                      onRemove: (String) -> Unit, choose: (Boolean) -> Unit,
                      tabs: @Composable () -> Unit = {}) {
    val context = LocalContext.current
    val pip = p.overlays.firstOrNull { it.id == selectedId } ?: p.overlays.firstOrNull()
    val total = p.durationMs.coerceAtLeast(1L)
    val sourceAspect = remember(pip?.uri, pip?.isImage) {
        pip?.let { mediaAspect(context, it.uri, it.isImage) } ?: 16f / 9f
    }
    val player = remember(p.id) { diagnosticPlayer(context) }
    var playing by remember(p.id) { mutableStateOf(false) }
    var previewTime by remember(p.id) { mutableLongStateOf(pip?.startAtMs ?: 0L) }
    val canvasIndex=AdaptiveVideoLayout.activeClipIndex(p.clips.map { it.durationMs },previewTime)
    val canvasClip=p.clips.getOrNull(canvasIndex)
    val canvasAspect=remember(canvasClip?.uri,canvasClip?.isImage) {
        canvasClip?.let { mediaAspect(context,it.uri,it.isImage) } ?: 16f/9f
    }
    var expanded by remember(p.id) { mutableStateOf<String?>(null) }
    var panelVisible by remember(p.id) { mutableStateOf(false) }
    var optionsVisible by remember(p.id) { mutableStateOf(false) }
    var showTimePresets by remember(p.id) { mutableStateOf(false) }
    val tools = listOf(
        Triple("position", "Posição", Icons.Default.OpenWith),
        Triple("size", "Tamanho", Icons.Default.AspectRatio),
        Triple("duration", "Duração", Icons.Default.Schedule),
        Triple("opacity", "Opacidade", Icons.Default.Opacity)
    )
    fun openPanel(section: String) {
        player.pause()
        expanded = section
        panelVisible = true
    }
    fun closePanel() {
        panelVisible = false
        showTimePresets = false
        expanded = null
    }
    var previewExpanded by remember(p.id) { mutableStateOf(false) }
    var showPipHelp by remember(p.id) { mutableStateOf(false) }
    val overlaySourceDuration by produceState<Long?>(null, pip?.uri, pip?.isImage) {
        value = if (pip == null || pip.isImage) null else kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            verifiedMediaDuration(context, android.net.Uri.parse(pip.uri)).takeIf { it > 0L }
        }
    }
    fun applyPreset(period: PipTimePresets.Period) {
        val selected = pip ?: return
        val adjusted = PipTimePresets.limited(period, selected.isImage,
            overlaySourceDuration?.minus(selected.sourceStartMs)?.coerceAtLeast(0L))
        if (adjusted == null) {
            android.widget.Toast.makeText(context,
                "Não foi possível confirmar a duração do vídeo do PiP.",
                android.widget.Toast.LENGTH_LONG).show()
            return
        }
        if (adjusted.durationMs != period.durationMs) {
            android.widget.Toast.makeText(context,
                "PiP limitado à duração da mídia original.",
                android.widget.Toast.LENGTH_LONG).show()
        }
        onChange(selected.copy(startAtMs=adjusted.startMs, durationMs=adjusted.durationMs))
        showTimePresets = false
    }
    var manual by remember(pip?.id) {
        mutableStateOf(pip?.let { (it.scale * 100f).toInt().toString() } ?: "30")
    }
    LaunchedEffect(panelVisible, expanded) {
        if (panelVisible && expanded == "size" && pip != null) manual = (pip.scale * 100f).toInt().toString()
    }
    DisposableEffect(player) { onDispose { player.release() } }
    LaunchedEffect(p.clips) {
        if (p.clips.isNotEmpty()) {
            player.setMediaItems(p.clips.map { clip ->
                ClipMediaItems.playback(clip)
            })
            player.prepare()
            seekProjectPosition(player, p.clips, previewTime)
        }
    }
    // A seleção de camada posiciona o cursor no início dela; mexer no slider não força seek.
    LaunchedEffect(pip?.id) {
        if (pip != null && p.clips.isNotEmpty() && !player.isPlaying) {
            previewTime = pip.startAtMs
            seekProjectPosition(player, p.clips, previewTime)
        }
        closePanel()
    }
    LaunchedEffect(player, p.clips) {
        while (true) {
            playing = player.isPlaying
            if (player.isPlaying) previewTime = projectPosition(player, p.clips, p.durationMs)
            delay(150)
        }
    }
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        if (!previewExpanded) {
            Heading("Camadas", back)
            tabs()
        }
        Column(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (!previewExpanded) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (p.overlays.isEmpty()) Text("Adicione uma imagem ou vídeo", Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall)
                else Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    p.overlays.forEachIndexed { index, item ->
                        FilterChip(selected = pip?.id == item.id, onClick = { onSelect(item.id) },
                            label = { Text("PiP ${index + 1}", fontSize = 12.sp, maxLines = 1) })
                    }
                }
                IconButton(onClick = { player.pause(); choose(false) },
                    enabled = p.overlays.size < OverlayPlacement.MAX_PIPS) {
                    Icon(Icons.Default.Add, "Adicionar PiP")
                }
                if (pip != null) Box {
                    IconButton(onClick = { optionsVisible = true }) {
                        Icon(Icons.Default.MoreVert, "Mais opções do PiP selecionado")
                    }
                    DropdownMenu(expanded = optionsVisible, onDismissRequest = { optionsVisible = false }) {
                        DropdownMenuItem(text = { Text("Trocar mídia") }, onClick = {
                            optionsVisible = false
                            player.pause()
                            choose(true)
                        })
                        if (!pip.isImage) DropdownMenuItem(text = { Text("Som do PiP") }, onClick = {
                            optionsVisible = false
                            openPanel("sound")
                        })
                    }
                }
            }
            if (pip != null) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Prévia", Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    if (!previewExpanded) IconButton(onClick = { showPipHelp = !showPipHelp }) {
                        Icon(Icons.Default.HelpOutline, "Ajuda dos controles", Modifier.size(18.dp))
                    }
                    TextButton(onClick = { previewExpanded = !previewExpanded },
                        contentPadding = PaddingValues(horizontal = 5.dp, vertical = 0.dp)) {
                        Icon(if (previewExpanded) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                            if (previewExpanded) "Reduzir prévia" else "Ampliar prévia",
                            modifier = Modifier.size(19.dp))
                        Text(if (previewExpanded) "Reduzir" else "Ampliar", fontSize = 12.sp)
                    }
                }
                LayerPreviewFrame(canvasAspect, Modifier.weight(1f)) {
                    AndroidView(factory = { previewPlayer(it).apply {
                        this.player = player
                        useController = false
                    } }, update = { it.player = player }, modifier = Modifier.fillMaxSize())
                    p.overlays.forEach { layer ->
                        val active = previewTime >= layer.startAtMs &&
                            previewTime < layer.startAtMs + layer.durationMs
                        if (active || (layer.id == pip?.id && !playing)) {
                            key(layer.id) {
                                PipLayer(layer, (previewTime - layer.startAtMs).coerceAtLeast(0L),
                                    playing && active, editable = layer.id == pip?.id,
                                    onUpdate = onChange)
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Text("Vídeo:", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        EditableSeekTime(previewTime, total, onSeek = { ms ->
                            previewTime = ms
                            seekProjectPosition(player, p.clips, ms)
                        }, modifier = Modifier.weight(1f))
                    }
                    TextButton(onClick = {
                        if (player.isPlaying) player.pause() else player.play()
                        playing = player.isPlaying
                    }, contentPadding = PaddingValues(horizontal = 2.dp)) {
                        Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                            "Reproduzir ou pausar vídeo")
                        Text(if (playing) "Pausar" else "Reproduzir", fontSize = 12.sp)
                    }
                }
                Slider(value = previewTime.toFloat().coerceIn(0f, total.toFloat()),
                    onValueChange = { next ->
                        previewTime = next.toLong()
                        seekProjectPosition(player, p.clips, previewTime)
                    }, valueRange = 0f..total.toFloat(),
                    modifier = Modifier.fillMaxWidth().height(36.dp))
                if (!previewExpanded && showPipHelp) Text(
                    "Arraste ou pince o PiP. Toque no horário para digitar ou abra um ajuste abaixo.",
                    fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (!previewExpanded) Row(Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    tools.forEach { (key, label, icon) ->
                        LayerTool(label, icon, Modifier.weight(1f),
                            value = if (key == "opacity") "${(pip.opacity * 100).toInt()}%" else null) {
                            openPanel(key)
                        }
                    }
                }
            }
        }
        if (!previewExpanded) Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = { pip?.let { onRemove(it.id) } }, enabled = pip != null,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = UaiShapes.button) {
                Icon(Icons.Default.Delete, null)
                Text(" Remover")
            }
            Button(onClick = back, modifier = Modifier.weight(1f).height(48.dp),
                shape = UaiShapes.button) {
                Icon(Icons.Default.Check, null)
                Text(" Aplicar")
            }
        }
    }
    androidx.activity.compose.BackHandler(enabled = previewExpanded && !panelVisible) { previewExpanded = false }
    // ModalBottomSheet ocupa a parte inferior e fica POR CIMA dos botões.
    // A altura é limitada para preservar a visualização do vídeo na parte superior.
    if (panelVisible && pip != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val sheetHeight = (LocalConfiguration.current.screenHeightDp * 0.64f).dp
        ModalBottomSheet(
            onDismissRequest = ::closePanel,
            sheetState = sheetState,
            shape = UaiShapes.sheet,
            containerColor = MaterialTheme.colorScheme.surface,
            scrimColor = Color.Black.copy(alpha = 0.12f),
            dragHandle = null
        ) {
            Column(Modifier.fillMaxWidth()
                .heightIn(max = sheetHeight)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (expanded == "sound") "Som do PiP" else
                        "Ajustes • PiP ${p.overlays.indexOfFirst { it.id == pip.id } + 1}",
                        style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    IconButton(onClick = ::closePanel) { Icon(Icons.Default.Close, "Fechar painel") }
                }
                if (expanded != "sound") tools.forEach { (key, label, _) ->
                    val value = when (key) {
                        "size" -> "${(pip.scale * 100).toInt()}%"
                        "duration" -> displayEditableTime(pip.durationMs)
                        "opacity" -> "${(pip.opacity * 100).toInt()}%"
                        else -> null
                    }
                    LayerOptionRow(label, value, expanded == key) {
                        if (key == "size" && expanded != key) manual = (pip.scale * 100f).toInt().toString()
                        expanded = if (expanded == key) null else key
                    }
                    if (expanded == key) {
                        when (key) {
                            "position" -> {
                                Text("Escolha um dos nove pontos. Os atalhos deixam um espaço até a borda. Para sobrepor ou reposicionar PiPs em conflito, revise a confirmação.",
                                    style = MaterialTheme.typography.bodySmall)
                                val names = listOf("Superior esquerdo", "Superior central", "Superior direito",
                                    "Meio esquerdo", "Centro", "Meio direito",
                                    "Inferior esquerdo", "Inferior central", "Inferior direito")
                                val icons = listOf(Icons.Default.NorthWest, Icons.Default.North,
                                    Icons.Default.NorthEast, Icons.Default.West, Icons.Default.CenterFocusStrong,
                                    Icons.Default.East, Icons.Default.SouthWest, Icons.Default.South,
                                    Icons.Default.SouthEast)
                                if (pip != null) for (row in 0..2) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        for (col in 0..2) {
                                            val i = row * 3 + col
                                            OutlinedButton(onClick = {
                                                val (wf, hf) = OverlayEditorLogic.dimensions(
                                                    pip.scale, sourceAspect, canvasAspect,pip.fitMode)
                                                val (x, y) = OverlayEditorLogic.quickPreset(row, col, wf, hf, canvasAspect)
                                                onChange(pip.copy(x = x, y = y))
                                            }, modifier = Modifier.weight(1f).height(50.dp)
                                                .semantics { contentDescription = names[i] },
                                                shape = RoundedCornerShape(12.dp),
                                                contentPadding = PaddingValues(0.dp)) {
                                                Icon(icons[i], null)
                                            }
                                        }
                                    }
                                }
                            }
                            "size" -> {
                                if (pip != null) {
                                    Text("Enquadramento",fontWeight=FontWeight.Medium)
                                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                                        FilterChip(selected=pip.fitMode==OverlayEditorLogic.FIT,
                                            onClick={onChange(pip.copy(fitMode=OverlayEditorLogic.FIT))},
                                            label={Text("Ajustar")})
                                        FilterChip(selected=pip.fitMode==OverlayEditorLogic.FILL,
                                            onClick={onChange(pip.copy(fitMode=OverlayEditorLogic.FILL))},
                                            label={Text("Preencher")})
                                    }
                                    Slider(value = pip.scale.coerceIn(.05f, 1.50f),
                                        onValueChange = {
                                            onChange(pip.copy(scale = it))
                                            manual = (it * 100f).toInt().toString()
                                        }, valueRange = .05f..1.50f)
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(value = manual, onValueChange = { value ->
                                            if (value.length <= 3 && value.all(Char::isDigit)) manual = value
                                        }, modifier = Modifier.weight(1f), singleLine = true,
                                            label = { Text("Tamanho manual") }, suffix = { Text("%") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            isError = manual.isNotBlank() && OverlayEditorLogic.parsePercentage(manual) == null)
                                        Button(onClick = {
                                            OverlayEditorLogic.parsePercentage(manual)?.let {
                                                onChange(pip.copy(scale = it))
                                            }
                                        }, enabled = OverlayEditorLogic.parsePercentage(manual) != null) {
                                            Icon(Icons.Default.Check, null)
                                            Text(" Usar")
                                        }
                                    }
                                    Text("5% a 150%. Preencher pode recortar as bordas.",
                                        style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            "duration" -> {
                                if (pip != null) {
                                    Box {
                                        OutlinedButton(onClick = { showTimePresets = true }) {
                                            Text("Aplicar a")
                                            Icon(Icons.Default.ArrowDropDown, null)
                                        }
                                        DropdownMenu(expanded = showTimePresets,
                                            onDismissRequest = { showTimePresets = false }) {
                                            DropdownMenuItem(text = { Text("Projeto inteiro") },
                                                onClick = { applyPreset(PipTimePresets.Period(0L, total)) })
                                            PipTimePresets.clipPeriods(p.clips.map { it.durationMs })
                                                .forEachIndexed { i, period ->
                                                    DropdownMenuItem(text = {
                                                        Text("${i + 1}º clipe · ${displayEditableTime(period.durationMs)}")
                                                    }, onClick = { applyPreset(period) })
                                                }
                                            DropdownMenuItem(text = { Text("Do cursor até o fim") },
                                                enabled = total - previewTime >= 500L,
                                                onClick = { applyPreset(PipTimePresets.Period(previewTime, total)) })
                                        }
                                    }
                                    val end = (pip.startAtMs + pip.durationMs).coerceAtMost(total)
                                    val minLength = minOf(500L, total)
                                    // A edição por horários deve respeitar também o recorte
                                    // da origem feito com as alças da timeline.
                                    val sourceRemaining = if (pip.isImage) total else
                                        (overlaySourceDuration?.minus(pip.sourceStartMs)
                                            ?.coerceAtLeast(minLength) ?: pip.durationMs)
                                    fun limitedEnd(at: Long): Long =
                                        minOf(total,at+sourceRemaining)
                                            .coerceAtLeast(at+minLength).coerceAtMost(total)
                                    Row(Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically) {
                                        TimeDigitsField("Início", pip.startAtMs, total,
                                            valid={it in 0L..(end-minLength).coerceAtLeast(0L)},
                                            onCommit={ typed -> onChange(pip.copy(
                                                startAtMs=typed,durationMs=minOf(end,limitedEnd(typed))-typed)) },
                                            modifier=Modifier.weight(1f),compact=true)
                                        TimeDigitsField("Fim", end, total,
                                            valid={it in (pip.startAtMs+minLength).coerceAtMost(total)..limitedEnd(pip.startAtMs)},
                                            onCommit={ typed -> onChange(pip.copy(
                                                durationMs=typed-pip.startAtMs)) },
                                            modifier=Modifier.weight(1f),compact=true)
                                    }
                                    Text("Intervalo selecionado: ${formatPipMs(pip.durationMs)}",
                                        style=MaterialTheme.typography.bodySmall)
                                    if (total > minLength) RangeSlider(
                                        value=pip.startAtMs.toFloat().coerceIn(0f,total.toFloat())..
                                            end.toFloat().coerceIn(0f,total.toFloat()),
                                        onValueChange={ range ->
                                            val begin=range.start.toLong().coerceIn(0L,total-minLength)
                                            val finish=range.endInclusive.toLong().coerceIn(begin+minLength,total)
                                            onChange(pip.copy(startAtMs=begin,
                                                durationMs=minOf(finish,limitedEnd(begin))-begin))
                                        },
                                        valueRange=0f..total.toFloat())
                                    Text("Toque em Início ou Fim para digitar; use a barra para ajustar ambos.",
                                        style=MaterialTheme.typography.bodySmall)
                                }
                            }
                            "opacity" -> {
                                Text("Opacidade do PiP", style = MaterialTheme.typography.bodySmall)
                                Slider(value = pip.opacity.coerceIn(0f, 1f),
                                    onValueChange = { value -> onChange(pip.copy(opacity=value)) },
                                    valueRange = 0f..1f)
                            }
                        }
                    }
                }
                if (expanded == "sound" && !pip.isImage) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Som do PiP selecionado", Modifier.weight(1f))
                        Switch(checked = !pip.muted,
                            onCheckedChange = { onChange(pip.copy(muted = !it)) })
                    }
                    if (!pip.muted) {
                        Text("Volume: ${(pip.volume * 100).toInt()}%")
                        Slider(value = pip.volume.coerceIn(0f, 1f),
                            onValueChange = { onChange(pip.copy(volume = it)) }, valueRange = 0f..1f)
                    }
                }
                Button(onClick = ::closePanel,
                    modifier = Modifier.fillMaxWidth(),
                    shape = UaiShapes.item) {
                    Icon(Icons.Default.Check, null)
                    Text(" Concluído")
                }
            }
        }
    }
}

/** Tocar no horário substitui o texto por um campo numérico na própria tela. */
@Composable
internal fun EditableSeekTime(currentMs: Long, totalMs: Long, onSeek: (Long) -> Unit,
                             modifier: Modifier = Modifier) {
    var editing by remember { mutableStateOf(false) }
    if (!editing) {
        TextButton(onClick = {
            editing = true
        }, modifier = modifier, contentPadding = PaddingValues(horizontal = 3.dp)) {
            Text("${displayEditableTime(currentMs)} / ${displayEditableTime(totalMs)}",
                style = MaterialTheme.typography.bodySmall)
            Icon(Icons.Default.Edit, "Digitar horário", Modifier.size(14.dp))
        }
    } else {
        Row(modifier, verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            TimeDigitsField("Ir para", currentMs, totalMs,
                onCommit = { onSeek(it); editing = false },
                modifier = Modifier.weight(1f))
            IconButton(onClick = { editing = false }) {
                Icon(Icons.Default.Close, "Cancelar edição")
            }
        }
    }
}

internal fun displayEditableTime(ms: Long): String =
    if (ms % 1000L == 0L) formatMs(ms) else formatPipMs(ms)

internal fun formatPipMs(ms: Long): String {
    val minutes = (ms / 60000L)
    val seconds = (ms % 60000L) / 1000L
    val tenths = (ms % 1000L) / 100L
    return "%02d:%02d.%d".format(minutes, seconds, tenths)
}
