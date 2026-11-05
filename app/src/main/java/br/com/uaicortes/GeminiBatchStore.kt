package br.com.uaicortes

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** Apenas IDs/metadados de jobs; NUNCA salvar prompt, resposta ou chave. Fora do backup Android. */
internal class GeminiBatchStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "gemini_batch_local_v1", Context.MODE_PRIVATE)

    @Synchronized fun list(): List<GeminiBatchRecord> {
        val entries = JSONArray(prefs.getString("records", "[]") ?: "[]")
        return (0 until entries.length()).mapNotNull { i ->
            val o = entries.optJSONObject(i) ?: return@mapNotNull null
            runCatching { GeminiBatchRecord(
                localId = o.getString("id"), profileId = o.getString("profile"),
                projectId = o.optString("project"), model = o.getString("model"),
                count = o.optInt("count"), remoteName = o.optString("remote"),
                state = o.optString("state"), createdAtMs = o.optLong("time"),
                flowProjectId = o.optString("flowProject"),
                flowScenes = o.optJSONArray("flowRefs")?.let { refs ->
                    (0 until refs.length()).mapNotNull { index ->
                        refs.optJSONObject(index)?.let { ref ->
                            val sceneIndex = ref.optInt("scene", -1)
                            val sha = ref.optString("sha")
                            if (sceneIndex in 0..11 && sha.matches(Regex("[a-f0-9]{64}")))
                                FlowBatchSceneRef(sceneIndex, sha) else null
                        }
                    }
                } ?: emptyList()
            ) }.getOrNull()
        }
    }

    private fun persist(items: List<GeminiBatchRecord>) {
        val array = JSONArray()
        items.takeLast(30).forEach { r -> array.put(JSONObject()
            .put("id", r.localId).put("profile", r.profileId)
            .put("project", r.projectId).put("model", r.model)
            .put("count", r.count).put("remote", r.remoteName)
            .put("state", r.state).put("time", r.createdAtMs)
            .put("flowProject", r.flowProjectId)
            .put("flowRefs", JSONArray().also { refs -> r.flowScenes.forEach { scene ->
                refs.put(JSONObject().put("scene", scene.index).put("sha", scene.fingerprint))
            } })) }
        check(prefs.edit().putString("records", array.toString()).commit()) {
            "Não foi possível gravar o registro do lote."
        }
    }

    /** Grava antes do POST. Se a resposta se perder, não reenvia automaticamente. */
    @Synchronized fun start(profileId: String, projectId: String, model: String, count: Int,
        flowProjectId: String = "", flowScenes: List<FlowBatchSceneRef> = emptyList()): GeminiBatchRecord {
        require(profileId.isNotBlank())
        require(count in 2..GeminiBatchPolicy.MAX_ITEMS)
        require(flowProjectId.isBlank() ||
            (flowProjectId.length <= 100 && flowScenes.size == count &&
                flowScenes.distinctBy { it.index }.size == count &&
                flowScenes.all { it.index in 0..11 && it.fingerprint.matches(Regex("[a-f0-9]{64}")) }))
        val record = GeminiBatchRecord("local-" + UUID.randomUUID(), profileId, projectId,
            GeminiBatchPolicy.model(model), count, "", "REVIEW", System.currentTimeMillis(),
            flowProjectId, flowScenes)
        persist(list() + record)
        return record
    }
    @Synchronized fun accept(record: GeminiBatchRecord, name: String) {
        val jobs = list()
        val existing = jobs.firstOrNull { it.localId == record.localId }
            ?: error("Lote não encontrado.")
        require(existing.remoteName.isBlank() || existing.remoteName == name) {
            "Não é permitido trocar o identificador remoto deste job."
        }
        persist(jobs.map { if (it.localId == record.localId)
            it.copy(remoteName = GeminiBatchPolicy.jobName(name), state = "JOB_STATE_PENDING") else it })
    }
    @Synchronized fun update(record: GeminiBatchRecord, newState: String) {
        persist(list().map { if (it.localId == record.localId) it.copy(state = newState) else it })
    }
    @Synchronized fun attach(profileId: String, projectId: String, model: String, name: String): GeminiBatchRecord {
        val remote = GeminiBatchPolicy.jobName(name)
        val existing = list().firstOrNull { it.profileId == profileId && it.remoteName == remote }
        if (existing != null) return existing
        val record = GeminiBatchRecord("local-" + UUID.randomUUID(), profileId, projectId,
            GeminiBatchPolicy.model(model), 0, remote, "", System.currentTimeMillis())
        persist(list() + record)
        return record
    }
    @Synchronized fun forget(record: GeminiBatchRecord) {
        persist(list().filterNot { it.localId == record.localId })
    }
}
