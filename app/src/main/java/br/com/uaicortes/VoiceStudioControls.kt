package br.com.uaicortes

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.io.File

internal enum class VoiceStudioTextAction(val label: String) {
    TONE("Sugerir tom"), WRITE("Gerar roteiro"), REVISE("Revisar roteiro")
}

@Composable
internal fun VoiceStudioScriptMenu(configured: Boolean, model: String, enabled: Boolean, hasText: Boolean,
    onConfigure: () -> Unit, onModels: () -> Unit, onAction: (VoiceStudioTextAction) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }, enabled = enabled) {
            Text("IA do roteiro", maxLines = 1)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (configured) DropdownMenuItem(
                text = { Text("Gemini · ${model.ifBlank { "modelo automático" }}", color = MaterialTheme.colorScheme.primary) },
                onClick = { expanded = false; onModels() })
            else {
                DropdownMenuItem(text = { Text("Gemini · sem API") }, onClick = {}, enabled = false)
                DropdownMenuItem(text = { Text("Configurar API") }, onClick = {
                    expanded = false; onConfigure()
                })
            }
            VoiceStudioTextAction.entries.forEach { action ->
                DropdownMenuItem(text = { Text(action.label) },
                    enabled = configured && (action == VoiceStudioTextAction.WRITE || hasText),
                    onClick = { expanded = false; onAction(action) })
            }
        }
    }
}

@Composable
internal fun VoiceStudioTextRequest(action: VoiceStudioTextAction, model: String,
    onClose: () -> Unit, onConfirm: (String) -> Unit) {
    var brief by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onClose, title = { Text(action.label) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (action == VoiceStudioTextAction.WRITE) OutlinedTextField(
                    value = brief, onValueChange = { if (it.length <= 1800) brief = it },
                    label = { Text("O que você quer narrar?") }, minLines = 3, maxLines = 5)
                Text("O conteúdo será enviado ao Gemini (${model.ifBlank { "modelo automático" }}) e pode consumir cota. " +
                    "O texto atual só muda se você aceitar a sugestão.")
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(brief.trim()) },
                enabled = action != VoiceStudioTextAction.WRITE || brief.isNotBlank()) { Text("Consultar") }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("Voltar") } })
}

@Composable
internal fun VoiceStudioSceneReview(scenes: List<VoiceStudioPlan.Scene>, ready: List<Pair<File, Long>?>,
    style: String, tone: VoiceStudioTone, busy: Boolean, status: String, canGenerate: Boolean,
    onClose: () -> Unit, onCancel: () -> Unit, onGenerate: () -> Unit, onRetry: (Int) -> Unit, onPlay: (File) -> Unit) {
    val complete = ready.count { it != null }
    val overflow = ready.indices.any { i -> (ready[i]?.second ?: 0L) > scenes[i].windowMs }
    AlertDialog(onDismissRequest = onClose, title = { Text("Narração por cenas") },
        text = {
            Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("$complete de ${scenes.size} trechos prontos", style = MaterialTheme.typography.titleSmall)
                if (status.isNotBlank()) Text(status, style = MaterialTheme.typography.bodySmall)
                Text("A duração é uma meta. O app preserva toda a fala e avisa se ela não couber.",
                    style = MaterialTheme.typography.bodySmall)
                scenes.forEachIndexed { i, scene ->
                    HorizontalDivider()
                    val audio = ready.getOrNull(i)
                    val duration = audio?.second
                    val estimated = VoiceStudioPlan.estimateMs(scene.text, style, tone)
                    val excess = (duration ?: estimated) - scene.windowMs
                    Text("Cena ${i + 1} · ${formatMs(scene.startMs)} · janela ${VoiceStudioPlan.measured(scene.windowMs)}",
                        style = MaterialTheme.typography.titleSmall)
                    Text(scene.text, style = MaterialTheme.typography.bodySmall)
                    if (duration != null || excess > 0) Text(
                        if (duration != null && excess > 0) "${VoiceStudioPlan.measured(duration)} de fala · excede ${VoiceStudioPlan.measured(excess)}"
                        else if (duration != null) "${VoiceStudioPlan.measured(duration)} de fala"
                        else "A fala pode passar da janela.",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (excess > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                    if (audio != null) Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TextButton(onClick = { onPlay(audio.first) }, enabled = !busy) { Text("Ouvir trecho") }
                        TextButton(onClick = { onRetry(i) }, enabled = canGenerate && !busy) {
                            Text(if (excess > 0) "Tentar com ritmo mais rápido" else "Gerar de novo")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onGenerate, enabled = !busy &&
                (if (complete == scenes.size) !overflow else canGenerate)) {
                Text(when {
                    complete == scenes.size -> "Montar WAV"
                    complete == 0 -> "Gerar cenas"
                    else -> "Retomar ${scenes.size - complete} pendentes"
                })
            }
        },
        dismissButton = {
            TextButton(onClick = if (busy) onCancel else onClose) {
                Text(if (busy) "Cancelar geração" else "Fechar")
            }
        })
}
