package br.com.uaicortes

import android.net.Uri
import android.os.Build
import android.media.MediaMetadataRetriever
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackParameters
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Extraído da MainActivity na v0.1.53 para reutilização de componentes.

@Composable
internal fun TrackAudioPreview(uri: String, startMs: Long, durationMs: Long,
    volume: Float, projectMs: Long, playing: Boolean, speed: Float, loop: Boolean,
    sourceStartMs: Long = 0L) {
    val context=LocalContext.current
    val player=remember(uri) { diagnosticPlayer(context) }
    DisposableEffect(player) { onDispose {player.release()} }
    LaunchedEffect(player,uri,loop) {
        player.repeatMode=if(loop)Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        player.setMediaItem(MediaItem.fromUri(uri));player.prepare()
    }
    LaunchedEffect(player,volume,speed) {
        player.volume=volume.coerceIn(0f,1f)
        player.setPlaybackParameters(PlaybackParameters(speed))
    }
    LaunchedEffect(player,projectMs,playing,startMs,durationMs,sourceStartMs) {
        val within=projectMs>=startMs && projectMs<startMs+durationMs
        if(!within || !playing) player.pause() else {
            val wanted=sourceStartMs+(projectMs-startMs).coerceAtLeast(0L)
            if(kotlin.math.abs(player.currentPosition-wanted)>600L) player.seekTo(wanted)
            player.play()
        }
    }
}

@Composable
internal fun RowScope.ClipSheetAction(label: String, icon: ImageVector,
    enabled: Boolean = true, click: () -> Unit) {
    OutlinedButton(onClick=click,enabled=enabled,modifier=Modifier.weight(1f),
        contentPadding=PaddingValues(horizontal=2.dp,vertical=8.dp),
        shape=RoundedCornerShape(7.dp)) {
        Column(horizontalAlignment=Alignment.CenterHorizontally) {
            Icon(icon,label,modifier=Modifier.size(18.dp))
            Text(label,fontSize=10.sp,maxLines=1)
        }
    }
}

@Composable
internal fun TimelineRow(label:String,color:Color, segments:List<Pair<String,Long>>,total:Long,position:Long,
    selectedId:String,onClip:(Int)->Unit,onBoundary:((Int)->Unit)?=null) {
    Row(Modifier.fillMaxWidth().height(34.dp).padding(horizontal=12.dp,vertical=2.dp),
        verticalAlignment=Alignment.CenterVertically) {
        Box(Modifier.width(60.dp).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(color),
            contentAlignment=Alignment.Center) { Text(label,color=Color.White,fontSize=11.sp) }
        Spacer(Modifier.width(6.dp))
        BoxWithConstraints(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(3.dp))
            .background(color.copy(alpha=.14f))) {
            // Captura a largura no escopo de BoxWithConstraints: o Row interno muda o receptor.
            val timelineWidth = maxWidth
            Row(Modifier.fillMaxSize()) {
                segments.forEachIndexed { index,(title,duration) ->
                    val fraction = (duration.toFloat()/total.coerceAtLeast(1L)).coerceIn(0f,1f)
                    Box(Modifier.width(timelineWidth*fraction).fillMaxHeight().padding(end=1.dp)
                        .background(color.copy(alpha=.36f)).clickable { onClip(index) },
                        contentAlignment=Alignment.Center) {
                        Text(title,maxLines=1,overflow=TextOverflow.Ellipsis,fontSize=10.sp,
                            modifier=Modifier.padding(horizontal=2.dp))
                    }
                }
            }
            if (onBoundary != null && segments.size > 1) {
                var elapsed = 0L
                segments.dropLast(1).forEachIndexed { index, segment ->
                    elapsed += segment.second
                    val offset = timelineWidth * (elapsed.toFloat() / total.coerceAtLeast(1L))
                    Box(Modifier.offset(x = offset - 11.dp).width(22.dp).fillMaxHeight()
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(5.dp))
                        .clickable { onBoundary(index) }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.AutoAwesomeMotion, "Editar transição ${index+1}",
                            tint=MaterialTheme.colorScheme.onPrimary,modifier=Modifier.size(15.dp))
                    }
                }
            }
        }
    }
}

/** Superfície gestual de timeline. Acumular pixels antes da conversão impede
 * perda de deslocamentos pequenos durante gestos lentos.
 */
@Composable
internal fun AudioDragSurface(
    modifier: Modifier, totalMs: Long, widthPx: Float,
    onStep: (Long) -> Unit, onFinish: () -> Unit = {},
    onTap: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val currentStep by rememberUpdatedState(onStep)
    val currentFinish by rememberUpdatedState(onFinish)
    val currentTap by rememberUpdatedState(onTap)
    val clickModifier = if (onTap != null) Modifier.clickable { currentTap?.invoke() } else Modifier
    Box(modifier.then(clickModifier).pointerInput(totalMs, widthPx) {
        var accumulatedPx = 0f
        var appliedMs = 0L
        detectDragGestures(
            onDragStart = { accumulatedPx = 0f; appliedMs = 0L },
            onDragEnd = { currentFinish() },
            onDragCancel = {},
            onDrag = { change, amount ->
                change.consume()
                accumulatedPx += amount.x
                val newMs = AudioTimelineEdit.pixelsToMs(accumulatedPx, widthPx, totalMs)
                val delta = newMs - appliedMs
                if (delta != 0L) { currentStep(delta); appliedMs = newMs }
            })
    }, content = content)
}

/** Na tela principal é possível arrastar cada faixa para outro momento.
 * O movimento é persistido apenas quando o gesto termina, evitando gravação em cada pixel.
 * Toque curto abre o editor detalhado da faixa.
 */
@Composable
internal fun InteractiveAudioTimeline(p: VideoProject,
    onSelectTrack: (String) -> Unit, onMoveTrack: (String, Long) -> Unit,
    onLegacyClick: () -> Unit) {
    val totalMs = p.durationMs.coerceAtLeast(1L)
    Row(Modifier.fillMaxWidth().height(38.dp).padding(horizontal=12.dp, vertical=2.dp),
        verticalAlignment=Alignment.CenterVertically) {
        Box(Modifier.width(60.dp).fillMaxHeight().background(Green, RoundedCornerShape(3.dp)),
            contentAlignment=Alignment.Center) { Text("Áudio", color=Color.White, fontSize=11.sp) }
        Spacer(Modifier.width(6.dp))
        BoxWithConstraints(Modifier.weight(1f).fillMaxHeight()
            .clip(RoundedCornerShape(3.dp)).background(Green.copy(alpha=.14f))) {
            val timelineWidth = maxWidth
            val timelineWidthPx = with(LocalDensity.current) { timelineWidth.toPx() }
            p.music?.let { music ->
                Box(Modifier.fillMaxSize().background(Green.copy(alpha=.16f))
                    .clickable(onClick=onLegacyClick), contentAlignment=Alignment.CenterStart) {
                    Text(music.title, modifier=Modifier.padding(start=3.dp),
                        fontSize=9.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
                }
            }
            p.audioTracks.forEach { audio ->
                key(audio.id) {
                    var candidateStart by remember(audio.id, audio.startMs) { mutableLongStateOf(audio.startMs) }
                    var changed by remember(audio.id, audio.startMs) { mutableStateOf(false) }
                    val range = AudioTimelineDraft(candidateStart, audio.sourceStartMs,
                        audio.sourceStartMs + audio.durationMs)
                    val startFraction = (candidateStart.toFloat()/totalMs).coerceIn(0f,1f)
                    val endFraction = (audio.durationMs.toFloat()/totalMs).coerceIn(0f,1f-startFraction)
                    val drawnWidth = (timelineWidth*endFraction).coerceAtLeast(22.dp).coerceAtMost(timelineWidth)
                    val drawnX = (timelineWidth*startFraction).coerceAtMost(timelineWidth-drawnWidth)
                    AudioDragSurface(
                        modifier=Modifier.offset(x=drawnX).width(drawnWidth).fillMaxHeight()
                            .background(Green.copy(alpha=.72f),RoundedCornerShape(3.dp))
                            .semantics { contentDescription = "${audio.title}: arrastar para mover; tocar para editar" },
                        totalMs=totalMs, widthPx=timelineWidthPx,
                        onStep={ delta ->
                            candidateStart = AudioTimelineEdit.move(
                                range.copy(timelineStartMs=candidateStart), delta, totalMs).timelineStartMs
                            changed = changed || candidateStart != audio.startMs
                        },
                        onFinish={
                            if (changed && candidateStart != audio.startMs) onMoveTrack(audio.id, candidateStart)
                            changed = false
                        },
                        onTap={onSelectTrack(audio.id)}) {
                        Text(audio.title, maxLines=1, overflow=TextOverflow.Ellipsis,
                            fontSize=10.sp,color=Color.White,modifier=Modifier.align(Alignment.Center).padding(horizontal=4.dp))
                    }
                }
            }
        }
    }
}

/** Ferramenta visual de três gestos independentes: início, deslocamento, fim.
 * Os campos numéricos permanecem disponíveis para recortes menores que as alças.
 */
@Composable
internal fun AudioTrimGestureTimeline(draft: AudioTimelineDraft, projectMs: Long, sourceMs: Long,
    onMove: (Long) -> Unit, onLeftTrim: (Long) -> Unit, onRightTrim: (Long) -> Unit) {
    val total = projectMs.coerceAtLeast(1L)
    BoxWithConstraints(Modifier.fillMaxWidth().height(74.dp).padding(vertical=12.dp)
        .background(Green.copy(alpha=.12f),RoundedCornerShape(6.dp))) {
        val timelineWidth = maxWidth
        val timelineWidthPx = with(LocalDensity.current) { timelineWidth.toPx() }
        val start = (draft.timelineStartMs.toFloat()/total).coerceIn(0f,1f)
        val length = (draft.durationMs.toFloat()/total).coerceIn(0f,1f-start)
        val w = (timelineWidth*length).coerceAtLeast(42.dp).coerceAtMost(timelineWidth)
        val x = (timelineWidth*start).coerceAtMost(timelineWidth-w)
        val edge = 15.dp
        Box(Modifier.offset(x=x).width(w).fillMaxHeight()
            .background(Green.copy(alpha=.46f),RoundedCornerShape(5.dp)))
        AudioDragSurface(Modifier.offset(x=x+edge).width((w-edge*2).coerceAtLeast(0.dp))
            .fillMaxHeight().semantics { contentDescription = "Arrastar trecho de áudio" },
            totalMs=total,widthPx=timelineWidthPx,onStep=onMove) {
            Icon(Icons.Default.OpenWith,null,tint=Color.White,
                modifier=Modifier.size(17.dp).align(Alignment.Center))
        }
        AudioDragSurface(Modifier.offset(x=x).width(edge).fillMaxHeight()
            .background(Green,RoundedCornerShape(topStart=5.dp,bottomStart=5.dp))
            .semantics { contentDescription = "Ajustar entrada do áudio" },
            totalMs=total,widthPx=timelineWidthPx,onStep=onLeftTrim) {
            Box(Modifier.align(Alignment.Center).width(2.dp).height(22.dp).background(Color.White))
        }
        AudioDragSurface(Modifier.offset(x=x+w-edge).width(edge).fillMaxHeight()
            .background(Green,RoundedCornerShape(topEnd=5.dp,bottomEnd=5.dp))
            .semantics { contentDescription = "Ajustar saída do áudio" },
            totalMs=total,widthPx=timelineWidthPx,
            onStep={ if (sourceMs >= AudioTrackTiming.MIN_TRIM_MS) onRightTrim(it) }) {
            Box(Modifier.align(Alignment.Center).width(2.dp).height(22.dp).background(Color.White))
        }
    }
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
        Text(formatPipMs(draft.timelineStartMs),fontSize=11.sp,
            color=MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Duração ${formatPipMs(draft.durationMs.coerceAtLeast(0L))}",fontSize=11.sp,
            color=MaterialTheme.colorScheme.onSurfaceVariant)
        Text(formatPipMs(draft.timelineEndMs),fontSize=11.sp,
            color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Pista compacta no editor. Cada bloco abre a legenda correta, mesmo quando
 * as legendas foram importadas fora da ordem cronológica.
 */
@Composable
internal fun SubtitleOverviewTimeline(cues:List<SubtitleCue>,totalMs:Long,
    onSelect:(String)->Unit,onOpen:()->Unit) {
    val gold=Color(0xFFC38A38)
    Row(Modifier.fillMaxWidth().height(34.dp).padding(horizontal=12.dp,vertical=2.dp),
        verticalAlignment=Alignment.CenterVertically) {
        Box(Modifier.width(60.dp).fillMaxHeight().background(gold,RoundedCornerShape(3.dp))
            .clickable { onOpen() },contentAlignment=Alignment.Center) {
            Text("Legenda",color=Color.White,fontSize=11.sp)
        }
        Spacer(Modifier.width(6.dp))
        BoxWithConstraints(Modifier.weight(1f).fillMaxHeight()
            .background(gold.copy(alpha=.14f),RoundedCornerShape(3.dp))) {
            val width=maxWidth
            cues.sortedBy { it.startMs }.forEach { cue ->
                key(cue.id) {
                    val start=(cue.startMs.toFloat()/totalMs.coerceAtLeast(1L)).coerceIn(0f,1f)
                    val span=((cue.endMs-cue.startMs).toFloat()/totalMs.coerceAtLeast(1L))
                        .coerceIn(0f,1f-start)
                    val drawn=(width*span).coerceAtLeast(8.dp).coerceAtMost(width)
                    val left=(width*start).coerceAtMost(width-drawn)
                    Box(Modifier.offset(x=left).width(drawn).fillMaxHeight()
                        .background(gold.copy(alpha=.65f),RoundedCornerShape(2.dp))
                        .clickable { onSelect(cue.id) }
                        .semantics { contentDescription="Editar legenda: ${cue.text.take(50)}" },
                        contentAlignment=Alignment.Center) {
                        Text(cue.text,maxLines=1,overflow=TextOverflow.Ellipsis,fontSize=9.sp,
                            color=Color.White)
                    }
                }
            }
        }
    }
}

/** Linha do tempo única e virtualizada em janelas de 10 segundos.
 * Cada janela usa tempos absolutos: legendas sobrepostas continuam selecionáveis pela lista.
 * Não são criadas 30 (ou 300) trilhas individuais na tela.
 */
@Composable
internal fun SubtitleCompactTimeline(cues: List<SubtitleCue>, totalMs: Long,
    selectedId: String, onSelect: (String) -> Unit) {
    val segmentMs = SubtitleCompactLayout.WINDOW_MS
    val duration = totalMs.coerceAtLeast(1L)
    val count = SubtitleCompactLayout.windowCount(totalMs)
    val listState = rememberLazyListState()
    val selectedStart = cues.firstOrNull { it.id == selectedId }?.startMs
    LaunchedEffect(selectedId, selectedStart, count) {
        if (selectedStart != null) {
            listState.animateScrollToItem(SubtitleCompactLayout.windowFor(selectedStart,totalMs))
        }
    }
    LazyRow(state=listState, modifier=Modifier.fillMaxWidth().height(62.dp),
        horizontalArrangement=Arrangement.spacedBy(3.dp)) {
        items(count=count, key={ it }) { segment ->
            val begin = segment.toLong() * segmentMs
            val end = minOf(begin + segmentMs, duration)
            val visible = cues.filter {
                SubtitleCompactLayout.visibleRange(segment,it.startMs,it.endMs,totalMs) != null
            }
            Column(Modifier.width(260.dp)) {
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                    Text(formatPipMs(begin),fontSize=10.sp,
                        color=MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatPipMs(end),fontSize=10.sp,
                        color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Box(Modifier.fillMaxWidth().height(38.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)) {
                    visible.forEach { cue ->
                        key(cue.id) {
                            val range = SubtitleCompactLayout.visibleRange(
                                segment,cue.startMs,cue.endMs,totalMs)!!
                            val leftMs = range.first
                            val rightMs = range.second
                            val fraction = (leftMs - begin).toFloat() / segmentMs
                            val span = (rightMs - leftMs).toFloat() / segmentMs
                            val width = (260.dp * span).coerceAtLeast(6.dp).coerceAtMost(260.dp)
                            val x = (260.dp * fraction).coerceIn(0.dp, 260.dp - width)
                            val lane = (cues.indexOf(cue) % 2) * 19
                            Box(Modifier.offset(x=x, y=lane.dp).width(width).height(18.dp)
                                .background(if(cue.id==selectedId)Blue
                                    else Color(0xFFC38A38),RoundedCornerShape(3.dp))
                                .clickable { onSelect(cue.id) }
                                .semantics { contentDescription="Selecionar legenda: ${cue.text.take(45)}" },
                                contentAlignment=Alignment.Center) {
                                Text(cue.text,color=Color.White,fontSize=9.sp,maxLines=1,
                                    overflow=TextOverflow.Ellipsis,
                                    modifier=Modifier.padding(horizontal=2.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Uma pista por legenda para não ocultar controles quando os tempos se sobrepõem.
 * Os valores são rascunho enquanto o gesto ocorre; somente onFinish grava.
 */
@Composable
internal fun SubtitleTimelineEditor(cues:List<SubtitleCue>,totalMs:Long,selectedId:String,
    onSelect:(String)->Unit,onCommit:(String,Long,Long)->Unit) {
    val gold=Color(0xFFC38A38)
    val total=totalMs.coerceAtLeast(1L)
    if(cues.isEmpty()) {
        Text("Nenhuma legenda adicionada.",fontSize=12.sp)
        return
    }
    cues.sortedWith(compareBy<SubtitleCue> { it.startMs }.thenBy { it.id })
        .forEachIndexed { index,cue ->
        key(cue.id) {
            var draftStart by remember(cue.id,cue.startMs,cue.endMs) { mutableLongStateOf(cue.startMs) }
            var draftEnd by remember(cue.id,cue.startMs,cue.endMs) { mutableLongStateOf(cue.endMs) }
            var changed by remember(cue.id,cue.startMs,cue.endMs) { mutableStateOf(false) }
            fun updateDraft(next:Pair<Long,Long>) {
                draftStart=next.first
                draftEnd=next.second
                changed=(draftStart!=cue.startMs || draftEnd!=cue.endMs)
            }
            fun commitDraft() {
                if(changed && (draftStart!=cue.startMs || draftEnd!=cue.endMs)) {
                    onCommit(cue.id,draftStart,draftEnd)
                }
                changed=false
            }
            Row(Modifier.fillMaxWidth().height(44.dp).padding(vertical=3.dp),
                verticalAlignment=Alignment.CenterVertically) {
                Box(Modifier.width(60.dp).fillMaxHeight()
                    .background(if(cue.id==selectedId)MaterialTheme.colorScheme.primary else gold,
                        RoundedCornerShape(4.dp))
                    .clickable { onSelect(cue.id) },contentAlignment=Alignment.Center) {
                    Text("Leg. ${index+1}",fontSize=11.sp,color=Color.White)
                }
                Spacer(Modifier.width(6.dp))
                BoxWithConstraints(Modifier.weight(1f).fillMaxHeight()
                    .background(gold.copy(alpha=.12f),RoundedCornerShape(4.dp))) {
                    val width=maxWidth
                    val widthPx=with(LocalDensity.current) { width.toPx() }
                    val start=(draftStart.toFloat()/total).coerceIn(0f,1f)
                    val fraction=((draftEnd-draftStart).toFloat()/total).coerceIn(0f,1f-start)
                    val drawWidth=(width*fraction).coerceAtLeast(54.dp).coerceAtMost(width)
                    val x=(width*start).coerceAtMost(width-drawWidth)
                    Row(Modifier.offset(x=x).width(drawWidth).fillMaxHeight()
                        .background(if(cue.id==selectedId)gold else gold.copy(alpha=.68f),
                            RoundedCornerShape(4.dp)),verticalAlignment=Alignment.CenterVertically) {
                        AudioDragSurface(Modifier.width(18.dp).fillMaxHeight()
                            .background(Color.Black.copy(alpha=.22f)),total,widthPx,
                            onStep={ delta -> updateDraft(SubtitleTimelineEdit.trimLeft(
                                draftStart,draftEnd,delta,total)) },onFinish=::commitDraft,
                            onTap={onSelect(cue.id)}) {
                            Text("|",color=Color.White,modifier=Modifier.align(Alignment.Center))
                        }
                        AudioDragSurface(Modifier.weight(1f).fillMaxHeight(),total,widthPx,
                            onStep={ delta -> updateDraft(SubtitleTimelineEdit.move(
                                draftStart,draftEnd,delta,total)) },onFinish=::commitDraft,
                            onTap={onSelect(cue.id)}) {
                            Text(cue.text,maxLines=1,overflow=TextOverflow.Ellipsis,fontSize=10.sp,
                                color=Color.White,modifier=Modifier.align(Alignment.Center)
                                    .padding(horizontal=2.dp))
                        }
                        AudioDragSurface(Modifier.width(18.dp).fillMaxHeight()
                            .background(Color.Black.copy(alpha=.22f)),total,widthPx,
                            onStep={ delta -> updateDraft(SubtitleTimelineEdit.trimRight(
                                draftStart,draftEnd,delta,total)) },onFinish=::commitDraft,
                            onTap={onSelect(cue.id)}) {
                            Text("|",color=Color.White,modifier=Modifier.align(Alignment.Center))
                        }
                    }
                }
            }
            if(cue.id==selectedId) {
                Row(Modifier.fillMaxWidth().padding(start=66.dp),
                    horizontalArrangement=Arrangement.SpaceBetween) {
                    Text("Início ${formatPipMs(draftStart)}",fontSize=10.sp)
                    Text("Fim ${formatPipMs(draftEnd)}",fontSize=10.sp)
                }
            }
        }
    }
}

/** Todas as faixas usam 12dp de margem, rótulo de 60dp e separação de 6dp. */
@Composable
internal fun TimelineTimedRow(label: String, color: Color,
    entries: List<Triple<String,Long,Long>>, total: Long, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(34.dp).padding(horizontal=12.dp,vertical=2.dp),
        verticalAlignment=Alignment.CenterVertically) {
        Box(Modifier.width(60.dp).fillMaxHeight().background(color,RoundedCornerShape(3.dp)),
            contentAlignment=Alignment.Center) {Text(label,color=Color.White,fontSize=11.sp)}
        Spacer(Modifier.width(6.dp))
        BoxWithConstraints(Modifier.weight(1f).fillMaxHeight()
            .background(color.copy(alpha=.14f),RoundedCornerShape(3.dp))) {
            entries.forEach { (name,start,length) ->
                val x=(start.toFloat()/total.coerceAtLeast(1L)).coerceIn(0f,1f)
                val width=(length.toFloat()/total.coerceAtLeast(1L)).coerceIn(0f,1f-x)
                Box(Modifier.offset(x=maxWidth*x).width(maxWidth*width).fillMaxHeight()
                    .background(color.copy(alpha=.48f),RoundedCornerShape(2.dp))
                    .clickable(onClick=onClick),contentAlignment=Alignment.Center) {
                    Text(name,maxLines=1,overflow=TextOverflow.Ellipsis,fontSize=10.sp)
                }
            }
        }
    }
}

@Composable
internal fun OverlayTimeline(overlays: List<Overlay>, total: Long, onClick: (String) -> Unit) {
    Column(verticalArrangement=Arrangement.spacedBy(2.dp)) {
        if (overlays.isEmpty()) {
            TimelineRow("PiP",Purple, emptyList(),total,0L,"", onClip = { })
        } else overlays.forEachIndexed { index,layer ->
            Row(Modifier.fillMaxWidth().height(34.dp).padding(horizontal=12.dp,vertical=2.dp),
                verticalAlignment=Alignment.CenterVertically) {
                Box(Modifier.width(60.dp).fillMaxHeight().clip(RoundedCornerShape(3.dp))
                    .background(Purple),contentAlignment=Alignment.Center) {
                    Text("PiP ${index+1}",color=Color.White,fontSize=11.sp)
                }
                Spacer(Modifier.width(6.dp))
                BoxWithConstraints(Modifier.weight(1f).fillMaxHeight()
                    .background(Purple.copy(alpha=.14f))) {
                    val start = (layer.startAtMs.toFloat()/total.coerceAtLeast(1L)).coerceIn(0f,1f)
                    val length = (layer.durationMs.toFloat()/total.coerceAtLeast(1L)).coerceIn(0f,1f-start)
                    Box(Modifier.offset(x=maxWidth*start).width(maxWidth*length)
                        .fillMaxHeight().background(Purple.copy(alpha=.45f))
                        .clickable {onClick(layer.id)},contentAlignment=Alignment.Center) {
                        Text(if(layer.isImage)"Imagem" else "Vídeo",fontSize=10.sp,maxLines=1)
                    }
                }
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
internal fun PipLayer(pip: Overlay, timeMs: Long, playing: Boolean,
                     speed: Float = 1f, editable: Boolean = true, onUpdate: (Overlay) -> Unit) {
    val context = LocalContext.current
    val sourceAspect = remember(pip.uri, pip.isImage) { mediaAspect(context, pip.uri, pip.isImage) }
    val pipPlayer = remember(pip.uri) { if (!pip.isImage) diagnosticPlayer(context) else null }
    DisposableEffect(pipPlayer) { onDispose { pipPlayer?.release() } }
    LaunchedEffect(pip.uri) {
        pipPlayer?.apply { setMediaItem(MediaItem.fromUri(pip.uri)); prepare() }
    }
    LaunchedEffect(playing, timeMs, pip.volume, pip.muted, speed) {
        pipPlayer?.apply {
            setPlaybackParameters(PlaybackParameters(speed))
            volume = if (pip.muted) 0f else pip.volume
            if (kotlin.math.abs(currentPosition - timeMs) > 400L) seekTo(timeMs.coerceAtLeast(0L))
            if (playing) play() else pause()
        }
    }
    val currentPip by rememberUpdatedState(pip)
    val currentUpdate by rememberUpdatedState(onUpdate)
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val canvasW = constraints.maxWidth.coerceAtLeast(1)
        val canvasH = constraints.maxHeight.coerceAtLeast(1)
        val (wFrac, hFrac) = OverlayEditorLogic.dimensions(pip.scale, sourceAspect,
            canvasW.toFloat() / canvasH)
        val centerX = OverlayEditorLogic.clampCenter(pip.x, wFrac)
        val centerY = OverlayEditorLogic.clampCenter(pip.y, hFrac)
        val pipWidth = maxWidth * wFrac
        val pipHeight = maxHeight * hFrac
        if (editable && kotlin.math.abs(centerX - 0.5f) < 0.012f) {
            Box(Modifier.align(Alignment.Center).width(1.dp).fillMaxHeight()
                .background(Blue.copy(alpha = 0.6f)))
        }
        if (editable && kotlin.math.abs(centerY - 0.5f) < 0.012f) {
            Box(Modifier.align(Alignment.Center).height(1.dp).fillMaxWidth()
                .background(Blue.copy(alpha = 0.6f)))
        }
        Box(
            Modifier.offset(x = maxWidth * (centerX - wFrac / 2f),
                            y = maxHeight * (centerY - hFrac / 2f))
                .width(pipWidth).height(pipHeight)
                .then(if (editable) Modifier.border(2.dp, Color(0xFF5796FF), RoundedCornerShape(6.dp))
                    else Modifier)
                .then(if (editable) Modifier.pointerInput(pip.uri) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val live = currentPip
                        val (wf, hf) = OverlayEditorLogic.dimensions(
                            (live.scale * zoom).coerceIn(.05f, .90f), sourceAspect,
                            canvasW.toFloat() / canvasH)
                        currentUpdate(live.copy(
                            scale = (live.scale * zoom).coerceIn(.05f, .90f),
                            x = OverlayEditorLogic.clampCenter(live.x + pan.x / canvasW, wf),
                            y = OverlayEditorLogic.clampCenter(live.y + pan.y / canvasH, hf)
                        ))
                    }
                } else Modifier)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.Transparent)
        ) {
            if (pip.isImage) {
                val bitmap = remember(pip.uri) {
                    runCatching {
                        val options = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        context.contentResolver.openInputStream(Uri.parse(pip.uri))?.use {
                            android.graphics.BitmapFactory.decodeStream(it, null, options)
                        }
                        var sample = 1
                        while (options.outWidth / sample > 1400 || options.outHeight / sample > 1400) {
                            sample *= 2
                        }
                        options.inJustDecodeBounds = false
                        options.inSampleSize = sample
                        context.contentResolver.openInputStream(Uri.parse(pip.uri))?.use {
                            android.graphics.BitmapFactory.decodeStream(it, null, options)
                        }
                    }.getOrNull()
                }
                // FIT + proporção original evita cortar letras ou bordas de logotipos.
                if (bitmap != null) Image(bitmap.asImageBitmap(), "Imagem sobreposta",
                    Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            } else {
                AndroidView(factory = { previewPlayer(it, transparentShutter = true).apply {
                    player = pipPlayer; useController = false
                } }, modifier = Modifier.fillMaxSize(), update = { it.player = pipPlayer })
            }
        }
    }
}

/** Entrada numérica com máscara visual. O valor editado guarda só dígitos.
 * Assim apagar não fica preso no ':' ou no '.', e a digitação é sempre do fim. */
internal object NumericTimeMask : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val output = NumericTime.format(text.text)
        val length = text.length
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int =
                if (offset <= 0 && length == 0) 0 else output.length
            override fun transformedToOriginal(offset: Int): Int = length
        }
        return TransformedText(AnnotatedString(output), mapping)
    }
}

/** Campo numérico compartilhado, com a opção de aplicar cada valor válido imediatamente.
 * Na prévia, não exige botão de confirmar; o player é reposicionado a cada digitação.
 * No PiP, mantém os botões de confirmar/limpar existentes. */
@Composable
internal fun TimeDigitsField(label: String, timeMs: Long, totalMs: Long,
    valid: (Long) -> Boolean = { it in 0L..totalMs },
    onCommit: (Long) -> Unit, modifier: Modifier = Modifier,
    autoApply: Boolean = false, compact: Boolean = false) {
    var digits by remember { mutableStateOf(NumericTime.fromMs(timeMs)) }
    var editing by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    LaunchedEffect(timeMs) { if (!editing) digits = NumericTime.fromMs(timeMs) }
    val parsed = NumericTime.toMs(digits)
    val allowed = parsed != null && valid(parsed)
    Row(modifier, verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        OutlinedTextField(
            value = digits,
            onValueChange = { typed ->
                digits = NumericTime.sanitize(typed)
                editing = true
                if (autoApply) {
                    NumericTime.toMs(digits)?.takeIf(valid)?.let(onCommit)
                }
            },
            visualTransformation = NumericTimeMask,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                if(compact && allowed) onCommit(parsed!!)
                editing = false
                focusManager.clearFocus()
            }),
            modifier = Modifier.weight(1f).onFocusChanged { state ->
                if (state.isFocused && !editing) {
                    digits = "" // uma nova digitação começa do zero, sem precisar apagar separadores
                    editing = true
                } else if (!state.isFocused && editing) {
                    editing = false
                    if(compact && allowed) onCommit(parsed!!)
                    // Entradas inválidas não substituem o último horário válido.
                    digits = NumericTime.fromMs(timeMs)
                }
            },
            label = { Text(label) }, singleLine = true, isError = digits.isNotBlank() && !allowed,
            supportingText = if (!compact && digits.isNotBlank() && !allowed && !autoApply) {
                { Text("Fora do intervalo") }
            } else null
        )
        if (!autoApply && !compact) {
            IconButton(onClick = { digits = ""; editing = true }) {
                Icon(Icons.Default.Backspace, "Limpar $label")
            }
            IconButton(onClick = {
                if (allowed) {
                    onCommit(parsed!!)
                    editing = false
                    focusManager.clearFocus()
                }
            }, enabled = allowed) {
                Icon(Icons.Default.Check, "Aplicar $label")
            }
        }
    }
}

/** Velocidade somente da prévia; não modifica cortes ou velocidade do MP4 exportado. */
@Composable
internal fun PlaybackSpeedButton(speed: Float, onSelect: (Float) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true },
            modifier = Modifier.height(38.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            shape = RoundedCornerShape(11.dp)) {
            Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(3.dp))
            Text(PreviewPlayback.label(speed), fontSize = 12.sp, maxLines = 1)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Text("Velocidade da prévia", modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelMedium)
            PreviewPlayback.SPEEDS.forEach { option ->
                DropdownMenuItem(
                    text = { Text(PreviewPlayback.label(option)) },
                    leadingIcon = if (option == speed) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    } else null,
                    onClick = { onSelect(option); expanded = false }
                )
            }
        }
    }
}

@Composable
internal fun TrimQuickAction(icon: ImageVector, label: String, enabled: Boolean,
    modifier: Modifier = Modifier, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, enabled = enabled,
        modifier = modifier.height(58.dp), shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(1.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(18.dp))
            Text(label, fontSize = 10.sp, maxLines = 1, softWrap = false)
        }
    }
}

/** Frames pequenos gerados em IO somente quando muda o clipe; evita decodificar a cada recomposição. */
@Composable
internal fun TrimThumbnails(uri: String, durationMs: Long, onSeek: (Long) -> Unit) {
    val context = LocalContext.current
    val thumbs by produceState<List<ImageBitmap>>(initialValue = emptyList(), uri, durationMs) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, Uri.parse(uri))
                    (0 until 6).mapNotNull { step ->
                        val microseconds = (durationMs.coerceAtLeast(1L) * step / 6L) * 1000L
                        val frame = if (Build.VERSION.SDK_INT >= 27) {
                            retriever.getScaledFrameAtTime(microseconds,
                                MediaMetadataRetriever.OPTION_CLOSEST_SYNC, 144, 88)
                        } else retriever.getFrameAtTime(microseconds,
                            MediaMetadataRetriever.OPTION_CLOSEST_SYNC)?.let { original ->
                            val scaled = android.graphics.Bitmap.createScaledBitmap(original, 144, 88, true)
                            if (scaled !== original) original.recycle()
                            scaled
                        }
                        frame?.asImageBitmap()
                    }
                } finally { retriever.release() }
            }.getOrDefault(emptyList())
        }
    }
    Row(Modifier.fillMaxWidth().height(60.dp).clip(RoundedCornerShape(9.dp))
        .background(MaterialTheme.colorScheme.surfaceVariant)) {
        repeat(6) { index ->
            Box(Modifier.weight(1f).fillMaxHeight().clickable {
                onSeek(durationMs.coerceAtLeast(0L) * (index * 2L + 1L) / 12L)
            }) {
                thumbs.getOrNull(index)?.let { bitmap ->
                    Image(bitmap, "Quadro ${index + 1}", Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop)
                }
            }
        }
    }
}

/** Um quadro por trecho visível, carregado sob demanda para economizar memória. */
@Composable
internal fun TrimSegmentFrame(clip: Clip) {
    val context = LocalContext.current
    val frame by produceState<ImageBitmap?>(initialValue = null,
        clip.uri, clip.startMs, clip.endMs) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, Uri.parse(clip.uri))
                    val offsetMs = minOf(clip.durationMs / 2L, 1500L)
                    val frameMs = (clip.startMs + offsetMs).coerceIn(
                        0L, (clip.sourceDurationMs - 1L).coerceAtLeast(0L))
                    val bitmap = if (Build.VERSION.SDK_INT >= 27) {
                        retriever.getScaledFrameAtTime(frameMs * 1000L,
                            MediaMetadataRetriever.OPTION_CLOSEST_SYNC, 200, 110)
                    } else {
                        retriever.getFrameAtTime(frameMs * 1000L,
                            MediaMetadataRetriever.OPTION_CLOSEST_SYNC)?.let { original ->
                            val scaled = android.graphics.Bitmap.createScaledBitmap(original, 200, 110, true)
                            if (scaled !== original) original.recycle()
                            scaled
                        }
                    }
                    bitmap?.asImageBitmap()
                } finally { retriever.release() }
            }.getOrNull()
        }
    }
    Box(Modifier.fillMaxWidth().height(58.dp).background(Color(0xFF1D1D23)),
        contentAlignment = Alignment.Center) {
        if (frame != null) Image(frame!!, "Miniatura do trecho", Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop)
        else Icon(Icons.Default.Movie, "Trecho de vídeo", tint = Color.LightGray)
    }
}

/** Trilha de pedaços: selecione tocando, sem precisar importar a mesma mídia novamente. */
@Composable
internal fun TrimSegmentStrip(p: VideoProject, selectedId: String, onSelect: (String) -> Unit) {
    val scroll = rememberLazyListState()
    val selectedIndex = p.clips.indexOfFirst { it.id == selectedId }
    LaunchedEffect(p.id, selectedId, selectedIndex) {
        if (selectedIndex >= 0) scroll.animateScrollToItem(selectedIndex)
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
        Text("Trechos separados (${p.clips.size})", fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.bodyMedium)
        Text("Deslize para o lado →", fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    LazyRow(state = scroll, horizontalArrangement = Arrangement.spacedBy(7.dp),
        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 3.dp),
        modifier = Modifier.fillMaxWidth()) {
        items(p.clips, key = { it.id }) { item ->
            val isSelected = item.id == selectedId
            Surface(onClick = { onSelect(item.id) },
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(if (isSelected) 2.dp else 1.dp,
                    if (isSelected) Blue else MaterialTheme.colorScheme.outlineVariant),
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.width(120.dp)) {
                Column {
                    TrimSegmentFrame(item)
                    Column(Modifier.padding(horizontal = 5.dp, vertical = 3.dp)) {
                        val part = p.clips.indexOfFirst { it.id == item.id } + 1
                        Text("Parte $part${if (isSelected) " · selecionada" else ""}",
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 10.sp, maxLines = 1)
                        Text("${formatPipMs(item.startMs)}–${formatPipMs(item.endMs)}",
                            fontSize = 9.sp, maxLines = 1)
                        Text("Duração: ${formatPipMs(item.durationMs)}",
                            fontSize = 9.sp, maxLines = 1)
                    }
                }
            }
        }
    }
}

