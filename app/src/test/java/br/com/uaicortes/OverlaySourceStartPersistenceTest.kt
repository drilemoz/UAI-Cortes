package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test
import org.json.JSONObject

class OverlaySourceStartPersistenceTest {
    @Test fun oldProjectPipDefaultsToFirstFrame() {
        val saved = JSONObject().put("uri", "content://media/pip/old")
            .put("isImage",false).put("durationMs",5000L).put("startAtMs",2000L)
        val restored=Overlay.read(saved)
        assertEquals(0L,restored.sourceStartMs)
        assertEquals(5000L,restored.durationMs)
    }
    @Test fun trimmedVideoPipKeepsSourceOffsetAfterReload() {
        val saved=Overlay(uri="content://media/pip/new",isImage=false,
            durationMs=3500L,startAtMs=4000L,sourceStartMs=1500L)
        val loaded=Overlay.read(saved.json())
        assertEquals(1500L,loaded.sourceStartMs)
        assertEquals(3500L,loaded.durationMs)
        assertEquals(saved.id,loaded.id)
    }
    @Test fun framingSettingIsOptionalInOldProjects() {
        val legacy=JSONObject().put("uri","content://media/pip/legacy")
            .put("isImage",true).put("durationMs",3000L)
        assertEquals(OverlayEditorLogic.FIT,Overlay.read(legacy).fitMode)
        val cropped=Overlay(uri="content://media/pip/new",isImage=true,
            durationMs=3000L,fitMode=OverlayEditorLogic.FILL)
        assertEquals(OverlayEditorLogic.FILL,Overlay.read(cropped.json()).fitMode)
        assertEquals(OverlayEditorLogic.FIT,Overlay.read(cropped.json()
            .put("fitMode","unsupported-value")).fitMode)
    }
    @Test fun sourceOffsetIsNonnegative() {
        val saved=Overlay(uri="content://media/pip/new",isImage=false,
            durationMs=3500L,sourceStartMs=0L).json()
        assertEquals(0L,Overlay.read(saved.put("sourceStartMs",-1000L)).sourceStartMs)
    }
}
