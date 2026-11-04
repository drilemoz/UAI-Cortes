package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test
import org.json.JSONObject

class FlowPromptWatermarkTest {
    @Test fun oneStyleAndSeparateScenes() {
        val draft = FlowPromptDraft("Dois jovens no quarto", listOf("Um joga", "Outro acorda"))
        val prompt = FlowPromptPolicy.compose("3D padrão", draft, 1)
        assertTrue(prompt.startsWith("3D padrão"))
        assertTrue(prompt.contains("Cena 2: Outro acorda"))
        assertFalse(prompt.contains("Um joga"))
        assertFalse(prompt.contains("9:16"))
    }
    @Test fun onlyReferenceModePreservesOriginalActions() {
        val d = FlowPromptDraft(scenes = listOf("Cena"), mode = "transform")
        assertTrue(FlowPromptPolicy.compose("3D", d, 0).contains("Vídeo de referência"))
        assertFalse(FlowPromptPolicy.compose("3D", d.copy(mode = "create"), 0)
            .contains("preservar movimentos"))
    }
    @Test fun optionalProjectDraftKeepsSchemaAndLegacy() {
        val draft = FlowPromptDraft("Mesmo quarto", listOf("Uma ação", "Outra ação"))
        val project = VideoProject(id = "p1", flowPromptDraft = draft)
        assertEquals(4, project.json().getInt("schemaVersion"))
        assertEquals(draft, VideoProject.read(JSONObject(project.json().toString())).flowPromptDraft)
        assertNull(VideoProject.read(project.json().apply { remove("flowPromptDraft") }).flowPromptDraft)
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
    @Test fun importedFieldsAreBounded() {
        val draft = FlowPromptDraft("x".repeat(1500), List(20) { "y".repeat(2000) }, "invalid").normalized()
        assertEquals(1200, draft.context.length)
        assertEquals(12, draft.scenes.size)
        assertEquals(1800, draft.scenes[0].length)
        assertEquals("create", draft.mode)
    }
}
