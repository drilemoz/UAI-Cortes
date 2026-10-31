package br.com.uaicortes

import android.content.ClipData
import android.content.ClipDescription
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun GeminiSettingsSection() {
    val ctx = LocalContext.current
    val store = remember(ctx) { GeminiCredentialStore(ctx) }
    val api = remember { GeminiTtsClient() }
    val scope = rememberCoroutineScope()
    var key by remember { mutableStateOf("") }
    var hasKey by remember { mutableStateOf(store.hasKey()) }
    var show by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var pendingImport by remember { mutableStateOf<String?>(null) }
    var confirm by remember { mutableStateOf("") }
    val importPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            busy = true
            try {
                val parsed = withContext(Dispatchers.IO) {
                    ctx.contentResolver.openInputStream(uri)?.use { stream ->
                        val output = java.io.ByteArrayOutputStream()
                        val buffer = ByteArray(4096)
                        while (output.size() <= 65536) {
                            val n = stream.read(buffer)
                            if (n < 0) break
                            if (output.size() + n > 65536) throw IllegalArgumentException("Arquivo muito grande.")
                            output.write(buffer, 0, n)
                        }
                        GeminiKeyParser.parse(output.toString("UTF-8"))
                    } ?: throw IllegalArgumentException("Arquivo inacessível.")
                }
                pendingImport = parsed
                confirm = "import"
            } catch (_: Exception) { feedback = "Importação rejeitada: arquivo ou formato inválido." }
            finally { busy = false }
        }
    }
    val exportPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri != null) scope.launch {
            busy = true
            try {
                withContext(Dispatchers.IO) {
                    val saved = store.read() ?: throw IllegalStateException("Sem chave salva")
                    ctx.contentResolver.openOutputStream(uri, "wt")?.use {
                        it.write("GEMINI_API_KEY=$saved\n".toByteArray(Charsets.UTF_8))
                        it.flush()
                    } ?: throw java.io.IOException("Destino indisponível")
                }
                feedback = "Arquivo exportado. Guarde-o em local seguro."
            } catch (_: Exception) { feedback = "Não foi possível exportar a chave." }
            finally { busy = false }
        }
    }
    DisposableEffect(api) { onDispose { api.cancel() } }
    HorizontalDivider()
    Row { Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(8.dp)); Text("Google Gemini — API de IA", style = MaterialTheme.typography.titleMedium) }
    Text("Chave pessoal para narrações por IA. Somente o texto informado será enviado ao Gemini.",
        style = MaterialTheme.typography.bodySmall)
    OutlinedTextField(value = key, onValueChange = { key = it.take(512) },
        modifier = Modifier.fillMaxWidth(), label = { Text("Chave da API") }, singleLine = true,
        visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
        supportingText = { Text(if (hasKey) "Há uma chave protegida salva no aparelho. Digite outra para substituir."
            else "Nenhuma chave salva. A chave digitada só será persistida ao tocar Salvar.") })
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedButton(onClick = {
            val clip = (ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager)
                .primaryClip?.getItemAt(0)?.coerceToText(ctx)?.toString().orEmpty()
            key = clip.take(512); feedback = "Texto colado. Confira antes de salvar."
        }, enabled = !busy, contentPadding = PaddingValues(horizontal = 9.dp)) { Text("Colar") }
        OutlinedButton(onClick = { confirm = "copy" }, enabled = !busy && (key.isNotBlank() || hasKey),
            contentPadding = PaddingValues(horizontal = 9.dp)) { Text("Copiar") }
        OutlinedButton(onClick = { show = !show }, enabled = !busy,
            contentPadding = PaddingValues(horizontal = 9.dp)) { Text(if (show) "Ocultar" else "Mostrar") }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedButton(onClick = { importPicker.launch(arrayOf("text/plain", "application/json", "application/octet-stream", "*/*")) },
            enabled = !busy, contentPadding = PaddingValues(horizontal = 10.dp)) { Text("Importar") }
        OutlinedButton(onClick = { confirm = "export" }, enabled = !busy && hasKey,
            contentPadding = PaddingValues(horizontal = 10.dp)) { Text("Exportar") }
        Button(onClick = {
            try {
                store.save(key); hasKey = true; key = ""; show = false
                feedback = "Chave armazenada com criptografia no dispositivo."
            } catch (_: Exception) { feedback = "A chave é inválida ou não foi possível protegê-la." }
        }, enabled = !busy && key.isNotBlank(), contentPadding = PaddingValues(horizontal = 10.dp)) { Text("Salvar") }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedButton(onClick = { confirm = "test" }, enabled = !busy && hasKey,
            contentPadding = PaddingValues(horizontal = 10.dp)) { Text("Testar TTS") }
        OutlinedButton(onClick = { confirm = "remove" }, enabled = !busy && hasKey,
            contentPadding = PaddingValues(horizontal = 10.dp)) { Text("Remover chave") }
    }
    TextButton(onClick = {
        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/api-keys")))
    }) { Text("Obter chave no Google AI Studio") }
    Text("A chave está protegida no armazenamento privado, mas dispositivos comprometidos e a área de transferência ainda podem expô-la. " +
        "Para distribuir um app com credenciais do desenvolvedor, use um backend seguro.",
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
    if (feedback.isNotEmpty()) Text(feedback, style = MaterialTheme.typography.bodySmall)
    if (confirm.isNotEmpty()) AlertDialog(onDismissRequest = { confirm = ""; pendingImport = null },
        title = { Text(when (confirm) {
            "import" -> "Importar chave?"; "copy" -> "Copiar credencial?"; "export" -> "Exportar chave?"
            "remove" -> "Remover chave?"; else -> "Testar serviço TTS?"
        }) },
        text = { Text(when (confirm) {
            "import" -> "Chave encontrada: ${GeminiKeyParser.masked(pendingImport.orEmpty())}. A chave atual será substituída apenas ao confirmar."
            "copy" -> "A chave ficará disponível na área de transferência do Android. Outros aplicativos ou o histórico da área de transferência podem acessá-la."
            "export" -> "O arquivo .env conterá sua chave em TEXTO LEGÍVEL. Ele não será criptografado."
            "remove" -> "A chave criptografada será removida deste aparelho. Os áudios já gerados não serão apagados."
            else -> "Uma geração curta de áudio verificará o acesso real ao TTS e poderá consumir cota ou gerar cobrança. Continuar?"
        }) },
        confirmButton = { TextButton(onClick = {
            val action = confirm; confirm = ""
            when (action) {
                "import" -> {
                    try { store.save(pendingImport ?: ""); hasKey = true; key = ""; show = false
                        feedback = "Chave importada e protegida." }
                    catch (_: Exception) { feedback = "Falha ao armazenar chave importada." }
                    finally { pendingImport = null }
                }
                "remove" -> try { store.remove(); hasKey = false; key = ""; feedback = "Chave removida." }
                    catch (_: Exception) { feedback = "Falha ao remover a chave." }
                "export" -> exportPicker.launch("Gemini-API-KEY.env")
                "copy" -> {
                    try {
                        val value = if (key.isNotBlank()) GeminiKeyParser.validate(key) else store.read() ?: ""
                        val clip = ClipData.newPlainText("Credencial privada", value)
                        if (Build.VERSION.SDK_INT >= 33) clip.description.extras = android.os.PersistableBundle().apply {
                            putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
                        }
                        (ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager)
                            .setPrimaryClip(clip)
                        feedback = "Chave copiada. Limpe a área de transferência quando terminar."
                    } catch (_: Exception) { feedback = "Não foi possível copiar a chave." }
                }
                "test" -> scope.launch {
                    busy = true; feedback = "Consultando modelo e gerando amostra curta..."
                    try {
                        val saved = withContext(Dispatchers.IO) { store.read() } ?: throw IllegalStateException("Chave ausente")
                        val model = GeminiModels.LITE
                        if (!api.checkModel(saved, model)) {
                            feedback = "Modelo Flash-Lite TTS indisponível para esta chave/projeto."
                        } else {
                            api.synthesize(saved, model, "Kore", "neutral and clear", "Olá, teste de narração.")
                            feedback = "Chave válida e serviço Gemini TTS acessível. Amostra gerada com sucesso."
                        }
                    } catch (e: CancellationException) { throw e }
                    catch (e: GeminiApiError) { feedback = e.message ?: "Falha na API." }
                    catch (e: Exception) { feedback = if (e is java.io.IOException) e.message ?: "Falha de rede." else "Falha ao testar TTS ou recuperar a chave." }
                    finally { busy = false }
                }
            }
        }) { Text("Confirmar") } },
        dismissButton = { TextButton(onClick = { confirm = ""; pendingImport = null }) { Text("Cancelar") } })
}
