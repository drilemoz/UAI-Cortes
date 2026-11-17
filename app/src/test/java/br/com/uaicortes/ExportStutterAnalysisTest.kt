package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test
import java.util.Random

class ExportStutterAnalysisTest {
    private fun frames() = (0..44).map { index ->
        val random = Random(index.toLong())
        val grid = ByteArray(ExportFrameOrder.PIXELS) { random.nextInt(128).plus(64).toByte() }
        ExportFrameOrder.Frame(index * 40_000L, grid)
    }

    @Test fun findsAnimationThatReturnsToEarlierFramesDespiteIncreasingOutputTimestamps() {
        val source = frames()
        val sequence = listOf(0, 1, 2, 1, 2, 3, 4, 5, 4, 5, 6, 7, 8, 9, 10)
        val output = sequence.mapIndexed { index, frame ->
            ExportFrameOrder.Frame(index * 33_333L + 10_000_000L, source[frame].luma)
        }
        val report = ExportFrameOrder.compare(source.take(13), 0L, output, 10_000_000L)
        assertTrue(report.sufficient())
        assertTrue(report.hasPattern())
        assertEquals(2, report.reversals)
        assertEquals(output[3].timeUs, report.firstReversalUs)
    }

    @Test fun conversionToHigherFpsCanRepeatAdjacentFramesWithoutReturning() {
        val source = frames()
        val output = (0..52).map { index ->
            val time = index * 33_333L
            ExportFrameOrder.Frame(time, source[(time / 40_000L).toInt()].luma)
        }
        val result = ExportFrameOrder.compare(source, 0L, output, 0L)
        assertTrue(result.sufficient())
        assertEquals(0, result.reversals)
    }

    @Test fun motionlessFramesDoNotYieldFalseConfidence() {
        val still = ByteArray(ExportFrameOrder.PIXELS) { 127 }
        val source = (0..50).map { ExportFrameOrder.Frame(it * 40_000L, still) }
        val result = ExportFrameOrder.compare(source, 0L, source, 0L)
        assertFalse(result.sufficient())
        assertFalse(result.hasPattern())
    }

    @Test fun chosenWindowKeepsClipTrimAndNeverCrossesTheCut() {
        val title = "Imagem"
        val photo = Clip(uri = "file:///photo", title = title, sourceDurationMs = 2_000L, isImage = true)
        val video = Clip(uri = "file:///video", title = "Clipe", sourceDurationMs = 20_000L,
            startMs = 3_000L, endMs = 11_000L)
        val source = stutterClips(VideoProject(clips = listOf(photo, video))).single()
        val selected = source.window(13_000L)
        assertEquals(1, selected.source.index)
        assertEquals(5_000L, selected.projectStartMs)
        assertEquals(6_000L, selected.sourceStartMs)
        val sample = selected.isolatedProject()
        assertEquals(5_000L, sample.durationMs)
        assertEquals(11_000L, sample.clips.single().endMs)
        assertTrue(sample.overlays.isEmpty() && sample.transitions.isEmpty() && sample.audioTracks.isEmpty())
        assertEquals(3_000L, video.startMs)
    }
}
