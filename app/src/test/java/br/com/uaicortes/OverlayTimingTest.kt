package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class OverlayTimingTest {
    @Test fun newPipStartsExactlyAfterLatestEnd() {
        val first = Overlay("content://one", true, 2000L, startAtMs = 500L)
        val second = Overlay("content://two", false, 1700L, startAtMs = 3000L)
        assertEquals(4700L, OverlayTiming.nextStart(listOf(first, second), 10000L))
        assertEquals(0L, OverlayTiming.nextStart(emptyList(), 10000L))
        assertEquals(4700L, OverlayTiming.nextStart(listOf(second, first), 10000L))
    }
    @Test fun noRoomRequiresUserChoice() {
        val full = Overlay("content://one", true, 7400L)
        assertNull(OverlayTiming.nextStart(listOf(full), 7400L))
        assertNull(OverlayTiming.nextStart(listOf(full.copy(durationMs = 7000L)), 7400L))
        assertEquals(0L, OverlayTiming.suggestedOverlapStart(listOf(full), 7400L))
        assertNull(OverlayTiming.nextStart(listOf(full), 0L))
    }
    @Test fun acceptsTypedClockFormats() {
        assertEquals(7000L, TimeInput.parseMs("00:07"))
        assertEquals(7400L, TimeInput.parseMs("00:07.4"))
        assertEquals(7400L, TimeInput.parseMs("00:07,400"))
        assertEquals(3723000L, TimeInput.parseMs("01:02:03"))
        assertEquals(1001L, TimeInput.parseMs("1.001"))
        assertEquals(60000L, TimeInput.parseMs("01:00"))
    }
    @Test fun rejectsInvalidTimes() {
        listOf("", "-1", "xx:07", "00:60", "01:02:60", "01:60:12",
            "00:07.1234", "1::2", "999999999999999999:00").forEach {
            assertNull("Deveria recusar $it", TimeInput.parseMs(it))
        }
    }
}
