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

/** Aviso pequeno; não sugere saldo, cota restante ou preço fictício. */
@Composable
internal fun GeminiUsageHint(operation: String, inputChars: Int? = null,
    durationMs: Long? = null, model: String = "") {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text("Consumo da IA · $operation", style = MaterialTheme.typography.labelMedium)
        if (model.isNotBlank()) Text("Modelo: $model", style = MaterialTheme.typography.bodySmall)
        if (inputChars != null && inputChars > 0) Text(GeminiUsagePolicy.estimateText(inputChars),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (durationMs != null) Text("Áudio de até ${((durationMs + 59_999) / 60_000).coerceAtLeast(1)} min; " +
            "tokens de áudio e resposta não podem ser previstos com segurança.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("O uso pode consumir cota ou gerar cobrança. Créditos restantes: consulte o Google.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Painel local com métricas verificáveis; não calcula saldo ou custo fictício. */
@Composable
internal fun GeminiUsagePanel() {
    val context = LocalContext.current
    val ledger = remember(context) { GeminiUsageLedger(context) }
    var expanded by remember { mutableStateOf(false) }
    var detailsExpanded by remember { mutableStateOf(false) }
    var period by remember { mutableIntStateOf(1) }
    var entries by remember { mutableStateOf(emptyList<GeminiUsageEvent>()) }
    var confirmClear by remember { mutableStateOf(false) }
    fun refresh() { entries = runCatching { ledger.events() }.getOrDefault(emptyList()) }
    Surface(shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.fillMaxWidth().padding(11.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(Modifier.fillMaxWidth().clickable {
                expanded = !expanded
                if (expanded) refresh()
            }, verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer) {
                    Icon(Icons.Default.BarChart, null, Modifier.padding(9.dp).size(20.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Consumo da IA", style = MaterialTheme.typography.titleSmall)
                    Text("Histórico local · Não informa saldo de créditos da conta Google.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    if (expanded) "Recolher consumo" else "Ver consumo", Modifier.size(21.dp))
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
                val known = filtered.mapNotNull { it.tokens?.total }
                val unknown = filtered.count { it.status == "ok" && it.tokens?.total == null }
                val total = known.sum()
                val limits = filtered.count { it.status == "429" }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GeminiUsageMetric("Aceitas", generated.toString(), Modifier.weight(1f))
                    GeminiUsageMetric("Tokens", "%,d".format(Locale("pt", "BR"), total), Modifier.weight(1f))
                    GeminiUsageMetric("Avisos 429", limits.toString(), Modifier.weight(1f),
                        warning = limits > 0)
                }
                if (filtered.isEmpty()) Text("Sem operações neste período.",
                    style = MaterialTheme.typography.bodySmall)
                if (unknown > 0) Text("$unknown resposta(s) sem contagem de tokens informada pela API.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Custos e créditos: consulte os dados oficiais no Google.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (filtered.isNotEmpty()) {
                    HorizontalDivider()
                    Row(Modifier.fillMaxWidth().clickable { detailsExpanded = !detailsExpanded }
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("Modelos e últimas operações", Modifier.weight(1f),
                            style = MaterialTheme.typography.titleSmall)
                        Icon(if (detailsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            if (detailsExpanded) "Recolher operações" else "Exibir operações",
                            Modifier.size(19.dp))
                    }
                    if (detailsExpanded) {
                        for ((project, events) in filtered.groupBy { it.project }) {
                            val sum = events.mapNotNull { it.tokens?.total }.sum()
                            Text("Projeto: ${project.ifBlank { "Não informado" }} · $sum tokens informados",
                                style = MaterialTheme.typography.titleSmall)
                            for ((model, byModel) in events.groupBy { it.model }) {
                                Text("${model.ifBlank { "Modelo não identificado" }} · ${byModel.size} tentativa(s)",
                                    style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Text("Últimas operações", style = MaterialTheme.typography.titleSmall)
                        val time = remember { SimpleDateFormat("dd/MM HH:mm", Locale("pt", "BR")) }
                        for (event in filtered.takeLast(5).asReversed()) {
                            val amount = event.tokens?.total?.let { "$it tokens" }
                                ?: if (event.status == "ok") "consumo não informado" else "sem medição"
                            Surface(shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)) {
                                Row(Modifier.fillMaxWidth().padding(9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(if (event.status == "ok") Icons.Default.CheckCircle
                                        else Icons.Default.Info,
                                        null, Modifier.size(17.dp),
                                        tint = if (event.status == "ok") MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.error)
                                    Column(Modifier.weight(1f)) {
                                        Text("${event.operation} · ${time.format(Date(event.at))}",
                                            style = MaterialTheme.typography.labelMedium)
                                        Text("${if (event.status == "429") "limite 429" else if (event.status == "ok") "gerado" else "falha"} · $amount",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = { refresh() }, modifier = Modifier.weight(1f).heightIn(min = 42.dp),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp)) {
                        Icon(Icons.Default.Refresh, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp)); Text("Atualizar", fontSize = 12.sp)
                    }
                    OutlinedButton(onClick = { confirmClear = true }, modifier = Modifier.weight(1f).heightIn(min = 42.dp),
                        enabled = entries.isNotEmpty(), shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp)) {
                        Icon(Icons.Default.DeleteOutline, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp)); Text("Limpar", fontSize = 12.sp)
                    }
                }
                TextButton(onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://aistudio.google.com/usage")))
                }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.OpenInNew, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(5.dp)); Text("Ver limites e créditos no Google", fontSize = 12.sp)
                }
            }
        }
    }
    if (confirmClear) AlertDialog(onDismissRequest = { confirmClear = false },
        title = { Text("Limpar histórico de consumo?") },
        text = { Text("Apaga apenas contagens locais deste aplicativo. " +
            "Não apaga chaves, projetos ou áudios e não altera o consumo registrado pelo Google.") },
        confirmButton = { TextButton(onClick = {
            runCatching { ledger.clear() }.onSuccess { refresh() }
            confirmClear = false
        }) { Text("Limpar histórico") } },
        dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancelar") } })
}

@Composable
private fun GeminiUsageMetric(label: String, number: String, modifier: Modifier,
    warning: Boolean = false) {
    val scheme = MaterialTheme.colorScheme
    Surface(modifier = modifier, shape = RoundedCornerShape(10.dp),
        color = if (warning) scheme.errorContainer.copy(alpha = 0.54f)
            else scheme.primaryContainer.copy(alpha = 0.43f)) {
        Column(Modifier.padding(horizontal = 9.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(number, style = MaterialTheme.typography.titleLarge,
                maxLines = 1, color = if (warning) scheme.error else scheme.onSurface)
            Text(label, style = MaterialTheme.typography.labelSmall,
                maxLines = 1, color = scheme.onSurfaceVariant)
        }
    }
}
