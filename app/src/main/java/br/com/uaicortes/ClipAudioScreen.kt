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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Import, listen, adjust and explicitly apply a single voice-over to a selected clip. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ClipAudioScreen(
    project: VideoProject,
    clipId: String,
    onBack: () -> Unit,
    onApply: (AudioTrack) -> Boolean,
    onRemove: () -> Boolean
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clip = project.clips.firstOrNull { it.id == clipId }
    val existing = project.audioTracks.firstOrNull { it.clipId == clipId }
    val resolved = ClipAudioBindings.resolved(project).firstOrNull { it.clipId == clipId }
    var sourceUri by remember(project.id, clipId) { mutableStateOf(existing?.uri.orEmpty()) }
    var sourceName by remember(project.id, clipId) { mutableStateOf(existing?.title.orEmpty()) }
    var sourceDuration by remember(project.id, clipId) {
        mutableLongStateOf(existing?.durationMs ?: 0L)
    }
    var volume by remember(project.id, clipId) { mutableFloatStateOf(existing?.volume ?: 1f) }
    var loading by remember(project.id, clipId) { mutableStateOf(false) }
    var error by remember(project.id, clipId) { mutableStateOf("") }
    var confirmRemove by remember(project.id, clipId) { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                loading = true
                error = ""
                try {
                    val data = withContext(Dispatchers.IO) {
                        val media = MediaImportController(context.applicationContext)
                        media.persistReadAccess(uri)
                        Triple(uri.toString(), media.title(uri), media.duration(uri))
                    }
                    if (data.third >= AudioTrackTiming.MIN_TRIM_MS) {
                        sourceUri = data.first
                        sourceName = data.second
                        sourceDuration = data.third
                    } else error = "Não foi possível confirmar a duração do áudio selecionado."
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: Exception) {
                    DiagnosticRecorder.exception("clip_audio_import", failure)
                    error = "O arquivo de áudio não está acessível ou não pode ser lido."
                } finally {
                    loading = false
                }
            }
        }
    }
    val preview = remember(context, project.id, clipId) { diagnosticPlayer(context) }
    var playing by remember(preview) { mutableStateOf(false) }
    DisposableEffect(preview) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) { playing = isPlaying }
        }
        preview.addListener(listener)
        onDispose { preview.removeListener(listener); preview.release() }
    }
    // Preview only the audible part of the selected clip; never modify the original media.
    LaunchedEffect(sourceUri, sourceDuration, clip?.startMs, clip?.endMs) {
        preview.pause()
        if (sourceUri.isBlank() || clip == null) {
            preview.clearMediaItems()
        } else {
            val sameTrack = existing?.uri == sourceUri
            val startMs = if (sameTrack) resolved?.sourceStartMs ?: 0L else 0L
            val playableMs = if (sameTrack) resolved?.durationMs ?: 0L
                else minOf(sourceDuration, clip.durationMs)
            if (playableMs > 0) {
                preview.setMediaItem(MediaItem.Builder().setUri(sourceUri)
                    .setClippingConfiguration(MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(startMs)
                        .setEndPositionMs(startMs + playableMs).build()).build())
                preview.prepare()
            } else preview.clearMediaItems()
        }
    }
    LaunchedEffect(volume) { preview.volume = volume.coerceIn(0f, 1f) }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Voltar") }
            Column {
                Text("Áudio do clipe", style = MaterialTheme.typography.titleLarge)
                Text("Somente no vídeo selecionado", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (clip == null) {
                Text("O clipe não está mais disponível. Volte ao editor.")
                Button(onClick = onBack) { Text("Voltar") }
            } else {
                AudioPanelCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Movie, null)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Clipe selecionado", fontWeight = FontWeight.SemiBold)
                            Text(clip.title, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text("Duração: ${formatPipMs(clip.durationMs)}",
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                AudioPanelCard {
                    Text("Narração ou música", fontWeight = FontWeight.SemiBold)
                    if (sourceUri.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AudioFile, null)
                            Spacer(Modifier.width(10.dp))
                            Text(sourceName, modifier = Modifier.weight(1f),
                                maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    } else {
                        Text("Nenhum arquivo selecionado.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    OutlinedButton(onClick = { picker.launch(arrayOf("audio/*")) },
                        modifier = Modifier.fillMaxWidth(), enabled = !loading) {
                        Icon(Icons.Default.FolderOpen, null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (sourceUri.isBlank()) "Importar áudio" else "Trocar áudio")
                    }
                    if (loading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            if (playing) preview.pause() else preview.play()
                        }, enabled = sourceUri.isNotBlank() && !loading &&
                            (existing?.uri != sourceUri || resolved != null)) {
                            Icon(if (playing) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                                if (playing) "Pausar áudio" else "Ouvir áudio",
                                modifier = Modifier.size(32.dp))
                        }
                        Text("Pré-escuta do áudio", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.weight(1f))
                        Text("${(volume * 100).toInt()}%")
                    }
                    Slider(value = volume, onValueChange = { volume = it }, valueRange = 0f..1f)
                    Text("O volume original do vídeo continua independente desta narração.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                AudioPanelCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Link, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Vinculado a este clipe", fontWeight = FontWeight.SemiBold)
                    }
                    Text("Começa junto do clipe e termina quando a narração ou o clipe acabar. " +
                        "Ao mover, dividir ou duplicar o clipe, o áudio acompanha o trecho correspondente.",
                        style = MaterialTheme.typography.bodySmall)
                }
                if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
                Button(onClick = {
                    if (sourceUri.isBlank()) { error = "Selecione um áudio primeiro."; return@Button }
                    // Existing audio keeps its source anchor when only volume is changed.
                    val track = if (existing?.uri == sourceUri) {
                        existing.copy(volume = volume)
                    } else AudioTrack(uri=sourceUri, title=sourceName,
                        durationMs=minOf(sourceDuration, clip.durationMs), volume=volume)
                    if (!onApply(track)) error = "Não foi possível salvar a narração no projeto."
                }, modifier = Modifier.fillMaxWidth(), enabled = !loading && sourceUri.isNotBlank()) {
                    Icon(Icons.Default.Check, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Aplicar somente a este clipe")
                }
                if (existing != null) OutlinedButton(onClick = { confirmRemove = true },
                    modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Delete, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Remover áudio deste clipe")
                }
            }
        }
    }
    if (confirmRemove) AlertDialog(onDismissRequest={confirmRemove=false},
        title={Text("Remover a narração?")},
        text={Text("O áudio será removido somente deste clipe. O arquivo original será preservado.")},
        confirmButton={TextButton(onClick = {
            confirmRemove=false
            if (!onRemove()) error="Não foi possível remover a narração."
        }) { Text("Remover") }},
        dismissButton={TextButton(onClick={confirmRemove=false}) { Text("Cancelar") }})
}
