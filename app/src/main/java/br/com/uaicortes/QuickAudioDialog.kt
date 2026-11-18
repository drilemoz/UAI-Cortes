package br.com.uaicortes

import android.net.Uri
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
    onSave: (VideoProject) -> Boolean,
    musicMode: Boolean = false
) {
    val music = if (musicMode) project.music else null
    val mediaLabel = if (musicMode) "Música" else "Áudio"
    val initialUri = music?.uri ?: track?.uri
    val context = LocalContext.current
    val media = remember(context) { MediaImportController(context) }
    val scope = rememberCoroutineScope()
    val latestProject by rememberUpdatedState(project)
    val latestSave by rememberUpdatedState(onSave)
    var uriText by rememberSaveable { mutableStateOf(initialUri) }
    var title by rememberSaveable { mutableStateOf((music?.title ?: track?.title).orEmpty()) }
    var audioMs by rememberSaveable {
        mutableLongStateOf(music?.let { item ->
            val available = (item.sourceDurationMs - item.sourceStartMs).coerceAtLeast(0L)
            item.durationMs.takeIf { it > 0L }?.coerceAtMost(available) ?: available
        } ?: track?.durationMs ?: 0L)
    }
    val initialStartMs = remember { (music?.startMs ?: track?.startMs ?: atMs).coerceIn(0L, project.durationMs) }
    var enteredTime by rememberSaveable { mutableStateOf<String?>(null) }
    val initialDigits = NumericTime.fromMs(initialStartMs)
    val startMs = if (enteredTime == null || enteredTime == initialDigits) initialStartMs
        else NumericTime.toMs(enteredTime.orEmpty())
    var volume by rememberSaveable { mutableFloatStateOf(music?.volume ?: track?.volume ?: if (musicMode) .25f else .35f) }
    var busy by remember { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf("") }
    var replacement by remember(project.id, track?.id) { mutableStateOf<Music?>(null) }

    fun moveToMusic(existing: Music?) {
        val selected = track ?: return
        scope.launch {
            busy = true
            error = ""
            try {
                val inspected = withContext(Dispatchers.IO) { media.duration(Uri.parse(selected.uri)) }
                val current = latestProject
                val changed = if (current.id == project.id)
                    MusicTrackTransfer.move(current, selected, inspected, existing) else null
                if (changed != null && latestSave(changed)) onDismiss()
                else error = "Não foi possível mover. Confira o arquivo e se a faixa ainda cabe no projeto."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                DiagnosticRecorder.exception("quick_audio_move_to_music", e)
                error = "Não foi possível ler o arquivo para mover à faixa Música."
            } finally {
                busy = false
            }
        }
    }
    val choose = rememberDeviceAudioPicker() { uri ->
        if (uri != null) scope.launch {
            busy = true
            error = ""
            try {
                val (name, duration) = withContext(Dispatchers.IO) {
                    media.persistReadAccess(uri)
                    media.title(uri).ifBlank { mediaLabel } to media.duration(uri)
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
    LaunchedEffect(track?.id, initialUri) {
        if (initialUri != null && audioMs <= 0L) {
            busy = true
            try {
                audioMs = withContext(Dispatchers.IO) { media.duration(Uri.parse(initialUri)) }
                    .minus(music?.sourceStartMs ?: track?.sourceStartMs ?: 0L).coerceAtLeast(0L)
                    .let { remaining -> music?.durationMs?.takeIf { it > 0L }
                        ?.coerceAtMost(remaining) ?: remaining }
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
    val legacyLoop = music != null && music.loops && startMs == 0L && uriText == music.uri
    val window = startMs?.let {
        QuickAudioPlacement.resolve(lengths, it, if (legacyLoop) project.durationMs else audioMs)
    }
    val replaceMusic = replacement
    if (replaceMusic != null) {
        AlertDialog(onDismissRequest = { replacement = null }, shape = UaiShapes.dialog,
            title = { Text("Substituir a música?") },
            text = { Text("“${replaceMusic.title.ifBlank { "Música atual" }}” sairá da faixa Música. O áudio selecionado será movido sem duplicar o arquivo.") },
            confirmButton = { Button(onClick = {
                replacement = null
                moveToMusic(replaceMusic)
            }) { Text("Substituir e mover") } },
            dismissButton = { TextButton(onClick = { replacement = null }) { Text("Cancelar") } })
        return
    }
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        shape = UaiShapes.dialog,
        title = {
            Text(if (musicMode) (if (music == null) "Adicionar música" else "Editar música")
                else (if (track == null) "Adicionar áudio" else "Editar áudio"))
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (track == null) OutlinedButton(onClick = { choose() },
                    enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                    Text(if (musicMode) (if (uriText == null) "Escolher música" else "Trocar música")
                        else (if (uriText == null) "Escolher áudio" else "Trocar áudio"))
                }
                if (uriText != null) {
                    Text(title.ifBlank { mediaLabel }, style = MaterialTheme.typography.titleSmall,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                    val durationLabel = if (music != null && music.uri == uriText &&
                        (music.sourceStartMs > 0L || music.durationMs > 0L)) "Duração do trecho" else "Duração do arquivo"
                    Text("$durationLabel: ${formatPipMs(audioMs)}", style = MaterialTheme.typography.bodySmall)
                    if (legacyLoop) Text("Esta música já estava configurada para repetir desde o início.",
                        style = MaterialTheme.typography.bodySmall)
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
                if (!musicMode && track != null && MusicTrackTransfer.eligible(track)) TextButton(
                    onClick = {
                        val existing = latestProject.music
                        if (existing == null) moveToMusic(null) else replacement = existing
                    }, enabled = !busy) { Text("Mover para Música") }
                if (track != null || music != null) TextButton(onClick = {
                    val current = latestProject
                    val changed = if (musicMode) current.copy(music = null)
                        else current.copy(audioTracks = current.audioTracks.filterNot { it.id == track?.id })
                    if (latestSave(changed)) onDismiss()
                    else error = "Não foi possível remover a faixa."
                }, enabled = !busy) {
                    Text(if (musicMode) "Remover música" else "Remover faixa",
                        color = MaterialTheme.colorScheme.error)
                }
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
                        val savedUri = if (track == null && selectedUri != initialUri) withContext(Dispatchers.IO) {
                            media.protectedCopy(Uri.parse(selectedUri), project.id, ProjectMediaCopies.Kind.AUDIO).toString()
                        } else selectedUri
                        val current = latestProject
                        if (current.id != project.id || current.clips.map(Clip::durationMs) != lengths ||
                            (musicMode && current.music?.uri != music?.uri) ||
                            (track != null && current.audioTracks.none { it.id == track.id })) {
                            error = "A linha do tempo mudou. Confira o alcance antes de aplicar."
                        } else if (musicMode) {
                            val updated = if (music != null && selectedUri == music.uri)
                                music.copy(startMs = placement.startMs, volume = volume, repeat = legacyLoop)
                            else Music(savedUri, title, volume,
                                startMs = placement.startMs, sourceDurationMs = audioMs, repeat = false)
                            if (latestSave(current.copy(music = updated))) onDismiss()
                            else error = "Não foi possível salvar a música."
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
    fun join(index: Int, video: String, photo: String) =
        "${if (clips[index].isImage) photo else video} ${label(index)}"
    val coverage = if (first.clipIndex == last.clipIndex)
        "Toca de $start até $end ${join(first.clipIndex, "no", "na")}."
    else "Vai de $start ${join(first.clipIndex, "do", "da")} até $end ${join(last.clipIndex, "do", "da")}."
    return when {
        last.clipIndex == clips.lastIndex -> coverage
        last.clipIndex == clips.lastIndex - 1 -> "$coverage ${join(clips.lastIndex, "O", "A")} não recebe esta faixa."
        else -> "$coverage Os clipes seguintes não recebem esta faixa."
    }
}

