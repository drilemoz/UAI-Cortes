package br.com.uaicortes

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackParameters
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.delay
import java.util.UUID

// Extraído da MainActivity na v0.1.53 para reutilização de componentes.

@OptIn(UnstableApi::class)
@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditorScreen(p:VideoProject, selectedId:String, currentMs:Long,
    onSelected:(String)->Unit,onPosition:(Long)->Unit,onBack:()->Unit,onEdit:(String)->Unit,
    onUpdate:(VideoProject)->Unit,onAddVideo:()->Unit,onPiP:()->Unit,onAudio:()->Unit,
    onSelectOverlay:(String)->Unit,onTransition:(Int)->Unit,onSaveHome:()->Unit,
    onSelectAudio:(String)->Unit,onSelectSubtitle:(String)->Unit) {
    val context=LocalContext.current
    val playbackVm: EditorPlaybackViewModel = viewModel()
    LaunchedEffect(p.id) { playbackVm.selectProject(p.id) }
    val playbackSpeed = playbackVm.speed
    val player=remember(p.id) { diagnosticPlayer(context).apply { repeatMode = Player.REPEAT_MODE_ALL } }
    var isPlaying by remember(player) { mutableStateOf(player.isPlaying) }
    var restoringMedia by remember(player) { mutableStateOf(true) }
    var scrubbing by remember(player) { mutableStateOf(false) }
    val latestPosition by rememberUpdatedState(currentMs)
    var clipMenuId by remember(p.id) { mutableStateOf<String?>(null) }
    var deletionRequested by remember(p.id) { mutableStateOf<String?>(null) }
    var feedback by remember(p.id) { mutableStateOf("") }
    LaunchedEffect(player, p.clips.map { it.id to it.volume }) {
        while (true) {
            player.volume = p.clips.getOrNull(player.currentMediaItemIndex)?.volume?.coerceIn(0f,1f) ?: 1f
            delay(120)
        }
    }
    LaunchedEffect(player, playbackSpeed) { player.setPlaybackParameters(PlaybackParameters(playbackSpeed)) }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener); player.release() }
    }
    LaunchedEffect(player, p.clips.map { Triple(it.uri, it.startMs, it.endMs) }) {
        restoringMedia = true
        player.setMediaItems(p.clips.map { clip ->
            MediaItem.Builder().setUri(clip.uri).setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder().setStartPositionMs(clip.startMs)
                    .setEndPositionMs(clip.endMs).build()).build()
        })
        player.prepare()
        // Restaura o playhead após reconfiguração/retorno à tela; não sobrescrever
        // o tempo salvo com o zero inicial do player durante o prepare().
        seekProjectPosition(player, p.clips, latestPosition)
        restoringMedia = false
    }
    LaunchedEffect(player, p.clips.map { it.id to it.durationMs }) {
        while (true) {
            if (!restoringMedia && !scrubbing && p.clips.isNotEmpty()) {
                val next = projectPosition(player, p.clips, p.durationMs)
                if (next != latestPosition) onPosition(next)
            }
            delay(200)
        }
    }
    fun seekGlobal(ms: Long) {
        val target = ms.coerceIn(0L, p.durationMs.coerceAtLeast(0L))
        seekProjectPosition(player, p.clips, target)
        onPosition(target)
    }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().height(66.dp).padding(horizontal=8.dp),verticalAlignment=Alignment.CenterVertically) {
            IconButton(onClick=onBack){Icon(Icons.Default.ArrowBack,"Voltar")}
            Column(Modifier.weight(1f),horizontalAlignment=Alignment.CenterHorizontally) {
                Text(p.title,fontWeight=FontWeight.Medium,maxLines=1,overflow=TextOverflow.Ellipsis)
                Text("Projeto em edição",style=MaterialTheme.typography.bodySmall,
                    color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            BlueButton("Exportar",{ onEdit("export") },Modifier.width(102.dp),enabled=p.clips.isNotEmpty())
        }
        // Separate audio players track the editor playhead without modifying source media.
        p.music?.let { music ->
            TrackAudioPreview(music.uri, 0L, p.durationMs, music.volume, currentMs,
                isPlaying, playbackSpeed, true)
        }
        p.audioTracks.forEach { audio ->
            key(audio.id) {
                TrackAudioPreview(audio.uri, audio.startMs, audio.durationMs, audio.volume,
                    currentMs, isPlaying, playbackSpeed, false, audio.sourceStartMs)
            }
        }
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)
            .background(Color(0xFF101014)).padding(10.dp), contentAlignment = Alignment.Center) {
            val activeIndex=AdaptiveVideoLayout.activeClipIndex(p.clips.map { it.durationMs },currentMs)
            val previewUri=p.clips.getOrNull(activeIndex)?.uri
            val actualAspect=remember(previewUri) {
                previewUri?.let { mediaAspect(context,it,false) } ?: 16f/9f
            }
            val fit=AdaptiveVideoLayout.fit(maxWidth.value,maxHeight.value,actualAspect)
            val videoW=fit.width.dp
            val videoH=fit.height.dp
            Box(Modifier.width(videoW).height(videoH)) {
                AndroidView(factory = { ctx -> previewPlayer(ctx).apply {
                    this.player = player
                    useController = false
                } }, update = { it.player = player }, modifier = Modifier.fillMaxSize())
                // Fade preview uses the same boundary IDs and durations as the exporter.
                val index = p.clips.indexOfFirst { clip ->
                    val first = p.clips.takeWhile { it.id != clip.id }.sumOf { it.durationMs }
                    currentMs in first until (first + clip.durationMs)
                }.coerceAtLeast(0)
                p.clips.getOrNull(index)?.let { clip ->
                    val before = p.clips.take(index).sumOf { it.durationMs }
                    val incoming = EditFeatures.boundary(p, index - 1)
                    val outgoing = EditFeatures.boundary(p, index)
                    val inMs = EditFeatures.transitionMs(incoming,
                        p.clips.getOrElse(index-1) { clip }.durationMs, clip.durationMs)
                    val outMs = EditFeatures.transitionMs(outgoing, clip.durationMs,
                        p.clips.getOrElse(index+1) { clip }.durationMs)
                    val local = (currentMs - before).coerceIn(0L,clip.durationMs)
                    TransitionPreviewTint(incoming, inMs, outgoing, outMs,
                        local, clip.durationMs)
                }
                p.overlays.forEach { pip ->
                    if (currentMs >= pip.startAtMs && currentMs < pip.startAtMs + pip.durationMs) {
                        key(pip.id) {
                            PipLayer(pip, currentMs - pip.startAtMs, isPlaying,
                                speed = playbackSpeed, editable = false) { }
                        }
                    }
                }
                p.subtitles.firstOrNull { currentMs >= it.startMs && currentMs < it.endMs }?.let { cue ->
                    SubtitleOverlayOnFrame(cue, editable = false, playbackMs=currentMs)
                }
                p.textWatermark?.toSubtitle(p.durationMs)?.let { watermark ->
                    SubtitleOverlayOnFrame(watermark, editable = false, playbackMs=currentMs)
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=6.dp),horizontalArrangement=Arrangement.Center,
            verticalAlignment=Alignment.CenterVertically) {
            IconButton(onClick={seekGlobal(TimelinePrecision.step(currentMs, -5000L, p.durationMs))}) { Icon(Icons.Default.SkipPrevious,"Voltar cinco segundos") }
            IconButton(onClick={if(isPlaying)player.pause() else player.play()}) {
                Icon(if(isPlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircle, "Reproduzir / pausar",
                    modifier=Modifier.size(35.dp))
            }
            IconButton(onClick={seekGlobal(TimelinePrecision.step(currentMs, 5000L, p.durationMs))}) { Icon(Icons.Default.SkipNext,"Avançar cinco segundos") }
            PlaybackSpeedButton(playbackSpeed, onSelect = playbackVm::updateSpeed)
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text("Vídeo:", style = MaterialTheme.typography.bodySmall)
            EditableSeekTime(currentMs, p.durationMs, onSeek = { ms ->
                seekGlobal(ms)
            }, modifier = Modifier.weight(1f))
        }
        // Passos temporais explícitos evitam tentar acertar um quadro em uma barra curta.
        Row(Modifier.fillMaxWidth().padding(horizontal=16.dp),
            verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(5.dp)) {
            Text("Ajuste fino",fontSize=11.sp,modifier=Modifier.weight(1f))
            listOf(-1000L to "−1s", -100L to "−0,1s",
                100L to "+0,1s", 1000L to "+1s").forEach { (delta,label) ->
                OutlinedButton(onClick={
                    val dest=TimelinePrecision.step(currentMs,delta,p.durationMs)
                    seekGlobal(dest)
                },contentPadding=PaddingValues(horizontal=7.dp,vertical=2.dp),
                    modifier=Modifier.height(32.dp),enabled=p.durationMs>0) {
                    Text(label,fontSize=11.sp)
                }
            }
        }
        if(p.durationMs>0) Slider(value=currentMs.toFloat().coerceIn(0f,p.durationMs.toFloat()),
            onValueChange={global ->
                scrubbing = true
                seekGlobal(global.toLong())
            },
            onValueChangeFinished = { scrubbing = false },
            valueRange=0f..p.durationMs.toFloat(),modifier=Modifier.fillMaxWidth().padding(horizontal=16.dp))
        TimelineRow("Vídeo",Blue,p.clips.map{ it.title to it.durationMs },p.durationMs,currentMs,selectedId,
            onClip={ id -> onSelected(p.clips[id].id); clipMenuId = p.clips[id].id },
            onBoundary=onTransition)
        OverlayTimeline(p.overlays, p.durationMs, onClick = { id -> onSelectOverlay(id); onEdit("pip") })
        InteractiveAudioTimeline(p, onSelectTrack = onSelectAudio,
            onMoveTrack = { trackId, start ->
                onUpdate(p.copy(audioTracks = p.audioTracks.map { audio ->
                    if (audio.id == trackId) audio.copy(startMs = start) else audio
                }))
                feedback = "Posição da faixa atualizada."
            }, onLegacyClick = { onEdit("audio") })
        SubtitleOverviewTimeline(p.subtitles,p.durationMs,onSelectSubtitle,
            onOpen={onEdit("subtitles")})
        Spacer(Modifier.height(5.dp));HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
        // Ferramentas em carrossel circular: salvar é um cartão, não um botão solto.
        // Começamos no centro de Int.MAX_VALUE para permitir deslizar nas duas direções.
        val actions = remember { listOf(
            Triple("Salvar", Icons.Default.Save, "save"),
            Triple("Cortar", Icons.Default.ContentCut, "trim"),
            Triple("Unir", Icons.Default.Add, "add"),
            Triple("PiP", Icons.Default.Layers, "pip"),
            Triple("Áudio", Icons.Default.MusicNote, "audio"),
            Triple("Transição", Icons.Default.AutoAwesomeMotion, "transition"),
            Triple("Legendas", Icons.Default.ClosedCaption, "subtitles"),
            Triple("Marca d'água", Icons.Default.TextFields, "watermark"),
            Triple("Animar Imagens", Icons.Default.AutoAwesomeMotion, "animate")
        ) }
        val carouselStart = (Int.MAX_VALUE / 2 / actions.size) * actions.size
        val toolsScroll = rememberLazyListState(initialFirstVisibleItemIndex = carouselStart)
        LazyRow(state = toolsScroll, modifier = Modifier.fillMaxWidth().navigationBarsPadding()
            .padding(vertical = 10.dp), contentPadding = PaddingValues(horizontal = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            items(count = Int.MAX_VALUE) { index ->
                val (name, icon, key) = actions[index % actions.size]
                Column(Modifier.width(66.dp).clip(RoundedCornerShape(12.dp)).clickable {
                    when (key) {
                        "save" -> onSaveHome()
                        "add" -> onAddVideo()
                        "pip" -> onPiP()
                        "audio" -> onEdit("audio")
                        else -> onEdit(key)
                    }
                }, horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(52.dp).clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                        Icon(icon, name, modifier = Modifier.size(22.dp))
                    }
                    Text(name, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                }
            }
        }
        if (feedback.isNotBlank()) Text(feedback, modifier=Modifier.padding(horizontal=12.dp,vertical=3.dp),
            color=MaterialTheme.colorScheme.primary, style=MaterialTheme.typography.bodySmall)
    }
    val active = p.clips.firstOrNull { it.id == clipMenuId }
    if (active != null) {
        ModalBottomSheet(onDismissRequest={clipMenuId=null}, dragHandle=null,
            sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true)) {
            Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
                verticalArrangement=Arrangement.spacedBy(9.dp)) {
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Editar clipe",fontWeight=FontWeight.Bold,fontSize=19.sp)
                        Text(active.title, maxLines=1,overflow=TextOverflow.Ellipsis,
                            style=MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick={clipMenuId=null}) {Icon(Icons.Default.Close,"Fechar")}
                }
                val idx=p.clips.indexOfFirst { it.id == active.id }
                fun commit(clips: List<Clip>, note: String) {
                    onUpdate(EditFeatures.repositionAfterDelete(p,clips))
                    feedback=note
                    clipMenuId=null
                }
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                    ClipSheetAction("Cortar",Icons.Default.ContentCut) {clipMenuId=null;onEdit("trim")}
                    ClipSheetAction("Duplicar",Icons.Default.ContentCopy) {
                        val list=p.clips.toMutableList();list.add(idx+1,active.copy(id=UUID.randomUUID().toString()))
                        commit(list,"Clipe duplicado")
                    }
                    ClipSheetAction("Antes",Icons.Default.ArrowBack,idx>0) {
                        val list=p.clips.toMutableList(); java.util.Collections.swap(list,idx,idx-1)
                        commit(list,"Clipe movido")
                    }
                    ClipSheetAction("Depois",Icons.Default.ArrowForward,idx<p.clips.lastIndex) {
                        val list=p.clips.toMutableList();java.util.Collections.swap(list,idx,idx+1)
                        commit(list,"Clipe movido")
                    }
                    ClipSheetAction("Excluir",Icons.Default.Delete) {
                        deletionRequested = active.id
                    }
                }
                Text("Volume apenas deste clipe: ${(active.volume*100).toInt()}%")
                Slider(value=active.volume.coerceIn(0f,1f), onValueChange={ v ->
                    onUpdate(p.copy(clips=p.clips.map { if(it.id==active.id) it.copy(volume=v) else it }))
                })
                Text("Excluir ou duplicar não altera o arquivo original.",
                    style=MaterialTheme.typography.bodySmall,
                    color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    deletionRequested?.let { id ->
        AlertDialog(onDismissRequest={deletionRequested=null},
            title={Text("Remover este clipe?")},
            text={Text("O trecho sairá da linha do tempo, sem apagar o arquivo original do aparelho.")},
            confirmButton={TextButton(onClick={
                onUpdate(EditFeatures.repositionAfterDelete(p,p.clips.filterNot{it.id==id}))
                clipMenuId=null;deletionRequested=null;feedback="Clipe excluído do projeto"
            }) { Text("Excluir") }},
            dismissButton={TextButton(onClick={deletionRequested=null}) {Text("Cancelar")}})
    }
}

