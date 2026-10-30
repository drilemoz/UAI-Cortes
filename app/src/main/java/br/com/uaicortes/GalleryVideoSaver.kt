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
    fun save(context: Context, source: File, displayName: String): Uri {
        require(source.isFile && source.length() > 0L) {
            "O arquivo MP4 temporário está vazio ou não foi gerado."
        }
        require(displayName.isNotBlank() && displayName.endsWith(".mp4", ignoreCase = true)) {
            "Nome inválido para vídeo MP4."
        }
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/UaiCortes")
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val target = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("Não foi possível criar o arquivo na galeria")
        try {
            val copied = resolver.openOutputStream(target, "w")?.use { output ->
                source.inputStream().use { input -> input.copyTo(output, DEFAULT_BUFFER_SIZE) }
            } ?: error("Não foi possível gravar o vídeo")
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
