package br.com.uaicortes

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Dados de consumo do provedor, nunca estimativas de créditos ou faturamento. */
internal data class GeminiTokenUsage(
    val input: Long?, val output: Long?, val thoughts: Long?,
    val cached: Long?, val total: Long?
)

/** Funções puras, com valores ausentes representados por null (nunca zero inventado). */
internal object GeminiUsagePolicy {
    private fun number(obj: JSONObject, name: String): Long? {
        if (!obj.has(name) || obj.isNull(name)) return null
        val value = obj.opt(name) as? Number ?: return null
        val number = value.toLong()
        return number.takeIf { it >= 0L && it <= 100_000_000_000L }
    }

    fun parse(response: JSONObject): GeminiTokenUsage? {
        val generate = response.optJSONObject("usageMetadata")
        val interaction = response.optJSONObject("usage")
        val metadata = generate ?: interaction ?: return null
        return if (generate != null) GeminiTokenUsage(
            number(metadata, "promptTokenCount"), number(metadata, "candidatesTokenCount"),
            number(metadata, "thoughtsTokenCount"), number(metadata, "cachedContentTokenCount"),
            number(metadata, "totalTokenCount")
        ) else GeminiTokenUsage(
            number(metadata, "total_input_tokens"), number(metadata, "total_output_tokens"),
            number(metadata, "total_thought_tokens"), number(metadata, "total_cached_tokens"),
            number(metadata, "total_tokens")
        )
    }

    /** Apenas texto fornecido; system prompt, modalidades e resposta não estão incluídos. */
    fun estimateText(chars: Int): String {
        if (chars <= 0) return "Consumo de entrada ainda não estimável."
        val low = ((chars + 5) / 6).coerceAtLeast(1)
        val high = ((chars + 1) / 2).coerceAtLeast(low)
        return "Texto informado: cerca de $low–$high tokens (estimativa local). " +
            "As instruções do aplicativo e a resposta aumentam o consumo."
    }
}

internal data class GeminiUsageEvent(
    val at: Long, val project: String, val model: String, val operation: String,
    val status: String, val tokens: GeminiTokenUsage?, val latencyMs: Long? = null
)

/** Histórico exclusivamente local. Não armazena prompts, mídia, respostas ou chaves. */
internal class GeminiUsageLedger(context: Context) {
    private companion object { val storageLock = Any() }
    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences("gemini_usage_local_v1", Context.MODE_PRIVATE)
    private val credentialStore = GeminiCredentialStore(app)

    private fun identifyProject(key: String): String = runCatching {
        credentialStore.profiles().firstOrNull { credentialStore.readProfile(it.id) == key }
            ?.projectId?.ifBlank { "Projeto não informado" }
    }.getOrNull() ?: "Projeto não informado"

    fun record(key: String, model: String, operation: String,
        response: JSONObject? = null, status: String = "ok", latencyMs: Long? = null) = synchronized(storageLock) {
        val now = System.currentTimeMillis()
        val event = JSONObject().put("at", now)
            .put("project", identifyProject(key).take(100))
            .put("model", model.take(90))
            .put("operation", operation.take(35))
            .put("status", status.take(24))
        latencyMs?.takeIf { it in 0L..600_000L }?.let { event.put("latencyMs", it) }
        val usage = response?.let(GeminiUsagePolicy::parse)
        if (usage != null) {
            usage.input?.let { event.put("input", it) }
            usage.output?.let { event.put("output", it) }
            usage.thoughts?.let { event.put("thoughts", it) }
            usage.cached?.let { event.put("cached", it) }
            usage.total?.let { event.put("total", it) }
        }
        val all = runCatching { JSONArray(prefs.getString("events", "[]").orEmpty().ifBlank { "[]" }) }.getOrDefault(JSONArray())
        val next = JSONArray()
        val oldest = now - 90L * 24L * 60L * 60L * 1000L
        // Ordenação crescente: mantém apenas as últimas 500 tentativas dos últimos 90 dias.
        for (i in maxOf(0, all.length() - 499) until all.length()) {
            val previous = all.optJSONObject(i) ?: continue
            if (previous.optLong("at", 0L) >= oldest) next.put(previous)
        }
        next.put(event)
        check(prefs.edit().putString("events", next.toString()).commit()) {
            "Não foi possível salvar o histórico local de consumo."
        }
    }

    fun events(): List<GeminiUsageEvent> = synchronized(storageLock) {
        val raw = runCatching { JSONArray(prefs.getString("events", "[]").orEmpty().ifBlank { "[]" }) }.getOrDefault(JSONArray())
        val out = mutableListOf<GeminiUsageEvent>()
        for (i in 0 until raw.length()) {
            val obj = raw.optJSONObject(i) ?: continue
            fun n(key: String) = if (obj.has(key) && !obj.isNull(key)) obj.optLong(key).takeIf { it >= 0L } else null
            val metrics = if (listOf("total", "input", "output", "thoughts", "cached").any(obj::has))
                GeminiTokenUsage(n("input"), n("output"), n("thoughts"), n("cached"), n("total")) else null
            out += GeminiUsageEvent(obj.optLong("at", 0L), obj.optString("project"),
                obj.optString("model"), obj.optString("operation"), obj.optString("status"), metrics,
                n("latencyMs")?.takeIf { it <= 600_000L })
        }
        out
    }

    fun lastMessage(operation: String): String = synchronized(storageLock) {
        val event = events().lastOrNull { it.operation == operation && it.status == "ok" }
            ?: return@synchronized "Consumo não informado pela API."
        event.tokens?.total?.let { "$it tokens informados pelo Gemini." }
            ?: "Consumo real não informado pela API."
    }

    fun clear() = synchronized(storageLock) {
        check(prefs.edit().remove("events").commit()) { "Falha ao limpar histórico." }
    }
}

/** Nunca interromper uma geração de IA por falha ao salvar estatísticas opcionais. */
internal fun GeminiUsageLedger.safeRecord(key: String, model: String, operation: String,
    response: JSONObject? = null, status: String = "ok", latencyMs: Long? = null) {
    runCatching { record(key, model, operation, response, status, latencyMs) }
}
