package br.com.uaicortes

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import kotlin.math.abs
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import kotlinx.coroutines.delay

// Extraído da MainActivity na v0.1.53 para reutilização de componentes.

internal fun subtitleFont(family: String): FontFamily = when (SubtitleStyles.normalizeFont(family)) {
    SubtitleStyles.FONT_SERIF -> FontFamily.Serif
    SubtitleStyles.FONT_MONO -> FontFamily.Monospace
    else -> FontFamily.SansSerif
}

/** Quadro sem barras pretas adicionais: respeita a proporção original e a
 * altura disponível em cada aparelho. Mantém overlays no mesmo referencial
 * geométrico da imagem, inclusive em vídeos verticais e quadrados. */
@Composable
internal fun InlineVideoPreview(aspect: Float, modifier: Modifier = Modifier,
    maxHeightDp: Float? = null, content: @Composable BoxScope.() -> Unit) {
    val screenHeight = LocalConfiguration.current.screenHeightDp.toFloat()
    BoxWithConstraints(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val fit = if (maxHeightDp == null) AdaptiveVideoLayout.inline(maxWidth.value, screenHeight, aspect)
            else AdaptiveVideoLayout.fit(maxWidth.value, maxHeightDp, aspect)
        Box(Modifier.width(fit.width.dp).height(fit.height.dp)
            .clip(RoundedCornerShape(12.dp)).background(Color(0xFF101014)),
            content = content)
    }
}

/** Mede a maior palavra para impedir que a prévia estreita quebre sílabas.
 * A redução é apenas visual; sizeSp e o arquivo SRT não são modificados. */
internal fun previewSubtitleScale(cue: SubtitleCue, widthPx: Int, textSizePx: Float): Float {
    if (widthPx <= 0 || textSizePx <= 0f || !textSizePx.isFinite()) return 1f
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        textSize = textSizePx
        typeface = android.graphics.Typeface.create(when (SubtitleStyles.normalizeFont(cue.fontFamily)) {
            SubtitleStyles.FONT_SERIF -> "serif"
            SubtitleStyles.FONT_MONO -> "monospace"
            else -> "sans-serif"
        }, when { cue.bold && cue.italic -> android.graphics.Typeface.BOLD_ITALIC
            cue.italic -> android.graphics.Typeface.ITALIC
            cue.bold -> android.graphics.Typeface.BOLD
            else -> android.graphics.Typeface.NORMAL })
    }
    val longest = cue.text.split(Regex("\\s+")).maxOfOrNull { paint.measureText(it) } ?: 0f
    val total = cue.text.lines().sumOf { paint.measureText(it).toDouble() }.toFloat()
    val available = (widthPx * .90f - textSizePx * .65f).coerceAtLeast(1f)
    return SubtitleAutoFit.factor(longest, total, available)
}

/** A mesma coordenada x/y usada pelo exportador Media3: centro na horizontal,
 * base do texto na vertical. Nunca usar deslocamento fixo em dp para a legenda. */
@Composable
internal fun SubtitleOverlayOnFrame(cue: SubtitleCue, editable: Boolean,
    playbackMs: Long = cue.startMs, onPositionCommit: (Float,Float)->Unit = { _,_ -> }) {
    val animationFrame=SubtitleAnimations.frame(cue,playbackMs)
    var frameSize by remember { mutableStateOf(IntSize.Zero) }
    var textSize by remember { mutableStateOf(IntSize.Zero) }
    var draftX by remember(cue.id) { mutableFloatStateOf(cue.x) }
    var draftY by remember(cue.id) { mutableFloatStateOf(cue.y) }
    var dragging by remember(cue.id) { mutableStateOf(false) }
    val latestCommit by rememberUpdatedState(onPositionCommit)
    LaunchedEffect(cue.id,cue.x,cue.y) {
        if (!dragging) { draftX=cue.x; draftY=cue.y }
    }
    Box(Modifier.fillMaxSize().onSizeChanged { frameSize=it }) {
        if (animationFrame.text.isNotBlank()) {
            val rawLocation=SubtitlePlacement.anchorPixels(frameSize.width,frameSize.height,
                textSize.width,textSize.height,draftX,draftY)
            val location=rawLocation.first.coerceIn(0,(frameSize.width-textSize.width).coerceAtLeast(0)) to
                rawLocation.second.coerceIn(0,(frameSize.height-textSize.height).coerceAtLeast(0))
            val density=LocalDensity.current
            val frameWidthDp=with(density) { frameSize.width.toDp() }
            val textLimitPx=minOf(frameSize.width * .94f,with(density) { 280.dp.toPx() })
            val fontScale=previewSubtitleScale(cue,textLimitPx.toInt(),
                with(density) { cue.sizeSp.sp.toPx() })
            val previewFontSp=cue.sizeSp * fontScale
            val family = subtitleFont(cue.fontFamily)
            val textWeight = if (cue.bold) FontWeight.Bold else FontWeight.Normal
            val fontStyle = if (cue.italic) androidx.compose.ui.text.font.FontStyle.Italic
                else androidx.compose.ui.text.font.FontStyle.Normal
            val outlineWidthPx = with(density) { cue.outlineWidth.dp.toPx() } * fontScale
            val shadowPx = with(density) { cue.shadowOffsetDp.dp.toPx() } * fontScale
            val alignment = when (SubtitleStyles.normalizeAlignment(cue.textAlignment)) {
                SubtitleStyles.ALIGN_LEFT -> TextAlign.Left
                SubtitleStyles.ALIGN_RIGHT -> TextAlign.Right
                else -> TextAlign.Center
            }
            val spacing = SubtitleStyles.normalizeLineSpacing(cue.lineSpacingPercent) / 100f
            val textLineHeight = if (cue.lineSpacingPercent > 100)
                (previewFontSp * 1.2f * spacing).sp else TextUnit.Unspecified
            val fillStyle = TextStyle(lineHeight = textLineHeight,
                shadow = if (cue.shadowOffsetDp > 0) Shadow(
                    color=Color(cue.shadowColor), offset=Offset(shadowPx,shadowPx),blurRadius=0f
                ) else null)
            Box(modifier=Modifier.offset { IntOffset(location.first,location.second) }
                .widthIn(max=minOf(280.dp,(frameWidthDp * .94f).coerceAtLeast(14.dp)))
                .onSizeChanged { textSize=it }
                .graphicsLayer {
                    alpha=animationFrame.alpha
                    scaleX=animationFrame.scale
                    scaleY=animationFrame.scale
                    translationY=animationFrame.slideY * frameSize.height
                    translationX=animationFrame.slideX * frameSize.width
                }
                .then(if (editable) Modifier.pointerInput(cue.id,frameSize) {
                    detectDragGestures(
                        onDragStart={dragging=true},
                        onDragEnd={dragging=false;latestCommit(draftX,draftY)},
                        onDragCancel={dragging=false;draftX=cue.x;draftY=cue.y},
                        onDrag={change,delta->
                            change.consume()
                            val next=SubtitlePlacement.drag(draftX,draftY,delta.x,delta.y,
                                frameSize.width,frameSize.height)
                            draftX=next.first;draftY=next.second
                        })
                } else Modifier)
                .background(Color(cue.backgroundColor),RoundedCornerShape(3.dp))
                .padding(horizontal=7.dp,vertical=2.dp)) {
                if (cue.outlineWidth > 0) {
                    Text(animationFrame.text,color=Color(cue.outlineColor),fontSize=previewFontSp.sp,
                        fontFamily=family,fontWeight=textWeight,fontStyle=fontStyle,textAlign=alignment,
                        style=TextStyle(lineHeight=textLineHeight,
                            drawStyle=Stroke(width=outlineWidthPx*2f,join=StrokeJoin.Round)))
                }
                val animatedText=buildAnnotatedString {
                    append(animationFrame.text)
                    SubtitleWordEffects.colors(cue,animationFrame.text,animationFrame.highlighted,
                        animationFrame.highlightColorOverride ?: cue.highlightColor)
                        .forEach { colored ->
                            addStyle(SpanStyle(color=Color(colored.color)),
                                colored.range.first,colored.range.last+1)
                        }
                    animationFrame.highlighted?.takeIf {
                        it.first >= 0 && it.last < animationFrame.text.length
                    }?.let { selected ->
                        if (animationFrame.boldActiveWord || animationFrame.underlineActiveWord)
                            addStyle(SpanStyle(
                                fontWeight=if (animationFrame.boldActiveWord) FontWeight.Bold else null,
                                textDecoration=if (animationFrame.underlineActiveWord)
                                    androidx.compose.ui.text.style.TextDecoration.Underline else null
                            ),selected.first,selected.last+1)
                    }
                }
                Text(animatedText,color=Color(cue.color),fontSize=previewFontSp.sp,
                    fontFamily=family,fontWeight=textWeight,fontStyle=fontStyle,textAlign=alignment,
                    style=fillStyle)
            }
        }
    }
}

/** Prévia independente da existência de legendas. Usa os mesmos MediaItems da
 * timeline principal, inclusive cortes e fotos. Um único cursor em tempo absoluto
 * alimenta player, régua, seleção e criação manual de legendas.
 * Somente onSeekRequest faz seek; polling de reprodução não realimenta o player.
 */
@Composable
internal fun SubtitlePositionPreview(p: VideoProject, cue: SubtitleCue?,
    cursorMs: Long, seekGeneration: Int, seekTargetMs: Long,
    onCursorChange: (Long) -> Unit,
    onSeekRequest: (Long) -> Unit, onPositionCommit: (Float, Float) -> Unit,
    onPrevious: () -> Unit, onNext: () -> Unit,
    hasPrevious: Boolean, hasNext: Boolean, compactPreview: Boolean = true,
    followPlayback: Boolean, onToggleFollow: () -> Unit,
    onToggleCompact: () -> Unit, onTimelineHelp: () -> Unit,
    zoomFactor: Int, onZoomChange: (Int) -> Unit) {
    val context = LocalContext.current
    val player = remember(p.id) { diagnosticPlayer(context) }
    var playing by remember(p.id) { mutableStateOf(false) }
    var scrubbing by remember(p.id) { mutableStateOf(false) }
    var scrubMs by remember(p.id) { mutableLongStateOf(0L) }
    var zoomMenuExpanded by remember(p.id) { mutableStateOf(false) }
    val latestCursor by rememberUpdatedState(cursorMs)
    DisposableEffect(player) { onDispose { player.release() } }
    LaunchedEffect(player, p.clips.map { Triple(it.uri, it.startMs, it.endMs) },
        p.clips.map { Triple(it.id, it.isImage, it.durationMs) }) {
        playing = false
        player.pause()
        player.setMediaItems(p.clips.map(ClipMediaItems::playback))
        if (p.clips.isNotEmpty()) {
            player.prepare()
            seekProjectPosition(player, p.clips, latestCursor)
        }
    }
    LaunchedEffect(seekGeneration) {
        if (seekGeneration > 0 && p.clips.isNotEmpty()) {
            player.pause()
            playing = false
            seekProjectPosition(player, p.clips, seekTargetMs)
            onCursorChange(seekTargetMs)
        }
    }
    LaunchedEffect(playing, player, p.clips.map { it.durationMs }, p.durationMs) {
        while (playing) {
            val position = projectPosition(player, p.clips, p.durationMs)
            onCursorChange(position)
            if (player.playbackState == Player.STATE_ENDED) {
                playing = false
                break
            }
            delay(120)
        }
    }
    val visibleCues = SubtitlePreviewPolicy.activeAt(p.subtitles, cursorMs)
    val source = remember(p.clips, cursorMs) {
        TimelinePrecision.sourceAt(p.clips.map { it.uri to it.durationMs }, cursorMs)
    }
    val clip = p.clips.getOrNull(source?.first ?: -1)
    val originalAspect = remember(clip?.uri, clip?.isImage) {
        clip?.let { mediaAspect(context, it.uri, it.isImage) } ?: 16f / 9f
    }
    val imageCanvasAspect = remember(p.clips.map { it.uri to it.isImage }) {
        p.clips.firstOrNull { !it.isImage }?.let { mediaAspect(context, it.uri, false) }
            ?: 9f / 16f
    }
    val aspect = if (clip?.isImage == true) imageCanvasAspect else originalAspect
    val imageTransform = clip?.takeIf { it.isImage }?.let {
        ImageFraming.resolve(it.imageFrameMode, it.imageZoom, it.imagePanX, it.imagePanY,
            originalAspect, aspect)
    }
    val total = p.durationMs.coerceAtLeast(1L)
    Column {
        InlineVideoPreview(aspect, maxHeightDp = if (compactPreview) 190f else null) {
            if (p.clips.isNotEmpty()) {
                AndroidView(factory = { ctx -> previewPlayer(ctx).apply {
                    this.player = player
                    useController = false
                } }, update = { it.player = player },
                    modifier = Modifier.fillMaxSize().graphicsLayer {
                        scaleX = imageTransform?.scale ?: 1f
                        scaleY = imageTransform?.scale ?: 1f
                        translationX = (imageTransform?.offsetX ?: 0f) * size.width / 2f
                        translationY = (imageTransform?.offsetY ?: 0f) * size.height / 2f
                    })
                visibleCues.forEach { visible ->
                    key(visible.id) {
                        SubtitleOverlayOnFrame(visible,
                            editable = !playing && visible.id == cue?.id,
                            playbackMs = cursorMs, onPositionCommit = onPositionCommit)
                    }
                }
            } else Text("Adicione um vídeo ou uma imagem ao projeto",
                color = Color.White, fontSize = 12.sp,
                modifier = Modifier.align(Alignment.Center))
        }
        // Uma barra única: título e ajuda à esquerda; navegação e opções à direita.
        // Não há rótulos de legenda duplicados enquanto o vídeo está em reprodução.
        Row(Modifier.fillMaxWidth().heightIn(min=40.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Linha do tempo", fontSize=12.sp, fontWeight=FontWeight.SemiBold,
                maxLines=1, softWrap=false, modifier=Modifier.weight(1f, fill=false))
            IconButton(onClick=onTimelineHelp,modifier=Modifier.size(30.dp)) {
                Icon(Icons.Default.HelpOutline,"Ajuda da linha do tempo",modifier=Modifier.size(18.dp))
            }
            IconButton(enabled=hasPrevious,onClick=onPrevious,modifier=Modifier.size(34.dp)) {
                Icon(Icons.Default.ChevronLeft,"Legenda anterior",modifier=Modifier.size(23.dp))
            }
            IconButton(enabled=p.clips.isNotEmpty(),onClick={
                onSeekRequest(cue?.startMs ?: 0L)
            },modifier=Modifier.size(34.dp)) {
                Icon(Icons.Default.SkipPrevious,
                    if(cue==null) "Voltar ao início" else "Reiniciar legenda",
                    modifier=Modifier.size(20.dp))
            }
            IconButton(enabled=p.clips.isNotEmpty(),onClick={
                if (playing) { player.pause(); playing=false }
                else {
                    if (player.playbackState == Player.STATE_ENDED ||
                        projectPosition(player,p.clips,p.durationMs)>=p.durationMs) {
                        seekProjectPosition(player, p.clips, 0L)
                        onCursorChange(0L)
                    }
                    player.play(); playing=true
                }
            },modifier=Modifier.size(36.dp)) {
                Icon(if(playing) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                    "Reproduzir ou pausar vídeo",modifier=Modifier.size(27.dp))
            }
            IconButton(enabled=hasNext,onClick=onNext,modifier=Modifier.size(34.dp)) {
                Icon(Icons.Default.ChevronRight,"Próxima legenda",modifier=Modifier.size(23.dp))
            }
            IconButton(onClick=onToggleFollow,modifier=Modifier.size(34.dp)) {
                Icon(if(followPlayback) Icons.Default.Sync else Icons.Default.SyncDisabled,
                    if(followPlayback) "Desativar acompanhamento automático" else "Acompanhar legenda em reprodução",
                    tint=if(followPlayback) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier=Modifier.size(21.dp))
            }
            IconButton(onClick=onToggleCompact,modifier=Modifier.size(34.dp)) {
                Icon(if(compactPreview) Icons.Default.Fullscreen else Icons.Default.FullscreenExit,
                    if(compactPreview) "Ampliar prévia" else "Compactar prévia",
                    modifier=Modifier.size(23.dp))
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal=2.dp),
            verticalAlignment=Alignment.CenterVertically) {
            Text(formatPipMs(if(scrubbing) scrubMs else cursorMs),fontSize=10.sp,
                modifier=Modifier.width(55.dp),textAlign=TextAlign.Center)
            Slider(value=(if(scrubbing) scrubMs else cursorMs).toFloat()
                    .coerceIn(0f,total.toFloat()),
                onValueChange={value->
                    scrubbing=true
                    scrubMs=value.toLong().coerceIn(0L,p.durationMs)
                },onValueChangeFinished={
                    scrubbing=false
                    onSeekRequest(scrubMs)
                },valueRange=0f..total.toFloat(),
                enabled=p.clips.isNotEmpty(),modifier=Modifier.weight(1f))
            Text(formatPipMs(p.durationMs),fontSize=10.sp,
                modifier=Modifier.width(55.dp),textAlign=TextAlign.Center)
            Box {
                TextButton(onClick={zoomMenuExpanded=true},
                    contentPadding=PaddingValues(horizontal=2.dp,vertical=2.dp),
                    modifier=Modifier.width(55.dp).height(36.dp)) {
                    Text("${zoomFactor}×",fontSize=11.sp)
                    Icon(Icons.Default.ArrowDropDown,"Selecionar zoom da timeline",
                        modifier=Modifier.size(16.dp))
                }
                DropdownMenu(expanded=zoomMenuExpanded,
                    onDismissRequest={zoomMenuExpanded=false}) {
                    listOf(1,2,4).forEach { factor ->
                        DropdownMenuItem(text={Text("Zoom ${factor}×")},
                            onClick={onZoomChange(factor);zoomMenuExpanded=false},
                            leadingIcon={if(zoomFactor==factor) Icon(Icons.Default.Check,null)})
                    }
                }
            }
        }
    }
}

@Composable
internal fun SubtitleScreen(p:VideoProject,back:()->Unit,update:(VideoProject)->Boolean,
    initialSelectedId: String? = null, initialPositionMs: Long = 0L,
    onSaveEditor: () -> Unit, onAutoSubtitles: () -> Unit) {
    val context=LocalContext.current
    var selectedId by remember(p.id,initialSelectedId) {
        mutableStateOf(initialSelectedId?.takeIf { id -> p.subtitles.any { it.id==id } }
            ?: p.subtitles.firstOrNull()?.id.orEmpty())
    }
    // Mesmo com zero legendas, o cursor e o player existem e podem navegar o vídeo.
    var cursorMs by remember(p.id) { mutableLongStateOf(
        (p.subtitles.firstOrNull { it.id == initialSelectedId }?.startMs
            ?: initialPositionMs).coerceIn(0L, p.durationMs.coerceAtLeast(0L))) }
    var seekGeneration by remember(p.id) { mutableIntStateOf(0) }
    var seekTargetMs by remember(p.id) { mutableLongStateOf(cursorMs) }
    fun requestSeek(position: Long) {
        val target = position.coerceIn(0L, p.durationMs.coerceAtLeast(0L))
        cursorMs = target
        seekTargetMs = target
        seekGeneration++
    }
    LaunchedEffect(p.durationMs) {
        if (cursorMs > p.durationMs) requestSeek(p.durationMs)
    }
    var status by remember { mutableStateOf("") }
    var batchOpen by remember(p.id) { mutableStateOf(false) }
    var speakerStyleMenu by remember(p.id) { mutableStateOf(false) }
    var speakerStyleEditId by remember(p.id) { mutableStateOf<String?>(null) }
    var batchUndo by remember(p.id) { mutableStateOf<SubtitleBatchUndo?>(null) }
    var batchUndoSpeakerStyles by remember(p.id) { mutableStateOf<Map<String, SubtitleCue>?>(null) }
    var deleteAllConfirm by remember(p.id) { mutableStateOf(false) }
    var deletedSnapshot by remember(p.id) { mutableStateOf<List<SubtitleCue>?>(null) }
    var helpPosition by remember(p.id) { mutableStateOf(false) }
    var exportMenu by remember(p.id) { mutableStateOf(false) }
    var exportFormat by remember(p.id) { mutableStateOf(SubtitleFileExport.Format.SRT) }
    val scope = rememberCoroutineScope()
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri != null) scope.launch {
            try {
                val snapshot = p.subtitles.toList()
                val selectedFormat = exportFormat
                withContext(Dispatchers.IO) {
                    val content = SubtitleFileExport.render(snapshot, selectedFormat)
                    context.contentResolver.openOutputStream(uri, "wt")?.use { output ->
                        output.write(content.toByteArray(Charsets.UTF_8))
                        output.flush()
                    } ?: throw java.io.IOException("Destino de exportação indisponível")
                }
                status = "${snapshot.size} legendas exportadas (${selectedFormat.name})"
            } catch (e: Exception) {
                DiagnosticRecorder.exception("subtitle_export", e)
                status = "Falha ao exportar legendas: ${e.message?.take(100) ?: "erro de arquivo"}"
            }
        }
    }
    // Deve preceder o callback do importador, que limpa a busca após importar SRT.
    var searchQuery by remember(p.id) { mutableStateOf("") }
    val importer=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if(uri!=null) {
            val loaded=runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { reader ->
                    SubtitleListNavigation.readSrtBounded(reader)
                }.orEmpty()
            }
            loaded.onSuccess { content ->
                val cues=EditFeatures.parseSrt(content).filter{it.startMs<p.durationMs}
                    .map{it.copy(endMs=it.endMs.coerceAtMost(p.durationMs))}
                if(cues.isNotEmpty()) {
                    update(p.copy(subtitles=p.subtitles+cues))
                    selectedId=cues.first().id
                    searchQuery=""
                    status="${cues.size} legendas importadas"
                } else status="Nenhuma legenda SRT válida no arquivo"
            }.onFailure {DiagnosticRecorder.exception("srt_import", it);status="Não foi possível ler o arquivo: ${it.message}"}
        }
    }
    val sorted = remember(p.subtitles) {
        p.subtitles.sortedWith(compareBy<SubtitleCue> { it.startMs }.thenBy { it.id })
    }
    var batchUndoMessage by remember(p.id) { mutableStateOf("") }
    val canUndoDeleted = deletedSnapshot != null && p.subtitles.isEmpty()
    val canUndoBatch = batchUndo != null && batchUndo?.updated == p.subtitles
    val undoAvailable = canUndoDeleted || canUndoBatch
    val undoDescription = when {
        canUndoDeleted -> "Desfazer remoção de legendas"
        canUndoBatch -> "Desfazer última edição em lote"
        else -> "Desfazer"
    }
    fun undoLatestChange() {
        when {
            canUndoDeleted -> {
                val restore = deletedSnapshot
                if (restore != null && update(p.copy(subtitles = restore))) {
                    selectedId = restore.firstOrNull()?.id.orEmpty()
                    deletedSnapshot = null
                    status = "Legendas restauradas"
                }
            }
            canUndoBatch -> {
                val restored = batchUndo?.restoreIfCurrent(p.subtitles)
                if (restored != null && update(p.copy(subtitles = restored,
                    subtitleSpeakerStyles = batchUndoSpeakerStyles ?: p.subtitleSpeakerStyles))) {
                    batchUndo = null
                    batchUndoSpeakerStyles = null
                    status = "Estilos anteriores restaurados"
                }
            }
        }
    }
    var editorOpen by remember(p.id) { mutableStateOf(false) }
    val selected=p.subtitles.firstOrNull{it.id==selectedId}
    // A legenda em reprodução é diferente da escolhida para editar.
    val activeCue=SubtitlePreviewPolicy.firstAt(p.subtitles, cursorMs)
    var compactPreview by remember(p.id) { mutableStateOf(true) }
    var zoomFactor by remember(p.id) { mutableIntStateOf(1) }
    val zoomWindowMs = SubtitleCompactLayout.WINDOW_MS / zoomFactor
    var speakerFilter by remember(p.id) { mutableStateOf<String?>(null) }
    var speakerFilterMenu by remember(p.id) { mutableStateOf(false) }
    var autoSubtitlesMenu by remember(p.id) { mutableStateOf(false) }
    var speakerManagerOpen by remember(p.id) { mutableStateOf(false) }
    var speakerPickerCueId by remember(p.id) { mutableStateOf<String?>(null) }
    var moreSubtitleTools by remember(p.id) { mutableStateOf(false) }
    LaunchedEffect(p.subtitleSpeakers.map { it.id }) {
        if (speakerFilter != null && speakerFilter != "__unassigned__" &&
            p.subtitleSpeakers.none { it.id == speakerFilter }) speakerFilter = null
    }
    val selectedIndex=sorted.indexOfFirst { it.id==selectedId }
    var showAdvanced by remember(p.id) { mutableStateOf(false) }
    var searchOpen by remember(p.id) { mutableStateOf(false) }
    var pendingDeleteId by remember(p.id) { mutableStateOf<String?>(null) }
    val filtered=remember(sorted,searchQuery,speakerFilter,p.subtitleSpeakers) {
        SubtitleSpeakerPolicy.filter(sorted, speakerFilter, p.subtitleSpeakers)
            .filter { SubtitleListNavigation.matches(it.text,searchQuery) }
    }
    val originalNumbers=remember(sorted) { sorted.mapIndexed { index,cue -> cue.id to index+1 }.toMap() }
    val captionsState=rememberLazyListState()
    // Conteúdo alterado não é motivo para rolar: conserva o ID e o offset do LazyColumn.
    // Somente uma nova seleção/filtro/busca ou acompanhamento explicitamente ativado move a lista.
    var followPlayback by remember(p.id) { mutableStateOf(true) }
    LaunchedEffect(selectedId, searchQuery, speakerFilter) {
        val index=filtered.indexOfFirst { it.id==selectedId }
        if(index>=0) captionsState.animateScrollToItem(index)
        else if(searchQuery.isNotBlank() && filtered.isNotEmpty()) captionsState.scrollToItem(0)
    }
    LaunchedEffect(activeCue?.id, followPlayback) {
        if (followPlayback && !editorOpen && searchQuery.isBlank()) {
            val index = filtered.indexOfFirst { it.id == activeCue?.id }
            if (index >= 0) captionsState.animateScrollToItem(index)
        }
    }
    LaunchedEffect(p.subtitles.size) {
        if(selectedId.isNotEmpty() && p.subtitles.none { it.id==selectedId }) {
            selectedId=p.subtitles.firstOrNull()?.id.orEmpty()
            editorOpen=false
        }
    }
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal=14.dp)) {
        Heading("Legendas",back)
        SubtitlePositionPreview(p, selected,
            cursorMs = cursorMs, seekGeneration = seekGeneration, seekTargetMs = seekTargetMs,
            onCursorChange = { cursorMs = it.coerceIn(0L, p.durationMs) },
            onSeekRequest = ::requestSeek,
            onPositionCommit = { x, y ->
                selected?.let { chosen ->
                    followPlayback=false
                    update(SubtitleSpeakerStyles.manualEdit(p, chosen, chosen.copy(x=x, y=y)))
                }
            },
            onPrevious = {
                SubtitleListNavigation.adjacentIndex(selectedIndex, sorted.size, -1)?.let { index ->
                    selectedId = sorted[index].id
                    requestSeek(sorted[index].startMs)
                    editorOpen = true; followPlayback = false; showAdvanced = false; searchQuery = ""
                }
            },
            onNext = {
                SubtitleListNavigation.adjacentIndex(selectedIndex, sorted.size, 1)?.let { index ->
                    selectedId = sorted[index].id
                    requestSeek(sorted[index].startMs)
                    editorOpen = true; followPlayback = false; showAdvanced = false; searchQuery = ""
                }
            },
            hasPrevious = SubtitleListNavigation.adjacentIndex(selectedIndex, sorted.size, -1) != null,
            hasNext = SubtitleListNavigation.adjacentIndex(selectedIndex, sorted.size, 1) != null,
            compactPreview = compactPreview,
            followPlayback = followPlayback, onToggleFollow = { followPlayback = !followPlayback },
            onToggleCompact = { compactPreview = !compactPreview },
            onTimelineHelp = { helpPosition = true },
            zoomFactor = zoomFactor, onZoomChange = { zoomFactor = it })
        // A faixa mostra TODAS as legendas; filtro/busca só reduzem a lista.
        // Legendas fora do filtro ficam esmaecidas, nunca desaparecem da linha do tempo.
        SubtitleCompactTimeline(sorted, p.clips, p.durationMs, selectedId, cursorMs,
            onSelect = { id ->
                if (filtered.none { it.id == id }) {
                    speakerFilter = null
                    searchQuery = ""
                }
                selectedId = id
                followPlayback = false
                editorOpen = false
                p.subtitles.firstOrNull { it.id == id }?.let { requestSeek(it.startMs) }
            }, onSeek = ::requestSeek, windowMs = zoomWindowMs,
            speakers = p.subtitleSpeakers, activeId = activeCue?.id,
            emphasizedIds = filtered.map { it.id }.toSet())
        Row(Modifier.fillMaxWidth().padding(vertical=2.dp),
            verticalAlignment=Alignment.CenterVertically) {
            Text("Legendas (${p.subtitles.size})",fontSize=13.sp,maxLines=1,
                fontWeight=FontWeight.SemiBold,modifier=Modifier.weight(1f))
            Box {
                val filterLabel = when(speakerFilter) {
                    null -> "Todas"
                    "__unassigned__" -> "Sem personagem"
                    else -> p.subtitleSpeakers.firstOrNull { it.id==speakerFilter }?.name ?: "Todas"
                }
                TextButton(onClick={speakerFilterMenu=true},
                    contentPadding=PaddingValues(horizontal=2.dp,vertical=3.dp),
                    modifier=Modifier.widthIn(max=110.dp)) {
                    Text(filterLabel,fontSize=11.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
                    Icon(Icons.Default.ArrowDropDown,"Filtrar por personagem",modifier=Modifier.size(16.dp))
                }
                DropdownMenu(expanded=speakerFilterMenu,onDismissRequest={speakerFilterMenu=false}) {
                    DropdownMenuItem(text={Text("Todas as legendas")},onClick={
                        speakerFilter=null;speakerFilterMenu=false })
                    DropdownMenuItem(text={Text("Sem personagem")},onClick={
                        speakerFilter="__unassigned__";speakerFilterMenu=false })
                    p.subtitleSpeakers.forEach { speaker ->
                        DropdownMenuItem(text={Text(speaker.name,maxLines=1)},onClick={
                            speakerFilter=speaker.id;speakerFilterMenu=false })
                    }
                    HorizontalDivider()
                    DropdownMenuItem(text={Text("Gerenciar personagens")},onClick={
                        speakerFilterMenu=false;speakerManagerOpen=true },
                        leadingIcon={Icon(Icons.Default.Groups,null)})
                }
            }
            TextButton(onClick=onSaveEditor,modifier=Modifier.widthIn(min=49.dp),
                contentPadding=PaddingValues(horizontal=2.dp,vertical=4.dp)) {
                Icon(Icons.Default.Save,"Salvar legendas e voltar ao editor",modifier=Modifier.size(19.dp))
                Spacer(Modifier.width(2.dp))
                Text("Salvar",fontSize=11.sp)
            }
            IconButton(onClick={if(undoAvailable)undoLatestChange()},enabled=undoAvailable,
                modifier=Modifier.size(34.dp)) {
                Icon(Icons.Default.Undo,undoDescription,
                    tint=if(undoAvailable)MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier=Modifier.size(20.dp))
            }
            IconButton(onClick={searchOpen=!searchOpen;if(!searchOpen)searchQuery=""},
                modifier=Modifier.size(34.dp)) {
                Icon(if(searchOpen)Icons.Default.SearchOff else Icons.Default.Search,
                    if(searchOpen)"Fechar busca" else "Buscar legendas",modifier=Modifier.size(20.dp))
            }
            Box {
                IconButton(onClick={moreSubtitleTools=true},modifier=Modifier.size(34.dp)) {
                    Icon(Icons.Default.MoreVert,"Mais ferramentas de legendas",modifier=Modifier.size(21.dp))
                }
                DropdownMenu(expanded=moreSubtitleTools,onDismissRequest={moreSubtitleTools=false}) {
                    DropdownMenuItem(text={Text("Importar SRT")},onClick={
                        moreSubtitleTools=false
                        importer.launch(arrayOf("text/*","application/x-subrip","*/*"))
                    },leadingIcon={Icon(Icons.Default.FileOpen,null)})
                    DropdownMenuItem(text={Text("Exportar legendas")},enabled=p.subtitles.isNotEmpty(),
                        onClick={moreSubtitleTools=false;exportMenu=true},
                        leadingIcon={Icon(Icons.Default.FileUpload,null)})
                    DropdownMenuItem(text={Text("Editar todas")},enabled=p.subtitles.isNotEmpty(),
                        onClick={moreSubtitleTools=false;batchOpen=true;followPlayback=false},
                        leadingIcon={Icon(Icons.Default.Tune,null)})
                    DropdownMenuItem(text={Text("Estilos por personagem")},
                        enabled=p.subtitleSpeakers.isNotEmpty(),
                        onClick={moreSubtitleTools=false;speakerStyleMenu=true;followPlayback=false},
                        leadingIcon={Icon(Icons.Default.Palette,null)})
                    HorizontalDivider()
                    DropdownMenuItem(text={Text("Apagar todas")},enabled=p.subtitles.isNotEmpty(),
                        onClick={moreSubtitleTools=false;deleteAllConfirm=true},
                        leadingIcon={Icon(Icons.Default.DeleteSweep,null,
                            tint=MaterialTheme.colorScheme.error)})
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(vertical=1.dp),
            horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            SubtitleToolButton(Icons.Default.Add,"Nova",p.durationMs>0,
                Modifier.weight(1f)) {
                SubtitleListNavigation.newInterval(p.durationMs,cursorMs)?.let { (start,end) ->
                    val cue=SubtitleCue(text="Nova legenda",startMs=start,endMs=end)
                    if(update(p.copy(subtitles=p.subtitles+cue))) {
                        batchUndo=null;deletedSnapshot=null
                        searchQuery="";speakerFilter=null;selectedId=cue.id
                        editorOpen=true;showAdvanced=false
                        requestSeek(cue.startMs)
                    }
                }
            }
            Box(Modifier.weight(1f)) {
                OutlinedButton(onClick={autoSubtitlesMenu=true},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp),
                    contentPadding=PaddingValues(horizontal=4.dp,vertical=6.dp)) {
                    Icon(Icons.Default.AutoAwesome,"Legendas automáticas",modifier=Modifier.size(17.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Automáticas",fontSize=12.sp,maxLines=1)
                    Icon(Icons.Default.ArrowDropDown,null,modifier=Modifier.size(16.dp))
                }
                DropdownMenu(expanded=autoSubtitlesMenu,onDismissRequest={autoSubtitlesMenu=false}) {
                    DropdownMenuItem(text={Text("Offline · Vosk")},onClick={
                        autoSubtitlesMenu=false;onAutoSubtitles()
                    },leadingIcon={Icon(Icons.Default.RecordVoiceOver,null)})
                    DropdownMenuItem(text={Text("Gemini IA")},onClick={
                        autoSubtitlesMenu=false;onAutoSubtitles()
                    },leadingIcon={Icon(Icons.Default.AutoAwesome,null)})
                }
            }
        }
        if(searchOpen) {
            OutlinedTextField(value=searchQuery,onValueChange={searchQuery=it.take(100)},
                label={Text("Buscar texto da legenda")},singleLine=true,
                modifier=Modifier.fillMaxWidth(),
                trailingIcon={if(searchQuery.isNotBlank()) IconButton(onClick={searchQuery=""}) {
                    Icon(Icons.Default.Clear,"Limpar busca")
                }})
            Text("${filtered.size} de ${sorted.size} legendas",fontSize=11.sp,
                color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if(status.isNotBlank()) Text(status,fontSize=12.sp,
            color=MaterialTheme.colorScheme.primary)
        LazyColumn(state=captionsState, modifier=Modifier.fillMaxWidth().weight(1f)
            .pointerInput(p.id) {
                awaitPointerEventScope {
                    while (true) {
                        if (awaitPointerEvent(PointerEventPass.Initial).type == PointerEventType.Press) {
                            followPlayback=false
                        }
                    }
                }
            },
            verticalArrangement=Arrangement.spacedBy(5.dp),
            contentPadding=PaddingValues(bottom=24.dp)) {
            if(filtered.isEmpty()) item {
                Text(if(sorted.isEmpty()) "Nenhuma legenda. Toque em Nova ou importe um SRT."
                    else "Nenhuma legenda corresponde à busca.",
                    color=MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier=Modifier.padding(vertical=20.dp))
            }
            itemsIndexed(filtered,key={_,cue->cue.id}) { _,cue ->
                val number=originalNumbers[cue.id] ?: 0
                Column {
                    Surface(onClick={
                        val same=selectedId==cue.id
                        followPlayback=false
                        selectedId=cue.id
                        followPlayback=false
                        requestSeek(cue.startMs)
                        editorOpen=if(same)!editorOpen else true
                        showAdvanced=false
                    },shape=RoundedCornerShape(8.dp),
                        color=if(selectedId==cue.id)MaterialTheme.colorScheme.primaryContainer
                            else if(activeCue?.id == cue.id) MaterialTheme.colorScheme.tertiaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant) {
                        Row(Modifier.fillMaxWidth().heightIn(min=44.dp).padding(horizontal=10.dp),
                            verticalAlignment=Alignment.CenterVertically) {
                            Text("$number",fontSize=12.sp,
                                color=MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier=Modifier.width(26.dp))
                            val speaker = p.subtitleSpeakers.firstOrNull { it.id == cue.speakerId }
                            Box(Modifier.size(10.dp).background(
                                Color(speaker?.color ?: 0xFF85858C.toInt()), RoundedCornerShape(3.dp)))
                            Spacer(Modifier.width(5.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(cue.text, fontSize=13.sp,
                                    maxLines=1, overflow=TextOverflow.Ellipsis)
                                if (speaker != null) Text(speaker.name, fontSize=10.sp,
                                    color=MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines=1, overflow=TextOverflow.Ellipsis)
                            }
                            Spacer(Modifier.width(6.dp))
                            Text(formatPipMs(cue.startMs),fontSize=11.sp,
                                color=MaterialTheme.colorScheme.onSurfaceVariant)
                            IconButton(onClick = { speakerPickerCueId = cue.id },
                                modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.PersonAdd, "Atribuir personagem à legenda",
                                    modifier = Modifier.size(18.dp))
                            }
                            Icon(if(selectedId==cue.id && editorOpen)Icons.Default.ExpandLess
                                else Icons.Default.Edit,contentDescription="Editar legenda",
                                modifier=Modifier.padding(start=8.dp).size(18.dp))
                        }
                    }
                }
            }
        }
    }
    speakerPickerCueId?.let { cueId ->
        SubtitleSpeakerPicker(p, cueId, update,
            dismiss = { speakerPickerCueId = null },
            manage = { speakerManagerOpen = true })
    }
    if(speakerManagerOpen) SubtitleSpeakerManager(p, update,
        dismiss = { speakerManagerOpen = false })
    if(speakerStyleMenu) AlertDialog(onDismissRequest={speakerStyleMenu=false},
        title={Text("Estilos por personagem")},
        text={Column(Modifier.heightIn(max=320.dp).verticalScroll(rememberScrollState())) {
            Text("Escolha um personagem para aplicar aparência e animação às suas falas. " +
                "Exceções individuais são preservadas.",fontSize=12.sp)
            p.subtitleSpeakers.forEach { speaker ->
                val count=p.subtitles.count { it.speakerId == speaker.id }
                TextButton(onClick={speakerStyleEditId=speaker.id;speakerStyleMenu=false},
                    modifier=Modifier.fillMaxWidth()) {
                    Box(Modifier.size(12.dp).background(Color(speaker.color),RoundedCornerShape(3.dp)))
                    Spacer(Modifier.width(8.dp))
                    Text(speaker.name,modifier=Modifier.weight(1f),maxLines=1)
                    Text("$count falas",fontSize=11.sp)
                    Icon(Icons.Default.ChevronRight,null)
                }
            }
        }},
        confirmButton={TextButton(onClick={speakerStyleMenu=false}) {Text("Fechar")}})
    speakerStyleEditId?.let { id ->
        val speaker=p.subtitleSpeakers.firstOrNull { it.id == id }
        if (speaker != null) {
            val source=p.subtitleSpeakerStyles[id]
                ?: p.subtitles.firstOrNull { it.speakerId == id }
                ?: SubtitleCue(text="Exemplo",startMs=0,endMs=3000)
            val affected=p.subtitles.count { it.speakerId == id && it.id !in p.subtitleStyleOverrides }
            SubtitleBatchEditDialog(source=source,count=affected,
                title="Estilo · ${speaker.name}", scopeLabel="falas de ${speaker.name}",
                onDismiss={speakerStyleEditId=null},
                onApply={draft,options ->
                    val changed=SubtitleSpeakerStyles.apply(p,id,draft,options)
                    if(changed != null && update(changed)) {
                        batchUndo=SubtitleBatchUndo(p.subtitles,changed.subtitles)
                        batchUndoSpeakerStyles=p.subtitleSpeakerStyles
                        speakerStyleEditId=null
                        status="Estilo salvo para ${speaker.name} · $affected falas atualizadas"
                    }
                })
        } else LaunchedEffect(id) { speakerStyleEditId=null }
    }
    if(editorOpen && selected!=null) {
        SubtitleEditDialog(cue=selected,totalMs=p.durationMs,
            number=originalNumbers[selected.id] ?: 1,onDismiss={editorOpen=false},
            onDelete={editorOpen=false;pendingDeleteId=selected.id},
            onRestoreSpeakerStyle=if(selected.speakerId?.let(p.subtitleSpeakerStyles::containsKey) == true)
                ({
                    SubtitleSpeakerStyles.reset(p,selected.id)?.let { restored ->
                        if(update(restored)) { editorOpen=false;followPlayback=false }
                    }
                }) else null,
            onSave={draft,copyOptions ->
                val single=SubtitleSpeakerStyles.manualEdit(p, selected, draft)
                val updated=single.subtitles
                val all=if(copyOptions!=null)
                    SubtitleStyles.applySelectedToAll(updated,draft,copyOptions) else updated
                if(update(single.copy(subtitles=all))) {
                    batchUndo=null
                    batchUndoSpeakerStyles=null
                    followPlayback=false
                    editorOpen=false
                }
            })
    }
    if(batchOpen && p.subtitles.isNotEmpty()) {
        SubtitleBatchEditDialog(
            source=selected ?: p.subtitles.first(), count=p.subtitles.size,
            onDismiss={batchOpen=false},
            onApply={draft,copyOptions ->
                val updated=p.subtitles
                val all=SubtitleStyles.applySelectedToAll(updated,draft,copyOptions)
                if(update(p.copy(subtitles=all))) {
                    batchUndo=SubtitleBatchUndo(updated,all)
                    batchUndoSpeakerStyles=null
                    followPlayback=false
                    batchUndoMessage=copyOptions.summary(all.size)
                    batchOpen=false
                    status=batchUndoMessage
                }
            })
    }
    if(helpPosition) AlertDialog(onDismissRequest={helpPosition=false},
        title={Text("Posicionar legendas")},
        text={Text("Toque em uma legenda na linha do tempo e arraste o texto " +
            "diretamente sobre a prévia para mudar sua posição. " +
            "Use Editar todas para aplicar a mesma posição às demais.")},
        confirmButton={TextButton(onClick={helpPosition=false}) {Text("Entendi")}})
    if(exportMenu) AlertDialog(onDismissRequest={exportMenu=false},
        title={Text("Exportar legendas")},
        text={Text("Escolha SRT (com horários) ou TXT (somente o texto). " +
            "Animações e cores não são incluídas nesses formatos.")},
        confirmButton={TextButton(onClick={
            exportMenu=false
            exportFormat=SubtitleFileExport.Format.SRT
            exporter.launch("UaiCortes-legendas.srt")
        }) { Text("SRT") }},
        dismissButton={Row {
            TextButton(onClick={
                exportMenu=false
                exportFormat=SubtitleFileExport.Format.TXT
                exporter.launch("UaiCortes-legendas.txt")
            }) { Text("TXT") }
            TextButton(onClick={exportMenu=false}) { Text("Cancelar") }
        }})
    if(deleteAllConfirm) AlertDialog(onDismissRequest={deleteAllConfirm=false},
        title={Text("Apagar todas as legendas?")},
        text={Text("${p.subtitles.size} legendas serão removidas do projeto. " +
            "O vídeo original e os arquivos externos não serão apagados. " +
            "É possível desfazer até a próxima edição.")},
        confirmButton={TextButton(onClick={
            val snapshot=p.subtitles.toList()
            if(update(p.copy(subtitles=emptyList()))) {
                deletedSnapshot=snapshot;batchUndo=null
                selectedId="";editorOpen=false;showAdvanced=false
                deleteAllConfirm=false;status="${snapshot.size} legendas removidas"
            }
        }) { Text("Apagar todas",color=MaterialTheme.colorScheme.error) }},
        dismissButton={TextButton(onClick={deleteAllConfirm=false}) { Text("Cancelar") }})
    pendingDeleteId?.let { id ->
        val toDelete=p.subtitles.firstOrNull { it.id==id }
        if(toDelete!=null) AlertDialog(onDismissRequest={pendingDeleteId=null},
            title={Text("Excluir esta legenda?")},
            text={Text("${toDelete.text.take(120)}\nEsta ação remove apenas a legenda, não o vídeo original.")},
            confirmButton={TextButton(onClick={
                if(update(p.copy(subtitles=p.subtitles.filterNot { it.id==id }))) {
                batchUndo=null
                if(selectedId==id) {
                    val remaining=sorted.filterNot { it.id==id }
                    selectedId=remaining.getOrNull(selectedIndex.coerceAtMost(remaining.lastIndex))?.id.orEmpty()
                    editorOpen=false;showAdvanced=false
                }
                pendingDeleteId=null
                }
            }) {Text("Excluir")}},
            dismissButton={TextButton(onClick={pendingDeleteId=null}) {Text("Cancelar")}})
        else LaunchedEffect(id) { pendingDeleteId=null }
    }
}

/** Editor individual: tempos/texto independentes e controles visuais organizados em abas. */
@Composable
internal fun SubtitleEditDialog(cue: SubtitleCue,totalMs: Long,number: Int,
    onDismiss: () -> Unit,onDelete: () -> Unit,
    onRestoreSpeakerStyle: (() -> Unit)? = null,
    onSave: (SubtitleCue,SubtitleStyles.CopyOptions?) -> Unit) {
    var draft by remember(cue.id) { mutableStateOf(cue) }
    var wordTimingOpen by remember(cue.id) { mutableStateOf(false) }
    val valid=draft.text.isNotBlank() && draft.startMs>=0L &&
        draft.endMs>draft.startMs && draft.endMs<=totalMs
    Dialog(onDismissRequest=onDismiss,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Surface(Modifier.fillMaxWidth(.96f)
            .heightIn(max=LocalConfiguration.current.screenHeightDp.dp*.86f),
            shape=UaiShapes.card,tonalElevation=5.dp) {
            Column(Modifier.fillMaxWidth().padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Text("Editar legenda $number",fontWeight=FontWeight.Bold,
                        style=MaterialTheme.typography.titleMedium,modifier=Modifier.weight(1f))
                    IconButton(onClick=onDelete) {
                        Icon(Icons.Default.Delete,"Excluir legenda",tint=MaterialTheme.colorScheme.error)
                    }
                    IconButton(onClick=onDismiss) { Icon(Icons.Default.Close,"Fechar") }
                }
                SubtitleVisualPreview(draft)
                Column(Modifier.weight(1f,fill=false).verticalScroll(rememberScrollState()),
                    verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value=draft.text,
                        onValueChange={
                            val updatedText=it.take(500)
                            draft=if(updatedText==draft.text) draft else draft.copy(
                                text=updatedText,wordTimings=emptyList(),wordColors=emptyList())
                        },
                        label={Text("Texto")},maxLines=3,modifier=Modifier.fillMaxWidth())
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp),modifier=Modifier.fillMaxWidth()) {
                        TimeDigitsField("Início",draft.startMs,totalMs,
                            valid={it>=0L && it<draft.endMs},
                            onCommit={draft=draft.copy(startMs=it,wordTimings=emptyList())},
                            modifier=Modifier.weight(1f),compact=true)
                        TimeDigitsField("Fim",draft.endMs,totalMs,
                            valid={it>draft.startMs && it<=totalMs},
                            onCommit={draft=draft.copy(endMs=it,wordTimings=emptyList())},
                            modifier=Modifier.weight(1f),compact=true)
                    }
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Karaokê por palavra",fontWeight=FontWeight.Medium,fontSize=13.sp)
                            Text(if(draft.wordTimings.isEmpty()) "Sem horários individuais"
                                else "${draft.wordTimings.size} palavras com horários salvos",
                                fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        OutlinedButton(onClick={wordTimingOpen=true},
                            enabled=SubtitleWordTimingEditorPolicy.editable(draft),
                            contentPadding=PaddingValues(horizontal=9.dp)) {
                            Text("Sincronizar",fontSize=12.sp)
                        }
                    }
                    SubtitleVisualControls(draft=draft,onChange={draft=it})
                    if (onRestoreSpeakerStyle != null) TextButton(
                        onClick=onRestoreSpeakerStyle,modifier=Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Restore,null,modifier=Modifier.size(16.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Restaurar estilo do personagem")
                    }
                }
                if(!valid) Text("Informe texto e horários válidos.",fontSize=11.sp,
                    color=MaterialTheme.colorScheme.error)
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick=onDismiss,modifier=Modifier.weight(1f)) {Text("Cancelar")}
                    Button(onClick={onSave(draft,null)},enabled=valid,
                        modifier=Modifier.weight(1f)) {Text("Salvar legenda")}
                }
            }
        }
    }
    if(wordTimingOpen) SubtitleWordTimingEditor(draft,
        onDismiss={wordTimingOpen=false},
        onSave={times ->
            draft=draft.copy(wordTimings=times,animation=SubtitleAnimations.KARAOKE)
            wordTimingOpen=false
        })
}

/** Botões compactos e consistentes em celulares estreitos, sem formato de cápsula. */
@Composable
private fun SubtitleToolButton(icon: ImageVector, label: String, enabled: Boolean,
    modifier: Modifier = Modifier, onClick: () -> Unit) {
    OutlinedButton(onClick=onClick,enabled=enabled,modifier=modifier.heightIn(min=46.dp),
        shape=RoundedCornerShape(8.dp),contentPadding=PaddingValues(horizontal=3.dp,vertical=5.dp)) {
        Icon(icon,label,modifier=Modifier.size(16.dp))
        Spacer(Modifier.width(3.dp))
        Text(label,fontSize=11.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
    }
}
