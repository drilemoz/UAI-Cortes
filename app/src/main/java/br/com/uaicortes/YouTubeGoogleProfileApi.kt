package br.com.uaicortes

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** E-mail verificado da conta que concedeu o OAuth; nunca gravar nem registrar o token. */
internal object YouTubeGoogleProfileApi {
    private const val USERINFO = "https://www.googleapis.com/oauth2/v3/userinfo"

    /** Interpretar somente o perfil recebido do endpoint Google, nunca da interface local. */
    internal fun parseVerifiedEmail(raw: String): String {
        val json = JSONObject(raw)
        val email = YouTubeGoogleAccountEmail.normalized(json.optString("email"))
        if (!json.optBoolean("email_verified", false) ||
            !YouTubeGoogleAccountEmail.valid(email))
            throw IOException("A conta Google não retornou um e-mail verificado.")
        return email
    }
    suspend fun verifiedEmail(accessToken: String): String = withContext(Dispatchers.IO) {
        require(accessToken.isNotBlank())
        val connection = (URL(USERINFO).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 15_000
            instanceFollowRedirects = false
            useCaches = false
            setRequestProperty("Authorization", "Bearer $accessToken")
            setRequestProperty("Accept", "application/json")
        }
        try {
            if (connection.responseCode != 200)
                throw IOException("Não foi possível identificar a conta Google autorizada.")
            val raw = connection.inputStream.use { input ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(1_024)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    if (output.size() + count > 4_096)
                        throw IOException("Resposta de conta Google inválida.")
                    output.write(buffer, 0, count)
                }
                output.toString(Charsets.UTF_8.name())
            }
            parseVerifiedEmail(raw)
        } finally { connection.disconnect() }
    }
}
