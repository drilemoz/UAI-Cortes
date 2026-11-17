package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class CompactTimelineAndMusicTransferTest {
    private val trim = AudioTrack("content://trim", "Trilha", startMs = 900L,
        durationMs = 1_700L, sourceStartMs = 2_100L, volume = .42f, id = "trim",
        voiceTone = VoiceTone(intensity = .55f))
    private val voice = AudioTrack("content://voice", "Fala", id = "voice",
        durationMs = 1_000L, kind = VoiceIaPolicy.KIND)
    private val project = VideoProject(clips = listOf(Clip(uri = "content://video",
        title = "Vídeo", sourceDurationMs = 6_000L)), audioTracks = listOf(trim, voice))

    @Test fun transferPreservesExcerptAndDoesNotDuplicateTheSource() {
        val moved = requireNotNull(MusicTrackTransfer.move(project, trim, 8_000L, null))
        val music = requireNotNull(moved.music)
        assertEquals(listOf(voice), moved.audioTracks)
        assertEquals(trim.uri, music.uri)
        assertEquals(trim.title, music.title)
        assertEquals(trim.volume, music.volume)
        assertEquals(trim.startMs, music.startMs)
        assertEquals(trim.sourceStartMs, music.sourceStartMs)
        assertEquals(trim.durationMs, music.durationMs)
        assertEquals(trim.voiceTone, music.voiceTone)
        assertFalse(music.loops)
        assertEquals(music, Music.read(music.json()))
        assertEquals(ExportAudioPolicy.window(trim.startMs, trim.sourceStartMs, trim.durationMs,
            8_000L, project.durationMs), ExportAudioPolicy.musicWindow(music, project.durationMs))
        assertNull(project.music)
        assertEquals(listOf(trim, voice), project.audioTracks)
    }

    @Test fun replacementRequiresTheExactPreviouslyConfirmedMusic() {
        val existing = Music("content://old", "Anterior")
        val occupied = project.copy(music = existing)
        assertNull(MusicTrackTransfer.move(occupied, trim, 8_000L, null))
        assertNull(MusicTrackTransfer.move(occupied, trim, 8_000L, existing.copy(title = "Outra")))
        assertEquals(trim.uri, MusicTrackTransfer.move(occupied, trim, 8_000L, existing)?.music?.uri)
    }

    @Test fun linkedVoicesMissingMediaAndStaleSelectionsAreNotMoved() {
        val bound = trim.copy(clipId = project.clips.first().id)
        assertNull(MusicTrackTransfer.move(project.copy(audioTracks = listOf(bound)), bound, 8_000L, null))
        assertNull(MusicTrackTransfer.move(project, voice, 8_000L, null))
        assertNull(MusicTrackTransfer.move(project, trim.copy(volume = .10f), 8_000L, null))
        assertNull(MusicTrackTransfer.move(project, trim, 0L, null))
    }

    @Test fun lanesAreCompactAndExtensionIsBoundedByMediaZoomAndProject() {
        assertEquals(34, UnifiedTimelinePolicy.laneHeightDp(TimelineKind.MAIN_VIDEO))
        assertEquals(28, UnifiedTimelinePolicy.laneHeightDp(TimelineKind.MUSIC))
        assertEquals(24, UnifiedTimelinePolicy.laneHeightDp(TimelineKind.SUBTITLE))
        assertEquals(0L, UnifiedTimelinePolicy.endExtensionMs(60_000L, 0L, 1))
        assertEquals(15_000L, UnifiedTimelinePolicy.endExtensionMs(60_000L, 30_000L, 1))
        assertEquals(700L, UnifiedTimelinePolicy.endExtensionMs(60_000L, 700L, 1))
        assertEquals(2_500L, UnifiedTimelinePolicy.endExtensionMs(60_000L, 30_000L, 8))
    }
}
