package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test
import org.json.JSONObject

class PipOpacityPersistenceTest {
    @Test fun legacyPipWithoutOpacityRemainsOpaque() {
        val legacy = JSONObject().put("uri", "content://media/pip/1")
            .put("isImage", true).put("durationMs", 3000L)
        assertEquals(1f, Overlay.read(legacy).opacity, 0f)
    }
    @Test fun alphaSurvivesProjectJsonRoundTrip() {
        val pip = Overlay(uri="content://media/pip/1",isImage=true,durationMs=3000L,
            opacity=.35f)
        val loaded = Overlay.read(pip.json())
        assertEquals(.35f,loaded.opacity,.0001f)
        assertEquals(pip.id,loaded.id)
        assertEquals(pip.durationMs,loaded.durationMs)
    }
    @Test fun invalidOpacityIsClamped() {
        val raw=Overlay(uri="x",isImage=true,durationMs=200L,opacity=2f).json()
        assertEquals(1f,Overlay.read(raw).opacity,0f)
        assertEquals(0f,Overlay.read(raw.put("opacity",-1.0)).opacity,0f)
    }
}
