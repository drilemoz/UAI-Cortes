package br.com.uaicortes

import android.net.Uri
import android.os.Build
import android.media.MediaMetadataRetriever
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Extraído da MainActivity na v0.1.53 para reutilização de componentes.

internal fun mediaName(context: android.content.Context, uri: Uri): String {
    val cursor = context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
    return cursor?.use { if (it.moveToFirst()) it.getString(0) else "Mídia" } ?: "Mídia"
}

@Composable
internal fun Heading(title: String, back: () -> Unit, accessory: @Composable (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().height(62.dp), horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = back) { Icon(Icons.Default.ArrowBack, "Voltar") }
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Medium)
        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) { accessory?.invoke() }
    }
}

@Composable internal fun BlueButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(onClick, modifier.height(56.dp), enabled = enabled, shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Blue)) { Text(label, fontWeight = FontWeight.SemiBold) }
}

@Composable internal fun ItemCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String,
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

@Composable internal fun MainHome(projects: List<VideoProject>, onCreate: () -> Unit, onAnimate: () -> Unit,
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
            Spacer(Modifier.height(16.dp))
            ItemCard(Icons.Default.AutoAwesomeMotion,"Animar Imagens",
                "Crie um MP4 animado offline com suas fotos") { onAnimate() }
            Spacer(Modifier.height(24.dp)); Text("Edições recentes",fontSize=21.sp, fontWeight=FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            if(projects.isEmpty()) Text("Seus projetos aparecerão aqui.", color=MaterialTheme.colorScheme.onSurfaceVariant,
                modifier=Modifier.padding(16.dp))
            projects.take(5).forEach { ProjectItem(it,onOpen,onDelete) }
        }
        MainNav("home", onTab)
    }
}

@Composable internal fun MainNav(active: String, onTab:(String)->Unit) {
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

@Composable internal fun ProjectItem(p:VideoProject,onOpen:(VideoProject)->Unit,onDelete:(VideoProject)->Unit){
    var menu by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().clickable {onOpen(p)}.padding(vertical=12.dp), verticalAlignment=Alignment.CenterVertically) {
        val context = LocalContext.current
        val first = p.clips.firstOrNull()
        val thumbnail by produceState<ImageBitmap?>(null, first?.uri, first?.startMs, p.id) {
            value = withContext(Dispatchers.IO) {
                first?.let { clip -> runCatching {
                    val retriever = MediaMetadataRetriever()
                    try {
                        retriever.setDataSource(context, Uri.parse(clip.uri))
                        val frameTime = clip.startMs.coerceAtLeast(0L) * 1000L
                        val frame = if (Build.VERSION.SDK_INT >= 27)
                            retriever.getScaledFrameAtTime(frameTime,
                                MediaMetadataRetriever.OPTION_CLOSEST_SYNC, 192, 128)
                        else retriever.getFrameAtTime(frameTime,
                            MediaMetadataRetriever.OPTION_CLOSEST_SYNC)?.let { original ->
                                val scaled = android.graphics.Bitmap.createScaledBitmap(original, 192, 128, true)
                                if (scaled != original) original.recycle()
                                scaled
                            }
                        frame?.asImageBitmap()
                    } finally { retriever.release() }
                }.getOrNull() }
            }
        }
        Box(Modifier.size(70.dp).clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment=Alignment.Center) {
            if (thumbnail != null) Image(thumbnail!!, "Miniatura do projeto ${p.title}",
                Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else Icon(Icons.Default.Videocam,null,tint=MaterialTheme.colorScheme.onSurfaceVariant)
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

@Composable internal fun ProjectsScreen(projects: List<VideoProject>,back:()->Unit,open:(VideoProject)->Unit,delete:(VideoProject)->Unit) {
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal=18.dp)) {
        Heading("Projetos",back)
        if (projects.isEmpty()) Text("Nenhum projeto salvo.",modifier=Modifier.padding(24.dp))
        LazyColumn { items(projects,key={it.id}) { ProjectItem(it,open,delete) } }
    }
}

@Composable internal fun SettingsScreen(back:()->Unit, onClick:(String)->Unit) {
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal=18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Heading("Configurações",back)
        ItemCard(Icons.Default.Tune,"Geral","Qualidade e armazenamento") {onClick("general")}
        ItemCard(Icons.Default.Palette,"Tela","Sistema, claro, escuro e AMOLED") {onClick("appearance")}
        ItemCard(Icons.Default.BugReport,"Diagnóstico","Capturar erros e toques, exportar relatório") {onClick("diagnostics")}
        ItemCard(Icons.Default.Info,"Sobre","Versão e informações do aplicativo") {onClick("about")}
    }
}

@Composable internal fun GeneralScreen(store:ProjectStore,back:()->Unit) {
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
        Text("Alta qualidade H.264: 720p solicita 8 Mb/s; 1080p solicita 18 Mb/s. " +
            "O resultado depende do vídeo original e do codificador do aparelho.",
            style=MaterialTheme.typography.bodySmall,
            color=MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp));Text("Os vídeos são salvos em Filmes/UaiCortes.", color=MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp)); Text("A edição ocorre no aparelho, sem enviar arquivos para a internet.",
            color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable internal fun AppearanceScreen(current:String,set:(String)->Unit,back:()->Unit) {
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

@Composable internal fun AboutScreen(back:()->Unit) {
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal=20.dp),horizontalAlignment=Alignment.CenterHorizontally) {
        Heading("Sobre",back)
        Icon(Icons.Default.MovieCreation,null,tint=Blue,modifier=Modifier.size(70.dp))
        Spacer(Modifier.height(14.dp));Text("Uai Cortes",fontSize=24.sp,fontWeight=FontWeight.Bold)
        Text("Versão 0.1.66 (67)",color=MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp));Text("Editor de vídeo local para Android.",textAlign=androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(14.dp));Text("Novidades: trechos visuais em Cortar e unir, seleção automática após dividir e cursor no último corte.",
            color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
