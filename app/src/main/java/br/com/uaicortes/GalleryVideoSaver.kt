package br.com.uaicortes

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import java.io.File

/**
 * Único ponto de publicação de MP4 para o MediaStore.
 * A URI só é retornada após a cópia integral e a retirada de IS_PENDING.
 * Em erro, descarta a entrada parcial. Chamador deve executar fora da thread UI.
 */
internal object GalleryVideoSaver {
    /** Cópia local em blocos: menos operações de I/O para MP4s grandes, memória limitada. */
    internal const val COPY_BUFFER_BYTES = 256 * 1024

    fun save(
        context: Context,
        source: File,
        displayName: String,
        subfolder: String = "",
        shouldContinue: () -> Boolean = { true }
    ): Uri {
        require(source.isFile && source.length() > 0L) {
            "O arquivo MP4 temporário está vazio ou não foi gerado."
        }
        require(displayName.isNotBlank() && displayName.endsWith(".mp4", ignoreCase = true)) {
            "Nome inválido para vídeo MP4."
        }
        require(subfolder.isEmpty() || subfolder == "Divididos") { "Subpasta de vídeo não permitida." }
        val folder = Environment.DIRECTORY_MOVIES + "/UaiCortes" +
            (if (subfolder.isEmpty()) "" else "/$subfolder")
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, folder)
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val target = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("Não foi possível criar o arquivo na galeria")
        try {
            val copied = resolver.openOutputStream(target, "w")?.use { output ->
                source.inputStream().use { input ->
                    val buffer = ByteArray(COPY_BUFFER_BYTES)
                    var total = 0L
                    while (true) {
                        // Checar entre blocos para não publicar um MP4 cujo salvamento foi cancelado.
                        if (!shouldContinue()) throw java.util.concurrent.CancellationException("Salvamento cancelado")
                        val size = input.read(buffer)
                        if (size == -1) break
                        output.write(buffer, 0, size)
                        total += size
                    }
                    total
                }
            } ?: error("Não foi possível gravar o vídeo")
            if (!shouldContinue()) throw java.util.concurrent.CancellationException("Salvamento cancelado")
            check(copied == source.length()) { "A cópia do MP4 para a galeria está incompleta" }
            val finished = resolver.update(target, ContentValues().apply {
                put(MediaStore.Video.Media.IS_PENDING, 0)
            }, null, null)
            check(finished > 0) { "Não foi possível finalizar o vídeo na galeria" }
            return target
        } catch (failure: Exception) {
            // Melhor esforço: manter a exceção original mesmo se a limpeza falhar.
            runCatching { resolver.delete(target, null, null) }
            throw failure
        }
    }
}
