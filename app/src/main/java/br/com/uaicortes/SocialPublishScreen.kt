package br.com.uaicortes

import android.content.Intent
import android.net.Uri
import android.media.MediaMetadataRetriever
import android.os.SystemClock
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Uma única escolha de destinos -> confirmação -> fila real -> resultado por canal.
 * Autorizações Google continuam individuais; tokens só vivem em RAM.
 * Upload fica ligado à tela: se o app for encerrado, UPLOAD_REVIEW impede repetição automática.
 */
@Composable
internal fun SocialPublishScreen(initialUri: Uri?, initialName: String,
                                 initialDraft: VideoPublicationDraft?, back: () -> Unit) {
    val context = LocalContext.current
    val store = remember(context) { SocialStore(context) }
    val receiptStore = remember(context) { SocialUploadReceiptStore(context) }
    val scope = rememberCoroutineScope()
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
    var uploadPrivacy by rememberSaveable { mutableStateOf("private") }
    var selectedAccounts by remember { mutableStateOf<Set<String>>(emptySet()) }
    var initializedDestination by remember { mutableStateOf(false) }
    var page by remember { mutableStateOf("edit") }
    var batchPosts by remember { mutableStateOf<List<SocialPublication>>(emptyList()) }
    var rows by remember { mutableStateOf<List<UploadReceipt>>(emptyList()) }
    var pendingAuth by remember { mutableStateOf<CompletableDeferred<String>?>(null) }
    var showBusyExplanation by remember { mutableStateOf(false) }
    var externalToConfirm by remember { mutableStateOf<SocialPublication?>(null) }
    val authorizeYouTube = rememberYouTubeAuthorization { reason ->
        // Sem token, sem upload. A mensagem vai à tela de progresso, não ao topo oculto do formulário.
        val request = pendingAuth
        pendingAuth = null
        if (request?.isActive == true) request.completeExceptionally(IllegalStateException(reason))
        else failure = reason
    }

    fun changeRow(id: String, update: (UploadReceipt) -> UploadReceipt) {
        rows = rows.map { if (it.publicationId == id) update(it) else it }
    }
    fun saveReceipt() {
        runCatching { if (rows.isNotEmpty()) receiptStore.save(rows) }
            .onFailure { failure = "Não foi possível guardar o último relatório. O catálogo de publicações permanece preservado." }
    }
    fun leavePage() {
        if (page == "uploading") showBusyExplanation = true
        else if (page == "edit") back()
        else page = "edit"
    }
    BackHandler(enabled = page != "edit") { leavePage() }

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
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        Heading(when (page) {
            "confirm" -> "Confirmar publicação"
            "uploading" -> "Enviando vídeos"
            "result" -> "Resultado dos envios"
            else -> "Publicar vídeo"
        }, ::leavePage)
        when (page) {
            "edit" -> Column(Modifier.weight(1f).verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (failure.isNotBlank()) Text(failure, color = MaterialTheme.colorScheme.error)
                OutlinedCard(shape = UaiShapes.card) {
                    if (videoUri.isBlank()) {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Escolha um vídeo para publicar", style = MaterialTheme.typography.titleMedium)
                            OutlinedButton(onClick = { picker.launch(arrayOf("video/mp4", "video/*")) }) {
                                Icon(Icons.Default.VideoLibrary, null)
                                Spacer(Modifier.width(6.dp)); Text("Escolher vídeo")
                            }
                        }
                    } else Row(Modifier.fillMaxWidth().padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.width(112.dp).height(77.dp).clickable {
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(Uri.parse(videoUri), "video/*")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }) }.onFailure { failure = "Não foi possível abrir a prévia do vídeo." }
                        }, contentAlignment = Alignment.Center) { SocialSelectedVideoThumbnail(videoUri) }
                        Column(Modifier.weight(1f)) {
                            Text(videoName, style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text("Toque na imagem para assistir", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { videoUri = ""; videoName = ""; title = ""; failure = "" }) {
                            Icon(Icons.Default.Close, "Remover vídeo selecionado")
                        }
                    }
                }
                Text("Publicar em", style = MaterialTheme.typography.titleMedium)
                if (data?.accounts.isNullOrEmpty()) Text(
                    "Conecte um canal em Configurações → Minhas contas e canais para publicar.")
                data?.accounts?.forEach { account ->
                    OutlinedCard(onClick = {
                        selectedAccounts = if (account.id in selectedAccounts)
                            selectedAccounts - account.id else selectedAccounts + account.id
                    }) {
                        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = account.id in selectedAccounts, onCheckedChange = null)
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
                    OutlinedTextField(value = title, onValueChange = { title = it.take(100) },
                        label = { Text("Título no YouTube") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth())
                    Text("Privacidade no YouTube", style = MaterialTheme.typography.labelMedium)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        listOf("private" to "Privado", "unlisted" to "Não listado", "public" to "Público")
                            .forEach { (id, label) ->
                                FilterChip(selected = uploadPrivacy == id, onClick = { uploadPrivacy = id },
                                    label = { Text(label, maxLines = 1) })
                            }
                    }
                }
                OutlinedTextField(value = description, onValueChange = { description = it.take(3000) },
                    label = { Text("Descrição") }, modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 4)
                OutlinedTextField(value = hashtags, onValueChange = { hashtags = it.take(1500) },
                    label = { Text("Hashtags") }, modifier = Modifier.fillMaxWidth(), minLines = 1, maxLines = 3)
                OutlinedButton(onClick = { copyText(context, socialCaption(description, hashtags)) },
                    enabled = description.isNotBlank() || hashtags.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.ContentCopy, null)
                    Spacer(Modifier.width(6.dp)); Text("Copiar descrição e hashtags")
                }
                Button(onClick = {
                    val state = data ?: return@Button
                    runCatching {
                        val next = SocialPublicationPolicy.prepare(state, selectedAccounts,
                            videoUri, videoName.take(200), description, hashtags, title = title)
                        store.save(next)
                        data = next
                        batchPosts = next.accounts.filter { it.id in selectedAccounts }.mapNotNull { account ->
                            next.publications.firstOrNull { it.accountId == account.id && it.videoUri == videoUri }
                        }
                        rows = batchPosts.map { post ->
                            val account = next.accounts.first { it.id == post.accountId }
                            val phase = when (post.status) {
                                SocialPublicationStatus.CONFIRMED_BY_API -> UploadPhase.RECEIVED
                                SocialPublicationStatus.UPLOAD_REVIEW -> UploadPhase.REVIEW
                                SocialPublicationStatus.PREPARED -> if (account.destination == SocialDestination.YOUTUBE)
                                    UploadPhase.WAITING else UploadPhase.MANUAL
                                else -> UploadPhase.MANUAL
                            }
                            UploadReceipt(post.id, account.id, account.label, account.destination,
                                phase, remoteVideoId = post.remoteVideoId)
                        }
                        failure = ""
                        page = "confirm"
                    }.onFailure { failure = "Não foi possível preparar as publicações. Verifique o vídeo e o armazenamento." }
                }, enabled = selectedAccounts.isNotEmpty() && SocialPublicationPolicy.isShareableVideo(videoUri) &&
                    data != null && title.trim().isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
                    Text("Continuar com ${selectedAccounts.size} destino(s)")
                }
                OutlinedButton(onClick = {
                    runCatching { receiptStore.load() }.onSuccess { last ->
                        if (last.isEmpty()) failure = "Ainda não há relatório de envio anterior."
                        else {
                            val snapshot = data
                            rows = last.map { item ->
                                val status = snapshot?.publications?.firstOrNull {
                                    it.id == item.publicationId
                                }?.status
                                if (item.phase == UploadPhase.REVIEW && status == SocialPublicationStatus.PREPARED)
                                    item.copy(phase = UploadPhase.FAILED_BEFORE_UPLOAD,
                                        details = "Interrompido antes de iniciar upload. Nenhum POST foi registrado.")
                                else item
                            }
                            batchPosts = snapshot?.publications?.filter { post ->
                                rows.any { it.publicationId == post.id }
                            }.orEmpty()
                            page = "result"
                            failure = ""
                        }
                    }.onFailure { failure = "Não foi possível ler o último relatório salvo." }
                }, modifier = Modifier.fillMaxWidth()) { Text("Ver último relatório de envios") }
                Spacer(Modifier.height(16.dp))
            }
            "confirm" -> Column(Modifier.weight(1f).verticalScroll(rememberScrollState())
                .padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Confira os destinos antes de iniciar", style = MaterialTheme.typography.titleMedium)
                SocialPublishVideoSummary(videoUri, videoName, title, uploadPrivacy)
                Text("Destinos selecionados (${rows.size})", style = MaterialTheme.typography.titleMedium)
                rows.forEach { item ->
                    OutlinedCard {
                        Column(Modifier.fillMaxWidth().padding(14.dp)) {
                            Text(item.accountName, fontWeight = FontWeight.SemiBold)
                            Text(item.destination.label, style = MaterialTheme.typography.bodySmall)
                            Text(when (item.phase) {
                                UploadPhase.WAITING -> "Pronto para autorizar o envio ao YouTube"
                                UploadPhase.RECEIVED -> "Já recebido pelo YouTube. Não será reenviado."
                                UploadPhase.REVIEW -> "Envio anterior incerto. Não será reenviado."
                                else -> "Publicação nesta rede exige confirmação específica ou compartilhamento manual."
                            }, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Text("O Google pode solicitar permissão para cada canal. Os envios YouTube " +
                    "serão sequenciais e sem repetição automática. Outras redes continuam exigindo " +
                    "seu próprio consentimento.", style = MaterialTheme.typography.bodySmall)
                Button(onClick = {
                    page = "uploading"
                    // Apenas uma confirmação global; a autorização OAuth segue por canal.
                    scope.launch {
                        for (post in batchPosts) {
                            val currentRow = rows.firstOrNull { it.publicationId == post.id } ?: continue
                            if (currentRow.phase != UploadPhase.WAITING) continue
                            val account = data?.accounts?.firstOrNull { it.id == post.accountId }
                            var marked = false
                            val startTime = System.currentTimeMillis()
                            changeRow(post.id) { it.copy(phase = UploadPhase.AUTHORIZING, startedAt = startTime,
                                details = "Solicitando permissão do Google...") }
                            saveReceipt()
                            try {
                                require(account != null && account.destination == SocialDestination.YOUTUBE &&
                                    account.remoteChannelId.isNotBlank()) { "O canal precisa estar vinculado antes de enviar." }
                                val request = CompletableDeferred<String>()
                                pendingAuth = request
                                val token = try {
                                    authorizeYouTube.forUpload(account.googleAccountEmail.ifBlank { null }) { granted ->
                                        if (request.isActive) request.complete(granted)
                                    }
                                    request.await()
                                } finally { pendingAuth = null }
                                changeRow(post.id) { it.copy(phase = UploadPhase.VERIFYING,
                                    details = "Conferindo o canal autorizado...") }
                                val channel = YouTubeSocialApi.channel(token)
                                require(channel.id == account.remoteChannelId) {
                                    "A conta Google autorizada não corresponde ao canal selecionado."
                                }
                                val snapshot = store.load()
                                val source = snapshot.publications.firstOrNull { it.id == post.id }
                                    ?: error("Publicação não encontrada.")
                                // Evita reenvio se a operação já foi iniciada no catálogo.
                                require(source.status == SocialPublicationStatus.PREPARED) {
                                    "Esta publicação já foi enviada ou requer revisão no YouTube Studio."
                                }
                                val uploadSize = withContext(Dispatchers.IO) {
                                    YouTubeSocialApi.uploadSize(context, Uri.parse(source.videoUri))
                                }
                                changeRow(post.id) { it.copy(total = uploadSize) }
                                val review = SocialPublicationPolicy.markUploadReview(source)
                                val prepared = snapshot.copy(publications = snapshot.publications.map {
                                    if (it.id == post.id) review else it
                                })
                                store.save(prepared)
                                data = prepared
                                marked = true
                                changeRow(post.id) { it.copy(phase = UploadPhase.UPLOADING,
                                    details = "Enviando vídeo ao YouTube...") }
                                saveReceipt()
                                val estimator = UploadRateEstimator()
                                val remoteId = YouTubeSocialApi.upload(context, token, review, uploadPrivacy) { sent, total ->
                                    val (rate, seconds) = estimator.update(sent, total, SystemClock.elapsedRealtime())
                                    scope.launch {
                                        changeRow(post.id) { current ->
                                            if (current.phase != UploadPhase.UPLOADING) current
                                            else current.copy(sent = sent, total = total,
                                                speedBytesPerSecond = rate, remainingSeconds = seconds)
                                        }
                                    }
                                }
                                val updated = store.load()
                                val actual = updated.publications.first { it.id == post.id }
                                val confirmed = SocialPublicationPolicy.confirmApiUpload(actual, remoteId)
                                val completed = updated.copy(publications = updated.publications.map {
                                    if (it.id == post.id) confirmed else it
                                })
                                store.save(completed)
                                data = completed
                                changeRow(post.id) { it.copy(phase = UploadPhase.RECEIVED,
                                    sent = it.total.coerceAtLeast(it.sent), remoteVideoId = remoteId,
                                    details = "YouTube confirmou o recebimento. Processamento ainda não verificado.",
                                    finishedAt = System.currentTimeMillis(), remainingSeconds = null) }
                            } catch (error: Exception) {
                                changeRow(post.id) { it.copy(
                                    phase = if (marked) UploadPhase.REVIEW else UploadPhase.FAILED_BEFORE_UPLOAD,
                                    details = (error.message ?: "Falha sem detalhes. Confira o YouTube Studio.").take(200),
                                    finishedAt = System.currentTimeMillis(), remainingSeconds = null) }
                            } finally { saveReceipt() }
                        }
                        saveReceipt()
                        page = "result"
                    }
                }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.CloudUpload, null)
                    Spacer(Modifier.width(8.dp)); Text("Confirmar e iniciar envios")
                }
                OutlinedButton(onClick = { page = "edit" }, modifier = Modifier.fillMaxWidth()) {
                    Text("Voltar e editar")
                }
            }
            "uploading", "result" -> Column(Modifier.weight(1f).verticalScroll(rememberScrollState())
                .padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (failure.isNotBlank()) Text(failure, color = MaterialTheme.colorScheme.error)
                val finished = page == "result"
                Text(if (finished) "Resultado por destino" else "Acompanhe cada destino",
                    style = MaterialTheme.typography.titleMedium)
                rows.forEach { item ->
                    OutlinedCard {
                        Column(Modifier.fillMaxWidth().padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(item.accountName, fontWeight = FontWeight.SemiBold)
                            Text(when (item.phase) {
                                UploadPhase.WAITING -> "Aguardando na fila"
                                UploadPhase.AUTHORIZING -> "Aguardando autorização Google"
                                UploadPhase.VERIFYING -> "Verificando o canal autorizado"
                                UploadPhase.UPLOADING -> "Enviando: ${item.percent}%"
                                UploadPhase.RECEIVED -> "Recebido pelo YouTube"
                                UploadPhase.REVIEW -> "Sem confirmação: verifique no YouTube Studio"
                                UploadPhase.FAILED_BEFORE_UPLOAD -> "Falhou antes do envio"
                                UploadPhase.MANUAL -> "Aguardando publicação manual ou autorização da rede"
                                UploadPhase.REMOTE_ACCEPTED -> "Recebido pela API da rede; confira o processamento"
                            }, style = MaterialTheme.typography.bodySmall)
                            if (item.phase == UploadPhase.UPLOADING || item.phase == UploadPhase.RECEIVED) {
                                LinearProgressIndicator(progress = { if (item.phase == UploadPhase.RECEIVED) 1f
                                    else item.percent.toFloat() / 100f }, modifier = Modifier.fillMaxWidth())
                                Text("${UploadReceiptFormat.bytes(item.sent)} / ${UploadReceiptFormat.bytes(item.total)}",
                                    style = MaterialTheme.typography.bodySmall)
                                if (item.phase == UploadPhase.UPLOADING) {
                                    if (item.speedBytesPerSecond > 0.0)
                                        Text("${UploadReceiptFormat.bytes(item.speedBytesPerSecond.toLong())}/s",
                                            style = MaterialTheme.typography.bodySmall)
                                    Text(UploadReceiptFormat.eta(item.remainingSeconds),
                                        style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            if (item.details.isNotBlank() && item.phase !in setOf(UploadPhase.UPLOADING,
                                UploadPhase.AUTHORIZING, UploadPhase.VERIFYING)) Text(item.details,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (finished && item.phase == UploadPhase.RECEIVED &&
                                SocialPublicationPolicy.validVideoId(item.remoteVideoId) &&
                                item.remoteVideoId.isNotBlank()) {
                                Text("ID: ${item.remoteVideoId}", style = MaterialTheme.typography.bodySmall)
                                OutlinedButton(onClick = {
                                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW,
                                        Uri.parse("https://studio.youtube.com/video/${item.remoteVideoId}/edit"))) }
                                        .onFailure { failure = "Não foi possível abrir o YouTube Studio." }
                                }) { Text("Abrir no YouTube Studio") }
                            }
                            if (finished && item.phase == UploadPhase.REVIEW) {
                                Text("Não tente reenviar automaticamente. Confira o canal antes de outra tentativa.",
                                    style = MaterialTheme.typography.bodySmall)
                                OutlinedButton(onClick = {
                                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW,
                                        Uri.parse("https://studio.youtube.com"))) }
                                }) { Text("Verificar no YouTube Studio") }
                            }
                            if (finished && item.phase == UploadPhase.MANUAL) {
                                val post = batchPosts.firstOrNull { it.id == item.publicationId }
                                if (post != null) OutlinedButton(onClick = {
                                    val account = data?.accounts?.firstOrNull { it.id == post.accountId }
                                    if (account?.destination == SocialDestination.FACEBOOK_PAGE ||
                                        account?.destination == SocialDestination.TIKTOK) externalToConfirm = post
                                    else if (tryShareVideo(context, post)) {
                                        runCatching {
                                            val latest = store.load()
                                            val next = latest.copy(publications = latest.publications.map {
                                                if (it.id == post.id && it.status == SocialPublicationStatus.PREPARED)
                                                    it.copy(status = SocialPublicationStatus.SHARED_UNCONFIRMED,
                                                        updatedAt = System.currentTimeMillis()) else it
                                            })
                                            store.save(next); data = next
                                            changeRow(post.id) { it.copy(details =
                                                "Compartilhamento aberto. A publicação ainda não foi confirmada.") }
                                            saveReceipt()
                                        }
                                    } else failure = "Não foi possível compartilhar este vídeo."
                                }) { Text("Abrir publicação neste destino") }
                            }
                        }
                    }
                }
                val youtubeRows = rows.filter { it.destination == SocialDestination.YOUTUBE }
                if (page == "uploading") {
                    val total = youtubeRows.sumOf { it.total }
                    val sent = youtubeRows.sumOf { it.sent }
                    if (total > 0) {
                        Text("Bytes confirmados nos envios ativos: ${UploadReceiptFormat.bytes(sent)} / " +
                            UploadReceiptFormat.bytes(total), style = MaterialTheme.typography.bodySmall)
                    }
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    Text("Mantenha o Uai Cortes aberto até aparecer o relatório final. " +
                        "Fechar o aplicativo pode interromper o envio; não há reenvio automático.",
                        style = MaterialTheme.typography.bodySmall)
                } else {
                    val received = rows.count { it.phase == UploadPhase.RECEIVED }
                    val uncertain = rows.count { it.phase == UploadPhase.REVIEW }
                    Text("Recebidos: $received · Exigem revisão: $uncertain · Destinos: ${rows.size}",
                        style = MaterialTheme.typography.bodyMedium)
                    Text("Recebido não significa que o YouTube já terminou o processamento ou " +
                        "tornou o vídeo público.", style = MaterialTheme.typography.bodySmall)
                    Button(onClick = { page = "edit" }, modifier = Modifier.fillMaxWidth()) {
                        Text("Concluir")
                    }
                }
            }
        }
    }
    if (showBusyExplanation) AlertDialog(onDismissRequest = { showBusyExplanation = false },
        title = { Text("Envio em andamento") },
        text = { Text("Acompanhe o progresso nesta tela. Sair ou encerrar o aplicativo " +
            "pode interromper o envio; operações incertas não são reenviadas automaticamente.") },
        confirmButton = { TextButton(onClick = { showBusyExplanation = false }) {
            Text("Continuar acompanhando")
        } })
    externalToConfirm?.let { post ->
        val account = data?.accounts?.firstOrNull { it.id == post.accountId }
        if (account != null) SocialExternalPublishDialog(account, post, store,
            onChanged = { latest ->
                data = latest
                failure = ""
                val current = latest.publications.firstOrNull { it.id == post.id }
                if (current?.status == SocialPublicationStatus.REMOTE_SUBMITTED) {
                    changeRow(post.id) { item -> item.copy(phase = UploadPhase.REMOTE_ACCEPTED,
                        remoteVideoId = current.remoteVideoId,
                        details = "Operação recebida pela API. Processamento e publicação não verificados.",
                        finishedAt = System.currentTimeMillis()) }
                    saveReceipt()
                }
            }, onDismiss = { externalToConfirm = null })
        else externalToConfirm = null
    }
}

@Composable
private fun SocialPublishVideoSummary(uri: String, name: String, title: String, privacy: String) {
    OutlinedCard {
        Row(Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(98.dp).height(74.dp), contentAlignment = Alignment.Center) {
                SocialSelectedVideoThumbnail(uri)
            }
            Column(Modifier.weight(1f)) {
                Text(name, maxLines = 2, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                Text("Título: $title", style = MaterialTheme.typography.bodySmall)
                Text("YouTube: " + when (privacy) {
                    "public" -> "Público"; "unlisted" -> "Não listado"; else -> "Privado"
                }, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

/** Miniatura apenas local, sem criar arquivos adicionais nem contatar servidores. */
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
