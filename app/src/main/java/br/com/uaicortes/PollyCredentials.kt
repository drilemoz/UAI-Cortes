package br.com.uaicortes

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec

/** Chaves fornecidas pelo dono do aparelho. Nunca presentes no APK, logs ou backups Android. */
internal data class PollyCredentials(
    val accessKeyId: String, val secretAccessKey: String, val region: String,
    val sessionToken: String = ""
) {
    fun validated(): PollyCredentials {
        val id = accessKeyId.trim()
        val secret = secretAccessKey.trim()
        val location = region.trim().lowercase()
        require(id.matches(Regex("[A-Z0-9]{16,128}"))) { "Access Key ID inválido." }
        require(secret.length in 20..256 && !secret.any(Char::isWhitespace)) { "Secret Access Key inválido." }
        require(location.matches(Regex("[a-z]{2}(?:-[a-z0-9]+)+-[0-9]")) && location.length <= 30) {
            "Região inválida, por exemplo us-east-1." }
        require(sessionToken.length <= 8192 && !sessionToken.any { it == '\r' || it == '\n' }) {
            "Token temporário inválido." }
        return copy(accessKeyId = id, secretAccessKey = secret, region = location,
            sessionToken = sessionToken.trim())
    }
}

/** Compartilhamento de texto intencional: contém segredos e não deve ser enviado ao GitHub. */
internal object PollyCredentialCodec {
    fun encode(value: PollyCredentials): String = value.validated().let {
        JSONObject().put("schema", 1).put("provider", "amazon-polly")
            .put("AWS_ACCESS_KEY_ID", it.accessKeyId)
            .put("AWS_SECRET_ACCESS_KEY", it.secretAccessKey)
            .put("AWS_REGION", it.region)
            .put("AWS_SESSION_TOKEN", it.sessionToken).toString(2)
    }
    fun decode(raw: String): PollyCredentials {
        require(raw.length <= 16_384) { "Arquivo muito grande." }
        val entries: Map<String, String> = if (raw.trimStart().startsWith("{")) {
            val obj = JSONObject(raw)
            require(obj.optString("provider", "amazon-polly") == "amazon-polly") { "Provedor incompatível." }
            listOf("AWS_ACCESS_KEY_ID", "AWS_SECRET_ACCESS_KEY", "AWS_REGION", "AWS_SESSION_TOKEN")
                .associateWith { obj.optString(it) }
        } else raw.lines().mapNotNull { line ->
            val s = line.trim().removePrefix("export ")
            if (s.startsWith("#") || s.isEmpty() || '=' !in s) null else {
                val pair = s.split('=', limit = 2)
                pair[0].trim() to pair[1].trim().trim('"', '\'')
            }
        }.toMap()
        return PollyCredentials(entries["AWS_ACCESS_KEY_ID"].orEmpty(),
            entries["AWS_SECRET_ACCESS_KEY"].orEmpty(), entries["AWS_REGION"].orEmpty(),
            entries["AWS_SESSION_TOKEN"].orEmpty()).validated()
    }
}

/** AES-GCM com alias diferente das chaves Gemini; segredo não passa por SharedPreferences em claro. */
internal class PollyCredentialStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("polly_secrets_local", Context.MODE_PRIVATE)
    private val alias = "uaicortes_polly_aes_v1"

    private fun key(): java.security.Key {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        ks.getKey(alias, null)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256).build())
        }.generateKey()
    }

    @Synchronized fun hasCredentials() = prefs.contains("sealed")
    @Synchronized fun read(): PollyCredentials? {
        val blob = prefs.getString("sealed", null) ?: return null
        try {
            val data = Base64.decode(blob, Base64.NO_WRAP)
            require(data.size > 28)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, data.copyOfRange(0, 12)))
            return PollyCredentialCodec.decode(String(cipher.doFinal(data.copyOfRange(12, data.size)), Charsets.UTF_8))
        } catch (_: Exception) {
            throw IllegalStateException("Credenciais Polly protegidas indisponíveis neste aparelho.")
        }
    }
    @Synchronized fun save(value: PollyCredentials) {
        val payload = PollyCredentialCodec.encode(value).toByteArray(Charsets.UTF_8)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val blob = Base64.encodeToString(cipher.iv + cipher.doFinal(payload), Base64.NO_WRAP)
        check(prefs.edit().putString("sealed", blob).commit()) { "Não foi possível salvar as credenciais." }
    }
    @Synchronized fun remove() {
        check(prefs.edit().remove("sealed").commit()) { "Não foi possível apagar as credenciais." }
    }
}
