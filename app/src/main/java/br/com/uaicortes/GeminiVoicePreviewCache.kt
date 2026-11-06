package br.com.uaicortes

import android.content.Context
import android.os.StatFs
import java.io.File
import java.io.IOException
import org.json.JSONObject

/** Amostras TTS do próprio usuário, sem texto de projetos ou credenciais.
 * Cache de longo prazo em filesDir (não expira por tempo); remoção manual, dados do app
 * ou falta de espaço podem apagá-lo. Toda leitura/gravação deve ocorrer em Dispatchers.IO.
 */
internal class GeminiVoicePreviewCache(context: Context) {
    private val appContext = context.applicationContext
    private val folder = File(appContext.filesDir, "gemini-voice-previews")
    private val namePattern = Regex("[0-9a-f]{64}\\.wav")
    private val index = File(folder, "voice-index.json")
    private fun readIndex(): Map<String, String> = runCatching {
        if (!index.isFile || index.length() > 128 * 1024L) return@runCatching emptyMap()
        val json = JSONObject(index.readText(Charsets.UTF_8))
        buildMap {
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val id = json.optString(key)
                if (key.matches(Regex("[0-9a-f]{64}")) && GeminiVoiceFavorites.valid(id))
                    put(key, id)
            }
        }
    }.getOrDefault(emptyMap())

    private fun rememberVoice(key: String, voiceId: String) {
        if (!GeminiVoiceFavorites.valid(voiceId)) return
        val data = (readIndex() + (key to voiceId)).filterKeys { exists(it) }
        val json = JSONObject()
        data.forEach { (hash, id) -> json.put(hash, id) }
        val temporary = File(folder, "voice-index.json.part")
        try {
            temporary.outputStream().use { it.write(json.toString().toByteArray(Charsets.UTF_8)); it.fd.sync() }
            if (!temporary.renameTo(index)) throw IOException("Índice de vozes indisponível.")
        } finally { temporary.delete() }
    }

    private fun path(id: String): File {
        require(id.matches(Regex("[0-9a-f]{64}"))) { "Identificador inválido." }
        return File(folder, "$id.wav")
    }
    fun exists(id: String): Boolean = runCatching {
        val file = path(id)
        file.isFile && file.length() in 45L..(32L * 1024 * 1024)
    }.getOrDefault(false)

    fun find(id: String): File? {
        val file = runCatching { path(id) }.getOrNull() ?: return null
        if (!file.isFile) return null
        val valid = runCatching {
            file.length() in 45L..(32L * 1024 * 1024) &&
                GeminiWaveParser.duration(file.readBytes(), "audio/wav") > 0L
        }.getOrDefault(false)
        if (!valid) { file.delete(); return null }
        file.setLastModified(System.currentTimeMillis())
        return file
    }
    fun save(id: String, bytes: ByteArray, voiceId: String? = null): File {
        val target = path(id)
        GeminiWaveParser.duration(bytes, "audio/wav")
        if (bytes.size > 32 * 1024 * 1024) throw IOException("Amostra maior que o limite individual.")
        if (!folder.isDirectory && !folder.mkdirs()) throw IOException("Armazenamento de prévias indisponível.")
        if (StatFs(folder.path).availableBytes < bytes.size + 4L * 1024 * 1024)
            throw IOException("Espaço insuficiente para armazenar a prévia.")
        val temp = File.createTempFile(".voice-", ".part", folder)
        try {
            temp.outputStream().use { it.write(bytes); it.fd.sync() }
            if (!temp.renameTo(target)) throw IOException("Falha ao salvar a prévia.")
            target.setLastModified(System.currentTimeMillis())
            if (voiceId != null) runCatching { rememberVoice(id, voiceId) }
            prune(target)
            return target
        } finally { temp.delete() }
    }
    fun usageBytes(): Long = folder.listFiles().orEmpty().filter { namePattern.matches(it.name) }
        .sumOf { it.length().coerceAtLeast(0L) }
    fun clear(): Int {
        val items = folder.listFiles().orEmpty().filter { namePattern.matches(it.name) }
        val deleted = items.count { it.delete() }
        index.delete()
        return deleted
    }
    /** LRU por último uso. Nunca remove a amostra recém-escrita; limites rígidos preservam o aparelho. */
    fun prune(keep: File? = null) {
        val maxBytes = GeminiVoiceLibrary(appContext).limitMb().toLong() * 1024 * 1024
        var total = usageBytes()
        if (total <= maxBytes) return
        val voices = readIndex()
        val pinned = GeminiVoiceFavorites(appContext).load()
        val old = folder.listFiles().orEmpty().filter { namePattern.matches(it.name) && it != keep }
            .sortedWith(compareBy<File>({ voices[it.nameWithoutExtension] in pinned }, { it.lastModified() }))
        for (file in old) {
            if (total <= maxBytes) break
            val size = file.length()
            if (file.delete()) total -= size
        }
    }
}
