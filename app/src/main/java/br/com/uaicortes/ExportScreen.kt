package br.com.uaicortes

import android.net.Uri
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import kotlinx.coroutines.delay

// Extraído da MainActivity na v0.1.53 para reutilização de componentes.

@Composable
internal fun ExportScreen(p:VideoProject,defaultQuality:Int,back:()->Unit) {
    val context=LocalContext.current
    val engine=remember { ExportEngine(context) }
    val previewPlayer=remember { diagnosticPlayer(context) }
    var previewPlaying by remember { mutableStateOf(false) }
    var exportClipIndex by remember(p.id) { mutableIntStateOf(0) }
    var exportPreviewMs by remember(p.id) { mutableLongStateOf(0L) }
    LaunchedEffect(previewPlayer, p.clips) {
        while(true) {
            if (p.clips.isNotEmpty()) {
                exportClipIndex=previewPlayer.currentMediaItemIndex.coerceIn(0,p.clips.lastIndex)
                exportPreviewMs=ExportTimingPolicy.projectTime(p.clips,exportClipIndex,
                    previewPlayer.currentPosition)
            }
            delay(100)
        }
    }
    val exportPreviewUri=p.clips.getOrNull(exportClipIndex)?.uri
    val exportPreviewAspect=remember(exportPreviewUri) {
        exportPreviewUri?.let { mediaAspect(context,it,false) } ?: 16f/9f
    }
    DisposableEffect(previewPlayer) { onDispose {previewPlayer.release()} }
    LaunchedEffect(p.clips) {
        previewPlayer.setMediaItems(p.clips.map { clip ->
            MediaItem.Builder().setUri(clip.uri).setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder().setStartPositionMs(clip.startMs)
                    .setEndPositionMs(clip.endMs).build()).build()
        })
        previewPlayer.prepare()
    }
    var name by remember(p.id) {mutableStateOf("UaiCortes_001.mp4")}
    var quality by remember {mutableIntStateOf(defaultQuality.takeIf { it == 720 || it == 1080 } ?: 1080)}
    var working by remember {mutableStateOf(false)}
    // O percentual só é exibido quando o Media3 o disponibiliza de verdade.
    var progress by remember {mutableStateOf<Int?>(null)}
    var exportPhase by remember {mutableStateOf(ExportPhase.PREPARING)}
    var elapsedSeconds by remember {mutableLongStateOf(0L)}
    var startedAtMs by remember {mutableLongStateOf(0L)}
    var confirmCancelExport by remember {mutableStateOf(false)}
    var exportingName by remember {mutableStateOf("")}
    var result by remember {mutableStateOf("")}
    // Estados do diálogo de sucesso devem pertencer ao ExportScreen, ANTES dos callbacks.
    // Se o projeto mudar, a confirmação anterior não pode apontar para outro MP4.
    var savedName by remember(p.id) { mutableStateOf("") }
    var savedVideo by remember(p.id) { mutableStateOf<Uri?>(null) }
    DisposableEffect(engine) {onDispose{engine.cancel()} }
    LaunchedEffect(working) {
        while (working) {
            elapsedSeconds = ((android.os.SystemClock.elapsedRealtime() - startedAtMs)
                .coerceAtLeast(0L) / 1000L)
            if (exportPhase == ExportPhase.RENDERING) progress = engine.progressOrNull()
            delay(400)
        }
    }
    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal=20.dp)) {
        Heading("Exportar vídeo",{if(!working)back() else confirmCancelExport=true},
            accessory={Icon(Icons.Default.Movie,tint=Blue,contentDescription=null)})
        Spacer(Modifier.height(8.dp))
        InlineVideoPreview(exportPreviewAspect) {
            AndroidView(factory={previewPlayer(it).apply { player=previewPlayer;useController=false }},
                modifier=Modifier.fillMaxSize(),update={it.player=previewPlayer})
            // Mesma camada visual do editor: renderize o PROJETO, não só o arquivo original.
            p.clips.getOrNull(exportClipIndex)?.let { clip ->
                val clipStart = p.clips.take(exportClipIndex).sumOf { it.durationMs }
                val local = (exportPreviewMs-clipStart).coerceIn(0L,clip.durationMs)
                val previous = EditFeatures.boundary(p,exportClipIndex-1)
                val next = EditFeatures.boundary(p,exportClipIndex)
                val fadeIn = EditFeatures.transitionMs(previous,
                    p.clips.getOrElse(exportClipIndex-1){clip}.durationMs,clip.durationMs)
                val fadeOut = EditFeatures.transitionMs(next,clip.durationMs,
                    p.clips.getOrElse(exportClipIndex+1){clip}.durationMs)
                TransitionPreviewTint(previous, fadeIn, next, fadeOut,
                    local, clip.durationMs)
            }
            p.overlays.forEach { pip ->
                if(exportPreviewMs >= pip.startAtMs && exportPreviewMs < pip.startAtMs+pip.durationMs) {
                    key(pip.id) {
                        PipLayer(pip,exportPreviewMs-pip.startAtMs,previewPlayer.isPlaying,
                            editable=false) { }
                    }
                }
            }
            p.subtitles.firstOrNull { cue ->
                exportPreviewMs >= cue.startMs && exportPreviewMs < cue.endMs && cue.text.isNotBlank()
            }?.let { cue -> SubtitleOverlayOnFrame(cue,editable=false,playbackMs=exportPreviewMs) }
            p.textWatermark?.toSubtitle(p.durationMs)?.let { watermark ->
                SubtitleOverlayOnFrame(watermark, editable=false, playbackMs=exportPreviewMs)
            }
            Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) {
                IconButton(onClick={
                    previewPlaying=!previewPlaying
                    if(previewPlaying) previewPlayer.play() else previewPlayer.pause()
                }) {Icon(if(previewPlaying)Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                    "Reproduzir prévia",tint=Color.White,modifier=Modifier.size(52.dp))}
            }
        }
        if(p.subtitles.isNotEmpty() || p.overlays.isNotEmpty() || p.textWatermark != null) {
            Text("Prévia: legendas, PiPs e transições. Confira o MP4 final para validar áudio e efeitos.",
                style=MaterialTheme.typography.bodySmall,
                color=MaterialTheme.colorScheme.onSurfaceVariant,
                modifier=Modifier.padding(top=7.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text("Nome do arquivo",color=MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value=name,onValueChange={name=it},singleLine=true,modifier=Modifier.fillMaxWidth(),
            shape=RoundedCornerShape(15.dp),enabled=!working)
        Spacer(Modifier.height(22.dp));Text("Qualidade",fontSize=21.sp,fontWeight=FontWeight.Medium)
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            listOf(720,1080).forEach {q->
                Surface(onClick={quality=q},enabled=!working,modifier=Modifier.weight(1f).height(104.dp),
                    shape=RoundedCornerShape(18.dp),border=BorderStroke(if(q==quality)2.dp else 1.dp,
                        if(q==quality)Blue else MaterialTheme.colorScheme.outlineVariant)) {
                    Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
                        Text("${q}p",fontSize=19.sp)
                        Text(if(q==720)"8 Mb/s" else "18 Mb/s",fontSize=13.sp,
                            color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        Text("Bitrate de vídeo solicitado. O aparelho pode ajustar o valor. " +
            "Espaço estimado: ~${VideoExportQuality.estimatedMiBPerMinute(quality, true)} MiB/min; " +
            "a origem limita a nitidez final.",
            style=MaterialTheme.typography.bodySmall,
            color=MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(18.dp))
        BlueButton("↓ Salvar vídeo",onClick={
            if(name.isNotBlank() && p.clips.isNotEmpty() && !working) {
                DiagnosticRecorder.action("export", "export_start_${quality}p")
                previewPlayer.pause()
                previewPlaying = false
                result = ""
                progress = null
                exportPhase = ExportPhase.PREPARING
                startedAtMs = android.os.SystemClock.elapsedRealtime()
                elapsedSeconds = 0L
                exportingName = name.trim().let { if(it.endsWith(".mp4",true)) it else "$it.mp4" }
                working = true
                runCatching { engine.start(p,quality,name,
                    onSaving = {
                        exportPhase = ExportPhase.SAVING
                        progress = null
                    },
                    onSuccess = { uri ->
                        working = false
                        DiagnosticRecorder.action("export", "export_success")
                        savedName = exportingName
                        savedVideo = uri
                    },
                    onError = { error ->
                        working = false
                        DiagnosticRecorder.failure("export", error)
                        result = "Erro: $error"
                    })
                }.onSuccess {
                    if (working && exportPhase == ExportPhase.PREPARING) exportPhase = ExportPhase.RENDERING
                }.onFailure {
                    working = false
                    DiagnosticRecorder.exception("export_start",it)
                    result = "Erro: ${it.message ?: "Falha ao iniciar a exportação"}"
                }
            }
        },modifier=Modifier.fillMaxWidth(),enabled=!working && name.isNotBlank()&&p.clips.isNotEmpty())
        if(result.isNotBlank()) Text(result,modifier=Modifier.padding(vertical=15.dp),
            color=if(result.startsWith("Erro"))MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(14.dp));Text("MP4 · H.264/AAC · Qualidade alta · Salvamento local.",
            color=MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.bodySmall)
    }
    // Enquanto o Media3 processa, a tela de exportação fica inteira visível.
    // Sem percentual disponível, usar indicador indeterminado em vez de inventar progresso.
    if (working) {
        Dialog(onDismissRequest = {}, properties = DialogProperties(
            dismissOnBackPress = false, dismissOnClickOutside = false,
            usePlatformDefaultWidth = false)) {
            Surface(Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.background) {
                Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
                        .padding(horizontal=28.dp,vertical=28.dp),
                    horizontalAlignment=Alignment.CenterHorizontally,
                    verticalArrangement=Arrangement.SpaceBetween) {
                    Column(horizontalAlignment=Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Movie,contentDescription=null,tint=Blue,
                            modifier=Modifier.size(40.dp))
                        Spacer(Modifier.height(14.dp))
                        Text("Exportando vídeo",style=MaterialTheme.typography.headlineSmall,
                            fontWeight=FontWeight.Bold)
                        Spacer(Modifier.height(9.dp))
                        Text(exportingName,maxLines=2,overflow=TextOverflow.Ellipsis,
                            textAlign=TextAlign.Center,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(horizontalAlignment=Alignment.CenterHorizontally,
                        modifier=Modifier.fillMaxWidth()) {
                        val currentProgress = ExportProgressUi.renderPercent(exportPhase,progress)
                        if (currentProgress != null) {
                            CircularProgressIndicator(progress={currentProgress / 100f},
                                modifier=Modifier.size(144.dp),strokeWidth=9.dp)
                            Spacer(Modifier.height(18.dp))
                            Text("$currentProgress%",fontSize=32.sp,fontWeight=FontWeight.Bold)
                        } else {
                            CircularProgressIndicator(modifier=Modifier.size(100.dp),strokeWidth=7.dp)
                        }
                        Spacer(Modifier.height(22.dp))
                        Text(ExportProgressUi.stageLabel(exportPhase),
                            textAlign=TextAlign.Center,style=MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(12.dp))
                        Text("Tempo decorrido: ${ExportProgressUi.elapsedLabel(elapsedSeconds)}",
                            color=MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(10.dp))
                        Text("Qualidade: ${quality}p · MP4",style=MaterialTheme.typography.bodySmall,
                            color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(horizontalAlignment=Alignment.CenterHorizontally,
                        modifier=Modifier.fillMaxWidth()) {
                        Text("Mantenha o aplicativo aberto até terminar.",
                            textAlign=TextAlign.Center,style=MaterialTheme.typography.bodySmall,
                            color=MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(18.dp))
                        OutlinedButton(onClick={confirmCancelExport=true},
                            enabled=ExportProgressUi.canCancel(exportPhase),
                            modifier=Modifier.fillMaxWidth()) {
                            Text(if(exportPhase==ExportPhase.SAVING) "Concluindo salvamento…" else "Cancelar exportação")
                        }
                    }
                }
            }
        }
    }
    if (confirmCancelExport && working && ExportProgressUi.canCancel(exportPhase)) {
        AlertDialog(onDismissRequest={confirmCancelExport=false},
            title={Text("Cancelar exportação?")},
            text={Text("O vídeo parcial será descartado. O projeto e os arquivos originais serão preservados.")},
            confirmButton={TextButton(onClick={
                confirmCancelExport=false
                engine.cancel()
                working=false
                progress=null
                result="Exportação cancelada."
                DiagnosticRecorder.action("export","export_cancelled")
            }) {Text("Cancelar exportação")}},
            dismissButton={TextButton(onClick={confirmCancelExport=false}) {Text("Continuar")}})
    }
    // Confirmação compartilhada com Animar Imagens, somente após MediaStore concluir.
    savedVideo?.let { savedUri ->
        VideoSavedDialog(uri=savedUri,name=savedName,editLabel="Voltar ao editor",
            onEdit={savedVideo=null;back()},onClose={savedVideo=null})
    }

}
