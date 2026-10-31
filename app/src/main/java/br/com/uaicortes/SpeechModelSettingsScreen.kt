package br.com.uaicortes

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch

/** Instalação voluntária do Vosk: download interno ou ZIP oficial via SAF. */
@Composable
internal fun SpeechModelSettingsScreen(back: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var installed by remember { mutableStateOf(AutoSubtitleEngine.installed(context)) }
    var progress by remember { mutableStateOf<Int?>(null) }
    var received by remember { mutableLongStateOf(0L) }
    var expected by remember { mutableStateOf<Long?>(null) }
    var stage by remember { mutableStateOf("") }
    var failure by remember { mutableStateOf("") }
    var task by remember { mutableStateOf<Job?>(null) }
    val busy = task?.isActive == true

    fun beginImport(uri: Uri?) {
        if (uri == null || task?.isActive == true || installed) return
        failure = ""; progress = null; received = 0; expected = null
        stage = "Abrindo ZIP selecionado…"
        task = scope.launch {
            try {
                AutoSubtitleEngine.importZip(context, uri,
                    onProgress = { pct -> withContext(Dispatchers.Main) { progress = pct } },
                    onStage = { value -> withContext(Dispatchers.Main) { stage = value } },
                    onTransfer = { count, total -> withContext(Dispatchers.Main) { received = count; expected = total } })
                installed = AutoSubtitleEngine.installed(context)
                if (installed) {
                    // A última etapa era a extração: nunca mostrá-la após sucesso.
                    stage = ""
                    progress = null
                    received = 0L
                    expected = null
                } else failure = "O ZIP não instalou um modelo válido."
            } catch (_: CancellationException) {
                stage = "Importação cancelada."
            } catch (e: Exception) {
                DiagnosticRecorder.exception("vosk_zip_import", e)
                failure = "Falha ao importar: ${e.message?.take(180) ?: "arquivo inválido"}"
            } finally { task = null }
        }
    }

    val pickZip = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        beginImport(uri)
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
        .padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Heading("Reconhecimento de voz", { if (!busy) back() })
        Text("Português brasileiro · Vosk", fontWeight = FontWeight.SemiBold)
        Text("Modelo de aproximadamente 31 MB. Depois de instalado, o reconhecimento " +
            "de fala funciona offline; o áudio dos seus vídeos não é enviado à internet.",
            style = MaterialTheme.typography.bodySmall)
        Text(if (installed) "Modelo instalado e disponível offline."
            else "Modelo ainda não instalado.",
            color = if (installed) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant)

        if (!installed) {
            // Três opções na mesma linha, com largura igual e ícones acima dos textos.
            // O seletor continua disponível mesmo quando a transferência interna falha.
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        failure = ""; progress = null; received = 0; expected = null
                        stage = "Conectando ao servidor oficial…"
                        task = scope.launch {
                            try {
                                AutoSubtitleEngine.install(context,
                                    onProgress = { pct -> withContext(Dispatchers.Main) { progress = pct } },
                                    onStage = { value -> withContext(Dispatchers.Main) { stage = value } },
                                    onTransfer = { count, total -> withContext(Dispatchers.Main) { received = count; expected = total } })
                                installed = AutoSubtitleEngine.installed(context)
                                if (installed) {
                                    stage = ""
                                    progress = null
                                    received = 0L
                                    expected = null
                                } else failure = "Modelo não pôde ser validado após o download."
                            } catch (_: CancellationException) {
                                stage = "Download cancelado."
                            } catch (e: Exception) {
                                DiagnosticRecorder.exception("vosk_install", e)
                                failure = "Falha no download: ${e.message?.take(180) ?: "erro de rede"}"
                            } finally { task = null }
                        }
                    }, enabled = !busy, modifier = Modifier.weight(1f).heightIn(min = 76.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 8.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.height(4.dp))
                        Text("Baixar", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    }
                }
                OutlinedButton(
                    onClick = {
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(AutoSubtitleEngine.MODEL_URL)))
                        } catch (e: Exception) {
                            failure = "Não foi possível abrir o navegador: ${e.message?.take(100) ?: "sem navegador"}"
                        }
                    }, enabled = !busy, modifier = Modifier.weight(1f).heightIn(min = 76.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 8.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.height(4.dp))
                        Text("Navegador", style = MaterialTheme.typography.labelSmall, maxLines = 1,
                            overflow = TextOverflow.Ellipsis)
                    }
                }
                OutlinedButton(
                    onClick = { pickZip.launch(arrayOf("*/*")) },
                    enabled = !busy, modifier = Modifier.weight(1f).heightIn(min = 76.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 8.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.height(4.dp))
                        Text("Importar ZIP", style = MaterialTheme.typography.labelSmall, maxLines = 1,
                            overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }

        if (busy) {
            Text(stage.ifBlank { "Preparando…" })
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.Download, null)
                Text(progress?.let { "$it%" } ?: "Aguardando dados…",
                    style = MaterialTheme.typography.titleMedium)
            }
            if (received > 0L) {
                Text("Recebidos: ${ModelTransferProgress.bytesLabel(received)}" +
                    (expected?.let { " de ${ModelTransferProgress.bytesLabel(it)}" } ?: ""),
                    style = MaterialTheme.typography.bodySmall)
            } else if (stage.contains("Baixando") || stage.contains("Copiando")) {
                Text("Aguardando os primeiros bytes do arquivo…",
                    style = MaterialTheme.typography.bodySmall)
            }
            if (progress != null) {
                val value = (progress ?: 0).coerceIn(0, 100) / 100f
                LinearProgressIndicator(progress = { value }, modifier = Modifier.fillMaxWidth())
            } else {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            OutlinedButton(onClick = { task?.cancel() }, modifier = Modifier.fillMaxWidth()) {
                Text("Cancelar operação")
            }
        } else if (!installed) {
            Text("Selecione vosk-model-small-pt-0.3.zip no armazenamento ou baixe pelo navegador. " +
                "O modelo oficial usa um formato de pasta antigo, agora aceito pelo instalador.",
                style = MaterialTheme.typography.bodySmall)
        }

        if (failure.isNotBlank()) Text(failure, color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall)
        SpeechInstallUiPolicy.idleStage(installed, busy, stage, failure)?.let { visibleStage ->
            Text(visibleStage, style = MaterialTheme.typography.bodySmall)
        }
        if (installed && !busy) {
            Text("Instalação reconhecida. Para confirmar o funcionamento, teste uma fala curta " +
                "em Legendas automáticas.", style = MaterialTheme.typography.bodySmall)
        } else if (!installed) {
            Text("Se o servidor travar em 0%, use o navegador e depois importe o arquivo ZIP. " +
                "Não é necessário descompactá-lo manualmente.",
                style = MaterialTheme.typography.bodySmall)
        }
    }
}

