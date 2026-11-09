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
        // A janela retrocede quando faltam 1.600 ms até o fim do clipe.
        // O frame é relativo à janela final, não ao lead ideal de 500 ms.
        val value = FrameSnapshotTiming.slice(1000, 2000, 1999)!!
        assertEquals(1000L, value.startMs)
        assertEquals(2000L, value.endMs)
        assertEquals(999L, value.frameAtMs)
        assertEquals(1000L, value.durationMs)
        assertTrue(value.frameAtMs in 0 until value.durationMs)
        assertEquals(1999L, value.startMs + value.frameAtMs)
    }

    @Test fun windowNearEndKeepsFrameWithinAvailableSource() {
        val value = FrameSnapshotTiming.slice(0, 10_000, 9_999)!!
        assertEquals(8_400L, value.startMs)
        assertEquals(10_000L, value.endMs)
        assertEquals(1_599L, value.frameAtMs)
        assertEquals(9_999L, value.startMs + value.frameAtMs)
    }

    @Test fun windowShorterThanDesiredNeverRequestsOutsideClip() {
        val value = FrameSnapshotTiming.slice(3_000, 3_240, 3_239)!!
        assertEquals(3_000L, value.startMs)
        assertEquals(3_240L, value.endMs)
        assertEquals(239L, value.frameAtMs)
        assertEquals(240L, value.durationMs)
        assertTrue(value.frameAtMs in 0 until value.durationMs)
    }

    @Test fun outOfBoundsRequestsClampToAvailableSamples() {
        val before = FrameSnapshotTiming.slice(2_000, 4_000, 0)!!
        assertEquals(2_000L, before.startMs + before.frameAtMs)
        val after = FrameSnapshotTiming.slice(2_000, 4_000, 5_000)!!
        assertEquals(3_999L, after.startMs + after.frameAtMs)
        assertTrue(after.frameAtMs in 0 until after.durationMs)
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
