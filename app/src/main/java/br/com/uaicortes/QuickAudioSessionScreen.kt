package br.com.uaicortes

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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

/** A extração não expõe avanço por bytes/quadros. Indicador é indeterminado. */
@Composable
internal fun QuickAudioProgressScreen(
    sourceName: String,
    durationMs: Long,
    phase: String,
    onCancel: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Heading("Extraindo áudio", onCancel)
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Spacer(Modifier.height(12.dp))
            Surface(shape = UaiShapes.card, color = colors.primaryContainer) {
                Icon(Icons.Default.GraphicEq, contentDescription = null,
                    modifier = Modifier.padding(24.dp).size(42.dp), tint = colors.onPrimaryContainer)
            }
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
                CircularProgressIndicator(modifier = Modifier.size(25.dp), strokeWidth = 3.dp)
                Text(phase.ifBlank { "Preparando extração" },
                    style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
            Text("Transformando a faixa sonora em M4A", style = MaterialTheme.typography.titleMedium,
                color = colors.primary)
            Surface(shape = UaiShapes.card, color = colors.surfaceVariant.copy(alpha = 0.55f)) {
                Column(Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Vídeo de origem", style = MaterialTheme.typography.labelMedium,
                        color = colors.onSurfaceVariant)
                    Text(sourceName, style = MaterialTheme.typography.bodyLarge, maxLines = 3,
                        overflow = TextOverflow.Ellipsis)
                    Text("Duração: ${formatMs(durationMs)}", style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant)
                }
            }
            Text("Etapas: extrair áudio → salvar M4A no local escolhido.",
                style = MaterialTheme.typography.bodyMedium)
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text("A duração do processamento depende do vídeo. O progresso interno não é estimado.",
                style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            Text("O vídeo original não será alterado.",
                style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }
        Surface(tonalElevation = 3.dp, color = colors.surface) {
            OutlinedButton(onClick = onCancel,
                modifier = Modifier.fillMaxWidth().navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp).heightIn(min = 54.dp),
                shape = UaiShapes.button) {
                Icon(Icons.Default.Close, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Cancelar extração")
            }
        }
    }
}

/** Apenas arquivos cuja gravação terminou com sucesso podem ser abertos. */
@Composable
internal fun QuickAudioResultScreen(
    sourceName: String,
    audioName: String,
    savedUri: Uri?,
    issue: String,
    fileError: String,
    incompleteUri: Uri?,
    deletingIncomplete: Boolean,
    onRemoveIncomplete: () -> Unit,
    onOpen: (Uri) -> Unit,
    onNew: () -> Unit,
    onFinish: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val success = savedUri != null && issue.isBlank()
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Heading("Resultado da extração", onFinish)
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Icon(if (success) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                contentDescription = null, modifier = Modifier.size(48.dp),
                tint = if (success) colors.primary else colors.error)
            Text(if (success) "Áudio extraído com sucesso" else "Extração interrompida",
                style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(sourceName, style = MaterialTheme.typography.bodyMedium, maxLines = 2,
                overflow = TextOverflow.Ellipsis, color = colors.onSurfaceVariant)
            if (issue.isNotBlank()) {
                Surface(shape = UaiShapes.item, color = colors.errorContainer) {
                    Text(issue, modifier = Modifier.fillMaxWidth().padding(12.dp),
                        color = colors.onErrorContainer, style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (success) {
                Text("Arquivo gerado", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold)
                Surface(shape = UaiShapes.card, color = colors.surface,
                    border = BorderStroke(1.dp, colors.outlineVariant)) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AudioFile, contentDescription = null, tint = colors.primary)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Arquivo M4A", fontWeight = FontWeight.SemiBold)
                            Text(audioName, style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant, maxLines = 3,
                                overflow = TextOverflow.Ellipsis)
                        }
                        IconButton(onClick = { onOpen(savedUri!!) }) {
                            Icon(Icons.Default.OpenInNew, contentDescription = "Abrir arquivo M4A")
                        }
                    }
                }
                OutlinedButton(onClick = { onOpen(savedUri!!) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = UaiShapes.button) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Ouvir áudio")
                }
                Text("Salvo no destino escolhido no Android. O arquivo original foi preservado.",
                    style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            } else {
                Text("Nenhum M4A concluído foi confirmado nesta tentativa. O Android pode ter criado um documento incompleto. Você pode tentar excluí-lo abaixo; alguns provedores exigem exclusão manual no app Arquivos.",
                    style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
            if (!success && incompleteUri != null) {
                OutlinedButton(onClick = onRemoveIncomplete, enabled = !deletingIncomplete,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp), shape = UaiShapes.button) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (deletingIncomplete) "Excluindo…" else "Excluir M4A incompleto")
                }
            }
            if (fileError.isNotBlank()) Text(fileError, style = MaterialTheme.typography.bodySmall,
                color = if (fileError == "O M4A incompleto foi excluído.") colors.primary else colors.error, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        }
        Surface(tonalElevation = 3.dp, color = colors.surface) {
            Column(Modifier.fillMaxWidth().navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onFinish, enabled = !deletingIncomplete,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    shape = UaiShapes.button) { Text("Concluído") }
                OutlinedButton(onClick = onNew, enabled = !deletingIncomplete,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    shape = UaiShapes.button) { Text("Nova extração") }
            }
        }
    }
}
