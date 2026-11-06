package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class SubtitleCompactLayoutTest {
    @Test fun zeroAndShortVideosHaveOneWindow() {
        assertEquals(1, SubtitleCompactLayout.windowCount(0))
        assertEquals(1, SubtitleCompactLayout.windowCount(500))
        assertEquals(1, SubtitleCompactLayout.windowCount(10_000))
    }

    @Test fun absoluteTimeScrollsIntoTheCorrectWindow() {
        assertEquals(9, SubtitleCompactLayout.windowCount(90_000))
        assertEquals(0, SubtitleCompactLayout.windowFor(9_999, 90_000))
        assertEquals(1, SubtitleCompactLayout.windowFor(10_000, 90_000))
        assertEquals(8, SubtitleCompactLayout.windowFor(90_000, 90_000))
        assertEquals(0, SubtitleCompactLayout.windowFor(-1_000, 90_000))
    }

    @Test fun blocksCrossWindowsWithoutChangingOriginalTimes() {
        assertEquals(9_000L to 10_000L,
            SubtitleCompactLayout.visibleRange(0, 9_000, 12_000, 30_000))
        assertEquals(10_000L to 12_000L,
            SubtitleCompactLayout.visibleRange(1, 9_000, 12_000, 30_000))
        assertNull(SubtitleCompactLayout.visibleRange(2, 9_000, 12_000, 30_000))
    }

    @Test fun overlappingAndOutOfBoundsCuesAreHandled() {
        assertEquals(2_000L to 8_000L,
            SubtitleCompactLayout.visibleRange(0, 2_000, 8_000, 10_000))
        assertEquals(6_000L to 10_000L,
            SubtitleCompactLayout.visibleRange(0, 6_000, 16_000, 10_000))
        assertNull(SubtitleCompactLayout.visibleRange(0, 5_000, 5_000, 10_000))
        assertNull(SubtitleCompactLayout.visibleRange(1, 0, 9_000, 10_000))
        assertNull(SubtitleCompactLayout.visibleRange(-1, 0, 9_000, 10_000))
    }
    @Test fun mainVideoClipsAreVisibleWithoutAnySubtitle() {
        val durations = listOf(6_000L, 8_000L, 5_000L)
        assertEquals(listOf(
            SubtitleCompactLayout.VisibleClip(0, 0L, 6_000L),
            SubtitleCompactLayout.VisibleClip(1, 6_000L, 10_000L)),
            SubtitleCompactLayout.clipsInWindow(durations, 0, 19_000L))
        assertEquals(listOf(
            SubtitleCompactLayout.VisibleClip(1, 10_000L, 14_000L),
            SubtitleCompactLayout.VisibleClip(2, 14_000L, 19_000L)),
            SubtitleCompactLayout.clipsInWindow(durations, 1, 19_000L))
    }

    @Test fun clipsAtWindowBoundaryDoNotBecomeDuplicates() {
        assertEquals(listOf(SubtitleCompactLayout.VisibleClip(1, 10_000L, 12_000L)),
            SubtitleCompactLayout.clipsInWindow(listOf(10_000L, 2_000L), 1, 12_000L))
        assertEquals(emptyList<SubtitleCompactLayout.VisibleClip>(),
            SubtitleCompactLayout.clipsInWindow(emptyList(), 0, 10_000L))
        assertEquals(emptyList<SubtitleCompactLayout.VisibleClip>(),
            SubtitleCompactLayout.clipsInWindow(listOf(0L, 0L), 0, 10_000L))
        assertEquals(emptyList<SubtitleCompactLayout.VisibleClip>(),
            SubtitleCompactLayout.clipsInWindow(listOf(1000L), -1, 10_000L))
    }

    @Test fun clipsDoNotExtendBeyondProjectDuration() {
        val windows = SubtitleCompactLayout.clipsInWindow(listOf(30_000L), 1, 15_000L)
        assertEquals(listOf(SubtitleCompactLayout.VisibleClip(0, 10_000L, 15_000L)), windows)
    }

}
