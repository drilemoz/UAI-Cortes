package br.com.uaicortes

import java.util.UUID

/** Keeps project audio compatible with legacy global tracks and the existing Media3 pipeline. */
internal object ClipAudioBindings {
    fun resolved(project: VideoProject): List<AudioTrack> {
        val starts = mutableMapOf<String, Pair<Clip, Long>>()
        var cursor = 0L
        project.clips.forEach { clip ->
            starts[clip.id] = clip to cursor
            cursor += clip.durationMs
        }
        return project.audioTracks.mapNotNull { audio ->
            val id = audio.clipId ?: return@mapNotNull audio
            val (clip, start) = starts[id] ?: return@mapNotNull null
            val window = ClipAudioTiming.resolve(start, clip.startMs, clip.durationMs,
                audio.clipSourceAnchorMs, audio.sourceStartMs, audio.durationMs)
                ?: return@mapNotNull null
            audio.copy(startMs=window.timelineStartMs, sourceStartMs=window.sourceStartMs,
                durationMs=window.durationMs)
        }
    }

    fun attach(project: VideoProject, clipId: String, source: AudioTrack): VideoProject? {
        val clip = project.clips.firstOrNull { it.id == clipId } ?: return null
        if (source.durationMs < AudioTrackTiming.MIN_TRIM_MS) return null
        val existing = project.audioTracks.firstOrNull { it.clipId == clipId }
        // Adjusting volume must not reset the time anchor of an existing recording.
        val attached = if (source.clipId == clipId && existing?.id == source.id &&
            existing.uri == source.uri) source.copy(volume = source.volume.coerceIn(0f, 1f))
        else source.copy(id = existing?.id ?: UUID.randomUUID().toString(), startMs = 0L,
            durationMs = source.durationMs.coerceAtMost(clip.durationMs),
            clipId = clip.id, clipSourceAnchorMs = clip.startMs)
        return project.copy(audioTracks=project.audioTracks.filterNot { it.clipId == clipId } + attached)
    }

    /** Clone only the audio bound to the duplicated/split video, never global tracks. */
    fun cloneAttached(project: VideoProject, oldId: String, newId: String): VideoProject =
        project.copy(audioTracks = project.audioTracks + project.audioTracks.filter { it.clipId == oldId }
            .map { it.copy(id=UUID.randomUUID().toString(), clipId=newId) })

    /** Removing a clip must not leave a hidden orphan track in storage. */
    fun pruneDeleted(project: VideoProject): VideoProject {
        val ids = project.clips.mapTo(mutableSetOf()) { it.id }
        return project.copy(audioTracks=project.audioTracks.filter { it.clipId == null || it.clipId in ids })
    }
}
