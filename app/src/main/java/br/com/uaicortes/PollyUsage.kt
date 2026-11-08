package br.com.uaicortes

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Tabela referencial de preços AWS US, não inclui taxas, descontos, região ou créditos Free Tier. */
internal object PollyCost {
    private val rates = mapOf("standard" to 4.0, "neural" to 16.0,
        "generative" to 30.0, "long-form" to 100.0)
    fun enginePrice(engine: String) = rates[engine]
    fun estimateUsd(chars: Int, engine: String): Double? =
        enginePrice(engine)?.let { chars.coerceAtLeast(0) * it / 1_000_000.0 }
    fun show(value: Double): String = String.format(Locale.US, "US$ %.5f", value)
    fun countCharacters(text: String): Int = text.codePointCount(0, text.length)
}

internal data class PollyUsageEvent(val timestamp: Long, val region: String,
    val engine: String, val voiceId: String, val billedCharacters: Int?, val estimatedUsd: Double?)

/** Histórico por dispositivo, somente operações concluídas; não guarda texto nem qualquer chave. */
internal class PollyUsageStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("polly_usage_local", Context.MODE_PRIVATE)
    @Synchronized fun events(): List<PollyUsageEvent> {
        val arr = JSONArray(prefs.getString("events_v1", "[]"))
        return (0 until arr.length()).mapNotNull { index ->
            val j = arr.optJSONObject(index) ?: return@mapNotNull null
            try {
                PollyUsageEvent(j.getLong("time"), j.optString("region"), j.getString("engine"),
                    j.optString("voice"), if (j.isNull("characters")) null else j.getInt("characters"),
                    if (j.isNull("cost")) null else j.getDouble("cost"))
            } catch (_: Exception) { null }
        }
    }
    @Synchronized fun record(audio: PollyAudio, region: String) {
        val existing = JSONArray(prefs.getString("events_v1", "[]"))
        val item = JSONObject().put("time", System.currentTimeMillis()).put("region", region)
            .put("voice", audio.voiceId).put("engine", audio.engine)
            .put("characters", audio.requestCharacters ?: JSONObject.NULL)
            .put("cost", audio.requestCharacters?.let { PollyCost.estimateUsd(it, audio.engine) } ?: JSONObject.NULL)
        val out = JSONArray()
        for (i in maxOf(0, existing.length() - 1999) until existing.length()) out.put(existing.get(i))
        out.put(item)
        check(prefs.edit().putString("events_v1", out.toString()).commit()) { "Não foi possível registrar uso local." }
    }
    @Synchronized fun limits(): Pair<Int, Int> =
        prefs.getInt("day_limit", 0).coerceAtLeast(0) to prefs.getInt("month_limit", 0).coerceAtLeast(0)
    @Synchronized fun setLimits(day: Int, month: Int) {
        require(day in 0..10_000_000 && month in 0..100_000_000)
        check(prefs.edit().putInt("day_limit", day).putInt("month_limit", month).commit())
    }
    fun sums(now: Long = System.currentTimeMillis()): Pair<Int, Int> {
        val startDay = Instant.ofEpochMilli(now).atZone(ZoneOffset.UTC).toLocalDate()
        val month = startDay.withDayOfMonth(1)
        val recent = events().filter { it.timestamp <= now && it.timestamp >= 0 }
        val day = recent.filter { Instant.ofEpochMilli(it.timestamp).atZone(ZoneOffset.UTC).toLocalDate() == startDay }
        val monthly = recent.filter { !Instant.ofEpochMilli(it.timestamp).atZone(ZoneOffset.UTC).toLocalDate().isBefore(month) }
        return day.sumOf { it.billedCharacters ?: 0 } to monthly.sumOf { it.billedCharacters ?: 0 }
    }
    fun allows(text: String): Boolean {
        val (dayMax, monthMax) = limits()
        val (day, month) = sums()
        val count = PollyCost.countCharacters(text)
        return (dayMax == 0 || day.toLong() + count <= dayMax) &&
            (monthMax == 0 || month.toLong() + count <= monthMax)
    }
    /** CSV sem textos nem credenciais; após 2.000 registros, representa apenas eventos locais retidos. */
    fun csv(): String = buildString {
        append("data_utc,regiao,voz,mecanismo,caracteres_confirmados,custo_bruto_estimado_usd\n")
        val fmt = DateTimeFormatter.ISO_INSTANT
        events().forEach { e ->
            append(fmt.format(Instant.ofEpochMilli(e.timestamp))).append(',')
                .append(e.region.replace(Regex("[^a-z0-9-]"), "")).append(',')
                .append(e.voiceId.replace(Regex("[^A-Za-z0-9_-]"), "")).append(',')
                .append(e.engine).append(',').append(e.billedCharacters ?: "").append(',')
                .append(e.estimatedUsd?.let { String.format(Locale.US, "%.7f", it) } ?: "")
                .append('\n')
        }
    }
}
