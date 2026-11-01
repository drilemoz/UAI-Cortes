package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class ProjectBackupPolicyTest {
    @Test fun invalidIntervalsFallBackToThirtySeconds() {
        assertEquals(30, ProjectBackupPolicy.interval(0))
        assertEquals(30, ProjectBackupPolicy.interval(9))
        assertEquals(30, ProjectBackupPolicy.interval(999))
        assertEquals(120, ProjectBackupPolicy.interval(120))
    }
    @Test fun historyBoundIsExplicitAndLimited() {
        assertEquals(5, ProjectBackupPolicy.count(-1))
        assertEquals(3, ProjectBackupPolicy.count(3))
        assertEquals(10, ProjectBackupPolicy.count(10))
    }
    @Test fun hashIsStableAndDoesNotExposeProjectId() {
        val key = ProjectBackupPolicy.hash("projeto privado")
        assertEquals(64, key.length)
        assertTrue(key.matches(Regex("[0-9a-f]{64}")))
        assertEquals(key, ProjectBackupPolicy.hash("projeto privado"))
        assertNotEquals(key, ProjectBackupPolicy.hash("outro projeto"))
    }
}
