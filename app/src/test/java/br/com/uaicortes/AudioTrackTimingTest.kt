package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class AudioTrackTimingTest {
    @Test fun validTrimInsideSourceAndProject() {
        assertTrue(AudioTrackTiming.valid(1_000, 4_000, 8_000, 3_000, 10_000))
        assertFalse(AudioTrackTiming.valid(1_000, 4_000, 8_000, 8_000, 10_000))
        assertFalse(AudioTrackTiming.valid(1_000, 8_100, 8_000, 0, 10_000))
    }
    @Test fun minimumDurationAndNegativeTimes() {
        assertTrue(AudioTrackTiming.valid(0, 250, 1_000, 0, 1_000))
        assertFalse(AudioTrackTiming.valid(0, 249, 1_000, 0, 1_000))
        assertFalse(AudioTrackTiming.valid(-1, 300, 1_000, 0, 1_000))
        assertFalse(AudioTrackTiming.valid(0, 300, 1_000, -1, 1_000))
    }
    @Test fun legacyRangesRespectRemainingTimeline() {
        assertEquals(0L..2_000L, AudioTrackTiming.initialRange(0, 0, 7_000, 8_000, 10_000))
        assertEquals(1_000L..2_000L, AudioTrackTiming.initialRange(1_000, 4_000, 7_000, 9_000, 10_000))
        assertNull(AudioTrackTiming.initialRange(0, 1_000, 200, 0, 10_000))
        assertNull(AudioTrackTiming.initialRange(0, 1_000, 1_000, 0, 100))
    }
    @Test fun trimCanBeMovedWithoutChangingSourceOffset() {
        assertTrue(AudioTrackTiming.valid(5_000, 7_000, 9_000, 0, 10_000))
        assertTrue(AudioTrackTiming.valid(5_000, 7_000, 9_000, 8_000, 10_000))
        assertFalse(AudioTrackTiming.valid(5_000, 7_000, 9_000, 8_001, 10_000))
    }
}
