# Validação v0.1.267 — CI 224

- Base: ZIP v0.1.266 com 833 arquivos; erro do workflow 224 na etapa `Validar código-fonte e regressões locais`.
- Falha: `tools/test_main_image_clip_169_contract.py` exigia `.apply { if (clip.isImage) setFrameRate(30) }`, enquanto o código real utiliza um bloco `.apply { ... }` multilinha para também definir `setRemoveAudio(true)` durante captura.
- Correção: contrato extrai apenas o builder `EditedMediaItem.Builder(item)` da trilha principal e aplica verificação tolerante a espaços para a regra `clip.isImage -> setFrameRate(30)`; confere também a remoção de áudio do snapshot.
- Motor de exportação sem alterações, projeto schema 4 e workflow de cinco APKs preservados.
- Requer repetir todas as verificações Python do CI e empacotar por `tools/package_source.py`; build Android completo depende do CI.
