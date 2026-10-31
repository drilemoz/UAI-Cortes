package br.com.uaicortes

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.delay

// Extraído da MainActivity na v0.1.53 para reutilização de componentes.

@OptIn(UnstableApi::class)
@Composable
internal fun AudioScreen(p:VideoProject,back:()->Unit,update:(VideoProject)->Unit,
                        chooseMusic:()->Unit, requestedTrackId:String? = null,
                        onSelectionConsumed:()->Unit = {},
                        onChooseExternalVideo:()->Unit = {}, externalVideoUri: Uri? = null,
                        onExternalVideoConsumed:()->Unit = {}) {
    val context = LocalContext.current
    val engine = remember { AudioExtractionEngine(context) }
    var sourceIndex by remember(p.id) { mutableIntStateOf(-1) } // -1 = all clips
    var message by remember { mutableStateOf("") }
    var volumesExpanded by remember(p.id) { mutableStateOf(p.clips.size <= 2) }
    var trackVolumeExpandedId by remember(p.id) { mutableStateOf<String?>(null) }
    var pendingAudioDeleteId by remember(p.id) { mutableStateOf<String?>(null) }
    var externalClip by remember { mutableStateOf<Clip?>(null) }
    var trimStart by remember { mutableFloatStateOf(0f) }
    var trimEnd by remember { mutableFloatStateOf(1f) }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            val duration = mediaDuration(context, uri)
            if (duration > 0) {
                externalClip = Clip(uri=uri.toString(),title=mediaName(context,uri),sourceDurationMs=duration)
                trimStart=0f; trimEnd=1f
            } else message = "Não foi possível ler a duração do vídeo externo."
        }
    }
    LaunchedEffect(externalVideoUri) {
        if (externalVideoUri != null) {
            val duration = mediaDuration(context, externalVideoUri)
            if (duration > 0) {
                externalClip = Clip(uri = externalVideoUri.toString(),
                    title = mediaName(context, externalVideoUri), sourceDurationMs = duration)
                trimStart = 0f; trimEnd = 1f
            } else message = "Não foi possível ler a duração do vídeo selecionado."
            onExternalVideoConsumed()
        }
    }
    // Pré-escuta da origem selecionada, sem alterar a linha do tempo.
    val previewPlayer = remember(context) { diagnosticPlayer(context) }
    var previewPlaying by remember { mutableStateOf(false) }
    val previewClip = externalClip ?: p.clips.getOrNull(sourceIndex)
    val previewStartMs = if (externalClip != null) (externalClip!!.sourceDurationMs * trimStart).toLong()
        else previewClip?.startMs ?: 0L
    val previewEndMs = if (externalClip != null) (externalClip!!.sourceDurationMs * trimEnd).toLong()
        else previewClip?.endMs ?: 0L
    LaunchedEffect(previewClip?.uri, previewStartMs, previewEndMs) {
        previewPlayer.pause()
        previewPlaying = false
        previewClip?.let { clip ->
            previewPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(clip.uri)))
            previewPlayer.prepare()
            previewPlayer.seekTo(previewStartMs)
        } ?: previewPlayer.clearMediaItems()
    }
    LaunchedEffect(previewPlaying, previewEndMs) {
        while (previewPlaying) {
            if (previewEndMs > previewStartMs && previewPlayer.currentPosition >= previewEndMs) {
                previewPlayer.pause()
                previewPlayer.seekTo(previewStartMs)
                previewPlaying = false
            }
            delay(100)
        }
    }
    DisposableEffect(previewPlayer) { onDispose { previewPlayer.release() } }
    // A edição de uma faixa só é aplicada quando o usuário confirma Salvar corte.
    var editingAudioId by remember(p.id) { mutableStateOf<String?>(null) }
    val editingAudio = p.audioTracks.firstOrNull { it.id == editingAudioId }
    val sourceAudioLength = remember(editingAudio?.uri) {
        editingAudio?.let { mediaDuration(context, Uri.parse(it.uri)) } ?: 0L
    }
    var editTimelineStart by remember(p.id) { mutableLongStateOf(0L) }
    var editSourceStart by remember(p.id) { mutableLongStateOf(0L) }
    var editSourceEnd by remember(p.id) { mutableLongStateOf(0L) }
    val editDuration = editSourceEnd - editSourceStart
    // A seleção feita na timeline do editor abre diretamente esta faixa para edição.
    LaunchedEffect(requestedTrackId, p.id) {
        val track = p.audioTracks.firstOrNull { it.id == requestedTrackId }
        if (track != null) {
            val duration = mediaDuration(context, Uri.parse(track.uri))
            val range = AudioTrackTiming.initialRange(track.sourceStartMs, track.durationMs,
                duration, track.startMs, p.durationMs)
            if (range != null) {
                editingAudioId = track.id
                editSourceStart = range.first
                editSourceEnd = range.last
                editTimelineStart = track.startMs.coerceIn(0L,
                    (p.durationMs - (range.last-range.first)).coerceAtLeast(0L))
                previewPlayer.pause()
                previewPlaying = false
            } else message = "Não foi possível abrir esta faixa para corte."
        }
        if (requestedTrackId != null) onSelectionConsumed()
    }
    val editValid = editingAudio != null && AudioTrackTiming.valid(
        editSourceStart, editSourceEnd, sourceAudioLength, editTimelineStart, p.durationMs)
    // Player separado evita interromper a prévia de extração de vídeo.
    val trackPlayer = remember(context) { diagnosticPlayer(context) }
    var trackPlaying by remember { mutableStateOf(false) }
    LaunchedEffect(editingAudioId, editingAudio?.uri, editSourceStart, editSourceEnd) {
        trackPlayer.pause()
        trackPlaying = false
        if (editingAudio != null && editValid) {
            trackPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(editingAudio.uri)))
            trackPlayer.prepare()
            trackPlayer.seekTo(editSourceStart)
        } else trackPlayer.clearMediaItems()
    }
    LaunchedEffect(trackPlaying, editSourceStart, editSourceEnd) {
        while (trackPlaying) {
            if (trackPlayer.currentPosition >= editSourceEnd) {
                trackPlayer.pause()
                trackPlayer.seekTo(editSourceStart)
                trackPlaying = false
            }
            delay(100)
        }
    }
    DisposableEffect(trackPlayer) { onDispose { trackPlayer.release() } }
    var outputExtension by remember { mutableStateOf("m4a") }
    var addAfterExtract by remember { mutableStateOf(false) }
    var exporting by remember { mutableStateOf(false) }
    val saveAudio = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("audio/mp4")) { destination ->
        if (destination != null) {
            val chosen = if (externalClip != null) listOf(externalClip!!.copy(
                startMs=(externalClip!!.sourceDurationMs*trimStart).toLong(),
                endMs=(externalClip!!.sourceDurationMs*trimEnd).toLong()))
                else if (sourceIndex == -1) p.clips else listOfNotNull(p.clips.getOrNull(sourceIndex))
            exporting = true
            runCatching {
                engine.start(chosen, destination, onSuccess = {
                    exporting = false
                    message = "Áudio extraído e salvo no local escolhido."
                    if (addAfterExtract) {
                        runCatching { context.contentResolver.takePersistableUriPermission(destination,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION) }
                        val track = AudioTrack(destination.toString(),"Áudio extraído",
                            startMs=0, durationMs=chosen.sumOf { it.durationMs })
                        update(p.copy(audioTracks=p.audioTracks+track))
                        message += " Adicionado à linha do tempo."
                    }
                },onError={error->exporting=false; DiagnosticRecorder.failure("audio_extract",error);message="Falha: $error"})
            }.onFailure { exporting=false; DiagnosticRecorder.exception("audio_extract_start", it);message="Falha: ${it.message}" }
        }
    }
    DisposableEffect(engine) { onDispose { engine.cancel() } }
    val pageScroll = rememberScrollState()
    val pageScope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(pageScroll)
        .padding(horizontal=16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        Heading("Áudio",back)
        AudioPanelCard {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Icon(Icons.Default.Tune,"Mixagem",tint=MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f)) {
                    Text("Mixagem do projeto",fontWeight=FontWeight.Bold,fontSize=18.sp)
                    Text("${AudioScreenLabels.activeTrackCount(p.clips.size, p.audioTracks.size, p.music != null)} faixas ativas",
                        style=MaterialTheme.typography.bodySmall,
                        color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick={volumesExpanded=!volumesExpanded}) {
                    Icon(Icons.Default.Tune,null,modifier=Modifier.size(17.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if(volumesExpanded) "Recolher" else "Gerenciar faixas",fontSize=12.sp)
                }
            }
            if(volumesExpanded) {
                p.clips.forEachIndexed { index,clip ->
                    AudioTrackCard {
                        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                            Icon(Icons.Default.PlayCircleOutline,null,tint=MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text(clip.title.ifBlank { "Vídeo ${index+1}" },modifier=Modifier.weight(1f),
                                maxLines=1,overflow=TextOverflow.Ellipsis,fontSize=13.sp)
                            Text("${(clip.volume*100).toInt()}%",fontSize=12.sp,
                                color=MaterialTheme.colorScheme.primary)
                        }
                        Slider(value=clip.volume.coerceIn(0f,1f),onValueChange={volume->
                            update(p.copy(clips=p.clips.map { if(it.id==clip.id) it.copy(volume=volume) else it }))
                        },modifier=Modifier.height(28.dp))
                    }
                }
                p.music?.let { music ->
                    AudioTrackCard {
                        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                            Icon(Icons.Default.MusicNote,null,tint=MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text(music.title,modifier=Modifier.weight(1f),maxLines=1,fontSize=13.sp)
                            Text("${(music.volume*100).toInt()}%",fontSize=12.sp)
                            IconButton(onClick={update(p.copy(music=null))},modifier=Modifier.size(40.dp)) {
                                Icon(Icons.Default.Delete,"Remover música")
                            }
                        }
                        Slider(value=music.volume.coerceIn(0f,1f),onValueChange={
                            update(p.copy(music=music.copy(volume=it)))
                        },modifier=Modifier.height(28.dp))
                    }
                }
            }
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Text("Faixas adicionais (${p.audioTracks.size})",fontWeight=FontWeight.SemiBold,
                    modifier=Modifier.weight(1f),fontSize=14.sp)
            }
            if(p.audioTracks.isEmpty()) {
                Text("Nenhuma faixa adicional. Use a seção Adicionar áudio.",fontSize=12.sp,
                    color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            p.audioTracks.forEach { track ->
                AudioTrackCard {
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                        Icon(Icons.Default.GraphicEq,null,tint=MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(AudioScreenLabels.readableFileName(track.title),modifier=Modifier.weight(1f),
                            maxLines=1,overflow=TextOverflow.Ellipsis,fontSize=13.sp)
                        TextButton(onClick={trackVolumeExpandedId=if(trackVolumeExpandedId==track.id) null else track.id},
                            contentPadding=PaddingValues(horizontal=4.dp)) {
                            Text("${(track.volume*100).toInt()}%",fontSize=12.sp)
                        }
                        IconButton(onClick={
                            val sourceDuration = mediaDuration(context, Uri.parse(track.uri))
                            val range = AudioTrackTiming.initialRange(track.sourceStartMs, track.durationMs,
                                sourceDuration, track.startMs, p.durationMs)
                            if (range == null) {
                                message = "O arquivo é curto demais ou não há espaço para editar a faixa."
                            } else {
                                editingAudioId = track.id
                                editTimelineStart = track.startMs.coerceIn(0L,
                                    (p.durationMs - (range.last - range.first)).coerceAtLeast(0L))
                                editSourceStart = range.first
                                editSourceEnd = range.last
                                previewPlayer.pause()
                                previewPlaying = false
                            }
                        },modifier=Modifier.size(40.dp)) { Icon(Icons.Default.ContentCut,"Editar corte da faixa") }
                        IconButton(onClick={pendingAudioDeleteId=track.id},modifier=Modifier.size(40.dp)) {
                            Icon(Icons.Default.Delete,"Remover faixa")
                        }
                    }
                    if(trackVolumeExpandedId==track.id) {
                        Slider(value=track.volume.coerceIn(0f,1f),onValueChange={v->
                            update(p.copy(audioTracks=p.audioTracks.map { if(it.id==track.id) it.copy(volume=v) else it }))
                        },modifier=Modifier.height(28.dp))
                    }
                }
            }
        editingAudio?.let { track ->
            HorizontalDivider(Modifier.padding(vertical=8.dp))
            Text("Editar trecho · ${track.title}",fontWeight=FontWeight.Bold,fontSize=17.sp)
            Text("Recorte a fonte e escolha quando ela entra no vídeo. O arquivo original fica intacto.",
                style=MaterialTheme.typography.bodySmall)
            Text("Arraste o centro para mover; as alças laterais ajustam o começo e o fim.",
                style=MaterialTheme.typography.bodySmall)
            AudioTrimGestureTimeline(
                AudioTimelineDraft(editTimelineStart, editSourceStart, editSourceEnd),
                p.durationMs, sourceAudioLength,
                onMove = { delta ->
                    val moved = AudioTimelineEdit.move(
                        AudioTimelineDraft(editTimelineStart, editSourceStart, editSourceEnd),
                        delta, p.durationMs)
                    editTimelineStart = moved.timelineStartMs
                },
                onLeftTrim = { delta ->
                    val changed = AudioTimelineEdit.left(
                        AudioTimelineDraft(editTimelineStart, editSourceStart, editSourceEnd), delta)
                    editTimelineStart = changed.timelineStartMs
                    editSourceStart = changed.sourceStartMs
                },
                onRightTrim = { delta ->
                    val changed = AudioTimelineEdit.right(
                        AudioTimelineDraft(editTimelineStart, editSourceStart, editSourceEnd),
                        delta, sourceAudioLength, p.durationMs)
                    editSourceEnd = changed.sourceEndMs
                })
            if (sourceAudioLength >= AudioTrackTiming.MIN_TRIM_MS &&
                p.durationMs >= AudioTrackTiming.MIN_TRIM_MS) {
                Text("Início no arquivo: ${formatPipMs(editSourceStart)}")
                Slider(value=editSourceStart.toFloat(),
                    valueRange=0f..(editSourceEnd-AudioTrackTiming.MIN_TRIM_MS).coerceAtLeast(0L).toFloat(),
                    onValueChange={ editSourceStart=it.toLong().coerceIn(0L,
                        (editSourceEnd-AudioTrackTiming.MIN_TRIM_MS).coerceAtLeast(0L)) })
                TimeDigitsField("Entrada",editSourceStart,sourceAudioLength,
                    valid={ it>=0L && it<=editSourceEnd-AudioTrackTiming.MIN_TRIM_MS },
                    onCommit={ editSourceStart=it })
                val maxSourceEnd = minOf(sourceAudioLength,
                    editSourceStart+(p.durationMs-editTimelineStart))
                Text("Fim no arquivo: ${formatPipMs(editSourceEnd)}")
                Slider(value=editSourceEnd.toFloat().coerceIn(
                        (editSourceStart+AudioTrackTiming.MIN_TRIM_MS).toFloat(),
                        maxOf(maxSourceEnd,editSourceStart+AudioTrackTiming.MIN_TRIM_MS).toFloat()),
                    valueRange=(editSourceStart+AudioTrackTiming.MIN_TRIM_MS).toFloat()..
                        maxOf(maxSourceEnd,editSourceStart+AudioTrackTiming.MIN_TRIM_MS).toFloat(),
                    onValueChange={ editSourceEnd=it.toLong().coerceAtLeast(
                        editSourceStart+AudioTrackTiming.MIN_TRIM_MS) })
                TimeDigitsField("Saída",editSourceEnd,sourceAudioLength,
                    valid={ it>=editSourceStart+AudioTrackTiming.MIN_TRIM_MS && it<=maxSourceEnd },
                    onCommit={ editSourceEnd=it })
                Text("Começa no projeto: ${formatPipMs(editTimelineStart)}")
                val latestStart=(p.durationMs-editDuration).coerceAtLeast(0L)
                Slider(value=editTimelineStart.coerceIn(0L,latestStart).toFloat(),
                    valueRange=0f..latestStart.toFloat(),
                    onValueChange={ editTimelineStart=it.toLong().coerceIn(0L,latestStart) })
                TimeDigitsField("Na timeline",editTimelineStart,p.durationMs,
                    valid={ it>=0L && it<=latestStart },onCommit={editTimelineStart=it})
                Text("Trecho selecionado: ${formatPipMs(editDuration.coerceAtLeast(0L))}",
                    style=MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick={
                        if (trackPlaying) {
                            trackPlayer.pause()
                            trackPlaying=false
                        } else if (editValid) {
                            if (trackPlayer.currentPosition < editSourceStart ||
                                trackPlayer.currentPosition >= editSourceEnd)
                                trackPlayer.seekTo(editSourceStart)
                            trackPlayer.play()
                            trackPlaying=true
                        }
                    },enabled=editValid) {
                        Icon(if(trackPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,null)
                        Text(if(trackPlaying) " Pausar" else " Ouvir corte")
                    }
                    TextButton(onClick={
                        trackPlayer.pause()
                        trackPlayer.seekTo(editSourceStart)
                        trackPlaying=false
                    }) { Text("Reiniciar") }
                }
                if (!editValid) Text("Trecho inválido ou ultrapassa o fim do vídeo.",
                    color=MaterialTheme.colorScheme.error)
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick={ editingAudioId=null },modifier=Modifier.weight(1f)) {
                        Text("Cancelar")
                    }
                    Button(onClick={
                        update(p.copy(audioTracks=p.audioTracks.map { saved ->
                            if (saved.id==track.id) saved.copy(startMs=editTimelineStart,
                                sourceStartMs=editSourceStart,durationMs=editDuration) else saved
                        }))
                        editingAudioId=null
                        message="Corte da faixa salvo no projeto."
                    },enabled=editValid,modifier=Modifier.weight(1f)) { Text("Salvar corte") }
                }
            } else {
                Text("Faixa curta demais para editar neste vídeo.")
                TextButton(onClick={editingAudioId=null}) { Text("Fechar") }
            }
            HorizontalDivider(Modifier.padding(vertical=8.dp))
        }
        }
        AudioPanelCard {
            Row(verticalAlignment=Alignment.CenterVertically) {
                Icon(Icons.Default.LibraryMusic,null,tint=MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(9.dp))
                Column {
                    Text("Adicionar áudio",fontWeight=FontWeight.Bold,fontSize=18.sp)
                    Text("Música, narração ou efeitos",style=MaterialTheme.typography.bodySmall,
                        color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                AudioQuickAction("Escolher áudio",Icons.Default.LibraryMusic,{
                    chooseMusic()
                },Modifier.weight(1f))
                AudioQuickAction("Extrair",Icons.Default.VideoLibrary,{
                    pageScope.launch { pageScroll.animateScrollTo(pageScroll.maxValue) }
                },Modifier.weight(1f))
            }
            Text("Os áudios selecionados são adicionados diretamente ao projeto pelo seletor de arquivos do Android.",
                style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        AudioPanelCard {
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Icon(Icons.Default.VideoLibrary,null,tint=MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(9.dp))
                    Column {
                        Text("Extrair áudio de vídeo",fontWeight=FontWeight.Bold,fontSize=18.sp)
                        Text("Extraia o áudio sem alterar o vídeo original.",
                            style=MaterialTheme.typography.bodySmall,
                            color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Text("Origem do vídeo",fontWeight=FontWeight.SemiBold)
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected=externalClip==null && sourceIndex==-1,
                        onClick={externalClip=null;sourceIndex=-1},label={Text("Projeto inteiro",fontSize=11.sp)},
                        enabled=!exporting,modifier=Modifier.weight(1f))
                    var showClips by remember { mutableStateOf(false) }
                    Box(Modifier.weight(1f)) {
                        FilterChip(selected=externalClip==null && sourceIndex>=0,
                            onClick={showClips=true},label={Text(if(sourceIndex>=0 && externalClip==null)
                                "Clipe ${sourceIndex+1}" else "Clipe atual",fontSize=11.sp,maxLines=1)},
                            enabled=!exporting && p.clips.isNotEmpty(),modifier=Modifier.fillMaxWidth())
                        DropdownMenu(expanded=showClips,onDismissRequest={showClips=false}) {
                            p.clips.forEachIndexed { i,clip ->
                                DropdownMenuItem(text={Text("${i+1} · ${clip.title}",maxLines=1)},
                                    onClick={sourceIndex=i;externalClip=null;showClips=false})
                            }
                        }
                    }
                    FilterChip(selected=externalClip!=null,onClick=onChooseExternalVideo,
                        label={Text("Vídeo externo",fontSize=11.sp,maxLines=1)},
                        enabled=!exporting,modifier=Modifier.weight(1f))
                }
                Text("Fonte externa",fontWeight=FontWeight.SemiBold)
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    AudioQuickAction("Galeria de vídeos",Icons.Default.VideoLibrary,onChooseExternalVideo,
                        Modifier.weight(1f),enabled=!exporting)
                    AudioQuickAction("Arquivos",Icons.Default.FolderOpen,
                        {videoPicker.launch(arrayOf("video/*"))},Modifier.weight(1f),enabled=!exporting)
                }
        externalClip?.let { clip ->
            Text("Origem externa: ${clip.title}",maxLines=2)
            Text("Início: ${formatPipMs((clip.sourceDurationMs*trimStart).toLong())}")
            Slider(value=trimStart,valueRange=0f..1f,onValueChange={trimStart=it.coerceAtMost(trimEnd-.001f)})
            Text("Fim: ${formatPipMs((clip.sourceDurationMs*trimEnd).toLong())}")
            Slider(value=trimEnd,valueRange=0f..1f,onValueChange={trimEnd=it.coerceAtLeast(trimStart+.001f)})
            TextButton(onClick={externalClip=null}) { Text("Voltar à origem do projeto") }
        }
        if (previewClip != null) {
            Text("Pré-escuta do trecho",fontWeight=FontWeight.Medium)
            Text("${formatPipMs(previewStartMs)} até ${formatPipMs(previewEndMs)}",
                style=MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically) {
                OutlinedButton(onClick={
                    if (previewPlaying) {
                        previewPlayer.pause()
                        previewPlaying=false
                    } else {
                        if (previewPlayer.currentPosition < previewStartMs ||
                            previewPlayer.currentPosition >= previewEndMs) previewPlayer.seekTo(previewStartMs)
                        previewPlayer.play()
                        previewPlaying=true
                    }
                },enabled=!exporting && previewEndMs>previewStartMs) {
                    Icon(if(previewPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,null)
                    Text(if(previewPlaying) " Pausar" else " Ouvir trecho")
                }
                TextButton(onClick={previewPlayer.pause();previewPlayer.seekTo(previewStartMs);previewPlaying=false}) {
                    Text("Reiniciar")
                }
            }
            Text("A pré-escuta reproduz o áudio do vídeo de origem; não modifica o arquivo.",
                style=MaterialTheme.typography.bodySmall)
        } else if (sourceIndex == -1) {
            Text("Para ouvir antes de extrair, escolha um clipe ou importe um vídeo externo.",
                style=MaterialTheme.typography.bodySmall)
        }
                Text("Formato de áudio",fontWeight=FontWeight.SemiBold)
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    listOf("m4a","mp4").forEach { format ->
                        FilterChip(selected=outputExtension==format,onClick={outputExtension=format},
                            label={Text(format.uppercase())},enabled=!exporting)
                    }
                }
                Text("M4A e MP4 usam AAC. MP3 e WAV não são suportados por esta extração.",
                    style=MaterialTheme.typography.bodySmall,
                    color=MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick={
                        addAfterExtract=false
                        saveAudio.launch("UaiCortes_audio.$outputExtension")
                    },modifier=Modifier.weight(1f),enabled=!exporting && (externalClip!=null || p.clips.isNotEmpty()),
                        contentPadding=PaddingValues(horizontal=8.dp)) {
                        Icon(Icons.Default.Save,null,modifier=Modifier.size(17.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Salvar",fontSize=12.sp,maxLines=1)
                    }
                    Button(onClick={
                        addAfterExtract=true
                        saveAudio.launch("UaiCortes_audio.$outputExtension")
                    },modifier=Modifier.weight(1f),enabled=!exporting && p.clips.isNotEmpty(),
                        contentPadding=PaddingValues(horizontal=8.dp)) {
                        Icon(Icons.Default.PlaylistAdd,null,modifier=Modifier.size(17.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Salvar e usar",fontSize=12.sp,maxLines=1)
                    }
                }
                if(exporting) {
                    LinearProgressIndicator(modifier=Modifier.fillMaxWidth())
                    Text("Extraindo áudio... mantenha esta tela aberta.",fontSize=12.sp)
                }
        }
        if(message.isNotBlank()) Text(message,color=MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(20.dp))
    }
    pendingAudioDeleteId?.let { id ->
        val candidate=p.audioTracks.firstOrNull { it.id==id }
        if(candidate!=null) AlertDialog(onDismissRequest={pendingAudioDeleteId=null},
            title={Text("Remover faixa de áudio?")},
            text={Text("Remover '${candidate.title}' da edição? O arquivo original não será apagado.")},
            confirmButton={TextButton(onClick={
                if(editingAudioId==id) editingAudioId=null
                if(trackVolumeExpandedId==id) trackVolumeExpandedId=null
                update(p.copy(audioTracks=p.audioTracks.filterNot { it.id==id }))
                message="Faixa removida do projeto."
                pendingAudioDeleteId=null
            }) {Text("Remover")}},
            dismissButton={TextButton(onClick={pendingAudioDeleteId=null}) {Text("Cancelar")}})
    }
}
