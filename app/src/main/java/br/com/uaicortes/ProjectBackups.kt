package br.com.uaicortes

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.AtomicFile
import androidx.documentfile.provider.DocumentFile
import org.json.JSONObject
import java.io.File
import java.io.ByteArrayOutputStream

/** A cópia histórica contém apenas o projeto (JSON schemaVersion 4), não os arquivos de mídia. */
internal data class ProjectBackupEntry(val file: File, val project: VideoProject, val timestamp: Long)

internal data class BackupStorageUsage(
    val snapshotCount: Int, val bytes: Long,
    val orphanDirectories: Int, val orphanBytes: Long,
    val privateFolder: String
)

/** Checagem de integridade antes de restaurar ou importar; arquivos truncados são rejeitados. */
internal object ProjectBackupCodec {
    const val MAX_BYTES = 2 * 1024 * 1024
    fun encode(project: VideoProject, timestamp: Long): String {
        val data = project.json().toString()
        return JSONObject().put("format", "uai-cortes-project-backup-v1")
            .put("savedAt", timestamp).put("payload", data)
            .put("sha256", ProjectBackupPolicy.hash(data)).toString()
    }
    fun decode(raw: String): Pair<VideoProject, Long> {
        require(raw.toByteArray(Charsets.UTF_8).size in 1..MAX_BYTES) { "Backup muito grande ou vazio" }
        val j = JSONObject(raw)
        require(j.optString("format") == "uai-cortes-project-backup-v1") { "Formato de backup desconhecido" }
        val data = j.getString("payload")
        require(ProjectBackupPolicy.hash(data) == j.getString("sha256")) { "Verificação de integridade falhou" }
        val project = VideoProject.read(JSONObject(data))
        require(project.id.isNotBlank()) { "Projeto sem identificação" }
        return project to j.getLong("savedAt").coerceAtLeast(0L)
    }
}

/** Configuração local: sem work em segundo plano, sem pedir acesso amplo ao armazenamento. */
internal class ProjectBackupPreferences(context: Context) {
    private val p = context.applicationContext.getSharedPreferences("project_backup_settings", Context.MODE_PRIVATE)
    var enabled: Boolean
        get() = p.getBoolean("enabled", true)
        set(v) { check(p.edit().putBoolean("enabled", v).commit()) }
    var intervalSeconds: Int
        get() = ProjectBackupPolicy.interval(p.getInt("interval_seconds", 30))
        set(v) { check(p.edit().putInt("interval_seconds", ProjectBackupPolicy.interval(v)).commit()) }
    var retention: Int
        get() = ProjectBackupPolicy.count(p.getInt("retention", 5))
        set(v) { check(p.edit().putInt("retention", ProjectBackupPolicy.count(v)).commit()) }
    var externalEnabled: Boolean
        get() = p.getBoolean("external_enabled", false)
        set(v) { check(p.edit().putBoolean("external_enabled", v).commit()) }
    var externalFolder: String?
        get() = p.getString("external_folder", null)
        set(v) { check(p.edit().putString("external_folder", v).commit()) }
    var lastExternalError: String?
        get() = p.getString("last_external_error", null)
        set(v) { p.edit().putString("last_external_error", v).apply() }
}

internal class ProjectBackupStore(private val context: Context) {
    val preferences = ProjectBackupPreferences(context)
    private val root = File(context.filesDir, "project_backups")
    private val guard = Any()
    private fun projectKey(id: String) = ProjectBackupPolicy.hash(id).take(24)
    private fun comparable(project: VideoProject): String = project.json().apply { remove("updated") }.toString()
    private fun folder(id: String) = File(root, projectKey(id))

    /** Contagem limitada a arquivos sob project_backups; não toca no armazenamento do usuário. */
    fun storageUsage(savedIds: Set<String>): BackupStorageUsage = synchronized(guard) {
        val allowed = savedIds.map(::projectKey).toSet()
        var snapshots = 0
        var bytes = 0L
        var orphanFolders = 0
        var orphanBytes = 0L
        root.listFiles()?.filter { it.isDirectory && Regex("[0-9a-f]{24}").matches(it.name) }
            ?.forEach { dir ->
                val files = dir.listFiles()?.filter { it.isFile && it.name.endsWith(".json") }.orEmpty()
                val size = files.sumOf { it.length().coerceAtLeast(0L) }
                snapshots += files.size
                bytes += size
                if (dir.name !in allowed) {
                    orphanFolders++
                    orphanBytes += size
                }
            }
        BackupStorageUsage(snapshots, bytes, orphanFolders, orphanBytes, root.absolutePath)
    }

    /** Exclusão deliberada apenas de diretórios órfãos criados pelo aplicativo. */
    fun deleteOrphanedSnapshots(savedIds: Set<String>): Int = synchronized(guard) {
        val allowed = savedIds.map(::projectKey).toSet()
        var removed = 0
        root.listFiles()?.filter { it.isDirectory && Regex("[0-9a-f]{24}").matches(it.name) &&
            it.name !in allowed }?.forEach { dir ->
            val children = dir.listFiles().orEmpty()
            // Evitar apagar arquivos inesperados mesmo dentro da pasta privada.
            if (children.all { it.isFile && (it.name.endsWith(".json") || it.name.endsWith(".json.bak") || it.name.endsWith(".json.new")) } &&
                children.all { it.delete() } && dir.delete()) removed++
        }
        removed
    }

    /** Snapshot diferente do último; só executado na thread IO. Nunca substitui o ProjectStore. */
    fun snapshot(projectId: String, force: Boolean = false): Boolean = synchronized(guard) {
        if ((!force && !preferences.enabled) || projectId.isBlank()) return@synchronized false
        val project = ProjectStore(context).load().firstOrNull { it.id == projectId }
            ?: return@synchronized false
        val newest = list(projectId).firstOrNull()
        if (newest != null && comparable(newest.project) == comparable(project)) return@synchronized false
        val dir = folder(projectId)
        check(dir.isDirectory || dir.mkdirs()) { "Não foi possível criar a pasta de backups" }
        val stamp = System.currentTimeMillis()
        val target = File(dir, "$stamp-${System.nanoTime()}.json")
        val payload = ProjectBackupCodec.encode(project, stamp)
        check(payload.toByteArray(Charsets.UTF_8).size <= ProjectBackupCodec.MAX_BYTES) {
            "Projeto excede o limite de cópia de segurança"
        }
        val atomic = AtomicFile(target)
        val stream = atomic.startWrite()
        try {
            stream.write(payload.toByteArray(Charsets.UTF_8))
            atomic.finishWrite(stream)
        } catch (e: Exception) {
            atomic.failWrite(stream)
            throw e
        }
        // A falha da cópia SAF não apaga o snapshot local válido.
        if (preferences.externalEnabled) {
            runCatching { writeExternal(projectId, target.name, payload) }
                .onSuccess { preferences.lastExternalError = null }
                .onFailure { preferences.lastExternalError = "Backup externo indisponível. Verifique a pasta e a permissão." }
        }
        list(projectId).drop(preferences.retention).forEach { it.file.delete() }
        true
    }

    fun list(projectId: String): List<ProjectBackupEntry> {
        if (projectId.isBlank()) return emptyList()
        return folder(projectId).listFiles()?.asSequence()?.filter { it.isFile && it.name.endsWith(".json") }
            ?.sortedByDescending { it.name }?.take(40)?.mapNotNull { file ->
                runCatching {
                    if (file.length() !in 1..ProjectBackupCodec.MAX_BYTES.toLong()) return@runCatching null
                    val (project, stamp) = ProjectBackupCodec.decode(file.readText(Charsets.UTF_8))
                    if (project.id != projectId) null else ProjectBackupEntry(file, project, stamp)
                }.getOrNull()
            }?.filterNotNull()?.toList() ?: emptyList()
    }

    fun listAll(): List<ProjectBackupEntry> = root.listFiles()?.asSequence()?.filter { it.isDirectory }
        ?.flatMap { dir -> dir.listFiles()?.asSequence()?.filter { it.isFile && it.name.endsWith(".json") }
            ?.take(30)?.mapNotNull { file -> runCatching {
                if (file.length() !in 1..ProjectBackupCodec.MAX_BYTES.toLong()) null else {
                    val (project, stamp) = ProjectBackupCodec.decode(file.readText(Charsets.UTF_8))
                    if (dir.name != projectKey(project.id)) null else ProjectBackupEntry(file, project, stamp)
                }
            }.getOrNull() } ?: emptySequence() }
        ?.sortedByDescending { it.timestamp }?.take(100)?.toList() ?: emptyList()

    fun importFrom(uri: Uri): VideoProject {
        val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                check(output.size() + n <= ProjectBackupCodec.MAX_BYTES) { "Backup excede o limite permitido" }
                output.write(buffer, 0, n)
            }
            output.toByteArray()
        } ?: error("Não foi possível ler o arquivo selecionado")
        return ProjectBackupCodec.decode(bytes.toString(Charsets.UTF_8)).first
    }

    fun exportTo(uri: Uri, project: VideoProject) {
        val bytes = ProjectBackupCodec.encode(project, System.currentTimeMillis()).toByteArray(Charsets.UTF_8)
        check(bytes.size <= ProjectBackupCodec.MAX_BYTES) { "Projeto excede o limite de backup" }
        context.contentResolver.openOutputStream(uri, "wt")?.use { output -> output.write(bytes) }
            ?: error("Não foi possível escrever o backup")
    }

    fun chooseFolder(uri: Uri) {
        context.contentResolver.takePersistableUriPermission(uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        preferences.externalFolder = uri.toString()
        preferences.lastExternalError = null
    }

    private fun writeExternal(projectId: String, fileName: String, payload: String) {
        val uri = preferences.externalFolder?.let(Uri::parse) ?: error("Pasta não configurada")
        val tree = DocumentFile.fromTreeUri(context, uri) ?: error("Pasta indisponível")
        check(tree.canWrite()) { "Sem permissão para escrita" }
        val prefix = "UaiCortes-${projectKey(projectId)}-"
        val name = prefix + fileName
        val file = tree.createFile("application/json", name) ?: error("Não foi possível criar backup externo")
        try {
            context.contentResolver.openOutputStream(file.uri, "w")?.use {
                it.write(payload.toByteArray(Charsets.UTF_8))
            } ?: error("Falha de gravação externa")
        } catch (e: Exception) {
            file.delete()
            throw e
        }
        // Nunca remove arquivos que não correspondem ao padrão exclusivo criado por este recurso.
        tree.listFiles().filter { it.isFile && it.name?.startsWith(prefix) == true &&
            it.name?.endsWith(".json") == true }.sortedByDescending { it.name }
            .drop(preferences.retention).forEach { it.delete() }
    }
}
