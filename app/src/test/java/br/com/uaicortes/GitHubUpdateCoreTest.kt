package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class GitHubUpdateCoreTest {
    @Test fun comparesNumericVersionsInsteadOfLexicographic() {
        assertTrue(GitHubUpdateCore.newer("v0.1.100", "0.1.99"))
        assertFalse(GitHubUpdateCore.newer("v0.1.99", "0.1.100"))
        assertFalse(GitHubUpdateCore.newer("v0.1.100", "0.1.100"))
        assertFalse(GitHubUpdateCore.newer("invalid", "0.1.99"))
        assertTrue(GitHubUpdateCore.newer("v1.0.0", "0.99.999"))
    }

    @Test fun onlyAcceptsRepositoryPaths() {
        assertEquals("owner/repo", GitHubUpdateCore.normalizedRepository("https://github.com/owner/repo/"))
        assertNull(GitHubUpdateCore.normalizedRepository("https://example.com/owner/repo"))
        assertNull(GitHubUpdateCore.normalizedRepository("owner/repo/extra"))
        assertNull(GitHubUpdateCore.normalizedRepository("owner/../repo"))
    }

    @Test fun prereleasesAreIncludedAndHighestVersionWins() {
        val json = """[
            ${release("v0.1.100", true)},
            ${release("v0.1.101", true)}
        ]"""
        val selected = GitHubUpdateCore.parseReleases(json, "owner/repo", "0.1.99")
        assertEquals("0.1.101", selected?.version)
        assertTrue(selected?.apkName?.endsWith("universal.apk") == true)
    }

    @Test fun ignoresDraftsAndOldVersions() {
        val json = "[${release("v0.1.101", true, draft = true)},${release("v0.1.99", false)}]"
        assertNull(GitHubUpdateCore.parseReleases(json, "owner/repo", "0.1.100"))
    }

    @Test fun ignoresWrongAssetAndForeignHost() {
        val json = "[${release("v0.1.101", true).replace("https://github.com/owner/repo/releases/download", "https://evil.example/releases/download")}]"
        assertNull(GitHubUpdateCore.parseReleases(json, "owner/repo", "0.1.100"))
    }

    @Test fun parsesLargeReleaseBodyBeyondPreviousReaderLimit() {
        val raw = release("v0.1.186", true)
            .replace("\"body\":\"Nova versão\"", "\"body\":\"${"x".repeat(1_300_000)}\"")
        val found = GitHubUpdateCore.parseReleases("[$raw]", "owner/repo", "0.1.185")
        assertEquals("0.1.186", found?.version)
        assertEquals(1800, found?.notes?.length)
    }

    @Test fun keepsMissingDigestNullToForbidInternalDownload() {
        val withoutDigest = release("v0.1.102", false)
            .replace("\"digest\":\"sha256:${"a".repeat(64)}\",", "")
        val json = "[$withoutDigest]"
        val release = GitHubUpdateCore.parseReleases(json, "owner/repo", "0.1.100")
        assertNotNull(release)
        assertNull(release?.sha256)
    }

    private fun release(tag: String, prerelease: Boolean, draft: Boolean = false): String {
        val version = tag.removePrefix("v")
        val name = "UaiCortes-v$version-universal.apk"
        return """{"tag_name":"$tag","prerelease":$prerelease,"draft":$draft,
            "body":"Nova versão", "assets":[{"name":"$name","state":"uploaded", "size":25000000,
            "digest":"sha256:${"a".repeat(64)}",
            "browser_download_url":"https://github.com/owner/repo/releases/download/$tag/$name"}]}"""
    }
}
