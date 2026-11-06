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
import androidx.compose.runtime.saveable.rememberSaveable
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
    Button(onClick, modifier.height(56.dp), enabled = enabled, shape = UaiShapes.card,
        colors = ButtonDefaults.buttonColors(containerColor = Blue)) { Text(label, fontWeight = FontWeight.SemiBold) }
}

@Composable internal fun ItemCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String,
                                enabled: Boolean = true, onClick: () -> Unit) {
    Surface(onClick = onClick, enabled = enabled, shape = UaiShapes.card, color = MaterialTheme.colorScheme.surface,
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

/** Fonte visual compartilhada: Home e prévia de validação na tela Sobre. */
@Composable internal fun AppVersionLabel() {
    Text("Versão ${AppReleaseNotes.VERSION_NAME} (${AppReleaseNotes.VERSION_CODE})",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** Ações de tamanho e cor equivalentes: a tela Sobre só mostra a prévia desabilitada. */
@Composable private fun HomeActionTile(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 110.dp),
        shape = UaiShapes.card,
        color = Blue,
        contentColor = Color.White,
        tonalElevation = 0.dp
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 5.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(30.dp),
                tint = Color.White)
            Spacer(Modifier.height(10.dp))
            Text(label, color = Color.White, fontSize = 12.sp,
                lineHeight = 15.sp, fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center, maxLines = 2,
                overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Três atalhos lado a lado, sem mudar os fluxos originais de criação/Flow. */
@Composable internal fun HomePrimaryActions(onCreate: () -> Unit, onAnimate: () -> Unit,
    preview: Boolean = false, onFlow: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top) {
        HomeActionTile("Novo projeto", Icons.Default.AddCircleOutline, onCreate,
            enabled = !preview, modifier = Modifier.weight(1f))
        HomeActionTile("Animar fotos", Icons.Default.AutoAwesomeMotion, onAnimate,
            enabled = !preview, modifier = Modifier.weight(1f))
        HomeActionTile("Estúdio de Cenas", Icons.Default.MovieFilter, onFlow,
            enabled = !preview, modifier = Modifier.weight(1f))
    }
}

/** Acesso rápido às ferramentas sem abrir o editor e sem tocar em projetos. */
@Composable internal fun HomeQuickMediaActions(onSplit: () -> Unit, onExtract: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = onSplit, modifier = Modifier.weight(1f).heightIn(min = 56.dp),
            shape = UaiShapes.button) {
            Icon(Icons.Default.ContentCut, null)
            Spacer(Modifier.width(6.dp))
            Text("Dividir vídeo", maxLines = 2)
        }
        OutlinedButton(onClick = onExtract, modifier = Modifier.weight(1f).heightIn(min = 56.dp),
            shape = UaiShapes.button) {
            Icon(Icons.Default.AudioFile, null)
            Spacer(Modifier.width(6.dp))
            Text("Extrair áudio", maxLines = 2)
        }
    }
}

@Composable internal fun MainHome(projects: List<VideoProject>, onCreate: () -> Unit, onAnimate: () -> Unit,
    onOpen: (VideoProject)->Unit, onDelete: (VideoProject)->Unit, onDeleteAll: ()->Unit,
    onSettings: ()->Unit, onTab:(String)->Unit, onFlow: () -> Unit = {},
    onVoiceCenter: () -> Unit = {}, onQuickSplit: () -> Unit = {},
    onQuickAudio: () -> Unit = {}) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Movie, null, tint = Blue, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Uai Cortes", fontSize = 23.sp, fontWeight = FontWeight.Bold)
                AppVersionLabel()
            }
            IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, "Configurações") }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal=20.dp)) {
            Spacer(Modifier.height(8.dp))
            HomePrimaryActions(onCreate, onAnimate, onFlow = onFlow)
            Spacer(Modifier.height(10.dp))
            ItemCard(Icons.Default.RecordVoiceOver, "Vozes Google",
                "Central de Vozes · catálogo salvo e prévias reutilizáveis",
                onClick = onVoiceCenter)
            Spacer(Modifier.height(12.dp))
            HomeQuickMediaActions(onQuickSplit, onQuickAudio)
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Edições recentes",fontSize=19.sp, fontWeight=FontWeight.SemiBold,
                    modifier=Modifier.weight(1f))
                if (projects.isNotEmpty()) {
                    TextButton(onClick={ onTab("projects") }) { Text("Ver todos") }
                    IconButton(onClick=onDeleteAll) {
                        Icon(Icons.Default.DeleteSweep, "Excluir todos os projetos")
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            if (projects.isEmpty()) {
                Surface(shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)) {
                    Row(Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp))
                        Text("Nenhuma edição ainda. Comece em Novo projeto.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            projects.take(5).forEach { ProjectItem(it,onOpen,onDelete) }
        }
        MainNav("home", onTab)
    }
}

@Composable internal fun MainNav(active: String, onTab:(String)->Unit) {
    Row(Modifier.fillMaxWidth().navigationBarsPadding().height(72.dp)
        .background(MaterialTheme.colorScheme.surface), horizontalArrangement=Arrangement.SpaceEvenly) {
        listOf(Triple("home",Icons.Default.Home,"Início"),Triple("projects",Icons.Default.Folder,"Projetos"),
            Triple("social",Icons.Default.Public,"Redes"),
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
    Row(Modifier.fillMaxWidth().clickable {onOpen(p)}.padding(vertical=12.dp), verticalAlignment=Alignment.CenterVertically) {
        val context = LocalContext.current
        val first = p.clips.firstOrNull()
        val thumbnail by produceState<ImageBitmap?>(null, first?.uri, first?.startMs, p.id) {
            value = withContext(Dispatchers.IO) {
                first?.let { clip -> runCatching {
                    if (clip.isImage) {
                        val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        context.contentResolver.openInputStream(Uri.parse(clip.uri))?.use {
                            android.graphics.BitmapFactory.decodeStream(it, null, bounds)
                        }
                        var sample = 1
                        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 384) sample *= 2
                        context.contentResolver.openInputStream(Uri.parse(clip.uri))?.use {
                            android.graphics.BitmapFactory.decodeStream(it, null,
                                android.graphics.BitmapFactory.Options().apply { inSampleSize = sample })
                        }?.asImageBitmap()
                    } else {
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
                    }
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
        IconButton(onClick={onDelete(p)}) {
            Icon(Icons.Default.DeleteOutline, "Excluir ${p.title}")
        }
    }
    HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant.copy(alpha=0.45f))
}

@Composable internal fun ProjectsScreen(projects: List<VideoProject>,back:()->Unit,open:(VideoProject)->Unit,
    delete:(VideoProject)->Unit, onTab:(String)->Unit) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Column(Modifier.weight(1f).padding(horizontal=18.dp)) {
            Heading("Projetos",back)
            if (projects.isEmpty()) Text("Nenhum projeto salvo.",modifier=Modifier.padding(24.dp))
            LazyColumn { items(projects,key={it.id}) { ProjectItem(it,open,delete) } }
        }
        MainNav("projects",onTab)
    }
}

@Composable internal fun SettingsScreen(back:()->Unit, onClick:(String)->Unit, onTab:(String)->Unit) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())
            .padding(horizontal=18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Heading("Configurações",back)
        ItemCard(Icons.Default.Tune,"Geral","Qualidade e armazenamento") {onClick("general")}
        ItemCard(Icons.Default.Backup,"Salvamento e segurança",
            "Cópias automáticas, restauração e backup externo") { onClick("backup_settings") }
        ItemCard(Icons.Default.Palette,"Tela","Sistema, claro, escuro e AMOLED") {onClick("appearance")}
        ItemCard(Icons.Default.RecordVoiceOver,"Reconhecimento de voz",
            "Instalar modelo offline de legendas em português") { onClick("speech_model") }
        ItemCard(Icons.Default.SettingsEthernet,"Central de APIs e Contas",
            "Gemini, YouTube, Facebook, TikTok e Kwai") { onClick("api_center") }
        ItemCard(Icons.Default.AutoAwesome,"Google Gemini — API de IA",
            "Chave, vozes e narrações") { onClick("gemini_settings") }
        ItemCard(Icons.Default.BugReport,"Diagnóstico","Capturar erros e toques, exportar relatório") {onClick("diagnostics")}
        ItemCard(Icons.Default.AutoAwesome,"Novidades","Veja o que mudou nas últimas versões") { onClick("whats_new") }
        ItemCard(Icons.Default.Info,"Sobre","Versão e informações do aplicativo") {onClick("about")}
        }
        MainNav("settings",onTab)
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

@Composable internal fun AboutScreen(back: () -> Unit, onUpdates: () -> Unit) {
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 20.dp)) {
        Heading("Sobre", back)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(18.dp))
            Icon(Icons.Default.MovieCreation, null,
                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(65.dp))
            Spacer(Modifier.height(12.dp))
            Text("Uai Cortes", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            AppVersionLabel()
            Spacer(Modifier.height(12.dp))
            Text("Editor de vídeos local para Android.",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = onUpdates, shape = RoundedCornerShape(15.dp)) {
                Icon(Icons.Default.SystemUpdate, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Verificar atualizações")
            }
            Spacer(Modifier.height(22.dp))
            ReleaseHighlights(AppReleaseNotes.forAbout())
            Spacer(Modifier.height(24.dp))
        }
    }
}
