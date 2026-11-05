package br.com.uaicortes

import android.net.Uri
import android.graphics.Bitmap
import android.os.Build
import android.graphics.BitmapFactory
import android.util.Size
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.withContext
import kotlin.math.abs

/** Tela modular. O botão automático só planeja a animação; salvar gera o MP4. */
@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AnimateImagesScreen(
    selected: List<Uri>, onChoose: () -> Unit, onBack: () -> Unit,
    onOrderChanged: (List<Uri>) -> Unit, onUseInEditor: (Uri, VideoPublicationDraft) -> Boolean,
    onSaveDraft: (List<Uri>, ImageAnimationOptions) -> Boolean,
    onSocialPublish: (Uri, String, VideoPublicationDraft) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val animationVm: ImageAnimationTaskViewModel = viewModel()
    val task = animationVm.task
    val busy = task.working
    val progress = task.progress
    val images = remember { mutableStateListOf<Uri>() }
    val listState=rememberLazyListState()
    LaunchedEffect(selected) {
        // Mantém as fotos escolhidas e permite reordenar sem perder a seleção ao voltar da galeria.
        if (images.map { it.toString() } != selected.map { it.toString() }) {
            images.clear(); images.addAll(selected)
        }
    }
    val draftStore = remember(context) { ImageAnimationDraftStore(context.applicationContext) }
    val originalDraft = remember(draftStore) { draftStore.load() }
    var draftStatus by remember { mutableStateOf(draftStore.inspect().status) }
    var requestDraftReset by remember { mutableStateOf(false) }
    val imageKeys = images.map(Uri::toString)
    val unavailableImages by produceState<List<String>?>(initialValue = null, imageKeys) {
        value = null
        value = ImageDraftMediaAccess(context).inaccessible(imageKeys.map(Uri::parse))
    }
    val allImagesReadable = unavailableImages?.isEmpty() == true
    var options by rememberSaveable(stateSaver = listSaver<ImageAnimationOptions, Any>(
        save = { listOf(it.intensity, it.secondsPerImage, it.aspect, it.quality, it.fps, it.transition, it.motion) },
        restore = { ImageAnimationOptions(it[0] as String, it[1] as Int, it[2] as String,
            it[3] as Int, it[4] as Int, it[5] as String, it.getOrNull(6) as? String ?: "auto") }
    )) { mutableStateOf(originalDraft?.options ?: ImageAnimationOptions()) }
    var lastSavedImages by remember { mutableStateOf(originalDraft?.images.orEmpty()) }
    var lastSavedOptions by remember { mutableStateOf(originalDraft?.options ?: ImageAnimationOptions()) }
    var confirmExit by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf(false) }
    var advanced by remember { mutableStateOf(false) }
    var animated by remember { mutableStateOf(false) }
    var playing by remember { mutableStateOf(true) }
    var previewQuality by rememberSaveable { mutableStateOf(ImagePreviewQuality.BALANCED.name) }
    var expandedPreview by remember { mutableStateOf(false) }
    var previewMs by remember { mutableLongStateOf(0L) }
    var confirmCancel by remember { mutableStateOf(false) }
    val total = ImageAnimationPlan.totalDuration(images.size,options)
    LaunchedEffect(animated, playing, busy, images.size, options) {
        if (animated && playing && !busy && total > 0L) {
            var lastNs = 0L
            while (true) {
                val now = withFrameNanos { it }
                if (lastNs != 0L) {
                    val elapsed = ((now - lastNs) / 1_000_000L).coerceIn(0L, 250L)
                    previewMs = (previewMs + elapsed) % total
                }
                lastNs = now
            }
        }
    }
    fun saveDraftAndReturn() {
        if (onSaveDraft(images.toList(), options)) {
            lastSavedImages = images.toList()
            lastSavedOptions = options
            saveError = false
            draftStatus = draftStore.inspect().status
            onBack()
        } else saveError = true
    }
    fun goBack() {
        if (busy) {
            if (animationVm.canCancel()) confirmCancel=true
            else android.widget.Toast.makeText(context,"O Uai Cortes está finalizando o vídeo.",
                android.widget.Toast.LENGTH_SHORT).show()
        } else if (ImageAnimationDraftRules.changed(lastSavedImages.map(Uri::toString),
            lastSavedOptions,images.map(Uri::toString),options)) confirmExit=true
        else onBack()
    }
    BackHandler { goBack() }
    if (confirmExit) AlertDialog(onDismissRequest={confirmExit=false},
        title={Text("Voltar à tela inicial?")},
        text={Text("Há alterações no rascunho. Salve para continuar depois ou descarte para voltar à tela inicial. Isso não exporta um MP4.")},
        confirmButton={TextButton(onClick={confirmExit=false;saveDraftAndReturn()}) {Text("Salvar e voltar")} },
        dismissButton={Row {
            TextButton(onClick={confirmExit=false;onBack()}) {Text("Descartar alterações")}
            TextButton(onClick={confirmExit=false}) {Text("Continuar")}
        }})
    if (requestDraftReset) AlertDialog(onDismissRequest={requestDraftReset=false},
        title={Text("Descartar cadastro de rascunho danificado?")},
        text={Text("Não foi possível ler o rascunho nem o backup. Esta ação reinicia somente o cadastro " +
            "do Animar Imagens; as fotos originais não serão excluídas.")},
        confirmButton={TextButton(onClick={
            requestDraftReset=false
            if (draftStore.resetDamaged()) {
                draftStatus = ImageDraftStatus.EMPTY
                lastSavedImages = emptyList()
                lastSavedOptions = ImageAnimationOptions()
                saveError = false
            } else saveError = true
        }) {Text("Reiniciar cadastro")} },
        dismissButton={TextButton(onClick={requestDraftReset=false}) {Text("Cancelar")} })
    if (confirmCancel) AlertDialog(onDismissRequest={confirmCancel=false},
        title={Text("Cancelar a animação?")},
        text={Text("O vídeo parcial será removido. As imagens selecionadas permanecem intactas.")},
        confirmButton={TextButton(onClick={confirmCancel=false;animationVm.cancel()}) {Text("Cancelar exportação")} },
        dismissButton={TextButton(onClick={confirmCancel=false}) {Text("Continuar")} })

    val currentIndex = if (images.isEmpty()) 0 else
        (previewMs / options.durationMs).toInt().coerceIn(0,images.lastIndex)
    val localFraction = if (options.durationMs > 0) (previewMs % options.durationMs).toFloat()/options.durationMs else 0f
    val frame = ImageAnimationPlan.frame(currentIndex,localFraction,options.intensity,options.motion)
    val fade=ImageAnimationPlan.fadeMs(options)
    val elapsed=previewMs % options.durationMs
    val fadeIn=if(currentIndex>0 && fade>0) ((fade-elapsed).toFloat()/fade).coerceIn(0f,1f) else 0f
    val fadeOut=if(currentIndex<images.lastIndex && fade>0) ((elapsed-(options.durationMs-fade)).toFloat()/fade).coerceIn(0f,1f) else 0f
    val fadeKind=if(fadeIn>fadeOut) ImageAnimationPlan.transitionKind(options,currentIndex-1)
        else ImageAnimationPlan.transitionKind(options,currentIndex)
    val fadeStrength=maxOf(fadeIn,fadeOut)
    val fadeColor=when(fadeKind) {
        "Fade branco" -> androidx.compose.ui.graphics.Color.White
        "Fade preto" -> androidx.compose.ui.graphics.Color.Black
        else -> androidx.compose.ui.graphics.Color.Transparent
    }
    val itemUri = images.getOrNull(currentIndex)
    val selectedPreviewQuality = ImagePreviewQuality.entries.firstOrNull { it.name == previewQuality }
        ?: ImagePreviewQuality.BALANCED
    val previewImage by produceState<ImagePreviewDecoded?>(initialValue = null, itemUri, selectedPreviewQuality) {
        value = null
        value = itemUri?.let { uri -> withContext(Dispatchers.IO) {
            ImageAnimationPreviewDecoder.decode(context, uri, selectedPreviewQuality)
        } }
    }
    if (expandedPreview) {
        Dialog(onDismissRequest = { expandedPreview = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Column(Modifier.fillMaxWidth(.96f)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
                .padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Prévia ampliada", style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f))
                    IconButton(onClick = { expandedPreview = false }) {
                        Icon(Icons.Default.Close, "Fechar prévia")
                    }
                }
                BoxWithConstraints(Modifier.fillMaxWidth().heightIn(max = 560.dp),
                    contentAlignment = Alignment.Center) {
                    val ratio = options.width.toFloat() / options.height
                    val targetHeight = minOf(560.dp, maxWidth / ratio)
                    AnimationFrameImage(previewImage?.bitmap, frame, fadeColor,
                        if (animated) fadeStrength else 0f, currentIndex, images.size,
                        animated, Modifier.size(targetHeight * ratio, targetHeight))
                }
                Text("Amostra ${selectedPreviewQuality.label.lowercase()} · Não altera o MP4",
                    style = MaterialTheme.typography.labelSmall)
            }
        }
    }
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().height(56.dp),verticalAlignment=Alignment.CenterVertically) {
            IconButton(onClick={::goBack}) {Icon(Icons.Default.ArrowBack,"Voltar à tela inicial")}
            Text("Animar Imagens",style=MaterialTheme.typography.titleLarge,
                modifier=Modifier.weight(1f))
            TextButton(onClick={::saveDraftAndReturn},enabled=!busy) {
                Icon(Icons.Default.Save,null,Modifier.size(18.dp))
                Spacer(Modifier.width(3.dp))
                Text("Rascunho",maxLines=1)
            }
            IconButton(onClick=onChoose,enabled=!busy) {
                Icon(Icons.Default.AddPhotoAlternate,"Importar imagens")
            }
        }
        if (busy) {
            Column(Modifier.fillMaxSize().padding(22.dp),verticalArrangement=Arrangement.Center,
                horizontalAlignment=Alignment.CenterHorizontally) {
                Icon(Icons.Default.MovieCreation,null,Modifier.size(50.dp))
                Spacer(Modifier.height(16.dp))
                Text(AnimationProgressUi.title(task.phase),style=MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(12.dp))
                // Progresso opcional: passar SEMPRE um Float não nulo para a lambda
                // usada pelo Material3. Evita NPE na árvore de semântica durante
                // transições rápidas entre fases indeterminadas/determinadas.
                val fixedProgress=AnimationProgressUi.fraction(progress)
                if (fixedProgress == null) LinearProgressIndicator(modifier=Modifier.fillMaxWidth())
                else {
                    val stableFraction: Float = fixedProgress.coerceIn(0f, 1f)
                    LinearProgressIndicator(progress={ stableFraction },modifier=Modifier.fillMaxWidth())
                    Text("${(stableFraction*100).toInt()}%")
                }
                Text("O Uai Cortes está preparando seu vídeo. Progresso real quando disponível.",
                    style=MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(18.dp))
                OutlinedButton(onClick={confirmCancel=true},enabled=animationVm.canCancel()) {
                    Text(if (animationVm.canCancel()) "Cancelar" else "Salvando…")
                }
            }
        } else {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement=Arrangement.spacedBy(12.dp)) {
                val previewRatio = options.width.toFloat() / options.height
                val stageHeight = when {
                    previewRatio < .8f -> 380.dp
                    previewRatio > 1.3f -> 220.dp
                    else -> 320.dp
                }
                BoxWithConstraints(Modifier.fillMaxWidth().height(stageHeight)
                    .clip(RoundedCornerShape(12.dp))
                    .background(androidx.compose.ui.graphics.Color(0xFF171923)),
                    contentAlignment = Alignment.Center) {
                    val fittedWidth = minOf(maxWidth, stageHeight * previewRatio)
                    val fittedHeight = fittedWidth / previewRatio
                    AnimationFrameImage(previewImage?.bitmap, frame, fadeColor,
                        if (animated) fadeStrength else 0f, currentIndex, images.size,
                        animated, Modifier.size(fittedWidth, fittedHeight))
                }
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Nitidez", style = MaterialTheme.typography.labelMedium)
                    ImagePreviewQuality.entries.forEach { quality ->
                        FilterChip(selected = previewQuality == quality.name,
                            onClick = { previewQuality = quality.name },
                            label = { Text(quality.label, fontSize = 11.sp, maxLines = 1) },
                            modifier = Modifier.weight(1f))
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(previewImage?.let { "Foto original: ${it.originalWidth} × ${it.originalHeight}" }
                        ?: if (images.isEmpty()) "Selecione uma imagem" else "Carregando imagem original…",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.weight(1f))
                    TextButton(onClick = { expandedPreview = true }, enabled = previewImage != null) {
                        Icon(Icons.Default.Fullscreen, null, Modifier.size(18.dp))
                        Text("Ampliar")
                    }
                }
                if (animated && images.isNotEmpty()) {
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        IconButton(onClick={playing=!playing}) {Icon(if(playing) Icons.Default.Pause else Icons.Default.PlayArrow,"Reproduzir ou pausar")}
                        Slider(value= if(total>0) previewMs.toFloat()/total else 0f,
                            onValueChange={ previewMs=(it*total).toLong().coerceIn(0,total.coerceAtLeast(1L)-1);playing=false },
                            modifier=Modifier.weight(1f))
                        Text(formatMs(previewMs),style=MaterialTheme.typography.labelSmall)
                    }
                    Text("Prévia proporcional da foto original; a qualidade escolhida aqui não muda o MP4.",
                        style=MaterialTheme.typography.bodySmall)
                }
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Text("Imagens (${images.size})",style=MaterialTheme.typography.titleMedium,modifier=Modifier.weight(1f))
                    TextButton(onClick=onChoose) {Text("+ Importar")}
                }
                if(images.isEmpty()) Text("Escolha fotos na galeria para começar.",style=MaterialTheme.typography.bodySmall)
                else {
                    Text("Toque na foto para pré-visualizar. Arraste ☰ ou use as setas para ordenar. Até 80 fotos.",
                        style=MaterialTheme.typography.labelSmall)
                    LazyColumn(Modifier.fillMaxWidth().heightIn(max=202.dp),state=listState,
                        verticalArrangement=Arrangement.spacedBy(5.dp)) {
                        itemsIndexed(images,key={_,uri->uri.toString()}) { index,uri ->
                            var dragPx by remember(uri) { mutableFloatStateOf(0f) }
                            var dragging by remember(uri) { mutableStateOf(false) }
                            Row(Modifier.fillMaxWidth().zIndex(if(dragging) 1f else 0f)
                                .graphicsLayer { translationY=dragPx }
                                .clickable { previewMs = ImageAnimationPlan.focusTimeMs(index, options); animated = true; playing = false }
                                .background(if(animated && currentIndex==index) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant,RoundedCornerShape(8.dp))
                                .padding(horizontal=4.dp,vertical=4.dp),
                                verticalAlignment=Alignment.CenterVertically) {
                                Icon(Icons.Default.DragHandle,"Arrastar imagem",
                                    Modifier.size(32.dp).pointerInput(uri) {
                                        val step=55.dp.toPx()
                                        detectDragGestures(
                                            onDragStart={dragging=true;dragPx=0f},
                                            onDragEnd={dragging=false;dragPx=0f},
                                            onDragCancel={dragging=false;dragPx=0f}
                                        ) { change,drag ->
                                            change.consume()
                                            dragPx+=drag.y
                                            while(abs(dragPx)>=step) {
                                                val from=images.indexOf(uri)
                                                val dir=if(dragPx>0) 1 else -1
                                                val to=from+dir
                                                if(from<0 || to !in images.indices) {dragPx=0f;break}
                                                images.removeAt(from);images.add(to,uri)
                                                onOrderChanged(images.toList());animated=false;previewMs=0
                                                dragPx-=dir*step
                                            }
                                            // Rola a sequência durante arrastos próximos das bordas.
                                            val visible=listState.layoutInfo.visibleItemsInfo
                                                .firstOrNull { it.key==uri.toString() }
                                            if(visible!=null) {
                                                val y=visible.offset+change.position.y
                                                val upper=listState.layoutInfo.viewportStartOffset+40.dp.toPx()
                                                val lower=listState.layoutInfo.viewportEndOffset-40.dp.toPx()
                                                val delta=when { y<upper -> -26f; y>lower -> 26f; else -> 0f }
                                                if(delta!=0f) scope.launch { listState.scrollBy(delta.dp.toPx()) }
                                            }
                                        }
                                    },tint=if(dragging) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant)
                                val small by produceState<Bitmap?>(initialValue=null,uri) {
                                    value=withContext(Dispatchers.IO){readPreviewThumbnail(context,uri,90)}
                                }
                                if(small!=null) Image(small!!.asImageBitmap(),"Imagem ${index+1}",
                                    Modifier.size(40.dp).clip(RoundedCornerShape(5.dp)),contentScale=ContentScale.Crop)
                                else Icon(Icons.Default.Image,null,Modifier.size(40.dp))
                                Spacer(Modifier.width(5.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("Foto ${index+1}",style=MaterialTheme.typography.labelMedium)
                                    Text(uri.lastPathSegment?.substringAfterLast('/')?.take(22) ?: "Imagem",
                                        style=MaterialTheme.typography.labelSmall,maxLines=1,
                                        overflow=TextOverflow.Ellipsis)
                                }
                                IconButton(onClick={
                                    val from=images.indexOf(uri)
                                    if(from>0){images.removeAt(from);images.add(from-1,uri)
                                        onOrderChanged(images.toList());animated=false;previewMs=0}
                                },enabled=index>0,modifier=Modifier.size(36.dp)) {
                                    Icon(Icons.Default.KeyboardArrowUp,"Subir")
                                }
                                IconButton(onClick={
                                    val from=images.indexOf(uri)
                                    if(from>=0 && from<images.lastIndex){images.removeAt(from);images.add(from+1,uri)
                                        onOrderChanged(images.toList());animated=false;previewMs=0}
                                },enabled=index<images.lastIndex,modifier=Modifier.size(36.dp)) {
                                    Icon(Icons.Default.KeyboardArrowDown,"Descer")
                                }
                                IconButton(onClick={images.remove(uri);onOrderChanged(images.toList());animated=false;previewMs=0},
                                    modifier=Modifier.size(36.dp)) { Icon(Icons.Default.Close,"Remover") }
                            }
                        }
                    }
                }
                if (draftStatus == ImageDraftStatus.DAMAGED) {
                    Text("O rascunho salvo está ilegível e não há backup válido. " +
                        "A gravação foi bloqueada para não sobrescrever dados.",
                        color=MaterialTheme.colorScheme.error)
                    OutlinedButton(onClick={requestDraftReset=true}) {Text("Reiniciar cadastro danificado")}
                } else if (draftStatus == ImageDraftStatus.BACKUP) {
                    Text("Rascunho recuperado da cópia de segurança. Salve para confirmar a recuperação.",
                        color=MaterialTheme.colorScheme.primary)
                }
                if (unavailableImages == null && images.isNotEmpty()) {
                    Text("Verificando acesso às fotos selecionadas…",
                        style=MaterialTheme.typography.labelSmall)
                } else if (!unavailableImages.isNullOrEmpty()) {
                    val missing = unavailableImages.orEmpty()
                    Text("${missing.size} foto(s) sem permissão de leitura. " +
                        "O rascunho foi mantido; selecione novamente as fotos pelo seletor Android ou remova os itens inacessíveis.",
                        color=MaterialTheme.colorScheme.error)
                    OutlinedButton(onClick={
                        images.removeAll { it.toString() in missing }
                        onOrderChanged(images.toList());animated=false;previewMs=0
                    }) {Text("Remover somente fotos inacessíveis")}
                }
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick={advanced=true},modifier=Modifier.weight(1f)) {
                        Icon(Icons.Default.Tune,null,Modifier.size(18.dp)); Spacer(Modifier.width(4.dp))
                        Text("Personalizar",fontSize=13.sp,maxLines=1)
                    }
                    Button(onClick={animated=true;playing=true;previewMs=0},enabled=images.isNotEmpty(),
                        modifier=Modifier.weight(1f)) {
                        Icon(Icons.Default.AutoAwesome,null,Modifier.size(18.dp));Spacer(Modifier.width(4.dp))
                        Text("Ver prévia",fontSize=13.sp,maxLines=1)
                    }
                }
                if (images.isNotEmpty()) {
                    Text("${images.size} foto(s) · ${formatMs(total)} · ${options.width} × ${options.height} " +
                        "· ${options.secondsPerImage}s por foto · ${ImageMotionUi.label(options.motion)}",
                        style = MaterialTheme.typography.labelSmall)
                }
                if(images.isNotEmpty()) {
                    Button(onClick={
                        if (!onSaveDraft(images.toList(),options)) saveError=true
                        else {
                            saveError=false
                            lastSavedImages = images.toList();lastSavedOptions = options
                            draftStatus = draftStore.inspect().status
                            animationVm.start(images.toList(),options)
                        }
                    },modifier=Modifier.fillMaxWidth(),enabled=images.isNotEmpty() && allImagesReadable &&
                        draftStatus != ImageDraftStatus.DAMAGED) {
                        Icon(Icons.Default.Save,null);Spacer(Modifier.width(8.dp));Text("Gerar MP4")
                    }
                }
                Text("Depois de gerar o MP4, use o editor para adicionar músicas, narração, legendas, cortes e PiP.",
                    style=MaterialTheme.typography.labelSmall)
                if(saveError) Text("Não foi possível salvar a edição. Verifique o armazenamento.",
                    color=MaterialTheme.colorScheme.error)
                if (task.message.isNotBlank()) Text(task.message,color=MaterialTheme.colorScheme.error)
            }
        }
    }
    if (advanced) {
        ModalBottomSheet(onDismissRequest={advanced=false}) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal=16.dp,vertical=8.dp),
                verticalArrangement=Arrangement.spacedBy(10.dp)) {
                Text("Personalizar animação",style=MaterialTheme.typography.titleLarge)
                Text("Movimento das fotos",style=MaterialTheme.typography.titleMedium)
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement=Arrangement.spacedBy(6.dp),
                    contentPadding=PaddingValues(end=12.dp)) {
                    items(ImageMotionUi.choices.size) { index ->
                        val motion = ImageMotionUi.choices[index]
                        FilterChip(selected=options.motion==motion,
                            onClick={options=options.copy(motion=motion);animated=false},
                            label={Text(ImageMotionUi.label(motion))})
                    }
                }
                Text(ImageMotionUi.description(options.motion),style=MaterialTheme.typography.bodySmall)
                EditorOptionRow("Intensidade",listOf("suave","normal","intensa"),options.intensity) {
                    options=options.copy(intensity=it); animated=false
                }
                HorizontalDivider()
                Text("Vídeo e transições",style=MaterialTheme.typography.titleMedium)
                EditorOptionRow("Tempo/foto",listOf(3,5,8),options.secondsPerImage) {
                    options=options.copy(secondsPerImage=it); animated=false
                }
                EditorOptionRow("Formato",listOf("9:16","16:9","1:1"),options.aspect) {
                    options=options.copy(aspect=it); animated=false
                }
                EditorOptionRow("Resolução",listOf(720,1080),options.quality) {
                    options=options.copy(quality=it); animated=false
                }
                EditorOptionRow("Transição",listOf("auto","corte","preto","branco"),options.transition) {
                    options=options.copy(transition=it); animated=false
                }
                Text("30 FPS · H.264 · ${VideoExportQuality.videoBitrateBps(options.quality)/1_000_000} Mb/s solicitados · offline. " +
                    "As imagens são preparadas em PNG temporário e precisam de espaço disponível.",
                    style=MaterialTheme.typography.bodySmall)
                Button(onClick={advanced=false},modifier=Modifier.fillMaxWidth()) {Text("Concluir ajustes")}
                Spacer(Modifier.height(18.dp))
            }
        }
    }
    animationVm.savedVideo?.let { uri ->
        VideoSavedDialog(uri=uri,name="",editLabel="Editar com áudio, legendas e PiP",
            onSocialPublish = { video, name, draft ->
                animationVm.dismissSaved()
                onSocialPublish(video, name, draft)
            },
            onEdit={ draft -> if (onUseInEditor(uri, draft)) animationVm.dismissSaved() },
            onClose={ animationVm.dismissSaved() })
    }
}

/** Miniaturas com amostragem para Android 8+ e acesso MediaStore/SAF. */
private fun readPreviewThumbnail(context: android.content.Context, uri: Uri, maxSide: Int): Bitmap? = runCatching {
    if (Build.VERSION.SDK_INT >= 29) context.contentResolver.loadThumbnail(uri,Size(maxSide,maxSide),null)
    else {
        val bounds=BitmapFactory.Options().apply {inJustDecodeBounds=true}
        context.contentResolver.openInputStream(uri)?.use {BitmapFactory.decodeStream(it,null,bounds)}
        var sample=1
        while(bounds.outWidth/sample>maxSide*2 || bounds.outHeight/sample>maxSide*2) sample*=2
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it,null,BitmapFactory.Options().apply {inSampleSize=sample})
        }
    }
}.getOrNull()


/** A mesma composição de fundo/primeiro plano usada pelo exportador (ImageAnimationEngine).
 * A câmera, o preenchimento e o fade se aplicam ao conjunto, como no vídeo final.
 */
@Composable
private fun AnimationFrameImage(bitmap: Bitmap?, frame: CameraFrame,
    fadeColor: androidx.compose.ui.graphics.Color, fadeStrength: Float,
    currentIndex: Int, imageCount: Int, animated: Boolean, modifier: Modifier = Modifier
) {
    Box(modifier.clip(RoundedCornerShape(10.dp))
        .background(androidx.compose.ui.graphics.Color.Black),
        contentAlignment = Alignment.Center) {
        if (bitmap != null) {
            Box(Modifier.fillMaxSize().graphicsLayer {
                scaleX = frame.scale; scaleY = frame.scale
                translationX = frame.panX * size.width
                translationY = frame.panY * size.height
            }) {
                Image(bitmap.asImageBitmap(), null, Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop)
                Box(Modifier.fillMaxSize().background(
                    androidx.compose.ui.graphics.Color.Black.copy(alpha = .6f)))
                Image(bitmap.asImageBitmap(), "Foto ${currentIndex + 1}", Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit)
            }
        } else Icon(Icons.Default.Image, null, Modifier.size(65.dp),
            tint = androidx.compose.ui.graphics.Color.White)
        if (animated && fadeColor != androidx.compose.ui.graphics.Color.Transparent && fadeStrength > 0f) {
            Box(Modifier.fillMaxSize().background(fadeColor.copy(alpha = fadeStrength)))
        }
        if (animated && imageCount > 0) {
            Text("${currentIndex + 1} / $imageCount",
                Modifier.align(Alignment.TopEnd).padding(8.dp)
                    .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = .6f),
                        RoundedCornerShape(5.dp)).padding(5.dp),
                color = androidx.compose.ui.graphics.Color.White)
        }
    }
}
