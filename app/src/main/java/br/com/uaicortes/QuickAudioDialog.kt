package br.com.uaicortes

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/** Selecionar a mídia não muda a edição. Só Aplicar copia/insere a nova faixa. */
@Composable
internal fun QuickAudioDialog(
    project: VideoProject,
    atMs: Long,
    track: AudioTrack?,
    onDismiss: () -> Unit,
    onSave: (VideoProject) -> Boolean
) {
    val context = LocalContext.current
    val media = remember(context) { MediaImportController(context) }
    val scope = rememberCoroutineScope()
    val latestProject by rememberUpdatedState(project)
    val latestSave by rememberUpdatedState(onSave)
    var uriText by rememberSaveable { mutableStateOf(track?.uri) }
    var title by rememberSaveable { mutableStateOf(track?.title.orEmpty()) }
    var audioMs by rememberSaveable { mutableLongStateOf(track?.durationMs ?: 0L) }
    val initialStartMs = remember { (track?.startMs ?: atMs).coerceIn(0L, project.durationMs) }
    var enteredTime by rememberSaveable { mutableStateOf<String?>(null) }
    val initialDigits = NumericTime.fromMs(initialStartMs)
    val startMs = if (enteredTime == null || enteredTime == initialDigits) initialStartMs
        else NumericTime.toMs(enteredTime.orEmpty())
    var volume by rememberSaveable { mutableFloatStateOf(track?.volume ?: .35f) }
    var busy by remember { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf("") }
    val choose = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            busy = true
            error = ""
            try {
                val (name, duration) = withContext(Dispatchers.IO) {
                    media.persistReadAccess(uri)
                    media.title(uri).ifBlank { "Áudio" } to media.duration(uri)
                }
                if (duration < AudioTrackTiming.MIN_TRIM_MS) {
                    error = "Não foi possível ler um áudio com pelo menos 0,25 s."
                } else {
                    title = name
                    audioMs = duration
                    uriText = uri.toString()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                DiagnosticRecorder.exception("quick_audio_probe", e)
                error = "Não foi possível ler o arquivo de áudio."
            } finally {
                busy = false
            }
        }
    }
    LaunchedEffect(track?.id) {
        if (track != null && audioMs <= 0L) {
            busy = true
            try {
                audioMs = withContext(Dispatchers.IO) { media.duration(Uri.parse(track.uri)) }
                    .minus(track.sourceStartMs).coerceAtLeast(0L)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                DiagnosticRecorder.exception("quick_audio_probe", e)
                error = "Não foi possível ler a duração do áudio."
            } finally {
                busy = false
            }
        }
    }
    val lengths = project.clips.map(Clip::durationMs)
    val window = startMs?.let { QuickAudioPlacement.resolve(lengths, it, audioMs) }
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        shape = UaiShapes.dialog,
        title = { Text(if (track == null) "Adicionar áudio" else "Editar áudio") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (track == null) OutlinedButton(onClick = { choose.launch(arrayOf("audio/*")) },
                    enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                    Text(if (uriText == null) "Escolher áudio" else "Trocar áudio")
                }
                if (uriText != null) {
                    Text(title.ifBlank { "Áudio" }, style = MaterialTheme.typography.titleSmall,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text("Duração do áudio: ${formatPipMs(audioMs)}", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(value = enteredTime ?: initialDigits,
                        onValueChange = { enteredTime = NumericTime.sanitize(it) },
                        label = { Text("Começar em") }, enabled = !busy,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        visualTransformation = NumericTimeMask, singleLine = true,
                        modifier = Modifier.fillMaxWidth(), isError = window == null && !busy)
                    if (startMs != 0L && !busy) TextButton(onClick = { enteredTime = NumericTime.fromMs(0L) }) {
                        Text("Começar do início")
                    }
                    if (window != null) {
                        Text(quickAudioCoverage(window, project.clips),
                            style = MaterialTheme.typography.bodySmall)
                        if (window.omittedMs > 0L)
                            Text("Só ${formatPipMs(window.durationMs)} cabem no projeto. O arquivo original não será cortado.",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    } else if (!busy) Text(if (startMs == null) "Informe um horário válido." else
                        "Não há tempo suficiente a partir desse ponto. Escolha um início mais cedo.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Volume", Modifier.weight(1f))
                        Text("${(volume * 100).roundToInt()}%")
                    }
                    Slider(volume, { volume = it }, enabled = !busy)
                }
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
                if (track != null) TextButton(onClick = {
                    val current = latestProject
                    if (latestSave(current.copy(audioTracks = current.audioTracks.filterNot { it.id == track.id }))) {
                        onDismiss()
                    } else error = "Não foi possível remover a faixa."
                }, enabled = !busy) { Text("Remover faixa", color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(onClick = {
                val selectedUri = uriText ?: return@Button
                val placement = window ?: return@Button
                scope.launch {
                    busy = true
                    error = ""
                    try {
                        val savedUri = if (track == null) withContext(Dispatchers.IO) {
                            media.protectedCopy(Uri.parse(selectedUri), project.id, ProjectMediaCopies.Kind.AUDIO).toString()
                        } else selectedUri
                        val current = latestProject
                        if (current.id != project.id || current.clips.map(Clip::durationMs) != lengths ||
                            (track != null && current.audioTracks.none { it.id == track.id })) {
                            error = "A linha do tempo mudou. Confira o alcance antes de aplicar."
                        } else {
                            val updated = if (track == null)
                                AudioTrack(savedUri, title, placement.startMs, placement.durationMs, volume)
                            else track.copy(startMs = placement.startMs, durationMs = placement.durationMs, volume = volume)
                            val tracks = if (track == null) current.audioTracks + updated else
                                current.audioTracks.map { if (it.id == track.id) updated else it }
                            if (latestSave(current.copy(audioTracks = tracks))) onDismiss()
                            else error = "Não foi possível salvar a faixa."
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        DiagnosticRecorder.exception("quick_audio_apply", e)
                        error = "Não foi possível importar o áudio."
                    } finally {
                        busy = false
                    }
                }
            }, enabled = !busy && window != null && uriText != null) { Text("Aplicar") }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("Cancelar") } }
    )
}

private fun quickAudioCoverage(window: QuickAudioPlacement.Window, clips: List<Clip>): String {
    fun label(index: Int): String {
        val photo = clips[index].isImage
        val number = clips.take(index + 1).count { it.isImage == photo }
        return if (photo) "foto $number" else "vídeo $number"
    }
    val first = window.first
    val last = window.last
    val end = formatPipMs(last.localMs)
    val start = formatPipMs(first.localMs)
    val coverage = if (first.clipIndex == last.clipIndex)
        "Toca de $start até $end no ${label(first.clipIndex)}."
    else "Vai de $start do ${label(first.clipIndex)} até $end do ${label(last.clipIndex)}."
    return when {
        last.clipIndex == clips.lastIndex -> coverage
        last.clipIndex == clips.lastIndex - 1 -> "$coverage O ${label(clips.lastIndex)} não recebe este áudio."
        else -> "$coverage Os clipes seguintes não recebem este áudio."
    }
}

@Composable
internal fun QuickMusicVolumeDialog(project: VideoProject, onDismiss: () -> Unit,
    onSave: (VideoProject) -> Boolean) {
    val music = project.music ?: return
    var volume by rememberSaveable { mutableFloatStateOf(music.volume) }
    var failed by rememberSaveable { mutableStateOf(false) }
    fun save(changed: VideoProject) {
        if (onSave(changed)) onDismiss() else failed = true
    }
    AlertDialog(onDismissRequest = onDismiss, shape = UaiShapes.dialog, title = { Text("Música") },
        text = { Column {
            Text(music.title, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("Volume: ${(volume * 100).roundToInt()}%")
            Slider(volume, { volume = it })
            TextButton(onClick = { save(project.copy(music = null)) }) { Text("Remover música") }
            if (failed) Text("Não foi possível salvar a alteração.", color = MaterialTheme.colorScheme.error)
        } },
        confirmButton = { Button(onClick = { save(project.copy(music = music.copy(volume = volume))) }) {
            Text("Aplicar")
        } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } })
}
