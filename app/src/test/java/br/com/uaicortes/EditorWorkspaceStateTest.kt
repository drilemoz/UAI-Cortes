package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class EditorWorkspaceStateTest {
    private val project = VideoProject(clips = listOf(
        Clip(id = "clip-1", uri = "content://teste/1", title = "A", sourceDurationMs = 10000L),
        Clip(id = "clip-2", uri = "content://teste/2", title = "B", sourceDurationMs = 5000L)
    ))

    @Test fun roundTripPreservesOnlySmallNavigationValues() {
        val state = EditorWorkspaceState().apply {
            screen = "subtitles"; previous = "editor"; selectedClip = "clip-2"
            selectedAudioTrackId = "audio-1"; selectedSubtitleId = "legenda-1"
            selectedOverlayId = "pip-1"; selectedBoundary = 1; positionMs = 6000L
        }
        val tokens = state.snapshotTokens()
        assertEquals(9, tokens.size)
        assertFalse(tokens.any { it.contains("content://") || it.contains("schemaVersion") })
        val restored = EditorWorkspaceState.fromTokens(tokens)
        assertEquals("subtitles", restored.screen)
        assertEquals("clip-2", restored.selectedClip)
        assertEquals("audio-1", restored.selectedAudioTrackId)
        assertEquals(1, restored.selectedBoundary)
        assertEquals(6000L, restored.positionMs)
    }

    @Test fun brokenOrUnknownSnapshotFallsBackSafely() {
        assertEquals("home", EditorWorkspaceState.fromTokens(listOf("invalid")).screen)
        val malformed = listOf("1", "unknown-route", "unknown", "missing", "", "", "", "-400", "-5")
        val restored = EditorWorkspaceState.fromTokens(malformed)
        assertEquals("home", restored.screen)
        assertEquals("home", restored.previous)
        assertEquals(0, restored.selectedBoundary)
        assertEquals(0L, restored.positionMs)
        assertNull(restored.selectedSubtitleId)
    }

    @Test fun missingProjectClearsSelectionsAndReturnsHome() {
        val restored = EditorWorkspaceState.fromTokens(
            listOf("1", "pip", "editor", "clip-1", "audio", "subtitle", "overlay", "2", "999")
        )
        restored.reconcile(null)
        assertEquals("home", restored.screen)
        assertEquals("", restored.selectedClip)
        assertNull(restored.selectedAudioTrackId)
        assertEquals(0L, restored.positionMs)
        assertEquals(0, restored.selectedBoundary)
    }

    @Test fun boundaryIsClampedWhenSameProjectLosesClips() {
        val threeClips = project.copy(clips = project.clips + Clip(
            id = "clip-3", uri = "content://teste/3", title = "C", sourceDurationMs = 3000L))
        val state = EditorWorkspaceState.fromTokens(listOf(
            "1", "transition", "editor", "clip-3", "", "", "", "999", "18000"))
        state.reconcile(threeClips)
        assertEquals(1, state.selectedBoundary) // três clipes: índices 0 e 1
        state.reconcile(project) // mesmo ID, mas clipe removido
        assertEquals(0, state.selectedBoundary)
        assertEquals("clip-1", state.selectedClip)
        assertEquals(15000L, state.positionMs)
    }

    @Test fun deletedSelectionsAreRevalidatedAgainstPersistedProject() {
        val restored = EditorWorkspaceState.fromTokens(
            listOf("1", "editor", "home", "inexistente", "audio", "legenda", "pip", "999", "99999")
        )
        restored.reconcile(project)
        assertEquals("editor", restored.screen)
        assertEquals("clip-1", restored.selectedClip)
        assertNull(restored.selectedAudioTrackId)
        assertNull(restored.selectedSubtitleId)
        assertEquals("", restored.selectedOverlayId)
        assertEquals(0, restored.selectedBoundary) // duas mídias possuem uma única junção: índice 0
        assertEquals(15000L, restored.positionMs)
    }
}
