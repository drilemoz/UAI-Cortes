package br.com.uaicortes

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File

/** Captura o quadro do ARQUIVO ORIGINAL no instante indicado, não um screenshot da interface. */
internal object VideoFrameSaver {
    fun save(context: Context, target: FrameCaptureTarget, displayName: String): Uri {
        require(displayName.endsWith(".png", ignoreCase = true)) { "Nome PNG inválido" }
        val retriever = MediaMetadataRetriever()
        val bitmap = try {
            retriever.setDataSource(context, Uri.parse(target.uri))
            // OPTION_CLOSEST busca o quadro solicitado, não apenas o keyframe mais próximo.
            retriever.getFrameAtTime(target.sourceTimeMs * 1_000L, MediaMetadataRetriever.OPTION_CLOSEST)
                ?: error("Não foi possível extrair o quadro do vídeo. Verifique o acesso ao arquivo.")
        } finally {
            retriever.release()
        }
        return saveBitmap(context, bitmap, displayName)
    }

    /** Lado menor da mídia real. Não inventar 1080p para uma origem 480p. */
    fun sourceShortSide(context: Context, uri: String): Int {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, Uri.parse(uri))
            val w = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                ?.toIntOrNull() ?: return 720
            val h = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                ?.toIntOrNull() ?: return 720
            minOf(w, h).takeIf { it > 0 }?.coerceIn(360, 1080) ?: 720
        } finally {
            retriever.release()
        }
    }

    /** Quadro do MP4 temporário produzido pelo MESMO compositor de exportação:
     * inclui PiP, legendas, marca d'água e resolução final sem margens de UI.
     */
    fun saveRenderedFrame(context: Context, file: File, frameMs: Long, name: String): Uri {
        val retriever = MediaMetadataRetriever()
        val bitmap = try {
            retriever.setDataSource(file.absolutePath)
            retriever.getFrameAtTime(frameMs.coerceAtLeast(0L) * 1000L,
                MediaMetadataRetriever.OPTION_CLOSEST)
                ?: error("O vídeo temporário não contém um quadro decodificável.")
        } finally {
            retriever.release()
        }
        return saveBitmap(context, bitmap, name)
    }

    /** Salva um bitmap renderizado. A origem nunca é ampliada a partir da tela. */
    fun saveBitmap(context: Context, bitmap: Bitmap, displayName: String): Uri {
        require(displayName.endsWith(".png", ignoreCase = true)) { "Nome PNG inválido" }
        try {
            // Android 10+: escrita segura e imediata na galeria; nenhuma permissão adicional.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/UaiCortes")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: error("Não foi possível criar a imagem na galeria")
                try {
                    resolver.openOutputStream(uri, "w")?.use { output ->
                        check(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, output)) {
                            "Falha ao codificar a imagem PNG"
                        }
                    } ?: error("Não foi possível gravar a imagem")
                    check(resolver.update(uri, ContentValues().apply {
                        put(MediaStore.Images.Media.IS_PENDING, 0)
                    }, null, null) > 0) { "Falha ao finalizar a imagem na galeria" }
                    return uri
                } catch (e: Exception) {
                    runCatching { resolver.delete(uri, null, null) }
                    throw e
                }
            }
            // Android 8/9: salva em pasta acessível ao aplicativo, sem pedir permissões perigosas.
            val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                ?: error("Armazenamento externo indisponível"), "UaiCortes")
            check(dir.exists() || dir.mkdirs()) { "Não foi possível criar a pasta de imagens" }
            val file = File(dir, displayName)
            file.outputStream().use { output ->
                check(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, output)) {
                    "Falha ao codificar a imagem PNG"
                }
            }
            MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf("image/png"), null)
            return Uri.fromFile(file)
        } finally {
            bitmap.recycle()
        }
    }
}
