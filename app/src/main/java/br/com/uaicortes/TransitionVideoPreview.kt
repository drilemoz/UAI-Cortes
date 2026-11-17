package br.com.uaicortes

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

/** One private, small MP4 via the exporter. No still-frame animation or server upload. */
@OptIn(UnstableApi::class)
@Composable
internal fun TransitionVideoPreview(project: VideoProject, boundary: Int, kind: String, durationMs: Long,
                                    canvasAspect: Float, audioOn: Boolean, onAudio: (Boolean) -> Unit) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val job = remember(project, boundary, kind, durationMs) {
        JunctionPreviewProject.prepare(project, boundary, kind, durationMs)
    } ?: return
    val engine = remember { ExportEngine(context) }
    val player = remember { diagnosticPlayer(context).apply {
        repeatMode = Player.REPEAT_MODE_ALL
        volume = 0f
    } }
    var file by remember { mutableStateOf<File?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var progress by remember { mutableStateOf<Int?>(null) }
    var playing by remember { mutableStateOf(false) }
    var attempt by remember { mutableIntStateOf(0) }
    val latestAudioOn by rememberUpdatedState(audioOn)
    DisposableEffect(engine, player, lifecycle) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) { playing = isPlaying }
            override fun onPlayerError(failure: PlaybackException) {
                error = "Não foi possível reproduzir a prévia renderizada."
            }
        }
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) player.pause()
        }
        player.addListener(listener)
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            player.removeListener(listener)
            engine.cancel()
            player.release()
            file?.delete()
        }
    }
    LaunchedEffect(player, audioOn) { player.volume = if (audioOn) 1f else 0f }
    LaunchedEffect(engine, attempt) {
        engine.cancel()
        player.stop()
        player.clearMediaItems()
        file?.delete()
        file = null
        error = null
        progress = null
        delay(220L) // Coalesce rapid selection changes without holding multiple Transformer jobs.
        try {
            engine.start(job.project, 360, "preview-transicao.mp4",
                preset = ExportBitratePreset.COMPACT, originalAspectOverride = canvasAspect,
                onSuccess = { _, _ -> }, onError = { error = it }, onPreviewReady = { file = it })
            while (file == null && error == null) {
                progress = engine.progressOrNull()
                delay(180L)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            engine.cancel()
            error = failure.message ?: "Não foi possível renderizar esta junção."
        }
    }
    val ready = file
    LaunchedEffect(ready) {
        if (ready != null) {
            player.volume = if (latestAudioOn) 1f else 0f
            player.setMediaItem(MediaItem.fromUri(Uri.fromFile(ready)))
            player.prepare()
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) player.play()
        }
    }

    val failure = error
    Box(Modifier.fillMaxWidth().height(170.dp).clip(RoundedCornerShape(12.dp))
        .background(Color(0xFF151619)), contentAlignment = Alignment.Center) {
        when {
            failure != null -> Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Prévia indisponível", color = Color.White, style = MaterialTheme.typography.labelLarge)
                Text(failure, color = Color.White, style = MaterialTheme.typography.bodySmall, maxLines = 3)
                TextButton(onClick = { attempt++ }) { Text("Tentar de novo", color = Color.White) }
            }
            ready == null -> Column(horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val p = progress
                if (p == null) CircularProgressIndicator(Modifier.size(24.dp), color = Color.White)
                else CircularProgressIndicator(progress = { p / 100f }, modifier = Modifier.size(24.dp), color = Color.White)
                Text("Preparando prévia da junção…", color = Color.White, style = MaterialTheme.typography.bodySmall)
            }
            else -> AndroidView(factory = { ctx -> previewPlayer(ctx).apply {
                this.player = player
                useController = false
            } }, update = { it.player = player }, modifier = Modifier.fillMaxSize())
        }
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { if (player.isPlaying) player.pause() else player.play() },
            enabled = ready != null && failure == null) {
            Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                if (playing) "Pausar prévia" else "Reproduzir prévia")
        }
        Text("Prévia em loop", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
        IconButton(onClick = { onAudio(!audioOn) }) {
            Icon(if (audioOn) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                if (audioOn) "Silenciar som dos clipes" else "Ouvir som dos clipes")
        }
    }
}
