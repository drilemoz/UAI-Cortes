package br.com.uaicortes

/** Planejamento de PiP sem UI/Android. Aspectos são informados pelo chamador. */
internal sealed interface OverlayUpdate {
    data class Confirm(val conflicts: Set<Pair<String, String>>) : OverlayUpdate
    data class Rejected(val reason: String) : OverlayUpdate
    data class Applied(val project: VideoProject) : OverlayUpdate
}

internal sealed interface OverlayInsertion {
    data object LimitReached : OverlayInsertion
    data object ConfirmSequential : OverlayInsertion
    data class Proposed(val requested: List<Overlay>, val selectedId: String) : OverlayInsertion
}

internal object OverlayOperations {
    fun apply(current: VideoProject, requested: List<Overlay>, preferredId: String?,
              canvasAspect: Float, mediaAspect: (Overlay) -> Float,
              decision: String = "ask"): OverlayUpdate {
        val slots = requested.map { layer ->
            val (width, height) = OverlayEditorLogic.dimensions(layer.scale, mediaAspect(layer), canvasAspect)
            PlacementItem(layer.id, layer.startAtMs, layer.durationMs, layer.x, layer.y, width, height)
        }
        val ids = requested.map { it.id }.toSet()
        val allowed = requested.flatMap { layer ->
            layer.overlapWith.filter { it in ids }.map { OverlayPlacement.pair(layer.id, it) }
        }.toSet()
        val newConflicts = OverlayPlacement.collisions(slots) - allowed
        if (newConflicts.isNotEmpty() && decision == "ask") return OverlayUpdate.Confirm(newConflicts)
        val approvedPairs = if (decision == "keep") allowed + newConflicts else allowed
        val updated = requested.map { layer ->
            if (decision == "keep" && layer.id == preferredId) {
                val linked = newConflicts.mapNotNull { (a, b) ->
                    when (layer.id) { a -> b; b -> a; else -> null }
                }
                layer.copy(overlapWith = (layer.overlapWith + linked).distinct())
            } else layer
        }
        val arranged = OverlayPlacement.arrange(slots, preferredId, approvedPairs)
            ?: return OverlayUpdate.Rejected("Não há espaço para distribuir esses PiPs. Reduza o tamanho ou ajuste a duração.")
        return OverlayUpdate.Applied(current.copy(overlays = updated.map { layer ->
            val (x, y) = arranged.getValue(layer.id)
            layer.copy(x = x, y = y)
        }))
    }

    fun insert(project: VideoProject, uri: String, isImage: Boolean, mediaMs: Long,
               selectedId: String, replacing: Boolean, positionMs: Long,
               allowTimeOverlap: Boolean = false): OverlayInsertion {
        val previous = if (replacing) project.overlays.firstOrNull { it.id == selectedId } else null
        if (previous == null && project.overlays.size >= OverlayPlacement.MAX_PIPS)
            return OverlayInsertion.LimitReached
        val sequential = if (project.overlays.isEmpty())
            positionMs.coerceIn(0L, (project.durationMs - 1L).coerceAtLeast(0L))
        else OverlayTiming.nextStart(project.overlays, project.durationMs)
        if (previous == null && sequential == null && !allowTimeOverlap)
            return OverlayInsertion.ConfirmSequential
        val start = (previous?.startAtMs ?: sequential ?:
            OverlayTiming.suggestedOverlapStart(project.overlays, project.durationMs))
            .coerceIn(0L, (project.durationMs - 1L).coerceAtLeast(0L))
        val remaining = (project.durationMs - start).coerceAtLeast(1L)
        val length = (if (isImage) previous?.durationMs ?: 5000L else mediaMs).coerceIn(1L, remaining)
        val inserted = previous?.copy(uri = uri, isImage = isImage, durationMs = length)
            ?: Overlay(uri, isImage, length, startAtMs = start, scale = .30f)
        val requested = if (previous != null) project.overlays.map {
            if (it.id == previous.id) inserted else it
        } else project.overlays + inserted
        return OverlayInsertion.Proposed(requested, inserted.id)
    }

    fun remove(project: VideoProject, id: String): Pair<VideoProject, String> =
        project.copy(overlays = project.overlays.filterNot { it.id == id }) to
            project.overlays.firstOrNull { it.id != id }?.id.orEmpty()
}
