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
    @Test fun requestedAndActualBitrateStaySeparate() {
        val report = ExportQualityReport(1080,1080,1920,6_000_000,192_000,
            12_000,360,10_000_000,"video/avc",null,
            requestedVideoBitrateBps=28_000_000,presetLabel="Máxima")
        assertTrue(report.bitrateWellBelowRequest)
        assertEquals("28,0 Mb/s", report.requestedBitrate)
        assertEquals("6,0 Mb/s", report.effectiveBitrate)
        assertTrue(report.lines().any { it.contains("Máxima") })
        assertTrue(report.lines().any { it.startsWith("Nota:") })
    }

    @Test fun bitrateVariabilityIsNotAlwaysReportedAsQualityFailure() {
        val report = ExportQualityReport(720,720,1280,8_000_000,192_000,
            8_000,240,8_000_000,"video/avc",null,
            requestedVideoBitrateBps=8_000_000,presetLabel="Alta")
        assertFalse(report.bitrateWellBelowRequest)
        assertFalse(report.lines().any { it.startsWith("Nota:") })
        val unknown = report.copy(videoBitrateBps=-1)
        assertFalse(unknown.bitrateWellBelowRequest)
    }

    @Test fun missingMeasurementsAreNeverGuessed() {
        val value = ExportQualityReport(1080, -1, -1, -1, -1,
            -1, -1, -1, null, null)
        assertEquals("Não informada", value.resolution)
        assertEquals("Não informado", value.effectiveBitrate)
        assertEquals("Não informado", value.averageFps)
        assertFalse(value.fellBackInResolution)
    }
    @Test fun actualElapsedTimesAreSeparatedAndNeverInvented() {
        val missing = ExportQualityReport(1080,1920,1080,12_000_000,192_000,
            8_000,240,12_000_000,"video/avc",null)
        assertNull(missing.encodeSpeedFactor)
        assertFalse(missing.lines().any { it.startsWith("Tempo de processamento:") })
        val measured = missing.copy(encodeElapsedMs=4_000L, galleryCopyElapsedMs=400L)
        assertEquals(2.0, measured.encodeSpeedFactor!!, 0.00001)
        assertTrue(measured.lines().any { it.contains("Tempo de processamento: 4,0 s") })
        assertTrue(measured.lines().any { it.contains("Tempo de cópia para a galeria: 0,4 s") })
        assertTrue(measured.lines().any { it.contains("2,00×") })
        val unmeasuredVideo = measured.copy(durationMs = -1L)
        assertNull(unmeasuredVideo.encodeSpeedFactor)
        assertFalse(unmeasuredVideo.lines().any { it.startsWith("Velocidade de exportação:") })
    }

    @Test fun portraitFullHdDoesNotTriggerFalseFallback() {
        val portrait = ExportQualityReport(1080,1080,1920,18_000_000,192_000,
            8000,240,18_000_000,"video/avc",null)
        assertFalse(portrait.fellBackInResolution)
        assertEquals("1080 × 1920",portrait.resolution)
    }
    @Test fun savedMp4RotationIsUsedForDisplayWithoutInventingAnOrientation() {
        val original = ExportQualityReport(720, 1280, 720, 9_000_000, 192_000,
            10_000, 300, 12_000_000, "video/avc", null,
            requestedFrameWidth = 720, requestedFrameHeight = 1280)
        assertTrue(original.fellBackInResolution)
        val rotated = original.copy(savedFileWidth=1280, savedFileHeight=720,
            savedRotationDegrees=90)
        assertEquals("720 × 1280", rotated.resolution)
        assertFalse(rotated.fellBackInResolution)
        assertTrue(rotated.lines().any { it.contains("rotação 90") })
        val wrongOrientation = original.copy(savedFileWidth=1280, savedFileHeight=720,
            savedRotationDegrees=0)
        assertTrue(wrongOrientation.fellBackInResolution)
    }

    @Test fun knownFullHdMetricsAreDisplayed() {
        val value = ExportQualityReport(1080,1920,1080,18_000_000,192_000,8000,240,
            18_000_000,"video/avc",null)
        assertFalse(value.fellBackInResolution)
        assertEquals("30,0 (média estimada)", value.averageFps)
        assertTrue(value.lines().any { it.contains("18,0 Mb/s") })
    }
}
