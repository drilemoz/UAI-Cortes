package br.com.uaicortes

import android.content.Context
import android.net.Uri
import java.io.File
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

internal data class StutterClip(val index: Int, val timelineStartMs: Long, val clip: Clip) {
    val timelineEndMs get() = timelineStartMs + clip.durationMs

    fun window(centerMs: Long): StutterWindow {
        val size = minOf(5_000L, clip.durationMs)
        val maxStart = clip.durationMs - size
        val offset = (centerMs - timelineStartMs - size / 2L).coerceIn(0L, maxStart)
        return StutterWindow(this, offset, size)
    }
}

internal data class StutterWindow(val source: StutterClip, val offsetMs: Long, val durationMs: Long) {
    val projectStartMs get() = source.timelineStartMs + offsetMs
    val sourceStartMs get() = source.clip.startMs + offsetMs

    fun isolatedProject(): VideoProject = VideoProject(clips = listOf(source.clip.copy(
        startMs = sourceStartMs, endMs = sourceStartMs + durationMs)))
}

internal fun stutterClips(project: VideoProject): List<StutterClip> = buildList {
    var at = 0L
    project.clips.forEachIndexed { i, clip ->
        if (!clip.isImage && clip.durationMs >= 500L) add(StutterClip(i, at, clip))
        at += clip.durationMs
    }
}

internal object ExportStutterAnalysis {
    enum class Stage(val text: String) {
        SOURCE("Lendo quadros do clipe original…"),
        MP4("Comparando com o MP4 da montagem…"),
        RENDER("Exportando teste sem PiP, transições ou camadas…"),
        TEST("Comparando com a exportação de teste…")
    }
    data class Outcome(val comparison: ExportFrameOrder.Result? = null, val problem: Problem? = null)
    enum class Problem(val text: String) {
        SOURCE("Não foi possível ler os quadros do original neste aparelho."),
        MP4("Não foi possível ler os quadros desse MP4 neste aparelho."),
        DIFFERENT_DURATION("A duração do MP4 não corresponde. Selecione uma exportação desta mesma montagem."),
        TEST_RENDER("A exportação de teste não terminou."),
        TEST_READ("Não foi possível ler os quadros da exportação de teste.")
    }
    data class Report(val window: StutterWindow, val exported: Outcome, val isolated: Outcome) {
        private fun line(label: String, outcome: Outcome, timelineOffsetUs: Long): String {
            val r = outcome.comparison ?: return "$label — ${outcome.problem?.text.orEmpty()}"
            val evidence = when {
                !r.sufficient() -> "inconclusivo, pouco movimento ou pouca correspondência"
                r.hasPattern() -> "${r.reversals} retornos de quadro, primeiro em " +
                    position((timelineOffsetUs + r.firstReversalUs) / 1_000)
                r.reversals > 0 -> "1 retorno isolado em " +
                    position((timelineOffsetUs + r.firstReversalUs) / 1_000) + ", sem padrão confirmado"
                else -> "sem retorno de quadro detectado"
            }
            return "$label — $evidence (${r.matched}/${r.frames} comparados)."
        }

        private fun position(ms: Long): String = java.lang.String.format(java.util.Locale.ROOT,
            "%02d:%02d.%03d", ms / 60_000, ms / 1_000 % 60, ms % 1_000)

        fun conclusion(): String {
            val a = exported.comparison
            val b = isolated.comparison
            return when {
                a == null || !a.sufficient() ->
                    "A comparação com o MP4 é inconclusiva. Confira se o arquivo e o trecho são os mesmos."
                !a.hasPattern() ->
                    "Não houve padrão de retorno detectado neste trecho. Isso não descarta outros tipos de engasgo."
                b == null || !b.sufficient() ->
                    "O padrão apareceu no MP4, mas o teste isolado não permite localizar a causa."
                b.hasPattern() ->
                    "O padrão também apareceu sem as camadas. Investigue a exportação básica deste clipe."
                b.reversals > 0 ->
                    "O MP4 tem um padrão e o teste isolado teve um retorno isolado. A causa ainda não foi isolada."
                else ->
                    "Só o MP4 da montagem apresentou o padrão neste teste. Isole PiP, transições e outros efeitos, um de cada vez."
            }
        }

        fun text(): String {
            val start = window.projectStartMs
            return listOf(
                "Trecho ${window.source.index + 1} · ${formatPipMs(start)} a ${formatPipMs(start + window.durationMs)}",
                line("MP4 da montagem", exported, 0L),
                line("Teste sem camadas", isolated, start * 1_000L),
                conclusion()
            ).joinToString("\n")
        }
    }

    suspend fun inspect(context: Context, window: StutterWindow, expectedDurationMs: Long, exported: Uri,
                        quality: Int, preset: ExportBitratePreset, destination: ExportDestination,
                        frameMode: VerticalFrameMode, onStage: (Stage) -> Unit): Report {
        val from = window.sourceStartMs * 1_000L
        val at = window.projectStartMs * 1_000L
        val size = window.durationMs * 1_000L
        val samplerContext = context.applicationContext
        onStage(Stage.SOURCE)
        val original = read(samplerContext, Uri.parse(window.source.clip.uri), from, size)
        if (original == null || original.frames.size < 8) {
            val problem = Outcome(problem = Problem.SOURCE)
            return Report(window, problem, problem)
        }
        suspend fun compare(sample: ExportFrameSampling.Sample, timelineUs: Long): Outcome =
            withContext(Dispatchers.Default) {
                Outcome(comparison = ExportFrameOrder.compare(original.frames, from, sample.frames, timelineUs))
            }
        onStage(Stage.MP4)
        val saved = read(samplerContext, exported, at, size)
        val began = when {
            saved == null -> Outcome(problem = Problem.MP4)
            saved.durationUs > 0L &&
                kotlin.math.abs(saved.durationUs / 1_000L - expectedDurationMs) > maxOf(1000L, expectedDurationMs / 100L) ->
                Outcome(problem = Problem.DIFFERENT_DURATION)
            else -> compare(saved, at)
        }
        val engine = ExportEngine(samplerContext)
        var temporary: File? = null
        try {
            onStage(Stage.RENDER)
            val testFile = try {
                withTimeoutOrNull(55_000L) {
                    suspendCancellableCoroutine<File?> { wait ->
                        engine.start(window.isolatedProject(), quality, "teste-engasgos.mp4", preset,
                            destination, frameMode, onSuccess = { _, _ -> },
                            onError = { if (wait.isActive) wait.resume(null) },
                            onPreviewReady = { file ->
                                if (wait.isActive) {
                                    temporary = file
                                    wait.resume(file)
                                } else file.delete()
                            })
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                DiagnosticRecorder.exception("stutter_test_render", error)
                null
            }
            if (testFile == null) return Report(window, began, Outcome(problem = Problem.TEST_RENDER))
            onStage(Stage.TEST)
            val sample = read(samplerContext, Uri.fromFile(testFile), 0L, size)
            return Report(window, began, if (sample == null) Outcome(problem = Problem.TEST_READ) else compare(sample, 0L))
        } finally {
            engine.cancel()
            temporary?.delete()
        }
    }

    private suspend fun read(context: Context, uri: Uri, start: Long, size: Long): ExportFrameSampling.Sample? =
        withContext(Dispatchers.IO) {
            try {
                ExportFrameSampling.read(context, uri, start, start + size)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                DiagnosticRecorder.exception("stutter_read_frames", error)
                null
            }
        }
}
