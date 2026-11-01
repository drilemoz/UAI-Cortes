package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class ExportSourceQualityPolicyTest {
    @Test fun warnsWhenAtLeastOneInputNeedsUpscaling() {
        val message = ExportSourceQualityPolicy.warning(480,1080)
        assertTrue(message?.contains("480px") == true)
        assertTrue(message?.contains("1080p") == true)
    }

    @Test fun doesntInventSourceSizeOrWarnForNativeResolution() {
        assertEquals(null,ExportSourceQualityPolicy.warning(null,1080))
        assertEquals(null,ExportSourceQualityPolicy.warning(0,1080))
        assertEquals(null,ExportSourceQualityPolicy.warning(1080,1080))
        assertEquals(null,ExportSourceQualityPolicy.warning(1920,720))
    }
}
