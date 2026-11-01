# Uai Cortes v0.1.92 — correção CI 77

## Evidência fornecida pelo usuário
- Workflow `Gerar APK Uai Cortes 77`, tarefa `:app:lintDebug`.
- Relatório: 1 erro `Range`, 92 avisos e 2 observações. Local: `SubtitleAudioCache.kt:57`.
- `:app:compileDebugKotlin`, `:app:compileReleaseKotlin`, `:app:testDebugUnitTest`, `:app:lintVitalRelease` e `:app:assembleRelease` foram concluídos no workflow 77, mas `lintDebug` falhou e impediu publicação.

## Correção implementada
- Verificar índice `>= 0` e não-nulidade antes de ler data de modificação em `Cursor`.
- Continuar usando `-1L` quando o provedor não informa a coluna/data; não alterar fingerprint/TTL nem dados persistidos.
- Validador estrutural previne reintrodução do padrão `getLong(getColumnIndex(...))`.
- Próxima versão `0.1.92` / `versionCode=93`; cinco APKs Release configurados no CI.

## Limitações
- Validação estrutural e parser Kotlin locais não substituem Android Lint/Gradle. Rodar `:app:testDebugUnitTest :app:lintDebug :app:assembleRelease` no CI antes de afirmar sucesso do build.
- Sem teste Android físico, sem teste de exportação de vídeo nem da API Gemini nesta alteração.
