package br.com.uaicortes

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/** Grava UTF-8. O provedor escolhido pelo usuário ainda pode decidir o nome final do documento. */
internal object SubtitleDocumentWriter {
    val canSaveDownloads: Boolean get() = Build.VERSION.SDK_INT >= 29

    suspend fun save(context: Context, cues: List<SubtitleCue>, format: SubtitleFileExport.Format,
        destination: Uri?, inDownloads: Boolean): String? = withContext(Dispatchers.IO) {
        val text = SubtitleFileExport.render(cues)
        require(text.isNotEmpty()) { "Sem legendas válidas para exportar." }
        val resolver = context.contentResolver
        if (inDownloads && Build.VERSION.SDK_INT >= 29) {
            return@withContext saveDownloads(resolver, format, text)
        }
        val uri = requireNotNull(destination)
        write(resolver, uri, text)
        displayName(resolver, uri)
    }

    private fun displayName(resolver: ContentResolver, uri: Uri): String? = runCatching {
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) it.getString(0) else null
        }?.takeIf { it.isNotBlank() }
    }.getOrNull()

    private fun write(resolver: ContentResolver, uri: Uri, content: String) {
        val accepted = resolver.openOutputStream(uri, "wt") ?: throw IOException("Destino indisponível.")
        accepted.bufferedWriter(Charsets.UTF_8).use { it.write(content) }
    }

    @RequiresApi(29)
    private fun saveDownloads(resolver: ContentResolver, format: SubtitleFileExport.Format, text: String): String {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, format.fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, format.mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/UaiCortes")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: throw IOException("O Android não aceitou o arquivo em Downloads.")
        var published = false
        try {
            write(resolver, uri, text)
            val actual = displayName(resolver, uri) ?: throw IOException("Não foi possível conferir o nome.")
            if (!format.hasExpectedExtension(actual)) throw IOException("O Android alterou a extensão.")
            val publish = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
            if (resolver.update(uri, publish, null, null) != 1)
                throw IOException("O Android não confirmou o salvamento.")
            published = true
            return actual
        } finally {
            if (!published) runCatching { resolver.delete(uri, null, null) }
        }
    }
}
