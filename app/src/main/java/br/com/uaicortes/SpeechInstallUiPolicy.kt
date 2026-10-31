package br.com.uaicortes

/** Evita exibir etapas transitórias depois que o modelo foi reconhecido como instalado.
 * Sem dependências Android para facilitar os testes JVM.
 */
internal object SpeechInstallUiPolicy {
    fun idleStage(installed: Boolean, busy: Boolean, stage: String, failure: String): String? =
        stage.takeIf { !installed && !busy && failure.isBlank() && it.isNotBlank() }
}
