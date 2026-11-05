package br.com.uaicortes

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class ImageClipPolicyTest {
    private val original = Clip(uri = "content://example/video1", title = "Vídeo",
        sourceDurationMs = 8_000L)
    private val another = Clip(uri = "content://example/video2", title = "Segundo",
        sourceDurationMs = 7_000L)

    @Test fun photoStartsTimelineAsCover() {
        val photo = ImageClipPolicy.create("content://example/capa", "Capa.png", "image/png", 3)
        val clips = ImageClipPolicy.insert(listOf(original, another), photo,
            ImageClipPolicy.Placement.START)
        assertEquals(listOf(photo.id, original.id, another.id), clips.map { it.id })
        assertEquals(3_000L, clips.first().durationMs)
        assertEquals(18_000L, VideoProject(clips = clips).durationMs)
        assertEquals(0f, photo.volume)
    }

    @Test fun insertionAroundSelectedClipAndAtEnd() {
        val photo = ImageClipPolicy.create("content://example/capa", "Foto", "image/jpeg", 5)
        val existing = listOf(original, another)
        assertEquals(1, ImageClipPolicy.index(existing, another.id,
            ImageClipPolicy.Placement.BEFORE_SELECTED))
        assertEquals(1, ImageClipPolicy.index(existing, original.id,
            ImageClipPolicy.Placement.AFTER_SELECTED))
        assertEquals(2, ImageClipPolicy.index(existing, "missing", ImageClipPolicy.Placement.END))
        assertEquals(photo.id, ImageClipPolicy.insert(existing, photo,
            ImageClipPolicy.Placement.AFTER_SELECTED, original.id)[1].id)
        assertEquals(photo.id, ImageClipPolicy.insert(existing, photo,
            ImageClipPolicy.Placement.END).last().id)
    }

    @Test fun changingPhotoDurationPreservesIdentityAndOriginalVideo() {
        val photo = ImageClipPolicy.create("content://example/capa", "Capa", "image/webp")
        val longer = ImageClipPolicy.changeDuration(photo, 10)
        assertEquals(photo.id, longer.id)
        assertEquals(10_000L, longer.durationMs)
        assertEquals(0L, longer.startMs)
        assertEquals(longer.durationMs, longer.sourceDurationMs)
        assertFalse(original.isImage)
        assertEquals(8_000L, original.durationMs)
    }

    @Test fun oldVideoJsonAndNewPhotoJsonStayCompatible() {
        val restoredOld = Clip.read(original.json().apply {
            remove("isImage"); remove("imageMimeType")
        })
        assertFalse(restoredOld.isImage)
        val photo = ImageClipPolicy.create("content://example/capa", "Capa", "image/png", 5)
        val project = VideoProject(clips = listOf(photo, restoredOld))
        val encoded = project.json()
        assertEquals(4, encoded.getInt("schemaVersion"))
        val restored = VideoProject.read(JSONObject(encoded.toString()))
        assertEquals(listOf(true, false), restored.clips.map { it.isImage })
        assertEquals("image/png", restored.clips.first().imageMimeType)
        assertEquals(13_000L, restored.durationMs)
    }

    @Test fun coverKeepsExistingCaptionsAndPipAligned() {
        val cue = SubtitleCue(text="Fala no vídeo", startMs=1000L, endMs=2200L,
            wordTimings=listOf(SubtitleWordTiming(1000L, 1400L),
                SubtitleWordTiming(1500L, 2200L)))
        val overlay = Overlay(uri="content://example/pip", isImage=true,
            durationMs=2000L, startAtMs=500L)
        val track = AudioTrack(uri="content://example/audio", title="Narração",
            startMs=700L, durationMs=2000L)
        val project = VideoProject(clips=listOf(original), subtitles=listOf(cue),
            overlays=listOf(overlay), audioTracks=listOf(track))
        val cover = ImageClipPolicy.create("content://example/cover", "capa", "image/jpeg", 4)
        val updated = ImageClipPolicy.insertIntoProject(project, cover,
            ImageClipPolicy.Placement.START)
        assertEquals(4_000L, updated.clips.first().durationMs)
        assertEquals(5_000L, updated.subtitles.single().startMs)
        assertEquals(6_200L, updated.subtitles.single().endMs)
        assertEquals(5_000L, updated.subtitles.single().wordTimings.first().startMs)
        assertEquals(4_500L, updated.overlays.single().startAtMs)
        assertEquals(4_700L, updated.audioTracks.single().startMs)
    }

    @Test fun resizePhotoRepositionsLaterElementsButNotMusicOrClipBoundAudio() {
        val cover = ImageClipPolicy.create("content://example/cover", "capa", "image/png", 3)
        val music = Music("content://example/music", "Trilha")
        val attached = AudioTrack(uri="content://example/voice", title="Voz", clipId=original.id,
            startMs=0L, durationMs=2000L)
        val global = AudioTrack(uri="content://example/global", title="Extra",
            startMs=3500L, durationMs=1000L)
        val initial = VideoProject(clips=listOf(cover,original),
            audioTracks=listOf(attached,global), music=music,
            subtitles=listOf(SubtitleCue(text="Depois da capa", startMs=4000L,endMs=5000L)))
        val extended = ImageClipPolicy.resizeInProject(initial, cover.id, 7)
        assertEquals(7_000L, extended.clips.first().durationMs)
        assertEquals(8_000L, extended.subtitles.single().startMs)
        assertEquals(7_500L, extended.audioTracks.last().startMs)
        assertEquals(0L, extended.audioTracks.first().startMs)
        assertEquals(music, extended.music)
    }

    @Test fun invalidSourcesDoNotBecomeImageClips() {
        assertThrows(IllegalArgumentException::class.java) {
            ImageClipPolicy.create("file:///tmp/cover.png", "Capa", "image/png")
        }
        assertThrows(IllegalArgumentException::class.java) {
            ImageClipPolicy.create("content://example/animation", "GIF", "image/gif")
        }
        assertEquals(1, ImageClipPolicy.seconds(-20))
        assertEquals(30, ImageClipPolicy.seconds(500))
    }
}
