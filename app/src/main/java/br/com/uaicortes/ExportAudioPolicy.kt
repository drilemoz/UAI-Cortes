package br.com.uaicortes

/** Regras puras compartilhadas pela pré-validação e composição Media3.
 * Nunca transforma duração desconhecida em um trecho fictício de 1 segundo.
 */
internal object ExportAudioPolicy {
    data class Window(val timelineStartMs: Long, val sourceStartMs: Long,
                      val playableMs: Long, val trailingMs: Long)

    fun audible(volume: Float): Boolean = volume.isFinite() && volume > 0f

    /** null significa que a faixa está integralmente fora da timeline ou da origem.
     * Uma duração de origem não verificável também retorna null e deve ser comunicada
     * explicitamente ao usuário antes da exportação, sem inferir valores.
     */
    fun window(timelineStartMs: Long, sourceStartMs: Long, requestedDurationMs: Long,
               sourceDurationMs: Long, projectDurationMs: Long): Window? {
        if (sourceDurationMs <= 0L || projectDurationMs <= 0L) return null
        val start = timelineStartMs.coerceAtLeast(0L)
        val source = sourceStartMs.coerceAtLeast(0L)
        if (start >= projectDurationMs || source >= sourceDurationMs) return null
        val available = sourceDurationMs - source
        val requested = requestedDurationMs.takeIf { it > 0L } ?: available
        val playable = minOf(requested, available, projectDurationMs - start)
        if (playable <= 0L) return null
        return Window(start, source, playable, projectDurationMs - start - playable)
    }

    fun activeExtraTracks(project: VideoProject): List<AudioTrack> =
        ClipAudioBindings.resolved(project).filter { audible(it.volume) &&
            it.startMs < project.durationMs }

    /** A presença real de áudio no PiP deve ser inspecionada no contêiner de origem.
     * Um vídeo PiP sem som, mesmo com volume habilitado, não deve exigir áudio no MP4.
     */
    fun hasRequiredAudio(project: VideoProject): Boolean = activeExtraTracks(project).isNotEmpty() ||
        project.music?.let { audible(it.volume) } == true
}
