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
        val original=VideoProject(title="Meu vídeo",clips=listOf(clip),pip=Overlay("content://example/img",true,2000),
            music=Music("content://example/audio","Som",0.25f))
        val restored=VideoProject.read(original.json())
        assertEquals(original.id,restored.id)
        assertEquals(3000L,restored.durationMs)
        assertEquals(2000L,restored.pip?.durationMs)
        assertEquals(0.25f,restored.music!!.volume,0.0001f)
    }
    @Test fun formatTime() {
        assertEquals("01:05",formatMs(65000))
        assertEquals("00:00",formatMs(-1))
    }
}
