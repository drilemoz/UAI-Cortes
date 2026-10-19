package br.com.uaicortes

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class Clip(
    val id: String = UUID.randomUUID().toString(),
    val uri: String, val title: String,
    val sourceDurationMs: Long,
    val startMs: Long = 0,
    val endMs: Long = sourceDurationMs,
    val volume: Float = 1f
) {
    val durationMs: Long get() = (endMs - startMs).coerceAtLeast(1L)
    fun json() = JSONObject().put("id", id).put("uri", uri).put("title", title)
        .put("sourceDurationMs", sourceDurationMs).put("startMs", startMs)
        .put("endMs", endMs).put("volume", volume.toDouble())
    companion object {
        fun read(j: JSONObject) = Clip(j.optString("id", UUID.randomUUID().toString()),
            j.getString("uri"), j.optString("title", "Vídeo"), j.optLong("sourceDurationMs", 1000L),
            j.optLong("startMs"), j.optLong("endMs", 1000L), j.optDouble("volume", 1.0).toFloat())
    }
}

data class Overlay(
    val uri: String, val isImage: Boolean, val durationMs: Long,
    val startAtMs: Long = 0, val x: Float = 0.5f, val y: Float = 0.5f,
    val scale: Float = 0.36f, val volume: Float = 0.5f,
    val muted: Boolean = false,
    val overlapWith: List<String> = emptyList(),
    val id: String = UUID.randomUUID().toString()
) {
    fun json() = JSONObject().put("uri", uri).put("isImage", isImage)
        .put("durationMs", durationMs).put("startAtMs", startAtMs)
        .put("x", x.toDouble()).put("y", y.toDouble()).put("scale", scale.toDouble())
        .put("volume", volume.toDouble()).put("muted", muted)
        .put("overlapWith", JSONArray().apply { overlapWith.forEach { put(it) } }).put("id", id)
    companion object {
        fun read(j: JSONObject) = Overlay(j.getString("uri"), j.optBoolean("isImage"),
            j.optLong("durationMs", 5000), j.optLong("startAtMs"),
            j.optDouble("x", 0.5).toFloat(), j.optDouble("y", 0.5).toFloat(),
            j.optDouble("scale", 0.36).toFloat(), j.optDouble("volume", 0.5).toFloat(),
            j.optBoolean("muted", false),
            j.optJSONArray("overlapWith")?.let { array ->
                (0 until array.length()).mapNotNull { index ->
                    array.optString(index).takeIf { it.isNotBlank() }
                }.distinct()
            } ?: emptyList(),
            j.optString("id").ifBlank { UUID.randomUUID().toString() })
    }
}

data class Music(val uri: String, val title: String, val volume: Float = 0.35f) {
    fun json() = JSONObject().put("uri", uri).put("title", title).put("volume", volume.toDouble())
    companion object { fun read(j: JSONObject) = Music(j.getString("uri"), j.optString("title"), j.optDouble("volume", 0.35).toFloat()) }
}

/** Texto com tempo absoluto na linha do tempo (sem reconhecimento de voz ou serviços externos). */
data class SubtitleCue(
    val id: String = UUID.randomUUID().toString(),
    val text: String, val startMs: Long, val endMs: Long,
    val color: Int = -1, val sizeSp: Int = 22, val y: Float = .85f, val x: Float = .5f
) {
    fun json() = JSONObject().put("id", id).put("text", text)
        .put("startMs", startMs).put("endMs", endMs).put("color", color)
        .put("sizeSp", sizeSp).put("y", y.toDouble()).put("x", x.toDouble())
    companion object {
        fun read(j: JSONObject) = SubtitleCue(j.optString("id").ifBlank { UUID.randomUUID().toString() },
            j.optString("text"), j.optLong("startMs"), j.optLong("endMs"),
            j.optInt("color", -1), j.optInt("sizeSp", 22).coerceIn(12, 48),
            j.optDouble("y", .85).toFloat().coerceIn(.1f, .95f),
            j.optDouble("x", .5).toFloat().coerceIn(.1f, .9f))
    }
}

/** Identifica a fronteira entre dois clipes por IDs (não por índices mutáveis). */
data class ClipTransition(
    val fromId: String, val toId: String,
    val kind: String = "Corte direto", val durationMs: Long = 500L
) {
    fun json() = JSONObject().put("fromId", fromId).put("toId", toId)
        .put("kind", kind).put("durationMs", durationMs)
    companion object { fun read(j: JSONObject) = ClipTransition(
        j.optString("fromId"), j.optString("toId"), j.optString("kind", "Corte direto"),
        j.optLong("durationMs", 500L).coerceIn(100L, 1500L)) }
}

/** Faixa de áudio: startMs é posição no projeto; sourceStartMs é a entrada no arquivo de origem.
 * durationMs determina quantos milissegundos da fonte entram na composição.
 * Projetos anteriores não tinham sourceStartMs e continuam iniciando no zero da fonte. */
data class AudioTrack(
    val uri: String, val title: String, val startMs: Long = 0L,
    val durationMs: Long = 0L, val volume: Float = .35f,
    val id: String = UUID.randomUUID().toString(),
    val sourceStartMs: Long = 0L
) {
    fun json() = JSONObject().put("uri", uri).put("title", title)
        .put("startMs", startMs).put("durationMs", durationMs)
        .put("volume", volume.toDouble()).put("id", id)
        .put("sourceStartMs", sourceStartMs)
    companion object { fun read(j: JSONObject) = AudioTrack(j.getString("uri"),
        j.optString("title"), j.optLong("startMs"), j.optLong("durationMs"),
        j.optDouble("volume", .35).toFloat().coerceIn(0f,1f),
        j.optString("id").ifBlank { UUID.randomUUID().toString() },
        j.optLong("sourceStartMs", 0L).coerceAtLeast(0L)) }
}

data class VideoProject(
    val id: String = UUID.randomUUID().toString(), val title: String = "Meu vídeo",
    val updated: Long = System.currentTimeMillis(),
    val clips: List<Clip> = emptyList(), val overlays: List<Overlay> = emptyList(),
    val music: Music? = null, val transition: String = "Corte direto",
    val subtitles: List<SubtitleCue> = emptyList(),
    val transitions: List<ClipTransition> = emptyList(),
    val audioTracks: List<AudioTrack> = emptyList()
) {
    val durationMs get() = clips.sumOf { it.durationMs }
    val pip: Overlay? get() = overlays.firstOrNull() // acesso legado somente leitura
    fun json(): JSONObject = JSONObject().put("id", id).put("title", title).put("updated", updated)
        .put("clips", JSONArray().also { a -> clips.forEach { a.put(it.json()) } })
        .put("schemaVersion", 4)
        .put("overlays", JSONArray().also { a -> overlays.take(4).forEach { a.put(it.json()) } })
        .put("music", music?.json()).put("transition", transition)
        .put("subtitles", JSONArray().also { a -> subtitles.forEach { a.put(it.json()) } })
        .put("transitions", JSONArray().also { a -> transitions.forEach { a.put(it.json()) } })
        .put("audioTracks", JSONArray().also { a -> audioTracks.forEach { a.put(it.json()) } })
    companion object {
        fun read(j: JSONObject): VideoProject {
            val a = j.optJSONArray("clips") ?: JSONArray()
            return VideoProject(j.getString("id"), j.optString("title", "Meu vídeo"), j.optLong("updated"),
                (0 until a.length()).map { Clip.read(a.getJSONObject(it)) },
                (j.optJSONArray("overlays")?.let { overlays ->
                    (0 until overlays.length()).mapNotNull { index ->
                        runCatching { Overlay.read(overlays.getJSONObject(index)) }.getOrNull()
                    }.take(4)
                } ?: listOfNotNull(j.optJSONObject("pip")?.let(Overlay::read))),
                j.optJSONObject("music")?.let(Music::read),
                j.optString("transition", "Corte direto"),
                j.optJSONArray("subtitles")?.let { arr ->
                    (0 until arr.length()).mapNotNull { i -> runCatching { SubtitleCue.read(arr.getJSONObject(i)) }.getOrNull() }
                } ?: emptyList(),
                j.optJSONArray("transitions")?.let { arr ->
                    (0 until arr.length()).mapNotNull { i -> runCatching { ClipTransition.read(arr.getJSONObject(i)) }.getOrNull() }
                } ?: emptyList(),
                j.optJSONArray("audioTracks")?.let { arr ->
                    (0 until arr.length()).mapNotNull { i -> runCatching { AudioTrack.read(arr.getJSONObject(i)) }.getOrNull() }
                } ?: emptyList())
        }
    }
}

class ProjectStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("projects", Context.MODE_PRIVATE)
    fun load(): List<VideoProject> = runCatching {
        val a = JSONArray(prefs.getString("projects", "[]"))
        (0 until a.length()).map { VideoProject.read(a.getJSONObject(it)) }.sortedByDescending { it.updated }
    }.getOrDefault(emptyList())
    fun save(project: VideoProject) {
        val list = load().filterNot { it.id == project.id } + project.copy(updated = System.currentTimeMillis())
        prefs.edit().putString("projects", JSONArray().apply { list.forEach { put(it.json()) } }.toString()).apply()
    }
    fun delete(id: String) {
        prefs.edit().putString("projects", JSONArray().apply { load().filterNot { it.id == id }.forEach { put(it.json()) } }.toString()).apply()
    }
    fun theme(): String = prefs.getString("theme", "Sistema") ?: "Sistema"
    fun setTheme(t: String) = prefs.edit().putString("theme", t).apply()
    fun quality(): Int = prefs.getInt("quality", 720)
    fun setQuality(q: Int) = prefs.edit().putInt("quality", q).apply()
}

fun mediaDuration(context: Context, uri: Uri): Long = runCatching {
    val retriever = MediaMetadataRetriever()
    try {
        retriever.setDataSource(context, uri)
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong()?.coerceAtLeast(1L) ?: 1000L
    } finally { retriever.release() }
}.getOrDefault(1000L)

fun formatMs(ms: Long): String {
    val sec = ms.coerceAtLeast(0) / 1000L
    return "%02d:%02d".format(sec / 60, sec % 60)
}
