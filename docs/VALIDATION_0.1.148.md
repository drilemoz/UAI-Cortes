# Verificações v0.1.148 · versionCode 149

Escopo: apenas Compose UI de `GeminiSettingsUi.kt`, `GeminiKeyPoolSettings.kt` e `GeminiUsageUi.kt`, mantendo armazenamento e chamadas Gemini intactos.

- Executar `tools/validate_source.py`, regressões Python e checagem PSI Kotlin. Reportar resultados separadamente da compilação Android.
- Em CI validar :app:testDebugUnitTest, :app:lintDebug e :app:assembleRelease.
- Testar em aparelho: layout com fontes 1.0/1.3, modo claro/escuro, teclado aberto no cadastro de chave, barra de 6 ações sem corte lateral, lista de 8 credenciais, estados de chave ativa e avisos 429.
- Segurança: confirmar copy/export/remove, testar TTS com consentimento, não imprimir segredos; validar troca 429 somente entre projetos distintos e somente se autorizada.
- Consumo: verificar totais locais de 24h/7d/30d e detalhes, sem tratar tokens ausentes como zero nem inventar custos/saldo.
