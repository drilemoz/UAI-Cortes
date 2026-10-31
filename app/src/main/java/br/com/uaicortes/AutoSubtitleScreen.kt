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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Instalador e transcrição iniciados apenas pelo usuário, com revisão explícita.
 * A edição continua offline após baixar o modelo PT.
 */
@Composable
fun AutoSubtitleScreen(project: VideoProject, back: () -> Unit,
    onManageModel: () -> Unit, onApply: (List<SubtitleCue>, Boolean) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val installed = AutoSubtitleEngine.installed(context)
    var busy by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf<Int?>(null) }
    var message by remember { mutableStateOf("") }
    var task by remember { mutableStateOf<Job?>(null) }
    var results by remember(project.id) { mutableStateOf<List<SubtitleCue>>(emptyList()) }
    var replace by remember(project.id) { mutableStateOf(false) }
    var editIndex by remember { mutableStateOf<Int?>(null) }
    var editText by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal=16.dp)) {
        Row(Modifier.fillMaxWidth().height(58.dp), verticalAlignment=Alignment.CenterVertically) {
            IconButton(onClick=back, enabled=!busy) { Icon(Icons.Default.ArrowBack, "Voltar") }
            Text("Legendas automáticas", style=MaterialTheme.typography.titleLarge)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text("Modelo de reconhecimento de voz", fontWeight=FontWeight.SemiBold)
            Text(if (installed) "Português brasileiro instalado. Transcrição offline disponível."
                else "Modelo ainda não instalado. Instale-o em Configurações para gerar legendas.",
                style=MaterialTheme.typography.bodySmall)
            if (!installed) OutlinedButton(onClick=onManageModel, modifier=Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Settings, null)
                Spacer(Modifier.width(8.dp))
                Text("Abrir configuração de voz")
            }
            HorizontalDivider()
            Text("Passo 2 · Gerar legendas", fontWeight=FontWeight.SemiBold)
            Text("O app extrai somente o áudio dos vídeos deste projeto, respeitando " +
                "cortes e posições na linha do tempo. Não usa microfone nem envia o áudio.",
                style=MaterialTheme.typography.bodySmall)
            Text("Duração do projeto: ${formatMs(project.durationMs)} · limite desta versão: 10 minutos",
                style=MaterialTheme.typography.bodySmall)
            Button(onClick={
                task=scope.launch {
                    busy=true;progress=0;message="Preparando decodificação de áudio…"
                    try {
                        val generated = AutoSubtitleEngine.transcribe(context, project) { pct, step ->
                            withContext(Dispatchers.Main) { progress=pct;message=step }
                        }
                        results=generated
                        message=if(generated.isEmpty())"Nenhuma fala reconhecida. Confira se o vídeo contém áudio audível."
                            else "${generated.size} legendas geradas. Revise os textos antes de aplicar."
                        progress=100
                    } catch (_: CancellationException) {
                        message="Transcrição cancelada. Nenhum texto foi aplicado ao projeto."
                    } catch (e: Exception) {
                        DiagnosticRecorder.exception("auto_subtitles", e)
                        message="Não foi possível transcrever: ${e.message?.take(180) ?: "erro de áudio/modelo"}"
                    } finally {busy=false;task=null}
                }
            }, enabled=installed && !busy && project.durationMs in 1..600000L,
                modifier=Modifier.fillMaxWidth()) {
                Icon(Icons.Default.RecordVoiceOver, null)
                Spacer(Modifier.width(8.dp))
                Text("Transcrever fala (offline)")
            }
            if (busy) {
                if (progress != null) LinearProgressIndicator(progress={ (progress ?: 0)/100f },
                    modifier=Modifier.fillMaxWidth())
                else LinearProgressIndicator(modifier=Modifier.fillMaxWidth())
                OutlinedButton(onClick={task?.cancel()},modifier=Modifier.fillMaxWidth()) {
                    Text("Cancelar operação")
                }
            }
            if(message.isNotBlank()) Text(message, style=MaterialTheme.typography.bodySmall)
            HorizontalDivider()
            Text("Passo 3 · Revisar e aplicar", fontWeight=FontWeight.SemiBold)
            Text("Reconhecimento automático pode errar palavras, pontuação e nomes. " +
                "Toque num resultado para corrigir o texto antes de salvar.",
                style=MaterialTheme.typography.bodySmall)
            results.forEachIndexed { index, cue ->
                Surface(onClick={editIndex=index;editText=cue.text},
                    shape=MaterialTheme.shapes.small,
                    color=MaterialTheme.colorScheme.surfaceVariant) {
                    Row(Modifier.fillMaxWidth().padding(10.dp),verticalAlignment=Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(cue.text, maxLines=2,overflow=TextOverflow.Ellipsis)
                            Text("${formatMs(cue.startMs)}–${formatMs(cue.endMs)}",
                                style=MaterialTheme.typography.labelSmall)
                        }
                        Icon(Icons.Default.Edit, "Corrigir texto")
                    }
                }
            }
            if (project.subtitles.isNotEmpty() && results.isNotEmpty()) {
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Checkbox(checked=replace,onCheckedChange={replace=it})
                    Text("Substituir ${project.subtitles.size} legendas existentes (irreversível após salvar)",
                        style=MaterialTheme.typography.bodySmall)
                }
                if (!replace) Text("As novas legendas serão adicionadas às existentes.",
                    style=MaterialTheme.typography.bodySmall)
            }
            Text("Dica: faça primeiro um teste curto com voz nítida e sem música. " +
                "Depois revise todas as falas no editor de legendas.",
                style=MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(12.dp))
        }
        Button(onClick={onApply(results,replace)},
            enabled=!busy && results.isNotEmpty(),
            modifier=Modifier.fillMaxWidth().navigationBarsPadding()) {
            Icon(Icons.Default.Check, null)
            Spacer(Modifier.width(8.dp))
            Text("Aplicar ${results.size} legendas ao projeto")
        }
    }
    editIndex?.let { index ->
        AlertDialog(onDismissRequest={editIndex=null},
            title={Text("Revisar legenda ${index+1}")},
            text={OutlinedTextField(value=editText,onValueChange={editText=it.take(170)},
                label={Text("Texto transcrito")},modifier=Modifier.fillMaxWidth(),minLines=2,maxLines=4)},
            confirmButton={TextButton(onClick={
                if(editText.isNotBlank()) results=results.toMutableList().also {
                    it[index]=it[index].copy(text=editText.trim())
                }
                editIndex=null
            }) {Text("Salvar correção")}},
            dismissButton={TextButton(onClick={editIndex=null}) {Text("Cancelar")}})
    }
}
