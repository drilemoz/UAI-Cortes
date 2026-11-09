package br.com.uaicortes

import android.content.Context
import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Cache privado somente de metadados, sem tokens OAuth nem credenciais. */
internal class SocialYouTubeSnapshotCache(context: Context) {
    private val file = AtomicFile(File(context.applicationContext.filesDir,
        "social_v1/youtube_dashboard_cache.json"))

    internal data class Snapshot(
        val videos: Map<String, List<YouTubeSocialApi.PreviousVideo>> = emptyMap(),
        val nextPages: Map<String, String> = emptyMap(),
        val stats: Map<String, YouTubeSocialApi.ChannelStatistics> = emptyMap()
    )

    fun load(): Snapshot = runCatching {
        val content = file.openRead().use { stream ->
            val bytes = stream.readBytes()
            require(bytes.size <= 4_000_000) { "Cache acima do limite" }
            JSONObject(String(bytes, Charsets.UTF_8))
        }
        require(content.optInt("schema") == 1)
        val entries = content.optJSONArray("channels") ?: JSONArray()
        val loadedVideos = linkedMapOf<String, List<YouTubeSocialApi.PreviousVideo>>()
        val nextPages = linkedMapOf<String, String>()
        val stats = linkedMapOf<String, YouTubeSocialApi.ChannelStatistics>()
        for (index in 0 until minOf(entries.length(), 30)) {
            val entry = entries.optJSONObject(index) ?: continue
            val id = entry.optString("id")
            if (id.isBlank() || !SocialPublicationPolicy.validChannel(id)) continue
            val items = entry.optJSONArray("videos") ?: JSONArray()
            val videos = buildList {
                for (j in 0 until minOf(items.length(), 300)) {
                    val item = items.optJSONObject(j) ?: continue
                    val videoId = item.optString("id")
                    if (videoId.isBlank() || !SocialPublicationPolicy.validVideoId(videoId)) continue
                    add(YouTubeSocialApi.PreviousVideo(
                        id = videoId, title = item.optString("title").take(160),
                        publishedAt = item.optString("date").take(40),
                        viewCount = optionalNumber(item, "views"),
                        channelId = id,
                        channelTitle = item.optString("channelTitle").take(100)
                    ))
                }
            }.distinctBy { it.id }
            loadedVideos[id] = videos
            nextPages[id] = entry.optString("nextPage").take(1024)
            entry.optJSONObject("stats")?.let { json ->
                stats[id] = YouTubeSocialApi.ChannelStatistics(id,
                    optionalNumber(json, "views"), optionalNumber(json, "videos"),
                    optionalNumber(json, "subscribers"),
                    checkedAt = json.optLong("checkedAt", 0L).coerceAtLeast(0L))
            }
        }
        Snapshot(loadedVideos, nextPages, stats)
    }.getOrDefault(Snapshot())

    fun save(snapshot: Snapshot) {
        val content = JSONObject().put("schema", 1)
        val channels = JSONArray()
        val ids = (snapshot.videos.keys + snapshot.stats.keys).distinct().take(30)
        ids.forEach { id ->
            if (id.isBlank() || !SocialPublicationPolicy.validChannel(id)) return@forEach
            val entry = JSONObject().put("id", id)
                .put("nextPage", snapshot.nextPages[id].orEmpty().take(1024))
            val entries = JSONArray()
            snapshot.videos[id].orEmpty().take(300).forEach { video ->
                if (!SocialPublicationPolicy.validVideoId(video.id)) return@forEach
                entries.put(JSONObject().put("id", video.id)
                    .put("title", video.title.take(160))
                    .put("date", video.publishedAt.take(40))
                    .put("views", video.viewCount ?: JSONObject.NULL)
                    .put("channelTitle", video.channelTitle.take(100)))
            }
            entry.put("videos", entries)
            snapshot.stats[id]?.let { stats ->
                entry.put("stats", JSONObject()
                    .put("views", stats.views ?: JSONObject.NULL)
                    .put("videos", stats.videos ?: JSONObject.NULL)
                    .put("subscribers", stats.subscribers ?: JSONObject.NULL)
                    .put("checkedAt", stats.checkedAt))
            }
            channels.put(entry)
        }
        content.put("channels", channels)
        val bytes = content.toString().toByteArray(Charsets.UTF_8)
        require(bytes.size <= 4_000_000)
        file.baseFile.parentFile?.mkdirs()
        val stream = file.startWrite()
        try {
            stream.write(bytes)
            file.finishWrite(stream)
        } catch (error: Exception) {
            file.failWrite(stream)
            throw error
        }
    }

    private fun optionalNumber(json: JSONObject, key: String): Long? =
        if (json.isNull(key)) null else json.optString(key).toLongOrNull()?.takeIf { it >= 0 }
}
