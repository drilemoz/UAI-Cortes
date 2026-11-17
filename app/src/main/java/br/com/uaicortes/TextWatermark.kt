package br.com.uaicortes

import org.json.JSONObject

/** Marca de texto persistida separadamente das legendas. Sem clipe vinculado, vale para o projeto inteiro.
 * Coordenadas normalizadas no quadro do vídeo, não na tela do aparelho.
 */
data class TextWatermark(
    val text: String = "",
    val sizeSp: Int = 14,
    val color: Int = 0xFFFFFFFF.toInt(),
    val opacity: Float = 0.75f,
    val x: Float = 0.82f,
    val y: Float = 0.90f,
    val fontFamily: String = SubtitleStyles.FONT_SANS,
    val outlineWidth: Int = 0,
    val outlineColor: Int = 0xFF000000.toInt(),
    val shadowOffsetDp: Int = 0,
    val shadowColor: Int = 0xFF000000.toInt(),
    val clipId: String? = null
) {
    fun normalized(): TextWatermark = copy(
        text = text.trim().take(80),
        sizeSp = sizeSp.coerceIn(12, 36),
        color = color or 0xFF000000.toInt(),
        opacity = if (opacity.isFinite()) opacity.coerceIn(0f, 1f) else 0.75f,
        x = if (x.isFinite()) x.coerceIn(0.1f, 0.9f) else 0.82f,
        y = if (y.isFinite()) y.coerceIn(0.1f, 0.95f) else 0.90f,
        fontFamily = SubtitleStyles.normalizeFont(fontFamily),
        outlineWidth = outlineWidth.coerceIn(0, 4),
        outlineColor = outlineColor or 0xFF000000.toInt(),
        shadowOffsetDp = shadowOffsetDp.coerceIn(0, 6),
        shadowColor = SubtitleStyles.normalizeShadowColor(shadowColor)
    )

    fun toSubtitle(durationMs: Long): SubtitleCue? =
        if (clipId == null) cueFor(0L, durationMs) else null

    /** Vínculo por ID: sobrevive a cortes e reordenação. Clipe ausente nunca vira "projeto inteiro". */
    internal fun toSubtitle(clips: List<Clip>): SubtitleCue? {
        val target = clipId ?: return toSubtitle(clips.sumOf { it.durationMs })
        var offset = 0L
        for (clip in clips) {
            val end = offset + clip.durationMs
            if (clip.id == target) return cueFor(offset, end)
            offset = end
        }
        return null
    }

    private fun cueFor(startMs: Long, endMs: Long): SubtitleCue? {
        val w = normalized()
        if (w.text.isEmpty() || endMs <= startMs) return null
        val alpha = (w.opacity * 255f + 0.5f).toInt().coerceIn(0, 255)
        val actualColor = (w.color and 0x00FFFFFF) or (alpha shl 24)
        return SubtitleCue(
            id = "watermark-text",
            text = w.text, startMs = startMs, endMs = endMs,
            color = actualColor, sizeSp = w.sizeSp,
            x = w.x, y = w.y,
            backgroundColor = SubtitleStyles.NO_BACKGROUND,
            bold = true, animation = SubtitleAnimations.NONE,
            fontFamily = w.fontFamily, outlineWidth = w.outlineWidth,
            outlineColor = w.outlineColor, shadowOffsetDp = w.shadowOffsetDp,
            shadowColor = w.shadowColor
        )
    }

    fun json(): JSONObject = normalized().let { w -> JSONObject()
        .put("text", w.text).put("sizeSp", w.sizeSp).put("color", w.color)
        .put("opacity", w.opacity.toDouble()).put("x", w.x.toDouble())
        .put("y", w.y.toDouble()).put("fontFamily", w.fontFamily)
        .put("outlineWidth", w.outlineWidth).put("outlineColor", w.outlineColor)
        .put("shadowOffsetDp", w.shadowOffsetDp).put("shadowColor", w.shadowColor)
        .put("clipId", w.clipId)
    }

    companion object {
        fun read(json: JSONObject): TextWatermark = TextWatermark(
            text = json.optString("text"),
            sizeSp = json.optInt("sizeSp", 14),
            color = json.optInt("color", -1),
            opacity = json.optDouble("opacity", 0.75).toFloat(),
            x = json.optDouble("x", 0.82).toFloat(),
            y = json.optDouble("y", 0.90).toFloat(),
            fontFamily = json.optString("fontFamily", SubtitleStyles.FONT_SANS),
            outlineWidth = json.optInt("outlineWidth", 0),
            outlineColor = json.optInt("outlineColor", 0xFF000000.toInt()),
            shadowOffsetDp = json.optInt("shadowOffsetDp", 0),
            shadowColor = json.optInt("shadowColor", 0xFF000000.toInt()),
            clipId = json.optString("clipId").takeIf { it.isNotBlank() && it != "null" }
        ).normalized()
    }
}
