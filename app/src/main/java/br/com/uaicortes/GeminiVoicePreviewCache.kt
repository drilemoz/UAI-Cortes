package br.com.uaicortes

import android.content.Context
import java.io.File
import java.io.IOException

/** Amostra fixa gerada pelo Gemini, não uma prévia oficial do catálogo.
 * Não armazena chave, texto do usuário ou áudio importado. Pasta só do app, sujeita à limpeza do Android.
 * Chamadas de leitura e escrita devem ocorrer em Dispatchers.IO.
 */
internal class GeminiVoicePreviewCache(context: Context) {
    private val folder = File(context.cacheDir, "gemini-voice-previews")
    private val ttlMs = 30L * 24 * 60 * 60 * 1000
    private val maxFiles = 24
    private val maxBytes = 12L * 1024 * 1024

    private fun path(id: String) = File(folder, "$id.wav")
    fun find(id: String): File? {
        if (!id.matches(Regex("[0-9a-f]{64}"))) return null
        val file = path(id)
        if (!file.isFile) return null
        val now = System.currentTimeMillis()
        if (file.lastModified() <= 0L || now - file.lastModified() > ttlMs ||
            file.length() !in 45L..(32L * 1024 * 1024)) {
            file.delete(); return null
        }
        val valid = runCatching {
            val bytes = file.readBytes()
            GeminiWaveParser.duration(bytes, "audio/wav") > 0L
        }.getOrDefault(false)
        if (!valid) { file.delete(); return null }
        file.setLastModified(now)
        return file
    }
    fun save(id: String, bytes: ByteArray): File {
        require(id.matches(Regex("[0-9a-f]{64}"))) { "Identificador de cache inválido." }
        GeminiWaveParser.duration(bytes, "audio/wav")
        if (bytes.size > maxBytes) throw IOException("Amostra maior que o limite do cache.")
        if (!folder.isDirectory && !folder.mkdirs()) throw IOException("Cache local indisponível.")
        val temp = File.createTempFile(".sample-", ".tmp", folder)
        val target = path(id)
        try {
            temp.outputStream().use { it.write(bytes) }
            if (!temp.renameTo(target)) throw IOException("Não foi possível salvar a amostra.")
            target.setLastModified(System.currentTimeMillis())
            prune(target)
            return target
        } finally { temp.delete() }
    }
    private fun prune(keep: File) {
        val now = System.currentTimeMillis()
        val items = folder.listFiles().orEmpty().filter { it.name.matches(Regex("[0-9a-f]{64}\\.wav")) }
            .sortedByDescending { it.lastModified() }
        var total = 0L
        var count = 0
        for (file in items) {
            if (file != keep && (file.lastModified() <= 0L || now - file.lastModified() > ttlMs)) {
                file.delete(); continue
            }
            val next = total + file.length()
            if (file != keep && (count >= maxFiles || next > maxBytes)) file.delete()
            else { total = next; count++ }
        }
    }
}
