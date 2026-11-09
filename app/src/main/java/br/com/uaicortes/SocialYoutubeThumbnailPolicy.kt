package br.com.uaicortes

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/** Limita URLs remotas à CDN de miniaturas do YouTube; IDs já validados pelo chamador. */
internal object SocialYoutubeThumbnailPolicy {
    fun safeUrl(id: String, candidate: String): String {
        if (!SocialPublicationPolicy.validVideoId(id) || id.isBlank()) return ""
        val canonical = "https://i.ytimg.com/vi/$id/mqdefault.jpg"
        val parsed = runCatching { Uri.parse(candidate) }.getOrNull() ?: return canonical
        val host = parsed.host?.lowercase().orEmpty()
        val segments = parsed.pathSegments.orEmpty()
        return if (parsed.scheme == "https" && parsed.port == -1 && parsed.userInfo == null &&
            host in setOf("i.ytimg.com", "img.youtube.com") &&
            segments.size == 3 && segments[0] == "vi" && segments[1] == id &&
            segments[2] in setOf("default.jpg", "mqdefault.jpg", "hqdefault.jpg", "sddefault.jpg"))
            "https://$host/vi/$id/${segments[2]}" else canonical
    }
}

/** Cache limitado a 6 MiB; evita recarregar thumbs nas recomposições e na busca. */
private object SocialThumbnailMemory {
    val cache = object : LruCache<String, Bitmap>(6 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }
    fun load(url: String): Bitmap? {
        synchronized(cache) { cache.get(url)?.let { return it } }
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 7_000; readTimeout = 7_000
            instanceFollowRedirects = false; useCaches = true
        }
        val bitmap = try {
            if (connection.responseCode != 200 || connection.contentLengthLong > 512_000L) null
            else connection.inputStream.use { input ->
                val output = java.io.ByteArrayOutputStream()
                val buf = ByteArray(4096)
                while (true) {
                    val count = input.read(buf)
                    if (count < 0) break
                    if (output.size() + count > 512_000) return null
                    output.write(buf, 0, count)
                }
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                val bytes = output.toByteArray()
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                if (options.outWidth !in 1..1920 || options.outHeight !in 1..1080) null
                else BitmapFactory.decodeByteArray(bytes, 0, bytes.size,
                    BitmapFactory.Options().apply { inSampleSize = 2 })
            }
        } finally { connection.disconnect() }
        if (bitmap != null) synchronized(cache) { cache.put(url, bitmap) }
        return bitmap
    }
}

@Composable
internal fun SocialYoutubeThumbnail(id: String, suppliedUrl: String, modifier: Modifier = Modifier) {
    val url = remember(id, suppliedUrl) { SocialYoutubeThumbnailPolicy.safeUrl(id, suppliedUrl) }
    val bitmap by produceState<Bitmap?>(null, url) {
        value = if (url.isNotBlank()) withContext(Dispatchers.IO) {
            runCatching { SocialThumbnailMemory.load(url) }.getOrNull()
        } else null
    }
    Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center) {
        if (bitmap != null) Image(bitmap!!.asImageBitmap(), "Miniatura do vídeo no YouTube",
            modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Icon(Icons.Default.PlayCircleOutline, "Miniatura indisponível",
            modifier = Modifier.size(30.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
