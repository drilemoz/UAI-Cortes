package br.com.uaicortes

import android.widget.Toast
import android.net.Uri
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
    incomingImage: Uri?, onImageConsumed: () -> Unit, openGallery: () -> Unit,
    onInserted: (VideoProject, Clip, Int) -> Boolean
): () -> Unit {
    val context = LocalContext.current
    var pending by remember { mutableStateOf<ImageClipSource?>(null) }
    var requestedProjectId by remember { mutableStateOf("") }
    var pendingProjectId by remember { mutableStateOf("") }
    var seconds by remember { mutableIntStateOf(ImageClipPolicy.DEFAULT_SECONDS) }
    var placement by remember { mutableStateOf(ImageClipPolicy.Placement.START) }
    fun cancelPending() {
        ImageClipImport.discardUnused(context.applicationContext, pending)
        pending = null
    }
    LaunchedEffect(project?.id, screen) {
        if (pendingProjectId != project?.id || screen != "editor") cancelPending()
    }
    // A escolha é feita pela MESMA galeria integrada de Vídeo e PiP.
    // OpenDocument permanece disponível pelo botão de arquivos desse navegador.
    LaunchedEffect(incomingImage, project?.id, screen) {
        val uri = incomingImage ?: return@LaunchedEffect
        if (screen != "editor" || project == null) return@LaunchedEffect
        val expectedId = project.id
        onImageConsumed()
        val accepted = coordinator.start(MediaTaskKind.IMAGE,
            MediaTaskOrigin(expectedId, screen, mediaAction)) {
            try {
                val source = ImageClipImport.inspect(context.applicationContext, uri)
                val durable = ImageClipImport.copyIntoProject(context.applicationContext, source)
                if (project.id == expectedId && screen == "editor") {
                    pendingProjectId = expectedId
                    pending = durable
                    seconds = ImageClipPolicy.DEFAULT_SECONDS
                    placement = ImageClipPolicy.Placement.START
                    DiagnosticRecorder.action("editor", "image_import_ready")
                } else {
                    ImageClipImport.discardUnused(context.applicationContext, durable)
                    Toast.makeText(context, "O projeto mudou. Importe novamente.", Toast.LENGTH_LONG).show()
                }
            } catch (error: Exception) {
                DiagnosticRecorder.exception("image_import", error)
                Toast.makeText(context, error.message ?: "Não foi possível importar a foto.",
                    Toast.LENGTH_LONG).show()
            }
        }
        if (!accepted) {
            DiagnosticRecorder.failure("image_import", "import busy")
            Toast.makeText(context, "Há outra importação em andamento. Tente novamente.",
                Toast.LENGTH_LONG).show()
        }
    }
    val source = pending
    if (source != null && screen == "editor" && project?.id == pendingProjectId) {
        AlertDialog(
            onDismissRequest = { cancelPending() },
            icon = { Icon(Icons.Default.AddPhotoAlternate, null) },
            title = { Text("Inserir imagem") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(source.title, maxLines = 2)
                    Text("Duração: ${seconds}s", fontWeight = FontWeight.SemiBold)
                    Slider(value = seconds.toFloat(), valueRange = 1f..30f, steps = 28,
                        onValueChange = { seconds = it.toInt().coerceIn(1, 30) })
                    Text("Onde inserir?", fontWeight = FontWeight.SemiBold)
                    MediaPlacementChoices(placement,
                        project.clips.any { it.id == selectedClipId }) { placement = it }
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
                    } else cancelPending()
                }) { Text("Inserir imagem") }
            },
            dismissButton = { TextButton(onClick = { cancelPending() }) { Text("Cancelar") } }
        )
    }
    return {
        if (project != null && screen == "editor") {
            requestedProjectId = project.id
            openGallery()
        }
    }
}
