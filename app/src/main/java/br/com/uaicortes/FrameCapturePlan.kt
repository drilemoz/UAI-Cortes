package br.com.uaicortes

/** Seleção testável do quadro original, respeitando cortes e tempo global do projeto. */
internal data class FrameCaptureClip(val uri: String, val startMs: Long, val endMs: Long) {
    val durationMs: Long get() = (endMs - startMs).coerceAtLeast(0L)
}
internal data class FrameCaptureTarget(val uri: String, val sourceTimeMs: Long)

internal object FrameCapturePlan {
    fun locate(clips: List<FrameCaptureClip>, projectMs: Long): FrameCaptureTarget? {
        if (clips.isEmpty() || clips.any { it.uri.isBlank() || it.endMs <= it.startMs || it.startMs < 0L }) return null
        val seek = TimelinePlaybackCoordinator.target(clips.map { it.durationMs }, projectMs) ?: return null
        val clip = clips[seek.clipIndex]
        // Fim é exclusivo; evita solicitar um quadro além da origem do vídeo.
        val at = (clip.startMs + seek.localMs).coerceIn(clip.startMs, clip.endMs - 1)
        return FrameCaptureTarget(clip.uri, at)
    }

    fun filename(projectTitle: String, epochMs: Long, sourceTimeMs: Long): String {
        val cleaned = ProjectNaming.exportName(projectTitle).removeSuffix(".mp4")
        return "${cleaned.take(55)}_quadro_${epochMs.coerceAtLeast(0L)}_${sourceTimeMs.coerceAtLeast(0L)}.png"
    }
}
