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
import androidx.compose.ui.graphics.Color
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
    onApiCenter: () -> Unit,
    accountsOnly: Boolean = false
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
    var googleLinkTarget by remember { mutableStateOf<SocialAccount?>(null) }
    var googleLinkInput by remember { mutableStateOf("") }
    var googleLinkBusy by remember { mutableStateOf(false) }
    var selectedChannelId by rememberSaveable { mutableStateOf("") }
    var selectedFacebookPageId by rememberSaveable { mutableStateOf("") }
    var activePlatform by rememberSaveable { mutableStateOf("youtube") }
    var facebookBusy by remember { mutableStateOf(false) }
    var facebookError by remember { mutableStateOf("") }
    var facebookAuthorized by remember { mutableStateOf(SocialMetaAuth.currentUserToken() != null) }
    var facebookPostsByPage by remember {
        mutableStateOf<Map<String, List<FacebookPageVideoPost>>>(emptyMap())
    }
    var facebookNextCursors by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var pendingFacebookPages by remember { mutableStateOf<List<SocialAccount>>(emptyList()) }
    var manageAccounts by rememberSaveable(accountsOnly) { mutableStateOf(accountsOnly) }
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
    // Identificado pela API Google com token apenas em RAM, antes de associar ao canal.
    var newChannelGoogleEmail by remember { mutableStateOf("") }
    var bindChannelGoogleEmail by remember { mutableStateOf("") }
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
        googleLinkBusy = false
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
        fun withVerifiedToken(token: String, needsAssociation: Boolean = false) {
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
                    youtubeLibraryError = "A conta Google vinculada não corresponde a ${account.label}. " +
                        "Corrija o e-mail em Ajustes → Minhas contas e canais."
                    youtubeBusy = false; youtubeLibraryBusy = false
                    pendingChannelQueue = emptyList()
                    return@launch
                }
                if (needsAssociation) {
                    // Nunca supor que a conta selecionada corresponde ao canal salvo.
                    // O ID foi confirmado acima; só então identificar e salvar o e-mail.
                    val emailResult = runCatching { YouTubeGoogleProfileApi.verifiedEmail(token) }
                    val discoveredEmail = emailResult.getOrNull()
                    if (discoveredEmail == null) {
                        youtubeLibraryError = "Não foi possível identificar a conta Google. " +
                            "Autorize o acesso ao e-mail ou vincule a conta em Ajustes."
                        youtubeBusy = false; youtubeLibraryBusy = false
                        pendingChannelQueue = emptyList()
                        return@launch
                    }
                    val saved = runCatching {
                        val latest = store.load()
                        val existing = latest.accounts.firstOrNull { it.id == account.id }
                            ?: error("Canal removido durante a atualização.")
                        require(existing.remoteChannelId == channelId) {
                            "O ID do canal foi alterado durante a verificação."
                        }
                        val updated = latest.copy(accounts = latest.accounts.map {
                            if (it.id == existing.id) it.copy(googleAccountEmail = discoveredEmail)
                            else it
                        })
                        store.save(updated)
                        updated
                    }
                    if (saved.isFailure) {
                        youtubeLibraryError = "O canal foi verificado, mas não foi possível salvar " +
                            "a conta no aparelho. Tente novamente."
                        youtubeBusy = false; youtubeLibraryBusy = false
                        pendingChannelQueue = emptyList()
                        return@launch
                    }
                    data = saved.getOrThrow()
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
        val email = account.googleAccountEmail
        if (email.isBlank()) {
            // Migração assistida: na primeira consulta seleciona a conta e associa
            // automaticamente seu e-mail após verificar o ID exato do canal.
            authorizeYouTube.forUnlinkedChannel { token ->
                withVerifiedToken(token, needsAssociation = true)
            }
        } else {
            val cached = youtubeSessionTokens[channelId]
            if (cached != null) withVerifiedToken(cached)
            else authorizeYouTube.forChannel(email) { token -> withVerifiedToken(token) }
        }
    }
    /** Leitura Meta sob ação explícita: Página e token são verificados na sessão.
     * Não há retry automático, persistência de token ou consulta em segundo plano.
     */
    fun fetchFacebookPosts(account: SocialAccount, cursor: String) {
        val id = account.remotePageId
        if (facebookBusy || metaBusy || !SocialPublicationPolicy.validPageId(id)) return
        if (!SocialMetaAuth.isConfigured) {
            facebookError = "A integração Meta não está configurada neste APK."
            pendingFacebookPages = emptyList()
            return
        }
        val userToken = SocialMetaAuth.currentUserToken()
        if (userToken == null) {
            facebookAuthorized = false
            facebookError = "Entre com Facebook para consultar as publicações da Página."
            pendingFacebookPages = emptyList()
            return
        }
        facebookAuthorized = true
        facebookBusy = true
        facebookError = ""
        scope.launch {
            try {
                val pages = SocialExternalApis.facebookPages(userToken)
                SocialMetaAuth.putPageTokens(pages)
                val page = pages.firstOrNull { it.id == id }
                    ?: error("A Página cadastrada não pertence às Páginas autorizadas nesta sessão.")
                val token = SocialMetaAuth.pageReadToken(id)
                    ?: error("Permissões pages_read_engagement/pages_read_user_content ausentes. Autorize a leitura das Páginas.")
                val pageResult = FacebookPagePostsApi.listVideoPosts(token, id,
                    page.name.ifBlank { account.label }, cursor)
                val old = if (cursor.isEmpty()) emptyList() else facebookPostsByPage[id].orEmpty()
                facebookPostsByPage = facebookPostsByPage + (id to
                    (old + pageResult.posts).distinctBy { it.id }.take(300))
                facebookNextCursors = facebookNextCursors +
                    (id to if (facebookPostsByPage[id].orEmpty().size >= 300) "" else pageResult.nextCursor)
                if (pageResult.posts.isEmpty() && pageResult.consulted > 0 && cursor.isEmpty())
                    facebookError = "Publicações consultadas, mas sem anexos de vídeo acessíveis. Alguns Reels não aparecem na API."
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                facebookError = if (error is IllegalStateException) error.message
                    ?: "Falha ao consultar a Página Facebook." else
                    "Não foi possível consultar as publicações da Página. Confira rede e permissões Meta."
                pendingFacebookPages = emptyList()
            } finally {
                facebookBusy = false
            }
        }
    }
    LaunchedEffect(facebookBusy, pendingFacebookPages) {
        if (!facebookBusy && pendingFacebookPages.isNotEmpty()) {
            val next = pendingFacebookPages.first()
            pendingFacebookPages = pendingFacebookPages.drop(1)
            fetchFacebookPosts(next, "")
        }
    }
    fun connectFacebookReader() {
        if (activity == null || !SocialMetaAuth.isConfigured) {
            facebookError = "Login Meta indisponível. Confira Serviços e APIs."
            return
        }
        facebookBusy = true
        SocialMetaAuth.start(activity, false) { ok, message ->
            facebookBusy = false
            facebookAuthorized = ok
            facebookError = if (ok) "Autorização concluída. Toque em atualizar para consultar suas Páginas." else message
        }
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
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(if (accountsOnly) "Minhas contas" else "Redes Sociais",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold)
                Text(if (accountsOnly) "Canais conectados e autorizações" else "Sua central de publicações",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (accountsOnly) IconButton(onClick = { onTab("settings") }) {
                Icon(Icons.Default.ArrowBack, "Voltar às configurações")
            }
            IconButton(onClick = { showSocialHelp = true }) {
                Icon(Icons.Default.HelpOutline, "Ajuda sobre Redes Sociais")
            }
            IconButton(onClick = onApiCenter) { Icon(Icons.Default.Settings, "Integrações e APIs") }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (failure.isNotBlank()) Text(failure, color = MaterialTheme.colorScheme.error)
            if (!accountsOnly) Surface(
                onClick = onCompose, enabled = data != null, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp), color = Blue, contentColor = Color.White
            ) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(Modifier.size(52.dp).background(Color.White.copy(alpha = 0.16f),
                        RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Upload, null, Modifier.size(27.dp), tint = Color.White)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Publicar vídeo", style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Escolha seu vídeo e os canais de destino",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.88f))
                    }
                    Icon(Icons.Default.ArrowForward, "Abrir publicação de vídeo",
                        Modifier.size(24.dp), tint = Color.White)
                }
            }
            val state = data
            if (state != null) {
                if (accountsOnly) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
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
                if (!accountsOnly) {
                    Row(Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = activePlatform == "youtube",
                            onClick = { activePlatform = "youtube" },
                            label = { Text("YouTube") },
                            leadingIcon = { Icon(Icons.Default.PlayCircleOutline, null,
                                Modifier.size(18.dp)) })
                        FilterChip(selected = activePlatform == "facebook",
                            onClick = { activePlatform = "facebook" },
                            label = { Text("Facebook · Páginas") },
                            leadingIcon = { Icon(Icons.Default.VideoLibrary, null,
                                Modifier.size(18.dp)) })
                    }
                    if (activePlatform == "youtube") SocialYouTubeLibraryUi(
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
                                val distinct = channels.distinctBy { it.remoteChannelId }
                                youtubeLibraryError = ""
                                // Processa também canais de versões antigas sem e-mail vinculado.
                                // Neles, o Google pode pedir uma escolha inicial; não a cada refresh.
                                pendingChannelQueue = distinct
                            }
                        }
                    ) else SocialFacebookPageLibraryUi(
                        accounts = state.accounts,
                        selectedPageId = selectedFacebookPageId,
                        onSelectPage = { selectedFacebookPageId = it; facebookError = "" },
                        videosByPage = facebookPostsByPage,
                        nextCursors = facebookNextCursors,
                        busy = facebookBusy,
                        error = facebookError,
                        configured = SocialMetaAuth.isConfigured,
                        authorized = facebookAuthorized,
                        onAuthorize = ::connectFacebookReader,
                        onLoad = ::fetchFacebookPosts,
                        onLoadAll = { pages ->
                            if (!facebookBusy && pages.isNotEmpty()) {
                                pendingFacebookPages = pages.distinctBy { it.remotePageId }
                            }
                        }
                    )
                }
                if (accountsOnly && manageAccounts) {
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
                                    account.googleAccountEmail.isNotBlank() -> "Conta Google associada"
                                    else -> "Associe uma conta Google"
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
                                        if (account.remoteChannelId.isBlank()) "Vincular canal" else "Trocar conta Google"
                                    SocialDestination.FACEBOOK_PAGE -> "Vincular Página"
                                    else -> null
                                },
                                busy = youtubeBusy || metaBusy,
                                onAction = {
                                    when (account.destination) {
                                        SocialDestination.YOUTUBE -> {
                                            if (account.remoteChannelId.isNotBlank()) {
                                                googleLinkInput = account.googleAccountEmail
                                                googleLinkTarget = account
                                            } else {
                                                youtubeBusy = true
                                                authorizeYouTube.forUnlinkedChannel { token ->
                                                    scope.launch {
                                                        runCatching {
                                                            val channel = YouTubeSocialApi.channel(token)
                                                            val email = YouTubeGoogleProfileApi.verifiedEmail(token)
                                                            channel to email
                                                        }.onSuccess { (channel, email) ->
                                                            bindChannelGoogleEmail = email
                                                            channelToBind = account to channel
                                                        }.onFailure {
                                                            failure = "Não foi possível identificar a conta e o canal Google."
                                                        }
                                                        youtubeBusy = false
                                                    }
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
                if (!accountsOnly) {
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
                        if (account.googleAccountEmail.isBlank()) {
                            failure = "Associe a conta Google desse canal em Minhas contas e canais."
                            youtubeBusy = false
                        } else authorizeYouTube.forChannel(account.googleAccountEmail) { token ->
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
                        if (account.googleAccountEmail.isBlank()) {
                            failure = "Associe a conta Google desse canal em Minhas contas e canais."
                            youtubeBusy = false
                        } else authorizeYouTube.forChannel(account.googleAccountEmail) { token ->
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
        }
        MainNav(if (accountsOnly) "settings" else "social", onTab)
    }
    if (showSocialHelp) AlertDialog(
        onDismissRequest = { showSocialHelp = false },
        shape = UaiShapes.dialog,
        title = { Text("Sobre o painel Redes") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Todos reúne os vídeos consultados dos canais e os envios confirmados pelo Uai Cortes. " +
                    "As visualizações do canal são totais oficiais do YouTube, diferentes da soma dos vídeos já carregados.")
                Text("Ao atualizar pela primeira vez um canal antigo, escolha a conta Google. " +
                    "Depois de verificar o canal, o app salva a associação local e tenta " +
                    "autorizar consultas seguintes sem abrir o seletor novamente.")
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
            authorizeYouTube.forUnlinkedChannel { token ->
                scope.launch {
                    runCatching {
                        val channel = YouTubeSocialApi.channel(token)
                        val email = YouTubeGoogleProfileApi.verifiedEmail(token)
                        channel to email
                    }.onSuccess { (channel, email) ->
                        newChannelToAdd = channel
                        newChannelGoogleEmail = email
                        failure = ""
                    }.onFailure { failure = "Não foi possível identificar a conta e o canal Google." }
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
            onDismiss = { newChannelToAdd = null; newChannelGoogleEmail = "" },
            onConfirm = {
                data?.let { current ->
                    runCatching { SocialPublicationPolicy.upsertYouTube(current, channel.id, channel.title) }
                        .onSuccess { next ->
                            val linked = next.copy(accounts = next.accounts.map {
                                if (it.destination == SocialDestination.YOUTUBE &&
                                    it.remoteChannelId == channel.id)
                                    it.copy(googleAccountEmail = newChannelGoogleEmail)
                                else it
                            })
                            persist(linked)
                            if (failure.isBlank()) verifiedYouTubeIds = verifiedYouTubeIds + channel.id
                        }.onFailure { failure = it.message ?: "Não foi possível adicionar o canal." }
                }
                newChannelToAdd = null
                newChannelGoogleEmail = ""
            })
    }
    googleLinkTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { if (!googleLinkBusy) googleLinkTarget = null },
            shape = UaiShapes.dialog,
            title = { Text("Conta Google deste canal") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Canal: ${target.label}", style = MaterialTheme.typography.titleSmall)
                    Text("Normalmente a conta é identificada automaticamente ao conectar " +
                        "ou atualizar o canal. Use esta opção apenas se precisar trocar ou " +
                        "corrigir uma conta. Nenhum token será salvo.",
                        style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = googleLinkInput,
                        onValueChange = { googleLinkInput = it.take(254) },
                        singleLine = true,
                        label = { Text("E-mail da conta Google") },
                        placeholder = { Text("conta@gmail.com") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !googleLinkBusy
                    )
                    if (googleLinkBusy) LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text("O Google pode pedir consentimento se a permissão de leitura ainda não " +
                        "tiver sido concedida.", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !googleLinkBusy &&
                        YouTubeGoogleAccountEmail.valid(YouTubeGoogleAccountEmail.normalized(googleLinkInput)),
                    onClick = {
                        val normalized = YouTubeGoogleAccountEmail.normalized(googleLinkInput)
                        googleLinkBusy = true
                        authorizeYouTube.forChannel(normalized) { token ->
                            scope.launch {
                                runCatching {
                                    val actual = YouTubeSocialApi.channel(token)
                                    require(actual.id == target.remoteChannelId) {
                                        "A conta escolhida não possui o canal ${target.label}. " +
                                            "Verifique o e-mail informado."
                                    }
                                    val current = store.load()
                                    val latest = current.accounts.firstOrNull { it.id == target.id }
                                        ?: error("Este canal não está mais cadastrado.")
                                    require(latest.remoteChannelId == target.remoteChannelId)
                                    val updated = current.copy(accounts = current.accounts.map {
                                        if (it.id == target.id)
                                            it.copy(googleAccountEmail = normalized)
                                        else it
                                    })
                                    store.save(updated)
                                    data = updated
                                    verifiedYouTubeIds = verifiedYouTubeIds + target.remoteChannelId
                                    youtubeSessionTokens[target.remoteChannelId] = token
                                }.onSuccess {
                                    failure = "Conta Google associada a ${target.label}. " +
                                        "A atualização poderá reutilizar essa conta."
                                    googleLinkTarget = null
                                }.onFailure { failure = it.message ?: "Falha ao associar conta Google." }
                                googleLinkBusy = false
                            }
                        }
                    }
                ) { Text("Verificar e salvar") }
            },
            dismissButton = {
                TextButton(enabled = !googleLinkBusy, onClick = { googleLinkTarget = null }) {
                    Text("Cancelar")
                }
            }
        )
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
                        if (state.publications.none { it.accountId == target.id }) {
                            persist(state.copy(accounts = state.accounts.filterNot { it.id == target.id }))
                            youtubeSessionTokens.remove(target.remoteChannelId)
                        }
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
            onDismiss = { channelToBind = null; bindChannelGoogleEmail = "" },
            onConfirm = {
                data?.let { state ->
                    val conflict = state.accounts.any {
                        it.id != account.id && it.remoteChannelId == channel.id
                    }
                    if (conflict) failure = "Este canal já está vinculado a outra conta."
                    else {
                        val updated = state.copy(accounts = state.accounts.map {
                            if (it.id == account.id) SocialPublicationPolicy.bindChannel(it, channel.id)
                                .copy(googleAccountEmail = bindChannelGoogleEmail) else it
                        })
                        persist(updated)
                        if (failure.isBlank()) {
                            verifiedYouTubeIds = verifiedYouTubeIds + channel.id
                            bindChannelGoogleEmail = ""
                        }
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

