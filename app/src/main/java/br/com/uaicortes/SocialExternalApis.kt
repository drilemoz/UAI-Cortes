package br.com.uaicortes

import android.content.Context
import android.net.Uri
import android.media.MediaMetadataRetriever
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Clientes de publicação autorizada, sem client secret no APK e sem tokens persistidos.
 * Um token OAuth válido deve ser fornecido pelo próprio usuário para CADA sessão avançada.
 * Não substitui um fluxo OAuth verificado da Meta ou do TikTok.
 */
internal object SocialExternalApis {
    data class FacebookPage(val id: String, val name: String, val accessToken: String = "")
    data class TikTokCreator(
        val name: String, val username: String, val privacyOptions: List<String>,
        val commentsDisabled: Boolean, val duetDisabled: Boolean,
        val stitchDisabled: Boolean, val maxDurationSeconds: Int
    )

    private const val META = "https://graph.facebook.com/v26.0"
    private const val TIKTOK = "https://open.tiktokapis.com"
    private const val CHUNK = 5 * 1024 * 1024 // 5 MiB; chunks TikTok, exceto último.
    private const val MAX_VIDEO_BYTES = 512L * 1024 * 1024 // limite conservador no Android.

    private fun connection(url: String, token: String, method: String, host: String): HttpURLConnection {
        val parsed = Uri.parse(url)
        require(parsed.scheme == "https" && parsed.host == host && parsed.userInfo == null &&
            parsed.port == -1) { "Destino HTTPS não permitido." }
        require(token.length in 10..4096 && !token.any(Char::isWhitespace)) {
            "Token ausente ou inválido. Use um token de acesso OAuth autorizado, não App Secret."
        }
        return (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 20_000
            readTimeout = 90_000
            instanceFollowRedirects = false
            useCaches = false
            setRequestProperty("Authorization", if (host == "rupload.facebook.com") "OAuth $token" else "Bearer $token")
            setRequestProperty("Accept", "application/json")
        }
    }

    private fun readSmall(conn: HttpURLConnection): String {
        val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
        return stream?.use { input ->
            val out = ByteArrayOutputStream()
            val block = ByteArray(4096)
            while (true) {
                val n = input.read(block)
                if (n == -1) break
                if (out.size() + n > 64_000) throw IOException("Resposta da API acima de 64 KB.")
                out.write(block, 0, n)
            }
            out.toString("UTF-8")
        }.orEmpty()
    }

    private fun json(conn: HttpURLConnection, operation: String): JSONObject {
        val status = conn.responseCode
        if (status !in 200..299) throw IOException("$operation: HTTP $status. Confira permissões, cota e autorização.")
        // Não incluir corpos de erro em logs ou mensagens: podem conter dados sensíveis.
        return JSONObject(readSmall(conn))
    }

    private fun postJson(url: String, token: String, host: String, body: JSONObject): JSONObject {
        val conn = connection(url, token, "POST", host)
        try {
            val bytes = body.toString().toByteArray(Charsets.UTF_8)
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            conn.setFixedLengthStreamingMode(bytes.size)
            conn.outputStream.use { it.write(bytes) }
            return json(conn, host)
        } finally { conn.disconnect() }
    }

    private fun form(url: String, token: String, fields: Map<String, String>): JSONObject {
        val conn = connection(url, token, "POST", "graph.facebook.com")
        try {
            val content = fields.entries.joinToString("&") {
                URLEncoder.encode(it.key, "UTF-8") + "=" + URLEncoder.encode(it.value, "UTF-8")
            }.toByteArray(Charsets.UTF_8)
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            conn.setFixedLengthStreamingMode(content.size)
            conn.outputStream.use { it.write(content) }
            return json(conn, "Facebook Página")
        } finally { conn.disconnect() }
    }

    private fun videoSize(context: Context, uri: Uri): Long {
        require(uri.scheme == "content") { "Escolha o vídeo pelo seletor do Android." }
        val columnSize = context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE),
            null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (index >= 0 && cursor.moveToFirst() && !cursor.isNull(index)) cursor.getLong(index)
            else null
        }
        val size = columnSize ?: context.contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize }
        require(size != null && size in 1..MAX_VIDEO_BYTES) {
            "O arquivo deve ter tamanho conhecido, maior que zero e até 512 MiB."
        }
        return size
    }

    private fun durationSeconds(context: Context, uri: Uri): Long? = runCatching {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()?.div(1000L)
        } finally { retriever.release() }
    }.getOrNull()

    private fun readChunk(input: java.io.InputStream, buffer: ByteArray, size: Int) {
        var at = 0
        while (at < size) {
            val n = input.read(buffer, at, size - at)
            if (n < 0) throw IOException("O vídeo foi interrompido durante a leitura.")
            if (n > 0) at += n
        }
    }

    suspend fun facebookPage(token: String): FacebookPage = withContext(Dispatchers.IO) {
        val conn = connection("$META/me?fields=id%2Cname", token, "GET", "graph.facebook.com")
        try {
            val obj = json(conn, "Consultar Página Meta")
            val id = obj.optString("id")
            require(SocialPublicationPolicy.validPageId(id)) { "O token não identificou uma Página válida." }
            FacebookPage(id, obj.optString("name").take(100))
        } finally { conn.disconnect() }
    }

    /** GET /me/accounts a partir do token de usuário Meta. Tokens retornados não são persistidos. */
    suspend fun facebookPages(userToken: String): List<FacebookPage> = withContext(Dispatchers.IO) {
        val conn = connection("$META/me/accounts?fields=id%2Cname%2Caccess_token&limit=50",
            userToken, "GET", "graph.facebook.com")
        try {
            val root = json(conn, "Consultar Páginas autorizadas")
            val records = root.optJSONArray("data") ?: error("Meta não retornou Páginas autorizadas.")
            (0 until records.length()).mapNotNull { index ->
                val item = records.optJSONObject(index) ?: return@mapNotNull null
                val id = item.optString("id")
                if (!SocialPublicationPolicy.validPageId(id)) null
                else FacebookPage(id, item.optString("name").take(100),
                    item.optString("access_token").take(4096))
            }
        } finally { conn.disconnect() }
    }

    /** Apenas Página vinculada: START -> upload binário -> FINISH. Sem retry de operações incertas. */
    suspend fun publishFacebookReel(
        context: Context, token: String, pageId: String, publication: SocialPublication,
        progress: (Long, Long) -> Unit
    ): String = withContext(Dispatchers.IO) {
        require(SocialPublicationPolicy.validPageId(pageId))
        val uri = Uri.parse(publication.videoUri)
        val size = videoSize(context, uri)
        val start = form("$META/$pageId/video_reels", token, mapOf("upload_phase" to "start"))
        val videoId = start.optString("video_id")
        require(videoId.matches(Regex("[0-9]{5,30}"))) { "Meta não forneceu um ID de vídeo válido." }
        val uploadUrl = start.optString("upload_url")
        val uploadConn = connection(uploadUrl, token, "POST", "rupload.facebook.com")
        try {
            uploadConn.doOutput = true
            uploadConn.setRequestProperty("Content-Type", "application/octet-stream")
            uploadConn.setRequestProperty("offset", "0")
            uploadConn.setRequestProperty("file_size", size.toString())
            uploadConn.setFixedLengthStreamingMode(size)
            context.contentResolver.openInputStream(uri)?.use { input ->
                val buffer = ByteArray(64 * 1024)
                var sent = 0L
                uploadConn.outputStream.use { output ->
                    while (true) {
                        val n = input.read(buffer)
                        if (n < 0) break
                        output.write(buffer, 0, n)
                        sent += n
                        progress(sent, size)
                    }
                }
                if (sent != size) throw IOException("Vídeo alterado durante o envio; confira a Página antes de repetir.")
            } ?: error("Vídeo não acessível.")
            val confirmed = json(uploadConn, "Transferência do Reel")
            require(confirmed.optBoolean("success")) { "Meta não confirmou o recebimento dos bytes." }
        } finally { uploadConn.disconnect() }
        val description = listOf(publication.description, publication.hashtags)
            .filter(String::isNotBlank).joinToString("\n\n")
        require(description.length <= 2200) { "Legenda do Facebook excede 2200 caracteres." }
        val result = form("$META/$pageId/video_reels", token, mapOf(
            "upload_phase" to "finish", "video_id" to videoId,
            "video_state" to "PUBLISHED", "description" to description,
            "title" to publication.videoName.substringBeforeLast('.').take(150)))
        require(result.optBoolean("success")) { "Meta ainda não confirmou a solicitação de publicação." }
        // FINISH aceita a solicitação, mas não garante vídeo público nem processamento concluído.
        videoId
    }

    suspend fun tikTokCreator(token: String): TikTokCreator = withContext(Dispatchers.IO) {
        val root = postJson("$TIKTOK/v2/post/publish/creator_info/query/", token,
            "open.tiktokapis.com", JSONObject())
        require(root.optJSONObject("error")?.optString("code") == "ok") {
            "TikTok não autorizou o acesso a video.publish. Confira a aprovação do aplicativo."
        }
        val obj = root.optJSONObject("data") ?: error("Informações do criador indisponíveis.")
        val options = obj.optJSONArray("privacy_level_options") ?: error("Privacidade indisponível.")
        val privacy = (0 until options.length()).map { options.optString(it) }
            .filter { it in setOf("PUBLIC_TO_EVERYONE", "MUTUAL_FOLLOW_FRIENDS", "FOLLOWER_OF_CREATOR", "SELF_ONLY") }
        require(privacy.isNotEmpty()) { "TikTok não forneceu opções válidas de privacidade." }
        val username = obj.optString("creator_username").take(80)
        require(username.isNotBlank()) { "TikTok não retornou identidade verificável do criador." }
        TikTokCreator(obj.optString("creator_nickname").take(80),
            username, privacy,
            obj.optBoolean("comment_disabled"), obj.optBoolean("duet_disabled"),
            obj.optBoolean("stitch_disabled"), obj.optInt("max_video_post_duration_sec", 0))
    }

    suspend fun publishTikTok(
        context: Context, token: String, publication: SocialPublication,
        creator: TikTokCreator, privacy: String, isAiGenerated: Boolean,
        disableComment: Boolean, disableDuet: Boolean, disableStitch: Boolean,
        progress: (Long, Long) -> Unit
    ): String = withContext(Dispatchers.IO) {
        require(privacy in creator.privacyOptions) { "Privacidade não disponibilizada pelo TikTok." }
        val uri = Uri.parse(publication.videoUri)
        val size = videoSize(context, uri)
        val duration = durationSeconds(context, uri)
        if (duration != null && creator.maxDurationSeconds > 0) require(duration <= creator.maxDurationSeconds) {
            "O vídeo excede a duração máxima informada pelo TikTok para esta conta."
        }
        val pieces = (size + CHUNK - 1) / CHUNK
        require(pieces in 1..1000) { "Vídeo excede a quantidade de blocos suportados." }
        val caption = listOf(publication.description, publication.hashtags)
            .filter(String::isNotBlank).joinToString("\n\n")
        require(caption.length <= 2200) { "Legenda do TikTok excede 2200 caracteres." }
        val payload = JSONObject()
            .put("post_info", JSONObject()
                .put("title", caption).put("privacy_level", privacy)
                .put("disable_comment", disableComment || creator.commentsDisabled)
                .put("disable_duet", disableDuet || creator.duetDisabled)
                .put("disable_stitch", disableStitch || creator.stitchDisabled)
                .put("is_aigc", isAiGenerated))
            .put("source_info", JSONObject().put("source", "FILE_UPLOAD")
                .put("video_size", size).put("chunk_size", minOf(size, CHUNK.toLong()))
                .put("total_chunk_count", pieces))
        val started = postJson("$TIKTOK/v2/post/publish/video/init/", token,
            "open.tiktokapis.com", payload)
        require(started.optJSONObject("error")?.optString("code") == "ok") {
            "TikTok recusou o início do envio. Confira permissões, privacidade e revisão do aplicativo."
        }
        val result = started.optJSONObject("data") ?: error("TikTok não retornou sessão.")
        val publishId = result.optString("publish_id")
        require(SocialPublicationPolicy.validRemoteReference(publishId)) { "ID de sessão do TikTok inválido." }
        val uploadUrl = result.optString("upload_url")
        // Não enviar o token TikTok ao host de upload; a URL assinada já autoriza o envio.
        val uploadUri = Uri.parse(uploadUrl)
        require(uploadUri.scheme == "https" && uploadUri.host == "open-upload.tiktokapis.com" &&
            uploadUri.userInfo == null && uploadUri.port == -1) { "URL de upload TikTok inválida." }
        context.contentResolver.openInputStream(uri)?.use { input ->
            val buffer = ByteArray(CHUNK)
            var offset = 0L
            repeat(pieces.toInt()) {
                val length = minOf(CHUNK.toLong(), size - offset).toInt()
                readChunk(input, buffer, length)
                val put = (URL(uploadUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "PUT"; connectTimeout = 20_000; readTimeout = 90_000
                    instanceFollowRedirects = false; doOutput = true; useCaches = false
                    setRequestProperty("Content-Type", "video/mp4")
                    setRequestProperty("Content-Range", "bytes $offset-${offset + length - 1}/$size")
                    setFixedLengthStreamingMode(length)
                }
                try {
                    put.outputStream.use { it.write(buffer, 0, length) }
                    if (put.responseCode !in 200..299)
                        throw IOException("Transferência TikTok: HTTP ${put.responseCode}. Verifique a publicação antes de repetir.")
                } finally { put.disconnect() }
                offset += length
                progress(offset, size)
            }
        } ?: error("Vídeo não acessível.")
        publishId // É ID da operação, NÃO ID de vídeo publicado.
    }

    suspend fun tikTokStatus(token: String, publishId: String): String = withContext(Dispatchers.IO) {
        require(SocialPublicationPolicy.validRemoteReference(publishId))
        val root = postJson("$TIKTOK/v2/post/publish/status/fetch/", token,
            "open.tiktokapis.com", JSONObject().put("publish_id", publishId))
        require(root.optJSONObject("error")?.optString("code") == "ok") {
            "TikTok não retornou o status da publicação." }
        root.optJSONObject("data")?.optString("status")?.take(70).orEmpty().ifBlank { "Status indisponível" }
    }
}
