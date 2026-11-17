package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test
import org.json.JSONArray
import org.json.JSONObject

class LegacySceneProjectWatermarkTest {
    @Test fun savedVideoProjectsKeepClipsAndOpaqueFormerSceneData() {
        val clip = Clip(uri = "content://saved/17", title = "Vídeo pronto", sourceDurationMs = 1000L)
        val project = VideoProject(id = "p1", clips = listOf(clip))
        val scenePayload = JSONObject().put("context", "Contexto antigo")
            .put("scenes", JSONArray().put("Cena 1")).put("unknownField", 7)
        val json = project.json().put("flowPromptDraft", scenePayload)
        val restored = VideoProject.read(json)
        val saved = restored.copy(title = "Nova edição").json()
        assertEquals(4, saved.getInt("schemaVersion"))
        assertEquals(project.clips, restored.clips)
        assertEquals(7, saved.getJSONObject("flowPromptDraft").getInt("unknownField"))
        assertEquals("Cena 1", saved.getJSONObject("flowPromptDraft").getJSONArray("scenes").getString(0))
        assertNull(VideoProject.read(project.json()).legacySceneDraftJson)
    }

    @Test fun oldWatermarkDefaultsAndNewValuesRoundtrip() {
        val old = TextWatermark.read(JSONObject().put("text", "Marca antiga"))
        assertEquals(0, old.outlineWidth)
        assertEquals(0, old.shadowOffsetDp)
        val modern = old.copy(fontFamily = SubtitleStyles.FONT_MONO, opacity = 0f,
            outlineWidth = 3, shadowOffsetDp = 2)
        assertEquals(modern, TextWatermark.read(modern.json()))
        assertEquals(0, modern.toSubtitle(1000)!!.color ushr 24)
    }
}
