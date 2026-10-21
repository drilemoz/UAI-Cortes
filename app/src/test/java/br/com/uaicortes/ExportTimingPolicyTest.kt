package br.com.uaicortes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportTimingPolicyTest {
    @Test fun validSourceIntervals() {
        assertTrue(ExportTimingPolicy.validClip(0, 1000, 1000))
        assertTrue(ExportTimingPolicy.validClip(250, 750, 1000))
        assertFalse(ExportTimingPolicy.validClip(-1, 500, 1000))
        assertFalse(ExportTimingPolicy.validClip(600, 600, 1000))
        assertFalse(ExportTimingPolicy.validClip(800, 700, 1000))
        assertFalse(ExportTimingPolicy.validClip(0, 1001, 1000))
        assertFalse(ExportTimingPolicy.validClip(0, 1, 0))
    }

    @Test fun projectTimeIncludesPreviousCutsNotSourceOffset() {
        val clips = listOf(
            Clip(uri="content://video/1", title="primeiro", sourceDurationMs=10000,
                startMs=2000,endMs=6000),
            Clip(uri="content://video/2", title="segundo", sourceDurationMs=12000,
                startMs=5000,endMs=9000))
        assertEquals(0L, ExportTimingPolicy.projectTime(clips,0,0))
        assertEquals(2500L, ExportTimingPolicy.projectTime(clips,0,2500))
        assertEquals(4000L, ExportTimingPolicy.projectTime(clips,1,0))
        assertEquals(5000L, ExportTimingPolicy.projectTime(clips,1,1000))
        assertEquals(8000L, ExportTimingPolicy.projectTime(clips,1,9000))
        assertEquals(0L, ExportTimingPolicy.projectTime(emptyList(),0,0))
    }
}
