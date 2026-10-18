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
}
