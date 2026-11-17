package br.com.uaicortes

import androidx.annotation.OptIn
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi

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
    var message by remember(p.id) { mutableStateOf("") }
    val player = remember(p.id) { diagnosticPlayer(context) }
    val previewMs = atMs.coerceIn(0L, (p.durationMs - 1L).coerceAtLeast(0L))
    val index = AdaptiveVideoLayout.activeClipIndex(p.clips.map(Clip::durationMs), previewMs)
    val clip = p.clips.getOrNull(index)
    val aspect = remember(clip?.uri, clip?.isImage) {
        clip?.let { mediaAspect(context, it.uri, it.isImage) } ?: 16f / 9f
    }
    DisposableEffect(player) { onDispose { player.release() } }
    LaunchedEffect(p.clips, previewMs) {
        player.setMediaItems(p.clips.map(ClipMediaItems::playback))
        if (p.clips.isNotEmpty()) {
            player.prepare()
            seekProjectPosition(player, p.clips, previewMs)
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
        Heading("Camadas", back)
        tabs()
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Prévia", fontWeight = FontWeight.SemiBold)
                TextButton(onClick = { previewExpanded = !previewExpanded },
                    contentPadding = PaddingValues(horizontal = 5.dp, vertical = 0.dp)) {
                    Icon(if (previewExpanded) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        contentDescription = null, modifier = Modifier.size(19.dp))
                    Text(if (previewExpanded) "Reduzir" else "Ampliar", fontSize = 12.sp)
                }
            }
            InlineVideoPreview(aspect, maxHeightDp = if (previewExpanded) null else 200f) {
                AndroidView(factory = { ctx -> previewPlayer(ctx).apply {
                    this.player = player
                    useController = false
                } }, update = { it.player = player }, modifier = Modifier.fillMaxSize())
                p.overlays.forEach { layer ->
                    if (previewMs >= layer.startAtMs && previewMs - layer.startAtMs < layer.durationMs) {
                        key(layer.id) {
                            PipLayer(layer, previewMs - layer.startAtMs, false, editable = false,
                                onUpdate = {})
                        }
                    }
                }
                if (guides) Box(Modifier.fillMaxSize().padding(12.dp)
                    .border(1.dp, Color.White.copy(alpha = .70f), RoundedCornerShape(5.dp)))
                draft.toSubtitle(p.durationMs)?.let { watermark ->
                    SubtitleOverlayOnFrame(watermark, editable = true, playbackMs = previewMs,
                        onPositionCommit = { x, y -> draft = draft.copy(x = x, y = y) })
                }
            }
            Text(if (draft.text.isBlank()) "Toque em Texto para criar a marca d'água."
                else "Arraste a marca na prévia ou toque em Posição.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                listOf(
                    Triple("text", "Texto", Icons.Default.Edit),
                    Triple("position", "Posição", Icons.Default.OpenWith),
                    Triple("size", "Tamanho", Icons.Default.AspectRatio),
                    Triple("style", "Aparência", Icons.Default.Palette)
                ).forEach { (id, label, icon) ->
                    LayerTool(label, icon, Modifier.weight(1f)) { panel = id }
                }
            }
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = UaiShapes.item) {
                Box(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp)) {
                    WatermarkStepSlider("Opacidade (%)", (draft.opacity * 100).toInt(), 0, 100) {
                        draft = draft.copy(opacity = it / 100f)
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = { removeConfirm = true }, enabled = p.textWatermark != null,
                modifier = Modifier.weight(1f).height(48.dp), shape = UaiShapes.button) {
                Icon(Icons.Default.DeleteOutline, null, Modifier.size(18.dp)); Text(" Remover")
            }
            Button(onClick = {
                val safe = draft.normalized()
                onApply(p.copy(textWatermark = safe.takeIf { it.text.isNotBlank() }))
            }, modifier = Modifier.weight(1f).height(48.dp), shape = UaiShapes.button) {
                Icon(Icons.Default.Check, null, Modifier.size(18.dp)); Text(" Aplicar")
            }
        }
    }
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
                        "size" -> "Tamanho do texto"
                        else -> "Aparência"
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
                    "size" -> WatermarkStepSlider("Tamanho", draft.sizeSp, 12, 36) {
                        draft = draft.copy(sizeSp = it)
                    }
                    else -> {
                        Text("Cor", style = MaterialTheme.typography.titleSmall)
                        listOf("Branco" to -1, "Amarelo" to 0xFFFFFF00.toInt(),
                            "Preto" to 0xFF000000.toInt(), "Azul" to 0xFF3399FF.toInt(),
                            "Vermelho" to 0xFFFF4444.toInt()).chunked(3).forEach { group ->
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                group.forEach { (label, color) -> FilterChip(
                                    selected = draft.color == color,
                                    onClick = { draft = draft.copy(color = color) },
                                    label = { Text(label, fontSize = 11.sp) }) }
                            }
                        }
                        Text("Fonte", style = MaterialTheme.typography.titleSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("Moderna" to SubtitleStyles.FONT_SANS,
                                "Serif" to SubtitleStyles.FONT_SERIF,
                                "Mono" to SubtitleStyles.FONT_MONO).forEach { (label, font) ->
                                FilterChip(selected = draft.fontFamily == font,
                                    onClick = { draft = draft.copy(fontFamily = font) },
                                    label = { Text(label) })
                            }
                        }
                        WatermarkStepSlider("Contorno", draft.outlineWidth, 0, 4) {
                            draft = draft.copy(outlineWidth = it)
                        }
                        WatermarkStepSlider("Sombra", draft.shadowOffsetDp, 0, 6) {
                            draft = draft.copy(shadowOffsetDp = it)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = {
                                val model = savedPresets.load()
                                message = if (model != null) { draft = model; "Padrão carregado." }
                                    else "Nenhum padrão salvo neste aparelho."
                            }, modifier = Modifier.weight(1f)) { Text("Usar padrão", fontSize = 12.sp) }
                            OutlinedButton(onClick = {
                                message = if (savedPresets.save(draft)) "Padrão salvo localmente."
                                    else "Falha ao salvar o padrão."
                            }, modifier = Modifier.weight(1f)) { Text("Salvar padrão", fontSize = 12.sp) }
                        }
                        if (message.isNotBlank()) Text(message, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun WatermarkStepSlider(title: String, value: Int, min: Int, max: Int,
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
                steps = (max-min-1).coerceAtLeast(0),
                onValueChange = { onChange(it.toInt().coerceIn(min, max)) },
                modifier = Modifier.weight(1f))
            IconButton(onClick = { onChange((value + 1).coerceAtMost(max)) }, enabled = value < max,
                modifier = Modifier.size(40.dp)) { Icon(Icons.Default.Add, "Aumentar $title") }
        }
    }
}
