package br.com.uaicortes

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import java.io.File

/** Interface intencionalmente separada do editor para minimizar efeitos sobre a mídia. */
@Composable
fun DiagnosticScreen(onBack: () -> Unit, onEnabledChanged: (Boolean) -> Unit) {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(DiagnosticRecorder.isEnabled()) }
    var report by remember { mutableStateOf("") }
    var feedback by remember { mutableStateOf("") }
    var confirmClear by remember { mutableStateOf(false) }
    var pendingText by remember { mutableStateOf("") }
    val saveTxt = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri, "w")?.use {
                    it.write(pendingText.toByteArray(Charsets.UTF_8))
                } ?: error("Não foi possível abrir o destino")
            }.onSuccess { feedback = "Relatório TXT salvo no local escolhido." }
                .onFailure { feedback = "Falha ao salvar o relatório."; DiagnosticRecorder.exception("diagnostic_save", it) }
        }
    }
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 18.dp)) {
        Row(Modifier.fillMaxWidth().height(62.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Voltar") }
            Text("Diagnóstico", style = MaterialTheme.typography.titleLarge)
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Captura local de erros e ações", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Registrar diagnóstico", fontWeight = FontWeight.Medium)
                    Text(if (enabled) "Ativado" else "Desativado (padrão)",
                        style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = enabled, onCheckedChange = {
                    DiagnosticRecorder.setEnabled(it)
                    enabled = it
                    onEnabledChanged(it)
                    feedback = if (it) "Captura iniciada." else "Captura interrompida. O relatório foi mantido."
                })
            }
            Text("Quando ativo, registra data/hora, tela, toques aproximados, ações de navegação e erros. " +
                "Não registra teclas digitadas, textos de legendas, nomes dos arquivos, caminhos, áudio, vídeos ou senhas. " +
                "O TXT inclui o modelo do aparelho e a versão do Android.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Os dados ficam no aparelho (limite de 512 KiB; eventos mais antigos são descartados). " +
                "Nada é enviado automaticamente. Você decide se deseja salvar ou compartilhar o relatório.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = {
                report = DiagnosticRecorder.report()
                feedback = "Relatório atualizado."
                DiagnosticRecorder.action("diagnostics", "view_report")
            }, modifier = Modifier.fillMaxWidth()) { Text("Visualizar relatório") }
            OutlinedButton(onClick = {
                pendingText = DiagnosticRecorder.report()
                saveTxt.launch("UaiCortes-diagnostico.txt")
                DiagnosticRecorder.action("diagnostics", "save_report")
            }, modifier = Modifier.fillMaxWidth()) { Text("Salvar relatório TXT") }
            OutlinedButton(onClick = {
                runCatching {
                    val folder = File(context.cacheDir, "diagnosticos").apply { mkdirs() }
                    val file = File(folder, "UaiCortes-diagnostico.txt")
                    file.writeText(DiagnosticRecorder.report(), Charsets.UTF_8)
                    val uri = FileProvider.getUriForFile(context,
                        "${context.packageName}.diagnostics", file)
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        clipData = android.content.ClipData.newUri(context.contentResolver,
                            "Diagnóstico Uai Cortes", uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, "Compartilhar diagnóstico"))
                    DiagnosticRecorder.action("diagnostics", "share_report")
                }.onFailure {
                    feedback = "Não foi possível compartilhar o relatório. Use Salvar TXT."
                    DiagnosticRecorder.exception("diagnostic_share", it)
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("Compartilhar relatório") }
            TextButton(onClick = { confirmClear = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Apagar histórico local", color = MaterialTheme.colorScheme.error)
            }
            if (feedback.isNotBlank()) Text(feedback, style = MaterialTheme.typography.bodySmall)
            if (report.isNotBlank()) {
                HorizontalDivider()
                Text("Prévia do relatório", fontWeight = FontWeight.SemiBold)
                Text(report.takeLast(9000), style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
    if (confirmClear) {
        AlertDialog(onDismissRequest = { confirmClear = false },
            title = { Text("Apagar relatório?") },
            text = { Text("O histórico de diagnóstico será removido deste aparelho. Isso não altera seus projetos.") },
            confirmButton = { TextButton(onClick = {
                DiagnosticRecorder.clear()
                report = ""
                feedback = "Histórico apagado."
                confirmClear = false
            }) { Text("Apagar") } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancelar") } })
    }
}
