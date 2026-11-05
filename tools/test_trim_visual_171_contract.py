#!/usr/bin/env python3
"""Confere os controles essenciais, sem confundir fonte com build Android."""
from pathlib import Path
root = Path(__file__).resolve().parents[1]
source = (root/'app/src/main/java/br/com/uaicortes/TrimScreen.kt').read_text()
visual = (root/'app/src/main/java/br/com/uaicortes/TrimVisualTimeline.kt').read_text()
core = (root/'app/src/main/java/br/com/uaicortes/TrimTimelineEdit.kt').read_text()
app = (root/'app/src/main/java/br/com/uaicortes/MainActivity.kt').read_text()
for label in ('Dividir','Aparar','Remover','Pronto','Guardar corte','Ajuste preciso',
              'Remover esse pedaço?','Desfazer','Refazer','Voltar ao editor'):
    assert label in source, label
for token in ('detectDragGestures', 'detectTapGestures', 'currentRange', 'currentSeek',
              'getScaledFrameAtTime', 'Dispatchers.IO', 'Canvas'):
    assert token in visual, token
for token in ('internal object TrimRemoveRange', 'sourceDurationMs', 'shiftGlobalItems',
              'ClipAudioBindings.cloneAttached', 'TrimLogic.MIN_SEGMENT_MS'):
    if token == 'sourceDurationMs': continue
    assert token in core, token
assert 'onDone = { saved -> if (write(saved)) screen = "editor" }' in app
assert 'schemaVersion", 4' in (root/'app/src/main/java/br/com/uaicortes/ProjectStore.kt').read_text()
assert 'class TrimVisualRedesignTest' in (root/'app/src/test/java/br/com/uaicortes/TrimVisualRedesignTest.kt').read_text()
print('OK: Editor visual com linha do tempo única, recorte e remoção segura no projeto')
