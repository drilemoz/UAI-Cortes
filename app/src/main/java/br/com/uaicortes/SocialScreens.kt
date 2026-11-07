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
    var confirmedPage by remember { mutableStateOf<Pair<SocialAccount, SocialExternalApis.FacebookPage>?>(null) }
    var showMetaPagePicker by remember { mutableStateOf(false) }
    var newPageToAdd by remember { mutableStateOf<SocialExternalApis.FacebookPage?>(null) }
    var newChannelToAdd by remember { mutableStateOf<YouTubeSocialApi.Channel?>(null) }
    var verifiedYouTubeIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var pageFromMeta by remember { mutableStateOf<SocialAccount?>(null) }
    fun consultMetaPages(accountToLink: SocialAccount? = null) {
        val userToken = SocialMetaAuth.currentUserToken()
        if (userToken == null) { failure = "A autorização Meta expirou."; return }
        metaBusy = true
        scope.launch {
            runCatching {
                val pages = SocialExternalApis.facebookPages(userToken)
                SocialMetaAuth.putPageTokens(pages)
                // Nunca transferir accessToken para Compose ou para SocialStore.
                metaPages = pages.map { it.copy(accessToken = "") }
                if (metaPages.isEmpty()) failure = "Nenhuma Página autorizada encontrada. Confira as permissões concedidas."
                else if (accountToLink != null) pageFromMeta = accountToLink
                else showMetaPagePicker = true
            }.onFailure { failure = "Não foi possível consultar suas Páginas autorizadas." }
            metaBusy = false
        }
    }
    fun connectMeta(publish: Boolean, accountToLink: SocialAccount? = null) {
        if (activity == null) { failure = "Esta tela requer uma Activity Android."; return }
        metaBusy = true
        metaPages = emptyList()
        SocialMetaAuth.start(activity, publish) { success, message ->
            failure = message
            if (success) consultMetaPages(accountToLink) else metaBusy = false
        }
    }
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
    var showAddDialog by remember { mutableStateOf(false) }
    var manualDestination by remember { mutableStateOf<SocialDestination?>(null) }
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
            IconButton(onClick = onApiCenter) { Icon(Icons.Default.Settings, "Integrações e APIs") }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (failure.isNotBlank()) Text(failure, color = MaterialTheme.colorScheme.error)
            Button(onClick = onCompose, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
                shape = UaiShapes.button, enabled = data != null) {
                Icon(Icons.Default.Upload, null)
                Spacer(Modifier.width(8.dp))
                Text("Preparar publicação")
            }
            val state = data
            if (state != null) {
                val metricPosts = state.publications.filter { it.metrics.hasData }
                val views = metricPosts.sumOf { it.metrics.views ?: 0L }
                var metricsExpanded by rememberSaveable { mutableStateOf(false) }
                OutlinedCard(shape = UaiShapes.card) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp)) {
                        Row(Modifier.fillMaxWidth().clickable { metricsExpanded = !metricsExpanded }
                            .heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.BarChart, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Desempenho", fontWeight = FontWeight.Medium)
                                Text(if (metricPosts.isEmpty()) "Sem métricas registradas" else
                                    "$views visualizações · ${metricPosts.size} com dados",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(if (metricsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                "Mostrar ou ocultar detalhes")
                        }
                        if (metricsExpanded) {
                            Text("Métricas manuais ou obtidas do YouTube com autorização. " +
                                "Outras redes não têm sincronização automática nesta versão.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Minhas contas (${state.accounts.size})", modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = { showAddDialog = true },
                        enabled = state.accounts.size < 30 && !youtubeBusy && !metaBusy) {
                        Icon(Icons.Default.Add, null); Text("Adicionar")
                    }
                }
                if (state.accounts.isEmpty()) Text("Conecte YouTube ou Facebook Página; outras redes usam cadastro manual.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                // Controles Meta aparecem apenas quando há Página cadastrada.
                // Configuração técnica e guia ficam na Central de Integrações.
                if (state.accounts.any { it.destination == SocialDestination.FACEBOOK_PAGE }) {
                    var metaExpanded by rememberSaveable { mutableStateOf(false) }
                    OutlinedCard(shape = UaiShapes.card) {
                        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
                            Row(Modifier.fillMaxWidth().clickable { metaExpanded = !metaExpanded }
                                .heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Link, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Text("Conexão Facebook Página", modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium)
                                Icon(if (metaExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    "Expandir conexão")
                            }
                            if (metaExpanded) {
                                Text(when {
                                    !SocialMetaAuth.isConfigured ->
                                        "Configuração Meta pendente. Veja Integrações para preparar o aplicativo."
                                    SocialMetaAuth.currentUserToken() != null ->
                                        "Sessão Meta ativa. Consulte as Páginas para vincular ao cadastro."
                                    else -> "Entre com Facebook para consultar suas Páginas."
                                }, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    TextButton(onClick = {
                                        if (SocialMetaAuth.currentUserToken() != null) consultMetaPages()
                                        else connectMeta(false)
                                    }, enabled = !metaBusy && SocialMetaAuth.isConfigured) {
                                        Text(if (SocialMetaAuth.currentUserToken() != null) "Consultar Páginas" else "Entrar com Facebook")
                                    }
                                    TextButton(onClick = { connectMeta(true) },
                                        enabled = !metaBusy && SocialMetaAuth.isConfigured &&
                                            SocialMetaAuth.currentUserToken() != null) {
                                        Text("Autorizar postagem")
                                    }
                                }
                                if (metaBusy) LinearProgressIndicator(Modifier.fillMaxWidth())
                                if (metaPages.isNotEmpty()) {
                                    Text("${metaPages.size} Página(s) disponíveis. Toque em Vincular na conta desejada.",
                                        style = MaterialTheme.typography.bodySmall)
                                }
                                TextButton(onClick = onApiCenter) { Text("Como configurar Meta") }
                                if (SocialMetaAuth.currentUserToken() != null) {
                                    TextButton(onClick = {
                                        SocialMetaAuth.disconnect()
                                        metaPages = emptyList()
                                        failure = "Sessão Meta encerrada. Cadastros locais preservados."
                                    }) { Text("Desconectar") }
                                }
                            }
                        }
                    }
                }
                state.accounts.forEach { account ->
                    OutlinedCard {
                        Row(Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccountCircle, null,
                                tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(account.label, fontWeight = FontWeight.Medium)
                                val connectionLabel = when (account.destination) {
                                    SocialDestination.YOUTUBE -> when {
                                        account.remoteChannelId.isBlank() -> "Cadastro manual · não vinculado"
                                        account.remoteChannelId in verifiedYouTubeIds -> "Canal verificado nesta sessão"
                                        else -> "Canal cadastrado · autorização necessária ao publicar"
                                    }
                                    SocialDestination.FACEBOOK_PAGE -> when {
                                        account.remotePageId.isBlank() -> "Cadastro manual · não vinculado"
                                        SocialMetaAuth.pageToken(account.remotePageId) != null -> "Publicação autorizada nesta sessão"
                                        SocialMetaAuth.currentUserToken() != null &&
                                            metaPages.any { it.id == account.remotePageId } ->
                                            "Página identificada · publicação não autorizada"
                                        else -> "Página salva · confirmar autorização"
                                    }
                                    else -> "Destino de compartilhamento manual"
                                }
                                Text("${account.destination.label} · $connectionLabel",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                val publicId = when (account.destination) {
                                    SocialDestination.YOUTUBE -> account.remoteChannelId
                                    SocialDestination.FACEBOOK_PAGE -> account.remotePageId
                                    else -> ""
                                }
                                if (publicId.isNotBlank()) Text("ID: $publicId",
                                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (account.destination == SocialDestination.YOUTUBE) {
                                Column(horizontalAlignment = Alignment.End) {
                                    TextButton(onClick = {
                                        youtubeBusy = true
                                        authorizeYouTube(true) { token ->
                                            scope.launch {
                                                runCatching { YouTubeSocialApi.channel(token) }
                                                    .onSuccess { channel ->
                                                        if (account.remoteChannelId.isBlank())
                                                            channelToBind = account to channel
                                                        else if (channel.id == account.remoteChannelId) {
                                                            verifiedYouTubeIds = verifiedYouTubeIds + channel.id
                                                            failure = "Canal ${channel.title} verificado nesta sessão."
                                                        } else failure = "O Google retornou ${channel.title} (${channel.id}), diferente deste destino. Use Adicionar para salvar outro canal."
                                                    }.onFailure { failure = it.message ?: "Falha ao consultar canal." }
                                                youtubeBusy = false
                                            }
                                        }
                                    }, enabled = !youtubeBusy) {
                                        Text(if (account.remoteChannelId.isBlank()) "Vincular" else "Verificar conta")
                                    }
                                }
                            }
                            if (account.destination == SocialDestination.FACEBOOK_PAGE) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(if (account.remotePageId.isBlank()) "Página não vinculada" else "Página vinculada",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    TextButton(onClick = {
                                        when {
                                            metaPages.isNotEmpty() -> pageFromMeta = account
                                            SocialMetaAuth.currentUserToken() != null -> consultMetaPages(account)
                                            SocialMetaAuth.isConfigured -> connectMeta(false, account)
                                            else -> pageToBind = account // Alternativa avançada com token da Página.
                                        }
                                    }) { Text("Vincular") }
                                }
                            }
                            IconButton(onClick = { deletingAccount = account }) {
                                Icon(Icons.Default.DeleteOutline, "Excluir destino")
                            }
                        }
                    }
                }
                SocialPublicationHistoryUi(
                    data = state,
                    busy = youtubeBusy,
                    onRefresh = { refresh() },
                    onDetails = { selectedPost = it },
                    onTikTokStatus = { tikTokStatusPost = it },
                    onShare = { publication ->
                        if (tryShareVideo(context, publication)) {
                            // O compartilhamento Android não prova publicação na rede.
                            val current = data ?: state
                            persist(current.copy(publications = current.publications.map {
                                if (it.id == publication.id && it.status in setOf(
                                        SocialPublicationStatus.PREPARED,
                                        SocialPublicationStatus.SHARED_UNCONFIRMED))
                                    it.copy(status = SocialPublicationStatus.SHARED_UNCONFIRMED,
                                        updatedAt = System.currentTimeMillis())
                                else it
                            }))
                        } else failure = "Vídeo indisponível. Selecione-o novamente na galeria."
                    },
                    onYouTubeMetrics = { publication, account ->
                        youtubeBusy = true
                        authorizeYouTube(true) { token ->
                            scope.launch {
                                runCatching {
                                    val channel = YouTubeSocialApi.channel(token)
                                    require(channel.id == account.remoteChannelId) {
                                        "A conta Google não corresponde ao canal vinculado." }
                                    YouTubeSocialApi.metrics(token, publication.remoteVideoId)
                                }.onSuccess { latest ->
                                    runCatching {
                                        val snapshot = store.load()
                                        val next = snapshot.copy(publications = snapshot.publications.map {
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
                    },
                    onYouTubeStatus = { publication, account ->
                        youtubeBusy = true
                        authorizeYouTube(true) { token ->
                            scope.launch {
                                runCatching {
                                    val channel = YouTubeSocialApi.channel(token)
                                    require(channel.id == account.remoteChannelId) {
                                        "A conta Google não corresponde ao canal vinculado." }
                                    YouTubeSocialApi.videoState(token, publication.remoteVideoId)
                                }.onSuccess { remote ->
                                    runCatching {
                                        val snapshot = store.load()
                                        val next = snapshot.copy(publications = snapshot.publications.map {
                                            if (it.id == publication.id)
                                                SocialPublicationPolicy.applyYouTubeState(it, remote)
                                            else it
                                        })
                                        store.save(next); data = next; failure = ""
                                    }.onFailure { failure = "Falha ao salvar o status no aparelho." }
                                }.onFailure { failure = it.message ?: "Status do vídeo indisponível." }
                                youtubeBusy = false
                            }
                        }
                    }
                )
                Spacer(Modifier.height(14.dp))
            }
        }
        MainNav("social", onTab)
    }
    if (showAddDialog) SocialAddDestinationDialog(
        metaAvailable = SocialMetaAuth.isConfigured,
        onDismiss = { showAddDialog = false },
        onYouTube = {
            showAddDialog = false
            youtubeBusy = true
            authorizeYouTube(true) { token ->
                scope.launch {
                    runCatching { YouTubeSocialApi.channel(token) }
                        .onSuccess { newChannelToAdd = it; failure = "" }
                        .onFailure { failure = it.message ?: "Não foi possível consultar o canal YouTube." }
                    youtubeBusy = false
                }
            }
        },
        onFacebook = {
            showAddDialog = false
            if (SocialMetaAuth.currentUserToken() != null) consultMetaPages()
            else connectMeta(false)
        },
        onManual = { manualDestination = it; showAddDialog = false }
    )
    manualDestination?.let { choice ->
        SocialAccountDialog(initialDestination = choice, onDismiss = { manualDestination = null },
            onSave = { account ->
                val state = data ?: return@SocialAccountDialog
                persist(state.copy(accounts = state.accounts + account))
                if (failure.isBlank()) manualDestination = null
            })
    }
    newChannelToAdd?.let { channel ->
        SocialIdentifiedAccountDialog(
            destination = SocialDestination.YOUTUBE,
            accountName = channel.title,
            publicId = channel.id,
            onDismiss = { newChannelToAdd = null },
            onConfirm = {
                data?.let { current ->
                    runCatching { SocialPublicationPolicy.upsertYouTube(current, channel.id, channel.title) }
                        .onSuccess { next ->
                            persist(next)
                            if (failure.isBlank()) verifiedYouTubeIds = verifiedYouTubeIds + channel.id
                        }.onFailure { failure = it.message ?: "Não foi possível adicionar o canal." }
                }
                newChannelToAdd = null
            })
    }
    if (showMetaPagePicker) AlertDialog(onDismissRequest = { showMetaPagePicker = false },
        title = { Text("Escolher Página Facebook") },
        text = { Column(Modifier.heightIn(max = 350.dp).verticalScroll(rememberScrollState())) {
            metaPages.forEach { page ->
                TextButton(onClick = { newPageToAdd = page; showMetaPagePicker = false },
                    modifier = Modifier.fillMaxWidth()) { Text(page.name) }
            }
        } },
        confirmButton = { TextButton(onClick = { showMetaPagePicker = false }) { Text("Cancelar") } })
    newPageToAdd?.let { page ->
        SocialIdentifiedAccountDialog(
            destination = SocialDestination.FACEBOOK_PAGE,
            accountName = page.name,
            publicId = page.id,
            onDismiss = { newPageToAdd = null },
            onConfirm = {
                data?.let { current ->
                    runCatching { SocialPublicationPolicy.upsertFacebookPage(current, page.id, page.name) }
                        .onSuccess { persist(it) }
                        .onFailure { failure = it.message ?: "Não foi possível adicionar a Página." }
                }
                newPageToAdd = null
            })
    }
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
        SocialIdentifiedAccountDialog(
            destination = SocialDestination.YOUTUBE,
            accountName = channel.title,
            publicId = channel.id,
            linkingExisting = true,
            onDismiss = { channelToBind = null },
            onConfirm = {
                data?.let { state ->
                    val conflict = state.accounts.any {
                        it.id != account.id && it.remoteChannelId == channel.id
                    }
                    if (conflict) failure = "Este canal já está vinculado a outra conta."
                    else {
                        persist(state.copy(accounts = state.accounts.map {
                            if (it.id == account.id) SocialPublicationPolicy.bindChannel(it, channel.id) else it
                        }))
                        if (failure.isBlank()) verifiedYouTubeIds = verifiedYouTubeIds + channel.id
                    }
                }
                channelToBind = null
            })
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
        SocialIdentifiedAccountDialog(
            destination = SocialDestination.FACEBOOK_PAGE,
            accountName = page.name,
            publicId = page.id,
            linkingExisting = true,
            onDismiss = { confirmedPage = null },
            onConfirm = {
                data?.let { state ->
                    if (state.accounts.any { it.id != account.id && it.remotePageId == page.id })
                        failure = "Esta Página já está vinculada a outro cadastro."
                    else persist(state.copy(accounts = state.accounts.map {
                        if (it.id == account.id) SocialPublicationPolicy.bindPage(it, page.id) else it
                    }))
                }
                confirmedPage = null
            })
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
private fun SocialAddDestinationDialog(metaAvailable: Boolean, onDismiss: () -> Unit,
                                       onYouTube: () -> Unit, onFacebook: () -> Unit,
                                       onManual: (SocialDestination) -> Unit) {
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text("Adicionar conta ou destino") },
        text = { Column(Modifier.heightIn(max = 450.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("CONTAS COM CONEXÃO", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(onClick = onYouTube, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.SmartDisplay, null); Spacer(Modifier.width(8.dp))
                Text("Conectar canal YouTube")
            }
            OutlinedButton(onClick = onFacebook, modifier = Modifier.fillMaxWidth(),
                enabled = metaAvailable) {
                Icon(Icons.Default.Pages, null); Spacer(Modifier.width(8.dp))
                Text("Conectar Página Facebook")
            }
            if (!metaAvailable) Text("Facebook Página: configure Meta no build para conectar automaticamente.",
                style = MaterialTheme.typography.bodySmall)
            HorizontalDivider()
            Text("COMPARTILHAMENTO MANUAL", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            listOf(SocialDestination.FACEBOOK_PROFILE, SocialDestination.TIKTOK,
                SocialDestination.KWAI).forEach { dest ->
                TextButton(onClick = { onManual(dest) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Adicionar ${dest.label}")
                }
            }
            TextButton(onClick = { onManual(SocialDestination.FACEBOOK_PAGE) }) {
                Text("Cadastrar Página manualmente (avançado)")
            }
        } },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fechar") } })
}

@Composable
private fun SocialAccountDialog(initialDestination: SocialDestination,
                                onDismiss: () -> Unit, onSave: (SocialAccount) -> Unit) {
    val selected = initialDestination
    var label by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    val valid = label.trim().length in 1..70 && SocialPublicationPolicy.isSafeHttps(url)
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text("Adicionar destino") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Cadastro manual: ${selected.label}. Para YouTube, use Conectar canal.",
                    style = MaterialTheme.typography.bodySmall)
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
    var title by rememberSaveable(initialName) {
        mutableStateOf(suggestedVideoTitle(initialName.ifBlank { "Vídeo.mp4" }))
    }
    var description by rememberSaveable { mutableStateOf(initialDraft?.description.orEmpty()) }
    var hashtags by rememberSaveable { mutableStateOf(initialDraft?.hashtags.orEmpty()) }
    var selectedAccounts by remember { mutableStateOf<Set<String>>(emptySet()) }
    var initializedDestination by remember { mutableStateOf(false) }
    var prepared by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var youtubeBusy by remember { mutableStateOf(false) }
    var youtubeProgress by remember { mutableStateOf("") }
    var activeUploadId by remember { mutableStateOf<String?>(null) }
    var uploadedResult by remember { mutableStateOf<SocialPublication?>(null) }
    var uploadToConfirm by remember { mutableStateOf<SocialPublication?>(null) }
    var externalToConfirm by remember { mutableStateOf<SocialPublication?>(null) }
    var uploadPrivacy by remember { mutableStateOf("private") }
    val authorizeYouTube = rememberYouTubeAuthorization { error ->
        failure = error; youtubeBusy = false; activeUploadId = null
    }
    LaunchedEffect(Unit) {
        runCatching { store.load() }.onSuccess { data = it }
            .onFailure { failure = "Não foi possível ler as contas. Catálogo preservado." }
    }
    LaunchedEffect(data?.accounts, initialUri) {
        if (!initializedDestination && data != null) {
            initializedDestination = true
            val youtubeAccounts = data!!.accounts.filter {
                it.destination == SocialDestination.YOUTUBE && it.remoteChannelId.isNotBlank()
            }
            // Um único canal associado é uma escolha inequívoca; nunca presumir quando há dois.
            if (initialUri != null && data!!.accounts.size == 1 && youtubeAccounts.size == 1)
                selectedAccounts = setOf(youtubeAccounts.single().id)
        }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                .onFailure { failure = "O acesso ao arquivo pode expirar; talvez seja necessário selecioná-lo novamente." }
            videoUri = uri.toString()
            videoName = runCatching { mediaName(context, uri) }.getOrDefault("Vídeo.mp4").take(200)
            title = suggestedVideoTitle(videoName)
            prepared = false
        }
    }
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        Heading("Publicar vídeo", back)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (failure.isNotBlank()) Text(failure, color = MaterialTheme.colorScheme.error)
            OutlinedCard {
                Column(Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (videoUri.isBlank()) "Escolha o MP4" else videoName,
                        style = MaterialTheme.typography.titleMedium)
                    OutlinedButton(onClick = { picker.launch(arrayOf("video/mp4", "video/*")) },
                        enabled = !youtubeBusy) {
                        Icon(Icons.Default.VideoLibrary, null)
                        Spacer(Modifier.width(6.dp)); Text("Escolher vídeo da galeria/arquivos")
                    }
                }
            }
            Text("Publicar em", style = MaterialTheme.typography.titleMedium)
            if (data?.accounts.isNullOrEmpty()) Text("Conecte um canal em Redes → Minhas contas para publicar.")
            data?.accounts?.forEach { account ->
                OutlinedCard(onClick = {
                    if (youtubeBusy) return@OutlinedCard
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
            if (selectedAccounts.any { id -> data?.accounts?.any {
                    it.id == id && it.destination == SocialDestination.YOUTUBE } == true }) {
                OutlinedTextField(value = title, onValueChange = {
                    title = it.take(100); prepared = false
                }, label = { Text("Título no YouTube") },
                    enabled = !youtubeBusy, singleLine = true,
                    modifier = Modifier.fillMaxWidth())
                Text("Privacidade no YouTube", style = MaterialTheme.typography.labelMedium)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    listOf("private" to "Privado", "unlisted" to "Não listado",
                        "public" to "Público").forEach { (id, label) ->
                        FilterChip(selected = uploadPrivacy == id,
                            onClick = { uploadPrivacy = id }, enabled = !youtubeBusy,
                            label = { Text(label, maxLines = 1) })
                    }
                }
            }
            OutlinedTextField(value = description, onValueChange = { description = it.take(3000); prepared = false },
                label = { Text("Descrição") }, modifier = Modifier.fillMaxWidth(),
                enabled = !youtubeBusy,
                minLines = 2, maxLines = 4)
            OutlinedTextField(value = hashtags, onValueChange = { hashtags = it.take(1500); prepared = false },
                label = { Text("Hashtags") }, modifier = Modifier.fillMaxWidth(),
                enabled = !youtubeBusy,
                minLines = 1, maxLines = 3)
            OutlinedButton(onClick = { copyText(context, socialCaption(description, hashtags)) },
                enabled = description.isNotBlank() || hashtags.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.ContentCopy, null); Spacer(Modifier.width(6.dp)); Text("Copiar descrição e hashtags")
            }
            Button(onClick = {
                val state = data ?: return@Button
                runCatching {
                    val next = SocialPublicationPolicy.prepare(state, selectedAccounts, videoUri,
                        videoName.take(200), description, hashtags, title = title)
                    store.save(next)
                    data = next
                    prepared = true
                    failure = ""
                    // Com um único canal YouTube, a confirmação vem diretamente daqui.
                    // Para várias redes, manter as ações individuais e consentimento separado.
                    val onlyId = selectedAccounts.singleOrNull()
                    val onlyAccount = next.accounts.firstOrNull { it.id == onlyId }
                    val onlyPost = next.publications.firstOrNull {
                        it.accountId == onlyId && it.videoUri == videoUri
                    }
                    if (onlyAccount?.destination == SocialDestination.YOUTUBE &&
                        onlyAccount.remoteChannelId.isNotBlank() &&
                        onlyPost?.status == SocialPublicationStatus.PREPARED)
                        uploadToConfirm = onlyPost
                }.onFailure { failure = "Não foi possível preparar as publicações. Verifique o vídeo e o armazenamento." }
            }, enabled = !youtubeBusy && selectedAccounts.isNotEmpty() &&
                SocialPublicationPolicy.isShareableVideo(videoUri) && data != null &&
                title.trim().isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
                Text(if (selectedAccounts.size == 1 && data?.accounts?.any {
                    it.id in selectedAccounts && it.destination == SocialDestination.YOUTUBE } == true)
                    "Continuar para enviar" else "Continuar com ${selectedAccounts.size} destino(s)")
            }
            if (prepared) {
                Text("Envios e compartilhamentos", style = MaterialTheme.typography.titleMedium)
                if (youtubeBusy) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text(youtubeProgress.ifBlank { "Aguardando autorização do YouTube..." },
                        style = MaterialTheme.typography.bodySmall)
                }
                if (selectedAccounts.any { id -> data?.accounts?.any {
                        it.id == id && it.destination != SocialDestination.YOUTUBE } == true })
                    Text("Nas redes com compartilhamento manual, abrir o aplicativo não confirma a postagem.",
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
                                    if (youtubeBusy && activeUploadId == publication.id)
                                        youtubeProgress.ifBlank { "Enviando ao YouTube..." }
                                    else "Envio sem confirmação. Confira o YouTube Studio antes de iniciar outro. " +
                                        "Não há reenvio automático.",
                                    style = MaterialTheme.typography.bodySmall)
                                SocialPublicationStatus.CONFIRMED_BY_API -> Text(
                                    "Recebido pelo YouTube · ID: ${publication.remoteVideoId}. " +
                                        "O processamento e a visibilidade ainda podem demorar.",
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
                        // Não oferecer outro compartilhamento como etapa obrigatória depois do envio.
                        if (publication.status in setOf(SocialPublicationStatus.PREPARED,
                                SocialPublicationStatus.SHARED_UNCONFIRMED)) OutlinedButton(onClick = {
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
            shape = UaiShapes.dialog,
            title = { Text("Confirmar envio ao YouTube") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(data?.accounts?.firstOrNull { it.id == post.accountId }?.label.orEmpty(),
                        fontWeight = FontWeight.SemiBold)
                    Text("Título: ${post.title}")
                    Text("Privacidade: " + when (uploadPrivacy) {
                        "public" -> "Público"; "unlisted" -> "Não listado"; else -> "Privado"
                    })
                    Text("O Google poderá pedir autorização desta conta novamente. " +
                        "Não há reenvio automático após uma interrupção.",
                        style = MaterialTheme.typography.bodySmall)
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
                    authorizeYouTube(true) { token ->
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
                                activeUploadId = post.id
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
                                youtubeProgress = "Envio recebido pelo YouTube."
                                uploadedResult = confirmed
                                failure = ""
                            }.onFailure { error ->
                                failure = error.message ?: "Envio não confirmado. Confira o YouTube Studio."
                            }
                            youtubeBusy = false
                            activeUploadId = null
                        }
                    }
                }
            }) { Text("Autorizar e enviar") } },
            dismissButton = { TextButton(enabled = !youtubeBusy,
                onClick = { uploadToConfirm = null }) { Text("Cancelar") } })
    }
    uploadedResult?.let { confirmed ->
        AlertDialog(onDismissRequest = { uploadedResult = null }, shape = UaiShapes.dialog,
            icon = { Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Vídeo recebido pelo YouTube") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(confirmed.title, fontWeight = FontWeight.SemiBold)
                Text("ID: ${confirmed.remoteVideoId}")
                Text("O YouTube confirmou o recebimento. O processamento e a disponibilidade " +
                    "pública não foram verificados.", style = MaterialTheme.typography.bodySmall)
            } },
            confirmButton = { TextButton(onClick = {
                uploadedResult = null
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://studio.youtube.com/video/${confirmed.remoteVideoId}/edit")))
                }.onFailure { failure = "Não foi possível abrir o YouTube Studio." }
            }) { Text("Abrir Studio") } },
            dismissButton = { TextButton(onClick = { uploadedResult = null }) {
                Text("Concluir")
            } })
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

