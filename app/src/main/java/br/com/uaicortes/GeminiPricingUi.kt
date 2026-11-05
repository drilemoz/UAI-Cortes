package br.com.uaicortes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
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

/** Resumo enxuto; números avançados ficam ocultos até o usuário abrir os detalhes. */
@Composable
internal fun GeminiAnalyticsSection(events: List<GeminiUsageEvent>, priceStore: GeminiPriceStore,
    periodDays: Int) {
    val stats = remember(events, periodDays) {
        GeminiAnalyticsPolicy.aggregate(events) { project, model -> priceStore.get(project, model) }
    }
    val estimated = stats.sumOf { it.knownCostUsd }
    val priced = stats.sumOf { it.priced }
    val incomplete = stats.sumOf { it.unpriced }
    var advanced by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HorizontalDivider()
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Custo e eficiência", style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f))
            GeminiUsageHelpButton("Estimativa de custo",
                "Valores de tarifas são fornecidos por você, inclusive quando copiados de referências públicas. " +
                "O subtotal só cobre respostas com tokens de entrada e saída medidos; não é a fatura. " +
                "Não inclui operações sem medição, cache, outras modalidades/tier, " +
                "uso externo ou ajustes de faturamento. Latência é a duração medida da chamada.")
        }
        Text(if (priced == 0) "Custo: não configurado ou sem medições suficientes" else
            "Subtotal parcial: US$ ${"%.5f".format(Locale.US, estimated)} · $priced medida(s)",
            style = MaterialTheme.typography.bodyMedium)
        if (stats.isNotEmpty()) {
            Text("Atividade por função", style = MaterialTheme.typography.labelMedium)
            stats.forEach { stat ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(stat.operation.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    Text("${stat.success}/${stat.attempts} concluídas",
                        style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        Row(Modifier.fillMaxWidth().clickable { advanced = !advanced }.padding(vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text("Gráficos e métricas", style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.weight(1f))
            Text(if (advanced) "Ocultar" else "Exibir", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary)
        }
        if (advanced) {
            Text("Tentativas: ${events.size} · preços e tokens completos: $priced · " +
                "sem estimativa: $incomplete", style = MaterialTheme.typography.bodySmall)
            stats.forEach { stat ->
                val successRatio = if (stat.attempts > 0) stat.success.toFloat() / stat.attempts else 0f
                Text("${stat.operation}: ${stat.success}/${stat.attempts} sucesso(s)",
                    style = MaterialTheme.typography.labelSmall)
                // Esta barra representa sucesso, não volume relativo de tentativas.
                LinearProgressIndicator(progress = { successRatio },
                    modifier = Modifier.fillMaxWidth().height(5.dp))
                Text("Erros: ${stat.errors} · resposta média: " +
                    (stat.averageLatencyMs?.let { "${"%.1f".format(Locale.US, it / 1000.0)} s" }
                        ?: "não medida") + " · custo parcial: " +
                    (if (stat.priced > 0) "US$ ${"%.5f".format(Locale.US, stat.knownCostUsd)}" else "não disponível"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        }
    }
}
