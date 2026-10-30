package br.com.uaicortes

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

/** Rascunho das fotos e ajustes, separado dos projetos de vídeo (schemaVersion 4). */
internal data class ImageAnimationDraft(val images: List<Uri>, val options: ImageAnimationOptions)

internal class ImageAnimationDraftStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("image_animation_draft", Context.MODE_PRIVATE)

    fun load(): ImageAnimationDraft? = runCatching {
        val raw = preferences.getString("current", null) ?: return@runCatching null
        val json = JSONObject(raw)
        val list = json.optJSONArray("images") ?: JSONArray()
        val images = (0 until minOf(list.length(), ImageAnimationPlan.MAX_IMAGES)).mapNotNull { index ->
            list.optString(index).takeIf { it.startsWith("content://") }?.let(Uri::parse)
        }.distinct()
        val opts = json.optJSONObject("options") ?: JSONObject()
        val options = ImageAnimationOptions(
            intensity = opts.optString("intensity", "normal"),
            secondsPerImage = opts.optInt("secondsPerImage", 5),
            aspect = opts.optString("aspect", "9:16"),
            quality = opts.optInt("quality", 720),
            transition = opts.optString("transition", "auto")
        ).checked()
        ImageAnimationDraft(images, options)
    }.getOrNull()

    /** commit síncrono: somente retorna sucesso após o rascunho ser gravado. */
    fun save(images: List<Uri>, options: ImageAnimationOptions): Boolean = runCatching {
        require(images.size <= ImageAnimationPlan.MAX_IMAGES)
        options.checked()
        val json = JSONObject().put("images", JSONArray().apply {
            images.forEach { uri -> put(uri.toString()) }
        }).put("options", JSONObject()
            .put("intensity", options.intensity)
            .put("secondsPerImage", options.secondsPerImage)
            .put("aspect", options.aspect)
            .put("quality", options.quality)
            .put("transition", options.transition))
        preferences.edit().putString("current", json.toString()).commit()
    }.getOrDefault(false)
}
