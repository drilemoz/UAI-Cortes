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
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private data class TrimHistoryFrame(val project: VideoProject, val selectedId: String)

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
    val marks = remember(p.id) { mutableStateMapOf<String, TrimWindow>() }
    val currentWindow = clip?.let(TrimMarksPolicy::clipWindow) ?: TrimWindow(0L, 0L)
    val keep = clip?.id?.let { marks[it] } ?: currentWindow
    var removing by remember(clip?.id, currentWindow) { mutableStateOf<TrimWindow?>(null) }
    val shownRange = removing ?: keep
    val hasMarks = clip != null && keep != currentWindow
    val canRemove = removing?.let { TrimMarksPolicy.canRemove(keep, it, p.clips.size > 1) } ?: false
    var playbackRequest by remember(clip?.id) { mutableIntStateOf(0) }
    var removeRequest by remember(clip?.id) { mutableIntStateOf(0) }
    fun setKeep(id: String, changed: TrimWindow) {
        if (changed == currentWindow) marks.remove(id) else marks[id] = changed
    }
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
            // O histórico restaura projetos completos, não mistura seus limites com marcas de outro estado.
            val restoredById = snapshot.project.clips.associateBy { it.id }
            marks.keys.toList().forEach { id ->
                val original = p.clips.firstOrNull { it.id == id }
                val restored = restoredById[id]
                if (original != restored) marks.remove(id)
            }
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
                Text("Escolha a parte que fica", fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
            IconButton(onClick = {
                if (removing != null) removing = null
                else if (hasMarks && clip != null) { marks.remove(clip.id); playbackRequest++ }
                else restore(undo, redo)
            }, enabled = undo.isNotEmpty() || hasMarks || removing != null) {
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
                    if (clip != null && !clip.isImage) DropdownMenuItem(
                        text = { Text("Selecionar intervalo para remover") },
                        leadingIcon = { Icon(Icons.Default.DeleteSweep, null) },
                        onClick = {
                            menuOpen = false
                            removeRequest++
                        }, enabled = keep.durationMs >= 2 * TrimLogic.MIN_SEGMENT_MS)
                    DropdownMenuItem(text = { Text("Excluir clipe") },
                        leadingIcon = { Icon(Icons.Default.DeleteOutline, null) },
                        onClick = { menuOpen = false; askDeleteClip = true }, enabled = p.clips.size > 1)
                }
            }
            Button(onClick = {
                val amended = TrimMarksPolicy.applyAll(p, marks)
                if (amended == null) notifyUser("Ajuste o trecho antes de concluir.")
                else onDone(amended)
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
            TrimImageDuration(p, clip, update) { changed ->
                val amended = TrimMarksPolicy.applyAll(changed, marks)
                if (amended == null) notifyUser("Ajuste o trecho antes de concluir.")
                else onDone(amended)
            }
            return@Column
        }
        val screenHeight = LocalConfiguration.current.screenHeightDp
        val controlsLimit = (screenHeight * .52f).coerceAtLeast(128f).dp
        val player = remember(clip.id) {
            diagnosticPlayer(context).apply { repeatMode = Player.REPEAT_MODE_OFF }
        }
        var cursorMs by remember(clip.id, clip.uri) { mutableLongStateOf(TrimTimelineEdit.cursorFor(p, clip)) }
        var isPlaying by remember(clip.id) { mutableStateOf(false) }
        var speed by remember(clip.id) { mutableFloatStateOf(1f) }
        var activeMediaWindow by remember(player) { mutableStateOf<TrimWindow?>(null) }
        var playingCut by remember(player) { mutableStateOf(false) }
        val frameFps by produceState<Float?>(null, clip.uri) {
            value = withContext(Dispatchers.IO) { VideoFrameRate.read(context, clip.uri) }
        }
        DisposableEffect(player) { onDispose { player.release() } }
        LaunchedEffect(player, speed) { player.setPlaybackParameters(PlaybackParameters(speed)) }
        fun load(window: TrimWindow, at: Long, autoPlay: Boolean) {
            player.pause()
            activeMediaWindow = window
            cursorMs = window.clamp(at)
            // A faixa de mídia termina na marca. Não depende da frequência de atualização da UI.
            val media = ClipMediaItems.playback(clip.copy(startMs = window.startMs, endMs = window.endMs))
            player.setMediaItem(media, cursorMs - window.startMs)
            player.prepare()
            if (autoPlay) player.play()
        }
        LaunchedEffect(player, clip.uri, currentWindow, playbackRequest) {
            playingCut = false
            load(currentWindow, cursorMs, false)
        }
        LaunchedEffect(player) {
            while (true) {
                val window = activeMediaWindow
                isPlaying = player.isPlaying
                if (window != null) {
                    val localMs = player.currentPosition
                    if (TrimPreviewEndPolicy.reachedEnd(localMs, window.durationMs,
                            player.playbackState == Player.STATE_ENDED)) {
                        if (player.playWhenReady) player.pause()
                        cursorMs = window.endMs
                        isPlaying = false
                    } else if (player.isPlaying) {
                        cursorMs = window.clamp(window.startMs + localMs)
                    }
                }
                delay(35L)
            }
        }
        fun pauseAtCursor(): Long {
            val window = activeMediaWindow
            if (player.isPlaying && window != null)
                cursorMs = window.clamp(window.startMs + player.currentPosition)
            player.pause()
            return cursorMs
        }
        fun seek(ms: Long) {
            player.pause()
            val target = currentWindow.clamp(ms)
            if (playingCut || activeMediaWindow != currentWindow) {
                playingCut = false
                load(currentWindow, target, false)
            } else {
                cursorMs = target
                player.seekTo(target - currentWindow.startMs)
            }
        }
        LaunchedEffect(clip.id, removeRequest) {
            if (removeRequest > 0) {
                val center = pauseAtCursor()
                val width = minOf(1800L, keep.durationMs)
                val begin = (center - width / 2).coerceIn(keep.startMs, keep.endMs - width)
                removing = TrimWindow(begin, begin + width)
            }
        }
        fun mark(start: Boolean) {
            val changed = TrimMarksPolicy.mark(currentWindow, keep, pauseAtCursor(), start)
            if (changed == null) notifyUser("Deixe pelo menos 0,25 s entre início e fim.")
            else setKeep(clip.id, changed)
        }
        fun previewCut() {
            if (playingCut && player.playWhenReady) {
                pauseAtCursor()
                return
            }
            playingCut = true
            load(keep, keep.startMs, true)
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
            Column(Modifier.fillMaxWidth().heightIn(max = controlsLimit)
                .verticalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 7.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("Trecho ${p.clips.indexOfFirst { it.id == clip.id } + 1}/${p.clips.size}",
                        fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.weight(1f))
                    Text("${formatPipMs(cursorMs - clip.startMs)} / ${formatPipMs(clip.durationMs)}",
                        fontWeight = FontWeight.Medium, style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary)
                }
                TrimVisualTimeline(uri = clip.uri, clipStartMs = clip.startMs, clipEndMs = clip.endMs,
                    cursorMs = cursorMs, rangeStartMs = shownRange.startMs, rangeEndMs = shownRange.endMs,
                    handleStartMs = if (removing != null) keep.startMs else clip.startMs,
                    handleEndMs = if (removing != null) keep.endMs else clip.endMs,
                    removing = removing != null,
                    onSeek = { seek(it) },
                    onRangeChange = { newStart, newEnd, point ->
                        val changed = TrimWindow(newStart, newEnd)
                        if (removing != null) removing = changed else setKeep(clip.id, changed)
                        seek(point)
                    })
                if (removing == null) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TrimMainAction(Icons.Default.FirstPage, "Marcar início",
                            currentWindow.durationMs >= TrimLogic.MIN_SEGMENT_MS, Modifier.weight(1f),
                            active = keep.startMs != currentWindow.startMs) { mark(true) }
                        TrimMainAction(Icons.Default.LastPage, "Marcar fim",
                            currentWindow.durationMs >= TrimLogic.MIN_SEGMENT_MS, Modifier.weight(1f),
                            active = keep.endMs != currentWindow.endMs) { mark(false) }
                    }
                    Text("De ${formatPipMs(keep.startMs - clip.startMs)} a ${formatPipMs(keep.endMs - clip.startMs)}" +
                        " · Mantém ${formatPipMs(keep.durationMs)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                // Trechos: botões pequenos e tocáveis, sem os cartões antigos ocupando a tela.
                if (p.clips.size > 1) Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
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
                    horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    IconButton(onClick = { seek(TrimMarksPolicy.step(currentWindow, cursorMs, -1, frameFps)) },
                        enabled = cursorMs > currentWindow.startMs) {
                        Icon(Icons.Default.SkipPrevious, "Voltar aproximadamente um quadro")
                    }
                    FilledIconButton(onClick = {
                        if (player.playWhenReady) pauseAtCursor() else {
                            val at = if (cursorMs >= currentWindow.endMs) currentWindow.startMs else cursorMs
                            if (playingCut || activeMediaWindow != currentWindow) {
                                playingCut = false
                                load(currentWindow, at, true)
                            } else {
                                player.seekTo(at - currentWindow.startMs)
                                player.play()
                            }
                        }
                    }, modifier = Modifier.size(43.dp)) {
                        Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            if (isPlaying) "Pausar" else "Reproduzir")
                    }
                    IconButton(onClick = { seek(TrimMarksPolicy.step(currentWindow, cursorMs, 1, frameFps)) },
                        enabled = cursorMs < currentWindow.endMs) {
                        Icon(Icons.Default.SkipNext, "Avançar aproximadamente um quadro")
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { speed = PreviewPlayback.next(speed) },
                        contentPadding = PaddingValues(horizontal = 6.dp)) {
                        Text(PreviewPlayback.label(speed), fontWeight = FontWeight.Bold)
                    }
                }
                if (removing != null) {
                    Text("A faixa laranja marca o intervalo que vai sair.", style = MaterialTheme.typography.labelMedium)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = { removing = null; pauseAtCursor() }, modifier = Modifier.weight(1f)) {
                            Text("Cancelar")
                        }
                        Button(onClick = { askRemove = true }, enabled = canRemove, modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                            Text("Remover trecho")
                        }
                    }
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = ::previewCut,
                            enabled = keep.durationMs >= TrimLogic.MIN_SEGMENT_MS, modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp)) {
                            Text(if (playingCut && isPlaying) "Pausar prévia" else "Prévia do corte", maxLines = 1)
                        }
                        TextButton(onClick = {
                            val point = pauseAtCursor()
                            val base = if (hasMarks) TrimSaveLogic.applyPending(p, clip.id, keep.startMs, keep.endMs) else p
                            val next = base?.let { TrimTimelineEdit.split(it, clip.id, point) }
                            if (next == null) notifyUser("Escolha um ponto dentro da parte mantida.")
                            else if (commit(next.project, next.selectedClipId)) marks.remove(clip.id)
                        }, enabled = TrimLogic.canSplit(keep.startMs, keep.endMs, cursorMs),
                            modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 4.dp)) {
                            Text("Dividir aqui", maxLines = 1)
                        }
                    }
                }
            }
        }
    }
    val removal = removing
    if (clip != null && askRemove && removal != null) {
        AlertDialog(onDismissRequest = { askRemove = false },
            title = { Text("Remover esse pedaço?") },
            text = { Text("De ${formatPipMs(removal.startMs - clip.startMs)} até " +
                "${formatPipMs(removal.endMs - clip.startMs)}. O restante será unido. O original não muda.") },
            confirmButton = {
                Button(onClick = {
                    askRemove = false
                    val base = if (hasMarks) TrimSaveLogic.applyPending(p, clip.id, keep.startMs, keep.endMs) else p
                    val next = base?.let { TrimRemoveRange.apply(it, clip.id, removal.startMs, removal.endMs) }
                    if (next == null) notifyUser("Ajuste as alças para preservar os trechos restantes.")
                    else if (commit(next.project, next.selectedClipId)) {
                        marks.remove(clip.id)
                        removing = null
                    }
                }, enabled = canRemove, shape = UaiShapes.button) { Text("Remover") }
            }, dismissButton = { TextButton(onClick = { askRemove = false }) { Text("Cancelar") } })
    }
    if (clip != null && askDeleteClip) {
        AlertDialog(onDismissRequest = { askDeleteClip = false },
            title = { Text("Excluir o clipe ${p.clips.indexOfFirst { it.id == clip.id } + 1}?") },
            text = { Text("Somente este trecho será removido do projeto. O arquivo original permanece salvo.") },
            confirmButton = {
                TextButton(onClick = {
                    askDeleteClip = false
                    TrimTimelineEdit.remove(p, clip.id)?.let {
                        if (commit(it.project, it.selectedClipId)) marks.remove(clip.id)
                    }
                }) { Text("Excluir") }
            }, dismissButton = { TextButton(onClick = { askDeleteClip = false }) { Text("Cancelar") } })
    }
    if (clip != null && showPrecise && !clip.isImage) {
        var preciseStart by remember(clip.id, showPrecise) { mutableLongStateOf(keep.startMs) }
        var preciseEnd by remember(clip.id, showPrecise) { mutableLongStateOf(keep.endMs) }
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
                    val changed = TrimWindow(preciseStart, preciseEnd)
                    if (TrimMarksPolicy.containsValid(currentWindow, changed)) {
                        setKeep(clip.id, changed)
                        showPrecise = false
                        removing = null
                    } else {
                        // O ajuste avançado ainda pode trazer de volta bordas da mídia original.
                        val next = TrimSaveLogic.applyPending(p, clip.id, preciseStart, preciseEnd)
                        if (next == null) notifyUser("Início e fim inválidos.")
                        else if (commit(next, clip.id)) { marks.remove(clip.id); showPrecise = false }
                    }
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
    Surface(onClick = onClick, enabled = enabled, modifier = modifier.height(50.dp),
        shape = UaiShapes.card, color = container) {
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
            Icon(icon, null, modifier = Modifier.size(19.dp))
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
