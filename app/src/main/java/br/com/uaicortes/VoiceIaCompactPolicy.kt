package br.com.uaicortes

/** Regras puras do lembrete de pendências; nunca disparam chamadas à API. */
internal object VoiceIaCompactPolicy {
    fun shouldRemind(total: Int, ready: Int, previouslyStarted: Boolean): Boolean =
        previouslyStarted && total > 0 && ready in 0 until total
}
