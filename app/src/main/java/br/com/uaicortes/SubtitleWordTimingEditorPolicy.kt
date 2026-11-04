package br.com.uaicortes

/** Edição MANUAL dos intervalos, sem presumir que foram obtidos de áudio.
 * Os horários ficam no mesmo formato absoluto do SRT/ASR e do renderizador existente.
 */
internal object SubtitleWordTimingEditorPolicy {
    const val MAX_EDITABLE_WORDS = 40
    const val MIN_STEP_MS = 60L

    fun words(cue: SubtitleCue): List<String> = SubtitleWordEffects.wordRanges(cue.text)
        .map { cue.text.substring(it) }

    fun editable(cue: SubtitleCue): Boolean {
        val count = words(cue).size
        return count in 1..MAX_EDITABLE_WORDS && cue.endMs > cue.startMs &&
            cue.endMs - cue.startMs >= count * MIN_STEP_MS
    }

    /** Não interpreta geração linear como reconhecimento vocal; só é acionada por toque. */
    fun distribute(cue: SubtitleCue): List<SubtitleWordTiming> {
        if (!editable(cue)) return emptyList()
        val count = words(cue).size
        val duration = cue.endMs - cue.startMs
        return (0 until count).map { i ->
            SubtitleWordTiming(cue.startMs + duration * i / count,
                cue.startMs + duration * (i + 1) / count)
        }
    }

    /** Aceita intervalos de reconhecimento que contenham pausas e tenham pelo menos 1 ms.
     * Rejeita sobreposição para permitir edição de uma única fronteira sem ambiguidade.
     */
    fun valid(cue: SubtitleCue, intervals: List<SubtitleWordTiming>): Boolean =
        intervals.isNotEmpty() && intervals.size == words(cue).size &&
            intervals.all { it.startMs >= cue.startMs && it.endMs <= cue.endMs && it.endMs > it.startMs } &&
            intervals.zipWithNext().all { (a, b) -> a.endMs <= b.startMs }

    /** Move a fronteira entre duas palavras adjacentes. Não reescreve as demais.
     * Os dois intervalos passam a se encontrar exatamente na nova fronteira.
     */
    fun moveBoundary(cue: SubtitleCue, intervals: List<SubtitleWordTiming>,
        leftIndex: Int, boundaryMs: Long): List<SubtitleWordTiming>? {
        if (!valid(cue, intervals) || leftIndex !in 0 until intervals.lastIndex) return null
        val left = intervals[leftIndex]
        val right = intervals[leftIndex + 1]
        if (boundaryMs <= left.startMs || boundaryMs >= right.endMs) return null
        return intervals.mapIndexed { i, item -> when (i) {
            leftIndex -> item.copy(endMs = boundaryMs)
            leftIndex + 1 -> item.copy(startMs = boundaryMs)
            else -> item
        } }
    }

    /** Converte horários ABSOLUTOS ao intervalo de demonstração, inclusive as pausas.
     * Sem esta conversão, a prévia de 3 s ignora os horários sincronizados reais.
     */
    fun previewCue(cue: SubtitleCue, durationMs: Long): SubtitleCue {
        val duration = durationMs.coerceAtLeast(1L)
        val originalDuration = (cue.endMs - cue.startMs).coerceAtLeast(1L)
        val wordsCount = words(cue).size
        val existing = cue.wordTimings
        val good = existing.size == wordsCount && wordsCount > 0 &&
            existing.all { it.startMs >= cue.startMs && it.endMs <= cue.endMs && it.endMs > it.startMs } &&
            existing.zipWithNext().all { (a, b) -> a.startMs <= b.startMs }
        if (!good) return cue.copy(startMs = 0L, endMs = duration, wordTimings = emptyList())
        val converted = existing.map { time ->
            val start = (time.startMs - cue.startMs) * duration / originalDuration
            val end = (time.endMs - cue.startMs) * duration / originalDuration
            SubtitleWordTiming(start, end)
        }
        // Um clipe muito curto pode produzir intervalos arredondados para zero.
        return cue.copy(startMs = 0L, endMs = duration,
            wordTimings = if (converted.all { it.endMs > it.startMs }) converted else emptyList())
    }
}
