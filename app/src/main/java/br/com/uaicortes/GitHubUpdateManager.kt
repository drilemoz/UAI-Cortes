package br.com.uaicortes

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

internal sealed interface GitHubUpdateResult {
    data object Idle : GitHubUpdateResult
    data object Checking : GitHubUpdateResult
    data object NotConfigured : GitHubUpdateResult
    data object UpToDate : GitHubUpdateResult
    data class Available(val release: GitHubUpdateRelease) : GitHubUpdateResult
    data class Error(val message: String) : GitHubUpdateResult
}

/** One check on every cold app launch. Never downloads or installs without a user action. */
internal class GitHubUpdateManager(private val context: Context) {
    private var lastForegroundCheckMs: Long = Long.MIN_VALUE
    private var dismissedVersion: String? = null
    var result: GitHubUpdateResult by mutableStateOf(GitHubUpdateResult.Idle)
        private set
    var promptRelease: GitHubUpdateRelease? by mutableStateOf(null)
        private set
    var downloading by mutableStateOf(false)
        private set
    var downloadPercent by mutableStateOf(0)
        private set
    var downloadedBytes by mutableStateOf(0L)
        private set
    var downloadTotalBytes by mutableStateOf(0L)
        private set
    var downloadSpeedBytesPerSecond by mutableStateOf(0L)
        private set
    var readyFile: File? by mutableStateOf(null)
        private set
    var downloadError: String? by mutableStateOf(null)
        private set

    /** The published app owns its update source; stale user-entered paths are ignored. */
    fun repository(): String = GitHubUpdateCore.normalizedRepository(
        BuildConfig.GITHUB_RELEASE_REPOSITORY).orEmpty()

    /** Checks whenever the app returns to foreground, with a one-minute anti-flood window. */
    suspend fun checkOnForeground() {
        if (downloading || readyFile != null) return
        val now = SystemClock.elapsedRealtime()
        if (lastForegroundCheckMs != Long.MIN_VALUE && now - lastForegroundCheckMs < 60_000L) return
        lastForegroundCheckMs = now
        check(showPrompt = true)
    }

    suspend fun check(showPrompt: Boolean = false) {
        if (result is GitHubUpdateResult.Checking) return
        val repo = GitHubUpdateCore.normalizedRepository(repository())
        if (repo == null) {
            result = GitHubUpdateResult.NotConfigured
            return
        }
        result = GitHubUpdateResult.Checking
        val updated = withContext(Dispatchers.IO) {
            try {
                val url = "https://api.github.com/repos/$repo/releases?per_page=20"
                val connection = openConnection(url)
                try {
                    val status = connection.responseCode
                    if (status != 200) {
                        GitHubUpdateResult.Error(GitHubUpdateDiagnostics.http(
                            status, connection.getHeaderField("X-RateLimit-Remaining")
                        ))
                    } else {
                        val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readTextLimited(8_000_000) }
                        val release = GitHubUpdateCore.parseReleases(body, repo, BuildConfig.VERSION_NAME)
                        if (release == null) GitHubUpdateResult.UpToDate
                        else GitHubUpdateResult.Available(release)
                    }
                } finally { connection.disconnect() }
            } catch (error: Exception) {
                GitHubUpdateResult.Error(GitHubUpdateDiagnostics.exception(error))
            }
        }
        result = updated
        // Do not offer an APK from a previous version/repository after another check.
        readyFile = null
        if (showPrompt && updated is GitHubUpdateResult.Available &&
            updated.release.version != dismissedVersion) promptRelease = updated.release
    }

    fun dismissPrompt() {
        dismissedVersion = promptRelease?.version
        promptRelease = null
    }

    suspend fun download(release: GitHubUpdateRelease) {
        if (downloading) return
        if (release.sha256 == null) {
            downloadError = "Esta Release não informa SHA-256. Abra a página oficial do GitHub para baixar manualmente."
            return
        }
        downloadError = null
        readyFile = null
        downloading = true
        downloadPercent = 0
        downloadedBytes = 0L
        downloadTotalBytes = release.size
        downloadSpeedBytesPerSecond = 0L
        try {
            val downloaded = withContext(Dispatchers.IO) {
                val dir = File(context.cacheDir, "updates").apply { mkdirs() }
                val partial = File(dir, "download-pending.apk")
                val final = File(dir, "uai-cortes-update.apk")
                partial.delete()
                try {
                    val connection = openConnection(release.apkUrl, allowGithubRedirects = true)
                    try {
                        if (connection.responseCode != 200) error("Download indisponível: HTTP ${connection.responseCode}")
                        val length = connection.contentLengthLong
                        if (length > GitHubUpdateCore.MAX_APK_BYTES) error("APK maior que o limite permitido")
                        val md = MessageDigest.getInstance("SHA-256")
                        var count = 0L
                        var lastReportedBytes = 0L
                        var lastReportedAt = SystemClock.elapsedRealtime()
                        var smoothedRate = 0L
                        connection.inputStream.use { source ->
                            partial.outputStream().buffered().use { target ->
                                val buffer = ByteArray(32 * 1024)
                                while (true) {
                                    val n = source.read(buffer)
                                    if (n < 0) break
                                    count += n
                                    if (count > GitHubUpdateCore.MAX_APK_BYTES) error("APK maior que o limite permitido")
                                    md.update(buffer, 0, n)
                                    target.write(buffer, 0, n)
                                    val progress = ((count * 100L) / release.size).toInt().coerceIn(0, 100)
                                    val now = SystemClock.elapsedRealtime()
                                    val elapsed = now - lastReportedAt
                                    // Atualizar a cada ~500 ms, não a cada 32 KiB lidos.
                                    if (elapsed >= 500L || count == release.size) {
                                        val instant = GitHubDownloadProgress.rateBytesPerSecond(
                                            count - lastReportedBytes, elapsed)
                                        if (instant > 0L) {
                                            smoothedRate = if (smoothedRate <= 0L) instant
                                                else (smoothedRate * 2L + instant) / 3L
                                        }
                                        withContext(Dispatchers.Main) {
                                            downloadPercent = progress
                                            downloadedBytes = count
                                            downloadSpeedBytesPerSecond = smoothedRate
                                        }
                                        lastReportedBytes = count
                                        lastReportedAt = now
                                    }
                                }
                            }
                        }
                        if (count != release.size) error("Tamanho do APK diferente da Release")
                        val digest = md.digest().joinToString("") { "%02x".format(it) }
                        if (!digest.equals(release.sha256, ignoreCase = true)) error("SHA-256 diferente do GitHub")
                        val archive = context.packageManager.getPackageArchiveInfo(partial.absolutePath, 0)
                            ?: error("O APK recebido não é válido")
                        if (archive.packageName != context.packageName) error("APK pertence a outro aplicativo")
                        @Suppress("DEPRECATION")
                        val archiveCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) archive.longVersionCode
                            else archive.versionCode.toLong()
                        if (archiveCode <= BuildConfig.VERSION_CODE) error("APK não possui versionCode superior ao instalado")
                        if (archive.versionName != release.version) error("Versão do APK diferente da Release")
                        final.delete()
                        if (!partial.renameTo(final)) error("Não foi possível preparar o APK")
                        final
                    } finally { connection.disconnect() }
                } finally { partial.delete() }
            }
            readyFile = downloaded
            downloadedBytes = release.size
            downloadPercent = 100
        } catch (error: Exception) {
            downloadSpeedBytesPerSecond = 0L
            downloadError = error.message?.take(180) ?: "Falha ao baixar o APK"
        } finally { downloading = false }
    }

    /** Alternativa manual sem token e sem download automático quando a API não responde. */
    fun openRepositoryReleases(repositoryInput: String): String? {
        val repo = GitHubUpdateCore.normalizedRepository(repositoryInput)
            ?: return "Configure um repositório válido antes de abrir as Releases."
        return try {
            context.startActivity(Intent(Intent.ACTION_VIEW,
                Uri.parse("https://github.com/$repo/releases"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            null
        } catch (_: Exception) {
            "Não foi possível abrir o navegador para consultar as Releases."
        }
    }

    fun openRelease(release: GitHubUpdateRelease) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(release.releaseUrl))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun installDownloaded(): String? {
        val file = readyFile?.takeIf { it.isFile } ?: return "Nenhum APK verificado disponível."
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
            return try {
                context.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                "Autorize esta fonte no Android e volte para tocar em Instalar novamente."
            } catch (_: Exception) {
                "Habilite a instalação de APKs para este aplicativo nas configurações do Android."
            }
        }
        return try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.diagnostics", file)
            context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            null
        } catch (_: Exception) {
            "Não foi possível abrir o instalador do Android."
        }
    }

    private fun openConnection(url: String, allowGithubRedirects: Boolean = false): HttpURLConnection {
        var current = url
        repeat(if (allowGithubRedirects) 6 else 1) {
            val target = URL(current)
            require(target.protocol == "https") { "HTTPS obrigatório" }
            val host = target.host.lowercase()
            require(if (allowGithubRedirects) host == "github.com" ||
                host.endsWith(".githubusercontent.com") else host == "api.github.com") {
                "Host de download não permitido"
            }
            val connection = (target.openConnection() as HttpURLConnection).apply {
                connectTimeout = 12000
                readTimeout = 25000
                instanceFollowRedirects = false
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "UaiCortes-Android")
            }
            val code = connection.responseCode
            if (allowGithubRedirects && code in 300..399) {
                val next = connection.getHeaderField("Location")
                connection.disconnect()
                require(!next.isNullOrBlank()) { "Redirecionamento sem destino" }
                current = URL(target, next).toString()
            } else return connection
        }
        error("Excesso de redirecionamentos do GitHub")
    }
}

private fun java.io.BufferedReader.readTextLimited(maxCharacters: Int): String {
    val builder = StringBuilder()
    val chars = CharArray(8192)
    while (true) {
        val read = read(chars)
        if (read < 0) return builder.toString()
        if (builder.length + read > maxCharacters) throw GitHubResponseTooLargeException()
        builder.append(chars, 0, read)
    }
}
