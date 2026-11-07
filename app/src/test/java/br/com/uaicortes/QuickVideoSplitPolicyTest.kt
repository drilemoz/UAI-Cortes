package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class QuickVideoSplitPolicyTest {
    @Test fun dividesWithExactMillisecondBoundaries() {
        val parts = QuickVideoSplitPolicy.parts(28_000L, QuickVideoSplitPolicy.SHORT_MS)
        assertEquals(3, parts.size)
        assertEquals(listOf(0L, 9_500L, 19_000L), parts.map { it.startMs })
        assertEquals(listOf(9_500L, 19_000L, 28_000L), parts.map { it.endMs })
        assertEquals(28_000L, parts.sumOf { it.durationMs })
    }

    @Test fun preservesLastShortPartAndVeryShortVideo() {
        val p = QuickVideoSplitPolicy.parts(20_100L, QuickVideoSplitPolicy.TEN_SECONDS_MS)
        assertEquals(listOf(10_000L, 10_000L, 100L), p.map { it.durationMs })
        assertEquals(1, QuickVideoSplitPolicy.parts(300L, QuickVideoSplitPolicy.SHORT_MS).size)
    }

    @Test fun rejectsInvalidOrVeryLargeJobs() {
        assertThrows(IllegalArgumentException::class.java) { QuickVideoSplitPolicy.parts(0L, 9_500L) }
        assertThrows(IllegalArgumentException::class.java) { QuickVideoSplitPolicy.parts(10_000L, 999L) }
        assertThrows(IllegalArgumentException::class.java) { QuickVideoSplitPolicy.parts(10_000L, 1_500L) }
        assertThrows(IllegalArgumentException::class.java) { QuickVideoSplitPolicy.parts(10_000L, 86_400_000L) }
        assertThrows(IllegalArgumentException::class.java) {
            QuickVideoSplitPolicy.parts((QuickVideoSplitPolicy.MAX_PARTS + 1L) * 9_500L, 9_500L)
        }
        val max = QuickVideoSplitPolicy.parts(QuickVideoSplitPolicy.MAX_PARTS * 9_500L, 9_500L)
        assertEquals(QuickVideoSplitPolicy.MAX_PARTS, max.size)
    }

    @Test fun supportsMinuteAndPlatformInspiredPresets() {
        val lengths = listOf(30_000L, 60_000L, 65_000L, 120_000L,
            179_000L, 180_000L, 240_000L, 480_000L)
        for (length in lengths) {
            val videoDuration = length * 2L + 1_400L
            val parts = QuickVideoSplitPolicy.parts(videoDuration, length)
            assertEquals(length.toString(), 3, parts.size)
            assertEquals(listOf(length, length, 1_400L), parts.map { it.durationMs })
            assertEquals(videoDuration, parts.sumOf { it.durationMs })
            assertEquals(listOf(0L, length, 2L * length), parts.map { it.startMs })
            assertEquals(videoDuration, parts.last().endMs)
        }
        assertEquals(2, QuickVideoSplitPolicy.parts(480_001L, QuickVideoSplitPolicy.EIGHT_MINUTES_MS).size)
    }

    @Test fun customDurationIsStrictAndBounded() {
        assertEquals(1_000L, QuickVideoSplitPolicy.customDurationMs(0, 0, 1))
        assertEquals(3_723_000L, QuickVideoSplitPolicy.customDurationMs(1, 2, 3))
        assertEquals(86_399_000L, QuickVideoSplitPolicy.customDurationMs(23, 59, 59))
        assertNull(QuickVideoSplitPolicy.customDurationMs(0, 0, 0))
        assertNull(QuickVideoSplitPolicy.customDurationMs(null, 0, 1))
        assertNull(QuickVideoSplitPolicy.customDurationMs(24, 0, 0))
        assertNull(QuickVideoSplitPolicy.customDurationMs(1, 60, 0))
        assertNull(QuickVideoSplitPolicy.customDurationMs(0, 0, 60))
        assertNull(QuickVideoSplitPolicy.customDurationMs(-1, 0, 0))
        val oneSecondParts = QuickVideoSplitPolicy.parts(2_500L, 1_000L)
        assertEquals(listOf(1_000L, 1_000L, 500L), oneSecondParts.map { it.durationMs })
    }

    @Test fun limitIsAppliedBeforeBuildingListForAnyDuration() {
        for (length in listOf(1_000L, 65_000L, 480_000L)) {
            assertEquals(180, QuickVideoSplitPolicy.parts(180L * length, length).size)
            assertThrows(IllegalArgumentException::class.java) {
                QuickVideoSplitPolicy.parts(180L * length + 1L, length)
            }
        }
    }

    @Test fun previewClockDoesNotLoseSubSecondOrHours() {
        assertEquals("00:09.500", QuickVideoSplitPolicy.timeLabel(9_500L))
        assertEquals("01:05", QuickVideoSplitPolicy.timeLabel(65_000L))
        assertEquals("01:02:03", QuickVideoSplitPolicy.timeLabel(3_723_000L))
        assertEquals("00:01.350", QuickVideoSplitPolicy.timeLabel(1_350L))
    }

    @Test fun stableUniqueFileNames() {
        assertEquals("V_deo_01_20261106_Parte_003.mp4",
            QuickVideoSplitPolicy.partName("Vídeo 01", "20261106", 3, "mp4"))
        assertEquals("UaiCortes", QuickVideoSplitPolicy.fileBase("..."))
        assertEquals("FLOW_20261106_Parte_001.m4a",
            QuickVideoSplitPolicy.partName("FLOW.mp4", "20261106", 1, "m4a"))
    }
}
