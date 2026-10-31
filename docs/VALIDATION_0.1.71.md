# Validação 0.1.71

- Regressão: `SpeechInstallUiPolicyTest` cobre mensagem antiga após sucesso, progresso ativo, falha e cancelamento.
- Confirmação local: `tools/validate_source.py`, parser Kotlin PSI, integridade ZIP e testes JVM isolados.
- Pendente: build Android completo no CI e teste físico de transcrição com áudio em português.
- Esta alteração não reinstala nem elimina o modelo previamente baixado.
