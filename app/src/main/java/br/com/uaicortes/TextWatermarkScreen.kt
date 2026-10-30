package br.com.uaicortes

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi

// Extraído da MainActivity na v0.1.53 para reutilização de componentes.

/** Editor local da marca d'água. O rascunho só altera o projeto após Aplicar. */
@OptIn(UnstableApi::class)
@Composable
internal fun TextWatermarkScreen(p: VideoProject, back: () -> Unit,
    onApply: (VideoProject) -> Unit) {
    val context = LocalContext.current
    var draft by remember(p.id) { mutableStateOf(p.textWatermark ?: TextWatermark()) }
    val player = remember(p.id) { diagnosticPlayer(context) }
    DisposableEffect(player) { onDispose { player.release() } }
    LaunchedEffect(p.clips.firstOrNull()?.uri, p.clips.firstOrNull()?.startMs) {
        p.clips.firstOrNull()?.let { clip ->
            player.setMediaItem(MediaItem.Builder().setUri(clip.uri)
                .setClippingConfiguration(MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(clip.startMs)
                    .setEndPositionMs(clip.endMs).build()).build())
            player.prepare()
        }
    }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Heading("Marca d'água em texto", back)
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Texto fixo durante todo o vídeo. A prévia usa o quadro original; nenhum arquivo é alterado.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            val first = p.clips.firstOrNull()
            val aspect = remember(first?.uri) {
                first?.uri?.let { mediaAspect(context, it, false) } ?: 16f / 9f
            }
            InlineVideoPreview(aspect) {
                AndroidView(factory = { ctx -> previewPlayer(ctx).apply {
                    this.player = player
                    useController = false
                } }, update = { it.player = player }, modifier = Modifier.fillMaxSize())
                draft.toSubtitle(p.durationMs)?.let { watermark ->
                    SubtitleOverlayOnFrame(watermark, editable = false, playbackMs = 0L)
                }
            }
            OutlinedTextField(value = draft.text,
                onValueChange = { draft = draft.copy(text = it.take(80)) },
                label = { Text("Texto da marca d'água") },
                placeholder = { Text("Ex.: Meu canal") },
                modifier = Modifier.fillMaxWidth(), maxLines = 2)
            Text("Tamanho: ${draft.sizeSp}")
            Slider(value = draft.sizeSp.toFloat(),
                onValueChange = { draft = draft.copy(sizeSp = it.toInt().coerceIn(12, 36)) },
                valueRange = 12f..36f)
            Text("Transparência: ${(draft.opacity * 100).toInt()}% de opacidade")
            Slider(value = draft.opacity, onValueChange = { draft = draft.copy(opacity = it) },
                valueRange = .1f..1f)
            Text("Cor")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Branca" to 0xFFFFFFFF.toInt(), "Amarela" to 0xFFFFFF00.toInt(),
                    "Preta" to 0xFF000000.toInt()).forEach { (label, color) ->
                    OutlinedButton(onClick = { draft = draft.copy(color = color) },
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(if (draft.color == color) 2.dp else 1.dp,
                            if (draft.color == color) Blue else MaterialTheme.colorScheme.outline)) {
                        Text(label, fontSize = 11.sp)
                    }
                }
            }
            Text("Posição no vídeo")
            val positions = listOf(
                Triple("↖", .15f, .18f), Triple("↑", .5f, .18f), Triple("↗", .85f, .18f),
                Triple("←", .15f, .52f), Triple("•", .5f, .52f), Triple("→", .85f, .52f),
                Triple("↙", .15f, .9f), Triple("↓", .5f, .9f), Triple("↘", .85f, .9f))
            positions.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { (symbol, x, y) ->
                        OutlinedButton(onClick = { draft = draft.copy(x = x, y = y) },
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(if (draft.x == x && draft.y == y) 2.dp else 1.dp,
                                if (draft.x == x && draft.y == y) Blue else MaterialTheme.colorScheme.outline)) {
                            Text(symbol)
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = { onApply(p.copy(textWatermark = null)) },
                modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.DeleteOutline, null, modifier = Modifier.size(17.dp))
                Text(" Remover")
            }
            Button(onClick = {
                val safe = draft.normalized()
                onApply(p.copy(textWatermark = safe.takeIf { it.text.isNotBlank() }))
            }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Check, null, modifier = Modifier.size(17.dp))
                Text(" Aplicar")
            }
        }
    }
}
