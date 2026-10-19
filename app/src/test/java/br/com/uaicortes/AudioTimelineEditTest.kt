package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class AudioTimelineEditTest {
    private val base = AudioTimelineDraft(timelineStartMs = 2_000L,
        sourceStartMs = 1_000L, sourceEndMs = 4_000L)

    @Test fun moveKeepsOriginalAudioRange() {
        assertEquals(base.copy(timelineStartMs = 5_000L), AudioTimelineEdit.move(base, 3_000L, 10_000L))
        assertEquals(base.copy(timelineStartMs = 7_000L), AudioTimelineEdit.move(base, 99_000L, 10_000L))
        assertEquals(base.copy(timelineStartMs = 0L), AudioTimelineEdit.move(base, -99_000L, 10_000L))
    }

    @Test fun trimLeftPreservesTimelineEndAndClampsSource() {
        val trimmed = AudioTimelineEdit.left(base, 700L)
        assertEquals(base.timelineEndMs, trimmed.timelineEndMs)
        assertEquals(2_700L, trimmed.timelineStartMs)
        assertEquals(1_700L, trimmed.sourceStartMs)
        assertEquals(4_000L, trimmed.sourceEndMs)
        assertEquals(base.copy(timelineStartMs = 1_000L, sourceStartMs = 0L),
            AudioTimelineEdit.left(base, -9_000L))
        assertEquals(AudioTrackTiming.MIN_TRIM_MS,
            AudioTimelineEdit.left(base, 99_000L).durationMs)
    }

    @Test fun trimRightRespectsBothLimits() {
        assertEquals(base.copy(sourceEndMs = 5_000L), AudioTimelineEdit.right(base, 1_000L, 8_000L, 10_000L))
        assertEquals(base.copy(sourceEndMs = 6_000L), AudioTimelineEdit.right(base, 99_000L, 6_000L, 10_000L))
        assertEquals(base.copy(sourceEndMs = 1_250L), AudioTimelineEdit.right(base, -99_000L, 8_000L, 10_000L))
        assertEquals(base.copy(sourceEndMs = 5_000L), AudioTimelineEdit.right(base, 99_000L, 8_000L, 6_000L))
    }

    @Test fun conversionsAndInvalidInput() {
        assertEquals(2_500L, AudioTimelineEdit.pixelsToMs(100f, 400f, 10_000L))
        assertEquals(-2_500L, AudioTimelineEdit.pixelsToMs(-100f, 400f, 10_000L))
        assertEquals(0L, AudioTimelineEdit.pixelsToMs(Float.NaN, 100f, 10_000L))
        assertEquals(0L, AudioTimelineEdit.pixelsToMs(100f, 0f, 10_000L))
        assertEquals(base, AudioTimelineEdit.move(base, 50L, 2_000L))
        assertTrue(AudioTrackTiming.valid(base.sourceStartMs, base.sourceEndMs, 8_000L,
            AudioTimelineEdit.move(base, 1_000L, 10_000L).timelineStartMs, 10_000L))
    }
}
