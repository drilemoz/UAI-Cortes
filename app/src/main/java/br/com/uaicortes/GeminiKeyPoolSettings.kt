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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
    var formOpen by remember { mutableStateOf(false) }
    var explainLimits by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf<GeminiKeyProfile?>(null) }
    var confirmAuto by remember { mutableStateOf(false) }
    fun refresh() {
        entries = runCatching { store.profiles() }.getOrDefault(emptyList())
        activeId = store.activeId()
        auto = store.autoFallbackEnabled()
        onChanged()
    }
    Surface(shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().clickable { expanded = !expanded; refresh() }
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(10.dp)) {
                    Icon(Icons.Default.VpnKey, null, modifier = Modifier.padding(9.dp).size(20.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Minhas chaves", style = MaterialTheme.typography.titleSmall)
                    Text("${entries.size}/8 protegidas · Limites por projeto Google Cloud",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    if (expanded) "Recolher chaves" else "Gerenciar chaves", Modifier.size(21.dp))
            }
            if (expanded) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Column(Modifier.fillMaxWidth().padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    store.lastAutomaticSwitch()?.let { name ->
                        Text("Última troca automática: $name", color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodySmall)
                    }
                    if (entries.isEmpty()) Surface(color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp)) {
                        Text("Nenhuma credencial cadastrada. Adicione uma chave para começar.",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall)
                    }
                    entries.forEach { profile ->
                        val active = profile.id == activeId
                        Surface(shape = RoundedCornerShape(12.dp),
                            color = if (active) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.34f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.33f),
                            border = BorderStroke(if (active) 1.5.dp else 1.dp,
                                if (active) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant)) {
                            Column(Modifier.fillMaxWidth().padding(11.dp),
                                verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                    Icon(Icons.Default.Key, null, Modifier.size(17.dp),
                                        tint = MaterialTheme.colorScheme.primary)
                                    Text(profile.label, modifier = Modifier.weight(1f),
                                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.titleSmall)
                                    if (active) Surface(shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.primary) {
                                        Text("ATIVA", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                                Text("${profile.masked} · Projeto: ${profile.projectId.ifEmpty { "não informado" }}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2)
                                if (store.temporarilyBlocked(profile.projectId)) Text(
                                    "Projeto em pausa após limite 429. Tente novamente mais tarde.",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall)
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedButton(onClick = {
                                        runCatching { store.select(profile.id); refresh(); feedback = "Chave ativa alterada." }
                                            .onFailure { feedback = "Não foi possível selecionar a chave." }
                                    }, enabled = !busy && !active,
                                        modifier = Modifier.weight(1f).heightIn(min = 40.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 5.dp)) {
                                        Text(if (active) "Em uso" else "Usar", fontSize = 12.sp)
                                    }
                                    OutlinedButton(onClick = {
                                        editing = profile; label = profile.label
                                        project = profile.projectId; secret = ""; formOpen = true
                                    }, enabled = !busy,
                                        modifier = Modifier.weight(1f).heightIn(min = 40.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 5.dp)) {
                                        Text("Editar", fontSize = 12.sp)
                                    }
                                    TextButton(onClick = { confirmDelete = profile }, enabled = !busy,
                                        modifier = Modifier.weight(1f).heightIn(min = 40.dp),
                                        contentPadding = PaddingValues(horizontal = 4.dp)) {
                                        Icon(Icons.Default.DeleteOutline, null, Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.error)
                                        Spacer(Modifier.width(3.dp))
                                        Text("Excluir", fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                    if (entries.size < 8) OutlinedButton(onClick = {
                        editing = null; label = ""; project = ""; secret = ""
                        visible = false; formOpen = true
                    }, modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp),
                        shape = RoundedCornerShape(9.dp)) {
                        Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp)); Text("Adicionar chave")
                    }
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
                        shape = RoundedCornerShape(11.dp)) {
                        Column(Modifier.fillMaxWidth().padding(11.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("Alternância após limite 429", style = MaterialTheme.typography.titleSmall)
                                    Text("Somente entre projetos Google Cloud distintos",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(checked = auto, onCheckedChange = { selected ->
                                    if (selected) confirmAuto = true else {
                                        runCatching { store.setAutoFallback(false); refresh() }
                                            .onFailure { feedback = "Não foi possível desligar a alternância." }
                                    }
                                }, enabled = !busy && entries.map { it.projectId }
                                    .filter { it.isNotBlank() }.distinct().size >= 2)
                            }
                            Row(Modifier.fillMaxWidth().clickable { explainLimits = !explainLimits }
                                    .padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("Entender limites e segurança", Modifier.weight(1f),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary)
                                Icon(if (explainLimits) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    null, Modifier.size(18.dp))
                            }
                            if (explainLimits) Text(
                                "Um erro HTTP 429 pode ser limite por minuto, tokens ou dia — não comprova créditos esgotados. " +
                                "O app pausa temporariamente o projeto e tenta, no máximo uma vez por outro projeto autorizado. " +
                                "Não resolve bloqueios de faturamento, não cria chaves e não ultrapassa cotas de um mesmo projeto.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
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
                    }, enabled = !busy && activeId != null,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp),
                        shape = RoundedCornerShape(9.dp)) {
                        Icon(Icons.Default.NetworkCheck, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp)); Text("Testar chave ativa (sem geração)")
                    }
                    if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                    if (feedback.isNotBlank()) Text(feedback, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
    // Edição e cadastro deixam de expandir todo o formulário no meio da página.
    if (formOpen) Dialog(onDismissRequest = { formOpen = false; editing = null; secret = "" },
        properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxWidth(0.94f).imePadding().heightIn(max = 570.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VpnKey, null, Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(9.dp))
                    Text(if (editing == null) "Adicionar chave Gemini" else "Editar chave Gemini",
                        Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = { formOpen = false; editing = null; secret = "" }) {
                        Icon(Icons.Default.Close, "Fechar")
                    }
                }
                Text("A credencial permanece protegida no dispositivo. O ID do projeto é necessário para alternância automática.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(value = label, onValueChange = { label = it.take(48) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    shape = RoundedCornerShape(9.dp), label = { Text("Nome da chave") })
                OutlinedTextField(value = project, onValueChange = { project = it.take(100) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    shape = RoundedCornerShape(9.dp), label = { Text("Projeto Google Cloud (ID)") })
                if (editing == null) {
                    OutlinedTextField(value = secret, onValueChange = { secret = it.take(512) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        shape = RoundedCornerShape(9.dp), label = { Text("Nova chave Gemini") },
                        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = { IconButton(onClick = { visible = !visible }) {
                            Icon(if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                "Mostrar/ocultar chave digitada")
                        } })
                    TextButton(onClick = {
                        val text = (ctx.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager)
                            .primaryClip?.getItemAt(0)?.coerceToText(ctx)?.toString().orEmpty()
                        secret = text.take(512)
                    }) {
                        Icon(Icons.Default.ContentPaste, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(5.dp)); Text("Colar chave")
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        formOpen = false; editing = null; secret = ""
                    }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(9.dp)) {
                        Text("Cancelar")
                    }
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
                            formOpen = false; refresh()
                        } catch (error: Exception) {
                            feedback = error.message?.take(140) ?: "Falha ao salvar configuração."
                        }
                    }, enabled = !busy && label.isNotBlank() && (editing != null || secret.isNotBlank()),
                        modifier = Modifier.weight(1f), shape = RoundedCornerShape(9.dp)) {
                        Text(if (editing == null) "Adicionar" else "Salvar")
                    }
                }
                if (feedback.isNotBlank()) Text(feedback,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
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
