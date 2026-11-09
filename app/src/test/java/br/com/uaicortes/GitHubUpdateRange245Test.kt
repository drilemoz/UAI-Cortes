package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class GitHubUpdateRange245Test {
    @Test fun acceptsMatching206Range() {
        assertTrue(GitHubUpdateCore.matchingContentRange("bytes 512-1023/1024",512,1024))
    }
    @Test fun refusesMismatchedOrMalformedIntervals() {
        assertFalse(GitHubUpdateCore.matchingContentRange("bytes 0-1023/1024",512,1024))
        assertFalse(GitHubUpdateCore.matchingContentRange("bytes 512-1023/1025",512,1024))
        assertFalse(GitHubUpdateCore.matchingContentRange("bytes 512-1024/1024",512,1024))
        assertFalse(GitHubUpdateCore.matchingContentRange(null,512,1024))
        assertFalse(GitHubUpdateCore.matchingContentRange("bytes */1024",512,1024))
    }
}
