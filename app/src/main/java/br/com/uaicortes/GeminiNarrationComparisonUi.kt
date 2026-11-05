package br.com.uaicortes

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import kotlinx.coroutines.delay
import java.io.File

/** Escuta comparativa sob demanda, sem alterar o projeto, o volume ou o arquivo exportado. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
internal fun GeminiNarrationComparisonDialog(
    project: VideoProject,
    segment: SubtitleNarrationTimeline.Segment,
    startMs: Long,
    generated: File,
    generatedDurationMs: Long,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val player = remember(context) { diagnosticPlayer(context) }
    var playingSource by remember { mutableStateOf("") }
    var feedback by remember { mutableStateOf("") }
    val originalEndMs = segment.subtitleEndMs.coerceAtLeast(startMs + 1L)
        .coerceAtMost(project.durationMs)
    val originalAvailable = project.clips.any { it.durationMs > 0L }
    DisposableEffect(player) { onDispose { player.release() } }
    LaunchedEffect(playingSource, project.id) {
        while (playingSource.isNotBlank()) {
            if (player.playbackState == Player.STATE_ENDED ||
                (playingSource == "original" &&
                    projectPosition(player, project.clips, project.durationMs) >= originalEndMs)) {
                player.pause()
                playingSource = ""
                break
            }
            delay(90L)
        }
    }
    AlertDialog(
        onDismissRequest = { player.pause(); onDismiss() },
        title = { Text("Comparar original e Gemini") },
        text = { Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(segment.text, style = MaterialTheme.typography.bodyMedium)
            Text("Início: ${NarrationReferenceAlignment.timeLabel(startMs)} · " +
                "Legenda até ${NarrationReferenceAlignment.timeLabel(segment.subtitleEndMs)}",
                style = MaterialTheme.typography.bodySmall)
            Text("Nova voz: ${NarrationReferenceAlignment.timeLabel(generatedDurationMs)}. " +
                "A escuta original usa o som do vídeo, mesmo se a faixa estiver silenciada na mixagem.",
                style = MaterialTheme.typography.bodySmall)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = {
                    player.pause(); playingSource = ""
                    if (!originalAvailable || originalEndMs <= startMs) {
                        feedback = "O vídeo não possui um intervalo original disponível."
                    } else {
                        val items = project.clips.filter { it.durationMs > 0L }.map { clip ->
                            ClipMediaItems.playback(clip)
                        }
                        player.setMediaItems(items)
                        player.prepare()
                        seekProjectPosition(player, project.clips.filter { it.durationMs > 0L }, startMs)
                        player.play()
                        playingSource = "original"
                        feedback = "Reproduzindo som original do vídeo."
                    }
                }, enabled = originalAvailable && originalEndMs > startMs,
                    modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Movie, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Original")
                }
                OutlinedButton(onClick = {
                    player.pause(); playingSource = ""
                    if (!generated.exists() || generated.length() <= 0L) {
                        feedback = "O WAV preparado não está mais disponível."
                    } else {
                        player.setMediaItem(MediaItem.fromUri(Uri.fromFile(generated)))
                        player.prepare(); player.play()
                        playingSource = "gemini"
                        feedback = "Reproduzindo narração Gemini."
                    }
                }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.RecordVoiceOver, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Gemini")
                }
            }
            if (playingSource.isNotBlank()) TextButton(onClick = {
                player.pause(); playingSource = ""; feedback = "Reprodução interrompida."
            }) { Icon(Icons.Default.Stop, null, Modifier.size(16.dp)); Text("Parar") }
            if (feedback.isNotBlank()) Text(feedback, style = MaterialTheme.typography.bodySmall)
            Text("Comparação A/B apenas para escuta; não mistura nem altera áudio, legendas ou exportação.",
                style = MaterialTheme.typography.bodySmall)
        } },
        confirmButton = { TextButton(onClick = { player.pause(); onDismiss() }) { Text("Fechar") } }
    )
}
