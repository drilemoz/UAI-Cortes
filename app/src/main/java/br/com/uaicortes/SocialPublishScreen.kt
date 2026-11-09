package br.com.uaicortes

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.ComponentActivity
import android.content.Intent
import android.net.Uri
import android.media.MediaMetadataRetriever
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
            OutlinedCard(shape = UaiShapes.card) {
                if (videoUri.isBlank()) {
                    Column(Modifier.fillMaxWidth().padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Escolha um vídeo para publicar", style = MaterialTheme.typography.titleMedium)
                        OutlinedButton(onClick = { picker.launch(arrayOf("video/mp4", "video/*")) },
                            enabled = !youtubeBusy) {
                            Icon(Icons.Default.VideoLibrary, null)
                            Spacer(Modifier.width(6.dp)); Text("Escolher vídeo")
                        }
                    }
                } else {
                    Row(Modifier.fillMaxWidth().padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.width(112.dp).height(77.dp).clickable {
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(Uri.parse(videoUri), "video/*")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }) }.onFailure { failure = "Não foi possível abrir a prévia do vídeo." }
                        }, contentAlignment = Alignment.Center) {
                            SocialSelectedVideoThumbnail(videoUri)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(videoName, style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold, maxLines = 2,
                                overflow = TextOverflow.Ellipsis)
                            Text("Toque na imagem para assistir", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = {
                            videoUri = ""; videoName = ""; title = ""; prepared = false
                            failure = ""
                        }, enabled = !youtubeBusy) {
                            Icon(Icons.Default.Close, "Remover vídeo selecionado")
                        }
                    }
                }
            }
            Text("Publicar em", style = MaterialTheme.typography.titleMedium)
            if (data?.accounts.isNullOrEmpty()) Text("Conecte um canal em Configurações → Minhas contas e canais para publicar.")
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
        UaiActionDialog(
            title = "Confirmar envio ao YouTube",
            description = "Confira o destino e a privacidade antes de autorizar o envio.",
            icon = Icons.Default.CloudUpload,
            primaryLabel = "Autorizar e enviar", secondaryLabel = "Cancelar",
            onDismiss = { if (!youtubeBusy) uploadToConfirm = null },
            confirmEnabled = !youtubeBusy,
            onConfirm = {

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
                    authorizeYouTube.forUpload(account.googleAccountEmail.ifBlank { null }) { token ->
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
            }, content = {
            Surface(shape = UaiShapes.item,
                color = MaterialTheme.colorScheme.surfaceVariant) {
                Column(Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(data?.accounts?.firstOrNull { it.id == post.accountId }?.label.orEmpty(),
                        fontWeight = FontWeight.SemiBold)
                    Text("Título: ${post.title}", maxLines = 3)
                    Text("Privacidade: " + when (uploadPrivacy) {
                        "public" -> "Público"; "unlisted" -> "Não listado"; else -> "Privado"
                    })
                }
            }
            Text("O Google poderá solicitar autorização novamente. " +
                "Uma interrupção não provoca reenvio automático.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        })
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


/** Só decodifica o primeiro quadro local; não cria outro item nem envia mídia pela internet. */
@Composable
private fun SocialSelectedVideoThumbnail(uri: String) {
    val context = LocalContext.current
    val bitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, uri) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, Uri.parse(uri))
                    val frame = if (android.os.Build.VERSION.SDK_INT >= 27)
                        retriever.getScaledFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, 256, 160)
                    else retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    frame?.asImageBitmap()
                } finally { retriever.release() }
            }.getOrNull()
        }
    }
    if (bitmap != null) Image(bitmap!!, "Prévia do vídeo selecionado",
        modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Icon(Icons.Default.Movie, "Prévia indisponível")
    }
}
