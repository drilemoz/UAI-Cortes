package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class ProjectDraftSessionTest {
    private fun project() = VideoProject(id = "saved-1", title = "Original",
        clips = listOf(Clip(id = "clip-1", uri = "content://sample/1",
            title = "Vídeo", sourceDurationMs = 10000)))

    @Test fun newProjectNeverPretendsToBeSaved() {
        val draft = ProjectDraftSession()
        draft.begin(project(), null)
        assertTrue(draft.active)
        assertTrue(draft.isNew)
        assertTrue(draft.dirty)
        assertNull(draft.discard())
        assertFalse(draft.active)
    }

    @Test fun savedProjectStartsCleanAndDetectsChanges() {
        val saved = project()
        val draft = ProjectDraftSession()
        draft.begin(saved, saved)
        assertFalse(draft.dirty)
        draft.stage(saved.copy(title = "Alterado"))
        assertTrue(draft.dirty)
        assertEquals(saved, draft.discard())
    }

    @Test fun committedProjectBecomesClean() {
        val saved = project()
        val draft = ProjectDraftSession()
        draft.begin(saved, saved)
        draft.stage(saved.copy(title = "Editado"))
        draft.committed(requireNotNull(draft.current))
        assertFalse(draft.dirty)
    }

    @Test fun timestampOnlyDoesNotMarkDirty() {
        val saved = project()
        val draft = ProjectDraftSession()
        draft.begin(saved, saved)
        draft.stage(saved.copy(updated = saved.updated + 100))
        assertFalse(draft.dirty)
    }

    @Test(expected = IllegalArgumentException::class)
    fun cannotStageDifferentProject() {
        val draft = ProjectDraftSession()
        draft.begin(project(), project())
        draft.stage(project().copy(id = "another"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun cannotCommitDifferentProject() {
        val draft = ProjectDraftSession()
        draft.begin(project(), null)
        draft.committed(project().copy(id = "wrong"))
    }
}
