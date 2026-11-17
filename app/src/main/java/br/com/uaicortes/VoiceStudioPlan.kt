package br.com.uaicortes

import java.util.Locale
import kotlin.math.ceil

internal enum class VoiceStudioMode(val label: String) {
    CONTINUOUS("Áudio contínuo"), SEVEN("Cenas de 7 s"), TEN("Cenas de 10 s"), PROJECT("Clipes do projeto")
}

internal data class VoiceStudioTone(val preset: String, val label: String, val guidance: String, val wpm: Int)

internal object VoiceStudioPlan {
    const val AUTO = "Automática"
    private val words = Regex("\\S+")
    private val sentence = Regex("[.!?…][\\\"'”’)]*$")
    private val comma = Regex("[,;:][\\\"'”’)]*$")
    private val paragraph = Regex("\\n\\s*\\n")
    val tones = listOf(
        VoiceStudioTone("Humorística", "Causo engraçado",
            "Use um tom de conversa, acelere nas viradas e faça uma pausa curta antes da piada.", 120),
        VoiceStudioTone("Contadora de histórias", "História narrada",
            "Conte com naturalidade, varie a energia e dê espaço às viradas da história.", 121),
        VoiceStudioTone("Misteriosa", "Suspense",
            "Comece contido, aumente a tensão aos poucos e pause antes das revelações.", 108),
        VoiceStudioTone("Narrador profissional", "Explicação",
            "Use ritmo claro e constante, dando espaço a números, nomes e informações importantes.", 120),
        VoiceStudioTone("Conversacional", "Conversa natural",
            "Fale como numa conversa, com ênfase discreta e pausas nas mudanças de ideia.", 125)
    )

    fun suggest(text: String): VoiceStudioTone {
        val t = text.lowercase(Locale.ROOT)
        return when {
            Regex("\\b(piada|engraçad[oa]|causo|fiat\\s+147|humor|ferrari)\\b").containsMatchIn(t) -> tones[0]
            Regex("\\b(suspense|sinistro|mistério|assombração)\\b").containsMatchIn(t) -> tones[2]
            Regex("\\b(como fazer|passo a passo|tutorial|segundo dados|por cento)\\b").containsMatchIn(t) -> tones[3]
            Regex("\\b(era uma vez|até que|anos atrás|naquela noite|de repente)\\b").containsMatchIn(t) -> tones[1]
            else -> tones.last()
        }
    }

    private fun wpm(style: String, tone: VoiceStudioTone): Int = when (style) {
        AUTO -> tone.wpm
        "Dramática", "Misteriosa", "Triste" -> 109
        "Animada", "Publicitária" -> 132
        else -> 120
    }

    private fun weight(word: String): Double = 1.0 + when {
        sentence.containsMatchIn(word) -> .32
        comma.containsMatchIn(word) -> .16
        else -> 0.0
    } + if (word.any { it.isDigit() }) .4 else 0.0

    fun estimateMs(text: String, style: String, tone: VoiceStudioTone): Long =
        ceil(words.findAll(text).sumOf { weight(it.value) } * 60_000.0 / wpm(style, tone) +
            paragraph.findAll(text).count() * 330.0).toLong()

    fun plannedSeconds(text: String, style: String, tone: VoiceStudioTone): Long =
        ((estimateMs(text, style, tone) + 4_999L) / 5_000L) * 5

    fun time(seconds: Long): String = if (seconds < 60) "${seconds}s"
        else "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"

    fun measured(ms: Long): String {
        val tenths = (ms.coerceAtLeast(0L) + 99) / 100
        val seconds = tenths / 10
        val fraction = if (tenths % 10 == 0L) "" else ",${tenths % 10}"
        return if (seconds < 60) "$seconds${fraction}s" else time(seconds) + fraction
    }

    data class Scene(val startMs: Long, val windowMs: Long, val text: String)
    const val MAX_SCENES = 160

    /** Preserve the source order and split only at a word boundary, favoring sentence endings. */
    fun scenes(text: String, mode: VoiceStudioMode, windows: List<Long>,
        style: String, tone: VoiceStudioTone): List<Scene> {
        val windowsMs = when (mode) {
            VoiceStudioMode.CONTINUOUS -> return emptyList()
            VoiceStudioMode.SEVEN, VoiceStudioMode.TEN ->
                List(MAX_SCENES) { if (mode == VoiceStudioMode.SEVEN) 7000L else 10000L }
            VoiceStudioMode.PROJECT -> windows.take(MAX_SCENES).takeWhile { it > 0L }
        }
        if (windowsMs.isEmpty()) return emptyList()
        val tokens = words.findAll(text).toList()
        val costs = tokens.map { weight(it.value) * 60_000 / wpm(style, tone) }
        var remaining = costs.sum()
        var remainingWindow = windowsMs.sum().toDouble()
        var from = 0
        var time = 0L
        return buildList {
            for ((i, window) in windowsMs.withIndex()) {
                if (from >= tokens.size) break
                val limit = window * maxOf(.90, remaining / remainingWindow)
                var end = from
                var used = 0.0
                var naturalEnd = from
                while (end < tokens.size && (end == from || used + costs[end] <= limit || i == windowsMs.lastIndex)) {
                    used += costs[end]
                    val hasParagraph = end < tokens.lastIndex && paragraph.containsMatchIn(
                        text.substring(tokens[end].range.last + 1, tokens[end + 1].range.first))
                    if ((sentence.containsMatchIn(tokens[end].value) || hasParagraph) && used >= limit * .62)
                        naturalEnd = end + 1
                    end++
                }
                if (end < tokens.size && naturalEnd > from) end = naturalEnd
                add(Scene(time, window, text.substring(tokens[from].range.first, tokens[end - 1].range.last + 1)))
                remaining -= costs.subList(from, end).sum()
                remainingWindow -= window
                from = end
                time += window
            }
        }
    }
}
