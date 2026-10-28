package br.com.uaicortes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportProgressUiTest {
    @Test fun percentIsNotInventedWhenMedia3DoesNotProvideIt() {
        assertNull(ExportProgressUi.renderPercent(ExportPhase.PREPARING,0))
        assertNull(ExportProgressUi.renderPercent(ExportPhase.RENDERING,null))
        assertNull(ExportProgressUi.renderPercent(ExportPhase.SAVING,99))
        assertEquals(0,ExportProgressUi.renderPercent(ExportPhase.RENDERING,0))
        assertEquals(75,ExportProgressUi.renderPercent(ExportPhase.RENDERING,75))
        assertEquals(100,ExportProgressUi.renderPercent(ExportPhase.RENDERING,120))
    }

    @Test fun allowCancelOnlyBeforeMediaStoreCopyStarts() {
        assertTrue(ExportProgressUi.canCancel(ExportPhase.PREPARING))
        assertTrue(ExportProgressUi.canCancel(ExportPhase.RENDERING))
        assertFalse(ExportProgressUi.canCancel(ExportPhase.SAVING))
    }

    @Test fun elapsedAndStageLabels() {
        assertEquals("00:00",ExportProgressUi.elapsedLabel(-30))
        assertEquals("01:05",ExportProgressUi.elapsedLabel(65))
        assertEquals("120:03",ExportProgressUi.elapsedLabel(7203))
        assertEquals("Finalizando e salvando na galeria…",ExportProgressUi.stageLabel(ExportPhase.SAVING))
    }
}
