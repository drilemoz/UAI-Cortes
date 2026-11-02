package br.com.uaicortes

import org.json.JSONArray

/** Pure release policy: GitHub Releases, including prereleases created by this project's CI. */
internal object GitHubUpdateCore {
    private val repositoryPattern = Regex("^[A-Za-z0-9_.-]{1,100}/[A-Za-z0-9_.-]{1,100}$")
    private val versionPattern = Regex("^v?(\\d+)\\.(\\d+)\\.(\\d+)$")
    private val shaPattern = Regex("^sha256:([a-fA-F0-9]{64})$")
    const val MAX_APK_BYTES = 200L * 1024L * 1024L

    fun normalizedRepository(value: String): String? = value.trim().removePrefix("https://github.com/")
        .removeSuffix("/").takeIf { repositoryPattern.matches(it) &&
            !it.split('/').any { part -> part == "." || part == ".." } }

    fun versionParts(tag: String): List<Int>? {
        val match = versionPattern.matchEntire(tag.trim()) ?: return null
        return (1..3).map { match.groupValues[it].toIntOrNull() ?: return null }
    }

    fun newer(candidate: String, installed: String): Boolean {
        val a = versionParts(candidate) ?: return false
        val b = versionParts(installed) ?: return false
        for (i in 0..2) {
            if (a[i] != b[i]) return a[i] > b[i]
        }
        return false
    }

    fun parseReleases(payload: String, repository: String, installed: String): GitHubUpdateRelease? {
        val repo = normalizedRepository(repository) ?: return null
        val entries = JSONArray(payload)
        var chosen: GitHubUpdateRelease? = null
        for (i in 0 until entries.length().coerceAtMost(100)) {
            val entry = entries.optJSONObject(i) ?: continue
            if (entry.optBoolean("draft", false)) continue
            val tag = entry.optString("tag_name")
            if (!newer(tag, installed)) continue
            val version = tag.removePrefix("v")
            val name = "UaiCortes-v$version-universal.apk"
            val expectedUrl = "https://github.com/$repo/releases/download/$tag/$name"
            val assets = entry.optJSONArray("assets") ?: continue
            for (j in 0 until assets.length()) {
                val asset = assets.optJSONObject(j) ?: continue
                val url = asset.optString("browser_download_url")
                if (asset.optString("name") != name || !url.equals(expectedUrl, ignoreCase = true)) continue
                val size = asset.optLong("size", -1L)
                if (size <= 0L || size > MAX_APK_BYTES || asset.optString("state") != "uploaded") continue
                val hash = shaPattern.matchEntire(asset.optString("digest"))?.groupValues?.get(1)?.lowercase()
                val releaseUrl = "https://github.com/$repo/releases/tag/$tag"
                val release = GitHubUpdateRelease(version, name, url, releaseUrl,
                    entry.optString("body").take(1800), size, hash)
                if (chosen == null || newer(release.version, chosen.version)) chosen = release
            }
        }
        return chosen
    }
}

internal data class GitHubUpdateRelease(
    val version: String,
    val apkName: String,
    val apkUrl: String,
    val releaseUrl: String,
    val notes: String,
    val size: Long,
    val sha256: String?
)
