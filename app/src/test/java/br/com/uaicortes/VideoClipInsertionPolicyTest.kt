package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class VideoClipInsertionPolicyTest {
    private val a = Clip(uri="content://video/a", title="A", sourceDurationMs=8000L)
    private val b = Clip(uri="content://video/b", title="B", sourceDurationMs=6000L)
    private val added = Clip(uri="content://video/c", title="C", sourceDurationMs=4000L)
    private val another = Clip(uri="content://video/d", title="D", sourceDurationMs=2000L)

    @Test fun insertBeforeMovesLaterTimedElements() {
        val cue = SubtitleCue(text="Fala B",startMs=8300L,endMs=9500L,
            wordTimings=listOf(SubtitleWordTiming(8300L,8600L)))
        val pip = Overlay(uri="content://pip/logo",isImage=true,durationMs=500L,startAtMs=9000L)
        val audio = AudioTrack(uri="content://audio/narracao",title="Narração",
            startMs=8500L,durationMs=1200L)
        val bound = AudioTrack(uri="content://audio/clip", title="Áudio clipe",
            clipId=b.id, startMs=0L,durationMs=800L)
        val before = VideoProject(clips=listOf(a,b),subtitles=listOf(cue),
            overlays=listOf(pip),audioTracks=listOf(audio,bound))
        val after = VideoClipInsertionPolicy.insert(before,listOf(added, another),
            ImageClipPolicy.Placement.BEFORE_SELECTED,b.id)
        assertEquals(listOf(a.id,added.id,another.id,b.id),after.clips.map { it.id })
        assertEquals(20000L,after.durationMs)
        assertEquals(14300L,after.subtitles.single().startMs)
        assertEquals(14600L,after.subtitles.single().wordTimings.single().endMs)
        assertEquals(15000L,after.overlays.single().startAtMs)
        assertEquals(14500L,after.audioTracks.first().startMs)
        assertEquals(0L,after.audioTracks.last().startMs)
        assertEquals(b.id,after.audioTracks.last().clipId)
        assertEquals(4,after.json().getInt("schemaVersion"))
    }

    @Test fun insertBeginningAndEndRespectOrdering() {
        val base = VideoProject(clips=listOf(a,b))
        val start = VideoClipInsertionPolicy.insert(base,listOf(added),
            ImageClipPolicy.Placement.START,b.id)
        val end = VideoClipInsertionPolicy.insert(base,listOf(added),
            ImageClipPolicy.Placement.END,b.id)
        assertEquals(added.id,start.clips.first().id)
        assertEquals(added.id,end.clips.last().id)
        assertEquals(listOf(a.id,b.id),base.clips.map { it.id })
    }

    @Test fun rejectBadOrDuplicateInputs() {
        val base = VideoProject(clips=listOf(a,b))
        assertThrows(IllegalArgumentException::class.java) {
            VideoClipInsertionPolicy.insert(base,listOf(a),ImageClipPolicy.Placement.END,a.id)
        }
        assertThrows(IllegalArgumentException::class.java) {
            VideoClipInsertionPolicy.insert(base,listOf(added,added),ImageClipPolicy.Placement.END,a.id)
        }
        val photo = ImageClipPolicy.create("content://image/photo", "Foto", "image/png")
        assertThrows(IllegalArgumentException::class.java) {
            VideoClipInsertionPolicy.insert(base,listOf(photo),ImageClipPolicy.Placement.START,a.id)
        }
    }
}
