package br.com.uaicortes

import java.text.Normalizer
import java.util.Locale

/** Rótulos locais, sem alterar o nome/ID original retornado pelo serviço.
 * Apenas expressões conhecidas são traduzidas; não inventar classificação de timbre.
 */
internal object GeminiVoicePresentation {
    private val roles = mapOf(
        "Authoritative Advisor" to "Conselheiro de tom firme",
        "Friendly Advisor" to "Conselheiro amigável",
        "Warm Narrator" to "Narrador acolhedor",
        "Professional Narrator" to "Narrador profissional",
        "News Anchor" to "Apresentador de notícias",
        "Storyteller" to "Contador de histórias",
        "Conversational" to "Conversacional",
        "Calm Narrator" to "Narrador calmo",
        "Energetic Presenter" to "Apresentador animado"
    )
    private val roleAliases = mapOf(
        "advisor" to "Conselheiro", "narrator" to "Narrador", "presenter" to "Apresentador",
        "newsreader" to "Locutor de notícias", "reporter" to "Repórter", "announcer" to "Locutor",
        "teacher" to "Professor", "guide" to "Guia", "storyteller" to "Contador de histórias",
        "host" to "Apresentador", "assistant" to "Assistente", "broadcaster" to "Locutor"
    )
    private val descriptorAliases = mapOf(
        "authoritative" to "de tom firme", "friendly" to "amigável", "warm" to "acolhedor",
        "calm" to "calmo", "professional" to "profissional", "energetic" to "animado",
        "expressive" to "expressivo", "cheerful" to "alegre", "confident" to "confiante",
        "gentle" to "suave", "serious" to "sério", "dramatic" to "dramático",
        "natural" to "natural", "neutral" to "neutro", "enthusiastic" to "entusiasmado",
        "upbeat" to "animado", "mature" to "maduro", "soft" to "suave",
        "lively" to "vibrante", "clear" to "claro", "soothing" to "tranquilo"
    )
    private val studio = mapOf(
        "Zephyr" to "Brilhante", "Puck" to "Animada", "Charon" to "Informativa",
        "Kore" to "Firme", "Fenrir" to "Entusiasmada", "Leda" to "Jovem",
        "Orus" to "Firme", "Aoede" to "Descontraída", "Callirrhoe" to "Tranquila",
        "Autonoe" to "Brilhante", "Enceladus" to "Soprosa", "Iapetus" to "Clara",
        "Umbriel" to "Tranquila", "Algieba" to "Suave", "Despina" to "Suave",
        "Erinome" to "Clara", "Algenib" to "Rouca", "Rasalgethi" to "Informativa",
        "Laomedeia" to "Animada", "Achernar" to "Suave", "Alnilam" to "Firme",
        "Schedar" to "Equilibrada", "Gacrux" to "Madura", "Pulcherrima" to "Projetada",
        "Achird" to "Amigável", "Zubenelgenubi" to "Casual", "Vindemiatrix" to "Gentil",
        "Sadachbia" to "Vibrante", "Sadaltager" to "Experiente", "Sulafat" to "Acolhedora"
    )
    fun label(voice: GeminiVoice): String {
        val original = voice.name.trim().ifBlank { voice.id }
        for ((english, translated) in roles) {
            if (original.equals(english, ignoreCase = true)) return translated
            val m = Regex("^${Regex.escape(english)}\\s+(\\d+)$", RegexOption.IGNORE_CASE).matchEntire(original)
            if (m != null) return "$translated ${m.groupValues[1]}"
        }
        // Traduz somente padrões com dois termos conhecidos (nunca nomes próprios arbitrários).
        val terms = Regex("^([A-Za-z]+)\\s+([A-Za-z]+)(?:\\s+(\\d+))?$").matchEntire(original)
        if (terms != null) {
            val adjective = descriptorAliases[terms.groupValues[1].lowercase(Locale.ROOT)]
            val role = roleAliases[terms.groupValues[2].lowercase(Locale.ROOT)]
            if (adjective != null && role != null) return listOf(role, adjective,
                terms.groupValues[3]).filter { it.isNotBlank() }.joinToString(" · ")
        }
        val studioName = studio.entries.firstOrNull { it.key.equals(original, true) }
        if (studioName != null) return "${studioName.key} · ${studioName.value}"
        return original
    }
    fun details(voice: GeminiVoice): String = buildList {
        val lang = voice.languageCode.trim()
        if (lang.isNotEmpty()) add(if (lang.equals("pt-BR", true)) "Português (Brasil)" else lang)
        val gender = when (voice.gender.lowercase(Locale.ROOT)) {
            "female" -> "Voz feminina"; "male" -> "Voz masculina"; "neutral" -> "Voz neutra"; else -> ""
        }
        if (gender.isNotEmpty()) add(gender)
        val pitch = when (voice.pitch.lowercase(Locale.ROOT)) {
            "low" -> "Grave"; "medium" -> "Média"; "high" -> "Aguda"; else -> ""
        }
        if (pitch.isNotEmpty()) add(pitch)
        if (voice.accent.isNotBlank()) add("Sotaque: ${voice.accent}")
        if (voice.persona.isNotBlank()) add("Perfil: ${voice.persona}")
        if (voice.context.isNotBlank()) add("Uso: ${voice.context}")
    }.joinToString(" · ")
    fun searchableText(voice: GeminiVoice): String = listOf(
        label(voice), voice.name, voice.id, voice.description, details(voice)
    ).joinToString(" ")
    fun fold(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "").lowercase(Locale.ROOT)
}
