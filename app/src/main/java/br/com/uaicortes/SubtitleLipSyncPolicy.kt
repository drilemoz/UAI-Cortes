package br.com.uaicortes

import kotlin.math.abs

/** Visual evidence adjusts subtitle boundaries only. It never supplies words or phoneme timing. */
internal object SubtitleLipSyncPolicy {
    const val MAX_CUES = 4
    const val FRAME_STEP_MS = 100L
    const val MAX_SHIFT_MS = 300L
    private const val MIN_SHIFT_MS = 80L
    private const val MIN_DURATION_MS = 300L

    data class Window(val uri: String, val timelineMs: List<Long>, val sourceMs: List<Long>)
    data class Task(val id: Int, val cue: SubtitleCue, val start: Window?, val end: Window?)
    data class Evidence(val taskId: Int, val startIndex: Int?, val endIndex: Int?, val sameSpeaker: Boolean)
    data class Change(val before: SubtitleCue, val after: SubtitleCue)

    fun tasks(project: VideoProject, selected: List<SubtitleCue>): List<Task> {
        var base = 0L
        val clips = project.clips.mapIndexed { index, clip ->
            val lo = base
            base += clip.durationMs
            val before = EditFeatures.boundary(project, index - 1)
            val after = EditFeatures.boundary(project, index)
            val incoming = if (before == null) 0L else EditFeatures.transitionMs(before,
                project.clips.getOrElse(index - 1) { clip }.durationMs, clip.durationMs)
            val outgoing = if (after == null) 0L else EditFeatures.transitionMs(after, clip.durationMs,
                project.clips.getOrElse(index + 1) { clip }.durationMs)
            VisualClip(clip, lo, base, lo + incoming, base - outgoing)
        }
        return selected.take(MAX_CUES).mapIndexedNotNull { index, cue ->
            if (cue.startMs < 0 || cue.endMs > project.durationMs || cue.endMs - cue.startMs < MIN_DURATION_MS ||
                !usableWordTimes(cue)) return@mapIndexedNotNull null
            val media = clips.firstOrNull { cue.startMs >= it.start && cue.endMs <= it.end }
                ?.takeUnless { it.clip.isImage || it.clip.uri.isBlank() } ?: return@mapIndexedNotNull null
            fun window(anchor: Long): Window? {
                val times = (-4..4).map { anchor + it * FRAME_STEP_MS }
                    .filter { it >= media.safeStart && it < media.safeEnd }
                // Require frames on both sides, and avoid frames where only the main track would
                // omit an active visual layer. This feature is deliberately for the main video.
                if (times.size < 5 || times.first() >= anchor || times.last() <= anchor) return null
                if (project.overlays.any { it.startAtMs <= times.last() &&
                        it.startAtMs + it.durationMs > times.first() }) return null
                return Window(media.clip.uri, times, times.map { media.clip.startMs + (it - media.start) })
            }
            val start = window(cue.startMs)
            val end = window(cue.endMs)
            if (start == null && end == null) null else Task(index + 1, cue, start, end)
        }
    }

    private data class VisualClip(val clip: Clip, val start: Long, val end: Long,
        val safeStart: Long, val safeEnd: Long)

    private fun usableWordTimes(cue: SubtitleCue): Boolean {
        val words = cue.wordTimings
        if (words.isEmpty()) return true
        return words.size == SubtitleWordEffects.wordRanges(cue.text).size &&
            words.all { it.startMs >= cue.startMs && it.endMs <= cue.endMs && it.endMs > it.startMs } &&
            words.zipWithNext().all { (a, b) -> a.startMs <= b.startMs }
    }

    fun proposals(tasks: List<Task>, evidence: List<Evidence>, all: List<SubtitleCue>,
        durationMs: Long): List<Change> {
        val unique = evidence.groupBy { it.taskId }
        val sorted = all.sortedWith(compareBy<SubtitleCue> { it.startMs }.thenBy { it.id })
        return tasks.mapNotNull { task ->
            val cue = task.cue
            val result = unique[task.id]?.singleOrNull()?.takeIf { it.sameSpeaker } ?: return@mapNotNull null
            val i = sorted.indexOfFirst { it.id == cue.id }
            if (i < 0 || sorted[i] != cue || !usableWordTimes(cue)) return@mapNotNull null
            val leftEnd = sorted.take(i).maxOfOrNull { it.endMs } ?: 0L
            val rightStart = sorted.getOrNull(i + 1)?.startMs ?: durationMs
            if (leftEnd > cue.startMs || rightStart < cue.endMs) return@mapNotNull null
            // Share gaps at their midpoint so any subset of accepted changes remains safe.
            val minStart = if (i == 0) 0L else leftEnd + (cue.startMs - leftEnd + 1) / 2
            val maxEnd = if (i == sorted.lastIndex) durationMs else cue.endMs + (rightStart - cue.endMs) / 2
            val firstWord = cue.wordTimings.minOfOrNull { it.startMs }
            val lastWord = cue.wordTimings.maxOfOrNull { it.endMs }
            var start = allowed(task.start, result.startIndex, cue.startMs)
                ?.takeIf { it >= minStart && (firstWord == null || it <= firstWord) } ?: cue.startMs
            var end = allowed(task.end, result.endIndex, cue.endMs)
                ?.takeIf { it <= maxEnd && (lastWord == null || it >= lastWord) } ?: cue.endMs
            if (end - start < MIN_DURATION_MS) {
                // Do not salvage conflicting evidence by inventing a third timestamp.
                start = cue.startMs
                end = cue.endMs
            }
            if (start == cue.startMs && end == cue.endMs) null
            else Change(cue, cue.copy(startMs = start, endMs = end))
        }
    }

    private fun allowed(window: Window?, index: Int?, anchor: Long): Long? {
        val times = window?.timelineMs ?: return null
        if (index == null || index <= 0 || index >= times.lastIndex) return null
        return times[index].takeIf { abs(it - anchor) in MIN_SHIFT_MS..MAX_SHIFT_MS }
    }
}
