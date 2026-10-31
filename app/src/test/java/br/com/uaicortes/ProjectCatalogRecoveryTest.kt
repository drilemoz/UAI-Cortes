package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class ProjectCatalogRecoveryTest {
    private val saved = VideoProject(id = "saved", title = "Deve continuar salvo")
    private val backup = ProjectCollectionCodec.encode(listOf(saved))

    @Test fun firstLaunchIsTheOnlyCaseWhereMissingKeysMeanEmptyCatalog() {
        val fresh = ProjectCollectionCodec.inspect(null, null)
        assertTrue(fresh.projects.isEmpty())
        assertEquals("[]", fresh.lastGoodRaw)
        assertFalse(fresh.usedBackup)
    }

    @Test fun missingPrimaryRecoversBackupInsteadOfPretendingThereAreNoProjects() {
        val result = ProjectCollectionCodec.inspect(null, backup)
        assertTrue(result.usedBackup)
        assertEquals("saved", result.projects.single().id)
        assertEquals(backup, result.lastGoodRaw)
    }

    @Test fun corruptedPrimaryRecoversBackupWithoutOverwritingIt() {
        val result = ProjectCollectionCodec.inspect("{bad", backup)
        assertTrue(result.usedBackup)
        val (lastGood, next) = ProjectCollectionCodec.prepareWrite(
            "{bad", backup, result.projects + VideoProject(id = "new"))
        assertEquals(backup, lastGood)
        assertEquals(setOf("saved", "new"), ProjectCollectionCodec.decode(next).map { it.id }.toSet())
    }

    @Test fun healthyPrimaryTakesPrecedenceOverOlderBackup() {
        val primary = ProjectCollectionCodec.encode(listOf(VideoProject(id = "latest")))
        val result = ProjectCollectionCodec.inspect(primary, backup)
        assertFalse(result.usedBackup)
        assertEquals("latest", result.projects.single().id)
        assertEquals(primary, ProjectCollectionCodec.prepareWrite(primary, backup, listOf(saved)).first)
    }

    @Test fun bothInvalidSourcesMustBlockAnyWrite() {
        assertThrows(IllegalStateException::class.java) {
            ProjectCollectionCodec.inspect("{bad", "not-json")
        }
        assertThrows(IllegalStateException::class.java) {
            ProjectCollectionCodec.prepareWrite("{bad", null, emptyList())
        }
    }

    @Test fun duplicateAndBlankIdsAreNeverAcceptedAsHealthyCatalog() {
        assertThrows(IllegalArgumentException::class.java) {
            ProjectCollectionCodec.encode(listOf(saved, saved.copy(title = "Duplicado")))
        }
        assertThrows(IllegalArgumentException::class.java) {
            ProjectCollectionCodec.encode(listOf(VideoProject(id = "")))
        }
        val duplicates = "[${saved.json()},${saved.json()}]"
        assertThrows(IllegalStateException::class.java) {
            ProjectCollectionCodec.inspect(duplicates, null)
        }
    }
}
