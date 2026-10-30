package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class ProjectSessionCoordinatorTest {
    private class FakeRepository : ProjectRepository {
        val data = linkedMapOf<String, VideoProject>()
        var failSave = false
        var failDelete = false
        override fun load() = data.values.toList()
        override fun save(project: VideoProject) {
            if (failSave) error("Storage failed")
            data[project.id] = project
        }
        override fun delete(id: String) {
            if (failDelete) error("Storage failed")
            data.remove(id)
        }
    }

    @Test fun restoreKnownProjectIdLoadsCurrentPersistedObject() {
        val repo = FakeRepository()
        repo.data["p"] = VideoProject(id = "p", title = "Persistido")
        var savedId = ""
        val session = ProjectSessionCoordinator(ProjectController(repo), "p") { savedId = it }
        assertEquals("p", session.snapshot.activeId)
        assertEquals("Persistido", session.snapshot.active?.title)
        assertEquals("p", savedId)
    }

    @Test fun missingRestoredProjectResetsIdWithoutInventingData() {
        var savedId = "old"
        val session = ProjectSessionCoordinator(ProjectController(FakeRepository()), "old") { savedId = it }
        assertEquals("", savedId)
        assertNull(session.snapshot.active)
        assertTrue(session.snapshot.projects.isEmpty())
    }

    @Test fun failedWritesDoNotChangeSessionOrSavedId() {
        val repo = FakeRepository()
        var savedId = ""
        val session = ProjectSessionCoordinator(ProjectController(repo)) { savedId = it }
        val initial = VideoProject(id = "first")
        session.save(initial)
        val before = session.snapshot
        assertEquals("first", savedId)
        repo.failSave = true
        assertThrows(IllegalStateException::class.java) { session.save(VideoProject(id = "second")) }
        assertEquals(before, session.snapshot)
        assertEquals("first", savedId)
        repo.failSave = false
        repo.failDelete = true
        assertThrows(IllegalStateException::class.java) { session.delete("first") }
        assertEquals(before, session.snapshot)
        assertEquals("first", savedId)
    }

    @Test fun deleteActiveClearsIdButKeepsOtherProjects() {
        val repo = FakeRepository()
        var savedId = ""
        val session = ProjectSessionCoordinator(ProjectController(repo)) { savedId = it }
        session.save(VideoProject(id = "a"))
        session.save(VideoProject(id = "b"))
        assertEquals("b", savedId)
        session.delete("b")
        assertEquals("", savedId)
        assertNull(session.snapshot.active)
        assertEquals(listOf("a"), session.snapshot.projects.map { it.id })
    }

    @Test fun openSameIdReloadsLatestFromStorage() {
        val repo = FakeRepository()
        repo.data["a"] = VideoProject(id = "a", title = "Anterior")
        val session = ProjectSessionCoordinator(ProjectController(repo), "a")
        repo.data["a"] = VideoProject(id = "a", title = "Atual")
        assertEquals("Atual", session.open("a").active?.title)
    }
}
