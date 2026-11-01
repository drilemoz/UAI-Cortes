package br.com.uaicortes

/** Legendas visíveis durante a reprodução contínua da timeline.
 * Intervalos são [início, fim): uma legenda termina sem interromper o vídeo,
 * outra pode começar no mesmo milissegundo e lacunas não mostram texto.
 * Preserva a ordem original, inclusive quando existem intervalos sobrepostos.
 */
internal object SubtitlePreviewPolicy {
    fun activeAt(cues: List<SubtitleCue>, projectMs: Long): List<SubtitleCue> =
        cues.filter { it.startMs <= projectMs && projectMs < it.endMs && it.text.isNotBlank() }
}
