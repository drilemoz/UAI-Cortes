# Uai Cortes 0.1.63: validação

## Alterações

- ViewModel específico para renderização de imagens com motor por tentativa e coordenador de exportação compartilhado.
- Opções de imagem salvas como dados simples na UI em rotação.
- Cancelamento durante preparo adia limpeza para evitar concorrência com a codificação PNG.
- Testes novos do ciclo de vida de estados e callbacks tardios.

## Verificações

- `python3 tools/validate_source.py`: aprovado.
- Verificação sintática PSI de 120 fontes Kotlin: aprovada.
- Verificação estrutural/CRC do ZIP: executar após empacotamento.
- Compilação Kotlin/JVM isolada e execução de nove testes de coordenadores: aprovadas.

## Pendente

- `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleRelease` no CI
- Teste Android real: rotação durante preparação/render, cancelar/retentar, armazenamento insuficiente e cópia MediaStore.
- Comparação visual de MP4 exportado; bitrate é solicitação ao codec, não garantia.
