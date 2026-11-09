package br.com.uaicortes

import android.content.Context
import android.os.StatFs
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Properties
import java.util.UUID

/** Armazenamento privado do Texto para Voz. Áudios legados seguem compatíveis. */
internal class StandaloneVoiceLibrary(private val root: File) {
    constructor(context: Context) : this(File(context.applicationContext.filesDir, "standalone_voice"))

    internal data class Info(val title: String = "", val voiceName: String = "",
        val excerpt: String = "", val style: String = "", val durationMs: Long = 0L,
        val favorite: Boolean = false)

    private val validName = Regex("voz_[0-9]{8}_[0-9]{6}_[a-f0-9-]{36}\\.wav")
    private fun valid(file: File): Boolean = runCatching {
        file.name.matches(validName) && file.canonicalFile.parentFile == root.canonicalFile && file.isFile
    }.getOrDefault(false)
    private fun infoFile(file: File) = File(root, file.name.removeSuffix(".wav") + ".info")

    fun list(): List<File> = root.listFiles()?.asSequence()
        ?.filter(::valid)?.sortedByDescending { it.lastModified() }?.take(100)?.toList().orEmpty()

    /** Lê somente até 8 KiB de metadados locais. Nunca inclui chave de API. */
    fun info(file: File): Info? {
        if (!valid(file)) return null
        val source = infoFile(file)
        if (!source.isFile || source.length() !in 1..8192) return null
        return runCatching {
            val p = Properties()
            source.inputStream().use { p.load(it) }
            if (p.getProperty("version") != "1") return@runCatching null
            Info(p.getProperty("title", "").take(80),
                p.getProperty("voice", "").take(120), p.getProperty("excerpt", "").take(120),
                p.getProperty("style", "").take(60),
                p.getProperty("duration", "0").toLongOrNull()?.coerceIn(0, 3600000) ?: 0,
                p.getProperty("favorite") == "true")
        }.getOrNull()
    }

    private fun writeInfo(file: File, info: Info) {
        val p = Properties().apply {
            setProperty("version", "1")
            setProperty("title", info.title.take(80))
            setProperty("voice", info.voiceName.take(120))
            setProperty("excerpt", info.excerpt.take(120))
            setProperty("style", info.style.take(60))
            setProperty("duration", info.durationMs.coerceIn(0, 3600000).toString())
            setProperty("favorite", info.favorite.toString())
        }
        val target = infoFile(file)
        val part = File(root, "${target.name}.part")
        try {
            part.outputStream().use { out -> p.store(out, "UaiCortes audio info"); out.fd.sync() }
            if (!part.renameTo(target)) throw IOException("Não foi possível salvar informações do áudio.")
        } finally { part.delete() }
    }

    fun rename(file: File, newTitle: String): Boolean {
        val title = newTitle.trim()
        if (!valid(file) || title.isBlank() || title.length > 80 || title.any(Char::isISOControl)) return false
        return runCatching {
            writeInfo(file, (info(file) ?: Info()).copy(title = title)); true
        }.getOrDefault(false)
    }

    fun toggleFavorite(file: File): Boolean {
        if (!valid(file)) return false
        return runCatching {
            val before = info(file) ?: Info()
            writeInfo(file, before.copy(favorite = !before.favorite)); true
        }.getOrDefault(false)
    }

    fun save(wave: GeminiWave, info: Info = Info()): File {
        require(wave.bytes.isNotEmpty()) { "Áudio vazio." }
        if (!root.isDirectory && !root.mkdirs()) throw IOException("Não foi possível criar a pasta dos áudios.")
        if (StatFs(root.absolutePath).availableBytes < wave.bytes.size + 4L * 1024 * 1024) {
            throw IOException("Armazenamento insuficiente para guardar o WAV.")
        }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ROOT).format(Date())
        val file = File(root, "voz_${stamp}_${UUID.randomUUID()}.wav")
        val pending = File(root, "${file.name}.part")
        try {
            pending.outputStream().use { it.write(wave.bytes); it.fd.sync() }
            if (!pending.renameTo(file)) throw IOException("Não foi possível concluir a gravação do áudio.")
            // Falha nos metadados não apaga nem corrompe o WAV já confirmado.
            runCatching { writeInfo(file, info.copy(durationMs = wave.durationMs)) }
            return file
        } finally { pending.delete() }
    }

    fun delete(file: File): Boolean {
        if (!valid(file) || !file.delete()) return false
        infoFile(file).delete() // Não remove cópias do áudio já importadas em projetos.
        return true
    }
}
