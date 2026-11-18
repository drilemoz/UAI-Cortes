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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun PollySettingsScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val store = remember(ctx) { PollyCredentialStore(ctx) }
    val usage = remember(ctx) { PollyUsageStore(ctx) }
    val scope = rememberCoroutineScope()
    var hasSaved by remember { mutableStateOf(store.hasCredentials()) }
    var editing by remember { mutableStateOf(!hasSaved) }
    var savedRegion by remember { mutableStateOf("") }
    var access by remember { mutableStateOf("") }
    var secret by remember { mutableStateOf("") }
    var region by remember { mutableStateOf("us-east-1") }
    var session by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    var temporary by remember { mutableStateOf(false) }
    var management by remember { mutableStateOf(false) }
    var voiceOptions by remember { mutableStateOf(false) }
    var usageOptions by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf<String?>(null) }
    var feedback by remember { mutableStateOf("") }
    var testFeedback by remember { mutableStateOf("") }
    var testError by remember { mutableStateOf(false) }
    var dialog by remember { mutableStateOf("") }
    var incoming by remember { mutableStateOf<PollyCredentials?>(null) }
    var voices by remember { mutableStateOf<List<PollyVoice>>(emptyList()) }
    var voiceId by remember { mutableStateOf(PollySetupPolicy.fallbackVoice.id) }
    var engine by remember { mutableStateOf("standard") }
    var selectedVoiceMenu by remember { mutableStateOf(false) }
    var selectedEngineMenu by remember { mutableStateOf(false) }
    var sample by remember { mutableStateOf(PollySetupPolicy.SAMPLE) }
    var audioFile by remember { mutableStateOf<File?>(null) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    val initialLimits = remember(usage) { usage.limits() }
    var dayLimitText by remember { mutableStateOf(initialLimits.first.toString()) }
    var monthLimitText by remember { mutableStateOf(initialLimits.second.toString()) }
    var revision by remember { mutableIntStateOf(0) }

    val activeVoice = voices.firstOrNull { it.id == voiceId } ?: PollySetupPolicy.fallbackVoice
    val source = PollySetupPolicy.source(editing, hasSaved, access, secret)
    val canTest = source != PollySetupPolicy.Source.NONE && busy == null
    val count = PollyCost.countCharacters(sample)
    val current = remember(revision) { usage.events() }
    val totals = remember(revision) { usage.sums() }
    val monthStart = LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1)
        .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val monthly = current.filter { it.timestamp >= monthStart }
    val spent = monthly.sumOf { it.estimatedUsd ?: 0.0 }

    LaunchedEffect(store, hasSaved) {
        savedRegion = if (!hasSaved) "" else withContext(Dispatchers.IO) {
            runCatching { store.read()?.region }.getOrNull().orEmpty()
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            player?.let { runCatching { it.release() } }
            player = null
            audioFile?.delete()
            audioFile = null
        }
    }
    fun clearPreview() {
        player?.let { runCatching { it.release() } }
        player = null
        audioFile?.delete()
        audioFile = null
    }
    fun resetTest() {
        clearPreview()
        testFeedback = ""
        testError = false
        voices = emptyList()
        voiceId = PollySetupPolicy.fallbackVoice.id
        engine = "standard"
    }
    fun closeEditor() {
        access = ""; secret = ""; session = ""; visible = false; temporary = false
        editing = false
    }
    fun showTest(message: String, error: Boolean = false) {
        testFeedback = message
        testError = error
    }
    fun play(file: File) {
        player?.let { runCatching { it.release() } }
        player = null
        val next = MediaPlayer()
        try {
            player = next
            next.setDataSource(file.absolutePath)
            next.setOnPreparedListener { if (player === it) it.start() }
            next.setOnErrorListener { failed, _, _ ->
                if (player === failed) {
                    player = null
                    runCatching { failed.release() }
                    showTest("Áudio recebido, mas não foi possível reproduzi-lo neste aparelho.", true)
                }
                true
            }
            next.prepareAsync()
        } catch (_: Exception) {
            player = null
            runCatching { next.release() }
            showTest("Áudio recebido, mas não foi possível reproduzi-lo neste aparelho.", true)
        }
    }
    fun clipboardManager() = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    fun clipboardValue(): String =
        clipboardManager().primaryClip?.getItemAt(0)?.coerceToText(ctx)?.toString().orEmpty()
    fun putClipboard(c: PollyCredentials) {
        val value = "AWS_ACCESS_KEY_ID=${c.accessKeyId}\nAWS_SECRET_ACCESS_KEY=${c.secretAccessKey}" +
            "\nAWS_REGION=${c.region}\nAWS_SESSION_TOKEN=${c.sessionToken}"
        val clip = ClipData.newPlainText("Credencial AWS sensível", value)
        if (Build.VERSION.SDK_INT >= 33) clip.description.extras = android.os.PersistableBundle().apply {
            putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
        }
        clipboardManager().setPrimaryClip(clip)
        feedback = "Credenciais copiadas. Limpe a área de transferência depois de usar."
    }
    fun draft() = PollyCredentials(access, secret, region, session).validated()
    suspend fun credentialsForTest(which: PollySetupPolicy.Source): PollyCredentials =
        when (which) {
            PollySetupPolicy.Source.FORM -> draft()
            PollySetupPolicy.Source.SAVED -> withContext(Dispatchers.IO) {
                store.read() ?: error("As credenciais salvas não estão disponíveis.")
            }
            else -> error("Preencha o ID e a chave de acesso secreta.")
        }
    fun message(error: Exception): String = when (error) {
        is IllegalArgumentException, is IllegalStateException -> error.message ?: "Verifique os campos."
        is java.net.SocketTimeoutException -> "A AWS demorou para responder. Verifique a conexão."
        is java.io.IOException -> error.message?.takeIf { "HTTP " in it }
            ?: "Não foi possível falar com a AWS. Verifique a internet e a região."
        else -> "Não foi possível concluir a operação do Polly."
    }

    val exportCreds = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null && busy == null) scope.launch {
            busy = "Exportando credenciais…"
            try {
                withContext(Dispatchers.IO) {
                    val raw = PollyCredentialCodec.encode(store.read() ?: error("Sem credenciais salvas"))
                    ctx.contentResolver.openOutputStream(uri, "wt")?.use { it.write(raw.toByteArray(Charsets.UTF_8)) }
                        ?: error("Destino inacessível")
                }
                feedback = "Arquivo sensível exportado. Não o compartilhe."
            } catch (c: CancellationException) { throw c }
            catch (_: Exception) { feedback = "Não foi possível exportar as credenciais." }
            finally { busy = null }
        }
    }
    val importCreds = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null && busy == null) scope.launch {
            busy = "Lendo credenciais…"
            try {
                incoming = withContext(Dispatchers.IO) {
                    val raw = ctx.contentResolver.openInputStream(uri)?.use { stream ->
                        val buffer = ByteArray(4096)
                        val out = ByteArrayOutputStream()
                        while (true) {
                            val n = stream.read(buffer)
                            if (n < 0) break
                            require(out.size() + n <= 16_384)
                            out.write(buffer, 0, n)
                        }
                        out.toString("UTF-8")
                    } ?: error("Arquivo inacessível")
                    PollyCredentialCodec.decode(raw)
                }
                dialog = "import"
            } catch (c: CancellationException) { throw c }
            catch (_: Exception) { feedback = "Arquivo inválido ou maior que 16 KB." }
            finally { busy = null }
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
                showTest("Áudio MP3 exportado.")
            } catch (c: CancellationException) { throw c }
            catch (_: Exception) { showTest("Não foi possível exportar o áudio.", true) }
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
                feedback = "Histórico exportado sem credenciais nem textos."
            } catch (c: CancellationException) { throw c }
            catch (_: Exception) { feedback = "Não foi possível exportar o histórico." }
        }
    }
    fun save(newCredentials: PollyCredentials) {
        if (busy != null) return
        scope.launch {
            busy = "Salvando credenciais…"
            try {
                withContext(Dispatchers.IO) { store.save(newCredentials) }
                savedRegion = newCredentials.region
                hasSaved = true
                closeEditor()
                feedback = "Credenciais salvas no aparelho."
            } catch (c: CancellationException) { throw c }
            catch (e: Exception) { feedback = message(e) }
            finally { busy = null }
        }
    }
    fun refreshVoices() {
        if (!canTest) return
        val which = source
        busy = "Consultando a AWS…"
        showTest("")
        scope.launch {
            try {
                val result = PollyApi.voices(credentialsForTest(which))
                voices = result
                val selected = result.firstOrNull { it.id == voiceId } ?:
                    result.firstOrNull { "neural" in it.engines } ?: result.firstOrNull()
                voiceId = selected?.id ?: PollySetupPolicy.fallbackVoice.id
                engine = selected?.let(PollySetupPolicy::preferredEngine) ?: "standard"
                showTest(if (result.isEmpty()) "Conexão OK. Nenhuma voz pt-BR foi listada nessa região."
                    else "Conexão OK. ${result.size} vozes pt-BR encontradas. Nenhum áudio gerado.")
            } catch (c: CancellationException) { throw c }
            catch (e: Exception) { showTest(message(e), true) }
            finally { busy = null }
        }
    }
    fun generate() {
        if (!canTest) return
        val which = source
        val voice = activeVoice
        val requestedEngine = engine
        val pending = File(ctx.cacheDir, "polly_preview_${UUID.randomUUID()}.mp3")
        busy = "Gerando a voz de teste…"
        clearPreview()
        showTest("")
        scope.launch {
            try {
                val credentials = credentialsForTest(which)
                if (!withContext(Dispatchers.IO) { usage.allows(sample) }) {
                    showTest("Limite local atingido. Confira os limites em Consumo.", true)
                    return@launch
                }
                // One POST after explicit confirmation. An uncertain response may still be billable.
                val result = PollyApi.synthesize(credentials, sample, voice, requestedEngine)
                val recorded = withContext(Dispatchers.IO) {
                    val saved = runCatching { usage.record(result, credentials.region) }.isSuccess
                    pending.writeBytes(result.audio)
                    saved
                }
                audioFile = pending
                revision++
                showTest(when {
                    !recorded -> "Voz recebida. Não foi possível registrar o consumo local."
                    result.requestCharacters != null -> "Voz recebida. ${result.requestCharacters} caracteres confirmados pela AWS."
                    else -> "Voz recebida. A AWS não informou a contagem de caracteres."
                })
                play(pending)
            } catch (c: CancellationException) { throw c }
            catch (e: Exception) { showTest(message(e) + " Se o pedido já saiu do app, confira o consumo antes de repetir.", true) }
            finally {
                if (audioFile != pending) pending.delete()
                busy = null
            }
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        Heading("Amazon Polly", onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedCard(Modifier.fillMaxWidth(), shape = UaiShapes.card) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Conexão com a AWS", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(if (editing) "Informe as duas chaves do IAM."
                        else "Credenciais salvas${savedRegion.takeIf { it.isNotEmpty() }?.let { " · $it" }.orEmpty()}",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (editing) {
                        OutlinedTextField(access, { access = it.take(128); resetTest() }, Modifier.fillMaxWidth(),
                            label = { Text("ID da chave de acesso") }, singleLine = true, enabled = busy == null,
                            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = { IconButton(onClick = { access = clipboardValue().trim().take(128); resetTest() },
                                enabled = busy == null) { Icon(Icons.Default.ContentPaste, "Colar ID da chave") } })
                        OutlinedTextField(secret, { secret = it.take(256); resetTest() }, Modifier.fillMaxWidth(),
                            label = { Text("Chave de acesso secreta") }, singleLine = true, enabled = busy == null,
                            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                Row {
                                    IconButton(onClick = { secret = clipboardValue().trim().take(256); resetTest() },
                                        enabled = busy == null) { Icon(Icons.Default.ContentPaste, "Colar chave secreta") }
                                    IconButton(onClick = { visible = !visible }, enabled = busy == null) {
                                        Icon(if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            if (visible) "Ocultar chaves" else "Mostrar chaves")
                                    }
                                }
                            })
                        OutlinedTextField(region, { region = it.take(30); resetTest() }, Modifier.fillMaxWidth(),
                            label = { Text("Região AWS") }, singleLine = true, enabled = busy == null,
                            placeholder = { Text("us-east-1") })
                        TextButton(onClick = { temporary = !temporary }, enabled = busy == null,
                            modifier = Modifier.fillMaxWidth()) {
                            Text(if (temporary) "Ocultar token de sessão" else "Token de sessão (opcional)")
                            Icon(if (temporary) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
                        }
                        if (temporary) OutlinedTextField(session, { session = it.take(8192); resetTest() },
                            Modifier.fillMaxWidth(), singleLine = true, enabled = busy == null,
                            label = { Text("Token de sessão") },
                            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation())
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(onClick = { refreshVoices() }, enabled = canTest,
                            modifier = Modifier.weight(1f), shape = UaiShapes.button,
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)) { Text("Testar conexão") }
                        Button(onClick = { dialog = "generate" }, enabled = canTest && sample.isNotBlank(),
                            modifier = Modifier.weight(1f), shape = UaiShapes.button,
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)) { Text("Testar voz") }
                    }
                    Text(when (source) {
                        PollySetupPolicy.Source.FORM -> "Você pode testar antes de salvar. Só o teste de voz gera áudio."
                        PollySetupPolicy.Source.SAVED -> "O teste usa as credenciais salvas. Só o teste de voz gera áudio."
                        else -> "Preencha o ID e a chave secreta para liberar os testes."
                    }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    busy?.let { LinearProgressIndicator(Modifier.fillMaxWidth()); Text(it, style = MaterialTheme.typography.bodySmall) }
                    if (testFeedback.isNotBlank()) {
                        Surface(color = if (testError) MaterialTheme.colorScheme.errorContainer
                            else MaterialTheme.colorScheme.primaryContainer, shape = UaiShapes.item) {
                            Text(testFeedback, Modifier.fillMaxWidth().padding(9.dp),
                                color = if (testError) MaterialTheme.colorScheme.onErrorContainer
                                else MaterialTheme.colorScheme.onPrimaryContainer,
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    audioFile?.let { file ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            TextButton(onClick = { play(file) }, modifier = Modifier.weight(1f), enabled = busy == null) {
                                Icon(Icons.Default.PlayArrow, null, Modifier.size(18.dp)); Text("Ouvir")
                            }
                            TextButton(onClick = { exportAudio.launch("uai-polly-voz.mp3") },
                                modifier = Modifier.weight(1f), enabled = busy == null) { Text("Exportar MP3") }
                        }
                    }
                    if (editing) {
                        Button(onClick = {
                            try { save(draft()) }
                            catch (e: IllegalArgumentException) { feedback = message(e) }
                        }, enabled = canTest, modifier = Modifier.fillMaxWidth(), shape = UaiShapes.button) {
                            Text("Salvar credenciais")
                        }
                        if (hasSaved) TextButton(onClick = { resetTest(); closeEditor() }, enabled = busy == null) {
                            Text("Cancelar alterações")
                        }
                    } else TextButton(onClick = {
                        scope.launch {
                            busy = "Abrindo credenciais…"
                            try {
                                val c = withContext(Dispatchers.IO) { store.read() } ?: error("Sem credenciais salvas.")
                                access = c.accessKeyId; secret = c.secretAccessKey; region = c.region; session = c.sessionToken
                                visible = false; temporary = session.isNotEmpty()
                            } catch (cancelled: CancellationException) { throw cancelled }
                            catch (e: Exception) { feedback = message(e) }
                            finally { editing = true; busy = null }
                        }
                    }, enabled = busy == null) { Text("Editar credenciais") }
                    if (feedback.isNotBlank()) Text(feedback, style = MaterialTheme.typography.bodySmall)
                }
            }

            PollyExpandable("Opções de voz", voiceOptions, { voiceOptions = !voiceOptions }) {
                Text("Voz: ${activeVoice.name} · ${PollySetupPolicy.engineName(engine)}",
                    style = MaterialTheme.typography.bodySmall)
                if (voices.isEmpty()) Text("Use Testar conexão para carregar as outras vozes em português.",
                    style = MaterialTheme.typography.bodySmall)
                if (voices.isNotEmpty()) {
                    Box {
                        OutlinedButton(onClick = { selectedVoiceMenu = true }, modifier = Modifier.fillMaxWidth(),
                            enabled = busy == null) { Text("Voz: ${activeVoice.name}"); Icon(Icons.Default.ArrowDropDown, null) }
                        DropdownMenu(selectedVoiceMenu, { selectedVoiceMenu = false }) {
                            voices.forEach { voice ->
                                DropdownMenuItem(text = { Text(voice.name) }, onClick = {
                                    voiceId = voice.id
                                    engine = PollySetupPolicy.preferredEngine(voice)
                                    selectedVoiceMenu = false
                                    clearPreview()
                                })
                            }
                        }
                    }
                }
                if (activeVoice.engines.size > 1) Box {
                    OutlinedButton(onClick = { selectedEngineMenu = true }, modifier = Modifier.fillMaxWidth(),
                        enabled = busy == null) {
                        Text("Tipo: ${PollySetupPolicy.engineName(engine)}"); Icon(Icons.Default.ArrowDropDown, null)
                    }
                    DropdownMenu(selectedEngineMenu, { selectedEngineMenu = false }) {
                        activeVoice.engines.forEach { type ->
                            DropdownMenuItem(text = { Text(PollySetupPolicy.engineName(type)) }, onClick = {
                                engine = type; selectedEngineMenu = false; clearPreview()
                            })
                        }
                    }
                }
                OutlinedTextField(sample, { sample = it.take(1000); clearPreview() },
                    Modifier.fillMaxWidth(), minLines = 2, maxLines = 4, enabled = busy == null,
                    label = { Text("Frase de teste") }, supportingText = { Text("$count de 1.000 caracteres") })
            }
            PollyExpandable("Gerenciar credenciais", management, { management = !management }) {
                TextButton(onClick = { importCreds.launch(arrayOf("application/json", "text/plain", "*/*")) },
                    enabled = busy == null) { Text("Importar credenciais") }
                if (hasSaved) {
                    TextButton(onClick = { dialog = "copy" }, enabled = busy == null) { Text("Copiar credenciais salvas") }
                    TextButton(onClick = { dialog = "export" }, enabled = busy == null) { Text("Exportar credenciais salvas") }
                    TextButton(onClick = { dialog = "remove" }, enabled = busy == null) { Text("Remover do aparelho") }
                }
                TextButton(onClick = { ctx.startActivity(Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://console.aws.amazon.com/iam/home#/users"))) }) { Text("Abrir o IAM da AWS") }
            }
            PollyExpandable("Consumo", usageOptions, { usageOptions = !usageOptions }) {
                Text("Hoje: ${totals.first} · Mês: ${totals.second} caracteres",
                    style = MaterialTheme.typography.bodyMedium)
                Text("${monthly.size} gerações no mês · Estimativa de ${PollyCost.show(spent)}",
                    style = MaterialTheme.typography.bodySmall)
                Text("Somente este aparelho, em horário UTC. Não representa a fatura nem os créditos da AWS.",
                    style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(dayLimitText, { dayLimitText = it.filter(Char::isDigit).take(9) },
                    label = { Text("Limite diário de caracteres") }, modifier = Modifier.fillMaxWidth(),
                    enabled = busy == null, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                OutlinedTextField(monthLimitText, { monthLimitText = it.filter(Char::isDigit).take(9) },
                    label = { Text("Limite mensal de caracteres") }, modifier = Modifier.fillMaxWidth(),
                    enabled = busy == null, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true,
                    supportingText = { Text("Zero significa sem limite local.") })
                TextButton(onClick = {
                    feedback = try {
                        usage.setLimits(dayLimitText.toInt(), monthLimitText.toInt())
                        "Limites locais salvos."
                    } catch (_: Exception) { "Limite inválido." }
                }, enabled = busy == null) { Text("Salvar limites") }
                TextButton(onClick = { exportUsage.launch("uai-polly-consumo.csv") },
                    enabled = busy == null) { Text("Exportar histórico CSV") }
            }
        }
    }

    if (dialog.isNotBlank()) AlertDialog(onDismissRequest = { dialog = ""; incoming = null },
        title = { Text(when (dialog) {
            "generate" -> "Testar voz do Polly?"
            "export" -> "Exportar credenciais secretas?"
            "import" -> "Importar credenciais?"
            "remove" -> "Remover credenciais locais?"
            else -> "Copiar credenciais salvas?"
        }) },
        text = { Text(when (dialog) {
            "generate" -> "Enviar $count caracteres à AWS com ${activeVoice.name} (${PollySetupPolicy.engineName(engine)}). " +
                "Custo bruto estimado: ${PollyCost.estimateUsd(count, engine)?.let(PollyCost::show) ?: "indisponível"}. " +
                "O valor não desconta créditos. O áudio será reproduzido se a AWS responder."
            "export" -> "O arquivo guarda o ID, a chave secreta e o token em texto legível. Não o compartilhe."
            "import" -> "As credenciais importadas substituem as atuais. O histórico de consumo é mantido."
            "remove" -> "Isso remove apenas as credenciais do aparelho. A chave da AWS e o histórico não são apagados."
            else -> "O ID e a chave secreta ficarão na área de transferência. Limpe-a depois de usar."
        }) },
        confirmButton = { Button(onClick = {
            val operation = dialog
            dialog = ""
            when (operation) {
                "generate" -> generate()
                "export" -> exportCreds.launch("uai-polly-CREDENCIAIS-PRIVADAS.json")
                "import" -> incoming?.let { resetTest(); save(it) }
                "remove", "copy" -> scope.launch {
                    busy = if (operation == "remove") "Removendo credenciais…" else "Lendo credenciais…"
                    try {
                        if (operation == "remove") {
                            withContext(Dispatchers.IO) { store.remove() }
                            closeEditor(); hasSaved = false; editing = true; region = "us-east-1"
                            resetTest()
                            feedback = "Credenciais removidas do aparelho."
                        } else {
                            val c = withContext(Dispatchers.IO) { store.read() }
                            if (c != null) putClipboard(c) else feedback = "Nenhuma credencial salva."
                        }
                    } catch (c: CancellationException) { throw c }
                    catch (e: Exception) { feedback = message(e) }
                    finally { busy = null }
                }
            }
            incoming = null
        }) { Text(if (dialog == "generate") "Gerar e ouvir" else "Confirmar") } },
        dismissButton = { TextButton(onClick = { dialog = ""; incoming = null }) { Text("Cancelar") } },
        shape = UaiShapes.dialog)
}

@Composable
private fun PollyExpandable(title: String, expanded: Boolean, onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth(), shape = UaiShapes.card) {
        TextButton(onClick = onToggle, modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)) {
            Text(title, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium)
            Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
        }
        if (expanded) Column(Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp), content = content)
    }
}
