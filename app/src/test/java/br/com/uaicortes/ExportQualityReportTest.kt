package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class ExportQualityReportTest {
    @Test fun actualMetricsRemainDistinctFromRequestedSettings() {
        val value = ExportQualityReport(1080, 720, 1280, 5_230_000, 180_000,
            10_000, 300, 7_000_000, "video/avc", "codec")
        assertEquals("720 × 1280", value.resolution)
        assertTrue(value.fellBackInResolution)
        assertEquals("5,2 Mb/s", value.effectiveBitrate)
        assertTrue(value.lines().any { it.startsWith("Aviso:") })
    }
    @Test fun missingMeasurementsAreNeverGuessed() {
        val value = ExportQualityReport(1080, -1, -1, -1, -1,
            -1, -1, -1, null, null)
        assertEquals("Não informada", value.resolution)
        assertEquals("Não informado", value.effectiveBitrate)
        assertEquals("Não informado", value.averageFps)
        assertFalse(value.fellBackInResolution)
    }
    @Test fun portraitFullHdDoesNotTriggerFalseFallback() {
        val portrait = ExportQualityReport(1080,1080,1920,18_000_000,192_000,
            8000,240,18_000_000,"video/avc",null)
        assertFalse(portrait.fellBackInResolution)
        assertEquals("1080 × 1920",portrait.resolution)
    }
    @Test fun knownFullHdMetricsAreDisplayed() {
        val value = ExportQualityReport(1080,1920,1080,18_000_000,192_000,8000,240,
            18_000_000,"video/avc",null)
        assertFalse(value.fellBackInResolution)
        assertEquals("30,0 (média estimada)", value.averageFps)
        assertTrue(value.lines().any { it.contains("18,0 Mb/s") })
    }
}
