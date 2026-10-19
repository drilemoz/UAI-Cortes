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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.AnnotatedString
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.geometry.Offset
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.common.MediaItem
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
        setContent { UaiApp() }
    }
}

@Composable
fun UaiApp() {
    val context = LocalContext.current
    val store = remember { ProjectStore(context) }
    var theme by remember { mutableStateOf(store.theme()) }
    var screen by remember { mutableStateOf("home") }
    var project by remember { mutableStateOf<VideoProject?>(null) }
    var projects by remember { mutableStateOf(store.load()) }
    var selectedClip by remember { mutableStateOf("") }
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
    fun write(p: VideoProject) { project = p; store.save(p); projects = store.load() }
    fun navTo(s: String) {
        if (s == "pip" && project != null && project!!.overlays.none { it.id == selectedOverlayId })
            selectedOverlayId = project!!.overlays.firstOrNull()?.id.orEmpty()
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
                val track = AudioTrack(uri.toString(), mediaName(context, uri),
                    startMs = positionMs.coerceIn(0L, current.durationMs),
                    durationMs = mediaDuration(context, uri).coerceAtMost(
                        (current.durationMs - positionMs).coerceAtLeast(1L)))
                write(current.copy(audioTracks = current.audioTracks + track))
            }
            screen = "audio"
        }
    }
    fun back() { screen = when (screen) {
        "editor" -> "home"
        "home" -> "home"
        "media" -> mediaOrigin
        "projects", "settings" -> "home"
        "general", "appearance", "about" -> "settings"
        "pip", "audio", "trim", "transition", "subtitles", "export" -> "editor"
        else -> previous
    } }
    BackHandler(screen != "home") { back() }
    MaterialTheme(colorScheme = palette) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            when (screen) {
                "home" -> MainHome(projects, onCreate = { pendingPicker = "new"; selectedClip = ""; videoPicker.launch(arrayOf("video/*")) },
                    onOpen = { project = it; selectedClip = it.clips.firstOrNull()?.id ?: ""; screen = "editor" },
                    onDelete = { store.delete(it.id); projects = store.load() },
                    onSettings = { screen = "settings" }, onTab = { screen = it })
                "projects" -> ProjectsScreen(projects, { back() }, { project = it; selectedClip = it.clips.firstOrNull()?.id ?: ""; screen = "editor" },
                    { store.delete(it.id); projects = store.load() })
                "settings" -> SettingsScreen({ back() }) { screen = it }
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
                    onSelectOverlay = { selectedOverlayId = it },
                    onTransition = { index -> selectedBoundary = index; screen = "transition" },
                    onSaveHome = { write(p); screen = "home" }) }
                "trim" -> project?.let { TrimScreen(it, selectedClip, positionMs, ::back, { write(it) }, { selectedClip = it }) }
                "audio" -> project?.let { AudioScreen(it, ::back, { write(it) }, { audioPicker.launch(arrayOf("audio/*")) }) }
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
                "subtitles" -> project?.let { SubtitleScreen(it, ::back, { write(it) }) }
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
        Text("Versão 0.1.20 (21)",color=MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp));Text("Editor de vídeo local para Android.",textAlign=androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(14.dp));Text("Novidades: prévia em repetição, seletor de velocidade no corte e editor e mini tutorial de corte.",
            color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(UnstableApi::class)
@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorScreen(p:VideoProject, selectedId:String, currentMs:Long,
    onSelected:(String)->Unit,onPosition:(Long)->Unit,onBack:()->Unit,onEdit:(String)->Unit,
    onUpdate:(VideoProject)->Unit,onAddVideo:()->Unit,onPiP:()->Unit,onAudio:()->Unit,
    onSelectOverlay:(String)->Unit,onTransition:(Int)->Unit,onSaveHome:()->Unit) {
    val context=LocalContext.current
    val player=remember { ExoPlayer.Builder(context).build().apply { repeatMode = Player.REPEAT_MODE_ALL } }
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
                    currentMs, player.isPlaying, playbackSpeed, false)
            }
        }
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)
            .background(Color(0xFF101014)).padding(10.dp), contentAlignment = Alignment.Center) {
            val actualAspect = remember(p.clips) {
                p.clips.firstOrNull()?.let { mediaAspect(context, it.uri, false) } ?: 16f / 9f
            }
            val videoW = minOf(maxWidth, maxHeight * actualAspect)
            val videoH = videoW / actualAspect
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
                    Box(Modifier.fillMaxSize().padding(horizontal=12.dp, vertical=8.dp),
                        contentAlignment = Alignment.Center) {
                        Text(cue.text, color=Color(cue.color), fontSize=cue.sizeSp.sp,
                            fontWeight=FontWeight.SemiBold, textAlign=TextAlign.Center,
                            modifier=Modifier.offset(x=((cue.x-.5f)*240).dp,y=((cue.y-.5f)*340).dp)
                                .background(Color.Black.copy(alpha=.64f),RoundedCornerShape(3.dp))
                                .padding(horizontal=7.dp, vertical=2.dp))
                    }
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
        Row(Modifier.fillMaxWidth().padding(horizontal=12.dp),horizontalArrangement=Arrangement.End) {
            TextButton(onClick=onSaveHome) { Icon(Icons.Default.Save,null); Spacer(Modifier.width(4.dp)); Text("Salvar e voltar à Home") }
        }
        OverlayTimeline(p.overlays, p.durationMs, onClick = { id -> onSelectOverlay(id); onEdit("pip") })
        TimelineTimedRow("Áudio",Green,
            (p.music?.let { listOf(Triple(it.title, 0L, p.durationMs)) } ?: emptyList()) +
                p.audioTracks.map { Triple(it.title,it.startMs,it.durationMs) },
            p.durationMs,onClick={onEdit("audio")})
        TimelineTimedRow("Legenda",Color(0xFFC38A38),
            p.subtitles.map { Triple(it.text,it.startMs,it.endMs-it.startMs) },
            p.durationMs,onClick={onEdit("subtitles")})
        Spacer(Modifier.height(5.dp));HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(10.dp), horizontalArrangement=Arrangement.SpaceEvenly) {
            listOf(Triple("Cortar",Icons.Default.ContentCut,"trim"),Triple("Unir",Icons.Default.Add,"add"),
                Triple("PiP",Icons.Default.Layers,"pip"),Triple("Áudio",Icons.Default.MusicNote,"audio"),
                Triple("Transição",Icons.Default.AutoAwesomeMotion,"transition"),
                Triple("Legendas",Icons.Default.ClosedCaption,"subtitles")).forEach { (name,icon,key) ->
                Column(Modifier.weight(1f).clickable {
                    when(key) {"add"->onAddVideo();"pip"->onPiP();
                        "audio"->onEdit("audio");else->onEdit(key)}
                },horizontalAlignment=Alignment.CenterHorizontally) {
                    Box(Modifier.size(52.dp).clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),contentAlignment=Alignment.Center) {
                        Icon(icon,name,modifier=Modifier.size(22.dp))
                    }
                    Text(name,fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant, maxLines=1)
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
    volume: Float, projectMs: Long, playing: Boolean, speed: Float, loop: Boolean) {
    val context=LocalContext.current
    val player=remember(uri) { ExoPlayer.Builder(context).build() }
    DisposableEffect(player) { onDispose {player.release()} }
    LaunchedEffect(player,uri,loop) {
        player.repeatMode=if(loop)Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        player.setMediaItem(MediaItem.fromUri(uri));player.prepare()
    }
    LaunchedEffect(player,volume,speed) {
        player.volume=volume.coerceIn(0f,1f)
        player.setPlaybackParameters(PlaybackParameters(speed))
    }
    LaunchedEffect(player,projectMs,playing,startMs,durationMs) {
        val within=projectMs>=startMs && projectMs<startMs+durationMs
        if(!within || !playing) player.pause() else {
            val wanted=(projectMs-startMs).coerceAtLeast(0L)
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
    val pipPlayer = remember(pip.uri) { if (!pip.isImage) ExoPlayer.Builder(context).build() else null }
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
    autoApply: Boolean = false) {
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
                editing = false
                focusManager.clearFocus()
            }),
            modifier = Modifier.weight(1f).onFocusChanged { state ->
                if (state.isFocused && !editing) {
                    digits = "" // uma nova digitação começa do zero, sem precisar apagar separadores
                    editing = true
                } else if (!state.isFocused && editing) {
                    editing = false
                    // Os dados digitados inválidos não substituem o último valor válido.
                    digits = NumericTime.fromMs(timeMs)
                }
            },
            label = { Text(label) }, singleLine = true, isError = digits.isNotBlank() && !allowed,
            supportingText = if (digits.isNotBlank() && !allowed && !autoApply) {
                { Text("Fora do intervalo") }
            } else null
        )
        if (!autoApply) {
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

@OptIn(UnstableApi::class)
@Composable
private fun TrimScreen(p: VideoProject, selectedId: String, globalMs: Long, back: () -> Unit,
                       update: (VideoProject) -> Unit, selected: (String) -> Unit) {
    val context = LocalContext.current
    val clip = p.clips.firstOrNull { it.id == selectedId } ?: p.clips.firstOrNull()
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        Heading("Cortar e unir", back)
        if (clip == null) {
            Text("Adicione um vídeo para editar.", modifier = Modifier.padding(16.dp))
            return@Column
        }
        val total = clip.sourceDurationMs.coerceAtLeast(1L)
        val player = remember(clip.id) { ExoPlayer.Builder(context).build().apply { repeatMode = Player.REPEAT_MODE_ONE } }
        var playbackSpeed by remember(clip.id) { mutableFloatStateOf(1f) }
        LaunchedEffect(player, playbackSpeed) { player.setPlaybackParameters(PlaybackParameters(playbackSpeed)) }
        DisposableEffect(player) { onDispose { player.release() } }
        var cursorMs by remember(clip.id) { mutableLongStateOf(clip.startMs) }
        var playing by remember(clip.id) { mutableStateOf(false) }
        var begin by remember(clip.id) { mutableLongStateOf(clip.startMs) }
        var finish by remember(clip.id) { mutableLongStateOf(clip.endMs) }
        // Um nível de desfazer cobre aplicar, dividir, reordenar e excluir.
        var undoClips by remember(p.id) { mutableStateOf<List<Clip>?>(null) }
        var undoSelected by remember(p.id) { mutableStateOf("") }
        LaunchedEffect(clip.id, clip.startMs, clip.endMs) {
            begin = clip.startMs
            finish = clip.endMs
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
                Text("Toque no clipe para selecionar", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                p.clips.forEachIndexed { i, c ->
                    OutlinedButton(onClick = { player.pause(); selected(c.id) },
                        border = BorderStroke(if (c.id == clip.id) 2.dp else 1.dp,
                            if (c.id == clip.id) Blue else MaterialTheme.colorScheme.outlineVariant),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                        Text("${i + 1} · ${c.title.take(21)}", maxLines = 1)
                    }
                }
            }
            // Prévia e miniaturas são blocos independentes: nenhum RangeSlider cobre as fotos.
            BoxWithConstraints(Modifier.fillMaxWidth().height(215.dp)
                .clip(RoundedCornerShape(14.dp)).background(Color(0xFF111114)),
                contentAlignment = Alignment.Center) {
                val videoAspect = remember(clip.uri) { mediaAspect(context, clip.uri, false) }
                val videoWidth = minOf(maxWidth, maxHeight * videoAspect)
                AndroidView(factory = { ctx -> previewPlayer(ctx).apply {
                    this.player = player
                    useController = false
                } }, modifier = Modifier.width(videoWidth).height(videoWidth / videoAspect),
                    update = { it.player = player })
            }
            TrimThumbnails(clip.uri, total) { time -> player.pause(); seek(time) }
            // Uma única barra para navegar; início e fim são definidos nos campos abaixo.
            Slider(value = cursorMs.toFloat().coerceIn(0f, total.toFloat()),
                onValueChange = { player.pause(); seek(it.toLong()) },
                valueRange = 0f..total.toFloat(), modifier = Modifier.fillMaxWidth().height(30.dp))
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
            Text("Dividir cria dois trechos; as setas mudam a ordem; a lixeira exclui um clipe.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            // Cinco ações em uma só linha, com ícones e rótulos curtos.
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                TrimQuickAction(Icons.Default.ContentCut, "Dividir", true, Modifier.weight(1f)) {
                    when {
                        dirty -> notifyUser("Aplique ou desfaça o corte antes de dividir.")
                        !canSplit -> notifyUser("Coloque o cursor dentro do trecho, afastado das extremidades.")
                        else -> {
                            rememberUndo()
                            val updated = p.clips.toMutableList()
                            updated[idx] = clip.copy(endMs = cursorMs)
                            updated.add(idx + 1, clip.copy(
                                id = UUID.randomUUID().toString(), startMs = cursorMs))
                            update(p.copy(clips = updated))
                        }
                    }
                }
                TrimQuickAction(Icons.Default.Undo, "Desfazer", undoClips != null,
                    Modifier.weight(1f)) {
                    undoClips?.let { snapshot ->
                        begin = snapshot.firstOrNull { it.id == undoSelected }?.startMs ?: begin
                        finish = snapshot.firstOrNull { it.id == undoSelected }?.endMs ?: finish
                        update(p.copy(clips = snapshot))
                        selected(undoSelected)
                        undoClips = null
                    }
                }
                TrimQuickAction(Icons.Default.ArrowBack, "Antes", idx > 0,
                    Modifier.weight(1f)) {
                    rememberUndo()
                    val list = p.clips.toMutableList()
                    val before = list[idx - 1]
                    list[idx - 1] = list[idx]
                    list[idx] = before
                    update(p.copy(clips = list))
                }
                TrimQuickAction(Icons.Default.ArrowForward, "Depois", idx < p.clips.lastIndex,
                    Modifier.weight(1f)) {
                    rememberUndo()
                    val list = p.clips.toMutableList()
                    val after = list[idx + 1]
                    list[idx + 1] = list[idx]
                    list[idx] = after
                    update(p.copy(clips = list))
                }
                TrimQuickAction(Icons.Default.Delete, "Excluir", p.clips.size > 1,
                    Modifier.weight(1f)) {
                    val other = p.clips.firstOrNull { it.id != clip.id }
                    if (other != null) {
                        rememberUndo()
                        update(p.copy(clips = p.clips.filterNot { it.id == clip.id }))
                        selected(other.id)
                    }
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
                    Text("3. Dividir separa o clipe no cursor; Antes/Depois reorganizam; Excluir remove.",
                        style = MaterialTheme.typography.bodySmall)
                    Text("4. Desfazer volta a última edição. O vídeo repete até pausar.",
                        style = MaterialTheme.typography.bodySmall)
                    Text("5. No botão de velocidade escolha de 0,25× a 2×. Afeta só a prévia, não o vídeo salvo.",
                        style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun AudioScreen(p:VideoProject,back:()->Unit,update:(VideoProject)->Unit,chooseMusic:()->Unit) {
    val context = LocalContext.current
    val engine = remember { AudioExtractionEngine(context) }
    var sourceIndex by remember(p.id) { mutableIntStateOf(-1) } // -1 = all clips
    var message by remember { mutableStateOf("") }
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
                },onError={error->exporting=false; message="Falha: $error"})
            }.onFailure { exporting=false; message="Falha: ${it.message}" }
        }
    }
    DisposableEffect(engine) { onDispose { engine.cancel() } }
    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())
        .padding(horizontal=16.dp)) {
        Heading("Áudio",back)
        Text("Volumes do projeto",fontWeight=FontWeight.Bold)
        p.clips.forEachIndexed { index,clip ->
            Text("Clipe ${index+1}: ${clip.title} · ${(clip.volume*100).toInt()}%",
                maxLines=1,overflow=TextOverflow.Ellipsis,fontSize=12.sp)
            Slider(value=clip.volume.coerceIn(0f,1f),onValueChange={volume->
                update(p.copy(clips=p.clips.map { if(it.id==clip.id) it.copy(volume=volume) else it }))
            })
        }
        p.music?.let { music ->
            Text("Música antiga: ${music.title} · ${(music.volume*100).toInt()}%")
            Slider(value=music.volume.coerceIn(0f,1f),onValueChange={update(p.copy(music=music.copy(volume=it)))})
            TextButton(onClick={update(p.copy(music=null))}) { Icon(Icons.Default.Delete,null);Text("Remover música") }
        }
        p.audioTracks.forEach { track ->
            Text(track.title,maxLines=1,overflow=TextOverflow.Ellipsis)
            Row(verticalAlignment=Alignment.CenterVertically) {
                Slider(value=track.volume.coerceIn(0f,1f),modifier=Modifier.weight(1f),onValueChange={v->
                    update(p.copy(audioTracks=p.audioTracks.map { if(it.id==track.id) it.copy(volume=v) else it }))
                })
                IconButton(onClick={update(p.copy(audioTracks=p.audioTracks.filterNot{it.id==track.id}))}) {
                    Icon(Icons.Default.Delete,"Remover faixa")
                }
            }
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
    val canvasAspect = remember(p.clips) {
        p.clips.firstOrNull()?.let { mediaAspect(context, it.uri, false) } ?: 16f / 9f
    }
    val player = remember(p.id) { ExoPlayer.Builder(context).build() }
    var playing by remember(p.id) { mutableStateOf(false) }
    var previewTime by remember(p.id) { mutableLongStateOf(pip?.startAtMs ?: 0L) }
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
                BoxWithConstraints(Modifier.fillMaxWidth().height(246.dp)
                    .clip(RoundedCornerShape(18.dp)).background(Color(0xFF141414)),
                    contentAlignment = Alignment.Center) {
                    val canvasWidth = minOf(maxWidth, maxHeight * canvasAspect)
                    val canvasHeight = canvasWidth / canvasAspect
                    Box(Modifier.width(canvasWidth).height(canvasHeight).background(Color.Black)) {
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

@Composable
private fun SubtitleScreen(p:VideoProject,back:()->Unit,update:(VideoProject)->Unit) {
    val context=LocalContext.current
    var selectedId by remember(p.id) { mutableStateOf(p.subtitles.firstOrNull()?.id.orEmpty()) }
    var status by remember { mutableStateOf("") }
    val importer=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if(uri!=null) {
            val loaded=runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { reader ->
                    val content=StringBuilder()
                    val chunk=CharArray(4096)
                    while(content.length<250_000) {
                        val received=reader.read(chunk,0,minOf(chunk.size,250_000-content.length))
                        if(received<=0) break
                        content.append(chunk,0,received)
                    }
                    content.toString()
                }.orEmpty()
            }
            loaded.onSuccess { content ->
                val cues=EditFeatures.parseSrt(content).filter{it.startMs<p.durationMs}
                    .map{it.copy(endMs=it.endMs.coerceAtMost(p.durationMs))}
                if(cues.isNotEmpty()) {
                    update(p.copy(subtitles=p.subtitles+cues))
                    selectedId=cues.first().id
                    status="${cues.size} legendas importadas"
                } else status="Nenhuma legenda SRT válida no arquivo"
            }.onFailure {status="Não foi possível ler o arquivo: ${it.message}"}
        }
    }
    val selected=p.subtitles.firstOrNull{it.id==selectedId}
    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())
        .padding(horizontal=16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Heading("Legendas",back)
        Text("Crie legendas manuais ou importe um arquivo .srt. Os textos serão exibidos na prévia e incorporados ao MP4.",
            style=MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            Button(onClick={
                val start=0L
                val cue=SubtitleCue(text="Nova legenda",startMs=start,
                    endMs=minOf(3000L,p.durationMs).coerceAtLeast(1L))
                update(p.copy(subtitles=p.subtitles+cue));selectedId=cue.id
            },modifier=Modifier.weight(1f)) {Icon(Icons.Default.Add,null);Text(" Nova")}
            OutlinedButton(onClick={importer.launch(arrayOf("text/*","application/x-subrip","*/*"))},
                modifier=Modifier.weight(1f)) {Icon(Icons.Default.FileOpen,null);Text(" Importar SRT")}
        }
        p.subtitles.forEachIndexed { index,cue ->
            Surface(onClick={selectedId=cue.id},shape=RoundedCornerShape(5.dp),
                color=if(selectedId==cue.id)MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant) {
                Row(Modifier.fillMaxWidth().padding(10.dp),verticalAlignment=Alignment.CenterVertically) {
                    Text("${index+1} · ${cue.text.take(45)}",modifier=Modifier.weight(1f),maxLines=1)
                    Text("${formatPipMs(cue.startMs)}",fontSize=11.sp)
                }
            }
        }
        if(selected!=null) {
            OutlinedTextField(value=selected.text,onValueChange={v->
                update(p.copy(subtitles=p.subtitles.map{if(it.id==selected.id) it.copy(text=v.take(500)) else it}))
            },label={Text("Texto")},minLines=2,modifier=Modifier.fillMaxWidth())
            TimeDigitsField("Início",selected.startMs,p.durationMs,
                valid={it<selected.endMs},onCommit={v->
                    update(p.copy(subtitles=p.subtitles.map{if(it.id==selected.id)it.copy(startMs=v) else it}))
                })
            TimeDigitsField("Fim",selected.endMs,p.durationMs,
                valid={it>selected.startMs && it<=p.durationMs},onCommit={v->
                    update(p.copy(subtitles=p.subtitles.map{if(it.id==selected.id)it.copy(endMs=v) else it}))
                })
            Text("Tamanho: ${selected.sizeSp} sp")
            Slider(value=selected.sizeSp.toFloat(),valueRange=12f..48f,onValueChange={v->
                update(p.copy(subtitles=p.subtitles.map{
                    if(it.id==selected.id)it.copy(sizeSp=v.toInt()) else it}))
            })
            Text("Cor")
            Row(horizontalArrangement=Arrangement.spacedBy(7.dp)) {
                listOf(-1,android.graphics.Color.YELLOW,android.graphics.Color.CYAN).forEach { shade ->
                    FilterChip(selected=selected.color==shade,onClick={
                        update(p.copy(subtitles=p.subtitles.map{
                            if(it.id==selected.id)it.copy(color=shade) else it}))
                    },label={Text(when(shade){-1->"Branco";android.graphics.Color.YELLOW->"Amarelo";else->"Ciano"})})
                }
            }
            Text("Posição horizontal: ${(selected.x*100).toInt()}%")
            Slider(value=selected.x,valueRange=.1f.. .9f,onValueChange={v->
                update(p.copy(subtitles=p.subtitles.map{if(it.id==selected.id)it.copy(x=v) else it}))
            })
            Text("Posição vertical: ${(selected.y*100).toInt()}%")
            Slider(value=selected.y,valueRange=.1f.. .9f,onValueChange={v->
                update(p.copy(subtitles=p.subtitles.map{if(it.id==selected.id)it.copy(y=v) else it}))
            })
            Text("Posição da legenda")
            Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                listOf("Cima" to .15f,"Centro" to .5f,"Baixo" to .85f).forEach { (name,y) ->
                    FilterChip(selected=kotlin.math.abs(selected.y-y)<.02f,onClick={
                        update(p.copy(subtitles=p.subtitles.map{
                            if(it.id==selected.id)it.copy(y=y) else it}))
                    },label={Text(name)})
                }
            }
            OutlinedButton(onClick={
                update(p.copy(subtitles=p.subtitles.filterNot{it.id==selected.id}))
                selectedId=""
            }) {Icon(Icons.Default.Delete,null);Text(" Excluir legenda")}
        }
        if(status.isNotBlank()) Text(status,color=MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun ExportScreen(p:VideoProject,defaultQuality:Int,back:()->Unit) {
    val context=LocalContext.current
    val engine=remember { ExportEngine(context) }
    val previewPlayer=remember { ExoPlayer.Builder(context).build() }
    var previewPlaying by remember { mutableStateOf(false) }
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
        Box(Modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF151515)),contentAlignment=Alignment.Center) {
            AndroidView(factory={previewPlayer(it).apply { player=previewPlayer;useController=false }},
                modifier=Modifier.fillMaxSize(),update={it.player=previewPlayer})
            IconButton(onClick={
                previewPlaying=!previewPlaying
                if(previewPlaying) previewPlayer.play() else previewPlayer.pause()
            }) {Icon(if(previewPlaying)Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                "Reproduzir prévia",tint=Color.White,modifier=Modifier.size(52.dp))}
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
                    result="";progress=0;working=true
                    runCatching {engine.start(p,quality,name,
                        onSuccess={uri->working=false;progress=100;result="Vídeo salvo em Filmes/UaiCortes"},
                        onError={error->working=false;result="Erro: $error"})}
                        .onFailure {working=false;result="Erro: ${it.message}"}
                }
            },modifier=Modifier.fillMaxWidth(),enabled=name.isNotBlank()&&p.clips.isNotEmpty())
        }
        if(result.isNotBlank()) Text(result,modifier=Modifier.padding(vertical=15.dp),
            color=if(result.startsWith("Erro"))MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(14.dp));Text("Formato MP4 · H.264/AAC · Salvamento local.",
            color=MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.bodySmall)
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
