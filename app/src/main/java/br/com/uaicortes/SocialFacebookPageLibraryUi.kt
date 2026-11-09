package br.com.uaicortes

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/** Apenas miniaturas autorizadas pela política local; URLs assinadas nunca persistidas no disco. */
private object FacebookImageMemory {
    private val cache = object : LruCache<String, Bitmap>(4 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }
    fun load(url: String): Bitmap? {
        synchronized(cache) { cache.get(url)?.let { return it } }
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 6_000; readTimeout = 6_000
            instanceFollowRedirects = false; useCaches = false
        }
        val bitmap = try {
            if (connection.responseCode != 200 || connection.contentLengthLong > 500_000L) null
            else connection.inputStream.use { input ->
                val out = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(4096)
                while (true) {
                    val n = input.read(buffer)
                    if (n < 0) break
                    if (out.size() + n > 500_000) return null
                    out.write(buffer, 0, n)
                }
                val data = out.toByteArray()
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(data, 0, data.size, bounds)
                if (bounds.outWidth !in 1..1920 || bounds.outHeight !in 1..1920) null
                else BitmapFactory.decodeByteArray(data, 0, data.size,
                    BitmapFactory.Options().apply { inSampleSize = 2 })
            }
        } finally { connection.disconnect() }
        if (bitmap != null) synchronized(cache) { cache.put(url, bitmap) }
        return bitmap
    }
}

@Composable
private fun SocialFacebookThumbnail(url: String, modifier: Modifier = Modifier) {
    val safe = remember(url) { FacebookPagePostPolicy.safeThumbnail(url) }
    val bitmap by produceState<Bitmap?>(null, safe) {
        value = if (safe.isBlank()) null else withContext(Dispatchers.IO) {
            runCatching { FacebookImageMemory.load(safe) }.getOrNull()
        }
    }
    Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center) {
        if (bitmap != null) Image(bitmap!!.asImageBitmap(), "Miniatura da publicação Facebook",
            Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Icon(Icons.Default.SmartDisplay, "Miniatura não disponibilizada pela Meta",
            Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun openFacebookPost(context: Context, permalink: String) {
    val safe = FacebookPagePostPolicy.safePostUrl(permalink)
    if (safe.isBlank()) return
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(safe))) }
}

/** Biblioteca de vídeos obtidos de posts da Página; não inclui todos os Reels por promessa. */
@Composable
internal fun SocialFacebookPageLibraryUi(
    accounts: List<SocialAccount>,
    selectedPageId: String,
    onSelectPage: (String) -> Unit,
    videosByPage: Map<String, List<FacebookPageVideoPost>>,
    nextCursors: Map<String, String>,
    busy: Boolean,
    error: String,
    configured: Boolean,
    authorized: Boolean,
    onAuthorize: () -> Unit,
    onLoad: (SocialAccount, String) -> Unit,
    onLoadAll: (List<SocialAccount>) -> Unit
) {
    val pages = accounts.filter { it.destination == SocialDestination.FACEBOOK_PAGE &&
        SocialPublicationPolicy.validPageId(it.remotePageId) }
    val selected = pages.firstOrNull { it.remotePageId == selectedPageId }
    val ids = pages.map { it.remotePageId }.toSet()
    val videos = videosByPage.filterKeys { it in ids }.values.flatten()
        .distinctBy { it.pageId + ":" + it.id }
        .filter { selected == null || it.pageId == selected.remotePageId }
        .sortedByDescending { it.createdTime }
    var query by rememberSaveable { mutableStateOf("") }
    var searching by rememberSaveable { mutableStateOf(false) }
    var limit by rememberSaveable { mutableIntStateOf(12) }
    val context = LocalContext.current

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Facebook · Páginas", style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold)
            Text("Publicações com vídeo retornadas pela API Meta",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Surface(shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer) {
            Text("${pages.size} Páginas", Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
    if (pages.isEmpty()) {
        Text("Nenhuma Página vinculada. Cadastre uma em Configurações > Minhas contas e canais.",
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = selected == null, onClick = { onSelectPage(""); limit = 12 },
            label = { Text("Todas") })
        pages.forEach { page ->
            FilterChip(selected = selected?.remotePageId == page.remotePageId,
                onClick = { onSelectPage(page.remotePageId); limit = 12 },
                label = { Text(page.label, maxLines = 1) })
        }
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Vídeos publicados", style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold)
            Text("${videos.size} publicações carregadas · visualizações não disponíveis",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = { query = ""; searching = true }, enabled = videos.isNotEmpty()) {
            Icon(Icons.Default.Search, "Pesquisar vídeos Facebook")
        }
        IconButton(onClick = {
            if (selected != null) onLoad(selected, "") else onLoadAll(pages)
        }, enabled = !busy && configured && authorized) {
            Icon(Icons.Default.Refresh, "Consultar publicações Facebook")
        }
    }
    if (!configured) Text("Integração Meta não configurada neste APK. Veja Serviços e APIs.",
        color = MaterialTheme.colorScheme.onSurfaceVariant)
    else if (!authorized) OutlinedButton(onClick = onAuthorize, enabled = !busy,
        modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Default.Login, null); Spacer(Modifier.width(8.dp))
        Text("Entrar com Facebook para consultar Páginas")
    }
    if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
    if (error.isNotBlank()) Surface(shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer) {
        Text(error, Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer)
    }
    if (videos.isEmpty() && !busy) Text("Nenhuma publicação com vídeo foi carregada. " +
        "A API da Meta pode não disponibilizar todos os Reels, mesmo com a Página autorizada.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
    videos.take(limit).chunked(2).forEach { pair ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            pair.forEach { post ->
                OutlinedCard(modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.clickable(enabled = post.permalink.isNotBlank()) {
                        openFacebookPost(context, post.permalink)
                    }) {
                        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
                            SocialFacebookThumbnail(post.thumbnail, Modifier.fillMaxSize())
                            Surface(Modifier.align(Alignment.BottomStart).padding(7.dp),
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f)) {
                                Text("Facebook", Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                    style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        Column(Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(post.title, maxLines = 2, minLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold)
                            Text(post.pageName, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Visualizações: —", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            if (pair.size == 1) Spacer(Modifier.weight(1f))
        }
    }
    if (videos.size > limit) TextButton(onClick = { limit += 12 }) {
        Text("Mostrar mais publicações carregadas")
    }
    val pagesWithMore = pages.filter { !nextCursors[it.remotePageId].isNullOrBlank() &&
        (selected == null || it.remotePageId == selected.remotePageId) }
    if (pagesWithMore.isNotEmpty()) {
        val target = pagesWithMore.first()
        OutlinedButton(onClick = {
            onLoad(target, nextCursors[target.remotePageId].orEmpty())
        }, modifier = Modifier.fillMaxWidth(), enabled = !busy && authorized) {
            Text("Carregar mais de ${target.label}", maxLines = 1,
                overflow = TextOverflow.Ellipsis)
        }
    }
    Text("A Meta pode omitir Reels e não fornecer visualizações nesta consulta. " +
        "Somente Páginas autorizadas; atualização manual, sem dados fictícios.",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant)

    if (searching) {
        val filtered = videos.filter { it.title.contains(query, true) ||
            it.pageName.contains(query, true) }
        AlertDialog(onDismissRequest = { searching = false },
            title = { Text("Pesquisar publicações Facebook") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = query, onValueChange = { query = it.take(100) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        label = { Text("Título ou Página") })
                    Text("${filtered.size} resultado(s) carregado(s)",
                        style = MaterialTheme.typography.labelSmall)
                    Column(Modifier.heightIn(max = 350.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        filtered.take(100).forEach { post ->
                            Text(post.title + " · " + post.pageName,
                                modifier = Modifier.fillMaxWidth()
                                    .clickable(enabled = post.permalink.isNotBlank()) {
                                        openFacebookPost(context, post.permalink)
                                    }.padding(6.dp),
                                style = MaterialTheme.typography.bodySmall)
                        }
                        if (filtered.isEmpty()) Text("Nenhuma publicação encontrada.")
                    }
                }
            }, confirmButton = { TextButton(onClick = { searching = false }) {
                Text("Fechar")
            } })
    }
}
