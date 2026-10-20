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
}
