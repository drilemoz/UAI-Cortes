package br.com.uaicortes

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Estilo único por aparelho, contexto e até 12 cenas por projeto. */
@Composable
internal fun FlowPromptScreen(project: VideoProject,
    onSave: (FlowPromptDraft) -> Boolean, back: () -> Unit) {
    val context = LocalContext.current
    val styleStore = remember(context) { FlowPromptStyleStore(context) }
    var style by remember { mutableStateOf(styleStore.load()) }
    var expandStyle by remember { mutableStateOf(false) }
    var draft by remember(project.id) { mutableStateOf(project.flowPromptDraft ?: FlowPromptDraft()) }
    var saved by remember(project.id) { mutableStateOf(project.flowPromptDraft ?: FlowPromptDraft()) }
    var selected by remember(project.id) { mutableIntStateOf(0) }
    var message by remember { mutableStateOf("") }
    var saveError by remember { mutableStateOf(false) }
    val latestSave by rememberUpdatedState(onSave)
    val gemini = remember(project.id) { GeminiVideoCaptionClient(context) }
    val scope = rememberCoroutineScope()
    DisposableEffect(gemini) { onDispose { gemini.cancel() } }
    var consent by remember(project.id) { mutableStateOf(false) }
    var countedInput by remember(project.id) { mutableStateOf<String?>(null) }
    var countingInput by remember(project.id) { mutableStateOf(false) }
    var busy by remember(project.id) { mutableStateOf(false) }
    var suggestion by remember(project.id) { mutableStateOf<String?>(null) }
    var suggestionIndex by remember(project.id) { mutableIntStateOf(0) }
    fun persist(): Boolean {
        if (saved == draft) return true
        val success = runCatching { latestSave(draft.normalized()) }.getOrDefault(false)
        if (success) saved = draft.normalized()
        saveError = !success
        return success
    }
    fun exit() { if (persist()) back() }
    LaunchedEffect(project.id, draft) { if (draft != saved) { delay(650); persist() } }
    BackHandler { exit() }
    if (consent) AlertDialog(onDismissRequest = { consent = false },
        title = { Text("Enviar cena ao Gemini?") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Somente a descrição da cena e seu contexto serão enviados. Nenhum MP4, áudio, imagem, URI ou estilo-base será enviado.")
            GeminiUsageHint("refinar cena Flow", draft.scenes[selected].length + draft.context.length)
            OutlinedButton(onClick = {
                countingInput = true
                countedInput = "Consultando entrada no Gemini…"
                scope.launch {
                    try {
                        val key = GeminiCredentialStore(context).read()
                            ?: error("Configure uma chave Gemini.")
                        val pref = context.applicationContext.getSharedPreferences(
                            "caption_model_preferences", Context.MODE_PRIVATE)
                        val model = GeminiCaptionModelPolicy.selectedOrDefault(pref.getString("model", null))
                        val tokens = gemini.countFlowInput(key, draft.scenes[selected], draft.context,
                            draft.mode, model)
                        countedInput = "$tokens tokens de entrada contados; resposta adicional."
                    } catch (cancel: CancellationException) { throw cancel }
                    catch (_: Exception) { countedInput = "Contagem indisponível; a geração continua disponível." }
                    finally { countingInput = false }
                }
            }, enabled = !countingInput) { Text("Conferir tokens de entrada") }
            countedInput?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        } },
        confirmButton = { TextButton(onClick = {
            consent = false
            if (!busy) {
                val at = selected
                val oldScene = draft.scenes[at]
                val sceneContext = draft.context
                val sceneMode = draft.mode
                busy = true
                scope.launch {
                    try {
                        val key = GeminiCredentialStore(context).read()
                            ?: error("Cadastre sua chave Gemini nas Configurações.")
                        val pref = context.applicationContext.getSharedPreferences(
                            "caption_model_preferences", Context.MODE_PRIVATE)
                        val model = GeminiCaptionModelPolicy.selectedOrDefault(pref.getString("model", null))
                        val result = gemini.refineFlowScene(key, oldScene, sceneContext, sceneMode, model)
                        suggestionIndex = at
                        suggestion = result
                        message = "Revise a sugestão antes de aplicar. " +
                            GeminiUsageLedger(context).lastMessage("flow")
                    } catch (cancel: CancellationException) { throw cancel }
                    catch (failure: Exception) {
                        message = failure.message?.take(220) ?: "Falha ao consultar Gemini."
                    } finally { busy = false }
                }
            }
        }, enabled = !countingInput) { Text("Enviar texto") } },
        dismissButton = { TextButton(onClick = { consent = false }) { Text("Cancelar") } })
    suggestion?.let { proposed -> AlertDialog(onDismissRequest = { suggestion = null },
        title = { Text("Revisar cena ${suggestionIndex + 1}") },
        text = { Column(Modifier.heightIn(max = 300.dp).verticalScroll(rememberScrollState())) {
            Text(proposed)
        } },
        confirmButton = { TextButton(onClick = {
            if (suggestionIndex in draft.scenes.indices) {
                draft = draft.copy(scenes = draft.scenes.toMutableList().also { it[suggestionIndex] = proposed })
                selected = suggestionIndex
            }
            suggestion = null
        }) { Text("Usar na cena") } },
        dismissButton = { TextButton(onClick = { suggestion = null }) { Text("Descartar") } }) }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { exit() }) { Icon(Icons.Default.ArrowBack, "Voltar") }
            Column(Modifier.weight(1f)) {
                Text("Prompts para o Flow", style = MaterialTheme.typography.titleLarge)
                Text(project.title, style = MaterialTheme.typography.bodySmall)
            }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Defina 9:16 no Google Flow. Um prompt por cena, sem especificar duração ou proporção no texto.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(onClick = { expandStyle = !expandStyle }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Palette, null)
                Text(" Estilo-base único (editável)", Modifier.weight(1f))
                Icon(if (expandStyle) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
            }
            if (expandStyle) {
                OutlinedTextField(value = style, onValueChange = { style = it.take(2000) },
                    label = { Text("Estilo fixo para todos os vídeos") }, minLines = 4, maxLines = 8,
                    modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { style = FlowPromptPolicy.DEFAULT_STYLE },
                        modifier = Modifier.weight(1f)) { Text("Restaurar 3D") }
                    Button(onClick = {
                        message = if (styleStore.save(style)) "Estilo salvo no aparelho."
                            else "Falha ao salvar o estilo."
                    }, modifier = Modifier.weight(1f)) { Text("Salvar estilo") }
                }
            }
            Text("Modo", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = draft.mode == "create", onClick = { draft = draft.copy(mode = "create") },
                    label = { Text("Texto → vídeo") })
                FilterChip(selected = draft.mode == "transform", onClick = { draft = draft.copy(mode = "transform") },
                    label = { Text("Vídeo de referência") })
            }
            OutlinedTextField(value = draft.context,
                onValueChange = { draft = draft.copy(context = it.take(1200)) },
                label = { Text("Personagens e cenários constantes (opcional)") },
                placeholder = { Text("Aparência, roupas e local que devem continuar iguais nas cenas") },
                minLines = 2, maxLines = 4, modifier = Modifier.fillMaxWidth())
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Cena ${selected + 1}/${draft.scenes.size}", Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = { selected-- }, enabled = selected > 0) {
                    Icon(Icons.Default.ChevronLeft, "Cena anterior")
                }
                IconButton(onClick = { selected++ }, enabled = selected < draft.scenes.lastIndex) {
                    Icon(Icons.Default.ChevronRight, "Próxima cena")
                }
            }
            OutlinedTextField(value = draft.scenes[selected], onValueChange = { value ->
                draft = draft.copy(scenes = draft.scenes.toMutableList().also { it[selected] = value.take(1800) })
            }, modifier = Modifier.fillMaxWidth(), minLines = 4, maxLines = 9,
                label = { Text("Ações desta cena") },
                placeholder = { Text("Descreva gestos, expressões, ações, movimentos de câmera e sons.") },
                supportingText = { Text("${draft.scenes[selected].length}/1800 caracteres") })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    if (draft.scenes.size < 12) {
                        draft = draft.copy(scenes = draft.scenes + "")
                        selected = draft.scenes.lastIndex
                    }
                }, enabled = draft.scenes.size < 12, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Add, null); Text(" Cena")
                }
                OutlinedButton(onClick = {
                    if (draft.scenes.size > 1) {
                        draft = draft.copy(scenes = draft.scenes.filterIndexed { i, _ -> i != selected })
                        selected = selected.coerceAtMost(draft.scenes.lastIndex)
                    }
                }, enabled = draft.scenes.size > 1, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.DeleteOutline, null); Text(" Excluir")
                }
            }
            OutlinedButton(onClick = { countedInput = null; consent = true },
                enabled = !busy && draft.scenes[selected].isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.AutoAwesome, null)
                Text(if (busy) " Aguarde Gemini..." else " Aprimorar cena com Gemini")
            }
            Text("Prompt completo desta cena", style = MaterialTheme.typography.titleSmall)
            val composed = runCatching { FlowPromptPolicy.compose(style, draft, selected) }.getOrNull()
            Text(composed ?: "Descreva a cena para montar o prompt.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (message.isNotBlank()) Text(message, style = MaterialTheme.typography.labelSmall)
            if (saveError) Text("Não foi possível salvar. O rascunho foi preservado nesta tela.",
                color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(8.dp))
        }
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { if (persist()) message = "Cenas salvas." },
                modifier = Modifier.weight(1f)) { Icon(Icons.Default.Save, null); Text(" Salvar") }
            Button(onClick = {
                val prompt = runCatching { FlowPromptPolicy.compose(style, draft, selected) }.getOrNull()
                if (prompt != null && persist()) {
                    (context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager)
                        ?.setPrimaryClip(ClipData.newPlainText("Prompt Flow ${selected + 1}", prompt))
                    message = "Prompt da cena ${selected + 1} copiado."
                }
            }, enabled = draft.scenes[selected].isNotBlank(), modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.ContentCopy, null); Text(" Copiar")
            }
        }
    }
}
