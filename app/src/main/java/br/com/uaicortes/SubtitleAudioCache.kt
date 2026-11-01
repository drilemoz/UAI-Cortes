package br.com.uaicortes

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

/** Fingerprint sem gravar URI de vídeos em nome de arquivo ou log. */
internal object SubtitleAudioFingerprint {
    fun key(parts: List<String>): String {
        val digest = MessageDigest.getInstance("SHA-256")
        for (part in parts) {
            val bytes=part.toByteArray(Charsets.UTF_8)
            digest.update(byteArrayOf((bytes.size ushr 24).toByte(),(bytes.size ushr 16).toByte(),
                (bytes.size ushr 8).toByte(),bytes.size.toByte()))
            digest.update(bytes)
        }
        return digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xFF) }
    }

    fun validWavLength(actual: Long, clipDurationsMs: List<Long>): Boolean {
        if (clipDurationsMs.isEmpty() || clipDurationsMs.any { it <= 0 }) return false
        val samples=clipDurationsMs.sumOf { it * 16000L / 1000L * 2L }
        return samples in 1..22_000_000 && actual == samples + 44L
    }
}

/** WAV extraído para transcrição Gemini, reaproveitado somente para o MESMO
 * conjunto ordenado de clipes e cortes. Áudio é salvo apenas no cache privado
 * do aplicativo, sem cópia no projeto, sem guardar tokens/URIs em metadados.
 * Arquivos vencem após 3 horas; no máximo dois WAVs são mantidos.
 */
internal object SubtitleAudioCache {
    private const val TTL_MS = 3L * 60L * 60L * 1000L
    private val mutex=Mutex()

    private fun sourceRevision(context: Context, value: String): String {
        val uri=Uri.parse(value)
        if (uri.scheme == "file") {
            val f=File(uri.path ?: return "inacessível")
            return "file:${f.length()}:${f.lastModified()}"
        }
        if (uri.scheme != "content") return "fonte_sem_revisao"
        return runCatching {
            context.contentResolver.query(uri,null,null,null,null)?.use { c ->
                if(!c.moveToFirst()) return@use "fonte_sem_revisao"
                val sizeColumn=c.getColumnIndex(OpenableColumns.SIZE)
                val size=if(sizeColumn>=0 && !c.isNull(sizeColumn)) c.getLong(sizeColumn) else -1L
                val modifiedColumn= listOf("last_modified",MediaStore.MediaColumns.DATE_MODIFIED)
                    .firstOrNull { c.getColumnIndex(it) >= 0 }
                // O provedor pode omitir ou retornar nula a coluna de data.
                // getColumnIndex() devolve -1 nesses casos: nunca passar -1 a getLong().
                val stamp = modifiedColumn?.let { name ->
                    val index = c.getColumnIndex(name)
                    if (index >= 0 && !c.isNull(index)) c.getLong(index) else -1L
                } ?: -1L
                "$size:$stamp"
            } ?: "fonte_sem_revisao"
        }.getOrDefault("fonte_sem_revisao")
    }

    suspend fun getOrPrepare(context: Context, project: VideoProject,
        onProgress:suspend (Int?,String)->Unit): File = mutex.withLock {
        withContext(Dispatchers.IO) {
            require(project.clips.isNotEmpty()) {"Nenhum vídeo no projeto"}
            val sourceParts = project.clips.flatMap { clip -> listOf(
                clip.id,clip.uri,clip.startMs.toString(),clip.endMs.toString(),
                clip.sourceDurationMs.toString(),sourceRevision(context,clip.uri))
            }
            val fingerprint=SubtitleAudioFingerprint.key(listOf("wav16k-mono-v1",project.id) + sourceParts)
            val dir=File(context.cacheDir,"subtitle-audio")
            if(!dir.isDirectory && !dir.mkdirs()) error("Não foi possível criar cache de áudio")
            val file=File(dir,"asr-$fingerprint.wav")
            val now=System.currentTimeMillis()
            val valid=file.exists() && now-file.lastModified() in 0..TTL_MS &&
                SubtitleAudioFingerprint.validWavLength(file.length(),
                    project.clips.map{it.durationMs})
            if(valid) {
                file.setLastModified(now)
                onProgress(50,"Reaproveitando áudio já extraído deste vídeo…")
                return@withContext file
            }
            // Nunca usar um arquivo parcial, velho ou de outros cortes.
            file.delete()
            val fresh=AutoSubtitleEngine.prepareWav(context,project,onProgress)
            try {
                if(!SubtitleAudioFingerprint.validWavLength(fresh.length(),
                        project.clips.map{it.durationMs}))
                    error("Arquivo de áudio extraído está incompleto")
                if(!fresh.renameTo(file)) {
                    val staged=File(dir,"asr-$fingerprint.tmp")
                    try {
                        fresh.copyTo(staged,overwrite=true)
                        if(!staged.renameTo(file)) error("Falha ao guardar áudio extraído")
                    } finally { staged.delete() }
                }
                dir.listFiles()?.filter { it.isFile && it.name.startsWith("asr-") &&
                    it.extension == "wav" && it != file }
                    ?.sortedByDescending { it.lastModified() }
                    ?.forEachIndexed { index,other ->
                        if(index>=1 || now-other.lastModified()>TTL_MS) other.delete()
                    }
                onProgress(50,"Áudio extraído e disponível para reutilização")
                file
            } finally { fresh.delete() }
        }
    }
}
