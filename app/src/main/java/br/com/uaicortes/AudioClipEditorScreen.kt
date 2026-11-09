package br.com.uaicortes

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.roundToLong

/** Ferramenta independente. Uma fonte, um intervalo e um arquivo M4A por operação. */
@Composable
internal fun AudioClipEditorScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val player = remember(context) { ExoPlayer.Builder(context.applicationContext).build() }
    val exporter = remember(context) { AudioClipExportEngine(context.applicationContext) }
    var source by remember { mutableStateOf<Uri?>(null) }
    var sourceName by remember { mutableStateOf("") }
    var durationMs by remember { mutableLongStateOf(0L) }
    var startMs by remember { mutableLongStateOf(0L) }
    var endMs by remember { mutableLongStateOf(0L) }
    var waveform by remember { mutableStateOf<List<Float>>(emptyList()) }
    var checking by remember { mutableStateOf(false) }
    var scanning by remember { mutableStateOf(false) }
    var playing by remember { mutableStateOf(false) }
    var currentMs by remember { mutableLongStateOf(0L) }
    var processing by remember { mutableStateOf(false) }
    var phase by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var waveHint by remember { mutableStateOf("") }
    var savedUri by remember { mutableStateOf<Uri?>(null) }
    var incompleteUri by remember { mutableStateOf<Uri?>(null) }
    var deleteIncomplete by remember { mutableStateOf(false) }
    var deletingIncomplete by remember { mutableStateOf(false) }
    var showExitConfirmation by remember { mutableStateOf(false) }
    var repeatSelection by remember { mutableStateOf(false) }
    var pendingSelection by remember { mutableStateOf<Pair<Long,Long>?>(null) }

    DisposableEffect(player, exporter) {
        onDispose { player.release(); exporter.cancel() }
    }

    fun stop() { player.pause(); playing = false }
    fun exit() {
        if (processing) showExitConfirmation = true else onBack()
    }
    BackHandler { exit() }

    fun changeSource(uri: Uri) {
        if (processing) return
        stop(); player.clearMediaItems()
        source = uri
        sourceName = runCatching { mediaName(context, uri) }.getOrDefault("Áudio")
        durationMs = 0L; startMs = 0L; endMs = 0L
        currentMs = 0L; waveform = emptyList(); waveHint = ""
        errorMessage = ""; savedUri = null
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
        DiagnosticRecorder.action("audio_editor", "source_selected")
    }

    val pickAudio = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            changeSource(uri)
        }
    }
    val saveAudio = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("audio/mp4")) { destination ->
        val chosen = pendingSelection
        pendingSelection = null
        val uri = source
        if (destination != null && chosen != null && uri != null &&
            !processing && AudioClipSelectionPolicy.valid(durationMs, chosen.first, chosen.second)) {
            stop()
            processing = true; errorMessage = ""; savedUri = null
            incompleteUri = destination; phase = "Preparando recorte"
            DiagnosticRecorder.action("audio_editor", "export_start")
            exporter.export(uri, durationMs, chosen.first, chosen.second, destination,
                onPhase = { phase = it },
                onDone = {
                    processing = false; phase = ""
                    incompleteUri = null; savedUri = destination
                    DiagnosticRecorder.action("audio_editor", "export_success")
                },
                onError = {
                    processing = false; phase = ""; errorMessage = it
                    DiagnosticRecorder.action("audio_editor", "export_failure")
                })
        }
    }

    // Primeiro revela a duração, depois decodifica amplitudes sem bloquear a interface.
    LaunchedEffect(source) {
        val uri = source ?: return@LaunchedEffect
        checking = true; scanning = false
        try {
            val duration = AudioClipWaveform.duration(context.applicationContext, uri)
            durationMs = duration; startMs = 0L; endMs = duration
            checking = false; scanning = true
            val scan = runCatching { AudioClipWaveform.inspect(context.applicationContext, uri) }
            scan.onSuccess { result ->
                waveform = result.peaks
                if (result.peaks.isEmpty()) waveHint = "Onda não carregada para arquivo longo; use os controles abaixo."
            }.onFailure {
                waveHint = "Onda indisponível neste formato; o recorte continua disponível."
                DiagnosticRecorder.action("audio_editor", "waveform_unavailable")
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            errorMessage = "Não foi possível abrir esta faixa de áudio."
            player.clearMediaItems()
            durationMs = 0L
            DiagnosticRecorder.exception("audio_editor_probe", e)
        } finally { checking = false; scanning = false }
    }

    LaunchedEffect(playing, endMs, startMs, repeatSelection) {
        while (playing) {
            val position = player.currentPosition.coerceAtLeast(0L)
            currentMs = position
            if (position >= endMs - 15L || !player.isPlaying && player.playbackState == androidx.media3.common.Player.STATE_ENDED) {
                if (repeatSelection) {
                    player.seekTo(startMs); player.play(); currentMs = startMs
                } else stop()
            }
            delay(45L)
        }
    }

    val ready = source != null && !checking && AudioClipSelectionPolicy.valid(durationMs, startMs, endMs)
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Heading("Editor de Áudio", ::exit)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Corte e ouça o trecho antes de salvar. Funciona offline.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .55f),
                shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Default.AudioFile, null, tint = Blue, modifier = Modifier.size(32.dp))
                        Column(Modifier.weight(1f)) {
                            Text(sourceName.ifBlank { "Nenhum arquivo selecionado" },
                                fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            if (durationMs > 0L) Text("Duração: ${AudioClipSelectionPolicy.time(durationMs)}",
                                style = MaterialTheme.typography.bodySmall)
                        }
                        if (source != null && !processing) {
                            IconButton(onClick = { stop(); source = null; waveform = emptyList(); durationMs=0L; player.clearMediaItems() }) {
                                Icon(Icons.Default.Close, "Remover áudio")
                            }
                        }
                    }
                    OutlinedButton(onClick = { pickAudio.launch(arrayOf("audio/*")) },
                        enabled = !processing, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.FolderOpen, null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (source == null) "Escolher áudio" else "Trocar arquivo")
                    }
                }
            }
            if (checking) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text("Verificando o arquivo…", style = MaterialTheme.typography.bodySmall)
            }
            if (ready) {
                Text("Selecionar trecho", style=MaterialTheme.typography.titleMedium, fontWeight=FontWeight.SemiBold)
                Text("Arraste as extremidades azuis da onda ou ajuste o tempo pelos controles.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                AudioClipWaveformSelection(waveform, durationMs, startMs, endMs,
                    onStartChange = { stop(); startMs = AudioClipSelectionPolicy.start(durationMs, it, endMs); currentMs=startMs },
                    onEndChange = { stop(); endMs = AudioClipSelectionPolicy.end(durationMs, startMs, it); currentMs=startMs })
                if (scanning) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text("Calculando onda sonora…", style=MaterialTheme.typography.bodySmall)
                } else if (waveHint.isNotBlank()) Text(waveHint,
                    style=MaterialTheme.typography.bodySmall, color=MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(AudioClipSelectionPolicy.time(0L), style=MaterialTheme.typography.labelSmall)
                    Text(AudioClipSelectionPolicy.time(durationMs), style=MaterialTheme.typography.labelSmall)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AudioClipEdgeField("Início", startMs, Modifier.weight(1f),
                        onMinus = { stop(); startMs = AudioClipSelectionPolicy.start(durationMs,startMs-100L,endMs) },
                        onPlus = { stop(); startMs = AudioClipSelectionPolicy.start(durationMs,startMs+100L,endMs) })
                    AudioClipEdgeField("Fim", endMs, Modifier.weight(1f),
                        onMinus = { stop(); endMs = AudioClipSelectionPolicy.end(durationMs,startMs,endMs-100L) },
                        onPlus = { stop(); endMs = AudioClipSelectionPolicy.end(durationMs,startMs,endMs+100L) })
                }
                Text("Trecho selecionado: ${AudioClipSelectionPolicy.time(endMs-startMs)}",
                    fontWeight = FontWeight.Medium)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center) {
                    IconButton(onClick = { stop(); player.seekTo(startMs); currentMs=startMs }, enabled = !processing) {
                        Icon(Icons.Default.Replay, "Voltar ao início da seleção")
                    }
                    FilledIconButton(onClick = {
                        if (playing) stop() else {
                            player.seekTo(startMs)
                            player.play(); playing = true
                            DiagnosticRecorder.action("audio_editor", "preview_selection")
                        }
                    }, enabled = !processing, modifier = Modifier.size(56.dp)) {
                        Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                            if (playing) "Pausar trecho" else "Ouvir trecho")
                    }
                    IconButton(onClick = { repeatSelection = !repeatSelection }, enabled = !processing) {
                        Icon(Icons.Default.Repeat, "Repetir seleção",
                            tint = if (repeatSelection) Blue else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Text("Posição: ${AudioClipSelectionPolicy.time(currentMs)}",
                    style=MaterialTheme.typography.labelSmall,
                    modifier=Modifier.align(Alignment.CenterHorizontally))
            }
            if (processing) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text(phase, style=MaterialTheme.typography.bodyMedium)
                OutlinedButton(onClick = { exporter.cancel(); processing=false; errorMessage="Exportação cancelada. O arquivo incompleto pode ser excluído abaixo." },
                    modifier=Modifier.fillMaxWidth()) { Text("Cancelar exportação") }
            }
            if (savedUri != null) {
                Surface(shape=RoundedCornerShape(16.dp), color=MaterialTheme.colorScheme.primaryContainer) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        Text("Trecho salvo!", fontWeight=FontWeight.Bold)
                        Text("O arquivo M4A está no local escolhido.", style=MaterialTheme.typography.bodySmall)
                        TextButton(onClick = {
                            val share = Intent(Intent.ACTION_SEND).setType("audio/mp4")
                                .putExtra(Intent.EXTRA_STREAM, savedUri)
                                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            runCatching { context.startActivity(Intent.createChooser(share, "Compartilhar áudio")) }
                        }) { Text("Compartilhar arquivo") }
                    }
                }
            }
            if (errorMessage.isNotBlank()) Text(errorMessage, color=MaterialTheme.colorScheme.error,
                style=MaterialTheme.typography.bodySmall)
            if (incompleteUri != null && !processing) {
                TextButton(onClick = { deleteIncomplete = true }, enabled = !deletingIncomplete) {
                    Icon(Icons.Default.DeleteOutline, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Excluir arquivo incompleto")
                }
            }
            Spacer(Modifier.height(10.dp))
        }
        Surface(tonalElevation=2.dp) {
            Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal=20.dp,vertical=12.dp)) {
                BlueButton("Exportar trecho M4A", onClick = {
                    if (ready && !processing) {
                        stop()
                        pendingSelection = startMs to endMs
                        val base = AudioClipSelectionPolicy.exportBase(sourceName)
                        saveAudio.launch("${base}-trecho.m4a")
                    }
                }, modifier=Modifier.fillMaxWidth(), enabled=ready && !processing)
            }
        }
    }
    if (showExitConfirmation) AlertDialog(onDismissRequest={showExitConfirmation=false},
        title={Text("Cancelar exportação?")},
        text={Text("O processamento será interrompido. Um arquivo incompleto poderá precisar ser removido.")},
        confirmButton={TextButton(onClick={exporter.cancel();showExitConfirmation=false;onBack()}){Text("Sair e cancelar")}},
        dismissButton={TextButton(onClick={showExitConfirmation=false}){Text("Continuar")}})
    if (deleteIncomplete) AlertDialog(onDismissRequest={deleteIncomplete=false},
        title={Text("Excluir arquivo incompleto?")}, text={Text("Somente o arquivo de saída desta tentativa será excluído. O original permanece intacto.")},
        confirmButton={TextButton(onClick={
            deleteIncomplete=false; deletingIncomplete=true
            val uri = incompleteUri
            if (uri != null) exporter.removeIncomplete(uri) { done ->
                deletingIncomplete=false
                if (done) incompleteUri=null else errorMessage="O local de destino não permitiu excluir o arquivo."
            }
        }){Text("Excluir")}},
        dismissButton={TextButton(onClick={deleteIncomplete=false}){Text("Manter")}})
}

@Composable
private fun AudioClipEdgeField(label: String, timeMs: Long, modifier: Modifier,
    onMinus: () -> Unit, onPlus: () -> Unit) {
    Surface(modifier, shape=RoundedCornerShape(14.dp), color=MaterialTheme.colorScheme.surfaceVariant) {
        Column(Modifier.padding(10.dp), horizontalAlignment=Alignment.CenterHorizontally) {
            Text(label, style=MaterialTheme.typography.labelMedium)
            Text(AudioClipSelectionPolicy.time(timeMs), fontWeight=FontWeight.SemiBold)
            Row(verticalAlignment=Alignment.CenterVertically) {
                IconButton(onClick=onMinus) { Icon(Icons.Default.Remove, "Voltar 0,1 segundo") }
                Text("0,1 s", style=MaterialTheme.typography.labelSmall)
                IconButton(onClick=onPlus) { Icon(Icons.Default.Add, "Avançar 0,1 segundo") }
            }
        }
    }
}

/** Alças sobre a onda real; a seleção pode ser usada mesmo sem amplitudes decodificáveis. */
@Composable
private fun AudioClipWaveformSelection(peaks: List<Float>, durationMs: Long, startMs: Long,
    endMs: Long, onStartChange: (Long)->Unit, onEndChange: (Long)->Unit) {
    val selected = Blue
    val muted = MaterialTheme.colorScheme.outlineVariant
    val startState by rememberUpdatedState(startMs)
    val endState by rememberUpdatedState(endMs)
    var dragStart by remember { mutableStateOf(true) }
    Canvas(Modifier.fillMaxWidth().height(125.dp)
        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(15.dp))
        .pointerInput(durationMs) {
            detectDragGestures(onDragStart={ position ->
                val begin = size.width * startState.toFloat()/durationMs.coerceAtLeast(1L)
                val finish = size.width * endState.toFloat()/durationMs.coerceAtLeast(1L)
                dragStart = abs(position.x-begin) <= abs(position.x-finish)
            }, onDrag={ change, _ ->
                change.consume()
                val value = ((change.position.x.coerceIn(0f,size.width.toFloat()) / size.width.coerceAtLeast(1)) * durationMs)
                    .roundToLong()
                if (dragStart) onStartChange(value) else onEndChange(value)
            })
        }) {
        val width = size.width
        val height = size.height
        val left = width * startMs.toFloat() / durationMs.coerceAtLeast(1L).toFloat()
        val right = width * endMs.toFloat() / durationMs.coerceAtLeast(1L).toFloat()
        drawRect(selected.copy(alpha=.10f), Offset(left,0f), Size((right-left).coerceAtLeast(0f),height))
        if (peaks.isNotEmpty()) {
            val step=width / peaks.size
            peaks.forEachIndexed { index, amplitude ->
                val x=step*(index+.5f)
                val peakHeight=height*.78f*amplitude.coerceIn(.025f,1f)
                drawLine(if (x in left..right) selected else muted,
                    Offset(x,(height-peakHeight)/2f),Offset(x,(height+peakHeight)/2f),strokeWidth=step.coerceAtMost(5f)*.7f)
            }
        } else {
            drawLine(muted,Offset(0f,height/2f),Offset(width,height/2f),strokeWidth=2.dp.toPx())
        }
        drawLine(selected,Offset(left,0f),Offset(left,height),strokeWidth=4.dp.toPx())
        drawLine(selected,Offset(right,0f),Offset(right,height),strokeWidth=4.dp.toPx())
        drawCircle(selected,radius=7.dp.toPx(),center=Offset(left,height/2f))
        drawCircle(selected,radius=7.dp.toPx(),center=Offset(right,height/2f))
    }
}
