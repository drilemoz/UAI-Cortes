package br.com.uaicortes

import android.content.Context
import android.net.Uri
import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Catálogo local, sem OAuth nem credenciais. Não representa login em nenhuma rede.
 * Dados não são copiados para VideoProject (schemaVersion 4 permanece intacto).
 */
internal enum class SocialDestination(val label: String, val help: String, val developerUrl: String) {
    FACEBOOK_PAGE("Facebook · Página", "Reels por API exigem token da Página, permissões e aprovação da Meta.",
        "https://developers.facebook.com/docs/video-api/guides/reels-publishing/"),
    FACEBOOK_PROFILE("Facebook · Perfil", "Compartilhamento manual; o app não publica por API em perfis pessoais.",
        "https://www.facebook.com/help/"),
    YOUTUBE("YouTube", "Upload por API depende de OAuth e autorização do canal.",
        "https://developers.google.com/youtube/v3/guides/uploading_a_video"),
    TIKTOK("TikTok", "Envio por API exige token OAuth, escopo video.publish e aprovação do TikTok.",
        "https://developers.tiktok.com/docs/en/content-posting-api-get-started"),
    KWAI("Kwai", "Publicação via compartilhamento manual; API brasileira não validada.",
        "https://www.kwai.com/");

    companion object {
        fun parse(value: String): SocialDestination? = entries.firstOrNull { it.name == value }
    }
}

internal enum class SocialPublicationStatus(val label: String) {
    PREPARED("Preparada"),
    SHARED_UNCONFIRMED("Compartilhamento aberto · confirmar postagem"),
    CONFIRMED_MANUALLY("Publicada · confirmada manualmente"),
    UPLOAD_REVIEW("Envio iniciado · verificar na plataforma"),
    CONFIRMED_BY_API("Enviada · confirmada pelo YouTube"),
    REMOTE_SUBMITTED("Recebida pela API · verifique o processamento");
    companion object {
        fun parse(value: String): SocialPublicationStatus =
            entries.firstOrNull { it.name == value } ?: PREPARED
    }
}

internal data class SocialAccount(
    val id: String = UUID.randomUUID().toString(),
    val destination: SocialDestination,
    val label: String,
    val profileUrl: String = "",
    val remoteChannelId: String = "",
    val remotePageId: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toJson() = JSONObject().put("id", id).put("destination", destination.name)
        .put("label", label).put("profileUrl", profileUrl)
        .put("remoteChannelId", remoteChannelId).put("remotePageId", remotePageId).put("createdAt", createdAt)
    companion object {
        fun fromJson(json: JSONObject): SocialAccount = SocialAccount(
            id = json.getString("id"),
            destination = requireNotNull(SocialDestination.parse(json.getString("destination"))),
            label = json.getString("label"), profileUrl = json.optString("profileUrl"),
            remoteChannelId = json.optString("remoteChannelId"),
            remotePageId = json.optString("remotePageId"), createdAt = json.optLong("createdAt", 0L)
        )
    }
}

internal data class SocialMetrics(
    val views: Long? = null, val likes: Long? = null,
    val comments: Long? = null, val shares: Long? = null,
    val measuredAt: Long = 0L, val source: String = "manual"
) {
    val hasData get() = listOf(views, likes, comments, shares).any { it != null }
    fun toJson() = JSONObject().put("views", views ?: JSONObject.NULL)
        .put("likes", likes ?: JSONObject.NULL).put("comments", comments ?: JSONObject.NULL)
        .put("shares", shares ?: JSONObject.NULL).put("measuredAt", measuredAt)
        .put("source", source)
    companion object {
        fun fromJson(json: JSONObject?): SocialMetrics = if (json == null) SocialMetrics() else SocialMetrics(
            views = json.optLongOrNull("views"), likes = json.optLongOrNull("likes"),
            comments = json.optLongOrNull("comments"), shares = json.optLongOrNull("shares"),
            measuredAt = json.optLong("measuredAt", 0L),
            source = json.optString("source", "manual")
        )
    }
}

private fun JSONObject.optLongOrNull(key: String): Long? =
    if (isNull(key) || !has(key)) null else getLong(key)

internal data class SocialPublication(
    val id: String = UUID.randomUUID().toString(),
    val accountId: String,
    val videoUri: String,
    val videoName: String,
    val description: String = "",
    val hashtags: String = "",
    val status: SocialPublicationStatus = SocialPublicationStatus.PREPARED,
    val postUrl: String = "",
    val remoteVideoId: String = "",
    val metrics: SocialMetrics = SocialMetrics(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt
) {
    fun toJson() = JSONObject().put("id", id).put("accountId", accountId)
        .put("videoUri", videoUri).put("videoName", videoName)
        .put("description", description).put("hashtags", hashtags)
        .put("status", status.name).put("postUrl", postUrl)
        .put("remoteVideoId", remoteVideoId)
        .put("metrics", metrics.toJson()).put("createdAt", createdAt).put("updatedAt", updatedAt)
    companion object {
        fun fromJson(json: JSONObject): SocialPublication = SocialPublication(
            id = json.getString("id"), accountId = json.getString("accountId"),
            videoUri = json.getString("videoUri"), videoName = json.getString("videoName"),
            description = json.optString("description"), hashtags = json.optString("hashtags"),
            status = SocialPublicationStatus.parse(json.optString("status")),
            postUrl = json.optString("postUrl"), remoteVideoId = json.optString("remoteVideoId"),
            metrics = SocialMetrics.fromJson(json.optJSONObject("metrics")),
            createdAt = json.optLong("createdAt", 0L), updatedAt = json.optLong("updatedAt", 0L)
        )
    }
}

internal data class SocialData(
    val accounts: List<SocialAccount> = emptyList(),
    val publications: List<SocialPublication> = emptyList()
)

/** Política pura de publicação assistida: nunca equiparar Intent aberta à postagem confirmada. */
internal object SocialPublicationPolicy {
    fun validChannel(id: String): Boolean =
        id.isBlank() || (id.startsWith("UC") && id.length in 20..64 &&
            id.all { it.isLetterOrDigit() || it == '_' || it == '-' })

    fun validPageId(id: String): Boolean = id.matches(Regex("[0-9]{5,30}"))

    fun validRemoteReference(id: String): Boolean =
        id.matches(Regex("[A-Za-z0-9_~.:-]{5,120}"))

    fun bindPage(account: SocialAccount, pageId: String): SocialAccount {
        require(account.destination == SocialDestination.FACEBOOK_PAGE && validPageId(pageId))
        return account.copy(remotePageId = pageId)
    }

    /** O servidor recebeu a operação, mas isso não significa vídeo já público. */
    fun markExternalSubmitted(post: SocialPublication, id: String): SocialPublication {
        require(post.status == SocialPublicationStatus.UPLOAD_REVIEW && validRemoteReference(id))
        return post.copy(status = SocialPublicationStatus.REMOTE_SUBMITTED, remoteVideoId = id,
            updatedAt = System.currentTimeMillis())
    }

    fun validVideoId(id: String): Boolean =
        id.isBlank() || Regex("[A-Za-z0-9_-]{11}").matches(id)

    /** Acesso autorizado é obtido a cada ação; só o ID público do canal fica em disco. */
    fun bindChannel(account: SocialAccount, id: String): SocialAccount {
        require(account.destination == SocialDestination.YOUTUBE && id.isNotBlank() && validChannel(id))
        return account.copy(remoteChannelId = id)
    }

    /** Gravar antes do primeiro POST: sem repetição após resposta incerta. */
    fun markUploadReview(post: SocialPublication): SocialPublication {
        require(post.status == SocialPublicationStatus.PREPARED ||
            post.status == SocialPublicationStatus.SHARED_UNCONFIRMED)
        return post.copy(status = SocialPublicationStatus.UPLOAD_REVIEW,
            updatedAt = System.currentTimeMillis())
    }

    fun confirmApiUpload(post: SocialPublication, videoId: String): SocialPublication {
        require(post.status == SocialPublicationStatus.UPLOAD_REVIEW &&
            videoId.isNotBlank() && validVideoId(videoId))
        return post.copy(status = SocialPublicationStatus.CONFIRMED_BY_API,
            remoteVideoId = videoId, postUrl = "https://www.youtube.com/watch?v=$videoId",
            updatedAt = System.currentTimeMillis())
    }

    fun isSafeHttps(url: String): Boolean {
        if (url.isBlank()) return true
        val uri = runCatching { Uri.parse(url.trim()) }.getOrNull() ?: return false
        return uri.scheme == "https" && !uri.host.isNullOrBlank() &&
            uri.userInfo == null && uri.port == -1
    }

    fun isShareableVideo(uri: String): Boolean =
        runCatching { Uri.parse(uri).scheme == "content" }.getOrDefault(false)

    fun prepare(data: SocialData, accountIds: Set<String>, videoUri: String,
                videoName: String, description: String, hashtags: String,
                now: Long = System.currentTimeMillis()): SocialData {
        require(isShareableVideo(videoUri)) { "Selecione um vídeo do armazenamento Android." }
        require(videoName.length in 1..200 && description.length <= 3000 && hashtags.length <= 1500)
        require(accountIds.isNotEmpty() && accountIds.size <= 10)
        require(accountIds.all { id -> data.accounts.any { it.id == id } })
        val output = data.publications.toMutableList()
        accountIds.forEach { id ->
            // O mesmo vídeo para a mesma conta não vira duas tarefas sem decisão do usuário.
            val existing = output.indexOfFirst { it.accountId == id && it.videoUri == videoUri }
            if (existing < 0) output.add(SocialPublication(accountId = id, videoUri = videoUri,
                videoName = videoName, description = description, hashtags = hashtags, createdAt = now))
            else if (output[existing].status == SocialPublicationStatus.PREPARED)
                output[existing] = output[existing].copy(videoName = videoName,
                    description = description, hashtags = hashtags, updatedAt = now)
        }
        return data.copy(publications = output)
    }
}

/** Escrita atômica e compatibilidade independente das bibliotecas e projetos anteriores. */
internal class SocialStore(context: Context) {
    private val directory = File(context.applicationContext.filesDir, "social_v1").apply { mkdirs() }
    private val atomic = AtomicFile(File(directory, "catalog.json"))

    @Synchronized fun load(): SocialData {
        if (!atomic.baseFile.exists() && !File(atomic.baseFile.path + ".bak").exists())
            return SocialData()
        val bytes = atomic.openRead().use { it.readBytes() }
        require(bytes.size <= 2_000_000) { "Catálogo de redes muito grande." }
        val json = JSONObject(String(bytes, Charsets.UTF_8))
        require(json.getInt("schema") == 1) { "Formato de catálogo de redes desconhecido." }
        val accounts = json.getJSONArray("accounts")
        val publications = json.getJSONArray("publications")
        require(accounts.length() <= 30 && publications.length() <= 1000)
        val result = SocialData(
            (0 until accounts.length()).map { SocialAccount.fromJson(accounts.getJSONObject(it)) },
            (0 until publications.length()).map { SocialPublication.fromJson(publications.getJSONObject(it)) }
        )
        validate(result)
        return result
    }

    private fun validate(data: SocialData) {
        require(data.accounts.size <= 30 && data.publications.size <= 1000)
        require(data.accounts.map { it.id }.distinct().size == data.accounts.size)
        require(data.publications.map { it.id }.distinct().size == data.publications.size)
        require(data.accounts.all { it.id.isNotBlank() && it.label.length in 1..70 &&
            SocialPublicationPolicy.isSafeHttps(it.profileUrl) &&
            SocialPublicationPolicy.validChannel(it.remoteChannelId) &&
            (it.remoteChannelId.isBlank() || it.destination == SocialDestination.YOUTUBE) &&
            (it.remotePageId.isBlank() ||
                (it.destination == SocialDestination.FACEBOOK_PAGE &&
                    SocialPublicationPolicy.validPageId(it.remotePageId))) })
        val ids = data.accounts.map { it.id }.toSet()
        require(data.publications.all { it.accountId in ids &&
            SocialPublicationPolicy.isShareableVideo(it.videoUri) &&
            it.videoName.length in 1..200 && it.description.length <= 3000 && it.hashtags.length <= 1500 &&
            SocialPublicationPolicy.isSafeHttps(it.postUrl) &&
            (it.remoteVideoId.isBlank() || when (it.status) {
                SocialPublicationStatus.CONFIRMED_BY_API -> SocialPublicationPolicy.validVideoId(it.remoteVideoId)
                SocialPublicationStatus.REMOTE_SUBMITTED, SocialPublicationStatus.CONFIRMED_MANUALLY ->
                    SocialPublicationPolicy.validRemoteReference(it.remoteVideoId)
                else -> false
            }) &&
            it.metrics.source in setOf("manual", "youtube_api") &&
            listOf(it.metrics.views,it.metrics.likes,it.metrics.comments,it.metrics.shares)
                .all { value -> value == null || value >= 0 } })
    }

    @Synchronized fun save(data: SocialData) {
        load() // Nunca substituir silenciosamente um catálogo corrompido.
        validate(data)
        val json = JSONObject().put("schema", 1)
            .put("accounts", JSONArray().also { a -> data.accounts.forEach { a.put(it.toJson()) } })
            .put("publications", JSONArray().also { a -> data.publications.forEach { a.put(it.toJson()) } })
        val bytes = json.toString().toByteArray(Charsets.UTF_8)
        require(bytes.size <= 2_000_000)
        var stream: FileOutputStream? = null
        try {
            stream = atomic.startWrite()
            stream.write(bytes)
            atomic.finishWrite(stream)
        } catch (error: Exception) {
            stream?.let { atomic.failWrite(it) }
            throw error
        }
    }
}
