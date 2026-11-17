package br.com.uaicortes

/** Uma ação atômica: promove a fonte existente sem duplicar mídia ou apagar outras faixas. */
internal object MusicTrackTransfer {
    fun eligible(track: AudioTrack): Boolean = track.kind == "audio" && track.clipId == null

    fun move(project: VideoProject, selected: AudioTrack, sourceDurationMs: Long,
        existingMusicToReplace: Music?): VideoProject? {
        val current = project.audioTracks.singleOrNull { it.id == selected.id } ?: return null
        if (current != selected || !eligible(current) || project.music != existingMusicToReplace ||
            current.uri.isBlank() || current.startMs < 0L || current.sourceStartMs < 0L ||
            current.durationMs < 0L || !current.volume.isFinite()) return null
        val music = Music(current.uri, current.title, current.volume, startMs = current.startMs,
            sourceDurationMs = sourceDurationMs, repeat = false, sourceStartMs = current.sourceStartMs,
            durationMs = current.durationMs, voiceTone = current.voiceTone)
        if (ExportAudioPolicy.musicWindow(music, project.durationMs) == null) return null
        return project.copy(music = music, audioTracks = project.audioTracks.filterNot { it.id == current.id })
    }
}
