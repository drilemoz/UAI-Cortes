package br.com.uaicortes

import android.content.Context
import android.net.Uri

/** Rascunho separado do catálogo de projetos; mantém o formato JSON antigo. */
internal data class ImageAnimationDraft(val images: List<Uri>, val options: ImageAnimationOptions)

internal class ImageAnimationDraftStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("image_animation_draft", Context.MODE_PRIVATE)
    private val primaryKey = "current"
    private val backupKey = "backup"

    fun inspect(): ImageDraftInspection = ImageAnimationDraftCodec.inspect(
        preferences.getString(primaryKey, null), preferences.getString(backupKey, null))

    fun load(): ImageAnimationDraft? = inspect().snapshot?.let { draft ->
        ImageAnimationDraft(draft.images.map(Uri::parse), draft.options)
    }

    /** Grava apenas dados validados e preserva o JSON íntegro anterior. */
    fun save(images: List<Uri>, options: ImageAnimationOptions): Boolean = runCatching {
        val (previous, next) = ImageAnimationDraftCodec.prepareWrite(
            preferences.getString(primaryKey, null), preferences.getString(backupKey, null),
            ImageDraftSnapshot(images.map(Uri::toString), options))
        val editor = preferences.edit().putString(primaryKey, next)
        if (previous != null) editor.putString(backupKey, previous)
        editor.commit()
    }.getOrDefault(false)

    /** Só permite recomeçar se as duas cópias estiverem danificadas. Nunca é automático. */
    fun resetDamaged(): Boolean = runCatching {
        if (inspect().status != ImageDraftStatus.DAMAGED) return@runCatching false
        preferences.edit().remove(primaryKey).remove(backupKey).commit()
    }.getOrDefault(false)
}
