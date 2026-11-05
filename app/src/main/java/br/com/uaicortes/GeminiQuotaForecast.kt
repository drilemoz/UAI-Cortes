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

/** Cada janela tem significado distinto: nunca apresentar RPM como saldo diário. */
internal data class GeminiForecast(
    val samples: Int,
    val averageTotal: Long?,
    val averageInput: Long?,
    val remainingByLocalLimits: Int?, // compatibilidade: capacidade imediata conservadora
    val usedToday: Int,
    val label: String,
    val remainingNow: Int? = null,
    val remainingToday: Int? = null,
    val remainingByRpm: Int? = null,
    val remainingByTpm: Int? = null,
    val usedLastMinute: Int = 0,
    val nextRpmWindowMs: Long? = null
)

/** Nunca atribui eventos legados sem ID a um projeto cadastrado posteriormente. */
internal object GeminiQuotaPolicy {
    private val pacific: ZoneId = ZoneId.of("America/Los_Angeles")
    private const val MINUTE = 60_000L
    fun operationName(value: String): String = when {
        value.contains("narra", ignoreCase = true) -> "narracao"
        value.contains("legenda", ignoreCase = true) -> "legendas"
        value.contains("descri", ignoreCase = true) -> "descricao"
        value.contains("flow", ignoreCase = true) -> "flow"
        else -> value
    }

    /** Espera de ritmo local baseada nas tentativas registradas, nunca em saldo oficial. */
    fun rpmWaitMs(events: List<GeminiUsageEvent>, project: String, model: String,
        limit: Int?, now: Long = System.currentTimeMillis()): Long? {
        if (limit == null || limit <= 0 || project.isBlank() || model.isBlank()) return null
        val recent = events.filter { it.project == project && it.model == model &&
            it.at > now - MINUTE && it.at <= now }.sortedBy { it.at }
        if (recent.size < limit) return null
        // Se houver excesso de chamadas, esperar a (n - limite + 1)-ésima mais antiga expirar.
        val oldestToExpire = recent[recent.size - limit].at
        return (oldestToExpire + MINUTE + 500L - now).coerceIn(1L, MINUTE + 500L)
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
        val lastMinute = modelEvents.filter { it.at > now - MINUTE }
        val availableDaily = limits.rpd?.let { (it - localToday).coerceAtLeast(0) }
        val availableRpm = limits.rpm?.let { (it - lastMinute.size).coerceAtLeast(0) }
        // TPM é de ENTRADA: nunca substituir tokens de entrada ausentes por total/output.
        val availableTpm = limits.tpmInput?.let { limit ->
            if (avgInput != null && lastMinute.all { it.tokens?.input != null }) {
                val used = lastMinute.sumOf { it.tokens?.input ?: 0L }
                ((limit - used).coerceAtLeast(0L) / avgInput)
                    .coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
            } else null
        }
        val availableNow = listOfNotNull(availableDaily, availableRpm, availableTpm).minOrNull()
        if (!limits.configured()) return GeminiForecast(totals.size, avgTotal, avgInput, null,
            localToday, "Sem limites configurados; não é possível estimar o restante.",
            usedLastMinute = lastMinute.size)
        val qualifier = when {
            availableNow == null -> "Limites ainda não disponíveis."
            limits.tpmInput != null && avgInput == null -> "TPM sem média de entrada; projeção parcial."
            else -> "Projeção parcial dos limites manuais; limites não informados e uso externo podem reduzir o disponível."
        }
        return GeminiForecast(totals.size, avgTotal, avgInput, availableNow, localToday, qualifier,
            remainingNow = availableNow, remainingToday = availableDaily,
            remainingByRpm = availableRpm, remainingByTpm = availableTpm,
            usedLastMinute = lastMinute.size,
            nextRpmWindowMs = if (availableRpm == 0) rpmWaitMs(modelEvents, project, model, limits.rpm, now) else null)
    }
}
