package br.com.uaicortes

import java.text.Normalizer
import java.util.Locale

internal object DeviceAudioSearch {
    private val diacritics = Regex("\\p{M}+")
    private val spaces = Regex("\\s+")
    private val waveTypes = setOf("audio/wav", "audio/x-wav", "audio/wave", "audio/vnd.wave")

    fun key(text: String): String =
        Normalizer.normalize(text.lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace(diacritics, "")

    fun terms(query: String): List<String> =
        key(query).trim().split(spaces).filter { it.isNotEmpty() }

    fun matches(nameKey: String, terms: List<String>): Boolean = terms.all { it in nameKey }

    fun isWave(name: String, mime: String): Boolean =
        name.endsWith(".wav", ignoreCase = true) || mime.lowercase(Locale.ROOT) in waveTypes
}
