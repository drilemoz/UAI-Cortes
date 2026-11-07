package br.com.uaicortes

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.time.LocalDate

/** Um único modal reúne limites e tarifas; nunca busca cota/saldo na API. */
@Composable
internal fun GeminiModelSettingsDialog(project: String, initialModel: String,
    quotas: GeminiQuotaStore, prices: GeminiPriceStore,
    onDismiss: () -> Unit, onSaved: () -> Unit) {
    val context = LocalContext.current
    var model by remember(project, initialModel) { mutableStateOf(initialModel) }
    var rpm by remember(project, initialModel) { mutableStateOf(quotas.get(project, initialModel).rpm?.toString().orEmpty()) }
    var tpm by remember(project, initialModel) { mutableStateOf(quotas.get(project, initialModel).tpmInput?.toString().orEmpty()) }
    var rpd by remember(project, initialModel) { mutableStateOf(quotas.get(project, initialModel).rpd?.toString().orEmpty()) }
    var input by remember(project, initialModel) { mutableStateOf(prices.get(project, initialModel).inputUsdPerMillion?.toString().orEmpty()) }
    var output by remember(project, initialModel) { mutableStateOf(prices.get(project, initialModel).outputUsdPerMillion?.toString().orEmpty()) }
    var paid by remember(project, initialModel) { mutableStateOf(true) }
    var freeConfirmed by remember(project, initialModel) { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }
    var problem by remember { mutableStateOf("") }
    var rateNotice by remember { mutableStateOf("") }
    val rate = remember(model) { GeminiPublicPriceCatalog.lookup(model) }
    fun changeModel(value: String) {
        val next = value.trim().take(100)
        if (next != model) {
            model = next
            val limit = quotas.get(project, next)
            val price = prices.get(project, next)
            rpm = limit.rpm?.toString().orEmpty(); tpm = limit.tpmInput?.toString().orEmpty()
            rpd = limit.rpd?.toString().orEmpty()
            input = price.inputUsdPerMillion?.toString().orEmpty()
            output = price.outputUsdPerMillion?.toString().orEmpty()
            paid = true; freeConfirmed = false; rateNotice = ""; problem = ""
        }
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxWidth(.94f).heightIn(max = 620.dp),
            shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Modelo e consumo", style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f))
                    IconButton(onClick = { showHelp = true }, modifier = Modifier.size(38.dp)) {
                        Icon(Icons.Default.HelpOutline, "Ajuda sobre cotas e tarifas")
                    }
                }
                Text("Projeto: $project", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(model, ::changeModel, modifier = Modifier.fillMaxWidth(),
                    label = { Text("ID exato do modelo") }, singleLine = true)
                Text("Limites locais", style = MaterialTheme.typography.titleSmall)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    OutlinedTextField(rpm, { rpm = it.filter(Char::isDigit).take(7) },
                        label = { Text("RPM") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(tpm, { tpm = it.filter(Char::isDigit).take(10) },
                        label = { Text("TPM") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(rpd, { rpd = it.filter(Char::isDigit).take(9) },
                        label = { Text("RPD") }, modifier = Modifier.weight(1f), singleLine = true)
                }
                TextButton(onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://aistudio.google.com/rate-limit")))
                }, modifier = Modifier.align(Alignment.End)) {
                    Text("Consultar limites no Google")
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Default.OpenInNew, null, Modifier.size(15.dp))
                }
                HorizontalDivider()
                Text("Tarifas estimadas · USD/1M tokens", style = MaterialTheme.typography.titleSmall)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = paid, onClick = { paid = true; freeConfirmed = false })
                    Text("Pago", modifier = Modifier.padding(end = 15.dp))
                    RadioButton(selected = !paid, onClick = { paid = false; freeConfirmed = false })
                    Text("Gratuito")
                }
                if (paid) {
                    if (rate != null) {
                        OutlinedButton(onClick = {
                            input = rate.inputUsdPerMillion.toString()
                            output = rate.outputUsdPerMillion.toString()
                            rateNotice = "Referência aplicada (${rate.verifiedOn}). Confira seu plano antes de salvar."
                        }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
                            Text("Preencher com preço de referência")
                        }
                    } else Text("Sem referência atual verificada para este modelo. Informe manualmente.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(input, { input = it.take(15) }, label = { Text("Entrada") },
                            modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(output, { output = it.take(15) }, label = { Text("Saída") },
                            modifier = Modifier.weight(1f), singleLine = true)
                    }
                    if (rateNotice.isNotBlank()) Text(rateNotice,
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = freeConfirmed, onCheckedChange = { freeConfirmed = it })
                        Text("Confirmei no Google que esta operação é gratuita neste projeto.",
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
                TextButton(onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://ai.google.dev/gemini-api/docs/pricing")))
                }, modifier = Modifier.align(Alignment.End)) { Text("Ver preços oficiais") }
                if (problem.isNotBlank()) Text(problem, color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancelar") }
                    Button(onClick = {
                        try {
                            fun positive(raw: String, max: Long): Long? {
                                if (raw.isBlank()) return null
                                return raw.toLongOrNull()?.takeIf { it in 1..max }
                                    ?: throw IllegalArgumentException("Limites precisam ser números positivos válidos.")
                            }
                            fun usd(raw: String): Double? {
                                if (raw.isBlank()) return null
                                return raw.trim().replace(',', '.').toDoubleOrNull()
                                    ?.takeIf { it.isFinite() && it in 0.0..100_000.0 }
                                    ?: throw IllegalArgumentException("Preços inválidos. Use valores em USD.")
                            }
                            require(model.isNotBlank()) { "Informe o ID exato do modelo." }
                            val nextLimits = GeminiManualLimits(
                                positive(rpm, 1_000_000L)?.toInt(),
                                positive(tpm, 1_000_000_000L),
                                positive(rpd, 100_000_000L)?.toInt())
                            val nextPrice = if (!paid) {
                                require(freeConfirmed) { "Confirme a gratuidade antes de informar tarifa zero." }
                                GeminiManualPrice(0.0, 0.0)
                            } else {
                                val a = usd(input); val b = usd(output)
                                require((a == null) == (b == null)) { "Preencha ambos os preços ou deixe ambos vazios." }
                                GeminiManualPrice(a, b)
                            }
                            quotas.save(project, model, nextLimits)
                            prices.save(project, model, nextPrice)
                            onSaved()
                        } catch (e: Exception) { problem = e.message ?: "Falha ao salvar." }
                    }, modifier = Modifier.weight(1f), shape = UaiShapes.button) { Text("Salvar") }
                }
            }
        }
    }
    if (showHelp) AlertDialog(onDismissRequest = { showHelp = false },
        title = { Text("Como funcionam os dados?") },
        text = { Text("RPM = solicitações/minuto; TPM = tokens de entrada/minuto; RPD = solicitações/dia (Pacífico). " +
            "Essas cotas dependem do projeto e modelo e devem ser conferidas no AI Studio. " +
            "Os preços de referência são tabelas públicas consultadas em 05/11/2026, Standard pago: " +
            "não representam fatura, nem incluem cache, Batch, Priority ou modalidades especiais. " +
            "O app não identifica automaticamente seu plano nem lê saldo oficial. " +
            "No plano gratuito, confirme a gratuidade da operação antes de salvar zero.") },
        confirmButton = { TextButton(onClick = { showHelp = false }) { Text("Entendi") } })
}
