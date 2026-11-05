package br.com.uaicortes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Ferramenta independente de texto, opt-in, sem alterar Vozes IA, Flow ou projetos. */
@Composable
internal fun GeminiBatchSection() {
    val ctx = LocalContext.current
    val credentials = remember(ctx) { GeminiCredentialStore(ctx) }
    val storage = remember(ctx) { GeminiBatchStore(ctx) }
    val client = remember { GeminiBatchClient() }
    val scope = rememberCoroutineScope()
    DisposableEffect(client) { onDispose { client.cancel() } }
    var expanded by remember { mutableStateOf(false) }
    var model by remember { mutableStateOf("gemini-2.5-flash") }
    var textInput by remember { mutableStateOf("") }
    var linkedName by remember { mutableStateOf("") }
    var acknowledgePaid by remember { mutableStateOf(false) }
    var confirmSubmit by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var info by remember { mutableStateOf("") }
    var records by remember { mutableStateOf(storage.list()) }
    var inspected by remember { mutableStateOf<GeminiBatchStatus?>(null) }
    var remoteJobs by remember { mutableStateOf<List<String>>(emptyList()) }
    fun reload() { records = storage.list() }
    fun profile(): GeminiKeyProfile? {
        val id = credentials.activeId() ?: return null
        return credentials.profiles().firstOrNull { it.id == id }
    }
    Card(shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth()) {
                Text(if (expanded) "▾ Lotes econômicos Gemini · texto" else "▸ Lotes econômicos Gemini · texto")
            }
            if (!expanded) return@Column
            Text("Módulo opcional Batch para tarefas de TEXTO não urgentes, com modelos compatíveis " +
                "e faturamento habilitado. O processamento é assíncrono, pode levar até 24 h e tem " +
                "cotas próprias. Nenhuma narração WAV, legenda ou vídeo é alterado.",
                style = MaterialTheme.typography.bodySmall)
            Text("Os pedidos só são enviados após confirmação. Não há repetição automática. " +
                "O histórico local guarda IDs de jobs, mas nunca pedidos, respostas ou chaves.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(value = model, onValueChange = { model = it.take(100) },
                label = { Text("Modelo Gemini de texto") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = textInput, onValueChange = { textInput = it.take(8001) },
                label = { Text("2 a 8 pedidos — um por linha") }, minLines = 4,
                maxLines = 8, modifier = Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Checkbox(checked = acknowledgePaid, onCheckedChange = { acknowledgePaid = it })
                Text("Entendo que o Batch pode gerar cobrança no meu projeto e que a " +
                    "disponibilidade depende do modelo/plano.", modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall)
            }
            Button(onClick = {
                info = try {
                    GeminiBatchPolicy.model(model)
                    GeminiBatchPolicy.prompts(textInput)
                    val p = profile() ?: error("Cadastre uma chave Gemini ativa.")
                    require(p.projectId.isNotBlank()) { "Preencha o ID do projeto da chave antes de enviar." }
                    require(acknowledgePaid) { "Confirme que compreende o possível custo." }
                    confirmSubmit = true
                    ""
                } catch (e: IllegalArgumentException) { e.message.orEmpty() }
                catch (e: IllegalStateException) { e.message.orEmpty() }
            }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Revisar e enviar lote de texto") }
            if (info.isNotBlank()) Text(info, style = MaterialTheme.typography.bodySmall)
            if (busy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            HorizontalDivider()
            Text("Consultar ou recuperar job", style = MaterialTheme.typography.titleSmall)
            Text("Se a conexão caiu durante um envio, consulte seus jobs no Google antes " +
                "de criar outro. O mesmo pedido pode ter sido aceito.",
                style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(value = linkedName, onValueChange = { linkedName = it.take(140) },
                label = { Text("Nome retornado pela API: batches/...") },
                singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedButton(onClick = {
                val p = profile()
                info = if (p == null || p.projectId.isBlank()) "Selecione uma chave com ID do projeto."
                else try {
                    storage.attach(p.id, p.projectId, GeminiBatchPolicy.model(model), linkedName)
                    reload(); "Job associado localmente. Consulte o status para confirmar."
                } catch (e: Exception) { e.message ?: "Não foi possível registrar o job." }
            }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Vincular job existente") }
            OutlinedButton(onClick = {
                scope.launch {
                    busy = true; info = ""
                    try {
                        val p = profile() ?: error("Selecione uma chave Gemini ativa.")
                        val key = credentials.readProfile(p.id) ?: error("Chave indisponível.")
                        remoteJobs = client.listJobs(key)
                        info = if (remoteJobs.isEmpty()) "Nenhum job retornado para esta chave." else
                            "Jobs encontrados: ${remoteJobs.size}. Selecione o correspondente ao envio incerto."
                    } catch (e: CancellationException) { throw e }
                    catch (e: Exception) { info = e.message ?: "Não foi possível consultar jobs." }
                    finally { busy = false }
                }
            }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Buscar jobs na API Gemini") }
            remoteJobs.take(12).forEach { remote ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SelectionContainer {
                        Text(remote, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick = {
                        val p = profile()
                        info = if (p == null || p.projectId.isBlank()) "Selecione um perfil com projeto."
                        else try {
                            storage.attach(p.id, p.projectId, GeminiBatchPolicy.model(model), remote)
                            reload(); "Job vinculado. Use Consultar para ver o resultado."
                        } catch (e: Exception) { e.message ?: "Falha ao vincular." }
                    }, enabled = !busy) { Text("Vincular") }
                }
            }
            records.asReversed().take(15).forEach { r ->
                Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                    Column(Modifier.fillMaxWidth().padding(9.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SelectionContainer {
                            Text(r.remoteName.ifBlank { "Envio sem resposta confirmada" },
                                style = MaterialTheme.typography.labelMedium)
                        }
                        Text("Projeto ${r.projectId} · ${r.model} · ${r.count} pedido(s)" +
                            " · ${GeminiBatchPolicy.status(r.state)}",
                            style = MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(onClick = {
                                val p = profile()
                                if (p?.id != r.profileId) {
                                    info = "Selecione a mesma chave/perfil que criou este job."
                                } else if (r.remoteName.isBlank()) {
                                    info = "Sem ID remoto: localize o job na API antes de repetir o envio."
                                } else {
                                    scope.launch {
                                        busy = true; info = ""
                                        try {
                                            val key = credentials.readProfile(r.profileId)
                                                ?: error("Chave original indisponível.")
                                            val result = client.status(key, r.remoteName)
                                            storage.update(r, result.state)
                                            inspected = result
                                            reload()
                                            info = "Consulta concluída."
                                        } catch (e: CancellationException) { throw e }
                                        catch (e: Exception) { info = e.message ?: "Falha ao consultar." }
                                        finally { busy = false }
                                    }
                                }
                            }, enabled = !busy && r.remoteName.isNotBlank()) { Text("Consultar") }
                            TextButton(onClick = { storage.forget(r); reload(); info = "Removido só deste aparelho." },
                                enabled = !busy) { Text("Ocultar") }
                        }
                    }
                }
            }
            inspected?.let { result ->
                Text("Resultado de ${result.name}: ${GeminiBatchPolicy.status(result.state)}",
                    style = MaterialTheme.typography.titleSmall)
                if (result.message.isNotBlank()) Text(result.message, style = MaterialTheme.typography.bodySmall)
                result.results.forEach { (label, content) ->
                    Text(label, style = MaterialTheme.typography.labelMedium)
                    SelectionContainer {
                        Text(content, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Text("Resultados exibidos apenas nesta sessão. Copie o texto desejado antes de fechar.",
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }
    if (confirmSubmit) {
        val count = runCatching { GeminiBatchPolicy.prompts(textInput).size }.getOrDefault(0)
        AlertDialog(onDismissRequest = { confirmSubmit = false },
            title = { Text("Confirmar Batch pago") },
            text = { Text("Enviar $count pedidos de texto ao Google Gemini? A operação pode gerar cobrança, " +
                "não tem resultado imediato e NÃO será repetida automaticamente em falhas de rede.") },
            confirmButton = { Button(onClick = {
                confirmSubmit = false
                scope.launch {
                    busy = true; info = ""
                    try {
                        val p = profile() ?: error("Chave não cadastrada.")
                        require(p.projectId.isNotBlank())
                        val prompts = GeminiBatchPolicy.prompts(textInput)
                        val id = GeminiBatchPolicy.model(model)
                        val key = credentials.readProfile(p.id) ?: error("Chave indisponível.")
                        // Persistir estado incerto ANTES de tocar na rede. Não criar o job novamente.
                        val pending = storage.start(p.id, p.projectId, id, prompts.size)
                        reload()
                        val remote = client.create(key, id, prompts)
                        storage.accept(pending, remote)
                        reload()
                        textInput = ""
                        info = "Job enviado: $remote. Consulte quando desejar; processamento assíncrono."
                    } catch (e: CancellationException) { throw e }
                    catch (e: Exception) {
                        reload()
                        info = (e.message ?: "Falha de rede.") +
                            " Se o envio foi iniciado, confira os jobs antes de reenviar."
                    } finally { busy = false }
                }
            }) { Text("Enviar uma vez") } },
            dismissButton = { TextButton(onClick = { confirmSubmit = false }) { Text("Cancelar") } })
    }
}
