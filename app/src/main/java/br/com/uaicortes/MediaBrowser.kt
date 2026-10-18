package br.com.uaicortes

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Build
import android.provider.MediaStore
import android.net.Uri
import android.util.LruCache
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class DeviceMedia(
    val uri: Uri,
    val name: String,
    val folder: String,
    val isImage: Boolean,
    val durationMs: Long
)

/** Consulta apenas mídias indexadas pelo Android, sem percorrer caminhos privados. */
internal object MediaLibrary {
    fun permissions(): Array<String> = when {
        Build.VERSION.SDK_INT >= 34 -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
        )
        Build.VERSION.SDK_INT >= 33 -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO
        )
        else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    fun canBrowse(context: Context): Boolean = permissions().any {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    fun load(context: Context, image: Boolean): List<DeviceMedia> {
        val collection = if (image) MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                         else MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val columns = mutableListOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.DATE_ADDED
        )
        if (Build.VERSION.SDK_INT >= 29) columns += MediaStore.MediaColumns.RELATIVE_PATH
        else columns += MediaStore.MediaColumns.DATA
        if (!image) columns += MediaStore.Video.Media.DURATION
        val output = ArrayList<DeviceMedia>()
        context.contentResolver.query(collection, columns.toTypedArray(), null, null,
            "${MediaStore.MediaColumns.DATE_ADDED} DESC")?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val titleIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val folderIndex = cursor.getColumnIndex(if (Build.VERSION.SDK_INT >= 29)
                MediaStore.MediaColumns.RELATIVE_PATH else MediaStore.MediaColumns.DATA)
            val durationIndex = if (image) -1 else cursor.getColumnIndex(MediaStore.Video.Media.DURATION)
            while (cursor.moveToNext() && output.size < 3000) {
                val id = cursor.getLong(idIndex)
                val fullPath = if (folderIndex >= 0) cursor.getString(folderIndex).orEmpty() else ""
                val folder = if (Build.VERSION.SDK_INT >= 29) fullPath.trim('/')
                    .ifBlank { "Outras pastas" }
                    else fullPath.substringBeforeLast('/', "").substringAfterLast('/').ifBlank { "Outras pastas" }
                output += DeviceMedia(ContentUris.withAppendedId(collection, id),
                    cursor.getString(titleIndex) ?: "Mídia", folder, image,
                    if (durationIndex >= 0) cursor.getLong(durationIndex).coerceAtLeast(0L) else 0L)
            }
        }
        return output
    }
}

private val thumbnails = object : LruCache<String, Bitmap>(20 * 1024 * 1024) {
    override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
}

private fun thumbnail(context: Context, media: DeviceMedia): Bitmap? {
    val key = media.uri.toString()
    synchronized(thumbnails) { thumbnails.get(key)?.let { return it } }
    val bmp = runCatching {
        if (Build.VERSION.SDK_INT >= 29) {
            context.contentResolver.loadThumbnail(media.uri, Size(240, 240), null)
        } else if (media.isImage) {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(media.uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            var sample = 1
            while (options.outWidth / sample > 320 || options.outHeight / sample > 320) sample *= 2
            val scaled = BitmapFactory.Options().apply { inSampleSize = sample }
            context.contentResolver.openInputStream(media.uri)?.use { BitmapFactory.decodeStream(it, null, scaled) }
        } else {
            val retriever = MediaMetadataRetriever()
            try { retriever.setDataSource(context, media.uri); retriever.getFrameAtTime(0) }
            finally { retriever.release() }
        }
    }.getOrNull()
    if (bmp != null) synchronized(thumbnails) { thumbnails.put(key, bmp) }
    return bmp
}

@Composable
internal fun DeviceMediaBrowser(onBack: () -> Unit, onSelect: (DeviceMedia) -> Unit,
                                openDocuments: () -> Unit) {
    val context = LocalContext.current
    var permissionEpoch by remember { mutableIntStateOf(0) }
    val requestAccess = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        permissionEpoch++
    }
    LaunchedEffect(Unit) {
        if (!MediaLibrary.canBrowse(context)) requestAccess.launch(MediaLibrary.permissions())
    }
    val permission = MediaLibrary.canBrowse(context)
    var images by remember { mutableStateOf(true) }
    var folder by remember { mutableStateOf("Todas") }
    var loading by remember { mutableStateOf(false) }
    var entries by remember { mutableStateOf<List<DeviceMedia>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(images, permissionEpoch, permission) {
        folder = "Todas"
        if (!permission) { entries = emptyList(); loading = false; return@LaunchedEffect }
        loading = true
        entries = emptyList()
        error = null
        try { entries = withContext(Dispatchers.IO) { MediaLibrary.load(context, images) } }
        catch (e: Exception) {
            entries = emptyList()
            error = "Não foi possível ler a biblioteca: ${e.localizedMessage ?: "acesso indisponível"}"
        } finally { loading = false }
    }
    val folders = remember(entries) { listOf("Todas") + entries.map { it.folder }.distinct().sorted() }
    val visible = remember(entries, folder) {
        if (folder == "Todas") entries else entries.filter { it.folder == folder }
    }
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        HeadingMedia(onBack, openDocuments)
        PrimaryTabRow(selectedTabIndex = if (images) 0 else 1) {
            Tab(selected = images, onClick = { images = true }, text = { Text("Imagens") },
                icon = { Icon(Icons.Default.Image, null) })
            Tab(selected = !images, onClick = { images = false }, text = { Text("Vídeos") },
                icon = { Icon(Icons.Default.VideoLibrary, null) })
        }
        if (!permission) {
            Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.PermMedia, null, modifier = Modifier.size(44.dp))
                Spacer(Modifier.height(14.dp))
                Text("Permita acesso às fotos e vídeos para ver miniaturas das pastas do aparelho.")
                Spacer(Modifier.height(12.dp))
                Button(onClick = { requestAccess.launch(MediaLibrary.permissions()) }) {
                    Icon(Icons.Default.LockOpen, null); Spacer(Modifier.width(8.dp)); Text("Permitir acesso")
                }
                TextButton(onClick = openDocuments) {
                    Icon(Icons.Default.FolderOpen, null); Spacer(Modifier.width(8.dp)); Text("Escolher pelos arquivos")
                }
            }
        } else {
            LazyRow(contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(folders) { name ->
                    FilterChip(selected = folder == name, onClick = { folder = name }, label = { Text(name) })
                }
            }
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (error != null) {
                Text(error!!, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.error)
            } else if (!loading && visible.isEmpty()) {
                Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center) {
                    Text("Nenhuma mídia disponível nesta categoria.")
                    Spacer(Modifier.height(10.dp))
                    TextButton(onClick = { requestAccess.launch(MediaLibrary.permissions()) }) {
                        Icon(Icons.Default.Refresh, null); Text(" Rever permissões")
                    }
                    TextButton(onClick = openDocuments) { Text("Abrir gerenciador de arquivos") }
                }
            } else {
                LazyVerticalGrid(columns = GridCells.Adaptive(105.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(visible, key = { it.uri.toString() }) { item ->
                        MediaThumbnail(item, onClick = { onSelect(item) })
                    }
                }
            }
        }
    }
}

@Composable
private fun HeadingMedia(onBack: () -> Unit, openDocuments: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(60.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Voltar") }
        Text("Biblioteca de mídia", style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f))
        IconButton(onClick = openDocuments) { Icon(Icons.Default.FolderOpen, "Procurar nos arquivos") }
    }
}

@Composable
private fun MediaThumbnail(item: DeviceMedia, onClick: () -> Unit) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(initialValue = null, item.uri) {
        value = withContext(Dispatchers.IO) { thumbnail(context, item) }
    }
    Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(6.dp))
        .background(MaterialTheme.colorScheme.surfaceVariant).clickable(onClick = onClick)) {
        if (bitmap != null) {
            Image(bitmap!!.asImageBitmap(), item.name,
                modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Icon(if (item.isImage) Icons.Default.Image else Icons.Default.Movie,
                contentDescription = null, modifier = Modifier.align(Alignment.Center))
        }
        if (!item.isImage) {
            Row(Modifier.align(Alignment.BottomEnd)
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.78f), RoundedCornerShape(topStart = 6.dp))
                .padding(horizontal = 5.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PlayArrow, null, tint = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier.size(15.dp))
                Text(formatMs(item.durationMs), color = androidx.compose.ui.graphics.Color.White,
                    style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
