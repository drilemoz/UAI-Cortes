package br.com.uaicortes

/** Legendas visíveis durante a reprodução contínua da timeline.
 * Intervalos são [início, fim): uma legenda termina sem interromper o vídeo,
 * outra pode começar no mesmo milissegundo e lacunas não mostram texto.
 * Preserva a ordem original, inclusive quando existem intervalos sobrepostos.
 */
internal object SubtitlePreviewPolicy {
    fun activeAt(cues: List<SubtitleCue>, projectMs: Long): List<SubtitleCue> =
        cues.filter { it.startMs <= projectMs && projectMs < it.endMs && it.text.isNotBlank() }

    /** A exportação TextOverlay desenha a PRIMEIRA legenda válida por instante.
     * Usa-se a mesma regra na prévia; cue vazia não pode ocultar a seguinte.
     */
    fun firstAt(cues: List<SubtitleCue>, projectMs: Long): SubtitleCue? =
        cues.firstOrNull { it.startMs <= projectMs && projectMs < it.endMs && it.text.isNotBlank() }
}
