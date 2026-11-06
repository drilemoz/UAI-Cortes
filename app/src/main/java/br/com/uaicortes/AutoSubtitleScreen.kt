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
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Instalador e transcrição iniciados apenas pelo usuário, com revisão explícita.
 * A edição continua offline após baixar o modelo PT.
 */
@Composable
fun AutoSubtitleScreen(project: VideoProject, back: () -> Unit,
    onManageModel: () -> Unit, onManageGemini: () -> Unit,
    onApply: (VideoProject) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val geminiStore = remember(context) { GeminiCredentialStore(context) }
    val geminiClient = remember(context) { GeminiSubtitleClient(context) }
    var geminiConfirm by remember { mutableStateOf(false) }
    var usageExpanded by remember { mutableStateOf(false) }
    var detectSpeakers by remember(project.id) { mutableStateOf(true) }
    var voiceDecisions by remember(project.id) { mutableStateOf<Map<String,String>>(emptyMap()) }
    var voiceNames by remember(project.id) { mutableStateOf<Map<String,String>>(emptyMap()) }
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
    val geminiCheckpoint = remember(context, project.id) {
        GeminiTaskCheckpointStore(context, GeminiTaskKind.LEGENDAS, project.id)
    }
    val mediaFingerprint = remember(project.clips, project.overlays, project.audioTracks, project.music) {
        GeminiTaskRecoveryPolicy.digest(buildString {
            project.clips.forEach { append(it.json().toString()) }
            project.overlays.forEach { append(it.json().toString()) }
            project.audioTracks.forEach { append(it.json().toString()) }
            append(project.music?.json()?.toString().orEmpty())
            append(project.durationMs)
        })
    }
    var savedGeminiTask by remember(project.id) { mutableStateOf<GeminiTaskCheckpoint?>(null) }
    LaunchedEffect(project.id, mediaFingerprint) {
        savedGeminiTask = withContext(Dispatchers.IO) {
            runCatching { geminiCheckpoint.load(mediaFingerprint) }.getOrNull()
        }
    }
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal=16.dp)) {
        Row(Modifier.fillMaxWidth().height(58.dp), verticalAlignment=Alignment.CenterVertically) {
            IconButton(onClick=back, enabled=!busy) { Icon(Icons.Default.ArrowBack, "Voltar") }
            Text("Legendas automáticas", style=MaterialTheme.typography.titleLarge)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement=Arrangement.spacedBy(9.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(if (installed) Icons.Default.CheckCircle else Icons.Default.Info,
                    contentDescription = null, modifier = Modifier.size(19.dp),
                    tint = if (installed) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(8.dp))
                Text(if (installed) "Modelo offline PT-BR pronto" else "Modelo offline não instalado",
                    modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2)
                SubtitleHelpButton("Reconhecimento offline",
                    "O Vosk usa somente áudio local e respeita cortes da linha do tempo. " +
                    "Não usa microfone nem envia áudio pela internet. " +
                    "O modelo de português precisa ser instalado voluntariamente.")
            }
            if (!installed) OutlinedButton(onClick=onManageModel, modifier=Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Settings, null, modifier=Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Instalar modelo offline")
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Transcrever", fontWeight=FontWeight.SemiBold,
                    modifier = Modifier.weight(1f))
                Text("${formatMs(project.durationMs)} / 10 min",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
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
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Gemini IA · online", fontWeight=FontWeight.SemiBold,
                    modifier=Modifier.weight(1f))
                SubtitleHelpButton("Gemini e privacidade",
                    "Somente após sua confirmação, o aplicativo envia ao Google o áudio " +
                    "extraído dos clipes; não envia o vídeo. A operação requer internet e " +
                    "pode consumir a cota ou gerar cobrança. O WAV pode ficar em cache privado " +
                    "por até 3 horas para evitar nova extração quando os cortes não mudaram. " +
                    "A diarização sugere Voz 1, Voz 2 etc., mas não descobre nomes de pessoas. " +
                    "Revise o texto e cada voz antes de aplicar.")
            }
            Text("Envia áudio ao Google · Pode consumir cota ou gerar cobrança",
                style=MaterialTheme.typography.bodySmall,
                color=MaterialTheme.colorScheme.onSurfaceVariant)
            if(!geminiConfigured) OutlinedButton(onClick=onManageGemini,
                modifier=Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Settings,null)
                Spacer(Modifier.width(8.dp))
                Text("Configurar chave Gemini")
            }
            Row(verticalAlignment=Alignment.CenterVertically) {
                Checkbox(checked=detectSpeakers,enabled=!busy,
                    onCheckedChange={detectSpeakers=it})
                Text("Sugerir Voz 1, Voz 2… (revisão obrigatória)",
                    style=MaterialTheme.typography.bodySmall,modifier=Modifier.weight(1f))
            }
            Button(onClick={usageExpanded=false;geminiConfirm=true},enabled=geminiConfigured && !busy &&
                project.durationMs in 1..600000L,modifier=Modifier.fillMaxWidth()) {
                Icon(Icons.Default.AutoAwesome,null)
                Spacer(Modifier.width(8.dp))
                Text("Transcrever com Gemini (online)")
            }
            savedGeminiTask?.let { saved ->
                if (!busy) Surface(shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant) {
                    Column(Modifier.fillMaxWidth().padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(if (saved.stage == GeminiTaskStage.READY)
                            "Legendas Gemini recuperáveis neste aparelho (sem nova chamada)."
                        else "A transcrição anterior foi interrompida ou tem resultado incerto. " +
                            "Repetir pode consumir cota novamente.",
                            style = MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (saved.stage == GeminiTaskStage.READY) {
                                OutlinedButton(onClick = { scope.launch {
                                    try {
                                        results = withContext(Dispatchers.IO) {
                                            val persisted = geminiCheckpoint.load(mediaFingerprint)
                                                ?: throw java.io.IOException("Resultado não encontrado")
                                            geminiCheckpoint.subtitles(persisted)
                                        }
                                        replace = false
                                        message = "Resultado local restaurado. Revise as legendas antes de aplicar."
                                    } catch (e: Exception) {
                                        message = "Não foi possível recuperar as legendas: " +
                                            (e.message ?: "cache inválido")
                                    }
                                } }) { Text("Restaurar") }
                            }
                            TextButton(onClick = { scope.launch {
                                withContext(Dispatchers.IO) { runCatching { geminiCheckpoint.clear() } }
                                savedGeminiTask = null
                            } }) { Text("Descartar registro") }
                        }
                    }
                }
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(if (runningCloud) "IA: progresso por etapa, sem % no servidor"
                                else "Offline: progresso do áudio decodificado",
                                style=MaterialTheme.typography.labelSmall,
                                modifier=Modifier.weight(1f))
                            SubtitleHelpButton("Progresso da transcrição",
                                "O progresso exibido corresponde à etapa de extração ou leitura " +
                                "do áudio. O servidor Gemini não fornece percentual contínuo; " +
                                "o processamento pode demorar mais que a duração do vídeo.")
                        }
                    }
                }
                OutlinedButton(onClick={geminiClient.cancel();task?.cancel()},modifier=Modifier.fillMaxWidth()) {
                    Text("Cancelar transcrição")
                }
            } else if(message.isNotBlank()) Text(message,style=MaterialTheme.typography.bodySmall)
            if (results.isNotEmpty()) {
                HorizontalDivider()
                Row(Modifier.fillMaxWidth(), verticalAlignment=Alignment.CenterVertically) {
                    Text("Revisar ${results.size} legendas", fontWeight=FontWeight.SemiBold,
                        modifier=Modifier.weight(1f))
                    SubtitleHelpButton("Revisão de legendas",
                        "Toque numa legenda para corrigir o texto. A transcrição pode errar " +
                        "palavras, horários e nomes. Se a IA sugerir vozes, revise as associações. " +
                        "Nada será aplicado ao projeto antes de confirmar no botão inferior.")
                }
            }
            GeminiVoiceReviewUi(results,project.subtitleSpeakers,voiceDecisions,voiceNames,
                onDecision={id,decision -> voiceDecisions=voiceDecisions+(id to decision)},
                onName={id,name -> voiceNames=voiceNames+(id to name)})
            results.forEachIndexed { index, cue ->
                Surface(onClick={editIndex=index;editText=cue.text},
                    shape=MaterialTheme.shapes.small,
                    color=MaterialTheme.colorScheme.surfaceVariant) {
                    Row(Modifier.fillMaxWidth().padding(10.dp),verticalAlignment=Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(cue.text, maxLines=2,overflow=TextOverflow.Ellipsis)
                            Text("${formatMs(cue.startMs)}–${formatMs(cue.endMs)}" +
                                (cue.speakerId?.let(GeminiDiarizationPolicy::label)?.let { " · $it" } ?: ""),
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
                    }
                }
            }
            if (project.subtitles.isNotEmpty() && results.isNotEmpty()) {
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Checkbox(checked=replace,onCheckedChange={replace=it})
                    Text("Substituir ${project.subtitles.size} legendas existentes (irreversível após salvar)",
                        style=MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        Button(onClick={
            val proposal=GeminiDiarizationPolicy.prepare(project,
                results.map { it.copy(sizeSp=generatedSize) },replace,voiceDecisions,voiceNames)
            if (proposal==null) message="Revise os nomes: não podem repetir outros " +
                "personagens nem superar 16 no projeto. Você pode vincular a um " +
                "personagem existente ou marcar Não atribuir."
            else onApply(proposal)
        }, enabled=!busy && results.isNotEmpty(),
            modifier=Modifier.fillMaxWidth().navigationBarsPadding()) {
            Icon(Icons.Default.Check, null)
            Spacer(Modifier.width(8.dp))
            Text("Aplicar ${results.size} legendas ao projeto")
        }
    }
    if(geminiConfirm) AlertDialog(onDismissRequest={geminiConfirm=false},
        title={Text("Enviar áudio ao Google Gemini?")},
        text={Column(Modifier.heightIn(max=420.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Somente o áudio extraído será enviado ao Google Gemini. " +
                "O vídeo não será enviado. A operação pode consumir cota ou gerar cobrança.")
            if (detectSpeakers) Text("O Gemini também tentará sugerir vozes. " +
                "Você revisará os rótulos antes de aplicar.",
                style=MaterialTheme.typography.bodySmall)
            Text("Modelo: gemini-3.5-transcribe · ${formatMs(project.durationMs)}",
                style=MaterialTheme.typography.labelSmall)
            TextButton(onClick={usageExpanded=!usageExpanded},
                modifier=Modifier.fillMaxWidth()) {
                Icon(if (usageExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    null, modifier=Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (usageExpanded) "Ocultar detalhes de consumo" else "Detalhes de consumo e privacidade")
            }
            if (usageExpanded) {
                Text("O WAV pode ficar em cache privado por até 3 horas. " +
                    "O envio não substitui automaticamente legendas existentes; " +
                    "a identificação de vozes não informa nomes reais.",
                    style=MaterialTheme.typography.bodySmall)
                GeminiUsageHint("legendas automáticas", durationMs = project.durationMs,
                    model = "gemini-3.5-transcribe")
            }
        }},
        confirmButton={TextButton(onClick={
            geminiConfirm=false
            task=scope.launch {
                startedAt=SystemClock.elapsedRealtime()
                elapsedSeconds=0L
                busy=true;runningCloud=true;progress=null;results=emptyList();replace=false
                message="Preparando áudio para IA…"
                var geminiStarted = false
                try {
                    val apiKey=withContext(Dispatchers.IO) {
                        geminiStore.read() ?: throw IllegalStateException("Configure sua chave Gemini.")
                    }
                    withContext(Dispatchers.IO) {
                        geminiCheckpoint.start(mediaFingerprint, "gemini-3.5-transcribe")
                    }
                    savedGeminiTask = null
                    geminiStarted = true
                    val generated=geminiClient.transcribe(context,project,apiKey,detectSpeakers) { pct,step ->
                        withContext(Dispatchers.Main) { progress=pct; message=step }
                    }
                    withContext(Dispatchers.IO) {
                        geminiCheckpoint.saveSubtitles(mediaFingerprint, "gemini-3.5-transcribe", generated)
                    }
                    savedGeminiTask = GeminiTaskCheckpoint(mediaFingerprint, "gemini-3.5-transcribe",
                        GeminiTaskStage.READY, System.currentTimeMillis()) // resultado está no arquivo privado
                    results=generated
                    voiceDecisions=emptyMap();voiceNames=emptyMap()
                    message=if(generated.isEmpty()) "Nenhuma palavra reconhecida."
                        else "${generated.size} legendas geradas por Gemini. " +
                            (if (detectSpeakers && GeminiDiarizationPolicy.suggestions(generated).isEmpty())
                                "A API não retornou rótulos de locutor; as falas ficarão sem personagem. "
                            else "Confira os rótulos de voz antes de aplicar. ") +
                            GeminiUsageLedger(context).lastMessage("legendas")
                    progress=100
                } catch(_: CancellationException) {
                    if (geminiStarted) {
                        withContext(NonCancellable + Dispatchers.IO) {
                            runCatching { geminiCheckpoint.review(mediaFingerprint, "gemini-3.5-transcribe") }
                        }
                        savedGeminiTask = GeminiTaskCheckpoint(mediaFingerprint, "gemini-3.5-transcribe",
                            GeminiTaskStage.REVIEW, System.currentTimeMillis())
                    }
                    message="Transcrição cancelada. Nenhuma legenda aplicada; verifique a tentativa anterior."
                } catch(e: Exception) {
                    if (geminiStarted) {
                        withContext(NonCancellable + Dispatchers.IO) {
                            runCatching { geminiCheckpoint.review(mediaFingerprint, "gemini-3.5-transcribe") }
                        }
                        savedGeminiTask = GeminiTaskCheckpoint(mediaFingerprint, "gemini-3.5-transcribe",
                            GeminiTaskStage.REVIEW, System.currentTimeMillis())
                    }
                    message = GeminiOperationFailure.label(GeminiOperationFailure.fromThrowable(e)) +
                        ". Nenhuma legenda foi aplicada."
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
