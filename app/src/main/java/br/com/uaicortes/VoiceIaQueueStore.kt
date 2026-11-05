package br.com.uaicortes

import android.content.Context
import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException

/** Manifesto local atômico para uma fila interativa. Não persiste conteúdo falado nem credenciais. */
internal class VoiceIaQueueStore(context: Context, projectId: String) {
    private val folder = File(context.applicationContext.filesDir, "voice_ia_queue")
    private val file = AtomicFile(File(folder, "${VoiceIaPolicy.digest(projectId)}.json"))
    private val owner = projectId

    fun load(): VoiceIaQueueSnapshot? {
        if (!file.baseFile.exists()) return null
        if (file.baseFile.length() !in 2L..1_048_576L) throw IOException("Fila excede limite permitido")
        val root = JSONObject(file.openRead().bufferedReader(Charsets.UTF_8).use { it.readText() })
        if (root.optInt("schema") != 1 || root.optString("projectId") != owner)
            throw IOException("Fila pertence a outro projeto ou versão incompatível")
        val rows = root.optJSONArray("items") ?: throw IOException("Fila sem itens")
        if (rows.length() !in 1..VoiceIaQueuePolicy.MAX_ITEMS) throw IOException("Quantidade inválida na fila")
        val items = (0 until rows.length()).map { index ->
            val row = rows.getJSONObject(index)
            val item = VoiceIaQueueItem(
                row.getString("cueId"), row.getString("fingerprint"), row.getString("voiceId"),
                row.getString("model"), row.getString("style"), row.getString("requestStyle"),
                VoiceIaQueueStage.valueOf(row.getString("stage")))
            if (!VoiceIaQueuePolicy.valid(item)) throw IOException("Parâmetros inválidos na fila")
            item
        }
        if (items.map { it.cueId }.toSet().size != items.size) throw IOException("Falas repetidas na fila")
        return VoiceIaQueueSnapshot(owner, items)
    }

    fun save(snapshot: VoiceIaQueueSnapshot) {
        require(snapshot.projectId == owner && snapshot.items.size in 1..VoiceIaQueuePolicy.MAX_ITEMS)
        require(snapshot.items.all(VoiceIaQueuePolicy::valid))
        if (!folder.exists() && !folder.mkdirs()) throw IOException("Não foi possível criar pasta da fila")
        val json = JSONObject().put("schema", 1).put("projectId", owner)
            .put("items", JSONArray().apply {
                snapshot.items.forEach { item -> put(JSONObject()
                    .put("cueId", item.cueId).put("fingerprint", item.cueFingerprint)
                    .put("voiceId", item.voiceId).put("model", item.model)
                    .put("style", item.style).put("requestStyle", item.requestStyle)
                    .put("stage", item.stage.name)) }
            }).toString().toByteArray(Charsets.UTF_8)
        require(json.size <= 1_048_576) { "Fila excede 1 MiB" }
        val output = file.startWrite()
        try {
            output.write(json)
            file.finishWrite(output)
        } catch (error: Exception) {
            file.failWrite(output)
            throw error
        }
    }

    fun clear() = file.delete()
}
