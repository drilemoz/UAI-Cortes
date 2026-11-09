package br.com.uaicortes

internal data class FrameSnapshotJob(
    val project: VideoProject,
    val targetInVideoMs: Long,
    val requestedShortSide: Int,
    val sourceFrame: FrameCaptureTarget,
    val directSourceFrame: Boolean
)

/** Reutiliza os mesmos efeitos do ExportEngine com um único trecho de vídeo,
 * mantendo as camadas PiP e textos nos tempos corretos.
 */
internal object FrameSnapshotProject {
    fun prepare(project: VideoProject, atMs: Long, sourceShortSide: Int): FrameSnapshotJob? {
        if (project.clips.isEmpty()) return null
        val durations = project.clips.map { it.durationMs }
        // Usar a mesma política de seek do player: na borda exata entre clipes,
        // o instante ainda pertence ao último quadro do clipe anterior.
        val selected = TimelinePlaybackCoordinator.target(durations, atMs)?.clipIndex
            ?: return null
        val clip = project.clips.getOrNull(selected)?.takeUnless { it.isImage } ?: return null
        val target = FrameCapturePlan.locate(project.clips.map {
            FrameCaptureClip(it.uri, it.startMs, it.endMs)
        }, atMs) ?: return null
        val slice = FrameSnapshotTiming.slice(clip.startMs, clip.endMs, target.sourceTimeMs)
            ?: return null
        val before = project.clips.take(selected).sumOf { it.durationMs }
        val projectStart = before + slice.startMs - clip.startMs
        val projectEnd = projectStart + slice.durationMs

        // Transições que cruzam clipes exigem dois vídeos simultâneos. Nunca exportar
        // uma composição que omita uma das camadas e chamá-la de quadro fiel.
        val prev = EditFeatures.boundary(project, selected - 1)
        val next = EditFeatures.boundary(project, selected)
        val startTransitionMs = if (prev != null) EditFeatures.transitionMs(prev,
            project.clips.getOrElse(selected - 1) { clip }.durationMs, clip.durationMs) else 0L
        val endTransitionMs = if (next != null) EditFeatures.transitionMs(next,
            clip.durationMs, project.clips.getOrElse(selected + 1) { clip }.durationMs) else 0L
        val localTime = target.sourceTimeMs - clip.startMs
        if ((startTransitionMs > 0L && localTime < startTransitionMs) ||
            (endTransitionMs > 0L && localTime >= clip.durationMs - endTransitionMs)) return null

        val overlays = project.overlays.mapNotNull { pip ->
            val portion = FrameSnapshotTiming.overlap(
                pip.startAtMs, pip.durationMs, projectStart, projectEnd) ?: return@mapNotNull null
            val globalFrom = portion.first
            val localStart = globalFrom - projectStart
            val length = portion.last - portion.first + 1L
            pip.copy(startAtMs = localStart, durationMs = length,
                sourceStartMs = if (pip.isImage) 0L
                    else pip.sourceStartMs + globalFrom - pip.startAtMs,
                volume = 0f, muted = true)
        }
        val titles = project.subtitles.filter { it.startMs < projectEnd && it.endMs > projectStart }
            .map { cue ->
                // Preservar inícios negativos mantém a fase correta de animações,
                // karaokê e word timings no instante solicitado.
                cue.copy(startMs = cue.startMs - projectStart,
                    endMs = cue.endMs - projectStart,
                    wordTimings = cue.wordTimings.map { timing ->
                        timing.copy(startMs = timing.startMs - projectStart,
                            endMs = timing.endMs - projectStart)
                    })
            }
        // Quando o quadro não usa camadas visuais, evitar uma exportação MP4
        // desnecessária: a extração nativa preserva os pixels da fonte e não
        // depende do muxer Media3 (que pode travar em clipes muito curtos).
        val hasActivePip = project.overlays.any { pip ->
            atMs >= pip.startAtMs && atMs < pip.startAtMs + pip.durationMs
        }
        val hasActiveSubtitle = SubtitlePreviewPolicy.firstAt(project.subtitles, atMs) != null
        val hasWatermark = project.textWatermark?.toSubtitle(project.durationMs) != null
        val directSourceFrame = !hasActivePip && !hasActiveSubtitle && !hasWatermark
        val shortSide = sourceShortSide.coerceIn(480, 1080)
        return FrameSnapshotJob(
            project.copy(clips = listOf(clip.copy(startMs = slice.startMs,
                endMs = slice.endMs, volume = 0f, voiceTone = VoiceTone())),
                overlays = overlays, subtitles = titles,
                transitions = emptyList(), music = null, audioTracks = emptyList()),
            slice.frameAtMs,
            shortSide,
            target,
            directSourceFrame
        )
    }
}
