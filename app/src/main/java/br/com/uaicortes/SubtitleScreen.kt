package br.com.uaicortes

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.ui.input.pointer.pointerInput
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
    content: @Composable BoxScope.() -> Unit) {
    val screenHeight = LocalConfiguration.current.screenHeightDp.toFloat()
    BoxWithConstraints(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val fit = AdaptiveVideoLayout.inline(maxWidth.value, screenHeight, aspect)
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
        }, if (cue.bold) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
    }
    val longest = cue.text.split(Regex("\\s+")).maxOfOrNull { paint.measureText(it) } ?: 0f
    if (longest <= 0f) return 1f
    val available = (widthPx * .90f - textSizePx * .65f).coerceAtLeast(1f)
    return (available / longest).coerceIn(.35f, 1f)
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
                        fontFamily=family,fontWeight=textWeight,textAlign=alignment,
                        style=TextStyle(lineHeight=textLineHeight,
                            drawStyle=Stroke(width=outlineWidthPx*2f,join=StrokeJoin.Round)))
                }
                val animatedText=buildAnnotatedString {
                    append(animationFrame.text)
                    animationFrame.highlighted?.let { range ->
                        if(range.first>=0 && range.last<animationFrame.text.length)
                            addStyle(SpanStyle(color=Color(0xFFFFFF00)),range.first,range.last+1)
                    }
                }
                Text(animatedText,color=Color(cue.color),fontSize=previewFontSp.sp,
                    fontFamily=family,fontWeight=textWeight,textAlign=alignment,
                    style=fillStyle)
            }
        }
    }
}

/** Prévia fixa e reproduzível: seleciona o quadro da legenda usando os
 * cortes reais do projeto. O texto pode ser arrastado sem tocar no MP4 original.
 * A reprodução para no fim da legenda selecionada; não substitui a exportação.
 */
@Composable
internal fun SubtitlePositionPreview(p:VideoProject,cue:SubtitleCue,
    onPositionCommit:(Float,Float)->Unit,
    onPrevious:()->Unit, onNext:()->Unit,
    hasPrevious:Boolean, hasNext:Boolean) {
    val context=LocalContext.current
    val player=remember(p.id) { diagnosticPlayer(context) }
    var playing by remember(p.id) { mutableStateOf(false) }
    var previewMs by remember(p.id) { mutableLongStateOf(cue.startMs) }
    DisposableEffect(player) { onDispose { player.release() } }
    LaunchedEffect(player,p.clips.map { Triple(it.uri,it.startMs,it.endMs) }) {
        playing=false
        player.pause()
        player.setMediaItems(p.clips.map { clip ->
            MediaItem.Builder().setUri(clip.uri).setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder().setStartPositionMs(clip.startMs)
                    .setEndPositionMs(clip.endMs).build()).build()
        })
        player.prepare()
    }
    LaunchedEffect(cue.id,cue.startMs,p.clips) {
        previewMs=cue.startMs
        playing=false
        player.pause()
        seekProjectPosition(player,p.clips,cue.startMs)
    }
    LaunchedEffect(playing,cue.id,cue.endMs) {
        while(playing) {
            previewMs=projectPosition(player,p.clips,p.durationMs)
            if(player.playbackState==Player.STATE_ENDED ||
                projectPosition(player,p.clips,p.durationMs)>=cue.endMs) {
                player.pause()
                playing=false
            }
            delay(120)
        }
    }
    val source=remember(p.clips,cue.startMs) {
        TimelinePrecision.sourceAt(p.clips.map { it.uri to it.durationMs },cue.startMs)
    }
    val clip=p.clips.getOrNull(source?.first ?: -1)
    val aspect=remember(clip?.uri) {
        clip?.let { mediaAspect(context,it.uri,false) } ?: 16f/9f
    }
    Column {
        InlineVideoPreview(aspect) {
            AndroidView(factory={ctx->previewPlayer(ctx).apply {
                this.player=player
                useController=false
            }},update={it.player=player},modifier=Modifier.fillMaxSize())
            SubtitleOverlayOnFrame(cue,editable=true,playbackMs=previewMs,
                onPositionCommit=onPositionCommit)
        }
        Row(Modifier.fillMaxWidth().height(38.dp),verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.Center) {
            IconButton(enabled=hasPrevious,onClick=onPrevious,modifier=Modifier.size(36.dp)) {
                Icon(Icons.Default.ChevronLeft,"Legenda anterior",modifier=Modifier.size(25.dp))
            }
            IconButton(onClick={
                player.pause();playing=false
                previewMs=cue.startMs
                seekProjectPosition(player,p.clips,cue.startMs)
            },modifier=Modifier.size(34.dp)) {
                Icon(Icons.Default.SkipPrevious,"Reiniciar legenda",modifier=Modifier.size(22.dp))
            }
            IconButton(enabled=p.clips.isNotEmpty(),onClick={
                if(playing) {player.pause();playing=false}
                else {
                    if(projectPosition(player,p.clips,p.durationMs)>=cue.endMs)
                        seekProjectPosition(player,p.clips,cue.startMs)
                    player.play();playing=true
                }
            },modifier=Modifier.size(36.dp)) {
                Icon(if(playing)Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                    "Reproduzir ou pausar prévia",modifier=Modifier.size(28.dp))
            }
            IconButton(enabled=hasNext,onClick=onNext,modifier=Modifier.size(36.dp)) {
                Icon(Icons.Default.ChevronRight,"Próxima legenda",modifier=Modifier.size(25.dp))
            }
            Text("${formatPipMs(cue.startMs)}–${formatPipMs(cue.endMs)}",
                fontSize=11.sp,maxLines=1,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun SubtitleScreen(p:VideoProject,back:()->Unit,update:(VideoProject)->Unit,
    initialSelectedId: String? = null, onSaveHome: () -> Unit, onAutoSubtitles: () -> Unit) {
    val context=LocalContext.current
    var selectedId by remember(p.id,initialSelectedId) {
        mutableStateOf(initialSelectedId?.takeIf { id -> p.subtitles.any { it.id==id } }
            ?: p.subtitles.firstOrNull()?.id.orEmpty())
    }
    var status by remember { mutableStateOf("") }
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
    val selected=p.subtitles.firstOrNull{it.id==selectedId}
    val selectedIndex=sorted.indexOfFirst { it.id==selectedId }
    var editorOpen by remember(p.id) { mutableStateOf(false) }
    var showAdvanced by remember(p.id) { mutableStateOf(false) }
    var searchOpen by remember(p.id) { mutableStateOf(false) }
    var pendingDeleteId by remember(p.id) { mutableStateOf<String?>(null) }
    val filtered=remember(sorted,searchQuery) {
        sorted.filter { SubtitleListNavigation.matches(it.text,searchQuery) }
    }
    val originalNumbers=remember(sorted) { sorted.mapIndexed { index,cue -> cue.id to index+1 }.toMap() }
    val captionsState=rememberLazyListState()
    LaunchedEffect(selectedId,searchQuery,filtered) {
        val index=filtered.indexOfFirst { it.id==selectedId }
        if(index>=0) captionsState.animateScrollToItem(index)
        else if(searchQuery.isNotBlank() && filtered.isNotEmpty()) captionsState.scrollToItem(0)
    }
    LaunchedEffect(p.subtitles.size) {
        if(selectedId.isNotEmpty() && p.subtitles.none { it.id==selectedId }) {
            selectedId=p.subtitles.firstOrNull()?.id.orEmpty()
            editorOpen=false
        }
    }
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal=14.dp)) {
        Heading("Legendas",back)
        if(selected!=null) {
            SubtitlePositionPreview(p,selected,
                onPositionCommit={ x,y ->
                    update(p.copy(subtitles=p.subtitles.map { cue ->
                        if(cue.id==selected.id) cue.copy(x=x,y=y) else cue
                    }))
                },
                onPrevious={
                    SubtitleListNavigation.adjacentIndex(selectedIndex,sorted.size,-1)?.let { index ->
                        selectedId=sorted[index].id;editorOpen=true;showAdvanced=false;searchQuery=""
                    }
                },
                onNext={
                    SubtitleListNavigation.adjacentIndex(selectedIndex,sorted.size,1)?.let { index ->
                        selectedId=sorted[index].id;editorOpen=true;showAdvanced=false;searchQuery=""
                    }
                },
                hasPrevious=SubtitleListNavigation.adjacentIndex(selectedIndex,sorted.size,-1)!=null,
                hasNext=SubtitleListNavigation.adjacentIndex(selectedIndex,sorted.size,1)!=null)
        } else Box(Modifier.fillMaxWidth().height(196.dp)
            .background(Color(0xFF101014),RoundedCornerShape(12.dp)),
            contentAlignment=Alignment.Center) {
            Text("Adicione uma legenda para visualizar o quadro",color=Color.White,fontSize=12.sp)
        }
        Text("Arraste a legenda sobre a prévia para posicionar",fontSize=11.sp,
            color=MaterialTheme.colorScheme.onSurfaceVariant,
            modifier=Modifier.padding(top=4.dp,bottom=4.dp))
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,
            verticalAlignment=Alignment.CenterVertically) {
            Text("Linha do tempo",fontSize=13.sp,fontWeight=FontWeight.SemiBold)
            Text(if(selected==null)"Sem seleção" else
                "${formatPipMs(selected.startMs)} – ${formatPipMs(selected.endMs)}",
                fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        SubtitleCompactTimeline(sorted,p.durationMs,selectedId,
            onSelect={ id -> selectedId=id;editorOpen=false })
        Row(Modifier.fillMaxWidth().padding(vertical=2.dp),
            verticalAlignment=Alignment.CenterVertically) {
            Text("Legendas (${p.subtitles.size})",fontSize=14.sp,maxLines=1,
                fontWeight=FontWeight.SemiBold,modifier=Modifier.weight(1f))
            IconButton(onClick={searchOpen=!searchOpen; if(!searchOpen) searchQuery=""},
                modifier=Modifier.size(30.dp)) {
                Icon(if(searchOpen) Icons.Default.SearchOff else Icons.Default.Search,
                    if(searchOpen) "Fechar busca" else "Buscar legendas",modifier=Modifier.size(19.dp))
            }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
            .padding(vertical=2.dp),
            verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            TextButton(enabled=p.durationMs>0,onClick={
                SubtitleListNavigation.newInterval(p.durationMs,selected?.endMs)?.let { (start,end) ->
                    val cue=SubtitleCue(text="Nova legenda",startMs=start,endMs=end)
                    update(p.copy(subtitles=p.subtitles+cue))
                    searchQuery="";selectedId=cue.id;editorOpen=true;showAdvanced=false
                }
            },contentPadding=PaddingValues(horizontal=1.dp,vertical=2.dp)) {
                Icon(Icons.Default.Add,"Nova legenda",Modifier.size(18.dp))
                Text("Nova",fontSize=11.sp,maxLines=1)
            }
            OutlinedButton(onClick=onAutoSubtitles,
                contentPadding=PaddingValues(horizontal=3.dp,vertical=3.dp)) {
                Icon(Icons.Default.AutoAwesome, "Gerar legendas automáticas", Modifier.size(17.dp))
                Text("Auto",fontSize=11.sp,maxLines=1)
            }
            OutlinedButton(onClick={importer.launch(arrayOf("text/*","application/x-subrip","*/*"))},
                contentPadding=PaddingValues(horizontal=4.dp,vertical=3.dp)) {
                Icon(Icons.Default.FileOpen,"Importar SRT",Modifier.size(17.dp))
                Text("SRT",fontSize=11.sp,maxLines=1)
            }
            OutlinedButton(onClick=onSaveHome,
                contentPadding=PaddingValues(horizontal=4.dp,vertical=3.dp)) {
                Icon(Icons.Default.Save,"Salvar projeto e voltar à tela inicial",Modifier.size(17.dp))
                Text("Salvar",fontSize=11.sp,maxLines=1)
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
        LazyColumn(state=captionsState, modifier=Modifier.fillMaxWidth().weight(1f),
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
                        selectedId=cue.id
                        editorOpen=if(same)!editorOpen else true
                        showAdvanced=false
                    },shape=RoundedCornerShape(8.dp),
                        color=if(selectedId==cue.id)MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant) {
                        Row(Modifier.fillMaxWidth().heightIn(min=44.dp).padding(horizontal=10.dp),
                            verticalAlignment=Alignment.CenterVertically) {
                            Text("$number",fontSize=12.sp,
                                color=MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier=Modifier.width(26.dp))
                            Text(cue.text,modifier=Modifier.weight(1f),fontSize=13.sp,
                                maxLines=1,overflow=TextOverflow.Ellipsis)
                            Spacer(Modifier.width(6.dp))
                            Text(formatPipMs(cue.startMs),fontSize=11.sp,
                                color=MaterialTheme.colorScheme.onSurfaceVariant)
                            Icon(if(selectedId==cue.id && editorOpen)Icons.Default.ExpandLess
                                else Icons.Default.Edit,contentDescription="Editar legenda",
                                modifier=Modifier.padding(start=8.dp).size(18.dp))
                        }
                    }
                }
            }
        }
    }
    if(editorOpen && selected!=null) {
        SubtitleEditDialog(cue=selected,totalMs=p.durationMs,
            number=originalNumbers[selected.id] ?: 1,onDismiss={editorOpen=false},
            onDelete={editorOpen=false;pendingDeleteId=selected.id},
            onSave={draft,copyOptions ->
                val updated=p.subtitles.map { cue -> if(cue.id==draft.id) draft else cue }
                val all=if(copyOptions!=null)
                    SubtitleStyles.applySelectedToAll(updated,draft,copyOptions) else updated
                update(p.copy(subtitles=all))
                editorOpen=false
            })
    }
    pendingDeleteId?.let { id ->
        val toDelete=p.subtitles.firstOrNull { it.id==id }
        if(toDelete!=null) AlertDialog(onDismissRequest={pendingDeleteId=null},
            title={Text("Excluir esta legenda?")},
            text={Text("${toDelete.text.take(120)}\nEsta ação remove apenas a legenda, não o vídeo original.")},
            confirmButton={TextButton(onClick={
                update(p.copy(subtitles=p.subtitles.filterNot { it.id==id }))
                if(selectedId==id) {
                    val remaining=sorted.filterNot { it.id==id }
                    selectedId=remaining.getOrNull(selectedIndex.coerceAtMost(remaining.lastIndex))?.id.orEmpty()
                    editorOpen=false;showAdvanced=false
                }
                pendingDeleteId=null
            }) {Text("Excluir")}},
            dismissButton={TextButton(onClick={pendingDeleteId=null}) {Text("Cancelar")}})
        else LaunchedEffect(id) { pendingDeleteId=null }
    }
}

/** Editor modal compacto. Alterações locais só são persistidas em Salvar. */
@Composable
internal fun SubtitleEditDialog(cue: SubtitleCue,totalMs: Long,number: Int,
    onDismiss: () -> Unit,onDelete: () -> Unit,
    onSave: (SubtitleCue,SubtitleStyles.CopyOptions?) -> Unit) {
    var draft by remember(cue.id) { mutableStateOf(cue) }
    var stylesOpen by remember(cue.id) { mutableStateOf(false) }
    var animationOpen by remember(cue.id) { mutableStateOf(false) }
    var applyAll by remember(cue.id) { mutableStateOf(false) }
    var copyAppearance by remember(cue.id) { mutableStateOf(true) }
    var copyPosition by remember(cue.id) { mutableStateOf(false) }
    var copyAnimation by remember(cue.id) { mutableStateOf(true) }
    var confirmAll by remember(cue.id) { mutableStateOf(false) }
    var sampleMs by remember(cue.id) { mutableLongStateOf(0L) }
    LaunchedEffect(draft.animation,draft.animationSpeed,draft.text) {
        sampleMs=0L
        while(true) {
            delay(80L)
            sampleMs=(sampleMs+80L)%3000L
        }
    }
    val valid=draft.text.isNotBlank() && draft.startMs>=0L &&
        draft.endMs>draft.startMs && draft.endMs<=totalMs
    fun submit() {
        if(!valid) return
        val options=if(applyAll) SubtitleStyles.CopyOptions(
            appearance=copyAppearance,position=copyPosition,animation=copyAnimation) else null
        onSave(draft,options)
    }
    Dialog(onDismissRequest=onDismiss,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        val screenHeight=LocalConfiguration.current.screenHeightDp.dp
        Surface(Modifier.fillMaxWidth(.96f).heightIn(max=screenHeight*.88f),
            shape=RoundedCornerShape(18.dp),tonalElevation=5.dp) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(14.dp),verticalArrangement=Arrangement.spacedBy(9.dp)) {
                Row(verticalAlignment=Alignment.CenterVertically,modifier=Modifier.fillMaxWidth()) {
                    Text("Editar legenda $number",fontWeight=FontWeight.Bold,
                        style=MaterialTheme.typography.titleMedium,modifier=Modifier.weight(1f))
                    IconButton(onClick=onDismiss,modifier=Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close,"Fechar")
                    }
                }
                // Simulação do texto, sem segunda miniatura de vídeo.
                val sample=SubtitleAnimations.frame(draft.copy(startMs=0L,endMs=3000L),sampleMs)
                Box(Modifier.fillMaxWidth().heightIn(min=76.dp)
                    .background(Color(0xFF13151A),RoundedCornerShape(10.dp))
                    .padding(12.dp),contentAlignment=Alignment.Center) {
                    val previewText=buildAnnotatedString {
                        append(sample.text)
                        sample.highlighted?.let { range ->
                            if(range.first>=0 && range.last<sample.text.length)
                                addStyle(SpanStyle(color=Color.Yellow),range.first,range.last+1)
                        }
                    }
                    Box(Modifier.graphicsLayer {
                        alpha=sample.alpha;scaleX=sample.scale;scaleY=sample.scale
                        translationY=sample.slideY*120f
                    }.background(Color(draft.backgroundColor),RoundedCornerShape(3.dp))
                        .padding(horizontal=7.dp,vertical=3.dp)) {
                        Text(previewText,color=Color(draft.color),
                            fontSize=draft.sizeSp.sp,fontFamily=subtitleFont(draft.fontFamily),
                            fontWeight=if(draft.bold) FontWeight.Bold else FontWeight.Normal,
                            textAlign=TextAlign.Center)
                    }
                }
                OutlinedTextField(value=draft.text,onValueChange={draft=draft.copy(text=it.take(500))},
                    label={Text("Texto")},maxLines=3,modifier=Modifier.fillMaxWidth())
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp),modifier=Modifier.fillMaxWidth()) {
                    TimeDigitsField("Início",draft.startMs,totalMs,
                        valid={it>=0L && it<draft.endMs},
                        onCommit={draft=draft.copy(startMs=it)},
                        modifier=Modifier.weight(1f),compact=true)
                    TimeDigitsField("Fim",draft.endMs,totalMs,
                        valid={it>draft.startMs && it<=totalMs},
                        onCommit={draft=draft.copy(endMs=it)},
                        modifier=Modifier.weight(1f),compact=true)
                }
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Text("Tamanho: ${draft.sizeSp}",fontSize=13.sp,
                        modifier=Modifier.weight(1f))
                    Slider(value=draft.sizeSp.toFloat(),valueRange=12f..48f,
                        modifier=Modifier.weight(2f),
                        onValueChange={draft=draft.copy(sizeSp=it.toInt().coerceIn(12,48))})
                }
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp)) {
                    OutlinedButton(onClick={stylesOpen=!stylesOpen},modifier=Modifier.weight(1f),
                        contentPadding=PaddingValues(5.dp)) {Text("Estilo",maxLines=1)}
                    OutlinedButton(onClick={animationOpen=!animationOpen},modifier=Modifier.weight(1f),
                        contentPadding=PaddingValues(5.dp)) {Text("Animação",maxLines=1)}
                    OutlinedButton(onClick=onDelete,contentPadding=PaddingValues(6.dp)) {
                        Icon(Icons.Default.Delete,"Excluir legenda",tint=MaterialTheme.colorScheme.error)
                    }
                }
                if(animationOpen) {
                    Text("Efeitos de entrada e revelação",fontWeight=FontWeight.SemiBold)
                    // Carrossel circular virtualizado: sempre permite rolar nos dois sentidos.
                    val choices=SubtitleAnimations.choices
                    val carouselStart=CircularCarousel.startIndex(choices.size)
                    val effectsScroll=rememberLazyListState(initialFirstVisibleItemIndex=carouselStart)
                    LazyRow(state=effectsScroll,modifier=Modifier.fillMaxWidth(),
                        horizontalArrangement=Arrangement.spacedBy(7.dp),
                        contentPadding=PaddingValues(horizontal=3.dp)) {
                        items(count=Int.MAX_VALUE) { index ->
                            val (key,label)=choices[CircularCarousel.optionIndex(index,choices.size)]
                            FilterChip(selected=draft.animation==key,onClick={
                                draft=draft.copy(animation=key)
                            },label={Text(label,maxLines=1)})
                        }
                    }
                    if(draft.animation!=SubtitleAnimations.NONE) {
                        Text("Velocidade: ${"%.1f".format(java.util.Locale.US,draft.animationSpeed)}×")
                        Slider(value=draft.animationSpeed,valueRange=.5f..2f,steps=5,
                            onValueChange={draft=draft.copy(animationSpeed=it)})
                        Text("Os efeitos seguem o tempo da legenda; não sincronizam com a voz.",
                            fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if(stylesOpen) {
                    Text("Estilos rápidos",fontWeight=FontWeight.SemiBold)
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                        SubtitleStyles.presets.forEach { preset ->
                            FilterChip(selected=draft.color==preset.foreground &&
                                draft.backgroundColor==preset.background,onClick={
                                draft=SubtitleStyles.applyToCue(draft,preset)
                            },label={Text(preset.name)})
                        }
                    }
                    Text("Fonte",fontWeight=FontWeight.SemiBold)
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                        SubtitleStyles.fonts.forEach { (family,name) ->
                            FilterChip(selected=draft.fontFamily==family,onClick={
                                draft=draft.copy(fontFamily=family)
                            },label={Text(name)})
                        }
                    }
                    Text("Contorno: ${draft.outlineWidth} dp")
                    Slider(value=draft.outlineWidth.toFloat(),valueRange=0f..4f,steps=3,
                        onValueChange={draft=draft.copy(outlineWidth=it.toInt())})
                    if(draft.outlineWidth>0) Row {
                        listOf("Preto" to 0xFF000000.toInt(),
                            "Branco" to 0xFFFFFFFF.toInt()).forEach { (label,color) ->
                            FilterChip(selected=draft.outlineColor==color,
                                onClick={draft=draft.copy(outlineColor=color)},label={Text(label)})
                        }
                    }
                    Text("Sombra: ${draft.shadowOffsetDp} dp")
                    Slider(value=draft.shadowOffsetDp.toFloat(),valueRange=0f..6f,steps=5,
                        onValueChange={draft=draft.copy(shadowOffsetDp=it.toInt())})
                    if(draft.shadowOffsetDp>0) Row {
                        listOf("Preta" to 0xFF000000.toInt(),
                            "Branca" to 0xFFFFFFFF.toInt()).forEach { (label,color) ->
                            FilterChip(selected=draft.shadowColor==color,
                                onClick={draft=draft.copy(shadowColor=color)},label={Text(label)})
                        }
                    }
                    Text("Espaçamento: ${draft.lineSpacingPercent}%")
                    Slider(value=draft.lineSpacingPercent.toFloat(),valueRange=100f..160f,steps=5,
                        onValueChange={draft=draft.copy(lineSpacingPercent=it.toInt())})
                    Text("Alinhamento")
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                        SubtitleStyles.alignments.forEach { (align,label) ->
                            FilterChip(selected=draft.textAlignment==align,onClick={
                                draft=draft.copy(textAlignment=align)
                            },label={Text(label)})
                        }
                    }
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        Text("Negrito",modifier=Modifier.weight(1f))
                        Switch(checked=draft.bold,onCheckedChange={draft=draft.copy(bold=it)})
                    }
                    Text("Posição horizontal: ${(draft.x*100).toInt()}%")
                    Slider(value=draft.x,valueRange=.1f.. .9f,
                        onValueChange={draft=draft.copy(x=it)})
                    Text("Posição vertical: ${(draft.y*100).toInt()}%")
                    Slider(value=draft.y,valueRange=.1f.. .9f,
                        onValueChange={draft=draft.copy(y=it)})
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                        listOf("Cima" to .15f,"Centro" to .5f,"Baixo" to .85f).forEach { (name,y) ->
                            FilterChip(selected=kotlin.math.abs(draft.y-y)<.02f,
                                onClick={draft=draft.copy(y=y)},label={Text(name)})
                        }
                    }
                }
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Checkbox(checked=applyAll,onCheckedChange={applyAll=it})
                    Text("Aplicar a todas as legendas",modifier=Modifier.weight(1f))
                }
                if(applyAll) {
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        Checkbox(checked=copyAppearance,onCheckedChange={copyAppearance=it})
                        Text("Aparência",fontSize=12.sp)
                        Checkbox(checked=copyPosition,onCheckedChange={copyPosition=it})
                        Text("Posição",fontSize=12.sp)
                    }
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        Checkbox(checked=copyAnimation,onCheckedChange={copyAnimation=it})
                        Text("Animação",fontSize=12.sp)
                    }
                    Text("Textos e horários nunca são copiados.",fontSize=11.sp,
                        color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if(!valid) Text("Informe um texto e horários válidos.",fontSize=11.sp,
                    color=MaterialTheme.colorScheme.error)
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp)) {
                    OutlinedButton(onClick=onDismiss,modifier=Modifier.weight(1f)) {Text("Cancelar")}
                    Button(onClick={if(applyAll) {confirmAll=true} else submit()},
                        modifier=Modifier.weight(1f),enabled=valid &&
                            (!applyAll || copyAppearance || copyPosition || copyAnimation)) {
                        Text("Salvar")
                    }
                }
            }
        }
    }
    if(confirmAll) AlertDialog(onDismissRequest={confirmAll=false},
        title={Text("Aplicar a todas?")},
        text={Text("Somente as propriedades escolhidas serão copiadas. Texto, horários e IDs das demais legendas permanecem inalterados.")},
        confirmButton={TextButton(onClick={confirmAll=false;submit()}) {Text("Aplicar")}},
        dismissButton={TextButton(onClick={confirmAll=false}) {Text("Cancelar")}})
}

