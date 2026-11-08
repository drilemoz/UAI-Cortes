package br.com.uaicortes

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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

/** Consulta vídeos anteriores do canal por OAuth, sem gravá-los no histórico de envios do app. */
@Composable
internal fun SocialYouTubeLibraryUi(
    accounts: List<SocialAccount>,
    busy: Boolean,
    videos: List<YouTubeSocialApi.PreviousVideo>,
    nextPageToken: String,
    loadedChannelId: String,
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
    val selected = channels.firstOrNull { it.id == selectedId } ?: channels.firstOrNull()
    // Dados consultados de outro canal nunca são mostrados sob uma nova seleção.
    val visibleVideos = if (loadedChannelId == selected?.remoteChannelId) videos else emptyList()
    val page = if (loadedChannelId == selected?.remoteChannelId) nextPageToken else ""
    val filtered = visibleVideos.filter { it.title.contains(query.trim(), ignoreCase = true) }
    val context = LocalContext.current

    OutlinedCard(shape = UaiShapes.card) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.VideoLibrary, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Vídeos do YouTube", fontWeight = FontWeight.SemiBold)
                    Text("Consultar vídeos antigos do canal", style = MaterialTheme.typography.bodySmall,
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
                                        selectedId = account.id; query = ""; menuOpen = false
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
                        Text(if (visibleVideos.isEmpty()) "Buscar vídeos anteriores" else "Atualizar vídeos")
                    }
                    if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                    if (error.isNotBlank())
                        Text(error, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error)
                    if (visibleVideos.isNotEmpty()) {
                        OutlinedTextField(value = query, onValueChange = { query = it.take(100) },
                            singleLine = true, label = { Text("Filtrar títulos carregados") },
                            leadingIcon = { Icon(Icons.Default.Search, null) },
                            modifier = Modifier.fillMaxWidth(), shape = UaiShapes.item)
                        Text("${visibleVideos.size} vídeo(s) consultados · dados do YouTube, não envios deste app",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelSmall)
                        if (filtered.isEmpty()) Text("Nenhum título encontrado nas páginas carregadas.",
                            style = MaterialTheme.typography.bodySmall)
                        filtered.forEach { video ->
                            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(video.title, style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium, maxLines = 2,
                                    overflow = TextOverflow.Ellipsis)
                                if (video.publishedAt.length >= 10)
                                    Text("Data informada: ${video.publishedAt.take(10)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                TextButton(onClick = {
                                    // Nunca abre URLs fornecidas pela API: somente ID de vídeo validado.
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
                        if (loadedChannelId == selected?.remoteChannelId)
                            "Nenhum vídeo foi retornado pelo YouTube nesta consulta."
                        else "Toque em Buscar. A consulta usa sua autorização Google e não altera o histórico local.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (page.isNotBlank()) OutlinedButton(
                        onClick = { selected?.let { onLoad(it, page) } },
                        enabled = !busy && selected != null, modifier = Modifier.fillMaxWidth()) {
                        Text("Carregar mais vídeos antigos")
                    }
                    if (visibleVideos.size >= 300) Text("Limite de 300 vídeos por consulta atingido. " +
                        "Para pesquisar todo o acervo, use o YouTube Studio.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("A busca por título filtra apenas os vídeos já carregados. " +
                        "Carregue mais páginas para explorar publicações anteriores.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
