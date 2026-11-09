package br.com.uaicortes

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

/**
 * A mídia importada recebe uma cópia independente da URI escolhida pelo usuário.
 * Todas as versões Android suportadas: cópia PRIVADA em filesDir/project_media_v1
 * através de FileProvider. Nunca registrar backups na galeria pública (MediaStore).
 * URIs legadas MediaStore continuam reconhecidas e acessíveis: não há migração
 * destrutiva nem exclusão automática dos projetos/cópias antigos.
 *
 * A URI retornada somente é entregue depois da conclusão integral da cópia.
 * Nunca exclui ou altera a origem; um erro elimina somente a cópia parcial criada.
 * Cópias já confirmadas nunca são apagadas automaticamente ao remover um projeto.
 */
internal object ProjectMediaCopies {
    internal enum class Kind(val prefix: String, val fallbackMime: String, val directory: String) {
        VIDEO("video/", "video/mp4", Environment.DIRECTORY_MOVIES),
        AUDIO("audio/", "audio/mp4", Environment.DIRECTORY_MUSIC),
        IMAGE("image/", "image/jpeg", Environment.DIRECTORY_PICTURES)
    }

    internal fun folder(projectId: String, kind: Kind): String {
        require(projectId.matches(Regex("[A-Za-z0-9_-]{1,64}"))) { "Identificador de projeto inválido." }
        return "${kind.directory}/UaiCortes/Projetos/$projectId"
    }

    private fun digest(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

    private fun displayName(context: Context, source: Uri, kind: Kind, key: String): String {
        val raw = runCatching { mediaName(context, source) }.getOrDefault("midia")
        val cleaned = raw.replace(Regex("[^\\p{L}\\p{N}_. -]"), "_").trim().trim('.')
            .take(70).ifBlank { "midia" }
        val extension = when (kind) {
            Kind.VIDEO -> "mp4"
            Kind.AUDIO -> "m4a"
            Kind.IMAGE -> "jpg"
        }
        val final = if ('.' in cleaned) cleaned else "$cleaned.$extension"
        return "${key.take(12)}_$final"
    }

    private fun alreadyProtected(context: Context, uri: Uri, projectId: String, kind: Kind): Boolean {
        if (uri.scheme == "content" && uri.authority == "${context.packageName}.diagnostics") {
            val parts = uri.pathSegments
            if (parts.firstOrNull() == "project_images" && kind == Kind.IMAGE) return true
            if (parts.size >= 4 && parts[0] == "project_media" &&
                parts[1] == projectId && parts[2] == kind.name.lowercase()) return true
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || uri.scheme != "content") return false
        return runCatching {
            context.contentResolver.query(uri, arrayOf(MediaStore.MediaColumns.RELATIVE_PATH),
                null, null, null)?.use {
                it.moveToFirst() && it.getString(0).orEmpty().trimEnd('/') == folder(projectId, kind)
            } == true
        }.getOrDefault(false)
    }

    /** Projetos anteriores também passam a usar cópias próprias quando abertos. */
    suspend fun protectProject(context: Context, project: VideoProject): VideoProject {
        suspend fun safe(raw: String, kind: Kind): String {
            val uri = Uri.parse(raw)
            if (alreadyProtected(context, uri, project.id, kind)) return raw
            // Mídia já desaparecida permanece como está, para a ferramenta Reparar mídia.
            if (!ProjectMediaAvailability.canOpen(context, raw)) return raw
            return copy(context, uri, project.id, kind).toString()
        }
        val clips = project.clips.map { it.copy(uri = safe(it.uri,
            if (it.isImage) Kind.IMAGE else Kind.VIDEO)) }
        val overlays = project.overlays.map { it.copy(uri = safe(it.uri,
            if (it.isImage) Kind.IMAGE else Kind.VIDEO)) }
        val music = project.music?.let { it.copy(uri = safe(it.uri, Kind.AUDIO)) }
        val tracks = project.audioTracks.map { it.copy(uri = safe(it.uri, Kind.AUDIO)) }
        return project.copy(clips = clips, overlays = overlays, music = music, audioTracks = tracks)
    }

    suspend fun copy(context: Context, source: Uri, projectId: String, kind: Kind): Uri =
        withContext(Dispatchers.IO) {
            require(source.scheme == "content" || source.scheme == "file") { "Selecione um arquivo local do Android." }
            folder(projectId, kind) // validate before any write
            if (alreadyProtected(context, source, projectId, kind)) return@withContext source
            val resolver = context.contentResolver
            val actualMime = resolver.getType(source)?.lowercase().orEmpty()
            require(actualMime.isBlank() || actualMime.startsWith(kind.prefix)) {
                "O arquivo selecionado não corresponde ao tipo de mídia esperado."
            }
            val key = digest("$projectId|${kind.name}|$source")
            val prefs = context.getSharedPreferences("project_media_copies_v1", Context.MODE_PRIVATE)
            val saved = prefs.getString(key, null)?.let(Uri::parse)
            if (saved != null && ProjectMediaAvailability.canOpen(context, saved.toString()))
                return@withContext saved

            val expectedSize = runCatching {
                resolver.query(source, arrayOf(OpenableColumns.SIZE), null, null, null)?.use {
                    if (it.moveToFirst() && !it.isNull(0)) it.getLong(0) else -1L
                } ?: -1L
            }.getOrDefault(-1L)
            require(expectedSize != 0L) { "O arquivo original está vazio." }
            val name = displayName(context, source, kind, key)
            // Cópia privada em TODAS as versões Android; não indexa mídia na Galeria.
            // A pasta é específica do projeto para evitar conflitos entre projetos.
            val dir = File(context.filesDir, "project_media_v1/$projectId/${kind.name.lowercase()}")
            require(dir.isDirectory || dir.mkdirs()) { "Não foi possível criar a pasta privada de mídias." }
            var privateFile = File(dir, name)
            if (privateFile.exists()) {
                // Nunca sobrescrever uma cópia anterior mesmo se o mapeamento local sumiu.
                privateFile = File(dir, "${java.util.UUID.randomUUID()}_$name")
            }
            val partial = File(dir, "${privateFile.name}.partial")
            try {
                var total = 0L
                partial.outputStream().use { output ->
                    (resolver.openInputStream(source)
                        ?: error("Não foi possível abrir a mídia original.")).use { input ->
                        val buffer = ByteArray(256 * 1024)
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val read = input.read(buffer)
                            if (read < 0) break
                            output.write(buffer, 0, read)
                            total += read
                        }
                        output.flush()
                        output.fd.sync()
                    }
                }
                check(total > 0L && (expectedSize < 0L || total == expectedSize)) {
                    "A cópia ficou incompleta; confira o espaço disponível."
                }
                check(!privateFile.exists() && partial.renameTo(privateFile)) {
                    "Não foi possível finalizar a cópia privada."
                }
                val destination = FileProvider.getUriForFile(context,
                    "${context.packageName}.diagnostics", privateFile)
                // Persistir mapeamento evita duplicar cópias do mesmo arquivo no mesmo projeto.
                prefs.edit().putString(key, destination.toString()).apply()
                destination
            } catch (failure: Throwable) {
                partial.delete()
                privateFile.delete()
                throw failure
            }
        }
}
