package br.com.uaicortes

internal data class ExportSample(val project: VideoProject, val playFromMs: Long, val playUntilMs: Long)

internal object ExportSamplePlan {
    fun prepare(p: VideoProject, position: Long, length: Long, sourceDuration: (String) -> Long): ExportSample {
        require(length > 0 && p.durationMs > 0)
        val duration = minOf(length, p.durationMs)
        val start = position.coerceIn(0, p.durationMs - duration)
        val end = start + duration
        var from = start
        var until = end
        val transitions = mutableListOf<ClipTransition>()
        var cut = 0L
        // Keep enough handles for the original fade duration even at the selection edges.
        for (i in 0 until p.clips.lastIndex) {
            cut += p.clips[i].durationMs
            val transition = EditFeatures.boundary(p, i) ?: continue
            val fade = EditFeatures.transitionMs(transition,
                p.clips[i].durationMs, p.clips[i + 1].durationMs)
            if (fade > 0 && cut - fade < end && cut + fade > start) {
                transitions += transition
                from = minOf(from, cut - 2 * fade)
                until = maxOf(until, cut + 2 * fade)
            }
        }
        var cursor = 0L
        val clips = p.clips.mapNotNull { clip ->
            val base = cursor
            cursor += clip.durationMs
            val lo = maxOf(base, from)
            val hi = minOf(cursor, until)
            if (hi <= lo) null else clip.copy(startMs = clip.startMs + lo - base,
                endMs = clip.startMs + hi - base)
        }
        val cues = p.subtitles.filter { it.startMs < until && it.endMs > from }.map { cue ->
            cue.copy(startMs = cue.startMs - from, endMs = cue.endMs - from,
                wordTimings = cue.wordTimings.map { it.copy(startMs = it.startMs - from, endMs = it.endMs - from) })
        }
        val overlays = p.overlays.mapNotNull { pip ->
            val lo = maxOf(from, pip.startAtMs)
            val hi = minOf(until, pip.startAtMs + pip.durationMs)
            if (hi <= lo) null else pip.copy(startAtMs = lo - from, durationMs = hi - lo,
                sourceStartMs = pip.sourceStartMs + if (pip.isImage) 0L else lo - pip.startAtMs)
        }
        val tracks = mutableListOf<AudioTrack>()
        fun addTrack(track: AudioTrack, window: ExportAudioPolicy.Window?) {
            if (window == null) return
            val lo = maxOf(from, window.timelineStartMs)
            val hi = minOf(until, window.timelineStartMs + window.playableMs)
            if (hi > lo) tracks += track.copy(startMs = lo - from, durationMs = hi - lo,
                sourceStartMs = window.sourceStartMs + lo - window.timelineStartMs, clipId = null)
        }
        val durations = mutableMapOf<String, Long>()
        fun measured(uri: String) = durations.getOrPut(uri) {
            sourceDuration(uri).also { require(it > 0) { "Não foi possível ler a duração do áudio." } }
        }
        ExportAudioPolicy.activeExtraTracks(p).filter { it.startMs < until }.forEach { track ->
            addTrack(track, ExportAudioPolicy.window(track.startMs, track.sourceStartMs,
                track.durationMs, measured(track.uri), p.durationMs))
        }
        val music = ExportAudioPolicy.activeMusic(p)
        if (music != null) {
            val period = measured(music.uri)
            val track = AudioTrack(uri = music.uri, title = music.title, volume = music.volume, voiceTone = music.voiceTone)
            if (music.loops) {
                var at = from
                while (at < until) {
                    val offset = at % period
                    val span = minOf(period - offset, until - at)
                    tracks += track.copy(startMs = at - from, sourceStartMs = offset, durationMs = span)
                    at += span
                }
            } else addTrack(track, ExportAudioPolicy.musicWindow(music, p.durationMs, period))
        }
        val short = p.copy(clips = clips, subtitles = cues, overlays = overlays, audioTracks = tracks,
            music = null, transitions = transitions)
        return ExportSample(short, start - from, end - from)
    }
}
