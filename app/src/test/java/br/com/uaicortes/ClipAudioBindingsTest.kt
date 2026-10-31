package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class ClipAudioBindingsTest {
    private val first = Clip(id="first",uri="content://video1",title="Vídeo 1",sourceDurationMs=15000)
    private val second = Clip(id="second",uri="content://video2",title="Vídeo 2",sourceDurationMs=8000)
    private val base = VideoProject(clips=listOf(first,second))

    @Test fun attachedAudioStartsOnChosenClipNotAtProjectBeginning() {
        val audio = AudioTrack("content://voice", "Narração", durationMs=6000, volume=.8f)
        val project = ClipAudioBindings.attach(base,"second",audio)!!
        val track = ClipAudioBindings.resolved(project).single()
        assertEquals("second",track.clipId)
        assertEquals(15000L,track.startMs)
        assertEquals(6000L,track.durationMs)
        assertEquals(.8f,track.volume)
    }

    @Test fun movingClipRepositionsVoiceWithoutRewritingAudioTimestamps() {
        val initial = ClipAudioBindings.attach(base,"second",AudioTrack("u","t",durationMs=6000))!!
        val moved = initial.copy(clips=listOf(second,first))
        assertEquals(0L,ClipAudioBindings.resolved(moved).single().startMs)
        assertEquals(15000L,ClipAudioBindings.resolved(initial).single().startMs)
    }

    @Test fun trimmingFromBeginningTrimsTheLinkedNarrationSource() {
        val project = ClipAudioBindings.attach(base,"first",AudioTrack("u","t",durationMs=8000))!!
        val cut = project.copy(clips=listOf(first.copy(startMs=3000),second))
        val audio = ClipAudioBindings.resolved(cut).single()
        assertEquals(0L,audio.startMs)
        assertEquals(3000L,audio.sourceStartMs)
        assertEquals(5000L,audio.durationMs)
    }

    @Test fun splitKeepsAudioAlignedAcrossBothNewSegments() {
        val project = ClipAudioBindings.attach(base,"first",AudioTrack("u","t",durationMs=10000))!!
        val split = TrimTimelineEdit.split(project,"first",4000,"right")!!.project
        val tracks = ClipAudioBindings.resolved(split)
        assertEquals(2,tracks.size)
        val left = tracks.first { it.clipId == "first" }
        val right = tracks.first { it.clipId == "right" }
        assertEquals(4000L,left.durationMs)
        assertEquals(4000L,right.sourceStartMs)
        assertEquals(4000L,right.startMs)
        assertEquals(6000L,right.durationMs)
        assertNotEquals(left.id,right.id)
    }

    @Test fun duplicatingAudioAndDeletingClipDoesNotLeaveOrphans() {
        val project = ClipAudioBindings.attach(base,"first",AudioTrack("u","t",durationMs=10000))!!
        val third = first.copy(id="third")
        val duplicated = ClipAudioBindings.cloneAttached(project.copy(clips=listOf(first,third,second)),"first","third")
        assertEquals(2,duplicated.audioTracks.size)
        assertNotEquals(duplicated.audioTracks[0].id,duplicated.audioTracks[1].id)
        val deleted = EditFeatures.repositionAfterDelete(duplicated,listOf(third,second))
        assertEquals("third",deleted.audioTracks.single().clipId)
        assertEquals(0L,ClipAudioBindings.resolved(deleted).single().startMs)
    }

    @Test fun changingVolumeDoesNotReanchorAudioAfterTrimming() {
        val initial=ClipAudioBindings.attach(base,"first",AudioTrack("u","t",durationMs=8000))!!
        val cut=initial.copy(clips=listOf(first.copy(startMs=2000),second))
        val unchanged=cut.audioTracks.single()
        val updated=ClipAudioBindings.attach(cut,"first",unchanged.copy(volume=.3f))!!
        assertEquals(unchanged.clipSourceAnchorMs,updated.audioTracks.single().clipSourceAnchorMs)
        assertEquals(2000L,ClipAudioBindings.resolved(updated).single().sourceStartMs)
    }

    @Test fun legacyTracksKeepTheirOriginalProjectPositions() {
        val old=AudioTrack("u","old",startMs=5000,durationMs=2000)
        val project=base.copy(audioTracks=listOf(old))
        assertEquals(old,ClipAudioBindings.resolved(project).single())
        assertEquals(old,VideoProject.read(project.json()).audioTracks.single())
    }

    @Test fun jsonRoundTripKeepsClipBinding() {
        val project=ClipAudioBindings.attach(base,"second",AudioTrack("u","t",durationMs=6000))!!
        val restored=VideoProject.read(project.json())
        assertEquals("second",restored.audioTracks.single().clipId)
        assertEquals(15000L,ClipAudioBindings.resolved(restored).single().startMs)
    }
}
