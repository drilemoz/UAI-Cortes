package br.com.uaicortes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Cada mídia principal aparece em exatamente uma pista: vídeo OU imagem.
 * Todas as pistas compartilham escala/scroll e a mesma posição do cursor.
 */
internal data class TimelineVisualSpan(val id: String, val title: String,
    val startMs: Long, val durationMs: Long)
internal data class TimelineSelection(val kind: TimelineKind, val id: String)

internal object UnifiedTimelinePolicy {
    fun mainSpans(clips: List<Clip>, image: Boolean): List<TimelineVisualSpan> {
        var at = 0L
        return buildList {
            clips.forEach { clip ->
                if (clip.isImage == image) add(TimelineVisualSpan(clip.id, clip.title, at, clip.durationMs))
                at += clip.durationMs
            }
        }
    }
    fun cursorFraction(positionMs: Long, durationMs: Long): Float =
        (positionMs.coerceIn(0L, durationMs.coerceAtLeast(0L)).toDouble() /
            durationMs.coerceAtLeast(1L).toDouble()).toFloat().coerceIn(0f,1f)
    fun position(fraction: Float, durationMs: Long): Long =
        (fraction.coerceIn(0f,1f) * durationMs.coerceAtLeast(0L)).toLong()

    fun preview(span: TimelineVisualSpan, handle: TimelineHandle,
        deltaMs: Long, totalMs: Long, minMs: Long): TimelineVisualSpan {
        val end = span.startMs + span.durationMs
        return when(handle) {
            TimelineHandle.MOVE -> span.copy(startMs=TimelinePrecision.step(span.startMs,deltaMs,
                (totalMs-span.durationMs).coerceAtLeast(0L)))
            TimelineHandle.LEFT -> {
                val newStart = TimelinePrecision.step(span.startMs,deltaMs,
                    (end-minMs).coerceAtLeast(0L))
                span.copy(startMs=newStart,durationMs=end-newStart)
            }
            TimelineHandle.RIGHT -> span.copy(durationMs=(span.durationMs+deltaMs)
                .coerceIn(minMs,(totalMs-span.startMs).coerceAtLeast(minMs)))
        }
    }
}

private val laneHeight = 52.dp
private val rulerHeight = 30.dp

/** A área central faz o drag; as alças de 18dp aparecem após seleção.
 * O único commit ocorre em onDragEnd. Tap NÃO navega para outras telas.
 */
@Composable
private fun TimelineSegment(span: TimelineVisualSpan, kind: TimelineKind,
    totalMs: Long, width: Dp, tint: Color, selected: Boolean,
    editable: Boolean, onSelect: () -> Unit,
    onGestureStart: () -> Unit, onGestureEnd: () -> Unit,
    onCommit: (TimelineHandle, Long, Long) -> Unit) {
    val density=LocalDensity.current
    val widthPx=with(density) { width.toPx() }
    val gripPx=with(density) { 18.dp.toPx() }
    val minimum = if (kind==TimelineKind.MAIN_IMAGE || kind==TimelineKind.PIP) 1_000L
        else if (kind==TimelineKind.SUBTITLE) SubtitleTimelineEdit.MIN_DURATION_MS
        else TimelineDirectEdit.MIN_VIDEO_MS
    var draft by remember(span.id,span.startMs,span.durationMs) { mutableStateOf(span) }
    var mode by remember(span.id) { mutableStateOf(TimelineHandle.MOVE) }
    var distancePx by remember(span.id) { mutableFloatStateOf(0f) }
    val currentSelect by rememberUpdatedState(onSelect)
    val currentCommit by rememberUpdatedState(onCommit)
    val selectedNow by rememberUpdatedState(selected)
    val currentGestureStart by rememberUpdatedState(onGestureStart)
    val currentGestureEnd by rememberUpdatedState(onGestureEnd)
    val drawableWidth = (width * (draft.durationMs.toDouble()/totalMs.coerceAtLeast(1L))
        .toFloat()).coerceAtLeast(5.dp).coerceAtMost(width)
    val left = (width * (draft.startMs.toDouble()/totalMs.coerceAtLeast(1L))
        .toFloat()).coerceIn(0.dp,(width-drawableWidth).coerceAtLeast(0.dp))
    Box(Modifier.offset(x=left).width(drawableWidth).height(laneHeight-4.dp)
        .padding(end=1.dp)
        .clip(RoundedCornerShape(7.dp))
        .background(tint.copy(alpha=if(selected) .88f else .62f))
        .then(if(selected) Modifier.border(2.dp,MaterialTheme.colorScheme.onSurface,
            RoundedCornerShape(7.dp)) else Modifier)
        .then(if(editable) Modifier.pointerInput(span.id,span.startMs,span.durationMs,widthPx,totalMs) {
            detectDragGestures(onDragStart={ at ->
                currentGestureStart()
                currentSelect()
                distancePx=0f
                val pxWidth=widthPx*(span.durationMs.toFloat()/totalMs.coerceAtLeast(1L))
                mode=if (!selectedNow || pxWidth < 2f*gripPx+14f) TimelineHandle.MOVE
                    else if(at.x < gripPx) TimelineHandle.LEFT
                    else if(at.x > pxWidth-gripPx) TimelineHandle.RIGHT
                    else TimelineHandle.MOVE
            },onDragCancel={draft=span;currentGestureEnd()},onDragEnd={
                val delta=AudioTimelineEdit.pixelsToMs(distancePx,widthPx,totalMs)
                if(delta!=0L) currentCommit(mode,delta,draft.startMs)
                draft=span
                currentGestureEnd()
            },onDrag={ change,amount ->
                change.consume()
                distancePx+=amount.x
                val delta=AudioTimelineEdit.pixelsToMs(distancePx,widthPx,totalMs)
                draft=UnifiedTimelinePolicy.preview(span,mode,delta,totalMs,minimum)
            })
        } else Modifier)
        .clickable { onSelect() }
        .semantics { contentDescription = "${span.title}, início ${formatPipMs(span.startMs)}, duração ${formatPipMs(span.durationMs)}. Toque para selecionar; arraste o centro para mover e as bordas para aparar." },
        contentAlignment=Alignment.Center) {
        if(selected && drawableWidth>=45.dp) {
            Row(Modifier.fillMaxSize(),verticalAlignment=Alignment.CenterVertically) {
                Box(Modifier.width(7.dp).fillMaxHeight().background(Color.White.copy(alpha=.94f)))
                Text(span.title,modifier=Modifier.weight(1f).padding(horizontal=3.dp),
                    color=Color.White,maxLines=1,overflow=TextOverflow.Ellipsis,
                    fontSize=10.sp,fontWeight=FontWeight.SemiBold)
                Box(Modifier.width(7.dp).fillMaxHeight().background(Color.White.copy(alpha=.94f)))
            }
        } else Text(span.title,maxLines=1,overflow=TextOverflow.Ellipsis,fontSize=10.sp,
            color=Color.White,modifier=Modifier.padding(horizontal=4.dp))
    }
}

@Composable
private fun TimelineLaneBody(kind: TimelineKind, entries: List<TimelineVisualSpan>,
    total: Long, tint: Color, width: Dp, selected: TimelineSelection?,
    onSelect: (TimelineSelection)->Unit, onEdit: (TimelineKind,String,TimelineHandle,Long,Long)->Unit,
    onGestureStart:()->Unit,onGestureEnd:()->Unit,
    canEdit: (String)->Boolean = {true},
    transitions: List<Clip> = emptyList(),onTransition:(Int)->Unit={}) {
    Box(Modifier.width(width).height(laneHeight).padding(vertical=2.dp)
        .clip(RoundedCornerShape(6.dp)).background(tint.copy(alpha=.13f))) {
        entries.forEach { span ->
            key(kind,span.id) {
                TimelineSegment(span,kind,total,width,tint,selected==TimelineSelection(kind,span.id),
                    editable=canEdit(span.id),onSelect={onSelect(TimelineSelection(kind,span.id))},
                    onGestureStart=onGestureStart,onGestureEnd=onGestureEnd,
                    onCommit={h,delta,start -> if(canEdit(span.id))onEdit(kind,span.id,h,delta,start)})
            }
        }
        if (transitions.size>1) {
            var elapsed=0L
            transitions.dropLast(1).forEachIndexed { index,clip ->
                elapsed+=clip.durationMs
                val x=width*(elapsed.toDouble()/total.coerceAtLeast(1L)).toFloat()
                Box(Modifier.offset(x=x-7.dp).width(14.dp).height(laneHeight-4.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha=.85f),RoundedCornerShape(4.dp))
                    .clickable {onTransition(index)},contentAlignment=Alignment.Center) {
                    Icon(Icons.Default.AutoAwesomeMotion,"Transição ${index+1}",
                        tint=MaterialTheme.colorScheme.onPrimary,modifier=Modifier.size(11.dp))
                }
            }
        }
    }
}

@Composable
internal fun UnifiedEditorTimeline(
    project:VideoProject, positionMs:Long,
    onSeek:(Long)->Unit,onClip:(String)->Unit,onSelectClip:(String)->Unit,
    onTransition:(Int)->Unit,onOverlay:(String)->Unit,onSelectOverlay:(String)->Unit,
    onAudio:(String)->Unit,onAddVideo:()->Unit,onAddImage:()->Unit,
    onOpenPiP:()->Unit,onOpenAudio:()->Unit,onOpenVoice:()->Unit,
    onSubtitle:(String)->Unit,onOpenSubtitle:()->Unit,
    onTimelineEdit:(TimelineKind,String,TimelineHandle,Long,Long)->Unit,
    onGestureStart:()->Unit,onGestureEnd:()->Unit,
    onUndo:()->Unit,onRedo:()->Unit,canUndo:Boolean,canRedo:Boolean,
    isEditable:(TimelineKind,String)->Boolean={_,_->true}
) {
    val total=project.durationMs.coerceAtLeast(1L)
    // Espaço útil após o último clipe para puxar a borda da capa final.
    val displayTotal=(total+15_000L).coerceAtLeast(total)
    val video=remember(project.clips) {UnifiedTimelinePolicy.mainSpans(project.clips,false)}
    val images=remember(project.clips) {UnifiedTimelinePolicy.mainSpans(project.clips,true)}
    val overlays=project.overlays.map {TimelineVisualSpan(it.id,if(it.isImage)"Foto PiP" else "Vídeo PiP",
        it.startAtMs,it.durationMs)}
    val resolved=ClipAudioBindings.resolved(project)
    val voice=resolved.filter { it.kind==VoiceIaPolicy.KIND }
    val audio=resolved.filter { it.kind!=VoiceIaPolicy.KIND }
    val audioSpans=audio.map {TimelineVisualSpan(it.id,it.title,it.startMs,it.durationMs)}+
        (project.music?.let {listOf(TimelineVisualSpan("music",it.title,0L,total))}?:emptyList())
    val voiceSpans=voice.map {TimelineVisualSpan(it.id,it.title,it.startMs,it.durationMs)}
    val subtitles=project.subtitles.map {TimelineVisualSpan(it.id,it.text,it.startMs,
        (it.endMs-it.startMs).coerceAtLeast(0L))}
    var selected by remember(project.id) {mutableStateOf<TimelineSelection?>(null)}
    var zoom by remember(project.id) {mutableIntStateOf(1)}
    var magnetic by remember(project.id) {mutableStateOf(true)}
    val scroll=rememberScrollState()
    val tracks=listOf(
        Triple(Icons.Default.Movie,"Vídeo",Blue),
        Triple(Icons.Default.Image,"Imagem",Color(0xFF9877CB)),
        Triple(Icons.Default.Layers,"PiP",Purple),
        Triple(Icons.Default.MusicNote,"Áudio",Green),
        Triple(Icons.Default.RecordVoiceOver,"Vozes",Purple),
        Triple(Icons.Default.ClosedCaption,"Legendas",Color(0xFFBE8B36)))
    val lanes=listOf(video,images,overlays,audioSpans,voiceSpans,subtitles)
    val kinds=listOf(TimelineKind.MAIN_VIDEO,TimelineKind.MAIN_IMAGE,TimelineKind.PIP,
        TimelineKind.AUDIO,TimelineKind.VOICE,TimelineKind.SUBTITLE)
    // Apenas faixas com conteúdo. Vídeo continua como faixa base;
    // botões de adicionar ficam disponíveis para os tipos ainda vazios.
    val visibleRows=tracks.indices.filter { it == 0 || lanes[it].isNotEmpty() }
    var addMenu by remember(project.id) { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(horizontal=10.dp)
        .clip(RoundedCornerShape(12.dp))
        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.20f))
        .padding(4.dp),verticalArrangement=Arrangement.spacedBy(3.dp)) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            Text("Linha do tempo",style=MaterialTheme.typography.labelMedium,
                modifier=Modifier.weight(1f))
            Box {
                TextButton(onClick={addMenu=true},contentPadding=PaddingValues(horizontal=6.dp)) {
                    Icon(Icons.Default.Add,"Adicionar mídia",Modifier.size(18.dp))
                    Text("Adicionar",fontSize=11.sp)
                }
                DropdownMenu(expanded=addMenu,onDismissRequest={addMenu=false}) {
                    tracks.forEachIndexed { index,track ->
                        DropdownMenuItem(text={Text(track.second)},leadingIcon={Icon(track.first,null)},
                            onClick={
                                addMenu=false
                                when(index) {0->onAddVideo();1->onAddImage();2->onOpenPiP();
                                    3->onOpenAudio();4->onOpenVoice();else->onOpenSubtitle()}
                            })
                    }
                }
            }
            Text("${zoom}×",fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
            IconButton(onClick={zoom=(zoom/2).coerceAtLeast(1)},enabled=zoom>1,
                modifier=Modifier.size(32.dp)) {Icon(Icons.Default.Remove,"Reduzir zoom",Modifier.size(18.dp))}
            IconButton(onClick={zoom=(zoom*2).coerceAtMost(8)},enabled=zoom<8,
                modifier=Modifier.size(32.dp)) {Icon(Icons.Default.Add,"Ampliar zoom",Modifier.size(18.dp))}
            IconButton(onClick={magnetic=!magnetic},modifier=Modifier.size(32.dp)) {
                Icon(Icons.Default.Link,"Encaixe magnético ${if(magnetic) "ligado" else "desligado"}",
                    Modifier.size(18.dp),tint=if(magnetic)MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick=onUndo,enabled=canUndo,modifier=Modifier.size(32.dp)) {
                Icon(Icons.Default.Undo,"Desfazer",Modifier.size(18.dp))}
            IconButton(onClick=onRedo,enabled=canRedo,modifier=Modifier.size(32.dp)) {
                Icon(Icons.Default.Redo,"Refazer",Modifier.size(18.dp))}
        }
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.Top) {
            Column(Modifier.width(46.dp),verticalArrangement=Arrangement.spacedBy(0.dp)) {
                Spacer(Modifier.height(rulerHeight))
                visibleRows.forEach { i ->
                    val item = tracks[i]
                    Box(Modifier.height(laneHeight).fillMaxWidth().padding(vertical=2.dp)
                        .clip(RoundedCornerShape(7.dp)).background(item.third)
                        .clickable {
                            when(i){0->onAddVideo();1->onAddImage();2->onOpenPiP();
                                3->onOpenAudio();4->onOpenVoice();else->onOpenSubtitle()}
                        }.semantics {contentDescription="Adicionar ou editar ${item.second}"},
                        contentAlignment=Alignment.Center) {
                        Icon(item.first,item.second,modifier=Modifier.size(20.dp),tint=Color.White)
                    }
                }
            }
            Spacer(Modifier.width(6.dp))
            BoxWithConstraints(Modifier.weight(1f)) {
                val stripWidth=maxWidth*zoom
                val stripWidthPx=with(LocalDensity.current){stripWidth.toPx()}
                Box(Modifier.fillMaxWidth().horizontalScroll(scroll)) {
                    Box(Modifier.width(stripWidth).height(rulerHeight+laneHeight*visibleRows.size)) {
                        Column(Modifier.fillMaxSize()) {
                            Box(Modifier.width(stripWidth).height(rulerHeight)
                                .pointerInput(total,stripWidthPx){
                                    detectTapGestures { onSeek(UnifiedTimelinePolicy.position(it.x/stripWidthPx,displayTotal).coerceAtMost(total)) }
                                }.pointerInput(total,stripWidthPx){
                                    detectDragGestures { change,_ -> change.consume()
                                        onSeek(UnifiedTimelinePolicy.position(change.position.x/stripWidthPx,displayTotal).coerceAtMost(total)) }
                                }) {
                                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                                    for(i in 0..4) Text(formatPipMs(displayTotal*i/4),fontSize=9.sp,
                                        color=MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            visibleRows.forEach { index ->
                                val spans=lanes[index]
                                TimelineLaneBody(kinds[index],spans,displayTotal,tracks[index].third,
                                    stripWidth,selected,onSelect={ item ->
                                        selected=item
                                        when(item.kind){
                                            TimelineKind.MAIN_VIDEO,TimelineKind.MAIN_IMAGE->onSelectClip(item.id)
                                            TimelineKind.PIP->onSelectOverlay(item.id)
                                            TimelineKind.AUDIO,TimelineKind.VOICE->{ /* toque apenas seleciona */ }
                                            TimelineKind.SUBTITLE->{ /* editar pelo lápis */ }
                                        }
                                    },onEdit={kind,id,handle,delta,target ->
                                        val adjusted=if(magnetic) TimelineMagnet.adjust(project,
                                            kind,id,handle,delta,positionMs) else delta
                                        onTimelineEdit(kind,id,handle,adjusted,target+adjusted-delta)
                                    },
                                    onGestureStart=onGestureStart,onGestureEnd=onGestureEnd,
                                    canEdit={id -> isEditable(kinds[index],id)},
                                    transitions=if(index==0)project.clips else emptyList(),
                                    onTransition=onTransition)
                            }
                        }
                        val playX=stripWidth*UnifiedTimelinePolicy.cursorFraction(positionMs,displayTotal)
                        // Cursor sem área de toque própria, não impede edição dos clipes.
                        Box(Modifier.offset(x=playX-1.dp,y=rulerHeight).width(2.dp)
                            .height(laneHeight*visibleRows.size).background(Color(0xFF176EFF)))
                        Box(Modifier.offset(x=playX-5.dp,y=rulerHeight-9.dp).size(10.dp)
                            .background(Color(0xFF176EFF),RoundedCornerShape(3.dp)))
                    }
                }
            }
        }
        val item=selected
        if(item!=null) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Text("Arraste para mover • puxe as alças para aparar",fontSize=10.sp,
                    color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.weight(1f),
                    maxLines=2)
                TextButton(onClick={
                    when(item.kind) {
                        TimelineKind.MAIN_VIDEO,TimelineKind.MAIN_IMAGE->onClip(item.id)
                        TimelineKind.PIP->onOverlay(item.id)
                        TimelineKind.AUDIO,TimelineKind.VOICE->onAudio(item.id)
                        TimelineKind.SUBTITLE->onSubtitle(item.id)
                    }
                }) {Icon(Icons.Default.Edit,"Editar detalhes",Modifier.size(16.dp))
                    Spacer(Modifier.width(3.dp));Text("Editar",fontSize=12.sp)}
            }
        } else Text("Toque no elemento para selecioná-lo. Os ícones à esquerda adicionam conteúdo.",
            fontSize=10.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,
            modifier=Modifier.padding(horizontal=4.dp,vertical=2.dp))
    }
}
