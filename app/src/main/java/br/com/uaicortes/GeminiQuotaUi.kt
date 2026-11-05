package br.com.uaicortes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/** Limites são copiados MANUALMENTE do AI Studio: nunca derivados do ID ou de outro projeto. */
@Composable
internal fun GeminiQuotaBudgetDialog(project: String, initialModel: String,
    quotaStore: GeminiQuotaStore, onDismiss: () -> Unit, onSaved: () -> Unit) {
    var model by remember(project, initialModel) { mutableStateOf(initialModel) }
    val initial = remember(project, initialModel) { quotaStore.get(project, initialModel) }
    var rpm by remember(project, initialModel) { mutableStateOf(initial.rpm?.toString().orEmpty()) }
    var rpd by remember(project, initialModel) { mutableStateOf(initial.rpd?.toString().orEmpty()) }
    var tpm by remember(project, initialModel) { mutableStateOf(initial.tpmInput?.toString().orEmpty()) }
    var error by remember { mutableStateOf("") }
    fun positive(s: String): Long? = s.trim().takeIf { it.isNotEmpty() }?.toLongOrNull()
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxWidth(0.94f).heightIn(max = 590.dp),
            shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Limites locais por modelo", style = MaterialTheme.typography.titleMedium)
                Text("Projeto: $project", style = MaterialTheme.typography.labelMedium)
                Text("Consulte os limites RPM, TPM de entrada e RPD para este modelo no Google AI Studio. " +
                    "Estes valores não são consultados automaticamente nem representam créditos disponíveis.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(model, { value ->
                    val next = value.trim().take(100)
                    if (next != model) {
                        model = next
                        rpm = ""; tpm = ""; rpd = ""
                        error = "Preencha limites específicos deste modelo, sem reutilizar os anteriores."
                    }
                }, label = { Text("Modelo exato") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Text("Ex.: gemini-3.8-flash-lite-tts; o modelo precisa coincidir com o usado na geração.",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(rpd, { rpd = it.filter(Char::isDigit).take(9) },
                    label = { Text("RPD · solicitações por dia") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(rpm, { rpm = it.filter(Char::isDigit).take(7) },
                        modifier = Modifier.weight(1f), label = { Text("RPM") }, singleLine = true)
                    OutlinedTextField(tpm, { tpm = it.filter(Char::isDigit).take(10) },
                        modifier = Modifier.weight(1f), label = { Text("TPM entrada") }, singleLine = true)
                }
                Text("Deixe um campo vazio quando não souber o limite. Contagens de outros dispositivos " +
                    "não entram na projeção. RPD reinicia no horário do Pacífico.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancelar") }
                    Button(onClick = {
                        try {
                            val rpdN = positive(rpd)
                            val rpmN = positive(rpm)
                            val tpmN = positive(tpm)
                            require(model.isNotBlank()) { "Informe o modelo exato." }
                            require(listOf(rpd to rpdN, rpm to rpmN, tpm to tpmN)
                                .all { (raw, n) -> raw.isBlank() || n != null && n > 0 }) {
                                "Os limites informados devem ser maiores que zero."
                            }
                            quotaStore.save(project, model, GeminiManualLimits(rpmN?.toInt(), tpmN, rpdN?.toInt()))
                            onSaved()
                        } catch (e: Exception) { error = e.message ?: "Não foi possível salvar os limites." }
                    }, modifier = Modifier.weight(1f)) { Text("Salvar") }
                }
            }
        }
    }
}
