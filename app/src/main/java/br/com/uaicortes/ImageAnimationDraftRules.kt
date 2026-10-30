package br.com.uaicortes

/** Regra compartilhável: sair sem aviso somente se nada mudou desde o último salvamento. */
internal object ImageAnimationDraftRules {
    fun changed(saved: List<String>, savedOptions: ImageAnimationOptions,
                current: List<String>, currentOptions: ImageAnimationOptions): Boolean =
        saved != current || savedOptions != currentOptions
}
