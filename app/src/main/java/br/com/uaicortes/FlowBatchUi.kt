package br.com.uaicortes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Gemini Batch de refinamento de cenas: somente texto, sem alterar projeto sem revisão. */
@Composable
internal fun FlowBatchSection(
    projectId: String, draft: FlowPromptDraft,
    onBeforeSend: () -> Boolean, onApply: (Int, String) -> Unit
) {
    val ctx = LocalContext.current
    val credentials = remember(ctx) { GeminiCredentialStore(ctx) }
    val storage = remember(ctx) { GeminiBatchStore(ctx) }
    val client = remember(projectId) { GeminiBatchClient() }
    val scope = rememberCoroutineScope()
    DisposableEffect(client) { onDispose { client.cancel() } }
    var expanded by remember(projectId) { mutableStateOf(false) }
    var chosen by remember(projectId) {
        mutableStateOf(draft.scenes.indices.filter { draft.scenes[it].isNotBlank() }.take(8).toSet())
    }
    var model by remember(projectId) { mutableStateOf(GeminiCaptionModelPolicy.DEFAULT_MODEL) }
    var acknowledged by remember(projectId) { mutableStateOf(false) }
    var confirmSubmission by remember(projectId) { mutableStateOf<Pair<FlowBatchPlan, String>?>(null) }
    var busy by remember(projectId) { mutableStateOf(false) }
    var message by remember(projectId) { mutableStateOf("") }
    var records by remember(projectId) { mutableStateOf(storage.list()) }
    var inspected by remember(projectId) {
        mutableStateOf<Pair<GeminiBatchRecord, GeminiBatchStatus>?>(null)
    }
    var pendingApply by remember(projectId) { mutableStateOf<Pair<Int, String>?>(null) }
    var remoteJobs by remember(projectId) { mutableStateOf<List<String>>(emptyList()) }
    var pendingLink by remember(projectId) { mutableStateOf<Pair<GeminiBatchRecord, String>?>(null) }
    fun reload() { records = storage.list() }
    fun currentProfile(): GeminiKeyProfile? {
        val id = credentials.activeId() ?: return null
        return credentials.profiles().firstOrNull { it.id == id }
    }
    val own = records.filter { it.flowProjectId == projectId }.asReversed().take(12)
    Card(shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            TextButton(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth()) {
                Text(if (expanded) "▾ Refinar várias cenas · Gemini Batch" else
                    "▸ Refinar várias cenas · Gemini Batch")
            }
            if (!expanded) return@Column
            Text("Opcional: envie de 2 a 8 cenas para processamento de TEXTO não urgente. " +
                "Requer modelo compatível, projeto com Batch disponível e possível cobrança. " +
                "O Google pode levar até 24 horas; nenhuma narração, mídia ou estilo-base é enviado.",
                style = MaterialTheme.typography.bodySmall)
            Text("Marque as cenas. Cada resposta precisa ser consultada e revisada antes de substituir o texto original.",
                style = MaterialTheme.typography.bodySmall)
            draft.scenes.forEachIndexed { index, scene ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Checkbox(checked = index in chosen, onCheckedChange = { selected ->
                        chosen = if (selected) chosen + index else chosen - index
                    }, enabled = scene.isNotBlank() && (index in chosen || chosen.size < 8))
                    Text("Cena ${index + 1}: " + scene.trim().take(72).ifBlank { "Sem texto" },
                        style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                }
            }
            OutlinedTextField(value = model, onValueChange = { model = it.take(100) },
                label = { Text("Modelo Gemini de texto compatível com Batch") },
                singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth()) {
                Checkbox(checked = acknowledged, onCheckedChange = { acknowledged = it })
                Text("Entendo que o envio pode consumir cota e gerar cobrança no projeto selecionado.",
                    style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            }
            Button(onClick = {
                message = try {
                    val p = currentProfile() ?: error("Selecione uma chave Gemini ativa.")
                    require(p.projectId.isNotBlank()) { "Cadastre o ID real do projeto Google Cloud da chave." }
                    GeminiBatchPolicy.model(model)
                    require(acknowledged) { "Confirme o possível custo antes de enviar." }
                    val plan = FlowBatchPolicy.prepare(draft, chosen)
                    val duplicates = own.any { old -> old.projectId == p.projectId &&
                        old.model == GeminiBatchPolicy.model(model) && old.flowScenes == plan.scenes &&
                        old.state !in setOf("JOB_STATE_FAILED", "JOB_STATE_CANCELLED", "JOB_STATE_EXPIRED") }
                    require(!duplicates) {
                        "Já existe um lote para estas cenas neste projeto. Consulte o job anterior antes de reenviar."
                    }
                    confirmSubmission = plan to GeminiBatchPolicy.model(model)
                    ""
                } catch (e: Exception) { e.message ?: "Não foi possível preparar o lote." }
            }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Revisar e enviar cenas") }
            if (busy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            if (message.isNotBlank()) Text(message, style = MaterialTheme.typography.bodySmall)
            HorizontalDivider()
            Text("Jobs deste conjunto de cenas", style = MaterialTheme.typography.titleSmall)
            if (own.isEmpty()) Text("Nenhum lote Flow enviado neste aparelho.",
                style = MaterialTheme.typography.bodySmall)
            own.forEach { item ->
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp)) {
                    Column(Modifier.fillMaxWidth().padding(9.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${item.flowScenes.joinToString { (it.index + 1).toString() }} · ${item.model}",
                            style = MaterialTheme.typography.labelMedium)
                        Text("Projeto ${item.projectId} · ${GeminiBatchPolicy.status(item.state)}" +
                            if (item.remoteName.isBlank()) " · ID remoto desconhecido" else " · ${item.remoteName}",
                            style = MaterialTheme.typography.bodySmall)
                        if (item.remoteName.isNotBlank()) {
                            OutlinedButton(onClick = {
                                val p = currentProfile()
                                if (p?.id != item.profileId || p.projectId != item.projectId) {
                                    message = "Selecione a chave e o projeto que criaram este job."
                                } else {
                                    scope.launch {
                                        busy = true; message = ""
                                        try {
                                            val key = credentials.readProfile(item.profileId)
                                                ?: error("Chave original indisponível.")
                                            val result = client.status(key, item.remoteName)
                                            storage.update(item, result.state)
                                            inspected = item to result
                                            reload()
                                            message = "Consulta concluída. Revise cada cena antes de aplicar."
                                        } catch (c: CancellationException) { throw c }
                                        catch (e: Exception) { message = e.message ?: "Falha ao consultar job." }
                                        finally { busy = false }
                                    }
                                }
                            }, enabled = !busy) { Text("Consultar resultado") }
                        } else {
                            Text("Envio incerto: localize o job na API. Não reenvie automaticamente.",
                                style = MaterialTheme.typography.bodySmall)
                            remoteJobs.take(8).forEach { name ->
                                TextButton(onClick = { pendingLink = item to name }, enabled = !busy) {
                                    Text("Vincular ${name.take(48)}")
                                }
                            }
                        }
                    }
                }
            }
            OutlinedButton(onClick = {
                scope.launch {
                    busy = true; message = ""
                    try {
                        val p = currentProfile() ?: error("Selecione a chave original.")
                        val key = credentials.readProfile(p.id) ?: error("Chave indisponível.")
                        remoteJobs = client.listJobs(key)
                        message = "${remoteJobs.size} job(s) encontrados. Vincule somente o que pertence a este lote."
                    } catch (c: CancellationException) { throw c }
                    catch (e: Exception) { message = e.message ?: "Falha ao localizar jobs." }
                    finally { busy = false }
                }
            }, enabled = !busy && own.any { it.remoteName.isBlank() },
                modifier = Modifier.fillMaxWidth()) { Text("Buscar jobs para envios incertos") }
            inspected?.let { (record, result) ->
                if (record.flowProjectId == projectId) {
                    HorizontalDivider()
                    Text("Resultado: ${GeminiBatchPolicy.status(result.state)}",
                        style = MaterialTheme.typography.titleSmall)
                    if (result.message.isNotBlank()) Text(result.message, style = MaterialTheme.typography.bodySmall)
                    record.flowScenes.forEach { ref ->
                        val answer = FlowBatchPolicy.resultFor(result, record.flowScenes, ref)
                        val compatible = answer != null && FlowBatchPolicy.mayApply(draft, ref, answer)
                        Text("Cena ${ref.index + 1}" + if (!compatible) " · indisponível ou modificada" else " · pronta para revisão",
                            style = MaterialTheme.typography.bodySmall)
                        if (compatible && answer != null) {
                            OutlinedButton(onClick = { pendingApply = ref.index to answer }) {
                                Text("Revisar cena ${ref.index + 1}")
                            }
                        }
                    }
                }
            }
        }
    }
    if (confirmSubmission != null) {
        val (plan, submittedModel) = confirmSubmission!!
        AlertDialog(onDismissRequest = { confirmSubmission = null },
            title = { Text("Confirmar Batch de cenas") },
            text = { Text("Enviar ${plan.requests.size} cenas ao projeto Gemini selecionado? " +
                "O processamento é assíncrono, pode ser cobrado e não será repetido após erro ou perda de conexão. " +
                "Somente cena e contexto textual são enviados.") },
            confirmButton = { Button(onClick = {
                confirmSubmission = null
                if (onBeforeSend()) scope.launch {
                    busy = true; message = ""
                    try {
                        val p = currentProfile() ?: error("Chave Gemini indisponível.")
                        require(p.projectId.isNotBlank())
                        val key = credentials.readProfile(p.id) ?: error("Chave indisponível.")
                        val pending = storage.start(p.id, p.projectId, submittedModel,
                            plan.requests.size, projectId, plan.scenes)
                        reload()
                        val name = client.createRequests(key, submittedModel, plan.requests)
                        storage.accept(pending, name)
                        reload()
                        message = "Lote enviado: $name. Consulte manualmente mais tarde."
                    } catch (c: CancellationException) { throw c }
                    catch (e: Exception) {
                        reload()
                        message = (e.message ?: "Falha no envio.") +
                            " Se a chamada começou, consulte os jobs antes de enviar outro lote."
                    } finally { busy = false }
                } else message = "Salve o rascunho antes de enviar."
            }) { Text("Enviar uma vez") } },
            dismissButton = { TextButton(onClick = { confirmSubmission = null }) { Text("Cancelar") } })
    }
    pendingLink?.let { (record, remote) ->
        AlertDialog(onDismissRequest = { pendingLink = null },
            title = { Text("Associar job incerto?") },
            text = { Text("Confirme no Google que $remote é exatamente o job enviado para este conjunto. " +
                "Vincular um job diferente pode associar respostas a cenas erradas.") },
            confirmButton = { Button(onClick = {
                pendingLink = null
                message = try {
                    val p = currentProfile()
                    require(p?.id == record.profileId && p.projectId == record.projectId) {
                        "Selecione o perfil original do envio."
                    }
                    storage.accept(record, remote); reload()
                    "ID associado. Consulte o resultado antes de aplicar."
                } catch (e: Exception) { e.message ?: "Não foi possível associar." }
            }) { Text("Confirmar vínculo") } },
            dismissButton = { TextButton(onClick = { pendingLink = null }) { Text("Cancelar") } })
    }
    pendingApply?.let { (index, answer) ->
        AlertDialog(onDismissRequest = { pendingApply = null },
            title = { Text("Revisar sugestão · cena ${index + 1}") },
            text = { Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                Text(answer)
            } },
            confirmButton = { Button(onClick = {
                val data = inspected
                val ref = data?.first?.flowScenes?.firstOrNull { it.index == index }
                val valid = data != null && ref != null && FlowBatchPolicy.mayApply(draft, ref, answer) &&
                    data.second.state == "JOB_STATE_SUCCEEDED" &&
                    FlowBatchPolicy.resultFor(data.second, data.first.flowScenes, ref) == answer
                if (valid) {
                    onApply(index, answer)
                    message = "Sugestão aplicada à cena ${index + 1}. Salve o projeto para preservar."
                } else message = "Cena modificada ou resultado inválido: sugestão não aplicada."
                pendingApply = null
            }) { Text("Aplicar nesta cena") } },
            dismissButton = { TextButton(onClick = { pendingApply = null }) { Text("Manter original") } })
    }
}
