package br.com.uaicortes

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExportFrameSamplingAndroidTest {
    @Test fun decodesRealH264AtPresentationTimesInsideRequest() = runBlocking {
        val tests = InstrumentationRegistry.getInstrumentation()
        val context = tests.targetContext
        val fixture = File.createTempFile("uai-frame-sampling", ".mp4", context.cacheDir)
        try {
            tests.context.assets.open("export_smoke_16x9.mp4").use { input ->
                fixture.outputStream().use { input.copyTo(it) }
            }
            val sample = ExportFrameSampling.read(context, Uri.fromFile(fixture), 180_000, 950_000)
            assertTrue(sample.frames.size >= 8)
            assertTrue(sample.frames.all { it.timeUs in 180_000L until 950_000L })
            assertTrue(sample.frames.zipWithNext().all { (a, b) -> a.timeUs < b.timeUs })
            assertTrue(sample.frames.all { it.luma.size == ExportFrameOrder.PIXELS })
            assertTrue(sample.frames.first().luma.distinct().size >= 2)
        } finally { fixture.delete() }
    }
}
