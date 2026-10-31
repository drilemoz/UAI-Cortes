package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class ClipAudioTimingTest {
    @Test fun directOnsetAndMinClipBoundary() {
        val w = ClipAudioTiming.resolve(15000,0,8000,0,0,4000)!!
        assertEquals(ClipAudioWindow(15000,0,4000),w)
        assertNull(ClipAudioTiming.resolve(15000,6000,8000,0,0,4000))
    }
    @Test fun audioWithLaterAnchorDelaysTheStart() {
        assertEquals(ClipAudioWindow(2500,0,3500),
            ClipAudioTiming.resolve(1000,0,5000,1500,0,3500))
    }
    @Test fun invalidRangesAreRejected() {
        assertNull(ClipAudioTiming.resolve(0,-1,1000,0,0,1000))
        assertNull(ClipAudioTiming.resolve(0,0,0,0,0,1000))
        assertNull(ClipAudioTiming.resolve(0,0,1000,0,0,-1))
    }
}
