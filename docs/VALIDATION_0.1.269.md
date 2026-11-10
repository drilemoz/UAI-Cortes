# Validação 0.1.269 / versionCode 270

- Solicitação: abrir galeria visual de vídeos no Editor de Áudio e restaurar acesso à importação de áudio exclusivo do clipe no menu da linha do tempo.
- Base: código-fonte UaiCortes v0.1.268.
- A galeria reutiliza DeviceMediaBrowser com GalleryScope.VIDEOS e seleção única, incluindo miniaturas/pastas/pesquisa e fallback OpenDocument.
- Menu de clipe oferece ação direta para ClipAudioScreen; mantém ações de mover/duplicar/volume em Editar e não altera os gestos do UnifiedEditorTimeline.
- Testes direcionados, análise sintática e validação do pacote devem ser executados. Build Android/gestos no dispositivo precisam de confirmação real.
