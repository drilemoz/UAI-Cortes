package br.com.uaicortes

import android.content.ClipDescription
import android.content.Context
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Chaves de backup são gerenciadas localmente, sem envio de credenciais a servidores próprios. */
@Composable
internal fun GeminiKeyPoolSettings(store: GeminiCredentialStore, onChanged: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var expanded by remember { mutableStateOf(false) }
    var entries by remember { mutableStateOf(runCatching { store.profiles() }.getOrDefault(emptyList())) }
    var activeId by remember { mutableStateOf(store.activeId()) }
    var auto by remember { mutableStateOf(store.autoFallbackEnabled()) }
    var busy by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf("") }
    var label by remember { mutableStateOf("") }
    var project by remember { mutableStateOf("") }
    var secret by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<GeminiKeyProfile?>(null) }
    var confirmDelete by remember { mutableStateOf<GeminiKeyProfile?>(null) }
    var confirmAuto by remember { mutableStateOf(false) }
    fun refresh() {
        entries = runCatching { store.profiles() }.getOrDefault(emptyList())
        activeId = store.activeId()
        auto = store.autoFallbackEnabled()
        onChanged()
    }
    Surface(shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Row(Modifier.fillMaxWidth().clickable { expanded = !expanded; refresh() }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.VpnKey, null, modifier = Modifier.size(19.dp),
                tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Chaves Gemini", style = MaterialTheme.typography.titleSmall)
                Text("${entries.size}/8 protegidas · Limites por projeto Google Cloud",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                if (expanded) "Recolher chaves" else "Gerenciar chaves", Modifier.size(20.dp))
        }
    }
    if (!expanded) return
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        store.lastAutomaticSwitch()?.let { name ->
            Text("Última troca automática: $name", color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodySmall)
        }
        if (entries.isEmpty()) Text("Adicione sua primeira chave para usar os recursos de IA.")
        entries.forEach { profile ->
            Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), shape = MaterialTheme.shapes.medium) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(profile.label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                        if (profile.id == activeId) AssistChip(onClick = {}, enabled = false,
                            label = { Text("Ativa") })
                    }
                    Text("${profile.masked}  ·  Projeto: ${profile.projectId.ifEmpty { "não informado" }}",
                        style = MaterialTheme.typography.bodySmall)
                    if (store.temporarilyBlocked(profile.projectId)) Text(
                        "Projeto em pausa após limite 429. Tente novamente mais tarde.",
                        color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = {
                            runCatching { store.select(profile.id); refresh(); feedback = "Chave ativa alterada." }
                                .onFailure { feedback = "Não foi possível selecionar a chave." }
                        }, enabled = !busy && profile.id != activeId) { Text("Usar") }
                        TextButton(onClick = { editing = profile; label = profile.label; project = profile.projectId; secret = "" },
                            enabled = !busy) { Text("Editar") }
                        TextButton(onClick = { confirmDelete = profile }, enabled = !busy) {
                            Icon(Icons.Default.DeleteOutline, null, Modifier.size(17.dp)); Text("Excluir")
                        }
                    }
                }
            }
        }
        if (editing != null || entries.size < 8) {
            Text(if (editing == null) "Adicionar chave" else "Editar identificação da chave",
                style = MaterialTheme.typography.titleSmall)
            OutlinedTextField(value = label, onValueChange = { label = it.take(48) }, modifier = Modifier.fillMaxWidth(),
                singleLine = true, label = { Text("Nome (ex.: Projeto vídeos)") })
            OutlinedTextField(value = project, onValueChange = { project = it.take(100) }, modifier = Modifier.fillMaxWidth(),
                singleLine = true, label = { Text("ID do projeto Google Cloud") },
                supportingText = { Text("Obrigatório para troca automática. Use o ID real do projeto associado à chave.") })
            if (editing == null) OutlinedTextField(value = secret, onValueChange = { secret = it.take(512) },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                label = { Text("Nova chave Gemini") },
                visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = { IconButton(onClick = { visible = !visible }) {
                    Icon(if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility, "Mostrar/ocultar")
                } })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    if (editing == null) {
                        val text = (ctx.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager)
                            .primaryClip?.getItemAt(0)?.coerceToText(ctx)?.toString().orEmpty()
                        secret = text.take(512)
                    } else { editing = null; label = ""; project = "" }
                }) { Text(if (editing == null) "Colar" else "Cancelar") }
                Button(onClick = {
                    try {
                        if (editing == null) {
                            store.add(secret, label, project)
                            feedback = "Chave adicional protegida. Nenhuma consulta foi feita à API."
                        } else {
                            store.rename(editing!!.id, label, project)
                            feedback = "Identificação atualizada."
                        }
                        editing = null; secret = ""; label = ""; project = ""; visible = false
                        refresh()
                    } catch (error: Exception) {
                        feedback = error.message?.take(140) ?: "Falha ao salvar configuração."
                    }
                }, enabled = !busy && label.isNotBlank() && (editing != null || secret.isNotBlank())) {
                    Icon(Icons.Default.Save, null, Modifier.size(17.dp))
                    Spacer(Modifier.width(5.dp)); Text(if (editing == null) "Adicionar" else "Salvar")
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Alternar após limite de uso", style = MaterialTheme.typography.titleSmall)
                Text("Somente entre projetos distintos configurados. Desativado até você autorizar.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = auto, onCheckedChange = { selected ->
                if (selected) confirmAuto = true else {
                    runCatching { store.setAutoFallback(false); refresh() }
                        .onFailure { feedback = "Não foi possível desligar a alternância." }
                }
            }, enabled = !busy && entries.map { it.projectId }.filter { it.isNotBlank() }.distinct().size >= 2)
        }
        Text("Um erro HTTP 429 pode ser limite por minuto, tokens ou dia — não comprova créditos esgotados. " +
            "O app pausa temporariamente o projeto e tenta, no máximo uma vez por outro projeto autorizado. " +
            "Não resolve bloqueios de faturamento, não cria chaves e não ultrapassa cotas de um mesmo projeto.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedButton(onClick = {
            busy = true
            scope.launch {
                try {
                    val current = withContext(Dispatchers.IO) { store.read() }
                        ?: error("Nenhuma chave ativa.")
                    val models = GeminiVideoCaptionClient().listModels(current)
                    feedback = "Conexão válida. ${models.size} modelo(s) de texto disponíveis para a chave ativa."
                } catch (cancel: CancellationException) { throw cancel }
                catch (e: Exception) { feedback = "Falha no teste da chave ativa: ${e.message?.take(120) ?: "erro desconhecido"}" }
                finally { busy = false }
            }
        }, enabled = !busy && activeId != null) {
            Icon(Icons.Default.NetworkCheck, null, Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp)); Text("Testar chave ativa (sem geração)")
        }
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (feedback.isNotBlank()) Text(feedback, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    confirmDelete?.let { item -> AlertDialog(onDismissRequest = { confirmDelete = null },
        title = { Text("Excluir ${item.label}?") },
        text = { Text("A credencial protegida será apagada deste aparelho. Projetos e arquivos gerados não serão excluídos.") },
        confirmButton = { TextButton(onClick = {
            runCatching { store.removeProfile(item.id); refresh(); feedback = "Chave removida." }
                .onFailure { feedback = "Falha ao excluir chave." }
            confirmDelete = null
        }) { Text("Excluir") } },
        dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("Cancelar") } }) }
    if (confirmAuto) AlertDialog(onDismissRequest = { confirmAuto = false },
        title = { Text("Ativar alternância automática?") },
        text = { Text("Após erro 429, uma nova tentativa poderá consumir a cota ou gerar cobranças " +
            "em OUTRO projeto Google Cloud autorizado. Identifique corretamente os IDs dos projetos. " +
            "Chaves de um mesmo projeto compartilham limites. Confirme que o uso está dentro dos termos e " +
            "do orçamento de cada projeto. Nenhuma tentativa ocorre em segundo plano.") },
        confirmButton = { TextButton(onClick = {
            runCatching { store.setAutoFallback(true); refresh() }
                .onFailure { feedback = "Falha ao ativar a alternância." }
            confirmAuto = false
        }) { Text("Ativar") } },
        dismissButton = { TextButton(onClick = { confirmAuto = false }) { Text("Cancelar") } })
}
