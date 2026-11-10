package br.com.uaicortes

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec

/** Chave fornecida pelo usuário. Nunca embarcada no APK, em diagnósticos ou em backups. */
internal object ElevenLabsKeyPolicy {
    fun validate(value: String): String {
        val key = value.trim()
        require(key.length in 10..512 && key.all { it.code in 33..126 } &&
            !key.contains(':') && !key.contains('"') && !key.contains('\\')) {
            "Chave ElevenLabs inválida. Cole a chave completa, sem espaços ou quebras de linha."
        }
        return key
    }

    /** Aceita chave pura, ENV simples ou JSON plano, sem executar comandos nem carregar outros segredos. */
    fun importText(value: String): String {
        require(value.length <= 16_384) { "Arquivo excede 16 KB." }
        val raw = value.trim()
        val key = if (raw.startsWith("{")) {
            org.json.JSONObject(raw).optString("ELEVENLABS_API_KEY")
        } else if (raw.contains('\n') || raw.startsWith("ELEVENLABS_API_KEY=")) {
            raw.lines().map { it.trim() }
                .firstOrNull { it.startsWith("ELEVENLABS_API_KEY=") }
                ?.substringAfter('=')?.trim()?.trim('"', '\'')
                ?: throw IllegalArgumentException("Variável ELEVENLABS_API_KEY não encontrada.")
        } else if (raw.substringBefore('=').matches(Regex("[A-Z_][A-Z0-9_]*")) && '=' in raw) {
            throw IllegalArgumentException("Variável ELEVENLABS_API_KEY não encontrada.")
        } else raw
        return validate(key)
    }
}

/** Segredo criptografado com chave exclusiva, gerenciada pelo Android Keystore. */
internal class ElevenLabsCredentialStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "elevenlabs_secret_local", Context.MODE_PRIVATE)
    private val alias = "uaicortes_elevenlabs_aes_v1"

    private fun key(): java.security.Key {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        ks.getKey(alias, null)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256).build())
        }.generateKey()
    }

    @Synchronized fun hasKey(): Boolean = prefs.contains("sealed")

    @Synchronized fun read(): String? {
        val blob = prefs.getString("sealed", null) ?: return null
        return try {
            val bytes = Base64.decode(blob, Base64.NO_WRAP)
            require(bytes.size > 28)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
            ElevenLabsKeyPolicy.validate(String(cipher.doFinal(bytes.copyOfRange(12, bytes.size)), Charsets.UTF_8))
        } catch (_: Exception) {
            throw IllegalStateException("Chave ElevenLabs protegida indisponível neste aparelho.")
        }
    }

    @Synchronized fun save(value: String) {
        val plain = ElevenLabsKeyPolicy.validate(value).toByteArray(Charsets.UTF_8)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val sealed = Base64.encodeToString(cipher.iv + cipher.doFinal(plain), Base64.NO_WRAP)
        check(prefs.edit().putString("sealed", sealed).commit()) { "Não foi possível salvar a chave." }
    }

    @Synchronized fun remove() {
        check(prefs.edit().remove("sealed").commit()) { "Não foi possível remover a chave." }
    }
}
