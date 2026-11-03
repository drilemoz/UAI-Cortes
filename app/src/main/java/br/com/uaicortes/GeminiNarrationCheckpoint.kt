package br.com.uaicortes

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Checkpoint durável POR PROJETO da geração por trechos. Não armazena credenciais,
 * não envia dados e nunca inicia uma chamada Gemini automaticamente.
 * Os WAVs pertencem ao armazenamento interno durável do aplicativo.
 */
internal class GeminiNarrationCheckpoint(context: Context, private val projectId: String) {
    internal data class Snapshot(
        val durationMs: Long,
        val text: String,
        val model: String,
        val voice: GeminiVoice?,
        val style: String,
        val pace: Int,
        val expressiveness: Int,
        val pitch: Int,
        val enabled: Boolean,
        val segments: List<SubtitleNarrationTimeline.Segment>,
        val files: List<GeminiTimedFile>,
        val starts: Map<String, Long>,
        val goals: Map<String, Long>
    )
    internal data class Recovery(val snapshot: Snapshot?, val message: String)

    private val dir = File(context.filesDir, "gemini_narration_sessions")
    private val audioDir = File(context.filesDir, "gemini_narrations")
    private val file = File(dir, "projeto_${hash(projectId)}.json")
    private val writeMutex = Mutex()
    private val revision = AtomicLong(0L)
    private var committed = 0L

    fun capture(draft: GeminiNarrationDraft, durationMs: Long) = Snapshot(
        durationMs = durationMs,
        text = draft.text.value,
        model = draft.model.value,
        voice = draft.selectedVoice.value,
        style = draft.style.value,
        pace = draft.pace.intValue,
        expressiveness = draft.expressiveness.intValue,
        pitch = draft.pitch.intValue,
        enabled = draft.useTimedSubtitles.value,
        segments = draft.timedLines.value.toList(),
        files = draft.timedFiles.value.toList(),
        starts = draft.referenceStarts.value.toMap(),
        goals = draft.durationGoals.value.toMap()
    )

    /** A revisão monotônica impede que um snapshot antigo sobrescreva um novo. */
    suspend fun save(snapshot: Snapshot) {
        val serial = revision.incrementAndGet()
        writeMutex.withLock {
            if (serial <= committed) return@withLock
            val json = encode(snapshot).toString().toByteArray(Charsets.UTF_8)
            require(json.size <= MAX_BYTES) { "Registro de narração acima do limite." }
            if (!dir.exists() && !dir.mkdirs()) throw IOException("Não foi possível criar pasta da sessão.")
            val temp = File(dir, "${file.name}.part")
            try {
                FileOutputStream(temp).use { out ->
                    out.write(json)
                    out.flush()
                    out.fd.sync()
                }
                if (!temp.renameTo(file)) throw IOException("Falha ao confirmar sessão de narração.")
                committed = serial
            } finally { temp.delete() }
        }
    }

    /** Recupera somente trechos pertencentes às legendas atuais e WAVs íntegros. */
    fun recover(project: VideoProject): Recovery {
        if (!file.isFile) return Recovery(null, "")
        return try {
            if (file.length() !in 2L..MAX_BYTES.toLong())
                return Recovery(null, "Registro de recuperação inválido; nenhuma geração foi iniciada.")
            val root = JSONObject(file.readText(Charsets.UTF_8))
            if (root.optInt("schema") != SCHEMA || root.optString("projectId") != project.id ||
                root.optLong("durationMs") != project.durationMs) {
                return Recovery(null, "Projeto alterado; sessão anterior não foi restaurada automaticamente.")
            }
            val stored = root.optJSONArray("segments") ?: JSONArray()
            if (stored.length() > SubtitleNarrationTimeline.MAX_SEGMENTS)
                return Recovery(null, "Registro possui falas além do limite permitido.")
            val segments = mutableListOf<SubtitleNarrationTimeline.Segment>()
            for (i in 0 until stored.length()) {
                val row = stored.optJSONObject(i) ?: continue
                val id = row.optString("id").take(160)
                val start = row.optLong("start", -1L)
                val end = row.optLong("end", -1L)
                val spoken = row.optString("text").take(SubtitleNarrationReuse.MAX_CHARACTERS)
                if (project.subtitles.none { it.id == id && it.startMs == start && it.endMs == end }) continue
                if (spoken.isBlank()) continue
                segments += SubtitleNarrationTimeline.Segment(id, spoken, start, end)
            }
            if (segments.isEmpty()) {
                if (stored.length() == 0) return Recovery(null, "")
                return Recovery(null, "Legendas anteriores mudaram; importe-as novamente.")
            }
            if (SubtitleNarrationTimeline.validate(segments, project.durationMs) != null)
                return Recovery(null, "Os horários recuperados precisam de nova revisão.")
            val allowed = segments.associateBy { it.cueId }
            val files = mutableListOf<GeminiTimedFile>()
            val saved = root.optJSONArray("files") ?: JSONArray()
            for (i in 0 until minOf(saved.length(), SubtitleNarrationTimeline.MAX_SEGMENTS)) {
                val row = saved.optJSONObject(i) ?: continue
                val segment = allowed[row.optString("id")] ?: continue
                val name = row.optString("name")
                val checksum = row.optString("sha256")
                val size = row.optLong("size")
                val ms = row.optLong("duration")
                val settings = row.optString("settings")
                if (!SAFE_FILE.matches(name) || !SHA256.matches(checksum) ||
                    ms < SubtitleNarrationTimeline.MIN_AUDIO_MS || ms > project.durationMs * 4 ||
                    settings.length !in 1..1024 || size !in 45..MAX_WAV_BYTES) continue
                val wav = File(audioDir, name)
                if (wav.canonicalFile.parentFile != audioDir.canonicalFile ||
                    !wav.isFile || wav.length() != size || hash(wav) != checksum) continue
                val goal = row.optLong("goal", -1L).takeIf { it > 0L }
                files += GeminiTimedFile(segment, wav, ms, settings, goal, checksum)
            }
            val voiceRow = root.optJSONObject("voice")
            val voice = voiceRow?.optString("id")?.takeIf { it.isNotBlank() && it.length <= 150 }
                ?.let { id -> GeminiVoice(id,
                    voiceRow.optString("name", id).take(120),
                    voiceRow.optString("description").take(300),
                    voiceRow.optString("language").take(30),
                    voiceRow.optString("accent").take(80),
                    voiceRow.optString("persona").take(100),
                    voiceRow.optString("context").take(80),
                    voiceRow.optString("gender").take(30),
                    voiceRow.optString("pitch").take(30)) }
            val starts = readMap(root.optJSONObject("starts"), allowed.keys, 0L, project.durationMs - 1L)
            val goals = readMap(root.optJSONObject("goals"), allowed.keys,
                NarrationDurationAssist.MIN_TARGET_MS, NarrationDurationAssist.MAX_TARGET_MS)
            val snapshot = Snapshot(project.durationMs, root.optString("text").take(SubtitleNarrationReuse.MAX_CHARACTERS),
                root.optString("model").takeIf { it in GeminiModels.supported } ?: GeminiModels.LITE,
                voice, root.optString("style").takeIf { it in GeminiNarrationStyling.presets.map { p -> p.label } }
                    ?: "Neutra", root.optInt("pace", 2).coerceIn(0, 4),
                root.optInt("expressiveness", 2).coerceIn(0, 4), root.optInt("pitch", 2).coerceIn(0, 4),
                root.optBoolean("enabled", true), segments, files.distinctBy { it.segment.cueId }, starts, goals)
            Recovery(snapshot, "Sessão recuperada: ${files.size}/${segments.size} WAVs verificados. " +
                "Para concluir o lote, confirme manualmente apenas as chamadas pendentes.")
        } catch (_: Exception) {
            Recovery(null, "Não foi possível verificar a sessão anterior. Nenhuma chamada foi iniciada.")
        }
    }

    private fun readMap(obj: JSONObject?, ids: Set<String>, min: Long, max: Long): Map<String, Long> {
        if (obj == null) return emptyMap()
        return ids.mapNotNull { id ->
            val n = obj.optLong(id, -1L)
            if (n in min..max) id to n else null
        }.toMap()
    }

    private fun encode(s: Snapshot): JSONObject {
        val root = JSONObject().put("schema", SCHEMA).put("projectId", projectId)
            .put("durationMs", s.durationMs).put("text", s.text.take(SubtitleNarrationReuse.MAX_CHARACTERS))
            .put("model", s.model).put("style", s.style).put("pace", s.pace)
            .put("expressiveness", s.expressiveness).put("pitch", s.pitch).put("enabled", s.enabled)
        s.voice?.let { v -> root.put("voice", JSONObject()
            .put("id", v.id).put("name", v.name).put("description", v.description)
            .put("language", v.languageCode).put("accent", v.accent).put("persona", v.persona)
            .put("context", v.context).put("gender", v.gender).put("pitch", v.pitch)) }
        root.put("segments", JSONArray().also { array -> s.segments.take(SubtitleNarrationTimeline.MAX_SEGMENTS).forEach { v ->
            array.put(JSONObject().put("id", v.cueId).put("text", v.text)
                .put("start", v.startMs).put("end", v.subtitleEndMs))
        } })
        val allowed = s.segments.map { it.cueId }.toSet()
        root.put("files", JSONArray().also { array -> s.files.filter {
            it.segment.cueId in allowed && it.file.parentFile == audioDir &&
                SAFE_FILE.matches(it.file.name) && SHA256.matches(it.sha256)
        }.take(SubtitleNarrationTimeline.MAX_SEGMENTS).forEach { v ->
            array.put(JSONObject().put("id", v.segment.cueId).put("name", v.file.name)
                .put("size", v.file.length()).put("sha256", v.sha256)
                .put("duration", v.durationMs).put("settings", v.settingsKey)
                .put("goal", v.durationGoalMs ?: -1L))
        } })
        val starts = JSONObject(); s.starts.forEach { (key, value) -> if (key in allowed) starts.put(key, value) }
        val goals = JSONObject(); s.goals.forEach { (key, value) -> if (key in allowed) goals.put(key, value) }
        return root.put("starts", starts).put("goals", goals)
    }

    companion object {
        private const val SCHEMA = 1
        private const val MAX_BYTES = 256 * 1024
        private const val MAX_WAV_BYTES = 32L * 1024 * 1024
        private val SAFE_FILE = Regex("narracao_[a-zA-Z0-9_.-]+\\.wav")
        private val SHA256 = Regex("[a-f0-9]{64}")
        fun hash(data: ByteArray): String = MessageDigest.getInstance("SHA-256")
            .digest(data).joinToString("") { "%02x".format(it) }
        private fun hash(text: String): String = hash(text.toByteArray(Charsets.UTF_8))
        private fun hash(file: File): String {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(8192)
                while (true) {
                    val n = input.read(buffer)
                    if (n < 0) break
                    digest.update(buffer, 0, n)
                }
            }
            return digest.digest().joinToString("") { "%02x".format(it) }
        }
    }
}
