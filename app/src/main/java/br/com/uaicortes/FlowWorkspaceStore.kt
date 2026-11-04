package br.com.uaicortes

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

internal data class FlowWorkspaceEntry(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "Vídeo Flow",
    val draft: FlowPromptDraft = FlowPromptDraft(),
    val updated: Long = System.currentTimeMillis()
) {
    fun json() = JSONObject().put("id", id).put("title", title.take(80))
        .put("draft", draft.json()).put("updated", updated)
    companion object {
        fun read(j: JSONObject) = FlowWorkspaceEntry(j.getString("id"),
            j.optString("title", "Vídeo Flow").take(80),
            FlowPromptDraft.read(j.optJSONObject("draft")) ?: FlowPromptDraft(),
            j.optLong("updated"))
    }
}

/** Catálogo separado de vídeos: útil para preparar cenas antes de gerar no Flow. */
internal class FlowWorkspaceStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("flow_projects_v1", Context.MODE_PRIVATE)
    fun read(): List<FlowWorkspaceEntry> {
        val raw = prefs.getString("catalog", null) ?: return emptyList()
        val arr = JSONArray(raw)
        require(arr.length() <= 30) { "Catálogo Flow acima do limite." }
        return (0 until arr.length()).map { FlowWorkspaceEntry.read(arr.getJSONObject(it)) }.also {
            require(it.map(FlowWorkspaceEntry::id).distinct().size == it.size) { "IDs duplicados." }
        }.sortedByDescending { it.updated }
    }
    fun write(list: List<FlowWorkspaceEntry>): Boolean {
        require(list.size <= 30 && list.map(FlowWorkspaceEntry::id).distinct().size == list.size)
        read() // Não sobrescrever conteúdo anterior corrompido.
        val raw = JSONArray().also { arr -> list.forEach { arr.put(it.json()) } }.toString()
        return prefs.edit().putString("catalog", raw).commit()
    }
}
