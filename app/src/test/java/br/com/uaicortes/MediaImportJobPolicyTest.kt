package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class MediaImportJobPolicyTest {
    @Test fun importCannotReplaceAnUnrelatedProject() {
        val append = VideoImportRequest("proj-1", createNew = false)
        assertTrue(append.stillTargets("proj-1"))
        assertFalse(append.stillTargets("proj-2"))
        assertFalse(append.stillTargets(""))
        val create = VideoImportRequest("", createNew = true)
        assertTrue(create.stillTargets(""))
        assertFalse(create.stillTargets("proj-1"))
    }
}
