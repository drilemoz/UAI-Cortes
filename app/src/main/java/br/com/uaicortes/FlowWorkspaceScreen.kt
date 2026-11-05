package br.com.uaicortes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/** Prompts antes de haver arquivo de vídeo. Usa armazenamento local separado dos editores. */
@Composable
internal fun FlowWorkspaceScreen(back: () -> Unit) {
    val context = LocalContext.current
    val store = remember(context) { FlowWorkspaceStore(context) }
    var entries by remember { mutableStateOf(runCatching { store.read() }.getOrNull()) }
    var currentId by remember { mutableStateOf<String?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleteId by remember { mutableStateOf<String?>(null) }
    var title by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    fun persist(next: List<FlowWorkspaceEntry>): Boolean {
        val ok = runCatching { store.write(next) }.getOrDefault(false)
        if (ok) entries = next.sortedByDescending { it.updated }
        else error = "Falha ao salvar. Dados anteriores preservados."
        return ok
    }
    val selected = entries?.firstOrNull { it.id == currentId }
    if (selected != null) {
        FlowPromptScreen(project = VideoProject(id = selected.id, title = selected.title,
            flowPromptDraft = selected.draft),
            onSave = { draft ->
                val target = entries?.firstOrNull { it.id == selected.id }
                if (target == null) false else persist(entries.orEmpty().map {
                    if (it.id == selected.id) it.copy(draft = draft, updated = System.currentTimeMillis()) else it
                })
            }, back = { currentId = null })
        return
    }
    if (creating) AlertDialog(onDismissRequest = { creating = false },
        title = { Text("Novo projeto de cenas") },
        text = { OutlinedTextField(value = title, onValueChange = { title = it.take(80) },
            label = { Text("Nome do vídeo") }, singleLine = true) },
        confirmButton = { TextButton(onClick = {
            val entry = FlowWorkspaceEntry(title = title.trim().ifBlank { "Vídeo Flow" })
            if (entries != null && entries.orEmpty().size < 30 && persist(entries.orEmpty() + entry)) {
                currentId = entry.id
                creating = false
            }
        }) { Text("Criar") } },
        dismissButton = { TextButton(onClick = { creating = false }) { Text("Cancelar") } })
    deleteId?.let { id -> AlertDialog(onDismissRequest = { deleteId = null },
        title = { Text("Excluir este conjunto de prompts?") },
        text = { Text("As cenas serão removidas deste aparelho. Não altera MP4s ou vídeos gerados.") },
        confirmButton = { TextButton(onClick = {
            if (persist(entries.orEmpty().filterNot { it.id == id })) {
                if (!FlowStudioStorage(context, id).deleteAll()) {
                    error = "Projeto removido do catálogo, mas alguns arquivos privados podem permanecer."
                }
                deleteId = null
            }
        }) { Text("Excluir") } },
        dismissButton = { TextButton(onClick = { deleteId = null }) { Text("Cancelar") } })
    }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Heading("Estúdio de Cenas", back)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Prepare cenas sem importar vídeo. Cada conjunto possui suas cenas e compartilha o mesmo estilo-base 3D salvo no aparelho.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = { title = ""; creating = true }, modifier = Modifier.fillMaxWidth(),
                enabled = entries != null && entries.orEmpty().size < 30) {
                Icon(Icons.Default.Add, null); Text(" Criar conjunto de cenas")
            }
            if (entries == null) Text("O catálogo salvo não pôde ser lido. Os dados foram preservados; recupere o armazenamento antes de criar novos projetos.",
                color = MaterialTheme.colorScheme.error)
            if (entries?.isEmpty() == true) Text("Nenhum conjunto de cenas salvo.")
            entries.orEmpty().forEach { entry ->
                OutlinedCard(onClick = { currentId = entry.id }, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MovieCreation, null)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(entry.title, style = MaterialTheme.typography.titleSmall)
                            val studio = remember(entry.id, currentId) {
                                runCatching { FlowStudioStorage(context, entry.id).read() }.getOrNull()
                            }
                            Text("${studio?.scenes?.size ?: entry.draft.scenes.size} cena(s) no Estúdio",
                                style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { deleteId = entry.id }) {
                            Icon(Icons.Default.DeleteOutline, "Excluir prompts")
                        }
                    }
                }
            }
            if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
            Text("Biblioteca local, separada do backup dos projetos de vídeo. Nenhuma cena é enviada ao Flow automaticamente.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
