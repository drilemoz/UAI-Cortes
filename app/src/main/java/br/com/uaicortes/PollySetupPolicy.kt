package br.com.uaicortes

internal object PollySetupPolicy {
    enum class Source { FORM, SAVED, NONE }

    /** Editing never borrows missing fields from the previous saved account. */
    fun source(editing: Boolean, saved: Boolean, id: String, secret: String): Source = when {
        editing && id.isNotBlank() && secret.isNotBlank() -> Source.FORM
        !editing && saved -> Source.SAVED
        else -> Source.NONE
    }

    fun engineName(engine: String): String = when (engine) {
        "standard" -> "Padrão"
        "neural" -> "Neural"
        "generative" -> "Generativo"
        "long-form" -> "Longa duração"
        else -> "Desconhecido"
    }

    fun preferredEngine(voice: PollyVoice): String =
        "neural".takeIf { it in voice.engines } ?: voice.engines.firstOrNull() ?: "standard"

    // A small, explicit test can run without a previous call to DescribeVoices.
    val fallbackVoice = PollyVoice("Camila", "Camila", "pt-BR", listOf("standard"))
    const val SAMPLE = "Olá! O Amazon Polly está funcionando no Uai Cortes."
}
