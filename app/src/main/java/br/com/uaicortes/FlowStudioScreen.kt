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
import kotlinx.coroutines.Job
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
    val referenceLibrary = remember(context) { StudioReferenceLibrary(context) }
    var libraryData by remember(context) { mutableStateOf(runCatching { referenceLibrary.read() }.getOrNull()) }
    var referenceImageRev by remember { mutableIntStateOf(0) }
    var galleryReference by remember { mutableStateOf<Pair<String, String>?>(null) }
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
        load.getOrNull() ?: FlowStudioDraft(story = StudioStory(title = project.title),
            scenes = legacy?.scenes.orEmpty().filter { it.isNotBlank() }
            .mapIndexed { i, text -> StudioScene(title = "Cena ${i + 1}", actions = text) })
    }
    var draft by remember(project.id) { mutableStateOf(initial) }
    var saved by remember(project.id) { mutableStateOf(initial) }
    var status by remember(project.id) { mutableStateOf("") }
    var saveError by remember(project.id) { mutableStateOf(false) }
    var busy by remember(project.id) { mutableStateOf(false) }
    var operation by remember(project.id) { mutableStateOf("") }
    var activeRequest by remember(project.id) { mutableStateOf<Job?>(null) }
    var pending by remember(project.id) { mutableStateOf<StudioJob?>(null) }
    var storySuggestion by remember(project.id) { mutableStateOf<Pair<StudioStory, Pair<List<StudioPerson>, List<StudioPlace>>>?>(null) }
    var sceneSuggestions by remember(project.id) { mutableStateOf<List<StudioScene>?>(null) }
    var singleSuggestion by remember(project.id) { mutableStateOf<Pair<String, StudioScene>?>(null) }
    var personSuggestion by remember(project.id) { mutableStateOf<Pair<String, StudioPerson>?>(null) }
    var placeSuggestion by remember(project.id) { mutableStateOf<Pair<String, StudioPlace>?>(null) }
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
    fun saveLibrary(next: StudioLibraryData) {
        try { referenceLibrary.save(next); libraryData = next; status = "Referência salva na biblioteca para outras histórias." }
        catch (_: Exception) { status = "Não foi possível salvar na biblioteca. Referências existentes preservadas." }
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
            operation = "Importando imagem da galeria"
            try {
                val filename = withContext(Dispatchers.IO) { storage.importImage(id, uri) }
                updateScene(id) { it.copy(imageAsset = filename) }
                imageRev++
                status = "Imagem importada. Referências visuais não garantem identidade perfeita."
            } catch (_: Exception) { status = "Falha ao importar. Escolha uma imagem PNG, JPEG ou WebP de até 12 MB." }
            finally { busy = false; operation = "" }
        }
    }
    val importReference = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val target = galleryReference
        galleryReference = null
        if (uri != null && target != null) scope.launch {
            busy = true; operation = "Importando imagem de referência"
            try {
                val filename = withContext(Dispatchers.IO) { referenceLibrary.importImage(target.second, uri) }
                if (target.first == "person") draft = draft.copy(people = draft.people.map { person ->
                    if (person.id == target.second) person.copy(imageAsset = filename) else person
                })
                else draft = draft.copy(places = draft.places.map { place ->
                    if (place.id == target.second) place.copy(imageAsset = filename) else place
                })
                referenceImageRev++
                status = "Imagem da galeria salva. Ela não será enviada à API sem confirmação."
            } catch (_: Exception) { status = "Falha ao importar referência. Use JPEG, PNG ou WebP até 12 MB." }
            finally { busy = false; operation = "" }
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
                operation = "Exportando pacote ZIP"
                try {
                    withContext(Dispatchers.IO) { storage.exportZip(uri, snapshot) }
                    status = "Pacote ZIP exportado pelo seletor de arquivos."
                } catch (_: Exception) { status = "Falha no ZIP; confira espaço e acesso às imagens de todas as cenas." }
                finally { busy = false; operation = "" }
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
        operation = when (job.type) {
            "story" -> "Gemini: criando título, história e personagens"
            "scenes" -> "Gemini: dividindo história e preparando as cenas"
            "image" -> "Gemini: gerando imagem inicial da cena ${job.index + 1}"
            "person" -> "Gemini: enriquecendo personagem ${job.index + 1}"
            "place" -> "Gemini: enriquecendo cenário ${job.index + 1}"
            else -> "Gemini: preparando roteiro da cena ${job.index + 1}"
        }
        status = "Solicitação iniciada. Aguarde a resposta ou cancele; o tempo depende do serviço."
        activeRequest = scope.launch {
            try {
                val keys = GeminiCredentialStore(context)
                val beforeKeyId = keys.activeId()
                val key = keys.read()
                    ?: throw IOException("Cadastre uma chave Gemini nas Configurações.")
                if (job.type == "image") {
                    if (models.none { it.id == chosenModel }) throw IOException("Consulte e selecione um modelo disponível para a chave.")
                    val scene = snapshot.scenes[job.index]
                    val refNames = scene.characters.split(',', ';').map { it.trim() }
                    val refs = (listOfNotNull(storage.asset(scene.imageAsset),
                        storage.asset(snapshot.scenes.getOrNull(job.index - 1)?.imageAsset.orEmpty())) +
                        snapshot.people.filter { it.name in refNames }.mapNotNull { referenceLibrary.asset(it.imageAsset) } +
                        snapshot.places.mapNotNull { referenceLibrary.asset(it.imageAsset) })
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
                        "person" -> FlowStudioAi.enrichPerson(snapshot.people[job.index], snapshot.story.style)
                        "place" -> FlowStudioAi.enrichPlace(snapshot.places[job.index], snapshot.story.style)
                        else -> FlowStudioAi.oneScene(snapshot, job.index)
                    }
                    val operationId = when (job.type) {
                        "story" -> "studio_story"
                        "scenes" -> "studio_scenes"
                        "person" -> "studio_person"
                        "place" -> "studio_place"
                        else -> "studio_scene"
                    }
                    val result = ai.generateStudioJson(key, instruction, textModel, operationId)
                    when (job.type) {
                        "story" -> storySuggestion = FlowStudioAi.parseStory(result, snapshot.story)
                        "scenes" -> sceneSuggestions = FlowStudioAi.parseScenes(result, snapshot.story.durationSeconds)
                        "person" -> personSuggestion = snapshot.people[job.index].id to
                            FlowStudioAi.parsePerson(result, snapshot.people[job.index])
                        "place" -> placeSuggestion = snapshot.places[job.index].id to
                            FlowStudioAi.parsePlace(result, snapshot.places[job.index])
                        else -> singleSuggestion = snapshot.scenes[job.index].id to
                            FlowStudioAi.parseSingle(result, snapshot.scenes[job.index])
                    }
                    status = "Resposta recebida. Revise antes de aplicar. " +
                        GeminiUsageLedger(context).lastMessage(operationId)
                }
                if (beforeKeyId != keys.activeId()) {
                    val newName = keys.profiles().firstOrNull { it.id == keys.activeId() }?.label
                    status += " Troca automática 429: ${newName ?: "chave de outro projeto"} ativada globalmente."
                }
            } catch (cancel: CancellationException) { throw cancel }
            catch (failure: Exception) {
                val classification = GeminiOperationFailure.label(GeminiOperationFailure.fromThrowable(failure))
                status = "$classification. " + (failure.message?.take(150) ?: "Requisição falhou.") +
                    " Confira o consumo antes de repetir; erros de rede/servidor não acionam nova geração."
            } finally { busy = false; operation = ""; activeRequest = null }
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
                     else if (job.type == "person" || job.type == "place")
                         "O Gemini receberá somente a ficha textual escolhida e o estilo. A resposta aparecerá para revisão antes de salvar; nenhuma imagem será enviada."
                     else "Serão enviados texto do roteiro, referências e contexto necessários. Nenhuma imagem ou vídeo é enviado nesta operação.")
                Text("Modelo: ${if (job.type == "image") modelName else textModel}")
                Text(if (job.type == "image") StudioImagePricing.note(modelName) else
                    "Consumo e cobrança dependem do modelo e projeto. Não é possível prever o custo exato.",
                    style = MaterialTheme.typography.bodySmall)
                val credentials = GeminiCredentialStore(context)
                val active = credentials.profiles().firstOrNull { it.id == credentials.activeId() }
                Text("Chave global: ${active?.label ?: "não cadastrada"}. " +
                    (if (credentials.autoFallbackEnabled())
                        "Troca automática ativada: em HTTP 429, outra chave de projeto distinto poderá receber a mesma solicitação. Cada tentativa pode consumir cota ou gerar cobrança."
                    else "Troca automática desligada. Selecione a chave em Configurações > Gemini."),
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
                val newPeople = proposed.second.first.filterNot { generated -> draft.people.any { it.name == generated.name } }
                val newPlaces = proposed.second.second.filterNot { generated -> draft.places.any { it.name == generated.name } }
                draft = draft.copy(story = proposed.first,
                    people = (draft.people + newPeople).take(30), places = (draft.places + newPlaces).take(30))
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
                draft = draft.copy(scenes = proposed, step = 3)
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
    personSuggestion?.let { (id, proposal) ->
        AlertDialog(onDismissRequest = { personSuggestion = null },
            title = { Text("Sugestão para ${proposal.name}") },
            text = { Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                Text("Aparência: ${proposal.appearance}\nRoupas: ${proposal.clothes}\nVoz: ${proposal.voice}\nPersonalidade: ${proposal.personality}\nObjetos: ${proposal.objects}")
            } }, confirmButton = { TextButton(onClick = {
                if (draft.people.any { it.id == id }) {
                    draft = draft.copy(people = draft.people.map { if (it.id == id) proposal else it })
                } else status = "Personagem mudou durante a solicitação; sugestão não aplicada."
                personSuggestion = null
            }) { Text("Aplicar à ficha") } },
            dismissButton = { TextButton(onClick = { personSuggestion = null }) { Text("Descartar") } })
    }
    placeSuggestion?.let { (id, proposal) ->
        AlertDialog(onDismissRequest = { placeSuggestion = null },
            title = { Text("Sugestão para ${proposal.name}") },
            text = { Text("Ambiente: ${proposal.details}\nObjetos: ${proposal.objects}") },
            confirmButton = { TextButton(onClick = {
                if (draft.places.any { it.id == id }) {
                    draft = draft.copy(places = draft.places.map { if (it.id == id) proposal else it })
                } else status = "Cenário mudou durante a solicitação; sugestão não aplicada."
                placeSuggestion = null
            }) { Text("Aplicar à ficha") } },
            dismissButton = { TextButton(onClick = { placeSuggestion = null }) { Text("Descartar") } })
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
        StudioSceneEditor(scene, draft.people.map { it.name }, onSave = { modified ->
            updateScene(scene.id) { modified }
            editingScene = null
        }, onDismiss = { editingScene = null })
    }
    if (imageDialog >= 0 && imageDialog < draft.scenes.size) {
        AlertDialog(onDismissRequest = { imageDialog = -1 },
            title = { Text("Modelo para imagem vertical") },
            text = { Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("A consulta usa a chave global ativa; após trocá-la em Configurações, consulte novamente. " +
                    "A listagem não garante acesso à geração.", style = MaterialTheme.typography.bodySmall)
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
        if (busy || modelBusy) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                Text(if (modelBusy) "Consultando modelos de imagem no Gemini" else operation,
                    style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                if (activeRequest != null) TextButton(onClick = {
                    activeRequest?.cancel()
                    ai.cancel(); imageClient.cancel()
                    activeRequest = null
                    busy = false; operation = ""
                    status = "Solicitação cancelada. Se o envio já começou, o consumo pode ser incerto. Verifique antes de repetir."
                }) { Text("Cancelar") }
            }
        } else if (status.isNotBlank()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, null, Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary)
                Text(" $status", style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f))
                IconButton(onClick = { status = "" }) {
                    Icon(Icons.Default.Close, contentDescription = "Dispensar aviso")
                }
            }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            StudioWizardStepHeader(draft.step, onNavigate = { if (!busy) draft = draft.copy(step = it) })
            when (draft.step) {
                0 -> StudioWizardStyle(draft.story, libraryData?.styles.orEmpty(),
                    onChange = { draft = draft.copy(story = it) },
                    onSaveStyle = { style ->
                        libraryData?.let { data -> saveLibrary(data.copy(styles = (data.styles + style).distinct().take(30))) }
                    })
                1 -> StudioWizardPeople(draft.people, libraryData, referenceLibrary, referenceImageRev,
                    onUpsert = { person ->
                        val people = if (draft.people.any { it.id == person.id })
                            draft.people.map { if (it.id == person.id) person else it }
                        else (draft.people + person).take(30)
                        draft = draft.copy(people = people)
                    }, onDelete = { id -> draft = draft.copy(people = draft.people.filterNot { it.id == id }) },
                    onImport = { id -> galleryReference = "person" to id; importReference.launch(arrayOf("image/*")) },
                    onSaveLibrary = { person ->
                        libraryData?.let { data ->
                            val savedPerson = person.copy(id = java.util.UUID.randomUUID().toString())
                            saveLibrary(data.copy(people = (data.people + savedPerson).take(100)))
                        }
                    }, onEnrich = { id ->
                        draft.people.indexOfFirst { it.id == id }.takeIf { it >= 0 }?.let { pending = StudioJob("person", it) }
                    }, busy = busy)
                2 -> StudioWizardPlaces(draft.places, libraryData, referenceLibrary, referenceImageRev,
                    onUpsert = { place ->
                        val places = if (draft.places.any { it.id == place.id })
                            draft.places.map { if (it.id == place.id) place else it }
                        else (draft.places + place).take(30)
                        draft = draft.copy(places = places)
                    }, onDelete = { id -> draft = draft.copy(places = draft.places.filterNot { it.id == id }) },
                    onImport = { id -> galleryReference = "place" to id; importReference.launch(arrayOf("image/*")) },
                    onSaveLibrary = { place ->
                        libraryData?.let { data ->
                            val savedPlace = place.copy(id = java.util.UUID.randomUUID().toString())
                            saveLibrary(data.copy(places = (data.places + savedPlace).take(100)))
                        }
                    }, onEnrich = { id ->
                        draft.places.indexOfFirst { it.id == id }.takeIf { it >= 0 }?.let { pending = StudioJob("place", it) }
                    }, busy = busy)
                3 -> {
                    Text("Monte os clipes", style = MaterialTheme.typography.headlineSmall)
                    Text("Uma cena = um vídeo do Flow. Selecione os personagens e descreva o que acontece.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${draft.scenes.size} clipe(s) · ${StudioSceneTiming.total(draft.scenes)}s estimados",
                        style = MaterialTheme.typography.labelMedium)
                    var planningOpen by remember { mutableStateOf(false) }
                    TextButton(onClick = { planningOpen = !planningOpen }) {
                        Icon(Icons.Default.AutoAwesome, null)
                        Text(" Pedir sugestões ao Gemini (opcional)")
                    }
                    if (planningOpen) {
                        StudioField("Ideia curta da história", draft.story.idea, 5000, 2) { idea ->
                            draft = draft.copy(story = draft.story.copy(idea = idea))
                        }
                        OutlinedButton(onClick = { pending = StudioJob("story") },
                            enabled = !busy && draft.story.idea.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                            Text("Sugerir história e personagens")
                        }
                    }
                    if (planningOpen) {
                        OutlinedButton(onClick = { pending = StudioJob("scenes") },
                            enabled = !busy && FlowStudioClipPolicy.canDivide(draft.story),
                            modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.AutoAwesome, null); Text(" Sugerir clipes com Gemini (opcional)")
                        }
                    }
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
                            onPrepare = {
                                runCatching { FlowStudioClipPolicy.prepare(scene, draft, i) }
                                    .onSuccess { prepared -> updateScene(scene.id) { prepared }; status = "Prompt preparado localmente; confira antes de usar no Flow." }
                                    .onFailure { status = it.message ?: "Descreva a cena antes." }
                            },
                            onImageGenerate = {
                                if (!FlowStudioClipPolicy.canPrepare(scene) && draft.story.idea.isBlank() &&
                                    draft.story.synopsis.isBlank())
                                    status = "Descreva uma ideia ou a ação da cena antes de pedir uma imagem ao Gemini."
                                else imageDialog = i
                            },
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
                    Button(onClick = {
                        if (draft.scenes.size < 60) {
                            val next = StudioScene(title = "Cena ${draft.scenes.size + 1}")
                            draft = draft.copy(scenes = draft.scenes + next)
                            editingScene = next
                        }
                    }, enabled = !busy && draft.scenes.size < 60, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Add, null); Text(" Nova cena (um clipe no Flow)")
                    }
                }
                4 -> {
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
                        draft.scenes.mapIndexed { i, s -> "Cena ${i + 1} (${s.durationSeconds}s estimados): ${s.title}\n${s.actions}\n${s.effectiveDialogue()}" }.joinToString("\n\n"),
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
                val keys = GeminiCredentialStore(context)
                val active = keys.profiles().firstOrNull { it.id == keys.activeId() }
                Text("Gemini: ${active?.label ?: "sem chave"} · Troca após 429: " +
                    (if (keys.autoFallbackEnabled()) "ativada" else "desativada"),
                    style = MaterialTheme.typography.bodySmall)
                StudioReferenceEditor(draft, onUpdate = { draft = it })
                Text("O modo manual mantém Texto → vídeo, Vídeo de referência, estilo-base, " +
                    "constantes, edição de cenas, aprimoramento e refinamento em lote Gemini.",
                    style = MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick = { if (saveNow()) onManual() }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Edit, null); Text(" Abrir editor manual de prompts")
                }
            }
            if (saveError) Text("Falha ao salvar rascunho; não saia antes de corrigir o armazenamento.",
                color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(8.dp))
        }
        HorizontalDivider()
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { draft = draft.copy(step = (draft.step - 1).coerceAtLeast(0)) },
                enabled = draft.step > 0 && !busy) { Icon(Icons.Default.ArrowBack, null); Text(" Voltar") }
            Button(onClick = { if (draft.step < 4) draft = draft.copy(step = draft.step + 1) },
                enabled = draft.step < 4 && !busy, modifier = Modifier.weight(1f)) {
                Text(if (draft.step == 3) "Preparar para Flow" else "Continuar")
                Icon(Icons.Default.ArrowForward, null)
            }
            if (draft.step == 4) Text("Projeto salvo", style = MaterialTheme.typography.labelSmall)
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
    onGenerate: () -> Unit, onSuggested: (String) -> Unit, onContinue: () -> Unit,
    onStartScenes: () -> Unit, disabled: Boolean) {
    var scriptOpen by remember { mutableStateOf(false) }
    var preferencesOpen by remember { mutableStateOf(false) }
    var detailsOpen by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    fun change(next: StudioStory) { onUpdate(next.copy(approved = false)) }
    Text("Sua ideia", style = MaterialTheme.typography.titleMedium)
    Text("Escreva em poucas palavras. Pode criar cada clipe sozinho, usar uma imagem de referência " +
        "ou pedir ao Gemini para sugerir a história. Não é obrigatório ter roteiro completo.",
        style = MaterialTheme.typography.bodySmall)
    StudioField("O que você imagina? Ex.: abacate e banana discutindo", story.idea, 5000, 2) {
        change(story.copy(idea = it))
    }
    Button(onClick = onStartScenes, enabled = !disabled, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Default.MovieCreation, null); Text(" Criar minhas cenas")
    }
    OutlinedButton(onClick = onGenerate,
        enabled = !disabled && (story.idea.isNotBlank() || story.originalScript.isNotBlank()),
        modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Default.AutoAwesome, null)
        Text(if (story.ready()) " Pedir outra história ao Gemini" else " Pedir ajuda ao Gemini com a história")
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
        val idea = ideas.filterNot { it == story.idea }.randomOrNull() ?: ideas.first()
        change(story.copy(idea = idea))
        onSuggested("Ideia sugerida localmente, sem Gemini e sem consumo de API. Toque em Gerar história com Gemini para desenvolver.")
    }, modifier = Modifier.fillMaxWidth(), enabled = !disabled) {
        Icon(Icons.Default.Casino, null); Text(" Surpreenda-me · sugestão grátis")
    }
    Text("Tipo de animação", style = MaterialTheme.typography.titleSmall)
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("Animação 3D", "Animação 2D", "Realista", "Personalizado").forEach { item ->
            FilterChip(selected = story.style == item, onClick = { change(story.copy(style = item)) },
                label = { Text(item) })
        }
    }
    OutlinedButton(onClick = { scriptOpen = !scriptOpen }, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Default.Description, null); Text(" Colar roteiro existente", Modifier.weight(1f))
        Icon(if (scriptOpen) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
    }
    if (scriptOpen) StudioField("Roteiro existente (não enviado automaticamente)", story.originalScript, 25000, 4) {
        change(story.copy(originalScript = it))
    }
    OutlinedButton(onClick = { preferencesOpen = !preferencesOpen }, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Default.Tune, null)
        Text(" ${story.durationSeconds}s · ${story.style} · ${story.genre}", Modifier.weight(1f),
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        Icon(if (preferencesOpen) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
    }
    if (preferencesOpen) {
        Text("Duração desejada", style = MaterialTheme.typography.titleSmall)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(15 to "15s", 30 to "30s", 60 to "1min", 75 to "1min15s", 120 to "2min", 180 to "3min")
                .forEach { (time, label) -> FilterChip(selected = story.durationSeconds == time,
                    onClick = { change(story.copy(durationSeconds = time)) }, label = { Text(label) }) }
            FilterChip(selected = story.durationSeconds !in listOf(15, 30, 60, 75, 120, 180),
                onClick = { change(story.copy(durationSeconds = 90)) }, label = { Text("Outra") })
        }
        if (story.durationSeconds !in listOf(15, 30, 60, 75, 120, 180)) {
            var customDuration by remember(story.durationSeconds) { mutableStateOf(story.durationSeconds.toString()) }
            OutlinedTextField(value = customDuration, onValueChange = { raw ->
                customDuration = raw.filter(Char::isDigit).take(3)
                customDuration.toIntOrNull()?.takeIf { it in 5..900 }?.let { change(story.copy(durationSeconds = it)) }
            }, label = { Text("Duração personalizada (5–900 s)") }, singleLine = true,
                modifier = Modifier.fillMaxWidth())
        }
        if (story.style !in listOf("Realista", "Animação 3D", "Animação 2D"))
            StudioField("Estilo personalizado", story.style, 120) { change(story.copy(style = it)) }
        Text("Gênero", style = MaterialTheme.typography.titleSmall)
        val genres = listOf("Humor", "Comédia absurda", "Terror cômico", "Aventura")
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            genres.forEach { item -> FilterChip(selected = story.genre == item,
                onClick = { change(story.copy(genre = item)) }, label = { Text(item) }) }
            FilterChip(selected = story.genre !in genres, onClick = { change(story.copy(genre = "Personalizado")) },
                label = { Text("Outro") })
        }
        if (story.genre !in genres) StudioField("Gênero personalizado", story.genre, 120) {
            change(story.copy(genre = it))
        }
    }
    if (story.ready()) {
        OutlinedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("História preparada", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary)
                Text(story.title, style = MaterialTheme.typography.titleMedium)
                Text(story.synopsis, style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = { detailsOpen = !detailsOpen }) {
                    Icon(if (detailsOpen) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
                    Text(if (detailsOpen) " Recolher roteiro" else " Ler roteiro completo")
                }
                if (detailsOpen) {
                    listOf("Começo" to story.beginning, "Desenvolvimento" to story.development,
                        "Reviravolta" to story.twist, "Final" to story.ending).forEach { (label, value) ->
                        if (value.isNotBlank()) {
                            Text(label, style = MaterialTheme.typography.labelLarge)
                            Text(value, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    } else {
        Text("A história completa e os personagens aparecerão aqui após a geração. Você não precisa preencher os campos manualmente.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    OutlinedButton(onClick = { editing = !editing }, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Default.Edit, null)
        Text(if (editing) " Ocultar edição detalhada" else " Editar título e detalhes manualmente", Modifier.weight(1f))
        Icon(if (editing) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
    }
    if (editing) {
        StudioField("Título da história", story.title, 250) { change(story.copy(title = it)) }
        StudioField("Sinopse", story.synopsis, 6000, 2) { change(story.copy(synopsis = it)) }
        StudioField("Começo", story.beginning, 6000, 2) { change(story.copy(beginning = it)) }
        StudioField("Desenvolvimento", story.development, 10000, 2) { change(story.copy(development = it)) }
        StudioField("Reviravolta", story.twist, 6000, 2) { change(story.copy(twist = it)) }
        StudioField("Final", story.ending, 6000, 2) { change(story.copy(ending = it)) }
    }
    if (story.ready()) OutlinedButton(onClick = onContinue, enabled = !disabled,
        modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Default.Check, null); Text(" Usar esta história e criar cenas")
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
    onUp: () -> Unit, onDown: () -> Unit, onRegenerate: () -> Unit, onPrepare: () -> Unit,
    onImageGenerate: () -> Unit, onImageImport: () -> Unit, onImageExport: () -> Unit,
    onShare: () -> Unit, onCopy: () -> Unit) {
    var menuOpen by remember(scene.id) { mutableStateOf(false) }
    var details by remember(scene.id) { mutableStateOf(false) }
    val mainAction = when {
        !FlowStudioClipPolicy.canPrepare(scene) -> "Descrever cena"
        scene.prompt.isBlank() -> "Preparar prompt sem IA"
        else -> "Copiar prompt"
    }
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Cena ${index + 1} · ${scene.durationSeconds}s estimados", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary)
                    Text(scene.title, style = MaterialTheme.typography.titleMedium)
                }
                IconButton(onClick = onUp, enabled = index > 0 && !busy) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Subir cena")
                }
                IconButton(onClick = onDown, enabled = index < count - 1 && !busy) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Descer cena")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                StudioSceneThumbnail(scene, storage, revision)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(scene.status(), style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(scene.actions.ifBlank { scene.visual.ifBlank { "Roteiro ainda não gerado." } },
                        maxLines = 3, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                    if (scene.characters.isNotBlank()) {
                        Text("Personagens · ${scene.characters}", maxLines = 2,
                            overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            val lines = scene.dialogueLines
            if (lines.isNotEmpty()) {
                Text("Falas", style = MaterialTheme.typography.labelLarge)
                lines.take(if (details) 20 else 2).forEach { line ->
                    Text("${line.speaker.ifBlank { "Personagem" }}: ${line.speech}",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = if (details) 4 else 2, overflow = TextOverflow.Ellipsis)
                }
                if (!details && lines.size > 2) Text("+ ${lines.size - 2} fala(s) · ver detalhes",
                    style = MaterialTheme.typography.labelSmall)
            } else if (scene.dialogue.isNotBlank()) {
                Text("Falas · ${scene.dialogue}", maxLines = if (details) 8 else 2,
                    style = MaterialTheme.typography.bodySmall, overflow = TextOverflow.Ellipsis)
            }
            if (scene.camera.isNotBlank()) Text("Câmera · ${scene.camera}", maxLines = if (details) 8 else 2,
                style = MaterialTheme.typography.bodySmall, overflow = TextOverflow.Ellipsis)
            if (scene.effects.isNotBlank()) Text("Sons · ${scene.effects}", maxLines = if (details) 8 else 2,
                style = MaterialTheme.typography.bodySmall, overflow = TextOverflow.Ellipsis)
            TextButton(onClick = { details = !details }) {
                Icon(if (details) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
                Text(if (details) " Ocultar detalhes" else " Ver ações e continuidade")
            }
            if (details) {
                if (scene.visual.isNotBlank()) Text("Visual · ${scene.visual}", style = MaterialTheme.typography.bodySmall)
                Text("Início · ${scene.initialState.ifBlank { "Não definido" }}", style = MaterialTheme.typography.bodySmall)
                Text("Final · ${scene.finalState.ifBlank { "Não definido" }}", style = MaterialTheme.typography.bodySmall)
            }
            OutlinedButton(onClick = onImageImport, modifier = Modifier.fillMaxWidth(), enabled = !busy) {
                Icon(Icons.Default.PhotoLibrary, null)
                Text(if (scene.imageAsset.isBlank()) " Usar imagem da galeria (opcional)" else " Trocar imagem da galeria")
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(onClick = {
                    when {
                        !FlowStudioClipPolicy.canPrepare(scene) -> onEdit()
                        scene.prompt.isBlank() -> onPrepare()
                        else -> onCopy()
                    }
                }, enabled = !busy, modifier = Modifier.weight(1f)) {
                    Icon(when {
                        !FlowStudioClipPolicy.canPrepare(scene) -> Icons.Default.Edit
                        scene.prompt.isBlank() -> Icons.Default.Description
                        else -> Icons.Default.ContentCopy
                    }, null)
                    Text(" $mainAction", maxLines = 2)
                }
                TextButton(onClick = onEdit, enabled = !busy) { Text("Editar") }
                Box {
                    IconButton(onClick = { menuOpen = true }, enabled = !busy) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Outras ações da cena")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(text = { Text("Aprimorar este clipe com Gemini") }, onClick = {
                            menuOpen = false; onRegenerate()
                        })
                        DropdownMenuItem(text = { Text("Preparar prompt sem IA") },
                            enabled = FlowStudioClipPolicy.canPrepare(scene), onClick = {
                            menuOpen = false; onPrepare()
                        })
                        DropdownMenuItem(text = { Text("Gerar ou regenerar imagem") }, onClick = {
                            menuOpen = false; onImageGenerate()
                        })
                        DropdownMenuItem(text = { Text("Copiar prompt") }, enabled = scene.prompt.isNotBlank(),
                            onClick = { menuOpen = false; onCopy() })
                        DropdownMenuItem(text = { Text("Importar imagem / último quadro") },
                            onClick = { menuOpen = false; onImageImport() })
                        DropdownMenuItem(text = { Text("Salvar imagem") }, enabled = scene.imageAsset.isNotBlank(),
                            onClick = { menuOpen = false; onImageExport() })
                        DropdownMenuItem(text = { Text("Compartilhar imagem") }, enabled = scene.imageAsset.isNotBlank(),
                            onClick = { menuOpen = false; onShare() })
                        DropdownMenuItem(text = { Text("Duplicar cena") }, enabled = count < 60,
                            onClick = { menuOpen = false; onDuplicate() })
                        DropdownMenuItem(text = { Text("Excluir cena") }, onClick = { menuOpen = false; onDelete() })
                    }
                }
            }
        }
    }
}

@Composable
private fun StudioSceneEditor(original: StudioScene, people: List<String>,
    onSave: (StudioScene) -> Unit, onDismiss: () -> Unit) {
    var value by remember(original.id) { mutableStateOf(original) }
    var advancedOpen by remember(original.id) { mutableStateOf(false) }
    var dialogueOpen by remember(original.id) {
        mutableStateOf(original.effectiveDialogue().isNotBlank())
    }
    var directionOpen by remember(original.id) {
        mutableStateOf(original.camera.isNotBlank() || original.effects.isNotBlank())
    }
    var rawDialogueOpen by remember(original.id) { mutableStateOf(false) }
    fun setLines(lines: List<StudioDialogueLine>) {
        value = value.copy(dialogueLines = lines,
            dialogue = lines.joinToString("\n") { "${it.speaker}: ${it.speech}" })
    }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Cena · ${original.title}") },
        text = { Column(Modifier.heightIn(max = 490.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Um cartão = um vídeo do Flow. Descreva a ação ou reação; o resto é opcional.",
                style = MaterialTheme.typography.bodySmall)
            if (people.isNotEmpty()) {
                Text("Quem aparece?", style = MaterialTheme.typography.labelMedium)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    people.filter { it.isNotBlank() }.forEach { name ->
                        val selectedNames = value.characters.split(',').map { it.trim() }.filter { it.isNotBlank() }
                        FilterChip(selected = name in selectedNames, onClick = {
                            val next = if (name in selectedNames) selectedNames - name else selectedNames + name
                            value = value.copy(characters = next.joinToString(", "))
                        }, label = { Text(name) })
                    }
                }
            }
            StudioField("O que acontece nesta cena?", value.actions, 6000, 3) {
                value = value.copy(actions = it)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(onClick = { dialogueOpen = !dialogueOpen }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.RecordVoiceOver, null)
                    Text(" Falas por personagem", maxLines = 2)
                }
                OutlinedButton(onClick = { directionOpen = !directionOpen }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Videocam, null)
                    Text(" Câmera e sons", maxLines = 2)
                }
            }
            if (dialogueOpen) {
                val currentLines = value.dialogueLines
                val firstLine = currentLines.firstOrNull() ?: StudioDialogueLine()
                if (people.isNotEmpty()) {
                    Text("Quem fala agora?", style = MaterialTheme.typography.labelSmall)
                    Row(Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        people.filter { it.isNotBlank() }.forEach { name ->
                            FilterChip(selected = firstLine.speaker == name, onClick = {
                                setLines(if (currentLines.isEmpty()) listOf(firstLine.copy(speaker = name))
                                    else currentLines.toMutableList().also { it[0] = firstLine.copy(speaker = name) })
                            }, label = { Text(name) })
                        }
                    }
                }
                StudioField("Quem fala?", firstLine.speaker, 120) { speaker ->
                    setLines(if (currentLines.isEmpty()) listOf(firstLine.copy(speaker = speaker))
                        else currentLines.toMutableList().also { it[0] = firstLine.copy(speaker = speaker) })
                }
                StudioField("O que esse personagem diz? (português brasileiro)", firstLine.speech, 800, 2) { speech ->
                    setLines(if (currentLines.isEmpty()) listOf(firstLine.copy(speech = speech))
                        else currentLines.toMutableList().also { it[0] = firstLine.copy(speech = speech) })
                }
                if (currentLines.isEmpty() && value.dialogue.isNotBlank()) Text(
                    "Falas antigas: ${value.dialogue.take(260)}. Você pode converter ou editar abaixo.",
                    style = MaterialTheme.typography.labelSmall)
                currentLines.drop(1).forEachIndexed { extraIndex, line ->
                    val i = extraIndex + 1
                    OutlinedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Fala ${i + 1}", modifier = Modifier.weight(1f))
                                IconButton(onClick = { setLines(currentLines.filterIndexed { index, _ -> index != i }) }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Remover fala ${i + 1}")
                                }
                            }
                            StudioField("Quem fala?", line.speaker, 120) { speaker ->
                                setLines(currentLines.mapIndexed { index, previous ->
                                    if (index == i) previous.copy(speaker = speaker) else previous })
                            }
                            StudioField("Texto exato em português", line.speech, 800, 2) { speech ->
                                setLines(currentLines.mapIndexed { index, previous ->
                                    if (index == i) previous.copy(speech = speech) else previous })
                            }
                        }
                    }
                }
                TextButton(onClick = {
                    val existing = if (currentLines.isEmpty() && value.dialogue.isNotBlank())
                        StudioDialoguePolicy.fromLegacy(value.dialogue) else currentLines
                    if (existing.size < 20) setLines(existing + StudioDialogueLine(people.firstOrNull().orEmpty(), ""))
                }, enabled = currentLines.size < 20 && (currentLines.isNotEmpty() ||
                    StudioDialoguePolicy.canConvert(value.dialogue))) {
                    Icon(Icons.Default.Add, null); Text(" Adicionar outra fala")
                }
                TextButton(onClick = { rawDialogueOpen = !rawDialogueOpen }) {
                    Text(if (rawDialogueOpen) "Ocultar texto livre" else "Falas antigas em texto livre")
                }
                if (rawDialogueOpen) StudioField("Falas (texto livre)", value.dialogue, 4000, 2) {
                    value = value.copy(dialogue = it, dialogueLines = emptyList())
                }
            }
            if (directionOpen) {
                StudioField("Câmera: quem aparece e como se move?", value.camera, 3000, 2) {
                    value = value.copy(camera = it)
                }
                StudioField("Sons ou efeitos (opcional)", value.effects, 3000) {
                    value = value.copy(effects = it)
                }
            }
            OutlinedButton(onClick = { advancedOpen = !advancedOpen }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Tune, null)
                Text(" Mais detalhes, visual e continuidade", Modifier.weight(1f))
                Icon(if (advancedOpen) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
            }
            if (advancedOpen) {
                StudioField("Título da cena", value.title, 200) { value = value.copy(title = it) }
                var durationText by remember(original.id) { mutableStateOf(value.durationSeconds.toString()) }
                OutlinedTextField(value = durationText, onValueChange = { raw ->
                    durationText = raw.filter(Char::isDigit).take(2)
                    durationText.toIntOrNull()?.takeIf { it in 1..60 }?.let { value = value.copy(durationSeconds = it) }
                }, label = { Text("Duração estimada da cena (1–60 s)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth())
                StudioField("Personagens da cena", value.characters, 2000) { value = value.copy(characters = it) }
                if (people.isNotEmpty()) Text("Referências: ${people.joinToString(", ")}",
                    style = MaterialTheme.typography.labelSmall)
                StudioField("Descrição visual", value.visual, 4000, 2) { value = value.copy(visual = it) }
                StudioField("Estado inicial", value.initialState, 4000, 2) { value = value.copy(initialState = it) }
                StudioField("Estado final", value.finalState, 4000, 2) { value = value.copy(finalState = it) }
                StudioField("Prompt individual em inglês", value.prompt, 10000, 4) { value = value.copy(prompt = it) }
                Text("Se modificar uma cena, revise o prompt antes de usá-lo ou prepare-o novamente.",
                    style = MaterialTheme.typography.bodySmall)
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
