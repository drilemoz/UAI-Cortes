package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class ProjectMediaRelinkTest {
    private val missing = "content://old.provider/not-found"
    private val restored = "content://com.android.providers.media.documents/document/video%3A12"
    private fun sample(): VideoProject = VideoProject(
        clips = listOf(Clip(id = "c1", uri = missing, title = "Primeiro",
            sourceDurationMs = 10_000L, startMs = 2_000L, endMs = 8_000L),
            Clip(id = "c2", uri = "content://existing", title = "Segundo", sourceDurationMs = 4_000L)),
        overlays = listOf(Overlay(uri = missing, isImage = false, durationMs = 2_000L,
            startAtMs = 500L, sourceStartMs = 1_000L, id = "pip1")),
        audioTracks = listOf(AudioTrack(uri = missing, title = "Som", id = "a1",
            startMs = 100L, sourceStartMs = 250L, durationMs = 1_000L)),
        music = Music(uri = missing, title = "Trilha"))

    @Test fun entriesRepresentEverySourceAndRequiredTrim() {
        val entries = ProjectMediaRelink.entries(sample())
        assertEquals(5, entries.size)
        assertEquals(8_000L, entries.first { it.id == "c1" }.minimumSourceEndMs)
        assertEquals(3_000L, entries.first { it.kind == "pip" }.minimumSourceEndMs)
        assertEquals(1_250L, entries.first { it.kind == "audio" }.minimumSourceEndMs)
    }

    @Test fun replacingClipDoesNotChangeCutsOrOtherReferences() {
        val base = sample()
        val entry = ProjectMediaRelink.entries(base).first { it.id == "c1" }
        val actual = ProjectMediaRelink.replace(base, entry, restored)
        assertEquals(restored, actual.clips[0].uri)
        assertEquals(2_000L, actual.clips[0].startMs)
        assertEquals(8_000L, actual.clips[0].endMs)
        assertEquals(base.clips[1], actual.clips[1])
        assertEquals(base.overlays, actual.overlays)
        assertEquals(base.audioTracks, actual.audioTracks)
    }

    @Test fun pipAndAudioKeepOriginalPositionsAndOffsets() {
        val base = sample()
        val pip = ProjectMediaRelink.entries(base).first { it.kind == "pip" }
        val audio = ProjectMediaRelink.entries(base).first { it.kind == "audio" }
        val updated = ProjectMediaRelink.replace(ProjectMediaRelink.replace(base, pip, restored), audio, restored)
        assertEquals(1_000L, updated.overlays[0].sourceStartMs)
        assertEquals(500L, updated.overlays[0].startAtMs)
        assertEquals(250L, updated.audioTracks[0].sourceStartMs)
        assertEquals(100L, updated.audioTracks[0].startMs)
        assertEquals(missing, updated.music?.uri)
    }

    @Test fun staleSelectionAndInvalidUriAreRejected() {
        val base = sample()
        val item = ProjectMediaRelink.entries(base).first()
        val changed = ProjectMediaRelink.replace(base, item, restored)
        try { ProjectMediaRelink.replace(changed, item, restored); fail("Deveria rejeitar seleção atrasada") }
        catch (_: IllegalArgumentException) { }
        try { ProjectMediaRelink.replace(base, item, "file:///tmp/unsafe.mp4"); fail("URI inválida") }
        catch (_: IllegalArgumentException) { }
    }
}
