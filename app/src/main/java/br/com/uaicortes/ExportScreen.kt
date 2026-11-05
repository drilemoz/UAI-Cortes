package br.com.uaicortes

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import androidx.lifecycle.viewmodel.compose.viewModel

// Extraído da MainActivity na v0.1.53 para reutilização de componentes.

@Composable
private fun CompactExportLabel(label: String, icon: ImageVector) {
    Row(verticalAlignment=Alignment.CenterVertically) {
        Icon(icon,contentDescription=null,modifier=Modifier.size(17.dp),tint=Blue)
        Spacer(Modifier.width(6.dp))
        Text(label,fontSize=14.sp,fontWeight=FontWeight.SemiBold)
    }
}

/** Componentes pequenos, mantendo área tocável de pelo menos 42 dp. */
@Composable
private fun CompactExportOption(label: String,icon: ImageVector?,selected: Boolean,
    enabled: Boolean,modifier: Modifier=Modifier,horizontal: Boolean=false,onSelect:()->Unit) {
    Surface(onClick=onSelect,enabled=enabled,modifier=modifier,
        color=if(selected) Blue.copy(alpha=.08f) else MaterialTheme.colorScheme.surface,
        shape=RoundedCornerShape(10.dp),border=BorderStroke(if(selected)2.dp else 1.dp,
            if(selected)Blue else MaterialTheme.colorScheme.outlineVariant)) {
        if(horizontal) {
            Row(Modifier.fillMaxSize().padding(horizontal=3.dp),
                verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.Center) {
                if(icon!=null) {
                    Icon(icon,contentDescription=null,modifier=Modifier.size(20.dp),
                        tint=if(selected)Blue else MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.width(4.dp))
                }
                Text(label,fontSize=13.sp,maxLines=1,overflow=TextOverflow.Ellipsis,
                    fontWeight=if(selected)FontWeight.SemiBold else FontWeight.Normal)
            }
        } else {
            Column(Modifier.fillMaxSize(),horizontalAlignment=Alignment.CenterHorizontally,
                verticalArrangement=Arrangement.Center) {
                if(icon!=null) Icon(icon,contentDescription=null,modifier=Modifier.size(25.dp),
                    tint=if(selected)Blue else MaterialTheme.colorScheme.onSurface)
                Text(label,fontSize=12.sp,maxLines=1,overflow=TextOverflow.Ellipsis,
                    fontWeight=if(selected)FontWeight.SemiBold else FontWeight.Normal)
            }
        }
    }
}

/** Diagrama ilustrativo, sem capturas de redes sociais ou falsas margens exatas. */
@Composable
private fun ExportFrameIllustration(crop: Boolean) {
    Column(horizontalAlignment=Alignment.CenterHorizontally) {
        Box(Modifier.width(77.dp).height(132.dp).clip(RoundedCornerShape(7.dp))
            .background(Color(0xFF192332)),contentAlignment=Alignment.Center) {
            Box(Modifier.fillMaxWidth(if(crop)1f else .96f)
                .fillMaxHeight(if(crop)1f else .37f)
                .background(Color(0xFF607F9C)),contentAlignment=Alignment.Center) {
                Icon(Icons.Default.Person,contentDescription=null,tint=Color.White,
                    modifier=Modifier.size(if(crop)58.dp else 29.dp))
            }
        }
        Text(if(crop)"Preencher" else "Mostrar tudo",fontSize=12.sp)
    }
}

@Composable
internal fun ExportScreen(p:VideoProject, defaultQuality:Int, back:()->Unit,
    onPublicationDraftSave: (VideoPublicationDraft) -> Boolean,
    onSocialPublish: (Uri, String, VideoPublicationDraft) -> Unit) {
    val context=LocalContext.current
    val exportVm: ExportTaskViewModel = viewModel()
    val exportState = exportVm.task
    val working = exportState.working
    val progress = exportState.progress
    val exportPhase = exportState.phase
    val elapsedSeconds = exportState.elapsedSeconds
    val exportingName = exportState.fileName
    val result = exportState.message
    val savedVideo = exportVm.savedVideo
    val savedName = exportVm.savedName
    val qualityReport = exportVm.qualityReport
    LaunchedEffect(p.id) { exportVm.setProject(p.id) }
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
    var destinationId by rememberSaveable(p.id) { mutableStateOf(ExportDestination.ORIGINAL.name) }
    var frameModeId by rememberSaveable(p.id) { mutableStateOf(VerticalFrameMode.FIT.name) }
    var showFrameGuides by rememberSaveable(p.id) { mutableStateOf(false) }
    val destination = ExportDestination.fromId(destinationId)
    val frameMode = VerticalFrameMode.fromId(frameModeId)
    val previewAspect = if (destination.isVertical) 9f / 16f else exportPreviewAspect
    DisposableEffect(previewPlayer) { onDispose {previewPlayer.release()} }
    LaunchedEffect(p.clips) {
        previewPlayer.setMediaItems(p.clips.map { clip ->
            MediaItem.Builder().setUri(clip.uri).setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder().setStartPositionMs(clip.startMs)
                    .setEndPositionMs(clip.endMs).build()).build()
        })
        previewPlayer.prepare()
    }
    var name by rememberSaveable(p.id) { mutableStateOf(ProjectNaming.exportName(p.title)) }
    var quality by rememberSaveable(p.id) {mutableIntStateOf(defaultQuality.takeIf { it in listOf(360, 480, 720, 1080) } ?: 1080)}
    var bitratePresetName by rememberSaveable(p.id) { mutableStateOf(ExportBitratePreset.HIGH.name) }
    var qualityHelpExpanded by rememberSaveable(p.id) { mutableStateOf(false) }
    var framingHelpExpanded by rememberSaveable(p.id) { mutableStateOf(false) }
    var previewExpanded by rememberSaveable(p.id) { mutableStateOf(false) }
    var guideKind by rememberSaveable(p.id) { mutableStateOf("BOTH") }
    LaunchedEffect(previewExpanded) {
        if (!previewExpanded) {
            previewPlayer.pause()
            previewPlaying = false
        }
    }
    val bitratePreset = ExportBitratePreset.fromId(bitratePresetName)
    var sourceShortSide by remember(p.id, p.clips) { mutableStateOf<Int?>(null) }
    // Só consulta metadados locais em IO: não bloqueia a tela e não faz upload da mídia.
    LaunchedEffect(p.id, p.clips) {
        sourceShortSide = withContext(Dispatchers.IO) {
            p.clips.mapNotNull { clip ->
                runCatching {
                    val retriever = MediaMetadataRetriever()
                    try {
                        retriever.setDataSource(context, Uri.parse(clip.uri))
                        val width = retriever.extractMetadata(
                            MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull()
                        val height = retriever.extractMetadata(
                            MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull()
                        if (width != null && height != null && width > 0 && height > 0)
                            minOf(width, height) else null
                    } finally { runCatching { retriever.release() } }
                }.getOrNull()
            }.minOrNull()
        }
    }
    var confirmCancelExport by remember {mutableStateOf(false)}
    // Conteúdo rolável e ação principal fixa. Em telas pequenas/alto zoom, as opções
    // permanecem acessíveis sem esconder o botão de exportação.
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())
            .padding(horizontal=16.dp)) {
            Row(Modifier.fillMaxWidth().height(46.dp), verticalAlignment=Alignment.CenterVertically) {
                IconButton(onClick={ if(!working) back() else confirmCancelExport=true }) {
                    Icon(Icons.Default.ArrowBack, "Voltar")
                }
                Text("Exportar vídeo",style=MaterialTheme.typography.titleMedium,
                    fontWeight=FontWeight.SemiBold,modifier=Modifier.weight(1f))
                Icon(Icons.Default.MovieCreation,contentDescription=null,tint=Blue)
            }
            Text("Nome do arquivo", style=MaterialTheme.typography.labelMedium,
                color=MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(value=name,onValueChange={name=it},singleLine=true,
                modifier=Modifier.fillMaxWidth(),textStyle=MaterialTheme.typography.bodyMedium,
                leadingIcon={Icon(Icons.Default.Description,contentDescription=null,modifier=Modifier.size(19.dp))},
                shape=RoundedCornerShape(12.dp),enabled=!working)
            Spacer(Modifier.height(12.dp))
            CompactExportLabel("Destino",Icons.Default.MovieFilter)
            Spacer(Modifier.height(5.dp))
            Row(horizontalArrangement=Arrangement.spacedBy(5.dp),modifier=Modifier.fillMaxWidth()) {
                listOf(
                    Triple(ExportDestination.ORIGINAL,Icons.Default.Movie,"Original"),
                    Triple(ExportDestination.SHORTS,Icons.Default.PlayCircle,"Shorts"),
                    Triple(ExportDestination.REELS,Icons.Default.Videocam,"Reels"),
                    Triple(ExportDestination.TIKTOK,Icons.Default.MusicNote,"TikTok")
                ).forEach { (item, icon, label) ->
                    CompactExportOption(label,icon,destination==item,!working,
                        Modifier.weight(1f).height(62.dp)) {
                        destinationId=item.name
                        bitratePresetName=SocialExportPolicy.recommendedBitrate(item).name
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            if (destination.isVertical) {
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
                    horizontalArrangement=Arrangement.SpaceBetween) {
                    CompactExportLabel("Enquadramento",Icons.Default.AspectRatio)
                    Text("9:16 vertical",style=MaterialTheme.typography.labelSmall,
                        color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(5.dp))
                Row(horizontalArrangement=Arrangement.spacedBy(7.dp),modifier=Modifier.fillMaxWidth()) {
                    CompactExportOption("Mostrar tudo",Icons.Default.AspectRatio,
                        frameMode==VerticalFrameMode.FIT,!working,
                        Modifier.weight(1f).height(42.dp), horizontal=true) {
                        frameModeId=VerticalFrameMode.FIT.name
                    }
                    CompactExportOption("Preencher",Icons.Default.CenterFocusStrong,
                        frameMode==VerticalFrameMode.CENTER_CROP,!working,
                        Modifier.weight(1f).height(50.dp),horizontal=true) {
                        frameModeId=VerticalFrameMode.CENTER_CROP.name
                    }
                }
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
                    horizontalArrangement=Arrangement.SpaceBetween) {
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        Checkbox(checked=showFrameGuides,onCheckedChange={
                            showFrameGuides=it
                            if(it) previewExpanded=true
                        },
                            enabled=!working,modifier=Modifier.size(38.dp))
                        Text("Guias na prévia",style=MaterialTheme.typography.labelMedium)
                    }
                    TextButton(onClick={framingHelpExpanded=true},enabled=!working,
                        contentPadding=PaddingValues(horizontal=4.dp,vertical=0.dp)) {
                        Icon(Icons.Default.HelpOutline,contentDescription=null,modifier=Modifier.size(17.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("Ver exemplos",fontSize=12.sp)
                    }
                }
                if (frameMode==VerticalFrameMode.CENTER_CROP) {
                    Text("Atenção: o corte central pode ocultar pessoas ou legendas nas bordas.",
                        fontSize=11.sp,color=MaterialTheme.colorScheme.tertiary,
                        lineHeight=14.sp)
                }
            }
            Surface(onClick={previewExpanded=!previewExpanded},
                modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(10.dp),
                color=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.48f)) {
                Row(Modifier.fillMaxWidth().padding(horizontal=10.dp,vertical=8.dp),
                    verticalAlignment=Alignment.CenterVertically) {
                    Icon(Icons.Default.Visibility,contentDescription=null,modifier=Modifier.size(18.dp),tint=Blue)
                    Spacer(Modifier.width(8.dp))
                    Text("Prévia do enquadramento",fontSize=12.sp,
                        modifier=Modifier.weight(1f),fontWeight=FontWeight.Medium)
                    Icon(if(previewExpanded)Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription=if(previewExpanded)"Recolher prévia" else "Expandir prévia",
                        modifier=Modifier.size(20.dp))
                }
            }
            if(previewExpanded) {
                Spacer(Modifier.height(8.dp))
                if(destination.isVertical && showFrameGuides) {
                    Row(horizontalArrangement=Arrangement.spacedBy(5.dp),
                        modifier=Modifier.fillMaxWidth()) {
                        listOf("TEXT" to "Legendas", "UI" to "Interface", "BOTH" to "Ambas").forEach { (key,label) ->
                            FilterChip(selected=guideKind==key,onClick={guideKind=key},
                                label={Text(label,fontSize=11.sp)},modifier=Modifier.height(32.dp),
                                enabled=!working)
                        }
                    }
                }
                Box(Modifier.fillMaxWidth(),contentAlignment=Alignment.Center) {
                    Box(Modifier.width(if(destination.isVertical) 204.dp else 300.dp)) {
                    InlineVideoPreview(previewAspect) {
                        AndroidView(factory={previewPlayer(it).apply {
                                player=previewPlayer; useController=false
                                configureExportPreviewFrame(this,
                                    destination.isVertical && frameMode==VerticalFrameMode.CENTER_CROP)
                            }},modifier=Modifier.fillMaxSize(),update={
                                it.player=previewPlayer
                                configureExportPreviewFrame(it,
                                    destination.isVertical && frameMode==VerticalFrameMode.CENTER_CROP)
                            })
                        p.clips.getOrNull(exportClipIndex)?.let { clip ->
                            val clipStart=p.clips.take(exportClipIndex).sumOf { it.durationMs }
                            val local=(exportPreviewMs-clipStart).coerceIn(0L,clip.durationMs)
                            val previous=EditFeatures.boundary(p,exportClipIndex-1)
                            val next=EditFeatures.boundary(p,exportClipIndex)
                            val fadeIn=EditFeatures.transitionMs(previous,
                                p.clips.getOrElse(exportClipIndex-1){clip}.durationMs,clip.durationMs)
                            val fadeOut=EditFeatures.transitionMs(next,clip.durationMs,
                                p.clips.getOrElse(exportClipIndex+1){clip}.durationMs)
                            TransitionPreviewTint(previous, fadeIn, next, fadeOut,
                                local, clip.durationMs)
                        }
                        p.overlays.forEach { pip ->
                            if(exportPreviewMs>=pip.startAtMs && exportPreviewMs<pip.startAtMs+pip.durationMs) {
                                key(pip.id) {
                                    PipLayer(pip,exportPreviewMs-pip.startAtMs,previewPlayer.isPlaying,
                                        editable=false) { }
                                }
                            }
                        }
                        p.subtitles.firstOrNull { cue ->
                            exportPreviewMs>=cue.startMs && exportPreviewMs<cue.endMs && cue.text.isNotBlank()
                        }?.let { cue -> SubtitleOverlayOnFrame(cue,editable=false,playbackMs=exportPreviewMs) }
                        p.textWatermark?.toSubtitle(p.durationMs)?.let { watermark ->
                            SubtitleOverlayOnFrame(watermark,editable=false,playbackMs=exportPreviewMs)
                        }
                        if(destination.isVertical && showFrameGuides) {
                            if(guideKind != "UI") {
                                Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) {
                                    Box(Modifier.fillMaxWidth(.82f).fillMaxHeight(.72f).border(
                                        1.dp,Color(0xFF5ED1E6),RoundedCornerShape(6.dp)))
                                }
                            }
                            if(guideKind != "TEXT") {
                                Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                                    .height(29.dp).background(Color.Black.copy(alpha=.47f)))
                                Box(Modifier.align(Alignment.CenterEnd).width(24.dp)
                                    .fillMaxHeight(.42f).background(Color.Black.copy(alpha=.30f)))
                            }
                        }
                        Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) {
                            IconButton(onClick={
                                previewPlaying=!previewPlaying
                                if(previewPlaying)previewPlayer.play() else previewPlayer.pause()
                            }) {
                                Icon(if(previewPlaying)Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                                    "Reproduzir prévia",tint=Color.White,
                                    modifier=Modifier.size(45.dp))
                            }
                        }
                    }
                    }
                }
                Text("Guias ilustrativas não entram no MP4; confira o arquivo final. " +
                    "A interface das plataformas muda e a prévia de efeitos pode ser aproximada.",
                    style=MaterialTheme.typography.labelSmall,
                    color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    CompactExportLabel("Resolução",Icons.Default.AspectRatio)
                    Spacer(Modifier.height(5.dp))
                    Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                        listOf(360,480,720,1080).forEach { q ->
                            CompactExportOption("${q}p",Icons.Default.Movie,
                                q==quality,!working,Modifier.weight(1f).height(58.dp)) {
                                quality=q
                            }
                        }
                    }
                }
            }
            Text(when(quality) {
                    360 -> "360p · arquivo menor e processamento mais leve"
                    480 -> "480p · econômico para prévias e envio rápido"
                    720 -> "720p · equilíbrio entre nitidez e tamanho"
                    else -> "1080p · melhor definição, com maior consumo de espaço"
                },fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,
                modifier=Modifier.padding(top=5.dp))
            Spacer(Modifier.height(11.dp))
            CompactExportLabel("Compressão",Icons.Default.Tune)
            Spacer(Modifier.height(5.dp))
            Row(horizontalArrangement=Arrangement.spacedBy(6.dp),modifier=Modifier.fillMaxWidth()) {
                ExportBitratePreset.values().forEach { preset ->
                    val mbps=VideoExportQuality.videoBitrateBps(quality,preset)/1_000_000
                    CompactExportOption("${preset.label} · ${mbps}M",null,
                        bitratePreset==preset,!working,Modifier.weight(1f).height(50.dp),horizontal=true) {
                        bitratePresetName=preset.name
                    }
                }
            }
            ExportSourceQualityPolicy.warning(sourceShortSide, quality)?.let { notice ->
                Text(notice,fontSize=11.sp,color=MaterialTheme.colorScheme.tertiary,
                    modifier=Modifier.padding(top=5.dp))
            }
            TextButton(onClick={qualityHelpExpanded=!qualityHelpExpanded},
                modifier=Modifier.fillMaxWidth(),enabled=!working,
                contentPadding=PaddingValues(vertical=2.dp)) {
                Icon(Icons.Default.Tune,contentDescription=null,modifier=Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(if(qualityHelpExpanded)"Ocultar detalhes técnicos" else "Detalhes e compatibilidade",
                    fontSize=12.sp)
                Spacer(Modifier.width(5.dp))
                Icon(if(qualityHelpExpanded)Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription=null,modifier=Modifier.size(16.dp))
            }
            if(qualityHelpExpanded) {
                Text("Resolução e bitrate são solicitações ao encoder; o MP4 real pode diferir. " +
                    "Bitrate elevado não recupera detalhes que a origem não tem. " +
                    "O arquivo fica na galeria e não é enviado à plataforma selecionada. " +
                    "Referência: ~${VideoExportQuality.estimatedMiBPerMinute(quality,true,bitratePreset)} MiB/min.",
                    style=MaterialTheme.typography.bodySmall,
                    color=MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Solicitado: ${SocialExportPolicy.requestedLabel(destination,quality)} · " +
                    "H.264/AAC · keyframe ~${VideoExportQuality.REQUESTED_KEYFRAME_INTERVAL_SECONDS}s",
                    style=MaterialTheme.typography.bodySmall,
                    color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if(result.isNotBlank()) Text(result,modifier=Modifier.padding(vertical=5.dp),
                color=if(result.startsWith("Erro"))MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurface)
            qualityReport?.let { report ->
                Spacer(Modifier.height(5.dp))
                Text("Último MP4 · diagnóstico",fontWeight=FontWeight.SemiBold)
                report.lines().forEach { line ->
                    Text(line,style=MaterialTheme.typography.bodySmall,
                        color=if(report.fellBackInResolution && line.startsWith("Aviso:"))
                            MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(10.dp))
        }
        Surface(shadowElevation=8.dp,color=MaterialTheme.colorScheme.surface) {
            Column(Modifier.fillMaxWidth().navigationBarsPadding()
                .padding(horizontal=16.dp,vertical=7.dp)) {
                Text("${SocialExportPolicy.requestedLabel(destination,quality)} · " +
                    "${VideoExportQuality.videoBitrateBps(quality,bitratePreset)/1_000_000} Mb/s · MP4",
                    fontSize=11.sp,maxLines=1,overflow=TextOverflow.Ellipsis,
                    color=MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Estimativa: ~${VideoExportQuality.estimatedMiBForDuration(p.durationMs,quality,true,bitratePreset)} MiB para este projeto",
                    fontSize=11.sp,maxLines=1,overflow=TextOverflow.Ellipsis,
                    color=MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(5.dp))
                Button(onClick={
                    if(name.isNotBlank() && p.clips.isNotEmpty() && !working) {
                        previewPlayer.pause()
                        previewPlaying=false
                        exportVm.start(p, quality, name, bitratePreset, destination, frameMode)
                    }
                },enabled=!working && name.isNotBlank() && p.clips.isNotEmpty(),
                    modifier=Modifier.fillMaxWidth().height(48.dp),
                    shape=RoundedCornerShape(14.dp),
                    colors=ButtonDefaults.buttonColors(containerColor=Blue)) {
                    Icon(Icons.Default.SaveAlt,contentDescription=null,modifier=Modifier.size(19.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Salvar vídeo",fontWeight=FontWeight.SemiBold)
                }
            }
        }
    }
    if(framingHelpExpanded) {
        AlertDialog(onDismissRequest={framingHelpExpanded=false},
            title={Text("Enquadramento e margens")},
            text={Column(verticalArrangement=Arrangement.spacedBy(9.dp)) {
                Row(horizontalArrangement=Arrangement.SpaceEvenly,modifier=Modifier.fillMaxWidth()) {
                    ExportFrameIllustration(false)
                    ExportFrameIllustration(true)
                }
                Text("Mostrar tudo: mantém o quadro completo, com barras quando a proporção muda.",
                    style=MaterialTheme.typography.bodySmall)
                Text("Preencher: usa a altura e a largura de 9:16, mas corta as laterais (ou topo/baixo) da origem.",
                    style=MaterialTheme.typography.bodySmall)
                Text("Guias de legendas: contorno central indica área sugerida. " +
                    "Guias de interface: faixas sombreadas simulam botões e descrição. " +
                    "As margens reais variam por rede e podem mudar.",
                    style=MaterialTheme.typography.bodySmall)
            }},
            confirmButton={TextButton(onClick={
                showFrameGuides=true
                previewExpanded=true
                framingHelpExpanded=false
            }) {Text("Ver guias na prévia")}},
            dismissButton={TextButton(onClick={framingHelpExpanded=false}) {Text("Fechar")}})
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
                Spacer(Modifier.height(5.dp))
                Text(ExportProgressUi.progressExplanation(exportPhase),
                    style=MaterialTheme.typography.labelSmall,
                    textAlign=TextAlign.Center,
                    color=MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(10.dp))
                        Text("${SocialExportPolicy.requestedLabel(destination,exportState.quality)} · " +
                            "${bitratePreset.label} · MP4",style=MaterialTheme.typography.bodySmall,
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
                exportVm.cancel()
            }) {Text("Cancelar exportação")}},
            dismissButton={TextButton(onClick={confirmCancelExport=false}) {Text("Continuar")}})
    }
    // Confirmação compartilhada com Animar Imagens, somente após MediaStore concluir.
    savedVideo?.let { savedUri ->
        VideoSavedDialog(uri=savedUri,name=savedName,editLabel="Voltar ao editor",
            exportDetails=qualityReport?.lines().orEmpty(), qualityReport=qualityReport,
            videoContext=remember(p.id, p.title, p.subtitles) {
                buildString {
                    if (p.title.isNotBlank() && p.title != "Meu vídeo")
                        append("Título do projeto: ${p.title.take(120)}\n")
                    val subtitleText = p.subtitles.sortedBy { it.startMs }
                        .map { it.text.trim() }.filter { it.isNotEmpty() }
                        .distinct().joinToString(" ").take(750)
                    if (subtitleText.isNotBlank()) append("Legendas do vídeo: $subtitleText")
                }
            },
            initialPublicationDraft=p.publicationDraft,
            onPublicationDraftSave=onPublicationDraftSave,
            onSocialPublish = { uri, name, draft ->
                exportVm.dismissSaved()
                onSocialPublish(uri, name, draft)
            },
            onEdit={ _ -> exportVm.dismissSaved();back()},onClose={exportVm.dismissSaved()})
    }

}
