package br.com.uaicortes

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.media.MediaMetadataRetriever
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerId
import kotlin.math.abs
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.PlaybackParameters
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date
import java.util.UUID

private val Blue = Color(0xFF0866FF)
private val Purple = Color(0xFF6954B5)
private val Green = Color(0xFF16866E)
private val Ink = Color(0xFF151515)
private val LightBg = Color(0xFFF7F8FA)

private data class OverlapConfirmation(
    val project: VideoProject, val requested: List<Overlay>,
    val preferredId: String, val conflicts: Set<Pair<String, String>>
)
private data class SequentialConfirmation(val uri: Uri, val isImage: Boolean, val mediaMs: Long)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        DiagnosticRecorder.initialize(applicationContext)
        setContent { UaiApp() }
    }
}

/** Observa falhas de reprodução em todos os players sem capturar caminhos de mídia. */
private fun diagnosticPlayer(context: android.content.Context): ExoPlayer =
    ExoPlayer.Builder(context).build().apply {
        addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                DiagnosticRecorder.exception("media3_player", error)
            }
        })
    }

@Composable
fun UaiApp() {
    val context = LocalContext.current
    val store = remember { ProjectStore(context) }
    var diagnosticsEnabled by remember { mutableStateOf(DiagnosticRecorder.isEnabled()) }
    var theme by remember { mutableStateOf(store.theme()) }
    var screen by remember { mutableStateOf("home") }
    var project by remember { mutableStateOf<VideoProject?>(null) }
    var projects by remember { mutableStateOf(store.load()) }
    var selectedClip by remember { mutableStateOf("") }
    var selectedAudioTrackId by remember { mutableStateOf<String?>(null) }
    var selectedSubtitleId by remember { mutableStateOf<String?>(null) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var pendingPicker by remember { mutableStateOf("") }
    var mediaOrigin by remember { mutableStateOf("editor") }
    var selectedOverlayId by remember { mutableStateOf("") }
    var mediaAction by remember { mutableStateOf("add") }
    var previous by remember { mutableStateOf("home") }
    var pendingOverlap by remember { mutableStateOf<OverlapConfirmation?>(null) }
    var pendingSequential by remember { mutableStateOf<SequentialConfirmation?>(null) }
    val dark = when (theme) {
        "Claro" -> false
        "Escuro", "AMOLED" -> true
        else -> isSystemInDarkTheme()
    }
    val palette = if (dark) darkColorScheme(primary = Color(0xFF72A5FF), background = if(theme=="AMOLED") Color.Black else Ink,
        surface = if(theme=="AMOLED") Color.Black else Color(0xFF202124))
    else lightColorScheme(primary = Blue, background = LightBg, surface = Color.White)
    fun write(p: VideoProject) {
        project = p; store.save(p); projects = store.load()
        DiagnosticRecorder.action(screen, "project_saved")
    }
    fun navTo(s: String) {
        if (s == "pip" && project != null && project!!.overlays.none { it.id == selectedOverlayId })
            selectedOverlayId = project!!.overlays.firstOrNull()?.id.orEmpty()
        DiagnosticRecorder.action(screen, "open_${DiagnosticPrivacy.safeIdentifier(s)}")
        previous = screen; screen = s
    }
    fun saveOverlays(current: VideoProject, requested: List<Overlay>, preferredId: String?,
                     decision: String = "ask"): Boolean {
        val canvasAspect = current.clips.firstOrNull()?.let { mediaAspect(context, it.uri, false) } ?: 16f / 9f
        val slots = requested.map { layer ->
            val aspect = mediaAspect(context, layer.uri, layer.isImage)
            val (w, h) = OverlayEditorLogic.dimensions(layer.scale, aspect, canvasAspect)
            PlacementItem(layer.id, layer.startAtMs, layer.durationMs, layer.x, layer.y, w, h)
        }
        val allowed = requested.flatMap { layer ->
            layer.overlapWith.filter { partner -> requested.any { it.id == partner } }
                .map { OverlayPlacement.pair(layer.id, it) }
        }.toSet()
        val newConflicts = OverlayPlacement.collisions(slots) - allowed
        if (newConflicts.isNotEmpty() && decision == "ask") {
            pendingOverlap = OverlapConfirmation(current, requested, preferredId.orEmpty(), newConflicts)
            return false
        }
        val approvedPairs = if (decision == "keep") allowed + newConflicts else allowed
        val updated = requested.map { layer ->
            if (decision == "keep" && layer.id == preferredId) {
                val linked = newConflicts.mapNotNull { (a, b) ->
                    when (layer.id) { a -> b; b -> a; else -> null }
                }
                layer.copy(overlapWith = (layer.overlapWith + linked).distinct())
            } else layer
        }
        val resolved = OverlayPlacement.arrange(slots, preferredId, approvedPairs)
        if (resolved == null) {
            android.widget.Toast.makeText(context,
                "Não há espaço para distribuir esses PiPs. Reduza o tamanho ou ajuste a duração.",
                android.widget.Toast.LENGTH_LONG).show()
            return false
        }
        write(current.copy(overlays = updated.map { layer ->
            val (x, y) = resolved.getValue(layer.id)
            layer.copy(x = x, y = y)
        }))
        return true
    }
    fun insertOverlay(uri: Uri, isImage: Boolean, mediaMs: Long, allowTimeOverlap: Boolean = false) {
        val current = project ?: return
        val replacing = mediaAction == "replace"
        if (!replacing && current.overlays.size >= OverlayPlacement.MAX_PIPS) {
            android.widget.Toast.makeText(context, "Limite de quatro PiPs atingido", android.widget.Toast.LENGTH_SHORT).show()
            screen = "pip"; return
        }
        val previousPip = if (replacing) current.overlays.firstOrNull { it.id == selectedOverlayId } else null
        val sequential = if (current.overlays.isEmpty()) positionMs.coerceIn(0L, (current.durationMs - 1).coerceAtLeast(0L))
            else OverlayTiming.nextStart(current.overlays, current.durationMs)
        if (previousPip == null && sequential == null && !allowTimeOverlap) {
            pendingSequential = SequentialConfirmation(uri, isImage, mediaMs)
            screen = "pip"
            return
        }
        val start = (previousPip?.startAtMs ?: sequential ?:
            OverlayTiming.suggestedOverlapStart(current.overlays, current.durationMs))
            .coerceIn(0L, (current.durationMs - 1).coerceAtLeast(0L))
        val remaining = (current.durationMs - start).coerceAtLeast(1L)
        val duration = (if (isImage) (previousPip?.durationMs ?: 5000L) else mediaMs)
            .coerceIn(1L, remaining)
        val added = previousPip?.copy(uri = uri.toString(), isImage = isImage,
            durationMs = duration) ?: Overlay(uri.toString(), isImage, duration,
            startAtMs = start, scale = .30f)
        val requested = if (previousPip != null) current.overlays.map {
            if (it.id == previousPip.id) added else it
        } else current.overlays + added
        if (saveOverlays(current, requested, added.id)) selectedOverlayId = added.id
        screen = "pip"
    }
    var selectedBoundary by remember { mutableIntStateOf(0) }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) {
            val clips = uris.map { uri ->
                runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                Clip(uri = uri.toString(), title = mediaName(context, uri), sourceDurationMs = mediaDuration(context, uri))
            }
            val base = if (pendingPicker == "new") VideoProject() else project ?: VideoProject()
            write(base.copy(clips = base.clips + clips))
            if (selectedClip.isBlank()) selectedClip = clips.first().id
            screen = "editor"
        }
    }
    val overlayPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            val image = context.contentResolver.getType(uri)?.startsWith("image/") == true
            val duration = (if (image) 5000L else mediaDuration(context, uri))
                .coerceAtMost(((project?.durationMs ?: 5000) - positionMs).coerceAtLeast(1L))
            insertOverlay(uri, image, duration)
        }
    }
    val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            project?.let { current ->
                val lastStart = (current.durationMs - AudioTrackTiming.MIN_TRIM_MS).coerceAtLeast(0L)
                val start = positionMs.coerceIn(0L, lastStart)
                val sourceDuration = mediaDuration(context, uri)
                val length = minOf(sourceDuration, current.durationMs - start)
                if (length >= AudioTrackTiming.MIN_TRIM_MS) {
                    val track = AudioTrack(uri.toString(), mediaName(context, uri),
                        startMs = start, durationMs = length)
                    write(current.copy(audioTracks = current.audioTracks + track))
                } else {
                    android.widget.Toast.makeText(context, "Sem espaço para inserir áudio no projeto",
                        android.widget.Toast.LENGTH_LONG).show()
                }
            }
            screen = "audio"
        }
    }
    fun back() { DiagnosticRecorder.action(screen, "back"); screen = when (screen) {
        "editor" -> "home"
        "home" -> "home"
        "media" -> mediaOrigin
        "projects", "settings" -> "home"
        "general", "appearance", "about", "diagnostics" -> "settings"
        "pip", "audio", "trim", "transition", "subtitles", "export" -> "editor"
        else -> previous
    } }
    BackHandler(screen != "home") { back() }
    LaunchedEffect(screen, diagnosticsEnabled) { DiagnosticRecorder.screen(screen) }
    MaterialTheme(colorScheme = palette) {
        val tapMonitor = if (diagnosticsEnabled) Modifier.pointerInput(screen) {
            val touches = mutableMapOf<PointerId, androidx.compose.ui.geometry.Offset>()
            val tolerance = viewConfiguration.touchSlop * 1.5f
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Final)
                    event.changes.forEach { change ->
                        when {
                            !change.previousPressed && change.pressed -> touches[change.id] = change.position
                            change.previousPressed && !change.pressed -> {
                                val start = touches.remove(change.id)
                                if (start != null && abs(start.x - change.position.x) <= tolerance &&
                                    abs(start.y - change.position.y) <= tolerance) {
                                    DiagnosticRecorder.tap(screen, change.position.x, change.position.y,
                                        size.width, size.height)
                                }
                            }
                        }
                    }
                }
            }
        } else Modifier
        Surface(modifier = Modifier.fillMaxSize().then(tapMonitor), color = MaterialTheme.colorScheme.background) {
            when (screen) {
                "home" -> MainHome(projects, onCreate = { pendingPicker = "new"; selectedClip = ""; videoPicker.launch(arrayOf("video/*")) },
                    onOpen = { project = it; selectedClip = TrimTimelineEdit.resumeClipId(it); screen = "editor" },
                    onDelete = { store.delete(it.id); projects = store.load() },
                    onSettings = { screen = "settings" }, onTab = { screen = it })
                "projects" -> ProjectsScreen(projects, { back() }, { project = it; selectedClip = TrimTimelineEdit.resumeClipId(it); screen = "editor" },
                    { store.delete(it.id); projects = store.load() })
                "settings" -> SettingsScreen({ back() }) { screen = it }
                "diagnostics" -> DiagnosticScreen(::back, onEnabledChanged = { diagnosticsEnabled = it })
                "general" -> GeneralScreen(store, ::back)
                "appearance" -> AppearanceScreen(theme, { theme = it; store.setTheme(it) }, ::back)
                "about" -> AboutScreen(::back)
                "editor" -> project?.let { p -> EditorScreen(p, selectedClip, positionMs,
                    onSelected = { selectedClip = it }, onPosition = { positionMs = it }, onBack = ::back,
                    onEdit = { navTo(it) }, onUpdate = { write(it) },
                    onAddVideo = { pendingPicker = "append"; videoPicker.launch(arrayOf("video/*")) },
                    onPiP = {
                        selectedOverlayId = project?.overlays?.firstOrNull()?.id.orEmpty()
                        screen = "pip"
                    },
                    onAudio = { audioPicker.launch(arrayOf("audio/*")) },
                    onSelectAudio = { id -> selectedAudioTrackId = id; navTo("audio") },
                    onSelectSubtitle = { id -> selectedSubtitleId = id; navTo("subtitles") },
                    onSelectOverlay = { selectedOverlayId = it },
                    onTransition = { index -> selectedBoundary = index; screen = "transition" },
                    onSaveHome = { write(p); screen = "home" }) }
                "trim" -> project?.let { TrimScreen(it, selectedClip, positionMs, ::back, { write(it) },
                    { selectedClip = it }, onSaveHome = { saved -> write(saved); screen = "home" }) }
                "audio" -> project?.let { AudioScreen(it, ::back, { write(it) },
                    { audioPicker.launch(arrayOf("audio/*")) }, selectedAudioTrackId,
                    onSelectionConsumed = { selectedAudioTrackId = null }) }
                "pip" -> project?.let { current -> PipScreen(current, selectedOverlayId,
                    onSelect = { selectedOverlayId = it }, back = ::back,
                    onChange = { changed ->
                        saveOverlays(current, current.overlays.map {
                            if (it.id == changed.id) changed else it
                        }, changed.id)
                    },
                    onRemove = { id ->
                        write(current.copy(overlays = current.overlays.filterNot { it.id == id }))
                        selectedOverlayId = current.overlays.firstOrNull { it.id != id }?.id.orEmpty()
                    },
                    choose = { replace ->
                        if (!replace && current.overlays.size >= OverlayPlacement.MAX_PIPS) {
                            android.widget.Toast.makeText(context, "Limite de quatro PiPs atingido",
                                android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            mediaAction = if (replace) "replace" else "add"
                            mediaOrigin = "pip"; screen = "media"
                        }
                    }) }
                "media" -> DeviceMediaBrowser(::back, onSelect = { chosen ->
                    val videoMs = if (chosen.isImage) 5000L else
                        chosen.durationMs.takeIf { it > 0 } ?: mediaDuration(context, chosen.uri)
                    insertOverlay(chosen.uri, chosen.isImage, videoMs)
                }, openDocuments = { overlayPicker.launch(arrayOf("image/*", "video/*")) })
                "transition" -> project?.let { TransitionScreen(it, ::back, { write(it) }, selectedBoundary) }
                "subtitles" -> project?.let { SubtitleScreen(it, ::back, { write(it) }, selectedSubtitleId,
                    onSaveHome = { write(it); screen = "home" }) }
                "export" -> project?.let { ExportScreen(it, store.quality(), ::back) }
            }
        }
        pendingSequential?.let { pending ->
            AlertDialog(onDismissRequest = { pendingSequential = null },
                icon = { Icon(Icons.Default.Schedule, null) },
                title = { Text("O PiP anterior já chega ao final") },
                text = { Text("Não há tempo disponível depois do último PiP. Quer adicionar o novo no mesmo intervalo do anterior? A posição será conferida para evitar que cubra outro PiP.") },
                confirmButton = { TextButton(onClick = {
                    pendingSequential = null
                    insertOverlay(pending.uri, pending.isImage, pending.mediaMs, allowTimeOverlap = true)
                }) { Text("Adicionar no mesmo período") } },
                dismissButton = { TextButton(onClick = { pendingSequential = null }) { Text("Cancelar") } })
        }
        pendingOverlap?.let { pending ->
            AlertDialog(onDismissRequest = { pendingOverlap = null },
                icon = { Icon(Icons.Default.Layers, null) },
                title = { Text("PiPs sobrepostos") },
                text = { Text("Os PiPs ocupam a mesma região durante parte do vídeo. Você pode reorganizá-los automaticamente ou confirmar que deseja manter um PiP sobre o outro.") },
                confirmButton = { Column {
                    TextButton(onClick = {
                        pendingOverlap = null
                        if (saveOverlays(pending.project, pending.requested, pending.preferredId, "keep")) {
                            selectedOverlayId = pending.preferredId
                        }
                    }) { Text("Manter sobreposição") }
                    TextButton(onClick = {
                        pendingOverlap = null
                        if (saveOverlays(pending.project, pending.requested, pending.preferredId, "rearrange")) {
                            selectedOverlayId = pending.preferredId
                        }
                    }) { Text("Reposicionar automaticamente") }
                } },
                dismissButton = { TextButton(onClick = { pendingOverlap = null }) { Text("Cancelar") } })
        }
    }
}

private fun mediaName(context: android.content.Context, uri: Uri): String {
    val cursor = context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
    return cursor?.use { if (it.moveToFirst()) it.getString(0) else "Mídia" } ?: "Mídia"
}

@Composable
private fun Heading(title: String, back: () -> Unit, accessory: @Composable (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().height(62.dp), horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = back) { Icon(Icons.Default.ArrowBack, "Voltar") }
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Medium)
        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) { accessory?.invoke() }
    }
}

@Composable private fun BlueButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(onClick, modifier.height(56.dp), enabled = enabled, shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Blue)) { Text(label, fontWeight = FontWeight.SemiBold) }
}

@Composable private fun ItemCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String,
                                onClick: () -> Unit) {
    Surface(onClick, shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Blue, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.SemiBold); Text(subtitle,
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable private fun MainHome(projects: List<VideoProject>, onCreate: () -> Unit,
    onOpen: (VideoProject)->Unit, onDelete: (VideoProject)->Unit, onSettings: ()->Unit, onTab:(String)->Unit) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Movie, null, tint = Blue, modifier = Modifier.size(30.dp))
            Spacer(Modifier.width(12.dp))
            Text("Uai Cortes", fontSize = 25.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, "Configurações") }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal=20.dp)) {
            Surface(color = Blue, shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.MovieCreation, null, tint = Color.White, modifier=Modifier.size(32.dp))
                    Text("Nova edição", fontSize=25.sp, fontWeight=FontWeight.Bold, color=Color.White)
                    Text("Importe seus vídeos e comece a editar.", color=Color.White)
                    Button(onClick=onCreate, modifier=Modifier.fillMaxWidth().height(52.dp),
                        colors=ButtonDefaults.buttonColors(containerColor=Color.White),
                        shape=RoundedCornerShape(30.dp)) {
                        Icon(Icons.Default.Add,null,tint=Blue);Spacer(Modifier.width(8.dp));Text("Criar projeto",color=Blue)
                    }
                }
            }
            Spacer(Modifier.height(24.dp)); Text("Edições recentes",fontSize=21.sp, fontWeight=FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            if(projects.isEmpty()) Text("Seus projetos aparecerão aqui.", color=MaterialTheme.colorScheme.onSurfaceVariant,
                modifier=Modifier.padding(16.dp))
            projects.take(5).forEach { ProjectItem(it,onOpen,onDelete) }
        }
        MainNav("home", onTab)
    }
}

@Composable private fun MainNav(active: String, onTab:(String)->Unit) {
    Row(Modifier.fillMaxWidth().navigationBarsPadding().height(72.dp)
        .background(MaterialTheme.colorScheme.surface), horizontalArrangement=Arrangement.SpaceEvenly) {
        listOf(Triple("home",Icons.Default.Home,"Início"),Triple("projects",Icons.Default.Folder,"Projetos"),
            Triple("settings",Icons.Default.Settings,"Ajustes")).forEach { (page,icon,label) ->
            Column(Modifier.weight(1f).fillMaxHeight().clickable {onTab(page)},horizontalAlignment=Alignment.CenterHorizontally,
                verticalArrangement=Arrangement.Center) {
                Icon(icon,label,tint=if(active==page) Blue else MaterialTheme.colorScheme.onSurfaceVariant)
                Text(label,fontSize=12.sp,color=if(active==page) Blue else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable private fun ProjectItem(p:VideoProject,onOpen:(VideoProject)->Unit,onDelete:(VideoProject)->Unit){
    var menu by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().clickable {onOpen(p)}.padding(vertical=12.dp), verticalAlignment=Alignment.CenterVertically) {
        Box(Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment=Alignment.Center) {
            Icon(Icons.Default.Videocam,null,tint=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(p.title,fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis)
            Text("Rascunho salvo · ${formatMs(p.durationMs)}",style=MaterialTheme.typography.bodySmall,
                color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box {
            IconButton(onClick={menu=true}) {Icon(Icons.Default.MoreVert,"Opções")}
            DropdownMenu(expanded=menu,onDismissRequest={menu=false}) {
                DropdownMenuItem(text={Text("Excluir projeto")},onClick={menu=false;onDelete(p)})
            }
        }
    }
    HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant.copy(alpha=0.45f))
}

@Composable private fun ProjectsScreen(projects: List<VideoProject>,back:()->Unit,open:(VideoProject)->Unit,delete:(VideoProject)->Unit) {
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal=18.dp)) {
        Heading("Projetos",back)
        if (projects.isEmpty()) Text("Nenhum projeto salvo.",modifier=Modifier.padding(24.dp))
        LazyColumn { items(projects,key={it.id}) { ProjectItem(it,open,delete) } }
    }
}

@Composable private fun SettingsScreen(back:()->Unit, onClick:(String)->Unit) {
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal=18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Heading("Configurações",back)
        ItemCard(Icons.Default.Tune,"Geral","Qualidade e armazenamento") {onClick("general")}
        ItemCard(Icons.Default.Palette,"Tela","Sistema, claro, escuro e AMOLED") {onClick("appearance")}
        ItemCard(Icons.Default.BugReport,"Diagnóstico","Capturar erros e toques, exportar relatório") {onClick("diagnostics")}
        ItemCard(Icons.Default.Info,"Sobre","Versão e informações do aplicativo") {onClick("about")}
    }
}

@Composable private fun GeneralScreen(store:ProjectStore,back:()->Unit) {
    var quality by remember { mutableIntStateOf(store.quality()) }
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal=20.dp)) {
        Heading("Geral",back)
        Text("Qualidade padrão",fontWeight=FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            listOf(720,1080).forEach { q ->
                OutlinedButton(onClick={quality=q;store.setQuality(q)},border=BorderStroke(2.dp,if(quality==q) Blue else MaterialTheme.colorScheme.outlineVariant)) {
                    Text("${q}p",color=if(quality==q) Blue else MaterialTheme.colorScheme.onSurface)
                }
            }
        }
        Spacer(Modifier.height(24.dp));Text("Os vídeos são salvos em Filmes/UaiCortes.", color=MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp)); Text("A edição ocorre no aparelho, sem enviar arquivos para a internet.",
            color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable private fun AppearanceScreen(current:String,set:(String)->Unit,back:()->Unit) {
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal=20.dp)) {
        Heading("Tela",back)
        listOf("Sistema","Claro","Escuro","AMOLED").forEach { option ->
            Row(Modifier.fillMaxWidth().clickable {set(option)}.height(60.dp),verticalAlignment=Alignment.CenterVertically) {
                RadioButton(selected=current==option,onClick={set(option)})
                Spacer(Modifier.width(12.dp));Text(option)
            };HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable private fun AboutScreen(back:()->Unit) {
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal=20.dp),horizontalAlignment=Alignment.CenterHorizontally) {
        Heading("Sobre",back)
        Icon(Icons.Default.MovieCreation,null,tint=Blue,modifier=Modifier.size(70.dp))
        Spacer(Modifier.height(14.dp));Text("Uai Cortes",fontSize=24.sp,fontWeight=FontWeight.Bold)
        Text("Versão 0.1.42 (43)",color=MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp));Text("Editor de vídeo local para Android.",textAlign=androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(14.dp));Text("Novidades: trechos visuais em Cortar e unir, seleção automática após dividir e cursor no último corte.",
            color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(UnstableApi::class)
@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorScreen(p:VideoProject, selectedId:String, currentMs:Long,
    onSelected:(String)->Unit,onPosition:(Long)->Unit,onBack:()->Unit,onEdit:(String)->Unit,
    onUpdate:(VideoProject)->Unit,onAddVideo:()->Unit,onPiP:()->Unit,onAudio:()->Unit,
    onSelectOverlay:(String)->Unit,onTransition:(Int)->Unit,onSaveHome:()->Unit,
    onSelectAudio:(String)->Unit,onSelectSubtitle:(String)->Unit) {
    val context=LocalContext.current
    val player=remember { diagnosticPlayer(context).apply { repeatMode = Player.REPEAT_MODE_ALL } }
    var playbackSpeed by remember(p.id) { mutableFloatStateOf(1f) }
    var clipMenuId by remember(p.id) { mutableStateOf<String?>(null) }
    var deletionRequested by remember(p.id) { mutableStateOf<String?>(null) }
    var feedback by remember(p.id) { mutableStateOf("") }
    LaunchedEffect(player, p.clips.map { it.id to it.volume }) {
        while (true) {
            player.volume = p.clips.getOrNull(player.currentMediaItemIndex)?.volume?.coerceIn(0f,1f) ?: 1f
            delay(120)
        }
    }
    LaunchedEffect(player, playbackSpeed) { player.setPlaybackParameters(PlaybackParameters(playbackSpeed)) }
    DisposableEffect(player) { onDispose { player.release() } }
    LaunchedEffect(p.clips.map { Triple(it.uri, it.startMs, it.endMs) }) {
        player.setMediaItems(p.clips.map { clip ->
            MediaItem.Builder().setUri(clip.uri).setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder().setStartPositionMs(clip.startMs)
                    .setEndPositionMs(clip.endMs).build()).build()
        })
        player.prepare()
    }
    LaunchedEffect(player,p.clips) {
        while(true) {
            val index=player.currentMediaItemIndex.coerceIn(0,(p.clips.size-1).coerceAtLeast(0))
            val before=p.clips.take(index).sumOf {it.durationMs}
            onPosition((before+player.currentPosition).coerceIn(0,p.durationMs))
            delay(200)
        }
    }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().height(66.dp).padding(horizontal=8.dp),verticalAlignment=Alignment.CenterVertically) {
            IconButton(onClick=onBack){Icon(Icons.Default.ArrowBack,"Voltar")}
            Column(Modifier.weight(1f),horizontalAlignment=Alignment.CenterHorizontally) {
                Text(p.title,fontWeight=FontWeight.Medium,maxLines=1,overflow=TextOverflow.Ellipsis)
                Text("Projeto em edição",style=MaterialTheme.typography.bodySmall,
                    color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            BlueButton("Exportar",{ onEdit("export") },Modifier.width(102.dp),enabled=p.clips.isNotEmpty())
        }
        // Separate audio players track the editor playhead without modifying source media.
        p.music?.let { music ->
            TrackAudioPreview(music.uri, 0L, p.durationMs, music.volume, currentMs,
                player.isPlaying, playbackSpeed, true)
        }
        p.audioTracks.forEach { audio ->
            key(audio.id) {
                TrackAudioPreview(audio.uri, audio.startMs, audio.durationMs, audio.volume,
                    currentMs, player.isPlaying, playbackSpeed, false, audio.sourceStartMs)
            }
        }
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)
            .background(Color(0xFF101014)).padding(10.dp), contentAlignment = Alignment.Center) {
            val activeIndex=AdaptiveVideoLayout.activeClipIndex(p.clips.map { it.durationMs },currentMs)
            val previewUri=p.clips.getOrNull(activeIndex)?.uri
            val actualAspect=remember(previewUri) {
                previewUri?.let { mediaAspect(context,it,false) } ?: 16f/9f
            }
            val fit=AdaptiveVideoLayout.fit(maxWidth.value,maxHeight.value,actualAspect)
            val videoW=fit.width.dp
            val videoH=fit.height.dp
            Box(Modifier.width(videoW).height(videoH)) {
                AndroidView(factory = { ctx -> previewPlayer(ctx).apply {
                    this.player = player
                    useController = false
                } }, update = { it.player = player }, modifier = Modifier.fillMaxSize())
                // Fade preview uses the same boundary IDs and durations as the exporter.
                val index = p.clips.indexOfFirst { clip ->
                    val first = p.clips.takeWhile { it.id != clip.id }.sumOf { it.durationMs }
                    currentMs in first until (first + clip.durationMs)
                }.coerceAtLeast(0)
                p.clips.getOrNull(index)?.let { clip ->
                    val before = p.clips.take(index).sumOf { it.durationMs }
                    val incoming = EditFeatures.boundary(p, index - 1)
                    val outgoing = EditFeatures.boundary(p, index)
                    val inMs = EditFeatures.transitionMs(incoming,
                        p.clips.getOrElse(index-1) { clip }.durationMs, clip.durationMs)
                    val outMs = EditFeatures.transitionMs(outgoing, clip.durationMs,
                        p.clips.getOrElse(index+1) { clip }.durationMs)
                    val local = (currentMs - before).coerceIn(0L,clip.durationMs)
                    val alpha = 1f - EditFeatures.fadeFraction(local,clip.durationMs,inMs,outMs)
                    if (alpha > .001f) {
                        val isWhite = if (inMs > 0 && local < inMs)
                            incoming?.kind == "Fade branco" else outgoing?.kind == "Fade branco"
                        Box(Modifier.fillMaxSize().background((if(isWhite) Color.White else Color.Black)
                            .copy(alpha=alpha)))
                    }
                }
                p.overlays.forEach { pip ->
                    if (currentMs >= pip.startAtMs && currentMs < pip.startAtMs + pip.durationMs) {
                        key(pip.id) {
                            PipLayer(pip, currentMs - pip.startAtMs, player.isPlaying,
                                speed = playbackSpeed, editable = false) { }
                        }
                    }
                }
                p.subtitles.firstOrNull { currentMs >= it.startMs && currentMs < it.endMs }?.let { cue ->
                    SubtitleOverlayOnFrame(cue, editable = false, playbackMs=currentMs)
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=6.dp),horizontalArrangement=Arrangement.Center,
            verticalAlignment=Alignment.CenterVertically) {
            IconButton(onClick={player.seekTo((player.currentPosition-5000).coerceAtLeast(0))}) { Icon(Icons.Default.SkipPrevious,"Voltar cinco segundos") }
            IconButton(onClick={if(player.isPlaying)player.pause() else player.play()}) {
                Icon(if(player.isPlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircle, "Reproduzir / pausar",
                    modifier=Modifier.size(35.dp))
            }
            IconButton(onClick={player.seekTo(player.currentPosition+5000)}) { Icon(Icons.Default.SkipNext,"Avançar cinco segundos") }
            PlaybackSpeedButton(playbackSpeed, onSelect = { playbackSpeed = it })
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text("Vídeo:", style = MaterialTheme.typography.bodySmall)
            EditableSeekTime(currentMs, p.durationMs, onSeek = { ms ->
                seekProjectPosition(player, p.clips, ms)
                onPosition(ms)
            }, modifier = Modifier.weight(1f))
        }
        // Passos temporais explícitos evitam tentar acertar um quadro em uma barra curta.
        Row(Modifier.fillMaxWidth().padding(horizontal=16.dp),
            verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(5.dp)) {
            Text("Ajuste fino",fontSize=11.sp,modifier=Modifier.weight(1f))
            listOf(-1000L to "−1s", -100L to "−0,1s",
                100L to "+0,1s", 1000L to "+1s").forEach { (delta,label) ->
                OutlinedButton(onClick={
                    val dest=TimelinePrecision.step(currentMs,delta,p.durationMs)
                    seekProjectPosition(player,p.clips,dest)
                    onPosition(dest)
                },contentPadding=PaddingValues(horizontal=7.dp,vertical=2.dp),
                    modifier=Modifier.height(32.dp),enabled=p.durationMs>0) {
                    Text(label,fontSize=11.sp)
                }
            }
        }
        if(p.durationMs>0) Slider(value=currentMs.toFloat().coerceIn(0f,p.durationMs.toFloat()),
            onValueChange={global ->
                var t=global.toLong()
                for ((i,c) in p.clips.withIndex()) {
                    if(t <= c.durationMs || i==p.clips.lastIndex) { player.seekTo(i,t.coerceIn(0,c.durationMs)); break }
                    t -= c.durationMs
                }
            },valueRange=0f..p.durationMs.toFloat(),modifier=Modifier.fillMaxWidth().padding(horizontal=16.dp))
        TimelineRow("Vídeo",Blue,p.clips.map{ it.title to it.durationMs },p.durationMs,currentMs,selectedId,
            onClip={ id -> onSelected(p.clips[id].id); clipMenuId = p.clips[id].id },
            onBoundary=onTransition)
        OverlayTimeline(p.overlays, p.durationMs, onClick = { id -> onSelectOverlay(id); onEdit("pip") })
        InteractiveAudioTimeline(p, onSelectTrack = onSelectAudio,
            onMoveTrack = { trackId, start ->
                onUpdate(p.copy(audioTracks = p.audioTracks.map { audio ->
                    if (audio.id == trackId) audio.copy(startMs = start) else audio
                }))
                feedback = "Posição da faixa atualizada."
            }, onLegacyClick = { onEdit("audio") })
        SubtitleOverviewTimeline(p.subtitles,p.durationMs,onSelectSubtitle,
            onOpen={onEdit("subtitles")})
        Spacer(Modifier.height(5.dp));HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
        // Ferramentas em carrossel circular: salvar é um cartão, não um botão solto.
        // Começamos no centro de Int.MAX_VALUE para permitir deslizar nas duas direções.
        val actions = remember { listOf(
            Triple("Salvar", Icons.Default.Save, "save"),
            Triple("Cortar", Icons.Default.ContentCut, "trim"),
            Triple("Unir", Icons.Default.Add, "add"),
            Triple("PiP", Icons.Default.Layers, "pip"),
            Triple("Áudio", Icons.Default.MusicNote, "audio"),
            Triple("Transição", Icons.Default.AutoAwesomeMotion, "transition"),
            Triple("Legendas", Icons.Default.ClosedCaption, "subtitles")
        ) }
        val carouselStart = (Int.MAX_VALUE / 2 / actions.size) * actions.size
        val toolsScroll = rememberLazyListState(initialFirstVisibleItemIndex = carouselStart)
        LazyRow(state = toolsScroll, modifier = Modifier.fillMaxWidth().navigationBarsPadding()
            .padding(vertical = 10.dp), contentPadding = PaddingValues(horizontal = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            items(count = Int.MAX_VALUE) { index ->
                val (name, icon, key) = actions[index % actions.size]
                Column(Modifier.width(66.dp).clip(RoundedCornerShape(12.dp)).clickable {
                    when (key) {
                        "save" -> onSaveHome()
                        "add" -> onAddVideo()
                        "pip" -> onPiP()
                        "audio" -> onEdit("audio")
                        else -> onEdit(key)
                    }
                }, horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(52.dp).clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                        Icon(icon, name, modifier = Modifier.size(22.dp))
                    }
                    Text(name, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        if (feedback.isNotBlank()) Text(feedback, modifier=Modifier.padding(horizontal=12.dp,vertical=3.dp),
            color=MaterialTheme.colorScheme.primary, style=MaterialTheme.typography.bodySmall)
    }
    val active = p.clips.firstOrNull { it.id == clipMenuId }
    if (active != null) {
        ModalBottomSheet(onDismissRequest={clipMenuId=null}, dragHandle=null,
            sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true)) {
            Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
                verticalArrangement=Arrangement.spacedBy(9.dp)) {
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Editar clipe",fontWeight=FontWeight.Bold,fontSize=19.sp)
                        Text(active.title, maxLines=1,overflow=TextOverflow.Ellipsis,
                            style=MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick={clipMenuId=null}) {Icon(Icons.Default.Close,"Fechar")}
                }
                val idx=p.clips.indexOfFirst { it.id == active.id }
                fun commit(clips: List<Clip>, note: String) {
                    onUpdate(EditFeatures.repositionAfterDelete(p,clips))
                    feedback=note
                    clipMenuId=null
                }
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                    ClipSheetAction("Cortar",Icons.Default.ContentCut) {clipMenuId=null;onEdit("trim")}
                    ClipSheetAction("Duplicar",Icons.Default.ContentCopy) {
                        val list=p.clips.toMutableList();list.add(idx+1,active.copy(id=UUID.randomUUID().toString()))
                        commit(list,"Clipe duplicado")
                    }
                    ClipSheetAction("Antes",Icons.Default.ArrowBack,idx>0) {
                        val list=p.clips.toMutableList(); java.util.Collections.swap(list,idx,idx-1)
                        commit(list,"Clipe movido")
                    }
                    ClipSheetAction("Depois",Icons.Default.ArrowForward,idx<p.clips.lastIndex) {
                        val list=p.clips.toMutableList();java.util.Collections.swap(list,idx,idx+1)
                        commit(list,"Clipe movido")
                    }
                    ClipSheetAction("Excluir",Icons.Default.Delete) {
                        deletionRequested = active.id
                    }
                }
                Text("Volume apenas deste clipe: ${(active.volume*100).toInt()}%")
                Slider(value=active.volume.coerceIn(0f,1f), onValueChange={ v ->
                    onUpdate(p.copy(clips=p.clips.map { if(it.id==active.id) it.copy(volume=v) else it }))
                })
                Text("Excluir ou duplicar não altera o arquivo original.",
                    style=MaterialTheme.typography.bodySmall,
                    color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    deletionRequested?.let { id ->
        AlertDialog(onDismissRequest={deletionRequested=null},
            title={Text("Remover este clipe?")},
            text={Text("O trecho sairá da linha do tempo, sem apagar o arquivo original do aparelho.")},
            confirmButton={TextButton(onClick={
                onUpdate(EditFeatures.repositionAfterDelete(p,p.clips.filterNot{it.id==id}))
                clipMenuId=null;deletionRequested=null;feedback="Clipe excluído do projeto"
            }) { Text("Excluir") }},
            dismissButton={TextButton(onClick={deletionRequested=null}) {Text("Cancelar")}})
    }
}

@Composable
private fun TrackAudioPreview(uri: String, startMs: Long, durationMs: Long,
    volume: Float, projectMs: Long, playing: Boolean, speed: Float, loop: Boolean,
    sourceStartMs: Long = 0L) {
    val context=LocalContext.current
    val player=remember(uri) { diagnosticPlayer(context) }
    DisposableEffect(player) { onDispose {player.release()} }
    LaunchedEffect(player,uri,loop) {
        player.repeatMode=if(loop)Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        player.setMediaItem(MediaItem.fromUri(uri));player.prepare()
    }
    LaunchedEffect(player,volume,speed) {
        player.volume=volume.coerceIn(0f,1f)
        player.setPlaybackParameters(PlaybackParameters(speed))
    }
    LaunchedEffect(player,projectMs,playing,startMs,durationMs,sourceStartMs) {
        val within=projectMs>=startMs && projectMs<startMs+durationMs
        if(!within || !playing) player.pause() else {
            val wanted=sourceStartMs+(projectMs-startMs).coerceAtLeast(0L)
            if(kotlin.math.abs(player.currentPosition-wanted)>600L) player.seekTo(wanted)
            player.play()
        }
    }
}

@Composable
private fun RowScope.ClipSheetAction(label: String, icon: ImageVector,
    enabled: Boolean = true, click: () -> Unit) {
    OutlinedButton(onClick=click,enabled=enabled,modifier=Modifier.weight(1f),
        contentPadding=PaddingValues(horizontal=2.dp,vertical=8.dp),
        shape=RoundedCornerShape(7.dp)) {
        Column(horizontalAlignment=Alignment.CenterHorizontally) {
            Icon(icon,label,modifier=Modifier.size(18.dp))
            Text(label,fontSize=10.sp,maxLines=1)
        }
    }
}

@Composable
private fun TimelineRow(label:String,color:Color, segments:List<Pair<String,Long>>,total:Long,position:Long,
    selectedId:String,onClip:(Int)->Unit,onBoundary:((Int)->Unit)?=null) {
    Row(Modifier.fillMaxWidth().height(34.dp).padding(horizontal=12.dp,vertical=2.dp),
        verticalAlignment=Alignment.CenterVertically) {
        Box(Modifier.width(60.dp).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(color),
            contentAlignment=Alignment.Center) { Text(label,color=Color.White,fontSize=11.sp) }
        Spacer(Modifier.width(6.dp))
        BoxWithConstraints(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(3.dp))
            .background(color.copy(alpha=.14f))) {
            // Captura a largura no escopo de BoxWithConstraints: o Row interno muda o receptor.
            val timelineWidth = maxWidth
            Row(Modifier.fillMaxSize()) {
                segments.forEachIndexed { index,(title,duration) ->
                    val fraction = (duration.toFloat()/total.coerceAtLeast(1L)).coerceIn(0f,1f)
                    Box(Modifier.width(timelineWidth*fraction).fillMaxHeight().padding(end=1.dp)
                        .background(color.copy(alpha=.36f)).clickable { onClip(index) },
                        contentAlignment=Alignment.Center) {
                        Text(title,maxLines=1,overflow=TextOverflow.Ellipsis,fontSize=10.sp,
                            modifier=Modifier.padding(horizontal=2.dp))
                    }
                }
            }
            if (onBoundary != null && segments.size > 1) {
                var elapsed = 0L
                segments.dropLast(1).forEachIndexed { index, segment ->
                    elapsed += segment.second
                    val offset = timelineWidth * (elapsed.toFloat() / total.coerceAtLeast(1L))
                    Box(Modifier.offset(x = offset - 11.dp).width(22.dp).fillMaxHeight()
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(5.dp))
                        .clickable { onBoundary(index) }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.AutoAwesomeMotion, "Editar transição ${index+1}",
                            tint=MaterialTheme.colorScheme.onPrimary,modifier=Modifier.size(15.dp))
                    }
                }
            }
        }
    }
}

/** Superfície gestual de timeline. Acumular pixels antes da conversão impede
 * perda de deslocamentos pequenos durante gestos lentos.
 */
@Composable
private fun AudioDragSurface(
    modifier: Modifier, totalMs: Long, widthPx: Float,
    onStep: (Long) -> Unit, onFinish: () -> Unit = {},
    onTap: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val currentStep by rememberUpdatedState(onStep)
    val currentFinish by rememberUpdatedState(onFinish)
    val currentTap by rememberUpdatedState(onTap)
    val clickModifier = if (onTap != null) Modifier.clickable { currentTap?.invoke() } else Modifier
    Box(modifier.then(clickModifier).pointerInput(totalMs, widthPx) {
        var accumulatedPx = 0f
        var appliedMs = 0L
        detectDragGestures(
            onDragStart = { accumulatedPx = 0f; appliedMs = 0L },
            onDragEnd = { currentFinish() },
            onDragCancel = {},
            onDrag = { change, amount ->
                change.consume()
                accumulatedPx += amount.x
                val newMs = AudioTimelineEdit.pixelsToMs(accumulatedPx, widthPx, totalMs)
                val delta = newMs - appliedMs
                if (delta != 0L) { currentStep(delta); appliedMs = newMs }
            })
    }, content = content)
}

/** Na tela principal é possível arrastar cada faixa para outro momento.
 * O movimento é persistido apenas quando o gesto termina, evitando gravação em cada pixel.
 * Toque curto abre o editor detalhado da faixa.
 */
@Composable
private fun InteractiveAudioTimeline(p: VideoProject,
    onSelectTrack: (String) -> Unit, onMoveTrack: (String, Long) -> Unit,
    onLegacyClick: () -> Unit) {
    val totalMs = p.durationMs.coerceAtLeast(1L)
    Row(Modifier.fillMaxWidth().height(38.dp).padding(horizontal=12.dp, vertical=2.dp),
        verticalAlignment=Alignment.CenterVertically) {
        Box(Modifier.width(60.dp).fillMaxHeight().background(Green, RoundedCornerShape(3.dp)),
            contentAlignment=Alignment.Center) { Text("Áudio", color=Color.White, fontSize=11.sp) }
        Spacer(Modifier.width(6.dp))
        BoxWithConstraints(Modifier.weight(1f).fillMaxHeight()
            .clip(RoundedCornerShape(3.dp)).background(Green.copy(alpha=.14f))) {
            val timelineWidth = maxWidth
            val timelineWidthPx = with(LocalDensity.current) { timelineWidth.toPx() }
            p.music?.let { music ->
                Box(Modifier.fillMaxSize().background(Green.copy(alpha=.16f))
                    .clickable(onClick=onLegacyClick), contentAlignment=Alignment.CenterStart) {
                    Text(music.title, modifier=Modifier.padding(start=3.dp),
                        fontSize=9.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
                }
            }
            p.audioTracks.forEach { audio ->
                key(audio.id) {
                    var candidateStart by remember(audio.id, audio.startMs) { mutableLongStateOf(audio.startMs) }
                    var changed by remember(audio.id, audio.startMs) { mutableStateOf(false) }
                    val range = AudioTimelineDraft(candidateStart, audio.sourceStartMs,
                        audio.sourceStartMs + audio.durationMs)
                    val startFraction = (candidateStart.toFloat()/totalMs).coerceIn(0f,1f)
                    val endFraction = (audio.durationMs.toFloat()/totalMs).coerceIn(0f,1f-startFraction)
                    val drawnWidth = (timelineWidth*endFraction).coerceAtLeast(22.dp).coerceAtMost(timelineWidth)
                    val drawnX = (timelineWidth*startFraction).coerceAtMost(timelineWidth-drawnWidth)
                    AudioDragSurface(
                        modifier=Modifier.offset(x=drawnX).width(drawnWidth).fillMaxHeight()
                            .background(Green.copy(alpha=.72f),RoundedCornerShape(3.dp))
                            .semantics { contentDescription = "${audio.title}: arrastar para mover; tocar para editar" },
                        totalMs=totalMs, widthPx=timelineWidthPx,
                        onStep={ delta ->
                            candidateStart = AudioTimelineEdit.move(
                                range.copy(timelineStartMs=candidateStart), delta, totalMs).timelineStartMs
                            changed = changed || candidateStart != audio.startMs
                        },
                        onFinish={
                            if (changed && candidateStart != audio.startMs) onMoveTrack(audio.id, candidateStart)
                            changed = false
                        },
                        onTap={onSelectTrack(audio.id)}) {
                        Text(audio.title, maxLines=1, overflow=TextOverflow.Ellipsis,
                            fontSize=10.sp,color=Color.White,modifier=Modifier.align(Alignment.Center).padding(horizontal=4.dp))
                    }
                }
            }
        }
    }
}

/** Ferramenta visual de três gestos independentes: início, deslocamento, fim.
 * Os campos numéricos permanecem disponíveis para recortes menores que as alças.
 */
@Composable
private fun AudioTrimGestureTimeline(draft: AudioTimelineDraft, projectMs: Long, sourceMs: Long,
    onMove: (Long) -> Unit, onLeftTrim: (Long) -> Unit, onRightTrim: (Long) -> Unit) {
    val total = projectMs.coerceAtLeast(1L)
    BoxWithConstraints(Modifier.fillMaxWidth().height(74.dp).padding(vertical=12.dp)
        .background(Green.copy(alpha=.12f),RoundedCornerShape(6.dp))) {
        val timelineWidth = maxWidth
        val timelineWidthPx = with(LocalDensity.current) { timelineWidth.toPx() }
        val start = (draft.timelineStartMs.toFloat()/total).coerceIn(0f,1f)
        val length = (draft.durationMs.toFloat()/total).coerceIn(0f,1f-start)
        val w = (timelineWidth*length).coerceAtLeast(42.dp).coerceAtMost(timelineWidth)
        val x = (timelineWidth*start).coerceAtMost(timelineWidth-w)
        val edge = 15.dp
        Box(Modifier.offset(x=x).width(w).fillMaxHeight()
            .background(Green.copy(alpha=.46f),RoundedCornerShape(5.dp)))
        AudioDragSurface(Modifier.offset(x=x+edge).width((w-edge*2).coerceAtLeast(0.dp))
            .fillMaxHeight().semantics { contentDescription = "Arrastar trecho de áudio" },
            totalMs=total,widthPx=timelineWidthPx,onStep=onMove) {
            Icon(Icons.Default.OpenWith,null,tint=Color.White,
                modifier=Modifier.size(17.dp).align(Alignment.Center))
        }
        AudioDragSurface(Modifier.offset(x=x).width(edge).fillMaxHeight()
            .background(Green,RoundedCornerShape(topStart=5.dp,bottomStart=5.dp))
            .semantics { contentDescription = "Ajustar entrada do áudio" },
            totalMs=total,widthPx=timelineWidthPx,onStep=onLeftTrim) {
            Box(Modifier.align(Alignment.Center).width(2.dp).height(22.dp).background(Color.White))
        }
        AudioDragSurface(Modifier.offset(x=x+w-edge).width(edge).fillMaxHeight()
            .background(Green,RoundedCornerShape(topEnd=5.dp,bottomEnd=5.dp))
            .semantics { contentDescription = "Ajustar saída do áudio" },
            totalMs=total,widthPx=timelineWidthPx,
            onStep={ if (sourceMs >= AudioTrackTiming.MIN_TRIM_MS) onRightTrim(it) }) {
            Box(Modifier.align(Alignment.Center).width(2.dp).height(22.dp).background(Color.White))
        }
    }
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
        Text(formatPipMs(draft.timelineStartMs),fontSize=11.sp,
            color=MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Duração ${formatPipMs(draft.durationMs.coerceAtLeast(0L))}",fontSize=11.sp,
            color=MaterialTheme.colorScheme.onSurfaceVariant)
        Text(formatPipMs(draft.timelineEndMs),fontSize=11.sp,
            color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Pista compacta no editor. Cada bloco abre a legenda correta, mesmo quando
 * as legendas foram importadas fora da ordem cronológica.
 */
@Composable
private fun SubtitleOverviewTimeline(cues:List<SubtitleCue>,totalMs:Long,
    onSelect:(String)->Unit,onOpen:()->Unit) {
    val gold=Color(0xFFC38A38)
    Row(Modifier.fillMaxWidth().height(34.dp).padding(horizontal=12.dp,vertical=2.dp),
        verticalAlignment=Alignment.CenterVertically) {
        Box(Modifier.width(60.dp).fillMaxHeight().background(gold,RoundedCornerShape(3.dp))
            .clickable { onOpen() },contentAlignment=Alignment.Center) {
            Text("Legenda",color=Color.White,fontSize=11.sp)
        }
        Spacer(Modifier.width(6.dp))
        BoxWithConstraints(Modifier.weight(1f).fillMaxHeight()
            .background(gold.copy(alpha=.14f),RoundedCornerShape(3.dp))) {
            val width=maxWidth
            cues.sortedBy { it.startMs }.forEach { cue ->
                key(cue.id) {
                    val start=(cue.startMs.toFloat()/totalMs.coerceAtLeast(1L)).coerceIn(0f,1f)
                    val span=((cue.endMs-cue.startMs).toFloat()/totalMs.coerceAtLeast(1L))
                        .coerceIn(0f,1f-start)
                    val drawn=(width*span).coerceAtLeast(8.dp).coerceAtMost(width)
                    val left=(width*start).coerceAtMost(width-drawn)
                    Box(Modifier.offset(x=left).width(drawn).fillMaxHeight()
                        .background(gold.copy(alpha=.65f),RoundedCornerShape(2.dp))
                        .clickable { onSelect(cue.id) }
                        .semantics { contentDescription="Editar legenda: ${cue.text.take(50)}" },
                        contentAlignment=Alignment.Center) {
                        Text(cue.text,maxLines=1,overflow=TextOverflow.Ellipsis,fontSize=9.sp,
                            color=Color.White)
                    }
                }
            }
        }
    }
}

/** Linha do tempo única e virtualizada em janelas de 10 segundos.
 * Cada janela usa tempos absolutos: legendas sobrepostas continuam selecionáveis pela lista.
 * Não são criadas 30 (ou 300) trilhas individuais na tela.
 */
@Composable
private fun SubtitleCompactTimeline(cues: List<SubtitleCue>, totalMs: Long,
    selectedId: String, onSelect: (String) -> Unit) {
    val segmentMs = SubtitleCompactLayout.WINDOW_MS
    val duration = totalMs.coerceAtLeast(1L)
    val count = SubtitleCompactLayout.windowCount(totalMs)
    val listState = rememberLazyListState()
    val selectedStart = cues.firstOrNull { it.id == selectedId }?.startMs
    LaunchedEffect(selectedId, selectedStart, count) {
        if (selectedStart != null) {
            listState.animateScrollToItem(SubtitleCompactLayout.windowFor(selectedStart,totalMs))
        }
    }
    LazyRow(state=listState, modifier=Modifier.fillMaxWidth().height(62.dp),
        horizontalArrangement=Arrangement.spacedBy(3.dp)) {
        items(count=count, key={ it }) { segment ->
            val begin = segment.toLong() * segmentMs
            val end = minOf(begin + segmentMs, duration)
            val visible = cues.filter {
                SubtitleCompactLayout.visibleRange(segment,it.startMs,it.endMs,totalMs) != null
            }
            Column(Modifier.width(260.dp)) {
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                    Text(formatPipMs(begin),fontSize=10.sp,
                        color=MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatPipMs(end),fontSize=10.sp,
                        color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Box(Modifier.fillMaxWidth().height(38.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)) {
                    visible.forEach { cue ->
                        key(cue.id) {
                            val range = SubtitleCompactLayout.visibleRange(
                                segment,cue.startMs,cue.endMs,totalMs)!!
                            val leftMs = range.first
                            val rightMs = range.second
                            val fraction = (leftMs - begin).toFloat() / segmentMs
                            val span = (rightMs - leftMs).toFloat() / segmentMs
                            val width = (260.dp * span).coerceAtLeast(6.dp).coerceAtMost(260.dp)
                            val x = (260.dp * fraction).coerceIn(0.dp, 260.dp - width)
                            val lane = (cues.indexOf(cue) % 2) * 19
                            Box(Modifier.offset(x=x, y=lane.dp).width(width).height(18.dp)
                                .background(if(cue.id==selectedId)Blue
                                    else Color(0xFFC38A38),RoundedCornerShape(3.dp))
                                .clickable { onSelect(cue.id) }
                                .semantics { contentDescription="Selecionar legenda: ${cue.text.take(45)}" },
                                contentAlignment=Alignment.Center) {
                                Text(cue.text,color=Color.White,fontSize=9.sp,maxLines=1,
                                    overflow=TextOverflow.Ellipsis,
                                    modifier=Modifier.padding(horizontal=2.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Uma pista por legenda para não ocultar controles quando os tempos se sobrepõem.
 * Os valores são rascunho enquanto o gesto ocorre; somente onFinish grava.
 */
@Composable
private fun SubtitleTimelineEditor(cues:List<SubtitleCue>,totalMs:Long,selectedId:String,
    onSelect:(String)->Unit,onCommit:(String,Long,Long)->Unit) {
    val gold=Color(0xFFC38A38)
    val total=totalMs.coerceAtLeast(1L)
    if(cues.isEmpty()) {
        Text("Nenhuma legenda adicionada.",fontSize=12.sp)
        return
    }
    cues.sortedWith(compareBy<SubtitleCue> { it.startMs }.thenBy { it.id })
        .forEachIndexed { index,cue ->
        key(cue.id) {
            var draftStart by remember(cue.id,cue.startMs,cue.endMs) { mutableLongStateOf(cue.startMs) }
            var draftEnd by remember(cue.id,cue.startMs,cue.endMs) { mutableLongStateOf(cue.endMs) }
            var changed by remember(cue.id,cue.startMs,cue.endMs) { mutableStateOf(false) }
            fun updateDraft(next:Pair<Long,Long>) {
                draftStart=next.first
                draftEnd=next.second
                changed=(draftStart!=cue.startMs || draftEnd!=cue.endMs)
            }
            fun commitDraft() {
                if(changed && (draftStart!=cue.startMs || draftEnd!=cue.endMs)) {
                    onCommit(cue.id,draftStart,draftEnd)
                }
                changed=false
            }
            Row(Modifier.fillMaxWidth().height(44.dp).padding(vertical=3.dp),
                verticalAlignment=Alignment.CenterVertically) {
                Box(Modifier.width(60.dp).fillMaxHeight()
                    .background(if(cue.id==selectedId)MaterialTheme.colorScheme.primary else gold,
                        RoundedCornerShape(4.dp))
                    .clickable { onSelect(cue.id) },contentAlignment=Alignment.Center) {
                    Text("Leg. ${index+1}",fontSize=11.sp,color=Color.White)
                }
                Spacer(Modifier.width(6.dp))
                BoxWithConstraints(Modifier.weight(1f).fillMaxHeight()
                    .background(gold.copy(alpha=.12f),RoundedCornerShape(4.dp))) {
                    val width=maxWidth
                    val widthPx=with(LocalDensity.current) { width.toPx() }
                    val start=(draftStart.toFloat()/total).coerceIn(0f,1f)
                    val fraction=((draftEnd-draftStart).toFloat()/total).coerceIn(0f,1f-start)
                    val drawWidth=(width*fraction).coerceAtLeast(54.dp).coerceAtMost(width)
                    val x=(width*start).coerceAtMost(width-drawWidth)
                    Row(Modifier.offset(x=x).width(drawWidth).fillMaxHeight()
                        .background(if(cue.id==selectedId)gold else gold.copy(alpha=.68f),
                            RoundedCornerShape(4.dp)),verticalAlignment=Alignment.CenterVertically) {
                        AudioDragSurface(Modifier.width(18.dp).fillMaxHeight()
                            .background(Color.Black.copy(alpha=.22f)),total,widthPx,
                            onStep={ delta -> updateDraft(SubtitleTimelineEdit.trimLeft(
                                draftStart,draftEnd,delta,total)) },onFinish=::commitDraft,
                            onTap={onSelect(cue.id)}) {
                            Text("|",color=Color.White,modifier=Modifier.align(Alignment.Center))
                        }
                        AudioDragSurface(Modifier.weight(1f).fillMaxHeight(),total,widthPx,
                            onStep={ delta -> updateDraft(SubtitleTimelineEdit.move(
                                draftStart,draftEnd,delta,total)) },onFinish=::commitDraft,
                            onTap={onSelect(cue.id)}) {
                            Text(cue.text,maxLines=1,overflow=TextOverflow.Ellipsis,fontSize=10.sp,
                                color=Color.White,modifier=Modifier.align(Alignment.Center)
                                    .padding(horizontal=2.dp))
                        }
                        AudioDragSurface(Modifier.width(18.dp).fillMaxHeight()
                            .background(Color.Black.copy(alpha=.22f)),total,widthPx,
                            onStep={ delta -> updateDraft(SubtitleTimelineEdit.trimRight(
                                draftStart,draftEnd,delta,total)) },onFinish=::commitDraft,
                            onTap={onSelect(cue.id)}) {
                            Text("|",color=Color.White,modifier=Modifier.align(Alignment.Center))
                        }
                    }
                }
            }
            if(cue.id==selectedId) {
                Row(Modifier.fillMaxWidth().padding(start=66.dp),
                    horizontalArrangement=Arrangement.SpaceBetween) {
                    Text("Início ${formatPipMs(draftStart)}",fontSize=10.sp)
                    Text("Fim ${formatPipMs(draftEnd)}",fontSize=10.sp)
                }
            }
        }
    }
}

/** Todas as faixas usam 12dp de margem, rótulo de 60dp e separação de 6dp. */
@Composable
private fun TimelineTimedRow(label: String, color: Color,
    entries: List<Triple<String,Long,Long>>, total: Long, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(34.dp).padding(horizontal=12.dp,vertical=2.dp),
        verticalAlignment=Alignment.CenterVertically) {
        Box(Modifier.width(60.dp).fillMaxHeight().background(color,RoundedCornerShape(3.dp)),
            contentAlignment=Alignment.Center) {Text(label,color=Color.White,fontSize=11.sp)}
        Spacer(Modifier.width(6.dp))
        BoxWithConstraints(Modifier.weight(1f).fillMaxHeight()
            .background(color.copy(alpha=.14f),RoundedCornerShape(3.dp))) {
            entries.forEach { (name,start,length) ->
                val x=(start.toFloat()/total.coerceAtLeast(1L)).coerceIn(0f,1f)
                val width=(length.toFloat()/total.coerceAtLeast(1L)).coerceIn(0f,1f-x)
                Box(Modifier.offset(x=maxWidth*x).width(maxWidth*width).fillMaxHeight()
                    .background(color.copy(alpha=.48f),RoundedCornerShape(2.dp))
                    .clickable(onClick=onClick),contentAlignment=Alignment.Center) {
                    Text(name,maxLines=1,overflow=TextOverflow.Ellipsis,fontSize=10.sp)
                }
            }
        }
    }
}

@Composable
private fun OverlayTimeline(overlays: List<Overlay>, total: Long, onClick: (String) -> Unit) {
    Column(verticalArrangement=Arrangement.spacedBy(2.dp)) {
        if (overlays.isEmpty()) {
            TimelineRow("PiP",Purple, emptyList(),total,0L,"", onClip = { })
        } else overlays.forEachIndexed { index,layer ->
            Row(Modifier.fillMaxWidth().height(34.dp).padding(horizontal=12.dp,vertical=2.dp),
                verticalAlignment=Alignment.CenterVertically) {
                Box(Modifier.width(60.dp).fillMaxHeight().clip(RoundedCornerShape(3.dp))
                    .background(Purple),contentAlignment=Alignment.Center) {
                    Text("PiP ${index+1}",color=Color.White,fontSize=11.sp)
                }
                Spacer(Modifier.width(6.dp))
                BoxWithConstraints(Modifier.weight(1f).fillMaxHeight()
                    .background(Purple.copy(alpha=.14f))) {
                    val start = (layer.startAtMs.toFloat()/total.coerceAtLeast(1L)).coerceIn(0f,1f)
                    val length = (layer.durationMs.toFloat()/total.coerceAtLeast(1L)).coerceIn(0f,1f-start)
                    Box(Modifier.offset(x=maxWidth*start).width(maxWidth*length)
                        .fillMaxHeight().background(Purple.copy(alpha=.45f))
                        .clickable {onClick(layer.id)},contentAlignment=Alignment.Center) {
                        Text(if(layer.isImage)"Imagem" else "Vídeo",fontSize=10.sp,maxLines=1)
                    }
                }
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun PipLayer(pip: Overlay, timeMs: Long, playing: Boolean,
                     speed: Float = 1f, editable: Boolean = true, onUpdate: (Overlay) -> Unit) {
    val context = LocalContext.current
    val sourceAspect = remember(pip.uri, pip.isImage) { mediaAspect(context, pip.uri, pip.isImage) }
    val pipPlayer = remember(pip.uri) { if (!pip.isImage) diagnosticPlayer(context) else null }
    DisposableEffect(pipPlayer) { onDispose { pipPlayer?.release() } }
    LaunchedEffect(pip.uri) {
        pipPlayer?.apply { setMediaItem(MediaItem.fromUri(pip.uri)); prepare() }
    }
    LaunchedEffect(playing, timeMs, pip.volume, pip.muted, speed) {
        pipPlayer?.apply {
            setPlaybackParameters(PlaybackParameters(speed))
            volume = if (pip.muted) 0f else pip.volume
            if (kotlin.math.abs(currentPosition - timeMs) > 400L) seekTo(timeMs.coerceAtLeast(0L))
            if (playing) play() else pause()
        }
    }
    val currentPip by rememberUpdatedState(pip)
    val currentUpdate by rememberUpdatedState(onUpdate)
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val canvasW = constraints.maxWidth.coerceAtLeast(1)
        val canvasH = constraints.maxHeight.coerceAtLeast(1)
        val (wFrac, hFrac) = OverlayEditorLogic.dimensions(pip.scale, sourceAspect,
            canvasW.toFloat() / canvasH)
        val centerX = OverlayEditorLogic.clampCenter(pip.x, wFrac)
        val centerY = OverlayEditorLogic.clampCenter(pip.y, hFrac)
        val pipWidth = maxWidth * wFrac
        val pipHeight = maxHeight * hFrac
        if (editable && kotlin.math.abs(centerX - 0.5f) < 0.012f) {
            Box(Modifier.align(Alignment.Center).width(1.dp).fillMaxHeight()
                .background(Blue.copy(alpha = 0.6f)))
        }
        if (editable && kotlin.math.abs(centerY - 0.5f) < 0.012f) {
            Box(Modifier.align(Alignment.Center).height(1.dp).fillMaxWidth()
                .background(Blue.copy(alpha = 0.6f)))
        }
        Box(
            Modifier.offset(x = maxWidth * (centerX - wFrac / 2f),
                            y = maxHeight * (centerY - hFrac / 2f))
                .width(pipWidth).height(pipHeight)
                .then(if (editable) Modifier.border(2.dp, Color(0xFF5796FF), RoundedCornerShape(6.dp))
                    else Modifier)
                .then(if (editable) Modifier.pointerInput(pip.uri) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val live = currentPip
                        val (wf, hf) = OverlayEditorLogic.dimensions(
                            (live.scale * zoom).coerceIn(.05f, .90f), sourceAspect,
                            canvasW.toFloat() / canvasH)
                        currentUpdate(live.copy(
                            scale = (live.scale * zoom).coerceIn(.05f, .90f),
                            x = OverlayEditorLogic.clampCenter(live.x + pan.x / canvasW, wf),
                            y = OverlayEditorLogic.clampCenter(live.y + pan.y / canvasH, hf)
                        ))
                    }
                } else Modifier)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.Transparent)
        ) {
            if (pip.isImage) {
                val bitmap = remember(pip.uri) {
                    runCatching {
                        val options = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        context.contentResolver.openInputStream(Uri.parse(pip.uri))?.use {
                            android.graphics.BitmapFactory.decodeStream(it, null, options)
                        }
                        var sample = 1
                        while (options.outWidth / sample > 1400 || options.outHeight / sample > 1400) {
                            sample *= 2
                        }
                        options.inJustDecodeBounds = false
                        options.inSampleSize = sample
                        context.contentResolver.openInputStream(Uri.parse(pip.uri))?.use {
                            android.graphics.BitmapFactory.decodeStream(it, null, options)
                        }
                    }.getOrNull()
                }
                // FIT + proporção original evita cortar letras ou bordas de logotipos.
                if (bitmap != null) Image(bitmap.asImageBitmap(), "Imagem sobreposta",
                    Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            } else {
                AndroidView(factory = { previewPlayer(it, transparentShutter = true).apply {
                    player = pipPlayer; useController = false
                } }, modifier = Modifier.fillMaxSize(), update = { it.player = pipPlayer })
            }
        }
    }
}

/** Entrada numérica com máscara visual. O valor editado guarda só dígitos.
 * Assim apagar não fica preso no ':' ou no '.', e a digitação é sempre do fim. */
private object NumericTimeMask : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val output = NumericTime.format(text.text)
        val length = text.length
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int =
                if (offset <= 0 && length == 0) 0 else output.length
            override fun transformedToOriginal(offset: Int): Int = length
        }
        return TransformedText(AnnotatedString(output), mapping)
    }
}

/** Campo numérico compartilhado, com a opção de aplicar cada valor válido imediatamente.
 * Na prévia, não exige botão de confirmar; o player é reposicionado a cada digitação.
 * No PiP, mantém os botões de confirmar/limpar existentes. */
@Composable
private fun TimeDigitsField(label: String, timeMs: Long, totalMs: Long,
    valid: (Long) -> Boolean = { it in 0L..totalMs },
    onCommit: (Long) -> Unit, modifier: Modifier = Modifier,
    autoApply: Boolean = false, compact: Boolean = false) {
    var digits by remember { mutableStateOf(NumericTime.fromMs(timeMs)) }
    var editing by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    LaunchedEffect(timeMs) { if (!editing) digits = NumericTime.fromMs(timeMs) }
    val parsed = NumericTime.toMs(digits)
    val allowed = parsed != null && valid(parsed)
    Row(modifier, verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        OutlinedTextField(
            value = digits,
            onValueChange = { typed ->
                digits = NumericTime.sanitize(typed)
                editing = true
                if (autoApply) {
                    NumericTime.toMs(digits)?.takeIf(valid)?.let(onCommit)
                }
            },
            visualTransformation = NumericTimeMask,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                if(compact && allowed) onCommit(parsed!!)
                editing = false
                focusManager.clearFocus()
            }),
            modifier = Modifier.weight(1f).onFocusChanged { state ->
                if (state.isFocused && !editing) {
                    digits = "" // uma nova digitação começa do zero, sem precisar apagar separadores
                    editing = true
                } else if (!state.isFocused && editing) {
                    editing = false
                    if(compact && allowed) onCommit(parsed!!)
                    // Entradas inválidas não substituem o último horário válido.
                    digits = NumericTime.fromMs(timeMs)
                }
            },
            label = { Text(label) }, singleLine = true, isError = digits.isNotBlank() && !allowed,
            supportingText = if (!compact && digits.isNotBlank() && !allowed && !autoApply) {
                { Text("Fora do intervalo") }
            } else null
        )
        if (!autoApply && !compact) {
            IconButton(onClick = { digits = ""; editing = true }) {
                Icon(Icons.Default.Backspace, "Limpar $label")
            }
            IconButton(onClick = {
                if (allowed) {
                    onCommit(parsed!!)
                    editing = false
                    focusManager.clearFocus()
                }
            }, enabled = allowed) {
                Icon(Icons.Default.Check, "Aplicar $label")
            }
        }
    }
}

/** Velocidade somente da prévia; não modifica cortes ou velocidade do MP4 exportado. */
@Composable
private fun PlaybackSpeedButton(speed: Float, onSelect: (Float) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true },
            modifier = Modifier.height(38.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            shape = RoundedCornerShape(11.dp)) {
            Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(3.dp))
            Text(PreviewPlayback.label(speed), fontSize = 12.sp, maxLines = 1)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Text("Velocidade da prévia", modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelMedium)
            PreviewPlayback.SPEEDS.forEach { option ->
                DropdownMenuItem(
                    text = { Text(PreviewPlayback.label(option)) },
                    leadingIcon = if (option == speed) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    } else null,
                    onClick = { onSelect(option); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun TrimQuickAction(icon: ImageVector, label: String, enabled: Boolean,
    modifier: Modifier = Modifier, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, enabled = enabled,
        modifier = modifier.height(58.dp), shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(1.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(18.dp))
            Text(label, fontSize = 10.sp, maxLines = 1, softWrap = false)
        }
    }
}

/** Frames pequenos gerados em IO somente quando muda o clipe; evita decodificar a cada recomposição. */
@Composable
private fun TrimThumbnails(uri: String, durationMs: Long, onSeek: (Long) -> Unit) {
    val context = LocalContext.current
    val thumbs by produceState<List<ImageBitmap>>(initialValue = emptyList(), uri, durationMs) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, Uri.parse(uri))
                    (0 until 6).mapNotNull { step ->
                        val microseconds = (durationMs.coerceAtLeast(1L) * step / 6L) * 1000L
                        val frame = if (Build.VERSION.SDK_INT >= 27) {
                            retriever.getScaledFrameAtTime(microseconds,
                                MediaMetadataRetriever.OPTION_CLOSEST_SYNC, 144, 88)
                        } else retriever.getFrameAtTime(microseconds,
                            MediaMetadataRetriever.OPTION_CLOSEST_SYNC)?.let { original ->
                            val scaled = android.graphics.Bitmap.createScaledBitmap(original, 144, 88, true)
                            if (scaled !== original) original.recycle()
                            scaled
                        }
                        frame?.asImageBitmap()
                    }
                } finally { retriever.release() }
            }.getOrDefault(emptyList())
        }
    }
    Row(Modifier.fillMaxWidth().height(60.dp).clip(RoundedCornerShape(9.dp))
        .background(MaterialTheme.colorScheme.surfaceVariant)) {
        repeat(6) { index ->
            Box(Modifier.weight(1f).fillMaxHeight().clickable {
                onSeek(durationMs.coerceAtLeast(0L) * (index * 2L + 1L) / 12L)
            }) {
                thumbs.getOrNull(index)?.let { bitmap ->
                    Image(bitmap, "Quadro ${index + 1}", Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop)
                }
            }
        }
    }
}

/** Um quadro por trecho visível, carregado sob demanda para economizar memória. */
@Composable
private fun TrimSegmentFrame(clip: Clip) {
    val context = LocalContext.current
    val frame by produceState<ImageBitmap?>(initialValue = null,
        clip.uri, clip.startMs, clip.endMs) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, Uri.parse(clip.uri))
                    val offsetMs = minOf(clip.durationMs / 2L, 1500L)
                    val frameMs = (clip.startMs + offsetMs).coerceIn(
                        0L, (clip.sourceDurationMs - 1L).coerceAtLeast(0L))
                    val bitmap = if (Build.VERSION.SDK_INT >= 27) {
                        retriever.getScaledFrameAtTime(frameMs * 1000L,
                            MediaMetadataRetriever.OPTION_CLOSEST_SYNC, 200, 110)
                    } else {
                        retriever.getFrameAtTime(frameMs * 1000L,
                            MediaMetadataRetriever.OPTION_CLOSEST_SYNC)?.let { original ->
                            val scaled = android.graphics.Bitmap.createScaledBitmap(original, 200, 110, true)
                            if (scaled !== original) original.recycle()
                            scaled
                        }
                    }
                    bitmap?.asImageBitmap()
                } finally { retriever.release() }
            }.getOrNull()
        }
    }
    Box(Modifier.fillMaxWidth().height(58.dp).background(Color(0xFF1D1D23)),
        contentAlignment = Alignment.Center) {
        if (frame != null) Image(frame!!, "Miniatura do trecho", Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop)
        else Icon(Icons.Default.Movie, "Trecho de vídeo", tint = Color.LightGray)
    }
}

/** Trilha de pedaços: selecione tocando, sem precisar importar a mesma mídia novamente. */
@Composable
private fun TrimSegmentStrip(p: VideoProject, selectedId: String, onSelect: (String) -> Unit) {
    val scroll = rememberLazyListState()
    val selectedIndex = p.clips.indexOfFirst { it.id == selectedId }
    LaunchedEffect(p.id, selectedId, selectedIndex) {
        if (selectedIndex >= 0) scroll.animateScrollToItem(selectedIndex)
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
        Text("Trechos separados (${p.clips.size})", fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.bodyMedium)
        Text("Deslize para o lado →", fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    LazyRow(state = scroll, horizontalArrangement = Arrangement.spacedBy(7.dp),
        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 3.dp),
        modifier = Modifier.fillMaxWidth()) {
        items(p.clips, key = { it.id }) { item ->
            val isSelected = item.id == selectedId
            Surface(onClick = { onSelect(item.id) },
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(if (isSelected) 2.dp else 1.dp,
                    if (isSelected) Blue else MaterialTheme.colorScheme.outlineVariant),
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.width(120.dp)) {
                Column {
                    TrimSegmentFrame(item)
                    Column(Modifier.padding(horizontal = 5.dp, vertical = 3.dp)) {
                        val part = p.clips.indexOfFirst { it.id == item.id } + 1
                        Text("Parte $part${if (isSelected) " · selecionada" else ""}",
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 10.sp, maxLines = 1)
                        Text("${formatPipMs(item.startMs)}–${formatPipMs(item.endMs)}",
                            fontSize = 9.sp, maxLines = 1)
                        Text("Duração: ${formatPipMs(item.durationMs)}",
                            fontSize = 9.sp, maxLines = 1)
                    }
                }
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun TrimScreen(p: VideoProject, selectedId: String, globalMs: Long, back: () -> Unit,
                       update: (VideoProject) -> Unit, selected: (String) -> Unit,
                       onSaveHome: (VideoProject) -> Unit) {
    val context = LocalContext.current
    val clip = p.clips.firstOrNull { it.id == selectedId } ?: p.clips.firstOrNull()
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        Heading("Cortar e unir", back)
        if (clip == null) {
            Text("Adicione um vídeo para editar.", modifier = Modifier.padding(16.dp))
            return@Column
        }
        val total = clip.sourceDurationMs.coerceAtLeast(1L)
        val player = remember(clip.id) { diagnosticPlayer(context).apply { repeatMode = Player.REPEAT_MODE_ONE } }
        var playbackSpeed by remember(clip.id) { mutableFloatStateOf(1f) }
        LaunchedEffect(player, playbackSpeed) { player.setPlaybackParameters(PlaybackParameters(playbackSpeed)) }
        DisposableEffect(player) { onDispose { player.release() } }
        var cursorMs by remember(clip.id) { mutableLongStateOf(TrimTimelineEdit.cursorFor(p, clip)) }
        var playing by remember(clip.id) { mutableStateOf(false) }
        var begin by remember(clip.id) { mutableLongStateOf(clip.startMs) }
        var finish by remember(clip.id) { mutableLongStateOf(clip.endMs) }
        // Um nível de desfazer cobre aplicar, dividir, reordenar e excluir.
        var undoClips by remember(p.id) { mutableStateOf<List<Clip>?>(null) }
        var undoSelected by remember(p.id) { mutableStateOf("") }
        var undoLastCut by remember(p.id) { mutableStateOf<Pair<String?, Long?>?>(null) }
        LaunchedEffect(clip.id, clip.startMs, clip.endMs) {
            begin = clip.startMs
            finish = clip.endMs
            if (cursorMs !in clip.startMs..clip.endMs) {
                cursorMs = cursorMs.coerceIn(clip.startMs, clip.endMs)
                player.seekTo(cursorMs)
            }
        }
        LaunchedEffect(clip.id, clip.uri) {
            player.setMediaItem(MediaItem.fromUri(clip.uri))
            player.prepare()
            player.seekTo(cursorMs)
        }
        LaunchedEffect(player) {
            while (true) {
                playing = player.isPlaying
                if (player.isPlaying) {
                    cursorMs = TrimLogic.clampCursor(player.currentPosition, total)
                                    }
                delay(90L)
            }
        }
        fun seek(ms: Long) {
            cursorMs = TrimLogic.clampCursor(ms, total)
            player.seekTo(cursorMs)
        }
        fun notifyUser(message: String) {
            android.widget.Toast.makeText(context, message,
                android.widget.Toast.LENGTH_SHORT).show()
        }
        fun rememberUndo() {
            undoClips = p.clips.toList()
            undoSelected = clip.id
            undoLastCut = p.lastTrimCutClipId to p.lastTrimCutSourceMs
        }
        val validRange = TrimLogic.valid(begin, finish, total)
        val dirty = begin != clip.startMs || finish != clip.endMs
        val idx = p.clips.indexOfFirst { it.id == clip.id }
        val canSplit = !dirty && TrimLogic.canSplit(clip.startMs, clip.endMs, cursorMs)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Clipe ${idx + 1} de ${p.clips.size}", fontWeight = FontWeight.SemiBold)
                Text("Escolha uma parte na faixa abaixo", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            // Prévia e miniaturas são blocos independentes: nenhum RangeSlider cobre as fotos.
            val videoAspect = remember(clip.uri) { mediaAspect(context, clip.uri, false) }
            InlineVideoPreview(videoAspect) {
                AndroidView(factory = { ctx -> previewPlayer(ctx).apply {
                    this.player = player
                    useController = false
                } }, modifier = Modifier.fillMaxSize(), update = { it.player = player })
            }
            TrimThumbnails(clip.uri, total) { time -> player.pause(); seek(time) }
            // Uma única barra para navegar; início e fim são definidos nos campos abaixo.
            Slider(value = cursorMs.toFloat().coerceIn(0f, total.toFloat()),
                onValueChange = { player.pause(); seek(it.toLong()) },
                valueRange = 0f..total.toFloat(), modifier = Modifier.fillMaxWidth().height(30.dp))
            TrimSegmentStrip(p, clip.id) { id -> player.pause(); selected(id) }
            if (p.lastTrimCutClipId == clip.id && p.lastTrimCutSourceMs != null) {
                Text("Último corte: ${formatPipMs(p.lastTrimCutSourceMs)} · cursor recuperado automaticamente.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary)
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                TimeDigitsField("Tempo", cursorMs, total, autoApply = true,
                    onCommit = { player.pause(); seek(it) }, modifier = Modifier.weight(1f))
                IconButton(onClick = { player.pause(); seek(cursorMs - 1000L) }) {
                    Icon(Icons.Default.KeyboardDoubleArrowLeft, "Recuar 1 segundo")
                }
                IconButton(onClick = { if (player.isPlaying) player.pause() else player.play() }) {
                    Icon(if (playing) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                        "Reproduzir ou pausar", Modifier.size(30.dp))
                }
                IconButton(onClick = { player.pause(); seek(cursorMs + 1000L) }) {
                    Icon(Icons.Default.KeyboardDoubleArrowRight, "Avançar 1 segundo")
                }
                PlaybackSpeedButton(playbackSpeed, onSelect = { playbackSpeed = it })
            }
            Text("Duração do vídeo: ${formatPipMs(total)} · toque no tempo ou nas miniaturas para navegar.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TimeDigitsField("Início", begin, total,
                    valid = { TrimLogic.valid(it, finish, total) },
                    onCommit = { begin = it }, autoApply = true,
                    modifier = Modifier.weight(1f))
                TimeDigitsField("Fim", finish, total,
                    valid = { TrimLogic.valid(begin, it, total) },
                    onCommit = { finish = it }, autoApply = true,
                    modifier = Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    if (TrimLogic.valid(cursorMs, finish, total)) begin = cursorMs
                    else notifyUser("Para marcar início, escolha um ponto antes do fim do trecho.")
                }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(4.dp)) {
                    Icon(Icons.Default.Flag, null, Modifier.size(17.dp))
                    Text(" Marcar início", fontSize = 12.sp, maxLines = 1)
                }
                OutlinedButton(onClick = {
                    if (TrimLogic.valid(begin, cursorMs, total)) finish = cursorMs
                    else notifyUser("Para marcar fim, avance o vídeo após o início (mínimo 0,25 s).")
                }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(4.dp)) {
                    Icon(Icons.Default.Flag, null, Modifier.size(17.dp))
                    Text(" Marcar fim", fontSize = 12.sp, maxLines = 1)
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Trecho: ${if (validRange) formatPipMs(finish - begin) else "inválido"}",
                    style = MaterialTheme.typography.bodyMedium)
                if (dirty) Text("Não aplicado", color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelSmall)
            }
            Button(onClick = {
                rememberUndo()
                update(p.copy(clips = p.clips.map { if (it.id == clip.id)
                    it.copy(startMs = begin, endMs = finish) else it }))
            }, modifier = Modifier.fillMaxWidth().height(46.dp), enabled = validRange && dirty,
                shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.Default.Check, null)
                Text(" Aplicar corte")
            }
            Text("Dividir seleciona o próximo trecho e coloca o cursor no corte; toque nas partes acima para escolher.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            // Rolagem horizontal impede esmagar os seis cartões em telas pequenas.
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                TrimQuickAction(Icons.Default.ContentCut, "Dividir", true, Modifier.width(86.dp)) {
                    when {
                        dirty -> notifyUser("Aplique ou desfaça o corte antes de dividir.")
                        !canSplit -> notifyUser("Coloque o cursor dentro do trecho, afastado das extremidades.")
                        else -> {
                            val result = TrimTimelineEdit.split(p, clip.id, cursorMs)
                            if (result == null) notifyUser("Não foi possível dividir neste ponto.")
                            else {
                                rememberUndo()
                                player.pause()
                                update(result.project)
                                selected(result.selectedClipId) // abre a metade direita exatamente no corte
                            }
                        }
                    }
                }
                TrimQuickAction(Icons.Default.Undo, "Desfazer", undoClips != null,
                    Modifier.width(86.dp)) {
                    undoClips?.let { snapshot ->
                        begin = snapshot.firstOrNull { it.id == undoSelected }?.startMs ?: begin
                        finish = snapshot.firstOrNull { it.id == undoSelected }?.endMs ?: finish
                        update(p.copy(clips = snapshot,
                            lastTrimCutClipId = undoLastCut?.first,
                            lastTrimCutSourceMs = undoLastCut?.second))
                        selected(undoSelected)
                        undoClips = null
                        undoLastCut = null
                    }
                }
                TrimQuickAction(Icons.Default.ArrowBack, "Antes", idx > 0,
                    Modifier.width(86.dp)) {
                    rememberUndo()
                    val list = p.clips.toMutableList()
                    val before = list[idx - 1]
                    list[idx - 1] = list[idx]
                    list[idx] = before
                    update(p.copy(clips = list))
                }
                TrimQuickAction(Icons.Default.ArrowForward, "Depois", idx < p.clips.lastIndex,
                    Modifier.width(86.dp)) {
                    rememberUndo()
                    val list = p.clips.toMutableList()
                    val after = list[idx + 1]
                    list[idx + 1] = list[idx]
                    list[idx] = after
                    update(p.copy(clips = list))
                }
                TrimQuickAction(Icons.Default.Delete, "Excluir", p.clips.size > 1,
                    Modifier.width(86.dp)) {
                    TrimTimelineEdit.remove(p, clip.id)?.let { result ->
                        rememberUndo()
                        player.pause()
                        update(result.project)
                        selected(result.selectedClipId)
                    }
                }
                TrimQuickAction(Icons.Default.Save, "Salvar", true, Modifier.width(86.dp)) {
                    val saved = TrimSaveLogic.applyPending(p, clip.id, begin, finish)
                    if (saved == null) notifyUser("Corte inválido. Ajuste início e fim antes de salvar.")
                    else onSaveHome(saved)
                }
            }
            Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .58f),
                shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(7.dp))
                        Text("Mini tutorial · Cortar e unir", fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyMedium)
                    }
                    Text("1. Toque nas miniaturas, arraste a barra ou digite Tempo para escolher o ponto.",
                        style = MaterialTheme.typography.bodySmall)
                    Text("2. Marcar início/fim define o trecho; Aplicar corte confirma. O original não muda.",
                        style = MaterialTheme.typography.bodySmall)
                    Text("3. Dividir cria duas partes, seleciona a da direita e lembra o último corte. Toque nas partes para alternar.",
                        style = MaterialTheme.typography.bodySmall)
                    Text("4. Excluir remove só a parte selecionada; Desfazer recupera a última edição. Salvar volta à Home.",
                        style = MaterialTheme.typography.bodySmall)
                    Text("5. O vídeo repete até pausar; velocidade de 0,25× a 2× afeta só a prévia, não o MP4 exportado.",
                        style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun AudioScreen(p:VideoProject,back:()->Unit,update:(VideoProject)->Unit,
                        chooseMusic:()->Unit, requestedTrackId:String? = null,
                        onSelectionConsumed:()->Unit = {}) {
    val context = LocalContext.current
    val engine = remember { AudioExtractionEngine(context) }
    var sourceIndex by remember(p.id) { mutableIntStateOf(-1) } // -1 = all clips
    var message by remember { mutableStateOf("") }
    var volumesExpanded by remember(p.id) { mutableStateOf(false) }
    var trackVolumeExpandedId by remember(p.id) { mutableStateOf<String?>(null) }
    var pendingAudioDeleteId by remember(p.id) { mutableStateOf<String?>(null) }
    var externalClip by remember { mutableStateOf<Clip?>(null) }
    var trimStart by remember { mutableFloatStateOf(0f) }
    var trimEnd by remember { mutableFloatStateOf(1f) }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            val duration = mediaDuration(context, uri)
            if (duration > 0) {
                externalClip = Clip(uri=uri.toString(),title=mediaName(context,uri),sourceDurationMs=duration)
                trimStart=0f; trimEnd=1f
            } else message = "Não foi possível ler a duração do vídeo externo."
        }
    }
    // Pré-escuta da origem selecionada, sem alterar a linha do tempo.
    val previewPlayer = remember(context) { diagnosticPlayer(context) }
    var previewPlaying by remember { mutableStateOf(false) }
    val previewClip = externalClip ?: p.clips.getOrNull(sourceIndex)
    val previewStartMs = if (externalClip != null) (externalClip!!.sourceDurationMs * trimStart).toLong()
        else previewClip?.startMs ?: 0L
    val previewEndMs = if (externalClip != null) (externalClip!!.sourceDurationMs * trimEnd).toLong()
        else previewClip?.endMs ?: 0L
    LaunchedEffect(previewClip?.uri, previewStartMs, previewEndMs) {
        previewPlayer.pause()
        previewPlaying = false
        previewClip?.let { clip ->
            previewPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(clip.uri)))
            previewPlayer.prepare()
            previewPlayer.seekTo(previewStartMs)
        } ?: previewPlayer.clearMediaItems()
    }
    LaunchedEffect(previewPlaying, previewEndMs) {
        while (previewPlaying) {
            if (previewEndMs > previewStartMs && previewPlayer.currentPosition >= previewEndMs) {
                previewPlayer.pause()
                previewPlayer.seekTo(previewStartMs)
                previewPlaying = false
            }
            delay(100)
        }
    }
    DisposableEffect(previewPlayer) { onDispose { previewPlayer.release() } }
    // A edição de uma faixa só é aplicada quando o usuário confirma Salvar corte.
    var editingAudioId by remember(p.id) { mutableStateOf<String?>(null) }
    val editingAudio = p.audioTracks.firstOrNull { it.id == editingAudioId }
    val sourceAudioLength = remember(editingAudio?.uri) {
        editingAudio?.let { mediaDuration(context, Uri.parse(it.uri)) } ?: 0L
    }
    var editTimelineStart by remember(p.id) { mutableLongStateOf(0L) }
    var editSourceStart by remember(p.id) { mutableLongStateOf(0L) }
    var editSourceEnd by remember(p.id) { mutableLongStateOf(0L) }
    val editDuration = editSourceEnd - editSourceStart
    // A seleção feita na timeline do editor abre diretamente esta faixa para edição.
    LaunchedEffect(requestedTrackId, p.id) {
        val track = p.audioTracks.firstOrNull { it.id == requestedTrackId }
        if (track != null) {
            val duration = mediaDuration(context, Uri.parse(track.uri))
            val range = AudioTrackTiming.initialRange(track.sourceStartMs, track.durationMs,
                duration, track.startMs, p.durationMs)
            if (range != null) {
                editingAudioId = track.id
                editSourceStart = range.first
                editSourceEnd = range.last
                editTimelineStart = track.startMs.coerceIn(0L,
                    (p.durationMs - (range.last-range.first)).coerceAtLeast(0L))
                previewPlayer.pause()
                previewPlaying = false
            } else message = "Não foi possível abrir esta faixa para corte."
        }
        if (requestedTrackId != null) onSelectionConsumed()
    }
    val editValid = editingAudio != null && AudioTrackTiming.valid(
        editSourceStart, editSourceEnd, sourceAudioLength, editTimelineStart, p.durationMs)
    // Player separado evita interromper a prévia de extração de vídeo.
    val trackPlayer = remember(context) { diagnosticPlayer(context) }
    var trackPlaying by remember { mutableStateOf(false) }
    LaunchedEffect(editingAudioId, editingAudio?.uri, editSourceStart, editSourceEnd) {
        trackPlayer.pause()
        trackPlaying = false
        if (editingAudio != null && editValid) {
            trackPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(editingAudio.uri)))
            trackPlayer.prepare()
            trackPlayer.seekTo(editSourceStart)
        } else trackPlayer.clearMediaItems()
    }
    LaunchedEffect(trackPlaying, editSourceStart, editSourceEnd) {
        while (trackPlaying) {
            if (trackPlayer.currentPosition >= editSourceEnd) {
                trackPlayer.pause()
                trackPlayer.seekTo(editSourceStart)
                trackPlaying = false
            }
            delay(100)
        }
    }
    DisposableEffect(trackPlayer) { onDispose { trackPlayer.release() } }
    var outputExtension by remember { mutableStateOf("m4a") }
    var addAfterExtract by remember { mutableStateOf(false) }
    var exporting by remember { mutableStateOf(false) }
    val saveAudio = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("audio/mp4")) { destination ->
        if (destination != null) {
            val chosen = if (externalClip != null) listOf(externalClip!!.copy(
                startMs=(externalClip!!.sourceDurationMs*trimStart).toLong(),
                endMs=(externalClip!!.sourceDurationMs*trimEnd).toLong()))
                else if (sourceIndex == -1) p.clips else listOfNotNull(p.clips.getOrNull(sourceIndex))
            exporting = true
            runCatching {
                engine.start(chosen, destination, onSuccess = {
                    exporting = false
                    message = "Áudio extraído e salvo no local escolhido."
                    if (addAfterExtract) {
                        runCatching { context.contentResolver.takePersistableUriPermission(destination,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION) }
                        val track = AudioTrack(destination.toString(),"Áudio extraído",
                            startMs=0, durationMs=chosen.sumOf { it.durationMs })
                        update(p.copy(audioTracks=p.audioTracks+track))
                        message += " Adicionado à linha do tempo."
                    }
                },onError={error->exporting=false; DiagnosticRecorder.failure("audio_extract",error);message="Falha: $error"})
            }.onFailure { exporting=false; DiagnosticRecorder.exception("audio_extract_start", it);message="Falha: ${it.message}" }
        }
    }
    DisposableEffect(engine) { onDispose { engine.cancel() } }
    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())
        .padding(horizontal=16.dp)) {
        Heading("Áudio",back)
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            Text("Mixagem de áudio",fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f))
            TextButton(onClick={volumesExpanded=!volumesExpanded}) {
                Text(if(volumesExpanded) "Recolher" else "Volumes (${p.clips.size})")
                Icon(if(volumesExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    "Mostrar ou ocultar volumes")
            }
        }
        if(volumesExpanded) {
            p.clips.forEachIndexed { index,clip ->
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Text("${index+1}. ${clip.title}",modifier=Modifier.weight(1f),
                        maxLines=1,overflow=TextOverflow.Ellipsis,fontSize=12.sp)
                    Text("${(clip.volume*100).toInt()}%",fontSize=11.sp)
                }
                Slider(value=clip.volume.coerceIn(0f,1f),onValueChange={volume->
                    update(p.copy(clips=p.clips.map { if(it.id==clip.id) it.copy(volume=volume) else it }))
                })
            }
            p.music?.let { music ->
                Text("Música antiga: ${music.title} · ${(music.volume*100).toInt()}%")
                Slider(value=music.volume.coerceIn(0f,1f),onValueChange={update(p.copy(music=music.copy(volume=it)))})
                TextButton(onClick={update(p.copy(music=null))}) {
                    Icon(Icons.Default.Delete,null);Text("Remover música")
                }
            }
        }
        Text("Faixas adicionais (${p.audioTracks.size})",fontWeight=FontWeight.SemiBold)
        if(p.audioTracks.isEmpty()) Text("Nenhuma faixa importada.",fontSize=12.sp,
            color=MaterialTheme.colorScheme.onSurfaceVariant)
        p.audioTracks.forEach { track ->
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Text(track.title,modifier=Modifier.weight(1f),maxLines=1,
                    overflow=TextOverflow.Ellipsis,fontSize=13.sp)
                TextButton(onClick={trackVolumeExpandedId=if(trackVolumeExpandedId==track.id)
                    null else track.id},contentPadding=PaddingValues(horizontal=4.dp)) {
                    Text("Vol ${(track.volume*100).toInt()}%",fontSize=11.sp)
                }
                IconButton(onClick={
                    val sourceDuration = mediaDuration(context, Uri.parse(track.uri))
                    val range = AudioTrackTiming.initialRange(track.sourceStartMs, track.durationMs,
                        sourceDuration, track.startMs, p.durationMs)
                    if (range == null) {
                        message = "O arquivo é curto demais ou não há espaço para editar a faixa."
                    } else {
                        editingAudioId = track.id
                        editTimelineStart = track.startMs.coerceIn(0L,
                            (p.durationMs - (range.last - range.first)).coerceAtLeast(0L))
                        editSourceStart = range.first
                        editSourceEnd = range.last
                        previewPlayer.pause()
                        previewPlaying = false
                    }
                },modifier=Modifier.size(38.dp)) { Icon(Icons.Default.ContentCut,"Editar corte da faixa") }
                IconButton(onClick={pendingAudioDeleteId=track.id},modifier=Modifier.size(38.dp)) {
                    Icon(Icons.Default.Delete,"Remover faixa")
                }
            }
            if(trackVolumeExpandedId==track.id) {
                Slider(value=track.volume.coerceIn(0f,1f),onValueChange={v->
                    update(p.copy(audioTracks=p.audioTracks.map { if(it.id==track.id) it.copy(volume=v) else it }))
                })
            }
        }
        editingAudio?.let { track ->
            HorizontalDivider(Modifier.padding(vertical=8.dp))
            Text("Editar trecho · ${track.title}",fontWeight=FontWeight.Bold,fontSize=17.sp)
            Text("Recorte a fonte e escolha quando ela entra no vídeo. O arquivo original fica intacto.",
                style=MaterialTheme.typography.bodySmall)
            Text("Arraste o centro para mover; as alças laterais ajustam o começo e o fim.",
                style=MaterialTheme.typography.bodySmall)
            AudioTrimGestureTimeline(
                AudioTimelineDraft(editTimelineStart, editSourceStart, editSourceEnd),
                p.durationMs, sourceAudioLength,
                onMove = { delta ->
                    val moved = AudioTimelineEdit.move(
                        AudioTimelineDraft(editTimelineStart, editSourceStart, editSourceEnd),
                        delta, p.durationMs)
                    editTimelineStart = moved.timelineStartMs
                },
                onLeftTrim = { delta ->
                    val changed = AudioTimelineEdit.left(
                        AudioTimelineDraft(editTimelineStart, editSourceStart, editSourceEnd), delta)
                    editTimelineStart = changed.timelineStartMs
                    editSourceStart = changed.sourceStartMs
                },
                onRightTrim = { delta ->
                    val changed = AudioTimelineEdit.right(
                        AudioTimelineDraft(editTimelineStart, editSourceStart, editSourceEnd),
                        delta, sourceAudioLength, p.durationMs)
                    editSourceEnd = changed.sourceEndMs
                })
            if (sourceAudioLength >= AudioTrackTiming.MIN_TRIM_MS &&
                p.durationMs >= AudioTrackTiming.MIN_TRIM_MS) {
                Text("Início no arquivo: ${formatPipMs(editSourceStart)}")
                Slider(value=editSourceStart.toFloat(),
                    valueRange=0f..(editSourceEnd-AudioTrackTiming.MIN_TRIM_MS).coerceAtLeast(0L).toFloat(),
                    onValueChange={ editSourceStart=it.toLong().coerceIn(0L,
                        (editSourceEnd-AudioTrackTiming.MIN_TRIM_MS).coerceAtLeast(0L)) })
                TimeDigitsField("Entrada",editSourceStart,sourceAudioLength,
                    valid={ it>=0L && it<=editSourceEnd-AudioTrackTiming.MIN_TRIM_MS },
                    onCommit={ editSourceStart=it })
                val maxSourceEnd = minOf(sourceAudioLength,
                    editSourceStart+(p.durationMs-editTimelineStart))
                Text("Fim no arquivo: ${formatPipMs(editSourceEnd)}")
                Slider(value=editSourceEnd.toFloat().coerceIn(
                        (editSourceStart+AudioTrackTiming.MIN_TRIM_MS).toFloat(),
                        maxOf(maxSourceEnd,editSourceStart+AudioTrackTiming.MIN_TRIM_MS).toFloat()),
                    valueRange=(editSourceStart+AudioTrackTiming.MIN_TRIM_MS).toFloat()..
                        maxOf(maxSourceEnd,editSourceStart+AudioTrackTiming.MIN_TRIM_MS).toFloat(),
                    onValueChange={ editSourceEnd=it.toLong().coerceAtLeast(
                        editSourceStart+AudioTrackTiming.MIN_TRIM_MS) })
                TimeDigitsField("Saída",editSourceEnd,sourceAudioLength,
                    valid={ it>=editSourceStart+AudioTrackTiming.MIN_TRIM_MS && it<=maxSourceEnd },
                    onCommit={ editSourceEnd=it })
                Text("Começa no projeto: ${formatPipMs(editTimelineStart)}")
                val latestStart=(p.durationMs-editDuration).coerceAtLeast(0L)
                Slider(value=editTimelineStart.coerceIn(0L,latestStart).toFloat(),
                    valueRange=0f..latestStart.toFloat(),
                    onValueChange={ editTimelineStart=it.toLong().coerceIn(0L,latestStart) })
                TimeDigitsField("Na timeline",editTimelineStart,p.durationMs,
                    valid={ it>=0L && it<=latestStart },onCommit={editTimelineStart=it})
                Text("Trecho selecionado: ${formatPipMs(editDuration.coerceAtLeast(0L))}",
                    style=MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick={
                        if (trackPlaying) {
                            trackPlayer.pause()
                            trackPlaying=false
                        } else if (editValid) {
                            if (trackPlayer.currentPosition < editSourceStart ||
                                trackPlayer.currentPosition >= editSourceEnd)
                                trackPlayer.seekTo(editSourceStart)
                            trackPlayer.play()
                            trackPlaying=true
                        }
                    },enabled=editValid) {
                        Icon(if(trackPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,null)
                        Text(if(trackPlaying) " Pausar" else " Ouvir corte")
                    }
                    TextButton(onClick={
                        trackPlayer.pause()
                        trackPlayer.seekTo(editSourceStart)
                        trackPlaying=false
                    }) { Text("Reiniciar") }
                }
                if (!editValid) Text("Trecho inválido ou ultrapassa o fim do vídeo.",
                    color=MaterialTheme.colorScheme.error)
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick={ editingAudioId=null },modifier=Modifier.weight(1f)) {
                        Text("Cancelar")
                    }
                    Button(onClick={
                        update(p.copy(audioTracks=p.audioTracks.map { saved ->
                            if (saved.id==track.id) saved.copy(startMs=editTimelineStart,
                                sourceStartMs=editSourceStart,durationMs=editDuration) else saved
                        }))
                        editingAudioId=null
                        message="Corte da faixa salvo no projeto."
                    },enabled=editValid,modifier=Modifier.weight(1f)) { Text("Salvar corte") }
                }
            } else {
                Text("Faixa curta demais para editar neste vídeo.")
                TextButton(onClick={editingAudioId=null}) { Text("Fechar") }
            }
            HorizontalDivider(Modifier.padding(vertical=8.dp))
        }
        OutlinedButton(onClick=chooseMusic,modifier=Modifier.fillMaxWidth()) {
            Icon(Icons.Default.LibraryMusic,null);Text(" Adicionar áudio à timeline")
        }
        HorizontalDivider(Modifier.padding(vertical=12.dp))
        Text("Extrair áudio de vídeo",fontWeight=FontWeight.Bold,fontSize=19.sp)
        Text("Escolha o vídeo de origem e salve o áudio separado. A extração não altera o vídeo original.",
            style=MaterialTheme.typography.bodySmall)
        var expanded by remember { mutableStateOf(false) }
        Box {
            OutlinedButton(onClick={expanded=true},enabled=!exporting) {
                Text(if(externalClip!=null)"Vídeo externo" else if(sourceIndex==-1)"Projeto inteiro" else "Clipe ${sourceIndex+1}")
                Icon(Icons.Default.ArrowDropDown,null)
            }
            DropdownMenu(expanded=expanded,onDismissRequest={expanded=false}) {
                DropdownMenuItem(text={Text("Projeto inteiro")},onClick={sourceIndex=-1;externalClip=null;expanded=false})
                p.clips.forEachIndexed { i,clip ->
                    DropdownMenuItem(text={Text("${i+1} · ${clip.title}",maxLines=1)},
                        onClick={sourceIndex=i;externalClip=null;expanded=false})
                }
            }
        }
        OutlinedButton(onClick={videoPicker.launch(arrayOf("video/*"))},enabled=!exporting) {
            Icon(Icons.Default.FileOpen,null); Text(" Importar vídeo externo")
        }
        externalClip?.let { clip ->
            Text("Origem externa: ${clip.title}",maxLines=2)
            Text("Início: ${formatPipMs((clip.sourceDurationMs*trimStart).toLong())}")
            Slider(value=trimStart,valueRange=0f..1f,onValueChange={trimStart=it.coerceAtMost(trimEnd-.001f)})
            Text("Fim: ${formatPipMs((clip.sourceDurationMs*trimEnd).toLong())}")
            Slider(value=trimEnd,valueRange=0f..1f,onValueChange={trimEnd=it.coerceAtLeast(trimStart+.001f)})
            TextButton(onClick={externalClip=null}) { Text("Voltar à origem do projeto") }
        }
        if (previewClip != null) {
            Text("Pré-escuta do trecho",fontWeight=FontWeight.Medium)
            Text("${formatPipMs(previewStartMs)} até ${formatPipMs(previewEndMs)}",
                style=MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically) {
                OutlinedButton(onClick={
                    if (previewPlaying) {
                        previewPlayer.pause()
                        previewPlaying=false
                    } else {
                        if (previewPlayer.currentPosition < previewStartMs ||
                            previewPlayer.currentPosition >= previewEndMs) previewPlayer.seekTo(previewStartMs)
                        previewPlayer.play()
                        previewPlaying=true
                    }
                },enabled=!exporting && previewEndMs>previewStartMs) {
                    Icon(if(previewPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,null)
                    Text(if(previewPlaying) " Pausar" else " Ouvir trecho")
                }
                TextButton(onClick={previewPlayer.pause();previewPlayer.seekTo(previewStartMs);previewPlaying=false}) {
                    Text("Reiniciar")
                }
            }
            Text("A pré-escuta reproduz o áudio do vídeo de origem; não modifica o arquivo.",
                style=MaterialTheme.typography.bodySmall)
        } else if (sourceIndex == -1) {
            Text("Para ouvir antes de extrair, escolha um clipe ou importe um vídeo externo.",
                style=MaterialTheme.typography.bodySmall)
        }
        Text("Formato de áudio",fontWeight=FontWeight.Medium)
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            listOf("m4a","mp4").forEach { format ->
                FilterChip(selected=outputExtension==format,onClick={outputExtension=format},
                    label={Text(format.uppercase())})
            }
        }
        Text("Ambos usam áudio AAC em contêiner MP4. MP3 e WAV exigem codificadores adicionais.",
            style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement=Arrangement.spacedBy(7.dp)) {
            OutlinedButton(onClick={
                addAfterExtract=false
                saveAudio.launch("UaiCortes_audio.$outputExtension")
            },modifier=Modifier.weight(1f),enabled=!exporting && p.clips.isNotEmpty()) {
                Icon(Icons.Default.Save,null);Text(" Salvar",fontSize=12.sp)
            }
            Button(onClick={
                addAfterExtract=true
                saveAudio.launch("UaiCortes_audio.$outputExtension")
            },modifier=Modifier.weight(1f),enabled=!exporting && p.clips.isNotEmpty()) {
                Icon(Icons.Default.PlaylistAdd,null);Text(" Salvar e usar",fontSize=12.sp)
            }
        }
        if(exporting) {
            LinearProgressIndicator(modifier=Modifier.fillMaxWidth())
            Text("Extraindo áudio... mantenha esta tela aberta.",fontSize=12.sp)
        }
        if(message.isNotBlank()) Text(message,color=MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(30.dp))
    }
    pendingAudioDeleteId?.let { id ->
        val candidate=p.audioTracks.firstOrNull { it.id==id }
        if(candidate!=null) AlertDialog(onDismissRequest={pendingAudioDeleteId=null},
            title={Text("Remover faixa de áudio?")},
            text={Text("Remover '${candidate.title}' da edição? O arquivo original não será apagado.")},
            confirmButton={TextButton(onClick={
                if(editingAudioId==id) editingAudioId=null
                if(trackVolumeExpandedId==id) trackVolumeExpandedId=null
                update(p.copy(audioTracks=p.audioTracks.filterNot { it.id==id }))
                message="Faixa removida do projeto."
                pendingAudioDeleteId=null
            }) {Text("Remover")}},
            dismissButton={TextButton(onClick={pendingAudioDeleteId=null}) {Text("Cancelar")}})
    }
}

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PipScreen(p: VideoProject, selectedId: String, onSelect: (String) -> Unit,
                      back: () -> Unit, onChange: (Overlay) -> Unit,
                      onRemove: (String) -> Unit, choose: (Boolean) -> Unit) {
    val context = LocalContext.current
    val pip = p.overlays.firstOrNull { it.id == selectedId } ?: p.overlays.firstOrNull()
    val total = p.durationMs.coerceAtLeast(1L)
    val sourceAspect = remember(pip?.uri, pip?.isImage) {
        pip?.let { mediaAspect(context, it.uri, it.isImage) } ?: 16f / 9f
    }
    val player = remember(p.id) { diagnosticPlayer(context) }
    var playing by remember(p.id) { mutableStateOf(false) }
    var previewTime by remember(p.id) { mutableLongStateOf(pip?.startAtMs ?: 0L) }
    val canvasIndex=AdaptiveVideoLayout.activeClipIndex(p.clips.map { it.durationMs },previewTime)
    val canvasUri=p.clips.getOrNull(canvasIndex)?.uri
    val canvasAspect=remember(canvasUri) {
        canvasUri?.let { mediaAspect(context,it,false) } ?: 16f/9f
    }
    var expanded by remember(p.id) { mutableStateOf<String?>(null) }
    var manual by remember(pip?.id) {
        mutableStateOf(pip?.let { (it.scale * 100f).toInt().toString() } ?: "30")
    }
    DisposableEffect(player) { onDispose { player.release() } }
    LaunchedEffect(p.clips) {
        if (p.clips.isNotEmpty()) {
            player.setMediaItems(p.clips.map { clip ->
                MediaItem.Builder().setUri(clip.uri).setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(clip.startMs).setEndPositionMs(clip.endMs).build()
                ).build()
            })
            player.prepare()
            seekProjectPosition(player, p.clips, previewTime)
        }
    }
    // A seleção de camada posiciona o cursor no início dela; mexer no slider não força seek.
    LaunchedEffect(pip?.id) {
        if (pip != null && p.clips.isNotEmpty() && !player.isPlaying) {
            previewTime = pip.startAtMs
            seekProjectPosition(player, p.clips, previewTime)
        }
        expanded = null
    }
    LaunchedEffect(player, p.clips) {
        while (true) {
            playing = player.isPlaying
            if (player.isPlaying) previewTime = projectPosition(player, p.clips, p.durationMs)
            delay(150)
        }
    }
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        Heading("Editar sobreposições", back)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text("PiPs · ${p.overlays.size} de 4", fontWeight = FontWeight.SemiBold)
                TextButton(onClick = { choose(false) },
                    enabled = p.overlays.size < OverlayPlacement.MAX_PIPS) {
                    Icon(Icons.Default.Add, null)
                    Text(" Adicionar PiP")
                }
            }
            if (p.overlays.isEmpty()) {
                Text("Adicione até quatro imagens ou vídeos para sobrepor ao projeto.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    p.overlays.forEachIndexed { index, item ->
                        val selected = pip?.id == item.id
                        OutlinedButton(onClick = { onSelect(item.id) },
                            border = BorderStroke(if (selected) 2.dp else 1.dp,
                                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
                            Icon(if (item.isImage) Icons.Default.Image else Icons.Default.Videocam,
                                null, Modifier.size(18.dp))
                            Text(" PiP ${index + 1}")
                            if (selected) Icon(Icons.Default.Check, null, Modifier.size(16.dp))
                        }
                    }
                }
                Text("Prévia no vídeo", fontWeight = FontWeight.SemiBold)
                InlineVideoPreview(canvasAspect) {
                    AndroidView(factory = { previewPlayer(it).apply {
                        this.player = player
                        useController = false
                    } }, update = { it.player = player }, modifier = Modifier.fillMaxSize())
                    p.overlays.forEach { layer ->
                        val active = previewTime >= layer.startAtMs &&
                            previewTime < layer.startAtMs + layer.durationMs
                        if (active || (layer.id == pip?.id && !playing)) {
                            key(layer.id) {
                                PipLayer(layer, (previewTime - layer.startAtMs).coerceAtLeast(0L),
                                    playing && active, editable = layer.id == pip?.id,
                                    onUpdate = onChange)
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Text("Vídeo:", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        EditableSeekTime(previewTime, total, onSeek = { ms ->
                            previewTime = ms
                            seekProjectPosition(player, p.clips, ms)
                        }, modifier = Modifier.weight(1f))
                    }
                    TextButton(onClick = {
                        if (player.isPlaying) player.pause() else player.play()
                        playing = player.isPlaying
                    }, contentPadding = PaddingValues(horizontal = 2.dp)) {
                        Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                            "Reproduzir ou pausar vídeo")
                        Text(if (playing) "Pausar" else "Ver prévia")
                    }
                }
                Slider(value = previewTime.toFloat().coerceIn(0f, total.toFloat()),
                    onValueChange = { next ->
                        previewTime = next.toLong()
                        seekProjectPosition(player, p.clips, previewTime)
                    }, valueRange = 0f..total.toFloat(),
                    modifier = Modifier.fillMaxWidth().height(40.dp))
                Text("Toque nos horários para digitar. Arraste ou pince o PiP na prévia.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                // Quatro ferramentas compactas na MESMA linha; os ajustes ficam em um sheet inferior.
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        Triple("position", "Posição", Icons.Default.OpenWith),
                        Triple("size", "Tamanho", Icons.Default.AspectRatio),
                        Triple("duration", "Duração", Icons.Default.Schedule),
                        Triple("media", "Mídia", Icons.Default.PermMedia)
                    ).forEach { (key, label, icon) ->
                        Surface(onClick = { if (pip != null) expanded = key },
                            modifier = Modifier.weight(1f).aspectRatio(1f),
                            enabled = pip != null,
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant) {
                            Column(Modifier.fillMaxSize().padding(4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center) {
                                Icon(icon, contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp))
                                Spacer(Modifier.height(5.dp))
                                Text(label, fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp, maxLines = 1,
                                    overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
                // Som é independente dos demais controles e das demais sobreposições.
                if (pip != null && !pip.isImage) {
                    Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(16.dp)) {
                        Column(Modifier.fillMaxWidth().padding(12.dp)) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Icon(if (pip.muted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                    "Som deste PiP", tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(10.dp))
                                Text("Som do PiP selecionado", modifier = Modifier.weight(1f))
                                Switch(checked = !pip.muted,
                                    onCheckedChange = { onChange(pip.copy(muted = !it)) })
                            }
                            if (!pip.muted) Slider(value = pip.volume.coerceIn(0f, 1f),
                                onValueChange = { onChange(pip.copy(volume = it)) }, valueRange = 0f..1f)
                        }
                    }
                }
                Text("Até quatro PiPs independentes. Sobreposições serão confirmadas antes de aplicar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = { pip?.let { onRemove(it.id) } }, enabled = pip != null,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Default.Delete, null)
                Text(" Remover")
            }
            Button(onClick = back, modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Default.Check, null)
                Text(" Aplicar")
            }
        }
    }
    // ModalBottomSheet ocupa a parte inferior e fica POR CIMA dos botões.
    // A altura é limitada para preservar a visualização do vídeo na parte superior.
    if (expanded != null && pip != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val sheetHeight = (LocalConfiguration.current.screenHeightDp * 0.64f).dp
        ModalBottomSheet(
            onDismissRequest = { expanded = null },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            scrimColor = Color.Black.copy(alpha = 0.12f),
            dragHandle = null
        ) {
            Column(Modifier.fillMaxWidth()
                .heightIn(max = sheetHeight)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(when (expanded) {
                        "position" -> Icons.Default.OpenWith
                        "size" -> Icons.Default.AspectRatio
                        "duration" -> Icons.Default.Schedule
                        else -> Icons.Default.PermMedia
                    }, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
                    Text(when (expanded) {
                        "position" -> "Alinhar posição"
                        "size" -> "Tamanho do PiP"
                        "duration" -> "Duração do PiP"
                        else -> "Selecionar mídia"
                    }, style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f))
                    IconButton(onClick = { expanded = null }) {
                        Icon(Icons.Default.Close, "Fechar painel")
                    }
                }
                                when (expanded) {
                                    "position" -> {
                                        Text("Escolha um dos nove pontos. Se houver outro PiP, ele será reposicionado automaticamente.",
                                            style = MaterialTheme.typography.bodySmall)
                                        val names = listOf("Superior esquerdo", "Superior central", "Superior direito",
                                            "Meio esquerdo", "Centro", "Meio direito",
                                            "Inferior esquerdo", "Inferior central", "Inferior direito")
                                        val icons = listOf(Icons.Default.NorthWest, Icons.Default.North,
                                            Icons.Default.NorthEast, Icons.Default.West, Icons.Default.CenterFocusStrong,
                                            Icons.Default.East, Icons.Default.SouthWest, Icons.Default.South,
                                            Icons.Default.SouthEast)
                                        if (pip != null) for (row in 0..2) {
                                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                for (col in 0..2) {
                                                    val i = row * 3 + col
                                                    OutlinedButton(onClick = {
                                                        val (wf, hf) = OverlayEditorLogic.dimensions(
                                                            pip.scale, sourceAspect, canvasAspect)
                                                        val (x, y) = OverlayEditorLogic.preset(row, col, wf, hf)
                                                        onChange(pip.copy(x = x, y = y))
                                                    }, modifier = Modifier.weight(1f).height(50.dp)
                                                        .semantics { contentDescription = names[i] },
                                                        shape = RoundedCornerShape(12.dp),
                                                        contentPadding = PaddingValues(0.dp)) {
                                                        Icon(icons[i], null)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    "size" -> {
                                        if (pip != null) {
                                            Text("Tamanho: ${(pip.scale * 100f).toInt()}%",
                                                fontWeight = FontWeight.Medium)
                                            listOf(5, 10, 15, 20, 25, 30, 40, 50, 60, 75, 90).chunked(4).forEach { row ->
                                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                                    row.forEach { percent ->
                                                        OutlinedButton(onClick = {
                                                            val scale = percent / 100f
                                                            onChange(pip.copy(scale = scale))
                                                            manual = percent.toString()
                                                        }, modifier = Modifier.weight(1f),
                                                            shape = RoundedCornerShape(10.dp),
                                                            contentPadding = PaddingValues(0.dp),
                                                            border = BorderStroke(if ((pip.scale * 100f).toInt() == percent) 2.dp else 1.dp,
                                                                MaterialTheme.colorScheme.primary)) {
                                                            Text("$percent%")
                                                        }
                                                    }
                                                    repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                                                }
                                            }
                                            Slider(value = pip.scale.coerceIn(.05f, .90f),
                                                onValueChange = {
                                                    onChange(pip.copy(scale = it))
                                                    manual = (it * 100f).toInt().toString()
                                                }, valueRange = .05f..90f/100f)
                                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                OutlinedTextField(value = manual, onValueChange = { value ->
                                                    if (value.length <= 3 && value.all(Char::isDigit)) manual = value
                                                }, modifier = Modifier.weight(1f), singleLine = true,
                                                    label = { Text("Porcentagem manual") }, suffix = { Text("%") },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    isError = manual.isNotBlank() && OverlayEditorLogic.parsePercentage(manual) == null)
                                                Button(onClick = {
                                                    OverlayEditorLogic.parsePercentage(manual)?.let {
                                                        onChange(pip.copy(scale = it))
                                                    }
                                                }, enabled = OverlayEditorLogic.parsePercentage(manual) != null) {
                                                    Icon(Icons.Default.Check, null)
                                                    Text(" Usar")
                                                }
                                            }
                                            Text("De 5% a 90%; aspecto original preservado.",
                                                style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                    "duration" -> {
                                        if (pip != null) {
                                            val end = (pip.startAtMs + pip.durationMs).coerceAtMost(total)
                                            Text("Digite somente números; a máscara preenche mm:ss.d automaticamente.",
                                                style = MaterialTheme.typography.bodySmall)
                                            val minLength = minOf(500L, end.coerceAtLeast(1L))
                                            TimeDigitsField("Começa em", pip.startAtMs, total,
                                                valid = { it in 0L..(end - minLength).coerceAtLeast(0L) },
                                                onCommit = { typed -> onChange(pip.copy(
                                                    startAtMs = typed, durationMs = end - typed)) })
                                            Slider(value = pip.startAtMs.coerceIn(0L, end).toFloat(),
                                                onValueChange = { value ->
                                                    val (newStart, newDuration) = OverlayEditorLogic.startChanged(
                                                        value.toLong(), pip.startAtMs, pip.durationMs, total)
                                                    onChange(pip.copy(startAtMs = newStart, durationMs = newDuration))
                                                }, valueRange = 0f..end.coerceAtLeast(1L).toFloat())
                                            val minEnd = (pip.startAtMs + 500L).coerceAtMost(total)
                                            TimeDigitsField("Termina em", end, total,
                                                valid = { it in minEnd..total },
                                                onCommit = { typed -> onChange(pip.copy(
                                                    durationMs = typed - pip.startAtMs)) })
                                            if (minEnd < total) {
                                                Slider(value = end.coerceIn(minEnd, total).toFloat(),
                                                    onValueChange = { value ->
                                                        onChange(pip.copy(durationMs = OverlayEditorLogic.endChanged(
                                                            value.toLong(), pip.startAtMs, total)))
                                                    }, valueRange = minEnd.toFloat()..total.toFloat())
                                            } else Text("Limite do vídeo alcançado")
                                            BoxWithConstraints(Modifier.fillMaxWidth().height(16.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = .14f))) {
                                                Box(Modifier.offset(x = maxWidth * (pip.startAtMs.toFloat() / total))
                                                    .width(maxWidth * (pip.durationMs.toFloat() / total))
                                                    .fillMaxHeight().clip(RoundedCornerShape(8.dp))
                                                    .background(MaterialTheme.colorScheme.primary))
                                            }
                                        }
                                    }
                                    "media" -> {
                                        Text("Troque somente a mídia do PiP selecionado.",
                                            style = MaterialTheme.typography.bodySmall)
                                        Button(onClick = { expanded = null; choose(true) }, modifier = Modifier.fillMaxWidth()) {
                                            Icon(Icons.Default.PermMedia, null)
                                            Text(" Escolher imagem ou vídeo")
                                        }
                                    }
                                }
                Button(onClick = { expanded = null },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)) {
                    Icon(Icons.Default.Check, null)
                    Text(" Concluído")
                }
            }
        }
    }
}

/** Tocar no horário substitui o texto por um campo numérico na própria tela. */
@Composable
private fun EditableSeekTime(currentMs: Long, totalMs: Long, onSeek: (Long) -> Unit,
                             modifier: Modifier = Modifier) {
    var editing by remember { mutableStateOf(false) }
    if (!editing) {
        TextButton(onClick = {
            editing = true
        }, modifier = modifier, contentPadding = PaddingValues(horizontal = 3.dp)) {
            Text("${displayEditableTime(currentMs)} / ${displayEditableTime(totalMs)}",
                style = MaterialTheme.typography.bodySmall)
            Icon(Icons.Default.Edit, "Digitar horário", Modifier.size(14.dp))
        }
    } else {
        Row(modifier, verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            TimeDigitsField("Ir para", currentMs, totalMs,
                onCommit = { onSeek(it); editing = false },
                modifier = Modifier.weight(1f))
            IconButton(onClick = { editing = false }) {
                Icon(Icons.Default.Close, "Cancelar edição")
            }
        }
    }
}

private fun displayEditableTime(ms: Long): String =
    if (ms % 1000L == 0L) formatMs(ms) else formatPipMs(ms)

private fun formatPipMs(ms: Long): String {
    val minutes = (ms / 60000L)
    val seconds = (ms % 60000L) / 1000L
    val tenths = (ms % 1000L) / 100L
    return "%02d:%02d.%d".format(minutes, seconds, tenths)
}

/** Prévia ilustrada com quadros reais da junção, sem renderização ou áudio. */
@Composable
private fun TransitionFramePreview(a: Clip, b: Clip, kind: String, durationMs: Long) {
    val context = LocalContext.current
    val frames by produceState<Pair<ImageBitmap?, ImageBitmap?>>(null to null, a.uri, b.uri, a.endMs, b.startMs) {
        value = withContext(Dispatchers.IO) {
            fun frame(clip: Clip, atMs: Long): ImageBitmap? = runCatching {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, Uri.parse(clip.uri))
                    val timeUs = atMs.coerceAtLeast(0L) * 1000L
                    val bitmap = if (Build.VERSION.SDK_INT >= 27) {
                        retriever.getScaledFrameAtTime(timeUs,
                            MediaMetadataRetriever.OPTION_CLOSEST_SYNC, 320, 180)
                    } else {
                        retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    }
                    bitmap?.asImageBitmap()
                } finally { retriever.release() }
            }.getOrNull()
            frame(a, (a.endMs - 100L).coerceAtLeast(a.startMs)) to frame(b, b.startMs)
        }
    }
    var progress by remember(a.id, b.id, kind, durationMs) { mutableFloatStateOf(0f) }
    LaunchedEffect(a.id, b.id, kind, durationMs) {
        while (true) {
            val total = (durationMs.coerceIn(250L, 1000L) * 2L).coerceAtLeast(500L)
            val steps = (total / 40L).toInt().coerceAtLeast(1)
            for (step in 0..steps) {
                progress = step.toFloat() / steps
                delay(40L)
            }
            delay(450L)
        }
    }
    val showFirst = progress < .5f
    val fade = if (kind == "Corte direto") 0f else
        if (showFirst) progress * 2f else (1f - progress) * 2f
    val fadeColor = if (kind == "Fade branco") Color.White else Color.Black
    Box(Modifier.fillMaxWidth().height(175.dp).clip(RoundedCornerShape(12.dp))
        .background(Color(0xFF222222)), contentAlignment = Alignment.Center) {
        val bitmap = if (showFirst) frames.first else frames.second
        if (bitmap != null) Image(bitmap, "Quadro da transição", Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit)
        else Text(if (showFirst) "Clipe A" else "Clipe B", color = Color.White)
        if (fade > 0f) Box(Modifier.fillMaxSize().background(fadeColor.copy(alpha = fade.coerceIn(0f, 1f))))
    }
    Text("Prévia animada com quadros dos dois clipes (sem áudio). A exportação usa o efeito Media3.",
        style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun TransitionScreen(p:VideoProject,back:()->Unit,update:(VideoProject)->Unit,initialBoundary:Int=0) {
    var boundary by remember(p.id,initialBoundary) { mutableIntStateOf(initialBoundary.coerceIn(0,(p.clips.size-2).coerceAtLeast(0))) }
    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())
        .padding(horizontal=16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Heading("Transições",back)
        if (p.clips.size < 2) Text("Adicione pelo menos dois clipes para criar uma transição.")
        else {
            Text("Escolha entre quais clipes aplicar",fontWeight=FontWeight.Medium)
            p.clips.zipWithNext().forEachIndexed { index,(a,b) ->
                FilterChip(selected=boundary==index,onClick={boundary=index},
                    label={Text("${index+1} → ${index+2}: ${a.title.take(11)} / ${b.title.take(11)}")})
            }
            val current=EditFeatures.boundary(p,boundary)
            val kind=current?.kind ?: "Corte direto"
            TransitionFramePreview(p.clips[boundary], p.clips[boundary+1], kind, current?.durationMs ?: 500L)
            Text("Efeito",fontWeight=FontWeight.Bold)
            Text("Toque em um efeito para aplicá-lo à junção selecionada.",style=MaterialTheme.typography.bodySmall)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                listOf("Corte direto","Fade preto","Fade branco").forEach { value ->
                    val selected=kind==value
                    Surface(onClick={
                        val a=p.clips[boundary];val b=p.clips[boundary+1]
                        update(p.copy(transitions=p.transitions.filterNot{
                            it.fromId==a.id && it.toId==b.id
                        }+ClipTransition(a.id,b.id,value,current?.durationMs?:500)))
                    },modifier=Modifier.weight(1f),shape=RoundedCornerShape(9.dp),
                        color=if(selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        border=BorderStroke(if(selected)2.dp else 1.dp,
                            if(selected)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)) {
                        Column(Modifier.padding(8.dp),horizontalAlignment=Alignment.CenterHorizontally,
                            verticalArrangement=Arrangement.spacedBy(6.dp)) {
                            Box(Modifier.fillMaxWidth().height(55.dp).background(Color(0xFF292929),RoundedCornerShape(5.dp)),
                                contentAlignment=Alignment.Center) {
                                when(value) {
                                    "Corte direto" -> Text("A | B",color=Color.White)
                                    "Fade preto" -> Text("A ▪ B",color=Color.White)
                                    else -> Text("A □ B",color=Color.White)
                                }
                            }
                            Text(value,fontSize=11.sp,maxLines=2,textAlign=TextAlign.Center)
                        }
                    }
                }
            }
            if(kind!="Corte direto") {
                Text("Duração do fade: ${current?.durationMs?:500} ms")
                Slider(value=(current?.durationMs?:500).toFloat(),valueRange=250f..1000f,steps=2,
                    onValueChange={value->
                        val a=p.clips[boundary];val b=p.clips[boundary+1]
                        update(p.copy(transitions=p.transitions.filterNot{
                            it.fromId==a.id && it.toId==b.id
                        }+ClipTransition(a.id,b.id,kind,value.toLong())))
                    })
            }
            Text("Fade preto/branco: o vídeo escurece ou clareia no fim do clipe e volta ao normal no começo do seguinte. A duração total não muda.",
                style=MaterialTheme.typography.bodySmall)
            Text("Dissolvência real entre duas imagens ainda não é suportada nesta composição Media3; não será aplicada como se estivesse pronta.",
                style=MaterialTheme.typography.bodySmall,
                color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun subtitleFont(family: String): FontFamily = when (SubtitleStyles.normalizeFont(family)) {
    SubtitleStyles.FONT_SERIF -> FontFamily.Serif
    SubtitleStyles.FONT_MONO -> FontFamily.Monospace
    else -> FontFamily.SansSerif
}

/** Quadro sem barras pretas adicionais: respeita a proporção original e a
 * altura disponível em cada aparelho. Mantém overlays no mesmo referencial
 * geométrico da imagem, inclusive em vídeos verticais e quadrados. */
@Composable
private fun InlineVideoPreview(aspect: Float, modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit) {
    val screenHeight = LocalConfiguration.current.screenHeightDp.toFloat()
    BoxWithConstraints(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val fit = AdaptiveVideoLayout.inline(maxWidth.value, screenHeight, aspect)
        Box(Modifier.width(fit.width.dp).height(fit.height.dp)
            .clip(RoundedCornerShape(12.dp)).background(Color(0xFF101014)),
            content = content)
    }
}

/** Mede a maior palavra para impedir que a prévia estreita quebre sílabas.
 * A redução é apenas visual; sizeSp e o arquivo SRT não são modificados. */
private fun previewSubtitleScale(cue: SubtitleCue, widthPx: Int, textSizePx: Float): Float {
    if (widthPx <= 0 || textSizePx <= 0f || !textSizePx.isFinite()) return 1f
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        textSize = textSizePx
        typeface = android.graphics.Typeface.create(when (SubtitleStyles.normalizeFont(cue.fontFamily)) {
            SubtitleStyles.FONT_SERIF -> "serif"
            SubtitleStyles.FONT_MONO -> "monospace"
            else -> "sans-serif"
        }, if (cue.bold) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
    }
    val longest = cue.text.split(Regex("\\s+")).maxOfOrNull { paint.measureText(it) } ?: 0f
    if (longest <= 0f) return 1f
    val available = (widthPx * .90f - textSizePx * .65f).coerceAtLeast(1f)
    return (available / longest).coerceIn(.35f, 1f)
}

/** A mesma coordenada x/y usada pelo exportador Media3: centro na horizontal,
 * base do texto na vertical. Nunca usar deslocamento fixo em dp para a legenda. */
@Composable
private fun SubtitleOverlayOnFrame(cue: SubtitleCue, editable: Boolean,
    playbackMs: Long = cue.startMs, onPositionCommit: (Float,Float)->Unit = { _,_ -> }) {
    val animationFrame=SubtitleAnimations.frame(cue,playbackMs)
    var frameSize by remember { mutableStateOf(IntSize.Zero) }
    var textSize by remember { mutableStateOf(IntSize.Zero) }
    var draftX by remember(cue.id) { mutableFloatStateOf(cue.x) }
    var draftY by remember(cue.id) { mutableFloatStateOf(cue.y) }
    var dragging by remember(cue.id) { mutableStateOf(false) }
    val latestCommit by rememberUpdatedState(onPositionCommit)
    LaunchedEffect(cue.id,cue.x,cue.y) {
        if (!dragging) { draftX=cue.x; draftY=cue.y }
    }
    Box(Modifier.fillMaxSize().onSizeChanged { frameSize=it }) {
        if (animationFrame.text.isNotBlank()) {
            val rawLocation=SubtitlePlacement.anchorPixels(frameSize.width,frameSize.height,
                textSize.width,textSize.height,draftX,draftY)
            val location=rawLocation.first.coerceIn(0,(frameSize.width-textSize.width).coerceAtLeast(0)) to
                rawLocation.second.coerceIn(0,(frameSize.height-textSize.height).coerceAtLeast(0))
            val density=LocalDensity.current
            val frameWidthDp=with(density) { frameSize.width.toDp() }
            val textLimitPx=minOf(frameSize.width * .94f,with(density) { 280.dp.toPx() })
            val fontScale=previewSubtitleScale(cue,textLimitPx.toInt(),
                with(density) { cue.sizeSp.sp.toPx() })
            val previewFontSp=cue.sizeSp * fontScale
            val family = subtitleFont(cue.fontFamily)
            val textWeight = if (cue.bold) FontWeight.Bold else FontWeight.Normal
            val outlineWidthPx = with(density) { cue.outlineWidth.dp.toPx() } * fontScale
            val shadowPx = with(density) { cue.shadowOffsetDp.dp.toPx() } * fontScale
            val alignment = when (SubtitleStyles.normalizeAlignment(cue.textAlignment)) {
                SubtitleStyles.ALIGN_LEFT -> TextAlign.Left
                SubtitleStyles.ALIGN_RIGHT -> TextAlign.Right
                else -> TextAlign.Center
            }
            val spacing = SubtitleStyles.normalizeLineSpacing(cue.lineSpacingPercent) / 100f
            val textLineHeight = if (cue.lineSpacingPercent > 100)
                (previewFontSp * 1.2f * spacing).sp else TextUnit.Unspecified
            val fillStyle = TextStyle(lineHeight = textLineHeight,
                shadow = if (cue.shadowOffsetDp > 0) Shadow(
                    color=Color(cue.shadowColor), offset=Offset(shadowPx,shadowPx),blurRadius=0f
                ) else null)
            Box(modifier=Modifier.offset { IntOffset(location.first,location.second) }
                .widthIn(max=minOf(280.dp,(frameWidthDp * .94f).coerceAtLeast(14.dp)))
                .onSizeChanged { textSize=it }
                .graphicsLayer {
                    alpha=animationFrame.alpha
                    scaleX=animationFrame.scale
                    scaleY=animationFrame.scale
                    translationY=animationFrame.slideY * frameSize.height
                }
                .then(if (editable) Modifier.pointerInput(cue.id,frameSize) {
                    detectDragGestures(
                        onDragStart={dragging=true},
                        onDragEnd={dragging=false;latestCommit(draftX,draftY)},
                        onDragCancel={dragging=false;draftX=cue.x;draftY=cue.y},
                        onDrag={change,delta->
                            change.consume()
                            val next=SubtitlePlacement.drag(draftX,draftY,delta.x,delta.y,
                                frameSize.width,frameSize.height)
                            draftX=next.first;draftY=next.second
                        })
                } else Modifier)
                .background(Color(cue.backgroundColor),RoundedCornerShape(3.dp))
                .padding(horizontal=7.dp,vertical=2.dp)) {
                if (cue.outlineWidth > 0) {
                    Text(animationFrame.text,color=Color(cue.outlineColor),fontSize=previewFontSp.sp,
                        fontFamily=family,fontWeight=textWeight,textAlign=alignment,
                        style=TextStyle(lineHeight=textLineHeight,
                            drawStyle=Stroke(width=outlineWidthPx*2f,join=StrokeJoin.Round)))
                }
                val animatedText=buildAnnotatedString {
                    append(animationFrame.text)
                    animationFrame.highlighted?.let { range ->
                        if(range.first>=0 && range.last<animationFrame.text.length)
                            addStyle(SpanStyle(color=Color(0xFFFFFF00)),range.first,range.last+1)
                    }
                }
                Text(animatedText,color=Color(cue.color),fontSize=previewFontSp.sp,
                    fontFamily=family,fontWeight=textWeight,textAlign=alignment,
                    style=fillStyle)
            }
        }
    }
}

/** Prévia fixa e reproduzível: seleciona o quadro da legenda usando os
 * cortes reais do projeto. O texto pode ser arrastado sem tocar no MP4 original.
 * A reprodução para no fim da legenda selecionada; não substitui a exportação.
 */
@Composable
private fun SubtitlePositionPreview(p:VideoProject,cue:SubtitleCue,
    onPositionCommit:(Float,Float)->Unit,
    onPrevious:()->Unit, onNext:()->Unit,
    hasPrevious:Boolean, hasNext:Boolean) {
    val context=LocalContext.current
    val player=remember(p.id) { diagnosticPlayer(context) }
    var playing by remember(p.id) { mutableStateOf(false) }
    var previewMs by remember(p.id) { mutableLongStateOf(cue.startMs) }
    DisposableEffect(player) { onDispose { player.release() } }
    LaunchedEffect(player,p.clips.map { Triple(it.uri,it.startMs,it.endMs) }) {
        playing=false
        player.pause()
        player.setMediaItems(p.clips.map { clip ->
            MediaItem.Builder().setUri(clip.uri).setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder().setStartPositionMs(clip.startMs)
                    .setEndPositionMs(clip.endMs).build()).build()
        })
        player.prepare()
    }
    LaunchedEffect(cue.id,cue.startMs,p.clips) {
        previewMs=cue.startMs
        playing=false
        player.pause()
        seekProjectPosition(player,p.clips,cue.startMs)
    }
    LaunchedEffect(playing,cue.id,cue.endMs) {
        while(playing) {
            previewMs=projectPosition(player,p.clips,p.durationMs)
            if(player.playbackState==Player.STATE_ENDED ||
                projectPosition(player,p.clips,p.durationMs)>=cue.endMs) {
                player.pause()
                playing=false
            }
            delay(120)
        }
    }
    val source=remember(p.clips,cue.startMs) {
        TimelinePrecision.sourceAt(p.clips.map { it.uri to it.durationMs },cue.startMs)
    }
    val clip=p.clips.getOrNull(source?.first ?: -1)
    val aspect=remember(clip?.uri) {
        clip?.let { mediaAspect(context,it.uri,false) } ?: 16f/9f
    }
    Column {
        InlineVideoPreview(aspect) {
            AndroidView(factory={ctx->previewPlayer(ctx).apply {
                this.player=player
                useController=false
            }},update={it.player=player},modifier=Modifier.fillMaxSize())
            SubtitleOverlayOnFrame(cue,editable=true,playbackMs=previewMs,
                onPositionCommit=onPositionCommit)
        }
        Row(Modifier.fillMaxWidth().height(38.dp),verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.Center) {
            IconButton(enabled=hasPrevious,onClick=onPrevious,modifier=Modifier.size(36.dp)) {
                Icon(Icons.Default.ChevronLeft,"Legenda anterior",modifier=Modifier.size(25.dp))
            }
            IconButton(onClick={
                player.pause();playing=false
                previewMs=cue.startMs
                seekProjectPosition(player,p.clips,cue.startMs)
            },modifier=Modifier.size(34.dp)) {
                Icon(Icons.Default.SkipPrevious,"Reiniciar legenda",modifier=Modifier.size(22.dp))
            }
            IconButton(enabled=p.clips.isNotEmpty(),onClick={
                if(playing) {player.pause();playing=false}
                else {
                    if(projectPosition(player,p.clips,p.durationMs)>=cue.endMs)
                        seekProjectPosition(player,p.clips,cue.startMs)
                    player.play();playing=true
                }
            },modifier=Modifier.size(36.dp)) {
                Icon(if(playing)Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                    "Reproduzir ou pausar prévia",modifier=Modifier.size(28.dp))
            }
            IconButton(enabled=hasNext,onClick=onNext,modifier=Modifier.size(36.dp)) {
                Icon(Icons.Default.ChevronRight,"Próxima legenda",modifier=Modifier.size(25.dp))
            }
            Text("${formatPipMs(cue.startMs)}–${formatPipMs(cue.endMs)}",
                fontSize=11.sp,maxLines=1,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SubtitleScreen(p:VideoProject,back:()->Unit,update:(VideoProject)->Unit,
    initialSelectedId: String? = null, onSaveHome: () -> Unit) {
    val context=LocalContext.current
    var selectedId by remember(p.id,initialSelectedId) {
        mutableStateOf(initialSelectedId?.takeIf { id -> p.subtitles.any { it.id==id } }
            ?: p.subtitles.firstOrNull()?.id.orEmpty())
    }
    var status by remember { mutableStateOf("") }
    // Deve preceder o callback do importador, que limpa a busca após importar SRT.
    var searchQuery by remember(p.id) { mutableStateOf("") }
    val importer=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if(uri!=null) {
            val loaded=runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { reader ->
                    SubtitleListNavigation.readSrtBounded(reader)
                }.orEmpty()
            }
            loaded.onSuccess { content ->
                val cues=EditFeatures.parseSrt(content).filter{it.startMs<p.durationMs}
                    .map{it.copy(endMs=it.endMs.coerceAtMost(p.durationMs))}
                if(cues.isNotEmpty()) {
                    update(p.copy(subtitles=p.subtitles+cues))
                    selectedId=cues.first().id
                    searchQuery=""
                    status="${cues.size} legendas importadas"
                } else status="Nenhuma legenda SRT válida no arquivo"
            }.onFailure {DiagnosticRecorder.exception("srt_import", it);status="Não foi possível ler o arquivo: ${it.message}"}
        }
    }
    val sorted = remember(p.subtitles) {
        p.subtitles.sortedWith(compareBy<SubtitleCue> { it.startMs }.thenBy { it.id })
    }
    val selected=p.subtitles.firstOrNull{it.id==selectedId}
    val selectedIndex=sorted.indexOfFirst { it.id==selectedId }
    var editorOpen by remember(p.id) { mutableStateOf(false) }
    var showAdvanced by remember(p.id) { mutableStateOf(false) }
    var searchOpen by remember(p.id) { mutableStateOf(false) }
    var pendingDeleteId by remember(p.id) { mutableStateOf<String?>(null) }
    val filtered=remember(sorted,searchQuery) {
        sorted.filter { SubtitleListNavigation.matches(it.text,searchQuery) }
    }
    val originalNumbers=remember(sorted) { sorted.mapIndexed { index,cue -> cue.id to index+1 }.toMap() }
    val captionsState=rememberLazyListState()
    LaunchedEffect(selectedId,searchQuery,filtered) {
        val index=filtered.indexOfFirst { it.id==selectedId }
        if(index>=0) captionsState.animateScrollToItem(index)
        else if(searchQuery.isNotBlank() && filtered.isNotEmpty()) captionsState.scrollToItem(0)
    }
    LaunchedEffect(p.subtitles.size) {
        if(selectedId.isNotEmpty() && p.subtitles.none { it.id==selectedId }) {
            selectedId=p.subtitles.firstOrNull()?.id.orEmpty()
            editorOpen=false
        }
    }
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal=14.dp)) {
        Heading("Legendas",back)
        if(selected!=null) {
            SubtitlePositionPreview(p,selected,
                onPositionCommit={ x,y ->
                    update(p.copy(subtitles=p.subtitles.map { cue ->
                        if(cue.id==selected.id) cue.copy(x=x,y=y) else cue
                    }))
                },
                onPrevious={
                    SubtitleListNavigation.adjacentIndex(selectedIndex,sorted.size,-1)?.let { index ->
                        selectedId=sorted[index].id;editorOpen=true;showAdvanced=false;searchQuery=""
                    }
                },
                onNext={
                    SubtitleListNavigation.adjacentIndex(selectedIndex,sorted.size,1)?.let { index ->
                        selectedId=sorted[index].id;editorOpen=true;showAdvanced=false;searchQuery=""
                    }
                },
                hasPrevious=SubtitleListNavigation.adjacentIndex(selectedIndex,sorted.size,-1)!=null,
                hasNext=SubtitleListNavigation.adjacentIndex(selectedIndex,sorted.size,1)!=null)
        } else Box(Modifier.fillMaxWidth().height(196.dp)
            .background(Color(0xFF101014),RoundedCornerShape(12.dp)),
            contentAlignment=Alignment.Center) {
            Text("Adicione uma legenda para visualizar o quadro",color=Color.White,fontSize=12.sp)
        }
        Text("Arraste a legenda sobre a prévia para posicionar",fontSize=11.sp,
            color=MaterialTheme.colorScheme.onSurfaceVariant,
            modifier=Modifier.padding(top=4.dp,bottom=4.dp))
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,
            verticalAlignment=Alignment.CenterVertically) {
            Text("Linha do tempo",fontSize=13.sp,fontWeight=FontWeight.SemiBold)
            Text(if(selected==null)"Sem seleção" else
                "${formatPipMs(selected.startMs)} – ${formatPipMs(selected.endMs)}",
                fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        SubtitleCompactTimeline(sorted,p.durationMs,selectedId,
            onSelect={ id -> selectedId=id;editorOpen=false })
        Row(Modifier.fillMaxWidth().padding(vertical=4.dp),
            verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(5.dp)) {
            Text("Legendas (${p.subtitles.size})",fontSize=14.sp,maxLines=1,
                fontWeight=FontWeight.SemiBold,modifier=Modifier.weight(1f))
            IconButton(onClick={searchOpen=!searchOpen; if(!searchOpen) searchQuery=""},
                modifier=Modifier.size(30.dp)) {
                Icon(if(searchOpen) Icons.Default.SearchOff else Icons.Default.Search,
                    if(searchOpen) "Fechar busca" else "Buscar legendas",modifier=Modifier.size(19.dp))
            }
            TextButton(enabled=p.durationMs>0,onClick={
                SubtitleListNavigation.newInterval(p.durationMs,selected?.endMs)?.let { (start,end) ->
                    val cue=SubtitleCue(text="Nova legenda",startMs=start,endMs=end)
                    update(p.copy(subtitles=p.subtitles+cue))
                    searchQuery="";selectedId=cue.id;editorOpen=true;showAdvanced=false
                }
            },contentPadding=PaddingValues(horizontal=1.dp,vertical=2.dp)) {
                Icon(Icons.Default.Add,"Nova legenda",Modifier.size(18.dp))
                Text("Nova",fontSize=11.sp,maxLines=1)
            }
            OutlinedButton(onClick={importer.launch(arrayOf("text/*","application/x-subrip","*/*"))},
                contentPadding=PaddingValues(horizontal=4.dp,vertical=3.dp)) {
                Icon(Icons.Default.FileOpen,"Importar SRT",Modifier.size(17.dp))
                Text("SRT",fontSize=11.sp,maxLines=1)
            }
            OutlinedButton(onClick=onSaveHome,
                contentPadding=PaddingValues(horizontal=4.dp,vertical=3.dp)) {
                Icon(Icons.Default.Save,"Salvar projeto e voltar à tela inicial",Modifier.size(17.dp))
                Text("Salvar",fontSize=11.sp,maxLines=1)
            }
        }
        if(searchOpen) {
            OutlinedTextField(value=searchQuery,onValueChange={searchQuery=it.take(100)},
                label={Text("Buscar texto da legenda")},singleLine=true,
                modifier=Modifier.fillMaxWidth(),
                trailingIcon={if(searchQuery.isNotBlank()) IconButton(onClick={searchQuery=""}) {
                    Icon(Icons.Default.Clear,"Limpar busca")
                }})
            Text("${filtered.size} de ${sorted.size} legendas",fontSize=11.sp,
                color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if(status.isNotBlank()) Text(status,fontSize=12.sp,
            color=MaterialTheme.colorScheme.primary)
        LazyColumn(state=captionsState, modifier=Modifier.fillMaxWidth().weight(1f),
            verticalArrangement=Arrangement.spacedBy(5.dp),
            contentPadding=PaddingValues(bottom=24.dp)) {
            if(filtered.isEmpty()) item {
                Text(if(sorted.isEmpty()) "Nenhuma legenda. Toque em Nova ou importe um SRT."
                    else "Nenhuma legenda corresponde à busca.",
                    color=MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier=Modifier.padding(vertical=20.dp))
            }
            itemsIndexed(filtered,key={_,cue->cue.id}) { _,cue ->
                val number=originalNumbers[cue.id] ?: 0
                Column {
                    Surface(onClick={
                        val same=selectedId==cue.id
                        selectedId=cue.id
                        editorOpen=if(same)!editorOpen else true
                        showAdvanced=false
                    },shape=RoundedCornerShape(8.dp),
                        color=if(selectedId==cue.id)MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant) {
                        Row(Modifier.fillMaxWidth().heightIn(min=44.dp).padding(horizontal=10.dp),
                            verticalAlignment=Alignment.CenterVertically) {
                            Text("$number",fontSize=12.sp,
                                color=MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier=Modifier.width(26.dp))
                            Text(cue.text,modifier=Modifier.weight(1f),fontSize=13.sp,
                                maxLines=1,overflow=TextOverflow.Ellipsis)
                            Spacer(Modifier.width(6.dp))
                            Text(formatPipMs(cue.startMs),fontSize=11.sp,
                                color=MaterialTheme.colorScheme.onSurfaceVariant)
                            Icon(if(selectedId==cue.id && editorOpen)Icons.Default.ExpandLess
                                else Icons.Default.Edit,contentDescription="Editar legenda",
                                modifier=Modifier.padding(start=8.dp).size(18.dp))
                        }
                    }
                }
            }
        }
    }
    if(editorOpen && selected!=null) {
        SubtitleEditDialog(cue=selected,totalMs=p.durationMs,
            number=originalNumbers[selected.id] ?: 1,onDismiss={editorOpen=false},
            onDelete={editorOpen=false;pendingDeleteId=selected.id},
            onSave={draft,copyOptions ->
                val updated=p.subtitles.map { cue -> if(cue.id==draft.id) draft else cue }
                val all=if(copyOptions!=null)
                    SubtitleStyles.applySelectedToAll(updated,draft,copyOptions) else updated
                update(p.copy(subtitles=all))
                editorOpen=false
            })
    }
    pendingDeleteId?.let { id ->
        val toDelete=p.subtitles.firstOrNull { it.id==id }
        if(toDelete!=null) AlertDialog(onDismissRequest={pendingDeleteId=null},
            title={Text("Excluir esta legenda?")},
            text={Text("${toDelete.text.take(120)}\nEsta ação remove apenas a legenda, não o vídeo original.")},
            confirmButton={TextButton(onClick={
                update(p.copy(subtitles=p.subtitles.filterNot { it.id==id }))
                if(selectedId==id) {
                    val remaining=sorted.filterNot { it.id==id }
                    selectedId=remaining.getOrNull(selectedIndex.coerceAtMost(remaining.lastIndex))?.id.orEmpty()
                    editorOpen=false;showAdvanced=false
                }
                pendingDeleteId=null
            }) {Text("Excluir")}},
            dismissButton={TextButton(onClick={pendingDeleteId=null}) {Text("Cancelar")}})
        else LaunchedEffect(id) { pendingDeleteId=null }
    }
}

/** Editor modal compacto. Alterações locais só são persistidas em Salvar. */
@Composable
private fun SubtitleEditDialog(cue: SubtitleCue,totalMs: Long,number: Int,
    onDismiss: () -> Unit,onDelete: () -> Unit,
    onSave: (SubtitleCue,SubtitleStyles.CopyOptions?) -> Unit) {
    var draft by remember(cue.id) { mutableStateOf(cue) }
    var stylesOpen by remember(cue.id) { mutableStateOf(false) }
    var animationOpen by remember(cue.id) { mutableStateOf(false) }
    var applyAll by remember(cue.id) { mutableStateOf(false) }
    var copyAppearance by remember(cue.id) { mutableStateOf(true) }
    var copyPosition by remember(cue.id) { mutableStateOf(false) }
    var copyAnimation by remember(cue.id) { mutableStateOf(true) }
    var confirmAll by remember(cue.id) { mutableStateOf(false) }
    var sampleMs by remember(cue.id) { mutableLongStateOf(0L) }
    LaunchedEffect(draft.animation,draft.animationSpeed,draft.text) {
        sampleMs=0L
        while(true) {
            delay(80L)
            sampleMs=(sampleMs+80L)%3000L
        }
    }
    val valid=draft.text.isNotBlank() && draft.startMs>=0L &&
        draft.endMs>draft.startMs && draft.endMs<=totalMs
    fun submit() {
        if(!valid) return
        val options=if(applyAll) SubtitleStyles.CopyOptions(
            appearance=copyAppearance,position=copyPosition,animation=copyAnimation) else null
        onSave(draft,options)
    }
    Dialog(onDismissRequest=onDismiss,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        val screenHeight=LocalConfiguration.current.screenHeightDp.dp
        Surface(Modifier.fillMaxWidth(.96f).heightIn(max=screenHeight*.88f),
            shape=RoundedCornerShape(18.dp),tonalElevation=5.dp) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(14.dp),verticalArrangement=Arrangement.spacedBy(9.dp)) {
                Row(verticalAlignment=Alignment.CenterVertically,modifier=Modifier.fillMaxWidth()) {
                    Text("Editar legenda $number",fontWeight=FontWeight.Bold,
                        style=MaterialTheme.typography.titleMedium,modifier=Modifier.weight(1f))
                    IconButton(onClick=onDismiss,modifier=Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close,"Fechar")
                    }
                }
                // Simulação do texto, sem segunda miniatura de vídeo.
                val sample=SubtitleAnimations.frame(draft.copy(startMs=0L,endMs=3000L),sampleMs)
                Box(Modifier.fillMaxWidth().heightIn(min=76.dp)
                    .background(Color(0xFF13151A),RoundedCornerShape(10.dp))
                    .padding(12.dp),contentAlignment=Alignment.Center) {
                    val previewText=buildAnnotatedString {
                        append(sample.text)
                        sample.highlighted?.let { range ->
                            if(range.first>=0 && range.last<sample.text.length)
                                addStyle(SpanStyle(color=Color.Yellow),range.first,range.last+1)
                        }
                    }
                    Box(Modifier.graphicsLayer {
                        alpha=sample.alpha;scaleX=sample.scale;scaleY=sample.scale
                        translationY=sample.slideY*120f
                    }.background(Color(draft.backgroundColor),RoundedCornerShape(3.dp))
                        .padding(horizontal=7.dp,vertical=3.dp)) {
                        Text(previewText,color=Color(draft.color),
                            fontSize=draft.sizeSp.sp,fontFamily=subtitleFont(draft.fontFamily),
                            fontWeight=if(draft.bold) FontWeight.Bold else FontWeight.Normal,
                            textAlign=TextAlign.Center)
                    }
                }
                OutlinedTextField(value=draft.text,onValueChange={draft=draft.copy(text=it.take(500))},
                    label={Text("Texto")},maxLines=3,modifier=Modifier.fillMaxWidth())
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp),modifier=Modifier.fillMaxWidth()) {
                    TimeDigitsField("Início",draft.startMs,totalMs,
                        valid={it>=0L && it<draft.endMs},
                        onCommit={draft=draft.copy(startMs=it)},
                        modifier=Modifier.weight(1f),compact=true)
                    TimeDigitsField("Fim",draft.endMs,totalMs,
                        valid={it>draft.startMs && it<=totalMs},
                        onCommit={draft=draft.copy(endMs=it)},
                        modifier=Modifier.weight(1f),compact=true)
                }
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Text("Tamanho: ${draft.sizeSp}",fontSize=13.sp,
                        modifier=Modifier.weight(1f))
                    Slider(value=draft.sizeSp.toFloat(),valueRange=12f..48f,
                        modifier=Modifier.weight(2f),
                        onValueChange={draft=draft.copy(sizeSp=it.toInt().coerceIn(12,48))})
                }
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp)) {
                    OutlinedButton(onClick={stylesOpen=!stylesOpen},modifier=Modifier.weight(1f),
                        contentPadding=PaddingValues(5.dp)) {Text("Estilo",maxLines=1)}
                    OutlinedButton(onClick={animationOpen=!animationOpen},modifier=Modifier.weight(1f),
                        contentPadding=PaddingValues(5.dp)) {Text("Animação",maxLines=1)}
                    OutlinedButton(onClick=onDelete,contentPadding=PaddingValues(6.dp)) {
                        Icon(Icons.Default.Delete,"Excluir legenda",tint=MaterialTheme.colorScheme.error)
                    }
                }
                if(animationOpen) {
                    Text("Efeitos de entrada e revelação",fontWeight=FontWeight.SemiBold)
                    // Carrossel circular virtualizado: sempre permite rolar nos dois sentidos.
                    val choices=SubtitleAnimations.choices
                    val carouselStart=CircularCarousel.startIndex(choices.size)
                    val effectsScroll=rememberLazyListState(initialFirstVisibleItemIndex=carouselStart)
                    LazyRow(state=effectsScroll,modifier=Modifier.fillMaxWidth(),
                        horizontalArrangement=Arrangement.spacedBy(7.dp),
                        contentPadding=PaddingValues(horizontal=3.dp)) {
                        items(count=Int.MAX_VALUE) { index ->
                            val (key,label)=choices[CircularCarousel.optionIndex(index,choices.size)]
                            FilterChip(selected=draft.animation==key,onClick={
                                draft=draft.copy(animation=key)
                            },label={Text(label,maxLines=1)})
                        }
                    }
                    if(draft.animation!=SubtitleAnimations.NONE) {
                        Text("Velocidade: ${"%.1f".format(java.util.Locale.US,draft.animationSpeed)}×")
                        Slider(value=draft.animationSpeed,valueRange=.5f..2f,steps=5,
                            onValueChange={draft=draft.copy(animationSpeed=it)})
                        Text("Os efeitos seguem o tempo da legenda; não sincronizam com a voz.",
                            fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if(stylesOpen) {
                    Text("Estilos rápidos",fontWeight=FontWeight.SemiBold)
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                        SubtitleStyles.presets.forEach { preset ->
                            FilterChip(selected=draft.color==preset.foreground &&
                                draft.backgroundColor==preset.background,onClick={
                                draft=SubtitleStyles.applyToCue(draft,preset)
                            },label={Text(preset.name)})
                        }
                    }
                    Text("Fonte",fontWeight=FontWeight.SemiBold)
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                        SubtitleStyles.fonts.forEach { (family,name) ->
                            FilterChip(selected=draft.fontFamily==family,onClick={
                                draft=draft.copy(fontFamily=family)
                            },label={Text(name)})
                        }
                    }
                    Text("Contorno: ${draft.outlineWidth} dp")
                    Slider(value=draft.outlineWidth.toFloat(),valueRange=0f..4f,steps=3,
                        onValueChange={draft=draft.copy(outlineWidth=it.toInt())})
                    if(draft.outlineWidth>0) Row {
                        listOf("Preto" to 0xFF000000.toInt(),
                            "Branco" to 0xFFFFFFFF.toInt()).forEach { (label,color) ->
                            FilterChip(selected=draft.outlineColor==color,
                                onClick={draft=draft.copy(outlineColor=color)},label={Text(label)})
                        }
                    }
                    Text("Sombra: ${draft.shadowOffsetDp} dp")
                    Slider(value=draft.shadowOffsetDp.toFloat(),valueRange=0f..6f,steps=5,
                        onValueChange={draft=draft.copy(shadowOffsetDp=it.toInt())})
                    if(draft.shadowOffsetDp>0) Row {
                        listOf("Preta" to 0xFF000000.toInt(),
                            "Branca" to 0xFFFFFFFF.toInt()).forEach { (label,color) ->
                            FilterChip(selected=draft.shadowColor==color,
                                onClick={draft=draft.copy(shadowColor=color)},label={Text(label)})
                        }
                    }
                    Text("Espaçamento: ${draft.lineSpacingPercent}%")
                    Slider(value=draft.lineSpacingPercent.toFloat(),valueRange=100f..160f,steps=5,
                        onValueChange={draft=draft.copy(lineSpacingPercent=it.toInt())})
                    Text("Alinhamento")
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                        SubtitleStyles.alignments.forEach { (align,label) ->
                            FilterChip(selected=draft.textAlignment==align,onClick={
                                draft=draft.copy(textAlignment=align)
                            },label={Text(label)})
                        }
                    }
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        Text("Negrito",modifier=Modifier.weight(1f))
                        Switch(checked=draft.bold,onCheckedChange={draft=draft.copy(bold=it)})
                    }
                    Text("Posição horizontal: ${(draft.x*100).toInt()}%")
                    Slider(value=draft.x,valueRange=.1f.. .9f,
                        onValueChange={draft=draft.copy(x=it)})
                    Text("Posição vertical: ${(draft.y*100).toInt()}%")
                    Slider(value=draft.y,valueRange=.1f.. .9f,
                        onValueChange={draft=draft.copy(y=it)})
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                        listOf("Cima" to .15f,"Centro" to .5f,"Baixo" to .85f).forEach { (name,y) ->
                            FilterChip(selected=kotlin.math.abs(draft.y-y)<.02f,
                                onClick={draft=draft.copy(y=y)},label={Text(name)})
                        }
                    }
                }
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Checkbox(checked=applyAll,onCheckedChange={applyAll=it})
                    Text("Aplicar a todas as legendas",modifier=Modifier.weight(1f))
                }
                if(applyAll) {
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        Checkbox(checked=copyAppearance,onCheckedChange={copyAppearance=it})
                        Text("Aparência",fontSize=12.sp)
                        Checkbox(checked=copyPosition,onCheckedChange={copyPosition=it})
                        Text("Posição",fontSize=12.sp)
                    }
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        Checkbox(checked=copyAnimation,onCheckedChange={copyAnimation=it})
                        Text("Animação",fontSize=12.sp)
                    }
                    Text("Textos e horários nunca são copiados.",fontSize=11.sp,
                        color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if(!valid) Text("Informe um texto e horários válidos.",fontSize=11.sp,
                    color=MaterialTheme.colorScheme.error)
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp)) {
                    OutlinedButton(onClick=onDismiss,modifier=Modifier.weight(1f)) {Text("Cancelar")}
                    Button(onClick={if(applyAll) {confirmAll=true} else submit()},
                        modifier=Modifier.weight(1f),enabled=valid &&
                            (!applyAll || copyAppearance || copyPosition || copyAnimation)) {
                        Text("Salvar")
                    }
                }
            }
        }
    }
    if(confirmAll) AlertDialog(onDismissRequest={confirmAll=false},
        title={Text("Aplicar a todas?")},
        text={Text("Somente as propriedades escolhidas serão copiadas. Texto, horários e IDs das demais legendas permanecem inalterados.")},
        confirmButton={TextButton(onClick={confirmAll=false;submit()}) {Text("Aplicar")}},
        dismissButton={TextButton(onClick={confirmAll=false}) {Text("Cancelar")}})
}

@Composable
private fun ExportScreen(p:VideoProject,defaultQuality:Int,back:()->Unit) {
    val context=LocalContext.current
    val engine=remember { ExportEngine(context) }
    val previewPlayer=remember { diagnosticPlayer(context) }
    var previewPlaying by remember { mutableStateOf(false) }
    var exportClipIndex by remember(p.id) { mutableIntStateOf(0) }
    var exportPreviewMs by remember(p.id) { mutableLongStateOf(0L) }
    LaunchedEffect(previewPlayer, p.clips) {
        while(true) {
            if (p.clips.isNotEmpty()) {
                exportClipIndex=previewPlayer.currentMediaItemIndex.coerceIn(0,p.clips.lastIndex)
                exportPreviewMs=ExportTimingPolicy.projectTime(p.clips,exportClipIndex,
                    previewPlayer.currentPosition)
            }
            delay(100)
        }
    }
    val exportPreviewUri=p.clips.getOrNull(exportClipIndex)?.uri
    val exportPreviewAspect=remember(exportPreviewUri) {
        exportPreviewUri?.let { mediaAspect(context,it,false) } ?: 16f/9f
    }
    DisposableEffect(previewPlayer) { onDispose {previewPlayer.release()} }
    LaunchedEffect(p.clips) {
        previewPlayer.setMediaItems(p.clips.map { clip ->
            MediaItem.Builder().setUri(clip.uri).setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder().setStartPositionMs(clip.startMs)
                    .setEndPositionMs(clip.endMs).build()).build()
        })
        previewPlayer.prepare()
    }
    var name by remember(p.id) {mutableStateOf("UaiCortes_001.mp4")}
    var quality by remember {mutableIntStateOf(defaultQuality)}
    var working by remember {mutableStateOf(false)}
    var progress by remember {mutableIntStateOf(0)}
    var result by remember {mutableStateOf("")}
    DisposableEffect(engine) {onDispose{engine.cancel()} }
    LaunchedEffect(working) {while(working){progress=engine.progress();delay(250)}}
    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal=20.dp)) {
        Heading("Exportar vídeo",{if(!working)back() else {engine.cancel();working=false;result="Exportação cancelada"}},
            accessory={Icon(Icons.Default.Movie,tint=Blue,contentDescription=null)})
        Spacer(Modifier.height(8.dp))
        InlineVideoPreview(exportPreviewAspect) {
            AndroidView(factory={previewPlayer(it).apply { player=previewPlayer;useController=false }},
                modifier=Modifier.fillMaxSize(),update={it.player=previewPlayer})
            // Mesma camada visual do editor: renderize o PROJETO, não só o arquivo original.
            p.clips.getOrNull(exportClipIndex)?.let { clip ->
                val clipStart = p.clips.take(exportClipIndex).sumOf { it.durationMs }
                val local = (exportPreviewMs-clipStart).coerceIn(0L,clip.durationMs)
                val previous = EditFeatures.boundary(p,exportClipIndex-1)
                val next = EditFeatures.boundary(p,exportClipIndex)
                val fadeIn = EditFeatures.transitionMs(previous,
                    p.clips.getOrElse(exportClipIndex-1){clip}.durationMs,clip.durationMs)
                val fadeOut = EditFeatures.transitionMs(next,clip.durationMs,
                    p.clips.getOrElse(exportClipIndex+1){clip}.durationMs)
                val shade=1f-EditFeatures.fadeFraction(local,clip.durationMs,fadeIn,fadeOut)
                if (shade > .001f) {
                    val white = if(fadeIn > 0 && local < fadeIn)
                        previous?.kind=="Fade branco" else next?.kind=="Fade branco"
                    Box(Modifier.fillMaxSize().background((if(white)Color.White else Color.Black)
                        .copy(alpha=shade)))
                }
            }
            p.overlays.forEach { pip ->
                if(exportPreviewMs >= pip.startAtMs && exportPreviewMs < pip.startAtMs+pip.durationMs) {
                    key(pip.id) {
                        PipLayer(pip,exportPreviewMs-pip.startAtMs,previewPlayer.isPlaying,
                            editable=false) { }
                    }
                }
            }
            p.subtitles.firstOrNull { cue ->
                exportPreviewMs >= cue.startMs && exportPreviewMs < cue.endMs && cue.text.isNotBlank()
            }?.let { cue -> SubtitleOverlayOnFrame(cue,editable=false,playbackMs=exportPreviewMs) }
            Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) {
                IconButton(onClick={
                    previewPlaying=!previewPlaying
                    if(previewPlaying) previewPlayer.play() else previewPlayer.pause()
                }) {Icon(if(previewPlaying)Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                    "Reproduzir prévia",tint=Color.White,modifier=Modifier.size(52.dp))}
            }
        }
        if(p.subtitles.isNotEmpty() || p.overlays.isNotEmpty()) {
            Text("Prévia: legendas, PiPs e transições. Confira o MP4 final para validar áudio e efeitos.",
                style=MaterialTheme.typography.bodySmall,
                color=MaterialTheme.colorScheme.onSurfaceVariant,
                modifier=Modifier.padding(top=7.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text("Nome do arquivo",color=MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value=name,onValueChange={name=it},singleLine=true,modifier=Modifier.fillMaxWidth(),
            shape=RoundedCornerShape(15.dp),enabled=!working)
        Spacer(Modifier.height(22.dp));Text("Qualidade",fontSize=21.sp,fontWeight=FontWeight.Medium)
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            listOf(720,1080).forEach {q->
                Surface(onClick={quality=q},enabled=!working,modifier=Modifier.weight(1f).height(104.dp),
                    shape=RoundedCornerShape(18.dp),border=BorderStroke(if(q==quality)2.dp else 1.dp,
                        if(q==quality)Blue else MaterialTheme.colorScheme.outlineVariant)) {
                    Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
                        Text("${q}p",fontSize=19.sp)
                        Text(if(q==720)"Mais leve" else "Mais detalhes",fontSize=13.sp,
                            color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        if(working) {
            Text("Exportando: $progress%")
            LinearProgressIndicator(progress={progress/100f},modifier=Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick={engine.cancel();working=false;result="Exportação cancelada"},modifier=Modifier.fillMaxWidth()) {
                Text("Cancelar exportação")
            }
        } else {
            BlueButton("↓ Salvar vídeo",onClick={
                if(name.isNotBlank() && p.clips.isNotEmpty()) {
                    DiagnosticRecorder.action("export", "export_start_${quality}p")
                    result="";progress=0;working=true
                    runCatching {engine.start(p,quality,name,
                        onSuccess={uri->
                            working=false;progress=100
                            DiagnosticRecorder.action("export", "export_success")
                            savedName=name.trim().let { if(it.endsWith(".mp4",true)) it else "$it.mp4" }
                            savedVideo=uri
                        },
                        onError={error->working=false;DiagnosticRecorder.failure("export", error);result="Erro: $error"})}
                        .onFailure {working=false;DiagnosticRecorder.exception("export_start",it);result="Erro: ${it.message}"}
                }
            },modifier=Modifier.fillMaxWidth(),enabled=name.isNotBlank()&&p.clips.isNotEmpty())
        }
        if(result.isNotBlank()) Text(result,modifier=Modifier.padding(vertical=15.dp),
            color=if(result.startsWith("Erro"))MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(14.dp));Text("Formato MP4 · H.264/AAC · Salvamento local.",
            color=MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.bodySmall)
    }
    // Confirmação de TELA CHEIA apenas após a cópia completa no MediaStore.
    savedVideo?.let { savedUri ->
        Dialog(onDismissRequest={savedVideo=null},
            properties=DialogProperties(usePlatformDefaultWidth=false)) {
            Surface(Modifier.fillMaxSize(), color=MaterialTheme.colorScheme.background) {
                Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp),
                    horizontalAlignment=Alignment.CenterHorizontally,
                    verticalArrangement=Arrangement.Center) {
                    Icon(Icons.Default.CheckCircle,contentDescription=null,
                        modifier=Modifier.size(96.dp),tint=Green)
                    Spacer(Modifier.height(20.dp))
                    Text("Vídeo salvo!",style=MaterialTheme.typography.headlineMedium,
                        fontWeight=FontWeight.Bold,textAlign=TextAlign.Center)
                    Spacer(Modifier.height(10.dp))
                    Text(savedName,style=MaterialTheme.typography.titleMedium,
                        textAlign=TextAlign.Center,maxLines=3,overflow=TextOverflow.Ellipsis)
                    Spacer(Modifier.height(8.dp))
                    Text("Salvo na galeria em Filmes/UaiCortes",textAlign=TextAlign.Center,
                        color=MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(32.dp))
                    BlueButton("▶ Abrir vídeo",onClick={
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(savedUri,"video/mp4")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            })
                        }.onFailure { result="Não há aplicativo disponível para abrir o vídeo.";savedVideo=null }
                    },modifier=Modifier.fillMaxWidth())
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(onClick={
                        runCatching {
                            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                                type="video/mp4"
                                putExtra(Intent.EXTRA_STREAM,savedUri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            },"Compartilhar vídeo"))
                        }.onFailure { result="Não foi possível abrir o compartilhamento.";savedVideo=null }
                    },modifier=Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Share,contentDescription=null)
                        Spacer(Modifier.width(8.dp))
                        Text("Compartilhar")
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(onClick={savedVideo=null;back()},modifier=Modifier.fillMaxWidth()) {
                        Text("Voltar ao editor")
                    }
                    TextButton(onClick={savedVideo=null}) { Text("Fechar") }
                }
            }
        }
    }
}

/** Calcula proporção sem carregar um bitmap integral em memória. */
private fun mediaAspect(context: android.content.Context, uri: String, isImage: Boolean): Float {
    return runCatching {
        if (isImage) {
            val options = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(Uri.parse(uri))?.use {
                android.graphics.BitmapFactory.decodeStream(it, null, options)
            }
            if (options.outWidth > 0 && options.outHeight > 0)
                options.outWidth.toFloat() / options.outHeight else 16f / 9f
        } else {
            val r = android.media.MediaMetadataRetriever()
            try {
                r.setDataSource(context, Uri.parse(uri))
                val w = r.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toFloatOrNull() ?: 16f
                val h = r.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toFloatOrNull() ?: 9f
                val rotation = r.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
                if (rotation % 180 != 0) h / w.coerceAtLeast(1f) else w / h.coerceAtLeast(1f)
            } finally { r.release() }
        }
    }.getOrDefault(16f / 9f).coerceIn(0.1f, 10f)
}

private fun projectPosition(player: ExoPlayer, clips: List<Clip>, total: Long): Long {
    if (clips.isEmpty()) return 0L
    val index = player.currentMediaItemIndex.coerceIn(0, clips.lastIndex)
    return (clips.take(index).sumOf { it.durationMs } + player.currentPosition).coerceIn(0L, total)
}

private fun seekProjectPosition(player: ExoPlayer, clips: List<Clip>, position: Long) {
    if (clips.isEmpty()) return
    var remaining = position.coerceIn(0, clips.sumOf { it.durationMs })
    clips.forEachIndexed { index, clip ->
        if (remaining <= clip.durationMs || index == clips.lastIndex) {
            player.seekTo(index, remaining.coerceIn(0L, clip.durationMs))
            return
        }
        remaining -= clip.durationMs
    }
}

/**
 * Centraliza as chamadas Media3 @UnstableApi fora das lambdas AndroidView do Compose.
 * Android Lint não considera o opt-in do composable externo suficiente em certas lambdas.
 * A prévia permanece em TextureView para compor PiPs e guias por cima do vídeo.
 */
@OptIn(UnstableApi::class)
private fun previewPlayer(
    context: android.content.Context,
    transparentShutter: Boolean = false,
): PlayerView =
    (android.view.LayoutInflater.from(context)
        .inflate(R.layout.player_preview_texture, null, false) as PlayerView).apply {
        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
        setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
        if (transparentShutter) setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
    }
