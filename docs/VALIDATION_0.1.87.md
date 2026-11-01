# Uai Cortes 0.1.87 — Animar Imagens

## Escopo

- Movimento da câmera selecionável: `auto`, `zoom_in`, `zoom_out`, `horizontal`, `vertical`.
- Compatibilidade dos rascunhos legados: opção `motion` ausente vira `auto`.
- Rascunhos com movimento desconhecido não sobrescrevem silenciosamente backup íntegro.
- Prévia com fundo ampliado escurecido e imagem inteira no centro, ambos movimentados; toque na foto salta ao meio da sua duração e pausa.
- Controles em ModalBottomSheet; ações independentes para guardar rascunho, visualizar prévia e gerar MP4.
- Um único planejador `ImageAnimationPlan.frame` é usado na prévia Compose e no efeito Media3.

## Validações locais executadas

- `python3 tools/validate_source.py`: aprovado.
- Kotlin PSI: 179 fontes Kotlin de `app/src`, zero erros de parsing.
- Kotlin/JVM isolado: 121.200 amostras de quadros (5 presets × 3 intensidades × 80 posições × 101 instantes), limites de zoom, pan, direção; validação de presets, seleção do tempo médio e opção legada. Aprovado.
- ZIP: CRC e contagem de arquivos validados após empacotamento.

## Ainda pendente

- Compilação real `:app:testDebugUnitTest :app:lintDebug :app:assembleRelease` em ambiente Gradle/Android SDK.
- Testes do codec JSON com JUnit/`org.json` no CI, inclusive persistência, versão antiga e recuperação de backup.
- Reprodução visual e exportação MP4 720p/1080p com fotos retrato, paisagem, EXIF e fontes SAF; comparação Compose versus Media3.
- Comportamento da BottomSheet e seleção/reordenação sob toques no Android físico.
- Perfis térmicos, consumo e pressão de memória com 15–80 fotos.

Não confundir sintaxe, inspeção estática e testes Kotlin/JVM isolados com build Android completo ou validação do MP4 final.
