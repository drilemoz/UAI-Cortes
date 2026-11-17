package br.com.uaicortes

import android.graphics.Bitmap
import android.graphics.Color
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

/** A marca deve surgir só no clipe do meio, nos dois caminhos de composição do exportador. */
@LargeTest
@RunWith(AndroidJUnit4::class)
class WatermarkTimingAndroidTest {
    @Test fun scopedWatermarkUsesClipLocalEffectsWithoutPip() = verify(withPip = false)
    @Test fun scopedWatermarkUsesProjectTimeAbovePip() = verify(withPip = true)

    private fun verify(withPip: Boolean) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val image = File.createTempFile("uai-watermark-base-", ".png", context.cacheDir)
        val saved = AtomicReference<Uri?>(null)
        val error = AtomicReference<String?>(null)
        val done = CountDownLatch(1)
        var engine: ExportEngine? = null
        try {
            val bitmap = Bitmap.createBitmap(360, 640, Bitmap.Config.ARGB_8888)
            try {
                bitmap.eraseColor(Color.BLACK)
                image.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            } finally {
                bitmap.recycle()
            }

            val uri = Uri.fromFile(image).toString()
            val clips = List(3) { index ->
                Clip(uri = uri, title = "Fundo ${index + 1}", sourceDurationMs = 900L,
                    isImage = true, imageMimeType = "image/png")
            }
            val overlays = if (withPip)
                listOf(Overlay(uri, true, 2700L, scale = .18f, x = .16f, y = .16f)) else emptyList()
            val project = VideoProject(title = "Duração da marca", clips = clips, overlays = overlays,
                textWatermark = TextWatermark(text = "MARCA", sizeSp = 36,
                    opacity = 1f, x = .5f, y = .5f, clipId = clips[1].id))

            instrumentation.runOnMainSync {
                engine = ExportEngine(context)
                try {
                    engine!!.start(project, 720, "Uai_Marca_Duracao_$withPip.mp4",
                        destination = ExportDestination.SHORTS,
                        onSuccess = { uri, _ -> saved.set(uri); done.countDown() },
                        onError = { message -> error.set(message); done.countDown() })
                } catch (failure: Exception) {
                    error.set(failure.message ?: failure.javaClass.simpleName)
                    done.countDown()
                }
            }
            assertTrue("Tempo excedido no teste de marca d'água", done.await(180, TimeUnit.SECONDS))
            assertNull("Falha ao exportar marca d'água", error.get())
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, requireNotNull(saved.get()))
                assertEquals("Primeiro clipe sem marca", 0, brightSamples(retriever, 400_000L))
                assertTrue("Segundo clipe contém a marca", brightSamples(retriever, 1_350_000L) > 20)
                assertEquals("Terceiro clipe sem marca", 0, brightSamples(retriever, 2_250_000L))
            } finally {
                retriever.release()
            }
        } finally {
            saved.get()?.let { runCatching { context.contentResolver.delete(it, null, null) } }
            instrumentation.runOnMainSync { engine?.cancel() }
            image.delete()
        }
    }

    private fun brightSamples(retriever: MediaMetadataRetriever, timeUs: Long): Int {
        val frame = requireNotNull(retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST))
        try {
            val rgb = requireNotNull(frame.copy(Bitmap.Config.ARGB_8888, false))
            try {
                var count = 0
                for (y in rgb.height / 3 until rgb.height * 2 / 3 step 3) {
                    for (x in rgb.width / 5 until rgb.width * 4 / 5 step 3) {
                        val p = rgb.getPixel(x, y)
                        if (minOf(Color.red(p), Color.green(p), Color.blue(p)) > 155) count++
                    }
                }
                return count
            } finally {
                rgb.recycle()
            }
        } finally {
            frame.recycle()
        }
    }
}
