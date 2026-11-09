package br.com.uaicortes

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.time.Instant
import java.util.Locale

/** Dados consultados nunca são confundidos com o histórico de envios. */
internal object SocialYouTubeLibraryAggregate {
    fun merge(videosByChannel: Map<String, List<YouTubeSocialApi.PreviousVideo>>): List<YouTubeSocialApi.PreviousVideo> =
        videosByChannel.values.asSequence().flatten().distinctBy { it.id }
            .sortedByDescending { it.publishedAt }.toList()

    fun mergeWithLocal(
        videosByChannel: Map<String, List<YouTubeSocialApi.PreviousVideo>>,
        publications: List<SocialPublication>,
        accounts: List<SocialAccount>
    ): List<YouTubeSocialApi.PreviousVideo> {
        val knownChannels = accounts.filter { it.destination == SocialDestination.YOUTUBE &&
            it.remoteChannelId.isNotBlank() }.associateBy { it.id }
        val local = publications.asSequence().filter {
            it.status == SocialPublicationStatus.CONFIRMED_BY_API &&
                SocialPublicationPolicy.validVideoId(it.remoteVideoId) && it.remoteVideoId.isNotBlank() &&
                knownChannels.containsKey(it.accountId)
        }.map { post ->
            val channel = knownChannels.getValue(post.accountId)
            YouTubeSocialApi.PreviousVideo(
                id = post.remoteVideoId, title = post.title.take(160),
                publishedAt = Instant.ofEpochMilli(post.createdAt.coerceIn(0L, 253_402_300_799_000L)).toString(),
                viewCount = post.metrics.views, channelId = channel.remoteChannelId,
                channelTitle = channel.label, fromUai = true
            )
        }.toList().associateBy { it.id }
        val remote = merge(videosByChannel).map { video ->
            val localMatch = local[video.id]
            if (localMatch == null) video else video.copy(
                viewCount = video.viewCount ?: localMatch.viewCount,
                channelId = video.channelId.ifBlank { localMatch.channelId },
                channelTitle = video.channelTitle.ifBlank { localMatch.channelTitle },
                fromUai = true
            )
        }
        return (remote + local.values.filter { localVideo -> remote.none { it.id == localVideo.id } })
            .sortedByDescending { it.publishedAt }
    }

    fun loadedChannelCount(videosByChannel: Map<String, List<YouTubeSocialApi.PreviousVideo>>): Int =
        videosByChannel.keys.size

    fun totalViews(videos: List<YouTubeSocialApi.PreviousVideo>): Long =
        videos.sumOf { it.viewCount ?: 0L }

    /** Apenas canais consultados, nunca inventa dados para os demais. */
    fun channelTotals(stats: Collection<YouTubeSocialApi.ChannelStatistics>): Triple<Long?, Long?, Long?> {
        fun sumKnown(values: List<Long?>): Long? =
            if (values.isEmpty() || values.any { it == null }) null else values.filterNotNull().sum()
        return Triple(sumKnown(stats.map { it.views }), sumKnown(stats.map { it.videos }),
            sumKnown(stats.map { it.subscribers }))
    }
}

internal fun socialFormatCount(value: Long): String =
    NumberFormat.getIntegerInstance(Locale.forLanguageTag("pt-BR")).format(value)

/** Lista única com filtros por canal; nenhuma consulta automática ou persistência de OAuth. */
@Composable
internal fun SocialYouTubeLibraryUi(
    accounts: List<SocialAccount>,
    selectedChannelId: String,
    onSelectChannel: (String) -> Unit,
    busy: Boolean,
    videosByChannel: Map<String, List<YouTubeSocialApi.PreviousVideo>>,
    nextPageTokensByChannel: Map<String, String>,
    channelStatistics: Map<String, YouTubeSocialApi.ChannelStatistics>,
    publications: List<SocialPublication>,
    error: String,
    onLoad: (SocialAccount, String) -> Unit,
    onLoadAll: (List<SocialAccount>) -> Unit
) {
    val channels = accounts.filter {
        it.destination == SocialDestination.YOUTUBE && it.remoteChannelId.isNotBlank()
    }
    val selected = channels.firstOrNull { it.remoteChannelId == selectedChannelId }
    val validIds = channels.map { it.remoteChannelId }.toSet()
    val allVideos = remember(videosByChannel, publications, accounts) {
        SocialYouTubeLibraryAggregate.mergeWithLocal(videosByChannel, publications, accounts)
    }
    val selectedVideos = allVideos.filter {
        it.channelId in validIds && (selected == null || it.channelId == selected.remoteChannelId)
    }
    var query by rememberSaveable { mutableStateOf("") }
    var visibleLimit by rememberSaveable { mutableIntStateOf(30) }
    val filtered = selectedVideos.filter { it.title.contains(query.trim(), ignoreCase = true) }
    val relevantStats = if (selected == null) channelStatistics.filterKeys { it in validIds }.values
        else listOfNotNull(channelStatistics[selected.remoteChannelId])
    val (views, videos, subscribers) = SocialYouTubeLibraryAggregate.channelTotals(relevantStats)
    val loadedCount = channelStatistics.keys.count { it in validIds }
    val nextPageChannels = if (selected != null) listOf(selected) else channels
    val context = LocalContext.current
    val lastChecked = relevantStats.map { it.checkedAt }.filter { it > 0L }.minOrNull()

    Text("Canais (${channels.size})", style = MaterialTheme.typography.titleMedium)
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = selected == null, onClick = { onSelectChannel(""); visibleLimit = 30 },
            label = { Text("Todos") }, leadingIcon = if (selected == null) { { Icon(Icons.Default.Check, null,
                Modifier.size(16.dp)) } } else null)
        channels.forEach { channel ->
            FilterChip(selected = selected?.remoteChannelId == channel.remoteChannelId,
                onClick = { onSelectChannel(channel.remoteChannelId); visibleLimit = 30 },
                label = { Text(channel.label, maxLines = 1) },
                leadingIcon = if (selected?.remoteChannelId == channel.remoteChannelId) { {
                    Icon(Icons.Default.Check, null, Modifier.size(16.dp))
                } } else null)
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SocialMetricTile("Visualizações", views, Modifier.weight(1f))
        SocialMetricTile("Vídeos", videos, Modifier.weight(1f))
        SocialMetricTile("Inscritos", subscribers, Modifier.weight(1f))
    }
    Text(if (selected == null) "$loadedCount de ${channels.size} canais com dados · totais da API" else
        if (relevantStats.isEmpty()) "Canal ainda não consultado" else
            "Última consulta: " + java.text.SimpleDateFormat("dd/MM HH:mm", Locale.forLanguageTag("pt-BR"))
                .format(java.util.Date(lastChecked ?: 0L)),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("Vídeos do YouTube", style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f))
        TextButton(onClick = {
            if (selected != null) onLoad(selected, "") else onLoadAll(channels)
        }, enabled = !busy && channels.isNotEmpty()) {
            Icon(Icons.Default.Refresh, null, Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text(if (selected == null) "Atualizar canais" else "Atualizar canal")
        }
    }
    if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
    if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall)
    if (channels.isEmpty()) Text("Adicione um canal YouTube em Gerenciar para consultar os vídeos.",
        style = MaterialTheme.typography.bodySmall)
    else {
        if (selectedVideos.isNotEmpty()) {
            OutlinedTextField(value = query, onValueChange = { query = it.take(100); visibleLimit = 30 },
                label = { Text("Pesquisar vídeos carregados") },
                leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true,
                modifier = Modifier.fillMaxWidth(), shape = UaiShapes.item)
        }
        val knownViewCount = selectedVideos.count { it.viewCount != null }
        Text("${socialFormatCount(selectedVideos.size.toLong())} vídeos carregados · " +
            if (knownViewCount == 0) "visualizações não consultadas" else
                "${socialFormatCount(SocialYouTubeLibraryAggregate.totalViews(selectedVideos))} " +
                    "visualizações em $knownViewCount vídeo(s)",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (filtered.isEmpty() && !busy) Text(
            if (selectedVideos.isEmpty()) "Toque em Atualizar para consultar o YouTube. " +
                "Vídeos confirmados pelo Uai Cortes também aparecem aqui."
            else "Nenhum título encontrado entre os vídeos carregados.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        filtered.take(visibleLimit).forEach { video ->
            OutlinedCard(shape = UaiShapes.card) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(video.title, style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold, maxLines = 2,
                        overflow = TextOverflow.Ellipsis)
                    val details = buildList {
                        if (selected == null && video.channelTitle.isNotBlank()) add(video.channelTitle)
                        if (video.publishedAt.length >= 10) add(video.publishedAt.take(10))
                        add(video.viewCount?.let { "${socialFormatCount(it)} visualizações" }
                            ?: "Visualizações não consultadas")
                    }
                    Text(details.joinToString(" · "), style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (video.fromUai) Text("Enviado pelo Uai Cortes",
                            modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary)
                        else Spacer(Modifier.weight(1f))
                        TextButton(onClick = {
                            if (SocialPublicationPolicy.validVideoId(video.id)) runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW,
                                    Uri.parse("https://www.youtube.com/watch?v=${video.id}")))
                            }
                        }) {
                            Icon(Icons.Default.OpenInNew, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp)); Text("Abrir")
                        }
                    }
                }
            }
        }
        if (visibleLimit < filtered.size) TextButton(onClick = { visibleLimit += 30 }) {
            Text("Mostrar mais vídeos carregados")
        }
        val toLoadMore = nextPageChannels.filter { nextPageTokensByChannel[it.remoteChannelId].orEmpty().isNotBlank() }
        if (toLoadMore.isNotEmpty()) {
            val current = toLoadMore.first()
            OutlinedButton(onClick = { onLoad(current, nextPageTokensByChannel[current.remoteChannelId].orEmpty()) },
                modifier = Modifier.fillMaxWidth(), enabled = !busy) {
                Text("Carregar mais vídeos de ${current.label}", maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
            }
        }
        Text("Os totais do canal vêm do YouTube; a lista inclui somente vídeos já consultados " +
            "e envios confirmados pelo aplicativo. Não há busca automática em segundo plano.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SocialMetricTile(title: String, value: Long?, modifier: Modifier) {
    Surface(modifier = modifier, shape = UaiShapes.item,
        color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(Modifier.padding(horizontal = 5.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value?.let(::socialFormatCount) ?: "—",
                style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(title, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
