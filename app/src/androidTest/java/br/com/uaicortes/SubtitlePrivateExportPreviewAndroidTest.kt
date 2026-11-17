package br.com.uaicortes

import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@LargeTest
@RunWith(AndroidJUnit4::class)
class SubtitlePrivateExportPreviewAndroidTest {
    @Test fun previewUsesPrivateMp4AndDoesNotInvokeGalleryCallback() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val source = File.createTempFile("uai-preview-source-", ".png", context.cacheDir)
        val output = AtomicReference<File?>()
        val error = AtomicReference<String?>()
        val finished = CountDownLatch(1)
        var engine: ExportEngine? = null
        try {
            val bitmap = Bitmap.createBitmap(360, 640, Bitmap.Config.ARGB_8888)
            try {
                bitmap.eraseColor(Color.BLACK)
                source.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            } finally { bitmap.recycle() }
            val project = VideoProject(clips = listOf(Clip(uri = Uri.fromFile(source).toString(),
                title = "Base", sourceDurationMs = 1_200L, isImage = true, imageMimeType = "image/png")),
                subtitles = listOf(SubtitleCue(text = "Prévia no cache", startMs = 120L, endMs = 1_100L)))
            instrumentation.runOnMainSync {
                engine = ExportEngine(context)
                try {
                    engine!!.start(project, 720, "Nao_Publicar_Previa.mp4",
                        onPreviewReady = { output.set(it); finished.countDown() },
                        onSuccess = { uri, _ ->
                            runCatching { context.contentResolver.delete(uri, null, null) }
                            error.set("A prévia publicou na galeria.")
                            finished.countDown()
                        },
                        onError = { error.set(it); finished.countDown() })
                } catch (failure: Exception) {
                    error.set(failure.message ?: failure.javaClass.simpleName)
                    finished.countDown()
                }
            }
            assertTrue("Tempo excedido ao gerar a prévia", finished.await(180, TimeUnit.SECONDS))
            assertNull(error.get())
            val video = requireNotNull(output.get())
            assertTrue(video.parentFile?.canonicalFile == context.cacheDir.canonicalFile)
            ExportMp4Inspector.requirePlayable(ExportMp4Inspector.inspectFile(context, video), false)
        } finally {
            instrumentation.runOnMainSync { engine?.cancel() }
            output.get()?.delete()
            source.delete()
        }
    }
}
