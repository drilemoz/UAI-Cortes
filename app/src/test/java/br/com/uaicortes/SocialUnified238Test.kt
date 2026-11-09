package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class SocialUnified238Test {
    private val account = SocialAccount(id = "local", destination = SocialDestination.YOUTUBE,
        label = "Meu canal", remoteChannelId = "UCabcdefghijklmnopqrstuv")

    @Test fun mergesConfirmedUploadWithRemoteVideoWithoutDuplicate() {
        val post = SocialPublication(accountId = account.id,
            videoUri = "content://example/video", videoName = "video.mp4",
            title = "Título local", status = SocialPublicationStatus.CONFIRMED_BY_API,
            remoteVideoId = "a1234567890", metrics = SocialMetrics(views = 7))
        val remote = YouTubeSocialApi.PreviousVideo(
            "a1234567890", "Título remoto", "2026-11-08T12:00:00Z", viewCount = 35,
            channelId = account.remoteChannelId, channelTitle = account.label)
        val merged = SocialYouTubeLibraryAggregate.mergeWithLocal(
            mapOf(account.remoteChannelId to listOf(remote)), listOf(post), listOf(account))
        assertEquals(1, merged.size)
        assertEquals("Título remoto", merged.first().title)
        assertEquals(35L, merged.first().viewCount)
        assertTrue(merged.first().fromUai)
    }

    @Test fun unknownVideoStaysVisibleAndUncertainUploadsStayOut() {
        val received = SocialPublication(accountId = account.id,
            videoUri = "content://example/video", videoName = "video.mp4",
            status = SocialPublicationStatus.CONFIRMED_BY_API,
            remoteVideoId = "a1234567890")
        val uncertain = received.copy(status = SocialPublicationStatus.UPLOAD_REVIEW,
            remoteVideoId = "b1234567890")
        val loaded = SocialYouTubeLibraryAggregate.mergeWithLocal(
            emptyMap(), listOf(received, uncertain), listOf(account))
        assertEquals(listOf("a1234567890"), loaded.map { it.id })
    }

    @Test fun totalsAreUnknownIfAnyChannelStatMissing() {
        val first = YouTubeSocialApi.ChannelStatistics(account.remoteChannelId, 100, 10, 20)
        val second = YouTubeSocialApi.ChannelStatistics("UC1111111111111111111111", null, 5, 3)
        val totals = SocialYouTubeLibraryAggregate.channelTotals(listOf(first, second))
        assertNull(totals.first)
        assertEquals(15L, totals.second)
        assertEquals(23L, totals.third)
        assertNull(SocialYouTubeLibraryAggregate.channelTotals(emptyList()).first)
    }
}
