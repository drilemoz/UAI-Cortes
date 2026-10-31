package br.com.uaicortes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Regressões compartilhadas da exportação de imagens via ExportTaskCoordinator. */
class ImageAnimationTaskFlowTest {
    @Test fun imageStagesHaveNoInventedProgress() {
        val tasks = ExportTaskCoordinator()
        val id = tasks.start("UaiCortes_Animado.mp4", 1080, 1000)!!
        assertTrue(tasks.tick(id, 50, 1200))
        assertNull(tasks.snapshot.progress) // preparação não oferece percentual Media3
        assertTrue(tasks.rendering(id))
        assertTrue(tasks.tick(id, 55, 2000))
        assertEquals(55, tasks.snapshot.progress)
        assertTrue(tasks.saving(id))
        assertNull(tasks.snapshot.progress)
        assertFalse(tasks.cancel(id)) // MediaStore deve finalizar sem interrupção
        assertTrue(tasks.success(id))
    }

    @Test fun cancelledImageTaskCannotFinishNewAttempt() {
        val tasks = ExportTaskCoordinator()
        val oldId = tasks.start("UaiCortes_Animado.mp4",720,0)!!
        assertTrue(tasks.cancel(oldId))
        val newId = tasks.start("UaiCortes_Animado.mp4",1080,1)!!
        assertFalse(tasks.rendering(oldId))
        assertFalse(tasks.saving(oldId))
        assertFalse(tasks.success(oldId))
        assertFalse(tasks.failure(oldId, "erro tardio"))
        assertTrue(tasks.snapshot.working)
        assertEquals(newId,tasks.snapshot.id)
        assertEquals("", tasks.snapshot.message)
    }

    @Test fun imageStageLabelsReuseSharedProgressState() {
        assertEquals("Preparando imagens", AnimationProgressUi.title(ExportPhase.PREPARING))
        assertEquals("Uai Cortes · Exportando vídeo", AnimationProgressUi.title(ExportPhase.RENDERING))
        assertEquals("Salvando na galeria", AnimationProgressUi.title(ExportPhase.SAVING))
        assertNull(AnimationProgressUi.fraction(null))
    }
}
