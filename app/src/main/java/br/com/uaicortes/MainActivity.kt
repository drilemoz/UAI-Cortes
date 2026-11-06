package br.com.uaicortes
import android.net.Uri
import android.os.Bundle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.*
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
internal val Blue = Color(0xFF0866FF); internal val Purple = Color(0xFF6954B5)
internal val Green = Color(0xFF16866E)
internal val Ink = Color(0xFF151515); internal val LightBg = Color(0xFFF7F8FA)
internal data class OverlapConfirmation(val project: VideoProject, val requested: List<Overlay>,
    val preferredId: String, val conflicts: Set<Pair<String, String>>)
internal data class SequentialConfirmation(val uri: Uri, val isImage: Boolean, val mediaMs: Long)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        DiagnosticRecorder.initialize(applicationContext)
        setContent { UaiApp() }
    }
    @Deprecated("Callback do SDK Meta")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data); SocialMetaAuth.onActivityResult(requestCode, resultCode, data)
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
@Composable fun UaiApp() {
    val context = LocalContext.current
    val projectSession: EditorProjectViewModel = viewModel()
    val store = projectSession.store
    val backupStore = remember(context) { ProjectBackupStore(context.applicationContext) }
    val draftRecoveryStore = remember(context) { ProjectDraftRecoveryStore(context.applicationContext) }
    val whatsNewSeen = remember(context) { WhatsNewSeenStore(context.applicationContext) }
    val githubUpdates = remember(context) { GitHubUpdateManager(context.applicationContext) }
    val mediaController = remember(context) { MediaImportController(context.applicationContext) }
    val importScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, githubUpdates, importScope) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) importScope.launch { githubUpdates.checkOnForeground() }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
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
    val workspace = rememberSaveable(saver = EditorWorkspaceState.Saver) {
        EditorWorkspaceState().apply {
            if (whatsNewSeen.shouldOpen()) {
                screen = "whats_new"
                previous = "home"
            }
        }
    }
    var screen by workspace::screen
    var pendingSocialVideoUri by rememberSaveable { mutableStateOf("") }; var pendingSocialVideoName by rememberSaveable { mutableStateOf("") }
    var pendingSocialDescription by rememberSaveable { mutableStateOf("") }; var pendingSocialHashtags by rememberSaveable { mutableStateOf("") }
    val activeProjectId = projectSession.activeProjectId
    val projects = projectSession.projects
    val project = projectSession.project
    val narrationDraft = remember(project?.id) { GeminiNarrationDraft() }
    var resumeVideoPreview by remember { mutableStateOf(false) }
    // Nunca criar backups históricos a partir de alterações ainda não confirmadas.
    ProjectBackupLifecycle(backupStore, if (projectSession.isEditingDraft) "" else activeProjectId, screen)
    var interruptedDraft by remember { mutableStateOf(draftRecoveryStore.load(projects)) }
    var invalidRecovery by remember { mutableStateOf(interruptedDraft == null && draftRecoveryStore.hasStoredDraft()) }
    LaunchedEffect(interruptedDraft, invalidRecovery) {
        if ((interruptedDraft != null || invalidRecovery) && screen != "home") screen = "home"
    }
    ProjectDraftRecoveryLifecycle(draftRecoveryStore, project,
        projectSession.draftBaseline, projectSession.hasPendingChanges)
    ProjectMediaAlert(if (screen == "editor") project else null)
    var selectedClip by workspace::selectedClip
    var selectedAudioTrackId by workspace::selectedAudioTrackId
    var selectedVoiceCueId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedSubtitleId by workspace::selectedSubtitleId
    var positionMs by workspace::positionMs
    var pendingPicker by rememberSaveable { mutableStateOf("") }
    var incomingMainImage by remember { mutableStateOf<Uri?>(null) }
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
    LaunchedEffect(project) {
        if (project == null && animationDestination == "append") animationDestination = "new"
        workspace.reconcile(project, mediaOrigin, mediaAction)
    }
    var pendingOverlap by remember { mutableStateOf<OverlapConfirmation?>(null) }
    var pendingSequential by remember { mutableStateOf<SequentialConfirmation?>(null) }
    var pendingProjectDelete by remember { mutableStateOf<VideoProject?>(null) }
    var pendingDeleteAll by remember { mutableStateOf(false) }
    var pendingExitEditor by remember { mutableStateOf(false) }
    var pendingNewProjectName by remember { mutableStateOf(false) }
    var newProjectName by rememberSaveable { mutableStateOf("Meu vídeo") }
    var catalogAlertDismissed by rememberSaveable { mutableStateOf(false) }
    val dark = when (theme) {
        "Claro" -> false
        "Escuro", "AMOLED" -> true
        else -> isSystemInDarkTheme()
    }
    val palette = if (dark) darkColorScheme(primary = Color(0xFF72A5FF), background = if(theme=="AMOLED") Color.Black else Ink,
        surface = if(theme=="AMOLED") Color.Black else Color(0xFF202124))
    else lightColorScheme(primary = Blue, background = LightBg, surface = Color.White)
    fun openSocialPublish(uri: Uri?, name: String = "", draft: VideoPublicationDraft? = null) {
        pendingSocialVideoUri = uri?.toString().orEmpty(); pendingSocialVideoName = name
        pendingSocialDescription = draft?.description.orEmpty(); pendingSocialHashtags = draft?.hashtags.orEmpty()
        previous = screen; screen = "social_publish"
    }
    fun write(p: VideoProject): Boolean = try {
        projectSession.stage(ClipAudioBindings.pruneDeleted(p))
        DiagnosticRecorder.action(screen, "project_draft_changed")
        true
    } catch (failure: Exception) {
        DiagnosticRecorder.exception("project_draft_failure", failure)
        android.widget.Toast.makeText(context,
            "Não foi possível aplicar a alteração; o projeto foi preservado.",
            android.widget.Toast.LENGTH_LONG).show()
        false
    }
    fun leaveEditor(decision: String) {
        val completed = runCatching {
            val savedProject = when (decision) {
                "save" -> projectSession.commit()
                "copy" -> projectSession.commitCopy()
                "discard" -> null
                else -> return
            }
            // Snapshot histórico somente depois do commit do cadastro.
            if (savedProject != null && backupStore.preferences.enabled)
                importScope.launch(Dispatchers.IO) {
                    runCatching { backupStore.snapshot(savedProject.id) }
                        .onFailure { DiagnosticRecorder.exception("backup_snapshot", it) }
                }
            draftRecoveryStore.clear()
            projectSession.finishWithoutSaving()
            workspace.reconcile(null)
            pendingExitEditor = false
            screen = "home"
            DiagnosticRecorder.action("editor", "project_${decision}")
        }
        completed.onFailure { failure ->
            DiagnosticRecorder.exception("project_commit_failure", failure)
            android.widget.Toast.makeText(context,
                "Não foi possível concluir. A edição permanece aberta; tente novamente.",
                android.widget.Toast.LENGTH_LONG).show()
        }
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
        val canvasAspect = current.clips.firstOrNull()?.let { mediaAspect(context, it.uri, it.isImage) } ?: 16f / 9f
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
            val base = projectSession.append(project, imported.clips, createNew = request.createNew)
            val updated = if (request.createNew) base.copy(title = ProjectNaming.title(newProjectName)) else base
            if (request.createNew) projectSession.beginNew(ClipAudioBindings.pruneDeleted(updated))
            else if (!write(updated)) return@startMediaTask
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
    val mainImageDocumentPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null && mediaAction == "image_insert") {
            mediaController.persistReadAccess(uri)
            incomingMainImage = uri
            screen = "editor"
        }
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
        if (screen == "whats_new" && !whatsNewSeen.markViewed()) {
            android.widget.Toast.makeText(context, "Não foi possível registrar a leitura das novidades",
                android.widget.Toast.LENGTH_SHORT).show()
        }
        DiagnosticRecorder.action(screen, "back")
        val destination = EditorNavigation.back(screen, previous, mediaOrigin, animationDestination)
        if (screen == "editor" && destination == "home") {
            if (projectSession.hasPendingChanges) {
                pendingExitEditor = true
                return
            }
            projectSession.finishWithoutSaving()
            draftRecoveryStore.clear()
            workspace.reconcile(null)
        }
        screen = destination
    }
    BackHandler(screen != "home") { back() }
    LaunchedEffect(screen, diagnosticsEnabled) { DiagnosticRecorder.screen(screen) }
    MaterialTheme(colorScheme = palette) {
    val mainImagePick = MainImageInsertionUi(project, screen, mediaAction, selectedClip,
        mediaTaskCoordinator, incomingMainImage, { incomingMainImage = null },
        { openGallery("editor", "image_insert") }) { changed, clip, index ->
        write(changed).also { saved -> if (saved) { positionMs = changed.clips.take(index).sumOf { it.durationMs }; selectedClip = clip.id } } }
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
                "home" -> MainHome(projects, onCreate = { newProjectName = "Meu vídeo"; pendingNewProjectName = true },
                    onAnimate = {
                        animationDestination = "new"
                        animationImages.clear()
                        animationImages.addAll(animationDraftStore.load()?.images.orEmpty())
                        screen = "animate"
                    },
                    onOpen = { openProject(it.id) },
                    onDelete = { pendingProjectDelete = it },
                    onDeleteAll = { pendingDeleteAll = true },
                    onSettings = { previous = "home"; screen = "settings" },
                    onTab = { if (it == "settings") previous = "home"; screen = it },
                    onFlow = { navTo("flow_workspace") })
                "flow_workspace" -> FlowWorkspaceScreen(::back)
                "animate" -> AnimateImagesScreen(animationImages.toList(),
                    onChoose = { openGallery("animate","animate_images") },
                    onBack = ::back,
                    onOrderChanged = { reordered -> animationImages.clear();animationImages.addAll(reordered) },
                    onSaveDraft = { chosen, opts -> animationDraftStore.save(chosen,opts) },
                    onSocialPublish = { uri, name, draft -> openSocialPublish(uri, name.ifBlank { "Vídeo animado.mp4" }, draft) },
                    onUseInEditor = { uri, publicationDraft ->
                        val duration = mediaDuration(context, uri)
                        if (duration > 0L) {
                            val clip = Clip(uri=uri.toString(),title="Vídeo animado",sourceDurationMs=duration)
                            val target = if(animationDestination=="append") project ?: VideoProject()
                                else VideoProject()
                            val edited = target.copy(clips=target.clips+clip,
                                publicationDraft=publicationDraft.takeUnless {
                                    it.description.isBlank() && it.hashtags.isBlank()
                                } ?: target.publicationDraft)
                            val accepted = if (animationDestination == "append") write(edited)
                                else { projectSession.beginNew(edited); true }
                            if (accepted) {
                                selectedClip=clip.id
                                screen="editor"
                                true
                            } else false
                        } else {
                            android.widget.Toast.makeText(context,"MP4 ainda não está disponível",
                                android.widget.Toast.LENGTH_LONG).show()
                            false
                        }
                    })
                "projects" -> ProjectsScreen(projects, { back() }, { openProject(it.id) },
                    { pendingProjectDelete = it }, onTab = { screen = it })
                "social" -> SocialDashboardScreen({ screen = it }, { openSocialPublish(null) }, { previous = "social"; screen = "api_center" })
                "social_publish" -> SocialPublishScreen(pendingSocialVideoUri.takeIf(String::isNotBlank)?.let(Uri::parse),
                    pendingSocialVideoName, VideoPublicationDraft(pendingSocialDescription, pendingSocialHashtags), { screen = "social" })
                "settings" -> SettingsScreen(::back, { previous = "settings"; screen = it }, { screen = it })
                "api_center" -> SocialApiCenterScreen(::back, { previous = "api_center"; screen = "gemini_settings" })
                "updates" -> GitHubUpdatesScreen(githubUpdates, ::back)
                "whats_new" -> WhatsNewScreen(::back)
                "diagnostics" -> DiagnosticScreen(::back, onEnabledChanged = { diagnosticsEnabled = it })
                "general" -> GeneralScreen(store, ::back)
                "backup_settings" -> ProjectBackupSettingsScreen(backupStore, projects,
                    onRestore = { restored -> runCatching { projectSession.persistRestored(restored); true }
                        .getOrElse { DiagnosticRecorder.exception("project_restore_failure", it); false } }, back = ::back)
                "speech_model" -> SpeechModelSettingsScreen(::back)
                "gemini_settings" -> GeminiSettingsScreen(::back)
                "appearance" -> AppearanceScreen(theme, { theme = it; store.setTheme(it) }, ::back)
                "about" -> AboutScreen(::back,
                    onShowWhatsNew = { previous = "about"; screen = "whats_new" },
                    onUpdates = { previous = "about"; screen = "updates" })
                "media_repair" -> project?.let { p -> ProjectMediaRepairScreen(p, ::back) { changed ->
                    if (projectSession.project != p) false else write(changed)
                } }
                "editor" -> project?.let { p -> EditorScreen(p, selectedClip, positionMs,
                    onSelected = { selectedClip = it }, onPosition = { positionMs = it }, onBack = ::back,
                    onEdit = {
                        if (it == "animate") {
                            animationDestination = "append"
                            animationImages.clear()
                            animationImages.addAll(animationDraftStore.load()?.images.orEmpty())
                            screen = "animate"
                        } else {
                            if (it == "voices") selectedVoiceCueId = null
                            navTo(it)
                        }
                    }, onUpdate = { write(it) },
                    onAddVideo = { pendingPicker = "append"; openGallery("editor", "append") }, onAddImage = mainImagePick,
                    onPiP = {
                        selectedOverlayId = project?.overlays?.firstOrNull()?.id.orEmpty()
                        screen = "pip"
                    },
                    onAudio = { audioPicker.launch(arrayOf("audio/*")) },
                    onSelectAudio = { id ->
                        val bound = project?.audioTracks?.firstOrNull { it.id == id }?.clipId
                        val track = project?.audioTracks?.firstOrNull { it.id == id }
                        if (track?.kind == VoiceIaPolicy.KIND) {
                            selectedVoiceCueId = track.subtitleId
                            navTo("voices")
                        } else if (bound != null) { selectedClip = bound; navTo("clip_audio") }
                        else { selectedAudioTrackId = id; navTo("audio") }
                    },
                    onSelectSubtitle = { id -> selectedSubtitleId = id; navTo("subtitles") },
                    onSelectOverlay = { selectedOverlayId = it },
                    onTransition = { index -> selectedBoundary = index; screen = "transition" },
                    onSaveHome = { if (write(p)) leaveEditor("save") },
                    onClipAudio = { id -> selectedClip=id; navTo("clip_audio") },
                    onRepairMedia = { navTo("media_repair") },
                    autoPlayOnReturn = resumeVideoPreview,
                    onReturnPlaybackConsumed = { resumeVideoPreview = false }) }
                "watermark" -> project?.let { current -> TextWatermarkScreen(current, ::back) { changed ->
                    if (write(changed)) screen = "editor"
                } }
                "trim" -> project?.let { TrimScreen(it, selectedClip, positionMs, ::back, { write(it) },
                    { selectedClip = it }, onDone = { saved -> if (write(saved)) screen = "editor" }) }
                "clip_audio" -> project?.let { current ->
                    ClipAudioScreen(current, selectedClip, ::back,
                        onApply = { track ->
                            val changed = ClipAudioBindings.attach(current, selectedClip, track)
                            if (changed != null && write(changed)) { screen="editor"; true } else false
                        },
                        onRemove = {
                            if (write(current.copy(audioTracks=current.audioTracks.filterNot {
                                it.clipId == selectedClip }))) { screen="editor"; true } else false
                        })
                }
                "audio" -> project?.let { AudioScreen(it, ::back, { write(it) },
                    { audioPicker.launch(arrayOf("audio/*")) }, selectedAudioTrackId,
                    onSelectionConsumed = { selectedAudioTrackId = null },
                    onChooseExternalVideo = { openGallery("audio", "audio_extract") },
                    externalVideoUri = importedAudioSourceVideo,
                    onExternalVideoConsumed = { importedAudioSourceVideo = null },
                    timelinePositionMs = positionMs,
                    onOpenGeminiSettings = { previous = "audio"; screen = "gemini_settings" },
                    narrationDraft = narrationDraft,
                    onSaveGeminiToAudio = { changed ->
                        write(changed)
                    },
                    onSaveAndReturn = { changed ->
                        if (write(changed)) { screen = "editor"; true } else false
                    }) }
                "voices" -> project?.let { current -> VoiceIaScreen(current,
                    back = ::back, onSave = { write(it) },
                    openGeminiSettings = { previous = "voices"; screen = "gemini_settings" },
                    initialCueId = selectedVoiceCueId) }
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
                        "image_insert" -> {
                            chosen.firstOrNull()?.let { incomingMainImage = it.uri }
                            screen = "editor"
                        }
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
                        "image_insert" -> mainImageDocumentPicker.launch(arrayOf("image/*"))
                        "new", "append" -> videoPicker.launch(arrayOf("video/*"))
                        "audio_extract" -> externalVideoPicker.launch(arrayOf("video/*"))
                        else -> overlayPicker.launch(arrayOf("image/*", "video/*"))
                    }
                }, scope = when (mediaAction) {
                    "animate_images", "image_insert" -> GalleryScope.IMAGES_ONLY
                    "new", "append", "audio_extract" -> GalleryScope.VIDEOS
                    else -> GalleryScope.IMAGES_AND_VIDEOS
                }, allowMultiple = mediaAction == "new" || mediaAction == "append" || mediaAction == "animate_images")
                "transition" -> project?.let { TransitionScreen(it, ::back, { write(it) }, selectedBoundary) }
                "autosubtitle" -> project?.let { current -> AutoSubtitleScreen(current, ::back,
                    onManageModel = { previous = "autosubtitle"; screen = "speech_model" },
                    onManageGemini = { previous = "autosubtitle"; screen = "gemini_settings" },
                    onApply = { reviewed ->
                    // Uma única alteração de rascunho para legendas e personagens revisados.
                    if (reviewed.id == current.id && write(reviewed)) {
                        selectedSubtitleId = reviewed.subtitles.firstOrNull()?.id
                        screen = "subtitles"
                    }
                }) }
                "subtitles" -> project?.let { SubtitleScreen(it, ::back, { write(it) }, selectedSubtitleId, initialPositionMs = positionMs,
                    onSaveEditor = { if (write(it)) screen = "editor" },
                    onAutoSubtitles = { screen = "autosubtitle" }) }
                "flow_prompt" -> project?.let { current -> FlowPromptScreen(current,
                    onSave = { draft -> projectSession.project?.takeIf { it.id == current.id }
                        ?.let { write(it.copy(flowPromptDraft = draft)) } ?: false }, back = ::back) }
                "publication" -> project?.let { current -> ProjectPublicationScreen(current,
                    onSave = { draft -> projectSession.project?.takeIf { it.id == current.id }
                        ?.let { write(it.copy(publicationDraft = draft)) } ?: false }, back = ::back) }
                "export" -> project?.let { current -> ExportScreen(current, store.quality(), ::back,
                    onPublicationDraftSave = { draft -> projectSession.project?.takeIf { it.id == current.id }
                        ?.let { write(it.copy(publicationDraft = draft)) } ?: false },
                    onSocialPublish = { uri, name, draft -> openSocialPublish(uri, name.ifBlank { "Vídeo exportado.mp4" }, draft) }) }
            }
        }
        if (pendingExitEditor) ConfirmEditorExit(projectSession.hasSavedOriginal, ::leaveEditor) {
            pendingExitEditor = false
        }
        if (screen == "home" && interruptedDraft != null) {
            val recovery = interruptedDraft!!
            ConfirmRecoverDraft(onRecover = {
                if (projectSession.restoreDraft(recovery)) {
                    selectedClip = TrimTimelineEdit.resumeClipId(recovery.project)
                    screen = "editor"
                } else android.widget.Toast.makeText(context,
                    "Projeto salvo mudou; rascunho não foi aplicado.", android.widget.Toast.LENGTH_LONG).show()
                interruptedDraft = null
            }, onDiscard = {
                draftRecoveryStore.clear()
                interruptedDraft = null
            })
        }
        if (screen == "home" && invalidRecovery) InvalidEditorDraft {
            draftRecoveryStore.clear()
            invalidRecovery = false
        }
        if (projectSession.storageWarning != null && !catalogAlertDismissed)
            CatalogStorageWarning(projectSession.storageWarning.orEmpty()) { catalogAlertDismissed = true }
        if (activeMediaTask != null) ActiveMediaImportDialog { mediaTaskCoordinator.cancel() }
        GitHubUpdatePrompt(githubUpdates) {
            previous = screen
            screen = "updates"
        }
        if (pendingNewProjectName) CreateProjectNameDialog(newProjectName,
            onNameChange = { newProjectName = it },
            onConfirm = {
                newProjectName = ProjectNaming.title(newProjectName)
                pendingNewProjectName = false
                pendingPicker = "new"; selectedClip = ""; openGallery("home", "new")
            }, onCancel = { pendingNewProjectName = false })
        if (pendingDeleteAll) DeleteAllProjectsDialog(projects.size, onConfirm = {
            try {
                projectSession.deleteAll()
                workspace.reconcile(null)
                screen = "home"
            } catch (failure: Exception) {
                DiagnosticRecorder.exception("project_delete_all_failure", failure)
                android.widget.Toast.makeText(context,
                    "Não foi possível excluir os projetos; os dados foram preservados.",
                    android.widget.Toast.LENGTH_LONG).show()
            }
            pendingDeleteAll = false
        }, onCancel = { pendingDeleteAll = false })
        pendingProjectDelete?.let { target ->
            ConfirmDeleteProjectDialog(target.title, onDelete = {
                val wasActive = project?.id == target.id
                try {
                    projectSession.delete(target.id)
                    if (wasActive) workspace.reconcile(null)
                } catch (failure: Exception) {
                    DiagnosticRecorder.exception("project_delete_failure", failure)
                    android.widget.Toast.makeText(context, "Não foi possível excluir o projeto.",
                        android.widget.Toast.LENGTH_LONG).show()
                }
                pendingProjectDelete = null
            }, onCancel = { pendingProjectDelete = null })
        }
        pendingSequential?.let { pending ->
            ConfirmSequentialOverlay(onConfirm = {
                pendingSequential = null
                insertOverlay(pending.uri, pending.isImage, pending.mediaMs, allowTimeOverlap = true)
            }, onCancel = { pendingSequential = null })
        }
        pendingOverlap?.let { pending ->
            ConfirmOverlayConflict(onKeep = {
                pendingOverlap = null
                if (saveOverlays(pending.project, pending.requested, pending.preferredId, "keep"))
                    selectedOverlayId = pending.preferredId
            }, onRearrange = {
                pendingOverlap = null
                if (saveOverlays(pending.project, pending.requested, pending.preferredId, "rearrange"))
                    selectedOverlayId = pending.preferredId
            }, onCancel = { pendingOverlap = null })
        }
    }
}
