package br.com.uaicortes

import android.content.Context
import android.os.StatFs
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/** Armazenamento exclusivo do Texto para Voz. Não altera projetos nem faixas do editor. */
internal class StandaloneVoiceLibrary(private val root: File) {
    constructor(context: Context) : this(File(context.applicationContext.filesDir, "standalone_voice"))

    fun list(): List<File> = root.listFiles()?.asSequence()
        ?.filter { it.isFile && it.name.matches(Regex("voz_[0-9]{8}_[0-9]{6}_[a-f0-9-]{36}\\.wav")) }
        ?.sortedByDescending { it.lastModified() }?.take(100)?.toList().orEmpty()

    fun save(wave: GeminiWave): File {
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
            return file
        } finally {
            pending.delete()
        }
    }

    fun delete(file: File): Boolean = file.canonicalFile.parentFile == root.canonicalFile &&
        list().any { it.canonicalFile == file.canonicalFile } && file.delete()
}

