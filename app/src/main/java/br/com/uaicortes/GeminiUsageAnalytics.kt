package br.com.uaicortes

import android.content.Context
import org.json.JSONObject
import java.security.MessageDigest
import java.time.Instant
import java.time.ZoneId

/** Tarifas MANUAIS para o nível/plano escolhido pelo usuário, USD por milhão de tokens.
 * Não representam cobrança, tier, cache, preços oficiais ou créditos do provedor.
 */
internal data class GeminiManualPrice(val inputUsdPerMillion: Double?, val outputUsdPerMillion: Double?) {
    fun complete() = inputUsdPerMillion != null && outputUsdPerMillion != null
}

internal class GeminiPriceStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("gemini_prices_local_v1", Context.MODE_PRIVATE)
    private fun key(project: String, model: String): String = "price_" +
        MessageDigest.getInstance("SHA-256")
            .digest((project + "\n" + model).toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    fun get(project: String, model: String): GeminiManualPrice {
        if (project.isBlank() || model.isBlank()) return GeminiManualPrice(null, null)
        val data = runCatching { JSONObject(prefs.getString(key(project, model), "{}").orEmpty()) }
            .getOrDefault(JSONObject())
        fun rate(name: String): Double? = data.opt(name).let { it as? Number }?.toDouble()
            ?.takeIf { it.isFinite() && it in 0.0..100_000.0 }
        return GeminiManualPrice(rate("input"), rate("output"))
    }

    fun save(project: String, model: String, price: GeminiManualPrice) {
        require(project.isNotBlank() && project.length <= 100 && model.isNotBlank() && model.length <= 100)
        require(listOf(price.inputUsdPerMillion, price.outputUsdPerMillion).all {
            it == null || it.isFinite() && it in 0.0..100_000.0
        })
        val data = JSONObject()
        price.inputUsdPerMillion?.let { data.put("input", it) }
        price.outputUsdPerMillion?.let { data.put("output", it) }
        check(prefs.edit().putString(key(project, model), data.toString()).commit()) { "Falha ao salvar preços locais." }
    }
}

internal data class GeminiOperationStats(
    val operation: String, val attempts: Int, val success: Int, val errors: Int,
    val measuredDuration: Int, val averageLatencyMs: Long?,
    val knownCostUsd: Double, val priced: Int, val unpriced: Int
)

/** Apenas contagens comprovadas no histórico local, com amostras incompletas separadas. */
internal object GeminiAnalyticsPolicy {
    fun cost(event: GeminiUsageEvent, price: GeminiManualPrice): Double? {
        if (event.status != "ok" || !price.complete()) return null
        val usage = event.tokens ?: return null
        val input = usage.input ?: return null
        val output = usage.output ?: return null
        if (input < 0 || output < 0) return null
        return ((input / 1_000_000.0) * price.inputUsdPerMillion!! +
            (output / 1_000_000.0) * price.outputUsdPerMillion!!)
            .takeIf { it.isFinite() && it >= 0.0 }
    }

    fun aggregate(events: List<GeminiUsageEvent>, prices: (String, String) -> GeminiManualPrice):
        List<GeminiOperationStats> = events.groupBy { GeminiQuotaPolicy.operationName(it.operation) }
            .map { (operation, group) ->
                val successful = group.filter { it.status == "ok" }
                val measured = successful.mapNotNull { it.latencyMs?.takeIf { n -> n >= 0 } }
                val costs = successful.map { cost(it, prices(it.project, it.model)) }
                GeminiOperationStats(operation, group.size, successful.size, group.size - successful.size,
                    measured.size, if (measured.isNotEmpty()) measured.average().toLong() else null,
                    costs.filterNotNull().sum(), costs.count { it != null }, costs.count { it == null })
            }.sortedBy { it.operation }

    fun dailyCounts(events: List<GeminiUsageEvent>, days: Int, now: Long,
        zone: ZoneId = ZoneId.systemDefault()): List<Pair<String, Int>> {
        require(days in 1..30)
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        return (days - 1 downTo 0).map { offset ->
            val date = today.minusDays(offset.toLong())
            val count = events.count { it.at in 1..now &&
                Instant.ofEpochMilli(it.at).atZone(zone).toLocalDate() == date }
            date.toString().substring(5) to count
        }
    }

    /** Previsão monetária só quando cinco medições completas comparáveis existem. */
    fun estimatedBatchCost(events: List<GeminiUsageEvent>, project: String, model: String,
        operation: String, requested: Int, price: GeminiManualPrice): Double? {
        if (project.isBlank() || model.isBlank() || requested !in 1..500) return null
        val sample = events.asSequence().filter { it.project == project && it.model == model &&
            it.status == "ok" && GeminiQuotaPolicy.operationName(it.operation) ==
            GeminiQuotaPolicy.operationName(operation) }
            .mapNotNull { cost(it, price) }.takeLastSafe(30)
        return if (sample.size >= 5) sample.average() * requested else null
    }

    private fun <T> Sequence<T>.takeLastSafe(limit: Int): List<T> = toList().takeLast(limit)
}
