package br.com.uaicortes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QuickAudioPlacementTest {
    @Test fun crossesClipsWithoutRestartingInTheThird() {
        val window = requireNotNull(QuickAudioPlacement.resolve(List(4) { 10_000L }, 0L, 26_000L))
        assertEquals(26_000L, window.durationMs)
        assertEquals(QuickAudioPlacement.Point(0, 0L), window.first)
        assertEquals(QuickAudioPlacement.Point(2, 6_000L), window.last)
        assertEquals(0L, window.omittedMs)
    }

    @Test fun oneLongClipShowsLocalStartAndEnd() {
        val window = requireNotNull(QuickAudioPlacement.resolve(listOf(60_000L), 5_000L, 26_000L))
        assertEquals(QuickAudioPlacement.Point(0, 5_000L), window.first)
        assertEquals(QuickAudioPlacement.Point(0, 31_000L), window.last)
    }

    @Test fun exactBoundariesUseTheNextClipForStartAndPreviousClipForEnd() {
        val window = requireNotNull(QuickAudioPlacement.resolve(List(4) { 10_000L }, 10_000L, 20_000L))
        assertEquals(QuickAudioPlacement.Point(1, 0L), window.first)
        assertEquals(QuickAudioPlacement.Point(2, 10_000L), window.last)
    }

    @Test fun capsAtProjectEndAndReportsUnusedAudio() {
        val window = requireNotNull(QuickAudioPlacement.resolve(listOf(0L, 10_000L), 9_000L, 5_000L))
        assertEquals(1_000L, window.durationMs)
        assertEquals(4_000L, window.omittedMs)
        assertEquals(QuickAudioPlacement.Point(1, 10_000L), window.last)
    }

    @Test fun rejectsTooLittleSpaceInsteadOfMovingTheChosenStart() {
        assertNull(QuickAudioPlacement.resolve(listOf(10_000L), 9_751L, 4_000L))
        assertNull(QuickAudioPlacement.resolve(listOf(10_000L), 0L, 249L))
        assertNull(QuickAudioPlacement.resolve(listOf(10_000L), -1L, 4_000L))
        assertNull(QuickAudioPlacement.resolve(emptyList(), 0L, 4_000L))
    }
}
