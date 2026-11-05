package br.com.uaicortes

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class FlowStudioModelTest {
    @Test fun roundTripPreservesSceneIdsAndHistory() {
        val scene = StudioScene(title = "Cena 1", initialState = "na praça", finalState = "na rua",
            prompt = "Camera follows a bicycle; text in Brazilian Portuguese.",
            videoUri = "content://test/video", videoName = "flow.mp4")
        val original = FlowStudioDraft(story = StudioStory(idea = "Bicicleta", title = "Viagem", synopsis = "Aventura",
            beginning = "Abre", ending = "Fecha", approved = true),
            people = listOf(StudioPerson("Pedro", "cabelo castanho", "blusa azul", "grave", "calmo", "capacete")),
            places = listOf(StudioPlace("Praça", "árvores", "banco")), scenes = listOf(scene), step = 2)
        val restored = FlowStudioDraft.read(original.json())
        assertEquals(original, restored)
        assertEquals("Vídeo importado", restored.scenes.first().status())
    }

    @Test fun exportedMetadataNeverIncludesLocalVideoUri() {
        val draft = FlowStudioDraft(scenes = listOf(StudioScene(videoUri = "content://private/provider/one")))
        val exported = draft.json(includeLocalUri = false).toString()
        assertFalse(exported.contains("content://private"))
        assertEquals("", FlowStudioDraft.read(JSONObject(exported)).scenes.first().videoUri)
    }

    @Test fun rejectsTooManyScenesAndDuplicateIds() {
        val duplicated = StudioScene(title = "mesmo ID")
        val data = FlowStudioDraft(scenes = listOf(duplicated, duplicated)).json()
        assertThrows(IllegalArgumentException::class.java) { FlowStudioDraft.read(data) }
        val large = FlowStudioDraft().json().put("scenes", JSONArray().apply {
            repeat(61) { put(StudioScene().json()) }
        })
        assertThrows(IllegalArgumentException::class.java) { FlowStudioDraft.read(large) }
    }

    @Test fun parsingGeminiStoryAndScenesRequiresStructure() {
        val story = JSONObject().put("title", "Viagem").put("synopsis", "Um personagem na estrada")
            .put("beginning", "Sai de casa").put("development", "Pedala")
            .put("twist", "Encontra uma nave").put("ending", "Chega ao destino")
            .put("characters", JSONArray().put(JSONObject().put("name", "Rui").put("clothes", "camisa verde")))
        val parsed = FlowStudioAi.parseStory(story, StudioStory(idea = "Exemplo"))
        assertEquals("Viagem", parsed.first.title)
        assertEquals("Rui", parsed.second.first.first().name)
        assertFalse(parsed.first.approved)
        val scenes = JSONObject().put("scenes", JSONArray().put(JSONObject()
            .put("title", "Partida").put("actions", "Pedala lentamente")
            .put("dialogue", "Vamos!").put("prompt", "The cyclist leaves home.")))
        assertEquals(1, FlowStudioAi.parseScenes(scenes).size)
        assertThrows(IllegalArgumentException::class.java) {
            FlowStudioAi.parseScenes(JSONObject().put("scenes", JSONArray()))
        }
    }

    @Test fun promptPolicyDoesNotInjectVideoDurationOrAspectRatio() {
        val draft = FlowStudioDraft(story = StudioStory(durationSeconds = 180))
        val prompt = StudioPromptPolicy.compose(StudioScene(title = "Teste"), draft, null)
        assertFalse(prompt.contains("180 seconds"))
        assertFalse(prompt.contains("9:16"))
        assertTrue(prompt.contains("Brazilian Portuguese"))
    }
}
