# Validação UaiCortes 0.1.268 / versionCode 269

- Fonte base: v0.1.267, 834 arquivos; log do workflow 225 anexado pelo usuário.
- Resultado do CI 225: regressões locais passaram; task `:app:testDebugUnitTest` reportou 714 testes concluídos, um reprovado (`FrameSnapshotTimingTest.nearEndNeverExceedsOriginal`, linha 17). A falha não foi de compilação Kotlin: o compilador executou e emitiu avisos de APIs deprecated.
- Causa: a janela de snapshot passou a ter `WINDOW_MS = 1600` e deslocamento para trás no final do clipe, fazendo o frame solicitado em 1999 ms do intervalo [1000,2000) ficar em `frameAtMs=999` dentro da janela [1000,2000), não em 500 ms.
- Teste corrigido para verificar o tempo relativo e os limites calculados, sem modificar `FrameSnapshotTiming.kt`. Incluídos casos de janela curta, fim de fonte longa e clamp fora de intervalo.
- Pendente: executar `:app:testDebugUnitTest` e os builds Android completos no CI, especialmente os cinco APKs Release.
