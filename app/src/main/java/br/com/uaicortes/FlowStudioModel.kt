package br.com.uaicortes

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** Modelo independente do ProjectStore/schemaVersion=4 e do FlowPromptDraft legado. */
internal data class StudioPerson(
    val name: String = "", val appearance: String = "", val clothes: String = "",
    val voice: String = "", val personality: String = "", val objects: String = ""
) {
    fun json() = JSONObject().put("name", name).put("appearance", appearance)
        .put("clothes", clothes).put("voice", voice).put("personality", personality).put("objects", objects)
    companion object {
        fun read(j: JSONObject) = StudioPerson(j.optString("name").take(120), j.optString("appearance").take(1500),
            j.optString("clothes").take(1000), j.optString("voice").take(500),
            j.optString("personality").take(800), j.optString("objects").take(800))
    }
}

internal data class StudioPlace(val name: String = "", val details: String = "", val objects: String = "") {
    fun json() = JSONObject().put("name", name).put("details", details).put("objects", objects)
    companion object {
        fun read(j: JSONObject) = StudioPlace(j.optString("name").take(120),
            j.optString("details").take(1800), j.optString("objects").take(900))
    }
}

internal data class StudioStory(
    val idea: String = "", val originalScript: String = "", val durationSeconds: Int = 60,
    val style: String = "Animação 3D", val genre: String = "Humor",
    val title: String = "", val synopsis: String = "", val beginning: String = "",
    val development: String = "", val twist: String = "", val ending: String = "",
    val approved: Boolean = false
) {
    fun json() = JSONObject().put("idea", idea).put("originalScript", originalScript)
        .put("durationSeconds", durationSeconds).put("style", style).put("genre", genre)
        .put("title", title).put("synopsis", synopsis).put("beginning", beginning)
        .put("development", development).put("twist", twist).put("ending", ending)
        .put("approved", approved)
    companion object {
        fun read(j: JSONObject) = StudioStory(
            j.optString("idea").take(5000), j.optString("originalScript").take(25000),
            j.optInt("durationSeconds", 60).coerceIn(5, 900), j.optString("style", "Animação 3D").take(120),
            j.optString("genre", "Humor").take(120), j.optString("title").take(250),
            j.optString("synopsis").take(6000), j.optString("beginning").take(6000),
            j.optString("development").take(10000), j.optString("twist").take(6000),
            j.optString("ending").take(6000), j.optBoolean("approved", false))
    }
    fun ready() = title.isNotBlank() && synopsis.isNotBlank() && beginning.isNotBlank() && ending.isNotBlank()
    fun fullText() = buildString {
        appendLine("Título: $title")
        appendLine("Sinopse: $synopsis")
        appendLine("Início: $beginning")
        appendLine("Desenvolvimento: $development")
        appendLine("Reviravolta: $twist")
        appendLine("Final: $ending")
    }
}

/** Falas atribuídas a cada personagem; campo opcional compatível com rascunhos v1. */
internal data class StudioDialogueLine(val speaker: String = "", val speech: String = "") {
    fun json() = JSONObject().put("speaker", speaker).put("speech", speech)
    companion object {
        fun read(j: JSONObject) = StudioDialogueLine(
            j.optString("speaker").take(120), j.optString("speech").take(800))
    }
}

/** Conversão explícita de falas legadas; não trunca conteúdo silenciosamente. */
internal object StudioDialoguePolicy {
    fun canConvert(text: String): Boolean {
        val lines = text.lines().filter { it.isNotBlank() }
        return lines.size < 20 && lines.all { line ->
            val separator = line.indexOf(':')
            if (separator in 1..120) line.substring(separator + 1).trim().length <= 800
            else line.trim().length <= 800
        }
    }
    fun fromLegacy(text: String): List<StudioDialogueLine> {
        require(canConvert(text)) { "Falas antigas muito longas. Edite em texto livre antes de separar." }
        return text.lines().filter { it.isNotBlank() }.map { line ->
            val separator = line.indexOf(':')
            if (separator in 1..120) StudioDialogueLine(line.substring(0, separator).trim(),
                line.substring(separator + 1).trim())
            else StudioDialogueLine("Personagem", line.trim())
        }
    }
}

/** Planejamento para cartões/roteiro: não altera a duração configurada no Google Flow. */
internal object StudioSceneTiming {
    fun distribute(targetSeconds: Int, count: Int): List<Int> {
        if (count <= 0) return emptyList()
        val seconds = targetSeconds.coerceIn(count, count * 60)
        val base = seconds / count
        val extra = seconds % count
        return List(count) { i -> base + if (i < extra) 1 else 0 }
    }
    fun total(scenes: List<StudioScene>): Int = scenes.sumOf { it.durationSeconds }
}

internal data class StudioScene(
    val id: String = UUID.randomUUID().toString(), val title: String = "Nova cena",
    val visual: String = "", val characters: String = "", val actions: String = "",
    val dialogue: String = "", val effects: String = "", val camera: String = "",
    val initialState: String = "", val finalState: String = "", val prompt: String = "",
    val imageAsset: String = "", val videoUri: String = "", val videoName: String = "",
    val durationSeconds: Int = 8, val dialogueLines: List<StudioDialogueLine> = emptyList()
) {
    fun json(includeLocalUri: Boolean = true) = JSONObject().put("id", id).put("title", title)
        .put("visual", visual).put("characters", characters).put("actions", actions)
        .put("dialogue", dialogue).put("effects", effects).put("camera", camera)
        .put("initialState", initialState).put("finalState", finalState).put("prompt", prompt)
        .put("imageAsset", imageAsset).put("videoUri", if (includeLocalUri) videoUri else "")
        .put("videoName", videoName).put("durationSeconds", durationSeconds)
        .put("dialogueLines", JSONArray().also { a -> dialogueLines.take(20).forEach { a.put(it.json()) } })
    fun effectiveDialogue() = if (dialogueLines.isNotEmpty())
        dialogueLines.filter { it.speech.isNotBlank() }.joinToString("\n") {
            "${it.speaker.ifBlank { "Personagem" }}: ${it.speech}"
        } else dialogue
    fun ready() = title.isNotBlank() && prompt.isNotBlank() && imageAsset.isNotBlank()
    fun status() = when {
        videoUri.isNotBlank() -> "Vídeo importado"
        ready() -> "Pronta"
        else -> "Não preparada"
    }
    companion object {
        fun read(j: JSONObject) = StudioScene(
            j.optString("id").takeIf { it.matches(Regex("[a-fA-F0-9-]{36}")) } ?: UUID.randomUUID().toString(),
            j.optString("title", "Nova cena").take(200), j.optString("visual").take(4000),
            j.optString("characters").take(2000), j.optString("actions").take(6000),
            j.optString("dialogue").take(4000), j.optString("effects").take(3000),
            j.optString("camera").take(3000), j.optString("initialState").take(4000),
            j.optString("finalState").take(4000), j.optString("prompt").take(10000),
            j.optString("imageAsset").takeIf { it.matches(Regex("[a-fA-F0-9-]{36}\\.(png|jpg|webp)")) }.orEmpty(),
            j.optString("videoUri").take(2048), j.optString("videoName").take(200),
            j.optInt("durationSeconds", 8).coerceIn(1, 60),
            (j.optJSONArray("dialogueLines") ?: JSONArray()).let { array ->
                (0 until minOf(array.length(), 20)).mapNotNull { i ->
                    array.optJSONObject(i)?.let { StudioDialogueLine.read(it) }
                }
            })
    }
}

internal data class FlowStudioDraft(
    val story: StudioStory = StudioStory(), val people: List<StudioPerson> = emptyList(),
    val places: List<StudioPlace> = emptyList(), val scenes: List<StudioScene> = emptyList(),
    val step: Int = 0
) {
    fun json(includeLocalUri: Boolean = true) = JSONObject().put("schema", 1)
        .put("story", story.json())
        .put("people", JSONArray().also { a -> people.forEach { a.put(it.json()) } })
        .put("places", JSONArray().also { a -> places.forEach { a.put(it.json()) } })
        .put("scenes", JSONArray().also { a -> scenes.forEach { a.put(it.json(includeLocalUri)) } })
        .put("step", step.coerceIn(0, 2))
    companion object {
        fun read(j: JSONObject): FlowStudioDraft {
            require(j.optInt("schema") == 1) { "Formato do Estúdio não reconhecido." }
            val persons = j.optJSONArray("people") ?: JSONArray()
            val locations = j.optJSONArray("places") ?: JSONArray()
            val scenes = j.optJSONArray("scenes") ?: JSONArray()
            require(persons.length() <= 30 && locations.length() <= 30 && scenes.length() <= 60) {
                "O Estúdio salvo excede os limites de segurança."
            }
            val result = FlowStudioDraft(
                StudioStory.read(j.optJSONObject("story") ?: JSONObject()),
                (0 until persons.length()).map { StudioPerson.read(persons.getJSONObject(it)) },
                (0 until locations.length()).map { StudioPlace.read(locations.getJSONObject(it)) },
                (0 until scenes.length()).map { StudioScene.read(scenes.getJSONObject(it)) },
                j.optInt("step").coerceIn(0, 2))
            require(result.scenes.map { it.id }.distinct().size == result.scenes.size) { "Cenas com identificadores duplicados." }
            return result
        }
    }
}

internal object StudioPromptPolicy {
    fun referenceText(draft: FlowStudioDraft) = buildString {
        draft.people.forEach { p -> appendLine("Character ${p.name}: physical=${p.appearance}; clothes=${p.clothes}; voice=${p.voice}; personality=${p.personality}; props=${p.objects}") }
        draft.places.forEach { p -> appendLine("Location ${p.name}: ${p.details}. Objects: ${p.objects}") }
    }.take(12000)

    /** Não insere duração/proporção no prompt, mesmo em cenas manuais. */
    fun compose(scene: StudioScene, draft: FlowStudioDraft, previous: StudioScene?): String = buildString {
        appendLine("Create one cinematic scene. Visual style: ${draft.story.style}. Genre: ${draft.story.genre}.")
        appendLine("Use the actual supplied reference image as the source of truth. Do not contradict its visible characters, clothing, props, setting or composition.")
        appendLine("Scene: ${scene.title}. Visual: ${scene.visual}")
        appendLine("Characters: ${scene.characters}. Actions in order: ${scene.actions}")
        appendLine("Exact speech in Brazilian Portuguese: ${scene.effectiveDialogue()}")
        appendLine("Camera: ${scene.camera}. Synchronized sound effects: ${scene.effects}")
        appendLine("Opening state: ${scene.initialState}. Closing state: ${scene.finalState}")
        previous?.let { appendLine("Previous closing state: ${it.finalState}") }
        appendLine("Keep all dialogue and on-screen text in Brazilian Portuguese. No logo, watermark or signature.")
    }.trim().take(10000)
}
