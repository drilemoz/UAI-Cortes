# Validação Uai Cortes 0.1.170 — CI143

## Falha recebida
- Origem: `UAI-Cortes-Gerar-APK-Uai-Cortes-143-logs.zip`.
- `:app:assembleRelease`, `:app:testDebugUnitTest` e `:app:compileDebugAndroidTestKotlin` concluíram no workflow 143, mas `:app:lintDebug` falhou.
- Erro: `UnsafeOptInUsageError` em `app/src/main/java/br/com/uaicortes/StaticImageClip.kt:145` no método `setImageDurationMs` da Media3; 1 erro, 141 avisos e 2 dicas.

## Correção
- `@OptIn(UnstableApi::class)` do **AndroidX** aplicado à função `ClipMediaItems.playback`; imports explícitos.
- Lint mantido habilitado. Não foram alterados os algoritmos de imagem, vídeo ou exportação.
- Teste de regressão de fonte `tools/test_ci143_media3_image_optin_170.py`.
- Versão 0.1.170 / versionCode 171 e arquivos de release sincronizados.

## Limites
- Teste de código-fonte **não prova** que a tarefa `:app:lintDebug` passará: é necessária nova execução do GitHub Actions.
- Teste de funcionamento com foto, prévia e MP4 real em Android ainda pendente.

## Testes executados nesta entrega
- `python tools/check_version_sync.py`: aprovado (`0.1.170`, `versionCode 171`).
- `python tools/validate_source.py`: aprovado.
- Suite de 66 scripts `tools/test_*.py` em processos isolados: 66 aprovados, 0 falhas.
- Parser PSI Kotlin para `StaticImageClip.kt` e `AppReleaseNotes.kt`: 2 arquivos, 0 erros sintáticos.
- O ambiente de trabalho não dispõe do executável Gradle; portanto, o `:app:lintDebug` corrigido, o Gradle Release e os testes Android não foram reexecutados nesta entrega.
