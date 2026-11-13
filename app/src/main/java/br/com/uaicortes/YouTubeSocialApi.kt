package br.com.uaicortes

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Cliente restrito ao YouTube Data API. Recebe o token OAuth somente em memória, por ação explícita. */
internal object YouTubeSocialApi {
    internal data class Channel(val id: String, val title: String)
    private const val API = "https://www.googleapis.com/youtube/v3"
    private const val UPLOAD = "https://www.googleapis.com/upload/youtube/v3/videos?uploadType=resumable&part=snippet,status"
    private const val CHUNK = 1_048_576 // 1 MiB, múltiplo de 256 KiB exigido pela API.

    private fun connect(url: String, token: String, method: String): HttpURLConnection {
        require(url.startsWith("https://www.googleapis.com/"))
        require(token.isNotBlank())
        return (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 20_000
            readTimeout = 60_000
            instanceFollowRedirects = false
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Accept", "application/json")
            useCaches = false
        }
    }

    private fun readLimited(connection: HttpURLConnection): String {
        val stream = (if (connection.responseCode in 200..299) connection.inputStream
            else connection.errorStream) ?: return ""
        return stream.use { input ->
            val output = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(4096)
            while (output.size() <= 128_000) {
                val n = input.read(buffer)
                if (n == -1) break
                if (output.size() + n > 128_000) throw IOException("Resposta da API muito grande")
                output.write(buffer, 0, n)
            }
            output.toString(Charsets.UTF_8.name())
        }
    }

    /** Retorna somente categorias conhecidas, sem expor mensagens, tokens ou URLs da API. */
    internal fun errorDescription(http: Int, raw: String): String {
        val detail = runCatching {
            JSONObject(raw).optJSONObject("error")?.let { error ->
                // Há respostas Google com ErrorInfo.details.reason em vez de errors[].reason.
                val details = error.optJSONArray("details")
                val detailReason = (0 until (details?.length() ?: 0)).asSequence()
                    .mapNotNull { details?.optJSONObject(it)?.optString("reason") }
                    .firstOrNull { it.isNotBlank() }.orEmpty()
                detailReason.ifBlank {
                    error.optJSONArray("errors")?.optJSONObject(0)?.optString("reason").orEmpty()
                }.ifBlank { error.optString("status") }
            }.orEmpty()
        }.getOrDefault("")
        return when (http) {
            401 -> "HTTP 401: autorização expirada ou conta Google sem acesso ao canal. Autorize novamente."
            403 -> when (detail) {
                "quotaExceeded", "dailyLimitExceeded", "rateLimitExceeded", "userRateLimitExceeded", "RESOURCE_EXHAUSTED" ->
                    "HTTP 403: cota ou limite de consultas do YouTube excedido neste projeto Google Cloud."
                "accessNotConfigured", "SERVICE_DISABLED", "accessNotConfiguredException" ->
                    "HTTP 403: YouTube Data API v3 desativada ou não habilitada neste projeto Google Cloud."
                "insufficientPermissions", "PERMISSION_DENIED", "channelForbidden", "forbidden", "accountDelegationForbidden" ->
                    "HTTP 403: acesso não permitido. Confira as permissões OAuth e o canal escolhido."
                "authenticatedUserNotChannel" ->
                    "HTTP 403: a conta Google autorizada não possui um canal YouTube acessível pela API."
                else -> "HTTP 403: acesso negado pelo YouTube (motivo não identificado). Confira API ativada, permissões e cotas no Google Cloud."
            }
            429 -> "HTTP 429: muitas solicitações ao YouTube. Tente mais tarde."
            else -> "HTTP $http: o YouTube não concluiu a operação."
        }
    }

    private fun ensureOk(connection: HttpURLConnection, operation: String) {
        val code = connection.responseCode
        DiagnosticRecorder.api("youtube", operation.lowercase().replace(' ', '_'), null, code,
            null, outcome=if (code in 200..299) "ok" else "http_error")
        if (code !in 200..299) {
            val description = errorDescription(code, runCatching { readLimited(connection) }.getOrDefault(""))
            throw IOException("$operation: $description")
        }
    }

    suspend fun channel(token: String): Channel = withContext(Dispatchers.IO) {
        val conn = connect("$API/channels?part=id%2Csnippet&mine=true", token, "GET")
        try {
            ensureOk(conn, "Canal YouTube")
            val json = JSONObject(readLimited(conn))
            val first = json.optJSONArray("items")?.optJSONObject(0)
                ?: throw IOException("Nenhum canal YouTube acessível nesta conta Google.")
            val id = first.optString("id")
            if (!SocialPublicationPolicy.validChannel(id) || id.isBlank())
                throw IOException("A API não retornou um ID de canal válido.")
            Channel(id, first.optJSONObject("snippet")?.optString("title")?.take(100).orEmpty())
        } finally { conn.disconnect() }
    }

    /** Página de vídeos já presentes no canal: consulta manual, sem download ou importação local. */
    internal data class PreviousVideo(
        val id: String,
        val title: String,
        val publishedAt: String,
        val viewCount: Long? = null,
        val channelId: String = "",
        val channelTitle: String = "",
        val fromUai: Boolean = false,
        val thumbnailUrl: String = ""
    )
    internal data class ChannelStatistics(
        val channelId: String,
        val views: Long?,
        val videos: Long?,
        val subscribers: Long?,
        val checkedAt: Long = System.currentTimeMillis()
    )
    internal data class PreviousPage(
        val videos: List<PreviousVideo>,
        val nextPageToken: String,
        val channelStatistics: ChannelStatistics? = null
    )

    /** Não usa search.list: a playlist de uploads permite paginação barata e inclui uploads antigos. */
    suspend fun previousVideos(
        token: String, expectedChannelId: String, pageToken: String = ""
    ): PreviousPage = withContext(Dispatchers.IO) {
        require(SocialPublicationPolicy.validChannel(expectedChannelId) && expectedChannelId.isNotBlank())
        require(pageToken.length <= 1024 && pageToken.none { it.isISOControl() })
        // Conferir o proprietário a CADA busca, inclusive em páginas posteriores.
        val channelConnection = connect("$API/channels?part=id%2CcontentDetails%2Cstatistics&mine=true" +
            "&fields=items(id%2CcontentDetails%2FrelatedPlaylists%2Fuploads%2C" +
            "statistics(viewCount%2CvideoCount%2CsubscriberCount%2ChiddenSubscriberCount))", token, "GET")
        val (uploadsId, channelStatistics) = try {
            ensureOk(channelConnection, "Canal YouTube")
            val current = JSONObject(readLimited(channelConnection)).optJSONArray("items")
                ?.optJSONObject(0) ?: throw IOException("Nenhum canal encontrado para esta conta Google.")
            if (current.optString("id") != expectedChannelId)
                throw IOException("Conta Google diferente do canal selecionado. Escolha a conta correta.")
            val uploads = current.optJSONObject("contentDetails")?.optJSONObject("relatedPlaylists")
                ?.optString("uploads").orEmpty().takeIf { it.isNotBlank() }
                ?: throw IOException("A playlist de envios do canal não está disponível.")
            val stats = current.optJSONObject("statistics")
            fun nonnegative(key: String): Long? = stats?.optString(key)
                ?.toLongOrNull()?.takeIf { it >= 0L }
            val subscribers = if (stats?.optBoolean("hiddenSubscriberCount") == true) null
                else nonnegative("subscriberCount")
            uploads to ChannelStatistics(expectedChannelId, nonnegative("viewCount"),
                nonnegative("videoCount"), subscribers)
        } finally { channelConnection.disconnect() }

        val idParam = java.net.URLEncoder.encode(uploadsId, "UTF-8")
        val pageParam = if (pageToken.isBlank()) "" else
            "&pageToken=" + java.net.URLEncoder.encode(pageToken, "UTF-8")
        val playlistConnection = connect(
            "$API/playlistItems?part=snippet%2CcontentDetails&playlistId=$idParam&maxResults=20$pageParam" +
                "&fields=nextPageToken%2Citems(contentDetails(videoId%2CvideoPublishedAt)%2C" +
                "snippet(title%2CpublishedAt%2CresourceId%2FvideoId%2Cthumbnails%2Fmedium%2Furl%2Cthumbnails%2Fdefault%2Furl))",
            token, "GET")
        try {
            ensureOk(playlistConnection, "Vídeos anteriores do YouTube")
            val body = JSONObject(readLimited(playlistConnection))
            val items = body.optJSONArray("items")
            val videos = buildList {
                for (index in 0 until (items?.length() ?: 0)) {
                    val entry = items?.optJSONObject(index) ?: continue
                    val snippet = entry.optJSONObject("snippet")
                    val details = entry.optJSONObject("contentDetails")
                    val id = details?.optString("videoId").orEmpty().ifBlank {
                        snippet?.optJSONObject("resourceId")?.optString("videoId").orEmpty()
                    }
                    if (!SocialPublicationPolicy.validVideoId(id) || id.isBlank()) continue
                    val title = snippet?.optString("title").orEmpty().trim().take(160)
                        .ifBlank { "Vídeo sem título" }
                    val date = details?.optString("videoPublishedAt").orEmpty()
                        .ifBlank { snippet?.optString("publishedAt").orEmpty() }
                    val thumbs = snippet?.optJSONObject("thumbnails")
                    val thumbnail = thumbs?.optJSONObject("medium")?.optString("url")
                        .orEmpty().ifBlank { thumbs?.optJSONObject("default")?.optString("url").orEmpty() }
                    add(PreviousVideo(id = id, title = title, publishedAt = date.take(40),
                        thumbnailUrl = SocialYoutubeThumbnailPolicy.safeUrl(id, thumbnail)))
                }
            }.distinctBy { it.id }
            val withStats = if (videos.isEmpty()) emptyList() else enrichPreviousVideos(token, videos)
            PreviousPage(withStats, body.optString("nextPageToken").take(1024), channelStatistics)
        } finally { playlistConnection.disconnect() }
    }


    private suspend fun enrichPreviousVideos(
        token: String,
        videos: List<PreviousVideo>
    ): List<PreviousVideo> = withContext(Dispatchers.IO) {
        val ids = videos.map { it.id }.filter { SocialPublicationPolicy.validVideoId(it) }.distinct()
        if (ids.isEmpty()) return@withContext videos
        val idParam = java.net.URLEncoder.encode(ids.joinToString(","), "UTF-8")
        val conn = connect(
            "$API/videos?part=snippet%2Cstatistics&id=$idParam&fields=items(id%2Csnippet(channelId%2CchannelTitle)%2Cstatistics(viewCount))",
            token,
            "GET"
        )
        try {
            ensureOk(conn, "Estatísticas de vídeos YouTube")
            val statsItems = JSONObject(readLimited(conn)).optJSONArray("items")
            val statsById = buildMap<String, Pair<Long?, Pair<String, String>>> {
                for (index in 0 until (statsItems?.length() ?: 0)) {
                    val item = statsItems?.optJSONObject(index) ?: continue
                    val id = item.optString("id")
                    if (!SocialPublicationPolicy.validVideoId(id) || id.isBlank()) continue
                    val snippet = item.optJSONObject("snippet")
                    val statistics = item.optJSONObject("statistics")
                    val viewCount = statistics?.optString("viewCount")?.toLongOrNull()?.takeIf { it >= 0L }
                    put(id, viewCount to Pair(
                        snippet?.optString("channelId").orEmpty(),
                        snippet?.optString("channelTitle").orEmpty().take(100)
                    ))
                }
            }
            videos.map { video ->
                val stat = statsById[video.id]
                video.copy(
                    viewCount = stat?.first,
                    channelId = stat?.second?.first.orEmpty(),
                    channelTitle = stat?.second?.second.orEmpty()
                )
            }
        } catch (_: Exception) {
            videos
        } finally { conn.disconnect() }
    }

    suspend fun metrics(token: String, videoId: String): SocialMetrics = withContext(Dispatchers.IO) {
        require(SocialPublicationPolicy.validVideoId(videoId) && videoId.isNotBlank())
        val conn = connect("$API/videos?part=statistics&id=$videoId", token, "GET")
        try {
            ensureOk(conn, "Métricas YouTube")
            val stats = JSONObject(readLimited(conn)).optJSONArray("items")?.optJSONObject(0)
                ?.optJSONObject("statistics") ?: throw IOException("Estatísticas indisponíveis para este vídeo.")
            fun number(key: String): Long? = stats.optString(key).toLongOrNull()?.takeIf { it >= 0L }
            SocialMetrics(views = number("viewCount"), likes = number("likeCount"),
                comments = number("commentCount"), measuredAt = System.currentTimeMillis(),
                source = "youtube_api")
        } finally { conn.disconnect() }
    }

    /** Consulta explícita ao vídeo do próprio canal; exige OAuth e não publica nem reenvia. */
    suspend fun videoState(token: String, videoId: String): SocialYouTubeState = withContext(Dispatchers.IO) {
        require(videoId.isNotBlank() && SocialPublicationPolicy.validVideoId(videoId))
        val conn = connect("$API/videos?part=status%2CprocessingDetails&id=$videoId", token, "GET")
        try {
            ensureOk(conn, "Status do vídeo YouTube")
            val video = JSONObject(readLimited(conn)).optJSONArray("items")?.optJSONObject(0)
                ?: throw IOException("Vídeo indisponível na conta Google autorizada. Confira no YouTube Studio.")
            require(video.optString("id") == videoId) { "Vídeo retornado não corresponde ao solicitado." }
            val status = video.optJSONObject("status")
            val processing = video.optJSONObject("processingDetails")
            SocialYouTubeState.fromJson(JSONObject()
                .put("privacy", status?.optString("privacyStatus").orEmpty())
                .put("upload", status?.optString("uploadStatus").orEmpty())
                .put("processing", processing?.optString("processingStatus").orEmpty())
                .put("checkedAt", System.currentTimeMillis()))
        } finally { conn.disconnect() }
    }

    private fun sizeOf(context: Context, uri: Uri): Long {
        val fromColumn = context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE),
            null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getColumnIndex(OpenableColumns.SIZE).takeIf { it >= 0 }
                ?.let { index -> if (c.isNull(index)) null else c.getLong(index) }
            else null
        }
        return fromColumn ?: context.contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: -1L
    }

    /** Pré-checagem antes de marcar UPLOAD_REVIEW: evita bloquear reenvio por arquivo ausente. */
    internal fun uploadSize(context: Context, uri: Uri): Long {
        val size = sizeOf(context, uri)
        require(size > 0L) {
            "O vídeo precisa ter tamanho conhecido e maior que zero para envio."
        }
        return size
    }

    private fun readExactly(input: java.io.InputStream, buffer: ByteArray, length: Int) {
        var at = 0
        while (at < length) {
            val n = input.read(buffer, at, length - at)
            if (n < 0) throw IOException("O arquivo terminou antes do tamanho informado.")
            if (n == 0) continue
            at += n
        }
    }

    /**
     * Inicia uma sessão recuperável após confirmação do usuário. Não faz retry automático
     * após falha de rede: o chamador mantém UPLOAD_REVIEW, para evitar vídeos duplicados.
     */
    suspend fun upload(context: Context, token: String, publication: SocialPublication,
                       privacy: String, progress: (Long, Long) -> Unit): String = withContext(Dispatchers.IO) {
        require(privacy in setOf("private", "unlisted", "public"))
        require(SocialPublicationPolicy.isShareableVideo(publication.videoUri))
        val uri = Uri.parse(publication.videoUri)
        val size = uploadSize(context, uri)
        val name = publication.title.trim().take(100).ifBlank { suggestedVideoTitle(publication.videoName) }
        val description = listOf(publication.description.trim(), publication.hashtags.trim())
            .filter { it.isNotEmpty() }.joinToString("\n\n").take(4800)
        val body = JSONObject().put("snippet", JSONObject().put("title", name)
            .put("description", description).put("categoryId", "22"))
            .put("status", JSONObject().put("privacyStatus", privacy)).toString()
            .toByteArray(Charsets.UTF_8)
        val setup = connect(UPLOAD, token, "POST")
        val session: String
        try {
            setup.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            setup.setRequestProperty("X-Upload-Content-Type", "video/mp4")
            setup.setRequestProperty("X-Upload-Content-Length", size.toString())
            setup.doOutput = true
            setup.setFixedLengthStreamingMode(body.size)
            setup.outputStream.use { it.write(body) }
            ensureOk(setup, "Iniciar upload YouTube")
            session = setup.getHeaderField("Location")
                ?: throw IOException("A API não devolveu a sessão de upload.")
            val address = Uri.parse(session)
            require(address.scheme == "https" && address.host == "www.googleapis.com" &&
                address.path == "/upload/youtube/v3/videos") { "Sessão de upload inválida." }
        } finally { setup.disconnect() }

        context.contentResolver.openInputStream(uri)?.use { input ->
            val buffer = ByteArray(CHUNK)
            var uploaded = 0L
            while (uploaded < size) {
                val count = minOf(CHUNK.toLong(), size - uploaded).toInt()
                readExactly(input, buffer, count)
                val put = connect(session, token, "PUT")
                try {
                    put.doOutput = true
                    put.setRequestProperty("Content-Type", "video/mp4")
                    put.setRequestProperty("Content-Range", "bytes $uploaded-${uploaded + count - 1}/$size")
                    put.setFixedLengthStreamingMode(count)
                    put.outputStream.use { it.write(buffer, 0, count) }
                    val code = put.responseCode
                    DiagnosticRecorder.api("youtube", "upload_chunk", null, code,
                        null, outcome=if (code == 308 || code in 200..299) "ok" else "http_error")
                    if (code == 308) {
                        val reported = put.getHeaderField("Range")
                            ?.let { Regex("bytes=0-(\\d+)").matchEntire(it) }?.groupValues?.get(1)
                            ?.toLongOrNull()?.plus(1L) ?: 0L
                        // Nunca presumir aceitação dos bytes não confirmados.
                        if (reported != uploaded + count.toLong())
                            throw IOException("Envio interrompido. Revise o status antes de tentar novamente.")
                        uploaded = reported
                        progress(uploaded, size)
                    } else if (code == 200 || code == 201) {
                        val result = JSONObject(readLimited(put)).optString("id")
                        if (!SocialPublicationPolicy.validVideoId(result) || result.isBlank())
                            throw IOException("O servidor não confirmou o ID do vídeo. Confira o YouTube Studio.")
                        progress(size, size)
                        return@withContext result
                    } else {
                        throw IOException("Upload YouTube: ${errorDescription(code, runCatching { readLimited(put) }.getOrDefault(""))} Consulte o YouTube Studio antes de repetir.")
                    }
                } finally { put.disconnect() }
            }
        } ?: throw IOException("Vídeo não está mais acessível no armazenamento.")
        throw IOException("O envio terminou sem confirmação da API. Confira o YouTube Studio.")
    }
}
