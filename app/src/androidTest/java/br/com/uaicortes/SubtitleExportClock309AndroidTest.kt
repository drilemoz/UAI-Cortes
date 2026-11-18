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
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Exercises the same GL/AV export used to save MP4, without writing to the gallery. */
@LargeTest
@RunWith(AndroidJUnit4::class)
class SubtitleExportClock309AndroidTest {
    @Test fun photoThenTrimmedVideoWithoutPipUsesTheProjectClock() = verify(withPip = false)
    @Test fun photoThenTrimmedVideoWithPipUsesTheProjectClock() = verify(withPip = true)

    private fun verify(withPip: Boolean) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val photo = File.createTempFile("uai-subtitle-clock-", ".png", context.cacheDir)
        val video = File.createTempFile("uai-subtitle-clock-", ".mp4", context.cacheDir)
        val output = AtomicReference<File?>(null)
        val failure = AtomicReference<String?>(null)
        val done = CountDownLatch(1)
        var engine: ExportEngine? = null
        try {
            val bitmap = Bitmap.createBitmap(180, 320, Bitmap.Config.ARGB_8888)
            try {
                bitmap.eraseColor(Color.BLACK)
                photo.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            } finally {
                bitmap.recycle()
            }
            instrumentation.context.assets.open("junction_293_red.mp4").use { input ->
                video.outputStream().use { input.copyTo(it) }
            }
            val photoUri = Uri.fromFile(photo).toString()
            val project = VideoProject(
                clips = listOf(
                    Clip(uri = photoUri, title = "Abertura", sourceDurationMs = 2_000L,
                        isImage = true, imageMimeType = "image/png"),
                    Clip(uri = Uri.fromFile(video).toString(), title = "Vídeo recortado",
                        sourceDurationMs = 1_600L, startMs = 200L, endMs = 1_500L)),
                overlays = if (withPip) listOf(Overlay(photoUri, true, 3_300L,
                    scale = .18f, x = .16f, y = .16f)) else emptyList(),
                subtitles = listOf(SubtitleCue(text = "I WWWWW", startMs = 2_250L,
                    endMs = 3_000L, sizeSp = 26, color = Color.GREEN,
                    backgroundColor = Color.TRANSPARENT, animation = SubtitleAnimations.SINGLE_WORD,
                    wordTimings = listOf(SubtitleWordTiming(2_250L, 2_450L),
                        SubtitleWordTiming(2_700L, 3_000L))))
            )

            instrumentation.runOnMainSync {
                runCatching {
                    engine = ExportEngine(context)
                    engine!!.start(project, 360, "Uai_Teste_Relogio_Legenda.mp4",
                        destination = ExportDestination.VERTICAL,
                        onSuccess = { _, _ -> failure.set("MP4 foi enviado à galeria"); done.countDown() },
                        onError = { failure.set(it); done.countDown() },
                        onPreviewReady = { output.set(it); done.countDown() })
                }.onFailure { failure.set(it.message ?: it.javaClass.simpleName); done.countDown() }
            }
            assertTrue("Tempo excedido no teste de exportação", done.await(120L, TimeUnit.SECONDS))
            assertNull("Falha do exportador", failure.get())
            val mp4 = requireNotNull(output.get())
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(mp4.absolutePath)
                assertEquals("Abertura sem texto da fala", 0, glyphPixels(retriever, 1_000L))
                assertEquals("Nenhuma legenda antes de 2,25 s", 0, glyphPixels(retriever, 2_100L))
                val first = glyphPixels(retriever, 2_330L)
                assertTrue("Primeira palavra visível no relógio do projeto", first > 10)
                assertEquals("Pausa entre palavras", 0, glyphPixels(retriever, 2_550L))
                assertTrue("Segunda palavra visível no horário correto",
                    glyphPixels(retriever, 2_800L) > first * 2)
                assertEquals("Legenda encerrada", 0, glyphPixels(retriever, 3_150L))
            } finally {
                retriever.release()
            }
            val audio = runBlocking { AudioClipWaveform.inspect(context, Uri.fromFile(mp4)) }
            fun peak(fromMs: Long, toMs: Long): Float = audio.peaks.filterIndexed { index, _ ->
                val midpointMs = (index + .5) / audio.peaks.size * audio.durationMs
                midpointMs in fromMs.toDouble()..toMs.toDouble()
            }.maxOrNull() ?: 0f
            assertTrue("A abertura continua silenciosa", peak(800L, 1_500L) < .04f)
            assertTrue("AAC do vídeo no trecho da legenda", peak(2_250L, 2_950L) > .06f)
        } finally {
            instrumentation.runOnMainSync { engine?.cancel() }
            output.get()?.delete()
            photo.delete()
            video.delete()
        }
    }

    private fun glyphPixels(retriever: MediaMetadataRetriever, timeMs: Long): Int {
        val frame = requireNotNull(retriever.getFrameAtTime(timeMs * 1_000L,
            MediaMetadataRetriever.OPTION_CLOSEST))
        return try {
            val pixels = IntArray(frame.width * frame.height)
            frame.getPixels(pixels, 0, frame.width, 0, 0, frame.width, frame.height)
            pixels.count { Color.green(it) > 120 && Color.green(it) > Color.red(it) + 55 }
        } finally {
            frame.recycle()
        }
    }
}
