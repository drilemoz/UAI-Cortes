package br.com.uaicortes

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/** Roteiro livre: permite Vozes IA em projetos sem legendas, sem criar nem alterar legendas. */
internal object VoiceIaFreeScript {
    const val ID_PREFIX = "livre-"
    const val MAX_LINES = 60
    const val MAX_LINE_CHARS = 300
    private const val MS_PER_CHAR = 80L
    private const val MIN_LINE_MS = 1200L
    private const val MAX_LINE_MS = 15000L
    private const val GAP_MS = 150L

    data class Plan(val cues: List<SubtitleCue>, val dropped: Int)

    /** Uma fala por linha, em sequência a partir de [startMs]. IDs estáveis: "livre-1", "livre-2"… */
    fun plan(script: String, startMs: Long, projectDurationMs: Long): Plan {
        val lines = script.lines().map { it.trim().take(MAX_LINE_CHARS) }
            .filter { it.isNotBlank() }.take(MAX_LINES)
        if (projectDurationMs <= 1L) return Plan(emptyList(), lines.size)
        val starts = ArrayList<Long>()
        val kept = ArrayList<String>()
        var cursor = startMs.coerceIn(0L, projectDurationMs - 1L)
        for (line in lines) {
            if (cursor >= projectDurationMs - 250L) break
            starts += cursor
            kept += line
            cursor += (line.length * MS_PER_CHAR).coerceIn(MIN_LINE_MS, MAX_LINE_MS)
        }
        val cues = kept.mapIndexed { index, text ->
            val rawEnd = if (index + 1 < starts.size) starts[index + 1] - GAP_MS else projectDurationMs
            val end = rawEnd.coerceAtLeast(starts[index] + 250L).coerceAtMost(projectDurationMs)
            SubtitleCue(id = "$ID_PREFIX${index + 1}", text = text, startMs = starts[index], endMs = end)
        }
        return Plan(cues, lines.size - kept.size)
    }
}

@Composable
internal fun VoiceIaFreeEntry(project: VideoProject, cursorMs: Long, back: () -> Unit,
    onOpenSubtitles: () -> Unit, onSave: (VideoProject) -> Boolean, openGeminiSettings: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("voice_free_script", Context.MODE_PRIVATE) }
    var script by remember(project.id) { mutableStateOf(prefs.getString(project.id, "").orEmpty()) }
    var plan by remember(project.id) { mutableStateOf<VoiceIaFreeScript.Plan?>(null) }
    var message by remember(project.id) { mutableStateOf<String?>(null) }
    val active = plan
    if (active != null && active.cues.isNotEmpty()) {
        // As falas livres existem só nesta sessão: o projeto salvo continua sem legendas.
        VoiceIaScreen(project.copy(subtitles = active.cues), back = { plan = null },
            onSave = { changed -> onSave(changed.copy(subtitles = project.subtitles)) },
            openGeminiSettings = openGeminiSettings)
        return
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Heading("Vozes IA", back)
        Text("Gerar vozes sem legendas", style = MaterialTheme.typography.titleLarge)
        Text("Escreva uma fala por linha. Elas entram em sequência a partir do cursor " +
            "(${formatMs(cursorMs)}), sem criar nem alterar legendas. A voz pode durar o tempo " +
            "que precisar: ajuste a posição depois no editor.")
        OutlinedTextField(value = script, onValueChange = { script = it.take(8000) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp),
            label = { Text("Falas (uma por linha)") })
        message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(onClick = {
            val planned = VoiceIaFreeScript.plan(script, cursorMs, project.durationMs)
            if (planned.cues.isEmpty()) {
                message = "Nenhuma linha cabe antes do fim do vídeo. Escreva uma fala ou mova o cursor."
            } else {
                message = null
                prefs.edit().putString(project.id, script).apply()
                plan = planned
            }
        }, modifier = Modifier.fillMaxWidth()) { Text("Continuar") }
        OutlinedButton(onClick = onOpenSubtitles, modifier = Modifier.fillMaxWidth()) {
            Text("Usar ou criar legendas")
        }
    }
}
