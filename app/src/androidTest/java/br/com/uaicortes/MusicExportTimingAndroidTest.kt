package br.com.uaicortes

import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.PI
import kotlin.math.sin

/** Mede áudio decodificado do MP4, não apenas a presença da faixa no contêiner. */
@LargeTest
@RunWith(AndroidJUnit4::class)
class MusicExportTimingAndroidTest {
    @Test fun musicStartsAndEndsAtItsOwnPositionWithoutReplacingAdditionalAudio() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val photo = File.createTempFile("uai-music-photo-", ".png", context.cacheDir)
        val wave = File.createTempFile("uai-music-tone-", ".wav", context.cacheDir)
        val saved = AtomicReference<Uri?>(null)
        val error = AtomicReference<String?>(null)
        val done = CountDownLatch(1)
        var engine: ExportEngine? = null
        try {
            val bitmap = Bitmap.createBitmap(320, 320, Bitmap.Config.ARGB_8888)
            try {
                bitmap.eraseColor(Color.BLACK)
                photo.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            } finally { bitmap.recycle() }
            writeTone(wave)
            val sound = Uri.fromFile(wave).toString()
            val project = VideoProject(title = "Música separada",
                clips = listOf(Clip(uri = Uri.fromFile(photo).toString(), title = "Foto",
                    sourceDurationMs = 3_000L, isImage = true, imageMimeType = "image/png")),
                audioTracks = listOf(AudioTrack(sound, "Narração", startMs = 200L,
                    durationMs = 400L, volume = .6f)),
                music = Music(sound, "Tema", .75f, startMs = 1_200L, sourceDurationMs = 750L, repeat = false))
            instrumentation.runOnMainSync {
                engine = ExportEngine(context)
                try {
                    engine!!.start(project, 720, "Uai_Musica_Tempo.mp4",
                        onSuccess = { uri, _ -> saved.set(uri); done.countDown() },
                        onError = { error.set(it); done.countDown() })
                } catch (failure: Exception) {
                    error.set(failure.message ?: failure.javaClass.simpleName)
                    done.countDown()
                }
            }
            assertTrue("Tempo excedido ao exportar a música", done.await(180, TimeUnit.SECONDS))
            assertNull("Exportação de música falhou", error.get())
            val inspection = runBlocking { AudioClipWaveform.inspect(context, requireNotNull(saved.get())) }
            fun peak(from: Long, to: Long): Float {
                val peaks = inspection.peaks.filterIndexed { i, _ ->
                    (i + .5) / inspection.peaks.size * inspection.durationMs in from.toDouble()..to.toDouble()
                }
                return requireNotNull(peaks.maxOrNull())
            }
            assertTrue("Áudio adicional preservado", peak(320L, 520L) > .10f)
            assertTrue("Silêncio antes da música", peak(810L, 1_050L) < .07f)
            assertTrue("Música no intervalo escolhido", peak(1_420L, 1_730L) > .13f)
            assertTrue("Música não repetiu após o fim", peak(2_220L, 2_580L) < .07f)
        } finally {
            saved.get()?.let { runCatching { context.contentResolver.delete(it, null, null) } }
            instrumentation.runOnMainSync { engine?.cancel() }
            photo.delete()
            wave.delete()
        }
    }

    private fun writeTone(file: File) {
        val rate = 48_000
        val samples = rate * 3 / 4
        val bytes = samples * 2
        val wav = ByteBuffer.allocate(44 + bytes).order(ByteOrder.LITTLE_ENDIAN)
        wav.put("RIFF".toByteArray()).putInt(36 + bytes).put("WAVE".toByteArray())
        wav.put("fmt ".toByteArray()).putInt(16).putShort(1.toShort()).putShort(1.toShort())
        wav.putInt(rate).putInt(rate * 2).putShort(2.toShort()).putShort(16.toShort())
        wav.put("data".toByteArray()).putInt(bytes)
        repeat(samples) { i -> wav.putShort((sin(2 * PI * 440 * i / rate) * 14_000).toInt().toShort()) }
        file.writeBytes(wav.array())
    }
}
