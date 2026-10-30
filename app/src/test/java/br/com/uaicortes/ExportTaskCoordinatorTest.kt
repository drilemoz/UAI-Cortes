package br.com.uaicortes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportTaskCoordinatorTest {
    @Test fun lifecycleUsesRealProgressOnlyDuringRendering() {
        val tasks = ExportTaskCoordinator()
        val id = tasks.start("saida.mp4", 1080, 1000L)!!
        assertTrue(tasks.snapshot.working)
        assertEquals(ExportPhase.PREPARING, tasks.snapshot.phase)
        assertTrue(tasks.tick(id, 87, 3200L))
        assertNull(tasks.snapshot.progress)
        assertEquals(2L, tasks.snapshot.elapsedSeconds)
        assertTrue(tasks.rendering(id))
        assertTrue(tasks.tick(id, 67, 5600L))
        assertEquals(67, tasks.snapshot.progress)
        assertTrue(tasks.saving(id))
        assertNull(tasks.snapshot.progress)
        assertFalse(tasks.cancel(id))
        assertTrue(tasks.success(id))
        assertFalse(tasks.snapshot.working)
    }

    @Test fun staleCallbacksCannotChangeTheNextExport() {
        val tasks = ExportTaskCoordinator()
        val old = tasks.start("old.mp4", 720, 1L)!!
        assertTrue(tasks.cancel(old))
        val latest = tasks.start("new.mp4", 1080, 2L)!!
        assertFalse(tasks.saving(old))
        assertFalse(tasks.failure(old, "antigo"))
        assertFalse(tasks.success(old))
        assertFalse(tasks.tick(old, 100, 600L))
        assertTrue(tasks.snapshot.working)
        assertEquals("new.mp4", tasks.snapshot.fileName)
        assertTrue(tasks.rendering(latest))
    }

    @Test fun duplicateStartAndSavingCancellationAreRefused() {
        val tasks = ExportTaskCoordinator()
        val id = tasks.start("video.mp4", 1080, 100L)!!
        assertNull(tasks.start("outro.mp4", 720, 100L))
        assertTrue(tasks.saving(id))
        assertFalse(tasks.cancel(id))
        assertFalse(tasks.reset())
        assertTrue(tasks.failure(id, "disco cheio"))
        assertEquals("Erro: disco cheio", tasks.snapshot.message)
        assertTrue(tasks.reset())
        assertEquals("", tasks.snapshot.message)
    }

    @Test fun cancelledExportCanRestartAndLateSuccessDoesNothing() {
        val tasks = ExportTaskCoordinator()
        val old = tasks.start("old.mp4", 720, 100L)!!
        assertTrue(tasks.rendering(old))
        assertTrue(tasks.cancel(old))
        assertEquals("Exportação cancelada.", tasks.snapshot.message)
        assertFalse(tasks.success(old))
        val next = tasks.start("next.mp4", 1080, 200L)!!
        assertTrue(next > old)
        assertEquals("", tasks.snapshot.message)
        assertTrue(tasks.rendering(next))
        assertTrue(tasks.success(next))
    }

    @Test fun progressAndElapsedAreBoundedWithoutFabricatingNumbers() {
        val tasks = ExportTaskCoordinator()
        val id = tasks.start("foo.mp4", 720, 5000L)!!
        assertTrue(tasks.rendering(id))
        assertTrue(tasks.tick(id, null, 3000L))
        assertNull(tasks.snapshot.progress)
        assertEquals(0L, tasks.snapshot.elapsedSeconds)
        assertTrue(tasks.tick(id, 250, 10500L))
        assertEquals(100, tasks.snapshot.progress)
        assertEquals(5L, tasks.snapshot.elapsedSeconds)
    }

    @Test fun invalidResolutionOrFileNameAreRejected() {
        val tasks = ExportTaskCoordinator()
        for (resolution in listOf(480, 2160)) {
            try { tasks.start("arquivo.mp4", resolution, 0L); throw AssertionError("deveria falhar") }
            catch (_: IllegalArgumentException) { }
        }
        try { tasks.start(" ", 720, 0L); throw AssertionError("deveria falhar") }
        catch (_: IllegalArgumentException) { }
    }
}
