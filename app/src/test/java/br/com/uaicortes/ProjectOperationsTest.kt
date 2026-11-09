package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class ProjectOperationsTest {
    private class FakeRepository : ProjectRepository {
        val projects = linkedMapOf<String, VideoProject>()
        var failSave = false
        var failDelete = false
        override fun load() = projects.values.toList()
        override fun save(project: VideoProject) {
            if (failSave) error("Sem armazenamento")
            projects[project.id] = project
        }
        override fun delete(id: String) {
            if (failDelete) error("Sem armazenamento")
            projects.remove(id)
        }
    }

    @Test fun saveAndDeleteKeepTheActiveSelectionConsistent() {
        val repo = FakeRepository()
        val controller = ProjectController(repo)
        val first = VideoProject(id="a")
        val second = VideoProject(id="b")
        assertEquals("a", controller.save(first).activeId)
        assertEquals("b", controller.save(second).activeId)
        val afterDeletingOther = controller.delete("b", "a")
        assertEquals("a", afterDeletingOther.activeId)
        assertEquals(listOf("a"), afterDeletingOther.projects.map { it.id })
        val afterDeletingActive = controller.delete("a", "a")
        assertNull(afterDeletingActive.active)
        assertTrue(afterDeletingActive.projects.isEmpty())
    }

    @Test fun failuresDoNotMutateTheRepository() {
        val repo = FakeRepository()
        val controller = ProjectController(repo)
        repo.failSave = true
        assertThrows(IllegalStateException::class.java) { controller.save(VideoProject(id="bad")) }
        assertTrue(repo.projects.isEmpty())
        repo.failSave = false
        controller.save(VideoProject(id="ok"))
        repo.failDelete = true
        assertThrows(IllegalStateException::class.java) { controller.delete("ok", "ok") }
        assertEquals(1, repo.projects.size)
    }

    @Test fun renamePreservesProjectContentAndId() {
        val repo = FakeRepository()
        val first = VideoProject(id = "one", title = "Antigo", updated = 1L,
            clips = listOf(Clip(id="clip", uri="content://video", title="Original", sourceDurationMs=5000)))
        val other = VideoProject(id = "two", title = "Outro")
        repo.save(first)
        repo.save(other)
        val result = ProjectController(repo).rename("one", "  Novo    nome  ", "one")
        assertEquals("Novo nome", result.active?.title)
        assertEquals("one", result.activeId)
        assertEquals(first.clips, result.active?.clips)
        assertEquals(first.id, result.active?.id)
        assertEquals(other, repo.projects["two"])
        assertTrue(result.active!!.updated >= first.updated)
    }

    @Test fun invalidMissingAndFailedRenameKeepOriginalUntouched() {
        val repo = FakeRepository()
        val original = VideoProject(id = "p", title = "Mantido")
        repo.save(original)
        val controller = ProjectController(repo)
        assertThrows(IllegalArgumentException::class.java) { controller.rename("p", "  ", "p") }
        assertThrows(NoSuchElementException::class.java) { controller.rename("missing", "Novo", "p") }
        repo.failSave = true
        assertThrows(IllegalStateException::class.java) { controller.rename("p", "Outro", "p") }
        assertEquals(original, repo.projects["p"])
    }

    @Test fun appendDoesNotModifyThePreviousProject() {
        val controller = ProjectController(FakeRepository())
        val original = VideoProject(id="base", clips=listOf(Clip(id="a", uri="a", title="A", sourceDurationMs=5000)))
        val incoming = Clip(id="b", uri="b", title="B", sourceDurationMs=3000)
        val appended = controller.append(original, listOf(incoming), createNew=false)
        assertEquals(listOf("a", "b"), appended.clips.map { it.id })
        assertEquals(1, original.clips.size)
        val fresh = controller.append(original, listOf(incoming), createNew=true)
        assertEquals(listOf("b"), fresh.clips.map { it.id })
        assertNotEquals(original.id, fresh.id)
    }
}
