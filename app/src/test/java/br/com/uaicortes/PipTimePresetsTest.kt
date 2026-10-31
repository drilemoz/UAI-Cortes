package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class PipTimePresetsTest {
    @Test fun appliesGlobalClippedPeriods() {
        val periods = PipTimePresets.clipPeriods(listOf(11_000L, 7_500L, 2_000L))
        assertEquals(PipTimePresets.Period(0, 11_000), periods[0])
        assertEquals(PipTimePresets.Period(11_000, 18_500), periods[1])
        assertEquals(PipTimePresets.Period(18_500, 20_500), periods[2])
    }
    @Test fun videoPipCannotExceedItsSource() {
        val selected = PipTimePresets.Period(11_000, 18_500)
        assertEquals(3_000L, PipTimePresets.limited(selected, false, 3_000L)?.durationMs)
        assertEquals(7_500L, PipTimePresets.limited(selected, true, null)?.durationMs)
        assertNull(PipTimePresets.limited(selected, false, null))
        assertNull(PipTimePresets.limited(selected, false, 250L))
    }
}
