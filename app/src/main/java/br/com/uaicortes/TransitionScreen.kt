package br.com.uaicortes

import android.net.Uri
import android.os.Build
import android.media.MediaMetadataRetriever
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Extraído da MainActivity na v0.1.53 para reutilização de componentes.

/** Aproxima a tintura das transições na prévia do ExoPlayer.
 * Preto e branco/sépia têm prévia correta na tela Transições com quadros estáticos;
 * a renderização em vídeo usa Media3 no MP4 exportado.
 */
@Composable
internal fun TransitionPreviewTint(incoming: ClipTransition?, incomingMs: Long,
    outgoing: ClipTransition?, outgoingMs: Long, localMs: Long, clipDurationMs: Long) {
    val incomingKind=incoming?.kind ?: TransitionEffects.CUT
    val outgoingKind=outgoing?.kind ?: TransitionEffects.CUT
    val at = localMs.coerceIn(0L,clipDurationMs.coerceAtLeast(0L))
    val inStrength = if (incomingMs > 0L) TransitionEffects.intensity(incomingKind,
        ((incomingMs - at).toFloat() / incomingMs).coerceIn(0f,1f)) else 0f
    val outStrength = if (outgoingMs > 0L) TransitionEffects.intensity(outgoingKind,
        ((at - (clipDurationMs - outgoingMs)).toFloat() / outgoingMs).coerceIn(0f,1f)) else 0f
    val kind = if (inStrength >= outStrength) incomingKind else outgoingKind
    val strength = maxOf(inStrength,outStrength)
    val tint = when (kind) {
        TransitionEffects.BLACK,TransitionEffects.FLASH_BLACK -> Color.Black
        TransitionEffects.WHITE,TransitionEffects.FLASH_WHITE -> Color.White
        TransitionEffects.BLUE -> Color(0xFF1448F2)
        TransitionEffects.GOLD -> Color(0xFFFFAD2E)
        else -> null
    }
    if (tint != null && strength > .001f)
        Box(Modifier.fillMaxSize().background(tint.copy(alpha=strength)))
}

/** Prévia ilustrada com quadros reais: não decodifica ambos simultaneamente.
 * Usa a mesma matriz de cor e a mesma curva temporal do exportador.
 */
@Composable
internal fun TransitionFramePreview(a: Clip, b: Clip, kind: String, durationMs: Long) {
    val context = LocalContext.current
    val frames by produceState<Pair<ImageBitmap?, ImageBitmap?>>(null to null, a.uri, b.uri, a.endMs, b.startMs) {
        value = withContext(Dispatchers.IO) {
            fun frame(clip: Clip, atMs: Long): ImageBitmap? = runCatching {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, Uri.parse(clip.uri))
                    val timeUs = atMs.coerceAtLeast(0L) * 1000L
                    val bitmap = if (Build.VERSION.SDK_INT >= 27) {
                        retriever.getScaledFrameAtTime(timeUs,
                            MediaMetadataRetriever.OPTION_CLOSEST_SYNC, 320, 180)
                    } else {
                        retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    }
                    bitmap?.asImageBitmap()
                } finally { retriever.release() }
            }.getOrNull()
            frame(a, (a.endMs - 100L).coerceAtLeast(a.startMs)) to frame(b, b.startMs)
        }
    }
    var progress by remember(a.id, b.id, kind, durationMs) { mutableFloatStateOf(0f) }
    LaunchedEffect(a.id, b.id, kind, durationMs) {
        while (true) {
            val total = (durationMs.coerceIn(250L, 1000L) * 2L).coerceAtLeast(500L)
            val steps = (total / 40L).toInt().coerceAtLeast(1)
            for (step in 0..steps) {
                progress = step.toFloat() / steps
                delay(40L)
            }
            delay(450L)
        }
    }
    val first = progress < .5f
    val nearCut = if (first) progress * 2f else (1f - progress) * 2f
    val intensity = TransitionEffects.intensity(kind, nearCut)
    val filter = androidx.compose.ui.graphics.ColorFilter.colorMatrix(
        androidx.compose.ui.graphics.ColorMatrix(
            TransitionEffects.composeValues(TransitionEffects.matrix(kind, intensity))))
    Box(Modifier.fillMaxWidth().height(175.dp).clip(RoundedCornerShape(12.dp))
        .background(Color(0xFF222222)), contentAlignment = Alignment.Center) {
        val bitmap = if (first) frames.first else frames.second
        if (bitmap != null) Image(bitmap, "Quadro da transição",
            Modifier.fillMaxSize().graphicsLayer {
                val zoom = TransitionEffects.motionScale(kind, intensity)
                scaleX = zoom; scaleY = zoom
                translationX = TransitionEffects.motionX(kind, intensity) * size.width / 2f
            }, contentScale = ContentScale.Fit, colorFilter = filter)
        else Text(if (first) "Clipe A" else "Clipe B", color = Color.White)
    }
    Text("Simulação com quadros dos dois clipes (sem áudio); exportação Media3 mantém a sequência.",
        style = MaterialTheme.typography.bodySmall)
}

@Composable
internal fun TransitionScreen(p:VideoProject,back:()->Unit,update:(VideoProject)->Unit,initialBoundary:Int=0) {
    var boundary by remember(p.id,initialBoundary) {
        mutableIntStateOf(TransitionBoundaryPolicy.clamp(initialBoundary, p.clips.size))
    }
    // A lista de clipes pode mudar sem que o ID do projeto mude.
    val safeBoundary = TransitionBoundaryPolicy.clamp(boundary, p.clips.size)
    LaunchedEffect(p.clips.size) { boundary = TransitionBoundaryPolicy.clamp(boundary, p.clips.size) }
    fun stored(index: Int) = EditFeatures.boundary(p, index)
    var kind by remember(p.id, initialBoundary) {
        mutableStateOf(stored(boundary)?.kind?.takeIf(TransitionEffects::supported) ?: TransitionEffects.CUT)
    }
    var fadeMs by remember(p.id,initialBoundary) {
        mutableFloatStateOf((stored(boundary)?.durationMs ?: 500L).coerceIn(250L,1000L).toFloat())
    }
    var applyAll by remember(p.id) { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().statusBarsPadding().imePadding()
        .padding(horizontal=16.dp), verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Heading("Transições", back)
        if (p.clips.size < 2) {
            Text("Adicione pelo menos dois clipes para criar uma transição.")
        } else {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement=Arrangement.spacedBy(10.dp)) {
                Text("Junção entre clipes",fontWeight=FontWeight.Medium)
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                    items(p.clips.lastIndex) { index ->
                        val a=p.clips[index];val b=p.clips[index+1]
                        FilterChip(selected=safeBoundary==index,onClick={
                            boundary=index
                            kind=stored(index)?.kind?.takeIf(TransitionEffects::supported) ?: TransitionEffects.CUT
                            fadeMs=(stored(index)?.durationMs ?: 500L).coerceIn(250L,1000L).toFloat()
                        },label={Text("${index+1} → ${index+2}: ${a.title.take(10)} / ${b.title.take(10)}")})
                    }
                }
                TransitionFramePreview(p.clips[safeBoundary],p.clips[safeBoundary+1],kind,fadeMs.toLong())
                Text("Efeito",fontWeight=FontWeight.Bold)
                Text("Escolha o efeito. As mudanças só são gravadas ao tocar em Aplicar.",
                    style=MaterialTheme.typography.bodySmall)
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    items(TransitionEffects.names.size) { index ->
                        val value=TransitionEffects.names[index]
                        val selected=kind==value
                        Surface(onClick={kind=value},shape=RoundedCornerShape(9.dp),
                            color=if(selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            border=BorderStroke(if(selected) 2.dp else 1.dp,
                                if(selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)) {
                            Column(Modifier.width(105.dp).padding(7.dp),
                                horizontalAlignment=Alignment.CenterHorizontally,
                                verticalArrangement=Arrangement.spacedBy(5.dp)) {
                                Box(Modifier.fillMaxWidth().height(48.dp)
                                    .background(Color(0xFF292929),RoundedCornerShape(5.dp)),
                                    contentAlignment=Alignment.Center) {
                                    Text(when (value) {
                                        TransitionEffects.CUT -> "A | B"
                                        TransitionEffects.ZOOM_IN -> "A ⊕ B"
                                        TransitionEffects.ZOOM_OUT -> "A ⊖ B"
                                        TransitionEffects.PAN -> "A → B"
                                        TransitionEffects.MONO -> "A ◐ B"
                                        TransitionEffects.SEPIA -> "A ◒ B"
                                        TransitionEffects.FLASH_WHITE,TransitionEffects.FLASH_BLACK -> "A ✦ B"
                                        else -> "A ▪ B"
                                    },color=Color.White)
                                }
                                Text(value,fontSize=11.sp,maxLines=2,textAlign=TextAlign.Center,
                                    minLines=2)
                            }
                        }
                    }
                }
                Text(TransitionEffects.description(kind),
                    style=MaterialTheme.typography.bodySmall)
                if (kind != TransitionEffects.CUT) {
                    Text("Duração: ${fadeMs.toInt()} ms",fontWeight=FontWeight.Medium)
                    Slider(value=fadeMs,valueRange=250f..1000f,steps=2,
                        onValueChange={fadeMs=it})
                }
                Text("Os efeitos atuam na saída e entrada dos vídeos. Não sobrepõem clipes nem alteram sua duração.",
                    style=MaterialTheme.typography.bodySmall,
                    color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            HorizontalDivider()
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Checkbox(checked=applyAll,onCheckedChange={applyAll=it})
                Column(Modifier.weight(1f)) {
                    Text("Aplicar a todas as junções",fontWeight=FontWeight.Medium)
                    Text("${p.clips.lastIndex} ${if(p.clips.lastIndex==1) "junção" else "junções"} entre vídeos",
                        style=MaterialTheme.typography.bodySmall)
                }
            }
            Button(onClick={
                update(TransitionBatch.apply(p,safeBoundary,kind,fadeMs.toLong(),applyAll))
                back()
            }, modifier=Modifier.fillMaxWidth().padding(bottom=8.dp)) {
                Icon(Icons.Default.Check,null)
                Spacer(Modifier.width(8.dp))
                Text("Aplicar")
            }
        }
    }
}

