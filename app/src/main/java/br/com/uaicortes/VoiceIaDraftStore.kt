package br.com.uaicortes

import android.content.Context
import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

/** Rascunhos de WAV independentes do projeto. Nunca guarda chaves de API ou texto de legendas. */
internal data class VoiceIaTake(
    val cueId: String, val cueFingerprint: String, val voiceId: String,
    val model: String, val style: String, val filePath: String,
    val durationMs: Long, val sha256: String,
    /** O WAV original permanece imutável após correção local. */
    val sourceFilePath: String? = null, val sourceDurationMs: Long? = null,
    val requestStyle: String? = null
) {
    fun json(): JSONObject = JSONObject().put("cueId", cueId)
        .put("cueFingerprint", cueFingerprint).put("voiceId", voiceId)
        .put("model", model).put("style", style).put("filePath", filePath)
        .put("durationMs", durationMs).put("sha256", sha256)
        .put("sourceFilePath", sourceFilePath).put("sourceDurationMs", sourceDurationMs)
        .put("requestStyle", requestStyle)
    companion object {
        fun read(value: JSONObject) = VoiceIaTake(value.getString("cueId"),
            value.getString("cueFingerprint"), value.getString("voiceId"),
            value.getString("model"), value.getString("style"),
            value.getString("filePath"), value.getLong("durationMs"),
            value.getString("sha256"),
            value.optString("sourceFilePath").takeUnless { it.isBlank() || it == "null" },
            value.optLong("sourceDurationMs", 0L).takeIf { it > 0L },
            value.optString("requestStyle").takeUnless { it.isBlank() || it == "null" })
    }
}

/** Contrato puro: só se reutiliza WAV cuja legenda e parâmetros ainda sejam idênticos. */
internal object VoiceIaPolicy {
    const val KIND = "gemini_voice"
    fun fingerprint(cue: SubtitleCue): String = digest("${cue.id}\u0000${cue.startMs}\u0000${cue.endMs}\u0000${cue.text}")
    fun digest(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    fun matches(take: VoiceIaTake, cue: SubtitleCue, voiceId: String, model: String, style: String): Boolean =
        take.cueId == cue.id && take.cueFingerprint == fingerprint(cue) &&
        take.voiceId == voiceId && take.model == model && take.style == style
    fun fits(start: Long, duration: Long, projectDuration: Long): Boolean =
        start >= 0L && duration >= SubtitleNarrationTimeline.MIN_AUDIO_MS &&
            start < projectDuration && duration <= projectDuration - start
    fun merge(project: VideoProject, cue: SubtitleCue, take: VoiceIaTake): VideoProject {
        val old = project.audioTracks.firstOrNull { it.kind == KIND && it.subtitleId == cue.id }
        val start = old?.startMs ?: cue.startMs
        require(fits(start, take.durationMs, project.durationMs)) { "A voz não cabe inteira na timeline." }
        val replacement = AudioTrack(uri = GeminiAudioFiles.uri(File(take.filePath)),
            title = "Voz · ${cue.text.trim().take(32)}", startMs = start,
            durationMs = take.durationMs, volume = old?.volume ?: 1f,
            id = old?.id ?: java.util.UUID.randomUUID().toString(),
            kind = KIND, subtitleId = cue.id, voiceId = take.voiceId,
            ttsModel = take.model, ttsStyle = take.style,
            subtitleFingerprint = take.cueFingerprint)
        return project.copy(audioTracks = project.audioTracks.filterNot {
            it.kind == KIND && it.subtitleId == cue.id
        } + replacement)
    }
}

internal class VoiceIaDraftStore(context: Context, projectId: String) {
    private val app = context.applicationContext
    private val folder = File(app.filesDir, "voice_ia_drafts")
    private val file = AtomicFile(File(folder, "${VoiceIaPolicy.digest(projectId)}.json"))
    private val audioDir = File(app.filesDir, "gemini_narrations").canonicalFile

    fun valid(take: VoiceIaTake): Boolean = try {
        val audio = File(take.filePath).canonicalFile
        val original = take.sourceFilePath?.let { File(it).canonicalFile }
        if (audio.parentFile != audioDir || !audio.isFile ||
            (original != null && (original.parentFile != audioDir || !original.isFile)) ||
            take.durationMs < 250L || take.sha256.length != 64 || audio.length() > 32L * 1024 * 1024) false
        else {
            val digest = MessageDigest.getInstance("SHA-256")
            audio.inputStream().buffered().use { input ->
                val buffer = ByteArray(8192)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    digest.update(buffer, 0, read)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) } == take.sha256
        }
    } catch (_: Exception) { false }

    fun load(): List<VoiceIaTake> {
        if (!file.baseFile.exists()) return emptyList()
        val json = JSONObject(file.openRead().bufferedReader().use { it.readText() })
        val entries = json.optJSONArray("takes") ?: JSONArray()
        return (0 until entries.length()).mapNotNull { i ->
            runCatching { VoiceIaTake.read(entries.getJSONObject(i)) }.getOrNull()
                ?.takeIf(::valid)
        }.distinctBy { it.cueId }.takeLast(150)
    }

    fun save(takes: List<VoiceIaTake>) {
        if (!folder.exists() && !folder.mkdirs()) error("Não foi possível criar a pasta dos rascunhos")
        val payload = JSONObject().put("version", 1).put("takes", JSONArray().apply {
            takes.takeLast(150).forEach { put(it.json()) }
        }).toString().toByteArray(Charsets.UTF_8)
        val output = file.startWrite()
        try {
            output.write(payload)
            file.finishWrite(output)
        } catch (e: Exception) { file.failWrite(output); throw e }
    }
}
