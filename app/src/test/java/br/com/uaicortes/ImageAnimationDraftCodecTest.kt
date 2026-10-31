package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class ImageAnimationDraftCodecTest {
    private val first = "content://media/external/images/media/10"
    private val second = "content://media/external/images/media/11"
    private val old = ImageDraftSnapshot(listOf(first), ImageAnimationOptions())

    @Test fun legacyDraftWithoutFpsStillLoads() {
        val legacy = """{"images":["$first"],"options":{"intensity":"normal","secondsPerImage":5,"aspect":"9:16","quality":720,"transition":"auto"}}"""
        val parsed = ImageAnimationDraftCodec.decode(legacy)
        assertEquals(30, parsed.options.fps)
        assertEquals(listOf(first), parsed.images)
    }

    @Test fun roundTripPreservesOrderAndOptions() {
        val next = ImageDraftSnapshot(listOf(second, first), ImageAnimationOptions(quality = 1080, transition = "preto"))
        assertEquals(next, ImageAnimationDraftCodec.decode(ImageAnimationDraftCodec.encode(next.images, next.options)))
    }

    @Test fun missingPrimaryUsesValidBackup() {
        val backup = ImageAnimationDraftCodec.encode(old.images, old.options)
        assertEquals(ImageDraftStatus.BACKUP, ImageAnimationDraftCodec.inspect(null, backup).status)
        assertEquals(old, ImageAnimationDraftCodec.inspect("{broken", backup).snapshot)
    }

    @Test fun validPrimaryIsPreferredOverBackup() {
        val primary = ImageAnimationDraftCodec.encode(listOf(second), old.options)
        val backup = ImageAnimationDraftCodec.encode(old.images, old.options)
        assertEquals(listOf(second), ImageAnimationDraftCodec.inspect(primary, backup).snapshot?.images)
    }

    @Test fun damagedDraftNeverTurnsIntoNewEmptyDraft() {
        assertEquals(ImageDraftStatus.DAMAGED, ImageAnimationDraftCodec.inspect("{broken", null).status)
        assertEquals(ImageDraftStatus.DAMAGED, ImageAnimationDraftCodec.inspect("{broken", "[]").status)
        assertThrows(IllegalStateException::class.java) {
            ImageAnimationDraftCodec.prepareWrite("{broken", "[]", old)
        }
    }

    @Test fun nextSaveKeepsLastValidSnapshotInsteadOfCorruptPrimary() {
        val backup = ImageAnimationDraftCodec.encode(old.images, old.options)
        val (safe, current) = ImageAnimationDraftCodec.prepareWrite("{broken", backup,
            ImageDraftSnapshot(listOf(second), old.options))
        assertEquals(backup, safe)
        assertEquals(listOf(second), ImageAnimationDraftCodec.decode(current).images)
    }

    @Test fun invalidUrisAndDuplicateImagesAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            ImageAnimationDraftCodec.encode(listOf("file:///tmp/photo.jpg"), old.options)
        }
        assertThrows(IllegalArgumentException::class.java) {
            ImageAnimationDraftCodec.encode(listOf(first, first), old.options)
        }
        assertEquals(ImageDraftStatus.DAMAGED, ImageAnimationDraftCodec.inspect(
            """{"images":["file:///tmp/a.jpg"],"options":{}}""", null).status)
    }

    @Test fun unavailableImagesAreReportedWithoutRemovingOthers() {
        val list = listOf(first, second)
        assertEquals(listOf(second), ImageDraftAccessPolicy.inaccessible(list) { it == first })
        assertEquals(listOf(first, second), list)
    }
}
