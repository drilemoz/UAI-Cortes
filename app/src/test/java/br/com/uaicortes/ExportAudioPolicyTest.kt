package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class ExportAudioPolicyTest {
    @Test fun silentAndNonFiniteVolumesNeverEnterExtraAudioComposition() {
        assertFalse(ExportAudioPolicy.audible(0f))
        assertFalse(ExportAudioPolicy.audible(-1f))
        assertFalse(ExportAudioPolicy.audible(Float.NaN))
        assertFalse(ExportAudioPolicy.audible(Float.POSITIVE_INFINITY))
        assertTrue(ExportAudioPolicy.audible(.4f))
        val project = VideoProject(clips = listOf(Clip(uri="content://clip", title="Base",
            sourceDurationMs=6000L)), audioTracks = listOf(
            AudioTrack(uri="content://muted", title="Silencioso", volume=0f),
            AudioTrack(uri="content://voice", title="Voz", startMs=1000L, volume=.8f)),
            music = Music(uri="content://music", title="Música", volume=0f))
        assertEquals(listOf("content://voice"), ExportAudioPolicy.activeExtraTracks(project).map { it.uri })
        assertTrue(ExportAudioPolicy.hasRequiredAudio(project))
        assertFalse(ExportAudioPolicy.hasRequiredAudio(project.copy(audioTracks=emptyList())))
    }

    @Test fun silentPipDoesNotCountAsVerifiedRequiredAudio() {
        val project = VideoProject(clips = listOf(Clip(uri="content://base", title="Base",
            sourceDurationMs=1200L)), overlays = listOf(
            Overlay(uri="content://pip", isImage=false, durationMs=1000L,
                volume=1f, muted=false)))
        // A verificação da trilha real é feita pelo ExportMp4Inspector no Android.
        assertFalse(ExportAudioPolicy.hasRequiredAudio(project))
    }

    @Test fun preservesSourceTrimTimelinePositionAndEndGaps() {
        val window = ExportAudioPolicy.window(1500, 250, 2000, 6000, 5000)!!
        assertEquals(1500L, window.timelineStartMs)
        assertEquals(250L, window.sourceStartMs)
        assertEquals(2000L, window.playableMs)
        assertEquals(1500L, window.trailingMs)
        val clipped = ExportAudioPolicy.window(3500, 1500, 5000, 3000, 5000)!!
        assertEquals(1500L, clipped.playableMs)
        assertEquals(0L, clipped.trailingMs)
    }

    @Test fun unknownDurationNeverBecomesOneSecondAndInvalidWindowsAreRejected() {
        assertNull(ExportAudioPolicy.window(0, 0, 0, 0, 5000))
        assertNull(ExportAudioPolicy.window(0, 6000, 0, 6000, 5000))
        assertNull(ExportAudioPolicy.window(5000, 0, 1000, 6000, 5000))
        assertNull(ExportAudioPolicy.window(0, 0, 1000, 6000, 0))
        assertEquals(3000L, ExportAudioPolicy.window(0, 1000, 0, 4000, 5000)!!.playableMs)
    }
}
