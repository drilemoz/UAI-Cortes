package br.com.uaicortes

import br.com.uaicortes.EditorDiagnosticPolicy.TransitionStage

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
import kotlinx.coroutines.withTimeoutOrNull

/** One private, small MP4 via the exporter. No still-frame animation or server upload. */
@OptIn(UnstableApi::class)
@Composable
internal fun TransitionVideoPreview(project: VideoProject, boundary: Int, kind: String, durationMs: Long,
                                    canvasAspect: Float, outputMode: PreviewOutputMode,
                                    audioOn: Boolean, onAudio: (Boolean) -> Unit,
                                    applyAll: Boolean, onApplyAll: (Boolean) -> Unit, canApplyAll: Boolean) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val diagnosis = TransitionDiagnosticContext(boundary, kind)
    val job = remember(project, boundary, kind, durationMs) {
        JunctionPreviewProject.prepare(project, boundary, kind, durationMs)?.let {
            JunctionPreviewProject.compactDirectCut(it, boundary, kind)
        }
    }
    if (job == null) {
        LaunchedEffect(boundary, kind) {
            DiagnosticRecorder.transitionStage(true, TransitionStage.PLAN_FAILED, diagnosis)
        }
        Text("Esta junção não permite gerar prévia. Confira a duração dos clipes.",
            color = MaterialTheme.colorScheme.error)
        return
    }
    val engine = remember { ExportEngine(context) }
    val player = remember { diagnosticPlayer(context).apply {
        repeatMode = Player.REPEAT_MODE_ALL
        volume = 0f
    } }
    var file by remember { mutableStateOf<File?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var progress by remember { mutableStateOf<Int?>(null) }
    var playing by remember { mutableStateOf(false) }
    var firstFrame by remember { mutableStateOf(false) }
    var renderRequested by remember { mutableStateOf(true) }
    var attempt by remember { mutableIntStateOf(0) }
    val latestAudioOn by rememberUpdatedState(audioOn)
    DisposableEffect(engine, player, lifecycle) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) { playing = isPlaying }
            override fun onRenderedFirstFrame() {
                if (file != null && error == null && !firstFrame) {
                    firstFrame = true
                    DiagnosticRecorder.previewEvent("transition", "first_frame")
                    DiagnosticRecorder.transitionStage(true, TransitionStage.FIRST_FRAME, diagnosis)
                }
            }
            override fun onPlayerError(failure: PlaybackException) {
                DiagnosticRecorder.previewEvent("transition", "play_error")
                DiagnosticRecorder.transitionStage(true, TransitionStage.PLAY_FAILED, diagnosis, failure.errorCode)
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
    LaunchedEffect(engine, attempt, renderRequested) {
        engine.cancel()
        player.stop()
        player.clearMediaItems()
        file?.delete()
        file = null
        error = null
        progress = null
        firstFrame = false
        if (!renderRequested) return@LaunchedEffect
        delay(220L) // Coalesce rapid selection changes without holding multiple Transformer jobs.
        val started = android.os.SystemClock.elapsedRealtime()
        DiagnosticRecorder.previewEvent("transition", "start", job.project.subtitles)
        DiagnosticRecorder.transitionStage(true, TransitionStage.RENDER_START, diagnosis)
        var timedOut = false
        try {
            engine.start(job.project, 360, "preview-transicao.mp4",
                preset = ExportBitratePreset.COMPACT, originalAspectOverride = canvasAspect,
                destination = outputMode.destination, frameMode = outputMode.frameMode,
                onSuccess = { _, _ -> }, onError = { error = it }, onPreviewReady = { file = it })
            val completed = withTimeoutOrNull(45_000L) {
                while (file == null && error == null) {
                    progress = engine.progressOrNull()
                    delay(180L)
                }
                true
            }
            if (completed == null && file == null && error == null) {
                timedOut = true
                engine.cancel()
                error = "A montagem demorou mais de 45 segundos para renderizar."
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            engine.cancel()
            error = failure.message ?: "Não foi possível renderizar esta junção."
        } finally {
            val outcome = when {
                timedOut -> "render_timeout"
                file != null -> "ready"
                error != null -> "failed"
                else -> "cancelled"
            }
            DiagnosticRecorder.previewEvent("transition", outcome,
                elapsedMs = android.os.SystemClock.elapsedRealtime() - started)
            DiagnosticRecorder.transitionStage(true, when (outcome) {
                "ready" -> TransitionStage.RENDER_READY
                "failed" -> TransitionStage.RENDER_FAILED
                "render_timeout" -> TransitionStage.RENDER_TIMEOUT
                else -> TransitionStage.CANCELLED
            }, diagnosis)
        }
    }
    val ready = file
    LaunchedEffect(ready) {
        if (ready != null) {
            player.volume = if (latestAudioOn) 1f else 0f
            val window = MediaItem.ClippingConfiguration.Builder()
                .setStartPositionMs(job.playbackStartMs).setEndPositionMs(job.playbackEndMs).build()
            player.setMediaItem(MediaItem.Builder().setUri(Uri.fromFile(ready))
                .setClippingConfiguration(window).build())
            player.prepare()
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) player.play()
            val displayed = withTimeoutOrNull(9_000L) {
                while (!firstFrame && error == null &&
                    lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) delay(100L)
                true
            }
            if (displayed == null && !firstFrame && error == null) {
                player.stop()
                error = "O vídeo da prévia foi gerado, mas o primeiro quadro não apareceu."
                DiagnosticRecorder.previewEvent("transition", "play_timeout")
                DiagnosticRecorder.transitionStage(true, TransitionStage.PLAY_TIMEOUT, diagnosis)
            }
        }
    }

    val failure = error
    Box(Modifier.fillMaxWidth().height(170.dp).clip(RoundedCornerShape(12.dp))
        .background(Color(0xFF151619)), contentAlignment = Alignment.Center) {
        when {
            failure != null -> Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Prévia indisponível", color = Color.White, style = MaterialTheme.typography.labelLarge)
                Text(failure, color = Color.White, style = MaterialTheme.typography.bodySmall, maxLines = 3)
                TextButton(onClick = { renderRequested = true; attempt++ }) {
                    Text("Tentar de novo", color = Color.White)
                }
            }
            !renderRequested -> TextButton(onClick = { renderRequested = true; attempt++ }) {
                Text("Carregar prévia", color = Color.White)
            }
            ready == null -> Column(horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val p = progress
                if (p == null) CircularProgressIndicator(Modifier.size(24.dp), color = Color.White)
                else CircularProgressIndicator(progress = { p / 100f }, modifier = Modifier.size(24.dp), color = Color.White)
                Text(if (p == null) "Preparando a prévia…" else "Renderizando prévia · $p%",
                    color = Color.White, style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = { renderRequested = false }) { Text("Cancelar", color = Color.White) }
            }
            else -> {
                AndroidView(factory = { ctx -> previewPlayer(ctx).apply {
                    this.player = player
                    useController = false
                } }, update = { it.player = player }, modifier = Modifier.fillMaxSize())
                if (!firstFrame) Text("Abrindo o vídeo da prévia…", color = Color.White,
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { if (player.isPlaying) player.pause() else player.play() },
            enabled = ready != null && failure == null) {
            Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                if (playing) "Pausar prévia" else "Reproduzir prévia")
        }
        Text("Prévia", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
        FilterChip(selected = applyAll, onClick = { onApplyAll(!applyAll) },
            enabled = canApplyAll, label = { Text("Aplicar a todos", maxLines = 1) })
        IconButton(onClick = { onAudio(!audioOn) }) {
            Icon(if (audioOn) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                if (audioOn) "Silenciar mixagem" else "Ouvir mixagem")
        }
    }
}
