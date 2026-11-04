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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.widget.Toast
import java.util.UUID

// Extraído da MainActivity na v0.1.53 para reutilização de componentes.

@OptIn(UnstableApi::class)
@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditorScreen(p:VideoProject, selectedId:String, currentMs:Long,
    onSelected:(String)->Unit,onPosition:(Long)->Unit,onBack:()->Unit,onEdit:(String)->Unit,
    onUpdate:(VideoProject)->Unit,onAddVideo:()->Unit,onPiP:()->Unit,onAudio:()->Unit,
    onSelectOverlay:(String)->Unit,onTransition:(Int)->Unit,onSaveHome:()->Unit,
    onSelectAudio:(String)->Unit,onSelectSubtitle:(String)->Unit,
    onClipAudio:(String)->Unit,
    autoPlayOnReturn: Boolean = false,
    onReturnPlaybackConsumed: () -> Unit = {}) {
    val context=LocalContext.current
    val captureScope = rememberCoroutineScope()
    var savingFrame by remember { mutableStateOf(false) }
    var captureAtMs by remember { mutableLongStateOf(0L) }
    val playbackVm: EditorPlaybackViewModel = viewModel()
    LaunchedEffect(p.id) { playbackVm.selectProject(p.id) }
    val playbackSpeed = playbackVm.speed
    // Estado exclusivamente da prévia; não é salvo no projeto nem enviado ao exportador.
    var previewVolume by remember(p.id) { mutableFloatStateOf(1f) }
    var previewVolumeMenu by remember { mutableStateOf(false) }
    val activeFrameIndex = AdaptiveVideoLayout.activeClipIndex(p.clips.map { it.durationMs }, currentMs)
    val activeFrameUri = p.clips.getOrNull(activeFrameIndex)?.uri
    // Um clipe novo pode ter FPS diferente. A leitura não bloqueia a interface.
    val frameFps by produceState<Float?>(initialValue = null, activeFrameUri) {
        value = activeFrameUri?.let { uri ->
            withContext(Dispatchers.IO) { VideoFrameRate.read(context, uri) }
        }
    }
    val player=remember(p.id) { diagnosticPlayer(context).apply { repeatMode = Player.REPEAT_MODE_ALL } }
    var isPlaying by remember(player) { mutableStateOf(player.isPlaying) }
    var restoringMedia by remember(player) { mutableStateOf(true) }
    var scrubbing by remember(player) { mutableStateOf(false) }
    val latestPosition by rememberUpdatedState(currentMs)
    var clipMenuId by remember(p.id) { mutableStateOf<String?>(null) }
    var deletionRequested by remember(p.id) { mutableStateOf<String?>(null) }
    var feedback by remember(p.id) { mutableStateOf("") }
    LaunchedEffect(player, p.clips.map { it.id to it.volume }, previewVolume) {
        while (true) {
            val clipGain = p.clips.getOrNull(player.currentMediaItemIndex)?.volume ?: 1f
            player.volume = EditorPreviewVolume.output(clipGain, previewVolume)
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
        if (autoPlayOnReturn) {
            // Só reinicia a prévia após carregar a mídia e restaurar o cursor.
            if (p.clips.isNotEmpty()) player.play()
            onReturnPlaybackConsumed()
        }
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
            TrackAudioPreview(music.uri, 0L, p.durationMs,
                EditorPreviewVolume.output(music.volume, previewVolume), currentMs,
                isPlaying, playbackSpeed, true)
        }
        ClipAudioBindings.resolved(p).forEach { audio ->
            key(audio.id) {
                TrackAudioPreview(audio.uri, audio.startMs, audio.durationMs,
                    EditorPreviewVolume.output(audio.volume, previewVolume),
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
            Box(Modifier.width(videoW).height(videoH).clipToBounds()) {
                val index = activeIndex
                val clip = p.clips.getOrNull(index)
                val before = p.clips.take(index.coerceAtLeast(0)).sumOf { it.durationMs }
                val incoming = if (clip != null) EditFeatures.boundary(p, index - 1) else null
                val outgoing = if (clip != null) EditFeatures.boundary(p, index) else null
                val inMs = if (clip != null) EditFeatures.transitionMs(incoming,
                    p.clips.getOrElse(index - 1) { clip }.durationMs, clip.durationMs) else 0L
                val outMs = if (clip != null) EditFeatures.transitionMs(outgoing,
                    clip.durationMs, p.clips.getOrElse(index + 1) { clip }.durationMs) else 0L
                val local = (currentMs - before).coerceIn(0L, clip?.durationMs ?: 0L)
                // Apenas o vídeo principal sofre o movimento: PiP e textos são
                // camadas independentes, como no Transformer.
                val motion = if (clip != null) TransitionEffects.motionAt(local, clip.durationMs,
                    incoming?.kind ?: TransitionEffects.CUT, inMs,
                    outgoing?.kind ?: TransitionEffects.CUT, outMs)
                    else TransitionEffects.Motion(1f, 0f)
                AndroidView(factory = { ctx -> previewPlayer(ctx).apply {
                    this.player = player
                    useController = false
                } }, update = { it.player = player }, modifier = Modifier.fillMaxSize()
                    .graphicsLayer {
                        scaleX = motion.scale
                        scaleY = motion.scale
                        translationX = motion.panX * size.width / 2f
                    })
                // Prévia de tintura ainda aproximada em efeitos monocromáticos/sépia.
                if (clip != null) TransitionPreviewTint(incoming, inMs, outgoing, outMs,
                    local, clip.durationMs)
                p.overlays.forEach { pip ->
                    if (currentMs >= pip.startAtMs && currentMs < pip.startAtMs + pip.durationMs) {
                        key(pip.id) {
                            PipLayer(pip, currentMs - pip.startAtMs, isPlaying,
                                speed = playbackSpeed, editable = false,
                                previewGain = previewVolume) { }
                        }
                    }
                }
                SubtitlePreviewPolicy.firstAt(p.subtitles, currentMs)?.let { cue ->
                    SubtitleOverlayOnFrame(cue, editable = false, playbackMs=currentMs)
                }
                p.textWatermark?.toSubtitle(p.durationMs)?.let { watermark ->
                    SubtitleOverlayOnFrame(watermark, editable = false, playbackMs=currentMs)
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal=4.dp,vertical=6.dp),horizontalArrangement=Arrangement.Center,
            verticalAlignment=Alignment.CenterVertically) {
            IconButton(onClick = {
                player.pause()
                seekGlobal(FrameStepPolicy.target(currentMs, p.durationMs, -1, frameFps))
            }, enabled = p.durationMs > 0, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.SkipPrevious, "Voltar aproximadamente um quadro")
            }
            IconButton(onClick={if(isPlaying)player.pause() else player.play()},
                modifier = Modifier.size(40.dp)) {
                Icon(if(isPlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircle, "Reproduzir / pausar",
                    modifier=Modifier.size(35.dp))
            }
            IconButton(onClick = {
                player.pause()
                seekGlobal(FrameStepPolicy.target(currentMs, p.durationMs, 1, frameFps))
            }, enabled = p.durationMs > 0, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.SkipNext, "Avançar aproximadamente um quadro")
            }
            Box {
                IconButton(onClick = { previewVolumeMenu = true },
                    modifier = Modifier.size(40.dp)) {
                    Icon(if (previewVolume <= 0f) Icons.Default.VolumeOff
                         else if (previewVolume < 0.6f) Icons.Default.VolumeDown
                         else Icons.Default.VolumeUp,
                        "Volume apenas da prévia: ${EditorPreviewVolume.percent(previewVolume)}%")
                }
                DropdownMenu(expanded = previewVolumeMenu,
                    onDismissRequest = { previewVolumeMenu = false },
                    modifier = Modifier.width(246.dp)) {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text("Volume da prévia · ${EditorPreviewVolume.percent(previewVolume)}%",
                            style = MaterialTheme.typography.titleSmall)
                        Slider(value = previewVolume,
                            onValueChange = { previewVolume = it.coerceIn(0f, 1f) },
                            valueRange = 0f..1f)
                        Text("Não modifica a mixagem ou o MP4 exportado.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            PlaybackSpeedButton(playbackSpeed, onSelect = playbackVm::updateSpeed)
            // Fotografa o quadro original no cursor; PiP/legendas não entram no PNG.
            IconButton(onClick = {
                // Pausar antes de escolher o instante: não use currentMs defasado pelo polling.
                player.pause()
                val frozenMs = projectPosition(player, p.clips, p.durationMs)
                    .coerceIn(0L, p.durationMs.coerceAtLeast(0L))
                onPosition(frozenMs)
                val selection = FrameCapturePlan.locate(p.clips.map {
                    FrameCaptureClip(it.uri, it.startMs, it.endMs)
                }, frozenMs)
                if (selection == null) {
                    Toast.makeText(context, "Nenhum quadro disponível para capturar.", Toast.LENGTH_SHORT).show()
                } else if (!savingFrame) {
                    val title = p.title
                    captureAtMs = frozenMs
                    savingFrame = true
                    captureScope.launch {
                        try {
                            val name = FrameCapturePlan.filename(title, System.currentTimeMillis(), selection.sourceTimeMs)
                            withContext(Dispatchers.IO) {
                                VideoFrameSaver.save(context.applicationContext, selection, name)
                            }
                            Toast.makeText(context, "Quadro salvo em Imagens/UaiCortes", Toast.LENGTH_LONG).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Não foi possível salvar o quadro: ${e.message ?: "falha de leitura"}", Toast.LENGTH_LONG).show()
                        } finally {
                            savingFrame = false
                        }
                    }
                }
            }, enabled = p.clips.isNotEmpty() && !savingFrame,
                modifier = Modifier.size(42.dp)) {
                if (savingFrame) CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                else Icon(Icons.Default.PhotoCamera, "Capturar quadro e salvar na galeria")
            }
        }
        if (savingFrame) Text("Capturando quadro em ${formatPipMs(captureAtMs)}… Vídeo pausado.",
            modifier=Modifier.fillMaxWidth().padding(horizontal=20.dp),
            style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.primary)
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text("Vídeo:", style = MaterialTheme.typography.bodySmall)
            EditableSeekTime(currentMs, p.durationMs, onSeek = { ms ->
                seekGlobal(ms)
            }, modifier = Modifier.weight(1f))
        }
        // Os botões junto de play/pause substituem os quatro ajustes finos redundantes.
        // O Slider continua disponível para deslocamentos longos e arrasto direto.
        if(p.durationMs>0) Slider(value=currentMs.toFloat().coerceIn(0f,p.durationMs.toFloat()),
            onValueChange={global ->
                scrubbing = true
                seekGlobal(global.toLong())
            },
            onValueChangeFinished = { scrubbing = false },
            valueRange=0f..p.durationMs.toFloat(),modifier=Modifier.fillMaxWidth().padding(horizontal=16.dp))
        TimelineRow("Vídeo",Blue,p.clips.mapIndexed{ index, clip -> "Clipe ${index+1}" to clip.durationMs },p.durationMs,currentMs,selectedId,
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
            Triple("Descrição", Icons.Default.Description, "publication"),
            Triple("Prompt Flow", Icons.Default.AutoAwesome, "flow_prompt"),
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
                        val newId = UUID.randomUUID().toString()
                        val list=p.clips.toMutableList();list.add(idx+1,active.copy(id=newId))
                        onUpdate(ClipAudioBindings.cloneAttached(
                            EditFeatures.repositionAfterDelete(p,list), active.id,newId))
                        feedback="Clipe e áudio vinculados duplicados"
                        clipMenuId=null
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
                OutlinedButton(onClick={ clipMenuId=null; onClipAudio(active.id) },
                    modifier=Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Mic, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (p.audioTracks.any { it.clipId == active.id })
                        "Editar áudio deste clipe" else "Adicionar áudio somente a este clipe")
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

