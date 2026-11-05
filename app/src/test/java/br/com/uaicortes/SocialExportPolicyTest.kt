package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class SocialExportPolicyTest {
    @Test fun socialDestinationsRequestExactVerticalFrames() {
        for (destination in ExportDestination.values().filter { it.isVertical }) {
            assertEquals(360 to 640, SocialExportPolicy.dimensions(destination, 360))
            assertEquals(480 to 854, SocialExportPolicy.dimensions(destination, 480))
            assertEquals(720 to 1280, SocialExportPolicy.dimensions(destination, 720))
            assertEquals(1080 to 1920, SocialExportPolicy.dimensions(destination, 1080))
            assertEquals("1080 × 1920 (9:16)", SocialExportPolicy.requestedLabel(destination,1080))
            assertEquals(ExportBitratePreset.COMPACT,
                SocialExportPolicy.recommendedBitrate(destination))
        }
    }

    @Test fun originalKeepsExistingGeometryAndHighQuality() {
        assertNull(SocialExportPolicy.dimensions(ExportDestination.ORIGINAL,720))
        assertEquals(ExportBitratePreset.HIGH,
            SocialExportPolicy.recommendedBitrate(ExportDestination.ORIGINAL))
        assertEquals(ExportDestination.ORIGINAL, ExportDestination.fromId("desconhecido"))
        assertEquals(VerticalFrameMode.FIT, VerticalFrameMode.fromId("desconhecido"))
    }

    @Test fun onlySupportedResolutionsAreAccepted() {
        assertThrows(IllegalArgumentException::class.java) {
            SocialExportPolicy.dimensions(ExportDestination.SHORTS, 240)
        }
        assertThrows(IllegalArgumentException::class.java) {
            SocialExportPolicy.dimensions(ExportDestination.ORIGINAL, 1440)
        }
    }

    @Test fun framingIsExplicitAndWarnsBeforeCropping() {
        assertTrue(SocialExportPolicy.framingDescription(VerticalFrameMode.FIT).contains("barras"))
        assertTrue(SocialExportPolicy.framingDescription(VerticalFrameMode.CENTER_CROP).contains("cortando"))
    }

    @Test fun exactPortraitReportDetectsChangedAspectEvenWhenShortSideMatches() {
        val report = ExportQualityReport(720,720,1280,5_000_000,192_000,
            10_000,300,7_000_000,"video/avc",null,
            requestedFrameWidth=720,requestedFrameHeight=1280,destinationLabel="Shorts")
        assertFalse(report.fellBackInResolution)
        assertTrue(report.lines().any { it.contains("720 × 1280") })
        assertTrue(report.lines().any { it.contains("Shorts") })
        assertTrue(report.copy(width=1280,height=720).fellBackInResolution)
        assertTrue(report.copy(width=720,height=1080).fellBackInResolution)
    }
}
