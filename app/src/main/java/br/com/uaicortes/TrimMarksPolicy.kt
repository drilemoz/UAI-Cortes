package br.com.uaicortes

internal data class TrimWindow(val startMs: Long, val endMs: Long) {
    val durationMs: Long get() = endMs - startMs
    fun clamp(at: Long): Long = at.coerceIn(startMs, endMs)
}

internal object TrimMarksPolicy {
    fun clipWindow(clip: Clip): TrimWindow = TrimWindow(clip.startMs, clip.endMs)

    fun containsValid(clip: TrimWindow, kept: TrimWindow): Boolean =
        kept.startMs >= clip.startMs && kept.endMs <= clip.endMs &&
            kept.durationMs >= TrimLogic.MIN_SEGMENT_MS

    fun mark(clip: TrimWindow, kept: TrimWindow, cursorMs: Long, start: Boolean): TrimWindow? {
        val at = clip.clamp(cursorMs)
        val changed = if (start) kept.copy(startMs = at) else kept.copy(endMs = at)
        return changed.takeIf { containsValid(clip, it) }
    }

    fun step(clip: TrimWindow, cursorMs: Long, direction: Int, fps: Float?): Long =
        clip.startMs + FrameStepPolicy.target(clip.clamp(cursorMs) - clip.startMs, clip.durationMs, direction, fps)

    fun canRemove(kept: TrimWindow, removed: TrimWindow, canDeleteClip: Boolean): Boolean {
        if (!containsValid(kept, removed)) return false
        val left = removed.startMs - kept.startMs
        val right = kept.endMs - removed.endMs
        return (left == 0L || left >= TrimLogic.MIN_SEGMENT_MS) &&
            (right == 0L || right >= TrimLogic.MIN_SEGMENT_MS) && (left > 0L || right > 0L || canDeleteClip)
    }

    /** Pronto aplica as marcações de todos os clipes revistos, sem alterar os arquivos de origem. */
    fun applyAll(project: VideoProject, marks: Map<String, TrimWindow>): VideoProject? {
        var updated = project
        for ((id, keep) in marks) {
            val clip = updated.clips.firstOrNull { it.id == id && !it.isImage } ?: return null
            if (!containsValid(clipWindow(clip), keep)) return null
            updated = TrimSaveLogic.applyPending(updated, id, keep.startMs, keep.endMs) ?: return null
        }
        return updated
    }
}
