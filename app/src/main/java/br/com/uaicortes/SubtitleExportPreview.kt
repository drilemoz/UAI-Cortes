package br.com.uaicortes

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import java.io.File
import kotlinx.coroutines.delay

@Composable
internal fun SubtitleExportPreviewDialog(project: VideoProject, cursorMs: Long, onClose: () -> Unit) {
    val context = LocalContext.current
    val quality = remember(context) { ProjectStore(context).quality() }
    var vertical by remember { mutableStateOf(false) }
    Dialog(onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 14.dp)) {
                Row(Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("Como vai exportar", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = onClose) { Icon(Icons.Default.Close, "Fechar prévia de exportação") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(!vertical, { vertical = false }, label = { Text("Original") })
                    FilterChip(vertical, { vertical = true }, label = { Text("Vertical 9:16") })
                }
                key(vertical) {
                    SubtitlePreviewRender(project, cursorMs, quality, vertical,
                        modifier = Modifier.weight(1f), onClose = onClose)
                }
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun SubtitlePreviewRender(project: VideoProject, cursorMs: Long, quality: Int, vertical: Boolean,
    modifier: Modifier, onClose: () -> Unit) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val engine = remember { ExportEngine(context) }
    val player = remember { diagnosticPlayer(context) }
    var file by remember { mutableStateOf<File?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var progress by remember { mutableStateOf<Int?>(null) }
    var attempt by remember { mutableIntStateOf(0) }
    var playing by remember { mutableStateOf(false) }
    var position by remember { mutableLongStateOf(cursorMs) }
    val total = project.durationMs.coerceAtLeast(1L)
    DisposableEffect(engine, player, lifecycle) {
        var open = true
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) { if (open) playing = isPlaying }
            override fun onPlayerError(errorValue: androidx.media3.common.PlaybackException) {
                if (open) error = "Não foi possível reproduzir a prévia renderizada."
            }
        }
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) player.pause()
        }
        player.addListener(listener)
        lifecycle.addObserver(observer)
        onDispose {
            open = false
            lifecycle.removeObserver(observer)
            player.removeListener(listener)
            engine.cancel()
            player.release()
            file?.delete()
        }
    }
    LaunchedEffect(attempt) {
        player.stop()
        player.clearMediaItems()
        file?.delete()
        file = null
        error = null
        progress = null
        try {
            engine.start(project, quality, "preview-legendas.mp4",
                destination = if (vertical) ExportDestination.SHORTS else ExportDestination.ORIGINAL,
                onSuccess = { _, _ -> }, onError = { error = it },
                onPreviewReady = { ready -> file = ready })
            while (file == null && error == null) {
                progress = engine.progressOrNull()
                delay(200)
            }
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            error = failure.message ?: "Não foi possível renderizar a prévia."
        }
    }
    val ready = file
    LaunchedEffect(ready) {
        if (ready != null) {
            player.setMediaItem(MediaItem.fromUri(Uri.fromFile(ready)))
            player.prepare()
            position = cursorMs.coerceIn(0L, (total - 1L).coerceAtLeast(0L))
            player.seekTo(position)
        }
    }
    LaunchedEffect(ready, player) {
        if (ready != null) while (true) {
            position = player.currentPosition.coerceIn(0L, total)
            delay(100)
        }
    }
    Column(modifier.fillMaxWidth()) {
        val failure = error
        if (failure != null || ready == null) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (failure == null) {
                        val percent = progress
                        if (percent == null) CircularProgressIndicator()
                        else CircularProgressIndicator(progress = { percent.coerceIn(0, 100) / 100f })
                        Text("Renderizando prévia em ${quality}p…")
                        Text("O vídeo temporário não será salvo na galeria.", style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = onClose) { Text("Cancelar") }
                    } else {
                        Text(failure, color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = { attempt++ }) { Text("Tentar novamente") }
                    }
                }
            }
        } else {
            val aspect = remember(ready) {
                mediaAspect(context, Uri.fromFile(ready).toString(), false)
            }
            LayerPreviewFrame(aspect, Modifier.weight(1f)) {
                AndroidView(factory = { ctx -> previewPlayer(ctx).apply {
                    this.player = player
                    useController = false
                } }, update = { it.player = player }, modifier = Modifier.fillMaxSize())
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    if (player.isPlaying) player.pause() else {
                        if (player.playbackState == Player.STATE_ENDED) player.seekTo(0L)
                        player.play()
                    }
                }) {
                    Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                        if (playing) "Pausar" else "Reproduzir")
                }
                Text("${displayEditableTime(position)} / ${displayEditableTime(total)}",
                    style = MaterialTheme.typography.bodySmall)
            }
            Slider(value = position.toFloat(), onValueChange = {
                position = it.toLong().coerceIn(0L, total)
                player.seekTo(position)
            }, valueRange = 0f..total.toFloat())
        }
    }
}
