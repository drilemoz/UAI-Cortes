package br.com.uaicortes

import java.util.Locale

/** Contrato de publicação: uma descrição, quebra de linha e exatamente 5 hashtags em até 100 caracteres. */
internal data class VideoCaption(val description: String, val hashtags: String) {
    val combined: String get() = "$description\n$hashtags"
}

internal object VideoCaptionPolicy {
    const val MAX_CHARACTERS = 100
    private val hashtag = Regex("#[\\p{L}\\p{N}_]{1,24}")
    private val emoticon = Regex("(?i)(?:[:;=][-~]?[)(/\\\\dpP]|\\^_\\^|<3)")

    fun totalCharacters(description: String, hashtags: String): Int =
        description.length + hashtags.length + if (description.isNotEmpty() && hashtags.isNotEmpty()) 1 else 0

    fun containsEmoticonOrEmoji(text: String): Boolean {
        if (emoticon.containsMatchIn(text)) return true
        var index = 0
        while (index < text.length) {
            val cp = Character.codePointAt(text, index)
            if (cp > 0xFFFF || cp == 0xFE0F || cp == 0x200D ||
                Character.getType(cp) == Character.OTHER_SYMBOL.toInt()) return true
            index += Character.charCount(cp)
        }
        return false
    }

    fun hashtagCount(input: String): Int = input.trim().split(Regex("\\s+"))
        .filter { it.isNotEmpty() }.size

    fun isValid(description: String, tags: String): Boolean {
        if (description.isBlank() || tags.isBlank() || description.contains('#') ||
            description.contains('\n') || tags.contains('\n') ||
            totalCharacters(description, tags) > MAX_CHARACTERS ||
            containsEmoticonOrEmoji(description) || containsEmoticonOrEmoji(tags)) return false
        val words = tags.trim().split(Regex("\\s+"))
        return words.size == 5 && words.all { hashtag.matches(it) } &&
            words.distinctBy { it.lowercase(Locale.ROOT) }.size == 5
    }

    /** Nunca completa hashtags inventadas. Respostas fora do padrão são rejeitadas. */
    fun fromModel(description: String, tags: List<String>): VideoCaption? {
        val cleanTags = tags.map { it.trim() }
        if (cleanTags.size != 5 || cleanTags.any { !hashtag.matches(it) } ||
            cleanTags.distinctBy { it.lowercase(Locale.ROOT) }.size != 5) return null
        val hashtags = cleanTags.joinToString(" ")
        val budget = MAX_CHARACTERS - hashtags.length - 1
        if (budget < 1) return null
        val cleaned = description.replace(Regex("[\\r\\n\\t]+"), " ")
            .replace(Regex("\\s+"), " ").trim()
        if (cleaned.isBlank() || containsEmoticonOrEmoji(cleaned)) return null
        val truncated = if (cleaned.length > budget) cleaned.take(budget).trimEnd() else cleaned
        val final = truncated.trimEnd(' ', ',', ';', ':', '-', '.')
        return VideoCaption(final, hashtags).takeIf { isValid(it.description, it.hashtags) }
    }
}
