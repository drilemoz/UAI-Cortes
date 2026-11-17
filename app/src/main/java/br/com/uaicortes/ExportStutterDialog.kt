package br.com.uaicortes

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
internal fun ExportStutterDialog(project: VideoProject, startMs: Long, initialMp4: Uri?,
    quality: Int, preset: ExportBitratePreset, destination: ExportDestination, frameMode: VerticalFrameMode,
    onClose: () -> Unit) {
    val clips = remember(project.clips) { stutterClips(project) }
    val initial = clips.indexOfFirst {
        startMs >= it.timelineStartMs && startMs < it.timelineEndMs
    }.takeIf { it >= 0 } ?: 0
    var selected by remember(project.clips) { mutableIntStateOf(initial) }
    val source = clips.getOrNull(selected)
    var center by remember(source) {
        mutableLongStateOf(source?.let {
            startMs.takeIf { ms -> ms >= it.timelineStartMs && ms < it.timelineEndMs }
                ?: (it.timelineStartMs + it.clip.durationMs / 2L)
        } ?: 0L)
    }
    val window = source?.window(center)
    var mp4 by remember(initialMp4) { mutableStateOf(initialMp4) }
    var report by remember { mutableStateOf<ExportStutterAnalysis.Report?>(null) }
    var stage by remember { mutableStateOf<ExportStutterAnalysis.Stage?>(null) }
    var job by remember { mutableStateOf<Job?>(null) }
    var failure by remember { mutableStateOf(false) }
    var copied by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val running = job != null
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            mp4 = uri
            report = null
            copied = false
        }
    }
    fun start(w: StutterWindow, file: Uri) {
        report = null
        failure = false
        copied = false
        job = scope.launch(start = kotlinx.coroutines.CoroutineStart.LAZY) {
            try {
                val result = ExportStutterAnalysis.inspect(context, w, project.durationMs, file,
                    quality, preset, destination, frameMode) { stage = it }
                DiagnosticRecorder.exportStutter(result)
                report = result
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                DiagnosticRecorder.exception("export_stutter", error)
                failure = true
            } finally {
                stage = null
                job = null
            }
        }
        job?.start()
    }

    AlertDialog(
        onDismissRequest = { if (!running) onClose() },
        title = { Text("Analisar engasgos") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Compare até 5 s com o MP4 desta mesma montagem. Depois, o app exporta o clipe isolado, sem PiP, transições nem outras camadas. O projeto não muda e o teste não vai para a galeria.")
                if (source == null || window == null) {
                    Text("É necessário um clipe de vídeo com pelo menos meio segundo.")
                } else {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = { selected--; report = null; copied = false },
                            enabled = selected > 0 && !running) { Text("Anterior") }
                        Text("Clipe ${source.index + 1}")
                        TextButton(onClick = { selected++; report = null; copied = false },
                            enabled = selected < clips.lastIndex && !running) { Text("Próximo") }
                    }
                    if (source.clip.durationMs > 5_000L) {
                        Slider(value = (center - source.timelineStartMs).toFloat(),
                            onValueChange = {
                                center = source.timelineStartMs + it.toLong()
                                report = null
                                copied = false
                            }, valueRange = 0f..source.clip.durationMs.toFloat(), enabled = !running)
                    }
                    Text("${formatPipMs(window.projectStartMs)} a " +
                        formatPipMs(window.projectStartMs + window.durationMs),
                        style = MaterialTheme.typography.labelLarge)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(if (mp4 != null) "MP4 selecionado" else "Nenhum MP4",
                            modifier = Modifier.weight(1f))
                        TextButton(onClick = { pick.launch(arrayOf("video/mp4")) },
                            enabled = !running) { Text(if (mp4 == null) "Escolher MP4" else "Trocar MP4") }
                    }
                }
                if (running) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text(stage?.text ?: "Iniciando diagnóstico…")
                }
                if (failure) Text("Não foi possível concluir o diagnóstico.",
                    color = MaterialTheme.colorScheme.error)
                report?.let { result ->
                    HorizontalDivider()
                    Text(result.text(), style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = {
                        clipboard.setText(AnnotatedString(result.text()))
                        copied = true
                    }) { Text(if (copied) "Copiado" else "Copiar diagnóstico") }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = { if (running) job?.cancel() else onClose() }) {
                Text(if (running) "Cancelar" else "Fechar")
            }
        },
        confirmButton = {
            Button(onClick = {
                val file = mp4
                if (!running && window != null && file != null) start(window, file)
            }, enabled = !running && window != null && mp4 != null) { Text("Analisar") }
        }
    )
}
