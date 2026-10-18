# Validação do Uai Cortes 0.1.3

## Evidência: GitHub Actions Gerar APK #3 (18/10/2026)

- Android API 36 e Build Tools 36.0.0: instalação concluída.
- Assinatura persistente: chave preparada a partir dos quatro Secrets (seus valores não são incluídos na distribuição).
- `:app:compileDebugKotlin` e `:app:compileReleaseKotlin`: concluídos, com avisos de APIs de ícones depreciadas.
- `:app:testDebugUnitTest`: concluído.
- `:app:assembleRelease`: concluído e APK empacotado no runner.
- `:app:lintDebug`: **falha**, com dois erros `UnsafeOptInUsageError` em `MainActivity.kt:343`, ligados ao `PlayerView.setShowBuffering(...)`, além de 30 avisos.
- Resultado geral: job falhou em `lintDebug`; verificação e publicação do APK não foram executadas.

## Correção aplicada na 0.1.3

- `MainActivity.kt`: `@androidx.annotation.OptIn(UnstableApi::class)` apenas em `EditorScreen`, conforme https://developer.android.com/media/media3/exoplayer/troubleshooting
- Conservados os controles da prévia e seu indicador de buffering.
- `tools/validate_source.py`: valida a presença do opt-in e a versão. A análise de Lint permanece obrigatória, sem baseline ou `abortOnError = false`.
- Sincronizado `versionCode=4`, `versionName=0.1.3`, APK `UaiCortes-v0.1.3.apk` e GitHub Release `v0.1.3`.

## Validação local possível

- `python3 tools/validate_source.py`
- Sintaxe do workflow YAML, integridade/CRC do ZIP e inspeção do versionamento.
- Ambiente local sem Android SDK/Gradle completos: **não afirmar que Lint da versão 0.1.3 passou antes da execução no GitHub Actions**.
- Testes físicos e exportação real, incluindo PiP/vídeo/áudio, continuam pendentes.
