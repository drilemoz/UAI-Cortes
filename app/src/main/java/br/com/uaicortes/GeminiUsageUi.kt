package br.com.uaicortes

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Estimativa local por operação/modelo; não confunde média com saldo oficial. */
@Composable
internal fun GeminiUsageHint(operation: String, inputChars: Int? = null,
    durationMs: Long? = null, model: String = "", requestedCount: Int = 1) {
    val context = LocalContext.current
    val ledger = remember(context) { GeminiUsageLedger(context) }
    val quota = remember(context) { GeminiQuotaStore(context) }
    val prices = remember(context) { GeminiPriceStore(context) }
    val keys = remember(context) { GeminiCredentialStore(context) }
    val activeProject = runCatching { keys.profiles().firstOrNull { it.id == keys.activeId() }?.projectId.orEmpty() }
        .getOrDefault("")
    val forecast = remember(operation, model, activeProject, requestedCount) {
        if (model.isBlank() || activeProject.isBlank()) null else runCatching {
            GeminiQuotaPolicy.compute(ledger.events(), activeProject, model, operation,
                quota.get(activeProject, model))
        }.getOrNull()
    }
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text("Consumo da IA · $operation", style = MaterialTheme.typography.labelMedium)
        if (model.isNotBlank()) Text("Modelo: $model", style = MaterialTheme.typography.bodySmall)
        if (inputChars != null && inputChars > 0) Text(GeminiUsagePolicy.estimateText(inputChars),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (durationMs != null) Text("Áudio de até ${((durationMs + 59_999) / 60_000).coerceAtLeast(1)} min; " +
            "tokens de áudio e resposta não podem ser previstos com segurança.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (model.isNotBlank() && activeProject.isNotBlank()) {
            val avgPrice = GeminiAnalyticsPolicy.estimatedBatchCost(ledger.events(), activeProject,
                model, operation, requestedCount.coerceIn(1, 500), prices.get(activeProject, model))
            if (avgPrice != null) Text("Lote semelhante: custo local estimado ~US$ " +
                "%.5f".format(Locale.US, avgPrice) +
                " (preços manuais e cinco medições completas).",
                style = MaterialTheme.typography.bodySmall)
        }
        if (forecast != null) {
            forecast.averageTotal?.let { average ->
                val total = average * requestedCount.coerceIn(1, 100)
                Text("Média observada: $average tokens/geração · ~${total} tokens para " +
                    "${requestedCount.coerceAtLeast(1)} solicitação(ões) semelhantes.",
                    style = MaterialTheme.typography.bodySmall)
            } ?: Text("Média: aguardando pelo menos 5 medições reais desta função e modelo.",
                style = MaterialTheme.typography.bodySmall)
            val paused = keys.temporarilyBlocked(activeProject, model)
            if (paused) Text("Projeto em pausa temporária após 429 para este modelo.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error)
            if (!paused) {
                forecast.remainingNow?.let { nowRemaining ->
                    Text("Agora: até $nowRemaining solicitação(ões) na janela local atual.",
                        style = MaterialTheme.typography.bodySmall)
                    if (requestedCount > nowRemaining) Text(
                        "O lote excede a capacidade imediata estimada; a fila aguarda o RPM " +
                            "quando esse limite estiver configurado.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error)
                }
                forecast.remainingToday?.let { daily ->
                    Text("Hoje (Pacífico): até $daily tentativa(s) restantes neste aparelho, " +
                        "segundo RPD manual.", style = MaterialTheme.typography.bodySmall)
                    if (requestedCount > daily) Text("O lote excede o restante diário local informado. " +
                        "As falas concluídas podem ser retomadas depois.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error)
                }
                forecast.nextRpmWindowMs?.let { wait ->
                    Text("Próxima liberação RPM local em ~${(wait + 999L) / 1000L} s.",
                        style = MaterialTheme.typography.labelSmall)
                }
            }
            Text(forecast.label, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else Text("Previsão indisponível: configure o ID do projeto e o modelo.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (keys.autoFallbackEnabled()) Text("Troca de chave: ativa apenas entre projetos distintos autorizados após limite 429.",
            style = MaterialTheme.typography.labelSmall)
        Text("Estimativas locais não indicam créditos oficiais; confirme as cotas no Google AI Studio.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Resumo limpo por padrão; dados técnicos permanecem acessíveis por expansão/ajuda. */
@Composable
internal fun GeminiUsagePanel() {
    val context = LocalContext.current
    val ledger = remember(context) { GeminiUsageLedger(context) }
    val keys = remember(context) { GeminiCredentialStore(context) }
    val quotas = remember(context) { GeminiQuotaStore(context) }
    val prices = remember(context) { GeminiPriceStore(context) }
    var expanded by remember { mutableStateOf(false) }
    var detailsExpanded by remember { mutableStateOf(false) }
    var period by remember { mutableIntStateOf(1) }
    var entries by remember { mutableStateOf(emptyList<GeminiUsageEvent>()) }
    var confirmClear by remember { mutableStateOf(false) }
    var configuringProject by remember { mutableStateOf<String?>(null) }
    var configuringModel by remember { mutableStateOf("") }
    var revision by remember { mutableIntStateOf(0) }
    fun refresh() { entries = runCatching { ledger.events() }.getOrDefault(emptyList()) }

    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.fillMaxWidth().padding(11.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(shape = RoundedCornerShape(9.dp),
                    color = MaterialTheme.colorScheme.primaryContainer) {
                    Icon(Icons.Default.BarChart, null, Modifier.padding(9.dp).size(19.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Column(Modifier.weight(1f).clickable {
                    expanded = !expanded
                    if (expanded) refresh()
                }, verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text("Consumo da IA", style = MaterialTheme.typography.titleSmall)
                    Text("Histórico local · Resumo", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                GeminiUsageHelpButton("Consumo e cotas",
                    "O histórico é local e registra até 500 tentativas de 90 dias. " +
                    "Tokens e durações só aparecem quando medidos pela API. " +
                    "Erros 429 podem indicar limite por minuto, tokens ou dia. " +
                    "As estimativas não representam saldo nem fatura oficial do Google.")
                IconButton(onClick = {
                    expanded = !expanded
                    if (expanded) refresh()
                }, modifier = Modifier.size(38.dp)) {
                    Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        if (expanded) "Recolher consumo" else "Ver consumo")
                }
            }
            if (expanded) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for ((days, label) in listOf(1 to "24 h", 7 to "7 dias", 30 to "30 dias")) {
                        FilterChip(selected = period == days, onClick = { period = days },
                            modifier = Modifier.weight(1f).heightIn(min = 38.dp),
                            shape = RoundedCornerShape(8.dp),
                            label = { Text(label, maxLines = 1, fontSize = 12.sp) })
                    }
                }
                val cutoff = System.currentTimeMillis() - period * 24L * 60L * 60L * 1000L
                val filtered = entries.filter { it.at >= cutoff }
                val generated = filtered.count { it.status == "ok" }
                val total = filtered.mapNotNull { it.tokens?.total }.sum()
                val limits = filtered.count { it.status == "429" }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GeminiUsageMetric("Aceitas", generated.toString(), Modifier.weight(1f))
                    GeminiUsageMetric("Tokens", "%,d".format(Locale("pt", "BR"), total), Modifier.weight(1f))
                    GeminiUsageMetric("Avisos 429", limits.toString(), Modifier.weight(1f),
                        warning = limits > 0)
                }
                if (filtered.isEmpty()) Text("Sem operações neste período.",
                    style = MaterialTheme.typography.bodySmall)
                key(revision) { GeminiAnalyticsSection(filtered, prices, period) }
                HorizontalDivider()
                Row(verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()) {
                    Text("Modelos e limites", style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f))
                    GeminiUsageHelpButton("Estimativas por modelo",
                        "O mesmo projeto compartilha limites entre as chaves. " +
                        "Agora indica a janela RPM/TPM; Hoje considera RPD no horário do Pacífico. " +
                        "Apenas operações deste aparelho entram nas estimativas. " +
                        "Informe cotas específicas no Google AI Studio; não são obtidas pela chave da API.")
                }
                val profiles = runCatching { keys.profiles() }.getOrDefault(emptyList())
                val projects = profiles.map { it.projectId }.filter { it.isNotBlank() }.distinct()
                if (projects.isEmpty()) Text("Adicione o ID do projeto em Minhas chaves.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                projects.forEach { project ->
                    Surface(shape = RoundedCornerShape(9.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.33f)) {
                        Column(Modifier.fillMaxWidth().padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Projeto: $project", style = MaterialTheme.typography.labelMedium,
                                maxLines = 1)
                            // Apenas modelos realmente usados por este projeto, não três defaults inativos.
                            val models = entries.asSequence().filter { it.project == project }
                                .map { it.model }.filter { it.isNotBlank() }.distinct().take(12).toList()
                            if (models.isEmpty()) Text("Sem modelos usados neste aparelho.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            models.forEach { model ->
                                key(model, revision) {
                                    val local = entries.filter { it.project == project && it.model == model }
                                    val operation = local.lastOrNull()?.operation ?: "narracao"
                                    val limit = quotas.get(project, model)
                                    val estimate = GeminiQuotaPolicy.compute(entries, project, model,
                                        operation, limit)
                                    val paused = keys.temporarilyBlocked(project, model)
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Column(Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(GeminiPublicPriceCatalog.displayName(model),
                                                style = MaterialTheme.typography.labelMedium, maxLines = 1)
                                            val state = when {
                                                paused -> "Aguardando após erro 429"
                                                !limit.configured() -> "Limites não informados"
                                                else -> "Agora: ${estimate.remainingNow?.toString() ?: "?"}" +
                                                    " · Hoje: ${estimate.remainingToday?.toString() ?: "?"}"
                                            }
                                            Text(state, style = MaterialTheme.typography.bodySmall,
                                                color = if (paused) MaterialTheme.colorScheme.error
                                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 2)
                                        }
                                        OutlinedButton(onClick = {
                                            configuringProject = project; configuringModel = model
                                        }, shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 9.dp),
                                            modifier = Modifier.heightIn(min = 40.dp)) {
                                            Text("Configurar", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                            TextButton(onClick = {
                                configuringProject = project; configuringModel = ""
                            }, modifier = Modifier.align(Alignment.End)) {
                                Text("Adicionar modelo", fontSize = 12.sp)
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().clickable { detailsExpanded = !detailsExpanded }
                        .padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Detalhes avançados", Modifier.weight(1f),
                        style = MaterialTheme.typography.titleSmall)
                    Icon(if (detailsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        if (detailsExpanded) "Recolher detalhes" else "Mostrar detalhes", Modifier.size(20.dp))
                }
                if (detailsExpanded) {
                    Text("Modelos e últimas operações", style = MaterialTheme.typography.labelMedium)
                    for ((project, events) in filtered.groupBy { it.project }) {
                        Text("${project.ifBlank { "Projeto não informado" }}: " +
                            "${events.size} tentativas · " +
                            "${events.mapNotNull { it.tokens?.total }.sum()} tokens registrados",
                            style = MaterialTheme.typography.bodySmall)
                        for ((model, group) in events.groupBy { it.model }) {
                            Text("${model.ifBlank { "Modelo desconhecido" }} · ${group.size} tentativa(s)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Text("Consumo por função e classificação de falhas", style = MaterialTheme.typography.labelMedium)
                    for ((operation, events) in filtered.groupBy { it.operation }.toSortedMap()) {
                        Text("$operation: ${events.count { it.status == "ok" }} concluídas · " +
                            "${events.count { it.status != "ok" }} erros",
                            style = MaterialTheme.typography.bodySmall)
                    }
                    Text("Últimas operações", style = MaterialTheme.typography.labelMedium)
                    val time = remember { SimpleDateFormat("dd/MM HH:mm", Locale("pt", "BR")) }
                    filtered.takeLast(5).asReversed().forEach { event ->
                        val statusLabel = if (event.status == "ok") "OK" else GeminiOperationFailure.label(
                            GeminiOperationFailure.fromHttp(
                                event.status.removePrefix("erro_").toIntOrNull() ?: -1))
                        Text("${time.format(Date(event.at))} · ${event.model} · " +
                            "$statusLabel · " +
                            "${event.tokens?.total?.toString() ?: "tokens não informados"}",
                            style = MaterialTheme.typography.bodySmall)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = { refresh() },
                            modifier = Modifier.weight(1f).heightIn(min = 40.dp),
                            shape = RoundedCornerShape(8.dp)) { Text("Atualizar") }
                        OutlinedButton(onClick = { confirmClear = true }, enabled = entries.isNotEmpty(),
                            modifier = Modifier.weight(1f).heightIn(min = 40.dp),
                            shape = RoundedCornerShape(8.dp)) { Text("Limpar") }
                    }
                }
                TextButton(onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://aistudio.google.com/usage")))
                }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.OpenInNew, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Ver limites e créditos no Google", fontSize = 12.sp)
                }
            }
        }
    }
    configuringProject?.let { project ->
        GeminiModelSettingsDialog(project, configuringModel, quotas, prices,
            onDismiss = { configuringProject = null },
            onSaved = { configuringProject = null; revision++; refresh() })
    }
    if (confirmClear) AlertDialog(onDismissRequest = { confirmClear = false },
        title = { Text("Limpar histórico de consumo?") },
        text = { Text("Apaga apenas as contagens locais. Não apaga chaves, projetos, áudios nem o consumo no Google.") },
        confirmButton = { TextButton(onClick = {
            runCatching { ledger.clear() }.onSuccess { refresh() }
            confirmClear = false
        }) { Text("Limpar histórico") } },
        dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancelar") } })
}

@Composable
internal fun GeminiUsageHelpButton(title: String, explanation: String) {
    var open by remember { mutableStateOf(false) }
    IconButton(onClick = { open = true }, modifier = Modifier.size(36.dp)) {
        Icon(Icons.Default.HelpOutline, "Ajuda: $title", modifier = Modifier.size(20.dp))
    }
    if (open) AlertDialog(onDismissRequest = { open = false },
        title = { Text(title) }, text = { Text(explanation) },
        confirmButton = { TextButton(onClick = { open = false }) { Text("Entendi") } })
}

@Composable
private fun GeminiUsageMetric(label: String, number: String, modifier: Modifier,
    warning: Boolean = false) {
    val scheme = MaterialTheme.colorScheme
    Surface(modifier = modifier, shape = RoundedCornerShape(10.dp),
        color = if (warning) scheme.errorContainer.copy(alpha = 0.54f)
            else scheme.primaryContainer.copy(alpha = 0.43f)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(number, style = MaterialTheme.typography.titleLarge,
                maxLines = 1, color = if (warning) scheme.error else scheme.onSurface)
            Text(label, style = MaterialTheme.typography.labelSmall,
                maxLines = 1, color = scheme.onSurfaceVariant)
        }
    }
}
