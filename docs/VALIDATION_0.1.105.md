# Uai Cortes v0.1.105 — Validação da prévia Animar Imagens

## Mudanças
- `ImageAnimationPreviewDecoder.decode`: lê o arquivo SAF/MediaStore por stream; evita miniaturas que mudam enquadramento, aplica orientação EXIF, amostra em duas etapas e não amplia imagens pequenas.
- `ImagePreviewSizing`: determina proporção, teto de decodificação e `inSampleSize` como rotina Kotlin sem Android.
- `AnimateImagesScreen`: altura de prévia maior, modos Rápida (720), Equilibrada (1280) e Alta (1920), botão Ampliar; mesma função visual para painel e diálogo.

## Validações automáticas
- `python3 tools/validate_source.py` e `python3 tools/test_version_sync.py`.
- Compilação isolada Kotlin de `ImagePreviewSizing.kt`, e testes funcionais de proporção, amostragem, modo sem upscaling.
- `python3 tools/package_source.py --output ...` com validação após extração.

## Pendências obrigatórias
- Compilar Gradle Android no GitHub Actions e executar os testes unitários Android.
- Testar fotos horizontais/verticais, grandes/pequenas, JPEG com EXIF e arquivos SAF em celulares físicos, verificando memória, nitidez, enquadramento e fluidez de 80 fotos.
- Comparar a prévia com MP4 exportado; a prévia não é renderização H.264 nem garantia de idêntica qualidade perceptiva.
