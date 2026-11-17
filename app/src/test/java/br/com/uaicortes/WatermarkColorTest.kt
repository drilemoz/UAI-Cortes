package br.com.uaicortes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WatermarkColorTest {
    @Test fun sixHexDigitsKeepOpacitySeparate() {
        assertEquals("12ABEF", WatermarkColor.hex(0x4012ABEF))
        assertEquals(0xFF12ABEF.toInt(), WatermarkColor.parse(" #12abef "))
        assertEquals(0xFF000000.toInt(), WatermarkColor.parse("000000"))
    }

    @Test fun rejectsInvalidAndAlphaHexCodes() {
        listOf("", "#123", "-12345", "GG1234", "AA112233").forEach {
            assertNull(WatermarkColor.parse(it))
        }
    }
}
