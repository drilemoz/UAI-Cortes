package br.com.uaicortes

import android.graphics.Bitmap
import android.graphics.Color
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class ImagePipFlashAndroidTest {
    @Test fun delayedLogoNeverTakesOverTheFrameAndKeepsItsInterval() {
        val tests = InstrumentationRegistry.getInstrumentation()
        val context = tests.targetContext
        val source = File.createTempFile("pip-blue-", ".mp4", context.cacheDir)
        val logo = File.createTempFile("pip-logo-", ".png", context.cacheDir)
        val output = AtomicReference<File?>()
        val failure = AtomicReference<String?>()
        val complete = CountDownLatch(1)
        var engine: ExportEngine? = null
        try {
            tests.context.assets.open("junction_293_blue.mp4").use { input ->
                source.outputStream().use { input.copyTo(it) }
            }
            val artwork = Bitmap.createBitmap(96, 64, Bitmap.Config.ARGB_8888)
            try {
                artwork.eraseColor(Color.MAGENTA)
                logo.outputStream().use { assertTrue(artwork.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            } finally { artwork.recycle() }

            val project = VideoProject(clips = listOf(
                Clip(uri = Uri.fromFile(source).toString(), title = "Fundo",
                    sourceDurationMs = 1_600, endMs = 1_600)),
                overlays = listOf(Overlay(uri = Uri.fromFile(logo).toString(), isImage = true,
                    startAtMs = 500, durationMs = 400, scale = .20f, x = .83f, y = .15f)))
            tests.runOnMainSync {
                try {
                    val exporter = ExportEngine(context)
                    engine = exporter
                    exporter.start(project, 360, "prova-pip.mp4", preset = ExportBitratePreset.COMPACT,
                        destination = ExportDestination.VERTICAL,
                        onSuccess = { _, _ -> error("O MP4 de teste não deve ir à galeria") },
                        onError = { reason -> failure.set(reason); complete.countDown() },
                        onPreviewReady = { file -> output.set(file); complete.countDown() })
                } catch (error: Exception) {
                    failure.set(error.message)
                    complete.countDown()
                }
            }
            assertTrue("Renderização do PiP não terminou", complete.await(100, TimeUnit.SECONDS))
            assertNull(failure.get())
            val file = requireNotNull(output.get())
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(file.absolutePath)
                fun blue(pixel: Int): Boolean = Color.blue(pixel) > 140 && Color.red(pixel) < 90
                fun magenta(pixel: Int): Boolean = Color.red(pixel) > 135 && Color.blue(pixel) > 120 &&
                    Color.green(pixel) < 110
                val times = listOf(100_000L, 400_000L, 450_000L, 483_000L, 500_000L, 517_000L,
                    533_000L, 550_000L, 567_000L, 583_000L, 700_000L, 850_000L,
                    883_000L, 900_000L, 917_000L, 950_000L, 1_100_000L)
                for (time in times) {
                    val frame = requireNotNull(retriever.getFrameAtTime(time, MediaMetadataRetriever.OPTION_CLOSEST))
                    try {
                        fun sample(x: Float, y: Float) =
                            frame.getPixel((x * frame.width).toInt(), (y * frame.height).toInt())
                        assertTrue("Plano de fundo perdido em $time µs", blue(sample(.50f, .50f)))
                        assertTrue("PiP fora da geometria em $time µs", blue(sample(.25f, .15f)))
                        if (time in 550_000L..850_000L)
                            assertTrue("Logo ausente em $time µs", magenta(sample(.83f, .15f)))
                        if (time < 450_000L || time > 950_000L)
                            assertTrue("Logo fora do intervalo em $time µs", blue(sample(.83f, .15f)))
                    } finally { frame.recycle() }
                }
            } finally { retriever.release() }
        } finally {
            tests.runOnMainSync { engine?.cancel() }
            output.get()?.delete()
            logo.delete()
            source.delete()
        }
    }
}
