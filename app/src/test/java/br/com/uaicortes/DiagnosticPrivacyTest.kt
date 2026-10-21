package br.com.uaicortes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticPrivacyTest {
    @Test fun hidesPersonalMediaLocations() {
        val privateMessage = "Asset loader error content://media/external/video/123 /storage/emulated/0/DCIM/secret.mp4"
        val safe = DiagnosticPrivacy.safeFailure(privateMessage)
        assertTrue(safe.contains("ASSET_LOADER"))
        assertFalse(safe.contains("content://"))
        assertFalse(safe.contains("secret.mp4"))
    }
    @Test fun categorizesMedia3Failure() {
        val safe = DiagnosticPrivacy.safeFailure(
            "ERROR_CODE_VIDEO_FRAME_PROCESSING_FAILED: width and height must be > 0")
        assertEquals("ERROR_CODE_VIDEO_FRAME_PROCESSING_FAILED reason=INVALID_FRAME_DIMENSIONS", safe)
    }
    @Test fun quantizesTouchAndBounds() {
        assertEquals("x=50% y=25%", DiagnosticPrivacy.touchGrid(300f, 200f, 600, 800))
        assertEquals("x=0% y=0%", DiagnosticPrivacy.touchGrid(-20f, Float.NaN, 600, 800))
        assertEquals("x=100% y=100%", DiagnosticPrivacy.touchGrid(900f, 1200f, 600, 800))
    }
    @Test fun safeActionName() {
        assertEquals("export_start_720p", DiagnosticPrivacy.safeIdentifier("export start 720p"))
        assertFalse(DiagnosticPrivacy.safeIdentifier("file://private").contains("/"))
    }
}
