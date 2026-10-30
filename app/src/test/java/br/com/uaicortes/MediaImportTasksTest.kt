package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class MediaImportTasksTest {
    @Test fun singleTaskAtATimeAndSafeRelease() {
        val gate = MediaTaskGate()
        assertTrue(gate.tryStart(MediaTaskKind.VIDEO))
        assertFalse(gate.tryStart(MediaTaskKind.AUDIO))
        assertFalse(gate.tryStart(MediaTaskKind.VIDEO))
        gate.finish(MediaTaskKind.AUDIO)
        assertEquals(MediaTaskKind.VIDEO, gate.active)
        gate.finish(MediaTaskKind.VIDEO)
        assertNull(gate.active)
        assertTrue(gate.tryStart(MediaTaskKind.OVERLAY))
    }

    @Test fun lateResultCannotApplyToAnotherToolOrRoute() {
        val origin = MediaTaskOrigin("project-1", "media", "append")
        assertTrue(origin.matches("project-1", "media", "append"))
        assertFalse(origin.matches("project-2", "media", "append"))
        assertFalse(origin.matches("project-1", "home", "append"))
        assertFalse(origin.matches("project-1", "media", "replace"))
    }
}
