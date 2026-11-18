package br.com.uaicortes

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt

@Composable
internal fun SubtitleOverlayOnFrame(cue: SubtitleCue, editable: Boolean,
    playbackMs: Long = cue.startMs, onPositionCommit: (Float, Float) -> Unit = { _, _ -> }) {
    var frameSize by remember { mutableStateOf(IntSize.Zero) }
    var draftX by remember(cue.id) { mutableFloatStateOf(cue.x) }
    var draftY by remember(cue.id) { mutableFloatStateOf(cue.y) }
    var dragging by remember(cue.id) { mutableStateOf(false) }
    val latestCue by rememberUpdatedState(cue)
    val latestCommit by rememberUpdatedState(onPositionCommit)
    LaunchedEffect(cue.id, cue.x, cue.y) {
        if (!dragging) {
            draftX = cue.x
            draftY = cue.y
        }
    }
    val renderer = remember { SubtitleSingleLineRenderer() }
    Box(Modifier.fillMaxSize().onSizeChanged { frameSize = it }) {
        if (frameSize.width >= 16 && frameSize.height > 0) {
            val frame = SubtitleAnimations.frame(cue, playbackMs)
            val bitmap = renderer.render(cue, frame, frameSize.width)
            if (bitmap != null && frame.alpha > 0f && frame.scale > 0f) {
                val position = SubtitleSingleLinePolicy.place(draftX, draftY, frame.slideX, frame.slideY,
                    frame.scale, bitmap.width, bitmap.height, frameSize.width, frameSize.height)
                val width = bitmap.width * position.scale
                val height = bitmap.height * position.scale
                val density = LocalDensity.current
                val image = remember(bitmap) { bitmap.asImageBitmap() }
                Image(image, contentDescription = SubtitleSingleLinePolicy.display(frame.text),
                    alpha = frame.alpha.coerceIn(0f, 1f), contentScale = ContentScale.FillBounds,
                    modifier = Modifier.offset {
                        IntOffset((position.centerX * frameSize.width - width / 2f).roundToInt(),
                            (position.bottomY * frameSize.height - height).roundToInt())
                    }.requiredSize(with(density) { width.toDp() }, with(density) { height.toDp() })
                        .then(if (editable) Modifier.pointerInput(cue.id, frameSize) {
                            detectDragGestures(onDragStart = { dragging = true },
                                onDragEnd = { dragging = false; latestCommit(draftX, draftY) },
                                onDragCancel = { dragging = false; draftX = latestCue.x; draftY = latestCue.y },
                                onDrag = { change, delta ->
                                    change.consume()
                                    val next = SubtitlePlacement.drag(draftX, draftY, delta.x, delta.y,
                                        frameSize.width, frameSize.height)
                                    draftX = next.first
                                    draftY = next.second
                                })
                        } else Modifier))
            }
        }
    }
}
