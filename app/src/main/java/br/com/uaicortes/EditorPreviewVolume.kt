package br.com.uaicortes

/** Ganho só da reprodução local. Nunca gravar em Clip/AudioTrack/Overlay ou exportar. */
internal object EditorPreviewVolume {
    fun output(sourceVolume: Float, previewVolume: Float): Float =
        (sourceVolume.coerceIn(0f, 1f) * previewVolume.coerceIn(0f, 1f)).coerceIn(0f, 1f)
    fun percent(previewVolume: Float): Int = (previewVolume.coerceIn(0f, 1f) * 100).toInt()
}
