package br.com.uaicortes

import java.util.UUID

/** Operações da lista de trechos, sem alterar os arquivos de vídeo originais. */
object TrimTimelineEdit {
    data class Result(val project: VideoProject, val selectedClipId: String)

    /** Corta no tempo da fonte, seleciona a parte da direita e memoriza a fronteira. */
    fun split(project: VideoProject, clipId: String, sourceMs: Long,
              newId: String = UUID.randomUUID().toString()): Result? {
        val index = project.clips.indexOfFirst { it.id == clipId }
        if (index < 0 || newId.isBlank() || project.clips.any { it.id == newId }) return null
        val clip = project.clips[index]
        if (!TrimLogic.canSplit(clip.startMs, clip.endMs, sourceMs)) return null
        val right = clip.copy(id = newId, startMs = sourceMs)
        val clips = project.clips.toMutableList()
        clips[index] = clip.copy(endMs = sourceMs)
        clips.add(index + 1, right)
        // Uma transição já existente entre o clipe original e o próximo
        // continua na fronteira original, agora após a metade direita.
        val followingId = project.clips.getOrNull(index + 1)?.id
        val transitions = project.transitions.map { transition ->
            if (transition.fromId == clip.id && transition.toId == followingId)
                transition.copy(fromId = newId) else transition
        }
        return Result(project.copy(clips = clips, transitions = transitions,
            lastTrimCutClipId = newId, lastTrimCutSourceMs = sourceMs), newId)
    }

    /** Posição inicial da régua para o clipe, priorizando o último ponto dividido. */
    fun cursorFor(project: VideoProject, clip: Clip): Long =
        (if (clip.id == project.lastTrimCutClipId) project.lastTrimCutSourceMs else null)
            ?.coerceIn(clip.startMs, clip.endMs) ?: clip.startMs

    /** Ao abrir um projeto, priorizar o último trecho recém-dividido, quando ainda existir. */
    fun resumeClipId(project: VideoProject): String =
        project.lastTrimCutClipId?.takeIf { id -> project.clips.any { it.id == id } }
            ?: project.clips.firstOrNull()?.id.orEmpty()

    /** Selecionar vizinho do clipe excluído e evitar ponteiro de corte pendurado. */
    fun remove(project: VideoProject, clipId: String): Result? {
        val index = project.clips.indexOfFirst { it.id == clipId }
        if (index < 0 || project.clips.size < 2) return null
        val remaining = project.clips.filterIndexed { i, _ -> i != index }
        val adjacent = remaining[index.coerceAtMost(remaining.lastIndex)]
        val markedClipId = project.lastTrimCutClipId
        val lastPoint = project.lastTrimCutSourceMs
        val keptMark = if (markedClipId == clipId) {
            remaining.firstOrNull { it.uri == project.clips[index].uri &&
                lastPoint != null && lastPoint in it.startMs..it.endMs }?.id
        } else markedClipId?.takeIf { id -> remaining.any { it.id == id } }
        val updated = EditFeatures.repositionAfterDelete(project, remaining).copy(
            lastTrimCutClipId = keptMark,
            lastTrimCutSourceMs = lastPoint.takeIf { keptMark != null })
        return Result(updated, adjacent.id)
    }
}
