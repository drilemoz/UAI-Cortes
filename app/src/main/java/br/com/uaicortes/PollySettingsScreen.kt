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
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Ferramenta AWS separada da Gemini, sem novas credenciais incluídas na distribuição Android. */
@Composable
internal fun PollySettingsScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val store = remember(ctx) { PollyCredentialStore(ctx) }
    val usage = remember(ctx) { PollyUsageStore(ctx) }
    val scope = rememberCoroutineScope()
    var hasSaved by remember { mutableStateOf(store.hasCredentials()) }
    var access by remember { mutableStateOf("") }
    var secret by remember { mutableStateOf("") }
    var region by remember { mutableStateOf("us-east-1") }
    var session by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var dialog by remember { mutableStateOf("") }
    var incoming by remember { mutableStateOf<PollyCredentials?>(null) }
    var voices by remember { mutableStateOf<List<PollyVoice>>(emptyList()) }
    var voiceId by remember { mutableStateOf("") }
    var engine by remember { mutableStateOf("neural") }
    var selectedVoiceMenu by remember { mutableStateOf(false) }
    var selectedEngineMenu by remember { mutableStateOf(false) }
    var sample by remember { mutableStateOf("Olá! Esta é uma prévia de voz do Amazon Polly para o Uai Cortes.") }
    var audioFile by remember { mutableStateOf<File?>(null) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var dayLimitText by remember { mutableStateOf(usage.limits().first.toString()) }
    var monthLimitText by remember { mutableStateOf(usage.limits().second.toString()) }
    var revision by remember { mutableIntStateOf(0) }
    DisposableEffect(Unit) {
        onDispose { player?.runCatching { release() }; player = null }
    }
    val activeVoice = voices.firstOrNull { it.id == voiceId }
    val current = remember(revision) { usage.events() }
    val totals = remember(revision) { usage.sums() }
    val monthly = current.filter { it.timestamp >= java.time.LocalDate.now(java.time.ZoneOffset.UTC)
        .withDayOfMonth(1).atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli() }
    val spent = monthly.sumOf { it.estimatedUsd ?: 0.0 }
    val unknown = monthly.count { it.billedCharacters == null }

    fun clipboardValue(): String = (ctx.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager)
        .primaryClip?.getItemAt(0)?.coerceToText(ctx)?.toString().orEmpty()
    fun putClipboard(value: String) {
        val clip = ClipData.newPlainText("Credencial AWS sensível", value)
        if (Build.VERSION.SDK_INT >= 33) clip.description.extras = android.os.PersistableBundle().apply {
            putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
        }
        (ctx.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager)
            .setPrimaryClip(clip)
        feedback = "Copiado. A área de transferência pode ser acessível a outros apps; limpe-a depois."
    }
    fun rememberSaved() {
        val c = store.read() ?: error("Configure credenciais primeiro.")
        access = c.accessKeyId; secret = c.secretAccessKey; region = c.region; session = c.sessionToken
    }
    val exportCreds = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            busy = true
            try {
                withContext(Dispatchers.IO) {
                    val raw = PollyCredentialCodec.encode(store.read() ?: error("Sem credenciais salvas"))
                    ctx.contentResolver.openOutputStream(uri, "wt")?.use { it.write(raw.toByteArray(Charsets.UTF_8)) }
                        ?: error("Destino inacessível")
                }
                feedback = "Arquivo sensível exportado. Não o compartilhe nem envie ao GitHub."
            } catch (_: Exception) { feedback = "Não foi possível exportar credenciais." }
            finally { busy = false }
        }
    }
    val importCreds = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            busy = true
            try {
                incoming = withContext(Dispatchers.IO) {
                    val raw = ctx.contentResolver.openInputStream(uri)?.use { stream ->
                        val buffer = ByteArray(4096)
                        val out = java.io.ByteArrayOutputStream()
                        while (true) {
                            val n = stream.read(buffer)
                            if (n < 0) break
                            require(out.size() + n <= 16_384) { "Arquivo acima de 16 KB" }
                            out.write(buffer, 0, n)
                        }
                        out.toString("UTF-8")
                    } ?: error("Arquivo inacessível")
                    PollyCredentialCodec.decode(raw)
                }
                dialog = "import"
            } catch (_: Exception) { feedback = "Importação rejeitada: arquivo inválido ou excede 16 KB." }
            finally { busy = false }
        }
    }
    val exportAudio = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("audio/mpeg")) { uri ->
        val file = audioFile
        if (uri != null && file != null) scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    ctx.contentResolver.openOutputStream(uri, "wt")?.use { output -> file.inputStream().use { it.copyTo(output) } }
                        ?: error("Destino indisponível")
                }
                feedback = "Áudio MP3 exportado."
            } catch (_: Exception) { feedback = "Não foi possível exportar áudio." }
        }
    }
    val exportUsage = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    ctx.contentResolver.openOutputStream(uri, "wt")?.use {
                        it.write(usage.csv().toByteArray(Charsets.UTF_8))
                    } ?: error("Destino inacessível")
                }
                feedback = "Relatório exportado sem credenciais nem textos."
            } catch (_: Exception) { feedback = "Não foi possível exportar o relatório." }
        }
    }
    fun refreshVoices() {
        if (busy) return
        busy = true
        scope.launch {
            try {
                val v = PollyApi.voices(store.read() ?: error("Salve as credenciais antes do teste."))
                voices = v
                voiceId = v.firstOrNull { "neural" in it.engines }?.id ?: v.firstOrNull()?.id.orEmpty()
                engine = v.firstOrNull { it.id == voiceId }?.engines?.firstOrNull() ?: "standard"
                feedback = "Conexão autorizada: ${v.size} vozes pt-BR consultadas. Nenhum áudio gerado."
            } catch (c: CancellationException) { throw c }
            catch (e: Exception) { feedback = e.message ?: "Falha ao consultar AWS." }
            finally { busy = false }
        }
    }
    fun generate() {
        val v = activeVoice ?: return
        if (busy) return
        busy = true
        scope.launch {
            try {
                // Não há retry de POST: uma falha depois do envio pode ter gerado consumo remoto.
                val credentials = store.read() ?: error("Credenciais indisponíveis")
                val result = PollyApi.synthesize(credentials, sample, v, engine)
                val file = withContext(Dispatchers.IO) {
                    File(ctx.cacheDir, "polly_preview_${System.currentTimeMillis()}.mp3").apply {
                        writeBytes(result.audio)
                    }
                }
                audioFile?.takeIf { it != file }?.delete()
                audioFile = file
                try {
                    withContext(Dispatchers.IO) { usage.record(result, credentials.region) }
                    revision++
                    feedback = if (result.requestCharacters != null)
                        "Áudio gerado: ${result.requestCharacters} caracteres confirmados pela AWS."
                    else "Áudio gerado; AWS não informou caracteres. Consumo não contabilizado."
                } catch (_: Exception) {
                    feedback = "Áudio gerado, mas o registro local falhou. Confira custos no console AWS."
                }
            } catch (c: CancellationException) { throw c }
            catch (e: Exception) { feedback = "Falha no envio: ${e.message ?: "erro de conexão"}. Confira o faturamento AWS; não repita sem verificar." }
            finally { busy = false }
        }
    }
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        Heading("Amazon Polly", onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Configuração independente do Gemini", style = MaterialTheme.typography.titleMedium)
            Text(if (hasSaved) "Credenciais protegidas no dispositivo" else "Sem credenciais configuradas",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(access, { access = it.take(128) }, Modifier.fillMaxWidth(),
                label = { Text("Access Key ID") }, singleLine = true,
                placeholder = { if (hasSaved) Text("Salva e oculta; tocar em Ver") })
            OutlinedTextField(secret, { secret = it.take(256) }, Modifier.fillMaxWidth(),
                label = { Text("Secret Access Key") }, singleLine = true,
                visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = { IconButton({ visible = !visible }) {
                    Icon(if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility, "Mostrar ou ocultar")
                } })
            OutlinedTextField(region, { region = it.take(30) }, Modifier.fillMaxWidth(),
                label = { Text("Região AWS (ex.: us-east-1)") }, singleLine = true)
            OutlinedTextField(session, { session = it.take(8192) }, Modifier.fillMaxWidth(),
                label = { Text("Session Token (opcional, credencial temporária)") },
                maxLines = 2, visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { access = clipboardValue().take(128) }, modifier = Modifier.weight(1f)) { Text("Colar ID") }
                OutlinedButton(onClick = { secret = clipboardValue().take(256) }, modifier = Modifier.weight(1f)) { Text("Colar Secret") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { runCatching { rememberSaved() }
                    .onFailure { feedback = it.message.orEmpty() } }, enabled = hasSaved, modifier = Modifier.weight(1f)) { Text("Ver salvas") }
                OutlinedButton(onClick = { dialog = "copy" }, enabled = hasSaved || access.isNotEmpty(), modifier = Modifier.weight(1f)) { Text("Copiar") }
                Button(onClick = { try {
                    store.save(PollyCredentials(access, secret, region, session))
                    hasSaved = true; access = ""; secret = ""; session = ""; visible = false
                    voices = emptyList(); voiceId = ""; audioFile = null
                    feedback = "Credenciais protegidas. Teste a conexão para conferir IAM e região."
                } catch (e: Exception) { feedback = e.message ?: "Não foi possível salvar." } },
                    enabled = !busy && access.isNotBlank() && secret.isNotBlank(), modifier = Modifier.weight(1f)) { Text("Salvar") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { importCreds.launch(arrayOf("application/json", "text/plain", "*/*")) }, modifier = Modifier.weight(1f)) { Text("Importar") }
                OutlinedButton(onClick = { dialog = "export" }, enabled = hasSaved, modifier = Modifier.weight(1f)) { Text("Exportar") }
                OutlinedButton(onClick = { dialog = "remove" }, enabled = hasSaved, modifier = Modifier.weight(1f)) { Text("Remover") }
            }
            OutlinedButton(onClick = { ctx.startActivity(Intent(Intent.ACTION_VIEW,
                Uri.parse("https://console.aws.amazon.com/iam/home#/users"))) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.HelpOutline, null)
                Spacer(Modifier.width(8.dp))
                Text("Como obter credenciais na AWS")
            }
            HorizontalDivider()
            Text("Testar conexão e voz", style = MaterialTheme.typography.titleMedium)
            Button(onClick = { refreshVoices() }, enabled = hasSaved && !busy,
                modifier = Modifier.fillMaxWidth()) { Text("Consultar vozes em português (sem gerar áudio)") }
            if (voices.isNotEmpty()) {
                Box {
                    OutlinedButton({ selectedVoiceMenu = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Voz: ${activeVoice?.name ?: "Selecionar"} · ${activeVoice?.id ?: ""}")
                        Icon(Icons.Default.ArrowDropDown, null)
                    }
                    DropdownMenu(selectedVoiceMenu, onDismissRequest = { selectedVoiceMenu = false }) {
                        voices.forEach { v -> DropdownMenuItem(text = { Text("${v.name} (${v.id})") }, onClick = {
                            voiceId = v.id; engine = v.engines.firstOrNull() ?: "standard"; selectedVoiceMenu = false
                        }) }
                    }
                }
                Box {
                    OutlinedButton({ selectedEngineMenu = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Mecanismo: $engine"); Icon(Icons.Default.ArrowDropDown, null)
                    }
                    DropdownMenu(selectedEngineMenu, onDismissRequest = { selectedEngineMenu = false }) {
                        activeVoice?.engines?.forEach { e -> DropdownMenuItem(text = { Text(e) },
                            onClick = { engine = e; selectedEngineMenu = false }) }
                    }
                }
                OutlinedTextField(sample, { sample = it.take(1000) }, Modifier.fillMaxWidth(),
                    minLines = 2, maxLines = 4, label = { Text("Frase de teste") },
                    supportingText = { Text("${PollyCost.countCharacters(sample)} caracteres · limite 1.000") })
                val count = PollyCost.countCharacters(sample)
                Text("Custo bruto provável: ${PollyCost.estimateUsd(count, engine)?.let(PollyCost::show) ?: "indisponível"}. " +
                    "Não inclui créditos, impostos ou tarifas regionais.", style = MaterialTheme.typography.bodySmall)
                Button(onClick = { dialog = "generate" }, enabled = !busy && count > 0 &&
                    count <= 1000 && activeVoice != null, modifier = Modifier.fillMaxWidth()) { Text("Gerar prévia MP3 (confirmar consumo)") }
                if (audioFile?.exists() == true) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { try {
                            player?.release(); player = MediaPlayer().apply {
                                setDataSource(audioFile!!.absolutePath); prepare(); start()
                            }
                        } catch (_: Exception) { feedback = "Não foi possível reproduzir o áudio." } },
                            modifier = Modifier.weight(1f)) { Icon(Icons.Default.PlayArrow, null); Text("Ouvir") }
                        OutlinedButton(onClick = { exportAudio.launch("uai-polly-voz.mp3") },
                            modifier = Modifier.weight(1f)) { Icon(Icons.Default.FileDownload, null); Text("Exportar MP3") }
                    }
                }
            }
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (feedback.isNotEmpty()) Text(feedback, color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall)
            HorizontalDivider()
            Text("Consumo local do Polly (separado do Gemini)", style = MaterialTheme.typography.titleMedium)
            Text("Hoje: ${totals.first} caracteres · Mês UTC: ${totals.second} caracteres",
                fontWeight = FontWeight.Medium)
            Text("${monthly.size} gerações registradas · Bruto estimado: ${PollyCost.show(spent)}" +
                if (unknown > 0) " · $unknown sem contagem da AWS" else "",
                style = MaterialTheme.typography.bodySmall)
            Text("Somente operações deste aparelho; preços públicos de referência (Standard 4, Neural 16, " +
                "Generative 30, Long-form 100 USD/milhão). Não representa a fatura AWS, saldo ou Free Tier.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(dayLimitText, { dayLimitText = it.filter(Char::isDigit).take(9) },
                label = { Text("Limite local diário, caracteres (0 = desligado)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(monthLimitText, { monthLimitText = it.filter(Char::isDigit).take(9) },
                label = { Text("Limite local mensal, caracteres (0 = desligado)") }, modifier = Modifier.fillMaxWidth())
            Button(onClick = { try {
                usage.setLimits(dayLimitText.toInt(), monthLimitText.toInt())
                feedback = "Limites locais salvos; não abrangem console ou outros apps."
            } catch (_: Exception) { feedback = "Limite inválido." } }, modifier = Modifier.fillMaxWidth()) { Text("Salvar limites") }
            OutlinedButton(onClick = { exportUsage.launch("uai-polly-consumo.csv") },
                modifier = Modifier.fillMaxWidth()) { Text("Exportar histórico CSV (sem credenciais)") }
        }
    }
    if (dialog.isNotBlank()) AlertDialog(onDismissRequest = { dialog = "" },
        title = { Text(when (dialog) {
            "generate" -> "Confirmar geração e consumo"
            "export" -> "Exportar credenciais secretas"
            "import" -> "Substituir credenciais"
            "remove" -> "Apagar credenciais Polly"
            "copy" -> "Copiar credenciais"
            else -> "Confirmação"
        }) },
        text = { Text(when (dialog) {
            "generate" -> "Enviar ${PollyCost.countCharacters(sample)} caracteres ao AWS Polly ($engine, ${activeVoice?.id}). " +
                "Custo bruto de referência: ${PollyCost.estimateUsd(PollyCost.countCharacters(sample), engine)?.let(PollyCost::show)}. " +
                "Créditos reais são desconhecidos. Não há repetição automática."
            "export" -> "O arquivo conterá Access Key ID, Secret Access Key e eventual token em texto legível. " +
                "Não o envie ao GitHub, Drive compartilhado ou mensageiros."
            "import" -> "Importar estas credenciais substituirá as atuais, sem alterar o consumo salvo."
            "remove" -> "Remover as credenciais locais sem apagar o histórico de consumo?"
            "copy" -> "Copiar o identificador e a chave secreta para a área de transferência, que não é armazenamento seguro."
            else -> ""
        }) },
        confirmButton = { Button(onClick = {
            val operation = dialog; dialog = ""
            when (operation) {
                "generate" -> {
                    if (!usage.allows(sample)) feedback = "Limite local atingido; ajuste os limites conscientemente."
                    else generate()
                }
                "export" -> exportCreds.launch("uai-polly-CREDENCIAIS-PRIVADAS.json")
                "import" -> try {
                    store.save(incoming ?: error("Importação vazia"))
                    hasSaved = true; access = ""; secret = ""; session = ""; voices = emptyList()
                    feedback = "Credenciais importadas e protegidas no dispositivo."
                } catch (_: Exception) { feedback = "Não foi possível importar." }
                "remove" -> try { store.remove(); hasSaved = false; voices = emptyList();
                    access = ""; secret = ""; session = ""; audioFile = null; feedback = "Credenciais removidas." }
                    catch (_: Exception) { feedback = "Não foi possível remover." }
                "copy" -> try { val c = store.read()
                    putClipboard(if (c != null) "AWS_ACCESS_KEY_ID=${c.accessKeyId}\nAWS_SECRET_ACCESS_KEY=${c.secretAccessKey}" +
                        "\nAWS_REGION=${c.region}\nAWS_SESSION_TOKEN=${c.sessionToken}" else access)
                    } catch (_: Exception) { feedback = "Não foi possível copiar." }
            }
            incoming = null
        }) { Text("Confirmar") } }, dismissButton = { TextButton({ dialog = ""; incoming = null }) { Text("Cancelar") } },
        shape = UaiShapes.dialog)
}
