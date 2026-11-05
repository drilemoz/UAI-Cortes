package br.com.uaicortes

import android.content.Context
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId

/** Limites declarados pelo usuário, NÃO cotas consultadas no Google nem saldo oficial. */
internal data class GeminiManualLimits(val rpm: Int?, val tpmInput: Long?, val rpd: Int?) {
    fun configured(): Boolean = rpm != null || tpmInput != null || rpd != null
}

internal class GeminiQuotaStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("gemini_limits_local_v1", Context.MODE_PRIVATE)
    private fun key(project: String, model: String) = "limit_" +
        java.security.MessageDigest.getInstance("SHA-256")
            .digest((project + "\n" + model).toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    fun get(project: String, model: String): GeminiManualLimits {
        if (project.isBlank() || model.isBlank()) return GeminiManualLimits(null, null, null)
        val data = runCatching { JSONObject(prefs.getString(key(project, model), "{}").orEmpty().ifBlank { "{}" }) }.getOrDefault(JSONObject())
        return GeminiManualLimits(data.optInt("rpm").takeIf { it > 0 },
            data.optLong("tpmInput").takeIf { it > 0L },
            data.optInt("rpd").takeIf { it > 0 })
    }

    fun save(project: String, model: String, limits: GeminiManualLimits) {
        require(project.isNotBlank() && model.isNotBlank() && model.length <= 100)
        require(limits.rpm == null || limits.rpm in 1..1_000_000)
        require(limits.rpd == null || limits.rpd in 1..100_000_000)
        require(limits.tpmInput == null || limits.tpmInput in 1L..1_000_000_000L)
        val content = JSONObject()
        limits.rpm?.let { content.put("rpm", it) }
        limits.rpd?.let { content.put("rpd", it) }
        limits.tpmInput?.let { content.put("tpmInput", it) }
        check(prefs.edit().putString(key(project, model), content.toString()).commit()) {
            "Não foi possível salvar limites locais."
        }
    }
}

internal data class GeminiForecast(
    val samples: Int,
    val averageTotal: Long?,
    val averageInput: Long?,
    val remainingByLocalLimits: Int?,
    val usedToday: Int,
    val label: String
)

/** Nunca atribui eventos legados sem ID a um projeto cadastrado posteriormente. */
internal object GeminiQuotaPolicy {
    private val pacific: ZoneId = ZoneId.of("America/Los_Angeles")
    fun operationName(value: String): String = when {
        value.contains("narra", ignoreCase = true) -> "narracao"
        value.contains("legenda", ignoreCase = true) -> "legendas"
        value.contains("descri", ignoreCase = true) -> "descricao"
        value.contains("flow", ignoreCase = true) -> "flow"
        else -> value
    }

    fun compute(events: List<GeminiUsageEvent>, project: String, model: String,
        operation: String, limits: GeminiManualLimits, now: Long = System.currentTimeMillis()): GeminiForecast {
        if (project.isBlank() || model.isBlank()) return GeminiForecast(0, null, null, null, 0,
            "Informe o ID do projeto e o modelo para acompanhar o consumo.")
        val modelEvents = events.filter { it.project == project && it.model == model && it.at in 1..now }
        val samples = modelEvents.filter { it.status == "ok" &&
            operationName(it.operation) == operationName(operation) &&
            it.at >= now - 30L * 24L * 60L * 60L * 1000L }
            .takeLast(30)
        // Não misturar modelos, funções ou contagens ausentes. Exigir 5 medições independentes.
        val totals = samples.mapNotNull { it.tokens?.total?.takeIf { n -> n > 0 } }
        val inputs = samples.mapNotNull { it.tokens?.input?.takeIf { n -> n > 0 } }
        val avgTotal = if (totals.size >= 5) totals.average().toLong().coerceAtLeast(1) else null
        val avgInput = if (inputs.size >= 5) inputs.average().toLong().coerceAtLeast(1) else null
        val today = Instant.ofEpochMilli(now).atZone(pacific).toLocalDate()
        val localToday = modelEvents.count {
            Instant.ofEpochMilli(it.at).atZone(pacific).toLocalDate() == today
        }
        if (!limits.configured()) return GeminiForecast(totals.size, avgTotal, avgInput, null,
            localToday, "Sem limites configurados; não é possível estimar o restante.")
        val available = mutableListOf<Long>()
        limits.rpd?.let { available += (it.toLong() - localToday).coerceAtLeast(0) }
        limits.rpm?.let { limit ->
            val used = modelEvents.count { it.at > now - 60_000L }
            available += (limit.toLong() - used).coerceAtLeast(0)
        }
        // TPM significa tokens de ENTRADA por minuto; nunca subtrair total/output como se fosse input.
        limits.tpmInput?.let { limit ->
            if (avgInput != null && modelEvents.filter { it.at > now - 60_000L }
                .all { it.tokens?.input != null }) {
                val used = modelEvents.filter { it.at > now - 60_000L }.sumOf { it.tokens?.input ?: 0L }
                available += ((limit - used).coerceAtLeast(0L) / avgInput)
            }
        }
        val remaining = available.minOrNull()?.coerceAtMost(Int.MAX_VALUE.toLong())?.toInt()
        val qualifier = when {
            remaining == null -> "Limites ainda não disponíveis."
            limits.tpmInput != null && avgInput == null -> "TPM sem média de entrada; projeção parcial."
            else -> "Projeção parcial dos limites manuais; limites não informados e uso externo podem reduzir o disponível."
        }
        return GeminiForecast(totals.size, avgTotal, avgInput, remaining, localToday, qualifier)
    }
}
