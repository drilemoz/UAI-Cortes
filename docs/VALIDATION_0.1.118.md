# Validação da v0.1.118 — etapa 1: exportação e testes

## Implementado

1. Fixture gerado com FFmpeg: `app/src/androidTest/assets/export_smoke_16x9.mp4`, 1,6 segundo, H.264 320×180, AAC, sem mídia de usuário.
2. Teste `ExportPipelineSmokeAndroidTest` cria projeto com clipe recortado e legenda, chama `ExportEngine` no main looper, aguarda conclusão e inspeciona o MP4 no MediaStore com `MediaMetadataRetriever` e `MediaExtractor` (vídeo e áudio). Por fim remove a mídia criada pelo teste e arquivo-fonte temporário.
3. CI executa todos os contratos Python, verificações extras de compatibilidade Kotlin/Media3, testes JVM, Lint, compilação do código AndroidTest e montagem dos cinco APKs Release.

## Comandos locais

```sh
python3 tools/check_version_sync.py
python3 tools/test_export_smoke_fixture.py
python3 tools/validate_source.py
for f in tools/test_*.py; do python3 "$f"; done
python3 tools/ci70_kotlin_visibility_test.py
python3 tools/ci72_media3_optin_test.py
```

## Execução pendente, obrigatória antes de considerar a exportação validada

- GitHub Actions: `gradle --no-daemon :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:lintDebug :app:assembleRelease`.
- Em Android físico/emulador com hardware/encoder suportado, executar `gradle --no-daemon :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=br.com.uaicortes.ExportPipelineSmokeAndroidTest` (requer dispositivo ou emulador conectado).
- Revisar com vídeos reais de 16:9/9:16: efeitos, cortes consecutivos, múltiplas faixas, transições, PiP e legenda. O smoke testa apenas UM caso, não toda a fidelidade visual/acústica.
- Não há Gradle nem SDK Android completo neste ambiente; os comandos Android acima não foram executados nesta etapa.
