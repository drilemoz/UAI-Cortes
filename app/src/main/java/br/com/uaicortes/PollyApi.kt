package br.com.uaicortes

import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/** Signer AWS Signature V4 para host fixo Amazon Polly; sem SDK e sem redirecionamento. */
internal object PollySigV4 {
    private fun sha(data: ByteArray): ByteArray = MessageDigest.getInstance("SHA-256").digest(data)
    private fun hex(bytes: ByteArray): String = bytes.joinToString("") { "%02x".format(it.toInt() and 255) }
    private fun hmac(key: ByteArray, data: String): ByteArray = Mac.getInstance("HmacSHA256").run {
        init(SecretKeySpec(key, "HmacSHA256")); doFinal(data.toByteArray(StandardCharsets.UTF_8))
    }
    /** Codificação RFC 3986 exigida pela AWS, evitando '+' para espaços. */
    fun encode(s: String): String = buildString {
        for (b in s.toByteArray(Charsets.UTF_8)) {
            val n = b.toInt() and 255
            if (n in 65..90 || n in 97..122 || n in 48..57 || n == 45 || n == 46 || n == 95 || n == 126)
                append(n.toChar()) else append("%%%02X".format(n))
        }
    }
    fun signedHeaders(credentials: PollyCredentials, method: String, path: String, query: String,
        payload: ByteArray, now: Instant = Instant.now(), contentType: String? = null): Map<String, String> {
        val c = credentials.validated()
        require(method in setOf("GET", "POST") && path in setOf("/v1/voices", "/v1/speech"))
        val host = "polly.${c.region}.amazonaws.com"
        val stamp = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC).format(now)
        val day = stamp.substring(0, 8)
        val headers = sortedMapOf("host" to host, "x-amz-content-sha256" to hex(sha(payload)),
            "x-amz-date" to stamp)
        if (contentType != null) headers["content-type"] = contentType
        if (c.sessionToken.isNotEmpty()) headers["x-amz-security-token"] = c.sessionToken
        val names = headers.keys.joinToString(";")
        // O canonical request exige uma linha vazia após os cabeçalhos ordenados.
        val canonicalRequest = "$method\n$path\n$query\n" +
            headers.entries.joinToString("") { "${it.key}:${it.value.trim()}\n" } +
            "\n$names\n${hex(sha(payload))}"
        val scope = "$day/${c.region}/polly/aws4_request"
        val toSign = "AWS4-HMAC-SHA256\n$stamp\n$scope\n${hex(sha(canonicalRequest.toByteArray(Charsets.UTF_8)))}"
        val kDay = hmac(("AWS4" + c.secretAccessKey).toByteArray(Charsets.UTF_8), day)
        val kRegion = hmac(kDay, c.region)
        val kService = hmac(kRegion, "polly")
        val signingKey = hmac(kService, "aws4_request")
        val signature = hex(hmac(signingKey, toSign))
        return headers + ("authorization" to "AWS4-HMAC-SHA256 Credential=${c.accessKeyId}/$scope," +
            " SignedHeaders=$names, Signature=$signature")
    }
}

internal data class PollyVoice(val id: String, val name: String, val language: String,
    val engines: List<String>)
internal data class PollyAudio(val audio: ByteArray, val requestCharacters: Int?,
    val engine: String, val voiceId: String)

internal object PollyApi {
    private const val RESPONSE_LIMIT = 512_000
    private const val AUDIO_LIMIT = 5_000_000
    private fun connection(c: PollyCredentials, method: String, path: String,
        query: String = "", payload: ByteArray = byteArrayOf(), contentType: String? = null): HttpURLConnection {
        val creds = c.validated()
        val host = "polly.${creds.region}.amazonaws.com"
        val url = URL("https://$host$path" + if (query.isEmpty()) "" else "?$query")
        val headers = PollySigV4.signedHeaders(creds, method, path, query, payload, contentType = contentType)
        return (url.openConnection() as HttpURLConnection).apply {
            requestMethod = method
            instanceFollowRedirects = false
            connectTimeout = 20_000
            readTimeout = 50_000
            useCaches = false
            for ((k, v) in headers) setRequestProperty(k, v)
            if (method == "POST") {
                doOutput = true
                setFixedLengthStreamingMode(payload.size)
                outputStream.use { it.write(payload) }
            }
        }
    }
    private fun readLimited(conn: HttpURLConnection, max: Int): ByteArray {
        return conn.inputStream.use { input ->
            val output = ByteArrayOutputStream()
            val buf = ByteArray(8192)
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                if (n + output.size() > max) throw IOException("Resposta do Polly excede o limite de segurança.")
                output.write(buf, 0, n)
            }
            output.toByteArray()
        }
    }
    private fun checked(conn: HttpURLConnection, action: String): HttpURLConnection {
        val status = conn.responseCode
        if (status !in 200..299) {
            conn.disconnect()
            throw IOException("$action: HTTP $status. Verifique chaves, região, permissões IAM e cotas AWS.")
        }
        return conn
    }
    suspend fun voices(credentials: PollyCredentials): List<PollyVoice> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val voices = linkedMapOf<String, PollyVoice>()
        var token: String? = null
        repeat(8) {
            val params = sortedMapOf("IncludeAdditionalLanguageCodes" to "yes", "LanguageCode" to "pt-BR")
            if (!token.isNullOrBlank()) params["NextToken"] = token!!
            val query = params.entries.joinToString("&") { "${PollySigV4.encode(it.key)}=${PollySigV4.encode(it.value)}" }
            val conn = connection(credentials, "GET", "/v1/voices", query)
            val root = try {
                checked(conn, "Consultar vozes")
                JSONObject(String(readLimited(conn, RESPONSE_LIMIT), Charsets.UTF_8))
            } finally { conn.disconnect() }
            val list = root.optJSONArray("Voices")
            for (i in 0 until (list?.length() ?: 0)) {
                val item = list?.optJSONObject(i) ?: continue
                val id = item.optString("Id")
                val engines = item.optJSONArray("SupportedEngines")?.let { arr ->
                    (0 until arr.length()).map { arr.optString(it) }.filter { PollyCost.enginePrice(it) != null }
                } ?: emptyList()
                if (id.matches(Regex("[A-Za-z0-9_-]{1,100}")) && engines.isNotEmpty())
                    voices[id] = PollyVoice(id, item.optString("Name", id), item.optString("LanguageCode"), engines)
            }
            token = root.optString("NextToken").takeIf { it.isNotEmpty() }
            if (token == null) return@withContext voices.values.sortedBy { it.name }
        }
        voices.values.sortedBy { it.name }
    }
    suspend fun synthesize(credentials: PollyCredentials, text: String, voice: PollyVoice, engine: String): PollyAudio =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            require(text.isNotBlank() && text.length <= 1000 && !text.any { it == '\u0000' })
            require(engine in voice.engines && PollyCost.enginePrice(engine) != null) { "Mecanismo incompatível com a voz." }
            val body = JSONObject().put("Engine", engine).put("VoiceId", voice.id)
                .put("Text", text).put("TextType", "text").put("OutputFormat", "mp3")
            val bytes = body.toString().toByteArray(Charsets.UTF_8)
            val conn = connection(credentials, "POST", "/v1/speech", payload = bytes,
                contentType = "application/json")
            try {
                checked(conn, "Gerar voz")
                require(conn.contentType.orEmpty().startsWith("audio/mpeg")) { "Resposta sem áudio MP3." }
                val header = conn.getHeaderField("x-amzn-RequestCharacters")
                val chars = header?.toIntOrNull()?.takeIf { it in 1..5000 }
                val audio = readLimited(conn, AUDIO_LIMIT)
                require(audio.size > 64) { "Áudio vazio ou inválido." }
                PollyAudio(audio, chars, engine, voice.id)
            } finally { conn.disconnect() }
        }
}
