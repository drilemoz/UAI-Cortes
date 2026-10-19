package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class PreviewPlaybackTest {
    @Test fun allSpeedsArePositiveAndOrdered() {
        assertEquals(7, PreviewPlayback.SPEEDS.size)
        assertEquals(0.25f, PreviewPlayback.SPEEDS.first(), 0.0001f)
        assertEquals(2f, PreviewPlayback.SPEEDS.last(), 0.0001f)
        assertEquals(PreviewPlayback.SPEEDS.distinct().sorted(), PreviewPlayback.SPEEDS)
    }
    @Test fun ptBrLabels() {
        assertEquals("1×", PreviewPlayback.label(1f))
        assertEquals("0,25×", PreviewPlayback.label(.25f))
        assertEquals("1,5×", PreviewPlayback.label(1.5f))
        assertEquals("2×", PreviewPlayback.label(2f))
    }
    @Test(expected = IllegalArgumentException::class)
    fun unsupportedSpeedRejected() { PreviewPlayback.label(3f) }
}
