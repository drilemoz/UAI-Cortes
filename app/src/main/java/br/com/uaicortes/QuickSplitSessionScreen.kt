package br.com.uaicortes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** Tela de execução separada das opções: não estima progresso interno do Media3. */
@Composable
internal fun QuickSplitProgressScreen(
    sourceName: String,
    partIndex: Int,
    totalParts: Int,
    completedParts: Int,
    phase: String,
    onCancel: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val total = totalParts.coerceAtLeast(1)
    val current = partIndex.coerceIn(1, total)
    val done = completedParts.coerceIn(0, total)
    val action = when (phase) {
        "Salvando MP4" -> "Salvando parte $current"
        "Extraindo áudio M4A" -> "Extraindo áudio da parte $current"
        "Gerando vídeo" -> "Cortando parte $current"
        else -> "Preparando divisão"
    }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Heading("Dividindo vídeo", onCancel)
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(Modifier.height(10.dp))
            Surface(shape = UaiShapes.card, color = colors.primaryContainer) {
                Icon(Icons.Default.ContentCut, contentDescription = null,
                    tint = colors.onPrimaryContainer, modifier = Modifier.padding(23.dp).size(42.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
                CircularProgressIndicator(modifier = Modifier.size(25.dp), strokeWidth = 3.dp)
                Text(action, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
            Text("Parte $current de $total", style = MaterialTheme.typography.titleMedium,
                color = colors.primary)
            Text(sourceName, style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            LinearProgressIndicator(progress = { done.toFloat() / total }, modifier = Modifier.fillMaxWidth())
            Text("$done de $total partes concluídas", style = MaterialTheme.typography.bodyMedium)
            Surface(shape = UaiShapes.card, color = colors.surfaceVariant.copy(alpha = 0.55f)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (done > 0) QuickSplitStatusRow("Partes 1 a $done", "Concluídas", Icons.Default.CheckCircle)
                    QuickSplitStatusRow("Parte $current", phase.ifBlank { "Preparando" }, Icons.Default.Sync)
                    if (current < total) QuickSplitStatusRow("Parte ${current + 1}", "Aguardando", Icons.Default.Schedule)
                }
            }
            Text("O vídeo original permanece intacto. As partes são salvas conforme ficam prontas.",
                style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            Spacer(Modifier.height(14.dp))
        }
        Surface(tonalElevation = 3.dp, color = colors.surface) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth().navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp).heightIn(min = 54.dp), shape = UaiShapes.button) {
                Icon(Icons.Default.Close, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Cancelar divisão")
            }
        }
    }
}

@Composable
private fun QuickSplitStatusRow(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Lista somente os arquivos retornados após a publicação real no MediaStore. */
@Composable
internal fun QuickSplitResultScreen(
    totalParts: Int,
    files: List<QuickSplitSavedFile>,
    issue: String,
    fileError: String,
    onOpen: (QuickSplitSavedFile) -> Unit,
    onNew: () -> Unit,
    onFinish: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val videos = files.count { !it.isAudio }
    val audios = files.count { it.isAudio }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Heading("Resultado da divisão", onFinish)
        Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Icon(if (issue.isBlank()) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                contentDescription = null, modifier = Modifier.size(42.dp),
                tint = if (issue.isBlank()) colors.primary else colors.error)
            Text(if (issue.isBlank()) "Divisão concluída" else "Divisão interrompida",
                style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("$videos de $totalParts MP4 salvos" +
                (if (audios > 0) " · $audios M4A salvos" else ""),
                style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            if (issue.isNotBlank()) {
                Surface(shape = UaiShapes.item, color = colors.errorContainer) {
                    Text(issue, modifier = Modifier.fillMaxWidth().padding(12.dp),
                        color = colors.onErrorContainer, style = MaterialTheme.typography.bodySmall)
                }
            }
            if (fileError.isNotBlank()) Text(fileError, color = colors.error,
                style = MaterialTheme.typography.bodySmall)
            Text("Arquivos gerados", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold)
        }
        if (files.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                Text("Nenhum arquivo concluído foi registrado nesta tentativa.",
                    style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
        } else {
            LazyColumn(Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(files, key = { it.uri.toString() }) { file ->
                    Surface(shape = UaiShapes.card, color = colors.surface,
                        border = BorderStroke(1.dp, colors.outlineVariant)) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(if (file.isAudio) Icons.Default.AudioFile else Icons.Default.VideoFile,
                                null, tint = colors.primary)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Parte ${file.partIndex.toString().padStart(3, '0')} · ${if (file.isAudio) "M4A" else "MP4"}",
                                    fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                Text(file.fileName, style = MaterialTheme.typography.bodySmall,
                                    color = colors.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                            IconButton(onClick = { onOpen(file) }) {
                                Icon(Icons.Default.OpenInNew, contentDescription = "Abrir ${file.fileName}")
                            }
                        }
                    }
                }
            }
        }
        Surface(tonalElevation = 3.dp, color = colors.surface) {
            Column(Modifier.fillMaxWidth().navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("MP4: Filmes/UaiCortes/Divididos" +
                    (if (audios > 0) " · M4A: Música/UaiCortes/Divididos" else ""),
                    style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                Button(onClick = onFinish, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    shape = UaiShapes.button) { Text("Concluído") }
                OutlinedButton(onClick = onNew, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    shape = UaiShapes.button) { Text("Nova divisão") }
            }
        }
    }
}
