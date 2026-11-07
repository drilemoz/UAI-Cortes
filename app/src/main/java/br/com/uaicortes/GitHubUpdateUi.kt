package br.com.uaicortes

import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/** A notificação é automática; baixar e instalar continuam sempre manuais. */
@Composable internal fun GitHubUpdatePrompt(manager: GitHubUpdateManager, onOpenUpdates: () -> Unit) {
    val pending = manager.promptRelease ?: return
    AlertDialog(
        onDismissRequest = manager::dismissPrompt,
        shape = UaiShapes.dialog,
        icon = { Icon(Icons.Default.SystemUpdate, null) },
        title = { Text("Atualização disponível: v${pending.version}") },
        text = { Text("Há uma versão mais recente do Uai Cortes. O download e a instalação precisam da sua autorização.") },
        confirmButton = { TextButton(onClick = {
            manager.dismissPrompt()
            onOpenUpdates()
        }, shape = UaiShapes.button) { Text("Ver atualização") } },
        dismissButton = { TextButton(onClick = manager::dismissPrompt, shape = UaiShapes.button) { Text("Agora não") } }
    )
}

@Composable internal fun GitHubUpdatesScreen(manager: GitHubUpdateManager, back: () -> Unit) {
    val scope = rememberCoroutineScope()
    var confirmDownload by remember { mutableStateOf<GitHubUpdateRelease?>(null) }
    var installMessage by remember { mutableStateOf<String?>(null) }
    val check = manager.result
    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Heading("Atualizações", back)
        Text("Versão instalada: ${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.titleMedium)
        Button(onClick = { scope.launch { manager.check() } },
            enabled = check !is GitHubUpdateResult.Checking && !manager.downloading,
            shape = UaiShapes.button,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Icon(Icons.Default.SystemUpdate, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Verificar atualizações")
        }
        when (check) {
            GitHubUpdateResult.Idle -> Text("Consulte as versões publicadas no GitHub.",
                style = MaterialTheme.typography.bodySmall)
            GitHubUpdateResult.Checking -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularProgressIndicator(Modifier.size(22.dp))
                Text("Consultando o GitHub…")
            }
            GitHubUpdateResult.NotConfigured -> Text(
                "Este APK não informa o repositório de origem. O atualizador precisa de uma compilação publicada pelo GitHub Actions.",
                color = MaterialTheme.colorScheme.error)
            GitHubUpdateResult.UpToDate -> Text("Seu Uai Cortes está atualizado.")
            is GitHubUpdateResult.Error -> Text(check.message, color = MaterialTheme.colorScheme.error)
            is GitHubUpdateResult.Available -> {
                val release = check.release
                Surface(shape = UaiShapes.card, color = MaterialTheme.colorScheme.surfaceVariant) {
                    Column(Modifier.fillMaxWidth().padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Nova versão: ${release.version}", style = MaterialTheme.typography.titleMedium)
                        Text("APK universal · ${GitHubDownloadProgress.readableBytes(release.size)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (release.sha256 != null) {
                    if (!manager.downloading && manager.readyFile == null) {
                        Button(onClick = { confirmDownload = release },
                            shape = UaiShapes.button,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Baixar atualização")
                        }
                    }
                } else {
                    Text("Esta versão não possui SHA-256 verificável. O download interno foi bloqueado por segurança.",
                        color = MaterialTheme.colorScheme.error)
                }
            }
        }
        if (manager.downloading) {
            Surface(shape = UaiShapes.card, color = MaterialTheme.colorScheme.surfaceVariant) {
                Column(Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Baixando APK: ${manager.downloadPercent}%",
                        style = MaterialTheme.typography.titleSmall)
                    LinearProgressIndicator(progress = { manager.downloadPercent / 100f },
                        modifier = Modifier.fillMaxWidth())
                    Text("${GitHubDownloadProgress.readableRate(manager.downloadSpeedBytesPerSecond)} · " +
                        "${GitHubDownloadProgress.readableBytes(manager.downloadedBytes)} de " +
                        GitHubDownloadProgress.readableBytes(manager.downloadTotalBytes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    GitHubDownloadProgress.remainingSeconds(manager.downloadedBytes,
                        manager.downloadTotalBytes, manager.downloadSpeedBytesPerSecond)
                        ?.takeIf { it in 1..3599 }
                        ?.let { seconds ->
                            Text(if (seconds < 60) "Restam aproximadamente ${seconds}s"
                                else "Restam aproximadamente ${(seconds + 59) / 60} min",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                }
            }
        }
        manager.downloadError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (manager.readyFile != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Text("APK verificado. Instale com a confirmação do Android.",
                    style = MaterialTheme.typography.bodyMedium)
            }
            Button(onClick = {
                installMessage = manager.installDownloaded()
                    ?: "Instalador aberto. Confirme a atualização no Android."
            }, shape = UaiShapes.button,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Instalar atualização") }
        }
        installMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        if (manager.readyFile != null || check is GitHubUpdateResult.Available) {
            Text("Não desinstale o aplicativo. A atualização precisa ter a mesma assinatura para preservar seus projetos.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(20.dp))
    }
    confirmDownload?.let { release ->
        AlertDialog(onDismissRequest = { confirmDownload = null }, shape = UaiShapes.dialog,
            title = { Text("Baixar versão ${release.version}?") },
            text = { Text("O APK será baixado do GitHub e verificado por SHA-256. A instalação dependerá da sua autorização no Android. Seus projetos não serão modificados pelo download.") },
            confirmButton = { TextButton(onClick = {
                confirmDownload = null
                scope.launch { manager.download(release) }
            }, shape = UaiShapes.button) { Text("Baixar") } },
            dismissButton = { TextButton(onClick = { confirmDownload = null }, shape = UaiShapes.button) { Text("Cancelar") } })
    }
}
