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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal fun copyText(context: Context, text: String) {
    (context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager)
        ?.setPrimaryClip(ClipData.newPlainText("Uai Cortes · Publicação", text))
}

internal fun socialCaption(description: String, hashtags: String): String =
    listOf(description.trim(), hashtags.trim()).filter(String::isNotBlank).joinToString("\n\n")

internal fun tryShareVideo(context: Context, publication: SocialPublication): Boolean = runCatching {
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
    val youtubeCache = remember(context) { SocialYouTubeSnapshotCache(context) }
    var data by remember { mutableStateOf<SocialData?>(null) }
    var failure by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    var youtubeBusy by remember { mutableStateOf(false) }
    var youtubeLibraryBusy by remember { mutableStateOf(false) }
    var youtubeLibraryError by remember { mutableStateOf("") }
    var youtubeLibraryVideosByChannel by remember {
        mutableStateOf<Map<String, List<YouTubeSocialApi.PreviousVideo>>>(emptyMap())
    }
    var youtubeLibraryNextPages by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var youtubeChannelStats by remember {
        mutableStateOf<Map<String, YouTubeSocialApi.ChannelStatistics>>(emptyMap())
    }
    // Tokens só em RAM e associados ao canal verificado; nunca entram no SocialStore.
    val youtubeSessionTokens = remember { mutableMapOf<String, String>() }
    DisposableEffect(Unit) { onDispose { youtubeSessionTokens.clear() } }
    var pendingChannelQueue by remember { mutableStateOf<List<SocialAccount>>(emptyList()) }
    var selectedChannelId by rememberSaveable { mutableStateOf("") }
    var manageAccounts by rememberSaveable { mutableStateOf(false) }
    var showHistory by rememberSaveable { mutableStateOf(false) }
    var showSocialHelp by rememberSaveable { mutableStateOf(false) }

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
        if (youtubeLibraryBusy) {
            pendingChannelQueue = emptyList()
            youtubeLibraryBusy = false
            youtubeLibraryError = error
        } else failure = error
    }
    fun fetchOldYouTubeVideos(account: SocialAccount, page: String) {
        val channelId = account.remoteChannelId
        if (youtubeLibraryBusy || youtubeBusy || channelId.isBlank()) return
        val isNextPage = page.isNotBlank()
        youtubeLibraryBusy = true
        youtubeBusy = true
        youtubeLibraryError = ""
        fun withVerifiedToken(token: String, allowAccountPicker: Boolean) {
            scope.launch {
                val channelResult = runCatching { YouTubeSocialApi.channel(token) }
                val channel = channelResult.getOrNull()
                if (channel == null) {
                    youtubeSessionTokens.remove(channelId)
                    youtubeLibraryError = channelResult.exceptionOrNull()?.message
                        ?: "Não foi possível verificar o canal Google."
                    youtubeBusy = false; youtubeLibraryBusy = false
                    pendingChannelQueue = emptyList()
                    return@launch
                }
                if (channel.id != channelId) {
                    youtubeSessionTokens.remove(channelId)
                    if (allowAccountPicker) {
                        // A seleção explícita é apenas fallback para a conta Google errada.
                        authorizeYouTube(true) { selectedToken ->
                            withVerifiedToken(selectedToken, false)
                        }
                    } else {
                        youtubeLibraryError = "A conta Google autorizada não corresponde a ${account.label}. " +
                            "Selecione a conta desse canal para continuar."
                        youtubeBusy = false; youtubeLibraryBusy = false
                        pendingChannelQueue = emptyList()
                    }
                    return@launch
                }
                verifiedYouTubeIds = verifiedYouTubeIds + channelId
                youtubeSessionTokens[channelId] = token
                runCatching {
                    YouTubeSocialApi.previousVideos(token, channelId, if (isNextPage) page else "")
                }.onSuccess { result ->
                    val previous = if (isNextPage) {
                        youtubeLibraryVideosByChannel[channelId].orEmpty()
                    } else emptyList()
                    val enriched = result.videos.map { video ->
                        video.copy(channelId = channelId,
                            channelTitle = video.channelTitle.ifBlank { account.label })
                    }
                    val merged = previous.plus(enriched).distinctBy { it.id }.take(300)
                    youtubeLibraryVideosByChannel = youtubeLibraryVideosByChannel + (channelId to merged)
                    youtubeLibraryNextPages = youtubeLibraryNextPages +
                        (channelId to if (merged.size >= 300) "" else result.nextPageToken)
                    result.channelStatistics?.let { stats ->
                        youtubeChannelStats = youtubeChannelStats + (channelId to stats)
                    }
                    youtubeLibraryError = ""
                    val snapshot = SocialYouTubeSnapshotCache.Snapshot(
                        youtubeLibraryVideosByChannel, youtubeLibraryNextPages, youtubeChannelStats)
                    runCatching { withContext(Dispatchers.IO) { youtubeCache.save(snapshot) } }
                }.onFailure { error ->
                    youtubeSessionTokens.remove(channelId)
                    youtubeLibraryError = error.message ?: "Não foi possível consultar o canal."
                    pendingChannelQueue = emptyList()
                }
                youtubeBusy = false
                youtubeLibraryBusy = false
            }
        }
        val cached = youtubeSessionTokens[channelId]
        if (cached != null) withVerifiedToken(cached, false)
        else authorizeYouTube(false) { token -> withVerifiedToken(token, true) }
    }
    // Ação explícita Atualizar canais: um canal por vez; sem autorização em segundo plano.
    LaunchedEffect(youtubeBusy, pendingChannelQueue) {
        if (!youtubeBusy && pendingChannelQueue.isNotEmpty()) {
            val next = pendingChannelQueue.first()
            pendingChannelQueue = pendingChannelQueue.drop(1)
            fetchOldYouTubeVideos(next, "")
        }
    }
    fun refresh() {
        runCatching { store.load() }.onSuccess { data = it; failure = "" }
            .onFailure { failure = "Não foi possível abrir o catálogo. Os dados anteriores foram preservados." }
    }
    fun persist(next: SocialData) {
        runCatching { store.save(next) }.onSuccess { data = next; failure = "" }
            .onFailure { failure = "Falha ao salvar. Nenhuma alteração foi aplicada." }
    }
    LaunchedEffect(Unit) {
        refresh()
        val saved = withContext(Dispatchers.IO) { youtubeCache.load() }
        val validIds = data?.accounts?.filter { it.destination == SocialDestination.YOUTUBE }
            ?.map { it.remoteChannelId }?.toSet().orEmpty()
        youtubeLibraryVideosByChannel = saved.videos.filterKeys { it in validIds }
        youtubeLibraryNextPages = saved.nextPages.filterKeys { it in validIds }
        youtubeChannelStats = saved.stats.filterKeys { it in validIds }
    }
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
            IconButton(onClick = { showSocialHelp = true }) {
                Icon(Icons.Default.HelpOutline, "Ajuda sobre Redes Sociais")
            }
            IconButton(onClick = onApiCenter) { Icon(Icons.Default.Settings, "Integrações e APIs") }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (failure.isNotBlank()) Text(failure, color = MaterialTheme.colorScheme.error)
            Button(onClick = onCompose, modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp),
                shape = UaiShapes.button, enabled = data != null) {
                Icon(Icons.Default.Upload, null)
                Spacer(Modifier.width(8.dp))
                Text("Publicar vídeo")
            }
            val state = data
            if (state != null) {
                // Controle de contas fora do painel: canais em faixa horizontal no resumo.
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Meus canais e contas (${state.accounts.size})",
                        modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = { manageAccounts = !manageAccounts }) {
                        Text(if (manageAccounts) "Fechar" else "Gerenciar")
                    }
                    IconButton(onClick = { showAddDialog = true },
                        enabled = state.accounts.size < 30 && !youtubeBusy && !metaBusy) {
                        Icon(Icons.Default.Add, "Adicionar conta")
                    }
                }
                SocialYouTubeLibraryUi(
                    accounts = state.accounts,
                    selectedChannelId = selectedChannelId,
                    onSelectChannel = { selectedChannelId = it; youtubeLibraryError = "" },
                    busy = youtubeLibraryBusy || youtubeBusy,
                    videosByChannel = youtubeLibraryVideosByChannel,
                    nextPageTokensByChannel = youtubeLibraryNextPages,
                    channelStatistics = youtubeChannelStats,
                    publications = state.publications,
                    error = youtubeLibraryError,
                    onLoad = ::fetchOldYouTubeVideos,
                    onLoadAll = { channels ->
                        if (!youtubeBusy && channels.isNotEmpty()) {
                            pendingChannelQueue = channels.distinctBy { it.remoteChannelId }
                        }
                    }
                )
                if (manageAccounts) {
                    if (state.accounts.isEmpty()) Text("Adicione um canal para consultar seus vídeos.",
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
                state.accounts.chunked(2).forEach { rowAccounts ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        rowAccounts.forEach { account ->
                            // A lista mantém o estado resumido; ações de conta ficam disponíveis ao expandir.
                            val status = when (account.destination) {
                                SocialDestination.YOUTUBE -> when {
                                    account.remoteChannelId.isBlank() -> "Canal não vinculado"
                                    account.remoteChannelId in verifiedYouTubeIds -> "Verificado nesta sessão"
                                    else -> "Canal cadastrado"
                                }
                                SocialDestination.FACEBOOK_PAGE -> when {
                                    account.remotePageId.isBlank() -> "Página não vinculada"
                                    SocialMetaAuth.pageToken(account.remotePageId) != null -> "Postagem autorizada nesta sessão"
                                    SocialMetaAuth.currentUserToken() != null &&
                                        metaPages.any { it.id == account.remotePageId } -> "Página identificada"
                                    else -> "Página cadastrada"
                                }
                                else -> "Compartilhamento manual"
                            }
                            SocialAccountCard(
                                account = account,
                                status = status,
                                actionLabel = when (account.destination) {
                                    SocialDestination.YOUTUBE ->
                                        if (account.remoteChannelId.isBlank()) "Vincular canal" else "Verificar conta"
                                    SocialDestination.FACEBOOK_PAGE -> "Vincular Página"
                                    else -> null
                                },
                                busy = youtubeBusy || metaBusy,
                                onAction = {
                                    when (account.destination) {
                                        SocialDestination.YOUTUBE -> {
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
                                        }
                                        SocialDestination.FACEBOOK_PAGE -> when {
                                            metaPages.isNotEmpty() -> pageFromMeta = account
                                            SocialMetaAuth.currentUserToken() != null -> consultMetaPages(account)
                                            SocialMetaAuth.isConfigured -> connectMeta(false, account)
                                            else -> pageToBind = account
                                        }
                                        else -> Unit
                                    }
                                },
                                onDelete = { deletingAccount = account },
                                modifier = Modifier.weight(1f),
                                compact = true
                            )
                        }
                        if (rowAccounts.size < 2) Spacer(Modifier.weight(1f))
                    }
                }
                }
                val reviewCount = state.publications.count {
                    it.status == SocialPublicationStatus.UPLOAD_REVIEW ||
                        it.status == SocialPublicationStatus.SHARED_UNCONFIRMED
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Histórico e envios", style = MaterialTheme.typography.titleMedium)
                        Text("${state.publications.size} registros · $reviewCount para conferir",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (reviewCount > 0) MaterialTheme.colorScheme.error else
                                MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = { showHistory = !showHistory }) {
                        Text(if (showHistory) "Recolher" else "Ver histórico")
                        Icon(if (showHistory) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
                    }
                }
                if (showHistory) {
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
                }
                Spacer(Modifier.height(14.dp))
            }
        }
        MainNav("social", onTab)
    }
    if (showSocialHelp) AlertDialog(
        onDismissRequest = { showSocialHelp = false },
        shape = UaiShapes.dialog,
        title = { Text("Sobre o painel Redes") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Todos reúne os vídeos consultados dos canais e os envios confirmados pelo Uai Cortes. " +
                    "As visualizações do canal são totais oficiais do YouTube, diferentes da soma dos vídeos já carregados.")
                Text("Atualizar canais é manual e pode solicitar a escolha da conta Google. " +
                    "Uma autorização válida é reutilizada apenas nesta sessão, depois de conferir o ID do canal.")
                Text("Histórico e envios guarda ações pendentes ou incertas. Nenhum upload será repetido automaticamente.")
            }
        },
        confirmButton = { TextButton(onClick = { showSocialHelp = false }) { Text("Entendi") } }
    )
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

