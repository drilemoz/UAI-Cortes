package br.com.uaicortes

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class SrtSpeechSource(val project: VideoProject, val offsetMs: Long)

private suspend fun prepareSrtSpeechSource(context: Context, project: VideoProject,
    track: AudioTrack?): SrtSpeechSource {
    if (track == null) return SrtSpeechSource(project, 0L)
    return withContext(Dispatchers.IO) {
        val duration = verifiedMediaDuration(context, Uri.parse(track.uri))
        val window = requireNotNull(ExportAudioPolicy.window(track.startMs, track.sourceStartMs,
            track.durationMs, duration, project.durationMs)) {
            "Não foi possível confirmar o trecho da narração escolhido."
        }
        val clip = Clip(id = track.id, uri = track.uri, title = track.title,
            sourceDurationMs = duration, startMs = window.sourceStartMs,
            endMs = window.sourceStartMs + window.playableMs)
        SrtSpeechSource(project.copy(clips = listOf(clip), overlays = emptyList(),
            audioTracks = emptyList(), music = null, subtitles = emptyList(),
            transitions = emptyList(), textWatermark = null), window.timelineStartMs)
    }
}

@Composable
internal fun SrtImportReview(project: VideoProject, original: List<SubtitleCue>,
    onImport: (List<SubtitleCue>, SrtSpeechAlignment.Result?) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val gemini = remember(context) { GeminiSubtitleClient(context) }
    val credentials = remember(context) { GeminiCredentialStore(context) }
    val hasGemini = remember(credentials) { credentials.hasKey() }
    val hasVosk = remember(context) { AutoSubtitleEngine.installed(context) }
    val references = remember(project.audioTracks, project.clips) { ClipAudioBindings.resolved(project) }
    var referenceId by remember { mutableStateOf<String?>(null) }
    var showSources by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<SrtSpeechAlignment.Result?>(null) }
    var cloud by remember { mutableStateOf(!hasVosk && hasGemini) }
    var job by remember { mutableStateOf<Job?>(null) }
    val track = references.firstOrNull { it.id == referenceId }
    DisposableEffect(gemini) { onDispose { job?.cancel(); gemini.cancel() } }

    fun checkSpeech() {
        val useCloud = cloud
        job = scope.launch {
            busy = true
            error = ""
            progress = "Preparando áudio…"
            try {
                val source = prepareSrtSpeechSource(context, project, track)
                val transcript = if (useCloud) {
                    val key = withContext(Dispatchers.IO) { credentials.read() }
                        ?: error("Configure sua chave Gemini antes de conferir.")
                    gemini.transcribe(context, source.project, key, false) { _, step ->
                        withContext(Dispatchers.Main) { progress = step }
                    }
                } else AutoSubtitleEngine.transcribe(context, source.project) { _, step ->
                    withContext(Dispatchers.Main) { progress = step }
                }
                result = withContext(Dispatchers.Default) {
                    SrtSpeechAlignment.align(original, transcript, project.durationMs, source.offsetMs)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                DiagnosticRecorder.exception("srt_speech_review", e)
                error = "Não foi possível conferir a fala. Você ainda pode manter os horários do SRT."
            } finally { busy = false }
        }
    }

    AlertDialog(onDismissRequest = { if (!busy) onDismiss() }, shape = UaiShapes.dialog,
        title = { Text("Importar SRT") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val reviewed = result
                Text("${original.size} legendas · tamanho $DEFAULT_SUBTITLE_SIZE_SP")
                if (busy) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text(progress)
                } else if (reviewed != null) {
                    Text("${reviewed.matched} com palavras conferidas e ${reviewed.adjusted} com horários ajustados.")
                    Text("${original.size - reviewed.matched} mantiveram os horários do arquivo. Nenhum texto foi reescrito.")
                } else {
                    Text("Mantenha os horários do arquivo ou confira com a fala antes de importar.")
                    if (hasVosk || hasGemini) {
                        Box {
                            OutlinedButton(onClick = { showSources = true }, modifier = Modifier.fillMaxWidth()) {
                                Text(track?.title?.ifBlank { "Faixa de áudio" } ?: "Áudio dos clipes",
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            DropdownMenu(expanded = showSources, onDismissRequest = { showSources = false },
                                modifier = Modifier.heightIn(max = 280.dp)) {
                                DropdownMenuItem(text = { Text("Áudio dos clipes") }, onClick = {
                                    referenceId = null; showSources = false
                                })
                                references.forEach { item ->
                                    DropdownMenuItem(text = { Text(item.title.ifBlank { "Faixa de áudio" }) },
                                        onClick = { referenceId = item.id; showSources = false })
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (hasVosk) FilterChip(!cloud, { cloud = false }, label = { Text("Vosk offline") })
                            if (hasGemini) FilterChip(cloud, { cloud = true }, label = { Text("Gemini") })
                        }
                        if (cloud) Text("Conferir com Gemini envia somente o áudio da fonte acima e pode consumir cota.",
                            style = MaterialTheme.typography.bodySmall)
                    } else Text("Para conferir com a fala, instale o Vosk ou configure o Gemini em Legendas automáticas.",
                        style = MaterialTheme.typography.bodySmall)
                }
                if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error)
                if (!busy) TextButton(onClick = { onImport(original, null) }) { Text("Manter horários e importar") }
            }
        },
        confirmButton = {
            val reviewed = result
            if (reviewed != null && reviewed.matched > 0) Button(onClick = { onImport(reviewed.cues, reviewed) }) {
                Text("Importar conferidas")
            } else if (reviewed == null && (hasVosk || hasGemini) && !busy)
                Button(onClick = ::checkSpeech) { Text(if (cloud) "Enviar áudio e conferir" else "Conferir com a fala") }
        },
        dismissButton = { TextButton(onClick = {
            if (busy) {
                job?.cancel()
                gemini.cancel()
            } else onDismiss()
        }) { Text("Cancelar") } })
}
