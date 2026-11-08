package br.com.uaicortes

import android.content.Intent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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

/** Filtros visuais que não alteram o histórico nem disparam operações remotas. */
internal enum class SocialHistoryFilter(val label: String) {
    ALL("Todas"), PENDING("Pendentes"), RECEIVED("Recebidas"), REVIEW("Revisar");

    fun matches(status: SocialPublicationStatus): Boolean = when (this) {
        ALL -> true
        PENDING -> status in setOf(SocialPublicationStatus.PREPARED, SocialPublicationStatus.SHARED_UNCONFIRMED)
        RECEIVED -> status in setOf(SocialPublicationStatus.CONFIRMED_BY_API,
            SocialPublicationStatus.REMOTE_SUBMITTED, SocialPublicationStatus.CONFIRMED_MANUALLY)
        REVIEW -> status == SocialPublicationStatus.UPLOAD_REVIEW
    }
}

/** A UI do histórico não faz OAuth, upload ou persistência: todos exigem callback explícito. */
@Composable
internal fun SocialPublicationHistoryUi(
    data: SocialData,
    busy: Boolean,
    onRefresh: () -> Unit,
    onDetails: (SocialPublication) -> Unit,
    onShare: (SocialPublication) -> Unit,
    onYouTubeMetrics: (SocialPublication, SocialAccount) -> Unit,
    onYouTubeStatus: (SocialPublication, SocialAccount) -> Unit,
    onTikTokStatus: (SocialPublication) -> Unit
) {
    var filterName by rememberSaveable { mutableStateOf(SocialHistoryFilter.ALL.name) }
    var expandedPublicationId by rememberSaveable { mutableStateOf<String?>(null) }
    val filter = SocialHistoryFilter.entries.firstOrNull { it.name == filterName } ?: SocialHistoryFilter.ALL
    val posts = data.publications.filter { filter.matches(it.status) }
        .sortedByDescending { it.updatedAt }.take(80)

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("Envios pelo Uai Cortes (${data.publications.size})", modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium)
        TextButton(onClick = onRefresh, enabled = !busy) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Text("Atualizar lista")
        }
    }
    // Filtros compactos e roláveis em uma linha: não ocupam duas fileiras de cartões.
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SocialHistoryFilter.entries.forEach { choice ->
            FilterChip(selected = choice == filter,
                onClick = { filterName = choice.name }, label = { Text(choice.label) })
        }
    }
    if (posts.isEmpty()) {
        Text(if (data.publications.isEmpty()) "Nenhuma publicação preparada." else
            "Não há publicações neste filtro.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium)
    }
    posts.forEach { publication ->
        val account = data.accounts.firstOrNull { it.id == publication.accountId }
        OutlinedCard(shape = UaiShapes.card) {
            Column(Modifier.fillMaxWidth().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(publication.title, style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("${account?.label ?: "Conta indisponível"} · ${publication.status.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                when (publication.status) {
                    SocialPublicationStatus.UPLOAD_REVIEW ->
                        Text("Não há confirmação de recebimento. Confira a plataforma antes de " +
                            "tentar um novo envio para evitar duplicatas.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error)
                    SocialPublicationStatus.CONFIRMED_BY_API -> {
                        if (publication.youtubeState.checkedAt > 0L) {
                            Text(publication.youtubeState.summary(),
                                style = MaterialTheme.typography.bodySmall)
                            Text("Última consulta: " + SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))
                                .format(Date(publication.youtubeState.checkedAt)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Privacidade consultada: ${publication.youtubeState.privacyLabel()}. " +
                                "Resultado da última consulta, não monitoramento em tempo real.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    SocialPublicationStatus.SHARED_UNCONFIRMED ->
                        Text("O compartilhamento foi aberto, mas a postagem não foi confirmada.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    SocialPublicationStatus.REMOTE_SUBMITTED ->
                        Text("A plataforma recebeu a solicitação. Confira a conclusão por lá.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    else -> Unit
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = {
                        expandedPublicationId = if (expandedPublicationId == publication.id) null else publication.id
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (expandedPublicationId == publication.id) "Recolher ações" else "Ações e detalhes")
                        Spacer(Modifier.width(6.dp))
                        Icon(if (expandedPublicationId == publication.id) Icons.Default.ExpandLess
                            else Icons.Default.ExpandMore,
                            contentDescription = if (expandedPublicationId == publication.id)
                                "Recolher publicação" else "Expandir publicação")
                    }
                }
                if (expandedPublicationId == publication.id) {
                if (publication.metrics.hasData) Text(
                    "${publication.metrics.views ?: "—"} visualizações · " +
                        if (publication.metrics.source == "youtube_api") "YouTube API" else "Dados manuais",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)

                if (account != null && account.destination == SocialDestination.YOUTUBE &&
                    publication.status == SocialPublicationStatus.CONFIRMED_BY_API &&
                    publication.remoteVideoId.isNotBlank()) {
                    OutlinedButton(onClick = { onYouTubeStatus(publication, account) },
                        enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Sync, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp)); Text("Consultar status no YouTube")
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { onYouTubeMetrics(publication, account) },
                            enabled = !busy, modifier = Modifier.weight(1f)) {
                            Text("Métricas")
                        }
                        val studioUrl = "https://studio.youtube.com/video/${publication.remoteVideoId}/edit"
                        val context = androidx.compose.ui.platform.LocalContext.current
                        TextButton(onClick = {
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(studioUrl))) }
                        }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.OpenInNew, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp)); Text("Studio")
                        }
                    }
                }
                if (account != null && account.destination == SocialDestination.TIKTOK &&
                    publication.status == SocialPublicationStatus.REMOTE_SUBMITTED &&
                    publication.remoteVideoId.isNotBlank()) {
                    TextButton(onClick = { onTikTokStatus(publication) }) { Text("Consultar TikTok") }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Uma publicação confirmada não oferece Compartilhar como se fosse etapa pendente.
                    if (publication.status in setOf(SocialPublicationStatus.PREPARED,
                            SocialPublicationStatus.SHARED_UNCONFIRMED)) {
                        OutlinedButton(onClick = { onShare(publication) }, modifier = Modifier.weight(1f),
                            enabled = !busy) {
                            Icon(Icons.Default.Share, null, Modifier.size(17.dp))
                            Spacer(Modifier.width(4.dp)); Text("Compartilhar")
                        }
                    }
                    OutlinedButton(onClick = { onDetails(publication) },
                        modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Info, null, Modifier.size(17.dp))
                        Spacer(Modifier.width(4.dp)); Text("Detalhes")
                    }
                }
                } // Somente as ações secundárias são recolhidas; alertas permanecem visíveis.
            }
        }
    }
}
