package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class ProjectStoreTest {
    @Test fun clipDuration() {
        val clip=Clip(uri="content://example/video",title="Teste",sourceDurationMs=10000,startMs=2000,endMs=7000)
        assertEquals(5000L,clip.durationMs)
        assertEquals(5000L,Clip.read(clip.json()).durationMs)
    }
    @Test fun projectJsonRoundTrip() {
        val clip=Clip(uri="content://example/video",title="Teste",sourceDurationMs=3000)
        val original=VideoProject(title="Meu vídeo",clips=listOf(clip),overlays=listOf(Overlay("content://example/img",true,2000)),
            music=Music("content://example/audio","Som",0.25f))
        val restored=VideoProject.read(original.json())
        assertEquals(original.id,restored.id)
        assertEquals(3000L,restored.durationMs)
        assertEquals(2000L,restored.pip?.durationMs)
        assertEquals(0.25f,restored.music!!.volume,0.0001f)
    }
    @Test fun mutedOverlaySurvivesProjectSerialization() {
        val overlay = Overlay("content://example/video", false, 1800,
            volume = .72f, muted = true)
        val project = VideoProject(overlays = listOf(overlay))
        val restored = VideoProject.read(project.json())
        assertEquals(true, restored.pip?.muted)
        assertEquals(.72f, restored.pip!!.volume, .0001f)
        val legacy = overlay.json().apply { remove("muted") }
        assertEquals(false, Overlay.read(legacy).muted)
    }
    @Test fun legacyProjectMigratesToOverlays() {
        val single = Overlay("content://old", true, 1000)
        val legacy = VideoProject().json().apply {
            remove("overlays")
            put("pip", single.json().apply { remove("id") })
        }
        val restored = VideoProject.read(legacy)
        assertEquals(1, restored.overlays.size)
        assertEquals("content://old", restored.overlays[0].uri)
        assertTrue(restored.overlays[0].id.isNotBlank())
    }
    @Test fun fourOverlaysRoundTrip() {
        val layers = (1..4).map { Overlay("content://pip/$it", true, 1000L, startAtMs = it * 200L) }
        val restored = VideoProject.read(VideoProject(overlays = layers).json())
        assertEquals(4, restored.overlays.size)
        assertEquals(layers.map { it.id }, restored.overlays.map { it.id })
    }
    @Test fun formatTime() {
        assertEquals("01:05",formatMs(65000))
        assertEquals("00:00",formatMs(-1))
    }
    @Test fun approvedOverlapsPersistByPair() {
        val a = Overlay("content://first", true, 1200L)
        val b = Overlay("content://second", true, 1200L, overlapWith = listOf(a.id))
        val stored = VideoProject(overlays = listOf(a,b)).json()
        assertEquals(4, stored.getInt("schemaVersion"))
        val restored = VideoProject.read(stored)
        assertEquals(listOf(a.id), restored.overlays[1].overlapWith)
        assertTrue(restored.overlays[0].overlapWith.isEmpty())
        val legacy = a.json().apply { remove("overlapWith") }
        assertTrue(Overlay.read(legacy).overlapWith.isEmpty())
    }
    @Test fun audioTrackSourceOffsetRoundTripAndLegacy() {
        val audio = AudioTrack("content://example/audio", "Trilha", startMs=2300,
            durationMs=1100, sourceStartMs=900)
        val restored = VideoProject(audioTracks=listOf(audio)).json().let(VideoProject::read)
        assertEquals(900L, restored.audioTracks.single().sourceStartMs)
        assertEquals(2300L, restored.audioTracks.single().startMs)
        assertEquals(1100L, restored.audioTracks.single().durationMs)
        val legacy = audio.json().apply { remove("sourceStartMs") }
        assertEquals(0L, AudioTrack.read(legacy).sourceStartMs)
    }

    @Test fun projectCollectionCodecRoundTrip() {
        val one = VideoProject(title = "Original", clips = listOf(Clip(
            uri = "content://video", title = "Teste", sourceDurationMs = 4000)))
        val two = VideoProject(title = "Segundo")
        val json = ProjectCollectionCodec.encode(listOf(one, two))
        val decoded = ProjectCollectionCodec.decode(json)
        assertEquals(setOf(one.id, two.id), decoded.map { it.id }.toSet())
        assertEquals(4, decoded.first { it.id == one.id }.json().getInt("schemaVersion"))
    }

    @Test fun corruptPrimaryUsesValidBackupWithoutClearingProjects() {
        val original = VideoProject(title = "Não perder")
        val backup = ProjectCollectionCodec.encode(listOf(original))
        assertEquals(original.id, ProjectCollectionCodec.recover("{ inválido", backup).single().id)
        try {
            ProjectCollectionCodec.recover("{ inválido", null)
            fail("JSON ilegível sem backup não pode virar lista vazia para escrita")
        } catch (expected: IllegalStateException) { /* escrita bloqueada */ }
        try {
            ProjectCollectionCodec.recover("{ inválido", "{ backup inválido")
            fail("Backup ilegível não pode autorizar escrita")
        } catch (expected: IllegalStateException) { /* escrita bloqueada */ }
    }

}
