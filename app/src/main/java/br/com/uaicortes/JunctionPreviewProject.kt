package br.com.uaicortes

internal data class JunctionPreviewJob(val project: VideoProject, val cutMs: Long,
    val playbackStartMs: Long, val playbackEndMs: Long)

/** Loops a short playback window, not a short project. Render on the original
 * project clock so repeating music, voice offsets, PiP audio and word timings
 * keep their exact phase through the selected cut. */
internal object JunctionPreviewProject {
    fun prepare(project: VideoProject, index: Int, kind: String, requestedMs: Long): JunctionPreviewJob? {
        if (!TransitionBoundaryPolicy.isValid(index, project.clips.size)) return null
        val a = project.clips[index]
        val b = project.clips[index + 1]
        val cut = project.clips.take(index + 1).sumOf { it.durationMs }
        val aLength = minOf(a.durationMs, 1100L)
        val bLength = minOf(b.durationMs, 1100L)
        val updated = TransitionBatch.apply(project, index, kind, requestedMs, false)
        return JunctionPreviewJob(updated, cut, cut - aLength, cut + bLength)
    }

    /** Conservative shortcut for a cut without independent audio, overlays or other transitions.
     * Caption phase must be identical: include the full duration of each cue in the excerpt.
     */
    fun compactDirectCut(original: JunctionPreviewJob, boundary: Int, kind: String): JunctionPreviewJob {
        val p = original.project
        if (kind != TransitionEffects.CUT || p.music != null || p.audioTracks.isNotEmpty() ||
            p.overlays.isNotEmpty() || p.textWatermark != null ||
            p.transitions.any { it.kind != TransitionEffects.CUT } ||
            p.transition != TransitionEffects.CUT || p.clips.any { it.voiceTone.safe().enabled }) return original
        if (!TransitionBoundaryPolicy.isValid(boundary, p.clips.size)) return original

        val near = p.subtitles.filter {
            it.startMs < original.playbackEndMs && it.endMs > original.playbackStartMs
        }
        val from = minOf(original.playbackStartMs, near.minOfOrNull { it.startMs } ?: Long.MAX_VALUE)
        val until = maxOf(original.playbackEndMs, near.maxOfOrNull { it.endMs } ?: 0L)
        val before = p.clips[boundary]
        val after = p.clips[boundary + 1]
        val left = original.cutMs - before.durationMs
        val right = original.cutMs + after.durationMs
        if (from < left || until > right || until - from > 4500L) return original
        val touching = p.subtitles.filter { it.startMs < until && it.endMs > from }
        if (touching.any { it.startMs < from || it.endMs > until }) return original
        val short = p.copy(
            clips = listOf(before.copy(startMs = before.startMs + from - left),
                after.copy(endMs = after.startMs + until - original.cutMs)),
            subtitles = touching.map { cue -> cue.copy(startMs = cue.startMs - from,
                endMs = cue.endMs - from, wordTimings = cue.wordTimings.map { word ->
                    word.copy(startMs = word.startMs - from, endMs = word.endMs - from)
                }) },
            transitions = emptyList())
        return JunctionPreviewJob(short, original.cutMs - from,
            original.playbackStartMs - from, original.playbackEndMs - from)
    }
}
