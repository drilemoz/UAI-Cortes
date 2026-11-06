package br.com.uaicortes

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

/** O histórico guarda só ajustes e referências: arquivos originais continuam necessários. */
@Composable
internal fun ProjectBackupSettingsScreen(
    backups: ProjectBackupStore,
    projects: List<VideoProject>,
    onRestore: (VideoProject) -> Boolean,
    back: () -> Unit
) {
    val settings = backups.preferences
    val scope = rememberCoroutineScope()
    var enabled by remember { mutableStateOf(settings.enabled) }
    var interval by remember { mutableIntStateOf(settings.intervalSeconds) }
    var count by remember { mutableIntStateOf(settings.retention) }
    var external by remember { mutableStateOf(settings.externalEnabled) }
    var folderSet by remember { mutableStateOf(!settings.externalFolder.isNullOrBlank()) }
    var message by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var refresh by remember { mutableIntStateOf(0) }
    var history by remember { mutableStateOf<List<ProjectBackupEntry>>(emptyList()) }
    var selectedId by remember { mutableStateOf(projects.firstOrNull()?.id.orEmpty()) }
    LaunchedEffect(projects) {
        if (projects.none { it.id == selectedId }) selectedId = projects.firstOrNull()?.id.orEmpty()
    }
    var chooseProject by remember { mutableStateOf(false) }
    var pendingRestore by remember { mutableStateOf<VideoProject?>(null) }
    var confirmOrphanCleanup by remember { mutableStateOf(false) }
    var storageUsage by remember { mutableStateOf<BackupStorageUsage?>(null) }
    val selectedProject = projects.firstOrNull { it.id == selectedId }
    LaunchedEffect(refresh, projects) {
        val (records, stats) = withContext(Dispatchers.IO) {
            backups.listAll() to backups.storageUsage(projects.map { it.id }.toSet())
        }
        history = records
        storageUsage = stats
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val p = selectedProject
        if (uri != null && p != null) scope.launch {
            busy = true
            message = withContext(Dispatchers.IO) {
                runCatching { backups.exportTo(uri, p); "Backup exportado para a pasta escolhida." }
                    .getOrElse { "Falha ao exportar backup; confira espaço e permissão." }
            }
            busy = false
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            busy = true
            val imported = withContext(Dispatchers.IO) { runCatching { backups.importFrom(uri) }.getOrNull() }
            busy = false
            if (imported != null) pendingRestore = imported
            else message = "Backup inválido, incompleto ou indisponível. Nenhum projeto foi substituído."
        }
    }
    val folderLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            val ok = runCatching { backups.chooseFolder(uri) }.isSuccess
            folderSet = ok
            message = if (ok) "Pasta de backup externo autorizada." else "Não foi possível autorizar a pasta."
            if (ok) { settings.externalEnabled = true; external = true }
        }
    }
    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())
        .padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Heading("Salvamento e segurança", back)
        Text("Edições são temporárias até você escolher Salvar ao sair. Cópias históricas são feitas somente de projetos confirmados.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text("Cópias automáticas", fontWeight = FontWeight.SemiBold)
                Text("Somente quando o projeto for alterado", style = MaterialTheme.typography.bodySmall)
            }
            Switch(checked = enabled, onCheckedChange = { enabled = it; settings.enabled = it })
        }
        Text("Intervalo", fontWeight = FontWeight.SemiBold)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            ProjectBackupPolicy.intervals.forEach { seconds ->
                FilterChip(selected = interval == seconds, enabled = enabled,
                    onClick = { interval = seconds; settings.intervalSeconds = seconds },
                    label = { Text(if (seconds < 60) "${seconds}s" else "${seconds/60}min") })
            }
        }
        Text("Versões por projeto", fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ProjectBackupPolicy.counts.forEach { number ->
                FilterChip(selected = count == number,
                    onClick = { count = number; settings.retention = number },
                    label = { Text("$number cópias") })
            }
        }
        HorizontalDivider()
        Text("Backup fora do aplicativo", fontWeight = FontWeight.SemiBold)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Copiar automaticamente para pasta escolhida", modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium)
            Switch(checked = external, onCheckedChange = { desired ->
                if (desired && !folderSet) folderLauncher.launch(null)
                else { external = desired; settings.externalEnabled = desired }
            })
        }
        OutlinedButton(onClick = { folderLauncher.launch(null) }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.FolderOpen, null); Spacer(Modifier.width(8.dp))
            Text(if (folderSet) "Trocar pasta de backup" else "Escolher pasta de backup")
        }
        settings.lastExternalError?.let { error ->
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        HorizontalDivider()
        Text("Exportar ou restaurar projeto", fontWeight = FontWeight.SemiBold)
        Box {
            OutlinedButton(onClick = { chooseProject = true }, modifier = Modifier.fillMaxWidth(),
                enabled = projects.isNotEmpty()) { Text("Projeto: ${selectedProject?.title ?: "Nenhum"}") }
            DropdownMenu(expanded = chooseProject, onDismissRequest = { chooseProject = false }) {
                projects.forEach { project ->
                    DropdownMenuItem(text = { Text(project.title) }, onClick = {
                        selectedId = project.id; chooseProject = false
                    })
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = {
                selectedProject?.let { project ->
                    exportLauncher.launch("UaiCortes-${project.id.take(8)}-backup.json")
                }
            }, enabled = !busy && selectedProject != null, modifier = Modifier.weight(1f)) {
                Text("Exportar")
            }
            OutlinedButton(onClick = { importLauncher.launch(arrayOf("*/*")) }, enabled = !busy,
                modifier = Modifier.weight(1f)) { Text("Importar") }
        }
        Button(onClick = {
            selectedProject?.let { project ->
                scope.launch {
                    busy = true
                    val result = withContext(Dispatchers.IO) {
                        runCatching { backups.snapshot(project.id) }.getOrNull()
                    }
                    message = when (result) {
                        true -> "Cópia de segurança criada."
                        false -> "Não há mudanças novas ou as cópias automáticas estão desativadas."
                        null -> "Não foi possível criar a cópia; projeto anterior preservado."
                    }
                    refresh++; busy = false
                }
            }
        }, enabled = !busy && selectedProject != null && enabled, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Backup, null); Spacer(Modifier.width(8.dp)); Text("Criar cópia agora")
        }
        HorizontalDivider()
        Text("Armazenamento dos backups", fontWeight = FontWeight.SemiBold)
        storageUsage?.let { usage ->
            Text("${usage.snapshotCount} cópias locais · ${usage.bytes / 1024L} KiB usados",
                style = MaterialTheme.typography.bodySmall)
            Text("Pasta privada: ${usage.privateFolder}",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Rascunho emergencial separado: files/editor_draft_recovery.json. Não aparece em Projetos e não é backup permanente.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${usage.orphanDirectories} pasta(s) de backup de projetos excluídos · ${usage.orphanBytes / 1024L} KiB",
                style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = { confirmOrphanCleanup = true },
                enabled = !busy && usage.orphanDirectories > 0,
                modifier = Modifier.fillMaxWidth()) { Text("Limpar backups órfãos") }
        }
        Text("Backups externos ficam exclusivamente na pasta que você autorizou; esta limpeza não toca neles.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        HorizontalDivider()
        Text("Recuperar versão anterior", fontWeight = FontWeight.SemiBold)
        if (history.isEmpty()) Text("Nenhuma cópia histórica disponível.", style = MaterialTheme.typography.bodySmall)
        history.take(20).forEach { entry ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(entry.project.title, maxLines = 1)
                    Text(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(entry.timestamp)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { pendingRestore = entry.project }, enabled = !busy) {
                    Icon(Icons.Default.Restore, "Restaurar esta versão")
                }
            }
        }
        Text("⚠️ Backups contêm configurações e referências, não os vídeos, fotos ou áudios originais. " +
            "Apagar mídias de Download pode impedir a reabertura de projetos que as utilizam. " +
            "Os MP4s exportados ficam em Filmes/UaiCortes.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        message?.let { Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall) }
        Spacer(Modifier.height(16.dp))
    }
    if (confirmOrphanCleanup) AlertDialog(
        onDismissRequest = { confirmOrphanCleanup = false },
        title = { Text("Excluir backups de projetos apagados?") },
        text = { Text("Serão removidas apenas as cópias locais de projetos que não existem mais no catálogo. Projetos salvos e arquivos de vídeo, foto e áudio não serão apagados. Backups externos não serão modificados.") },
        confirmButton = { TextButton(onClick = {
            confirmOrphanCleanup = false
            scope.launch {
                busy = true
                val count = withContext(Dispatchers.IO) {
                    runCatching { backups.deleteOrphanedSnapshots(projects.map { it.id }.toSet()) }
                }
                message = count.fold(
                    onSuccess = { "$it pasta(s) de cópias órfãs removida(s)." },
                    onFailure = { "Não foi possível concluir a limpeza; outras cópias foram preservadas." })
                refresh++
                busy = false
            }
        }) { Text("Excluir cópias órfãs") } },
        dismissButton = { TextButton(onClick = { confirmOrphanCleanup = false }) { Text("Cancelar") } })
    pendingRestore?.let { restored ->
        AlertDialog(onDismissRequest = { pendingRestore = null },
            title = { Text("Restaurar projeto?") },
            text = { Text("Restaurar '${restored.title}' substituirá a versão atual do mesmo projeto, " +
                "se existir. Textos, clipes e ajustes voltarão ao backup. As mídias originais não são copiadas.") },
            confirmButton = { TextButton(onClick = {
                pendingRestore = null
                scope.launch {
                    busy = true
                    // Não perder a versão corrente ao restaurar uma mais antiga.
                    val oldExists = projects.any { it.id == restored.id }
                    val safe = if (!oldExists) true else withContext(Dispatchers.IO) {
                        runCatching { backups.snapshot(restored.id, force = true) }.isSuccess
                    }
                    val ok = if (safe) onRestore(restored) else false
                    message = if (ok) "Projeto restaurado. Você pode abri-lo em Projetos."
                        else "Restauração não realizada; não foi possível preservar a versão anterior ou salvar."
                    refresh++; busy = false
                }
            }) { Text("Restaurar") } },
            dismissButton = { TextButton(onClick = { pendingRestore = null }) { Text("Cancelar") } })
    }
}
