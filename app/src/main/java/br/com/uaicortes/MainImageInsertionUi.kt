package br.com.uaicortes

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Ferramenta única, opcional, para inserir uma foto na faixa principal.
 * Não muda a seleção do projeto até o usuário confirmar; read permission via SAF.
 */
@Composable
internal fun MainImageInsertionUi(
    project: VideoProject?, screen: String, mediaAction: String,
    selectedClipId: String, coordinator: MediaTaskCoordinator,
    onInserted: (VideoProject, Clip, Int) -> Boolean
): () -> Unit {
    val context = LocalContext.current
    val mediaController = remember(context) { MediaImportController(context.applicationContext) }
    var pending by remember { mutableStateOf<ImageClipSource?>(null) }
    var requestedProjectId by remember { mutableStateOf("") }
    var pendingProjectId by remember { mutableStateOf("") }
    var seconds by remember { mutableIntStateOf(ImageClipPolicy.DEFAULT_SECONDS) }
    var placement by remember { mutableStateOf(ImageClipPolicy.Placement.START) }
    LaunchedEffect(project?.id, screen) {
        if (pendingProjectId != project?.id || screen != "editor") pending = null
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val expectedId = requestedProjectId
        if (uri != null && screen == "editor" && expectedId.isNotBlank() &&
            project?.id == expectedId) {
            coordinator.start(MediaTaskKind.IMAGE,
                MediaTaskOrigin(expectedId, screen, mediaAction)) {
                val result = runCatching { ImageClipImport.inspect(context.applicationContext, uri) }
                result.onSuccess { source ->
                    if (project?.id == expectedId && screen == "editor") {
                        mediaController.persistReadAccess(uri)
                        pendingProjectId = expectedId
                        pending = source
                        seconds = ImageClipPolicy.DEFAULT_SECONDS
                        placement = ImageClipPolicy.Placement.START
                    } else Toast.makeText(context, "O projeto mudou. Selecione a foto novamente.",
                        Toast.LENGTH_LONG).show()
                }.onFailure { error ->
                    Toast.makeText(context, error.message ?: "Imagem não suportada.",
                        Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    val source = pending
    if (source != null && screen == "editor" && project?.id == pendingProjectId) {
        AlertDialog(
            onDismissRequest = { pending = null },
            icon = { Icon(Icons.Default.AddPhotoAlternate, null) },
            title = { Text("Adicionar imagem ao vídeo") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(source.title, maxLines = 2)
                    Text("Duração: ${seconds}s", fontWeight = FontWeight.SemiBold)
                    Slider(value = seconds.toFloat(), valueRange = 1f..30f, steps = 28,
                        onValueChange = { seconds = it.toInt().coerceIn(1, 30) })
                    Text("Onde inserir?", fontWeight = FontWeight.SemiBold)
                    listOf(
                        ImageClipPolicy.Placement.START to "Capa no início",
                        ImageClipPolicy.Placement.BEFORE_SELECTED to "Antes do selecionado",
                        ImageClipPolicy.Placement.AFTER_SELECTED to "Depois do selecionado",
                        ImageClipPolicy.Placement.END to "No final"
                    ).forEach { (choice, label) ->
                        Row(modifier = Modifier.fillMaxWidth().clickable { placement = choice },
                            verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = placement == choice,
                                onClick = { placement = choice })
                            Text(label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Text("Imagem estática, sem som original. O vídeo existente não será apagado.",
                        style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                Button(onClick = {
                    val latest = project.takeIf { it.id == pendingProjectId }
                    if (latest != null) {
                        val clip = ImageClipPolicy.create(source.uri, source.title, source.mimeType, seconds)
                        val index = ImageClipPolicy.index(latest.clips, selectedClipId, placement)
                        val updated = ImageClipPolicy.insertIntoProject(
                            latest, clip, placement, selectedClipId)
                        if (onInserted(updated, clip, index)) {
                            pending = null
                            Toast.makeText(context, "Imagem adicionada à timeline.", Toast.LENGTH_SHORT).show()
                        }
                    } else pending = null
                }) { Text("Adicionar imagem") }
            },
            dismissButton = { TextButton(onClick = { pending = null }) { Text("Cancelar") } }
        )
    }
    return {
        if (project != null && screen == "editor") {
            requestedProjectId = project.id
            picker.launch(arrayOf("image/*"))
        }
    }
}
