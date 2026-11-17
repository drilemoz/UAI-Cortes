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
    fun laneHeightDp(kind: TimelineKind): Int = when (kind) {
        TimelineKind.MAIN_VIDEO, TimelineKind.MAIN_IMAGE, TimelineKind.PIP -> 34
        TimelineKind.SUBTITLE -> 24
        else -> 28
    }

    /** Só reserva espaço em modo de extensão, sem alterar a escala do vídeo. */
    fun endExtensionMs(totalMs: Long, sourceRemainingMs: Long, zoom: Int): Long =
        minOf(sourceRemainingMs.coerceAtLeast(0L), 15_000L,
            (totalMs.coerceAtLeast(1L) / (3L * zoom.coerceAtLeast(1))).coerceAtLeast(1L))

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

private val rulerHeight = 22.dp

/** A área central faz o drag; as alças de 18dp aparecem após seleção.
 * O único commit ocorre em onDragEnd. Tap NÃO navega para outras telas.
 */
@Composable
private fun TimelineSegment(span: TimelineVisualSpan, kind: TimelineKind,
    totalMs: Long, width: Dp, tint: Color, selected: Boolean,
    editable: Boolean, laneHeight: Dp, canExtendRight: Boolean,
    onSelect: () -> Unit, onTap: () -> Unit,
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
        .background(tint.copy(alpha=if(selected) .98f else .78f))
        .then(if(selected) Modifier.border(2.dp,MaterialTheme.colorScheme.onSurface,
            RoundedCornerShape(7.dp)) else Modifier)
        .then(if(editable) Modifier.pointerInput(span.id,span.startMs,span.durationMs,widthPx,totalMs,canExtendRight) {
            detectDragGestures(onDragStart={ at ->
                currentGestureStart()
                currentSelect()
                distancePx=0f
                val pxWidth=widthPx*(span.durationMs.toFloat()/totalMs.coerceAtLeast(1L))
                // Em Estender final, até um clipe estreito tem uma borda direita utilizável.
                mode=if (canExtendRight && at.x > pxWidth - minOf(gripPx, pxWidth / 2f))
                        TimelineHandle.RIGHT
                    else if (!selectedNow || pxWidth < 2f*gripPx+14f) TimelineHandle.MOVE
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
        .clickable { onSelect(); onTap() }
        .semantics {
            contentDescription = "${span.title}, início ${formatPipMs(span.startMs)}, duração ${formatPipMs(span.durationMs)}. " +
                (if (kind == TimelineKind.SUBTITLE) "Toque para selecionar a legenda. Toque novamente para editar."
                else if (editable) "Toque para abrir as ações; arraste o centro para mover e as bordas para aparar."
                else "Toque para editar.")
        },
        contentAlignment=Alignment.Center) {
        if(editable && selected && drawableWidth>=45.dp) {
            Row(Modifier.fillMaxSize(),verticalAlignment=Alignment.CenterVertically) {
                Box(Modifier.width(7.dp).fillMaxHeight().background(Color.White.copy(alpha=.94f)))
                Text(span.title,modifier=Modifier.weight(1f).padding(horizontal=3.dp),
                    color=Color.White,maxLines=1,overflow=TextOverflow.Ellipsis,
                    fontSize=10.sp,fontWeight=FontWeight.SemiBold)
                Box(Modifier.width(7.dp).fillMaxHeight().background(Color.White.copy(alpha=.94f)))
            }
        } else if (drawableWidth >= 40.dp) Text(span.title,maxLines=1,overflow=TextOverflow.Ellipsis,fontSize=10.sp,
            color=Color.White,modifier=Modifier.padding(horizontal=4.dp))
    }
}

@Composable
private fun TimelineLaneBody(kind: TimelineKind, entries: List<TimelineVisualSpan>,
    total: Long, tint: Color, width: Dp, selected: TimelineSelection?,
    extendId: String? = null, onEmptyMusic: (() -> Unit)? = null,
    onSelect: (TimelineSelection)->Unit, onEntryTap: (TimelineSelection, Boolean)->Unit,
    onEdit: (TimelineKind,String,TimelineHandle,Long,Long)->Unit,
    onGestureStart:()->Unit,onGestureEnd:()->Unit,
    canEdit: (String)->Boolean = {true},
    transitions: List<Clip> = emptyList(),onTransition:(Int)->Unit={}) {
    val laneHeight = UnifiedTimelinePolicy.laneHeightDp(kind).dp
    Box(Modifier.width(width).height(laneHeight).padding(vertical=2.dp)
        .clip(RoundedCornerShape(5.dp)).background(tint.copy(alpha=.07f))) {
        if (entries.isEmpty() && onEmptyMusic != null) {
            Box(Modifier.fillMaxSize().clickable(onClick = onEmptyMusic).padding(start = 8.dp),
                contentAlignment = Alignment.CenterStart) {
                Text("+ Adicionar música", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
            }
        }
        entries.forEach { span ->
            key(kind,span.id) {
                TimelineSegment(span,kind,total,width,tint,selected==TimelineSelection(kind,span.id),
                    editable=canEdit(span.id),laneHeight=laneHeight,canExtendRight=extendId==span.id,
                    onSelect={onSelect(TimelineSelection(kind,span.id))},
                    onTap={
                        val item = TimelineSelection(kind,span.id)
                        onEntryTap(item, selected == item)
                    },
                    onGestureStart=onGestureStart,onGestureEnd=onGestureEnd,
                    onCommit={h,delta,start -> if(canEdit(span.id))onEdit(kind,span.id,h,delta,start)})
            }
        }
        if (transitions.size>1) {
            var elapsed=0L
            transitions.dropLast(1).forEachIndexed { index,clip ->
                elapsed+=clip.durationMs
                val x=width*(elapsed.toDouble()/total.coerceAtLeast(1L)).toFloat()
                Box(Modifier.offset(x=x-9.dp).width(18.dp).height(13.dp)
                    .clickable {onTransition(index)}.semantics {
                        contentDescription = "Transição ${index+1}"
                    }, contentAlignment=Alignment.TopCenter) {
                    Box(Modifier.padding(top=1.dp).size(7.dp)
                        .background(MaterialTheme.colorScheme.onPrimary,
                            RoundedCornerShape(2.dp))
                        .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp)))
                }
            }
        }
    }
}

@Composable
internal fun UnifiedEditorTimeline(
    project:VideoProject, positionMs:Long, selectedClipId: String,
    onSeek:(Long)->Unit,onClip:(String)->Unit,onSelectClip:(String)->Unit,
    onTransition:(Int)->Unit,onOverlay:(String)->Unit,onSelectOverlay:(String)->Unit,
    onAudio:(String)->Unit,onAddVideo:()->Unit,onAddImage:()->Unit,
    onOpenPiP:()->Unit,onOpenAudio:()->Unit,onOpenMusic:()->Unit,onOpenVoice:()->Unit,
    onSubtitle:(String)->Unit,onOpenSubtitle:()->Unit,
    onTimelineEdit:(TimelineKind,String,TimelineHandle,Long,Long)->Unit,
    onGestureStart:()->Unit,onGestureEnd:()->Unit,
    onUndo:()->Unit,onRedo:()->Unit,canUndo:Boolean,canRedo:Boolean,
    isEditable:(TimelineKind,String)->Boolean={_,_->true}
) {
    val total=project.durationMs.coerceAtLeast(1L)
    val video=remember(project.clips) {UnifiedTimelinePolicy.mainSpans(project.clips,false)}
    val images=remember(project.clips) {UnifiedTimelinePolicy.mainSpans(project.clips,true)}
    val overlays=project.overlays.map {TimelineVisualSpan(it.id,if(it.isImage)"Foto PiP" else "Vídeo PiP",
        it.startAtMs,it.durationMs)}
    val resolved=ClipAudioBindings.resolved(project)
    val voice=resolved.filter { it.kind==VoiceIaPolicy.KIND }
    val audio=resolved.filter { it.kind!=VoiceIaPolicy.KIND }
    val audioSpans=audio.map {TimelineVisualSpan(it.id,it.title,it.startMs,it.durationMs)}
    val musicSpan = project.music?.let { music ->
        ExportAudioPolicy.musicWindow(music, project.durationMs)?.let {
            TimelineVisualSpan("music", music.title.ifBlank { "Música" },
                it.timelineStartMs, it.playableMs)
        }
    }
    val voiceSpans=voice.map {TimelineVisualSpan(it.id,it.title,it.startMs,it.durationMs)}
    val subtitles=project.subtitles.map {TimelineVisualSpan(it.id,it.text,it.startMs,
        (it.endMs-it.startMs).coerceAtLeast(0L))}
    var selected by remember(project.id) {mutableStateOf<TimelineSelection?>(null)}
    var zoom by remember(project.id) {mutableIntStateOf(1)}
    var magnetic by remember(project.id) {mutableStateOf(true)}
    val last = project.clips.lastOrNull()
    val sourceRemaining = last?.let {
        if (it.isImage) TimelineDirectEdit.MAX_IMAGE_MS - it.durationMs else it.sourceDurationMs - it.endMs
    } ?: 0L
    val canExtend = last != null && sourceRemaining > 0L &&
        (selected?.id ?: selectedClipId) == last.id
    var extendId by remember(project.id, last?.id) { mutableStateOf<String?>(null) }
    val extending = canExtend && extendId != null
    val displayTotal = total + if (extending)
        UnifiedTimelinePolicy.endExtensionMs(total, sourceRemaining, zoom) else 0L
    val scroll=rememberScrollState()
    LaunchedEffect(extending, scroll.maxValue) {
        if (extending && scroll.maxValue < Int.MAX_VALUE) scroll.scrollTo(scroll.maxValue)
    }
    LaunchedEffect(canExtend) { if (!canExtend) extendId = null }
    val tracks=listOf(
        Triple(Icons.Default.Movie,"Vídeo",Blue),
        Triple(Icons.Default.Image,"Imagem",Color(0xFF9877CB)),
        Triple(Icons.Default.Layers,"PiP",Purple),
        Triple(Icons.Default.GraphicEq,"Áudio",Green),
        Triple(Icons.Default.MusicNote,"Música",Color(0xFFA25125)),
        Triple(Icons.Default.RecordVoiceOver,"Vozes",Purple),
        Triple(Icons.Default.ClosedCaption,"Legendas",Color(0xFFBE8B36)))
    val lanes=listOf(video,images,overlays,audioSpans,listOfNotNull(musicSpan),voiceSpans,subtitles)
    val kinds=listOf(TimelineKind.MAIN_VIDEO,TimelineKind.MAIN_IMAGE,TimelineKind.PIP,
        TimelineKind.AUDIO,TimelineKind.MUSIC,TimelineKind.VOICE,TimelineKind.SUBTITLE)
    // Música fica explícita, inclusive vazia. As demais pistas auxiliares só aparecem se houver itens.
    val visibleRows=tracks.indices.filter {
        it == 0 || lanes[it].isNotEmpty() || kinds[it] == TimelineKind.MUSIC
    }
    val lanesHeight = visibleRows.sumOf { UnifiedTimelinePolicy.laneHeightDp(kinds[it]) }.dp
    val selectedSpan = selected?.let { current ->
        lanes.getOrNull(kinds.indexOf(current.kind))?.firstOrNull { it.id == current.id }
    }
    fun openTrack(index: Int) {
        when (kinds[index]) {
            TimelineKind.MAIN_VIDEO -> onAddVideo()
            TimelineKind.MAIN_IMAGE -> onAddImage()
            TimelineKind.PIP -> onOpenPiP()
            TimelineKind.AUDIO -> onOpenAudio()
            TimelineKind.MUSIC -> onOpenMusic()
            TimelineKind.VOICE -> onOpenVoice()
            TimelineKind.SUBTITLE -> onOpenSubtitle()
        }
    }
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
                                openTrack(index)
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
            Column(Modifier.width(62.dp),verticalArrangement=Arrangement.spacedBy(0.dp)) {
                Spacer(Modifier.height(rulerHeight))
                visibleRows.forEach { i ->
                    val item = tracks[i]
                    val kind = kinds[i]
                    Row(Modifier.height(UnifiedTimelinePolicy.laneHeightDp(kind).dp).fillMaxWidth()
                        .clickable { openTrack(i) }.semantics {contentDescription="Adicionar ou editar ${item.second}"},
                        verticalAlignment=Alignment.CenterVertically,
                        horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                        Icon(item.first, null, modifier=Modifier.size(14.dp), tint=item.third)
                        Text(if (kind == TimelineKind.SUBTITLE) "Legendas" else item.second,
                            fontSize=9.sp, maxLines=1, overflow=TextOverflow.Ellipsis,
                            color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.width(6.dp))
            BoxWithConstraints(Modifier.weight(1f)) {
                val stripWidth=maxWidth*zoom*(displayTotal.toDouble()/total).toFloat()
                val stripWidthPx=with(LocalDensity.current){stripWidth.toPx()}
                Box(Modifier.fillMaxWidth().horizontalScroll(scroll)) {
                    Box(Modifier.width(stripWidth).height(rulerHeight+lanesHeight)) {
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
                                    stripWidth,selected,
                                    extendId = extendId.takeIf { extending },
                                    onEmptyMusic = if (kinds[index] == TimelineKind.MUSIC && project.music == null)
                                        onOpenMusic else null,
                                    onSelect={ item ->
                                        selected=item
                                        when(item.kind){
                                            TimelineKind.MAIN_VIDEO,TimelineKind.MAIN_IMAGE->onSelectClip(item.id)
                                            TimelineKind.PIP->onSelectOverlay(item.id)
                                            TimelineKind.AUDIO,TimelineKind.VOICE,TimelineKind.MUSIC->{ /* toque apenas seleciona */ }
                                            TimelineKind.SUBTITLE->{ /* editar pelo lápis */ }
                                        }
                                    },onEntryTap={ item, wasSelected ->
                                        when(item.kind) {
                                            TimelineKind.MAIN_VIDEO,TimelineKind.MAIN_IMAGE->onClip(item.id)
                                            TimelineKind.PIP->onOverlay(item.id)
                                            TimelineKind.AUDIO,TimelineKind.VOICE->onAudio(item.id)
                                            TimelineKind.MUSIC->onOpenMusic()
                                            TimelineKind.SUBTITLE->if (wasSelected) onSubtitle(item.id)
                                        }
                                    },onEdit={kind,id,handle,delta,target ->
                                        val extendingRight = extending && id == extendId && handle == TimelineHandle.RIGHT
                                        // Não deixe o encaixe no antigo fim bloquear uma extensão curta.
                                        val adjusted = if (extendingRight && delta > 0L)
                                            delta.coerceAtMost(displayTotal - total)
                                        else if (magnetic) TimelineMagnet.adjust(project,
                                            kind,id,handle,delta,positionMs) else delta
                                        onTimelineEdit(kind,id,handle,adjusted,target+adjusted-delta)
                                        if (extending && id == extendId && handle == TimelineHandle.RIGHT) extendId = null
                                    },
                                    onGestureStart=onGestureStart,onGestureEnd=onGestureEnd,
                                    canEdit={id -> isEditable(kinds[index],id)},
                                    transitions=if(index==0)project.clips else emptyList(),
                                    onTransition=onTransition)
                            }
                        }
                        val playX=stripWidth*UnifiedTimelinePolicy.cursorFraction(positionMs,displayTotal)
                        // Cursor sem área de toque própria, não impede edição dos clipes.
                        val cursorX = playX.coerceIn(0.dp, (stripWidth - 1.dp).coerceAtLeast(0.dp))
                        Box(Modifier.offset(x=cursorX,y=rulerHeight).width(1.dp)
                            .height(lanesHeight).background(Color(0xFF176EFF)))
                        Box(Modifier.offset(x=(cursorX-3.dp).coerceIn(0.dp,
                            (stripWidth-6.dp).coerceAtLeast(0.dp)),y=rulerHeight-6.dp).size(6.dp)
                            .background(Color(0xFF176EFF),RoundedCornerShape(3.dp)))
                    }
                }
            }
        }
        if (selectedSpan != null || canExtend) {
            Row(Modifier.fillMaxWidth().heightIn(min=24.dp), verticalAlignment=Alignment.CenterVertically) {
                Text(if (extending) "Arraste a borda direita do último clipe."
                    else selectedSpan?.title.orEmpty(), fontSize=10.sp,
                    maxLines=if (selected?.kind == TimelineKind.SUBTITLE) 2 else 1,
                    overflow=TextOverflow.Ellipsis, color=MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier=Modifier.weight(1f).padding(horizontal=3.dp))
                val caption = selected?.takeIf { it.kind == TimelineKind.SUBTITLE && selectedSpan != null }
                if (caption != null) Text("Editar legenda", color=MaterialTheme.colorScheme.primary,
                    fontSize=10.sp, modifier=Modifier.clip(RoundedCornerShape(4.dp))
                        .clickable { onSubtitle(caption.id) }.padding(horizontal=6.dp, vertical=5.dp))
                if (canExtend) Text(if (extending) "Fechar extensão" else "Estender final",
                    color=MaterialTheme.colorScheme.primary, fontSize=10.sp,
                    modifier=Modifier.clip(RoundedCornerShape(4.dp)).clickable {
                        extendId = if (extending) null else last?.id
                    }.padding(horizontal=6.dp, vertical=5.dp))
            }
        }
    }
}
