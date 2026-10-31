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

/** Resultado explícito: um cadastro realmente novo não é o mesmo que uma leitura corrompida. */
internal data class ProjectCatalogRead(
    val projects: List<VideoProject>,
    val lastGoodRaw: String,
    val usedBackup: Boolean
)

/** Codec e regras de recuperação puras; o esquema de cada projeto permanece na versão 4. */
internal object ProjectCollectionCodec {
    fun decode(raw: String): List<VideoProject> {
        val entries = JSONArray(raw)
        val projects = (0 until entries.length()).map { VideoProject.read(entries.getJSONObject(it)) }
        require(projects.all { it.id.isNotBlank() }) { "Há um projeto sem identificador." }
        require(projects.map { it.id }.distinct().size == projects.size) {
            "Há identificadores de projeto duplicados."
        }
        return projects.sortedByDescending { it.updated }
    }

    fun encode(projects: List<VideoProject>): String {
        require(projects.all { it.id.isNotBlank() } &&
            projects.map { it.id }.distinct().size == projects.size) {
            "O cadastro contém projetos sem ID ou com ID duplicado."
        }
        return JSONArray().apply { projects.forEach { put(it.json()) } }.toString()
    }

    /** Ausência das duas chaves indica primeira execução. Se houver backup, nunca
     * converter uma chave primária ausente em lista vazia sem examinar esse backup. */
    fun inspect(primary: String?, backup: String?): ProjectCatalogRead {
        if (primary == null && backup == null) return ProjectCatalogRead(emptyList(), "[]", false)
        if (primary != null) {
            val current = runCatching { decode(primary) }.getOrNull()
            if (current != null) return ProjectCatalogRead(current, primary, false)
        }
        if (backup != null) {
            val recovered = runCatching { decode(backup) }.getOrNull()
            if (recovered != null) return ProjectCatalogRead(recovered, backup, true)
        }
        throw IllegalStateException("Cadastro de projetos ilegível e sem cópia recuperável. Nenhuma alteração será gravada.")
    }

    /** Mantido para consumidores existentes da versão anterior. */
    fun recover(primary: String, backup: String?): List<VideoProject> =
        inspect(primary, backup).projects

    /** Backup recebe exclusivamente dados previamente validados, nunca um JSON corrompido. */
    fun prepareWrite(primary: String?, backup: String?, projects: List<VideoProject>): Pair<String, String> {
        val previous = inspect(primary, backup).lastGoodRaw
        val next = encode(projects)
        return previous to next
    }
}

class ProjectStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("projects", Context.MODE_PRIVATE)
    private val currentKey = "projects"
    private val backupKey = "projects_backup"

    /** Leitura problemática fica visível à interface, sem expor caminhos ou mídias no diagnóstico. */
    @Volatile var lastReadIssue: String? = null
        private set

    private fun inspect(): ProjectCatalogRead {
        val result = ProjectCollectionCodec.inspect(
            prefs.getString(currentKey, null), prefs.getString(backupKey, null))
        lastReadIssue = if (result.usedBackup)
            "O cadastro principal está ausente ou ilegível. Foi recuperada a cópia de segurança; confira seus projetos antes de editar."
        else null
        return result
    }

    private fun readForMutation(): List<VideoProject> = inspect().projects

    fun load(): List<VideoProject> = try {
        readForMutation()
    } catch (failure: Exception) {
        lastReadIssue = "Não foi possível ler os projetos ou sua cópia de segurança. O salvamento está bloqueado para proteger os dados."
        DiagnosticRecorder.exception("project_store_read", failure)
        // A tela recebe um aviso explícito; operações de escrita continuam proibidas.
        emptyList()
    }

    private fun persist(projects: List<VideoProject>) {
        val (safePrevious, updated) = ProjectCollectionCodec.prepareWrite(
            prefs.getString(currentKey, null), prefs.getString(backupKey, null), projects)
        check(prefs.edit()
            .putString(backupKey, safePrevious)
            .putString(currentKey, updated)
            .commit()) { "Falha ao persistir projetos no armazenamento local" }
    }

    fun save(project: VideoProject) {
        val updated = project.copy(updated = System.currentTimeMillis())
        persist(readForMutation().filterNot { it.id == project.id } + updated)
    }

    fun delete(id: String) {
        persist(readForMutation().filterNot { it.id == id })
    }

    /** Uma transação de preferência para excluir o cadastro; mantém backup validado. */
    fun deleteAll() { persist(emptyList()) }

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
