package br.com.uaicortes

import org.json.JSONArray
import org.json.JSONObject

/** Contrato de leitura compatível com o JSON anterior do Animar Imagens (independente de projetos). */
internal data class ImageDraftSnapshot(val images: List<String>, val options: ImageAnimationOptions)
internal enum class ImageDraftStatus { EMPTY, PRIMARY, BACKUP, DAMAGED }
internal data class ImageDraftInspection(
    val snapshot: ImageDraftSnapshot?,
    val status: ImageDraftStatus
)

/** Não transforma JSON ilegível em rascunho vazio nem permite sobrescrevê-lo por acidente. */
internal object ImageAnimationDraftCodec {
    fun decode(raw: String): ImageDraftSnapshot {
        val json = JSONObject(raw)
        val values = json.getJSONArray("images")
        require(values.length() <= ImageAnimationPlan.MAX_IMAGES) { "Limite de fotos excedido" }
        val images = (0 until values.length()).map { index ->
            values.getString(index).also { require(it.startsWith("content://") && it.length <= 4096) }
        }
        require(images.distinct().size == images.size) { "Fotos duplicadas no rascunho" }
        val opts = json.getJSONObject("options")
        val options = ImageAnimationOptions(
            intensity = opts.optString("intensity", "normal"),
            secondsPerImage = opts.optInt("secondsPerImage", 5),
            aspect = opts.optString("aspect", "9:16"),
            quality = opts.optInt("quality", 720),
            fps = opts.optInt("fps", 30),
            transition = opts.optString("transition", "auto")
        ).checked()
        return ImageDraftSnapshot(images, options)
    }

    fun encode(images: List<String>, options: ImageAnimationOptions): String {
        require(images.size <= ImageAnimationPlan.MAX_IMAGES)
        require(images.distinct().size == images.size)
        require(images.all { it.startsWith("content://") && it.length <= 4096 })
        options.checked()
        return JSONObject().put("images", JSONArray(images)).put("options", JSONObject()
            .put("intensity", options.intensity)
            .put("secondsPerImage", options.secondsPerImage)
            .put("aspect", options.aspect)
            .put("quality", options.quality)
            .put("fps", options.fps)
            .put("transition", options.transition)).toString()
    }

    fun inspect(primary: String?, backup: String?): ImageDraftInspection {
        if (primary == null && backup == null) return ImageDraftInspection(null, ImageDraftStatus.EMPTY)
        val main = primary?.let { runCatching { decode(it) }.getOrNull() }
        if (main != null) return ImageDraftInspection(main, ImageDraftStatus.PRIMARY)
        val previous = backup?.let { runCatching { decode(it) }.getOrNull() }
        if (previous != null) return ImageDraftInspection(previous, ImageDraftStatus.BACKUP)
        return ImageDraftInspection(null, ImageDraftStatus.DAMAGED)
    }

    /** Devolve o último JSON íntegro a preservar como backup, mais o novo primário. */
    fun prepareWrite(primary: String?, backup: String?, next: ImageDraftSnapshot): Pair<String?, String> {
        val inspection = inspect(primary, backup)
        check(inspection.status != ImageDraftStatus.DAMAGED) { "Rascunho ilegível; restauração explícita necessária" }
        val safePrevious = when (inspection.status) {
            ImageDraftStatus.PRIMARY -> primary
            ImageDraftStatus.BACKUP -> backup
            else -> null
        }
        return safePrevious to encode(next.images, next.options)
    }
}

/** Mantém as imagens sem acesso no rascunho até uma remoção explícita pelo usuário. */
internal object ImageDraftAccessPolicy {
    fun inaccessible(images: List<String>, canRead: (String) -> Boolean): List<String> =
        images.distinct().filter { !canRead(it) }
}
