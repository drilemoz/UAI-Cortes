package br.com.uaicortes

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.OverlaySettings
import androidx.media3.common.VideoCompositorSettings
import androidx.media3.common.util.Size
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Presentation
import androidx.media3.effect.StaticOverlaySettings
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import java.io.File
import kotlin.math.roundToInt
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

internal object JunctionTransitionFrames {
    /** Invoked on IO. The caller owns and removes the private directory on all exits. */
    suspend fun prepare(context: Context, joins: List<JunctionTransitionPlan.Join>, directory: File,
                        shortSide: Int): Map<JunctionTransitionPlan.Side, File> {
        val holds = joins.flatMap { listOf(it.incoming, it.outgoing) }.filter { it.heldFrame }
        val unique = mutableMapOf<Pair<String, Long>, File>()
        return buildMap {
            for (side in holds) {
                currentCoroutineContext().ensureActive()
                put(side, unique.getOrPut(side.clip.uri to side.sourceStartMs) {
                    val file = File(directory, "edge-${unique.size}.jpg")
                    save(context, side, shortSide, file)
                    file
                })
            }
        }
    }

    private fun save(context: Context, side: JunctionTransitionPlan.Side, shortSide: Int, file: File) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, Uri.parse(side.clip.uri))
            val aspect = mediaAspect(context, side.clip.uri, false)
            val (width, height) = transitionOutputSize(aspect, shortSide)
            val atUs = side.sourceStartMs.coerceAtLeast(0L) * 1000L
            val frame = if (Build.VERSION.SDK_INT >= 27)
                retriever.getScaledFrameAtTime(atUs, MediaMetadataRetriever.OPTION_CLOSEST, width, height)
            else retriever.getFrameAtTime(atUs, MediaMetadataRetriever.OPTION_CLOSEST)
            requireNotNull(frame) { "Não foi possível obter o quadro de apoio de ${side.clip.title}." }
            try {
                require(file.outputStream().buffered().use { frame.compress(Bitmap.CompressFormat.JPEG, 94, it) }) {
                    "Não foi possível gravar o quadro temporário da transição."
                }
            } finally { frame.recycle() }
        } finally { retriever.release() }
    }
}

internal fun transitionOutputSize(aspect: Float, shortSide: Int): Pair<Int, Int> {
    val r = aspect.takeIf { it.isFinite() && it > 0f }?.coerceIn(.1f, 10f) ?: (16f / 9f)
    fun even(value: Float) = ((value / 2f).roundToInt() * 2).coerceAtLeast(2)
    return if (r >= 1f) even(shortSide * r) to shortSide else shortSide to even(shortSide / r)
}

internal fun JunctionTransitionPlan.Side.renderClip(held: Map<JunctionTransitionPlan.Side, File>): Clip =
    if (heldFrame) {
        val uri = Uri.fromFile(held.getValue(this)).toString()
        clip.copy(uri = uri, isImage = true, imageMimeType = "image/jpeg", imageFrameMode = ImageFraming.FIT,
            imageZoom = 1f, imagePanX = 0f, imagePanY = 0f,
            startMs = 0L, endMs = lengthMs, sourceDurationMs = lengthMs)
    } else if (clip.isImage) clip.copy(startMs = 0L, endMs = lengthMs, sourceDurationMs = lengthMs)
    else clip.copy(startMs = sourceStartMs, endMs = sourceStartMs + lengthMs)

/** A single secondary video sequence for any number of joins, with no audio. */
@OptIn(UnstableApi::class)
internal fun junctionVideoTrack(joins: List<JunctionTransitionPlan.Join>, total: Long,
    held: Map<JunctionTransitionPlan.Side, File>, layout: (Clip) -> List<androidx.media3.common.Effect>): EditedMediaItemSequence {
    val sequence = EditedMediaItemSequence.Builder(setOf(C.TRACK_TYPE_VIDEO))
    var at = 0L
    for (join in joins) for (side in listOf(join.incoming, join.outgoing)) {
        if (side.timelineStartMs > at) sequence.addGap((side.timelineStartMs - at) * 1000L)
        val clip = side.renderClip(held)
        sequence.addItem(EditedMediaItem.Builder(ClipMediaItems.playback(clip)).setRemoveAudio(true)
            .apply { if (clip.isImage) setFrameRate(30) }
            .setEffects(Effects(emptyList(), layout(clip))).build())
        at = side.timelineStartMs + side.lengthMs
    }
    if (total > at) sequence.addGap((total - at) * 1000L)
    return sequence.build()
}

/** PiP stays above a shared canvas. Main stays above support so it can fade onto it.
 * Only those two images participate, while captions are drawn after composition. */
@OptIn(UnstableApi::class)
internal class JunctionVideoCompositor(private val pips: List<Overlay>,
    private val joins: List<JunctionTransitionPlan.Join>) : VideoCompositorSettings {
    private val pipLayout = PipCompositor(pips)
    override fun getOutputSize(inputSizes: MutableList<Size>): Size = pipLayout.getOutputSize(inputSizes)

    override fun getOverlaySettings(inputId: Int, presentationTimeUs: Long): OverlaySettings {
        if (inputId < pips.size) return pipLayout.getOverlaySettings(inputId, presentationTimeUs)
        val placement = JunctionTransitionPlan.placement(joins, presentationTimeUs)
        val main = inputId == pips.size
        // Moving an entire canvas needs ±2 NDC. Both individual anchors must
        // stay within Media3's ±1 range, so split the translation between them.
        val x = (if (main) placement.mainX else placement.supportX) / 2f
        return StaticOverlaySettings.Builder()
            .setOverlayFrameAnchor(-x, 0f)
            .setBackgroundFrameAnchor(x, 0f)
            .setAlphaScale(if (main) placement.mainAlpha else if (placement.supportVisible) 1f else 0f)
            .build()
    }
}

/** The portrait/landscape normalization is identical for both source images. */
@OptIn(UnstableApi::class)
internal fun junctionPresentation(size: Pair<Int, Int>, crop: Boolean): Presentation =
    Presentation.createForWidthAndHeight(size.first, size.second,
        if (crop) Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP else Presentation.LAYOUT_SCALE_TO_FIT)
