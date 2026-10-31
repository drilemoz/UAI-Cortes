package br.com.uaicortes

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec

/** AES-256-GCM no Android Keystore, dados cifrados em prefs privados; backup do aplicativo desabilitado. */
internal class GeminiCredentialStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("gemini_secret_local", Context.MODE_PRIVATE)
    private val alias = "uaicortes_gemini_aes_v1"
    private fun key(): java.security.Key {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        ks.getKey(alias, null)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setKeySize(256).build())
        }.generateKey()
    }
    fun hasKey() = prefs.contains("ciphertext")
    fun read(): String? {
        val blob = prefs.getString("ciphertext", null) ?: return null
        try {
            val raw = Base64.decode(blob, Base64.NO_WRAP)
            require(raw.size > 28) { "Credencial criptografada inválida." }
            val iv = raw.copyOfRange(0, 12)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
            return GeminiKeyParser.validate(String(cipher.doFinal(raw.copyOfRange(12, raw.size)), Charsets.UTF_8))
        } catch (_: Exception) {
            // Nunca reutilizar blob inacessível de uma restauração/invalidação do Keystore.
            prefs.edit().remove("ciphertext").commit()
            runCatching { KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.deleteEntry(alias) }
            throw IllegalStateException("Credencial não pôde ser recuperada. Cadastre sua chave novamente.")
        }
    }
    fun save(value: String) {
        val valid = GeminiKeyParser.validate(value)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val blob = cipher.iv + cipher.doFinal(valid.toByteArray(Charsets.UTF_8))
        check(prefs.edit().putString("ciphertext", Base64.encodeToString(blob, Base64.NO_WRAP)).commit()) {
            "Não foi possível persistir a configuração."
        }
    }
    fun remove() { check(prefs.edit().remove("ciphertext").commit()) { "Falha ao remover credencial." } }
}
