package br.com.uaicortes

import android.content.Intent
import android.net.Uri
import android.os.Bundle
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.window.Dialog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.geometry.Offset
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Date
import java.util.UUID

private val Blue = Color(0xFF0866FF)
private val Purple = Color(0xFF6954B5)
private val Green = Color(0xFF16866E)
private val Ink = Color(0xFF151515)
private val LightBg = Color(0xFFF7F8FA)

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
    var previous by remember { mutableStateOf("home") }
    val dark = when (theme) {
        "Claro" -> false
        "Escuro", "AMOLED" -> true
        else -> isSystemInDarkTheme()
    }
    val palette = if (dark) darkColorScheme(primary = Color(0xFF72A5FF), background = if(theme=="AMOLED") Color.Black else Ink,
        surface = if(theme=="AMOLED") Color.Black else Color(0xFF202124))
    else lightColorScheme(primary = Blue, background = LightBg, surface = Color.White)
    fun write(p: VideoProject) { project = p; store.save(p); projects = store.load() }
    fun navTo(s: String) { previous = screen; screen = s }
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
            project?.let { write(it.copy(pip = Overlay(uri.toString(), image, duration, positionMs))) }
            screen = "pip"
        }
    }
    val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            project?.let { write(it.copy(music = Music(uri.toString(), mediaName(context, uri)))) }
            screen = "audio"
        }
    }
    fun back() { screen = when (screen) {
        "editor" -> "home"
        "home" -> "home"
        "media" -> mediaOrigin
        "projects", "settings" -> "home"
        "general", "appearance", "about" -> "settings"
        "pip", "audio", "trim", "transition", "export" -> "editor"
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
                    onPiP = { mediaOrigin = "editor"; screen = "media" },
                    onAudio = { audioPicker.launch(arrayOf("audio/*")) }) }
                "trim" -> project?.let { TrimScreen(it, selectedClip, positionMs, ::back, { write(it) }, { selectedClip = it }) }
                "audio" -> project?.let { AudioScreen(it, ::back, { write(it) }, { audioPicker.launch(arrayOf("audio/*")) }) }
                "pip" -> project?.let { PipScreen(it, ::back, { write(it) }, {
                    mediaOrigin = "pip"; screen = "media"
                }) }
                "media" -> DeviceMediaBrowser(::back, onSelect = { chosen ->
                    project?.let { current ->
                        val start = positionMs.coerceIn(0L, (current.durationMs - 1L).coerceAtLeast(0L))
                        val remaining = (current.durationMs - start).coerceAtLeast(1L)
                        val duration = (if (chosen.isImage) 5000L else chosen.durationMs.takeIf { it > 0 }
                            ?: mediaDuration(context, chosen.uri)).coerceAtMost(remaining)
                        write(current.copy(pip = Overlay(chosen.uri.toString(), chosen.isImage,
                            duration, start)))
                    }
                    screen = "pip"
                }, openDocuments = { overlayPicker.launch(arrayOf("image/*", "video/*")) })
                "transition" -> project?.let { TransitionScreen(it, ::back, { write(it) }) }
                "export" -> project?.let { ExportScreen(it, store.quality(), ::back) }
            }
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
        Text("Versão 0.1.7 (8)",color=MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp));Text("Editor de vídeo local para Android.",textAlign=androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(14.dp));Text("Novidades: biblioteca por tipo de mídia, PiP com ajustes em janelas e controle independente de som.",
            color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun EditorScreen(p:VideoProject, selectedId:String, currentMs:Long,
    onSelected:(String)->Unit,onPosition:(Long)->Unit,onBack:()->Unit,onEdit:(String)->Unit,
    onUpdate:(VideoProject)->Unit,onAddVideo:()->Unit,onPiP:()->Unit,onAudio:()->Unit) {
    val context=LocalContext.current
    val player=remember { ExoPlayer.Builder(context).build() }
    DisposableEffect(player) { onDispose { player.release() } }
    LaunchedEffect(p.clips) {
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
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                } }, update = { it.player = player }, modifier = Modifier.fillMaxSize())
                p.pip?.let { pip ->
                    if (currentMs >= pip.startAtMs && currentMs < pip.startAtMs + pip.durationMs) {
                        PipLayer(pip, currentMs - pip.startAtMs, player.isPlaying,
                            editable = false) { overlay -> onUpdate(p.copy(pip = overlay)) }
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
        }
        Row(Modifier.fillMaxWidth().padding(horizontal=16.dp), horizontalArrangement=Arrangement.SpaceBetween) {
            Text(formatMs(currentMs),style=MaterialTheme.typography.bodySmall)
            Text(formatMs(p.durationMs),style=MaterialTheme.typography.bodySmall)
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
            onClip={ id -> onSelected(p.clips[id].id);player.seekTo(id,0L) })
        TimelineRow("PiP",Purple,p.pip?.let{listOf((if(it.isImage) "Imagem" else "Vídeo") to it.durationMs)} ?: emptyList(),p.durationMs,currentMs,"",onClip={onEdit("pip")})
        TimelineRow("Áudio",Green,p.music?.let{listOf(it.title to p.durationMs)} ?: emptyList(),p.durationMs,currentMs,"",onClip={onEdit("audio")})
        Spacer(Modifier.height(5.dp));HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(10.dp), horizontalArrangement=Arrangement.SpaceEvenly) {
            listOf(Triple("Cortar",Icons.Default.ContentCut,"trim"),Triple("Unir",Icons.Default.Add,"add"),
                Triple("PiP",Icons.Default.Layers,"pip"),Triple("Áudio",Icons.Default.MusicNote,"audio"),
                Triple("Transição",Icons.Default.AutoAwesomeMotion,"transition")).forEach { (name,icon,key) ->
                Column(Modifier.weight(1f).clickable {
                    when(key) {"add"->onAddVideo();"pip"->if(p.pip==null)onPiP() else onEdit("pip");
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
    }
}

@Composable
private fun TimelineRow(label:String,color:Color, segments:List<Pair<String,Long>>,total:Long,position:Long,
    selectedId:String,onClip:(Int)->Unit) {
    Row(Modifier.fillMaxWidth().height(40.dp).padding(horizontal=16.dp,vertical=3.dp),verticalAlignment=Alignment.CenterVertically) {
        Box(Modifier.width(58.dp).fillMaxHeight().clip(RoundedCornerShape(9.dp)).background(color),
            contentAlignment=Alignment.Center){Text(label,color=Color.White,fontSize=12.sp)}
        Spacer(Modifier.width(6.dp))
        Row(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha=0.16f)).horizontalScroll(rememberScrollState())) {
            segments.forEachIndexed { index, (title,duration) ->
                Box(Modifier.width(((duration.toFloat()/total.coerceAtLeast(1))*260f).coerceAtLeast(42f).dp)
                    .fillMaxHeight().padding(end=2.dp).clip(RoundedCornerShape(6.dp))
                    .background(color.copy(alpha=0.22f)).clickable {onClip(index)},contentAlignment=Alignment.Center) {
                    Text(title,maxLines=1,overflow=TextOverflow.Ellipsis,fontSize=11.sp,
                        color=MaterialTheme.colorScheme.onSurface,modifier=Modifier.padding(horizontal=4.dp))
                }
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun PipLayer(pip: Overlay, timeMs: Long, playing: Boolean,
                     editable: Boolean = true, onUpdate: (Overlay) -> Unit) {
    val context = LocalContext.current
    val sourceAspect = remember(pip.uri, pip.isImage) { mediaAspect(context, pip.uri, pip.isImage) }
    val pipPlayer = remember(pip.uri) { if (!pip.isImage) ExoPlayer.Builder(context).build() else null }
    DisposableEffect(pipPlayer) { onDispose { pipPlayer?.release() } }
    LaunchedEffect(pip.uri) {
        pipPlayer?.apply { setMediaItem(MediaItem.fromUri(pip.uri)); prepare() }
    }
    LaunchedEffect(playing, timeMs, pip.volume, pip.muted) {
        pipPlayer?.apply {
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
                AndroidView(factory = { previewPlayer(it).apply {
                    player = pipPlayer; useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                } }, modifier = Modifier.fillMaxSize(), update = { it.player = pipPlayer })
            }
        }
    }
}

@Composable
private fun TrimScreen(p:VideoProject,selectedId:String,globalMs:Long,back:()->Unit,
                       update:(VideoProject)->Unit,selected:(String)->Unit) {
    val clip=p.clips.firstOrNull{it.id==selectedId} ?: p.clips.firstOrNull()
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal=18.dp)) {
        Heading("Cortar e unir",back)
        if(clip==null){Text("Adicione um vídeo para editar.");return@Column}
        Text("Selecione um clipe",fontWeight=FontWeight.SemiBold)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            p.clips.forEachIndexed { i,c ->
                OutlinedButton(onClick={selected(c.id)},border=BorderStroke(if(c.id==clip.id) 2.dp else 1.dp,
                    if(c.id==clip.id)Blue else MaterialTheme.colorScheme.outlineVariant)) {Text("${i+1} · ${c.title.take(14)}",maxLines=1)}
            }
        }
        Spacer(Modifier.height(20.dp));Text("Início: ${formatMs(clip.startMs)}")
        Slider(value=clip.startMs.toFloat(),onValueChange={value->
            update(p.copy(clips=p.clips.map {if(it.id==clip.id) it.copy(startMs=value.toLong().coerceAtMost(it.endMs-250L)) else it}))
        },valueRange=0f..(clip.endMs-250L).coerceAtLeast(1L).toFloat())
        Text("Fim: ${formatMs(clip.endMs)}")
        Slider(value=clip.endMs.toFloat(),onValueChange={value->
            update(p.copy(clips=p.clips.map {if(it.id==clip.id) it.copy(endMs=value.toLong().coerceAtLeast(it.startMs+250L)) else it}))
        },valueRange=(clip.startMs+250L).coerceAtMost(clip.sourceDurationMs).toFloat()..clip.sourceDurationMs.toFloat())
        Text("Trecho mantido: ${formatMs(clip.durationMs)}",color=MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(20.dp))
        val idx=p.clips.indexOfFirst{it.id==clip.id}
        val startAt=p.clips.take(idx.coerceAtLeast(0)).sumOf{it.durationMs}
        val splitMs=clip.startMs+(globalMs-startAt).coerceIn(0,clip.durationMs)
        BlueButton("Dividir no cursor (${formatMs(globalMs)})",{
            if(splitMs>clip.startMs+250 && splitMs<clip.endMs-250) {
                val updated=p.clips.toMutableList()
                updated[idx]=clip.copy(endMs=splitMs)
                updated.add(idx+1,clip.copy(id=UUID.randomUUID().toString(),startMs=splitMs))
                update(p.copy(clips=updated))
            }
        },Modifier.fillMaxWidth(),enabled=splitMs>clip.startMs+250 && splitMs<clip.endMs-250)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            OutlinedButton(onClick={
                if(idx>0){val list=p.clips.toMutableList();val tmp=list[idx-1];list[idx-1]=list[idx];list[idx]=tmp;update(p.copy(clips=list))}
            },enabled=idx>0) {Icon(Icons.Default.ArrowBack,null);Text("Mover")}
            OutlinedButton(onClick={
                if(idx<p.clips.lastIndex){val list=p.clips.toMutableList();val tmp=list[idx+1];list[idx+1]=list[idx];list[idx]=tmp;update(p.copy(clips=list))}
            },enabled=idx<p.clips.lastIndex) {Text("Mover");Icon(Icons.Default.ArrowForward,null)}
            OutlinedButton(onClick={
                update(p.copy(clips=p.clips.filterNot{it.id==clip.id}));selected(p.clips.firstOrNull{it.id!=clip.id}?.id?:"")
            },enabled=p.clips.size>1) {Icon(Icons.Default.Delete,null)}
        }
        Spacer(Modifier.height(14.dp));Text("Os arquivos originais não são modificados.",
            style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AudioScreen(p:VideoProject,back:()->Unit,update:(VideoProject)->Unit,chooseMusic:()->Unit) {
    val original=p.clips.firstOrNull()?.volume ?: 1f
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(20.dp)) {
        Heading("Áudio",back)
        Spacer(Modifier.height(15.dp))
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
            Text("Som do vídeo",fontSize=19.sp,fontWeight=FontWeight.Medium)
            Text("${(original*100).toInt()}%",color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(value=original.coerceIn(0f,1f),onValueChange={v->update(p.copy(clips=p.clips.map{it.copy(volume=v)}))})
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
            Text("Música adicionada",fontSize=19.sp,fontWeight=FontWeight.Medium)
            Text("${((p.music?.volume?:0f)*100).toInt()}%",color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(value=p.music?.volume?:0f,onValueChange={v->p.music?.let {update(p.copy(music=it.copy(volume=v))) }},enabled=p.music!=null)
        if(p.music!=null) {
            Text("Arquivo: ${p.music.title}",style=MaterialTheme.typography.bodySmall)
            TextButton(onClick={update(p.copy(music=null))}){Icon(Icons.Default.Delete,null);Text(" Remover música")}
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick={update(p.copy(clips=p.clips.map{it.copy(volume=if(original==0f)1f else 0f)}))},
                modifier=Modifier.weight(1f)) {Icon(Icons.Default.VolumeOff,null);Text(if(original==0f)" Ativar" else " Silenciar")}
            OutlinedButton(onClick={
                // Por clipe, em vez de cortar segmentos de áudio separadamente nesta primeira versão.
                update(p.copy(clips=p.clips.mapIndexed {index,c-> if(index==0)c.copy(volume=0f) else c}))
            },modifier=Modifier.weight(1f)) {Icon(Icons.Default.ContentCut,null);Text(" 1º clipe")}
        }
        Spacer(Modifier.height(20.dp));BlueButton("+ Adicionar música / áudio",chooseMusic,Modifier.fillMaxWidth())
        Spacer(Modifier.height(14.dp));Text("O volume do PiP em vídeo é ajustado na tela Sobreposição.",
            color=MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.bodySmall)
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun PipScreen(p: VideoProject, back: () -> Unit, update: (VideoProject) -> Unit,
                      choose: () -> Unit) {
    val context = LocalContext.current
    val pip = p.pip
    val total = p.durationMs.coerceAtLeast(1L)
    val sourceAspect = remember(pip?.uri) {
        pip?.let { mediaAspect(context, it.uri, it.isImage) } ?: 16f / 9f
    }
    val canvasAspect = remember(p.clips) {
        p.clips.firstOrNull()?.let { mediaAspect(context, it.uri, false) } ?: 16f / 9f
    }
    val player = remember(p.id) { ExoPlayer.Builder(context).build() }
    var playing by remember(p.id) { mutableStateOf(false) }
    var previewTime by remember(p.id) { mutableLongStateOf(pip?.startAtMs ?: 0L) }
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
            seekProjectPosition(player, p.clips, pip?.startAtMs ?: 0L)
        }
    }
    LaunchedEffect(pip?.startAtMs) {
        if (!player.isPlaying && pip != null && p.clips.isNotEmpty()) {
            previewTime = pip.startAtMs
            seekProjectPosition(player, p.clips, pip.startAtMs)
        }
    }
    LaunchedEffect(player, p.clips) {
        while (true) {
            playing = player.isPlaying
            if (player.isPlaying) previewTime = projectPosition(player, p.clips, p.durationMs)
            delay(150)
        }
    }
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        Heading("Editar sobreposição", back)
        if (pip == null) {
            Spacer(Modifier.height(40.dp))
            Button(onClick = choose, modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp)
                .height(56.dp), shape = RoundedCornerShape(18.dp)) {
                Icon(Icons.Default.AddPhotoAlternate, null)
                Text(" Adicionar imagem ou vídeo")
            }
            return@Column
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Prévia no vídeo", fontWeight = FontWeight.SemiBold)
            // A área PiP corresponde ao quadro real, não às barras pretas do player.
            BoxWithConstraints(Modifier.fillMaxWidth().height(246.dp)
                .clip(RoundedCornerShape(18.dp)).background(Color(0xFF141414)),
                contentAlignment = Alignment.Center) {
                val canvasWidth = minOf(maxWidth, maxHeight * canvasAspect)
                val canvasHeight = canvasWidth / canvasAspect
                Box(Modifier.width(canvasWidth).height(canvasHeight)
                    .background(Color.Black)) {
                    AndroidView(factory = { previewPlayer(it).apply {
                        this.player = player; useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    } }, update = { it.player = player }, modifier = Modifier.fillMaxSize())
                    PipLayer(pip, (previewTime - pip.startAtMs).coerceAtLeast(0L), playing,
                        editable = true) { overlay -> update(p.copy(pip = overlay)) }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("Vídeo: ${formatMs(previewTime)} / ${formatMs(p.durationMs)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = {
                    if (player.isPlaying) player.pause() else player.play()
                    playing = player.isPlaying
                }) {
                    Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                        "Reproduzir ou pausar vídeo")
                    Text(if (playing) "Pausar" else "Ver prévia")
                }
            }
            Text("Arraste a imagem ou use os atalhos de posição. Pinça para ajustar o tamanho.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            var popup by remember { mutableStateOf<String?>(null) }
            ItemCard(Icons.Default.OpenWith, "Posição", "Alinhar em nove pontos ou arrastar na prévia") {
                popup = "position"
            }
            ItemCard(Icons.Default.AspectRatio, "Tamanho", "${(pip.scale * 100).toInt()}% · atalhos, barra ou valor manual") {
                popup = "size"
            }
            ItemCard(Icons.Default.Schedule, "Duração", "${formatPipMs(pip.startAtMs)} até ${formatPipMs((pip.startAtMs + pip.durationMs).coerceAtMost(total))}") {
                popup = "duration"
            }
            if (!pip.isImage) {
                Surface(shape = RoundedCornerShape(18.dp), tonalElevation = 1.dp,
                    color = MaterialTheme.colorScheme.surface) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()) {
                            Icon(if (pip.muted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                null, tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp))
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Som do vídeo PiP", fontWeight = FontWeight.SemiBold)
                                Text(if (pip.muted) "Silenciado" else "Volume ${(pip.volume * 100).toInt()}%",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall)
                            }
                            Switch(checked = !pip.muted,
                                onCheckedChange = { update(p.copy(pip = pip.copy(muted = !it))) })
                        }
                        if (!pip.muted) Slider(value = pip.volume.coerceIn(0f, 1f),
                            onValueChange = { update(p.copy(pip = pip.copy(volume = it))) },
                            valueRange = 0f..1f)
                    }
                }
            }
            ItemCard(Icons.Default.PermMedia, "Trocar imagem ou vídeo", "Abrir biblioteca de mídia") { choose() }

            if (popup == "position") {
                PipPopup("Alinhar posição", Icons.Default.OpenWith, onDismiss = { popup = null }) {
                    Text("Escolha um alinhamento. O arrasto livre continua disponível na prévia.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val names = listOf("Superior esquerdo", "Superior central", "Superior direito",
                        "Meio esquerdo", "Centro", "Meio direito", "Inferior esquerdo",
                        "Inferior central", "Inferior direito")
                    val icons = listOf(Icons.Default.NorthWest, Icons.Default.North,
                        Icons.Default.NorthEast, Icons.Default.West, Icons.Default.CenterFocusStrong,
                        Icons.Default.East, Icons.Default.SouthWest, Icons.Default.South,
                        Icons.Default.SouthEast)
                    for (row in 0..2) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (col in 0..2) {
                                val i = row * 3 + col
                                OutlinedButton(onClick = {
                                    val (wf, hf) = OverlayEditorLogic.dimensions(
                                        pip.scale, sourceAspect, canvasAspect)
                                    val (x, y) = OverlayEditorLogic.preset(row, col, wf, hf)
                                    update(p.copy(pip = pip.copy(x = x, y = y)))
                                }, modifier = Modifier.weight(1f).height(54.dp)
                                    .semantics { contentDescription = names[i] },
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(0.dp)) {
                                    Icon(icons[i], contentDescription = null)
                                }
                            }
                        }
                    }
                }
            }
            if (popup == "size") {
                var manual by remember(popup) { mutableStateOf((pip.scale * 100).toInt().toString()) }
                PipPopup("Tamanho do PiP", Icons.Default.AspectRatio, onDismiss = { popup = null }) {
                    Text("Tamanho: ${(pip.scale * 100).toInt()}%",
                        fontWeight = FontWeight.SemiBold)
                    val presets = listOf(5, 10, 15, 20, 25, 30, 40, 50, 60, 75, 90)
                    for (row in presets.chunked(4)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            row.forEach { percent ->
                                OutlinedButton(onClick = {
                                    val scale = percent / 100f
                                    val (wf, hf) = OverlayEditorLogic.dimensions(scale, sourceAspect, canvasAspect)
                                    update(p.copy(pip = pip.copy(scale = scale,
                                        x = OverlayEditorLogic.clampCenter(pip.x, wf),
                                        y = OverlayEditorLogic.clampCenter(pip.y, hf))))
                                    manual = percent.toString()
                                }, modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 0.dp),
                                    border = BorderStroke(1.dp, if ((pip.scale * 100).toInt() == percent)
                                        MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)) {
                                    Text("$percent%")
                                }
                            }
                            repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                    Slider(value = pip.scale.coerceIn(.05f, .90f), onValueChange = { value ->
                        val (wf, hf) = OverlayEditorLogic.dimensions(value, sourceAspect, canvasAspect)
                        update(p.copy(pip = pip.copy(scale = value,
                            x = OverlayEditorLogic.clampCenter(pip.x, wf),
                            y = OverlayEditorLogic.clampCenter(pip.y, hf))))
                        manual = (value * 100).toInt().toString()
                    }, valueRange = 0.05f..0.90f)
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = manual, onValueChange = { value ->
                            if (value.length <= 3 && value.all(Char::isDigit)) manual = value
                        }, modifier = Modifier.weight(1f), singleLine = true,
                            label = { Text("Porcentagem manual") }, suffix = { Text("%") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = manual.isNotBlank() && OverlayEditorLogic.parsePercentage(manual) == null)
                        Button(onClick = {
                            OverlayEditorLogic.parsePercentage(manual)?.let { scale ->
                                val (wf, hf) = OverlayEditorLogic.dimensions(scale, sourceAspect, canvasAspect)
                                update(p.copy(pip = pip.copy(scale = scale,
                                    x = OverlayEditorLogic.clampCenter(pip.x, wf),
                                    y = OverlayEditorLogic.clampCenter(pip.y, hf))))
                            }
                        }, enabled = OverlayEditorLogic.parsePercentage(manual) != null) {
                            Icon(Icons.Default.Check, null)
                            Text(" Usar")
                        }
                    }
                    Text("Valores de 5% a 90%. A proporção original é preservada.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (popup == "duration") {
                PipPopup("Tempo na tela", Icons.Default.Schedule, onDismiss = { popup = null }) {
                    val end = (pip.startAtMs + pip.durationMs).coerceAtMost(total)
                    Text("Começa em: ${formatPipMs(pip.startAtMs)}", fontWeight = FontWeight.Medium)
                    Slider(value = pip.startAtMs.coerceIn(0L, end).toFloat(),
                        onValueChange = { value ->
                            val (newStart, newDuration) = OverlayEditorLogic.startChanged(
                                value.toLong(), pip.startAtMs, pip.durationMs, total)
                            update(p.copy(pip = pip.copy(startAtMs = newStart,
                                durationMs = newDuration)))
                        }, valueRange = 0f..end.coerceAtLeast(1L).toFloat())
                    Text("Termina em: ${formatPipMs(end)}", fontWeight = FontWeight.Medium)
                    val minEnd = (pip.startAtMs + 500L).coerceAtMost(total)
                    if (minEnd < total) {
                        Slider(value = end.coerceIn(minEnd, total).toFloat(),
                            onValueChange = { value ->
                                update(p.copy(pip = pip.copy(durationMs =
                                    OverlayEditorLogic.endChanged(value.toLong(), pip.startAtMs, total))))
                            }, valueRange = minEnd.toFloat()..total.toFloat())
                    } else {
                        Text("Limite do vídeo alcançado",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    BoxWithConstraints(Modifier.fillMaxWidth().height(16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = .15f))) {
                        Box(Modifier.offset(x = maxWidth * (pip.startAtMs.toFloat() / total))
                            .width(maxWidth * (pip.durationMs.toFloat() / total))
                            .fillMaxHeight().clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary))
                    }
                    Text("O trecho destacado mostra quando o PiP aparece no vídeo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = { update(p.copy(pip = null)); back() },
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
}

@Composable
private fun PipPopup(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector,
                     onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface,
            tonalElevation = 5.dp, modifier = Modifier.fillMaxWidth().heightIn(max = 620.dp)) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
                    Text(title, style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold)
                }
                content()
                Button(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Icon(Icons.Default.Check, null)
                    Text(" Concluído")
                }
            }
        }
    }
}

private fun formatPipMs(ms: Long): String {
    val minutes = (ms / 60000L)
    val seconds = (ms % 60000L) / 1000L
    val tenths = (ms % 1000L) / 100L
    return "%02d:%02d.%d".format(minutes, seconds, tenths)
}

@Composable
private fun TransitionScreen(p:VideoProject,back:()->Unit,update:(VideoProject)->Unit) {
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal=18.dp)) {
        Heading("Transições",back)
        Spacer(Modifier.height(14.dp))
        Text("Entre os clipes",fontWeight=FontWeight.Bold,fontSize=18.sp)
        Spacer(Modifier.height(12.dp))
        ItemCard(Icons.Default.ContentCut,"Corte direto","Disponível nesta versão") {
            update(p.copy(transition="Corte direto"))
        }
        Spacer(Modifier.height(20.dp))
        Text("Fade e dissolvência ainda não estão disponíveis na exportação desta versão. " +
            "A opção Corte direto preserva o resultado entre todos os vídeos.",
            color=MaterialTheme.colorScheme.onSurfaceVariant)
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

/** TextureView permite compor PiP e linhas-guia por cima do vídeo em Compose. */
private fun previewPlayer(context: android.content.Context): PlayerView =
    android.view.LayoutInflater.from(context)
        .inflate(R.layout.player_preview_texture, null, false) as PlayerView
