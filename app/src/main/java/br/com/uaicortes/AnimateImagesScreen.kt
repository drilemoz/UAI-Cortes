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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

/** Tela modular. O botão automático só planeja a animação; salvar gera o MP4. */
@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AnimateImagesScreen(
    selected: List<Uri>, onChoose: () -> Unit, onBack: () -> Unit,
    onOrderChanged: (List<Uri>) -> Unit, onUseInEditor: (Uri) -> Unit,
    onSaveDraft: (List<Uri>, ImageAnimationOptions) -> Boolean
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val engine = remember { ImageAnimationEngine(context.applicationContext) }
    val images = remember { mutableStateListOf<Uri>() }
    val listState=rememberLazyListState()
    LaunchedEffect(selected) {
        // Mantém as fotos escolhidas e permite reordenar sem perder a seleção ao voltar da galeria.
        if (images.map { it.toString() } != selected.map { it.toString() }) {
            images.clear(); images.addAll(selected)
        }
    }
    val originalDraft = remember { ImageAnimationDraftStore(context).load() }
    var options by remember { mutableStateOf(originalDraft?.options ?: ImageAnimationOptions()) }
    var lastSavedImages by remember { mutableStateOf(selected.toList()) }
    var lastSavedOptions by remember { mutableStateOf(options) }
    var confirmExit by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf(false) }
    var advanced by remember { mutableStateOf(false) }
    var animated by remember { mutableStateOf(false) }
    var playing by remember { mutableStateOf(true) }
    var previewMs by remember { mutableLongStateOf(0L) }
    var busy by remember { mutableStateOf(false) }
    var stage by remember { mutableStateOf("") }
    var progress by remember { mutableStateOf<Int?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var saved by remember { mutableStateOf<Uri?>(null) }
    var confirmCancel by remember { mutableStateOf(false) }
    var job by remember { mutableStateOf<Job?>(null) }
    val total = ImageAnimationPlan.totalDuration(images.size,options)
    LaunchedEffect(animated, playing, busy, images.size, options) {
        if (animated && playing && !busy && total > 0L) {
            while (true) { delay(33); previewMs = (previewMs + 33) % total }
        }
    }
    LaunchedEffect(busy,stage) {
        if (!busy) progress=null
        else while (true) {
            progress=if(stage=="Exportando vídeo") runCatching { engine.progress() }.getOrNull() else null
            delay(250)
        }
    }
    DisposableEffect(Unit) {
        onDispose { if (!engine.isSaving) engine.cancel() }
    }
    fun saveDraftAndReturn() {
        if (onSaveDraft(images.toList(), options)) {
            lastSavedImages = images.toList()
            lastSavedOptions = options
            saveError = false
            onBack()
        } else saveError = true
    }
    fun goBack() {
        if (busy) {
            if (!engine.isSaving) confirmCancel=true
            else android.widget.Toast.makeText(context,"O Uai Cortes está finalizando o vídeo.",
                android.widget.Toast.LENGTH_SHORT).show()
        } else if (ImageAnimationDraftRules.changed(lastSavedImages.map(Uri::toString),
            lastSavedOptions,images.map(Uri::toString),options)) confirmExit=true
        else onBack()
    }
    BackHandler { goBack() }
    if (confirmExit) AlertDialog(onDismissRequest={confirmExit=false},
        title={Text("Salvar edição das imagens?")},
        text={Text("Guarde a sequência e os ajustes para continuar depois. Isso não exporta um MP4.")},
        confirmButton={TextButton(onClick={confirmExit=false;saveDraftAndReturn()}) {Text("Salvar e voltar")} },
        dismissButton={Row {
            TextButton(onClick={confirmExit=false;onBack()}) {Text("Descartar alterações")}
            TextButton(onClick={confirmExit=false}) {Text("Continuar")}
        }})
    if (confirmCancel) AlertDialog(onDismissRequest={confirmCancel=false},
        title={Text("Cancelar a animação?")},
        text={Text("O vídeo parcial será removido. As imagens selecionadas permanecem intactas.")},
        confirmButton={TextButton(onClick={confirmCancel=false;job?.cancel();engine.cancel();busy=false;stage=""}) {Text("Cancelar exportação")} },
        dismissButton={TextButton(onClick={confirmCancel=false}) {Text("Continuar")} })

    val currentIndex = if (images.isEmpty()) 0 else
        (previewMs / options.durationMs).toInt().coerceIn(0,images.lastIndex)
    val localFraction = if (options.durationMs > 0) (previewMs % options.durationMs).toFloat()/options.durationMs else 0f
    val frame = ImageAnimationPlan.frame(currentIndex,localFraction,options.intensity)
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
    val bitmap by produceState<Bitmap?>(initialValue=null,itemUri) {
        value = itemUri?.let { uri -> withContext(Dispatchers.IO) {
            readPreviewThumbnail(context,uri,640)
        } }
    }
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().height(56.dp),verticalAlignment=Alignment.CenterVertically) {
            IconButton(onClick={::goBack}) {Icon(Icons.Default.ArrowBack,"Voltar")}
            Text("Animar Imagens",style=MaterialTheme.typography.titleLarge,
                modifier=Modifier.weight(1f))
            TextButton(onClick={::saveDraftAndReturn},enabled=!busy) {
                Icon(Icons.Default.Save,null,Modifier.size(18.dp))
                Spacer(Modifier.width(3.dp))
                Text("Salvar",maxLines=1)
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
                Text(AnimationProgressUi.title(stage),style=MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(12.dp))
                val fixedProgress=AnimationProgressUi.fraction(progress)
                if(fixedProgress==null) LinearProgressIndicator(modifier=Modifier.fillMaxWidth())
                else {
                    LinearProgressIndicator(progress={fixedProgress},modifier=Modifier.fillMaxWidth())
                    Text("${(fixedProgress*100).toInt()}%")
                }
                Text("O Uai Cortes está preparando seu vídeo. Progresso real quando disponível.",
                    style=MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(18.dp))
                OutlinedButton(onClick={confirmCancel=true},enabled=!engine.isSaving) {
                    Text(if (engine.isSaving) "Salvando…" else "Cancelar")
                }
            }
        } else {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement=Arrangement.spacedBy(12.dp)) {
                val previewRatio=options.width.toFloat()/options.height
                BoxWithConstraints(Modifier.fillMaxWidth().height(238.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(androidx.compose.ui.graphics.Color(0xFF171923)),
                    contentAlignment=Alignment.Center) {
                    val fittedWidth=minOf(maxWidth,238.dp*previewRatio)
                    val fittedHeight=fittedWidth/previewRatio
                    Box(Modifier.size(fittedWidth,fittedHeight).clip(RoundedCornerShape(10.dp)),
                        contentAlignment=Alignment.Center) {
                    if (bitmap != null) {
                        // Prévia leve; usa a mesma curva temporal dos quadros Media3.
                        Image(bitmap!!.asImageBitmap(),"Prévia da imagem ${currentIndex+1}",
                            Modifier.fillMaxSize().graphicsLayer {
                                scaleX=frame.scale;scaleY=frame.scale
                                translationX=frame.panX*size.width;translationY=frame.panY*size.height
                            },contentScale=ContentScale.Fit)
                    } else Icon(Icons.Default.Image,null,Modifier.size(65.dp),
                        tint=androidx.compose.ui.graphics.Color.White)
                    if (animated && fadeColor != androidx.compose.ui.graphics.Color.Transparent && fadeStrength>0f) {
                        Box(Modifier.fillMaxSize().background(fadeColor.copy(alpha=fadeStrength)))
                    }
                    if (animated && images.isNotEmpty()) {
                        Text("${currentIndex+1} / ${images.size}",
                            Modifier.align(Alignment.TopEnd).padding(8.dp).background(
                                androidx.compose.ui.graphics.Color.Black.copy(alpha=.6f),RoundedCornerShape(5.dp))
                                .padding(5.dp),color=androidx.compose.ui.graphics.Color.White)
                    }
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
                    Text("Prévia leve dos movimentos e fades; confira o MP4 final para avaliar nitidez.",
                        style=MaterialTheme.typography.bodySmall)
                }
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Text("Imagens (${images.size})",style=MaterialTheme.typography.titleMedium,modifier=Modifier.weight(1f))
                    TextButton(onClick=onChoose) {Text("+ Importar")}
                }
                if(images.isEmpty()) Text("Escolha fotos na galeria para começar.",style=MaterialTheme.typography.bodySmall)
                else {
                    Text("Segure ☰ e arraste para reorganizar. Até 80 imagens.",
                        style=MaterialTheme.typography.labelSmall)
                    LazyColumn(Modifier.fillMaxWidth().heightIn(max=202.dp),state=listState,
                        verticalArrangement=Arrangement.spacedBy(5.dp)) {
                        itemsIndexed(images,key={_,uri->uri.toString()}) { index,uri ->
                            var dragPx by remember(uri) { mutableFloatStateOf(0f) }
                            var dragging by remember(uri) { mutableStateOf(false) }
                            Row(Modifier.fillMaxWidth().zIndex(if(dragging) 1f else 0f)
                                .graphicsLayer { translationY=dragPx }
                                .background(MaterialTheme.colorScheme.surfaceVariant,RoundedCornerShape(8.dp))
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
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick={advanced=!advanced},modifier=Modifier.weight(1f)) {
                        Icon(Icons.Default.Tune,null,Modifier.size(18.dp)); Spacer(Modifier.width(4.dp))
                        Text(if(advanced) "Ocultar" else "Ajustes",fontSize=13.sp,maxLines=1)
                    }
                    Button(onClick={animated=true;playing=true;previewMs=0},enabled=images.isNotEmpty(),
                        modifier=Modifier.weight(1f)) {
                        Icon(Icons.Default.AutoAwesome,null,Modifier.size(18.dp));Spacer(Modifier.width(4.dp))
                        Text("Animar",fontSize=13.sp,maxLines=1)
                    }
                }
                if(advanced) {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(10.dp),verticalArrangement=Arrangement.spacedBy(2.dp)) {
                            EditorOptionRow("Intensidade",listOf("suave","normal","intensa"),options.intensity){options=options.copy(intensity=it);animated=false}
                            EditorOptionRow("Duração",listOf(3,5,8),options.secondsPerImage){options=options.copy(secondsPerImage=it);animated=false}
                            EditorOptionRow("Formato",listOf("9:16","16:9","1:1"),options.aspect){options=options.copy(aspect=it);animated=false}
                            EditorOptionRow("Qualidade",listOf(720,1080),options.quality){options=options.copy(quality=it);animated=false}
                            EditorOptionRow("Transição",listOf("auto","corte","preto","branco"),options.transition){options=options.copy(transition=it);animated=false}
                            Text("30 FPS · H.264 · ${VideoExportQuality.videoBitrateBps(options.quality)/1_000_000} Mb/s solicitados · offline. " +
                                "Imagens preparadas sem perdas (PNG temporário); exige mais espaço livre.",
                                style=MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                if(animated) {
                    Button(onClick={
                        error=null
                        if (!onSaveDraft(images.toList(),options)) saveError=true
                        else {
                            saveError=false
                            lastSavedImages = images.toList();lastSavedOptions = options
                            saved=null;busy=true;stage="Preparando imagens";progress=null
                            val snapshot=images.toList()
                            job=scope.launch {
                                try {engine.start(snapshot,options,{stage=it},{uri->saved=uri;busy=false},
                                    {reason->error=reason;busy=false})}
                                catch(e:CancellationException){engine.cancel();busy=false}
                                catch(e:Exception){DiagnosticRecorder.exception("image_animation",e)
                                    error=e.message ?: "Erro ao gerar animação";busy=false;engine.cancel()}
                            }
                        }
                    },modifier=Modifier.fillMaxWidth(),enabled=images.isNotEmpty()) {
                        Icon(Icons.Default.Save,null);Spacer(Modifier.width(8.dp));Text("Salvar MP4")
                    }
                }
                Text("Depois de gerar o MP4, use o editor para adicionar músicas, narração, legendas, cortes e PiP.",
                    style=MaterialTheme.typography.labelSmall)
                if(saveError) Text("Não foi possível salvar a edição. Verifique o armazenamento.",
                    color=MaterialTheme.colorScheme.error)
                error?.let {Text("Erro: $it",color=MaterialTheme.colorScheme.error)}
            }
        }
    }
    saved?.let { uri ->
        VideoSavedDialog(uri=uri,name="",editLabel="Editar com áudio, legendas e PiP",
            onEdit={ onUseInEditor(uri) }, onClose={ saved=null })
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
