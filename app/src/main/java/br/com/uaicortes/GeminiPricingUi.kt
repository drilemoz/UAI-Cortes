package br.com.uaicortes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.util.Locale

/** Fonte de preços sempre declarada pelo usuário, nunca obtida da API de consumo. */
@Composable
internal fun GeminiPricingDialog(project: String, model: String, store: GeminiPriceStore,
    onDismiss: () -> Unit, onSaved: () -> Unit) {
    val initial = remember(project, model) { store.get(project, model) }
    var input by remember(project, model) { mutableStateOf(initial.inputUsdPerMillion?.toString().orEmpty()) }
    var output by remember(project, model) { mutableStateOf(initial.outputUsdPerMillion?.toString().orEmpty()) }
    var warning by remember(project, model) { mutableStateOf("") }
    fun price(raw: String): Double? = raw.trim().replace(',', '.').toDoubleOrNull()
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxWidth(.94f).heightIn(max = 550.dp),
            shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Tarifas para estimativa", style = MaterialTheme.typography.titleMedium)
                Text("Projeto: $project", style = MaterialTheme.typography.labelMedium)
                Text("Modelo: $model", style = MaterialTheme.typography.bodySmall)
                Text("Informe o preço em USD por 1 milhão de tokens do plano/modalidade em uso. " +
                    "Não é a cobrança da Google. Tarifas podem variar por tier, modalidade, cache e data. " +
                    "Para Free, só preencha zero se tiver confirmado gratuidade desta operação.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(value = input, onValueChange = { input = it.take(15) },
                    label = { Text("Entrada · USD / 1M") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = output, onValueChange = { output = it.take(15) },
                    label = { Text("Saída · USD / 1M") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Text("Deixe vazio para desativar a estimativa. Só serão computadas respostas " +
                    "com tokens de entrada e saída informados. Valores e tempos são locais.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (warning.isNotBlank()) Text(warning, color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancelar") }
                    Button(onClick = {
                        val inVal = price(input)
                        val outVal = price(output)
                        if ((input.isNotBlank() && inVal == null) ||
                            (output.isNotBlank() && outVal == null) ||
                            listOfNotNull(inVal, outVal).any { !it.isFinite() || it !in 0.0..100_000.0 } ||
                            (input.isBlank() != output.isBlank())) {
                            warning = "Preencha ambos os preços com números válidos ou deixe ambos vazios."
                        } else {
                            runCatching { store.save(project, model, GeminiManualPrice(inVal, outVal)) }
                                .onSuccess { onSaved() }
                                .onFailure { warning = it.message ?: "Não foi possível salvar." }
                        }
                    }, modifier = Modifier.weight(1f)) { Text("Salvar") }
                }
            }
        }
    }
}

/** Gráficos simples alimentados EXCLUSIVAMENTE pelos eventos armazenados neste aparelho. */
@Composable
internal fun GeminiAnalyticsSection(events: List<GeminiUsageEvent>, priceStore: GeminiPriceStore,
    periodDays: Int) {
    val stats = remember(events, periodDays) {
        GeminiAnalyticsPolicy.aggregate(events) { project, model -> priceStore.get(project, model) }
    }
    val estimated = stats.sumOf { it.knownCostUsd }
    val withPrices = stats.sumOf { it.priced }
    val missing = stats.sumOf { it.unpriced }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        HorizontalDivider()
        Text("Eficiência e custo estimado", style = MaterialTheme.typography.titleSmall)
        Text("Tentativas: ${events.size} · com preço e tokens completos: $withPrices" +
            " · sem estimativa: $missing", style = MaterialTheme.typography.bodySmall)
        Text(if (withPrices == 0) "Custo: indisponível (preencha tarifas e aguarde medições)." else
            "Subtotal estimado: US$ ${"%.5f".format(Locale.US, estimated)} em $withPrices geração(ões) medidas.",
            style = MaterialTheme.typography.bodyMedium)
        if (missing > 0 || withPrices > 0) Text("Subtotal parcial, não fatura: não inclui operações sem tokens/tarifas, " +
            "cache, modalidades/tier especiais, ajustes de faturamento ou uso fora do app.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        val maxAttempts = (stats.maxOfOrNull { it.attempts } ?: 0).coerceAtLeast(1)
        stats.forEach { stat ->
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(stat.operation.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                    Text("${stat.success}/${stat.attempts} concluídas",
                        style = MaterialTheme.typography.labelSmall)
                }
                LinearProgressIndicator(progress = { stat.attempts.toFloat() / maxAttempts },
                    modifier = Modifier.fillMaxWidth().height(6.dp))
                Text("Erros: ${stat.errors} · resposta média: " +
                    (stat.averageLatencyMs?.let { "${"%.1f".format(Locale.US, it / 1000.0)} s (${stat.measuredDuration} medidas)" }
                        ?: "sem tempo medido") +
                    " · custo parcial: " + (if (stat.priced > 0) "US$ ${"%.5f".format(Locale.US, stat.knownCostUsd)}" else "indisponível"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        val days = minOf(periodDays, 7)
        val counts = GeminiAnalyticsPolicy.dailyCounts(events, days, System.currentTimeMillis())
        val maxDaily = (counts.maxOfOrNull { it.second } ?: 0).coerceAtLeast(1)
        Text("Atividade diária · últimos $days dia(s)", style = MaterialTheme.typography.labelMedium)
        counts.forEach { (day, n) ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(day, style = MaterialTheme.typography.labelSmall)
                LinearProgressIndicator(progress = { n.toFloat() / maxDaily },
                    modifier = Modifier.weight(1f).height(5.dp))
                Text(n.toString(), style = MaterialTheme.typography.labelSmall)
            }
        }
        Text("Gráficos usam apenas os últimos eventos locais, sujeitos à retenção de 500 registros/90 dias. " +
            "Latência é tempo de chamada; não inclui preparação offline nem garante tempo futuro.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
