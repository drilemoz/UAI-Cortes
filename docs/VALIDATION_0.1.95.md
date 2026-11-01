# Validação v0.1.95 / versionCode 96 — 01/11/2026

## Alterações
- `GeminiTtsCore.kt`: captura opcional de idioma, sotaque, persona, contexto, apresentação vocal e altura informados por `ListVoices`; nomes e IDs originais preservados.
- `GeminiVoicePresentation.kt`, `GeminiVoiceSearch.kt`: rótulos em português somente para termos conhecidos (inclusive `Authoritative Advisor 4`), nome original disponível, busca por tradução/nome/ID/metadados.
- `GeminiVoicePreviewCache.kt`, `GeminiPreviewIdentity.kt`, `GeminiNarrationUi.kt`: prévia cache-first com identificação SHA-256, armazenada em cache privado validado WAV; primeira síntese tem confirmação explícita, cache hit não consulta o serviço. Limites: 24 amostras, 12 MiB, expiração por inatividade em 30 dias; Android pode limpar antecipadamente.
- Sem extração de prévia do site AI Studio: `ListVoices` omite `sample_audio`, e `GetVoice` não atende vozes predefinidas. Referência: https://ai.google.dev/api/voices e https://ai.google.dev/gemini-api/docs/speech-generation
- Projeto/legendas/Áudio/schemas anteriores preservados, inclusive `schemaVersion=4`.

## Testes de regressão
- Executar `python3 tools/validate_source.py` e `python3 tools/ci70_kotlin_visibility_test.py` e `python3 tools/ci72_media3_optin_test.py`.
- `GeminiVoicePresentationTest.kt`: tradução conservadora, voz desconhecida, metadados opcionais, busca e isolamento do hash por conta/modelo/voz/estilo.
- `GeminiVoicePreviewCacheAndroidTest.kt`: armazenamento, leitura, rejeição a corrupção e expiração; requer emulador/aparelho e ainda não foi executado.
- CI: `:app:testDebugUnitTest :app:lintDebug :app:assembleRelease` e geração dos 5 APKs.

## Verificações locais efetivamente executadas
- `tools/validate_source.py`: aprovado; validações anteriores (inclusive v0.1.94) aprovadas.
- `tools/ci70_kotlin_visibility_test.py` e `tools/ci72_media3_optin_test.py`: aprovados.
- Kotlin/JVM: 15 verificações executadas sobre implementação real de rótulos, busca, metadados e SHA-256.
- Kotlin/JVM: 7 verificações de cache de WAV executadas com parser WAV real e contexto Android simulado, incluindo expiração e corrupção. Isso NÃO substitui o teste Android.

## Testes físicos pendentes
1. Atualizar catálogo com chave válida, conferir 107 vozes (ou quantidade real informada pela API) e rótulos/IDs.
2. Sem rede: após gerar amostra, tocar novamente para validar a ausência de chamada e reprodução offline. Mudar voz/modelo/estilo e confirmar que ocorre novo consentimento.
3. Após limpeza de cache do Android: confirmar que prévia volta a exigir nova síntese/consentimento.
4. Testar voz com nome desconhecido e voz personalizada, e verificar metadados ausentes sem atributos inventados.
5. Gerar narração completa, salvar no SAF, adicionar à timeline, fechar/reabrir diálogo e comparar áudio exportado.

Este documento não equivale a build Android, teste do serviço com credenciais nem teste em dispositivo.
