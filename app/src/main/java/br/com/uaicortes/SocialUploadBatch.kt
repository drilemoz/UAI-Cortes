package br.com.uaicortes

import android.content.Context
import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

/** Um relatório local por lote, sem tokens, URLs da sessão OAuth ou dados de autorização. */
internal enum class UploadPhase {
    WAITING, AUTHORIZING, VERIFYING, UPLOADING, RECEIVED, REVIEW, FAILED_BEFORE_UPLOAD, MANUAL, REMOTE_ACCEPTED
}

internal data class UploadReceipt(
    val publicationId: String,
    val accountId: String,
    val accountName: String,
    val destination: SocialDestination,
    val phase: UploadPhase,
    val sent: Long = 0,
    val total: Long = 0,
    val speedBytesPerSecond: Double = 0.0,
    val remainingSeconds: Long? = null,
    val remoteVideoId: String = "",
    val details: String = "",
    val startedAt: Long = 0,
    val finishedAt: Long = 0
) {
    val percent: Int get() = if (total <= 0L) 0 else
        ((sent.coerceIn(0L, total).toDouble() / total.toDouble()) * 100.0).toInt().coerceIn(0, 100)
    fun toJson(): JSONObject = JSONObject().put("publicationId", publicationId)
        .put("accountId", accountId).put("accountName", accountName)
        .put("destination", destination.name).put("phase", phase.name)
        .put("sent", sent).put("total", total)
        .put("remoteVideoId", remoteVideoId).put("details", details.take(240))
        .put("startedAt", startedAt).put("finishedAt", finishedAt)
    companion object {
        fun fromJson(json: JSONObject): UploadReceipt {
            val phase = UploadPhase.entries.firstOrNull { it.name == json.optString("phase") }
                ?: UploadPhase.REVIEW
            // Após reiniciar o processo, nunca dizer que um envio continua ativo.
            val safe = when (phase) {
                UploadPhase.UPLOADING, UploadPhase.VERIFYING -> UploadPhase.REVIEW
                UploadPhase.AUTHORIZING, UploadPhase.WAITING -> UploadPhase.FAILED_BEFORE_UPLOAD
                else -> phase
            }
            return UploadReceipt(
                publicationId = json.getString("publicationId"),
                accountId = json.getString("accountId"),
                accountName = json.optString("accountName").take(70),
                destination = SocialDestination.parse(json.getString("destination"))
                    ?: SocialDestination.YOUTUBE,
                phase = safe,
                sent = json.optLong("sent").coerceAtLeast(0L),
                total = json.optLong("total").coerceAtLeast(0L),
                remoteVideoId = json.optString("remoteVideoId").take(30),
                details = if (phase != safe) "Operação interrompida; confira o YouTube Studio antes de repetir." else
                    json.optString("details").take(240),
                startedAt = json.optLong("startedAt").coerceAtLeast(0L),
                finishedAt = json.optLong("finishedAt").coerceAtLeast(0L)
            )
        }
    }
}

/** AtomicFile: relatório consultável após reiniciar, sem dados de OAuth. */
internal class SocialUploadReceiptStore(context: Context) {
    private val file = AtomicFile(File(context.applicationContext.filesDir, "social_upload_last.json"))

    @Synchronized fun save(rows: List<UploadReceipt>, savedAt: Long = System.currentTimeMillis()) {
        require(rows.size in 1..10)
        val json = JSONObject().put("schema", 1).put("savedAt", savedAt)
            .put("rows", JSONArray().also { a -> rows.forEach { a.put(it.toJson()) } })
        val bytes = json.toString().toByteArray(Charsets.UTF_8)
        require(bytes.size <= 48_000)
        var stream: FileOutputStream? = null
        try {
            stream = file.startWrite()
            stream.write(bytes)
            file.finishWrite(stream)
        } catch (error: Exception) {
            stream?.let(file::failWrite)
            throw error
        }
    }

    @Synchronized fun load(): List<UploadReceipt> {
        if (!file.baseFile.exists() && !File(file.baseFile.path + ".bak").exists()) return emptyList()
        val bytes = file.openRead().use { stream -> stream.readBytes() }
        require(bytes.size <= 48_000)
        val json = JSONObject(String(bytes, Charsets.UTF_8))
        require(json.optInt("schema") == 1)
        val rows = json.getJSONArray("rows")
        require(rows.length() in 1..10)
        return (0 until rows.length()).map { UploadReceipt.fromJson(rows.getJSONObject(it)) }
    }
}
