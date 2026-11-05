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
        return Result(ClipAudioBindings.cloneAttached(project.copy(clips = clips, transitions = transitions,
            lastTrimCutClipId = newId, lastTrimCutSourceMs = sourceMs), clip.id, newId), newId)
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

/** Operações de corte visual que alteram somente os intervalos do projeto.
 * O arquivo original nunca é regravado. */
internal object TrimRemoveRange {
    /** Converte um intervalo removido da fonte em dois trechos unidos na timeline.
     * As partes remanescentes devem ter no mínimo 250 ms.
     */
    fun apply(project: VideoProject, clipId: String, fromMs: Long, toMs: Long,
        firstNewId: String = UUID.randomUUID().toString(),
        secondNewId: String = UUID.randomUUID().toString()): TrimTimelineEdit.Result? {
        val index = project.clips.indexOfFirst { it.id == clipId }
        if (index < 0) return null
        val clip = project.clips[index]
        if (clip.isImage || fromMs < clip.startMs || toMs > clip.endMs ||
            toMs - fromMs < TrimLogic.MIN_SEGMENT_MS) return null
        val leftMs = fromMs - clip.startMs
        val rightMs = clip.endMs - toMs
        if ((leftMs > 0L && leftMs < TrimLogic.MIN_SEGMENT_MS) ||
            (rightMs > 0L && rightMs < TrimLogic.MIN_SEGMENT_MS)) return null
        if (leftMs == 0L && rightMs == 0L && project.clips.size == 1) return null

        val beforeDuration = project.clips.take(index).sumOf { it.durationMs }
        val globalFrom = beforeDuration + leftMs
        val globalTo = globalFrom + (toMs - fromMs)
        val result: TrimTimelineEdit.Result = when {
            leftMs == 0L && rightMs == 0L -> TrimTimelineEdit.remove(project, clipId) ?: return null
            leftMs == 0L -> {
                // Reaproveitar o ID mantém vínculos de áudio/seleção e transições posteriores.
                val changed = project.copy(clips = project.clips.mapIndexed { i, c ->
                    if (i == index) c.copy(startMs = toMs) else c
                }, lastTrimCutClipId = null, lastTrimCutSourceMs = null)
                TrimTimelineEdit.Result(changed, clipId)
            }
            rightMs == 0L -> {
                val changed = project.copy(clips = project.clips.mapIndexed { i, c ->
                    if (i == index) c.copy(endMs = fromMs) else c
                }, lastTrimCutClipId = null, lastTrimCutSourceMs = null)
                TrimTimelineEdit.Result(changed, clipId)
            }
            else -> {
                if (firstNewId == clipId || firstNewId.isBlank() ||
                    firstNewId == secondNewId || secondNewId.isBlank() ||
                    project.clips.any { it.id == firstNewId || it.id == secondNewId }) return null
                val left = clip.copy(endMs = fromMs)
                val right = clip.copy(id = secondNewId, startMs = toMs)
                val clips = project.clips.toMutableList().apply {
                    set(index, left)
                    add(index + 1, right)
                }
                val following = project.clips.getOrNull(index + 1)?.id
                val transitions = project.transitions.map { t ->
                    if (t.fromId == clipId && t.toId == following) t.copy(fromId = right.id) else t
                }
                val updated = ClipAudioBindings.cloneAttached(project.copy(clips = clips,
                    transitions = transitions, lastTrimCutClipId = right.id,
                    lastTrimCutSourceMs = toMs), clipId, right.id)
                TrimTimelineEdit.Result(updated, right.id)
            }
        }
        return result.copy(project = shiftGlobalItems(result.project, globalFrom, globalTo))
    }

    private fun shiftGlobalItems(project: VideoProject, from: Long, to: Long): VideoProject {
        val removed = to - from
        fun adjusted(time: Long): Long = when {
            time <= from -> time
            time >= to -> time - removed
            else -> from
        }
        return project.copy(
            subtitles = project.subtitles.mapNotNull { cue ->
                val begin = adjusted(cue.startMs)
                val end = adjusted(cue.endMs)
                if (end <= begin) null else cue.copy(startMs = begin, endMs = end,
                    wordTimings = cue.wordTimings.mapNotNull { word ->
                        val wStart = adjusted(word.startMs)
                        val wEnd = adjusted(word.endMs)
                        if (wEnd <= wStart) null else word.copy(startMs = wStart, endMs = wEnd)
                    })
            },
            overlays = project.overlays.mapNotNull { overlay ->
                val begin = adjusted(overlay.startAtMs)
                val end = adjusted(overlay.startAtMs + overlay.durationMs)
                if (end <= begin) null else overlay.copy(startAtMs = begin,
                    durationMs = end - begin)
            },
            audioTracks = project.audioTracks.map { audio ->
                if (audio.clipId != null) audio else audio.copy(startMs = adjusted(audio.startMs))
            }
        )
    }
}
