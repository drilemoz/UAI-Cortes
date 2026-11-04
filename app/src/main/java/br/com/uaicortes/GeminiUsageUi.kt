package br.com.uaicortes

import android.content.Intent
import android.net.Uri
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

@Composable
internal fun GeminiUsagePanel() {
    val context = LocalContext.current
    val ledger = remember(context) { GeminiUsageLedger(context) }
    var expanded by remember { mutableStateOf(false) }
    var period by remember { mutableIntStateOf(1) }
    var entries by remember { mutableStateOf(emptyList<GeminiUsageEvent>()) }
    var confirmClear by remember { mutableStateOf(false) }
    fun refresh() { entries = runCatching { ledger.events() }.getOrDefault(emptyList()) }
    HorizontalDivider(Modifier.padding(vertical = 5.dp))
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.BarChart, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(8.dp))
        Text("Consumo da IA", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
        IconButton(onClick = { expanded = !expanded; if (expanded) refresh() }) {
            Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                if (expanded) "Recolher histórico" else "Ver histórico")
        }
    }
    Text("Histórico local: somente solicitações feitas neste aparelho. " +
        "Não informa saldo de créditos da conta.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
    if (expanded) {
        Row(Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for ((days, label) in listOf(1 to "24 horas", 7 to "7 dias", 30 to "30 dias")) {
                FilterChip(selected = period == days, onClick = { period = days }, label = { Text(label) })
            }
        }
        val cutoff = System.currentTimeMillis() - period * 24L * 60L * 60L * 1000L
        val filtered = entries.filter { it.at >= cutoff }
        val generated = filtered.count { it.status == "ok" }
        val known = filtered.mapNotNull { it.tokens?.total }
        val unknown = filtered.count { it.status == "ok" && it.tokens?.total == null }
        val total = known.sum()
        Surface(shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)) {
            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("$generated resposta(s) aceitas · ${"%,d".format(Locale("pt", "BR"), total)} tokens registrados",
                    style = MaterialTheme.typography.titleSmall)
                if (unknown > 0) Text("$unknown resposta(s) sem contagem informada pela API.",
                    style = MaterialTheme.typography.bodySmall)
                val limits = filtered.count { it.status == "429" }
                Text(if (limits > 0) "$limits aviso(s) de limite 429 no período."
                    else "Nenhum erro de limite 429 registrado neste período.",
                    style = MaterialTheme.typography.bodySmall)
                Text("Custo e saldo: indisponíveis sem dados oficiais de faturamento.",
                    style = MaterialTheme.typography.bodySmall)
            }
        }
        if (filtered.isEmpty()) Text("Ainda não há consumo registrado neste período.",
            style = MaterialTheme.typography.bodySmall)
        for ((project, events) in filtered.groupBy { it.project }) {
            val sum = events.mapNotNull { it.tokens?.total }.sum()
            Text("Projeto: $project · $sum tokens informados",
                style = MaterialTheme.typography.titleSmall)
            val models = events.groupBy { it.model }
            for ((model, byModel) in models) {
                Text("${model.ifBlank { "Modelo não identificado" }} · ${byModel.size} tentativa(s)",
                    style = MaterialTheme.typography.bodySmall)
            }
        }
        // Máximo de cinco itens: evitar transformar configurações num painel complexo.
        if (filtered.isNotEmpty()) {
            Text("Últimas operações", style = MaterialTheme.typography.titleSmall)
            val time = remember { SimpleDateFormat("dd/MM HH:mm", Locale("pt", "BR")) }
            for (event in filtered.takeLast(5).asReversed()) {
                val amount = event.tokens?.total?.let { "$it tokens" }
                    ?: if (event.status == "ok") "consumo não informado" else "sem medição"
                Text("${time.format(Date(event.at))} · ${event.operation} · " +
                    "${if (event.status == "429") "limite 429" else if (event.status == "ok") "gerado" else "falha"} · $amount",
                    style = MaterialTheme.typography.bodySmall)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(onClick = { refresh() }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Refresh, null, Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp)); Text("Atualizar")
            }
            OutlinedButton(onClick = { confirmClear = true }, modifier = Modifier.weight(1f),
                enabled = entries.isNotEmpty()) {
                Icon(Icons.Default.DeleteOutline, null, Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp)); Text("Limpar")
            }
        }
        TextButton(onClick = {
            context.startActivity(Intent(Intent.ACTION_VIEW,
                Uri.parse("https://aistudio.google.com/usage")))
        }) {
            Icon(Icons.Default.OpenInNew, null, Modifier.size(16.dp))
            Spacer(Modifier.width(5.dp)); Text("Ver limites e créditos no Google")
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
