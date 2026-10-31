# Arquitetura da exportação de imagens 0.1.63

`AnimateImagesScreen` -> `ImageAnimationTaskViewModel` -> `ExportTaskCoordinator` + `ImageAnimationEngine`.

- A tela controla sequência, opções e preview; não guarda Transformer/Job/progresso do processamento.
- O ViewModel mantém somente a tentativa ativa e seu próprio motor, com contexto `Application`.
- O coordenador rejeita callbacks de tentativas anteriores e separa preparação, renderização e gravação final.
- Cada tentativa usa um novo motor, evitando que IO cancelado afete arquivos temporários de outra.
- A gravação no MediaStore é uma etapa não cancelável pelo botão de UI.
- Estado temporário de exportação não é persistido entre processos.
- Abertura do MP4 no editor continua uma ação explícita após a galeria confirmar salvamento.
