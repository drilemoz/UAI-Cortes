package br.com.uaicortes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Pages
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Confirmação compartilhada pela Central de Integrações e por Minhas contas.
 * Exibe apenas dados públicos vindos da API; o token permanece fora do catálogo.
 * onConfirm continua sob responsabilidade do chamador, sem salvamento implícito.
 */
@Composable
internal fun SocialIdentifiedAccountDialog(
    destination: SocialDestination,
    accountName: String,
    publicId: String,
    linkingExisting: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val youtube = destination == SocialDestination.YOUTUBE
    val typeName = if (youtube) "canal" else "Página"
    val icon = if (youtube) Icons.Default.SmartDisplay else Icons.Default.Pages
    val actionLabel = if (linkingExisting) "Vincular" else "Adicionar"
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth().widthIn(max = 440.dp),
                shape = UaiShapes.dialog,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                shadowElevation = 8.dp
            ) {
                Column(
                    Modifier.verticalScroll(rememberScrollState()).padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircleOutline, null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(if (linkingExisting) "Vincular $typeName" else "Confirmar $typeName",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold)
                    }
                    Text("Identificado pela ${if (youtube) "API do YouTube" else "API do Facebook"}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall)
                    Surface(shape = UaiShapes.card,
                        color = MaterialTheme.colorScheme.surfaceVariant) {
                        Row(Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Icon(icon, null, tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(30.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(accountName, fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text("ID público: $publicId",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    Text(if (linkingExisting)
                        "Somente o ID público será vinculado. Nenhum token será salvo."
                    else
                        "Somente o nome e o ID públicos serão salvos. Nenhum token será armazenado.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = onDismiss,
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                            shape = UaiShapes.button) {
                            Text("Cancelar")
                        }
                        Button(onClick = onConfirm,
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                            shape = UaiShapes.button) {
                            Text(actionLabel)
                        }
                    }
                }
            }
        }
    }
}
