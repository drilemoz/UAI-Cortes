package br.com.uaicortes

import android.content.ClipData
import android.content.ClipDescription
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import java.io.File

/** Configuração ElevenLabs independente de Gemini/Polly. Não modifica projetos ou a voz padrão do editor. */
@Composable
internal fun ElevenLabsSettingsScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val store = remember(ctx) { ElevenLabsCredentialStore(ctx) }
    val scope = rememberCoroutineScope()
    var key by remember { mutableStateOf("") }
    var hasSaved by remember { mutableStateOf(store.hasKey()) }
    var visible by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf("") }
    var showRemove by remember { mutableStateOf(false) }
    var showGenerate by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }
    var account by remember { mutableStateOf<ElevenLabsAccount?>(null) }
    var voices by remember { mutableStateOf<List<ElevenLabsVoice>>(emptyList()) }
    var nextPage by remember { mutableStateOf<String?>(null) }
    var voiceId by remember { mutableStateOf("") }
    var voiceMenu by remember { mutableStateOf(false) }
    var sample by remember { mutableStateOf("Olá! Este é um teste da ElevenLabs no Uai Cortes.") }
    var audio by remember { mutableStateOf<File?>(null) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { player?.release() }; player = null
            audio?.let { runCatching { it.delete() } }; audio = null
        }
    }

    fun clearPreview() {
        runCatching { player?.release() }; player = null
        audio?.let { runCatching { it.delete() } }; audio = null
    }
    fun clipboard(): String = (ctx.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager)
        .primaryClip?.getItemAt(0)?.coerceToText(ctx)?.toString().orEmpty()
    fun copySensitive(value: String) {
        val clip = ClipData.newPlainText("Chave ElevenLabs sensível", value)
        if (Build.VERSION.SDK_INT >= 33) clip.description.extras = android.os.PersistableBundle().apply {
            putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
        }
        (ctx.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager)
            .setPrimaryClip(clip)
        feedback = "Chave copiada. Limpe a área de transferência após utilizar."
    }
    fun connectionTest() {
        if (busy) return
        busy = true
        scope.launch {
            try {
                account = ElevenLabsApi.account(store.read() ?: error("Salve uma chave primeiro."))
                feedback = "Chave aceita. Conta consultada sem gerar áudio nem consumir créditos de narração."
            } catch (c: CancellationException) { throw c }
            catch (e: Exception) {
                account = null
                feedback = e.message ?: "Falha de conexão. Confira sua internet e a chave."
            } finally { busy = false }
        }
    }
    fun loadVoices(more: Boolean) {
        if (busy) return
        busy = true
        scope.launch {
            try {
                val page = ElevenLabsApi.voices(store.read() ?: error("Salve uma chave primeiro."),
                    if (more) nextPage else null)
                voices = if (more) (voices + page.voices).distinctBy { it.id } else page.voices
                nextPage = page.nextPageToken
                if (voiceId !in voices.map { it.id }) voiceId = voices.firstOrNull()?.id.orEmpty()
                feedback = "${voices.size} voz(es) carregadas. Nenhuma geração de áudio foi solicitada."
            } catch (c: CancellationException) { throw c }
            catch (e: Exception) { feedback = e.message ?: "Não foi possível consultar as vozes." }
            finally { busy = false }
        }
    }
    fun generatePreview() {
        if (busy) return
        showGenerate = false
        busy = true
        scope.launch {
            try {
                // Nunca repetir automaticamente POST: uma falha de rede pode ter consumido créditos.
                val result = ElevenLabsApi.synthesize(store.read() ?: error("Chave não encontrada."),
                    voiceId, sample.trim())
                val file = withContext(Dispatchers.IO) {
                    File.createTempFile("elevenlabs_preview_", ".mp3", ctx.cacheDir).apply {
                        writeBytes(result)
                    }
                }
                clearPreview()
                audio = file
                feedback = "Prévia MP3 criada. A geração pode consumir créditos da sua conta."
            } catch (c: CancellationException) { throw c }
            catch (e: Exception) {
                feedback = "Falha na geração: ${e.message ?: "erro de conexão"}. Verifique seus créditos antes de repetir."
            } finally { busy = false }
        }
    }
    val importFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null && !busy) scope.launch {
            busy = true
            try {
                val parsed = withContext(Dispatchers.IO) {
                    val stream = ctx.contentResolver.openInputStream(uri) ?: error("Arquivo inacessível.")
                    stream.use { input ->
                        val out = java.io.ByteArrayOutputStream()
                        val buffer = ByteArray(2048)
                        while (true) {
                            val n = input.read(buffer)
                            if (n < 0) break
                            require(out.size() + n <= 16_384) { "Arquivo acima de 16 KB." }
                            out.write(buffer, 0, n)
                        }
                        ElevenLabsKeyPolicy.importText(out.toString("UTF-8"))
                    }
                }
                // Importar preenche o campo, mas não substitui automaticamente a chave salva.
                key = parsed
                visible = false
                feedback = "Chave importada no campo. Toque em Salvar para protegê-la."
            } catch (_: Exception) { feedback = "Importação rejeitada. Use ELEVENLABS_API_KEY=... ou JSON simples." }
            finally { busy = false }
        }
    }
    val exportAudio = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("audio/mpeg")) { uri ->
        val source = audio
        if (uri != null && source?.isFile == true) scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    ctx.contentResolver.openOutputStream(uri, "wt")?.use { output ->
                        source.inputStream().use { it.copyTo(output) }
                    } ?: error("Destino indisponível")
                }
                feedback = "Prévia MP3 salva no local escolhido."
            } catch (_: Exception) { feedback = "Não foi possível salvar a prévia MP3." }
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        Heading("ElevenLabs", onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Configuração independente de Gemini e Amazon Polly", style = MaterialTheme.typography.titleMedium)
            Text(if (hasSaved) "Chave protegida no Android Keystore" else "Nenhuma chave configurada",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(key, { key = it.take(512) }, Modifier.fillMaxWidth(),
                label = { Text("API Key ElevenLabs") }, singleLine = true,
                placeholder = { if (hasSaved) Text("Chave salva e oculta: use Ver") else Text("Cole sua chave") },
                visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { visible = !visible }) {
                        Icon(if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            "Mostrar ou ocultar chave")
                    }
                })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { key = clipboard().trim().take(512); visible = false },
                    modifier = Modifier.weight(1f), enabled = !busy) { Text("Colar") }
                OutlinedButton(onClick = {
                    runCatching { key = store.read().orEmpty(); visible = true }
                        .onFailure { feedback = it.message ?: "Chave indisponível" }
                }, modifier = Modifier.weight(1f), enabled = hasSaved && !busy) { Text("Ver") }
                OutlinedButton(onClick = {
                    runCatching { copySensitive(if (key.isNotBlank()) ElevenLabsKeyPolicy.validate(key)
                        else store.read() ?: error("Sem chave cadastrada")) }
                        .onFailure { feedback = it.message ?: "Chave indisponível" }
                }, modifier = Modifier.weight(1f), enabled = (hasSaved || key.isNotBlank()) && !busy) {
                    Text("Copiar")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    try {
                        store.save(key)
                        key = ""; visible = false; hasSaved = true
                        account = null; voices = emptyList(); voiceId = ""; nextPage = null
                        clearPreview()
                        feedback = "Chave salva de forma protegida. Use Testar conexão."
                    } catch (e: Exception) { feedback = e.message ?: "Falha ao salvar chave." }
                }, enabled = !busy && key.isNotBlank(), modifier = Modifier.weight(1f)) { Text("Salvar") }
                OutlinedButton(onClick = { importFile.launch(arrayOf("text/plain", "application/json", "*/*")) },
                    enabled = !busy, modifier = Modifier.weight(1f)) { Text("Importar") }
                OutlinedButton(onClick = { showRemove = true }, enabled = !busy && hasSaved,
                    modifier = Modifier.weight(1f)) { Text("Remover") }
            }
            OutlinedButton(onClick = { showHelp = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.HelpOutline, null)
                Spacer(Modifier.width(8.dp))
                Text("Como obter e configurar a chave (?)")
            }
            HorizontalDivider()
            Text("Verificar acesso", style = MaterialTheme.typography.titleMedium)
            Button(onClick = { connectionTest() }, modifier = Modifier.fillMaxWidth(),
                enabled = hasSaved && !busy) { Text("Testar conexão (sem gerar áudio)") }
            account?.let { data ->
                Text("Plano: ${data.tier ?: "não informado"}. " +
                    (data.remainingCredits?.let { "Saldo calculado pelos contadores de caracteres: $it." }
                        ?: "Cota: consulte o painel oficial da ElevenLabs."),
                    style = MaterialTheme.typography.bodySmall)
            }
            OutlinedButton(onClick = { loadVoices(false) }, enabled = hasSaved && !busy,
                modifier = Modifier.fillMaxWidth()) { Text("Consultar vozes da conta") }
            if (voices.isNotEmpty()) {
                val selected = voices.firstOrNull { it.id == voiceId }
                Box {
                    OutlinedButton(onClick = { voiceMenu = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Voz: ${selected?.name ?: "Selecionar"}")
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Default.ArrowDropDown, null)
                    }
                    DropdownMenu(expanded = voiceMenu, onDismissRequest = { voiceMenu = false }) {
                        voices.forEach { voice ->
                            DropdownMenuItem(text = { Text("${voice.name} · ${voice.language ?: voice.category ?: "voz"}") },
                                onClick = { voiceId = voice.id; voiceMenu = false; clearPreview() })
                        }
                    }
                }
                if (nextPage != null) OutlinedButton(onClick = { loadVoices(true) },
                    enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Carregar mais vozes") }
                HorizontalDivider()
                Text("Teste opcional de narração", style = MaterialTheme.typography.titleMedium)
                Text("Gerar MP3 pode consumir créditos, mesmo em testes. A prévia não é inserida automaticamente no projeto.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(sample, { sample = it.take(500) }, Modifier.fillMaxWidth(),
                    label = { Text("Frase de teste") }, minLines = 2, maxLines = 4,
                    supportingText = { Text("${sample.length}/500 caracteres. Modelo: eleven_multilingual_v2") })
                Button(onClick = { showGenerate = true },
                    enabled = !busy && sample.isNotBlank() && voiceId.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()) { Text("Gerar prévia MP3 (com confirmação)") }
                if (audio?.isFile == true) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            try {
                                runCatching { player?.release() }; player = null
                                val file = audio ?: error("Prévia não disponível")
                                val p = MediaPlayer().apply {
                                    setDataSource(file.absolutePath)
                                    setOnPreparedListener { it.start() }
                                    setOnErrorListener { _, _, _ ->
                                        feedback = "Erro ao reproduzir MP3."; true
                                    }
                                }
                                player = p
                                p.prepareAsync()
                            } catch (_: Exception) { feedback = "Não foi possível reproduzir a prévia." }
                        }, modifier = Modifier.weight(1f)) { Text("Ouvir") }
                        OutlinedButton(onClick = { exportAudio.launch("UaiCortes-ElevenLabs.mp3") },
                            modifier = Modifier.weight(1f)) { Text("Salvar MP3") }
                    }
                }
            }
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (feedback.isNotBlank()) Text(feedback, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("A chave é mantida apenas neste aparelho. Como a API é chamada diretamente do Android, " +
                "evite distribuir o APK com sua chave e restrinja permissões e créditos no painel ElevenLabs.",
                style = MaterialTheme.typography.bodySmall)
        }
    }

    if (showGenerate) AlertDialog(onDismissRequest = { showGenerate = false },
        title = { Text("Confirmar geração de voz") },
        text = { Text("Esta solicitação envia ${sample.length} caracteres para a ElevenLabs e pode consumir créditos. Deseja gerar a prévia?") },
        confirmButton = { TextButton(onClick = { generatePreview() }) { Text("Gerar áudio") } },
        dismissButton = { TextButton(onClick = { showGenerate = false }) { Text("Cancelar") } })
    if (showRemove) AlertDialog(onDismissRequest = { showRemove = false },
        title = { Text("Remover chave ElevenLabs?") },
        text = { Text("A chave salva neste aparelho será apagada. Isso não revoga a chave no painel ElevenLabs.") },
        confirmButton = { TextButton(onClick = {
            try {
                store.remove(); hasSaved = false; key = ""; visible = false
                account = null; voices = emptyList(); voiceId = ""; nextPage = null
                clearPreview(); feedback = "Chave removida do aparelho."
            } catch (e: Exception) { feedback = e.message ?: "Falha ao remover." }
            showRemove = false
        }) { Text("Remover") } },
        dismissButton = { TextButton(onClick = { showRemove = false }) { Text("Cancelar") } })
    if (showHelp) AlertDialog(onDismissRequest = { showHelp = false },
        title = { Text("Configurar ElevenLabs") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("1. Entre na ElevenLabs e abra Developers > API Keys.")
            Text("2. Crie uma chave, copie-a na criação e configure permissões para leitura de usuário/vozes e síntese de fala, conforme sua necessidade.")
            Text("3. Cole a chave, toque em Salvar e depois em Testar conexão. Algumas chaves restritas podem negar /v1/user e ainda permitir síntese.")
            Text("4. Consultar vozes não gera áudio. Somente Gerar prévia envia texto e pode consumir créditos.")
            Text("5. Em caso de HTTP 403, revise os escopos e eventuais restrições de IP da chave.")
        } },
        confirmButton = { TextButton(onClick = { showHelp = false }) { Text("Fechar") } },
        dismissButton = { TextButton(onClick = {
            showHelp = false
            runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW,
                Uri.parse("https://elevenlabs.io/app/settings/api-keys"))) }
        }) { Text("Abrir ElevenLabs") } })
}
