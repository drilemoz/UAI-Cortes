package br.com.uaicortes

import java.util.Locale

/** Modelo retornado por models.list; a API decide quais estão liberados para cada chave. */
internal data class GeminiCaptionModel(val id: String, val label: String)

/** Regras locais de segurança: não aceitar caminhos arbitrários nem modelos de outras modalidades. */
internal object GeminiCaptionModelPolicy {
    const val DEFAULT_MODEL = "gemini-2.5-flash"
    private val modelIdPattern = Regex("gemini-[a-z0-9][a-z0-9._-]{0,79}")
    private val unsupportedFragments = listOf(
        "-tts", "-live", "-image", "-audio", "-embedding", "-robotics", "-computer-use"
    )

    fun validId(id: String): Boolean = modelIdPattern.matches(id) &&
        unsupportedFragments.none { id.contains(it) }

    fun selectedOrDefault(saved: String?): String = saved?.takeIf(::validId) ?: DEFAULT_MODEL

    /** A listagem confirma generateContent, mas só a geração pode confirmar saída JSON. */
    fun eligible(apiName: String, generationMethods: List<String>): String? {
        if (!apiName.startsWith("models/")) return null
        val id = apiName.removePrefix("models/")
        return id.takeIf { validId(it) && generationMethods.contains("generateContent") }
    }

    fun label(id: String, apiDisplayName: String): String {
        val clean = apiDisplayName.replace(Regex("[\\r\\n\\t]"), " ").trim().take(64)
        return if (clean.isBlank()) id else "$clean ($id)"
    }

    fun order(models: List<GeminiCaptionModel>): List<GeminiCaptionModel> = models
        .distinctBy { it.id.lowercase(Locale.ROOT) }
        .sortedWith(compareBy<GeminiCaptionModel> {
            when {
                it.id.contains("flash-lite") -> 0
                it.id.contains("flash") -> 1
                it.id.contains("pro") -> 2
                else -> 3
            }
        }.thenBy { it.id })
}
