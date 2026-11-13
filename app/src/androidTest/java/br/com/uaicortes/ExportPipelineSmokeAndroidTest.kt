package br.com.uaicortes

import android.media.MediaExtractor
import android.media.MediaFormat
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

/**
 * Teste de ponta a ponta com um arquivo MP4 H.264/AAC REAL e curto, sem rede.
 * Executar em aparelho/emulador com encoder AVC e GL funcionais. Não é teste JVM.
 * O vídeo é gerado fora do app e incluído exclusivamente no APK de androidTest.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class ExportPipelineSmokeAndroidTest {
    @Test fun pipAudioProbeDifferentiatesVideoOnlyFromVideoWithAudio() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val fixtures = listOf("export_smoke_16x9.mp4" to true,
            "export_smoke_video_only.mp4" to false)
        fixtures.forEach { (name, expected) ->
            val file = File.createTempFile("uai-pip-probe-", ".mp4", context.cacheDir)
            try {
                instrumentation.context.assets.open(name).use { source ->
                    file.outputStream().use { source.copyTo(it) }
                }
                assertEquals("Leitura de áudio para $name", expected,
                    ExportMp4Inspector.sourceHasAudio(context, Uri.fromFile(file)))
            } finally { file.delete() }
        }
    }

    /** Confere contêiner e presença das faixas, não prova áudio audível nem pixels de legenda. */
    @Test fun inspectorRecognizesRealVideoAndAudioFromFixture() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val file = File.createTempFile("uai-mp4-inspector-", ".mp4", context.cacheDir)
        try {
            instrumentation.context.assets.open("export_smoke_16x9.mp4").use { input ->
                file.outputStream().use { input.copyTo(it) }
            }
            val streams = ExportMp4Inspector.inspectFile(context, file)
            ExportMp4Inspector.requirePlayable(streams, expectedAudio = true)
            assertTrue(streams.video)
            assertTrue(streams.audio)
            assertTrue(streams.durationMs > 0L)
        } finally { file.delete() }
    }

    /** Exercita a composição multi-input e o efeito global de legenda/água.
     * O sucesso do MP4 NÃO prova a ordem visual dos pixels; isso exige inspeção
     * de quadros no aparelho, inclusive sobreposições de duas janelas PiP.
     */
    @Test fun exportPipWithSubtitleAboveCompositorProducesReadableMp4() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val input = File.createTempFile("uai-layer-smoke-", ".mp4", context.cacheDir)
        val saved = AtomicReference<Uri?>(null)
        val error = AtomicReference<String?>(null)
        val completed = CountDownLatch(1)
        var engine: ExportEngine? = null
        try {
            instrumentation.context.assets.open("export_smoke_16x9.mp4").use { original ->
                input.outputStream().use { original.copyTo(it) }
            }
            val source = Uri.fromFile(input).toString()
            val project = VideoProject(
                title = "Teste de camadas locais",
                clips = listOf(Clip(uri = source, title = "Base",
                    sourceDurationMs = 1600L, startMs = 0L, endMs = 1600L)),
                overlays = listOf(Overlay(uri = source, isImage = false,
                    durationMs = 1000L, startAtMs = 200L, muted = true,
                    scale = .5f, x = .5f, y = .5f)),
                subtitles = listOf(SubtitleCue(text = "TEXTO SOBRE PiP",
                    startMs = 100L, endMs = 1500L, x = .5f, y = .5f))
            )
            instrumentation.runOnMainSync {
                engine = ExportEngine(context)
                try {
                    engine!!.start(project, 720, "UaiCortes_Teste_Camadas.mp4",
                        preset = ExportBitratePreset.COMPACT,
                        onSuccess = { uri, _ -> saved.set(uri); completed.countDown() },
                        onError = { reason -> error.set(reason); completed.countDown() })
                } catch (failure: Exception) {
                    error.set(failure.message ?: failure.javaClass.simpleName)
                    completed.countDown()
                }
            }
            assertTrue("Composição PiP não terminou em 180 s: ${error.get()}",
                completed.await(180, TimeUnit.SECONDS))
            assertNull("Falha de composição PiP: ${error.get()}", error.get())
            val output = requireNotNull(saved.get()) { "MediaStore não confirmou MP4 com PiP" }
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, output)
                val duration = retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                assertTrue("Duração inválida do MP4 com PiP: $duration ms", duration in 900L..2400L)
            } finally { retriever.release() }
        } finally {
            saved.get()?.let { runCatching { context.contentResolver.delete(it, null, null) } }
            instrumentation.runOnMainSync { engine?.cancel() }
            input.delete()
        }
    }

    /** Vídeo 16:9 convertido explicitamente para Shorts em 9:16 com corte central.
     * Compilar o teste no CI não o executa: requer encoder H.264 e OpenGL reais. */
    @Test fun exportSocialPortraitWithCenterCropProducesPortraitMp4() {
        exportSocialPortrait(VerticalFrameMode.CENTER_CROP)
    }

    @Test fun exportSocialPortraitWithFitProducesPortraitMp4() {
        exportSocialPortrait(VerticalFrameMode.FIT)
    }

    private fun exportSocialPortrait(mode: VerticalFrameMode) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val input = File.createTempFile("uai-social-smoke-", ".mp4", context.cacheDir)
        val saved = AtomicReference<Uri?>(null)
        val error = AtomicReference<String?>(null)
        val completed = CountDownLatch(1)
        var engine: ExportEngine? = null
        try {
            instrumentation.context.assets.open("export_smoke_16x9.mp4").use { original ->
                input.outputStream().use { destination -> original.copyTo(destination) }
            }
            val project = VideoProject(
                title = "Teste vertical local",
                clips = listOf(Clip(uri = Uri.fromFile(input).toString(), title = "Fonte 16:9",
                    sourceDurationMs = 1600L, startMs = 0L, endMs = 1400L)),
                subtitles = listOf(SubtitleCue(text = "Teste social", startMs=0L, endMs=1200L))
            )
            instrumentation.runOnMainSync {
                engine = ExportEngine(context)
                try {
                    engine!!.start(project, 720, "UaiCortes_Test_Social_${mode.name}.mp4",
                        preset=ExportBitratePreset.COMPACT,
                        destination=ExportDestination.SHORTS, frameMode=mode,
                        onSuccess={ uri, report ->
                            if (report.fellBackInResolution)
                                error.set("Saída diferente dos 720×1280 solicitados: ${report.resolution}")
                            saved.set(uri)
                            completed.countDown()
                        },
                        onError={ reason -> error.set(reason); completed.countDown() })
                } catch (failure: Exception) {
                    error.set(failure.message ?: failure.javaClass.simpleName)
                    completed.countDown()
                }
            }
            assertTrue("Saída vertical não terminou em 180 s: ${error.get()}",
                completed.await(180, TimeUnit.SECONDS))
            assertNull("Saída vertical ${mode.name}: ${error.get()}", error.get())
            val output = requireNotNull(saved.get()) { "MediaStore não confirmou saída vertical" }
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, output)
                val width = retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
                val height = retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
                assertTrue("Resolução social inválida: ${width}x${height}",
                    width == 720 && height == 1280)
            } finally { retriever.release() }
        } finally {
            saved.get()?.let { runCatching { context.contentResolver.delete(it, null, null) } }
            instrumentation.runOnMainSync { engine?.cancel() }
            input.delete()
        }
    }

    @Test fun exportClippedVideoWithAudioAndSubtitleProducesReadableMp4() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val input = File.createTempFile("uai-export-smoke-", ".mp4", context.cacheDir)
        val saved = AtomicReference<Uri?>(null)
        val error = AtomicReference<String?>(null)
        val completed = CountDownLatch(1)
        var engine: ExportEngine? = null
        try {
            instrumentation.context.assets.open("export_smoke_16x9.mp4").use { original ->
                input.outputStream().use { destination -> original.copyTo(destination) }
            }
            val project = VideoProject(
                title = "Teste local de exportação",
                clips = listOf(Clip(
                    uri = Uri.fromFile(input).toString(), title = "Vídeo de teste",
                    sourceDurationMs = 1600L, startMs = 200L, endMs = 1400L
                )),
                subtitles = listOf(SubtitleCue(
                    text = "Legenda de teste", startMs = 200L, endMs = 1000L
                ))
            )
            instrumentation.runOnMainSync {
                engine = ExportEngine(context)
                try {
                    engine!!.start(project, 720, "UaiCortes_Teste_Exportacao.mp4",
                        preset = ExportBitratePreset.COMPACT,
                        onSuccess = { uri, _ -> saved.set(uri); completed.countDown() },
                        onError = { reason -> error.set(reason); completed.countDown() })
                } catch (failure: Exception) {
                    error.set(failure.message ?: failure.javaClass.simpleName)
                    completed.countDown()
                }
            }
            assertTrue("Exportação não terminou em 180 segundos: ${error.get()}",
                completed.await(180, TimeUnit.SECONDS))
            assertNull("Erro do Media3: ${error.get()}", error.get())
            val output = requireNotNull(saved.get()) { "MediaStore não confirmou o MP4" }
            context.contentResolver.openFileDescriptor(output, "r")?.use {
                assertTrue("MP4 exportado está vazio", it.statSize > 1024L)
            } ?: throw AssertionError("Não foi possível abrir o MP4 exportado")

            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, output)
                val width = retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
                val height = retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
                val duration = retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                assertTrue("Dimensões MP4 inválidas: ${width}x${height}", width > 0 && height > 0)
                assertTrue("Duração do corte fora da margem: $duration ms", duration in 600L..1800L)
            } finally {
                retriever.release()
            }
            val extractor = MediaExtractor()
            try {
                extractor.setDataSource(context, output, null)
                val types = (0 until extractor.trackCount).map { index ->
                    extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME).orEmpty()
                }
                assertTrue("Faixa de vídeo ausente: $types", types.any { it.startsWith("video/") })
                assertTrue("Faixa de áudio ausente: $types", types.any { it.startsWith("audio/") })
            } finally {
                extractor.release()
            }
        } finally {
            saved.get()?.let { runCatching { context.contentResolver.delete(it, null, null) } }
            instrumentation.runOnMainSync { engine?.cancel() }
            input.delete()
        }
    }
}
