package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class FrameStepPolicyTest {
    @Test fun nominalFpsControlsInterval() {
        assertEquals(33L, FrameStepPolicy.intervalMs(30f))
        assertEquals(40L, FrameStepPolicy.intervalMs(25f))
        assertEquals(17L, FrameStepPolicy.intervalMs(60f))
        assertEquals(33L, FrameStepPolicy.intervalMs(null))
        assertEquals(33L, FrameStepPolicy.intervalMs(Float.NaN))
        assertEquals(33L, FrameStepPolicy.intervalMs(1000f))
    }
    @Test fun clampsAndCrossesClipBoundariesInGlobalTime() {
        assertEquals(0L, FrameStepPolicy.target(0, 5000, -1, 25f))
        assertEquals(5000L, FrameStepPolicy.target(5000, 5000, 1, 25f))
        assertEquals(980L, FrameStepPolicy.target(1020, 5000, -1, 25f))
        assertEquals(1060L, FrameStepPolicy.target(1020, 5000, 1, 25f))
        assertEquals(0L, FrameStepPolicy.target(200, 0, 1, 25f))
    }
    @Test(expected = IllegalArgumentException::class)
    fun invalidDirectionRejected() { FrameStepPolicy.target(100, 1000, 0, 30f) }
}
