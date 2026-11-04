package br.com.uaicortes

import androidx.compose.foundation.layout.*
import android.os.SystemClock
import androidx.compose.foundation.shape.RoundedCornerShape
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
    onManageModel: () -> Unit, onManageGemini: () -> Unit,
    onApply: (List<SubtitleCue>, Boolean) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val geminiStore = remember(context) { GeminiCredentialStore(context) }
    val geminiClient = remember(context) { GeminiSubtitleClient(context) }
    var geminiConfirm by remember { mutableStateOf(false) }
    val geminiConfigured = geminiStore.hasKey()
    DisposableEffect(geminiClient) { onDispose {geminiClient.cancel()} }
    val installed = AutoSubtitleEngine.installed(context)
    var busy by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf<Int?>(null) }
    var runningCloud by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var task by remember { mutableStateOf<Job?>(null) }
    var startedAt by remember { mutableLongStateOf(0L) }
    var elapsedSeconds by remember { mutableLongStateOf(0L) }
    LaunchedEffect(busy) {
        while(busy) {
            elapsedSeconds=((SystemClock.elapsedRealtime()-startedAt).coerceAtLeast(0L))/1000L
            kotlinx.coroutines.delay(1000L)
        }
    }
    var results by remember(project.id) { mutableStateOf<List<SubtitleCue>>(emptyList()) }
    var replace by remember(project.id) { mutableStateOf(false) }
    var generatedSize by remember(project.id) { mutableIntStateOf(20) }
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
            Text("O modo offline extrai áudio dos vídeos, respeitando cortes e posições " +
                "na linha do tempo. Não usa microfone nem envia áudio para a internet.",
                style=MaterialTheme.typography.bodySmall)
            Text("Duração do projeto: ${formatMs(project.durationMs)} · limite desta versão: 10 minutos",
                style=MaterialTheme.typography.bodySmall)
            Button(onClick={
                task=scope.launch {
                    startedAt=SystemClock.elapsedRealtime()
                    elapsedSeconds=0L
                    busy=true;runningCloud=false;progress=null;results=emptyList();replace=false
                    message="Preparando transcrição offline…"
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
            HorizontalDivider()
            Text("Transcrição por IA · Gemini", fontWeight=FontWeight.SemiBold)
            Text("Usa a chave da API em Configurações. Requer internet e pode consumir sua " +
                "cota ou gerar cobrança. O áudio será enviado ao Google somente após confirmação. " +
                "Se os clipes e cortes não mudaram, o áudio extraído pode ser reaproveitado " +
                "do cache privado por até 3 horas.",
                style=MaterialTheme.typography.bodySmall)
            if(!geminiConfigured) OutlinedButton(onClick=onManageGemini,
                modifier=Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Settings,null)
                Spacer(Modifier.width(8.dp))
                Text("Configurar chave Gemini")
            }
            Button(onClick={geminiConfirm=true},enabled=geminiConfigured && !busy &&
                project.durationMs in 1..600000L,modifier=Modifier.fillMaxWidth()) {
                Icon(Icons.Default.AutoAwesome,null)
                Spacer(Modifier.width(8.dp))
                Text("Transcrever com Gemini (online)")
            }
            if(busy) {
                Surface(shape=RoundedCornerShape(14.dp),
                    color=MaterialTheme.colorScheme.surfaceVariant) {
                    Column(Modifier.fillMaxWidth().padding(14.dp),
                        verticalArrangement=Arrangement.spacedBy(12.dp)) {
                        Text("Processamento da transcrição",fontWeight=FontWeight.SemiBold)
                        Text(message,style=MaterialTheme.typography.bodyMedium)
                        if(progress==null) LinearProgressIndicator(modifier=Modifier.fillMaxWidth())
                        else LinearProgressIndicator(progress={ (progress ?: 0)/100f },
                            modifier=Modifier.fillMaxWidth())
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                            Text("Tempo decorrido: ${formatMs(elapsedSeconds*1000L)}",
                                style=MaterialTheme.typography.labelSmall)
                            Text(progress?.let { "$it% do processamento" } ?: "Etapa sem porcentagem",
                                style=MaterialTheme.typography.labelSmall)
                        }
                        Text(if(runningCloud)
                            "Extração e envio têm progresso por etapa; processamento no servidor " +
                                "não informa porcentagem. O tempo pode superar a duração do vídeo."
                            else "O tempo de reconhecimento pode superar a duração do vídeo. " +
                                "O progresso acompanha o áudio realmente decodificado.",
                            style=MaterialTheme.typography.bodySmall)
                    }
                }
                OutlinedButton(onClick={geminiClient.cancel();task?.cancel()},modifier=Modifier.fillMaxWidth()) {
                    Text("Cancelar transcrição")
                }
            } else if(message.isNotBlank()) Text(message,style=MaterialTheme.typography.bodySmall)
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
            if(results.isNotEmpty()) {
                Surface(shape=RoundedCornerShape(12.dp),
                    color=MaterialTheme.colorScheme.surfaceVariant) {
                    Column(Modifier.fillMaxWidth().padding(12.dp)) {
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                            Text("Tamanho das legendas geradas",fontWeight=FontWeight.Medium)
                            Text("$generatedSize sp")
                        }
                        Slider(value=generatedSize.toFloat(),onValueChange={
                            generatedSize=it.toInt().coerceIn(12,30)
                        },valueRange=12f..30f)
                        Text("Ajuste antes de aplicar. Também é possível alterar todas depois.",
                            style=MaterialTheme.typography.bodySmall)
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
        Button(onClick={onApply(results.map {it.copy(sizeSp=generatedSize)},replace)},
            enabled=!busy && results.isNotEmpty(),
            modifier=Modifier.fillMaxWidth().navigationBarsPadding()) {
            Icon(Icons.Default.Check, null)
            Spacer(Modifier.width(8.dp))
            Text("Aplicar ${results.size} legendas ao projeto")
        }
    }
    if(geminiConfirm) AlertDialog(onDismissRequest={geminiConfirm=false},
        title={Text("Enviar áudio ao Google Gemini?")},
        text={Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("O Uai Cortes enviará somente o áudio extraído (até 10 minutos) ao Gemini após confirmar. " +
                "O vídeo não será enviado. O WAV poderá ficar em cache privado por até 3 horas. " +
                "Confirmar não substitui legendas existentes.")
            GeminiUsageHint("legendas automáticas", durationMs = project.durationMs,
                model = "gemini-3.5-transcribe")
        }},
        confirmButton={TextButton(onClick={
            geminiConfirm=false
            task=scope.launch {
                startedAt=SystemClock.elapsedRealtime()
                elapsedSeconds=0L
                busy=true;runningCloud=true;progress=null;results=emptyList();replace=false
                message="Preparando áudio para IA…"
                try {
                    val apiKey=withContext(Dispatchers.IO) {
                        geminiStore.read() ?: throw IllegalStateException("Configure sua chave Gemini.")
                    }
                    val generated=geminiClient.transcribe(context,project,apiKey) { pct,step ->
                        withContext(Dispatchers.Main) { progress=pct; message=step }
                    }
                    results=generated
                    message=if(generated.isEmpty()) "Nenhuma palavra reconhecida."
                        else "${generated.size} legendas geradas por Gemini. Revise antes de aplicar. " +
                            GeminiUsageLedger(context).lastMessage("legendas")
                    progress=100
                } catch(_: CancellationException) {
                    message="Envio/transcrição cancelados. Nenhuma legenda foi aplicada."
                } catch(e: Exception) {
                    // Não registrar chave, áudio ou corpo da resposta da API.
                    message="Não foi possível transcrever: ${e.message?.take(160) ?: "falha na API"}"
                } finally {busy=false;task=null}
            }
        }) { Text("Enviar e transcrever") }},
        dismissButton={TextButton(onClick={geminiConfirm=false}) { Text("Cancelar") }})
    editIndex?.let { index ->
        AlertDialog(onDismissRequest={editIndex=null},
            title={Text("Revisar legenda ${index+1}")},
            text={OutlinedTextField(value=editText,onValueChange={editText=it.take(170)},
                label={Text("Texto transcrito")},modifier=Modifier.fillMaxWidth(),minLines=2,maxLines=4)},
            confirmButton={TextButton(onClick={
                if(editText.isNotBlank()) results=results.toMutableList().also {
                    val corrected=editText.trim()
                    if (corrected!=it[index].text) {
                        it[index]=it[index].copy(text=corrected,
                            wordTimings=emptyList(),wordColors=emptyList())
                    }
                }
                editIndex=null
            }) {Text("Salvar correção")}},
            dismissButton={TextButton(onClick={editIndex=null}) {Text("Cancelar")}})
    }
}
