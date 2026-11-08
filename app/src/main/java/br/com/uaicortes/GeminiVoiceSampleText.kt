package br.com.uaicortes

import android.content.Context

/** Prévia curta/equivalente entre modelos; o usuário controla qualquer chamada paga. */
internal object GeminiVoiceSampleText {
    const val STANDARD = "Padrão"
    const val SHORT = "Curta"
    const val CUSTOM = "Personalizada"
    const val MAX_CUSTOM = 180
    const val SHORT_TEXT = "Olá! Esta é uma amostra da voz no Uai Cortes."
    val modes = listOf(STANDARD, SHORT, CUSTOM)

    fun content(mode: String, custom: String): String = when (mode) {
        SHORT -> SHORT_TEXT
        CUSTOM -> custom.trim().takeIf { it.length in 3..MAX_CUSTOM && it.none(Char::isISOControl) }
            ?: GeminiPreviewIdentity.TEXT
        else -> GeminiPreviewIdentity.TEXT
    }
    fun customValid(value: String): Boolean = value.trim().length in 3..MAX_CUSTOM &&
        value.none(Char::isISOControl)
}

internal class GeminiVoiceSamplePrefs(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("gemini_voice_sample", Context.MODE_PRIVATE)
    fun mode(): String = prefs.getString("mode", GeminiVoiceSampleText.STANDARD)
        ?.takeIf { it in GeminiVoiceSampleText.modes } ?: GeminiVoiceSampleText.STANDARD
    fun custom(): String = prefs.getString("custom", "").orEmpty().take(GeminiVoiceSampleText.MAX_CUSTOM)
    fun save(mode: String, custom: String): Boolean {
        if (mode !in GeminiVoiceSampleText.modes || custom.length > GeminiVoiceSampleText.MAX_CUSTOM ||
            custom.any(Char::isISOControl)) return false
        if (mode == GeminiVoiceSampleText.CUSTOM && !GeminiVoiceSampleText.customValid(custom)) return false
        return prefs.edit().putString("mode", mode).putString("custom", custom.trim()).commit()
    }
}
