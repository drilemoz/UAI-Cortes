package br.com.uaicortes

import org.json.JSONObject

/** Marca de texto persistida separadamente das legendas; vale para o projeto inteiro.
 * Coordenadas normalizadas no quadro do vídeo, não na tela do aparelho.
 */
data class TextWatermark(
    val text: String = "",
    val sizeSp: Int = 14,
    val color: Int = 0xFFFFFFFF.toInt(),
    val opacity: Float = 0.75f,
    val x: Float = 0.82f,
    val y: Float = 0.90f
) {
    fun normalized(): TextWatermark = copy(
        text = text.trim().take(80),
        sizeSp = sizeSp.coerceIn(12, 36),
        color = color or 0xFF000000.toInt(),
        opacity = if (opacity.isFinite()) opacity.coerceIn(0.1f, 1f) else 0.75f,
        x = if (x.isFinite()) x.coerceIn(0.1f, 0.9f) else 0.82f,
        y = if (y.isFinite()) y.coerceIn(0.1f, 0.95f) else 0.90f
    )

    fun toSubtitle(durationMs: Long): SubtitleCue? {
        val w = normalized()
        if (w.text.isEmpty() || durationMs <= 0L) return null
        val alpha = (w.opacity * 255f + 0.5f).toInt().coerceIn(0, 255)
        val actualColor = (w.color and 0x00FFFFFF) or (alpha shl 24)
        return SubtitleCue(
            id = "watermark-text",
            text = w.text, startMs = 0L, endMs = durationMs,
            color = actualColor, sizeSp = w.sizeSp,
            x = w.x, y = w.y,
            backgroundColor = SubtitleStyles.NO_BACKGROUND,
            bold = true, animation = SubtitleAnimations.NONE
        )
    }

    fun json(): JSONObject = normalized().let { w -> JSONObject()
        .put("text", w.text).put("sizeSp", w.sizeSp).put("color", w.color)
        .put("opacity", w.opacity.toDouble()).put("x", w.x.toDouble())
        .put("y", w.y.toDouble())
    }

    companion object {
        fun read(json: JSONObject): TextWatermark = TextWatermark(
            text = json.optString("text"),
            sizeSp = json.optInt("sizeSp", 14),
            color = json.optInt("color", -1),
            opacity = json.optDouble("opacity", 0.75).toFloat(),
            x = json.optDouble("x", 0.82).toFloat(),
            y = json.optDouble("y", 0.90).toFloat()
        ).normalized()
    }
}
