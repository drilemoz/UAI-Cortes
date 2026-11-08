package br.com.uaicortes

import android.content.Intent
import android.net.Uri
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
import java.util.Locale

internal object SocialYouTubeLibraryAggregate {
    fun merge(videosByChannel: Map<String, List<YouTubeSocialApi.PreviousVideo>>): List<YouTubeSocialApi.PreviousVideo> =
        videosByChannel.values.asSequence().flatten().distinctBy { it.id }
            .sortedByDescending { it.publishedAt }.toList()

    fun loadedChannelCount(videosByChannel: Map<String, List<YouTubeSocialApi.PreviousVideo>>): Int =
        videosByChannel.count { it.value.isNotEmpty() }

    fun totalViews(videos: List<YouTubeSocialApi.PreviousVideo>): Long =
        videos.sumOf { it.viewCount ?: 0L }
}

internal fun socialFormatCount(value: Long): String =
    NumberFormat.getIntegerInstance(Locale.forLanguageTag("pt-BR")).format(value)

/** Consulta vídeos anteriores do canal por OAuth, sem gravá-los no histórico de envios do app. */
@Composable
internal fun SocialYouTubeLibraryUi(
    accounts: List<SocialAccount>,
    busy: Boolean,
    videosByChannel: Map<String, List<YouTubeSocialApi.PreviousVideo>>,
    nextPageTokensByChannel: Map<String, String>,
    error: String,
    onLoad: (SocialAccount, String) -> Unit,
    onAccountSelected: () -> Unit
) {
    val channels = accounts.filter {
        it.destination == SocialDestination.YOUTUBE && it.remoteChannelId.isNotBlank()
    }
    var expanded by rememberSaveable { mutableStateOf(false) }
    var selectedId by rememberSaveable { mutableStateOf("") }
    var query by rememberSaveable { mutableStateOf("") }
    var menuOpen by remember { mutableStateOf(false) }
    val selected = channels.firstOrNull { it.id == selectedId } ?: channels.firstOrNull().also {
        if (it != null && selectedId.isBlank()) selectedId = it.id
    }
    val selectedChannelVideos = videosByChannel[selected?.remoteChannelId].orEmpty()
    val allLoadedVideos = remember(videosByChannel) { SocialYouTubeLibraryAggregate.merge(videosByChannel) }
    val filtered = allLoadedVideos.filter { it.title.contains(query.trim(), ignoreCase = true) }
    val loadedChannelCount = remember(videosByChannel) {
        SocialYouTubeLibraryAggregate.loadedChannelCount(videosByChannel)
    }
    val totalViews = remember(allLoadedVideos) { SocialYouTubeLibraryAggregate.totalViews(allLoadedVideos) }
    val currentPage = nextPageTokensByChannel[selected?.remoteChannelId].orEmpty()
    val context = LocalContext.current

    OutlinedCard(shape = UaiShapes.card) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.VideoLibrary, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Vídeos do YouTube", fontWeight = FontWeight.SemiBold)
                    Text("Consultar vídeos antigos e juntar dados dos canais carregados",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "Recolher" else "Explorar")
                    Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
                }
            }
            if (expanded) {
                HorizontalDivider()
                if (channels.isEmpty()) {
                    Text("Conecte um canal YouTube em Minhas contas para consultar seus vídeos.",
                        style = MaterialTheme.typography.bodySmall)
                } else {
                    if (channels.size > 1) {
                        Box {
                            OutlinedButton(onClick = { menuOpen = true }, enabled = !busy) {
                                Text(selected?.label.orEmpty(), maxLines = 1,
                                    overflow = TextOverflow.Ellipsis)
                                Icon(Icons.Default.ArrowDropDown, null)
                            }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                channels.forEach { account ->
                                    DropdownMenuItem(text = { Text(account.label) }, onClick = {
                                        selectedId = account.id; menuOpen = false
                                        onAccountSelected()
                                    })
                                }
                            }
                        }
                    } else Text(selected?.label.orEmpty(), style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(onClick = { selected?.let { onLoad(it, "") } },
                        modifier = Modifier.fillMaxWidth(), enabled = !busy && selected != null) {
                        Icon(Icons.Default.Refresh, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (selectedChannelVideos.isEmpty())
                            "Buscar vídeos do canal selecionado" else "Atualizar canal selecionado")
                    }
                    if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                    if (error.isNotBlank())
                        Text(error, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error)
                    if (allLoadedVideos.isNotEmpty()) {
                        OutlinedTextField(value = query, onValueChange = { query = it.take(100) },
                            singleLine = true, label = { Text("Filtrar títulos carregados") },
                            leadingIcon = { Icon(Icons.Default.Search, null) },
                            modifier = Modifier.fillMaxWidth(), shape = UaiShapes.item)
                        Text(
                            "${socialFormatCount(loadedChannelCount.toLong())} canal(is) carregado(s) · " +
                                "${socialFormatCount(allLoadedVideos.size.toLong())} vídeo(s) · " +
                                "${socialFormatCount(totalViews)} visualizações somadas",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text("A lista abaixo junta os vídeos já consultados de todos os canais carregados nesta sessão. " +
                            "Use o seletor acima para atualizar um canal específico.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (filtered.isEmpty()) Text("Nenhum título encontrado nos vídeos já carregados.",
                            style = MaterialTheme.typography.bodySmall)
                        filtered.forEach { video ->
                            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(video.title, style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium, maxLines = 2,
                                    overflow = TextOverflow.Ellipsis)
                                val metaParts = buildList {
                                    if (video.channelTitle.isNotBlank()) add(video.channelTitle)
                                    if (video.publishedAt.length >= 10) add(video.publishedAt.take(10))
                                    video.viewCount?.let { add("${socialFormatCount(it)} visualizações") }
                                }
                                if (metaParts.isNotEmpty()) {
                                    Text(metaParts.joinToString(" · "),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                                }
                                TextButton(onClick = {
                                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW,
                                        Uri.parse("https://www.youtube.com/watch?v=${video.id}"))) }
                                }) {
                                    Icon(Icons.Default.OpenInNew, null, Modifier.size(16.dp))
                                    Spacer(Modifier.width(5.dp)); Text("Ver no YouTube")
                                }
                                HorizontalDivider()
                            }
                        }
                    } else if (!busy && error.isBlank()) Text(
                        "Toque em Buscar. A consulta usa sua autorização Google e não altera o histórico local.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (currentPage.isNotBlank()) OutlinedButton(
                        onClick = { selected?.let { onLoad(it, currentPage) } },
                        enabled = !busy && selected != null, modifier = Modifier.fillMaxWidth()) {
                        Text("Carregar mais do canal selecionado")
                    }
                    if (selectedChannelVideos.size >= 300) Text("Limite de 300 vídeos por consulta atingido para este canal. " +
                        "Para pesquisar todo o acervo, use o YouTube Studio.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("A busca por título filtra apenas os vídeos já carregados. " +
                        "Carregue mais páginas para ampliar a lista agregada.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
