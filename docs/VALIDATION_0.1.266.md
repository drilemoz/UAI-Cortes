# Uai Cortes 0.1.266 — validação

Base: 0.1.265 / versionCode 266. Nova versão: 0.1.266 / versionCode 267.

Problema observado em `UaiCortes-diagnostico.txt`: `ERROR_CODE_MUXING_TIMEOUT` em `rendered_frame_capture`; o muxer do Transformer não concluiu a exportação temporária usada para o quadro.

Correção: usar `VideoFrameSaver.save()` diretamente quando não houver camadas visuais ativas; renderização com camadas usa janela maior (`FrameSnapshotTiming`) com trilha de vídeo apenas, watchdog de 30 s específico; se falhar, apresentar opção explícita de PNG original sem camadas.

UI: `UnifiedEditorTimeline` chama `onClip` no toque curto em clipe principal; a seleção durante arrasto continua separada; ações Cortar, Editar e Excluir estão no menu; botão Editar da faixa auxiliar removido.

Validações: executar `tools/validate_source.py`, `tools/test_frame_capture_clip_menu_266.py`, `tools/test_frame_snapshot_timing_265.py`, contratos anteriores e `tools/package_source.py`. Build Android e teste físico ainda necessários. Não considerar teste de sintaxe como build completo.
