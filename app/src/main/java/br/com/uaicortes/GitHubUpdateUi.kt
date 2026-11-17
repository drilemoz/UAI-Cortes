package br.com.uaicortes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/** A notificação é automática; baixar e instalar continuam sempre manuais. */
@Composable internal fun GitHubUpdatePrompt(manager: GitHubUpdateManager, onOpenUpdates: () -> Unit) {
    val pending = manager.promptRelease ?: return
    val colors = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = manager::dismissPrompt,
        shape = UaiShapes.dialog,
        icon = {
            Surface(color = colors.primaryContainer, shape = UaiShapes.card) {
                Icon(Icons.Default.SystemUpdate, null,
                    tint = colors.primary,
                    modifier = Modifier.padding(12.dp).size(22.dp))
            }
        },
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(color = colors.secondaryContainer, shape = UaiShapes.item) {
                    Text("Versão nova pronta para baixar", modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge, color = colors.onSecondaryContainer)
                }
                Text("Atualização disponível", style = MaterialTheme.typography.headlineSmall)
                Text("v${pending.version}", style = MaterialTheme.typography.titleLarge,
                    color = colors.primary)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val summary = GitHubUpdateCore.displayNotes(pending.notes)
                if (summary.isNotBlank()) Text(summary, maxLines = 3,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                else Text("Há uma versão mais recente do Uai Cortes.")
                Surface(color = colors.surfaceVariant.copy(alpha = 0.7f), shape = UaiShapes.card) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null,
                            tint = colors.primary, modifier = Modifier.size(18.dp))
                        Text("Seus projetos não são alterados até você baixar e instalar o APK.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                manager.dismissPrompt()
                onOpenUpdates()
            }, shape = UaiShapes.button) {
                Text("Ver atualização")
            }
        },
        dismissButton = {
            TextButton(onClick = manager::dismissPrompt, shape = UaiShapes.button) { Text("Agora não") }
        }
    )
}

@Composable internal fun GitHubUpdatesScreen(manager: GitHubUpdateManager, back: () -> Unit) {
    val scope = rememberCoroutineScope()
    var confirmDownload by remember { mutableStateOf<GitHubUpdateRelease?>(null) }
    var installMessage by remember { mutableStateOf<String?>(null) }
    var confirmStop by remember { mutableStateOf(false) }
    val check = manager.result
    LaunchedEffect(manager.downloading, manager.paused, check) {
        if (manager.downloading || manager.paused || check is GitHubUpdateResult.Checking)
            installMessage = null
    }
    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Heading("Atualizações", back)
        Text("Versão instalada: ${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.titleMedium)
        Button(onClick = { scope.launch { manager.check() } },
            enabled = check !is GitHubUpdateResult.Checking && !manager.downloading && !manager.paused,
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
                        AvailableUpdateNotes(release)
                    }
                }
                if (release.sha256 != null) {
                    if (!manager.downloading && !manager.paused && manager.readyFile == null) {
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
        if (manager.downloading || manager.paused) {
            Surface(shape = UaiShapes.card, color = MaterialTheme.colorScheme.surfaceVariant) {
                Column(Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (manager.paused) "Download pausado: ${manager.downloadPercent}%"
                        else "Baixando APK: ${manager.downloadPercent}%",
                        style = MaterialTheme.typography.titleSmall)
                    LinearProgressIndicator(progress = { manager.downloadPercent / 100f },
                        modifier = Modifier.fillMaxWidth())
                    Text("${GitHubDownloadProgress.readableRate(manager.downloadSpeedBytesPerSecond)} · " +
                        "${GitHubDownloadProgress.readableBytes(manager.downloadedBytes)} de " +
                        GitHubDownloadProgress.readableBytes(manager.downloadTotalBytes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (manager.downloading) GitHubDownloadProgress.remainingSeconds(
                        manager.downloadedBytes, manager.downloadTotalBytes,
                        manager.downloadSpeedBytesPerSecond)?.takeIf { it in 1..3599 }?.let { seconds ->
                        Text(if (seconds < 60) "Restam aproximadamente ${seconds}s"
                            else "Restam aproximadamente ${(seconds + 59) / 60} min",
                            style = MaterialTheme.typography.bodySmall)
                    }
                    val release = (check as? GitHubUpdateResult.Available)?.release
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        CompactActionTile(if (manager.paused) "Retomar" else "Pausar",
                            if (manager.paused) Icons.Default.PlayArrow else Icons.Default.PauseCircle,
                            primary = true, enabled = release?.let(manager::canResume) == true || manager.downloading) {
                            if (manager.paused && release != null) scope.launch { manager.resumeDownload(release) }
                            else manager.pauseDownload()
                        }
                        CompactActionTile("Parar", Icons.Default.SystemUpdate) { confirmStop = true }
                        CompactActionTile("Reiniciar", Icons.Default.RestartAlt, enabled = release != null) {
                            if (release != null) scope.launch { manager.restartDownload(release) }
                        }
                    }
                    if (manager.paused) Text("O download parcial será verificado antes da instalação. " +
                        "Se o servidor não aceitar retomada, o download recomeçará do zero.",
                        style = MaterialTheme.typography.bodySmall)
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
        if (!manager.downloading && !manager.paused)
            installMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        if (manager.readyFile != null || check is GitHubUpdateResult.Available) {
            Text("Não desinstale o aplicativo. A atualização precisa ter a mesma assinatura para preservar seus projetos.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(20.dp))
    }
    if (confirmStop) AlertDialog(onDismissRequest = { confirmStop = false },
        shape = UaiShapes.dialog,
        icon = { Icon(Icons.Default.PauseCircle, contentDescription = null) },
        title = { Text("Interromper download?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Parar remove o APK incompleto. Para apenas suspender, use Pausar.")
                Surface(color = MaterialTheme.colorScheme.errorContainer, shape = UaiShapes.card) {
                    Text("O arquivo parcial será apagado.", modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        },
        confirmButton = { TextButton(onClick = {
            confirmStop = false
            manager.stopDownload()
        }) { Text("Parar e apagar parcial") } },
        dismissButton = { TextButton(onClick = { confirmStop = false }) { Text("Continuar") } })
    confirmDownload?.let { release ->
        AlertDialog(onDismissRequest = { confirmDownload = null }, shape = UaiShapes.dialog,
            icon = { Icon(Icons.Default.Download, contentDescription = null) },
            title = { Text("Baixar versão ${release.version}?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("O APK será baixado do GitHub e verificado por SHA-256. A instalação dependerá da sua autorização no Android.")
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f), shape = UaiShapes.card) {
                        Text("Seus projetos não serão modificados pelo download.", modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            confirmButton = { Button(onClick = {
                confirmDownload = null
                scope.launch { manager.download(release) }
            }, shape = UaiShapes.button) { Text("Baixar") } },
            dismissButton = { TextButton(onClick = { confirmDownload = null }, shape = UaiShapes.button) { Text("Cancelar") } })
    }
}


@Composable
private fun AvailableUpdateNotes(release: GitHubUpdateRelease) {
    var expanded by remember(release.version) { mutableStateOf(false) }
    val summary = GitHubUpdateCore.displayNotes(release.notes)
    Text("O que mudou", style = MaterialTheme.typography.labelLarge)
    Text(summary.ifBlank { "A publicação não trouxe notas para esta versão." },
        maxLines = if (expanded) 20 else 4,
        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        style = MaterialTheme.typography.bodySmall)
    if (summary.isNotBlank() || release.previousNotes.isNotEmpty()) TextButton(onClick = { expanded = !expanded }) {
        Text(if (expanded) "Ver menos" else "Ver notas")
    }
    if (expanded) release.previousNotes.forEach { note ->
        Text("Versão ${note.version}", style = MaterialTheme.typography.labelLarge)
        Text(GitHubUpdateCore.displayNotes(note.notes), style = MaterialTheme.typography.bodySmall)
    }
}
