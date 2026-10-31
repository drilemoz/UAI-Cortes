package br.com.uaicortes

/** Pure, millisecond-based clip/audio synchronization, independent of Media3. */
internal data class ClipAudioWindow(
    val timelineStartMs: Long,
    val sourceStartMs: Long,
    val durationMs: Long
)

internal object ClipAudioTiming {
    /** An attached track has one source anchor; trimming/splitting must keep it aligned. */
    fun resolve(clipProjectStartMs: Long, clipSourceStartMs: Long, clipDurationMs: Long,
                audioAnchorMs: Long, audioSourceStartMs: Long, audioDurationMs: Long): ClipAudioWindow? {
        if (clipProjectStartMs < 0 || clipSourceStartMs < 0 || clipDurationMs <= 0 ||
            audioAnchorMs < 0 || audioSourceStartMs < 0 || audioDurationMs <= 0) return null
        val skippedFromAudio = (clipSourceStartMs - audioAnchorMs).coerceAtLeast(0L)
        if (skippedFromAudio >= audioDurationMs) return null
        val delayedOnClip = (audioAnchorMs - clipSourceStartMs).coerceAtLeast(0L)
        if (delayedOnClip >= clipDurationMs) return null
        val length = minOf(audioDurationMs - skippedFromAudio, clipDurationMs - delayedOnClip)
        if (length <= 0L) return null
        return ClipAudioWindow(clipProjectStartMs + delayedOnClip,
            audioSourceStartMs + skippedFromAudio, length)
    }
}
