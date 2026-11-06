package br.com.uaicortes

import android.content.Context
import android.util.AtomicFile
import org.json.JSONObject
import java.io.File
import java.util.concurrent.atomic.AtomicLong

/** Rascunho recuperável, separado dos projetos e de project_backups/. Nunca é publicado na Home. */
internal data class DraftRecovery(val project: VideoProject, val baselineHash: String?)

internal class ProjectDraftRecoveryStore(context: Context,
    fileName: String = "editor_draft_recovery.json") {
    init { require(Regex("editor_draft_[a-zA-Z0-9_-]+\\.json").matches(fileName)) }
    private val file = AtomicFile(File(context.filesDir, fileName))
    private val lock = Any()
    private val generation = AtomicLong(0)

    fun requestVersion(): Long = generation.incrementAndGet()

    fun saveIfCurrent(version: Long, current: VideoProject, baseline: VideoProject?) = synchronized(lock) {
        if (generation.get() != version) return@synchronized
        val raw = current.json().toString()
        val checksum = ProjectBackupPolicy.hash(raw)
        val obj = JSONObject().put("format", "uai-cortes-edit-draft-v1")
            .put("project", raw).put("sha256", checksum)
            .put("baselineHash", baseline?.json()?.toString()?.let(ProjectBackupPolicy::hash))
        val bytes = obj.toString().toByteArray(Charsets.UTF_8)
        require(bytes.size in 1..ProjectBackupCodec.MAX_BYTES) { "Rascunho excede limite local" }
        val out = file.startWrite()
        try {
            out.write(bytes)
            file.finishWrite(out)
        } catch (failure: Exception) {
            file.failWrite(out)
            throw failure
        }
    }

    fun load(savedProjects: List<VideoProject>): DraftRecovery? = synchronized(lock) {
        if (!file.baseFile.exists()) return@synchronized null
        runCatching {
            require(file.baseFile.length() in 1..ProjectBackupCodec.MAX_BYTES.toLong())
            val json = JSONObject(file.openRead().use { it.readBytes().toString(Charsets.UTF_8) })
            require(json.getString("format") == "uai-cortes-edit-draft-v1")
            val raw = json.getString("project")
            require(ProjectBackupPolicy.hash(raw) == json.getString("sha256"))
            val project = VideoProject.read(JSONObject(raw))
            val signature = json.optString("baselineHash").takeIf { it.isNotBlank() && it != "null" }
            val saved = savedProjects.firstOrNull { it.id == project.id }
            // Não sobrescrever uma versão salva depois que o rascunho foi criado.
            require(if (signature == null) saved == null else saved != null &&
                ProjectBackupPolicy.hash(saved.json().toString()) == signature) {
                "O projeto salvo mudou depois da sessão interrompida"
            }
            DraftRecovery(project, signature)
        }.getOrNull()
    }

    fun hasStoredDraft(): Boolean = synchronized(lock) { file.baseFile.exists() }

    fun clear() {
        generation.incrementAndGet()
        synchronized(lock) { file.delete() }
    }
}
