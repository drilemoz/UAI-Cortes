package br.com.uaicortes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/** Token copiado de OAuth autorizado apenas na memória deste diálogo; nunca salvo, copiado ou logado. */
@Composable
internal fun SocialFacebookPageBindDialog(
    onDismiss: () -> Unit, onBind: (SocialExternalApis.FacebookPage) -> Unit
) {
    var token by remember { mutableStateOf("") }
    var page by remember { mutableStateOf<SocialExternalApis.FacebookPage?>(null) }
    var busy by remember { mutableStateOf(false) }
    var failure by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    AlertDialog(onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("Identificar Página do Facebook") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Modo avançado. Informe um token de acesso DA PÁGINA, autorizado na Meta com " +
                "pages_manage_posts, pages_read_engagement e pages_show_list. " +
                "Ele não é salvo no aparelho; nunca informe o App Secret.",
                style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(value = token, onValueChange = { token = it.trim().take(4096); page = null },
                modifier = Modifier.fillMaxWidth(), label = { Text("Token temporário da Página") },
                visualTransformation = PasswordVisualTransformation(), maxLines = 1)
            if (page != null) {
                Text("Página identificada: ${page!!.name} (ID ${page!!.id})", fontWeight = FontWeight.Medium)
                Text("Confirme que esta é a Página que deseja associar a este destino local.",
                    style = MaterialTheme.typography.bodySmall)
            }
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (failure.isNotBlank()) Text(failure, color = MaterialTheme.colorScheme.error)
        } },
        confirmButton = {
            if (page != null) TextButton(onClick = {
                page?.let(onBind)
                token = ""
            }, enabled = !busy) { Text("Vincular Página") }
            else TextButton(onClick = {
                busy = true; failure = ""
                scope.launch {
                    runCatching { SocialExternalApis.facebookPage(token) }
                        .onSuccess { page = it; failure = "" }
                        .onFailure { failure = it.message ?: "Falha ao verificar a Página." }
                    busy = false
                }
            }, enabled = !busy && token.length >= 10) { Text("Testar token") }
        },
        dismissButton = { TextButton(onClick = { token = ""; onDismiss() }, enabled = !busy) {
            Text("Cancelar")
        } }
    )
}

@Composable
internal fun SocialExternalApiTestDialog(destination: SocialDestination, onDismiss: () -> Unit) {
    var token by remember(destination) { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    AlertDialog(onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("Testar acesso · ${destination.label}") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Requer um token OAuth já autorizado pela plataforma. O teste apenas consulta " +
                "a conta; não envia vídeos. Token mantido só nesta janela e descartado ao fechar.",
                style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(value = token, onValueChange = { token = it.trim().take(4096) },
                label = { Text("Token autorizado") }, modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(), singleLine = true)
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (feedback.isNotBlank()) Text(feedback, style = MaterialTheme.typography.bodySmall)
        } },
        confirmButton = { TextButton(enabled = !busy && token.length >= 10, onClick = {
            busy = true; feedback = "Consultando API oficial..."
            scope.launch {
                runCatching {
                    when (destination) {
                        SocialDestination.FACEBOOK_PAGE -> SocialExternalApis.facebookPage(token).let {
                            "Página: ${it.name} · ID ${it.id} (somente consulta)"
                        }
                        SocialDestination.TIKTOK -> SocialExternalApis.tikTokCreator(token).let {
                            "Criador: ${it.name} · @${it.username} · ${it.privacyOptions.size} opções de privacidade"
                        }
                        else -> error("Integração não disponível para este serviço.")
                    }
                }.onSuccess { feedback = it }.onFailure { feedback = it.message ?: "Acesso não autorizado." }
                token = ""; busy = false
            }
        }) { Text("Testar") } },
        dismissButton = { TextButton(onClick = { token = ""; onDismiss() }, enabled = !busy) {
            Text("Fechar")
        } })
}

/** Preflight, consentimento por destino e gravação antes do primeiro POST. */
@Composable
internal fun SocialExternalPublishDialog(
    account: SocialAccount, post: SocialPublication, store: SocialStore,
    onChanged: (SocialData) -> Unit, onDismiss: () -> Unit
) {
    var token by remember(post.id) { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var confirmedIdentity by remember { mutableStateOf(false) }
    var verifiedPage by remember { mutableStateOf<SocialExternalApis.FacebookPage?>(null) }
    var creator by remember { mutableStateOf<SocialExternalApis.TikTokCreator?>(null) }
    var privacy by remember { mutableStateOf("") }
    var aiGenerated by remember { mutableStateOf(false) }
    var disableComment by remember { mutableStateOf(false) }
    var disableDuet by remember { mutableStateOf(false) }
    var disableStitch by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf("") }
    var failure by remember { mutableStateOf("") }
    var completed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val isPage = account.destination == SocialDestination.FACEBOOK_PAGE
    val captionLength = listOf(post.description, post.hashtags)
        .filter(String::isNotBlank).joinToString("\n\n").length
    val verified = if (isPage) verifiedPage != null else creator != null
    val canPublish = verified && confirmedIdentity && (isPage || privacy.isNotBlank()) &&
        captionLength <= 2200 && !busy && !completed
    AlertDialog(onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(if (isPage) "Enviar Reel à Página" else "Enviar vídeo ao TikTok") },
        text = { Column(Modifier.heightIn(max = 500.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Destino local: ${account.label}", fontWeight = FontWeight.SemiBold)
            Text("Somente com token OAuth válido obtido após autorização da plataforma. " +
                "Este modo avançado não realiza login OAuth: você fornece um token temporário " +
                "a cada sessão. Nenhum segredo é armazenado, exportado ou registrado em logs.",
                style = MaterialTheme.typography.bodySmall)
            Text("Vídeo: ${post.videoName}", style = MaterialTheme.typography.bodySmall)
            if (isPage) Text("Reels da Meta devem atender às regras atuais de formato, " +
                "duração e elegibilidade da Página.", style = MaterialTheme.typography.bodySmall)
            else Text("O TikTok pode limitar clientes não auditados a publicação privada. " +
                "O escopo video.publish e aprovação são obrigatórios.",
                style = MaterialTheme.typography.bodySmall)
            Text("Legenda: $captionLength/2200 caracteres", style = MaterialTheme.typography.labelSmall,
                color = if (captionLength > 2200) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant)
            if (!verified && !completed) {
                OutlinedTextField(value = token,
                    onValueChange = { token = it.trim().take(4096); confirmedIdentity = false },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    label = { Text(if (isPage) "Token OAuth da Página" else "Token OAuth do TikTok") },
                    visualTransformation = PasswordVisualTransformation())
            }
            verifiedPage?.let { page ->
                Text("Página: ${page.name} · ID ${page.id}", fontWeight = FontWeight.Medium)
                Text("A Página foi comparada com o ID vinculado ao destino.",
                    style = MaterialTheme.typography.bodySmall)
            }
            creator?.let { user ->
                Text("Criador: ${user.name} · @${user.username}", fontWeight = FontWeight.Medium)
                if (user.maxDurationSeconds > 0) Text(
                    "Duração máxima informada pela conta: ${user.maxDurationSeconds}s.",
                    style = MaterialTheme.typography.bodySmall)
                Text("Selecione uma opção de privacidade fornecida agora pela API:",
                    style = MaterialTheme.typography.bodySmall)
                user.privacyOptions.forEach { option ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = privacy == option, onClick = { privacy = option })
                        Text(option, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Text("Interações do vídeo (opções de restrição)", style = MaterialTheme.typography.labelMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = disableComment || user.commentsDisabled,
                        enabled = !user.commentsDisabled, onCheckedChange = { disableComment = it })
                    Text("Desativar comentários", style = MaterialTheme.typography.bodySmall)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = disableDuet || user.duetDisabled,
                        enabled = !user.duetDisabled, onCheckedChange = { disableDuet = it })
                    Text("Desativar dueto", style = MaterialTheme.typography.bodySmall)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = disableStitch || user.stitchDisabled,
                        enabled = !user.stitchDisabled, onCheckedChange = { disableStitch = it })
                    Text("Desativar costura", style = MaterialTheme.typography.bodySmall)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = aiGenerated, onCheckedChange = { aiGenerated = it })
                    Text("Este vídeo contém conteúdo gerado por IA", style = MaterialTheme.typography.bodySmall)
                }
            }
            if (verified && !completed) Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = confirmedIdentity, onCheckedChange = { confirmedIdentity = it })
                Text("Confiro a conta, a legenda e autorizo este envio. Sei que " +
                    "ele pode consumir internet e cotas.", style = MaterialTheme.typography.bodySmall)
            }
            if (busy) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text(progress.ifBlank { "Consultando API..." }, style = MaterialTheme.typography.bodySmall)
            }
            if (completed) Text("Operação recebida. Verifique na plataforma se o vídeo terminou " +
                "de processar e se está público; não haverá reenvio automático.",
                style = MaterialTheme.typography.bodySmall)
            if (failure.isNotBlank()) Text(failure, color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall)
        } },
        confirmButton = {
            if (!verified && !completed) TextButton(onClick = {
                busy = true; failure = ""; progress = "Identificando conta autorizada..."
                scope.launch {
                    runCatching {
                        if (isPage) {
                            val page = SocialExternalApis.facebookPage(token)
                            require(page.id == account.remotePageId) {
                                "Este token pertence a outra Página. Vincule a Página correta primeiro." }
                            verifiedPage = page
                        } else {
                            creator = SocialExternalApis.tikTokCreator(token)
                            privacy = "" // Usuário deve escolher uma opção oferecida ao vivo.
                        }
                    }.onFailure { failure = it.message ?: "Token não autorizado." }
                    busy = false; progress = ""
                }
            }, enabled = !busy && token.length >= 10) { Text("Verificar conta") }
            else if (completed) TextButton(onClick = { token = ""; onDismiss() }) { Text("Concluir") }
            else TextButton(onClick = {
                busy = true; failure = ""; progress = "Registrando tentativa antes do envio..."
                val currentToken = token
                val selectedPrivacy = privacy
                val selectedCreator = creator
                scope.launch {
                    runCatching {
                        val latest = store.load()
                        val source = latest.publications.singleOrNull { it.id == post.id }
                            ?: error("Publicação não encontrada.")
                        val acct = latest.accounts.singleOrNull { it.id == account.id }
                            ?: error("Conta não encontrada.")
                        require(acct.destination == account.destination) { "Tipo de conta alterado." }
                        if (isPage) require(acct.remotePageId == verifiedPage?.id) {
                            "A Página foi alterada. Revalide a autorização." }
                        val marked = SocialPublicationPolicy.markUploadReview(source)
                        val started = latest.copy(publications = latest.publications.map {
                            if (it.id == source.id) marked else it
                        })
                        store.save(started)
                        onChanged(started)
                        val remote = if (isPage) {
                            progress = "Enviando Reel ao Facebook..."
                            SocialExternalApis.publishFacebookReel(context, currentToken,
                                acct.remotePageId, marked) { sent, total ->
                                scope.launch { progress = "Facebook: ${sent * 100 / total}%" }
                            }
                        } else {
                            progress = "Enviando ao TikTok..."
                            SocialExternalApis.publishTikTok(context, currentToken, marked,
                                requireNotNull(selectedCreator), selectedPrivacy, aiGenerated,
                                disableComment, disableDuet, disableStitch) { sent, total ->
                                scope.launch { progress = "TikTok: ${sent * 100 / total}%" }
                            }
                        }
                        val saved = store.load()
                        val updated = SocialPublicationPolicy.markExternalSubmitted(
                            saved.publications.single { it.id == post.id }, remote)
                        val next = saved.copy(publications = saved.publications.map {
                            if (it.id == post.id) updated else it
                        })
                        store.save(next); onChanged(next)
                        completed = true
                    }.onFailure {
                        failure = it.message ?: "Resultado incerto. Confira a rede antes de qualquer novo envio."
                    }
                    token = ""; busy = false; progress = ""
                }
            }, enabled = canPublish) { Text("Confirmar e enviar") }
        },
        dismissButton = { TextButton(onClick = { token = ""; onDismiss() }, enabled = !busy) {
            Text(if (completed) "Fechar" else "Cancelar")
        } })
}

/** Consulta explícita, sem confundir publish_id com endereço público de vídeo. */
@Composable
internal fun SocialTikTokStatusDialog(post: SocialPublication, onDismiss: () -> Unit) {
    var token by remember(post.id) { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    AlertDialog(onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("Consultar processamento TikTok") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Operação ${post.remoteVideoId}. O identificador não é um link de vídeo.",
                style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(value = token, onValueChange = { token = it.trim().take(4096) },
                label = { Text("Token OAuth da conta") }, singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation())
            Text("A consulta é manual, não publica nem repete o upload.",
                style = MaterialTheme.typography.bodySmall)
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (result.isNotBlank()) Text(result, style = MaterialTheme.typography.bodySmall)
        } },
        confirmButton = { TextButton(enabled = !busy && token.length >= 10, onClick = {
            busy = true; result = "Consultando status..."
            scope.launch {
                runCatching { SocialExternalApis.tikTokStatus(token, post.remoteVideoId) }
                    .onSuccess { result = "Status informado pelo TikTok: $it. Confira sua conta para verificar a visibilidade." }
                    .onFailure { result = it.message ?: "Status indisponível." }
                token = ""; busy = false
            }
        }) { Text("Consultar") } },
        dismissButton = { TextButton(enabled = !busy, onClick = { token = ""; onDismiss() }) {
            Text("Fechar")
        } })
}
