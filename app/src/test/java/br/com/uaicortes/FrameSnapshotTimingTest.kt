package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class FrameSnapshotTimingTest {
    @Test fun slicesWithinTrimmedSource() {
        val value = FrameSnapshotTiming.slice(1000, 5000, 3250)!!
        assertEquals(2750L, value.startMs)
        assertEquals(4350L, value.endMs)
        assertEquals(500L, value.frameAtMs)
    }

    @Test fun nearEndNeverExceedsOriginal() {
        val value = FrameSnapshotTiming.slice(1000, 2000, 1999)!!
        assertEquals(2000L, value.endMs)
        assertEquals(500L, value.frameAtMs)
    }

    @Test fun startAndInvalidInputs() {
        val initial = FrameSnapshotTiming.slice(100, 500, 100)!!
        assertEquals(100L, initial.startMs)
        assertEquals(0L, initial.frameAtMs)
        assertNull(FrameSnapshotTiming.slice(400, 300, 350))
    }

    @Test fun overlayClippedToWindowWithoutChangingOriginal() {
        assertEquals(200L..239L, FrameSnapshotTiming.overlap(190L, 50L, 200L, 250L))
        assertNull(FrameSnapshotTiming.overlap(0L, 30L, 100L, 200L))
        assertEquals(100L..129L, FrameSnapshotTiming.overlap(100L, 30L, 100L, 200L))
    }
}
