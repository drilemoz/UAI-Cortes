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
    val scale: Float = 0.36f, val volume: Float = 0.5f
) {
    fun json() = JSONObject().put("uri", uri).put("isImage", isImage)
        .put("durationMs", durationMs).put("startAtMs", startAtMs)
        .put("x", x.toDouble()).put("y", y.toDouble()).put("scale", scale.toDouble())
        .put("volume", volume.toDouble())
    companion object {
        fun read(j: JSONObject) = Overlay(j.getString("uri"), j.optBoolean("isImage"),
            j.optLong("durationMs", 5000), j.optLong("startAtMs"),
            j.optDouble("x", 0.5).toFloat(), j.optDouble("y", 0.5).toFloat(),
            j.optDouble("scale", 0.36).toFloat(), j.optDouble("volume", 0.5).toFloat())
    }
}

data class Music(val uri: String, val title: String, val volume: Float = 0.35f) {
    fun json() = JSONObject().put("uri", uri).put("title", title).put("volume", volume.toDouble())
    companion object { fun read(j: JSONObject) = Music(j.getString("uri"), j.optString("title"), j.optDouble("volume", 0.35).toFloat()) }
}

data class VideoProject(
    val id: String = UUID.randomUUID().toString(), val title: String = "Meu vídeo",
    val updated: Long = System.currentTimeMillis(),
    val clips: List<Clip> = emptyList(), val pip: Overlay? = null,
    val music: Music? = null, val transition: String = "Corte direto"
) {
    val durationMs get() = clips.sumOf { it.durationMs }
    fun json(): JSONObject = JSONObject().put("id", id).put("title", title).put("updated", updated)
        .put("clips", JSONArray().also { a -> clips.forEach { a.put(it.json()) } })
        .put("pip", pip?.json()).put("music", music?.json()).put("transition", transition)
    companion object {
        fun read(j: JSONObject): VideoProject {
            val a = j.optJSONArray("clips") ?: JSONArray()
            return VideoProject(j.getString("id"), j.optString("title", "Meu vídeo"), j.optLong("updated"),
                (0 until a.length()).map { Clip.read(a.getJSONObject(it)) },
                j.optJSONObject("pip")?.let(Overlay::read), j.optJSONObject("music")?.let(Music::read),
                j.optString("transition", "Corte direto"))
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
