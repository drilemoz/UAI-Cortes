package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class PreviewPlaybackTest {
    @Test fun speedsArePositiveSortedAndReachBothExtremes() {
        assertEquals(14, PreviewPlayback.SPEEDS.size)
        assertEquals(0.1f, PreviewPlayback.SPEEDS.first(), 0.0001f)
        assertEquals(6f, PreviewPlayback.SPEEDS.last(), 0.0001f)
        assertEquals(PreviewPlayback.SPEEDS.distinct().sorted(), PreviewPlayback.SPEEDS)
    }
    @Test fun labelsInPortuguese() {
        assertEquals("0,1×", PreviewPlayback.label(.1f))
        assertEquals("0,25×", PreviewPlayback.label(.25f))
        assertEquals("1×", PreviewPlayback.label(1f))
        assertEquals("2,5×", PreviewPlayback.label(2.5f))
        assertEquals("6×", PreviewPlayback.label(6f))
    }
    @Test fun plusMinusTraverseOrderedSpeedsAndClampAtLimits() {
        assertEquals(.75f, PreviewPlayback.previous(1f))
        assertEquals(1.25f, PreviewPlayback.next(1f))
        assertEquals(.1f, PreviewPlayback.previous(.1f))
        assertEquals(6f, PreviewPlayback.next(6f))
        assertEquals(1.25f, PreviewPlayback.next(PreviewPlayback.next(.75f)))
    }
    @Test(expected = IllegalArgumentException::class)
    fun unsupportedSpeedRejected() { PreviewPlayback.label(7f) }
}
