package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class NumericTimeTest {
    @Test fun `digitos autoformatados em decimos`() {
        assertEquals("00:00.0", NumericTime.format("00000"))
        assertEquals("00:00.1", NumericTime.format("00001"))
        assertEquals("00:07.4", NumericTime.format("00074"))
        assertEquals("01:23.5", NumericTime.format("01235"))
        assertEquals("01:01:01.0", NumericTime.format("0101010"))
        assertEquals(7400L, NumericTime.toMs("00074"))
        assertEquals("00074", NumericTime.fromMs(7499L))
        assertNull(NumericTime.toMs(""))
    }
    @Test fun `validacao de trecho e ponto de divisao`() {
        assertTrue(TrimLogic.valid(1000, 3000, 7500))
        assertFalse(TrimLogic.valid(3000, 1000, 7500))
        assertFalse(TrimLogic.valid(0, 8000, 7500))
        assertTrue(TrimLogic.canSplit(1000, 3000, 2000))
        assertFalse(TrimLogic.canSplit(1000, 3000, 1250))
        assertEquals(7000L, TrimLogic.clampCursor(9000, 7000))
    }
}
