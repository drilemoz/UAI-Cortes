package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class TimelineDirectEditTest {
    private fun video(id:String, length:Long=10_000L) = Clip(id=id,uri="content://demo/$id",
        title=id,sourceDurationMs=length)
    private fun photo(id:String,length:Long=3_000L)=Clip(id=id,uri="content://demo/$id",
        title=id,sourceDurationMs=length,endMs=length,isImage=true,imageMimeType="image/jpeg")

    @Test fun trimMainVideoNeverExtendsBeyondOriginalSource() {
        val p=VideoProject(clips=listOf(video("v")))
        assertFalse(TimelineDirectEdit.main(p,"v",TimelineHandle.RIGHT,5_000).changed)
        val shortened=TimelineDirectEdit.main(p,"v",TimelineHandle.LEFT,3_000)
        assertTrue(shortened.changed)
        assertEquals(3_000L,shortened.project.clips.single().startMs)
        assertEquals(7_000L,shortened.project.durationMs)
        val extended=TimelineDirectEdit.main(shortened.project,"v",TimelineHandle.LEFT,-100_000)
        assertEquals(0L,extended.project.clips.single().startMs)
        assertEquals(10_000L,extended.project.durationMs)
    }

    @Test fun trimmingVideoBeginningMovesContainedSubtitlesWithContent() {
        val cue=SubtitleCue(text="Fala",startMs=5_000,endMs=6_000)
        val p=VideoProject(clips=listOf(video("v")),subtitles=listOf(cue))
        val result=TimelineDirectEdit.main(p,"v",TimelineHandle.LEFT,3_000)
        assertTrue(result.changed)
        assertEquals(2_000L,result.project.subtitles.single().startMs)
        assertEquals(3_000L,result.project.subtitles.single().endMs)
    }

    @Test fun trimmingOverCaptionRequiresMovingCaptionFirst() {
        val cue=SubtitleCue(text="Fala",startMs=1_000,endMs=2_000)
        val p=VideoProject(clips=listOf(video("v")),subtitles=listOf(cue))
        val result=TimelineDirectEdit.main(p,"v",TimelineHandle.LEFT,3_000)
        assertFalse(result.changed)
        assertTrue(result.explanation.contains("Mova o elemento"))
    }

    @Test fun photoDurationCanGrowButNeverBelowOneSecond() {
        val p=VideoProject(clips=listOf(photo("cover"),video("v")))
        val longer=TimelineDirectEdit.main(p,"cover",TimelineHandle.RIGHT,30_000)
        assertEquals(33_000L,longer.project.clips[0].durationMs)
        assertEquals(43_000L,longer.project.durationMs)
        val shorter=TimelineDirectEdit.main(p,"cover",TimelineHandle.RIGHT,-50_000)
        assertEquals(1_000L,shorter.project.clips[0].durationMs)
    }

    @Test fun mainReorderingMovesFullyContainedSubtitlesAndPip() {
        val p=VideoProject(clips=listOf(video("a"),video("b")),
            subtitles=listOf(SubtitleCue(text="Oi",startMs=1_000,endMs=3_000)),
            overlays=listOf(Overlay("content://demo/overlay",true,2_000,startAtMs=2_000)))
        val moved=TimelineDirectEdit.reorder(p,"a",14_000)
        assertEquals(listOf("b","a"),moved.project.clips.map {it.id})
        assertEquals(11_000L,moved.project.subtitles.single().startMs)
        assertEquals(12_000L,moved.project.overlays.single().startAtMs)
    }

    @Test fun trimPipVideoTracksSourceOffsetAndRespectsDuration() {
        val p=VideoProject(clips=listOf(video("v",20_000)),overlays=listOf(
            Overlay("content://demo/pip",false,8_000,startAtMs=2_000)))
        val trimmed=TimelineDirectEdit.overlay(p,p.overlays[0].id,TimelineHandle.LEFT,3_000,10_000)
        assertTrue(trimmed.changed)
        assertEquals(5_000L,trimmed.project.overlays[0].startAtMs)
        assertEquals(5_000L,trimmed.project.overlays[0].durationMs)
        assertEquals(3_000L,trimmed.project.overlays[0].sourceStartMs)
        val grown=TimelineDirectEdit.overlay(trimmed.project,p.overlays[0].id,
            TimelineHandle.RIGHT,50_000,10_000)
        assertEquals(7_000L,grown.project.overlays[0].durationMs)
    }

    @Test fun temporalCollisionBlocksUnauthorizedPipOverlap() {
        val first=Overlay("content://demo/one",true,3_000,startAtMs=0,x=.5f,y=.5f)
        val second=Overlay("content://demo/two",true,3_000,startAtMs=8_000,x=.5f,y=.5f)
        val p=VideoProject(clips=listOf(video("v",20_000)),overlays=listOf(first,second))
        val result=TimelineDirectEdit.overlay(p,second.id,TimelineHandle.MOVE,-7_000,null)
        assertFalse(result.changed)
        assertTrue(result.explanation.contains("sobrepõem"))
    }

    @Test fun moveAudioPreservesSourceAndTrimCannotExceedSource() {
        val track=AudioTrack("content://demo/sound","Som",startMs=2_000,
            durationMs=4_000,sourceStartMs=1_000)
        val p=VideoProject(clips=listOf(video("v",20_000)),audioTracks=listOf(track))
        val moved=TimelineDirectEdit.audio(p,track.id,TimelineHandle.MOVE,5_000,10_000)
        assertEquals(7_000L,moved.project.audioTracks[0].startMs)
        assertEquals(1_000L,moved.project.audioTracks[0].sourceStartMs)
        val grown=TimelineDirectEdit.audio(p,track.id,TimelineHandle.RIGHT,99_000,7_000)
        assertEquals(6_000L,grown.project.audioTracks[0].durationMs)
    }

    @Test fun magneticSnapCanBeAppliedOrDisabledByTheUi() {
        assertEquals(5_000L,TimelineMagnet.snap(5_200L,listOf(0L,5_000L,10_000L)))
        assertEquals(5_800L,TimelineMagnet.snap(5_800L,listOf(0L,5_000L,10_000L)))
    }

    @Test fun attachedAudioRefusesIndependentMove() {
        val track=AudioTrack("content://demo/sound","Fala",startMs=0,
            durationMs=2_000,clipId="v")
        val p=VideoProject(clips=listOf(video("v")),audioTracks=listOf(track))
        val result=TimelineDirectEdit.audio(p,track.id,TimelineHandle.MOVE,1_000,5_000)
        assertFalse(result.changed)
        assertTrue(result.explanation.contains("vinculado"))
    }

    @Test fun subtitleMovePreservesWordsAndTrimDropsInvalidTiming() {
        val cue=SubtitleCue(id="s",text="Oi",startMs=1_000,endMs=3_000,
            wordTimings=listOf(SubtitleWordTiming(1_100,2_900)))
        val p=VideoProject(clips=listOf(video("v")),subtitles=listOf(cue))
        val moved=TimelineDirectEdit.subtitle(p,"s",TimelineHandle.MOVE,2_000)
        assertEquals(3_100L,moved.project.subtitles[0].wordTimings[0].startMs)
        val trimmed=TimelineDirectEdit.subtitle(p,"s",TimelineHandle.LEFT,1_500)
        assertEquals(2_500L,trimmed.project.subtitles[0].startMs)
        assertTrue(trimmed.project.subtitles[0].wordTimings.isEmpty())
    }
}
