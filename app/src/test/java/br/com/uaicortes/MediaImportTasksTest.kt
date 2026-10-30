package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class MediaImportTasksTest {
    @Test fun singleTaskAtATimeAndSafeRelease() {
        val gate = MediaTaskGate()
        val first = requireNotNull(gate.tryStart(MediaTaskKind.VIDEO))
        assertNull(gate.tryStart(MediaTaskKind.AUDIO))
        assertNull(gate.tryStart(MediaTaskKind.VIDEO))
        assertFalse(gate.finish(MediaTaskTicket(0L, MediaTaskKind.AUDIO)))
        assertEquals(first, gate.active)
        assertTrue(gate.finish(first))
        val second = requireNotNull(gate.tryStart(MediaTaskKind.VIDEO))
        assertNotEquals(first, second)
        assertFalse(gate.finish(first))
        assertEquals(second, gate.active)
        assertTrue(gate.finish(second))
        assertNull(gate.active)
    }

    @Test fun lateResultCannotApplyToAnotherToolOrRoute() {
        val origin = MediaTaskOrigin("project-1", "media", "append")
        assertTrue(origin.matches("project-1", "media", "append"))
        assertFalse(origin.matches("project-2", "media", "append"))
        assertFalse(origin.matches("project-1", "home", "append"))
        assertFalse(origin.matches("project-1", "media", "replace"))
    }
}
