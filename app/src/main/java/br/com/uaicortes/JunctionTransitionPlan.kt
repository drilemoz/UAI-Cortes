package br.com.uaicortes

/** Real transitions are centred on the existing cut. The primary audio and the entire
 * project clock are unchanged. Only the video opposite the existing cut is supplied
 * here, using unused source footage where possible, or a held edge otherwise. */
internal object JunctionTransitionPlan {
    const val DISSOLVE = "Dissolver"
    const val PUSH_LEFT = "Empurrão para esquerda"
    const val PUSH_RIGHT = "Empurrão para direita"
    val kinds = listOf(DISSOLVE, PUSH_LEFT, PUSH_RIGHT)

    data class Side(val timelineStartMs: Long, val lengthMs: Long, val clip: Clip,
                    val sourceStartMs: Long, val heldFrame: Boolean)
    data class Join(val kind: String, val cutMs: Long, val halfMs: Long,
                    val incoming: Side, val outgoing: Side) {
        val startMs get() = cutMs - halfMs
        val endMs get() = cutMs + halfMs
        val durationMs get() = halfMs * 2L
        val usesHold get() = incoming.heldFrame || outgoing.heldFrame
    }
    data class Placement(val mainAlpha: Float = 1f, val mainX: Float = 0f, val supportX: Float = 0f,
                         val supportVisible: Boolean = false)

    fun contains(kind: String) = kind in kinds
    fun duration(requestedMs: Long) = requestedMs.coerceIn(120L, 1000L)
    fun half(requestedMs: Long, aMs: Long, bMs: Long) =
        minOf(duration(requestedMs) / 2L, aMs / 2L, bMs / 2L).coerceAtLeast(0L)

    fun boundary(a: Clip, b: Clip, cutMs: Long, kind: String, requestedMs: Long): Join? {
        if (!contains(kind)) return null
        val halfMs = half(requestedMs, a.durationMs, b.durationMs)
        if (halfMs < 34L) return null // at least roughly one frame on each side
        val needsFirstFrame = !b.isImage && b.startMs < halfMs
        val needsLastFrame = !a.isImage && a.sourceDurationMs - a.endMs < halfMs
        return Join(kind, cutMs, halfMs,
            Side(cutMs - halfMs, halfMs, b,
                if (b.isImage || needsFirstFrame) b.startMs else b.startMs - halfMs, needsFirstFrame),
            Side(cutMs, halfMs, a,
                if (needsLastFrame) maxOf(a.startMs, a.endMs - 1L) else a.endMs, needsLastFrame))
    }

    fun project(project: VideoProject): List<Join> = buildList<Join> {
        var cutMs = 0L
        val byPair = project.transitions.groupBy { it.fromId to it.toId }
        project.clips.zipWithNext().forEach { (a, b) ->
            cutMs += a.durationMs
            val transition = byPair[a.id to b.id]?.firstOrNull() ?: return@forEach
            // Check the effect names, not the resulting list of Join objects.
            if (transition.kind !in kinds) return@forEach
            val join = requireNotNull(boundary(a, b, cutMs, transition.kind, transition.durationMs)) {
                "Clipes curtos demais para a transição em ${cutMs} ms. Escolha Corte direto ou revise o corte."
            }
            add(join)
        }
    }

    fun placement(joins: List<Join>, atUs: Long): Placement {
        // Windows are sorted and cannot overlap because each consumes at most half a clip.
        var lo = 0
        var hi = joins.lastIndex
        while (lo <= hi) {
            val mid = (lo + hi) ushr 1
            val join = joins[mid]
            val start = join.startMs * 1000L
            val end = join.endMs * 1000L
            when {
                atUs < start -> hi = mid - 1
                atUs >= end -> lo = mid + 1
                else -> {
                    val t = (atUs - start).toFloat() / (end - start).toFloat()
                    val p = t * t * (3f - 2f * t)
                    val before = atUs < join.cutMs * 1000L
                    if (join.kind == DISSOLVE) return Placement(
                        mainAlpha = if (before) 1f - p else p, supportVisible = true)
                    val direction = if (join.kind == PUSH_RIGHT) -1f else 1f
                    val a = -2f * direction * p
                    val b = 2f * direction * (1f - p)
                    return Placement(mainX = if (before) a else b,
                        supportX = if (before) b else a, supportVisible = true)
                }
            }
        }
        return Placement()
    }
}
