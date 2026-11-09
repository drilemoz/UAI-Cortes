package br.com.uaicortes

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.draw.clip
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
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
                thumbnailUrl = video.thumbnailUrl.ifBlank { localMatch.thumbnailUrl },
                fromUai = true
            )
        }
        return (remote + local.values.filter { localVideo -> remote.none { it.id == localVideo.id } })
            .sortedByDescending { it.publishedAt }
    }

    fun loadedChannelCount(videosByChannel: Map<String, List<YouTubeSocialApi.PreviousVideo>>): Int =
        videosByChannel.count { it.value.isNotEmpty() }

    fun matches(video: YouTubeSocialApi.PreviousVideo, text: String): Boolean {
        val search = text.trim()
        return search.isEmpty() || video.title.contains(search, ignoreCase = true) ||
            video.channelTitle.contains(search, ignoreCase = true)
    }

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
    var searching by rememberSaveable { mutableStateOf(false) }
    var visibleLimit by rememberSaveable { mutableIntStateOf(30) }
    val filtered = selectedVideos.filter { SocialYouTubeLibraryAggregate.matches(it, query) }
    val relevantStats = if (selected == null) channelStatistics.filterKeys { it in validIds }.values
        else listOfNotNull(channelStatistics[selected.remoteChannelId])
    val (views, videos, subscribers) = SocialYouTubeLibraryAggregate.channelTotals(relevantStats)
    val loadedCount = channelStatistics.keys.count { it in validIds }
    val nextPageChannels = if (selected != null) listOf(selected) else channels
    val context = LocalContext.current
    val lastChecked = relevantStats.map { it.checkedAt }.filter { it > 0L }.minOrNull()

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Visão geral", style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold)
            Text("Acompanhe seus canais do YouTube",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Surface(shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer) {
            Text("${channels.size} canais", Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = selected == null, onClick = { onSelectChannel(""); visibleLimit = 30 },
            shape = RoundedCornerShape(12.dp),
            label = { Text("Todos") }, leadingIcon = if (selected == null) { { Icon(Icons.Default.Check, null,
                Modifier.size(16.dp)) } } else null)
        channels.forEach { channel ->
            FilterChip(selected = selected?.remoteChannelId == channel.remoteChannelId,
                shape = RoundedCornerShape(12.dp),
                onClick = { onSelectChannel(channel.remoteChannelId); visibleLimit = 30 },
                label = { Text(channel.label, maxLines = 1) },
                leadingIcon = if (selected?.remoteChannelId == channel.remoteChannelId) { {
                    Icon(Icons.Default.Check, null, Modifier.size(16.dp))
                } } else null)
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SocialMetricTile("Visualizações", views, Icons.Default.Visibility, Modifier.weight(1f))
        SocialMetricTile("Vídeos", videos, Icons.Default.PlayCircleOutline, Modifier.weight(1f))
        SocialMetricTile("Inscritos", subscribers, Icons.Default.PeopleOutline, Modifier.weight(1f))
    }
    Text(if (selected == null) "$loadedCount de ${channels.size} canais com dados · totais da API" else
        if (relevantStats.isEmpty()) "Canal ainda não consultado" else
            "Última consulta: " + java.text.SimpleDateFormat("dd/MM HH:mm", Locale.forLanguageTag("pt-BR"))
                .format(java.util.Date(lastChecked ?: 0L)),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(4.dp))
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Vídeos publicados", style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold)
            Text("Miniaturas e desempenho dos vídeos carregados",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = { query = ""; searching = true },
            enabled = selectedVideos.isNotEmpty()) {
            Icon(Icons.Default.Search, "Pesquisar vídeos publicados")
        }
        IconButton(onClick = {
            if (selected != null) onLoad(selected, "") else onLoadAll(channels)
        }, enabled = !busy && channels.isNotEmpty()) {
            Icon(Icons.Default.Refresh, "Atualizar vídeos dos canais")
        }
    }
    if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
    if (error.isNotBlank()) Surface(shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer) {
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.ErrorOutline, null, tint = MaterialTheme.colorScheme.onErrorContainer)
            Text(error, color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodySmall)
        }
    }
    if (channels.isEmpty()) SocialVideosEmptyState(
        "Nenhum canal do YouTube cadastrado",
        "Adicione seus canais em Configurações > Minhas contas e canais.")
    else {
        // Pesquisa local acontece no diálogo e não dispara requisição a cada tecla.
        val knownViewCount = selectedVideos.count { it.viewCount != null }
        Text("${socialFormatCount(selectedVideos.size.toLong())} vídeos carregados · " +
            if (knownViewCount == 0) "visualizações não consultadas" else
                "${socialFormatCount(SocialYouTubeLibraryAggregate.totalViews(selectedVideos))} " +
                    "visualizações em $knownViewCount vídeo(s)",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (selectedVideos.isEmpty() && !busy) SocialVideosEmptyState(
            "Nenhum vídeo carregado",
            "Toque em atualizar para consultar os vídeos deste canal.")
        selectedVideos.take(visibleLimit).chunked(2).forEach { rowVideos ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rowVideos.forEach { video ->
                    SocialPublishedVideoCard(video, context, Modifier.weight(1f))
                }
                if (rowVideos.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        if (visibleLimit < selectedVideos.size) TextButton(onClick = { visibleLimit += 30 }) {
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
    if (searching) {
        Dialog(onDismissRequest = { searching = false }) {
            Surface(shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Pesquisar publicações", style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f))
                        IconButton(onClick = { searching = false }) {
                            Icon(Icons.Default.Close, "Fechar pesquisa")
                        }
                    }
                    OutlinedTextField(value = query, onValueChange = { query = it.take(100) },
                        label = { Text("Título ou canal") }, singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Clear, "Limpar pesquisa")
                        } }, modifier = Modifier.fillMaxWidth())
                    Text("${filtered.size} resultado(s) entre os vídeos carregados",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LazyColumn(Modifier.heightIn(max = 365.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(filtered, key = { it.id }) { video ->
                            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant).clickable {
                                openSocialYouTubeVideo(context, video.id)
                            }.padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                SocialYoutubeThumbnail(video.id, video.thumbnailUrl, Modifier.size(75.dp, 50.dp)
                                    .clip(RoundedCornerShape(7.dp)))
                                Column(Modifier.weight(1f)) {
                                    Text(video.title, maxLines = 2, overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                    Text(video.channelTitle, style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                }
                            }
                        }
                    }
                    if (filtered.isEmpty()) Text("Nenhum vídeo corresponde à pesquisa.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = { searching = false }, modifier = Modifier.align(Alignment.End)) {
                        Text("Fechar")
                    }
                }
            }
        }
    }
}

private fun openSocialYouTubeVideo(context: android.content.Context, id: String) {
    if (SocialPublicationPolicy.validVideoId(id) && id.isNotBlank()) runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW,
            Uri.parse("https://www.youtube.com/watch?v=$id")))
    }
}

/** Mesmo layout e os mesmos dados reais em temas claro, escuro e AMOLED. */
@Composable
private fun SocialPublishedVideoCard(
    video: YouTubeSocialApi.PreviousVideo, context: android.content.Context,
    modifier: Modifier = Modifier
) {
    OutlinedCard(onClick = { openSocialYouTubeVideo(context, video.id) },
        modifier = modifier, shape = RoundedCornerShape(18.dp)) {
        Column {
            Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
                SocialYoutubeThumbnail(video.id, video.thumbnailUrl, Modifier.fillMaxSize())
                Surface(modifier = Modifier.align(Alignment.BottomStart).padding(7.dp),
                    shape = RoundedCornerShape(7.dp), color = Color.Black.copy(alpha = 0.72f)) {
                    Text("YouTube", Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        color = Color.White, fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold)
                }
            }
            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(video.title, maxLines = 2, minLines = 2, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                Text(video.channelTitle.ifBlank { "Canal" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Icon(Icons.Default.Visibility, null, Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary)
                    Text(video.viewCount?.let(::socialFormatCount) ?: "—",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary,
                        maxLines = 1)
                }
                Text("visualizações", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SocialVideosEmptyState(title: String, detail: String) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.VideoLibrary, null, Modifier.size(30.dp),
                tint = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold)
            Text(detail, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SocialMetricTile(
    title: String, value: Long?, icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier
) {
    Surface(modifier = modifier, shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 13.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, null, Modifier.size(19.dp),
                tint = MaterialTheme.colorScheme.primary)
            Text(value?.let(::socialFormatCount) ?: "—",
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(title, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
