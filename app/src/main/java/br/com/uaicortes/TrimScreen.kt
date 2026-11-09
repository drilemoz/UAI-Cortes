package br.com.uaicortes

import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private data class TrimHistoryFrame(val project: VideoProject, val selectedId: String)
private enum class TrimMode { SEEK, KEEP, REMOVE }

/** Editor de cortes de uma tela: prévia, faixa de quadros e três verbos simples.
 * Toda edição passa pelo projeto, nunca altera a mídia original. */
@OptIn(UnstableApi::class)
@Composable
internal fun TrimScreen(
    p: VideoProject, selectedId: String, globalMs: Long, back: () -> Unit,
    update: (VideoProject) -> Boolean, selected: (String) -> Unit,
    onDone: (VideoProject) -> Unit
) {
    val context = LocalContext.current
    val clip = p.clips.firstOrNull { it.id == selectedId } ?: p.clips.firstOrNull()
    val undo = remember(p.id) { mutableStateListOf<TrimHistoryFrame>() }
    val redo = remember(p.id) { mutableStateListOf<TrimHistoryFrame>() }
    var menuOpen by remember { mutableStateOf(false) }
    var showPrecise by remember { mutableStateOf(false) }
    var askDeleteClip by remember { mutableStateOf(false) }
    var askRemove by remember { mutableStateOf(false) }
    var mode by remember(clip?.id) { mutableStateOf(TrimMode.SEEK) }
    var start by remember(clip?.id) { mutableLongStateOf(clip?.startMs ?: 0L) }
    var end by remember(clip?.id) { mutableLongStateOf(clip?.endMs ?: 0L) }
    fun notifyUser(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
    fun commit(next: VideoProject, targetId: String): Boolean {
        if (next == p) return false
        if (!update(next)) {
            notifyUser("Não foi possível salvar a edição. Tente novamente.")
            return false
        }
        undo.add(TrimHistoryFrame(p, clip?.id.orEmpty()))
        if (undo.size > 15) undo.removeAt(0)
        redo.clear()
        selected(targetId)
        return true
    }
    fun restore(history: SnapshotStateList<TrimHistoryFrame>, inverse: SnapshotStateList<TrimHistoryFrame>) {
        if (history.isEmpty()) return
        val snapshot = history.last()
        if (update(snapshot.project)) {
            history.removeAt(history.lastIndex)
            inverse.add(TrimHistoryFrame(p, clip?.id.orEmpty()))
            selected(snapshot.selectedId)
        } else notifyUser("Não foi possível recuperar a edição.")
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
        .background(MaterialTheme.colorScheme.background)) {
        Row(Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = back, modifier = Modifier.size(46.dp)) {
                Icon(Icons.Default.ArrowBack, "Voltar ao editor")
            }
            Column(Modifier.weight(1f).padding(start = 3.dp)) {
                Text("Cortar vídeo", fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleMedium)
                Text("Simples, visual e sem perder o original", fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
            IconButton(onClick = { restore(undo, redo) }, enabled = undo.isNotEmpty()) {
                Icon(Icons.Default.Undo, "Desfazer")
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Default.MoreVert, "Mais ferramentas")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(text = { Text("Ajuste preciso") },
                        leadingIcon = { Icon(Icons.Default.Edit, null) },
                        onClick = { menuOpen = false; showPrecise = true }, enabled = clip != null && !clip.isImage)
                    DropdownMenuItem(text = { Text("Refazer") },
                        leadingIcon = { Icon(Icons.Default.Redo, null) },
                        onClick = { menuOpen = false; restore(redo, undo) }, enabled = redo.isNotEmpty())
                    DropdownMenuItem(text = { Text("Mover clipe para antes") },
                        leadingIcon = { Icon(Icons.Default.ArrowBack, null) },
                        onClick = {
                            menuOpen = false
                            clip?.let { current ->
                                val i = p.clips.indexOfFirst { it.id == current.id }
                                if (i > 0) {
                                    val reordered = p.clips.toMutableList()
                                    val displaced = reordered.removeAt(i)
                                    reordered.add(i - 1, displaced)
                                    commit(p.copy(clips = reordered), current.id)
                                }
                            }
                        }, enabled = clip != null && p.clips.indexOfFirst { it.id == clip.id } > 0)
                    DropdownMenuItem(text = { Text("Mover clipe para depois") },
                        leadingIcon = { Icon(Icons.Default.ArrowForward, null) },
                        onClick = {
                            menuOpen = false
                            clip?.let { current ->
                                val i = p.clips.indexOfFirst { it.id == current.id }
                                if (i >= 0 && i < p.clips.lastIndex) {
                                    val reordered = p.clips.toMutableList()
                                    val displaced = reordered.removeAt(i)
                                    reordered.add(i + 1, displaced)
                                    commit(p.copy(clips = reordered), current.id)
                                }
                            }
                        }, enabled = clip != null && p.clips.indexOfFirst { it.id == clip.id } in 0 until p.clips.lastIndex)
                    DropdownMenuItem(text = { Text("Excluir clipe") },
                        leadingIcon = { Icon(Icons.Default.DeleteOutline, null) },
                        onClick = { menuOpen = false; askDeleteClip = true }, enabled = p.clips.size > 1)
                }
            }
            Button(onClick = {
                if (clip != null && !clip.isImage && mode == TrimMode.KEEP &&
                    (start != clip.startMs || end != clip.endMs)) {
                    val amended = TrimSaveLogic.applyPending(p, clip.id, start, end)
                    if (amended == null) notifyUser("Ajuste o trecho antes de concluir.")
                    else onDone(amended)
                } else onDone(p)
            }, shape = UaiShapes.card,
                contentPadding = PaddingValues(horizontal = 17.dp, vertical = 12.dp)) {
                Icon(Icons.Default.Check, null, Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Pronto")
            }
        }
        if (clip == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Adicione um vídeo no editor para começar.")
            }
            return@Column
        }
        if (clip.isImage) {
            TrimImageDuration(p, clip, update, onDone)
            return@Column
        }
        val sourceTotal = clip.sourceDurationMs.coerceAtLeast(1L)
        val player = remember(clip.id) {
            diagnosticPlayer(context).apply { repeatMode = Player.REPEAT_MODE_OFF }
        }
        var cursorMs by remember(clip.id) { mutableLongStateOf(TrimTimelineEdit.cursorFor(p, clip)) }
        var isPlaying by remember(clip.id) { mutableStateOf(false) }
        var speed by remember(clip.id) { mutableFloatStateOf(1f) }
        DisposableEffect(player) { onDispose { player.release() } }
        LaunchedEffect(player, speed) { player.setPlaybackParameters(PlaybackParameters(speed)) }
        LaunchedEffect(player, clip.uri) {
            player.setMediaItem(MediaItem.fromUri(clip.uri))
            player.prepare()
            player.seekTo(cursorMs.coerceIn(clip.startMs, clip.endMs))
        }
        LaunchedEffect(player, clip.id, clip.startMs, clip.endMs) {
            // A prévia respeita os limites do trecho que restou após os cortes.
            while (true) {
                isPlaying = player.isPlaying
                val reachedEnd = TrimPreviewEndPolicy.reachedEnd(
                    player.currentPosition, clip.endMs,
                    player.playbackState == Player.STATE_ENDED
                )
                if (reachedEnd) {
                    // Uma seleção toca uma vez. Recomeçar requer novo toque em Reproduzir.
                    player.pause()
                    cursorMs = clip.endMs
                    isPlaying = false
                } else if (player.isPlaying) {
                    cursorMs = player.currentPosition.coerceIn(clip.startMs, clip.endMs)
                }
                delay(100L)
            }
        }
        LaunchedEffect(clip.id, clip.startMs, clip.endMs) {
            if (mode != TrimMode.REMOVE) {
                start = clip.startMs
                end = clip.endMs
            }
            cursorMs = cursorMs.coerceIn(clip.startMs, clip.endMs)
            player.seekTo(cursorMs)
        }
        fun seek(ms: Long) {
            player.pause()
            cursorMs = ms.coerceIn(clip.startMs, clip.endMs)
            player.seekTo(cursorMs)
        }
        fun selectMode(next: TrimMode) {
            player.pause()
            mode = if (mode == next) TrimMode.SEEK else next
            if (mode == TrimMode.REMOVE) {
                val window = minOf(1800L, clip.durationMs)
                val begin = (cursorMs - window / 2L).coerceIn(clip.startMs, clip.endMs - window)
                start = begin
                end = begin + window
            } else {
                start = clip.startMs
                end = clip.endMs
            }
        }
        // A prévia ocupa a área livre e mantém a proporção original no Media3 PlayerView.
        Box(Modifier.fillMaxWidth().weight(1f).background(Color(0xFF111115))
            .padding(horizontal = 10.dp, vertical = 4.dp), contentAlignment = Alignment.Center) {
            AndroidView(factory = { ctx -> previewPlayer(ctx).apply {
                this.player = player
                useController = false
                setBackgroundColor(android.graphics.Color.BLACK)
            } }, modifier = Modifier.fillMaxSize(), update = { it.player = player })
        }
        Surface(tonalElevation = 2.dp,
            shape = UaiShapes.sheet,
            color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("Trecho ${p.clips.indexOfFirst { it.id == clip.id } + 1}/${p.clips.size}",
                        fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.weight(1f))
                    Text("${formatPipMs(cursorMs - clip.startMs)} / ${formatPipMs(clip.durationMs)}",
                        fontWeight = FontWeight.Medium, style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary)
                }
                TrimVisualTimeline(uri = clip.uri, durationMs = sourceTotal,
                    clipStartMs = clip.startMs, clipEndMs = clip.endMs,
                    cursorMs = cursorMs, rangeStartMs = start, rangeEndMs = end,
                    editMode = mode != TrimMode.SEEK, removing = mode == TrimMode.REMOVE,
                    onSeek = { seek(it) },
                    onRangeChange = { newStart, newEnd, point ->
                        start = newStart
                        end = newEnd
                        seek(point)
                    })
                // Trechos: botões pequenos e tocáveis, sem os cartões antigos ocupando a tela.
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    p.clips.forEachIndexed { index, item ->
                        val active = item.id == clip.id
                        Surface(onClick = { player.pause(); selected(item.id) },
                            shape = UaiShapes.item,
                            color = if (active) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.height(35.dp)) {
                            Row(Modifier.padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Text("${index + 1}", fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp)
                                Spacer(Modifier.width(6.dp))
                                Text(formatPipMs(item.durationMs), fontSize = 11.sp,
                                    maxLines = 1)
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = { seek(cursorMs - 1000L) }) {
                        Icon(Icons.Default.Replay10, "Voltar no vídeo")
                    }
                    FilledIconButton(onClick = {
                        if (player.isPlaying) player.pause() else {
                            if (cursorMs >= clip.endMs - 100L) {
                                cursorMs = clip.startMs
                                player.seekTo(cursorMs)
                            }
                            player.play()
                        }
                    }, modifier = Modifier.size(43.dp)) {
                        Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            if (isPlaying) "Pausar" else "Reproduzir")
                    }
                    IconButton(onClick = { seek(cursorMs + 1000L) }) {
                        Icon(Icons.Default.Forward10, "Avançar no vídeo")
                    }
                    TextButton(onClick = { speed = PreviewPlayback.next(speed) },
                        contentPadding = PaddingValues(horizontal = 6.dp),
                        modifier = Modifier.width(62.dp)) {
                        Text(PreviewPlayback.label(speed), fontWeight = FontWeight.Bold)
                    }
                }
                when (mode) {
                    TrimMode.SEEK -> {
                        Text("Arraste a linha do tempo e escolha o ponto do corte.",
                            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2)
                    }
                    TrimMode.KEEP -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Alças roxas: escolha o que fica.", modifier = Modifier.weight(1f),
                                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            TextButton(onClick = { selectMode(TrimMode.SEEK) }) { Text("Cancelar") }
                        }
                        Button(onClick = {
                            val next = TrimSaveLogic.applyPending(p, clip.id, start, end)
                            if (next == null) notifyUser("Deixe pelo menos 0,25 s de vídeo.")
                            else if (commit(next, clip.id)) { mode = TrimMode.SEEK }
                        }, enabled = start != clip.startMs || end != clip.endMs,
                            modifier = Modifier.fillMaxWidth(), shape = UaiShapes.card) {
                            Icon(Icons.Default.Check, null)
                            Spacer(Modifier.width(6.dp))
                            Text("Guardar corte")
                        }
                    }
                    TrimMode.REMOVE -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Alças amarelas: escolha o que vai sair.", modifier = Modifier.weight(1f),
                                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            TextButton(onClick = { selectMode(TrimMode.SEEK) }) { Text("Cancelar") }
                        }
                        Button(onClick = { askRemove = true }, modifier = Modifier.fillMaxWidth(),
                            shape = UaiShapes.card, colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error)) {
                            Icon(Icons.Default.DeleteSweep, null)
                            Spacer(Modifier.width(6.dp))
                            Text("Remover este trecho (${formatPipMs(end - start)})")
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TrimMainAction(Icons.Default.ContentCut, "Dividir", mode == TrimMode.SEEK,
                        Modifier.weight(1f)) {
                        val next = TrimTimelineEdit.split(p, clip.id, cursorMs)
                        if (next == null) notifyUser("Arraste o cursor para dentro do trecho antes de dividir.")
                        else if (commit(next.project, next.selectedClipId)) mode = TrimMode.SEEK
                    }
                    TrimMainAction(Icons.Default.Crop, "Aparar", true,
                        Modifier.weight(1f), active = mode == TrimMode.KEEP) {
                        selectMode(TrimMode.KEEP)
                    }
                    TrimMainAction(Icons.Default.DeleteSweep, "Remover", true,
                        Modifier.weight(1f), active = mode == TrimMode.REMOVE) {
                        selectMode(TrimMode.REMOVE)
                    }
                }
            }
        }
    }
    if (clip != null && askRemove) {
        AlertDialog(onDismissRequest = { askRemove = false },
            title = { Text("Remover esse pedaço?") },
            text = { Text("De ${formatPipMs(start)} até ${formatPipMs(end)}. " +
                "O restante será unido. Seu vídeo original não será apagado.") },
            confirmButton = {
                Button(onClick = {
                    askRemove = false
                    val next = TrimRemoveRange.apply(p, clip.id, start, end)
                    if (next == null) notifyUser("Ajuste as alças. Não é possível apagar todo o único clipe.")
                    else if (commit(next.project, next.selectedClipId)) mode = TrimMode.SEEK
                }, shape = UaiShapes.button) { Text("Remover") }
            }, dismissButton = { TextButton(onClick = { askRemove = false }) { Text("Cancelar") } })
    }
    if (clip != null && askDeleteClip) {
        AlertDialog(onDismissRequest = { askDeleteClip = false },
            title = { Text("Excluir o clipe ${p.clips.indexOfFirst { it.id == clip.id } + 1}?") },
            text = { Text("Somente este trecho será removido do projeto. O arquivo original permanece salvo.") },
            confirmButton = {
                TextButton(onClick = {
                    askDeleteClip = false
                    TrimTimelineEdit.remove(p, clip.id)?.let { commit(it.project, it.selectedClipId) }
                }) { Text("Excluir") }
            }, dismissButton = { TextButton(onClick = { askDeleteClip = false }) { Text("Cancelar") } })
    }
    if (clip != null && showPrecise && !clip.isImage) {
        var preciseStart by remember(clip.id, showPrecise) { mutableLongStateOf(clip.startMs) }
        var preciseEnd by remember(clip.id, showPrecise) { mutableLongStateOf(clip.endMs) }
        AlertDialog(onDismissRequest = { showPrecise = false },
            title = { Text("Ajuste preciso") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Opcional: escolha os horários exatos do trecho que deseja manter.",
                        style = MaterialTheme.typography.bodySmall)
                    TimeDigitsField("Início", preciseStart, clip.sourceDurationMs,
                        valid = { TrimLogic.valid(it, preciseEnd, clip.sourceDurationMs) },
                        autoApply = true, onCommit = { preciseStart = it })
                    TimeDigitsField("Fim", preciseEnd, clip.sourceDurationMs,
                        valid = { TrimLogic.valid(preciseStart, it, clip.sourceDurationMs) },
                        autoApply = true, onCommit = { preciseEnd = it })
                }
            }, confirmButton = {
                TextButton(onClick = {
                    val next = TrimSaveLogic.applyPending(p, clip.id, preciseStart, preciseEnd)
                    if (next == null) notifyUser("Início e fim inválidos.")
                    else { commit(next, clip.id); showPrecise = false }
                }) { Text("Aplicar") }
            }, dismissButton = { TextButton(onClick = { showPrecise = false }) { Text("Cancelar") } })
    }
}

@Composable
private fun TrimMainAction(icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String, enabled: Boolean, modifier: Modifier = Modifier, active: Boolean = false,
    onClick: () -> Unit) {
    val container = if (active) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant
    Surface(onClick = onClick, enabled = enabled, modifier = modifier.height(62.dp),
        shape = UaiShapes.card, color = container) {
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
            Icon(icon, title, modifier = Modifier.size(23.dp))
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.Medium,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ColumnScope.TrimImageDuration(p: VideoProject, clip: Clip,
    update: (VideoProject) -> Boolean, onDone: (VideoProject) -> Unit) {
    val context = LocalContext.current
    var seconds by remember(clip.id) { mutableIntStateOf((clip.durationMs / 1000L).toInt().coerceIn(1, 30)) }
    val bitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, clip.uri) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(Uri.parse(clip.uri))?.use {
                    BitmapFactory.decodeStream(it, null, bounds)
                }
                var sample = 1
                while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 1000) sample *= 2
                val options = BitmapFactory.Options().apply { inSampleSize = sample }
                context.contentResolver.openInputStream(Uri.parse(clip.uri))?.use {
                    BitmapFactory.decodeStream(it, null, options)
                }?.asImageBitmap()
            }.getOrNull()
        }
    }
    Box(Modifier.fillMaxWidth().weight(1f).background(Color(0xFF111115)),
        contentAlignment = Alignment.Center) {
        if (bitmap != null) Image(bitmap!!, "Foto selecionada",
            Modifier.fillMaxSize().padding(12.dp), contentScale = ContentScale.Fit)
        else Icon(Icons.Default.Image, "Foto", tint = Color.White)
    }
    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Quanto tempo a foto fica na tela?", style = MaterialTheme.typography.titleMedium)
        Text("$seconds segundos", style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary)
        Slider(value = seconds.toFloat(), valueRange = 1f..30f,
            steps = 28, onValueChange = { seconds = it.toInt().coerceIn(1,30) })
        Button(onClick = {
            val changed = ImageClipPolicy.resizeInProject(p, clip.id, seconds)
            if (update(changed)) onDone(changed)
            else Toast.makeText(context, "Não foi possível salvar a foto.", Toast.LENGTH_SHORT).show()
        }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = UaiShapes.button) { Text("Guardar duração") }
    }
}
