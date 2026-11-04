# Uai Cortes 0.1.119 — Etapa 2: fidelidade prévia/exportação

## Divergências corrigidas no código

1. Legendas e marca d'água ficavam atrás do PiP na saída Media3, mas apareciam acima na prévia Compose. Com PiP, a posição dos textos mudou de efeito por `EditedMediaItem` para efeito do `Composition` (ordem Media3: item, compositor, composição). Sem PiP, a rota antiga permanece.
2. Duas janelas PiP sobrepostas tinham ordenação invertida. O projeto e a prévia desenham a última por cima; o compositor Media3 desenha a primeira entrada sobre as demais. `ExportLayerOrder` passa as entradas na ordem inversa, sem mudar os dados salvos.
3. Preview principal ignorava movimento de zoom/pan/recuo. O quadro do player recebe o mesmo `Motion` usado pela matriz exportada, sem mover textos ou PiP.
4. Um cue de texto vazio e longo escondia outro cue válido apenas na prévia. Compartilhada `SubtitlePreviewPolicy.firstAt` com o exportador.

## Testes incluídos

- `ExportLayerOrderTest` — ordem de sobreposição e lista original preservada.
- `SubtitlePreviewPolicyTest` — cue vazia/overlap/boundaries.
- `TransitionMotionTest` — mesma curva no preview e Media3.
- `ExportPipelineSmokeAndroidTest.exportPipWithSubtitleAboveCompositorProducesReadableMp4` — cenário instrumental PiP + legenda, exporta e confere MP4 com limpeza no MediaStore; NÃO verifica pixel por pixel, e precisa de Android real.
- `tools/test_preview_export_parity.py` — contratos de ligação entre prévia e exportador.

## Execução Android ainda pendente

```sh
gradle --no-daemon :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:lintDebug :app:assembleRelease
gradle --no-daemon :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=br.com.uaicortes.ExportPipelineSmokeAndroidTest
```

É necessário inspecionar visualmente um vídeo com **duas PiPs sobrepostas e legenda cruzando ambas**, outro com **transição de pan/zoom**, e comparar som e sincronismo. O preview de efeitos coloridos/sépia/monocromático ainda é aproximado. Não houve build Android neste ambiente sem SDK/Gradle.
