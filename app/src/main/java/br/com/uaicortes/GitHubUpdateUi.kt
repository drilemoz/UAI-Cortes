package br.com.uaicortes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/** A notification about the release, never an automatic APK download or install. */
@Composable internal fun GitHubUpdatePrompt(manager: GitHubUpdateManager, onOpenUpdates: () -> Unit) {
    val pending = manager.promptRelease ?: return
    AlertDialog(
        onDismissRequest = manager::dismissPrompt,
        icon = { Icon(Icons.Default.SystemUpdate, null) },
        title = { Text("Atualização disponível: v${pending.version}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Versão instalada: v${BuildConfig.VERSION_NAME}")
                Text("Uma nova Release do Uai Cortes está disponível no GitHub.")
                if (pending.notes.isNotBlank()) Text(pending.notes.take(500),
                    style = MaterialTheme.typography.bodySmall)
                Text("Nada será baixado nem instalado sem sua confirmação.",
                    style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { TextButton(onClick = {
            manager.dismissPrompt()
            onOpenUpdates()
        }) { Text("Ver atualização") } },
        dismissButton = { TextButton(onClick = manager::dismissPrompt) { Text("Agora não") } }
    )
}

@Composable internal fun GitHubUpdatesScreen(manager: GitHubUpdateManager, back: () -> Unit) {
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf(manager.repository()) }
    var editError by remember { mutableStateOf<String?>(null) }
    var confirmDownload by remember { mutableStateOf<GitHubUpdateRelease?>(null) }
    var installMessage by remember { mutableStateOf<String?>(null) }
    val check = manager.result
    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Heading("Atualizações pelo GitHub", back)
        Text("O Uai Cortes consulta as Releases publicadas sempre que é aberto, se o repositório estiver configurado. " +
            "Nenhuma atualização é baixada ou instalada automaticamente.",
            style = MaterialTheme.typography.bodyMedium)
        OutlinedTextField(value = input, onValueChange = { input = it; editError = null },
            label = { Text("Repositório (dono/nome)") },
            placeholder = { Text("usuario/Uai-Cortes") },
            supportingText = { Text("Para APKs criados no GitHub Actions, o repositório é detectado no build. " +
                "Em builds locais, informe um repositório público com Releases.") },
            singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedButton(onClick = {
            if (manager.saveRepository(input)) {
                input = manager.repository()
                editError = null
                scope.launch { manager.check() }
            } else editError = "Informe um repositório válido: dono/nome."
        }) { Text("Salvar repositório") }
        editError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        HorizontalDivider()
        Button(onClick = { scope.launch { manager.check() } },
            enabled = check !is GitHubUpdateResult.Checking && !manager.downloading,
            modifier = Modifier.fillMaxWidth()) { Text("Verificar atualizações agora") }
        OutlinedButton(onClick = {
            installMessage = manager.openRepositoryReleases(input)
        }, enabled = GitHubUpdateCore.normalizedRepository(input) != null,
            modifier = Modifier.fillMaxWidth()) {
            Text("Abrir Releases no navegador")
        }
        Text("Se a consulta falhar, abra as Releases no navegador para verificar se o " +
            "repositório está público e acessível. A verificação automática não usa token privado.",
            style = MaterialTheme.typography.bodySmall)
        when (check) {
            GitHubUpdateResult.Idle -> Text("Consulta ainda não realizada.")
            GitHubUpdateResult.Checking -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularProgressIndicator(Modifier.size(22.dp))
                Text("Consultando GitHub…")
            }
            GitHubUpdateResult.NotConfigured -> Text("Configure o repositório acima para ativar a verificação na abertura.")
            GitHubUpdateResult.UpToDate -> Text("Nenhuma versão mais recente com APK universal foi encontrada nas Releases consultadas.")
            is GitHubUpdateResult.Error -> Text(check.message, color = MaterialTheme.colorScheme.error)
            is GitHubUpdateResult.Available -> {
                val release = check.release
                Text("Disponível: v${release.version} (instalada: v${BuildConfig.VERSION_NAME})",
                    style = MaterialTheme.typography.titleMedium)
                if (release.notes.isNotBlank()) Text(release.notes.take(1200),
                    style = MaterialTheme.typography.bodySmall)
                Text("APK universal · ${release.size / (1024 * 1024)} MiB · " +
                    if (release.sha256 == null) "sem SHA-256 publicado" else "SHA-256 disponível")
                OutlinedButton(onClick = { manager.openRelease(release) }) {
                    Text("Abrir Release no GitHub")
                }
                if (release.sha256 != null) Button(onClick = { confirmDownload = release },
                    enabled = !manager.downloading, modifier = Modifier.fillMaxWidth()) {
                    Text("Baixar APK verificado")
                } else Text("Sem SHA-256, o download interno fica indisponível por segurança. " +
                    "Use a página oficial da Release.", style = MaterialTheme.typography.bodySmall)
            }
        }
        if (manager.downloading) {
            Text("Baixando o APK: ${manager.downloadPercent}%")
            LinearProgressIndicator(progress = { manager.downloadPercent / 100f },
                modifier = Modifier.fillMaxWidth())
        }
        manager.downloadError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (manager.readyFile != null) {
            Text("APK baixado e conferido. A instalação precisa ser autorizada pelo Android.")
            Button(onClick = { installMessage = manager.installDownloaded() ?: "Instalador aberto. Confirme a instalação no Android." },
                modifier = Modifier.fillMaxWidth()) { Text("Instalar atualização") }
        }
        installMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        Text("Importante: para atualizar sem desinstalar, o novo APK deve ter o mesmo identificador " +
            "e certificado de assinatura do aplicativo instalado. Não desinstale: seus projetos podem ser perdidos.",
            style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(22.dp))
    }
    confirmDownload?.let { release ->
        AlertDialog(onDismissRequest = { confirmDownload = null },
            title = { Text("Baixar v${release.version}?") },
            text = { Text("O download do APK universal será feito da Release oficial no GitHub. " +
                "O arquivo será conferido por SHA-256 e versão antes de oferecer a instalação. " +
                "Nenhum projeto será alterado nesta etapa.") },
            confirmButton = { TextButton(onClick = {
                confirmDownload = null
                scope.launch { manager.download(release) }
            }) { Text("Baixar") } },
            dismissButton = { TextButton(onClick = { confirmDownload = null }) { Text("Cancelar") } })
    }
}
