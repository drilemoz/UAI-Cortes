# Uai Cortes 0.1.55: validação e pendências

## Verificações nesta entrega

1. `python3 tools/validate_source.py`: contratos de interface, versionamento, módulos de mídia e testes; verificação estrutural, não semântica de Kotlin.
2. Parser Kotlin PSI: 102 arquivos de produção e testes analisados, nenhum erro sintático (recontar após última alteração).
3. Compilação Kotlin/JVM isolada de `ProjectOperations.kt`, `OverlayOperations.kt`, `MediaImportOperations.kt`, `OverlayPlacement.kt`, `OverlayEditorLogic.kt`, `OverlayTiming.kt` e `ImageAnimationPlan.kt` usando modelos e Android simulados. Smoke com asserções para inclusão PiP, colisão, importação, limites e persistência simulada passou.
4. Os 10 métodos JUnit dos três arquivos novos foram compilados e executados em JVM isolada usando simulações de Android e `org.junit`. Todos passaram nesse ambiente; **não foram executados via Gradle Android**.
5. ZIP com CRC verificado.

## Pendente

- `gradle :app:testDebugUnitTest :app:lintDebug :app:assembleRelease` no CI, e publicação das cinco variantes de APK somente após sucesso.
- Em celular, importar vídeo por galeria e SAF, adicionar e substituir PiP, manter sobreposição e reposicionar, carregar projeto existente, excluir, acrescentar clipes e inserir áudio.
- Validar rotação e retomada de seleções da 0.1.54 após abrir/editar projetos da 0.1.55.
- Testar mídias ilegíveis e Uri sem permissão: `mediaDuration()` atualmente retorna 1000 ms em certas falhas, não declarar verificação real de conteúdo somente por resultado positivo.
- Conferir prévia e MP4 exportado; execução isolada do Kotlin não comprova Media3 em dispositivo real.

A entrega é um ZIP do código-fonte, não um APK compilado.
