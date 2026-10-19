package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class TimelinePrecisionTest {
    @Test fun bounds() {
        assertEquals(0L,TimelinePrecision.step(50,-100,1000))
        assertEquals(1000L,TimelinePrecision.step(950,100,1000))
        assertEquals(350L,TimelinePrecision.step(250,100,1000))
        assertEquals(1000L,TimelinePrecision.step(Long.MAX_VALUE-1,100,1000))
    }
    @Test fun sourcesAtBoundaries() {
        val c=listOf("a" to 400L,"b" to 600L)
        assertEquals(0 to 399L,TimelinePrecision.sourceAt(c,399))
        assertEquals(1 to 0L,TimelinePrecision.sourceAt(c,400))
        assertEquals(1 to 600L,TimelinePrecision.sourceAt(c,1000))
        assertNull(TimelinePrecision.sourceAt(emptyList(),0))
    }
    @Test fun proportionalSubtitleCoordinates() {
        assertEquals(400 to 240,SubtitlePlacement.anchorPixels(1000,500,200,40,.5f,.56f))
        val dragged=SubtitlePlacement.drag(.5f,.5f,100f,-50f,1000,500)
        assertEquals(.6f,dragged.first,0.0001f)
        assertEquals(.4f,dragged.second,0.0001f)
        assertEquals(.9f,SubtitlePlacement.nudge(.9f,.9f,.1f,.1f).first,0.0001f)
        assertEquals(.1f,SubtitlePlacement.nudge(.1f,.1f,-.1f,-.1f).second,0.0001f)
    }
}
