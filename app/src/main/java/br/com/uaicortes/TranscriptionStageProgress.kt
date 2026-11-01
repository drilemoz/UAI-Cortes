package br.com.uaicortes

/** Progresso de mídia efetivamente decodificada, não progresso baseado em temporizador.
 * Reserva os 2% finais para organizar e devolver as legendas.
 */
internal object TranscriptionStageProgress {
    fun decoding(clipIndex:Int,clipCount:Int,fraction:Float):Int {
        if(clipCount <= 0) return 0
        val completed=clipIndex.coerceIn(0,clipCount)
        val current=if(fraction.isFinite()) fraction.coerceIn(0f,1f) else 0f
        return (98f*(completed+current)/clipCount).toInt().coerceIn(0,98)
    }
}
