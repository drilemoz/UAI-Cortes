package br.com.uaicortes

/** Edição não destrutiva: só altera referências e intervalos do projeto.
 * Todas as operações de um gesto são calculadas e persistidas UMA vez, ao soltar.
 */
internal enum class TimelineHandle { MOVE, LEFT, RIGHT }
internal enum class TimelineKind { MAIN_VIDEO, MAIN_IMAGE, PIP, AUDIO, VOICE, SUBTITLE }
internal data class TimelineEditResult(val project: VideoProject, val explanation: String = "", val changed: Boolean = false)

internal object TimelineDirectEdit {
    const val MIN_VIDEO_MS = 250L
    const val MIN_IMAGE_MS = 1_000L
    const val MAX_IMAGE_MS = 15L * 60_000L

    /** Resolve o alvo da reordenação sem criar lacunas no vídeo principal. */
    fun reorder(project: VideoProject, id: String, targetStartMs: Long): TimelineEditResult {
        val origin = project.clips.indexOfFirst { it.id == id }
        if (origin < 0) return TimelineEditResult(project, "Clipe indisponível")
        val clip = project.clips[origin]
        val other = project.clips.filterNot { it.id == id }
        var position = 0L
        val midpoint = (targetStartMs + clip.durationMs / 2).coerceAtLeast(0L)
        var destination = other.size
        for ((i, candidate) in other.withIndex()) {
            if (midpoint < position + candidate.durationMs / 2) { destination = i; break }
            position += candidate.durationMs
        }
        val changed = other.toMutableList().apply { add(destination, clip) }
        if (changed.map { it.id } == project.clips.map { it.id }) return TimelineEditResult(project)
        // Elementos totalmente contidos no clipe acompanham a nova posição.
        // Elementos que atravessam cortes não são deslocados silenciosamente.
        fun spans(clips: List<Clip>): Map<String, Pair<Long,Long>> {
            var at=0L
            return clips.associate { c ->
                val first=at; at+=c.durationMs; c.id to (first to at)
            }
        }
        val before=spans(project.clips)
        val after=spans(changed)
        fun offset(start:Long,end:Long):Long? {
            val clipId=before.entries.firstOrNull { (_,range) ->
                start>=range.first && end<=range.second
            }?.key ?: return null
            return after.getValue(clipId).first-before.getValue(clipId).first
        }
        var crossed=0
        fun countMiss(start:Long,end:Long) { if(offset(start,end)==null) crossed++ }
        val moved=EditFeatures.repositionAfterDelete(project,changed).copy(
            overlays=project.overlays.map { layer ->
                countMiss(layer.startAtMs,layer.startAtMs+layer.durationMs)
                layer.copy(startAtMs=layer.startAtMs+(offset(layer.startAtMs,
                    layer.startAtMs+layer.durationMs)?:0L))
            },
            subtitles=project.subtitles.map { cue ->
                val delta=offset(cue.startMs,cue.endMs)
                if(delta==null) crossed++
                cue.copy(startMs=cue.startMs+(delta?:0L),endMs=cue.endMs+(delta?:0L),
                    wordTimings=cue.wordTimings.map { w -> w.copy(
                        startMs=w.startMs+(delta?:0L),endMs=w.endMs+(delta?:0L)) })
            },
            audioTracks=project.audioTracks.map { track ->
                if(track.clipId!=null) track else {
                    val delta=offset(track.startMs,track.startMs+track.durationMs)
                    if(delta==null) crossed++
                    track.copy(startMs=track.startMs+(delta?:0L))
                }
            }
        )
        val note=if(crossed>0) "Clipe movido; $crossed elementos atravessam cortes e mantiveram seu horário. Revise-os."
            else "Clipe reposicionado com elementos internos sincronizados"
        return TimelineEditResult(moved,note,true)
    }

    fun main(project: VideoProject, id: String, handle: TimelineHandle, deltaMs: Long,
        targetStartMs: Long = 0L): TimelineEditResult {
        if (handle == TimelineHandle.MOVE) return reorder(project, id, targetStartMs)
        val index = project.clips.indexOfFirst { it.id == id }
        if (index < 0) return TimelineEditResult(project, "Clipe indisponível")
        val old = project.clips[index]
        val minimum = if (old.isImage) MIN_IMAGE_MS else MIN_VIDEO_MS
        val next = if (old.isImage) {
            val length = (old.durationMs + if (handle == TimelineHandle.LEFT) -deltaMs else deltaMs)
                .coerceIn(minimum, MAX_IMAGE_MS)
            old.copy(sourceDurationMs = length, startMs = 0L, endMs = length)
        } else if (handle == TimelineHandle.LEFT) {
            old.copy(startMs = TimelinePrecision.step(old.startMs, deltaMs,
                (old.endMs - minimum).coerceAtLeast(0L)))
        } else {
            old.copy(endMs = TimelinePrecision.step(old.endMs, deltaMs,
                old.sourceDurationMs.coerceAtLeast(old.startMs + minimum))
                .coerceAtLeast(old.startMs + minimum))
        }
        if (old == next) return TimelineEditResult(project,
            "Limite da mídia original ou duração mínima atingido")
        val begin = project.clips.take(index).sumOf { it.durationMs }
        val oldEnd = begin + old.durationMs
        val lengthDelta = next.durationMs-old.durationMs
        val sourceDelta = if (old.isImage || handle!=TimelineHandle.LEFT) 0L
            else next.startMs-old.startMs
        // Proteção: não apagar silenciosamente elementos inteiramente contidos
        // na parte do clipe que deixará de ser exibida após o recorte.
        val removed = when {
            sourceDelta>0L -> begin to (begin+sourceDelta)
            lengthDelta<0L -> (oldEnd+lengthDelta) to oldEnd
            else -> null
        }
        if (removed!=null) {
            fun containedAndAffected(start:Long,end:Long):Boolean =
                start>=begin && end<=oldEnd && start<removed.second && end>removed.first
            val hasCues=project.subtitles.any { containedAndAffected(it.startMs,it.endMs) }
            val hasPips=project.overlays.any {
                containedAndAffected(it.startAtMs,it.startAtMs+it.durationMs) }
            val hasAudio=project.audioTracks.any { it.clipId==null &&
                containedAndAffected(it.startMs,it.startMs+it.durationMs) }
            if (hasCues || hasPips || hasAudio) return TimelineEditResult(project,
                "Há legenda, PiP ou áudio no trecho removido. Mova o elemento antes de aparar.")
        }
        val shifted = shiftAfter(project, oldEnd, lengthDelta)
        // Aparar a entrada de vídeo desloca conteúdo contido na área mantida.
        // Áudio associado por clipId já segue o recorte via ClipAudioBindings.
        val adjusted = if(sourceDelta!=0L) {
            fun deltaFor(start:Long,end:Long):Long =
                if(start>=begin && end<=oldEnd) -sourceDelta else 0L
            shifted.copy(
                overlays=shifted.overlays.map { layer ->
                    layer.copy(startAtMs=layer.startAtMs+deltaFor(layer.startAtMs,
                        layer.startAtMs+layer.durationMs))
                },
                subtitles=shifted.subtitles.map { cue ->
                    val offset=deltaFor(cue.startMs,cue.endMs)
                    cue.copy(startMs=cue.startMs+offset,endMs=cue.endMs+offset,
                        wordTimings=cue.wordTimings.map {w -> w.copy(
                            startMs=w.startMs+offset,endMs=w.endMs+offset)})
                },
                audioTracks=shifted.audioTracks.map { track ->
                    if(track.clipId!=null) track else track.copy(startMs=track.startMs+
                        deltaFor(track.startMs,track.startMs+track.durationMs))
                }
            )
        } else shifted
        return TimelineEditResult(adjusted.copy(clips=project.clips.mapIndexed { i, c ->
            if (i == index) next else c
        }), if (old.isImage) "Duração da imagem ajustada" else "Recorte do vídeo ajustado", true)
    }

    fun overlay(project: VideoProject, id: String, handle: TimelineHandle, deltaMs: Long,
        sourceDurationMs: Long?, aspectRatio: (Overlay)->Float = {1f},
        canvasAspect: Float = 9f/16f): TimelineEditResult {
        val old = project.overlays.firstOrNull { it.id == id } ?: return TimelineEditResult(project,"PiP indisponível")
        val total = project.durationMs
        val min = if (old.isImage) MIN_IMAGE_MS else MIN_VIDEO_MS
        val end = old.startAtMs + old.durationMs
        val next = when (handle) {
            TimelineHandle.MOVE -> old.copy(startAtMs=TimelinePrecision.step(old.startAtMs,
                deltaMs, (total-old.durationMs).coerceAtLeast(0L)))
            TimelineHandle.LEFT -> {
                val earliest = if (old.isImage) 0L else
                    (old.startAtMs - old.sourceStartMs).coerceAtLeast(0L)
                val latest = (end-min).coerceAtLeast(old.startAtMs.coerceAtMost(end))
                val start = TimelinePrecision.step(old.startAtMs,deltaMs,latest).coerceAtLeast(earliest)
                val offset = if (old.isImage) 0L else old.sourceStartMs + start-old.startAtMs
                old.copy(startAtMs=start,sourceStartMs=offset.coerceAtLeast(0L),durationMs=end-start)
            }
            TimelineHandle.RIGHT -> {
                val sourceMax = if (old.isImage) MAX_IMAGE_MS else sourceDurationMs?.let {
                    (it-old.sourceStartMs).coerceAtLeast(0L)
                } ?: old.durationMs // Sem metadados NÃO estender a fonte desconhecida.
                if(sourceMax<min) return TimelineEditResult(project,
                    "Não foi possível confirmar a duração da mídia original")
                val max = minOf((total-old.startAtMs).coerceAtLeast(min),sourceMax)
                val length = (old.durationMs + deltaMs).coerceIn(min, max.coerceAtLeast(min))
                old.copy(durationMs=length)
            }
        }
        if (old == next) return TimelineEditResult(project,"Limite do PiP ou da mídia original atingido")
        if (next.startAtMs+next.durationMs>total || next.durationMs<min)
            return TimelineEditResult(project,"O PiP precisa caber no vídeo")
        // Verifica tempo E espaço. Conflitos novos exigem autorização no editor PiP.
        val requested=project.overlays.map { if(it.id==id)next else it }
        val slots=requested.map { layer ->
            val ratio=aspectRatio(layer).takeIf { it.isFinite() && it>0f }?:1f
            val (w,h)=OverlayEditorLogic.dimensions(layer.scale,ratio,canvasAspect,layer.fitMode)
            PlacementItem(layer.id,layer.startAtMs,layer.durationMs,layer.x,layer.y,w,h)
        }
        val approved=requested.flatMap { layer -> layer.overlapWith.map {
            OverlayPlacement.pair(layer.id,it) } }.toSet()
        val beforeSlots=project.overlays.map { layer ->
            val ratio=aspectRatio(layer).takeIf { it.isFinite() && it>0f }?:1f
            val (w,h)=OverlayEditorLogic.dimensions(layer.scale,ratio,canvasAspect,layer.fitMode)
            PlacementItem(layer.id,layer.startAtMs,layer.durationMs,layer.x,layer.y,w,h)
        }
        val preexisting=OverlayPlacement.collisions(beforeSlots)-approved
        val conflicts=OverlayPlacement.collisions(slots)-approved-preexisting
        if(conflicts.isNotEmpty()) return TimelineEditResult(project,
            "PiPs se sobrepõem. Ajuste a posição ou autorize na ferramenta PiP.")
        return TimelineEditResult(project.copy(overlays=project.overlays.map { if (it.id==id) next else it }),
            "PiP atualizado",true)
    }

    fun audio(project: VideoProject, id: String, handle: TimelineHandle,
        deltaMs: Long, sourceDurationMs: Long?): TimelineEditResult {
        val old = project.audioTracks.firstOrNull { it.id==id }
            ?: return TimelineEditResult(project,"Faixa de áudio não encontrada")
        if (old.clipId != null) return TimelineEditResult(project,
            "Áudio vinculado: ele acompanha o vídeo. Edite no menu do clipe.")
        if (old.durationMs < AudioTrackTiming.MIN_TRIM_MS) return TimelineEditResult(project,"Áudio muito curto")
        val draft = AudioTimelineDraft(old.startMs,old.sourceStartMs,old.sourceStartMs+old.durationMs)
        val next = when(handle) {
            TimelineHandle.MOVE -> AudioTimelineEdit.move(draft,deltaMs,project.durationMs)
            TimelineHandle.LEFT -> AudioTimelineEdit.left(draft,deltaMs)
            TimelineHandle.RIGHT -> AudioTimelineEdit.right(draft,deltaMs,
                sourceDurationMs ?: draft.sourceEndMs,project.durationMs)
        }
        val track = old.copy(startMs=next.timelineStartMs,sourceStartMs=next.sourceStartMs,
            durationMs=next.durationMs)
        if (old == track) return TimelineEditResult(project,"Limite do áudio original ou do projeto atingido")
        return TimelineEditResult(project.copy(audioTracks=project.audioTracks.map { if(it.id==id) track else it }),
            "Áudio atualizado",true)
    }

    fun subtitle(project:VideoProject,id:String,handle:TimelineHandle,deltaMs:Long): TimelineEditResult {
        val old=project.subtitles.firstOrNull { it.id==id }
            ?: return TimelineEditResult(project,"Legenda indisponível")
        val newRange=when(handle) {
            TimelineHandle.MOVE -> SubtitleTimelineEdit.move(old.startMs,old.endMs,deltaMs,project.durationMs)
            TimelineHandle.LEFT -> SubtitleTimelineEdit.trimLeft(old.startMs,old.endMs,deltaMs,project.durationMs)
            TimelineHandle.RIGHT -> SubtitleTimelineEdit.trimRight(old.startMs,old.endMs,deltaMs,project.durationMs)
        }
        if (newRange.first==old.startMs && newRange.second==old.endMs) return TimelineEditResult(project)
        val timings = if (handle==TimelineHandle.MOVE) old.wordTimings.map {
            it.copy(startMs=it.startMs+newRange.first-old.startMs,
                endMs=it.endMs+newRange.first-old.startMs)
        } else old.wordTimings.filter { it.startMs>=newRange.first && it.endMs<=newRange.second }
            .takeIf { it.size==old.wordTimings.size } ?: emptyList()
        val cue=old.copy(startMs=newRange.first,endMs=newRange.second,wordTimings=timings)
        return TimelineEditResult(project.copy(subtitles=project.subtitles.map {if(it.id==id) cue else it}),
            "Legenda atualizada",true)
    }

    /** Desloca somente eventos posteriores à fronteira, preservando vínculos por clipId. */
    private fun shiftAfter(project:VideoProject, boundary:Long, delta:Long):VideoProject {
        if(delta==0L) return project
        fun moved(t:Long)=if(t>=boundary) (t+delta).coerceAtLeast(0L) else t
        return project.copy(
            overlays=project.overlays.map {it.copy(startAtMs=moved(it.startAtMs))},
            audioTracks=project.audioTracks.map {if(it.clipId==null)it.copy(startMs=moved(it.startMs)) else it},
            subtitles=project.subtitles.map {cue->cue.copy(startMs=moved(cue.startMs),endMs=moved(cue.endMs),
                wordTimings=cue.wordTimings.map { it.copy(startMs=moved(it.startMs),endMs=moved(it.endMs)) })}
        )
    }
}

/** Encaixe opcional e determinístico em pontos conhecidos da linha do tempo.
 * Nunca move eventos por conta própria; apenas ajusta o delta do gesto em curso.
 */
internal object TimelineMagnet {
    private const val THRESHOLD_MS = 350L

    fun snap(valueMs: Long, targets: List<Long>, thresholdMs: Long = THRESHOLD_MS): Long {
        val nearby=targets.minByOrNull { kotlin.math.abs(it-valueMs) } ?: return valueMs
        return if (kotlin.math.abs(nearby-valueMs) <= thresholdMs) nearby else valueMs
    }

    fun adjust(project: VideoProject, kind: TimelineKind, id: String,
        handle: TimelineHandle, deltaMs: Long, playheadMs: Long): Long {
        val starts=UnifiedTimelinePoints.main(project.clips)
        val entry=when(kind) {
            TimelineKind.MAIN_VIDEO,TimelineKind.MAIN_IMAGE -> starts[id]
            TimelineKind.PIP -> project.overlays.firstOrNull {it.id==id}?.let {it.startAtMs to it.durationMs}
            TimelineKind.AUDIO,TimelineKind.VOICE -> ClipAudioBindings.resolved(project)
                .firstOrNull {it.id==id}?.let {it.startMs to it.durationMs}
            TimelineKind.SUBTITLE -> project.subtitles.firstOrNull {it.id==id}?.let {
                it.startMs to it.endMs-it.startMs }
        } ?: return deltaMs
        if(kind==TimelineKind.MAIN_VIDEO || kind==TimelineKind.MAIN_IMAGE) {
            if(handle!=TimelineHandle.MOVE) return deltaMs
        }
        val anchor=entry.first+(if(handle==TimelineHandle.RIGHT)entry.second else 0L)+deltaMs
        val targets=buildList {
            add(0L);add(project.durationMs);add(playheadMs)
            starts.filterKeys {it!=id}.values.forEach {add(it.first);add(it.first+it.second)}
            project.overlays.filterNot {it.id==id}.forEach {add(it.startAtMs);add(it.startAtMs+it.durationMs)}
            ClipAudioBindings.resolved(project).filterNot {it.id==id}.forEach {
                add(it.startMs);add(it.startMs+it.durationMs) }
            project.subtitles.filterNot {it.id==id}.forEach {add(it.startMs);add(it.endMs)}
        }
        return deltaMs+(snap(anchor,targets)-anchor)
    }
}

internal object UnifiedTimelinePoints {
    fun main(clips: List<Clip>): Map<String,Pair<Long,Long>> {
        var at=0L
        return clips.associate {c ->val begin=at;at+=c.durationMs;c.id to (begin to c.durationMs)}
    }
}
