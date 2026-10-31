package br.com.uaicortes

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/** Verifica uma concessão de leitura real sem decodificar fotografias nem alterar o rascunho. */
internal class ImageDraftMediaAccess(context: Context) {
    private val resolver = context.applicationContext.contentResolver

    suspend fun inaccessible(images: List<Uri>): List<String> = withContext(Dispatchers.IO) {
        val jobContext = currentCoroutineContext()
        ImageDraftAccessPolicy.inaccessible(images.map(Uri::toString)) { raw ->
            jobContext.ensureActive()
            runCatching {
                resolver.openFileDescriptor(Uri.parse(raw), "r")?.use { true } == true
            }.getOrDefault(false)
        }
    }
}
