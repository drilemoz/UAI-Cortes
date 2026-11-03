package br.com.uaicortes

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import kotlinx.coroutines.delay

/** Referência local, opcional e guiada. Não envia o áudio ao Gemini e não clona a voz. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
internal fun AudioReferenceAlignmentDialog(
    project: VideoProject,
    segments: List<SubtitleNarrationTimeline.Segment>,
    initialStarts: Map<String, Long>,
    onApply: (Map<String, Long>) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var source by remember(project.id) { mutableStateOf("project") }
    var trackId by remember(project.id) { mutableStateOf<String?>(null) }
    var importedUri by remember(project.id) { mutableStateOf<Uri?>(null) }
    var importedName by remember(project.id) { mutableStateOf("") }
    var importedDuration by remember(project.id) { mutableLongStateOf(0L) }
    var externalOffset by remember(project.id) { mutableLongStateOf(0L) }
    var marks by remember(project.id) { mutableStateOf(NarrationReferenceAlignment.cleaned(segments, initialStarts)) }
    var feedback by remember { mutableStateOf("") }
    var currentMs by remember { mutableLongStateOf(0L) }
    var playing by remember { mutableStateOf(false) }
    val referencePlayer = remember(context) { diagnosticPlayer(context) }
    val tracks = project.audioTracks.filter { it.durationMs > 0L && it.uri.isNotBlank() }
    val chosenTrack = tracks.firstOrNull { it.id == trackId }
    val externalPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            val measuredDuration = verifiedMediaDuration(context, uri)
            if (measuredDuration > 0L) {
                importedUri = uri
                importedDuration = measuredDuration
                importedName = mediaName(context, uri)
                source = "external"
                externalOffset = 0L
                feedback = "Arquivo selecionado. Informe onde o áudio começa no projeto, se necessário."
            } else feedback = "Não foi possível verificar a duração deste áudio. Escolha outro arquivo."
        }
    }
    DisposableEffect(referencePlayer) { onDispose { referencePlayer.release() } }
    LaunchedEffect(source, trackId, importedUri, project.clips, project.id) {
        playing = false
        referencePlayer.pause()
        referencePlayer.clearMediaItems()
        when (source) {
            "project" -> {
                val clips = project.clips.filter { it.endMs > it.startMs }
                referencePlayer.setMediaItems(clips.map { clip ->
                    MediaItem.Builder().setUri(Uri.parse(clip.uri))
                        .setClippingConfiguration(MediaItem.ClippingConfiguration.Builder()
                            .setStartPositionMs(clip.startMs).setEndPositionMs(clip.endMs).build())
                        .build()
                })
            }
            "track" -> chosenTrack?.let { referencePlayer.setMediaItem(MediaItem.fromUri(Uri.parse(it.uri))) }
            "external" -> importedUri?.let { referencePlayer.setMediaItem(MediaItem.fromUri(it)) }
        }
        if (referencePlayer.mediaItemCount > 0) {
            referencePlayer.prepare()
            if (source == "track") referencePlayer.seekTo(chosenTrack?.sourceStartMs ?: 0L)
            currentMs = when (source) { "track" -> chosenTrack?.startMs ?: 0L; "external" -> externalOffset; else -> 0L }
        }
    }
    LaunchedEffect(referencePlayer, playing, source, chosenTrack, externalOffset, project.durationMs) {
        while (playing) {
            currentMs = when (source) {
                "project" -> projectPosition(referencePlayer, project.clips, project.durationMs)
                "track" -> chosenTrack?.let {
                    NarrationReferenceAlignment.sourceToProject(
                        referencePlayer.currentPosition - it.sourceStartMs, it.startMs, project.durationMs)
                } ?: 0L
                else -> NarrationReferenceAlignment.sourceToProject(
                    referencePlayer.currentPosition, externalOffset, project.durationMs)
            }
            if (referencePlayer.playbackState == Player.STATE_ENDED ||
                (source == "track" && chosenTrack != null &&
                    referencePlayer.currentPosition >= chosenTrack.sourceStartMs + chosenTrack.durationMs)) {
                referencePlayer.pause(); playing = false
            }
            delay(120)
        }
    }
    fun seekToProjectPosition(positionMs: Long) {
        val target = positionMs.coerceIn(0L, (project.durationMs - 1L).coerceAtLeast(0L))
        when (source) {
            "project" -> seekProjectPosition(referencePlayer, project.clips, target)
            "track" -> chosenTrack?.let {
                referencePlayer.seekTo(it.sourceStartMs + (target - it.startMs).coerceIn(0L, it.durationMs))
            }
            "external" -> referencePlayer.seekTo((target - externalOffset).coerceIn(0L, importedDuration))
        }
        currentMs = target
    }
    val minMs = when (source) { "track" -> chosenTrack?.startMs ?: 0L; "external" -> externalOffset; else -> 0L }
        .coerceIn(0L, (project.durationMs - 1L).coerceAtLeast(0L))
    val maxMs = when (source) {
        "track" -> chosenTrack?.let { it.startMs + it.durationMs } ?: 0L
        "external" -> externalOffset + importedDuration
        else -> project.durationMs
    }.coerceIn(minMs + 1L, project.durationMs.coerceAtLeast(minMs + 1L))
    LaunchedEffect(source, externalOffset) {
        if (source == "external") currentMs = NarrationReferenceAlignment.sourceToProject(
            referencePlayer.currentPosition, externalOffset, project.durationMs)
    }
    val available = when (source) {
        "project" -> project.clips.isNotEmpty()
        "track" -> chosenTrack != null
        else -> importedUri != null && importedDuration > 0L
    }
    val problems = NarrationReferenceAlignment.validate(segments, marks, project.durationMs)
    AlertDialog(
        onDismissRequest = { referencePlayer.pause(); onDismiss() },
        title = { Text("Áudio como referência") },
        text = {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Ouça o áudio original e marque o início de cada fala. O áudio não será enviado " +
                    "à IA; os textos das legendas continuam sendo usados na síntese.",
                    style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = source == "project", onClick = { source = "project" },
                        label = { Text("Vídeo") })
                    FilterChip(selected = source == "track", onClick = { source = "track" },
                        label = { Text("Faixas") })
                    FilterChip(selected = source == "external", onClick = {
                        if (importedUri != null) source = "external" else
                            externalPicker.launch(arrayOf("audio/*", "video/*"))
                    }, label = { Text("Arquivo") })
                }
                if (source == "track") {
                    if (tracks.isEmpty()) Text("Nenhuma faixa de áudio adicionada ao projeto.",
                        style = MaterialTheme.typography.bodySmall)
                    else tracks.forEach { track ->
                        FilterChip(selected = trackId == track.id,
                            onClick = { trackId = track.id },
                            label = { Text(track.title.ifBlank { "Áudio do projeto" },
                                maxLines = 1, overflow = TextOverflow.Ellipsis) })
                    }
                }
                if (source == "external") {
                    Text(importedName.ifBlank { "Nenhum arquivo selecionado" },
                        style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(onClick = { externalPicker.launch(arrayOf("audio/*", "video/*")) }) {
                        Text("Escolher outro áudio")
                    }
                    Text("O arquivo externo começa no projeto em: " +
                        NarrationReferenceAlignment.timeLabel(externalOffset),
                        style = MaterialTheme.typography.bodySmall)
                    Slider(value = externalOffset.toFloat(), onValueChange = {
                        externalOffset = it.toLong()
                    }, valueRange = 0f..project.durationMs.coerceAtLeast(1L).toFloat())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { externalOffset = (externalOffset - 500L).coerceAtLeast(0L) },
                            enabled = externalOffset > 0L) { Text("−0,5 s") }
                        OutlinedButton(onClick = { externalOffset = (externalOffset + 500L)
                            .coerceAtMost((project.durationMs - 1L).coerceAtLeast(0L)) }) { Text("+0,5 s") }
                    }
                }
                if (feedback.isNotBlank()) Text(feedback, style = MaterialTheme.typography.bodySmall)
                if (available) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            if (referencePlayer.isPlaying) {
                                referencePlayer.pause(); playing = false
                            } else {
                                referencePlayer.play(); playing = true
                            }
                        }) { Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                            "Ouvir ou pausar áudio de referência") }
                        Text("Referência: ${NarrationReferenceAlignment.timeLabel(currentMs)}",
                            style = MaterialTheme.typography.bodyMedium)
                    }
                    Slider(value = currentMs.coerceIn(minMs, maxMs).toFloat(),
                        onValueChange = { seekToProjectPosition(it.toLong()) },
                        valueRange = minMs.toFloat()..maxMs.toFloat())
                    Text("Toque em 'Marcar aqui' na fala correspondente. Os horários originais " +
                        "permanecem nas legendas.", style = MaterialTheme.typography.bodySmall)
                } else Text("Selecione uma origem de áudio antes de marcar os tempos.",
                    style = MaterialTheme.typography.bodySmall)
                segments.forEach { segment ->
                    val actual = NarrationReferenceAlignment.start(segment, marks)
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(segment.text, maxLines = 2, overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall)
                        Text("Legenda: ${NarrationReferenceAlignment.timeLabel(segment.startMs)} · " +
                            "Voz: ${NarrationReferenceAlignment.timeLabel(actual)}",
                            style = MaterialTheme.typography.labelSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(enabled = available, onClick = {
                                seekToProjectPosition(segment.startMs)
                            }) { Text("Ir à legenda") }
                            OutlinedButton(enabled = available, onClick = {
                                marks = NarrationReferenceAlignment.cleaned(segments,
                                    marks + (segment.cueId to currentMs))
                            }) { Text("Marcar aqui") }
                        }
                        if (segment.cueId in marks) TextButton(onClick = {
                            marks = marks - segment.cueId
                        }) { Text("Restaurar horário original") }
                    }
                    HorizontalDivider()
                }
                if (problems != null) Text(problems, color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
                Text("Os ajustes só serão aplicados após confirmar. A geração Gemini " +
                    "continua exigindo confirmação e pode consumir cota.",
                    style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { TextButton(onClick = {
            if (problems == null) { referencePlayer.pause(); onApply(NarrationReferenceAlignment.cleaned(segments, marks)) }
        }, enabled = problems == null) { Text("Aplicar horários") } },
        dismissButton = { TextButton(onClick = { referencePlayer.pause(); onDismiss() }) { Text("Cancelar") } }
    )
}
