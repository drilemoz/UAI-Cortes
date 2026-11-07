package br.com.uaicortes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/** Modelos do usuário: só alteram a aparência do rascunho até tocar em Salvar ou Aplicar a todas. */
@Composable
internal fun SubtitleUserPresetPanel(cue: SubtitleCue, onChange: (SubtitleCue) -> Unit) {
    val context = LocalContext.current
    val store = remember(context) { SubtitleUserPresetStore(context) }
    val scope = rememberCoroutineScope()
    var presets by remember { mutableStateOf<List<SubtitleUserPreset>>(emptyList()) }
    var ready by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf("") }
    var failure by remember { mutableStateOf<String?>(null) }
    var dialogMode by remember { mutableStateOf<String?>(null) }
    var dialogName by remember { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(store) {
        try {
            presets = store.read()
            ready = true
        } catch (e: Exception) {
            // Não sobrescrever a biblioteca se falhou a leitura.
            failure = "Não foi possível ler seus modelos. Os dados existentes foram preservados."
            DiagnosticRecorder.exception("subtitle_preset_read", e)
        }
    }

    fun saveAll(next: List<SubtitleUserPreset>, onSuccess: () -> Unit) {
        if (busy || !ready) return
        busy = true
        scope.launch {
            try {
                store.write(next)
                presets = next
                failure = null
                onSuccess()
            } catch (e: Exception) {
                failure = "Falha ao salvar modelos no aparelho. Tente novamente."
                DiagnosticRecorder.exception("subtitle_preset_write", e)
            } finally {
                busy = false
            }
        }
    }

    val selected = presets.firstOrNull { it.id == selectedId }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Meus modelos (${presets.size})", fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            TextButton(onClick = {
                dialogName = ""
                dialogMode = "save"
                expanded = true
                failure = null
            }, enabled = ready && !busy && presets.size < SubtitleUserPresets.MAX_ITEMS) {
                Text("Salvar atual", fontSize = 12.sp)
            }
            TextButton(onClick = { expanded = !expanded }, enabled = ready) {
                Text(if (expanded) "Recolher" else "Ver", fontSize = 12.sp)
            }
        }
        if (expanded) {
            if (presets.isEmpty()) {
                Text("Guarde a aparência da legenda para reutilizar em outros projetos.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(end = 8.dp)) {
                    items(presets, key = { it.id }) { preset ->
                        FilterChip(selected = selectedId == preset.id,
                            enabled = !busy,
                            onClick = {
                                selectedId = preset.id
                                onChange(preset.applyTo(cue))
                                status = "Modelo '${preset.name}' aplicado ao rascunho. Salve a legenda para confirmar."
                            }, label = { Text(preset.name, maxLines = 1, fontSize = 12.sp) },
                            shape = RoundedCornerShape(9.dp))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        dialogName = selected?.name.orEmpty()
                        dialogMode = "rename"
                        failure = null
                    }, enabled = selected != null && !busy,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp), shape = UaiShapes.button) {
                        Text("Renomear", fontSize = 12.sp)
                    }
                    OutlinedButton(onClick = { pendingDelete = selected?.id },
                        enabled = selected != null && !busy,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp), shape = UaiShapes.button) {
                        Text("Excluir", fontSize = 12.sp)
                    }
                }
                Text("Para aplicar em várias legendas, use Editar todas e confirme a mudança.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (busy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        if (status.isNotBlank()) Text(status, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        failure?.let { Text(it, color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall) }
    }

    val mode = dialogMode
    if (mode != null) {
        val renameId = selected?.id
        val nameError = SubtitleUserPresets.nameError(
            dialogName, presets, if (mode == "rename") renameId else null)
        AlertDialog(onDismissRequest = { if (!busy) dialogMode = null },
            title = { Text(if (mode == "save") "Salvar modelo de legenda" else "Renomear modelo") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = dialogName, onValueChange = { dialogName = it.take(64) },
                        singleLine = true, label = { Text("Nome do modelo") },
                        supportingText = {
                            Text(nameError ?: "Aparência, fonte, contorno, sombra e animação; sem texto e horários.")
                        }, isError = nameError != null)
                    failure?.let { Text(it, color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall) }
                }
            },
            confirmButton = {
                TextButton(enabled = nameError == null && !busy && ready,
                    onClick = {
                        val next = runCatching {
                            if (mode == "save") SubtitleUserPresets.add(presets, dialogName, cue)
                            else SubtitleUserPresets.rename(presets, requireNotNull(renameId), dialogName)
                        }.getOrElse { failure = it.message ?: "Modelo inválido."; return@TextButton }
                        val savedId = if (mode == "save") next.last().id else renameId
                        saveAll(next) {
                            selectedId = savedId
                            status = if (mode == "save") "Modelo salvo neste aparelho."
                                else "Modelo renomeado."
                            dialogMode = null
                        }
                    }) { Text("Salvar") }
            },
            dismissButton = { TextButton(enabled = !busy, onClick = { dialogMode = null }) {
                Text("Cancelar")
            } })
    }

    pendingDelete?.let { id ->
        val item = presets.firstOrNull { it.id == id }
        if (item != null) AlertDialog(onDismissRequest = { if (!busy) pendingDelete = null },
            title = { Text("Excluir modelo pessoal?") },
            text = { Text("'${item.name}' será apagado da biblioteca local. Legendas já aplicadas não serão modificadas.") },
            confirmButton = {
                TextButton(enabled = !busy, onClick = {
                    saveAll(SubtitleUserPresets.remove(presets, id)) {
                        selectedId = null
                        pendingDelete = null
                        status = "Modelo excluído. Legendas dos projetos preservadas."
                    }
                }) { Text("Excluir", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(enabled = !busy, onClick = { pendingDelete = null }) {
                Text("Cancelar")
            } })
    }
}
