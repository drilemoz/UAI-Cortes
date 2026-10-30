package br.com.uaicortes

import android.net.Uri
import android.graphics.Bitmap
import android.os.Build
import android.graphics.BitmapFactory
import android.util.Size
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/** Tela modular. O botão automático só planeja a animação; salvar gera o MP4. */
@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AnimateImagesScreen(
    selected: List<Uri>, onChoose: () -> Unit, onBack: () -> Unit,
    onOrderChanged: (List<Uri>) -> Unit, onUseInEditor: (Uri) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val engine = remember { ImageAnimationEngine(context.applicationContext) }
    val images = remember { mutableStateListOf<Uri>() }
    LaunchedEffect(selected) {
        // Mantém as fotos escolhidas e permite reordenar sem perder a seleção ao voltar da galeria.
        if (images.map { it.toString() } != selected.map { it.toString() }) {
            images.clear(); images.addAll(selected)
        }
    }
    var options by remember { mutableStateOf(ImageAnimationOptions()) }
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
    LaunchedEffect(busy) {
        while (busy) { progress = if (stage == "Exportando vídeo") engine.progress() else null; delay(250) }
    }
    DisposableEffect(Unit) {
        onDispose { if (!engine.isSaving) engine.cancel() }
    }
    fun goBack() { if (busy && !engine.isSaving) confirmCancel = true else if (!busy) onBack() }
    BackHandler { goBack() }
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
            IconButton(onClick={::goBack},enabled=!engine.isSaving) {Icon(Icons.Default.ArrowBack,"Voltar")}
            Text("Animar Imagens",style=MaterialTheme.typography.titleLarge,
                modifier=Modifier.weight(1f))
            IconButton(onClick=onChoose,enabled=!busy) {Icon(Icons.Default.AddPhotoAlternate,"Importar imagens")}
        }
        if (busy) {
            Column(Modifier.fillMaxSize().padding(22.dp),verticalArrangement=Arrangement.Center,
                horizontalAlignment=Alignment.CenterHorizontally) {
                Icon(Icons.Default.MovieCreation,null,Modifier.size(50.dp))
                Spacer(Modifier.height(16.dp))
                Text(stage,style=MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(12.dp))
                if (progress != null) {
                    LinearProgressIndicator(progress={progress!!.toFloat()/100f},modifier=Modifier.fillMaxWidth())
                    Text("${progress}%")
                } else LinearProgressIndicator(modifier=Modifier.fillMaxWidth())
                Text("O progresso é informado pelo Media3 quando disponível.",style=MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(18.dp))
                OutlinedButton(onClick={confirmCancel=true},enabled=!engine.isSaving) {
                    Text(if (engine.isSaving) "Salvando…" else "Cancelar")
                }
            }
        } else {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement=Arrangement.spacedBy(12.dp)) {
                val previewRatio=options.width.toFloat()/options.height
                Box(Modifier.fillMaxWidth().heightIn(max=390.dp).aspectRatio(previewRatio)
                    .clip(RoundedCornerShape(14.dp)).background(androidx.compose.ui.graphics.Color(0xFF171923)),
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
                Text("Máximo de 80 imagens por animação. Arraste pela alça ou use as setas para ordenar.",
                    style=MaterialTheme.typography.bodySmall)
                if (images.isEmpty()) {
                    Text("Escolha uma ou mais imagens na galeria para começar.")
                } else {
                    // Altura limitada evita renderizar todas as imagens em projetos grandes.
                    LazyColumn(Modifier.fillMaxWidth().heightIn(max=230.dp),
                        verticalArrangement=Arrangement.spacedBy(5.dp)) {
                        itemsIndexed(images,key={_,uri -> uri.toString()}) { index, uri ->
                            var dragDistance by remember(uri) { mutableFloatStateOf(0f) }
                            Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(9.dp)).padding(8.dp),verticalAlignment=Alignment.CenterVertically) {
                                Icon(Icons.Default.DragHandle,"Arraste para reordenar",
                                    Modifier.size(36.dp).pointerInput(uri,images.size) {
                                        detectDragGestures(onDragEnd={
                                            val delta=(dragDistance/64.dp.toPx()).roundToInt()
                                            val from=images.indexOf(uri)
                                            if(from>=0 && delta!=0) {
                                                val to=(from+delta).coerceIn(0,images.lastIndex)
                                                images.removeAt(from);images.add(to,uri)
                                                onOrderChanged(images.toList())
                                                animated=false;previewMs=0
                                            }
                                            dragDistance=0f
                                        },onDragCancel={dragDistance=0f}) { change,drag ->
                                            change.consume(); dragDistance += drag.y
                                        }
                                    })
                                val tiny by produceState<Bitmap?>(initialValue=null,uri) {
                                    value=withContext(Dispatchers.IO) { readPreviewThumbnail(context,uri,90) }
                                }
                                if (tiny!=null) Image(tiny!!.asImageBitmap(),"Imagem ${index+1}",
                                    modifier=Modifier.size(42.dp).clip(RoundedCornerShape(5.dp)),
                                    contentScale=ContentScale.Crop)
                                else Icon(Icons.Default.Image,null,Modifier.size(42.dp))
                                Spacer(Modifier.width(5.dp))
                                Text("${index+1}.",modifier=Modifier.width(27.dp))
                                Text(uri.lastPathSegment?.substringAfterLast('/')?.take(45) ?: "Imagem",
                                    modifier=Modifier.weight(1f),maxLines=1,overflow=TextOverflow.Ellipsis)
                                IconButton(onClick={if(index>0) {images.removeAt(index);images.add(index-1,uri);onOrderChanged(images.toList());animated=false} }) {
                                    Icon(Icons.Default.KeyboardArrowUp,"Mover para cima")
                                }
                                IconButton(onClick={if(index<images.lastIndex){images.removeAt(index);images.add(index+1,uri);onOrderChanged(images.toList());animated=false}}) {
                                    Icon(Icons.Default.KeyboardArrowDown,"Mover para baixo")
                                }
                                IconButton(onClick={images.remove(uri);onOrderChanged(images.toList());animated=false}) {Icon(Icons.Default.Close,"Remover da sequência")}
                            }
                        }
                    }
                }
                OutlinedButton(onClick={advanced=!advanced},modifier=Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Tune,null);Spacer(Modifier.width(8.dp))
                    Text(if(advanced) "Ocultar ajustes" else "Ajustes opcionais")
                }
                if(advanced) {
                    optionRow("Intensidade",listOf("suave","normal","intensa"),options.intensity){options=options.copy(intensity=it);animated=false}
                    optionRow("Duração por imagem",listOf(3,5,8),options.secondsPerImage){options=options.copy(secondsPerImage=it);animated=false}
                    optionRow("Formato",listOf("9:16","16:9","1:1"),options.aspect){options=options.copy(aspect=it);animated=false}
                    optionRow("Qualidade",listOf(720,1080),options.quality){options=options.copy(quality=it);animated=false}
                    optionRow("Transições",listOf("auto","corte","preto","branco"),options.transition){options=options.copy(transition=it);animated=false}
                    Text("30 FPS · H.264 · somente no aparelho",style=MaterialTheme.typography.bodySmall)
                }
                Button(onClick={animated=true;playing=true;previewMs=0},enabled=images.isNotEmpty(),
                    modifier=Modifier.fillMaxWidth().height(54.dp)) {
                    Icon(Icons.Default.AutoAwesome,null); Spacer(Modifier.width(8.dp))
                    Text("Animar automaticamente")
                }
                if(animated) {
                    Button(onClick={
                        error=null;saved=null;busy=true;stage="Preparando imagens";progress=null
                        val snapshot=images.toList()
                        job=scope.launch {
                            try {engine.start(snapshot,options,{stage=it},{uri->saved=uri;busy=false},
                                {reason->error=reason;busy=false})}
                            catch (e:CancellationException) {engine.cancel();busy=false}
                            catch (e:Exception) {DiagnosticRecorder.exception("image_animation",e)
                                error=e.message ?: "Erro ao gerar animação";busy=false;engine.cancel()}
                        }
                    },modifier=Modifier.fillMaxWidth(),enabled=images.isNotEmpty()) {
                        Icon(Icons.Default.Save,null); Spacer(Modifier.width(8.dp)); Text("Salvar vídeo MP4")
                    }
                }
                if(saved!=null) {
                    Text("Vídeo animado salvo em Filmes/UaiCortes",color=MaterialTheme.colorScheme.primary)
                    Button(onClick={onUseInEditor(saved!!) },modifier=Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.VideoLibrary,null);Spacer(Modifier.width(8.dp));Text("Enviar para timeline do editor")
                    }
                }
                error?.let {Text("Erro: $it",color=MaterialTheme.colorScheme.error)}
            }
        }
    }
}

@Composable
private fun <T> optionRow(title:String, values:List<T>, selected:T, choose:(T)->Unit) {
    Text(title,style=MaterialTheme.typography.labelLarge)
    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
        items(values.size) { index ->
            val value=values[index]
            FilterChip(selected=selected==value,onClick={choose(value)},label={Text(value.toString())})
        }
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
