package br.com.uaicortes

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.util.Locale

private data class DeviceAudio(
    val uri: Uri, val name: String, val durationMs: Long, val dateAdded: Long
) {
    val searchKey = DeviceAudioSearch.key(name)
}

private object DeviceAudioLibrary {
    val permission: String
        get() = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
            else Manifest.permission.READ_EXTERNAL_STORAGE

    fun hasAccess(context: Context) =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    suspend fun load(context: Context, wavOnly: Boolean): List<DeviceAudio> = withContext(Dispatchers.IO) {
        val collections = if (Build.VERSION.SDK_INT >= 29) {
            // Concrete volumes keep the selected URI valid for internal shared storage and SD.
            MediaStore.getExternalVolumeNames(context).map { MediaStore.Audio.Media.getContentUri(it) }
        } else listOf(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)
        val columns = arrayOf(
            MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.MIME_TYPE
        )
        val job = currentCoroutineContext()
        val rows = mutableListOf<DeviceAudio>()
        var successfulVolumes = 0
        var failure: Exception? = null
        for (collection in collections) {
            job.ensureActive()
            // A volume may disappear (e.g. SD removal). Keep results from readable volumes.
            try {
                val cursor = context.contentResolver.query(collection, columns, null, null, null)
                    ?: error("Biblioteca de áudio indisponível.")
                val found = mutableListOf<DeviceAudio>()
                cursor.use {
                    while (it.moveToNext()) {
                        job.ensureActive()
                        val name = it.getString(1).orEmpty()
                        if (wavOnly && !DeviceAudioSearch.isWave(name, it.getString(4).orEmpty())) continue
                        found += DeviceAudio(ContentUris.withAppendedId(collection, it.getLong(0)),
                            name.ifBlank { "Áudio sem nome" }, it.getLong(2), it.getLong(3))
                    }
                }
                rows.addAll(found)
                successfulVolumes++
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                failure = error
            }
        }
        if (successfulVolumes == 0) failure?.let { throw it }
        rows.sortedWith(compareByDescending<DeviceAudio> { it.dateAdded }
            .thenBy { it.searchKey }.thenBy { it.uri.toString() })
    }
}

private enum class DeviceAudioState { LOADING, READY, FAILED }

/** Uses the same URI contract for device audio and the original Android document route. */
@Composable
internal fun rememberDeviceAudioPicker(
    wavOnly: Boolean = false,
    includeVideoInFiles: Boolean = false,
    onResult: (Uri?) -> Unit
): () -> Unit {
    val context = LocalContext.current
    val callback by rememberUpdatedState(onResult)
    var open by rememberSaveable { mutableStateOf(false) }
    var hasAccess by remember { mutableStateOf(DeviceAudioLibrary.hasAccess(context)) }
    var refresh by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasAccess = granted
    }
    val files = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { callback(it) }

    if (open) {
        DisposableEffect(lifecycleOwner, context) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    hasAccess = DeviceAudioLibrary.hasAccess(context)
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }
        DeviceAudioPickerDialog(
            hasAccess = hasAccess,
            refresh = refresh,
            wavOnly = wavOnly,
            filesLabel = if (includeVideoInFiles) "Procurar arquivos ou vídeos" else "Procurar arquivos",
            requestAccess = { permission.launch(DeviceAudioLibrary.permission) },
            retry = { refresh++ },
            onFiles = {
                open = false
                files.launch(if (includeVideoInFiles) arrayOf("audio/*", "video/*") else arrayOf("audio/*"))
            },
            onCancel = {
                open = false
                callback(null)
            },
            onPick = {
                open = false
                callback(it)
            }
        )
    }
    return {
        hasAccess = DeviceAudioLibrary.hasAccess(context)
        open = true
    }
}

@Composable
private fun DeviceAudioPickerDialog(
    hasAccess: Boolean,
    refresh: Int,
    wavOnly: Boolean,
    filesLabel: String,
    requestAccess: () -> Unit,
    retry: () -> Unit,
    onFiles: () -> Unit,
    onCancel: () -> Unit,
    onPick: (Uri) -> Unit
) {
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    var state by remember { mutableStateOf(DeviceAudioState.LOADING) }
    var audio by remember { mutableStateOf(emptyList<DeviceAudio>()) }
    var query by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(context, hasAccess, refresh, wavOnly) {
        state = DeviceAudioState.LOADING
        audio = emptyList()
        if (hasAccess) {
            try {
                audio = DeviceAudioLibrary.load(context.applicationContext, wavOnly)
                state = DeviceAudioState.READY
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                state = DeviceAudioState.FAILED
            }
        }
    }
    val filtered = remember(audio, query) {
        val terms = DeviceAudioSearch.terms(query)
        if (terms.isEmpty()) audio else audio.filter { DeviceAudioSearch.matches(it.searchKey, terms) }
    }
    Dialog(onDismissRequest = onCancel) {
        Surface(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (wavOnly) "Arquivos WAV do aparelho" else "Áudios do aparelho",
                        Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = onCancel) { Icon(Icons.Default.Close, "Fechar") }
                }
                if (hasAccess && state == DeviceAudioState.READY && audio.isNotEmpty()) {
                    OutlinedTextField(
                        value = query, onValueChange = { query = it }, singleLine = true,
                        label = { Text("Buscar pelo nome") },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        trailingIcon = if (query.isNotEmpty()) {
                            { IconButton(onClick = { query = "" }) { Icon(Icons.Default.Close, "Limpar busca") } }
                        } else null,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                when {
                    !hasAccess -> {
                        Text("Permita o acesso para ver músicas, gravações e outros áudios do aparelho.",
                            style = MaterialTheme.typography.bodyMedium)
                        Button(onClick = requestAccess, modifier = Modifier.fillMaxWidth()) {
                            Text("Permitir acesso aos áudios")
                        }
                    }
                    state == DeviceAudioState.LOADING -> {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                        Text("Carregando áudios…", style = MaterialTheme.typography.bodyMedium)
                    }
                    state == DeviceAudioState.FAILED -> {
                        Text("Não foi possível carregar os áudios do aparelho.")
                        TextButton(onClick = retry) { Text("Tentar de novo") }
                    }
                    audio.isEmpty() -> Text(if (wavOnly) "Nenhum arquivo WAV encontrado."
                        else "Nenhum áudio encontrado no aparelho.")
                    filtered.isEmpty() -> Text("Nenhum áudio corresponde à busca.")
                    else -> {
                        Text(if (query.isBlank()) "Mais recentes" else "Resultados da busca",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        LazyColumn(Modifier.fillMaxWidth().weight(1f, fill = false).heightIn(max = 340.dp)) {
                            items(filtered, key = { it.uri.toString() }) { item ->
                                Row(Modifier.fillMaxWidth().clickable(onClickLabel = "Selecionar áudio") {
                                    onPick(item.uri)
                                }.heightIn(min = 52.dp).padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AudioFile, null, tint = MaterialTheme.colorScheme.primary)
                                    Text(item.name, Modifier.weight(1f), maxLines = 2,
                                        overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                                    if (item.durationMs > 0) {
                                        val total = item.durationMs / 1000
                                        Text(String.format(Locale.ROOT, "%d:%02d", total / 60, total % 60),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
                HorizontalDivider()
                OutlinedButton(onClick = onFiles, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.FolderOpen, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(filesLabel)
                }
            }
        }
    }
}
