package br.com.uaicortes

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

/** Ajuste manual de sincronia: move todas as legendas juntas; o app oferece desfazer em seguida. */
@Composable
internal fun SubtitleSyncDialog(subtitles: List<SubtitleCue>, durationMs: Long,
    onDismiss: () -> Unit, onApply: (Long) -> Unit) {
    var delta by remember { mutableLongStateOf(0L) }
    val allowed = SubtitleGlobalShift.allowed(subtitles, delta, durationMs)
    fun label(ms: Long) = String.format(Locale.ROOT, "%+d ms", ms)
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text("Ajustar sincronia") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Move todas as legendas juntas. Valor negativo faz aparecerem mais cedo; " +
                "positivo, mais tarde. Textos e durações não mudam.", fontSize = 13.sp)
            Text("Ajuste: ${label(allowed)}", style = MaterialTheme.typography.titleMedium)
            if (allowed != delta) Text("Limitado para nenhuma legenda sair do vídeo.",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
            for (steps in listOf(listOf(-500L, -100L, -50L), listOf(50L, 100L, 500L))) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (step in steps) OutlinedButton(onClick = {
                        delta = (allowed + step).coerceIn(-SubtitleGlobalShift.MAX_MS, SubtitleGlobalShift.MAX_MS)
                    }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 2.dp)) {
                        Text(label(step), fontSize = 12.sp, maxLines = 1)
                    }
                }
            }
            TextButton(onClick = { delta = 0L }, enabled = delta != 0L) { Text("Zerar") }
        } },
        confirmButton = { TextButton(onClick = { onApply(allowed) }, enabled = allowed != 0L) { Text("Aplicar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } })
}
