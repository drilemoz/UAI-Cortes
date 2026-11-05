package br.com.uaicortes

import android.content.Context
import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException

/** Rascunhos temporários locais: não armazenam chave, prompt, áudio ou URI. */
/** Um manifesto por tarefa + identidade do projeto/arquivo; gravação atômica, sem backup de projeto. */
internal class GeminiTaskCheckpointStore(context: Context, kind: GeminiTaskKind, identity: String) {
    private val directory = File(context.applicationContext.filesDir, "gemini_task_checkpoints")
    private val path = AtomicFile(File(directory,
        "${kind.name.lowercase()}_${GeminiTaskRecoveryPolicy.digest(identity)}.json"))

    fun load(fingerprint: String): GeminiTaskCheckpoint? {
        if (!path.baseFile.exists()) return null
        if (path.baseFile.length() !in 2L..GeminiTaskRecoveryPolicy.MAX_BYTES.toLong())
            throw IOException("Checkpoint Gemini acima do limite")
        val root = JSONObject(path.openRead().bufferedReader(Charsets.UTF_8).use { it.readText() })
        if (root.optInt("schema") != 1) throw IOException("Checkpoint Gemini incompatível")
        val snapshot = GeminiTaskCheckpoint(root.getString("fingerprint"), root.getString("model"),
            GeminiTaskStage.valueOf(root.getString("stage")), root.getLong("timestamp"),
            root.optString("payload").takeIf { root.has("payload") && !root.isNull("payload") })
        val now = System.currentTimeMillis()
        val result = GeminiTaskRecoveryPolicy.recover(snapshot, fingerprint, now)
        // Expiração também remove bytes do cache privado, não apenas esconde o cartão.
        if (now > snapshot.timestamp &&
            now - snapshot.timestamp > GeminiTaskRecoveryPolicy.RETENTION_MS) path.delete()
        return result
    }

    fun start(fingerprint: String, model: String) = save(GeminiTaskCheckpoint(
        fingerprint, model, GeminiTaskStage.IN_FLIGHT, System.currentTimeMillis()))

    fun review(fingerprint: String, model: String) = save(GeminiTaskCheckpoint(
        fingerprint, model, GeminiTaskStage.REVIEW, System.currentTimeMillis()))

    fun saveSubtitles(fingerprint: String, model: String, cues: List<SubtitleCue>) {
        require(cues.size <= GeminiTaskRecoveryPolicy.MAX_CUES)
        val payload = JSONArray().apply { cues.forEach { put(it.json()) } }.toString()
        save(GeminiTaskCheckpoint(fingerprint, model, GeminiTaskStage.READY,
            System.currentTimeMillis(), payload))
    }

    fun subtitles(snapshot: GeminiTaskCheckpoint): List<SubtitleCue> {
        require(snapshot.stage == GeminiTaskStage.READY)
        val rows = JSONArray(snapshot.payload ?: throw IOException("Resultado não salvo"))
        if (rows.length() > GeminiTaskRecoveryPolicy.MAX_CUES) throw IOException("Legendas demais")
        return (0 until rows.length()).map { SubtitleCue.read(rows.getJSONObject(it)) }
    }

    fun saveDescription(fingerprint: String, model: String, draft: VideoPublicationDraft) {
        save(GeminiTaskCheckpoint(fingerprint, model, GeminiTaskStage.READY,
            System.currentTimeMillis(), draft.json().toString()))
    }

    fun description(snapshot: GeminiTaskCheckpoint): VideoPublicationDraft {
        require(snapshot.stage == GeminiTaskStage.READY)
        return VideoPublicationDraft.read(JSONObject(snapshot.payload ?: "{}"))
            ?: throw IOException("Rascunho de descrição ausente")
    }

    private fun save(snapshot: GeminiTaskCheckpoint) {
        require(GeminiTaskRecoveryPolicy.valid(snapshot))
        val result = JSONObject().put("schema", 1).put("fingerprint", snapshot.fingerprint)
            .put("model", snapshot.model).put("stage", snapshot.stage.name)
            .put("timestamp", snapshot.timestamp)
        snapshot.payload?.let { result.put("payload", it) }
        val bytes = result.toString().toByteArray(Charsets.UTF_8)
        require(bytes.size <= GeminiTaskRecoveryPolicy.MAX_BYTES) { "Checkpoint excede 1 MiB" }
        if (!directory.exists() && !directory.mkdirs()) throw IOException("Erro ao criar cache de tarefas")
        val out = path.startWrite()
        try { out.write(bytes); path.finishWrite(out) }
        catch (error: Exception) { path.failWrite(out); throw error }
    }

    fun clear() = path.delete()
}
