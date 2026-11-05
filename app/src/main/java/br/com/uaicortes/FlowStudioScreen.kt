package br.com.uaicortes

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

/** Entrada atual: preserva o editor anterior integralmente, acessível em Avançado > Modo manual. */
@Composable
internal fun FlowPromptScreen(project: VideoProject, onSave: (FlowPromptDraft) -> Boolean, back: () -> Unit) {
    var manual by remember(project.id) { mutableStateOf(false) }
    if (manual) FlowLegacyPromptScreen(project, onSave, back = { manual = false })
    else FlowStudioScreen(project, back, onManual = { manual = true })
}

private data class StudioJob(val type: String, val index: Int = -1)

@Composable
private fun FlowStudioScreen(project: VideoProject, back: () -> Unit, onManual: () -> Unit) {
    val context = LocalContext.current
    val storage = remember(project.id) { FlowStudioStorage(context, project.id) }
    val load = remember(project.id) { runCatching { storage.read() } }
    if (load.isFailure) {
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Estúdio de Cenas", style = MaterialTheme.typography.titleLarge)
            Text("O rascunho anterior não pôde ser lido. Nenhum arquivo foi sobrescrito. " +
                "Recupere o armazenamento antes de usar este projeto.", color = MaterialTheme.colorScheme.error)
            Button(onClick = back) { Text("Voltar sem modificar") }
            OutlinedButton(onClick = onManual) { Text("Abrir editor manual existente") }
        }
        return
    }
    val legacy = remember(project.id) { project.flowPromptDraft }
    val initial = remember(project.id) {
        load.getOrNull() ?: FlowStudioDraft(scenes = legacy?.scenes.orEmpty().filter { it.isNotBlank() }
            .mapIndexed { i, text -> StudioScene(title = "Cena ${i + 1}", actions = text) })
    }
    var draft by remember(project.id) { mutableStateOf(initial) }
    var saved by remember(project.id) { mutableStateOf(initial) }
    var status by remember(project.id) { mutableStateOf("") }
    var saveError by remember(project.id) { mutableStateOf(false) }
    var busy by remember(project.id) { mutableStateOf(false) }
    var pending by remember(project.id) { mutableStateOf<StudioJob?>(null) }
    var storySuggestion by remember(project.id) { mutableStateOf<Pair<StudioStory, Pair<List<StudioPerson>, List<StudioPlace>>>?>(null) }
    var sceneSuggestions by remember(project.id) { mutableStateOf<List<StudioScene>?>(null) }
    var singleSuggestion by remember(project.id) { mutableStateOf<Pair<String, StudioScene>?>(null) }
    var deletingScene by remember(project.id) { mutableStateOf<String?>(null) }
    var editingScene by remember(project.id) { mutableStateOf<StudioScene?>(null) }
    var imageDialog by remember(project.id) { mutableIntStateOf(-1) }
    var models by remember(project.id) { mutableStateOf<List<StudioImageModel>>(emptyList()) }
    var modelName by remember(project.id) { mutableStateOf("") }
    var modelBusy by remember(project.id) { mutableStateOf(false) }
    var imageRev by remember(project.id) { mutableIntStateOf(0) }
    var advanced by remember { mutableStateOf(false) }
    var targetImage by remember { mutableStateOf<String?>(null) }
    var targetCopy by remember { mutableStateOf<String?>(null) }
    var targetVideo by remember { mutableStateOf<String?>(null) }
    var confirmReplaceImage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val ai = remember(project.id) { GeminiVideoCaptionClient(context) }
    val imageClient = remember(project.id) { GeminiSceneImageClient(context) }
    DisposableEffect(ai, imageClient) { onDispose { ai.cancel(); imageClient.cancel() } }

    fun saveNow(): Boolean {
        if (saved == draft) return true
        return try { storage.save(draft); saved = draft; saveError = false; true }
        catch (_: Exception) { saveError = true; false }
    }
    fun exit() { if (saveNow()) back() }
    BackHandler { exit() }
    LaunchedEffect(draft) {
        if (draft != saved) {
            delay(700)
            val snapshot = draft
            // Serializado no thread principal com saveNow/BackHandler: evita autosave antigo
            // sobrescrever a versão nova quando o usuário sair durante uma escrita em IO.
            val ok = runCatching { storage.save(snapshot) }.isSuccess
            if (ok) { saved = snapshot; saveError = false } else saveError = true
        }
    }

    fun updateScene(id: String, change: (StudioScene) -> StudioScene) {
        draft = draft.copy(scenes = draft.scenes.map { if (it.id == id) change(it) else it })
    }
    fun copyText(value: String, label: String) {
        (context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager)
            ?.setPrimaryClip(ClipData.newPlainText(label, value))
        status = "$label copiado."
    }
    val importImage = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val id = targetImage
        targetImage = null
        if (uri != null && id != null) scope.launch {
            busy = true
            try {
                val filename = withContext(Dispatchers.IO) { storage.importImage(id, uri) }
                updateScene(id) { it.copy(imageAsset = filename) }
                imageRev++
                status = "Imagem importada. Referências visuais não garantem identidade perfeita."
            } catch (_: Exception) { status = "Falha ao importar. Escolha uma imagem PNG, JPEG ou WebP de até 12 MB." }
            finally { busy = false }
        }
    }
    val saveImage = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/*")) { uri ->
        val name = targetCopy
        targetCopy = null
        if (uri != null && name != null) scope.launch {
            try { withContext(Dispatchers.IO) { storage.copyImage(name, uri) }; status = "Imagem exportada." }
            catch (_: Exception) { status = "Falha ao exportar a imagem." }
        }
    }
    val importVideo = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val id = targetVideo
        targetVideo = null
        if (uri != null && id != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            val name = runCatching {
                context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                    if (it.moveToFirst()) it.getString(0)?.take(200) else null
                }
            }.getOrNull() ?: "Vídeo importado"
            updateScene(id) { it.copy(videoUri = uri.toString(), videoName = name) }
            status = "Vídeo vinculado à cena. Montagem final permanece no editor de vídeo."
        }
    }
    val exportZip = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) {
            val snapshot = draft
            scope.launch {
                busy = true
                try {
                    withContext(Dispatchers.IO) { storage.exportZip(uri, snapshot) }
                    status = "Pacote ZIP exportado pelo seletor de arquivos."
                } catch (_: Exception) { status = "Falha no ZIP; confira espaço e acesso às imagens de todas as cenas." }
                finally { busy = false }
            }
        }
    }
    fun shareImage(scene: StudioScene) {
        val file = storage.asset(scene.imageAsset)
        if (file == null) { status = "Imagem interna não encontrada."; return }
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.diagnostics", file)
            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                type = when (file.extension) { "png" -> "image/png"; "webp" -> "image/webp"; else -> "image/jpeg" }
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = ClipData.newUri(context.contentResolver, "Imagem da cena", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }, "Compartilhar imagem (Google Flow ou outro app)"))
        } catch (_: Exception) { status = "Não foi possível compartilhar esta imagem." }
    }
    fun runJob(job: StudioJob) {
        val snapshot = draft
        val chosenModel = modelName
        pending = null
        busy = true
        status = "Consultando Gemini. Progresso indeterminado; não feche a tela durante esta chamada."
        scope.launch {
            try {
                val key = GeminiCredentialStore(context).read()
                    ?: throw IOException("Cadastre uma chave Gemini nas Configurações.")
                if (job.type == "image") {
                    if (models.none { it.id == chosenModel }) throw IOException("Consulte e selecione um modelo disponível para a chave.")
                    val scene = snapshot.scenes[job.index]
                    val refs = listOfNotNull(storage.asset(scene.imageAsset),
                        storage.asset(snapshot.scenes.getOrNull(job.index - 1)?.imageAsset.orEmpty()))
                        .distinctBy { it.path }.take(2)
                    val bytes = withContext(Dispatchers.IO) {
                        imageClient.createImage(key, chosenModel, FlowStudioAi.image(snapshot, job.index), refs.map { it.readBytes() })
                    }
                    val filename = withContext(Dispatchers.IO) { storage.saveImage(scene.id, bytes) }
                    updateScene(scene.id) { it.copy(imageAsset = filename) }
                    imageRev++
                    status = "Imagem gerada e salva localmente. Confira a fidelidade visual antes de animar."
                } else {
                    val prefs = context.getSharedPreferences("caption_model_preferences", Context.MODE_PRIVATE)
                    val textModel = GeminiCaptionModelPolicy.selectedOrDefault(prefs.getString("model", null))
                    val instruction = when (job.type) {
                        "story" -> FlowStudioAi.story(snapshot.story)
                        "scenes" -> FlowStudioAi.scenes(snapshot)
                        else -> FlowStudioAi.oneScene(snapshot, job.index)
                    }
                    val result = ai.generateStudioJson(key, instruction, textModel, when (job.type) {
                        "story" -> "studio_story"; "scenes" -> "studio_scenes"; else -> "studio_scene"
                    })
                    when (job.type) {
                        "story" -> storySuggestion = FlowStudioAi.parseStory(result, snapshot.story)
                        "scenes" -> sceneSuggestions = FlowStudioAi.parseScenes(result)
                        else -> singleSuggestion = snapshot.scenes[job.index].id to
                            FlowStudioAi.parseSingle(result, snapshot.scenes[job.index])
                    }
                    status = "Resposta recebida. Revise antes de aplicar. " +
                        GeminiUsageLedger(context).lastMessage(when (job.type) {
                            "story" -> "studio_story"; "scenes" -> "studio_scenes"; else -> "studio_scene"
                        })
                }
            } catch (cancel: CancellationException) { throw cancel }
            catch (failure: Exception) {
                status = (failure.message?.take(200) ?: "Requisição falhou.") +
                    " Em falha de rede, verifique o uso antes de repetir: o resultado pode ser incerto."
            } finally { busy = false }
        }
    }

    confirmReplaceImage?.let { id ->
        AlertDialog(onDismissRequest = { confirmReplaceImage = null },
            title = { Text("Substituir imagem da cena?") },
            text = { Text("A imagem interna atual será substituída pela imagem escolhida. " +
                "Os vídeos já produzidos no Flow não serão alterados.") },
            confirmButton = { TextButton(onClick = {
                confirmReplaceImage = null
                targetImage = id
                importImage.launch(arrayOf("image/*"))
            }) { Text("Escolher nova imagem") } },
            dismissButton = { TextButton(onClick = { confirmReplaceImage = null }) { Text("Cancelar") } })
    }
    if (pending != null) {
        val job = pending!!
        val textModel = GeminiCaptionModelPolicy.selectedOrDefault(context.getSharedPreferences(
            "caption_model_preferences", Context.MODE_PRIVATE).getString("model", null))
        AlertDialog(onDismissRequest = { pending = null },
            title = { Text(if (job.type == "image") "Confirmar geração de imagem" else "Enviar texto ao Gemini?") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (job.type == "image") "Será enviada a descrição desta cena e até duas imagens de referência. Nenhuma geração começa sem esta confirmação."
                     else "Serão enviados texto do roteiro, referências e contexto necessários. Nenhuma imagem ou vídeo é enviado nesta operação.")
                Text("Modelo: ${if (job.type == "image") modelName else textModel}")
                Text(if (job.type == "image") StudioImagePricing.note(modelName) else
                    "Consumo de tokens e cobrança dependem do modelo e do projeto. " +
                    "Não é possível prever o custo exato antes de receber os tokens reais.",
                    style = MaterialTheme.typography.bodySmall)
                if (job.type == "image" && draft.scenes.getOrNull(job.index)?.imageAsset?.isNotBlank() == true)
                    Text("Uma nova geração substituirá a imagem atual dessa cena no armazenamento interno.",
                        color = MaterialTheme.colorScheme.error)
                if (job.type == "scenes" && draft.scenes.isNotEmpty())
                    Text("As cenas atuais serão substituídas somente após você revisar e aprovar a nova divisão.",
                        color = MaterialTheme.colorScheme.error)
            } },
            confirmButton = { TextButton(onClick = { runJob(job) }) { Text("Confirmar e enviar") } },
            dismissButton = { TextButton(onClick = { pending = null }) { Text("Cancelar") } })
    }
    storySuggestion?.let { proposed ->
        AlertDialog(onDismissRequest = { storySuggestion = null },
            title = { Text("Revisar história sugerida") },
            text = { Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                Text(proposed.first.fullText())
                Text("${proposed.second.first.size} personagem(ns) e ${proposed.second.second.size} cenário(s).")
            } },
            confirmButton = { TextButton(onClick = {
                draft = draft.copy(story = proposed.first, people = proposed.second.first,
                    places = proposed.second.second, step = 0)
                storySuggestion = null
                status = "Proposta aplicada. Edite e aprove antes de dividir em cenas."
            }) { Text("Usar proposta") } },
            dismissButton = { TextButton(onClick = { storySuggestion = null }) { Text("Descartar") } })
    }
    sceneSuggestions?.let { proposed ->
        AlertDialog(onDismissRequest = { sceneSuggestions = null },
            title = { Text("Revisar divisão em ${proposed.size} cenas") },
            text = { Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                proposed.forEachIndexed { i, scene ->
                    Text("${i + 1}. ${scene.title}", style = MaterialTheme.typography.titleSmall)
                    Text(scene.actions.take(250), style = MaterialTheme.typography.bodySmall)
                }
                Text("A aplicação substituirá a lista atual, sem apagar arquivos de imagens no aparelho.")
            } },
            confirmButton = { TextButton(onClick = {
                draft = draft.copy(scenes = proposed, step = 1)
                sceneSuggestions = null
            }) { Text("Aplicar cenas") } },
            dismissButton = { TextButton(onClick = { sceneSuggestions = null }) { Text("Manter atuais") } })
    }
    singleSuggestion?.let { proposed ->
        AlertDialog(onDismissRequest = { singleSuggestion = null },
            title = { Text("Revisar roteiro da cena") },
            text = { Column(Modifier.heightIn(max = 370.dp).verticalScroll(rememberScrollState())) {
                Text("${proposed.second.title}\n${proposed.second.actions}\n\nPrompt:\n${proposed.second.prompt}")
            } },
            confirmButton = { TextButton(onClick = {
                updateScene(proposed.first) { current -> proposed.second.copy(id = current.id,
                    imageAsset = current.imageAsset, videoUri = current.videoUri, videoName = current.videoName) }
                singleSuggestion = null
            }) { Text("Aplicar roteiro") } },
            dismissButton = { TextButton(onClick = { singleSuggestion = null }) { Text("Descartar") } })
    }
    deletingScene?.let { id ->
        AlertDialog(onDismissRequest = { deletingScene = null }, title = { Text("Excluir cena?") },
            text = { Text("Remove esta cena do roteiro e da exportação. Não apaga vídeos originais nem arquivos da galeria.") },
            confirmButton = { TextButton(onClick = {
                draft = draft.copy(scenes = draft.scenes.filterNot { it.id == id })
                deletingScene = null
            }) { Text("Excluir") } },
            dismissButton = { TextButton(onClick = { deletingScene = null }) { Text("Cancelar") } })
    }
    editingScene?.let { scene ->
        StudioSceneEditor(scene, onSave = { modified ->
            updateScene(scene.id) { modified }
            editingScene = null
        }, onDismiss = { editingScene = null })
    }
    if (imageDialog >= 0 && imageDialog < draft.scenes.size) {
        AlertDialog(onDismissRequest = { imageDialog = -1 },
            title = { Text("Modelo para imagem vertical") },
            text = { Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("A consulta identifica modelos listados para sua chave; só a geração confirma acesso efetivo.",
                    style = MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick = {
                    modelBusy = true
                    scope.launch {
                        try {
                            val key = GeminiCredentialStore(context).read()
                                ?: throw IOException("Cadastre uma chave Gemini nas Configurações.")
                            models = imageClient.listImageModels(key)
                            if (models.none { it.id == modelName }) modelName = models.first().id
                            status = "Modelos de imagem consultados para a chave ativa."
                        } catch (failure: Exception) {
                            models = emptyList(); modelName = ""
                            status = failure.message?.take(160) ?: "A consulta de modelos falhou."
                        } finally { modelBusy = false }
                    }
                }, enabled = !modelBusy && !busy) { Text(if (modelBusy) "Consultando..." else "Consultar modelos disponíveis") }
                models.forEach { m ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = modelName == m.id, onClick = { modelName = m.id })
                        Column { Text(m.label, style = MaterialTheme.typography.bodySmall); Text(m.id,
                            style = MaterialTheme.typography.labelSmall) }
                    }
                }
                if (modelName.isNotBlank()) Text(StudioImagePricing.note(modelName),
                    style = MaterialTheme.typography.bodySmall)
                Text("Proporção solicitada: 9:16. Imagem de referência pode conter dados pessoais; verifique antes de enviar.",
                    style = MaterialTheme.typography.labelSmall)
            } },
            confirmButton = { TextButton(onClick = {
                val i = imageDialog; imageDialog = -1; pending = StudioJob("image", i)
            }, enabled = models.any { it.id == modelName } && !modelBusy) { Text("Continuar") } },
            dismissButton = { TextButton(onClick = { imageDialog = -1 }) { Text("Cancelar") } })
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { exit() }) { Icon(Icons.Default.ArrowBack, contentDescription = "Voltar") }
            Column(Modifier.weight(1f)) {
                Text("Estúdio de Cenas", style = MaterialTheme.typography.titleLarge)
                Text(project.title, style = MaterialTheme.typography.labelMedium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = { if (saveNow()) status = "Rascunho salvo localmente." }, enabled = !busy) {
                Icon(Icons.Default.Save, contentDescription = "Salvar rascunho")
            }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("1 · História", "2 · Cenas", "3 · Exportar para Flow").forEachIndexed { index, label ->
                FilterChip(selected = draft.step == index, onClick = { draft = draft.copy(step = index) },
                    label = { Text(label) })
            }
        }
        if (busy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when (draft.step) {
                0 -> {
                    StudioStoryStep(draft.story, onUpdate = { s -> draft = draft.copy(story = s) },
                        onGenerate = { pending = StudioJob("story") },
                        onContinue = { draft = draft.copy(story = draft.story.copy(approved = true), step = 1) },
                        disabled = busy)
                    StudioReferenceEditor(draft, onUpdate = { draft = it })
                }
                1 -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${draft.scenes.size} cena(s)", style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f))
                        TextButton(onClick = { draft = draft.copy(step = 2) }) { Text("Exportar") }
                    }
                    Text("Continuidade é aproximada: revise o último quadro, personagens e figurinos. " +
                        "Importe um quadro final do Flow como imagem inicial da próxima cena.",
                        style = MaterialTheme.typography.bodySmall)
                    Button(onClick = { pending = StudioJob("scenes") },
                        enabled = draft.story.approved && !busy && draft.story.ready(), modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.AutoAwesome, null); Text(" Dividir história em cenas")
                    }
                    if (!draft.story.approved) Text("Aprove a história na etapa 1 antes de usar a divisão com IA.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    StudioReferenceEditor(draft, onUpdate = { draft = it })
                    draft.scenes.forEachIndexed { i, scene ->
                        StudioSceneCard(scene, i, draft.scenes.size, storage, imageRev,
                            busy = busy, onEdit = { editingScene = scene },
                            onDuplicate = {
                                if (draft.scenes.size < 60) draft = draft.copy(scenes = draft.scenes.toMutableList().also {
                                    it.add(i + 1, scene.copy(id = java.util.UUID.randomUUID().toString(),
                                        imageAsset = "", videoUri = "", videoName = "", title = scene.title + " (cópia)"))
                                })
                            }, onDelete = { deletingScene = scene.id },
                            onUp = { draft = draft.copy(scenes = swapScenes(draft.scenes, i, i - 1)) },
                            onDown = { draft = draft.copy(scenes = swapScenes(draft.scenes, i, i + 1)) },
                            onRegenerate = { pending = StudioJob("scene", i) },
                            onImageGenerate = { imageDialog = i },
                            onImageImport = {
                                if (scene.imageAsset.isNotBlank()) confirmReplaceImage = scene.id
                                else { targetImage = scene.id; importImage.launch(arrayOf("image/*")) }
                            },
                            onImageExport = {
                                if (scene.imageAsset.isNotBlank()) {
                                    targetCopy = scene.imageAsset
                                    saveImage.launch("cena-${i + 1}.${scene.imageAsset.substringAfterLast('.')}")
                                }
                            }, onShare = { shareImage(scene) },
                            onCopy = { copyText(scene.prompt, "Prompt da cena ${i + 1}") })
                    }
                    OutlinedButton(onClick = {
                        if (draft.scenes.size < 60) draft = draft.copy(scenes = draft.scenes + StudioScene(title = "Cena ${draft.scenes.size + 1}"))
                    }, enabled = !busy && draft.scenes.size < 60, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Add, null); Text(" Adicionar cena manualmente")
                    }
                }
                else -> {
                    Text("Preparar para o Google Flow", style = MaterialTheme.typography.titleMedium)
                    Text("Configure 9:16 e a duração no Flow, separadamente. Os prompts não contêm esses parâmetros. " +
                        "O envio de imagens e prompts é manual; não existe integração de projeto editável com o Flow neste aplicativo.",
                        style = MaterialTheme.typography.bodySmall)
                    draft.scenes.forEachIndexed { i, scene ->
                        OutlinedCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("${i + 1}. ${scene.title} · ${scene.status()}", style = MaterialTheme.typography.titleSmall)
                                StudioSceneThumbnail(scene, storage, imageRev)
                                Text(scene.prompt.ifBlank { "Prompt ainda não preparado." },
                                    maxLines = 4, style = MaterialTheme.typography.bodySmall,
                                    overflow = TextOverflow.Ellipsis)
                                Row(Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    TextButton(onClick = { copyText(scene.prompt, "Prompt ${i + 1}") },
                                        enabled = scene.prompt.isNotBlank()) { Icon(Icons.Default.ContentCopy, null); Text(" Copiar prompt") }
                                    TextButton(onClick = { targetCopy = scene.imageAsset; saveImage.launch("cena-${i + 1}.${scene.imageAsset.substringAfterLast('.')}") },
                                        enabled = scene.imageAsset.isNotBlank()) { Text("Salvar imagem") }
                                    TextButton(onClick = { shareImage(scene) }, enabled = scene.imageAsset.isNotBlank()) { Text("Compartilhar") }
                                    TextButton(onClick = { targetVideo = scene.id; importVideo.launch(arrayOf("video/*")) }) {
                                        Icon(Icons.Default.VideoFile, null); Text(" Importar vídeo")
                                    }
                                }
                                if (scene.videoName.isNotBlank()) Text("Vídeo: ${scene.videoName}",
                                    style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                    OutlinedButton(onClick = { copyText(draft.story.fullText() + "\n" +
                        draft.scenes.mapIndexed { i, s -> "Cena ${i + 1}: ${s.title}\n${s.actions}\n${s.dialogue}" }.joinToString("\n\n"),
                        "Roteiro completo") }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.ContentCopy, null); Text(" Copiar roteiro inteiro")
                    }
                    Button(onClick = { if (saveNow()) exportZip.launch("UaiCortes-Flow-${project.id.take(8)}.zip") },
                        enabled = !busy && draft.scenes.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Inventory2, null); Text(" Exportar projeto (ZIP)")
                    }
                }
            }
            HorizontalDivider()
            OutlinedButton(onClick = { advanced = !advanced }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Tune, null); Text(" Avançado e modo manual", Modifier.weight(1f))
                Icon(if (advanced) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
            }
            if (advanced) {
                Text("O modo manual mantém Texto → vídeo, Vídeo de referência, estilo-base, " +
                    "constantes, edição de cenas, aprimoramento e refinamento em lote Gemini.",
                    style = MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick = { if (saveNow()) onManual() }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Edit, null); Text(" Abrir editor manual de prompts")
                }
            }
            if (status.isNotBlank()) Text(status, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (saveError) Text("Falha ao salvar rascunho; não saia antes de corrigir o armazenamento.",
                color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(8.dp))
        }
    }
}

private fun swapScenes(list: List<StudioScene>, i: Int, j: Int): List<StudioScene> =
    list.toMutableList().also { if (i in it.indices && j in it.indices) {
        val swap = it[i]; it[i] = it[j]; it[j] = swap
    } }

@Composable
private fun StudioField(label: String, value: String, limit: Int = 5000,
    minLines: Int = 1, onChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = { onChange(it.take(limit)) },
        label = { Text(label) }, modifier = Modifier.fillMaxWidth(),
        minLines = minLines, maxLines = if (minLines == 1) 3 else 7)
}

@Composable
private fun StudioStoryStep(story: StudioStory, onUpdate: (StudioStory) -> Unit,
    onGenerate: () -> Unit, onContinue: () -> Unit, disabled: Boolean) {
    var scriptOpen by remember { mutableStateOf(story.originalScript.isNotBlank()) }
    fun change(next: StudioStory) { onUpdate(next.copy(approved = false)) }
    Text("História", style = MaterialTheme.typography.titleMedium)
    Text("Descreva uma ideia em poucas palavras ou cole um roteiro. " +
        "A geração só começa após confirmar o uso do Gemini.",
        style = MaterialTheme.typography.bodySmall)
    StudioField("Qual é a ideia do vídeo?", story.idea, 5000, 2) { change(story.copy(idea = it)) }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = onGenerate, enabled = !disabled && (story.idea.isNotBlank() || story.originalScript.isNotBlank()),
            modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.AutoAwesome, null); Text(" Gerar história com IA")
        }
        OutlinedButton(onClick = {
            val ideas = listOf(
                "Um entregador robô encontra uma porteira que reconhece somente galinhas.",
                "Um peixe tenta ensinar um pescador a pescar, mas ambos se confundem.",
                "O elevador de um prédio começa a atender andares de outros planetas.",
                "Uma avó vence um campeonato de corrida usando um carrinho de supermercado.",
                "Um astronauta descobre que a Lua tem um cachorro como zelador.",
                "Um espantalho decide pedir férias e as aves organizam uma festa.",
                "Uma bicicleta inteligente começa a dar conselhos muito ruins ao ciclista.",
                "Uma tempestade faz os guarda-chuvas fugirem das pessoas na rua."
            )
            change(story.copy(idea = ideas.random()))
        }) { Icon(Icons.Default.Casino, null); Text(" Surpreenda-me") }
    }
    OutlinedButton(onClick = { scriptOpen = !scriptOpen }, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Default.Description, null); Text(" Colar roteiro existente", Modifier.weight(1f))
        Icon(if (scriptOpen) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
    }
    if (scriptOpen) StudioField("Roteiro original (sem enviar automaticamente)", story.originalScript, 25000, 4) {
        change(story.copy(originalScript = it))
    }
    Text("Duração desejada", style = MaterialTheme.typography.titleSmall)
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(15 to "15s", 30 to "30s", 60 to "1min", 75 to "1min15s", 120 to "2min", 180 to "3min")
            .forEach { (time, label) -> FilterChip(selected = story.durationSeconds == time,
                onClick = { change(story.copy(durationSeconds = time)) }, label = { Text(label) }) }
    }
    var customDuration by remember(story.durationSeconds) { mutableStateOf(story.durationSeconds.toString()) }
    OutlinedTextField(value = customDuration, onValueChange = { raw ->
        customDuration = raw.filter(Char::isDigit).take(3)
        customDuration.toIntOrNull()?.takeIf { it in 5..900 }?.let { change(story.copy(durationSeconds = it)) }
    }, label = { Text("Personalizada (segundos, 5–900)") }, singleLine = true,
        modifier = Modifier.fillMaxWidth())
    Text("Estilo", style = MaterialTheme.typography.titleSmall)
    val styles = listOf("Realista", "Animação 3D", "Animação 2D")
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        styles.forEach { item -> FilterChip(selected = story.style == item,
            onClick = { change(story.copy(style = item)) }, label = { Text(item) }) }
        FilterChip(selected = story.style !in styles, onClick = { change(story.copy(style = "Personalizado")) },
            label = { Text("Personalizado") })
    }
    if (story.style !in styles) StudioField("Descreva o estilo personalizado", story.style, 120) {
        change(story.copy(style = it))
    }
    Text("Gênero", style = MaterialTheme.typography.titleSmall)
    val genres = listOf("Humor", "Comédia absurda", "Terror cômico", "Aventura")
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        genres.forEach { item -> FilterChip(selected = story.genre == item,
            onClick = { change(story.copy(genre = item)) }, label = { Text(item) }) }
        FilterChip(selected = story.genre !in genres, onClick = { change(story.copy(genre = "Personalizado")) },
            label = { Text("Personalizado") })
    }
    if (story.genre !in genres) StudioField("Descreva o gênero personalizado", story.genre, 120) {
        change(story.copy(genre = it))
    }
    HorizontalDivider()
    Text("Revisar a história", style = MaterialTheme.typography.titleMedium)
    StudioField("Título", story.title, 250) { change(story.copy(title = it)) }
    StudioField("Sinopse", story.synopsis, 6000, 2) { change(story.copy(synopsis = it)) }
    StudioField("Começo", story.beginning, 6000, 2) { change(story.copy(beginning = it)) }
    StudioField("Desenvolvimento", story.development, 10000, 2) { change(story.copy(development = it)) }
    StudioField("Reviravolta", story.twist, 6000, 2) { change(story.copy(twist = it)) }
    StudioField("Final", story.ending, 6000, 2) { change(story.copy(ending = it)) }
    Button(onClick = onContinue, enabled = !disabled && story.ready(), modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Default.Check, null)
        Text(if (story.approved) " Continuar para cenas" else " Aprovar história e continuar")
    }
}

@Composable
private fun StudioSceneThumbnail(scene: StudioScene, storage: FlowStudioStorage, revision: Int) {
    val asset = storage.asset(scene.imageAsset)
    val bitmap = remember(asset?.path, revision) {
        asset?.let { file -> runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            val options = BitmapFactory.Options().apply {
                inSampleSize = if (bounds.outWidth > 1600) 4 else if (bounds.outWidth > 800) 2 else 1
            }
            BitmapFactory.decodeFile(file.absolutePath, options)?.asImageBitmap()
        }.getOrNull() }
    }
    if (bitmap == null) Text("Sem imagem inicial", style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
    else Image(bitmap = bitmap, contentDescription = "Imagem inicial da cena", modifier = Modifier
        .width(86.dp).height(145.dp), contentScale = ContentScale.Fit)
}

@Composable
private fun StudioSceneCard(scene: StudioScene, index: Int, count: Int, storage: FlowStudioStorage,
    revision: Int, busy: Boolean, onEdit: () -> Unit, onDuplicate: () -> Unit, onDelete: () -> Unit,
    onUp: () -> Unit, onDown: () -> Unit, onRegenerate: () -> Unit,
    onImageGenerate: () -> Unit, onImageImport: () -> Unit, onImageExport: () -> Unit,
    onShare: () -> Unit, onCopy: () -> Unit) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${index + 1}. ${scene.title}", modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall)
                IconButton(onClick = onUp, enabled = index > 0 && !busy) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Subir cena")
                }
                IconButton(onClick = onDown, enabled = index < count - 1 && !busy) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Descer cena")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                StudioSceneThumbnail(scene, storage, revision)
                Column(Modifier.weight(1f)) {
                    Text(scene.visual.ifBlank { scene.actions.ifBlank { "Cena não descrita." } },
                        maxLines = 3, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                    Text(scene.status(), style = MaterialTheme.typography.labelSmall)
                    Text("Abertura: ${scene.initialState.ifBlank { "Pendente" }}",
                        maxLines = 2, style = MaterialTheme.typography.labelSmall)
                    Text("Fechamento: ${scene.finalState.ifBlank { "Pendente" }}",
                        maxLines = 2, style = MaterialTheme.typography.labelSmall)
                }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onEdit, enabled = !busy) { Icon(Icons.Default.Edit, null); Text(" Editar") }
                TextButton(onClick = onRegenerate, enabled = !busy) { Icon(Icons.Default.AutoAwesome, null); Text(" Roteiro com IA") }
                TextButton(onClick = onDuplicate, enabled = !busy && count < 60) { Icon(Icons.Default.ContentCopy, null); Text(" Duplicar") }
                TextButton(onClick = onDelete, enabled = !busy) { Icon(Icons.Default.DeleteOutline, null); Text(" Excluir") }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onImageGenerate, enabled = !busy) { Icon(Icons.Default.Image, null)
                    Text(if (scene.imageAsset.isBlank()) " Gerar imagem com IA" else " Regenerar imagem") }
                TextButton(onClick = onImageImport, enabled = !busy) { Icon(Icons.Default.AddPhotoAlternate, null); Text(" Importar quadro") }
                TextButton(onClick = onImageExport, enabled = scene.imageAsset.isNotBlank()) { Text("Salvar imagem") }
                TextButton(onClick = onShare, enabled = scene.imageAsset.isNotBlank()) { Text("Compartilhar") }
            }
            OutlinedButton(onClick = onCopy, enabled = scene.prompt.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.ContentCopy, null); Text(" Copiar prompt")
            }
        }
    }
}

@Composable
private fun StudioSceneEditor(original: StudioScene, onSave: (StudioScene) -> Unit, onDismiss: () -> Unit) {
    var value by remember(original.id) { mutableStateOf(original) }
    var tab by remember(original.id) { mutableIntStateOf(0) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Editar ${original.title}") },
        text = { Column(Modifier.heightIn(max = 480.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Visual", "Som e câmera", "Continuidade", "Prompt").forEachIndexed { i, label ->
                    FilterChip(selected = tab == i, onClick = { tab = i }, label = { Text(label) })
                }
            }
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                when (tab) {
                    0 -> {
                        StudioField("Título", value.title, 200) { value = value.copy(title = it) }
                        StudioField("Descrição visual", value.visual, 4000, 2) { value = value.copy(visual = it) }
                        StudioField("Personagens participantes", value.characters, 2000) { value = value.copy(characters = it) }
                        StudioField("Ações e movimentos na ordem", value.actions, 6000, 2) { value = value.copy(actions = it) }
                    }
                    1 -> {
                        StudioField("Falas exatas (pt-BR)", value.dialogue, 4000, 2) { value = value.copy(dialogue = it) }
                        StudioField("Efeitos sonoros", value.effects, 3000, 2) { value = value.copy(effects = it) }
                        StudioField("Movimentos de câmera", value.camera, 3000, 2) { value = value.copy(camera = it) }
                    }
                    2 -> {
                        StudioField("Estado inicial", value.initialState, 4000, 2) { value = value.copy(initialState = it) }
                        StudioField("Estado final", value.finalState, 4000, 2) { value = value.copy(finalState = it) }
                        Text("Verifique a ligação entre os estados de cenas adjacentes antes de gerar no Flow.",
                            style = MaterialTheme.typography.bodySmall)
                    }
                    3 -> {
                        StudioField("Prompt individual em inglês", value.prompt, 10000, 4) { value = value.copy(prompt = it) }
                        Text("Falas e texto na cena devem permanecer em pt-BR. Não inclua duração nem proporção no prompt.",
                            style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        } },
        confirmButton = { TextButton(onClick = { onSave(value) }) { Text("Salvar cena") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } })
}

@Composable
private fun StudioReferenceEditor(draft: FlowStudioDraft, onUpdate: (FlowStudioDraft) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var editPerson by remember { mutableStateOf<Pair<Int, StudioPerson>?>(null) }
    var editPlace by remember { mutableStateOf<Pair<Int, StudioPlace>?>(null) }
    var deletePerson by remember { mutableIntStateOf(-1) }
    var deletePlace by remember { mutableIntStateOf(-1) }
    OutlinedButton(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Default.Groups, null)
        Text(" Referências (${draft.people.size} personagens · ${draft.places.size} cenários)",
            modifier = Modifier.weight(1f))
        Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
    }
    if (expanded) {
        Text("Aparência, roupas, voz, personalidade, objetos e cenários são reutilizados nos pedidos " +
            "de IA. A imagem inicial real sempre prevalece.", style = MaterialTheme.typography.bodySmall)
        draft.people.forEachIndexed { i, p ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(p.name.ifBlank { "Personagem ${i + 1}" }, modifier = Modifier.weight(1f))
                IconButton(onClick = { editPerson = i to p }) { Icon(Icons.Default.Edit, "Editar personagem") }
                IconButton(onClick = { deletePerson = i }) { Icon(Icons.Default.DeleteOutline, "Excluir personagem") }
            }
        }
        OutlinedButton(onClick = { editPerson = -1 to StudioPerson() }, enabled = draft.people.size < 30) {
            Text("Adicionar personagem")
        }
        draft.places.forEachIndexed { i, place ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(place.name.ifBlank { "Cenário ${i + 1}" }, modifier = Modifier.weight(1f))
                IconButton(onClick = { editPlace = i to place }) { Icon(Icons.Default.Edit, "Editar cenário") }
                IconButton(onClick = { deletePlace = i }) { Icon(Icons.Default.DeleteOutline, "Excluir cenário") }
            }
        }
        OutlinedButton(onClick = { editPlace = -1 to StudioPlace() }, enabled = draft.places.size < 30) {
            Text("Adicionar cenário")
        }
    }
    editPerson?.let { (index, original) ->
        var value by remember(index, original) { mutableStateOf(original) }
        AlertDialog(onDismissRequest = { editPerson = null }, title = { Text("Ficha de personagem") },
            text = { Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                StudioField("Nome", value.name, 120) { value = value.copy(name = it) }
                StudioField("Aparência física", value.appearance, 1500) { value = value.copy(appearance = it) }
                StudioField("Roupas", value.clothes, 1000) { value = value.copy(clothes = it) }
                StudioField("Voz", value.voice, 500) { value = value.copy(voice = it) }
                StudioField("Personalidade", value.personality, 800) { value = value.copy(personality = it) }
                StudioField("Objetos importantes", value.objects, 800) { value = value.copy(objects = it) }
            } }, confirmButton = { TextButton(onClick = {
                onUpdate(draft.copy(people = if (index == -1) draft.people + value
                    else draft.people.mapIndexed { i, p -> if (index == i) value else p }))
                editPerson = null
            }) { Text("Salvar referência") } },
            dismissButton = { TextButton(onClick = { editPerson = null }) { Text("Cancelar") } })
    }
    editPlace?.let { (index, original) ->
        var value by remember(index, original) { mutableStateOf(original) }
        AlertDialog(onDismissRequest = { editPlace = null }, title = { Text("Ficha de cenário") },
            text = { Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                StudioField("Nome do lugar", value.name, 120) { value = value.copy(name = it) }
                StudioField("Aparência e detalhes", value.details, 1800, 2) { value = value.copy(details = it) }
                StudioField("Objetos do ambiente", value.objects, 900) { value = value.copy(objects = it) }
            } }, confirmButton = { TextButton(onClick = {
                onUpdate(draft.copy(places = if (index == -1) draft.places + value
                    else draft.places.mapIndexed { i, p -> if (index == i) value else p }))
                editPlace = null
            }) { Text("Salvar cenário") } },
            dismissButton = { TextButton(onClick = { editPlace = null }) { Text("Cancelar") } })
    }
    if (deletePerson >= 0 || deletePlace >= 0) AlertDialog(
        onDismissRequest = { deletePerson = -1; deletePlace = -1 },
        title = { Text("Excluir referência?") },
        text = { Text("A ficha deixará de ser incluída nas próximas gerações. Não altera imagens existentes.") },
        confirmButton = { TextButton(onClick = {
            if (deletePerson >= 0) onUpdate(draft.copy(people = draft.people.filterIndexed { i, _ -> i != deletePerson }))
            else onUpdate(draft.copy(places = draft.places.filterIndexed { i, _ -> i != deletePlace }))
            deletePerson = -1; deletePlace = -1
        }) { Text("Excluir") } },
        dismissButton = { TextButton(onClick = { deletePerson = -1; deletePlace = -1 }) { Text("Cancelar") } })
}
