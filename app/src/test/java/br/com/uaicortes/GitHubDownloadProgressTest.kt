package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class GitHubDownloadProgressTest {
    @Test fun speedUsesBytesAndElapsedMilliseconds() {
        assertEquals(1_048_576L, GitHubDownloadProgress.rateBytesPerSecond(524_288L, 500L))
        assertEquals(0L, GitHubDownloadProgress.rateBytesPerSecond(0L, 500L))
        assertEquals(0L, GitHubDownloadProgress.rateBytesPerSecond(100L, 0L))
        assertEquals(0L, GitHubDownloadProgress.rateBytesPerSecond(-1L, 50L))
    }

    @Test fun progressLabelsAreClearAndDoNotAssumeAnOfficialSpeed() {
        assertEquals("1,0 MiB", GitHubDownloadProgress.readableBytes(1_048_576L))
        assertEquals("1,00 MiB/s", GitHubDownloadProgress.readableRate(1_048_576L))
        assertTrue(GitHubDownloadProgress.readableRate(0L).contains("Calculando"))
    }

    @Test fun remainingTimeIsEstimatedOnlyWithValidSpeedAndTotal() {
        assertEquals(5L, GitHubDownloadProgress.remainingSeconds(5_000L, 10_000L, 1_000L) ?: -1L)
        assertNull(GitHubDownloadProgress.remainingSeconds(5_000L, 10_000L, 0L))
        assertNull(GitHubDownloadProgress.remainingSeconds(10_000L, 10_000L, 1_000L))
        assertNull(GitHubDownloadProgress.remainingSeconds(1L, 0L, 1_000L))
    }
}
