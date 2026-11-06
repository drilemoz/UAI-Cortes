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
        assertThrows(IllegalArgumentException::class.java) { QuickVideoSplitPolicy.parts(10_000L, 1_000L) }
        assertThrows(IllegalArgumentException::class.java) {
            QuickVideoSplitPolicy.parts((QuickVideoSplitPolicy.MAX_PARTS + 1L) * 9_500L, 9_500L)
        }
        val max = QuickVideoSplitPolicy.parts(QuickVideoSplitPolicy.MAX_PARTS * 9_500L, 9_500L)
        assertEquals(QuickVideoSplitPolicy.MAX_PARTS, max.size)
    }

    @Test fun stableUniqueFileNames() {
        assertEquals("V_deo_01_20261106_Parte_003.mp4",
            QuickVideoSplitPolicy.partName("Vídeo 01", "20261106", 3, "mp4"))
        assertEquals("UaiCortes", QuickVideoSplitPolicy.fileBase("..."))
        assertEquals("FLOW_20261106_Parte_001.m4a",
            QuickVideoSplitPolicy.partName("FLOW.mp4", "20261106", 1, "m4a"))
    }
}
