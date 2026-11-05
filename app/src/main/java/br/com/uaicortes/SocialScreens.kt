package br.com.uaicortes

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.ComponentActivity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import kotlinx.coroutines.launch

private fun copyText(context: Context, text: String) {
    (context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager)
        ?.setPrimaryClip(ClipData.newPlainText("Uai Cortes · Publicação", text))
}

private fun socialCaption(description: String, hashtags: String): String =
    listOf(description.trim(), hashtags.trim()).filter(String::isNotBlank).joinToString("\n\n")

private fun tryShareVideo(context: Context, publication: SocialPublication): Boolean = runCatching {
    val uri = Uri.parse(publication.videoUri)
    require(SocialPublicationPolicy.isShareableVideo(publication.videoUri))
    val caption = socialCaption(publication.description, publication.hashtags)
    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
        type = "video/mp4"
        putExtra(Intent.EXTRA_STREAM, uri)
        if (caption.isNotBlank()) putExtra(Intent.EXTRA_TEXT, caption)
        clipData = ClipData.newUri(context.contentResolver, "Vídeo", uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }, "Escolha o aplicativo para compartilhar"))
}.isSuccess

/** Seletor de apps não confirma publicação; isso só ocorre depois de confirmação manual. */
@Composable
internal fun SocialDashboardScreen(
    onTab: (String) -> Unit,
    onCompose: () -> Unit,
    onApiCenter: () -> Unit
) {
    val context = LocalContext.current
    val store = remember(context) { SocialStore(context) }
    var data by remember { mutableStateOf<SocialData?>(null) }
    var failure by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    var youtubeBusy by remember { mutableStateOf(false) }
    var channelToBind by remember { mutableStateOf<Pair<SocialAccount, YouTubeSocialApi.Channel>?>(null) }
    var pageToBind by remember { mutableStateOf<SocialAccount?>(null) }
    var metaBusy by remember { mutableStateOf(false) }
    var metaPages by remember { mutableStateOf<List<SocialExternalApis.FacebookPage>>(emptyList()) }
    val activity = context as? ComponentActivity
    fun connectMeta(publish: Boolean) {
        if (activity == null) { failure = "Esta tela requer uma Activity Android."; return }
        metaBusy = true
        metaPages = emptyList()
        SocialMetaAuth.start(activity, publish) { success, message ->
            failure = message
            if (success) {
                scope.launch {
                    runCatching {
                        val userToken = SocialMetaAuth.currentUserToken()
                            ?: error("A sessão Meta não retornou autorização válida.")
                        val pages = SocialExternalApis.facebookPages(userToken)
                        SocialMetaAuth.putPageTokens(pages)
                        // Não guardar tokens em estado de UI ou no catálogo.
                        metaPages = pages.map { it.copy(accessToken = "") }
                    }.onFailure { failure = "Não foi possível consultar suas Páginas autorizadas." }
                    metaBusy = false
                }
            } else metaBusy = false
        }
    }

    var confirmedPage by remember { mutableStateOf<Pair<SocialAccount, SocialExternalApis.FacebookPage>?>(null) }
    var pageFromMeta by remember { mutableStateOf<SocialAccount?>(null) }
    val authorizeYouTube = rememberYouTubeAuthorization { error ->
        youtubeBusy = false
        failure = error
    }
    fun refresh() {
        runCatching { store.load() }.onSuccess { data = it; failure = "" }
            .onFailure { failure = "Não foi possível abrir o catálogo. Os dados anteriores foram preservados." }
    }
    fun persist(next: SocialData) {
        runCatching { store.save(next) }.onSuccess { data = next; failure = "" }
            .onFailure { failure = "Falha ao salvar. Nenhuma alteração foi aplicada." }
    }
    LaunchedEffect(Unit) { refresh() }
    var showAccountDialog by remember { mutableStateOf(false) }
    var deletingAccount by remember { mutableStateOf<SocialAccount?>(null) }
    var selectedPost by remember { mutableStateOf<SocialPublication?>(null) }
    var tikTokStatusPost by remember { mutableStateOf<SocialPublication?>(null) }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Redes Sociais", style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold)
                Text("Contas e publicações", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onApiCenter) { Icon(Icons.Default.Settings, "Central de APIs") }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (failure.isNotBlank()) Text(failure, color = MaterialTheme.colorScheme.error)
            Card {
                Column(Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Share, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Publicação assistida", style = MaterialTheme.typography.titleMedium)
                    }
                    Text("Prepare a mesma descrição e hashtags para várias contas. " +
                        "Compartilhamento Android permanece disponível; envios por API exigem " +
                        "permissões válidas e confirmação individual.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = onCompose, modifier = Modifier.fillMaxWidth(),
                        enabled = data != null) { Icon(Icons.Default.Upload, null)
                        Spacer(Modifier.width(8.dp)); Text("Preparar publicação") }
                }
            }
            val state = data
            if (state != null) {
                val metricPosts = state.publications.filter { it.metrics.hasData }
                val views = metricPosts.sumOf { it.metrics.views ?: 0L }
                OutlinedCard {
                    Column(Modifier.fillMaxWidth().padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Desempenho registrado", style = MaterialTheme.typography.titleMedium)
                        Text(if (metricPosts.isEmpty()) "Sem métricas disponíveis" else
                            "$views visualizações informadas · ${metricPosts.size} publicação(ões) com dados",
                            style = MaterialTheme.typography.bodyMedium)
                        Text("Dados manuais ou consultas YouTube autorizadas. Sem sincronização de " +
                            "métricas TikTok, Meta ou Kwai nesta versão.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                OutlinedCard {
                    Column(Modifier.fillMaxWidth().padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text("Conexão Facebook Página", style = MaterialTheme.typography.titleMedium)
                        Text(if (!SocialMetaAuth.isConfigured)
                            "O login oficial precisa de META_APP_ID, META_CLIENT_TOKEN, hash de assinatura e permissões aprovadas. O compartilhamento manual segue disponível."
                        else "Autorização pelo Facebook. O app guarda apenas IDs públicos de Páginas; tokens permanecem em sessão.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { connectMeta(false) },
                                enabled = !metaBusy && SocialMetaAuth.isConfigured) {
                                Text("Conectar Meta")
                            }
                            TextButton(onClick = { connectMeta(true) },
                                enabled = !metaBusy && SocialMetaAuth.isConfigured &&
                                    SocialMetaAuth.currentUserToken() != null) {
                                Text("Autorizar postagem")
                            }
                        }
                        if (metaBusy) LinearProgressIndicator(Modifier.fillMaxWidth())
                        if (metaPages.isNotEmpty()) {
                            Text("Páginas autorizadas (selecione um cadastro abaixo para vincular):",
                                style = MaterialTheme.typography.labelSmall)
                            metaPages.forEach { page ->
                                Text("• ${page.name} (ID ${page.id})",
                                    style = MaterialTheme.typography.bodySmall)
                            }
                            TextButton(onClick = {
                                SocialMetaAuth.disconnect(); metaPages = emptyList()
                                failure = "Sessão Meta desconectada. IDs públicos já cadastrados foram preservados."
                            }) { Text("Desconectar Meta") }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Meus destinos (${state.accounts.size})", modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = { showAccountDialog = true },
                        enabled = state.accounts.size < 30) {
                        Icon(Icons.Default.Add, null); Text("Adicionar")
                    }
                }
                if (state.accounts.isEmpty()) Text("Adicione Facebook Página, Facebook Perfil, " +
                    "YouTube, TikTok ou Kwai. O cadastro local não conecta uma conta.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                state.accounts.forEach { account ->
                    OutlinedCard {
                        Row(Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccountCircle, null,
                                tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(account.label, fontWeight = FontWeight.Medium)
                                Text("${account.destination.label} · " + if (account.destination ==
                                    SocialDestination.FACEBOOK_PAGE &&
                                    SocialMetaAuth.pageToken(account.remotePageId) != null)
                                    "Meta autorizada nesta sessão" else "Cadastro local",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (account.destination == SocialDestination.YOUTUBE) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(if (account.remoteChannelId.isBlank()) "Não vinculado" else "Canal vinculado",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    TextButton(onClick = {
                                        youtubeBusy = true
                                        authorizeYouTube { token ->
                                            scope.launch {
                                                runCatching { YouTubeSocialApi.channel(token) }
                                                    .onSuccess { channel -> channelToBind = account to channel; failure = "" }
                                                    .onFailure { failure = it.message ?: "Falha ao consultar canal." }
                                                youtubeBusy = false
                                            }
                                        }
                                    }, enabled = !youtubeBusy) { Text("Vincular") }
                                }
                            }
                            if (account.destination == SocialDestination.FACEBOOK_PAGE) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(if (account.remotePageId.isBlank()) "Página não vinculada" else "Página vinculada",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    TextButton(onClick = {
                                        if (metaPages.isNotEmpty()) {
                                            // A seleção será feita em diálogo, com verificação do destino.
                                            pageFromMeta = account
                                        } else pageToBind = account
                                    }) { Text("Vincular") }
                                }
                            }
                            IconButton(onClick = { deletingAccount = account }) {
                                Icon(Icons.Default.DeleteOutline, "Excluir destino")
                            }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Publicações (${state.publications.size})", modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = { refresh() }) {
                        Icon(Icons.Default.Refresh, null); Text("Atualizar")
                    }
                }
                if (state.publications.isEmpty()) Text("Ainda não há publicações preparadas.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                state.publications.sortedByDescending { it.updatedAt }.take(80).forEach { publication ->
                    val account = state.accounts.firstOrNull { it.id == publication.accountId }
                    OutlinedCard {
                        Column(Modifier.fillMaxWidth().padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(publication.videoName, fontWeight = FontWeight.Medium,
                                maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text("${account?.label ?: "Conta indisponível"} · ${publication.status.label}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall)
                            if (publication.metrics.hasData) {
                                Text("${publication.metrics.views ?: "—"} visualizações · " +
                                    if (publication.metrics.source == "youtube_api") "YouTube API" else "Dados manuais",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (account?.destination == SocialDestination.YOUTUBE &&
                                publication.status == SocialPublicationStatus.CONFIRMED_BY_API &&
                                publication.remoteVideoId.isNotBlank()) {
                                TextButton(onClick = {
                                    youtubeBusy = true
                                    authorizeYouTube { token ->
                                        scope.launch {
                                            runCatching {
                                                val channel = YouTubeSocialApi.channel(token)
                                                require(channel.id == account.remoteChannelId) {
                                                    "A conta Google não corresponde ao canal vinculado." }
                                                YouTubeSocialApi.metrics(token, publication.remoteVideoId)
                                            }.onSuccess { latest ->
                                                runCatching {
                                                    val state = store.load()
                                                    val next = state.copy(publications = state.publications.map {
                                                        if (it.id == publication.id &&
                                                            it.status == SocialPublicationStatus.CONFIRMED_BY_API)
                                                            it.copy(metrics = latest, updatedAt = System.currentTimeMillis())
                                                        else it
                                                    })
                                                    store.save(next); data = next; failure = ""
                                                }.onFailure { failure = "Falha ao salvar métricas no aparelho." }
                                            }.onFailure { failure = it.message ?: "Métricas indisponíveis." }
                                            youtubeBusy = false
                                        }
                                    }
                                }, enabled = !youtubeBusy) { Text("Atualizar métricas YouTube") }
                            }
                            if (account?.destination == SocialDestination.TIKTOK &&
                                publication.status == SocialPublicationStatus.REMOTE_SUBMITTED &&
                                publication.remoteVideoId.isNotBlank()) {
                                TextButton(onClick = { tikTokStatusPost = publication }) {
                                    Text("Consultar status TikTok")
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(onClick = {
                                    val sent = tryShareVideo(context, publication)
                                    if (sent) persist(state.copy(publications = state.publications.map {
                                        if (it.id == publication.id &&
                                            it.status in setOf(SocialPublicationStatus.PREPARED,
                                                SocialPublicationStatus.SHARED_UNCONFIRMED))
                                            it.copy(status = SocialPublicationStatus.SHARED_UNCONFIRMED,
                                                updatedAt = System.currentTimeMillis()) else it
                                    })) else failure = "Vídeo indisponível. Selecione-o novamente na galeria."
                                }, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.Share, null, Modifier.size(17.dp))
                                    Spacer(Modifier.width(4.dp)); Text("Compartilhar", fontSize = 12.sp)
                                }
                                OutlinedButton(onClick = { selectedPost = publication },
                                    modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.Edit, null, Modifier.size(17.dp))
                                    Spacer(Modifier.width(4.dp)); Text("Detalhes", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
            }
        }
        MainNav("social", onTab)
    }
    if (showAccountDialog) SocialAccountDialog(onDismiss = { showAccountDialog = false },
        onSave = { account ->
            val state = data ?: return@SocialAccountDialog
            persist(state.copy(accounts = state.accounts + account))
            if (failure.isBlank()) showAccountDialog = false
        })
    deletingAccount?.let { target ->
        AlertDialog(onDismissRequest = { deletingAccount = null },
            title = { Text("Remover destino?") },
            text = { Text(if (data?.publications?.any { it.accountId == target.id } == true)
                "Este destino tem publicações no histórico. Elas precisam ser preservadas; " +
                    "a remoção não está disponível." else
                "Remover ${target.label} da lista local? Não altera contas na rede social.") },
            confirmButton = {
                TextButton(onClick = {
                    data?.let { state ->
                        if (state.publications.none { it.accountId == target.id })
                            persist(state.copy(accounts = state.accounts.filterNot { it.id == target.id }))
                    }
                    deletingAccount = null
                }, enabled = data?.publications?.none { it.accountId == target.id } == true) {
                    Text("Remover")
                }
            }, dismissButton = { TextButton(onClick = { deletingAccount = null }) { Text("Cancelar") } })
    }
    channelToBind?.let { (account, channel) ->
        AlertDialog(onDismissRequest = { channelToBind = null },
            title = { Text("Vincular canal YouTube?") },
            text = { Text("Canal: ${channel.title}\nID: ${channel.id}\n\n" +
                "Este ID público será salvo na conta local. O token OAuth não será armazenado.") },
            confirmButton = { TextButton(onClick = {
                data?.let { state ->
                    val conflict = state.accounts.any {
                        it.id != account.id && it.remoteChannelId == channel.id
                    }
                    if (conflict) failure = "Este canal já está vinculado a outra conta."
                    else persist(state.copy(accounts = state.accounts.map {
                        if (it.id == account.id) SocialPublicationPolicy.bindChannel(it, channel.id) else it
                    }))
                }
                channelToBind = null
            }) { Text("Vincular") } },
            dismissButton = { TextButton(onClick = { channelToBind = null }) { Text("Cancelar") } })
    }
    pageFromMeta?.let { account ->
        AlertDialog(onDismissRequest = { pageFromMeta = null },
            title = { Text("Escolha a Página Meta") },
            text = { Column(Modifier.heightIn(max = 350.dp).verticalScroll(rememberScrollState())) {
                metaPages.forEach { page ->
                    TextButton(onClick = { confirmedPage = account to page; pageFromMeta = null },
                        modifier = Modifier.fillMaxWidth()) { Text("${page.name} · ${page.id}") }
                }
            } },
            confirmButton = { TextButton(onClick = { pageFromMeta = null }) { Text("Cancelar") } })
    }
    pageToBind?.let { account ->
        SocialFacebookPageBindDialog(onDismiss = { pageToBind = null },
            onBind = { page ->
                confirmedPage = account to page
                pageToBind = null
            })
    }
    confirmedPage?.let { (account, page) ->
        AlertDialog(onDismissRequest = { confirmedPage = null },
            title = { Text("Vincular Página verificada?") },
            text = { Text("${page.name} · ID ${page.id}\n\n" +
                "O app salva apenas o ID público da Página. O token temporário foi descartado.") },
            confirmButton = { TextButton(onClick = {
                data?.let { state ->
                    if (state.accounts.any { it.id != account.id && it.remotePageId == page.id })
                        failure = "Esta Página já está vinculada a outro cadastro."
                    else persist(state.copy(accounts = state.accounts.map {
                        if (it.id == account.id) SocialPublicationPolicy.bindPage(it, page.id) else it
                    }))
                }
                confirmedPage = null
            }) { Text("Vincular") } },
            dismissButton = { TextButton(onClick = { confirmedPage = null }) { Text("Cancelar") } })
    }
    tikTokStatusPost?.let { post ->
        SocialTikTokStatusDialog(post, onDismiss = { tikTokStatusPost = null })
    }
    selectedPost?.let { current ->
        SocialPublicationDetailDialog(current, onDismiss = { selectedPost = null },
            onSave = { updated ->
                data?.let { state ->
                    persist(state.copy(publications = state.publications.map {
                        if (it.id == updated.id) updated else it
                    }))
                }
                if (failure.isBlank()) selectedPost = null
            })
    }
}

@Composable
private fun SocialAccountDialog(onDismiss: () -> Unit, onSave: (SocialAccount) -> Unit) {
    var selected by remember { mutableStateOf(SocialDestination.FACEBOOK_PAGE) }
    var expanded by remember { mutableStateOf(false) }
    var label by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    val valid = label.trim().length in 1..70 && SocialPublicationPolicy.isSafeHttps(url)
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text("Adicionar destino") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Cadastro local para organizar publicações. Não é um login.",
                    style = MaterialTheme.typography.bodySmall)
                Box {
                    OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(selected.label, modifier = Modifier.weight(1f)); Icon(Icons.Default.ArrowDropDown, null)
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        SocialDestination.entries.forEach { dest ->
                            DropdownMenuItem(text = { Text(dest.label) }, onClick = {
                                selected = dest; expanded = false
                            })
                        }
                    }
                }
                OutlinedTextField(value = label, onValueChange = { label = it.take(70) },
                    modifier = Modifier.fillMaxWidth(), label = { Text("Nome da conta ou Página") },
                    singleLine = true)
                OutlinedTextField(value = url, onValueChange = { url = it.take(300) },
                    modifier = Modifier.fillMaxWidth(), label = { Text("Link HTTPS (opcional)") },
                    singleLine = true, isError = !SocialPublicationPolicy.isSafeHttps(url))
            }
        }, confirmButton = { TextButton(onClick = {
            onSave(SocialAccount(destination = selected, label = label.trim(), profileUrl = url.trim()))
        }, enabled = valid) { Text("Salvar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } })
}

@Composable
private fun SocialPublicationDetailDialog(current: SocialPublication, onDismiss: () -> Unit,
                                          onSave: (SocialPublication) -> Unit) {
    var confirmed by remember(current.id) {
        mutableStateOf(current.status == SocialPublicationStatus.CONFIRMED_MANUALLY)
    }
    var url by remember(current.id) { mutableStateOf(current.postUrl) }
    var views by remember(current.id) { mutableStateOf(current.metrics.views?.toString().orEmpty()) }
    var likes by remember(current.id) { mutableStateOf(current.metrics.likes?.toString().orEmpty()) }
    var comments by remember(current.id) { mutableStateOf(current.metrics.comments?.toString().orEmpty()) }
    var shares by remember(current.id) { mutableStateOf(current.metrics.shares?.toString().orEmpty()) }
    fun parse(s: String) = if (s.isBlank()) null else s.toLongOrNull()?.takeIf { it >= 0 }
    val metricsValid = listOf(views, likes, comments, shares).all {
        it.isBlank() || (it.toLongOrNull()?.let { n -> n >= 0 } == true)
    }
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text("Status e métricas") },
        text = {
            Column(Modifier.heightIn(max = 490.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("O Android não confirma se o compartilhamento foi publicado. " +
                    "Marque apenas após conferir na rede.", style = MaterialTheme.typography.bodySmall)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = confirmed, enabled = current.status != SocialPublicationStatus.CONFIRMED_BY_API,
                        onCheckedChange = { confirmed = it })
                    Text("Confirmo que publiquei")
                }
                OutlinedTextField(value = url, onValueChange = { url = it.take(300) },
                    label = { Text("Link da publicação (opcional)") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    isError = !SocialPublicationPolicy.isSafeHttps(url))
                Text("Métricas informadas manualmente (opcionais)",
                    style = MaterialTheme.typography.labelMedium)
                listOf("Visualizações" to views, "Curtidas" to likes,
                    "Comentários" to comments, "Compartilhamentos" to shares).forEach { (title, value) ->
                    OutlinedTextField(value = value, onValueChange = { input ->
                        val clean = input.filter(Char::isDigit).take(16)
                        when (title) {
                            "Visualizações" -> views = clean
                            "Curtidas" -> likes = clean
                            "Comentários" -> comments = clean
                            else -> shares = clean
                        }
                    }, label = { Text(title) }, singleLine = true,
                        modifier = Modifier.fillMaxWidth())
                }
            }
        }, confirmButton = { TextButton(onClick = {
            val now = System.currentTimeMillis()
            val nextMetrics = SocialMetrics(parse(views), parse(likes), parse(comments), parse(shares),
                if (listOf(views, likes, comments, shares).any { it.isNotBlank() }) now else 0L)
            onSave(current.copy(status = when {
                current.status == SocialPublicationStatus.CONFIRMED_BY_API -> SocialPublicationStatus.CONFIRMED_BY_API
                current.status == SocialPublicationStatus.UPLOAD_REVIEW -> SocialPublicationStatus.UPLOAD_REVIEW
                confirmed -> SocialPublicationStatus.CONFIRMED_MANUALLY
                current.status == SocialPublicationStatus.CONFIRMED_MANUALLY -> SocialPublicationStatus.SHARED_UNCONFIRMED
                else -> current.status
            }, postUrl = if (current.status == SocialPublicationStatus.CONFIRMED_BY_API) current.postUrl else url.trim(),
                metrics = nextMetrics, updatedAt = now))
        }, enabled = SocialPublicationPolicy.isSafeHttps(url) && metricsValid) { Text("Salvar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } })
}

/** Novo vídeo ou MP4 exportado; cria uma tarefa por conta, nunca envia sem ação por destino. */
@Composable
internal fun SocialPublishScreen(initialUri: Uri?, initialName: String,
                                 initialDraft: VideoPublicationDraft?, back: () -> Unit) {
    val context = LocalContext.current
    val store = remember(context) { SocialStore(context) }
    var data by remember { mutableStateOf<SocialData?>(null) }
    var failure by remember { mutableStateOf("") }
    var videoUri by rememberSaveable(initialUri?.toString()) {
        mutableStateOf(initialUri?.toString().orEmpty())
    }
    var videoName by rememberSaveable(initialName) {
        mutableStateOf(initialName.ifBlank { "Vídeo.mp4" })
    }
    var description by rememberSaveable { mutableStateOf(initialDraft?.description.orEmpty()) }
    var hashtags by rememberSaveable { mutableStateOf(initialDraft?.hashtags.orEmpty()) }
    var selectedAccounts by remember { mutableStateOf<Set<String>>(emptySet()) }
    var prepared by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var youtubeBusy by remember { mutableStateOf(false) }
    var youtubeProgress by remember { mutableStateOf("") }
    var uploadToConfirm by remember { mutableStateOf<SocialPublication?>(null) }
    var externalToConfirm by remember { mutableStateOf<SocialPublication?>(null) }
    var uploadPrivacy by remember { mutableStateOf("private") }
    val authorizeYouTube = rememberYouTubeAuthorization { error ->
        failure = error; youtubeBusy = false
    }
    LaunchedEffect(Unit) {
        runCatching { store.load() }.onSuccess { data = it }
            .onFailure { failure = "Não foi possível ler as contas. Catálogo preservado." }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                .onFailure { failure = "O acesso ao arquivo pode expirar; talvez seja necessário selecioná-lo novamente." }
            videoUri = uri.toString()
            videoName = runCatching { mediaName(context, uri) }.getOrDefault("Vídeo.mp4").take(200)
            prepared = false
        }
    }
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        Heading("Publicar nas redes", back)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (failure.isNotBlank()) Text(failure, color = MaterialTheme.colorScheme.error)
            OutlinedCard {
                Column(Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (videoUri.isBlank()) "Escolha o MP4" else videoName,
                        style = MaterialTheme.typography.titleMedium)
                    OutlinedButton(onClick = { picker.launch(arrayOf("video/mp4", "video/*")) }) {
                        Icon(Icons.Default.VideoLibrary, null)
                        Spacer(Modifier.width(6.dp)); Text("Escolher vídeo da galeria/arquivos")
                    }
                }
            }
            Text("Destinos", style = MaterialTheme.typography.titleMedium)
            if (data?.accounts.isNullOrEmpty()) Text("Cadastre seus destinos na aba Redes Sociais antes de preparar a publicação.")
            data?.accounts?.forEach { account ->
                OutlinedCard(onClick = {
                    selectedAccounts = if (account.id in selectedAccounts)
                        selectedAccounts - account.id else selectedAccounts + account.id
                    prepared = false
                }) {
                    Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = account.id in selectedAccounts,
                            onCheckedChange = null)
                        Spacer(Modifier.width(6.dp))
                        Column {
                            Text(account.label)
                            Text(account.destination.label, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            OutlinedTextField(value = description, onValueChange = { description = it.take(3000); prepared = false },
                label = { Text("Descrição comum") }, modifier = Modifier.fillMaxWidth(),
                minLines = 2, maxLines = 4)
            OutlinedTextField(value = hashtags, onValueChange = { hashtags = it.take(1500); prepared = false },
                label = { Text("Hashtags") }, modifier = Modifier.fillMaxWidth(),
                minLines = 1, maxLines = 3)
            OutlinedButton(onClick = { copyText(context, socialCaption(description, hashtags)) },
                enabled = description.isNotBlank() || hashtags.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.ContentCopy, null); Spacer(Modifier.width(6.dp)); Text("Copiar descrição e hashtags")
            }
            Button(onClick = {
                val state = data ?: return@Button
                runCatching {
                    val next = SocialPublicationPolicy.prepare(state, selectedAccounts, videoUri,
                        videoName.take(200), description, hashtags)
                    store.save(next)
                    data = next
                    prepared = true
                    failure = ""
                }.onFailure { failure = "Não foi possível preparar as publicações. Verifique o vídeo e o armazenamento." }
            }, enabled = selectedAccounts.isNotEmpty() &&
                SocialPublicationPolicy.isShareableVideo(videoUri) && data != null,
                modifier = Modifier.fillMaxWidth()) {
                Text("Preparar ${selectedAccounts.size} destino(s)")
            }
            if (prepared) {
                Text("Pronto para publicar por destino", style = MaterialTheme.typography.titleMedium)
                if (youtubeBusy) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text(youtubeProgress.ifBlank { "Aguardando autorização do YouTube..." },
                        style = MaterialTheme.typography.bodySmall)
                }
                Text("Escolha o aplicativo correto no seletor Android. Abrir o seletor não confirma publicação. " +
                    "Algumas redes não recebem a descrição automaticamente: copie o texto quando necessário.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                data?.publications?.filter { it.accountId in selectedAccounts && it.videoUri == videoUri }
                    ?.forEach { publication ->
                        val account = data?.accounts?.firstOrNull { it.id == publication.accountId }
                        if (account?.destination == SocialDestination.YOUTUBE) {
                            when (publication.status) {
                                SocialPublicationStatus.PREPARED, SocialPublicationStatus.SHARED_UNCONFIRMED -> {
                                    Button(onClick = { uploadToConfirm = publication },
                                        enabled = !youtubeBusy && account.remoteChannelId.isNotBlank(),
                                        modifier = Modifier.fillMaxWidth()) {
                                        Icon(Icons.Default.CloudUpload, null)
                                        Spacer(Modifier.width(6.dp)); Text("Enviar ao YouTube pela API")
                                    }
                                    if (account.remoteChannelId.isBlank()) Text(
                                        "Vincule o canal na aba Redes Sociais antes do envio direto.",
                                        style = MaterialTheme.typography.bodySmall)
                                }
                                SocialPublicationStatus.UPLOAD_REVIEW -> Text(
                                    "Upload incerto: confira o YouTube Studio antes de criar outro envio. " +
                                        "Não há repetição automática.",
                                    style = MaterialTheme.typography.bodySmall)
                                SocialPublicationStatus.CONFIRMED_BY_API -> Text(
                                    "YouTube confirmou o vídeo: ${publication.remoteVideoId}",
                                    style = MaterialTheme.typography.bodySmall)
                                else -> Unit
                            }
                        }
                        if (account != null && (account.destination == SocialDestination.FACEBOOK_PAGE ||
                            account.destination == SocialDestination.TIKTOK)) {
                            when (publication.status) {
                                SocialPublicationStatus.PREPARED, SocialPublicationStatus.SHARED_UNCONFIRMED -> {
                                    Button(onClick = { externalToConfirm = publication },
                                        modifier = Modifier.fillMaxWidth(),
                                        enabled = account?.destination != SocialDestination.FACEBOOK_PAGE ||
                                            account?.remotePageId?.isNotBlank() == true) {
                                        Icon(Icons.Default.CloudUpload, null)
                                        Spacer(Modifier.width(6.dp))
                                        Text(if (account?.destination == SocialDestination.FACEBOOK_PAGE)
                                            "Enviar Reel à Página (token temporário)" else
                                            "Enviar ao TikTok (token temporário)")
                                    }
                                    if (account?.destination == SocialDestination.FACEBOOK_PAGE &&
                                        account.remotePageId.isBlank()) Text(
                                        "Vincule a Página no painel Redes Sociais antes do envio direto.",
                                        style = MaterialTheme.typography.bodySmall)
                                }
                                SocialPublicationStatus.UPLOAD_REVIEW -> Text(
                                    "Resultado incerto. Confira a rede antes de tentar de novo.",
                                    style = MaterialTheme.typography.bodySmall)
                                SocialPublicationStatus.REMOTE_SUBMITTED -> Text(
                                    "API recebeu a operação ${publication.remoteVideoId}. " +
                                        "Verifique o processamento e a visibilidade na plataforma.",
                                    style = MaterialTheme.typography.bodySmall)
                                else -> Unit
                            }
                        }
                        OutlinedButton(onClick = {
                            if (tryShareVideo(context, publication)) {
                                data?.let { state ->
                                    runCatching {
                                        val next = state.copy(publications = state.publications.map {
                                            if (it.id == publication.id &&
                                                it.status in setOf(SocialPublicationStatus.PREPARED,
                                                SocialPublicationStatus.SHARED_UNCONFIRMED))
                                                it.copy(status = SocialPublicationStatus.SHARED_UNCONFIRMED,
                                                    updatedAt = System.currentTimeMillis()) else it
                                        })
                                        store.save(next); data = next; failure = ""
                                    }.onFailure { failure = "Compartilhamento aberto, mas o status local não pôde ser salvo." }
                                }
                            } else failure = "Não foi possível abrir o compartilhamento deste vídeo."
                        }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Share, null)
                            Spacer(Modifier.width(6.dp))
                            Text("Compartilhar: ${account?.label.orEmpty()}")
                        }
                    }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
    externalToConfirm?.let { post ->
        val account = data?.accounts?.firstOrNull { it.id == post.accountId }
        if (account != null) SocialExternalPublishDialog(account, post, store,
            onChanged = { data = it; failure = "" },
            onDismiss = { externalToConfirm = null })
        else externalToConfirm = null
    }
    uploadToConfirm?.let { post ->
        AlertDialog(onDismissRequest = { if (!youtubeBusy) uploadToConfirm = null },
            title = { Text("Enviar vídeo ao YouTube?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Você autorizará a conta Google e enviará o vídeo para o canal vinculado. " +
                        "Este envio consome sua internet e a cota de API, e não será repetido automaticamente.")
                    Text(post.videoName, fontWeight = FontWeight.SemiBold)
                    Text("Privacidade", style = MaterialTheme.typography.labelMedium)
                    listOf("private" to "Privado", "unlisted" to "Não listado",
                        "public" to "Público").forEach { (id, label) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = uploadPrivacy == id,
                                onClick = { uploadPrivacy = id })
                            Text(label)
                        }
                    }
                    Text("A confirmação da API significa upload recebido; o processamento e " +
                        "a disponibilidade pública podem demorar.", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = { TextButton(enabled = !youtubeBusy, onClick = {
                val account = data?.accounts?.firstOrNull { it.id == post.accountId }
                if (account == null || account.destination != SocialDestination.YOUTUBE ||
                    account.remoteChannelId.isBlank()) {
                    failure = "Vincule primeiro um canal YouTube válido."
                    uploadToConfirm = null
                } else {
                    uploadToConfirm = null
                    youtubeBusy = true
                    youtubeProgress = "Autorizando canal..."
                    val chosenPrivacy = uploadPrivacy
                    authorizeYouTube { token ->
                        scope.launch {
                            runCatching {
                                val channel = YouTubeSocialApi.channel(token)
                                require(channel.id == account.remoteChannelId) {
                                    "A conta Google atual não corresponde ao canal vinculado."
                                }
                                val snapshot = store.load()
                                val source = snapshot.publications.firstOrNull { it.id == post.id }
                                    ?: error("Publicação não encontrada no catálogo.")
                                val marked = SocialPublicationPolicy.markUploadReview(source)
                                val started = snapshot.copy(publications = snapshot.publications.map {
                                    if (it.id == post.id) marked else it
                                })
                                store.save(started); data = started
                                youtubeProgress = "Enviando vídeo..."
                                val remoteId = YouTubeSocialApi.upload(context, token, marked,
                                    chosenPrivacy) { sent, total ->
                                    scope.launch { youtubeProgress =
                                        "Upload: ${(sent * 100 / total).coerceIn(0L, 100L)}%" }
                                }
                                val latest = store.load()
                                val confirmed = SocialPublicationPolicy.confirmApiUpload(
                                    latest.publications.first { it.id == post.id }, remoteId)
                                val next = latest.copy(publications = latest.publications.map {
                                    if (it.id == post.id) confirmed else it
                                })
                                store.save(next); data = next
                                youtubeProgress = "Upload confirmado pelo YouTube."
                                failure = ""
                            }.onFailure { error ->
                                failure = error.message ?: "Envio não confirmado. Confira o YouTube Studio."
                            }
                            youtubeBusy = false
                        }
                    }
                }
            }) { Text("Autorizar e enviar") } },
            dismissButton = { TextButton(enabled = !youtubeBusy,
                onClick = { uploadToConfirm = null }) { Text("Cancelar") } })
    }
}

/** Importação/exportação APENAS de identificadores públicos (nunca tokens OAuth). */
internal object SocialPublicConfig {
    fun export(data: SocialData): String = JSONObject().put("format", "uai-social-public-v1")
        .put("accounts", JSONArray().also { a -> data.accounts.forEach { account ->
            a.put(JSONObject().put("destination", account.destination.name)
                .put("label", account.label))
        } }).toString(2)

    fun merge(data: SocialData, bytes: ByteArray): SocialData {
        require(bytes.size in 1..100_000)
        val json = JSONObject(String(bytes, Charsets.UTF_8))
        require(json.length() == 2 && json.getString("format") == "uai-social-public-v1")
        val accounts = json.getJSONArray("accounts")
        require(accounts.length() <= 30)
        val merged = data.accounts.toMutableList()
        repeat(accounts.length()) { index ->
            val entry = accounts.getJSONObject(index)
            require(entry.length() == 2) { "Configuração inválida." }
            val dest = requireNotNull(SocialDestination.parse(entry.getString("destination")))
            val label = entry.getString("label").trim()
            require(label.length in 1..70)
            if (merged.none { it.destination == dest && it.label == label })
                merged.add(SocialAccount(id = UUID.randomUUID().toString(), destination = dest,
                    label = label))
        }
        require(merged.size <= 30)
        return data.copy(accounts = merged)
    }
}

@Composable
internal fun SocialApiCenterScreen(back: () -> Unit, openGemini: () -> Unit) {
    val context = LocalContext.current
    val store = remember(context) { SocialStore(context) }
    var feedback by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    var testBusy by remember { mutableStateOf(false) }
    var externalTest by remember { mutableStateOf<SocialDestination?>(null) }
    val authorizeYouTube = rememberYouTubeAuthorization { error ->
        feedback = error; testBusy = false
    }
    val exporter = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) runCatching {
            val bytes = SocialPublicConfig.export(store.load()).toByteArray(Charsets.UTF_8)
            context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(bytes) }
                ?: error("Falha ao criar arquivo")
        }.onSuccess { feedback = "Configuração pública exportada, sem chaves ou tokens." }
            .onFailure { feedback = "Falha ao exportar configuração pública." }
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) runCatching {
            val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
                val buffer = ByteArray(8192)
                val result = java.io.ByteArrayOutputStream()
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    require(result.size() + count <= 100_000) { "Arquivo excede 100 KB." }
                    result.write(buffer, 0, count)
                }
                result.toByteArray()
            } ?: error("Falha ao ler")
            store.save(SocialPublicConfig.merge(store.load(), bytes))
        }.onSuccess { feedback = "Contas locais importadas. Nenhuma conexão OAuth foi configurada." }
            .onFailure { feedback = "Arquivo incompatível ou armazenamento indisponível. Nenhum dado foi substituído." }
    }
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        Heading("Central de APIs e Contas", back)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Conexões de serviços", style = MaterialTheme.typography.titleMedium)
            Text("Chaves de desenvolvedor não equivalem a autorização para publicar na conta. " +
                "YouTube usa autorização Google no Android. Meta Página agora pode iniciar " +
                    "login oficial quando a integração estiver configurada; TikTok requer backend OAuth. " +
                    "O modo avançado por token temporário continua opcional.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            ItemCard(Icons.Default.AutoAwesome, "Google Gemini", "Chaves e testes já existentes") { openGemini() }
            OutlinedCard {
                Column(Modifier.fillMaxWidth().padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Autorização Meta — Facebook Página", fontWeight = FontWeight.SemiBold)
                    Text(if (SocialMetaAuth.isConfigured)
                        "App ID e Client Token públicos configurados no build. Conecte a conta pela aba Redes Sociais."
                    else "Aguardando configuração de META_APP_ID e META_CLIENT_TOKEN no CI e registro do hash da assinatura na Meta. Nenhum App Secret deve ir para o APK.",
                        style = MaterialTheme.typography.bodySmall)
                    Text("TikTok: Login Kit requer domínio HTTPS registrado, certificado do app e um backend seu para trocar o código OAuth por tokens. Sem esses requisitos, o modo avançado por token continua disponível.",
                        style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = {
                        SocialMetaAuth.disconnect()
                        feedback = "Sessão Meta desconectada. Cadastros locais mantidos."
                    }, enabled = SocialMetaAuth.isConfigured &&
                        SocialMetaAuth.currentUserToken() != null) { Text("Desconectar sessão Meta") }
                }
            }

            SocialDestination.entries.forEach { dest ->
                OutlinedCard {
                    Column(Modifier.fillMaxWidth().padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text(dest.label, fontWeight = FontWeight.SemiBold)
                        Text((if (dest == SocialDestination.YOUTUBE) "Autorização por ação · "
                            else "Não conectado · ") + dest.help, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = {
                                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW,
                                    Uri.parse(dest.developerUrl))) }
                                    .onFailure { feedback = "Não há navegador disponível." }
                            }) { Text("Ajuda oficial") }
                            TextButton(onClick = {
                                copyText(context, dest.developerUrl)
                                feedback = "Link oficial copiado."
                            }) { Icon(Icons.Default.ContentCopy, null, Modifier.size(16.dp)); Text("Copiar") }
                        }
                        OutlinedButton(onClick = {
                            if (dest == SocialDestination.FACEBOOK_PAGE || dest == SocialDestination.TIKTOK) {
                                externalTest = dest
                            } else if (dest == SocialDestination.YOUTUBE) {
                                testBusy = true
                                authorizeYouTube { token ->
                                    scope.launch {
                                        runCatching { YouTubeSocialApi.channel(token) }
                                            .onSuccess { feedback = "YouTube autorizado: ${it.title} (${it.id})" }
                                            .onFailure { feedback = it.message ?: "Erro ao consultar canal." }
                                        testBusy = false
                                    }
                                }
                            }
                        }, enabled = (dest == SocialDestination.YOUTUBE ||
                            dest == SocialDestination.FACEBOOK_PAGE || dest == SocialDestination.TIKTOK) && !testBusy) {
                            Icon(Icons.Default.CheckCircleOutline, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp)); Text(when (dest) {
                                SocialDestination.YOUTUBE -> "Testar YouTube com autorização"
                                SocialDestination.FACEBOOK_PAGE, SocialDestination.TIKTOK ->
                                    "Testar token OAuth temporário"
                                else -> "Teste depende de OAuth"
                            })
                        }
                        Text(when (dest) {
                            SocialDestination.YOUTUBE -> "Para publicar, vincule o canal na aba Redes."
                            SocialDestination.FACEBOOK_PAGE -> "Para publicar, vincule uma Página e autorize cada envio."
                            SocialDestination.TIKTOK -> "A autorização e privacidade serão verificadas antes de cada envio."
                            else -> "Sem OAuth, nenhum teste fictício é realizado."
                        },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            HorizontalDivider()
            Text("Backup de cadastros públicos", style = MaterialTheme.typography.titleMedium)
            Text("Exporta e importa somente nomes e tipos de redes. Links privados, " +
                "credenciais, tokens, vídeos e métricas não são exportados.",
                style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { exporter.launch("uai-redes-config.json") },
                    modifier = Modifier.weight(1f)) { Text("Exportar") }
                OutlinedButton(onClick = { importer.launch(arrayOf("application/json", "text/plain")) },
                    modifier = Modifier.weight(1f)) { Text("Importar") }
            }
            if (feedback.isNotBlank()) Text(feedback, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary)
        }
    }
    externalTest?.let { dest ->
        SocialExternalApiTestDialog(dest, onDismiss = { externalTest = null })
    }
}
