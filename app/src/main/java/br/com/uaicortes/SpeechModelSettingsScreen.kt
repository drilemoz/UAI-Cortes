package br.com.uaicortes

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

/** Instalação voluntária e isolada da tela de revisão de legendas. */
@Composable
internal fun SpeechModelSettingsScreen(back: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var installed by remember { mutableStateOf(AutoSubtitleEngine.installed(context)) }
    var progress by remember { mutableStateOf<Int?>(null) }
    var stage by remember { mutableStateOf("") }
    var failure by remember { mutableStateOf("") }
    var task by remember { mutableStateOf<Job?>(null) }
    val busy = task?.isActive == true
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
        .padding(horizontal=20.dp), verticalArrangement=Arrangement.spacedBy(16.dp)) {
        Heading("Reconhecimento de voz", { if (!busy) back() })
        Text("Português brasileiro · Vosk", fontWeight=FontWeight.SemiBold)
        Text("Modelo de aproximadamente 31 MB, baixado uma vez pela internet. " +
            "Depois, a transcrição é local e não envia o áudio à nuvem.",
            style=MaterialTheme.typography.bodySmall)
        Text(if (installed) "Modelo instalado e disponível offline."
            else "Modelo ainda não instalado.",
            color=if(installed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        if (busy) {
            Text(stage.ifBlank { "Conectando…" })
            Row(verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.Download, null)
                Text(progress?.let { "$it%" } ?: "Calculando…",
                    style=MaterialTheme.typography.titleMedium)
            }
            if (progress != null) LinearProgressIndicator(
                progress={ (progress ?: 0).coerceIn(0,100)/100f }, modifier=Modifier.fillMaxWidth())
            else LinearProgressIndicator(modifier=Modifier.fillMaxWidth())
            OutlinedButton(onClick={task?.cancel()}, modifier=Modifier.fillMaxWidth()) {
                Text("Cancelar download")
            }
        } else {
            Button(onClick={
                failure = ""; progress=null;stage="Conectando ao servidor oficial…"
                task=scope.launch {
                    try {
                        AutoSubtitleEngine.install(context,
                            onProgress={ pct -> withContext(Dispatchers.Main) {
                                progress=pct
                                stage=if(pct==null) "Baixando modelo…" else "Baixando modelo: $pct%"
                            } },
                            onStage={ step -> withContext(Dispatchers.Main) { stage=step } })
                        installed=AutoSubtitleEngine.installed(context)
                        if (!installed) failure="A instalação terminou sem arquivos válidos. Tente novamente."
                    } catch (_: CancellationException) {
                        stage="Download cancelado."
                    } catch (e: Exception) {
                        DiagnosticRecorder.exception("vosk_install", e)
                        failure="Falha na instalação: ${e.message?.take(160) ?: "erro de rede ou armazenamento"}"
                    } finally { task=null }
                }
            }, enabled=!installed, modifier=Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Download, null)
                Spacer(Modifier.width(8.dp))
                Text(if(installed) "Modelo instalado" else "Baixar e instalar modelo")
            }
        }
        if (failure.isNotBlank()) Text(failure, color=MaterialTheme.colorScheme.error,
            style=MaterialTheme.typography.bodySmall)
        if (!busy && stage.isNotBlank() && failure.isBlank()) Text(stage,
            style=MaterialTheme.typography.bodySmall)
        Text("Se falhar, confira a conexão, o espaço livre e a mensagem de erro. " +
            "A instalação incompleta não é considerada um modelo válido.",
            style=MaterialTheme.typography.bodySmall)
    }
}
