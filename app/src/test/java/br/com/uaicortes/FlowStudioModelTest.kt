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
    @Test fun sceneDurationAndSpeakerLinesRoundTripWithoutBreakingOldDrafts() {
        val scene = StudioScene(title = "Festa", durationSeconds = 9,
            dialogue = "Ana: Uai!", dialogueLines = listOf(StudioDialogueLine("Ana", "Uai!"),
                StudioDialogueLine("João", "E agora?")))
        val restored = FlowStudioDraft.read(FlowStudioDraft(scenes = listOf(scene)).json())
        assertEquals(9, restored.scenes.first().durationSeconds)
        assertEquals(2, restored.scenes.first().dialogueLines.size)
        assertTrue(restored.scenes.first().effectiveDialogue().contains("João: E agora?"))
        // Rascunhos da versão anterior não possuíam os novos campos.
        val legacy = scene.json().apply { remove("durationSeconds"); remove("dialogueLines") }
        assertEquals(8, StudioScene.read(legacy).durationSeconds)
        assertEquals("Ana: Uai!", StudioScene.read(legacy).effectiveDialogue())
    }

    @Test fun legacyDialogueCanBeSeparatedWithoutDroppingSpeaker() {
        val lines = StudioDialoguePolicy.fromLegacy("Ana: Uai!\nJoão: E agora?\nSem nome")
        assertEquals(3, lines.size)
        assertEquals("João", lines[1].speaker)
        assertEquals("Sem nome", lines[2].speech)
    }

    @Test fun wizardMigrationPreservesOldProjectsAndStep() {
        val legacy = FlowStudioDraft(story = StudioStory(style = "Realista"),
            people = listOf(StudioPerson("Alien", "verde")), scenes = listOf(StudioScene(actions = "Olha")))
            .json().apply { remove("wizardVersion"); put("step", 1) }
        val restored = FlowStudioDraft.read(legacy)
        assertEquals(3, restored.step) // Cenas antigas -> Cenas do Wizard
        assertEquals("Realista", restored.story.style)
        assertEquals("Alien", restored.people.first().name)
        assertEquals("Olha", restored.scenes.first().actions)
        assertEquals(4, FlowStudioDraft.read(legacy.put("step", 2)).step)
        assertEquals(0, FlowStudioDraft.read(legacy.put("step", 0)).step)
    }

    @Test fun wizardImagesAndReusableDetailsRoundTrip() {
        val picture = "550e8400-e29b-41d4-a716-446655440000.png"
        val person = StudioPerson(name = "Abacate", appearance = "olhos grandes", imageAsset = picture)
        val place = StudioPlace(name = "Cozinha", imageAsset = picture)
        val draft = FlowStudioDraft(people = listOf(person), places = listOf(place), step = 4,
            scenes = listOf(StudioScene(prompt = "Camera focuses on the avocado", imageAsset = "")))
        val restored = FlowStudioDraft.read(draft.json())
        assertEquals(draft, restored)
        assertEquals(picture, restored.people.first().imageAsset)
        assertEquals(picture, restored.places.first().imageAsset)
        assertEquals("Prompt pronto · imagem opcional", restored.scenes.first().status())
    }

    @Test fun localSuggestionsAndEnrichmentNeverDiscardReferenceIdentity() {
        val alien = StudioReferenceSuggestions.person("👽 Alienígena")
        assertTrue(alien.appearance.isNotBlank())
        assertTrue(StudioReferenceSuggestions.place("🍳 Cozinha").details.isNotBlank())
        val enhanced = FlowStudioAi.parsePerson(JSONObject().put("name", "Nome errado")
            .put("appearance", "Ser de quatro olhos").put("clothes", "Macacão verde"), alien)
        assertEquals(alien.id, enhanced.id)
        assertEquals(alien.name, enhanced.name)
        assertEquals("Ser de quatro olhos", enhanced.appearance)
        assertFalse(FlowStudioAi.enrichPerson(alien, "Animação 3D").contains("9:16"))
    }

    @Test fun manualClipDoesNotRequireStoryApprovalOrGemini() {
        val idea = StudioStory(idea = "Abacate e banana brigando", approved = false)
        val scene1 = StudioScene(title = "Encontro", actions = "Os dois se olham em silêncio", finalState = "Os dois se olham")
        val scene2 = StudioScene(title = "O abacate fala", actions = "Abacate gesticula",
            camera = "close no abacate", dialogueLines = listOf(
                StudioDialogueLine("Abacate", "Não gosto de vitamina com banana!")))
        val draft = FlowStudioDraft(story = idea, scenes = listOf(scene1, scene2))
        assertTrue(FlowStudioClipPolicy.canDivide(idea))
        assertFalse(idea.ready())
        assertTrue(FlowStudioClipPolicy.canPrepare(scene1))
        val output = FlowStudioClipPolicy.prepare(scene2, draft, 1)
        assertEquals(scene2.id, output.id)
        assertTrue(output.prompt.contains("Não gosto de vitamina com banana!"))
        assertTrue(output.prompt.contains("close no abacate"))
        assertTrue(output.prompt.contains("Os dois se olham"))
        assertFalse(output.prompt.contains("9:16"))
        assertEquals("", scene2.prompt) // Original nunca alterado implicitamente
    }

    @Test fun incompleteSpeakerDoesNotCreateFakeSpeechOrRequireApi() {
        val empty = StudioScene(dialogueLines = listOf(StudioDialogueLine("Banana", "")))
        assertEquals("", empty.effectiveDialogue())
        assertFalse(FlowStudioClipPolicy.canPrepare(empty))
        assertFalse(FlowStudioClipPolicy.canDivide(StudioStory()))
    }

    @Test fun sceneTimeBudgetMatchesStoryWithoutPuttingDurationInPrompts() {
        assertEquals(listOf(9, 9, 9, 8, 8, 8, 8, 8, 8), StudioSceneTiming.distribute(75, 9))
        assertEquals(75, StudioSceneTiming.distribute(75, 9).sum())
        val json = JSONObject().put("scenes", JSONArray().apply {
            repeat(9) { put(JSONObject().put("title", "Cena $it")
                .put("actions", "Movimento $it")
                .put("dialogueLines", JSONArray().put(JSONObject().put("speaker", "Ana").put("speech", "Oi")))) }
        })
        val result = FlowStudioAi.parseScenes(json, 75)
        assertEquals(75, StudioSceneTiming.total(result))
        assertEquals("Ana", result.first().dialogueLines.first().speaker)
        assertFalse(StudioPromptPolicy.compose(result.first(), FlowStudioDraft(), null).contains("9:16"))
    }
}
