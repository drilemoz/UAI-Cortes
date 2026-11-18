package br.com.uaicortes

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.annotation.OptIn
import androidx.media3.common.OverlaySettings
import androidx.media3.common.util.Size
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.StaticOverlaySettings
import java.io.File

/** The top contiguous photos can be rendered after the video compositor without changing Z order. */
internal object ImagePipExportPlan {
    fun timedCount(topFirst: List<Overlay>): Int = topFirst.takeWhile { it.isImage }.size

    fun contains(pip: Overlay, timeUs: Long): Boolean {
        val startUs = pip.startAtMs * 1_000L
        return timeUs >= startUs && timeUs - startUs < pip.durationMs * 1_000L
    }
}

/**
 * A photo cannot become a primary stream: it is painted onto an already-composed video frame.
 * Outside the interval we provide both zero alpha and a transparent texture.
 */
@OptIn(UnstableApi::class)
internal class TimedImagePipOverlay(private val pip: Overlay, private val bitmap: Bitmap) : BitmapOverlay() {
    private val transparent = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
    private val hidden = StaticOverlaySettings.Builder().setAlphaScale(0f).build()
    private var placement: OverlaySettings = hidden

    override fun configure(videoSize: Size) {
        super.configure(videoSize)
        val width = videoSize.width.coerceAtLeast(1).toFloat()
        val height = videoSize.height.coerceAtLeast(1).toFloat()
        val (wf, hf) = OverlayEditorLogic.dimensions(pip.scale,
            bitmap.width.toFloat() / bitmap.height, width / height, pip.fitMode)
        val x = OverlayEditorLogic.clampCenter(pip.x, wf)
        val y = OverlayEditorLogic.clampCenter(pip.y, hf)
        // BitmapOverlay's unscaled size is expressed in output pixels, not full-frame fractions.
        placement = StaticOverlaySettings.Builder()
            .setScale(wf * width / bitmap.width, hf * height / bitmap.height)
            .setOverlayFrameAnchor(0f, 0f)
            .setBackgroundFrameAnchor(x * 2f - 1f, 1f - y * 2f)
            .setAlphaScale(pip.opacity.coerceIn(0f, 1f))
            .build()
    }

    override fun getBitmap(presentationTimeUs: Long): Bitmap =
        if (ImagePipExportPlan.contains(pip, presentationTimeUs)) bitmap else transparent

    override fun getOverlaySettings(presentationTimeUs: Long): OverlaySettings =
        if (ImagePipExportPlan.contains(pip, presentationTimeUs)) placement else hidden

    companion object {
        /** Caller decodes once on IO. Keep an immutable, CPU-backed bitmap with its original alpha. */
        fun read(context: Context, uri: Uri, maxEdge: Int): Bitmap {
            fun sampleFor(width: Int, height: Int): Int {
                require(width > 0 && height > 0)
                var sample = 1
                while (maxOf(width, height) / sample > maxEdge) sample *= 2
                return sample
            }
            if (Build.VERSION.SDK_INT >= 28) {
                val source = if (uri.scheme == "file")
                    ImageDecoder.createSource(File(requireNotNull(uri.path)))
                else ImageDecoder.createSource(context.contentResolver, uri)
                return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    decoder.setTargetSampleSize(sampleFor(info.size.width, info.size.height))
                }
            }
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            }
            options.inSampleSize = sampleFor(options.outWidth, options.outHeight)
            options.inPreferredConfig = Bitmap.Config.ARGB_8888
            options.inJustDecodeBounds = false
            return requireNotNull(context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            })
        }
    }
}
