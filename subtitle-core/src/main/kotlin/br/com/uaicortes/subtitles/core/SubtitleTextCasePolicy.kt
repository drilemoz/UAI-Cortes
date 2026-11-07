package br.com.uaicortes.subtitles.core

import java.util.Locale

/** Visual presentation only: never modify the underlying narration/SRT text. */
object SubtitleTextCasePolicy {
    const val ORIGINAL = "original"
    const val UPPER = "upper"
    const val LOWER = "lower"
    private val locale = Locale.forLanguageTag("pt-BR")

    fun normalize(value: String?): String = when (value) {
        UPPER, LOWER -> value
        else -> ORIGINAL
    }

    fun display(original: String, mode: String?): String = when (normalize(mode)) {
        UPPER -> original.uppercase(locale)
        LOWER -> original.lowercase(locale)
        else -> original
    }
}
