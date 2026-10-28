package br.com.uaicortes

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal enum class GalleryScope { VIDEOS, IMAGES_AND_VIDEOS }
internal data class DeviceMedia(
    val uri: Uri,
    val name: String,
    val folder: String,
    val isImage: Boolean,
    val durationMs: Long,
    val dateAddedSeconds: Long = 0L
)

/** Mídia indexada e permitida pelo Android. Não varre caminhos privados. */
internal object MediaLibrary {
    fun permissions(scope: GalleryScope): Array<String> = when {
        Build.VERSION.SDK_INT >= 34 -> (if (scope == GalleryScope.VIDEOS)
            arrayOf(Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
        else arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED))
        Build.VERSION.SDK_INT >= 33 -> if (scope == GalleryScope.VIDEOS)
            arrayOf(Manifest.permission.READ_MEDIA_VIDEO)
        else arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
        else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    fun canBrowse(context: Context, scope: GalleryScope): Boolean = permissions(scope).any {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    fun load(context: Context, image: Boolean): List<DeviceMedia> {
        val collection = if (image) MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                         else MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val columns = mutableListOf(MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.DATE_ADDED)
        if (Build.VERSION.SDK_INT >= 29) columns += MediaStore.MediaColumns.RELATIVE_PATH
        else columns += MediaStore.MediaColumns.DATA
        if (!image) columns += MediaStore.Video.Media.DURATION
        val output = ArrayList<DeviceMedia>()
        context.contentResolver.query(collection, columns.toTypedArray(), null, null,
            "${MediaStore.MediaColumns.DATE_ADDED} DESC")?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val titleIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val dateIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
            val folderIndex = cursor.getColumnIndex(if (Build.VERSION.SDK_INT >= 29)
                MediaStore.MediaColumns.RELATIVE_PATH else MediaStore.MediaColumns.DATA)
            val durationIndex = if (image) -1 else cursor.getColumnIndex(MediaStore.Video.Media.DURATION)
            while (cursor.moveToNext() && output.size < 3000) {
                val id = cursor.getLong(idIndex)
                val path = if (folderIndex >= 0) cursor.getString(folderIndex).orEmpty() else ""
                val folder = if (Build.VERSION.SDK_INT >= 29) path.trim('/').ifBlank { "Outras pastas" }
                    else path.substringBeforeLast('/', "").substringAfterLast('/').ifBlank { "Outras pastas" }
                output += DeviceMedia(ContentUris.withAppendedId(collection, id),
                    cursor.getString(titleIndex) ?: "Mídia", folder, image,
                    if (durationIndex >= 0) cursor.getLong(durationIndex).coerceAtLeast(0L) else 0L,
                    cursor.getLong(dateIndex))
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

/** Galeria única para criar projetos, adicionar vídeos, PiP e extrair áudio de vídeo. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DeviceMediaBrowser(
    onBack: () -> Unit,
    onSelect: (List<DeviceMedia>) -> Unit,
    openDocuments: () -> Unit,
    scope: GalleryScope = GalleryScope.IMAGES_AND_VIDEOS,
    allowMultiple: Boolean = false
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var permissionEpoch by remember { mutableIntStateOf(0) }
    val requestAccess = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        permissionEpoch++
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) permissionEpoch++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(scope) {
        if (!MediaLibrary.canBrowse(context, scope)) requestAccess.launch(MediaLibrary.permissions(scope))
    }
    val permission = MediaLibrary.canBrowse(context, scope)
    var tab by remember(scope) { mutableStateOf(if (scope == GalleryScope.VIDEOS) GalleryTab.VIDEOS else GalleryTab.RECENT) }
    var folder by remember { mutableStateOf("Todas") }
    var search by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var entries by remember { mutableStateOf<List<DeviceMedia>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    val selected = remember { mutableStateListOf<String>() }
    var forceReload by remember { mutableIntStateOf(0) }
    LaunchedEffect(permissionEpoch, permission, forceReload, scope) {
        if (!permission) {
            entries = emptyList(); loading = false
            return@LaunchedEffect
        }
        loading = true
        error = null
        try {
            entries = withContext(Dispatchers.IO) {
                // Consultas por tipo; quando só vídeos são necessários não pede acesso a imagens.
                val videos = try { MediaLibrary.load(context, false) }
                    catch (_: SecurityException) { emptyList() }
                val images = if (scope == GalleryScope.IMAGES_AND_VIDEOS) {
                    try { MediaLibrary.load(context, true) }
                    catch (_: SecurityException) { emptyList() }
                } else emptyList()
                (videos + images).sortedByDescending { it.dateAddedSeconds }
            }
        } catch (e: SecurityException) {
            entries = emptyList()
            error = "Acesso à galeria alterado. Revise as permissões e tente novamente."
        } catch (e: Exception) {
            entries = emptyList()
            error = "Não foi possível carregar a galeria: ${e.localizedMessage ?: "erro de leitura"}"
        } finally { loading = false }
    }
    val folders = remember(entries) { listOf("Todas") + entries.map { it.folder }.distinct().sorted() }
    val visible = remember(entries, folder, search, tab) {
        entries.filter {
            GalleryRules.accepts(tab, it.isImage) && (folder == "Todas" || it.folder == folder) &&
                GalleryRules.matches(it.name, it.folder, search)
        }
    }
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().height(58.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Voltar") }
            Text(if (scope == GalleryScope.VIDEOS) "Galeria de vídeos" else "Galeria de mídia",
                style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            IconButton(onClick = { forceReload++ }) { Icon(Icons.Default.Refresh, "Atualizar galeria") }
            IconButton(onClick = openDocuments) { Icon(Icons.Default.FolderOpen, "Procurar nos arquivos") }
        }
        if (!permission) {
            Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.PermMedia, null, modifier = Modifier.size(44.dp))
                Spacer(Modifier.height(14.dp))
                Text("Permita o acesso aos vídeos e imagens que deseja importar.")
                Spacer(Modifier.height(12.dp))
                Button(onClick = { requestAccess.launch(MediaLibrary.permissions(scope)) }) {
                    Icon(Icons.Default.LockOpen, null); Spacer(Modifier.width(8.dp)); Text("Permitir acesso")
                }
                TextButton(onClick = openDocuments) { Text("Escolher pelo Android") }
            }
        } else {
            OutlinedTextField(value = search, onValueChange = { search = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                singleLine = true, label = { Text("Buscar pelo nome ou pasta") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = { if (search.isNotEmpty()) IconButton(onClick = { search = "" }) {
                    Icon(Icons.Default.Close, "Limpar busca")
                } })
            LazyRow(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val tabs = if (scope == GalleryScope.VIDEOS) listOf(GalleryTab.VIDEOS)
                    else listOf(GalleryTab.RECENT, GalleryTab.VIDEOS, GalleryTab.IMAGES)
                items(tabs) { item ->
                    FilterChip(selected = tab == item, onClick = { tab = item; folder = "Todas" },
                        label = { Text(when (item) {
                            GalleryTab.RECENT -> "Recentes"; GalleryTab.VIDEOS -> "Vídeos";
                            GalleryTab.IMAGES -> "Imagens"
                        }) })
                }
            }
            LazyRow(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(folders) { name ->
                    FilterChip(selected = folder == name, onClick = { folder = name }, label = { Text(name) })
                }
            }
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (error != null) {
                Text(error!!, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.error)
            } else if (!loading && visible.isEmpty()) {
                Column(Modifier.weight(1f).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center) {
                    Text("Nenhuma mídia disponível neste filtro.")
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { requestAccess.launch(MediaLibrary.permissions(scope)) }) {
                        Text("Selecionar mais fotos e vídeos")
                    }
                    TextButton(onClick = openDocuments) { Text("Abrir arquivos do Android") }
                }
            } else {
                LazyVerticalGrid(columns = GridCells.Adaptive(110.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(visible, key = { it.uri.toString() }) { item ->
                        val index = selected.indexOf(item.uri.toString())
                        MediaThumbnail(item, if (index < 0) null else index + 1) {
                            if (allowMultiple) {
                                if (index >= 0) selected.remove(item.uri.toString())
                                else selected.add(item.uri.toString())
                            } else onSelect(listOf(item))
                        }
                    }
                }
            }
            if (allowMultiple && selected.isNotEmpty()) {
                Button(onClick = {
                    val ids = selected.toSet()
                    val chosen = entries.filter { it.uri.toString() in ids }
                        .sortedBy { selected.indexOf(it.uri.toString()) }
                    if (chosen.isNotEmpty()) onSelect(chosen)
                }, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Icon(Icons.Default.Check, null)
                    Text(" Importar ${selected.size} vídeo${if (selected.size != 1) "s" else ""}")
                }
            }
        }
    }
}

@Composable
private fun MediaThumbnail(item: DeviceMedia, selectionOrder: Int?, onClick: () -> Unit) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(initialValue = null, item.uri) {
        value = withContext(Dispatchers.IO) { thumbnail(context, item) }
    }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(9.dp))
        .background(MaterialTheme.colorScheme.surfaceVariant).clickable(onClick = onClick)) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f)) {
            if (bitmap != null) {
                Image(bitmap!!.asImageBitmap(), item.name,
                    modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Icon(if (item.isImage) Icons.Default.Image else Icons.Default.Movie,
                    contentDescription = null, modifier = Modifier.align(Alignment.Center))
            }
            if (selectionOrder != null) {
                Surface(modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                    color = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(24.dp)) {
                    Text("$selectionOrder", modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        color = MaterialTheme.colorScheme.onPrimary)
                }
            }
            if (!item.isImage) {
                Text(formatMs(item.durationMs), modifier = Modifier.align(Alignment.BottomEnd)
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = .8f)).padding(4.dp),
                    color = androidx.compose.ui.graphics.Color.White,
                    style = MaterialTheme.typography.labelSmall)
            }
        }
        Text(item.name, modifier = Modifier.padding(horizontal = 5.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall, maxLines = 1,
            overflow = TextOverflow.Ellipsis)
    }
}
