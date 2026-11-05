package br.com.uaicortes

import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

/** Mapeamento visual sem depender de Compose ou de codec para permitir testes JVM. */
internal object TrimVisualMath {
    fun timeAt(x: Float, width: Float, duration: Long): Long {
        if (width <= 0f || duration <= 0) return 0L
        return (((x / width).coerceIn(0f, 1f).toDouble()) * duration.toDouble()).toLong().coerceIn(0L, duration)
    }
    fun fraction(time: Long, duration: Long): Float =
        if (duration <= 0L) 0f else (time.toDouble() / duration).toFloat().coerceIn(0f, 1f)

    fun selection(start: Long, end: Long, touched: Long, target: Int,
        minStart: Long, maxEnd: Long): Pair<Long, Long> {
        val minMs = TrimLogic.MIN_SEGMENT_MS
        return when (target) {
            1 -> touched.coerceIn(minStart, end - minMs) to end
            2 -> start to touched.coerceIn(start + minMs, maxEnd)
            else -> start to end
        }
    }
}

/** Uma única régua: tocar ou deslizar procura o quadro, arrastar as alças altera a seleção. */
@Composable
internal fun TrimVisualTimeline(
    uri: String, durationMs: Long, clipStartMs: Long, clipEndMs: Long,
    cursorMs: Long, rangeStartMs: Long, rangeEndMs: Long, editMode: Boolean,
    removing: Boolean, onSeek: (Long) -> Unit,
    onRangeChange: (Long, Long, Long) -> Unit
) {
    val context = LocalContext.current
    val frames by produceState<List<ImageBitmap?>>(List(12) { null }, uri, durationMs) {
        value = withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, Uri.parse(uri))
                (0 until 12).map { i ->
                    runCatching {
                        val frameUs = (durationMs.coerceAtLeast(1L) * (i * 2L + 1L) / 24L) * 1000L
                        val bitmap = if (Build.VERSION.SDK_INT >= 27) {
                            retriever.getScaledFrameAtTime(frameUs,
                                MediaMetadataRetriever.OPTION_CLOSEST_SYNC, 128, 90)
                        } else retriever.getFrameAtTime(frameUs,
                            MediaMetadataRetriever.OPTION_CLOSEST_SYNC)?.let { original ->
                                val scaled = android.graphics.Bitmap.createScaledBitmap(original, 128, 90, true)
                                if (scaled !== original) original.recycle()
                                scaled
                            }
                        bitmap?.asImageBitmap()
                    }.getOrNull()
                }
            } catch (_: Exception) { List(12) { null } }
            finally { runCatching { retriever.release() } }
        }
    }
    val currentSeek by rememberUpdatedState(onSeek)
    val currentRange by rememberUpdatedState(onRangeChange)
    val currentBegin by rememberUpdatedState(rangeStartMs)
    val currentEnd by rememberUpdatedState(rangeEndMs)
    val currentMode by rememberUpdatedState(editMode)
    val density = LocalDensity.current
    val hitPx = with(density) { 28.dp.toPx() }
    val selectionColor = if (removing) Color(0xFFF59E0B) else Color(0xFFB599FF)
    var dragTarget by remember(uri) { mutableIntStateOf(0) }
    BoxWithConstraints(Modifier.fillMaxWidth().height(94.dp)
        .clip(RoundedCornerShape(14.dp)).background(Color(0xFF141418))) {
        val widthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        Row(Modifier.fillMaxSize()) {
            repeat(12) { index ->
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    val frame = frames.getOrNull(index)
                    if (frame != null) Image(frame, "Quadro ${index + 1}",
                        Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    else Box(Modifier.fillMaxSize().background(Color(0xFF252530))) {
                        Icon(Icons.Default.Movie, null, tint = Color(0xFF777788),
                            modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
        Canvas(Modifier.fillMaxSize()) {
            val x0 = TrimVisualMath.fraction(rangeStartMs, durationMs) * size.width
            val x1 = TrimVisualMath.fraction(rangeEndMs, durationMs) * size.width
            if (x0 > 0f) drawRect(Color.Black.copy(alpha = .63f),
                size = Size(x0, size.height))
            if (x1 < size.width) drawRect(Color.Black.copy(alpha = .63f),
                topLeft = Offset(x1, 0f), size = Size(size.width - x1, size.height))
            drawRect(selectionColor, topLeft = Offset(x0, 0f),
                size = Size((x1 - x0).coerceAtLeast(0f), size.height),
                style = Stroke(width = 3.dp.toPx()))
            if (editMode) {
                listOf(x0, x1).forEach { x ->
                    drawRoundRect(selectionColor,
                        topLeft = Offset((x - 7.dp.toPx()).coerceIn(0f, size.width - 14.dp.toPx()), 9.dp.toPx()),
                        size = Size(14.dp.toPx(), size.height - 18.dp.toPx()),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()))
                }
            }
            val cursorX = TrimVisualMath.fraction(cursorMs, durationMs) * size.width
            drawLine(Color(0xFF4EE3E2), Offset(cursorX, 0f),
                Offset(cursorX, size.height), strokeWidth = 3.dp.toPx())
        }
        Box(Modifier.fillMaxSize().semantics {
            contentDescription = "Linha do tempo. Arraste para escolher o momento; no modo de edição arraste as alças das extremidades."
        }.pointerInput(uri, durationMs, clipStartMs, clipEndMs) {
            detectDragGestures(onDragStart = { offset ->
                val startX = TrimVisualMath.fraction(currentBegin, durationMs) * widthPx
                val endX = TrimVisualMath.fraction(currentEnd, durationMs) * widthPx
                dragTarget = if (currentMode) {
                    when {
                        abs(offset.x - startX) <= hitPx && abs(offset.x - startX) <= abs(offset.x - endX) -> 1
                        abs(offset.x - endX) <= hitPx -> 2
                        else -> 0
                    }
                } else 0
                if (dragTarget == 0) currentSeek(TrimVisualMath.timeAt(offset.x, widthPx, durationMs))
            }, onDrag = { change, _ ->
                change.consume()
                val touched = TrimVisualMath.timeAt(change.position.x, widthPx, durationMs)
                if (dragTarget == 0) currentSeek(touched)
                else {
                    val (begin, end) = TrimVisualMath.selection(currentBegin, currentEnd,
                        touched, dragTarget, clipStartMs, clipEndMs)
                    currentRange(begin, end, touched)
                }
            }, onDragEnd = { dragTarget = 0 }, onDragCancel = { dragTarget = 0 })
        }.pointerInput(uri, durationMs) {
            detectTapGestures { offset -> currentSeek(TrimVisualMath.timeAt(offset.x, widthPx, durationMs)) }
        })
    }
}
