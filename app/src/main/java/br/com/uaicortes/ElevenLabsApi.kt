package br.com.uaicortes

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

internal data class ElevenLabsAccount(val tier: String?, val remainingCredits: Long?)
internal data class ElevenLabsVoice(val id: String, val name: String, val category: String?,
    val language: String?)
internal data class ElevenLabsVoicePage(val voices: List<ElevenLabsVoice>,
    val nextPageToken: String?)

/** HTTP limitado a api.elevenlabs.io, sem redirects, retries de POST ou logging de chave/texto. */
internal object ElevenLabsApi {
    private const val HOST = "https://api.elevenlabs.io"

    internal fun statusMessage(code: Int): String = when (code) {
        401 -> "Chave inválida ou expirada (HTTP 401)."
        403 -> "Acesso negado (HTTP 403). Confira permissões da chave e restrições de IP."
        404 -> "Recurso não encontrado (HTTP 404). Confira a voz selecionada."
        422 -> "Solicitação não aceita (HTTP 422). Confira texto, voz e modelo."
        429 -> "Limite de requisições ou créditos atingido (HTTP 429)."
        in 500..599 -> "Serviço ElevenLabs indisponível ou instável (HTTP $code)."
        else -> "A ElevenLabs recusou a operação (HTTP $code)."
    }

    internal fun parseAccount(raw: String): ElevenLabsAccount {
        val user = JSONObject(raw)
        require(user.optString("user_id").isNotBlank()) { "Resposta de conta inválida." }
        val subscription = user.optJSONObject("subscription")
        val limit = subscription?.optLong("character_limit", -1L) ?: -1L
        val used = subscription?.optLong("character_count", -1L) ?: -1L
        // O endpoint pode retornar contagens em créditos/unidades diferentes conforme o plano.
        val remaining = if (limit >= 0 && used >= 0) (limit - used).coerceAtLeast(0) else null
        return ElevenLabsAccount(subscription?.optString("tier")?.takeIf { it.isNotBlank() }, remaining)
    }

    internal fun parseVoices(raw: String): ElevenLabsVoicePage {
        val json = JSONObject(raw)
        val array = json.optJSONArray("voices") ?: throw IOException("Lista de vozes inesperada.")
        val voices = buildList {
            for (i in 0 until array.length()) {
                val entry = array.optJSONObject(i) ?: continue
                val id = entry.optString("voice_id").trim()
                val name = entry.optString("name").trim()
                if (id.isBlank() || name.isBlank()) continue
                val labels = entry.optJSONObject("labels")
                add(ElevenLabsVoice(id, name, entry.optString("category").takeIf { it.isNotBlank() },
                    labels?.optString("language")?.takeIf { it.isNotBlank() }))
            }
        }
        val next = if (json.optBoolean("has_more", false)) {
            json.optString("next_page_token").takeIf { it.isNotBlank() && it.length <= 1024 }
        } else null
        return ElevenLabsVoicePage(voices, next)
    }

    private fun response(conn: HttpURLConnection, maxBytes: Int): ByteArray {
        val code = conn.responseCode
        if (code !in 200..299) throw IOException(statusMessage(code))
        return conn.inputStream.use { input ->
            val out = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                if (out.size() + count > maxBytes) throw IOException("Resposta da ElevenLabs excede o limite de segurança.")
                out.write(buffer, 0, count)
            }
            out.toByteArray()
        }
    }

    private fun connection(path: String, key: String, method: String): HttpURLConnection {
        require(path.startsWith("/") && !path.contains("//") && !path.contains('\r') &&
            !path.contains('\n')) { "Rota inválida." }
        return (URL(HOST + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            instanceFollowRedirects = false
            connectTimeout = 12_000
            readTimeout = 30_000
            useCaches = false
            setRequestProperty("xi-api-key", ElevenLabsKeyPolicy.validate(key))
            setRequestProperty("Accept", if (method == "POST") "audio/mpeg" else "application/json")
        }
    }

    suspend fun account(key: String): ElevenLabsAccount = withContext(Dispatchers.IO) {
        val conn = connection("/v1/user", key, "GET")
        try { parseAccount(String(response(conn, 512_000), Charsets.UTF_8)) }
        finally { conn.disconnect() }
    }

    suspend fun voices(key: String, token: String? = null): ElevenLabsVoicePage = withContext(Dispatchers.IO) {
        val safeToken = token?.let {
            require(it.isNotBlank() && it.length <= 1024 && it.none { char -> char.isISOControl() }) {
                "Página de vozes inválida."
            }
            java.net.URLEncoder.encode(it, "UTF-8")
        }
        val path = "/v2/voices?page_size=30" + (safeToken?.let { "&next_page_token=$it" } ?: "")
        val conn = connection(path, key, "GET")
        try { parseVoices(String(response(conn, 1024_000), Charsets.UTF_8)) }
        finally { conn.disconnect() }
    }

    /** Este POST pode consumir créditos. Chamado somente após confirmação explícita na interface. */
    suspend fun synthesize(key: String, voiceId: String, text: String): ByteArray = withContext(Dispatchers.IO) {
        require(voiceId.matches(Regex("[A-Za-z0-9_-]{6,128}"))) { "Voz ElevenLabs inválida." }
        require(text.isNotBlank() && text.length <= 500) { "Digite até 500 caracteres." }
        val conn = connection("/v1/text-to-speech/$voiceId?output_format=mp3_44100_128", key, "POST")
        try {
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            val payload = JSONObject().put("text", text).put("model_id", "eleven_multilingual_v2")
                .toString().toByteArray(Charsets.UTF_8)
            conn.setFixedLengthStreamingMode(payload.size)
            conn.outputStream.use { it.write(payload) }
            val audio = response(conn, 8 * 1024 * 1024)
            require(audio.size >= 100) { "A ElevenLabs devolveu áudio vazio ou incompleto." }
            val contentType = conn.contentType.orEmpty().lowercase()
            require(contentType.startsWith("audio/") || contentType.startsWith("application/octet-stream")) {
                "O servidor não retornou conteúdo de áudio."
            }
            audio
        } finally { conn.disconnect() }
    }
}
