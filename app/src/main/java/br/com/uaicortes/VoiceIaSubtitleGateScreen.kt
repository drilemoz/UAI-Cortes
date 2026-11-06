package br.com.uaicortes

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Vozes vinculadas precisam de uma fala com texto e intervalo válido.
 * A Central de Vozes e a narração independente continuam disponíveis.
 */
internal object VoiceIaSubtitleGate {
    fun hasCues(project: VideoProject): Boolean = project.subtitles.any {
        it.text.isNotBlank() && it.startMs >= 0L && it.endMs > it.startMs &&
            it.endMs <= project.durationMs
    }
}

@Composable
internal fun VoiceIaSubtitleGateScreen(onBack: () -> Unit, onOpenSubtitles: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(18.dp),
        verticalArrangement=Arrangement.spacedBy(14.dp)) {
        Heading("Vozes IA",onBack)
        Icon(Icons.Default.ClosedCaption,contentDescription=null,
            tint=MaterialTheme.colorScheme.primary,modifier=Modifier.size(38.dp))
        Text("Adicione legendas antes de gerar vozes", style=MaterialTheme.typography.titleLarge)
        Text("Vozes IA precisa de pelo menos uma legenda com texto e horário válido. " +
            "Crie, importe um SRT ou gere legendas no editor. " +
            "A Central de Vozes e a narração independente continuam disponíveis.")
        Button(onClick=onOpenSubtitles,modifier=Modifier.fillMaxWidth()) {
            Text("Abrir Legendas")
        }
    }
}
