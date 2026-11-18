package br.com.uaicortes

import org.json.JSONArray
import org.json.JSONObject

/** O Flash-Lite entende áudio, mas seus horários por frase são estimados pelo modelo. */
internal object GeminiLiteTranscription {
    const val MODEL = GeminiModelRecovery.TEXT_LITE
    const val LABEL = "Gemini 3.5 Flash-Lite"
    const val NOTICE = "Flash-Lite · horários aproximados por frase, sem identificação de vozes."
    private const val MAX_CUES = 600
    private const val MAX_LINE = 170

    fun body(input: JSONObject, durationMs: Long, sample: Boolean = false): JSONObject {
        val language = if (sample) "Keep the speech in English." else "Mantenha o idioma original da fala."
        val instruction = "Transcreva somente a fala do áudio, sem resumir ou traduzir. $language " +
            "Use frases curtas em ordem, com até $MAX_LINE caracteres e sem quebras de linha. " +
            "Estime o início e o fim de cada frase em milissegundos inteiros a partir do começo do áudio. " +
            "Use startMs inclusivo e endMs exclusivo, de 0 a $durationMs, sem sobreposição. " +
            "Não invente fala durante o silêncio. Sem fala, devolva segments vazio."
        val media = if (input.getString("type") == "audio")
            JSONObject().put("inlineData", JSONObject().put("mimeType", "audio/wav")
                .put("data", input.getString("data")))
        else JSONObject().put("fileData", JSONObject().put("mimeType", "audio/wav")
            .put("fileUri", input.getString("file_uri")))
        val segment = JSONObject().put("type", "OBJECT")
            .put("properties", JSONObject()
                .put("startMs", JSONObject().put("type", "INTEGER"))
                .put("endMs", JSONObject().put("type", "INTEGER"))
                .put("text", JSONObject().put("type", "STRING")))
            .put("required", JSONArray(listOf("startMs", "endMs", "text")))
        val schema = JSONObject().put("type", "OBJECT")
            .put("properties", JSONObject().put("segments", JSONObject().put("type", "ARRAY").put("items", segment)))
            .put("required", JSONArray(listOf("segments")))
        return JSONObject().put("contents", JSONArray().put(JSONObject().put("role", "user")
            .put("parts", JSONArray().put(JSONObject().put("text", instruction)).put(media))))
            .put("generationConfig", JSONObject().put("responseMimeType", "application/json")
                .put("responseSchema", schema).put("maxOutputTokens", 16_384)
                .put("thinkingConfig", JSONObject().put("thinkingLevel", "MINIMAL")))
    }

    fun parse(response: JSONObject, durationMs: Long): List<SubtitleCue> {
        require(durationMs > 0)
        val candidate = response.getJSONArray("candidates").getJSONObject(0)
        require(candidate.getString("finishReason") == "STOP")
        val parts = candidate.getJSONObject("content").getJSONArray("parts")
        val text = (0 until parts.length()).joinToString("") {
            val part = parts.getJSONObject(it)
            if (part.optBoolean("thought")) "" else part.optString("text")
        }
        val rows = JSONObject(text).getJSONArray("segments")
        require(rows.length() <= MAX_CUES)
        var lastEnd = 0L
        return List(rows.length()) { index ->
            val row = rows.getJSONObject(index)
            val start = integer(row.get("startMs"))
            val end = integer(row.get("endMs"))
            val phrase = (row.get("text") as? String)?.replace(Regex("\\s+"), " ")?.trim().orEmpty()
            require(start >= lastEnd && start < end && end <= durationMs && phrase.length in 1..MAX_LINE)
            lastEnd = end
            SubtitleCue(text = phrase, startMs = start, endMs = end, sizeSp = DEFAULT_SUBTITLE_SIZE_SP,
                backgroundColor = SubtitleStyles.NO_BACKGROUND, outlineWidth = 2, shadowOffsetDp = 0,
                animation = SubtitleAnimations.NONE)
        }
    }

    private fun integer(value: Any): Long {
        require(value is Int || value is Long)
        return (value as Number).toLong()
    }
}

internal data class GeminiSubtitleProbe(val model: String, val count: Int)
