package br.com.uaicortes

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.security.KeyStore
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec

/**
 * Credenciais somente locais, protegidas individualmente com AES-GCM/Android Keystore.
 * Mantém a chave legada (ciphertext) para instalações anteriores à 0.1.130.
 * As cotas da API Gemini pertencem ao PROJETO Google Cloud, não a cada chave.
 */
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
    private fun decrypt(blob: String): String {
        try {
            val raw = Base64.decode(blob, Base64.NO_WRAP)
            require(raw.size > 28) { "Credencial criptografada inválida." }
            val iv = raw.copyOfRange(0, 12)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
            return GeminiKeyParser.validate(String(cipher.doFinal(raw.copyOfRange(12, raw.size)), Charsets.UTF_8))
        } catch (_: Exception) {
            // Nunca apagar automaticamente outras chaves quando o Keystore está indisponível.
            throw IllegalStateException("Chave protegida indisponível. Confira o armazenamento seguro deste aparelho.")
        }
    }
    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val bytes = cipher.iv + cipher.doFinal(GeminiKeyParser.validate(value).toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
    private fun rawProfiles(): JSONArray = JSONArray(prefs.getString("profiles_v2", "[]") ?: "[]")
    private fun persist(array: JSONArray) {
        check(prefs.edit().putString("profiles_v2", array.toString()).commit()) { "Não foi possível salvar as credenciais." }
    }
    @Synchronized fun profiles(): List<GeminiKeyProfile> {
        val output = mutableListOf<GeminiKeyProfile>()
        if (prefs.contains("ciphertext")) output += GeminiKeyProfile(
            id = "legacy", label = prefs.getString("legacy_label", "Chave original") ?: "Chave original",
            projectId = prefs.getString("legacy_project", "").orEmpty(),
            masked = "••••${runCatching { decrypt(prefs.getString("ciphertext", "") ?: "").takeLast(4) }.getOrDefault("????")}")
        val array = rawProfiles()
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            output += GeminiKeyProfile(item.getString("id"), item.getString("label"),
                item.optString("project"), "••••${runCatching { decrypt(item.getString("blob")).takeLast(4) }.getOrDefault("????")}")
        }
        return output
    }
    @Synchronized fun hasKey(): Boolean = prefs.contains("ciphertext") || rawProfiles().length() > 0
    @Synchronized fun activeId(): String? {
        val saved = prefs.getString("active_profile", null)
        val ids = profiles().map { it.id }
        return saved?.takeIf { it in ids } ?: ids.firstOrNull()
    }
    @Synchronized fun read(): String? = activeId()?.let { readProfile(it) }
    @Synchronized fun readProfile(id: String): String? {
        if (id == "legacy") return prefs.getString("ciphertext", null)?.let(::decrypt)
        val arr = rawProfiles()
        for (i in 0 until arr.length()) {
            val item = arr.optJSONObject(i) ?: continue
            if (item.optString("id") == id) return decrypt(item.getString("blob"))
        }
        return null
    }
    @Synchronized fun save(value: String) {
        val id = activeId()
        val encrypted = encrypt(value)
        if (id != null && id != "legacy") {
            val arr = rawProfiles()
            for (i in 0 until arr.length()) if (arr.getJSONObject(i).getString("id") == id) {
                arr.getJSONObject(i).put("blob", encrypted)
                persist(arr)
                return
            }
        }
        check(prefs.edit().putString("ciphertext", encrypted).commit()) { "Falha ao salvar chave." }
    }
    @Synchronized fun add(value: String, label: String, projectId: String): String {
        require(profiles().size < 8) { "Limite de oito credenciais neste aparelho." }
        val valid = GeminiKeyParser.validate(value)
        require(profiles().none { readProfile(it.id) == valid }) { "Esta chave já está cadastrada." }
        val safeLabel = label.trim().take(48)
        require(safeLabel.isNotEmpty()) { "Informe um nome para a chave." }
        val project = checkProject(projectId)
        val id = UUID.randomUUID().toString()
        val arr = rawProfiles().put(JSONObject().put("id", id).put("label", safeLabel)
            .put("project", project).put("blob", encrypt(valid)))
        persist(arr)
        if (activeId() == null) select(id)
        return id
    }
    @Synchronized fun rename(id: String, label: String, projectId: String) {
        val safeLabel = label.trim().take(48)
        require(safeLabel.isNotBlank()) { "Informe um nome para a chave." }
        val project = checkProject(projectId)
        if (id == "legacy" && prefs.contains("ciphertext")) {
            check(prefs.edit().putString("legacy_label",safeLabel).putString("legacy_project",project).commit())
            return
        }
        val arr = rawProfiles()
        for (i in 0 until arr.length()) {
            val item = arr.getJSONObject(i)
            if (item.optString("id") == id) {
                item.put("label", safeLabel).put("project", project)
                persist(arr)
                return
            }
        }
        error("Credencial não encontrada.")
    }
    @Synchronized fun select(id: String) {
        require(profiles().any { it.id == id }) { "Chave não encontrada." }
        check(prefs.edit().putString("active_profile",id).commit())
    }
    @Synchronized fun remove() { activeId()?.let(::removeProfile) }
    @Synchronized fun removeProfile(id: String) {
        if (id == "legacy") {
            check(prefs.edit().remove("ciphertext").remove("legacy_label").remove("legacy_project").commit())
        } else {
            val arr = rawProfiles()
            val reduced = JSONArray()
            var found = false
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                if (item.optString("id") == id) found = true else reduced.put(item)
            }
            require(found) { "Chave não encontrada." }
            persist(reduced)
        }
        if (prefs.getString("active_profile", null) == id) prefs.edit().remove("active_profile").commit()
    }
    @Synchronized fun markAutomaticSwitch(id: String) {
        prefs.edit().putString("last_auto_switch_id", id)
            .putLong("last_auto_switch_at", System.currentTimeMillis()).commit()
    }
    @Synchronized fun lastAutomaticSwitch(): String? {
        val id = prefs.getString("last_auto_switch_id", null) ?: return null
        return profiles().firstOrNull { it.id == id }?.label
    }
    @Synchronized fun autoFallbackEnabled(): Boolean = prefs.getBoolean("auto_fallback_v2", false)
    @Synchronized fun setAutoFallback(value: Boolean) {
        check(prefs.edit().putBoolean("auto_fallback_v2", value).commit())
    }
    /** Pausa específica por projeto+modelo; não bloqueia transcrição por falha do TTS. */
    private fun pauseKey(project: String, model: String): String = "pause_" + project +
        if (model.isBlank()) "" else "_model_" + model
    @Synchronized fun temporarilyBlocked(project: String, model: String = ""): Boolean =
        project.isNotBlank() && (prefs.getLong(pauseKey(project, model), 0L) > System.currentTimeMillis() ||
            (model.isNotBlank() && prefs.getLong("pause_" + project, 0L) > System.currentTimeMillis()))
    @Synchronized fun markExhausted(project: String, model: String = "") {
        if (project.isNotBlank()) prefs.edit().putLong(pauseKey(project, model),
            System.currentTimeMillis() + 10L * 60 * 1000).commit()
    }
    companion object {
        fun checkProject(value: String): String {
            val project = value.trim()
            // Valor vazio é permitido, mas impede failover automático para/desde esta chave.
            require(project.isEmpty() || project.matches(Regex("[a-zA-Z0-9][a-zA-Z0-9._:-]{2,99}"))) {
                "Identificador de projeto inválido." }
            return project
        }
    }
}

internal data class GeminiKeyProfile(val id: String, val label: String, val projectId: String, val masked: String)
