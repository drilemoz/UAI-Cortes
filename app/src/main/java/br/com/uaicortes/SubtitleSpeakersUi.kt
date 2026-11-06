package br.com.uaicortes

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import java.util.UUID

/** Atribuição declarada pelo usuário; o app NÃO identifica pessoas pela voz ou imagem. */
@Composable
internal fun SubtitleSpeakerPicker(project: VideoProject, cueId: String,
    update: (VideoProject) -> Boolean, dismiss: () -> Unit, manage: () -> Unit) {
    val cue = project.subtitles.firstOrNull { it.id == cueId }
    Dialog(onDismissRequest = dismiss) {
        Surface(shape = RoundedCornerShape(24.dp), tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth().widthIn(max = 420.dp)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Quem está falando?", style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f))
                    SubtitleHelpButton("Atribuir personagem",
                        "Selecione quem falou nesta legenda. Essa associação é manual: " +
                        "a voz ou a imagem não identificam uma pessoa automaticamente. " +
                        "Para cadastrar outra pessoa, toque em Personagens.")
                }
                Column(Modifier.heightIn(max = 300.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    TextButton(onClick = {
                        if (SubtitleSpeakerPolicy.assign(project, cueId, null)?.let(update) == true) dismiss()
                    }, modifier = Modifier.fillMaxWidth().heightIn(min = 46.dp)) {
                        if (cue?.speakerId == null) Icon(Icons.Default.Check, null,
                            modifier=Modifier.size(18.dp))
                        else Icon(Icons.Default.PersonOff, null, modifier=Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Não identificado", modifier=Modifier.weight(1f))
                    }
                    project.subtitleSpeakers.forEach { candidate ->
                        TextButton(onClick = {
                            if (SubtitleSpeakerPolicy.assign(project, cueId, candidate.id)?.let(update) == true) dismiss()
                        }, modifier = Modifier.fillMaxWidth().heightIn(min = 46.dp)) {
                            Box(Modifier.size(16.dp).background(Color(candidate.color),
                                RoundedCornerShape(4.dp)))
                            Spacer(Modifier.width(9.dp))
                            Text(candidate.name, modifier = Modifier.weight(1f), maxLines=1)
                            if (cue?.speakerId == candidate.id)
                                Icon(Icons.Default.Check, "Personagem selecionado",
                                    modifier=Modifier.size(18.dp))
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = dismiss) { Text("Cancelar") }
                    TextButton(onClick = { dismiss(); manage() }) {
                        Text("Personagens")
                    }
                }
            }
        }
    }
}

/** Cadastro limitado a 16 personagens. Excluir só remove vínculos de locutor; não apaga legendas. */
@Composable
internal fun SubtitleSpeakerManager(project: VideoProject,
    update: (VideoProject) -> Boolean, dismiss: () -> Unit) {
    var editId by remember(project.id) { mutableStateOf<String?>(null) }
    var name by remember(project.id) { mutableStateOf("") }
    var color by remember(project.id) { mutableIntStateOf(SubtitleSpeakerPolicy.COLORS.first()) }
    var error by remember(project.id) { mutableStateOf("") }
    var deletingId by remember(project.id) { mutableStateOf<String?>(null) }
    var confirmColors by remember(project.id) { mutableStateOf(false) }
    AlertDialog(onDismissRequest = dismiss,
        title = { Text("Personagens e vozes") },
        text = {
            Column(Modifier.heightIn(max = 450.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Defina quem fala. As associações são manuais e pertencem somente a este projeto.",
                    style = MaterialTheme.typography.bodySmall)
                project.subtitleSpeakers.forEach { speaker ->
                    Row(verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()) {
                        Box(Modifier.size(16.dp).background(Color(speaker.color), RoundedCornerShape(4.dp)))
                        Spacer(Modifier.width(7.dp))
                        Text(speaker.name, modifier = Modifier.weight(1f), maxLines = 1)
                        IconButton(onClick = {
                            editId = speaker.id; name = speaker.name; color = speaker.color; error = ""
                        }, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.Edit, "Renomear personagem") }
                        IconButton(onClick = { deletingId = speaker.id }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Delete, "Remover personagem")
                        }
                    }
                }
                OutlinedTextField(value = name, onValueChange = { name = it.take(40); error = "" },
                    label = { Text(if (editId == null) "Novo personagem" else "Renomear personagem") },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SubtitleSpeakerPolicy.COLORS.forEach { candidate ->
                        Surface(onClick = { color = candidate }, shape = RoundedCornerShape(5.dp),
                            border = if (color == candidate) BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface) else null,
                            color = Color(candidate), modifier = Modifier.weight(1f).height(30.dp)) {}
                    }
                }
                Button(onClick = {
                    val changed = if (editId != null) SubtitleSpeakerPolicy.rename(project, editId!!, name, color)
                        else SubtitleSpeakerPolicy.add(project, name, UUID.randomUUID().toString())?.let { created ->
                            created.copy(subtitleSpeakers = created.subtitleSpeakers.map { s ->
                                if (s.id == created.subtitleSpeakers.last().id) s.copy(color = color) else s
                            })
                        }
                    if (changed != null && update(changed)) {
                        editId = null; name = ""; color = SubtitleSpeakerPolicy.COLORS[
                            changed.subtitleSpeakers.size % SubtitleSpeakerPolicy.COLORS.size]
                        error = ""
                    } else error = "Informe um nome diferente e válido (até 16 personagens)."
                }, enabled = name.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                    Text(if (editId == null) "Adicionar personagem" else "Salvar personagem")
                }
                if (editId != null) TextButton(onClick = { editId = null; name = ""; error = "" }) {
                    Text("Cancelar edição do nome")
                }
                if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
                HorizontalDivider()
                TextButton(onClick = { confirmColors = true },
                    enabled = project.subtitles.any { it.speakerId != null },
                    modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Palette, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Aplicar cores dos personagens às legendas")
                }
                Text("As cores só afetam o vídeo exportado após esta confirmação.",
                    style = MaterialTheme.typography.labelSmall)
            }
        }, confirmButton = { TextButton(onClick = dismiss) { Text("Concluído") } })
    if (deletingId != null) AlertDialog(onDismissRequest = { deletingId = null },
        title = { Text("Remover personagem?") },
        text = { Text("As legendas permanecem, mas ficarão sem personagem atribuído.") },
        confirmButton = { TextButton(onClick = {
            val id = deletingId ?: return@TextButton
            if (update(SubtitleSpeakerPolicy.remove(project, id))) {
                if (editId == id) { editId = null; name = "" }
                deletingId = null
            }
        }) { Text("Remover") } },
        dismissButton = { TextButton(onClick = { deletingId = null }) { Text("Cancelar") } })
    if (confirmColors) AlertDialog(onDismissRequest = { confirmColors = false },
        title = { Text("Aplicar cores às legendas?") },
        text = { Text("A cor do texto de cada legenda atribuída será substituída pela cor do personagem, inclusive na exportação. Texto, tempo e posição não mudam.") },
        confirmButton = { TextButton(onClick = {
            if (update(SubtitleSpeakerPolicy.applyColors(project))) confirmColors = false
        }) { Text("Aplicar cores") } },
        dismissButton = { TextButton(onClick = { confirmColors = false }) { Text("Cancelar") } })
}
