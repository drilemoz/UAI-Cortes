package br.com.uaicortes

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Usa arquivo exclusivo para não tocar no rascunho real do usuário. */
@RunWith(AndroidJUnit4::class)
class ProjectDraftRecoveryAndroidTest {
    private fun freshStore() = ProjectDraftRecoveryStore(
        InstrumentationRegistry.getInstrumentation().targetContext,
        "editor_draft_test_${System.nanoTime()}.json")

    @Test fun newUnsavedDraftIsRecoverableAndDiscardable() {
        val store = freshStore()
        try {
            val unsaved = VideoProject(id = "temp-${System.nanoTime()}", title = "Não salvo")
            store.saveIfCurrent(store.requestVersion(), unsaved, null)
            val recovered = store.load(emptyList())
            assertEquals(unsaved.id, recovered?.project?.id)
            assertNull(recovered?.baselineHash)
        } finally { store.clear() }
        assertFalse(store.hasStoredDraft())
    }

    @Test fun staleProjectOrGenerationCannotReplaceCurrentCatalog() {
        val store = freshStore()
        try {
            val original = VideoProject(id = "existing-${System.nanoTime()}", title = "Original")
            store.saveIfCurrent(store.requestVersion(), original.copy(title = "Editado"), original)
            assertEquals("Editado", store.load(listOf(original))?.project?.title)
            assertNull(store.load(listOf(original.copy(title = "Alterado fora do rascunho"))))
            val staleVersion = store.requestVersion()
            store.clear()
            store.saveIfCurrent(staleVersion, original, original)
            assertFalse(store.hasStoredDraft())
        } finally { store.clear() }
    }
}
