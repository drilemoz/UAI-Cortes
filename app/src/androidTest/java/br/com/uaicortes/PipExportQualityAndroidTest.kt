package br.com.uaicortes

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/** Audita pixels reais após composição, codificação AVC e cópia para o MediaStore. */
@LargeTest
@RunWith(AndroidJUnit4::class)
class PipExportQualityAndroidTest {
    @Test fun vertical720RetainsPipMarginsAndFineDetails() = verify(720, 1280)
    @Test fun vertical1080RetainsPipMarginsAndFineDetails() = verify(1080, 1920)

    private fun verify(width: Int, height: Int) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val base = File.createTempFile("uai-base-", ".png", context.cacheDir)
        val logo = File.createTempFile("uai-logo-", ".png", context.cacheDir)
        val video = File.createTempFile("uai-detail-", ".mp4", context.cacheDir)
        val result = AtomicReference<Uri?>(null)
        val error = AtomicReference<String?>(null)
        val done = CountDownLatch(1)
        var engine: ExportEngine? = null
        try {
            savePng(base, 360, 640) { drawColor(Color.BLACK) }
            savePng(logo, 320, 160) {
                val paint = Paint().apply { color = Color.RED }
                // The top rows are transparent, so the true background must show through.
                drawRect(0f, 20f, 320f, 160f, paint)
                paint.color = Color.WHITE
                for (x in 40..280 step 40) drawRect(x.toFloat(), 40f, (x + 8).toFloat(), 130f, paint)
            }
            instrumentation.context.assets.open("export_pip_detail_280.mp4").use { input ->
                video.outputStream().use { input.copyTo(it) }
            }
            val aspect = 9f / 16f
            val scale = .24f
            val (w, h) = OverlayEditorLogic.dimensions(scale, 2f, aspect)
            val (left, top) = OverlayEditorLogic.quickPreset(0, 0, w, h, aspect)
            val (right, bottom) = OverlayEditorLogic.quickPreset(2, 2, w, h, aspect)
            val project = VideoProject(title = "Qualidade PiP", clips = listOf(Clip(
                uri = Uri.fromFile(base).toString(), title = "Base",
                sourceDurationMs = 1400L, startMs = 0L, endMs = 1400L,
                isImage = true, imageMimeType = "image/png")), overlays = listOf(
                Overlay(Uri.fromFile(logo).toString(), true, 1400L, scale = scale, x = left, y = top),
                Overlay(Uri.fromFile(video).toString(), false, 1400L, scale = scale, x = right, y = bottom, muted = true)
            ))
            instrumentation.runOnMainSync {
                engine = ExportEngine(context)
                try {
                    engine!!.start(project, width, "Uai_PiP_Pixels_$width.mp4",
                        destination = ExportDestination.SHORTS,
                        onSuccess = { uri, _ -> result.set(uri); done.countDown() },
                        onError = { message -> error.set(message); done.countDown() })
                } catch (failure: Exception) {
                    error.set(failure.message ?: failure.javaClass.simpleName)
                    done.countDown()
                }
            }
            assertTrue("Tempo excedido ao exportar o PiP", done.await(180, TimeUnit.SECONDS))
            assertNull("Falha ao exportar PiP", error.get())
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, requireNotNull(result.get()))
                val frame = requireNotNull(retriever.getFrameAtTime(600_000L,
                    MediaMetadataRetriever.OPTION_CLOSEST)) { "MP4 sem quadro decodificável" }
                try {
                    val rgb = requireNotNull(frame.copy(Bitmap.Config.ARGB_8888, false))
                    try {
                        verifyPixels(rgb, width, height)
                    } finally {
                        rgb.recycle()
                    }
                } finally {
                    frame.recycle()
                }
            } finally {
                retriever.release()
            }
        } finally {
            result.get()?.let { runCatching { context.contentResolver.delete(it, null, null) } }
            instrumentation.runOnMainSync { engine?.cancel() }
            listOf(base, logo, video).forEach { it.delete() }
        }
    }

    private fun verifyPixels(frame: Bitmap, width: Int, height: Int) {
        assertEquals(width, frame.width)
        assertEquals(height, frame.height)
        val margin = width * .04f
        val pipWidth = width * .24f
        val pipHeight = pipWidth / 2f
        val logoY = margin + pipHeight / 2f
        val videoX = width - margin - pipWidth
        val videoY = height - margin - pipHeight / 2f
        fun sample(x: Float, y: Float) = frame.getPixel(x.toInt(), y.toInt())
        fun dark(p: Int) = Color.red(p) < 45 && Color.green(p) < 45 && Color.blue(p) < 45
        fun white(p: Int) = minOf(Color.red(p), Color.green(p), Color.blue(p)) > 155
        fun red(p: Int) = Color.red(p) > 130 && Color.green(p) < 85
        fun green(p: Int) = Color.green(p) > 100 && Color.red(p) < 85
        assertTrue("Margem esquerda do logo", dark(sample(margin / 2f, logoY)))
        assertTrue("Logo no canto superior esquerdo", red(sample(margin + pipWidth * .06f, logoY)))
        assertTrue("PNG transparente", dark(sample(margin + pipWidth / 2f, margin + pipHeight * .06f)))
        assertTrue("Margem direita do vídeo", dark(sample(width - margin / 2f, videoY)))
        assertTrue("PiP no canto inferior direito", green(sample(videoX + pipWidth * .06f, videoY)))
        for (x in listOf(44, 124, 204, 284)) {
            val inside = pipWidth * x / 320f
            val between = pipWidth * (x + 14f) / 320f
            assertTrue("Traço claro preservado no PNG ($x)", white(sample(margin + inside, logoY)))
            assertTrue("Separação dos traços no PNG ($x)", red(sample(margin + between, logoY)))
            assertTrue("Traço claro preservado no vídeo ($x)", white(sample(videoX + inside, videoY)))
            assertTrue("Separação dos traços no vídeo ($x)", green(sample(videoX + between, videoY)))
        }
    }

    private fun savePng(file: File, width: Int, height: Int, draw: Canvas.() -> Unit) {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        try {
            Canvas(bitmap).draw()
            file.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        } finally {
            bitmap.recycle()
        }
    }
}
