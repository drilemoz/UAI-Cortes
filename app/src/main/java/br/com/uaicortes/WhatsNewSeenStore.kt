package br.com.uaicortes

import android.content.Context

/** Só persistimos a assinatura das novidades lidas, nunca dados de projetos. */
internal class WhatsNewSeenStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("uai_cortes_novidades", Context.MODE_PRIVATE)
    private val key = "assinaturas_lidas"
    private fun seen(): Set<String> = prefs.getStringSet(key, emptySet())?.toSet().orEmpty()

    fun shouldOpen(): Boolean = WhatsNewPolicy.shouldOpen(seen(), AppReleaseNotes.currentFingerprint)

    /** Grava antes de navegar para evitar repetir após fechar/reabrir o aplicativo. */
    fun markViewed(): Boolean = prefs.edit()
        .putStringSet(key, WhatsNewPolicy.acknowledged(seen(), AppReleaseNotes.currentFingerprint))
        .commit()
}
