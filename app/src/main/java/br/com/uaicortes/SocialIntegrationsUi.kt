package br.com.uaicortes

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/** Conteúdo de ajuda separado da navegação e dos clientes OAuth. Nenhum segredo é solicitado aqui. */
internal data class SocialIntegrationGuide(
    val intro: String,
    val userSteps: List<String>,
    val developerSteps: List<String>,
    val setupUrl: String,
    val setupLabel: String,
    val manualOnly: Boolean = false
)

internal object SocialIntegrationGuides {
    fun forDestination(dest: SocialDestination): SocialIntegrationGuide = when (dest) {
        SocialDestination.YOUTUBE -> SocialIntegrationGuide(
            "Escolha uma conta Google. O canal identificado pela API será adicionado após sua confirmação.",
            listOf("Toque em Conectar canal YouTube.",
                "Escolha a conta Google e autorize o acesso.",
                "Confirme o nome e o ID do canal para adicioná-lo automaticamente em Minhas contas.",
                "Para outra conta, repita a conexão e selecione o segundo canal."),
            listOf("Abra o Google Cloud Console e selecione um projeto.",
                "Ative YouTube Data API v3 e configure a tela de consentimento OAuth.",
                "Crie um cliente OAuth Android para br.com.uaicortes e registre a SHA-1 do certificado de assinatura.",
                "Configure escopos, usuários de teste e verificação quando exigida."),
            "https://console.cloud.google.com/apis/credentials", "Abrir Google Cloud Console")
        SocialDestination.FACEBOOK_PAGE -> SocialIntegrationGuide(
            "Conecte sua conta Facebook e escolha uma Página autorizada. Conta cadastrada não significa login ativo.",
            listOf("Entre com Facebook e conceda acesso às Páginas.",
                "Escolha uma Página retornada pela API e toque em Adicionar.",
                "Para publicar via API, autorize a postagem quando necessário."),
            listOf("Crie um aplicativo em Meta for Developers e configure Facebook Login para Android.",
                "Cadastre o identificador do pacote e o hash de assinatura da versão instalada.",
                "Defina META_APP_ID e META_CLIENT_TOKEN nos Secrets do CI e gere um novo APK assinado.",
                "Solicite as permissões e a análise da Meta exigidas para publicar. Nunca coloque App Secret no APK."),
            "https://developers.facebook.com/apps/", "Abrir Meta for Developers")
        SocialDestination.TIKTOK -> SocialIntegrationGuide(
            "Publicação por API requer autorização e permissões aprovadas. Login simplificado ainda não está disponível.",
            listOf("Por enquanto, compartilhe o vídeo pelo Android.",
                "Se já possui token OAuth válido, é possível testá-lo em Opções avançadas."),
            listOf("Cadastre um app no TikTok for Developers.",
                "Ative Content Posting API e solicite a aprovação do escopo video.publish.",
                "Configure redirecionamento HTTPS e backend seguro para a troca de código por token.",
                "Realize auditoria quando exigida; clientes não auditados podem ficar restritos a conteúdo privado."),
            "https://developers.tiktok.com/docs/en/content-posting-api-get-started", "Abrir documentação TikTok")
        SocialDestination.FACEBOOK_PROFILE -> SocialIntegrationGuide(
            "O Uai Cortes não publica por API em perfis pessoais. Use o compartilhamento Android.",
            listOf("Cadastre o perfil em Minhas contas.",
                "Prepare um vídeo e escolha Compartilhar para abrir o aplicativo Facebook.",
                "Confirme a publicação somente após verificá-la no Facebook."),
            emptyList(), "https://www.facebook.com/help/", "Abrir ajuda Facebook", true)
        SocialDestination.KWAI -> SocialIntegrationGuide(
            "A publicação pelo Kwai está disponível por compartilhamento Android, sem API validada no aplicativo.",
            listOf("Cadastre o destino Kwai em Minhas contas.",
                "Prepare o vídeo e use Compartilhar para selecionar o Kwai.",
                "Confirme a postagem depois de verificá-la no Kwai."),
            emptyList(), "https://www.kwai.com/", "Abrir Kwai", true)
    }

    fun subtitle(dest: SocialDestination): String = when (dest) {
        SocialDestination.YOUTUBE -> "Autorização Google por ação"
        SocialDestination.FACEBOOK_PAGE -> "Login Meta e permissões"
        SocialDestination.TIKTOK -> "Token temporário; login pendente"
        SocialDestination.FACEBOOK_PROFILE, SocialDestination.KWAI -> "Compartilhamento manual"
    }
}

/** Entrada compacta, reaproveita a escala de formas do editor e os temas existentes. */
@Composable
private fun IntegrationRow(icon: androidx.compose.ui.graphics.vector.ImageVector,
                           title: String, summary: String, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = UaiShapes.item, color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp) {
        Row(Modifier.fillMaxWidth().heightIn(min = 66.dp).padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Medium)
                Text(summary, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun socialIntegrationIcon(dest: SocialDestination) = when (dest) {
    SocialDestination.YOUTUBE -> Icons.Default.SmartDisplay
    SocialDestination.FACEBOOK_PAGE -> Icons.Default.Pages
    SocialDestination.TIKTOK -> Icons.Default.MusicNote
    SocialDestination.KWAI -> Icons.Default.Share
    SocialDestination.FACEBOOK_PROFILE -> Icons.Default.Person
}

@Composable
private fun SocialGuideSection(guide: SocialIntegrationGuide, showDeveloper: Boolean) {
    val context = LocalContext.current
    var advanced by rememberSaveable(guide.setupUrl) { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Como usar", style = MaterialTheme.typography.titleMedium)
        guide.userSteps.forEachIndexed { index, step ->
            Row(verticalAlignment = Alignment.Top) {
                Text("${index + 1}.", color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold, modifier = Modifier.width(28.dp))
                Text(step, style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (showDeveloper && guide.developerSteps.isNotEmpty()) {
            HorizontalDivider()
            Row(Modifier.fillMaxWidth().clickable { advanced = !advanced }.heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text("Configuração para desenvolvedores", modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.Medium)
                Icon(if (advanced) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    "Mostrar configurações de desenvolvedor")
            }
            if (advanced) guide.developerSteps.forEachIndexed { index, step ->
                Row(verticalAlignment = Alignment.Top) {
                    Text("${index + 1}.", color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.width(28.dp))
                    Text(step, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        OutlinedButton(onClick = {
            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(guide.setupUrl))) }
        }, modifier = Modifier.fillMaxWidth(), shape = UaiShapes.button) {
            Icon(Icons.Default.OpenInNew, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(guide.setupLabel)
        }
    }
}

/** Tela de serviços sem misturar cadastro local, credenciais de desenvolvedor e autorização de usuário. */
@Composable
internal fun SocialApiCenterScreen(
    back: () -> Unit, openGemini: () -> Unit, openSocial: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val store = remember(context) { SocialStore(context) }
    val scope = rememberCoroutineScope()
    var selected by rememberSaveable { mutableStateOf<SocialDestination?>(null) }
    var feedback by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var showBackup by rememberSaveable { mutableStateOf(false) }
    var externalTest by remember { mutableStateOf<SocialDestination?>(null) }
    var metaPages by remember { mutableStateOf<List<SocialExternalApis.FacebookPage>>(emptyList()) }
    var youtubeChannelToAdd by remember { mutableStateOf<YouTubeSocialApi.Channel?>(null) }
    var verifiedYoutubeId by remember { mutableStateOf("") }
    var savedSocialAccounts by remember { mutableStateOf<List<SocialAccount>>(emptyList()) }
    LaunchedEffect(selected) {
        runCatching { store.load().accounts }
            .onSuccess { savedSocialAccounts = it }
            .onFailure { feedback = "Não foi possível ler os destinos salvos." }
    }
    val youtubeAuth = rememberYouTubeAuthorization { error -> feedback = error; busy = false }

    fun loadMetaPages() {
        val userToken = SocialMetaAuth.currentUserToken()
        if (userToken == null) { feedback = "A sessão Facebook expirou. Autorize novamente."; return }
        busy = true
        scope.launch {
            runCatching {
                val pages = SocialExternalApis.facebookPages(userToken)
                SocialMetaAuth.putPageTokens(pages)
                metaPages = pages.map { it.copy(accessToken = "") }
                feedback = "${pages.size} Página(s) consultada(s). Vincule uma Página em Minhas contas."
            }.onFailure { feedback = "Não foi possível consultar as Páginas autorizadas." }
            busy = false
        }
    }

    fun connectMeta(forPublishing: Boolean) {
        if (activity == null) { feedback = "Autorização Meta indisponível nesta tela."; return }
        if (!SocialMetaAuth.isConfigured) {
            feedback = "A configuração Meta deve ser incluída no build do aplicativo. Veja o guia abaixo."
            return
        }
        busy = true
        metaPages = emptyList()
        SocialMetaAuth.start(activity, forPublishing) { success, message ->
            feedback = message
            if (success) loadMetaPages() else busy = false
        }
    }

    val exporter = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) runCatching {
            val bytes = SocialPublicConfig.export(store.load()).toByteArray(Charsets.UTF_8)
            context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(bytes) }
                ?: error("Falha ao criar arquivo")
        }.onSuccess { feedback = "Cadastros públicos exportados sem credenciais." }
            .onFailure { feedback = "Não foi possível exportar os cadastros." }
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) runCatching {
            val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
                val result = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    val n = input.read(buffer)
                    if (n < 0) break
                    require(result.size() + n <= 100_000) { "Arquivo excede 100 KB." }
                    result.write(buffer, 0, n)
                }
                result.toByteArray()
            } ?: error("Falha ao ler")
            store.save(SocialPublicConfig.merge(store.load(), bytes))
        }.onSuccess { feedback = "Cadastros importados; autorizações não são importadas." }
            .onFailure { feedback = "Arquivo incompatível. Os dados anteriores foram preservados." }
    }

    val dest = selected
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        Heading(if (dest == null) "Integrações" else dest.label, {
            if (selected == null) back() else { selected = null; feedback = ""; metaPages = emptyList() }
        })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (dest == null) {
                Text("INTELIGÊNCIA ARTIFICIAL", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                IntegrationRow(Icons.Default.AutoAwesome, "Google Gemini", "Chaves, modelos e testes", openGemini)
                Text("REDES SOCIAIS", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                listOf(SocialDestination.YOUTUBE, SocialDestination.FACEBOOK_PAGE,
                    SocialDestination.TIKTOK, SocialDestination.KWAI,
                    SocialDestination.FACEBOOK_PROFILE).forEach { item ->
                    IntegrationRow(socialIntegrationIcon(item), item.label,
                        SocialIntegrationGuides.subtitle(item)) {
                        selected = item; feedback = ""; metaPages = emptyList()
                    }
                }
                HorizontalDivider()
                IntegrationRow(Icons.Default.People, "Minhas contas",
                    "Cadastrar e vincular canais ou Páginas", openSocial)
                OutlinedCard(shape = UaiShapes.card) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp)) {
                        Row(Modifier.fillMaxWidth().clickable { showBackup = !showBackup }
                            .heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Settings, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Text("Backup de cadastros", modifier = Modifier.weight(1f))
                            Icon(if (showBackup) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                "Mostrar backup")
                        }
                        if (showBackup) {
                            Text("Exporta apenas nomes e tipos de contas. Não inclui vídeos, links, " +
                                "tokens ou métricas.", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { exporter.launch("uai-redes-config.json") },
                                    modifier = Modifier.weight(1f), shape = UaiShapes.button) {
                                    Text("Exportar")
                                }
                                OutlinedButton(onClick = { importer.launch(arrayOf("application/json", "text/plain")) },
                                    modifier = Modifier.weight(1f), shape = UaiShapes.button) {
                                    Text("Importar")
                                }
                            }
                        }
                    }
                }
            } else {
                val guide = SocialIntegrationGuides.forDestination(dest)
                Surface(shape = UaiShapes.card, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(socialIntegrationIcon(dest), null,
                                tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(10.dp))
                            Text(when (dest) {
                                SocialDestination.YOUTUBE -> "Autorização ao utilizar"
                                SocialDestination.FACEBOOK_PAGE -> when {
                                    !SocialMetaAuth.isConfigured -> "Configuração do app pendente"
                                    SocialMetaAuth.currentUserToken() != null -> "Sessão Meta autorizada"
                                    else -> "Conta não autorizada"
                                }
                                SocialDestination.TIKTOK -> "Login pelo app indisponível"
                                else -> "Somente compartilhamento manual"
                            }, fontWeight = FontWeight.SemiBold)
                        }
                        Text(guide.intro, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                when (dest) {
                    SocialDestination.YOUTUBE -> {
                        Button(onClick = {
                            busy = true
                            youtubeAuth(true) { token ->
                                scope.launch {
                                    runCatching { YouTubeSocialApi.channel(token) }
                                        .onSuccess { youtubeChannelToAdd = it; feedback = "" }
                                        .onFailure { feedback = it.message ?: "Não foi possível consultar o canal." }
                                    busy = false
                                }
                            }
                        }, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                            shape = UaiShapes.button) {
                            Icon(Icons.Default.Link, null); Spacer(Modifier.width(8.dp))
                            Text("Conectar canal YouTube")
                        }
                        Text("Para adicionar outro canal, toque novamente e escolha outra conta Google.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        val knownChannels = savedSocialAccounts.filter {
                            it.destination == SocialDestination.YOUTUBE && it.remoteChannelId.isNotBlank()
                        }
                        if (knownChannels.isNotEmpty()) {
                            Text("Canais adicionados", style = MaterialTheme.typography.titleSmall)
                            knownChannels.take(10).forEach { account ->
                                Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 3.dp)) {
                                    Text(account.label, fontWeight = FontWeight.Medium)
                                    Text(if (account.remoteChannelId == verifiedYoutubeId)
                                        "Verificado nesta sessão · ${account.remoteChannelId}" else
                                        "Salvo · reautorize para utilizar · ${account.remoteChannelId}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            if (knownChannels.size > 10) Text("Demais canais disponíveis em Minhas contas.",
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    SocialDestination.FACEBOOK_PAGE -> {
                        Button(onClick = {
                            if (SocialMetaAuth.currentUserToken() != null) loadMetaPages()
                            else connectMeta(false)
                        }, enabled = !busy && SocialMetaAuth.isConfigured,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                            shape = UaiShapes.button) {
                            Text(if (SocialMetaAuth.currentUserToken() != null) "Consultar minhas Páginas"
                                else "Entrar com Facebook")
                        }
                        if (SocialMetaAuth.isConfigured) {
                            OutlinedButton(onClick = { connectMeta(true) },
                                enabled = !busy && SocialMetaAuth.currentUserToken() != null,
                                modifier = Modifier.fillMaxWidth(), shape = UaiShapes.button) {
                                Text("Autorizar publicação")
                            }
                        }
                        if (metaPages.isNotEmpty()) {
                            Text("Páginas disponíveis", style = MaterialTheme.typography.titleSmall)
                            metaPages.forEach { page ->
                                OutlinedButton(onClick = {
                                    runCatching {
                                        val next = SocialPublicationPolicy.upsertFacebookPage(
                                            store.load(), page.id, page.name)
                                        store.save(next)
                                        next.accounts
                                    }.onSuccess { accounts ->
                                        savedSocialAccounts = accounts
                                        feedback = "Página ${page.name} adicionada em Minhas contas."
                                    }.onFailure {
                                        feedback = it.message ?: "Falha ao salvar a Página no aparelho."
                                    }
                                }, modifier = Modifier.fillMaxWidth(), shape = UaiShapes.button) {
                                    Text("Adicionar ${page.name}")
                                }
                            }
                        }
                    }
                    SocialDestination.TIKTOK -> {
                        OutlinedButton(onClick = openSocial, modifier = Modifier.fillMaxWidth(),
                            shape = UaiShapes.button) { Text("Usar compartilhamento manual") }
                    }
                    else -> {
                        Button(onClick = openSocial, modifier = Modifier.fillMaxWidth(),
                            shape = UaiShapes.button) { Text("Gerenciar meus destinos") }
                    }
                }
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                if (dest == SocialDestination.YOUTUBE || dest == SocialDestination.FACEBOOK_PAGE) {
                    TextButton(onClick = openSocial, modifier = Modifier.fillMaxWidth()) {
                        Text("Ir para Minhas contas")
                        Icon(Icons.Default.ChevronRight, null)
                    }
                }
                HorizontalDivider()
                SocialGuideSection(guide, showDeveloper = !guide.manualOnly)
                if (dest == SocialDestination.FACEBOOK_PAGE || dest == SocialDestination.TIKTOK) {
                    var advanced by rememberSaveable(dest) { mutableStateOf(false) }
                    HorizontalDivider()
                    Row(Modifier.fillMaxWidth().clickable { advanced = !advanced }.heightIn(min = 48.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("Teste avançado de token", modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleSmall)
                        Icon(if (advanced) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            "Expandir teste avançado")
                    }
                    if (advanced) {
                        Text("Para desenvolvedores com token OAuth já autorizado. O token " +
                            "é usado apenas nesta consulta e não é salvo.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedButton(onClick = { externalTest = dest },
                            modifier = Modifier.fillMaxWidth(), shape = UaiShapes.button) {
                            Text("Testar token temporário")
                        }
                    }
                }
                if (dest == SocialDestination.FACEBOOK_PAGE && SocialMetaAuth.currentUserToken() != null) {
                    TextButton(onClick = {
                        SocialMetaAuth.disconnect()
                        metaPages = emptyList()
                        feedback = "Sessão Meta encerrada. Seus cadastros locais permanecem salvos."
                    }) { Text("Desconectar Facebook") }
                }
            }
            if (feedback.isNotBlank()) {
                Surface(shape = UaiShapes.item, color = MaterialTheme.colorScheme.secondaryContainer) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Default.Info, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(feedback, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
    youtubeChannelToAdd?.let { channel ->
        SocialIdentifiedAccountDialog(
            destination = SocialDestination.YOUTUBE,
            accountName = channel.title,
            publicId = channel.id,
            onDismiss = { youtubeChannelToAdd = null },
            onConfirm = {
                runCatching {
                    val next = SocialPublicationPolicy.upsertYouTube(store.load(), channel.id, channel.title)
                    store.save(next)
                    next.accounts
                }.onSuccess { accounts ->
                    savedSocialAccounts = accounts
                    verifiedYoutubeId = channel.id
                    feedback = "Canal ${channel.title} adicionado em Minhas contas."
                }.onFailure { feedback = it.message ?: "Falha ao salvar canal no aparelho." }
                youtubeChannelToAdd = null
            })
    }
    externalTest?.let { test ->
        SocialExternalApiTestDialog(test, onDismiss = { externalTest = null })
    }
}
