package br.com.uaicortes

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerId
import kotlin.math.abs
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.*
import androidx.compose.ui.geometry.Offset
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

internal val Blue = Color(0xFF0866FF)
internal val Purple = Color(0xFF6954B5)
internal val Green = Color(0xFF16866E)
internal val Ink = Color(0xFF151515)
internal val LightBg = Color(0xFFF7F8FA)

internal data class OverlapConfirmation(
    val project: VideoProject, val requested: List<Overlay>,
    val preferredId: String, val conflicts: Set<Pair<String, String>>
)
internal data class SequentialConfirmation(val uri: Uri, val isImage: Boolean, val mediaMs: Long)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        DiagnosticRecorder.initialize(applicationContext)
        setContent { UaiApp() }
    }
}

/** Observa falhas de reprodução em todos os players sem capturar caminhos de mídia. */
internal fun diagnosticPlayer(context: android.content.Context): ExoPlayer =
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
    val projectSession: EditorProjectViewModel = viewModel()
    val store = projectSession.store
    val mediaController = remember(context) { MediaImportController(context.applicationContext) }
    val importScope = rememberCoroutineScope()
    val mediaTasks = remember(mediaController) { MediaImportTasks(mediaController) }
    var activeMediaTask by remember { mutableStateOf<MediaTaskKind?>(null) }
    val mediaTaskCoordinator = remember(importScope) {
        MediaTaskCoordinator(importScope,
            onActiveChange = { activeMediaTask = it },
            onError = { error ->
                DiagnosticRecorder.exception("media_import", error)
                android.widget.Toast.makeText(context, "Não foi possível importar esta mídia",
                    android.widget.Toast.LENGTH_LONG).show()
            })
    }
    DisposableEffect(mediaTaskCoordinator) {
        onDispose { mediaTaskCoordinator.cancel() }
    }
    val animationDraftStore = remember { ImageAnimationDraftStore(context.applicationContext) }
    var diagnosticsEnabled by remember { mutableStateOf(DiagnosticRecorder.isEnabled()) }
    var theme by remember { mutableStateOf(store.theme()) }
    val workspace = rememberSaveable(saver = EditorWorkspaceState.Saver) { EditorWorkspaceState() }
    var screen by workspace::screen
    // O ViewModel mantém o cadastro durante rotação; SavedStateHandle preserva apenas o ID.
    val activeProjectId = projectSession.activeProjectId
    val projects = projectSession.projects
    val project = projectSession.project
    var selectedClip by workspace::selectedClip
    var selectedAudioTrackId by workspace::selectedAudioTrackId
    var selectedSubtitleId by workspace::selectedSubtitleId
    var positionMs by workspace::positionMs
    var pendingPicker by rememberSaveable { mutableStateOf("") }
    var importedAudioSourceVideo by rememberSaveable { mutableStateOf<Uri?>(null) }
    var mediaOrigin by rememberSaveable { mutableStateOf("editor") }
    var selectedOverlayId by workspace::selectedOverlayId
    var mediaAction by rememberSaveable { mutableStateOf("add") }
    var previous by workspace::previous
    val animationImages = rememberSaveable(saver = listSaver<androidx.compose.runtime.snapshots.SnapshotStateList<Uri>, String>(
        save = { images -> images.take(ImageAnimationPlan.MAX_IMAGES).map(Uri::toString) },
        restore = { values -> mutableStateListOf<Uri>().also { list ->
            list.addAll(values.filter(String::isNotBlank).map(Uri::parse).distinct())
        } }
    )) { mutableStateListOf<Uri>() }
    var animationDestination by rememberSaveable { mutableStateOf("new") }
    // Sanitiza seleções restauradas, inclusive se o projeto tiver sido excluído.
    LaunchedEffect(project) {
        if (project == null && animationDestination == "append") animationDestination = "new"
        workspace.reconcile(project, mediaOrigin, mediaAction)
    }
    var pendingOverlap by remember { mutableStateOf<OverlapConfirmation?>(null) }
    var pendingSequential by remember { mutableStateOf<SequentialConfirmation?>(null) }
    var pendingProjectDelete by remember { mutableStateOf<VideoProject?>(null) }
    val dark = when (theme) {
        "Claro" -> false
        "Escuro", "AMOLED" -> true
        else -> isSystemInDarkTheme()
    }
    val palette = if (dark) darkColorScheme(primary = Color(0xFF72A5FF), background = if(theme=="AMOLED") Color.Black else Ink,
        surface = if(theme=="AMOLED") Color.Black else Color(0xFF202124))
    else lightColorScheme(primary = Blue, background = LightBg, surface = Color.White)
    fun write(p: VideoProject) {
        projectSession.save(p)
        DiagnosticRecorder.action(screen, "project_saved")
    }
    fun openProject(id: String) {
        val opened = projectSession.open(id)
        if (opened != null) {
            selectedClip = TrimTimelineEdit.resumeClipId(opened)
            screen = "editor"
        } else {
            workspace.reconcile(null)
            screen = "home"
        }
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
        return when (val result = OverlayOperations.apply(current, requested, preferredId,
                canvasAspect, { mediaAspect(context, it.uri, it.isImage) }, decision)) {
            is OverlayUpdate.Confirm -> {
                pendingOverlap = OverlapConfirmation(current, requested, preferredId.orEmpty(), result.conflicts)
                false
            }
            is OverlayUpdate.Rejected -> {
                android.widget.Toast.makeText(context, result.reason, android.widget.Toast.LENGTH_LONG).show()
                false
            }
            is OverlayUpdate.Applied -> {
                write(result.project)
                true
            }
        }
    }
    fun insertOverlay(uri: Uri, isImage: Boolean, mediaMs: Long, allowTimeOverlap: Boolean = false) {
        val current = project ?: return
        when (val insertion = OverlayOperations.insert(current, uri.toString(), isImage,
                mediaMs, selectedOverlayId, mediaAction == "replace", positionMs, allowTimeOverlap)) {
            OverlayInsertion.LimitReached -> {
                android.widget.Toast.makeText(context, "Limite de quatro PiPs atingido",
                    android.widget.Toast.LENGTH_SHORT).show()
            }
            OverlayInsertion.ConfirmSequential -> {
                pendingSequential = SequentialConfirmation(uri, isImage, mediaMs)
            }
            is OverlayInsertion.Proposed -> {
                if (saveOverlays(current, insertion.requested, insertion.selectedId))
                    selectedOverlayId = insertion.selectedId
            }
        }
        screen = "pip"
    }
    var selectedBoundary by workspace::selectedBoundary
    /** Importações são canceláveis e vinculadas ao contexto que iniciou a leitura. */
    fun startMediaTask(kind: MediaTaskKind, origin: MediaTaskOrigin, operation: suspend () -> Unit) {
        if (!mediaTaskCoordinator.start(kind, origin, operation)) {
            android.widget.Toast.makeText(context, "Aguarde a importação atual terminar",
                android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    LaunchedEffect(activeProjectId, screen, mediaAction, mediaTaskCoordinator) {
        mediaTaskCoordinator.cancelIfDestinationChanged(activeProjectId, screen, mediaAction)
    }
    fun importVideoClips(uris: List<Uri>, persistDocumentAccess: Boolean) {
        if (uris.isEmpty()) return
        val request = VideoImportRequest(project?.id.orEmpty(), pendingPicker == "new")
        val origin = MediaTaskOrigin(project?.id.orEmpty(), screen, mediaAction)
        startMediaTask(MediaTaskKind.VIDEO, origin) {
            val imported = mediaTasks.videos(uris, persistDocumentAccess)
            if (!request.stillTargets(project?.id.orEmpty()) ||
                !origin.matches(project?.id.orEmpty(), screen, mediaAction)) {
                android.widget.Toast.makeText(context, "O destino mudou durante a importação. Selecione novamente.",
                    android.widget.Toast.LENGTH_LONG).show()
                return@startMediaTask
            }
            if (imported.clips.isEmpty()) {
                android.widget.Toast.makeText(context, "Não foi possível abrir os vídeos selecionados",
                    android.widget.Toast.LENGTH_LONG).show()
                return@startMediaTask
            }
            val updated = projectSession.append(project, imported.clips, createNew = request.createNew)
            write(updated)
            if (selectedClip.isBlank()) selectedClip = imported.clips.first().id
            screen = "editor"
            if (imported.rejected > 0) android.widget.Toast.makeText(context,
                "Alguns vídeos não puderam ser importados", android.widget.Toast.LENGTH_LONG).show()
        }
    }
    fun openGallery(origin: String, action: String) {
        mediaOrigin = origin
        mediaAction = action
        screen = "media"
    }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        uris.forEach(mediaController::persistReadAccess)
        val images = MediaImportRules.images(animationImages.map(Uri::toString), uris.map(Uri::toString))
        animationImages.clear()
        animationImages.addAll(images.map(Uri::parse))
        if (uris.isNotEmpty()) screen = "animate"
    }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        importVideoClips(uris, persistDocumentAccess = true)
    }
    val externalVideoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            mediaController.persistReadAccess(uri)
            importedAudioSourceVideo = uri
            screen = "audio"
        }
    }
    val overlayPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val origin = MediaTaskOrigin(project?.id.orEmpty(), screen, mediaAction)
            val initialProject = project
            val insertionCursor = positionMs
            val selectedId = selectedOverlayId
            startMediaTask(MediaTaskKind.OVERLAY, origin) {
                val remaining = ((initialProject?.durationMs ?: 0L) - insertionCursor).coerceAtLeast(0L)
                val loaded = mediaTasks.overlay(uri, remaining)
                if (!origin.matches(project?.id.orEmpty(), screen, mediaAction) ||
                    project !== initialProject || selectedOverlayId != selectedId || positionMs != insertionCursor) {
                    android.widget.Toast.makeText(context, "O editor mudou. Selecione o PiP novamente.",
                        android.widget.Toast.LENGTH_LONG).show()
                    return@startMediaTask
                }
                if (loaded == null) {
                    android.widget.Toast.makeText(context, "Não foi possível obter a duração desta mídia",
                        android.widget.Toast.LENGTH_LONG).show()
                    return@startMediaTask
                }
                insertOverlay(uri, loaded.isImage, loaded.durationMs)
            }
        }
    }
    val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val initialProject = project
            val origin = MediaTaskOrigin(project?.id.orEmpty(), screen, mediaAction)
            val insertionCursor = positionMs
            startMediaTask(MediaTaskKind.AUDIO, origin) {
                val track = mediaTasks.audio(uri, initialProject?.durationMs ?: 0L, insertionCursor)
                if (!origin.matches(project?.id.orEmpty(), screen, mediaAction) || project !== initialProject) {
                    android.widget.Toast.makeText(context, "O projeto mudou. Selecione o áudio novamente.",
                        android.widget.Toast.LENGTH_LONG).show()
                    return@startMediaTask
                }
                if (track != null && initialProject != null) {
                    write(initialProject.copy(audioTracks = initialProject.audioTracks + track))
                } else {
                    android.widget.Toast.makeText(context, "Não foi possível inserir o áudio neste projeto",
                        android.widget.Toast.LENGTH_LONG).show()
                }
                screen = "audio"
            }
        }
    }
    fun back() {
        DiagnosticRecorder.action(screen, "back")
        screen = EditorNavigation.back(screen, previous, mediaOrigin, animationDestination)
    }
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
                "home" -> MainHome(projects, onCreate = { pendingPicker = "new"; selectedClip = ""; openGallery("home", "new") },
                    onAnimate = {
                        animationDestination = "new"
                        animationImages.clear()
                        animationImages.addAll(animationDraftStore.load()?.images.orEmpty())
                        screen = "animate"
                    },
                    onOpen = { openProject(it.id) },
                    onDelete = { pendingProjectDelete = it },
                    onSettings = { screen = "settings" }, onTab = { screen = it })
                "animate" -> AnimateImagesScreen(animationImages.toList(),
                    onChoose = { openGallery("animate","animate_images") },
                    onBack = { screen = if(animationDestination=="append") "editor" else "home" },
                    onOrderChanged = { reordered -> animationImages.clear();animationImages.addAll(reordered) },
                    onSaveDraft = { chosen, opts -> animationDraftStore.save(chosen,opts) },
                    onUseInEditor = { uri ->
                        val duration = mediaDuration(context,uri)
                        if (duration > 0L) {
                            val clip = Clip(uri=uri.toString(),title="Vídeo animado",sourceDurationMs=duration)
                            val target=if(animationDestination=="append") project ?: VideoProject()
                                else VideoProject()
                            write(target.copy(clips=target.clips+clip))
                            selectedClip=clip.id; screen="editor"
                        } else android.widget.Toast.makeText(context,"MP4 ainda não está disponível",android.widget.Toast.LENGTH_LONG).show()
                    })
                "projects" -> ProjectsScreen(projects, { back() }, { openProject(it.id) },
                    { pendingProjectDelete = it })
                "settings" -> SettingsScreen({ back() }) { screen = it }
                "diagnostics" -> DiagnosticScreen(::back, onEnabledChanged = { diagnosticsEnabled = it })
                "general" -> GeneralScreen(store, ::back)
                "appearance" -> AppearanceScreen(theme, { theme = it; store.setTheme(it) }, ::back)
                "about" -> AboutScreen(::back)
                "editor" -> project?.let { p -> EditorScreen(p, selectedClip, positionMs,
                    onSelected = { selectedClip = it }, onPosition = { positionMs = it }, onBack = ::back,
                    onEdit = {
                        if (it == "animate") {
                            animationDestination = "append"
                            animationImages.clear()
                            animationImages.addAll(animationDraftStore.load()?.images.orEmpty())
                            screen = "animate"
                        } else navTo(it)
                    }, onUpdate = { write(it) },
                    onAddVideo = { pendingPicker = "append"; openGallery("editor", "append") },
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
                "watermark" -> project?.let { current -> TextWatermarkScreen(current, ::back) { changed ->
                    write(changed)
                    screen = "editor"
                } }
                "trim" -> project?.let { TrimScreen(it, selectedClip, positionMs, ::back, { write(it) },
                    { selectedClip = it }, onSaveHome = { saved -> write(saved); screen = "home" }) }
                "audio" -> project?.let { AudioScreen(it, ::back, { write(it) },
                    { audioPicker.launch(arrayOf("audio/*")) }, selectedAudioTrackId,
                    onSelectionConsumed = { selectedAudioTrackId = null },
                    onChooseExternalVideo = { openGallery("audio", "audio_extract") },
                    externalVideoUri = importedAudioSourceVideo,
                    onExternalVideoConsumed = { importedAudioSourceVideo = null }) }
                "pip" -> project?.let { current -> PipScreen(current, selectedOverlayId,
                    onSelect = { selectedOverlayId = it }, back = ::back,
                    onChange = { changed ->
                        saveOverlays(current, current.overlays.map {
                            if (it.id == changed.id) changed else it
                        }, changed.id)
                    },
                    onRemove = { id ->
                        val (updated, nextSelected) = OverlayOperations.remove(current, id)
                        write(updated)
                        selectedOverlayId = nextSelected
                    },
                    choose = { replace ->
                        if (!replace && current.overlays.size >= OverlayPlacement.MAX_PIPS) {
                            android.widget.Toast.makeText(context, "Limite de quatro PiPs atingido",
                                android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            openGallery("pip", if (replace) "replace" else "add")
                        }
                    }) }
                "media" -> DeviceMediaBrowser(::back, onSelect = { chosen ->
                    when (mediaAction) {
                        "animate_images" -> {
                            val images = MediaImportRules.images(animationImages.map(Uri::toString),
                                chosen.filter { it.isImage }.map { it.uri.toString() })
                            animationImages.clear()
                            animationImages.addAll(images.map(Uri::parse))
                            if (animationImages.size == ImageAnimationPlan.MAX_IMAGES)
                                android.widget.Toast.makeText(context,"Limite de 80 imagens por animação",
                                    android.widget.Toast.LENGTH_LONG).show()
                            screen="animate"
                        }
                        "new", "append" -> importVideoClips(chosen.map { it.uri }, persistDocumentAccess = false)
                        "audio_extract" -> {
                            chosen.firstOrNull()?.let { importedAudioSourceVideo = it.uri }
                            screen = "audio"
                        }
                        else -> chosen.firstOrNull()?.let { item ->
                            val videoMs = if (item.isImage) 5000L else
                                item.durationMs.takeIf { it > 0 } ?: mediaDuration(context, item.uri)
                            insertOverlay(item.uri, item.isImage, videoMs)
                        }
                    }
                }, openDocuments = {
                    when (mediaAction) {
                        "animate_images" -> imagePicker.launch(arrayOf("image/*"))
                        "new", "append" -> videoPicker.launch(arrayOf("video/*"))
                        "audio_extract" -> externalVideoPicker.launch(arrayOf("video/*"))
                        else -> overlayPicker.launch(arrayOf("image/*", "video/*"))
                    }
                }, scope = when (mediaAction) {
                    "animate_images" -> GalleryScope.IMAGES_ONLY
                    "new", "append", "audio_extract" -> GalleryScope.VIDEOS
                    else -> GalleryScope.IMAGES_AND_VIDEOS
                }, allowMultiple = mediaAction == "new" || mediaAction == "append" || mediaAction == "animate_images")
                "transition" -> project?.let { TransitionScreen(it, ::back, { write(it) }, selectedBoundary) }
                "autosubtitle" -> project?.let { current -> AutoSubtitleScreen(current, ::back, onApply = { cues, replace ->
                    val existing = if (replace) emptyList() else current.subtitles
                    write(current.copy(subtitles = existing + cues))
                    selectedSubtitleId = cues.firstOrNull()?.id
                    screen = "subtitles"
                }) }
                "subtitles" -> project?.let { SubtitleScreen(it, ::back, { write(it) }, selectedSubtitleId,
                    onSaveHome = { write(it); screen = "home" },
                    onAutoSubtitles = { screen = "autosubtitle" }) }
                "export" -> project?.let { ExportScreen(it, store.quality(), ::back) }
            }
        }
        if (activeMediaTask != null) {
            AlertDialog(onDismissRequest = {},
                title = { Text("Importando mídia") },
                text = { Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp))
                    Text("Lendo duração e informações dos arquivos locais…")
                } },
                confirmButton = { TextButton(onClick = { mediaTaskCoordinator.cancel() }) {
                    Text("Cancelar importação")
                } })
        }
        pendingProjectDelete?.let { target ->
            AlertDialog(onDismissRequest = { pendingProjectDelete = null },
                icon = { Icon(Icons.Default.DeleteOutline, null) },
                title = { Text("Excluir projeto?") },
                text = { Text("O projeto \"${target.title}\" e suas edições salvas serão removidos. Os vídeos e fotos originais não serão apagados. Esta ação não pode ser desfeita.") },
                confirmButton = { TextButton(onClick = {
                    val wasActive = project?.id == target.id
                    projectSession.delete(target.id)
                    if (wasActive) workspace.reconcile(null)
                    pendingProjectDelete = null
                }) { Text("Excluir projeto", color = MaterialTheme.colorScheme.error) } },
                dismissButton = { TextButton(onClick = { pendingProjectDelete = null }) { Text("Cancelar") } })
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

