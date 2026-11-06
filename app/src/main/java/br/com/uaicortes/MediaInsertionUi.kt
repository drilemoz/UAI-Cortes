package br.com.uaicortes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** As duas inserções da faixa principal têm exatamente as mesmas escolhas de posição. */
@Composable
internal fun MediaPlacementChoices(value: ImageClipPolicy.Placement,
    selectedAvailable: Boolean, onChange: (ImageClipPolicy.Placement) -> Unit) {
    val options = listOf(
        Triple(ImageClipPolicy.Placement.START, "Início", Icons.Default.FirstPage),
        Triple(ImageClipPolicy.Placement.BEFORE_SELECTED, "Antes", Icons.Default.ArrowBack),
        Triple(ImageClipPolicy.Placement.AFTER_SELECTED, "Depois", Icons.Default.ArrowForward),
        Triple(ImageClipPolicy.Placement.END, "Final", Icons.Default.LastPage)
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        options.forEach { (choice, title, symbol) ->
            val enabled = selectedAvailable || choice == ImageClipPolicy.Placement.START ||
                choice == ImageClipPolicy.Placement.END
            val selected = value == choice
            Surface(onClick = { onChange(choice) }, enabled = enabled,
                modifier = Modifier.weight(1f).height(72.dp),
                shape = RoundedCornerShape(12.dp),
                color = if (selected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant,
                border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null) {
                Column(Modifier.fillMaxSize().padding(3.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(symbol, contentDescription = null, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.height(4.dp))
                    Text(title, fontSize = 11.sp, maxLines = 1,
                        overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
    }
}

/** Importação aguarda confirmação: nenhum projeto é alterado ao retornar do seletor. */
internal data class PendingVideoInsertion(
    val projectId: String, val selectedClipId: String,
    val clips: List<Clip>, val rejected: Int
)

internal object VideoClipInsertionPolicy {
    fun insert(project: VideoProject, incoming: List<Clip>,
        placement: ImageClipPolicy.Placement, selectedClipId: String): VideoProject {
        require(incoming.isNotEmpty() && incoming.none { it.isImage }) {
            "Somente vídeos podem ser inseridos por esta ferramenta."
        }
        require(incoming.map { it.id }.distinct().size == incoming.size &&
            incoming.none { item -> project.clips.any { it.id == item.id } }) {
            "Vídeos repetidos na seleção."
        }
        return ImageClipPolicy.insertMediaIntoProject(project, incoming, placement, selectedClipId)
    }
}

@Composable
internal fun VideoInsertionDialog(pending: PendingVideoInsertion,
    selectedAvailable: Boolean, onInsert: (ImageClipPolicy.Placement) -> Unit,
    onCancel: () -> Unit) {
    var placement by remember(pending) { mutableStateOf(ImageClipPolicy.Placement.END) }
    AlertDialog(onDismissRequest = onCancel,
        icon = { Icon(Icons.Default.VideoLibrary, contentDescription = null) },
        title = { Text("Inserir vídeo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(if (pending.clips.size == 1) pending.clips.first().title
                    else "${pending.clips.size} vídeos selecionados", maxLines = 2,
                    overflow = TextOverflow.Ellipsis)
                Text("Duração total: ${pending.clips.sumOf { it.durationMs } / 1000L}s",
                    style = MaterialTheme.typography.bodySmall)
                Text("Onde inserir?", fontWeight = FontWeight.SemiBold)
                MediaPlacementChoices(placement, selectedAvailable) { placement = it }
                Text("Os vídeos existentes permanecem na timeline. Legendas, áudios e PiPs " +
                    "posteriores acompanham o deslocamento.",
                    style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { Button(onClick = { onInsert(placement) }) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(5.dp))
            Text("Inserir ${if (pending.clips.size == 1) "vídeo" else "vídeos"}")
        } },
        dismissButton = { TextButton(onClick = onCancel) { Text("Cancelar") } }
    )
}

/** Só efetiva os vídeos depois da escolha; se o projeto/seleção mudar, recusa o commit. */
@Composable
internal fun MainVideoInsertionUi(project: VideoProject?, screen: String,
    pending: PendingVideoInsertion?,
    onInserted: (VideoProject, Clip, Int) -> Boolean,
    onCancel: () -> Unit) {
    if (pending == null || project == null || project.id != pending.projectId ||
        screen != "editor") return
    val context = androidx.compose.ui.platform.LocalContext.current
    val selectedAvailable = project.clips.any { it.id == pending.selectedClipId }
    VideoInsertionDialog(pending, selectedAvailable,
        onInsert = { placement ->
            if (placement in listOf(ImageClipPolicy.Placement.BEFORE_SELECTED,
                    ImageClipPolicy.Placement.AFTER_SELECTED) && !selectedAvailable) {
                android.widget.Toast.makeText(context, "Selecione um clipe para inserir antes ou depois.",
                    android.widget.Toast.LENGTH_LONG).show()
            } else {
                runCatching {
                    val index = ImageClipPolicy.index(project.clips,
                        pending.selectedClipId, placement)
                    val updated = VideoClipInsertionPolicy.insert(project,
                        pending.clips, placement, pending.selectedClipId)
                    if (onInserted(updated, pending.clips.first(), index) && pending.rejected > 0) {
                        android.widget.Toast.makeText(context,
                            "Alguns vídeos não puderam ser importados.",
                            android.widget.Toast.LENGTH_LONG).show()
                    }
                }.onFailure { error ->
                    DiagnosticRecorder.exception("video_insert", error)
                    android.widget.Toast.makeText(context,
                        "Não foi possível inserir os vídeos; o projeto foi preservado.",
                        android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }, onCancel = onCancel)
}
