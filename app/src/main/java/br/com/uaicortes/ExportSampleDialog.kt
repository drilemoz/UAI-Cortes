package br.com.uaicortes

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(UnstableApi::class)
@Composable
internal fun ExportSampleDialog(project: VideoProject, position: Long, quality: Int,
    preset: ExportBitratePreset, destination: ExportDestination, frameMode: VerticalFrameMode, close: () -> Unit) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var seconds by remember { mutableIntStateOf(5) }
    val duration = minOf(seconds * 1000L, project.durationMs)
    val maxStart = project.durationMs - duration
    var selected by remember { mutableLongStateOf(position) }
    val start = selected.coerceIn(0, maxStart)
    var requested by remember { mutableStateOf(false) }
    var file by remember { mutableStateOf<File?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val engine = remember { ExportEngine(context) }
    val player = remember { diagnosticPlayer(context) }
    DisposableEffect(player, lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_PAUSE) player.pause() }
        val listener = object : Player.Listener {
            override fun onPlayerError(failure: PlaybackException) { error = "Falha ao reproduzir o trecho." }
        }
        lifecycle.addObserver(observer)
        player.addListener(listener)
        onDispose {
            lifecycle.removeObserver(observer)
            player.removeListener(listener)
        }
    }
    DisposableEffect(engine, player) {
        onDispose { engine.cancel(); player.release(); file?.delete() }
    }
    LaunchedEffect(requested) {
        if (!requested) return@LaunchedEffect
        try {
            val (sample, aspect) = withContext(Dispatchers.IO) {
                val first = project.clips.first()
                ExportSamplePlan.prepare(project, start, duration) { uri ->
                    verifiedMediaDuration(context, Uri.parse(uri))
                } to if (destination.isVertical) null
                    else mediaAspect(context, first.uri, first.isImage)
            }
            engine.start(sample.project, quality, "teste-trecho.mp4", preset, destination, frameMode,
                originalAspectOverride = aspect, onSuccess = { _, _ -> }, onError = { error = it },
                onPreviewReady = { mp4 ->
                    file = mp4
                    val trim = MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(sample.playFromMs).setEndPositionMs(sample.playUntilMs).build()
                    player.setMediaItem(MediaItem.Builder().setUri(Uri.fromFile(mp4)).setClippingConfiguration(trim).build())
                    player.repeatMode = Player.REPEAT_MODE_ONE
                    player.prepare()
                    if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) player.play()
                })
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { error = e.message ?: "Falha ao gerar o trecho." }
    }
    AlertDialog(onDismissRequest = close, title = { Text("Testar trecho") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("${sampleTime(start)} – ${sampleTime(start + duration)} · ${quality}p · ${preset.label}")
            when {
                error != null -> Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)
                !requested -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (s in 3..5) FilterChip(selected = s == seconds, onClick = { seconds = s }, label = { Text("${s} s") })
                    }
                    if (maxStart > 0) {
                        Text("Início do trecho")
                        Slider(value = start.toFloat(), onValueChange = { selected = it.toLong() },
                            valueRange = 0f..maxStart.toFloat())
                    }
                }
                file == null -> { LinearProgressIndicator(); Text("Gerando MP4 de teste…") }
                else -> AndroidView(factory = { ctx -> previewPlayer(ctx).apply {
                    this.player = player; useController = true
                } }, modifier = Modifier.fillMaxWidth().height(245.dp))
            }
        }
    }, confirmButton = {
        if (!requested) TextButton(onClick = { requested = true }) { Text("Gerar teste") }
        else if (file != null || error != null) TextButton(onClick = {
            engine.cancel(); player.stop(); player.clearMediaItems(); file?.delete()
            file = null; error = null; requested = false
        }) { Text("Outro trecho") }
    }, dismissButton = { TextButton(onClick = close) { Text(if (requested && file == null && error == null) "Cancelar" else "Fechar") } })
}

private fun sampleTime(ms: Long): String = java.lang.String.format(java.util.Locale.ROOT,
    "%d:%02d.%01d", ms / 60_000, ms / 1000 % 60, ms / 100 % 10)
