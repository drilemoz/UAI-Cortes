package br.com.uaicortes

/** Direções de interpretação, não parâmetros numéricos garantidos pelo serviço TTS.
 * O texto da narração permanece verbatim. Somente speech_metadata.style recebe estas frases.
 */
internal data class GeminiNarrationPreset(val label: String, val direction: String)

internal object GeminiNarrationStyling {
    val presets = listOf(
        GeminiNarrationPreset("Neutra", "clear, neutral and natural Brazilian Portuguese narration"),
        GeminiNarrationPreset("Jornalística", "informative broadcast newsreader, deliberate and articulate"),
        GeminiNarrationPreset("Animada", "cheerful, upbeat and expressive"),
        GeminiNarrationPreset("Calma", "soft, relaxed and soothing"),
        GeminiNarrationPreset("Sussurrada", "softly whispered, intimate"),
        GeminiNarrationPreset("Narrador profissional", "confident, polished, professional documentary narration"),
        GeminiNarrationPreset("Humorística", "playful, comical delivery with natural comedic timing"),
        GeminiNarrationPreset("Sarcástica", "dry, subtle sarcasm with understated irony"),
        GeminiNarrationPreset("Assustada", "frightened, tense and cautious delivery"),
        GeminiNarrationPreset("Triste", "sad, quiet and reflective delivery"),
        GeminiNarrationPreset("Irritada", "annoyed and firm, without shouting"),
        GeminiNarrationPreset("Misteriosa", "mysterious, suspenseful and controlled delivery"),
        GeminiNarrationPreset("Dramática", "dramatic and cinematic, expressive but intelligible"),
        GeminiNarrationPreset("Contadora de histórias", "immersive storytelling with engaging, natural inflection"),
        GeminiNarrationPreset("Publicitária", "energetic and persuasive commercial narration"),
        GeminiNarrationPreset("Conversacional", "warm, natural and informal conversation")
    )
    val paceLabels = listOf("Muito lenta", "Lenta", "Natural", "Rápida", "Muito rápida")
    val expressivenessLabels = listOf("Muito discreta", "Discreta", "Natural", "Expressiva", "Muito expressiva")
    val pitchLabels = listOf("Bem grave", "Grave", "Natural", "Aguda", "Bem aguda")

    /** 2 representa neutro; nenhuma instrução adicional é enviada neste caso.
     * Preserva exatamente os estilos da v0.1.95 quando nenhum ajuste é feito (cache compatível).
     */
    fun instruction(label: String, pace: Int = 2, expressiveness: Int = 2, pitch: Int = 2): String {
        require(pace in 0..4 && expressiveness in 0..4 && pitch in 0..4) { "Ajuste fora da faixa disponível." }
        val preset = presets.firstOrNull { it.label == label }
            ?: throw IllegalArgumentException("Estilo de narração não reconhecido.")
        val result = mutableListOf(preset.direction)
        when (pace) {
            0 -> result += "speaking very slowly, with natural pauses"
            1 -> result += "speaking slowly and clearly"
            3 -> result += "speaking briskly and clearly"
            4 -> result += "speaking rapidly while keeping words intelligible"
        }
        when (expressiveness) {
            0 -> result += "very restrained emotional expression"
            1 -> result += "subtle emotional expression"
            3 -> result += "expressive emotional inflection"
            4 -> result += "highly expressive emotional inflection without exaggerating pronunciation"
        }
        when (pitch) {
            0 -> result += "noticeably lower vocal pitch while retaining the selected voice identity"
            1 -> result += "slightly lower vocal pitch"
            3 -> result += "slightly higher vocal pitch"
            4 -> result += "noticeably higher vocal pitch while retaining the selected voice identity"
        }
        return result.joinToString(", ")
    }
}
