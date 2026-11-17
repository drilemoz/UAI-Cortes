package br.com.uaicortes

internal data class JunctionPreviewJob(val project: VideoProject, val cutMs: Long)

/** Export only a short local window around the selected cut. No full project render. */
internal object JunctionPreviewProject {
    fun prepare(project: VideoProject, index: Int, kind: String, requestedMs: Long): JunctionPreviewJob? {
        if (!TransitionBoundaryPolicy.isValid(index, project.clips.size)) return null
        val a = project.clips[index]
        val b = project.clips[index + 1]
        val cut = project.clips.take(index + 1).sumOf { it.durationMs }
        val aLength = minOf(a.durationMs, 1100L)
        val bLength = minOf(b.durationMs, 1100L)
        val start = cut - aLength
        val end = cut + bLength
        val clips = listOf(a.copy(startMs = a.endMs - aLength), b.copy(endMs = b.startMs + bLength))
        val subtitles = project.subtitles.filter { it.startMs < end && it.endMs > start }.map { cue ->
            // Preserve timing/animation phase instead of clipping it at the preview start.
            cue.copy(startMs = cue.startMs - start, endMs = cue.endMs - start,
                wordTimings = cue.wordTimings.map { timing ->
                    timing.copy(startMs = timing.startMs - start, endMs = timing.endMs - start)
                })
        }
        val overlays = project.overlays.mapNotNull { pip ->
            val span = FrameSnapshotTiming.overlap(pip.startAtMs, pip.durationMs, start, end)
                ?: return@mapNotNull null
            pip.copy(startAtMs = span.first - start, durationMs = span.last - span.first + 1L,
                sourceStartMs = if (pip.isImage) 0L else pip.sourceStartMs + span.first - pip.startAtMs,
                muted = true)
        }
        val sample = project.copy(clips = clips, subtitles = subtitles, overlays = overlays,
            audioTracks = emptyList(), music = null, transitions = emptyList())
        return JunctionPreviewJob(TransitionBatch.apply(sample, 0, kind, requestedMs, false), aLength)
    }
}
