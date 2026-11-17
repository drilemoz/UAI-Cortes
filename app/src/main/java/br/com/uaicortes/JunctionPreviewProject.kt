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
}
