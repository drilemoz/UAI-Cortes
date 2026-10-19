package br.com.uaicortes

import java.util.UUID

/** Pure functions shared by the preview, persistence and the exporter. */
object EditFeatures {
    fun boundary(project: VideoProject, index: Int): ClipTransition? {
        if (index !in 0 until project.clips.lastIndex) return null
        val a = project.clips[index]
        val b = project.clips[index + 1]
        return project.transitions.firstOrNull { it.fromId == a.id && it.toId == b.id }
    }

    fun transitionMs(t: ClipTransition?, firstMs: Long, secondMs: Long): Long =
        if (t == null || t.kind == "Corte direto") 0L else
            t.durationMs.coerceIn(0L, minOf(firstMs, secondMs) / 2L)

    fun fadeFraction(clipMs: Long, clipDurationMs: Long, fadeInMs: Long,
                     fadeOutMs: Long): Float {
        val incoming = if (fadeInMs > 0) (clipMs.toFloat() / fadeInMs).coerceIn(0f,1f) else 1f
        val outgoing = if (fadeOutMs > 0) ((clipDurationMs - clipMs).toFloat() / fadeOutMs).coerceIn(0f,1f) else 1f
        return minOf(incoming, outgoing)
    }

    /** SRT import, restricted to a bounded number of cues to protect memory. */
    fun parseSrt(text: String, maxCues: Int = 1000): List<SubtitleCue> {
        val normalized = text.removePrefix("\uFEFF").replace("\r\n", "\n").replace('\r', '\n')
        val pattern = Regex("""(?m)^(\d{1,2}:)?\d{2}:\d{2}[,.]\d{1,3}\s*-->\s*(\d{1,2}:)?\d{2}:\d{2}[,.]\d{1,3}.*$""")
        val result = mutableListOf<SubtitleCue>()
        val blocks = normalized.split(Regex("\\n\\s*\\n"))
        for (block in blocks) {
            if (result.size >= maxCues) break
            val lines = block.lines().map { it.trim() }.filter { it.isNotBlank() }
            val timeIdx = lines.indexOfFirst { pattern.matches(it) }
            if (timeIdx < 0 || timeIdx + 1 >= lines.size) continue
            val range = lines[timeIdx].substringBefore("-->").trim() // start timestamp
            val end = lines[timeIdx].substringAfter("-->").trim().substringBefore(" ")
            val startMs = parseSrtTime(range) ?: continue
            val endMs = parseSrtTime(end) ?: continue
            val caption = lines.drop(timeIdx + 1).joinToString("\n").take(600).trim()
            if (caption.isNotBlank() && endMs > startMs)
                result += SubtitleCue(id = UUID.randomUUID().toString(), text = caption,
                    startMs = startMs, endMs = endMs)
        }
        return result
    }

    private fun parseSrtTime(input: String): Long? {
        val p = input.replace(',', '.').split(':')
        if (p.size != 3) return null
        val seconds = p[2].split('.')
        if (seconds.size != 2) return null
        val hh = p[0].toLongOrNull() ?: return null
        val mm = p[1].toLongOrNull() ?: return null
        val ss = seconds[0].toLongOrNull() ?: return null
        val fraction = seconds[1]
        if (mm > 59 || ss > 59 || fraction.length !in 1..3 || !fraction.all(Char::isDigit)) return null
        val ms = fraction.padEnd(3, '0').toLong()
        return ((hh * 3600 + mm * 60 + ss) * 1000 + ms).takeIf { it in 0L..86_400_000L }
    }

    fun repositionAfterDelete(old: VideoProject, newClips: List<Clip>) = old.copy(
        clips = newClips,
        transitions = old.transitions.filter { t ->
            newClips.zipWithNext().any { (a,b) -> a.id == t.fromId && b.id == t.toId }
        }
    )
}
