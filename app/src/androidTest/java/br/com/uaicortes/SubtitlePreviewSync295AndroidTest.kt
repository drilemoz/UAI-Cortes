package br.com.uaicortes

import android.graphics.Bitmap
import android.graphics.Color
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Checks a muxed private preview and a published export, including decoded sound. */
@RunWith(AndroidJUnit4::class)
class SubtitlePreviewSync295AndroidTest {
    @Test fun sameTimedWordAndVoiceOnBothSidesOfTheCut() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val photo = File.createTempFile("uai-sync-", ".png", context.cacheDir)
        val wave = File.createTempFile("uai-sync-", ".wav", context.cacheDir)
        val ready = mutableListOf<File>()
        val saved = mutableListOf<Uri>()
        try {
            val bitmap = Bitmap.createBitmap(360, 640, Bitmap.Config.ARGB_8888)
            try {
                bitmap.eraseColor(Color.BLACK)
                photo.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            } finally { bitmap.recycle() }
            writeTone(wave)
            val image = Uri.fromFile(photo).toString()
            val sound = Uri.fromFile(wave).toString()
            val project = VideoProject(clips = listOf(
                Clip(uri = image, title = "A", sourceDurationMs = 1500L, isImage = true, imageMimeType = "image/png"),
                Clip(uri = image, title = "B", sourceDurationMs = 1500L, isImage = true, imageMimeType = "image/png")),
                audioTracks = listOf(AudioTrack(sound, "Voz 1", 1180L, durationMs = 250L),
                    AudioTrack(sound, "Voz 2", 1600L, durationMs = 250L)),
                subtitles = listOf(SubtitleCue(text = "I WWWWW", startMs = 1180L, endMs = 1850L,
                    color = Color.GREEN, sizeSp = 26, backgroundColor = Color.TRANSPARENT,
                    animation = SubtitleAnimations.SINGLE_WORD,
                    wordTimings = listOf(SubtitleWordTiming(1180L, 1430L), SubtitleWordTiming(1600L, 1850L)))))
            val job = requireNotNull(JunctionPreviewProject.prepare(project, 0, TransitionEffects.CUT, 300L))
            fun render(source: VideoProject, preview: Boolean): Uri {
                var engine: ExportEngine? = null
                var output: Uri? = null
                var error: String? = null
                val done = CountDownLatch(1)
                val collectPreview: (File) -> Unit = { file ->
                    ready.add(file)
                    output = Uri.fromFile(file)
                    done.countDown()
                }
                val onPreviewReady = collectPreview.takeIf { preview }
                try {
                    instrumentation.runOnMainSync {
                        engine = ExportEngine(context)
                        runCatching {
                            engine!!.start(source, 360, "Uai_Teste_Sincronia_295.mp4",
                                destination = ExportDestination.VERTICAL,
                                onSuccess = { uri, _ -> saved.add(uri); output = uri; done.countDown() },
                                onError = { error = it; done.countDown() }, onPreviewReady = onPreviewReady)
                        }.onFailure { error = it.message ?: it.javaClass.simpleName; done.countDown() }
                    }
                    assertTrue("Tempo excedido na renderização", done.await(120L, TimeUnit.SECONDS))
                    assertNull(error)
                    return requireNotNull(output)
                } finally { instrumentation.runOnMainSync { engine?.cancel() } }
            }
            val preview = render(job.project, true)
            val exported = render(project, false)
            for (uri in listOf(preview, exported)) {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, uri)
                    fun glyphPixels(ms: Long): Int {
                        val frame = requireNotNull(retriever.getFrameAtTime(ms * 1000L,
                            MediaMetadataRetriever.OPTION_CLOSEST))
                        try {
                            val pixels = IntArray(frame.width * frame.height)
                            frame.getPixels(pixels, 0, frame.width, 0, 0, frame.width, frame.height)
                            return pixels.count { Color.green(it) > 120 && Color.green(it) > Color.red(it) + 55 }
                        } finally { frame.recycle() }
                    }
                    val firstWord = glyphPixels(1300L)
                    assertTrue(firstWord > 10)
                    assertEquals(0, glyphPixels(1500L))
                    assertTrue(glyphPixels(1730L) > firstWord * 2)
                } finally { retriever.release() }
                val audio = runBlocking { AudioClipWaveform.inspect(context, uri) }
                fun peak(start: Long, end: Long): Float = audio.peaks.filterIndexed { i, _ ->
                    (i + .5) / audio.peaks.size * audio.durationMs in start.toDouble()..end.toDouble()
                }.maxOrNull() ?: 0f
                assertTrue(peak(1250L, 1360L) > .10f)
                assertTrue(peak(1470L, 1550L) < .06f)
                assertTrue(peak(1680L, 1800L) > .10f)
            }
        } finally {
            saved.forEach { context.contentResolver.delete(it, null, null) }
            ready.forEach { it.delete() }
            photo.delete()
            wave.delete()
        }
    }

    private fun writeTone(file: File) {
        val rate = 48_000
        val samples = rate / 4
        val bytes = samples * 2
        val wav = ByteBuffer.allocate(44 + bytes).order(ByteOrder.LITTLE_ENDIAN)
        wav.put("RIFF".toByteArray()).putInt(36 + bytes).put("WAVE".toByteArray())
        wav.put("fmt ".toByteArray()).putInt(16).putShort(1.toShort()).putShort(1.toShort())
        wav.putInt(rate).putInt(rate * 2).putShort(2.toShort()).putShort(16.toShort())
        wav.put("data".toByteArray()).putInt(bytes)
        repeat(samples) { i ->
            wav.putShort((kotlin.math.sin(2 * Math.PI * 440 * i / rate) * 14_000).toInt().toShort())
        }
        file.writeBytes(wav.array())
    }
}
