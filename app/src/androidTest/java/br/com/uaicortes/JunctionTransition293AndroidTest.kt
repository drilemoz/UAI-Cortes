package br.com.uaicortes

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
import kotlin.math.abs
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Validate actual decoded pixels, audio and project length instead of only settings. */
@LargeTest
@RunWith(AndroidJUnit4::class)
class JunctionTransition293AndroidTest {
    @Test fun dissolveCombinesTwoVideoImagesWithHeldEdges() =
        render(JunctionTransitionPlan.DISSOLVE, useMargins = false)

    @Test fun leftPushMovesTwoVideoImagesWithSourceMargins() =
        render(JunctionTransitionPlan.PUSH_LEFT, useMargins = true)

    @Test fun rightPushMovesTwoVideoImagesWithSourceMargins() =
        render(JunctionTransitionPlan.PUSH_RIGHT, useMargins = true)

    private fun render(kind: String, useMargins: Boolean) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val inputs = listOf("red", "blue").map { color ->
            File.createTempFile("uai-junction-$color-", ".mp4", context.cacheDir).also { file ->
                instrumentation.context.assets.open("junction_293_$color.mp4").use { input ->
                    file.outputStream().use { input.copyTo(it) }
                }
            }
        }
        val files = AtomicReference<File?>(null)
        val errors = AtomicReference<String?>(null)
        val done = CountDownLatch(1)
        var engine: ExportEngine? = null
        val edge = if (useMargins) 200L else 0L
        val clips = inputs.map { Clip(uri = Uri.fromFile(it).toString(), title = it.name,
            sourceDurationMs = 1600L, startMs = edge, endMs = 1600L - edge) }
        val p = TransitionBatch.apply(VideoProject(clips = clips), 0, kind, 400L, false)
        try {
            instrumentation.runOnMainSync {
                engine = ExportEngine(context)
                try {
                    engine!!.start(p, 360, "private-junction-test.mp4",
                        destination = ExportDestination.VERTICAL, onSuccess = { _, _ -> },
                        onError = { errors.set(it); done.countDown() },
                        onPreviewReady = { files.set(it); done.countDown() })
                } catch (error: Exception) {
                    errors.set(error.message ?: error.javaClass.simpleName)
                    done.countDown()
                }
            }
            assertTrue("Tempo excedido ao renderizar $kind", done.await(120L, TimeUnit.SECONDS))
            assertNull("Falha na composição de $kind", errors.get())
            val video = requireNotNull(files.get())
            val streams = ExportMp4Inspector.inspectFile(context, video)
            assertTrue(streams.video && streams.audio)
            assertTrue("Duração preservada: ${streams.durationMs}", abs(streams.durationMs - p.durationMs) < 150L)
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(video.absolutePath)
                fun pixels(ms: Long): Pair<Int, Int> {
                    val frame = requireNotNull(retriever.getFrameAtTime(ms * 1000L,
                        MediaMetadataRetriever.OPTION_CLOSEST))
                    return try {
                        assertEquals(360, frame.width)
                        assertEquals(640, frame.height)
                        frame.getPixel(frame.width / 4, frame.height / 2) to
                            frame.getPixel(frame.width * 3 / 4, frame.height / 2)
                    } finally { frame.recycle() }
                }
                fun red(color: Int) = Color.red(color) > Color.blue(color) + 65
                fun blue(color: Int) = Color.blue(color) > Color.red(color) + 65
                val cut = clips.first().durationMs
                assertTrue(red(pixels(cut - 400L).first))
                assertTrue(blue(pixels(cut + 400L).second))
                val (left, right) = pixels(cut)
                when (kind) {
                    JunctionTransitionPlan.DISSOLVE -> assertTrue(
                        "Esperava os canais das duas fontes no mesmo quadro",
                        Color.red(left) > 65 && Color.blue(left) > 65)
                    JunctionTransitionPlan.PUSH_LEFT -> assertTrue(red(left) && blue(right))
                    JunctionTransitionPlan.PUSH_RIGHT -> assertTrue(blue(left) && red(right))
                }
            } finally { retriever.release() }
        } finally {
            instrumentation.runOnMainSync { engine?.cancel() }
            files.get()?.delete()
            inputs.forEach { it.delete() }
        }
    }
}
