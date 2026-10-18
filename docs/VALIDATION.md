# Validação do Uai Cortes 0.1.1

## Fonte do erro
Workflow **Gerar APK Uai Cortes #1**, `SDK Android` (18/10/2026):
`Warning: Failed to find package 'tools'`; `sdkmanager` encerrou com código 1.
Falha aconteceu antes do passo Gradle. Não há evidência de erro Kotlin nesses logs.

## Correções
- `android-actions/setup-android@v4` com `packages: 'platform-tools'`, excluindo `tools` legado.
- `sdkmanager --install 'platforms;android-36' 'build-tools;36.0.0'`, com checagem dos binários essenciais.
- `compileSdk=36`, `targetSdk=36`, `versionName=0.1.1`, `versionCode=2`.
- Nome APK, release tag, About, README e release notes sincronizados.

## Validação local
- Sintaxe estrutural do YAML, correspondência de versões, integridade do ZIP e imagens: verificar no pacote.
- Sem SDK/Gradle completo neste ambiente: não é possível declarar que `assembleRelease` compilou.
- No próximo run, executar `testDebugUnitTest`, `lintDebug`, `assembleRelease`, verificação `apksigner` e a publicação em Release Assets.
- Posteriormente testar PiP, áudio e exportação em dispositivos reais, incluindo restrições de mídia e Android 8 (minSdk=26).
