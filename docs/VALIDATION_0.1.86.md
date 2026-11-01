# Uai Cortes v0.1.86 — Correção do workflow 72

## Evidência do log

O arquivo enviado `UAI-Cortes-Gerar-APK-Uai-Cortes-72-logs.zip` registrou `:app:lintDebug FAILED`, após `:app:assembleRelease` ter executado. O Lint reportou 2 erros, 84 warnings e 2 hints. Primeiro erro explícito em `VideoEffects.kt:56`, acesso a `TextOverlay.TEXT_SIZE_PIXELS` no contorno. O mesmo acesso aparece na classe de sombra, que não tinha opt-in. O log resume somente o primeiro dos dois erros; não presumir que o segundo erro foi exibido individualmente.

## Correção

Importar `androidx.annotation.OptIn` e adicionar `@OptIn(UnstableApi::class)` exclusivamente a `SubtitleOutlineSpan` e `SubtitleShadowSpan`. Não alterar parâmetros de renderização, nem usar supressão de lint / baseline. A solução corresponde à documentação oficial do Android Media3: https://developer.android.com/media/media3/exoplayer/troubleshooting

## Validação

- `python3 tools/ci72_media3_optin_test.py`: verificação estática, não Lint real.
- `python3 tools/validate_source.py`: validação estrutural do projeto.
- Integridade de ZIP, XML/YAML e sintaxe Kotlin podem ser verificados separadamente.
- **Pendente:** Gradle Android real e `:app:lintDebug` no GitHub Actions, montagem de 5 APKs e testes físicos.

`schemaVersion=4`, signing e Secrets, TTS, PiP, exportação e composição de legendas são preservados.
