package br.com.uaicortes

import org.junit.Assert.assertEquals
import org.junit.Test

class SubtitleTimelineEditTest {
    @Test fun moveKeepsLengthAndLimits() {
        assertEquals(200L to 900L, SubtitleTimelineEdit.move(100,800,100,1000))
        assertEquals(300L to 1000L, SubtitleTimelineEdit.move(100,800,900,1000))
        assertEquals(0L to 700L, SubtitleTimelineEdit.move(100,800,-900,1000))
        assertEquals(300L to 1000L, SubtitleTimelineEdit.move(100,800,Long.MAX_VALUE,1000))
        assertEquals(0L to 700L, SubtitleTimelineEdit.move(100,800,Long.MIN_VALUE,1000))
    }

    @Test fun trimHandlesAndMinimum() {
        assertEquals(300L to 800L, SubtitleTimelineEdit.trimLeft(100,800,200,1000))
        assertEquals(700L to 800L, SubtitleTimelineEdit.trimLeft(100,800,9999,1000))
        assertEquals(0L to 800L, SubtitleTimelineEdit.trimLeft(100,800,-9999,1000))
        assertEquals(100L to 900L, SubtitleTimelineEdit.trimRight(100,800,100,1000))
        assertEquals(100L to 200L, SubtitleTimelineEdit.trimRight(100,800,-9999,1000))
        assertEquals(100L to 1000L, SubtitleTimelineEdit.trimRight(100,800,9999,1000))
    }

    @Test fun importedShortCuesStayEditable() {
        assertEquals(10L to 60L, SubtitleTimelineEdit.move(5,55,5,100))
        assertEquals(10L to 55L, SubtitleTimelineEdit.trimLeft(5,55,5,100))
        assertEquals(5L to 50L, SubtitleTimelineEdit.trimRight(5,55,-5,100))
    }

    @Test fun edgeCasesNeverLeaveVideo() {
        assertEquals(0L to 1L, SubtitleTimelineEdit.move(0,2,100,1))
        assertEquals(0L to 1L, SubtitleTimelineEdit.trimRight(0,1,-100,1))
        assertEquals(0L to 1L, SubtitleTimelineEdit.trimLeft(0,1,100,1))
    }
}
