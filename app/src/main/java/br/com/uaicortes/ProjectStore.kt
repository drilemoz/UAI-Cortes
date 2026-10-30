package br.com.uaicortes

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class Clip(
    val id: String = UUID.randomUUID().toString(),
    val uri: String, val title: String,
    val sourceDurationMs: Long,
    val startMs: Long = 0,
    val endMs: Long = sourceDurationMs,
    val volume: Float = 1f
) {
    val durationMs: Long get() = (endMs - startMs).coerceAtLeast(1L)
    fun json() = JSONObject().put("id", id).put("uri", uri).put("title", title)
        .put("sourceDurationMs", sourceDurationMs).put("startMs", startMs)
        .put("endMs", endMs).put("volume", volume.toDouble())
    companion object {
        fun read(j: JSONObject) = Clip(j.optString("id", UUID.randomUUID().toString()),
            j.getString("uri"), j.optString("title", "Vídeo"), j.optLong("sourceDurationMs", 1000L),
            j.optLong("startMs"), j.optLong("endMs", 1000L), j.optDouble("volume", 1.0).toFloat())
    }
}

data class Overlay(
    val uri: String, val isImage: Boolean, val durationMs: Long,
    val startAtMs: Long = 0, val x: Float = 0.5f, val y: Float = 0.5f,
    val scale: Float = 0.36f, val volume: Float = 0.5f,
    val muted: Boolean = false,
    val overlapWith: List<String> = emptyList(),
    val id: String = UUID.randomUUID().toString()
) {
    fun json() = JSONObject().put("uri", uri).put("isImage", isImage)
        .put("durationMs", durationMs).put("startAtMs", startAtMs)
        .put("x", x.toDouble()).put("y", y.toDouble()).put("scale", scale.toDouble())
        .put("volume", volume.toDouble()).put("muted", muted)
        .put("overlapWith", JSONArray().apply { overlapWith.forEach { put(it) } }).put("id", id)
    companion object {
        fun read(j: JSONObject) = Overlay(j.getString("uri"), j.optBoolean("isImage"),
            j.optLong("durationMs", 5000), j.optLong("startAtMs"),
            j.optDouble("x", 0.5).toFloat(), j.optDouble("y", 0.5).toFloat(),
            j.optDouble("scale", 0.36).toFloat(), j.optDouble("volume", 0.5).toFloat(),
            j.optBoolean("muted", false),
            j.optJSONArray("overlapWith")?.let { array ->
                (0 until array.length()).mapNotNull { index ->
                    array.optString(index).takeIf { it.isNotBlank() }
                }.distinct()
            } ?: emptyList(),
            j.optString("id").ifBlank { UUID.randomUUID().toString() })
    }
}

data class Music(val uri: String, val title: String, val volume: Float = 0.35f) {
    fun json() = JSONObject().put("uri", uri).put("title", title).put("volume", volume.toDouble())
    companion object { fun read(j: JSONObject) = Music(j.getString("uri"), j.optString("title"), j.optDouble("volume", 0.35).toFloat()) }
}

/** Texto com tempo absoluto na linha do tempo (sem reconhecimento de voz ou serviços externos). */
data class SubtitleCue(
    val id: String = UUID.randomUUID().toString(),
    val text: String, val startMs: Long, val endMs: Long,
    val color: Int = -1, val sizeSp: Int = 12, val y: Float = .85f, val x: Float = .5f,
    val backgroundColor: Int = SubtitleStyles.DEFAULT_BACKGROUND, val bold: Boolean = true,
    val fontFamily: String = SubtitleStyles.FONT_SANS,
    val outlineWidth: Int = 0, val outlineColor: Int = 0xFF000000.toInt(),
    val shadowOffsetDp: Int = 0, val shadowColor: Int = 0xFF000000.toInt(),
    val lineSpacingPercent: Int = 100, val textAlignment: String = SubtitleStyles.ALIGN_CENTER,
    val animation: String = SubtitleAnimations.NONE, val animationSpeed: Float = 1f
) {
    fun json() = JSONObject().put("id", id).put("text", text)
        .put("startMs", startMs).put("endMs", endMs).put("color", color)
        .put("sizeSp", sizeSp).put("y", y.toDouble()).put("x", x.toDouble())
        .put("backgroundColor", backgroundColor).put("bold", bold)
        .put("fontFamily", fontFamily).put("outlineWidth", outlineWidth)
        .put("outlineColor", outlineColor)
        .put("shadowOffsetDp", shadowOffsetDp).put("shadowColor", shadowColor)
        .put("lineSpacingPercent", lineSpacingPercent).put("textAlignment", textAlignment)
        .put("animation", animation).put("animationSpeed", animationSpeed.toDouble())
    companion object {
        fun read(j: JSONObject) = SubtitleCue(j.optString("id").ifBlank { UUID.randomUUID().toString() },
            j.optString("text"), j.optLong("startMs"), j.optLong("endMs"),
            j.optInt("color", -1), j.optInt("sizeSp", 22).coerceIn(12, 48),
            j.optDouble("y", .85).toFloat().coerceIn(.1f, .95f),
            j.optDouble("x", .5).toFloat().coerceIn(.1f, .9f),
            j.optInt("backgroundColor", SubtitleStyles.DEFAULT_BACKGROUND),
            j.optBoolean("bold", true),
            SubtitleStyles.normalizeFont(j.optString("fontFamily", SubtitleStyles.FONT_SANS)),
            j.optInt("outlineWidth", 0).coerceIn(0, 4),
            j.optInt("outlineColor", 0xFF000000.toInt()),
            j.optInt("shadowOffsetDp", 0).coerceIn(0, 6),
            SubtitleStyles.normalizeShadowColor(j.optInt("shadowColor", 0xFF000000.toInt())),
            SubtitleStyles.normalizeLineSpacing(j.optInt("lineSpacingPercent", 100)),
            SubtitleStyles.normalizeAlignment(j.optString("textAlignment", SubtitleStyles.ALIGN_CENTER)),
            SubtitleAnimations.normalize(j.optString("animation", SubtitleAnimations.NONE)),
            SubtitleAnimations.speed(j.optDouble("animationSpeed", 1.0).toFloat()))
    }
}

/** Identifica a fronteira entre dois clipes por IDs (não por índices mutáveis). */
data class ClipTransition(
    val fromId: String, val toId: String,
    val kind: String = "Corte direto", val durationMs: Long = 500L
) {
    fun json() = JSONObject().put("fromId", fromId).put("toId", toId)
        .put("kind", kind).put("durationMs", durationMs)
    companion object { fun read(j: JSONObject) = ClipTransition(
        j.optString("fromId"), j.optString("toId"), j.optString("kind", "Corte direto"),
        j.optLong("durationMs", 500L).coerceIn(100L, 1500L)) }
}

/** Faixa de áudio: startMs é posição no projeto; sourceStartMs é a entrada no arquivo de origem.
 * durationMs determina quantos milissegundos da fonte entram na composição.
 * Projetos anteriores não tinham sourceStartMs e continuam iniciando no zero da fonte. */
data class AudioTrack(
    val uri: String, val title: String, val startMs: Long = 0L,
    val durationMs: Long = 0L, val volume: Float = .35f,
    val id: String = UUID.randomUUID().toString(),
    val sourceStartMs: Long = 0L
) {
    fun json() = JSONObject().put("uri", uri).put("title", title)
        .put("startMs", startMs).put("durationMs", durationMs)
        .put("volume", volume.toDouble()).put("id", id)
        .put("sourceStartMs", sourceStartMs)
    companion object { fun read(j: JSONObject) = AudioTrack(j.getString("uri"),
        j.optString("title"), j.optLong("startMs"), j.optLong("durationMs"),
        j.optDouble("volume", .35).toFloat().coerceIn(0f,1f),
        j.optString("id").ifBlank { UUID.randomUUID().toString() },
        j.optLong("sourceStartMs", 0L).coerceAtLeast(0L)) }
}

data class VideoProject(
    val id: String = UUID.randomUUID().toString(), val title: String = "Meu vídeo",
    val updated: Long = System.currentTimeMillis(),
    val clips: List<Clip> = emptyList(), val overlays: List<Overlay> = emptyList(),
    val music: Music? = null, val transition: String = "Corte direto",
    val subtitles: List<SubtitleCue> = emptyList(),
    val transitions: List<ClipTransition> = emptyList(),
    val audioTracks: List<AudioTrack> = emptyList(),
    val lastTrimCutClipId: String? = null, val lastTrimCutSourceMs: Long? = null,
    val textWatermark: TextWatermark? = null
) {
    val durationMs get() = clips.sumOf { it.durationMs }
    val pip: Overlay? get() = overlays.firstOrNull() // acesso legado somente leitura
    fun json(): JSONObject = JSONObject().put("id", id).put("title", title).put("updated", updated)
        .put("clips", JSONArray().also { a -> clips.forEach { a.put(it.json()) } })
        .put("schemaVersion", 4)
        .put("overlays", JSONArray().also { a -> overlays.take(4).forEach { a.put(it.json()) } })
        .put("music", music?.json()).put("transition", transition)
        .put("subtitles", JSONArray().also { a -> subtitles.forEach { a.put(it.json()) } })
        .put("transitions", JSONArray().also { a -> transitions.forEach { a.put(it.json()) } })
        .put("audioTracks", JSONArray().also { a -> audioTracks.forEach { a.put(it.json()) } })
        .put("lastTrimCutClipId", lastTrimCutClipId).put("lastTrimCutSourceMs", lastTrimCutSourceMs)
        .put("textWatermark", textWatermark?.json())
    companion object {
        fun read(j: JSONObject): VideoProject {
            val a = j.optJSONArray("clips") ?: JSONArray()
            return VideoProject(j.getString("id"), j.optString("title", "Meu vídeo"), j.optLong("updated"),
                (0 until a.length()).map { Clip.read(a.getJSONObject(it)) },
                (j.optJSONArray("overlays")?.let { overlays ->
                    (0 until overlays.length()).mapNotNull { index ->
                        runCatching { Overlay.read(overlays.getJSONObject(index)) }.getOrNull()
                    }.take(4)
                } ?: listOfNotNull(j.optJSONObject("pip")?.let(Overlay::read))),
                j.optJSONObject("music")?.let(Music::read),
                j.optString("transition", "Corte direto"),
                j.optJSONArray("subtitles")?.let { arr ->
                    (0 until arr.length()).mapNotNull { i -> runCatching { SubtitleCue.read(arr.getJSONObject(i)) }.getOrNull() }
                } ?: emptyList(),
                j.optJSONArray("transitions")?.let { arr ->
                    (0 until arr.length()).mapNotNull { i -> runCatching { ClipTransition.read(arr.getJSONObject(i)) }.getOrNull() }
                } ?: emptyList(),
                j.optJSONArray("audioTracks")?.let { arr ->
                    (0 until arr.length()).mapNotNull { i -> runCatching { AudioTrack.read(arr.getJSONObject(i)) }.getOrNull() }
                } ?: emptyList(),
                j.optString("lastTrimCutClipId").takeIf { it.isNotBlank() },
                if (j.isNull("lastTrimCutSourceMs")) null else j.optLong("lastTrimCutSourceMs"),
                j.optJSONObject("textWatermark")?.let(TextWatermark::read))
        }
    }
}

/** Codec reutilizável para armazenamento e importação futura de projetos (schema 4). */
internal object ProjectCollectionCodec {
    fun decode(raw: String): List<VideoProject> {
        val entries = JSONArray(raw)
        return (0 until entries.length()).map { VideoProject.read(entries.getJSONObject(it)) }
            .sortedByDescending { it.updated }
    }

    fun encode(projects: List<VideoProject>): String =
        JSONArray().apply { projects.forEach { put(it.json()) } }.toString()

    /** Não transforma um JSON inválido em lista vazia ao decidir salvar/excluir. */
    fun recover(primary: String, backup: String?): List<VideoProject> =
        runCatching { decode(primary) }.getOrElse { invalid ->
            if (backup == null) throw IllegalStateException(
                "O cadastro de projetos está ilegível e não há cópia recuperável.", invalid)
            runCatching { decode(backup) }.getOrElse { invalidBackup ->
                throw IllegalStateException("O cadastro e sua cópia de segurança estão ilegíveis.", invalidBackup)
            }
        }
}

class ProjectStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("projects", Context.MODE_PRIVATE)
    private val currentKey = "projects"
    private val backupKey = "projects_backup"

    private fun rawPrimary(): String = prefs.getString(currentKey, "[]") ?: "[]"
    private fun readForMutation(): List<VideoProject> = ProjectCollectionCodec.recover(
        rawPrimary(), prefs.getString(backupKey, null))

    fun load(): List<VideoProject> = try {
        readForMutation()
    } catch (failure: Exception) {
        DiagnosticRecorder.exception("project_store_read", failure)
        // A UI pode mostrar zero projetos, mas save/delete NÃO podem sobrescrever dados ilegíveis.
        emptyList()
    }

    private fun persist(projects: List<VideoProject>) {
        val primary = rawPrimary()
        // Se o primário foi corrompido, conservar o último backup válido.
        val safePrevious = runCatching { ProjectCollectionCodec.decode(primary); primary }
            .getOrElse { prefs.getString(backupKey, null) ?: throw IllegalStateException(
                "Não é seguro sobrescrever projetos ilegíveis.") }
        check(prefs.edit()
            .putString(backupKey, safePrevious)
            .putString(currentKey, ProjectCollectionCodec.encode(projects))
            .commit()) { "Falha ao persistir projetos no armazenamento local" }
    }

    fun save(project: VideoProject) {
        val updated = project.copy(updated = System.currentTimeMillis())
        persist(readForMutation().filterNot { it.id == project.id } + updated)
    }

    fun delete(id: String) {
        persist(readForMutation().filterNot { it.id == id })
    }

    fun theme(): String = prefs.getString("theme", "Sistema") ?: "Sistema"
    fun setTheme(t: String) = prefs.edit().putString("theme", t).apply()
    fun quality(): Int = prefs.getInt("quality", 1080).takeIf { it == 720 || it == 1080 } ?: 1080
    fun setQuality(q: Int) { require(q == 720 || q == 1080); prefs.edit().putInt("quality", q).apply() }
}

/** Retorna zero quando a duração não é verificável. Recomendado para importações novas. */
fun verifiedMediaDuration(context: Context, uri: Uri): Long = runCatching {
    val retriever = MediaMetadataRetriever()
    try {
        retriever.setDataSource(context, uri)
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            ?.toLongOrNull()?.takeIf { it > 0L } ?: 0L
    } finally { retriever.release() }
}.getOrDefault(0L)

/** Fallback histórico usado em fluxos legados; não utilizar para validar mídia importada. */
fun mediaDuration(context: Context, uri: Uri): Long =
    verifiedMediaDuration(context, uri).takeIf { it > 0L } ?: 1000L

fun formatMs(ms: Long): String {
    val sec = ms.coerceAtLeast(0) / 1000L
    return "%02d:%02d".format(sec / 60, sec % 60)
}
