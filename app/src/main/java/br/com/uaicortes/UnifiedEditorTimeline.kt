package br.com.uaicortes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Uma única escala e um único cursor para vídeo, imagens, PiP, áudio, voz e legendas.
 * Fotografias de capa são clipes PRINCIPAIS; a faixa Imagem é uma representação
 * separada dos mesmos clipes, nunca uma segunda sobreposição na exportação.
 */
internal data class TimelineVisualSpan(val id: String, val title: String,
    val startMs: Long, val durationMs: Long)

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
            durationMs.coerceAtLeast(1L).toDouble()).toFloat().coerceIn(0f, 1f)
    fun position(fraction: Float, durationMs: Long): Long =
        (fraction.coerceIn(0f, 1f) * durationMs.coerceAtLeast(0L)).toLong()
}

@Composable
private fun UnifiedLane(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String,
    tint: Color, entries: List<TimelineVisualSpan>, durationMs: Long,
    onSelect: (String) -> Unit, onOpen: () -> Unit,
    onMove: ((String, Long) -> Unit)? = null,
    canMove: (String) -> Boolean = { true },
    transitions: List<Clip> = emptyList(), onTransition: (Int) -> Unit = {}) {
    Row(Modifier.fillMaxWidth().height(38.dp).padding(vertical=2.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(width=42.dp,height=34.dp).background(tint, RoundedCornerShape(8.dp))
            .clickable(onClick=onOpen).semantics { contentDescription = description },
            contentAlignment=Alignment.Center) {
            Icon(icon, description, tint=Color.White, modifier=Modifier.size(20.dp))
        }
        Spacer(Modifier.width(6.dp))
        BoxWithConstraints(Modifier.weight(1f).fillMaxHeight()
            .clip(RoundedCornerShape(6.dp)).background(tint.copy(alpha=.13f))) {
            val width=maxWidth
            val total=durationMs.coerceAtLeast(1L)
            val widthPx=with(LocalDensity.current) { width.toPx() }
            entries.forEach { span ->
                key(span.id) {
                    var previewStart by remember(span.id,span.startMs) { mutableLongStateOf(span.startMs) }
                    val start=(previewStart.toDouble()/total).toFloat().coerceIn(0f,1f)
                    val end=((previewStart+span.durationMs).toDouble()/total).toFloat().coerceIn(start,1f)
                    val spanWidth=(width*(end-start)).coerceAtLeast(3.dp).coerceAtMost(width)
                    val x=(width*start).coerceAtMost(width-spanWidth)
                    val visual=Modifier.offset(x=x).width(spanWidth).fillMaxHeight().padding(end=1.dp)
                        .background(tint.copy(alpha=.70f),RoundedCornerShape(4.dp))
                    if (onMove!=null && canMove(span.id)) {
                        AudioDragSurface(visual,total,widthPx,
                            onStep={ delta -> previewStart=(previewStart+delta)
                                .coerceIn(0L,(total-span.durationMs).coerceAtLeast(0L)) },
                            onFinish={ if(previewStart!=span.startMs) onMove(span.id,previewStart) },
                            onTap={onSelect(span.id)}) {
                            Text(span.title,fontSize=9.sp,color=Color.White,maxLines=1,
                                overflow=TextOverflow.Ellipsis,modifier=Modifier.align(Alignment.Center)
                                    .padding(horizontal=3.dp))
                        }
                    } else {
                        Box(visual.clickable { onSelect(span.id) },contentAlignment=Alignment.Center) {
                            Text(span.title, fontSize=9.sp,color=Color.White,maxLines=1,
                                overflow=TextOverflow.Ellipsis,modifier=Modifier.padding(horizontal=3.dp))
                        }
                    }
                }
            }
            if (transitions.size > 1) {
                var elapsed=0L
                transitions.dropLast(1).forEachIndexed { index, clip ->
                    elapsed+=clip.durationMs
                    val x=width*(elapsed.toDouble()/total).toFloat().coerceIn(0f,1f)
                    Box(Modifier.offset(x=x-9.dp).width(18.dp).fillMaxHeight()
                        .background(MaterialTheme.colorScheme.primary,RoundedCornerShape(5.dp))
                        .clickable { onTransition(index) }, contentAlignment=Alignment.Center) {
                        Icon(Icons.Default.AutoAwesomeMotion,"Transição ${index+1}",
                            tint=MaterialTheme.colorScheme.onPrimary,modifier=Modifier.size(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
internal fun UnifiedEditorTimeline(
    project: VideoProject, positionMs: Long,
    onSeek: (Long) -> Unit, onClip: (String) -> Unit, onTransition: (Int) -> Unit,
    onOverlay: (String) -> Unit, onAudio: (String) -> Unit,
    onAddVideo: () -> Unit, onAddImage: () -> Unit, onOpenPiP: () -> Unit,
    onOpenAudio: () -> Unit, onOpenVoice: () -> Unit,
    onSubtitle: (String) -> Unit, onOpenSubtitle: () -> Unit,
    onMoveAudio: (String, Long) -> Unit
) {
    val total=project.durationMs.coerceAtLeast(1L)
    val video = remember(project.clips) { UnifiedTimelinePolicy.mainSpans(project.clips, false) }
    val images = remember(project.clips) { UnifiedTimelinePolicy.mainSpans(project.clips, true) }
    val overlays = project.overlays.map { TimelineVisualSpan(it.id,
        if (it.isImage) "Imagem PiP" else "PiP", it.startAtMs, it.durationMs) }
    val resolved = ClipAudioBindings.resolved(project)
    val voice = resolved.filter { it.kind == VoiceIaPolicy.KIND }
    val audio = resolved.filter { it.kind != VoiceIaPolicy.KIND }
    val audioSpans = audio.map { TimelineVisualSpan(it.id,it.title,it.startMs,it.durationMs) } +
        (project.music?.let { listOf(TimelineVisualSpan("music",it.title,0L,total)) } ?: emptyList())
    val voiceSpans = voice.map { TimelineVisualSpan(it.id,it.title,it.startMs,it.durationMs) }
    val subtitles = project.subtitles.map { TimelineVisualSpan(it.id,it.text,
        it.startMs,(it.endMs-it.startMs).coerceAtLeast(0L)) }
    val iconLabel=42.dp; val laneGap=6.dp
    Column(Modifier.fillMaxWidth().padding(horizontal=12.dp).clip(RoundedCornerShape(12.dp))
        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.25f)).padding(3.dp)) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val contentWidth = maxWidth
            val rulerWidth = (contentWidth-iconLabel-laneGap).coerceAtLeast(1.dp)
            val fraction = UnifiedTimelinePolicy.cursorFraction(positionMs,total)
            val cursorX = iconLabel+laneGap+rulerWidth*fraction
            val density=LocalDensity.current
            val rulerWidthPx=with(density) { rulerWidth.toPx() }
            Column(Modifier.fillMaxWidth()) {
                Row(Modifier.height(28.dp),verticalAlignment=Alignment.CenterVertically) {
                    Text("◁",modifier=Modifier.width(iconLabel),fontSize=12.sp)
                    Spacer(Modifier.width(laneGap))
                    Box(Modifier.weight(1f).fillMaxHeight()
                        .pointerInput(total,rulerWidthPx) {
                            detectTapGestures { offset ->
                                onSeek(UnifiedTimelinePolicy.position(offset.x / rulerWidthPx,total))
                            }
                        }.pointerInput(total,rulerWidthPx) {
                            detectDragGestures { change,_ ->
                                change.consume()
                                onSeek(UnifiedTimelinePolicy.position(change.position.x/rulerWidthPx,total))
                            }
                        }, contentAlignment=Alignment.CenterStart) {
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                            for (part in 0..4) Text(formatPipMs(total*part/4),fontSize=9.sp,
                                color=MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                UnifiedLane(Icons.Default.Movie,"Vídeos",Blue,video,total,onClip,onAddVideo,
                    transitions=project.clips,onTransition=onTransition)
                UnifiedLane(Icons.Default.Image,"Imagens",Color(0xFF9877CB),images,total,onClip,onAddImage)
                UnifiedLane(Icons.Default.Layers,"PiP",Purple,overlays,total,onOverlay,onOpenPiP)
                // O gesto de deslocar áudio fica disponível na tela Áudio; os blocos
                // compartilham a mesma escala aqui e não mudam o projeto por acidente.
                UnifiedLane(Icons.Default.MusicNote,"Áudio",Green,audioSpans,total,
                    { if (it=="music") onOpenAudio() else onAudio(it) },onOpenAudio,
                    onMove=onMoveAudio,canMove={ id -> id!="music" &&
                        audio.firstOrNull { it.id==id }?.clipId==null })
                UnifiedLane(Icons.Default.RecordVoiceOver,"Vozes",Purple,voiceSpans,total,
                    onAudio,onOpenVoice,onMove=onMoveAudio,
                    canMove={id -> voice.firstOrNull {it.id==id}?.clipId==null})
                UnifiedLane(Icons.Default.ClosedCaption,"Legendas",Color(0xFFBE8B36),
                    subtitles,total,onSubtitle,onOpenSubtitle)
            }
            // Sem hitbox: a barra segue a reprodução e não bloqueia tocar nas faixas.
            Box(Modifier.offset(x=cursorX-1.dp,y=28.dp).width(2.dp)
                .height(38.dp*6).background(Color(0xFF146BFF)))
            Box(Modifier.offset(x=cursorX-5.dp,y=21.dp).size(10.dp)
                .background(Color(0xFF146BFF),RoundedCornerShape(3.dp)))
        }
    }
}
