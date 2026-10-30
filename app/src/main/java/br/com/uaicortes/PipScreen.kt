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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.annotation.OptIn
import kotlinx.coroutines.delay

// Extraído da MainActivity na v0.1.53 para reutilização de componentes.

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PipScreen(p: VideoProject, selectedId: String, onSelect: (String) -> Unit,
                      back: () -> Unit, onChange: (Overlay) -> Unit,
                      onRemove: (String) -> Unit, choose: (Boolean) -> Unit) {
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
    val canvasUri=p.clips.getOrNull(canvasIndex)?.uri
    val canvasAspect=remember(canvasUri) {
        canvasUri?.let { mediaAspect(context,it,false) } ?: 16f/9f
    }
    var expanded by remember(p.id) { mutableStateOf<String?>(null) }
    var manual by remember(pip?.id) {
        mutableStateOf(pip?.let { (it.scale * 100f).toInt().toString() } ?: "30")
    }
    DisposableEffect(player) { onDispose { player.release() } }
    LaunchedEffect(p.clips) {
        if (p.clips.isNotEmpty()) {
            player.setMediaItems(p.clips.map { clip ->
                MediaItem.Builder().setUri(clip.uri).setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(clip.startMs).setEndPositionMs(clip.endMs).build()
                ).build()
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
        expanded = null
    }
    LaunchedEffect(player, p.clips) {
        while (true) {
            playing = player.isPlaying
            if (player.isPlaying) previewTime = projectPosition(player, p.clips, p.durationMs)
            delay(150)
        }
    }
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        Heading("Editar sobreposições", back)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text("PiPs · ${p.overlays.size} de 4", fontWeight = FontWeight.SemiBold)
                TextButton(onClick = { choose(false) },
                    enabled = p.overlays.size < OverlayPlacement.MAX_PIPS) {
                    Icon(Icons.Default.Add, null)
                    Text(" Adicionar PiP")
                }
            }
            if (p.overlays.isEmpty()) {
                Text("Adicione até quatro imagens ou vídeos para sobrepor ao projeto.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    p.overlays.forEachIndexed { index, item ->
                        val selected = pip?.id == item.id
                        OutlinedButton(onClick = { onSelect(item.id) },
                            border = BorderStroke(if (selected) 2.dp else 1.dp,
                                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
                            Icon(if (item.isImage) Icons.Default.Image else Icons.Default.Videocam,
                                null, Modifier.size(18.dp))
                            Text(" PiP ${index + 1}")
                            if (selected) Icon(Icons.Default.Check, null, Modifier.size(16.dp))
                        }
                    }
                }
                Text("Prévia no vídeo", fontWeight = FontWeight.SemiBold)
                InlineVideoPreview(canvasAspect) {
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
                        Text(if (playing) "Pausar" else "Ver prévia")
                    }
                }
                Slider(value = previewTime.toFloat().coerceIn(0f, total.toFloat()),
                    onValueChange = { next ->
                        previewTime = next.toLong()
                        seekProjectPosition(player, p.clips, previewTime)
                    }, valueRange = 0f..total.toFloat(),
                    modifier = Modifier.fillMaxWidth().height(40.dp))
                Text("Toque nos horários para digitar. Arraste ou pince o PiP na prévia.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                // Quatro ferramentas compactas na MESMA linha; os ajustes ficam em um sheet inferior.
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        Triple("position", "Posição", Icons.Default.OpenWith),
                        Triple("size", "Tamanho", Icons.Default.AspectRatio),
                        Triple("duration", "Duração", Icons.Default.Schedule),
                        Triple("media", "Mídia", Icons.Default.PermMedia)
                    ).forEach { (key, label, icon) ->
                        Surface(onClick = { if (pip != null) expanded = key },
                            modifier = Modifier.weight(1f).aspectRatio(1f),
                            enabled = pip != null,
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant) {
                            Column(Modifier.fillMaxSize().padding(4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center) {
                                Icon(icon, contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp))
                                Spacer(Modifier.height(5.dp))
                                Text(label, fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp, maxLines = 1,
                                    overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
                // Som é independente dos demais controles e das demais sobreposições.
                if (pip != null && !pip.isImage) {
                    Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(16.dp)) {
                        Column(Modifier.fillMaxWidth().padding(12.dp)) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Icon(if (pip.muted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                    "Som deste PiP", tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(10.dp))
                                Text("Som do PiP selecionado", modifier = Modifier.weight(1f))
                                Switch(checked = !pip.muted,
                                    onCheckedChange = { onChange(pip.copy(muted = !it)) })
                            }
                            if (!pip.muted) Slider(value = pip.volume.coerceIn(0f, 1f),
                                onValueChange = { onChange(pip.copy(volume = it)) }, valueRange = 0f..1f)
                        }
                    }
                }
                Text("Até quatro PiPs independentes. Sobreposições serão confirmadas antes de aplicar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = { pip?.let { onRemove(it.id) } }, enabled = pip != null,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Default.Delete, null)
                Text(" Remover")
            }
            Button(onClick = back, modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Default.Check, null)
                Text(" Aplicar")
            }
        }
    }
    // ModalBottomSheet ocupa a parte inferior e fica POR CIMA dos botões.
    // A altura é limitada para preservar a visualização do vídeo na parte superior.
    if (expanded != null && pip != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val sheetHeight = (LocalConfiguration.current.screenHeightDp * 0.64f).dp
        ModalBottomSheet(
            onDismissRequest = { expanded = null },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
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
                    Icon(when (expanded) {
                        "position" -> Icons.Default.OpenWith
                        "size" -> Icons.Default.AspectRatio
                        "duration" -> Icons.Default.Schedule
                        else -> Icons.Default.PermMedia
                    }, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
                    Text(when (expanded) {
                        "position" -> "Alinhar posição"
                        "size" -> "Tamanho do PiP"
                        "duration" -> "Duração do PiP"
                        else -> "Selecionar mídia"
                    }, style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f))
                    IconButton(onClick = { expanded = null }) {
                        Icon(Icons.Default.Close, "Fechar painel")
                    }
                }
                                when (expanded) {
                                    "position" -> {
                                        Text("Escolha um dos nove pontos. Se houver outro PiP, ele será reposicionado automaticamente.",
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
                                                            pip.scale, sourceAspect, canvasAspect)
                                                        val (x, y) = OverlayEditorLogic.preset(row, col, wf, hf)
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
                                            Text("Tamanho: ${(pip.scale * 100f).toInt()}%",
                                                fontWeight = FontWeight.Medium)
                                            listOf(5, 10, 15, 20, 25, 30, 40, 50, 60, 75, 90).chunked(4).forEach { row ->
                                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                                    row.forEach { percent ->
                                                        OutlinedButton(onClick = {
                                                            val scale = percent / 100f
                                                            onChange(pip.copy(scale = scale))
                                                            manual = percent.toString()
                                                        }, modifier = Modifier.weight(1f),
                                                            shape = RoundedCornerShape(10.dp),
                                                            contentPadding = PaddingValues(0.dp),
                                                            border = BorderStroke(if ((pip.scale * 100f).toInt() == percent) 2.dp else 1.dp,
                                                                MaterialTheme.colorScheme.primary)) {
                                                            Text("$percent%")
                                                        }
                                                    }
                                                    repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                                                }
                                            }
                                            Slider(value = pip.scale.coerceIn(.05f, .90f),
                                                onValueChange = {
                                                    onChange(pip.copy(scale = it))
                                                    manual = (it * 100f).toInt().toString()
                                                }, valueRange = .05f..90f/100f)
                                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                OutlinedTextField(value = manual, onValueChange = { value ->
                                                    if (value.length <= 3 && value.all(Char::isDigit)) manual = value
                                                }, modifier = Modifier.weight(1f), singleLine = true,
                                                    label = { Text("Porcentagem manual") }, suffix = { Text("%") },
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
                                            Text("De 5% a 90%; aspecto original preservado.",
                                                style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                    "duration" -> {
                                        if (pip != null) {
                                            val end = (pip.startAtMs + pip.durationMs).coerceAtMost(total)
                                            Text("Digite somente números; a máscara preenche mm:ss.d automaticamente.",
                                                style = MaterialTheme.typography.bodySmall)
                                            val minLength = minOf(500L, end.coerceAtLeast(1L))
                                            TimeDigitsField("Começa em", pip.startAtMs, total,
                                                valid = { it in 0L..(end - minLength).coerceAtLeast(0L) },
                                                onCommit = { typed -> onChange(pip.copy(
                                                    startAtMs = typed, durationMs = end - typed)) })
                                            Slider(value = pip.startAtMs.coerceIn(0L, end).toFloat(),
                                                onValueChange = { value ->
                                                    val (newStart, newDuration) = OverlayEditorLogic.startChanged(
                                                        value.toLong(), pip.startAtMs, pip.durationMs, total)
                                                    onChange(pip.copy(startAtMs = newStart, durationMs = newDuration))
                                                }, valueRange = 0f..end.coerceAtLeast(1L).toFloat())
                                            val minEnd = (pip.startAtMs + 500L).coerceAtMost(total)
                                            TimeDigitsField("Termina em", end, total,
                                                valid = { it in minEnd..total },
                                                onCommit = { typed -> onChange(pip.copy(
                                                    durationMs = typed - pip.startAtMs)) })
                                            if (minEnd < total) {
                                                Slider(value = end.coerceIn(minEnd, total).toFloat(),
                                                    onValueChange = { value ->
                                                        onChange(pip.copy(durationMs = OverlayEditorLogic.endChanged(
                                                            value.toLong(), pip.startAtMs, total)))
                                                    }, valueRange = minEnd.toFloat()..total.toFloat())
                                            } else Text("Limite do vídeo alcançado")
                                            BoxWithConstraints(Modifier.fillMaxWidth().height(16.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = .14f))) {
                                                Box(Modifier.offset(x = maxWidth * (pip.startAtMs.toFloat() / total))
                                                    .width(maxWidth * (pip.durationMs.toFloat() / total))
                                                    .fillMaxHeight().clip(RoundedCornerShape(8.dp))
                                                    .background(MaterialTheme.colorScheme.primary))
                                            }
                                        }
                                    }
                                    "media" -> {
                                        Text("Troque somente a mídia do PiP selecionado.",
                                            style = MaterialTheme.typography.bodySmall)
                                        Button(onClick = { expanded = null; choose(true) }, modifier = Modifier.fillMaxWidth()) {
                                            Icon(Icons.Default.PermMedia, null)
                                            Text(" Escolher imagem ou vídeo")
                                        }
                                    }
                                }
                Button(onClick = { expanded = null },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)) {
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
