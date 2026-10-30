package br.com.uaicortes

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackParameters
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.delay

// Extraído da MainActivity na v0.1.53 para reutilização de componentes.

@OptIn(UnstableApi::class)
@Composable
internal fun TrimScreen(p: VideoProject, selectedId: String, globalMs: Long, back: () -> Unit,
                       update: (VideoProject) -> Unit, selected: (String) -> Unit,
                       onSaveHome: (VideoProject) -> Unit) {
    val context = LocalContext.current
    val clip = p.clips.firstOrNull { it.id == selectedId } ?: p.clips.firstOrNull()
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        Heading("Cortar e unir", back)
        if (clip == null) {
            Text("Adicione um vídeo para editar.", modifier = Modifier.padding(16.dp))
            return@Column
        }
        val total = clip.sourceDurationMs.coerceAtLeast(1L)
        val player = remember(clip.id) { diagnosticPlayer(context).apply { repeatMode = Player.REPEAT_MODE_ONE } }
        var playbackSpeed by remember(clip.id) { mutableFloatStateOf(1f) }
        LaunchedEffect(player, playbackSpeed) { player.setPlaybackParameters(PlaybackParameters(playbackSpeed)) }
        DisposableEffect(player) { onDispose { player.release() } }
        var cursorMs by remember(clip.id) { mutableLongStateOf(TrimTimelineEdit.cursorFor(p, clip)) }
        var playing by remember(clip.id) { mutableStateOf(false) }
        var begin by remember(clip.id) { mutableLongStateOf(clip.startMs) }
        var finish by remember(clip.id) { mutableLongStateOf(clip.endMs) }
        // Um nível de desfazer cobre aplicar, dividir, reordenar e excluir.
        var undoClips by remember(p.id) { mutableStateOf<List<Clip>?>(null) }
        var undoSelected by remember(p.id) { mutableStateOf("") }
        var undoLastCut by remember(p.id) { mutableStateOf<Pair<String?, Long?>?>(null) }
        LaunchedEffect(clip.id, clip.startMs, clip.endMs) {
            begin = clip.startMs
            finish = clip.endMs
            if (cursorMs !in clip.startMs..clip.endMs) {
                cursorMs = cursorMs.coerceIn(clip.startMs, clip.endMs)
                player.seekTo(cursorMs)
            }
        }
        LaunchedEffect(clip.id, clip.uri) {
            player.setMediaItem(MediaItem.fromUri(clip.uri))
            player.prepare()
            player.seekTo(cursorMs)
        }
        LaunchedEffect(player) {
            while (true) {
                playing = player.isPlaying
                if (player.isPlaying) {
                    cursorMs = TrimLogic.clampCursor(player.currentPosition, total)
                                    }
                delay(90L)
            }
        }
        fun seek(ms: Long) {
            cursorMs = TrimLogic.clampCursor(ms, total)
            player.seekTo(cursorMs)
        }
        fun notifyUser(message: String) {
            android.widget.Toast.makeText(context, message,
                android.widget.Toast.LENGTH_SHORT).show()
        }
        fun rememberUndo() {
            undoClips = p.clips.toList()
            undoSelected = clip.id
            undoLastCut = p.lastTrimCutClipId to p.lastTrimCutSourceMs
        }
        val validRange = TrimLogic.valid(begin, finish, total)
        val dirty = begin != clip.startMs || finish != clip.endMs
        val idx = p.clips.indexOfFirst { it.id == clip.id }
        val canSplit = !dirty && TrimLogic.canSplit(clip.startMs, clip.endMs, cursorMs)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Clipe ${idx + 1} de ${p.clips.size}", fontWeight = FontWeight.SemiBold)
                Text("Escolha uma parte na faixa abaixo", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            // Prévia e miniaturas são blocos independentes: nenhum RangeSlider cobre as fotos.
            val videoAspect = remember(clip.uri) { mediaAspect(context, clip.uri, false) }
            InlineVideoPreview(videoAspect) {
                AndroidView(factory = { ctx -> previewPlayer(ctx).apply {
                    this.player = player
                    useController = false
                } }, modifier = Modifier.fillMaxSize(), update = { it.player = player })
            }
            TrimThumbnails(clip.uri, total) { time -> player.pause(); seek(time) }
            // Uma única barra para navegar; início e fim são definidos nos campos abaixo.
            Slider(value = cursorMs.toFloat().coerceIn(0f, total.toFloat()),
                onValueChange = { player.pause(); seek(it.toLong()) },
                valueRange = 0f..total.toFloat(), modifier = Modifier.fillMaxWidth().height(30.dp))
            TrimSegmentStrip(p, clip.id) { id -> player.pause(); selected(id) }
            if (p.lastTrimCutClipId == clip.id && p.lastTrimCutSourceMs != null) {
                Text("Último corte: ${formatPipMs(p.lastTrimCutSourceMs)} · cursor recuperado automaticamente.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary)
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                TimeDigitsField("Tempo", cursorMs, total, autoApply = true,
                    onCommit = { player.pause(); seek(it) }, modifier = Modifier.weight(1f))
                IconButton(onClick = { player.pause(); seek(cursorMs - 1000L) }) {
                    Icon(Icons.Default.KeyboardDoubleArrowLeft, "Recuar 1 segundo")
                }
                IconButton(onClick = { if (player.isPlaying) player.pause() else player.play() }) {
                    Icon(if (playing) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                        "Reproduzir ou pausar", Modifier.size(30.dp))
                }
                IconButton(onClick = { player.pause(); seek(cursorMs + 1000L) }) {
                    Icon(Icons.Default.KeyboardDoubleArrowRight, "Avançar 1 segundo")
                }
                PlaybackSpeedButton(playbackSpeed, onSelect = { playbackSpeed = it })
            }
            Text("Duração do vídeo: ${formatPipMs(total)} · toque no tempo ou nas miniaturas para navegar.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TimeDigitsField("Início", begin, total,
                    valid = { TrimLogic.valid(it, finish, total) },
                    onCommit = { begin = it }, autoApply = true,
                    modifier = Modifier.weight(1f))
                TimeDigitsField("Fim", finish, total,
                    valid = { TrimLogic.valid(begin, it, total) },
                    onCommit = { finish = it }, autoApply = true,
                    modifier = Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    if (TrimLogic.valid(cursorMs, finish, total)) begin = cursorMs
                    else notifyUser("Para marcar início, escolha um ponto antes do fim do trecho.")
                }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(4.dp)) {
                    Icon(Icons.Default.Flag, null, Modifier.size(17.dp))
                    Text(" Marcar início", fontSize = 12.sp, maxLines = 1)
                }
                OutlinedButton(onClick = {
                    if (TrimLogic.valid(begin, cursorMs, total)) finish = cursorMs
                    else notifyUser("Para marcar fim, avance o vídeo após o início (mínimo 0,25 s).")
                }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(4.dp)) {
                    Icon(Icons.Default.Flag, null, Modifier.size(17.dp))
                    Text(" Marcar fim", fontSize = 12.sp, maxLines = 1)
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Trecho: ${if (validRange) formatPipMs(finish - begin) else "inválido"}",
                    style = MaterialTheme.typography.bodyMedium)
                if (dirty) Text("Não aplicado", color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelSmall)
            }
            Button(onClick = {
                rememberUndo()
                update(p.copy(clips = p.clips.map { if (it.id == clip.id)
                    it.copy(startMs = begin, endMs = finish) else it }))
            }, modifier = Modifier.fillMaxWidth().height(46.dp), enabled = validRange && dirty,
                shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.Default.Check, null)
                Text(" Aplicar corte")
            }
            Text("Dividir seleciona o próximo trecho e coloca o cursor no corte; toque nas partes acima para escolher.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            // Rolagem horizontal impede esmagar os seis cartões em telas pequenas.
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                TrimQuickAction(Icons.Default.ContentCut, "Dividir", true, Modifier.width(86.dp)) {
                    when {
                        dirty -> notifyUser("Aplique ou desfaça o corte antes de dividir.")
                        !canSplit -> notifyUser("Coloque o cursor dentro do trecho, afastado das extremidades.")
                        else -> {
                            val result = TrimTimelineEdit.split(p, clip.id, cursorMs)
                            if (result == null) notifyUser("Não foi possível dividir neste ponto.")
                            else {
                                rememberUndo()
                                player.pause()
                                update(result.project)
                                selected(result.selectedClipId) // abre a metade direita exatamente no corte
                            }
                        }
                    }
                }
                TrimQuickAction(Icons.Default.Undo, "Desfazer", undoClips != null,
                    Modifier.width(86.dp)) {
                    undoClips?.let { snapshot ->
                        begin = snapshot.firstOrNull { it.id == undoSelected }?.startMs ?: begin
                        finish = snapshot.firstOrNull { it.id == undoSelected }?.endMs ?: finish
                        update(p.copy(clips = snapshot,
                            lastTrimCutClipId = undoLastCut?.first,
                            lastTrimCutSourceMs = undoLastCut?.second))
                        selected(undoSelected)
                        undoClips = null
                        undoLastCut = null
                    }
                }
                TrimQuickAction(Icons.Default.ArrowBack, "Antes", idx > 0,
                    Modifier.width(86.dp)) {
                    rememberUndo()
                    val list = p.clips.toMutableList()
                    val before = list[idx - 1]
                    list[idx - 1] = list[idx]
                    list[idx] = before
                    update(p.copy(clips = list))
                }
                TrimQuickAction(Icons.Default.ArrowForward, "Depois", idx < p.clips.lastIndex,
                    Modifier.width(86.dp)) {
                    rememberUndo()
                    val list = p.clips.toMutableList()
                    val after = list[idx + 1]
                    list[idx + 1] = list[idx]
                    list[idx] = after
                    update(p.copy(clips = list))
                }
                TrimQuickAction(Icons.Default.Delete, "Excluir", p.clips.size > 1,
                    Modifier.width(86.dp)) {
                    TrimTimelineEdit.remove(p, clip.id)?.let { result ->
                        rememberUndo()
                        player.pause()
                        update(result.project)
                        selected(result.selectedClipId)
                    }
                }
                TrimQuickAction(Icons.Default.Save, "Salvar", true, Modifier.width(86.dp)) {
                    val saved = TrimSaveLogic.applyPending(p, clip.id, begin, finish)
                    if (saved == null) notifyUser("Corte inválido. Ajuste início e fim antes de salvar.")
                    else onSaveHome(saved)
                }
            }
            Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .58f),
                shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(7.dp))
                        Text("Mini tutorial · Cortar e unir", fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyMedium)
                    }
                    Text("1. Toque nas miniaturas, arraste a barra ou digite Tempo para escolher o ponto.",
                        style = MaterialTheme.typography.bodySmall)
                    Text("2. Marcar início/fim define o trecho; Aplicar corte confirma. O original não muda.",
                        style = MaterialTheme.typography.bodySmall)
                    Text("3. Dividir cria duas partes, seleciona a da direita e lembra o último corte. Toque nas partes para alternar.",
                        style = MaterialTheme.typography.bodySmall)
                    Text("4. Excluir remove só a parte selecionada; Desfazer recupera a última edição. Salvar volta à Home.",
                        style = MaterialTheme.typography.bodySmall)
                    Text("5. O vídeo repete até pausar; velocidade de 0,25× a 2× afeta só a prévia, não o MP4 exportado.",
                        style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

