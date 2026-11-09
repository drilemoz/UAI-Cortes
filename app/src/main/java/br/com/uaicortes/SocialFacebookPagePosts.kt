package br.com.uaicortes

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Consulta parcial de posts de Páginas Meta com anexo de vídeo.
 * Não promete listar todos os Reels: a disponibilidade depende da Graph API e das permissões.
 */
internal data class FacebookPageVideoPost(
    val id: String,
    val pageId: String,
    val pageName: String,
    val title: String,
    val createdTime: String,
    val permalink: String,
    val thumbnail: String
)

internal object FacebookPagePostPolicy {
    fun safePostUrl(url: String): String {
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return ""
        return if (uri.scheme == "https" && uri.port == -1 && uri.userInfo == null &&
            uri.host?.lowercase() in setOf("facebook.com", "www.facebook.com", "m.facebook.com") &&
            url.length <= 1200) url else ""
    }

    fun safeThumbnail(url: String): String {
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return ""
        val host = uri.host?.lowercase().orEmpty()
        return if (uri.scheme == "https" && uri.port == -1 && uri.userInfo == null &&
            (host == "fbcdn.net" || host.endsWith(".fbcdn.net")) && url.length <= 2000) url else ""
    }

    fun safeCursor(value: String): String {
        require(value.length <= 1024 && value.all { it.isLetterOrDigit() || it in "_-+/=." }) {
            "Paginação da Meta inválida. Atualize a consulta desde o início."
        }
        return value
    }

    private fun videoAttachment(item: JSONObject): JSONObject? {
        val arr = item.optJSONObject("attachments")?.optJSONArray("data") ?: return null
        for (i in 0 until minOf(arr.length(), 10)) {
            val att = arr.optJSONObject(i) ?: continue
            val mediaType = att.optString("media_type").lowercase()
            val type = att.optString("type").lowercase()
            if (mediaType == "video" || mediaType.startsWith("video_") ||
                type == "video" || type.startsWith("video_") || type.contains("reel")) return att
        }
        return null
    }

    fun parse(root: JSONObject, pageId: String, pageName: String): PageResult {
        require(SocialPublicationPolicy.validPageId(pageId))
        val data = root.optJSONArray("data") ?: error("A Meta não retornou uma lista de publicações.")
        val posts = buildList {
            for (i in 0 until minOf(data.length(), 100)) {
                val item = data.optJSONObject(i) ?: continue
                val id = item.optString("id")
                if (!id.matches(Regex("[0-9_]{5,75}"))) continue
                val attachment = videoAttachment(item) ?: continue
                val image = attachment.optJSONObject("media")?.optJSONObject("image")?.optString("src").orEmpty()
                val title = item.optString("message").lineSequence().firstOrNull()
                    .orEmpty().trim().take(140).ifBlank { "Vídeo da Página" }
                add(FacebookPageVideoPost(id, pageId, pageName.take(100), title,
                    item.optString("created_time").take(40),
                    safePostUrl(item.optString("permalink_url")), safeThumbnail(image)))
            }
        }.distinctBy { it.id }
        val paging = root.optJSONObject("paging")
        val next = if (paging?.optString("next").isNullOrBlank()) "" else
            paging?.optJSONObject("cursors")?.optString("after").orEmpty()
        // Cursor opcional malformado não deve descartar vídeos da primeira página.
        return PageResult(posts, runCatching { safeCursor(next) }.getOrDefault(""), data.length())
    }

    data class PageResult(val posts: List<FacebookPageVideoPost>, val nextCursor: String,
        val consulted: Int)
}

internal object FacebookPagePostsApi {
    private const val BASE = "https://graph.facebook.com/v26.0"

    /** Só é chamado a partir de ação explícita; token de Página fica apenas em RAM. */
    suspend fun listVideoPosts(token: String, pageId: String, pageName: String,
        cursor: String = ""): FacebookPagePostPolicy.PageResult = withContext(Dispatchers.IO) {
        require(SocialPublicationPolicy.validPageId(pageId))
        require(token.length in 10..4096 && !token.any(Char::isWhitespace))
        val next = FacebookPagePostPolicy.safeCursor(cursor)
        val fields = "id,message,created_time,permalink_url,attachments{media_type,type,media}"
        val url = "$BASE/$pageId/posts?limit=25&fields=" + URLEncoder.encode(fields, "UTF-8") +
            if (next.isEmpty()) "" else "&after=" + URLEncoder.encode(next, "UTF-8")
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 20_000
            readTimeout = 45_000
            instanceFollowRedirects = false
            useCaches = false
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Accept", "application/json")
        }
        try {
            val status = conn.responseCode
            DiagnosticRecorder.api("meta", "page_video_posts", null, status, null,
                outcome = if (status in 200..299) "ok" else "http_error")
            if (status !in 200..299) {
                throw IllegalStateException(when (status) {
                    400, 403 -> "A Meta não autorizou a leitura das publicações desta Página. Confira pages_read_engagement e as permissões aprovadas."
                    429 -> "Limite de consultas da Meta atingido. Tente mais tarde."
                    else -> "A Meta não disponibilizou as publicações (HTTP $status)."
                })
            }
            val bytes = conn.inputStream.use { input ->
                val out = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(4096)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    require(out.size() + read <= 256_000) { "Resposta da Meta excedeu o limite de tamanho." }
                    out.write(buffer, 0, read)
                }
                out.toByteArray()
            }
            FacebookPagePostPolicy.parse(JSONObject(String(bytes, Charsets.UTF_8)), pageId, pageName)
        } finally { conn.disconnect() }
    }
}
