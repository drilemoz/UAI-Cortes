package br.com.uaicortes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** Cartão de conta: informações essenciais visíveis, ações secundárias sob demanda. */
@Composable
internal fun SocialAccountCard(
    account: SocialAccount,
    status: String,
    actionLabel: String?,
    busy: Boolean,
    onAction: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    var expanded by rememberSaveable(account.id) { mutableStateOf(false) }
    OutlinedCard(modifier = modifier, shape = UaiShapes.card) {
        Column(Modifier.fillMaxWidth().padding(horizontal = if (compact) 10.dp else 12.dp,
            vertical = if (compact) 4.dp else 6.dp)) {
            Row(Modifier.fillMaxWidth().clickable { expanded = !expanded }
                    .heightIn(min = if (compact) 48.dp else 52.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Icon(when (account.destination) {
                    SocialDestination.YOUTUBE -> Icons.Default.SmartDisplay
                    SocialDestination.FACEBOOK_PAGE, SocialDestination.FACEBOOK_PROFILE -> Icons.Default.People
                    else -> Icons.Default.Share
                }, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(if (compact) 8.dp else 12.dp))
                Column(Modifier.weight(1f)) {
                    Text(account.label, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.SemiBold)
                    Text("${account.destination.label} · $status",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (compact) 1 else 2, overflow = TextOverflow.Ellipsis)
                }
                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Recolher conta" else "Gerenciar conta")
            }
            if (expanded) {
                HorizontalDivider(Modifier.padding(vertical = 6.dp))
                val publicId = when (account.destination) {
                    SocialDestination.YOUTUBE -> account.remoteChannelId
                    SocialDestination.FACEBOOK_PAGE -> account.remotePageId
                    else -> ""
                }
                if (publicId.isNotBlank()) Text("ID público: $publicId",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (account.destination == SocialDestination.YOUTUBE && publicId.isNotBlank())
                    Text("A autorização será conferida ao publicar; canal cadastrado não significa login permanente.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (compact) {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
                        if (actionLabel != null) TextButton(onClick = onAction, enabled = !busy) {
                            Text(actionLabel, maxLines = 1)
                        }
                        IconButton(onClick = onDelete, enabled = !busy) {
                            Icon(Icons.Default.DeleteOutline, "Remover destino")
                        }
                    }
                } else {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End) {
                        if (actionLabel != null) TextButton(onClick = onAction, enabled = !busy) {
                            Text(actionLabel)
                        }
                        IconButton(onClick = onDelete, enabled = !busy) {
                            Icon(Icons.Default.DeleteOutline, "Remover destino")
                        }
                    }
                }
            }
        }
    }
}
