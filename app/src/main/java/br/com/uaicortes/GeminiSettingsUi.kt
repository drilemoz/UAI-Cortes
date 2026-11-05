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
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
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
    val api = remember(ctx) { GeminiTtsClient(ctx) }
    val scope = rememberCoroutineScope()
    var key by remember { mutableStateOf("") }
    var hasKey by remember { mutableStateOf(store.hasKey()) }
    var show by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var pendingImport by remember { mutableStateOf<String?>(null) }
    var confirm by remember { mutableStateOf("") }
    var privacyExpanded by remember { mutableStateOf(false) }
    var keysRevision by remember { mutableIntStateOf(0) }
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
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp))
            Text("Google Gemini · API de IA", style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f))
            if (hasKey) Surface(shape = RoundedCornerShape(7.dp),
                color = MaterialTheme.colorScheme.primaryContainer) {
                Text("Protegida", modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                    fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
        Text(if (hasKey) "Chave salva protegida. Para substituir, digite ou importe outra."
             else "Configure sua chave pessoal para gerar narrações.",
             style = MaterialTheme.typography.bodySmall,
             color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(value = key, onValueChange = { key = it.take(512) },
            modifier = Modifier.fillMaxWidth(), label = { Text("Chave da API") }, singleLine = true,
            placeholder = { if (hasKey) Text("Chave salva não é exibida") },
            shape = RoundedCornerShape(9.dp),
            visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { show = !show }, enabled = !busy) {
                    Icon(if (show) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        if (show) "Ocultar chave digitada" else "Mostrar chave digitada")
                }
            })
        // Uma única barra. Se a fonte/tela não comportar 6 áreas de toque de 48 dp,
        // habilitar rolagem horizontal em vez de cortar ou comprimir os comandos.
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val compactScreen = maxWidth < 312.dp
            Row(modifier = if (compactScreen) Modifier.horizontalScroll(rememberScrollState())
                    else Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                GeminiKeyAction("Colar", Icons.Default.ContentPaste,
                    if (compactScreen) Modifier.width(58.dp) else Modifier.weight(1f), !busy) {
                    val clip = (ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                        as android.content.ClipboardManager)
                        .primaryClip?.getItemAt(0)?.coerceToText(ctx)?.toString().orEmpty()
                    key = clip.take(512); feedback = "Texto colado. Confira antes de salvar."
                }
                GeminiKeyAction("Copiar", Icons.Default.ContentCopy,
                    if (compactScreen) Modifier.width(58.dp) else Modifier.weight(1f),
                    !busy && (key.isNotBlank() || hasKey)) { confirm = "copy" }
                GeminiKeyAction("Importar", Icons.Default.FileOpen,
                    if (compactScreen) Modifier.width(58.dp) else Modifier.weight(1f), !busy) {
                    importPicker.launch(arrayOf("text/plain", "application/json", "application/octet-stream", "*/*"))
                }
                GeminiKeyAction("Exportar", Icons.Default.FileUpload,
                    if (compactScreen) Modifier.width(58.dp) else Modifier.weight(1f), !busy && hasKey) {
                    confirm = "export"
                }
                GeminiKeyAction("Obter", Icons.Default.OpenInNew,
                    if (compactScreen) Modifier.width(58.dp) else Modifier.weight(1f), !busy) {
                    ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/api-keys")))
                }
                GeminiKeyAction("Remover", Icons.Default.DeleteOutline,
                    if (compactScreen) Modifier.width(58.dp) else Modifier.weight(1f),
                    !busy && hasKey, dangerous = true) { confirm = "remove" }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { confirm = "test" }, enabled = !busy && hasKey,
                shape = RoundedCornerShape(9.dp),
                contentPadding = PaddingValues(horizontal = 8.dp),
                modifier = Modifier.weight(1f).heightIn(min = 46.dp)) {
                Icon(Icons.Default.NetworkCheck, null, Modifier.size(18.dp))
                Spacer(Modifier.width(5.dp))
                Text("Testar TTS", maxLines = 1, fontSize = 12.sp)
            }
            Button(onClick = {
                try {
                    store.save(key); hasKey = true; key = ""; show = false
                    feedback = "Chave ativa atualizada e protegida no dispositivo."; keysRevision++
                } catch (_: Exception) { feedback = "A chave é inválida ou não foi possível protegê-la." }
            }, enabled = !busy && key.isNotBlank(), shape = RoundedCornerShape(9.dp),
                contentPadding = PaddingValues(horizontal = 8.dp),
                modifier = Modifier.weight(1f).heightIn(min = 46.dp)) {
                Icon(Icons.Default.Save, null, Modifier.size(18.dp))
                Spacer(Modifier.width(5.dp))
                Text(if (hasKey) "Atualizar ativa" else "Salvar chave", maxLines = 1,
                    overflow = TextOverflow.Ellipsis, fontSize = 12.sp)
            }
        }
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (feedback.isNotBlank()) Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            shape = RoundedCornerShape(9.dp)) {
            Text(feedback, Modifier.padding(10.dp), style = MaterialTheme.typography.bodySmall)
        }
        Surface(shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f)) {
            Column(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().clickable { privacyExpanded = !privacyExpanded }
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Shield, null, modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Privacidade e segurança", Modifier.weight(1f),
                        style = MaterialTheme.typography.titleSmall)
                    Icon(if (privacyExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        if (privacyExpanded) "Recolher avisos" else "Expandir avisos", Modifier.size(20.dp))
                }
                if (privacyExpanded) Text(
                    "Somente o texto e os parâmetros da narração são enviados ao Gemini. " +
                    "A chave está protegida no armazenamento privado, mas dispositivos comprometidos, " +
                    "a área de transferência e arquivos exportados ainda podem expô-la. " +
                    "Para distribuir um aplicativo com credenciais administradas pelo desenvolvedor, use backend seguro.",
                    modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        androidx.compose.runtime.key(keysRevision) { GeminiKeyPoolSettings(store) { hasKey = store.hasKey() } }
    }
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
                        feedback = "Chave importada e protegida."; keysRevision++ }
                    catch (_: Exception) { feedback = "Falha ao armazenar chave importada." }
                    finally { pendingImport = null }
                }
                "remove" -> try { store.remove(); hasKey = false; key = ""; feedback = "Chave removida."; keysRevision++ }
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


@Composable
internal fun GeminiSettingsScreen(back: () -> Unit) {
    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())
        .padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Heading("Google Gemini", back)
        GeminiSettingsSection()
        GeminiUsagePanel()
        Spacer(Modifier.height(12.dp))
    }
}

/** Ferramenta compacta com ação preservada e alvo de toque >= 48 dp. */
@Composable
private fun GeminiKeyAction(label: String, icon: ImageVector,
    modifier: Modifier = Modifier, enabled: Boolean = true, dangerous: Boolean = false,
    onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val foreground = when {
        !enabled -> scheme.onSurface.copy(alpha = 0.38f)
        dangerous -> scheme.error
        else -> scheme.primary
    }
    Surface(onClick = onClick, enabled = enabled,
        modifier = modifier.height(56.dp).semantics { contentDescription = label },
        shape = RoundedCornerShape(8.dp),
        color = scheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, scheme.outlineVariant)) {
        Column(Modifier.fillMaxSize().padding(horizontal = 1.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
            Icon(icon, null, Modifier.size(18.dp), tint = foreground)
            Spacer(Modifier.height(3.dp))
            Text(label, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                color = foreground)
        }
    }
}
