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

/** Only the update notice is automatic. The APK download and installation require user action. */
@Composable internal fun GitHubUpdatePrompt(manager: GitHubUpdateManager, onOpenUpdates: () -> Unit) {
    val pending = manager.promptRelease ?: return
    AlertDialog(
        onDismissRequest = manager::dismissPrompt,
        icon = { Icon(Icons.Default.SystemUpdate, null) },
        title = { Text("Atualização disponível: v${pending.version}") },
        text = { Text("O Uai Cortes encontrou uma versão mais recente. Nada será baixado ou instalado sem sua autorização.") },
        confirmButton = { TextButton(onClick = {
            manager.dismissPrompt()
            onOpenUpdates()
        }) { Text("Ver atualização") } },
        dismissButton = { TextButton(onClick = manager::dismissPrompt) { Text("Agora não") } }
    )
}

@Composable internal fun GitHubUpdatesScreen(manager: GitHubUpdateManager, back: () -> Unit) {
    val scope = rememberCoroutineScope()
    var confirmDownload by remember { mutableStateOf<GitHubUpdateRelease?>(null) }
    var installMessage by remember { mutableStateOf<String?>(null) }
    val check = manager.result
    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Heading("Atualizações", back)
        Text("Versão instalada: ${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.titleMedium)
        Button(onClick = { scope.launch { manager.check() } },
            enabled = check !is GitHubUpdateResult.Checking && !manager.downloading,
            modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.SystemUpdate, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Verificar atualizações")
        }
        when (check) {
            GitHubUpdateResult.Idle -> Text("Toque em Verificar para consultar as versões publicadas.")
            GitHubUpdateResult.Checking -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularProgressIndicator(Modifier.size(22.dp))
                Text("Consultando o GitHub…")
            }
            GitHubUpdateResult.NotConfigured -> Text(
                "Este APK não informa o repositório de origem. A atualização integrada precisa de uma compilação publicada pelo GitHub Actions.",
                color = MaterialTheme.colorScheme.error)
            GitHubUpdateResult.UpToDate -> Text("Nenhuma atualização mais recente encontrada.")
            is GitHubUpdateResult.Error -> Text(check.message, color = MaterialTheme.colorScheme.error)
            is GitHubUpdateResult.Available -> {
                val release = check.release
                Text("Nova versão: ${release.version}", style = MaterialTheme.typography.titleLarge)
                Text("APK universal · ${"%.1f".format(java.util.Locale.US, release.size / 1048576.0)} MB")
                if (release.sha256 != null) {
                    Button(onClick = { confirmDownload = release }, enabled = !manager.downloading,
                        modifier = Modifier.fillMaxWidth()) { Text("Baixar atualização") }
                } else {
                    Text("Esta versão não possui a verificação SHA-256 exigida pelo aplicativo. " +
                        "O download interno foi bloqueado por segurança.",
                        color = MaterialTheme.colorScheme.error)
                }
            }
        }
        if (manager.downloading) {
            Text("Baixando APK: ${manager.downloadPercent}%")
            LinearProgressIndicator(progress = { manager.downloadPercent / 100f },
                modifier = Modifier.fillMaxWidth())
        }
        manager.downloadError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (manager.readyFile != null) {
            Text("Download verificado. O Android pedirá sua autorização para instalar.")
            Button(onClick = {
                installMessage = manager.installDownloaded()
                    ?: "Instalador aberto. Confirme a atualização no Android."
            }, modifier = Modifier.fillMaxWidth()) { Text("Instalar atualização") }
        }
        installMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        if (manager.readyFile != null || check is GitHubUpdateResult.Available) {
            Text("Não desinstale a versão atual. Para preservar os projetos, a atualização deve usar " +
                "a mesma assinatura do aplicativo instalado.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(22.dp))
    }
    confirmDownload?.let { release ->
        AlertDialog(onDismissRequest = { confirmDownload = null },
            title = { Text("Baixar versão ${release.version}?") },
            text = { Text("O aplicativo baixará o APK da Release oficial do GitHub, verificará o SHA-256 " +
                "e pedirá sua autorização antes de instalar. Seus projetos não serão modificados pelo download.") },
            confirmButton = { TextButton(onClick = {
                confirmDownload = null
                scope.launch { manager.download(release) }
            }) { Text("Baixar") } },
            dismissButton = { TextButton(onClick = { confirmDownload = null }) { Text("Cancelar") } })
    }
}
