#!/usr/bin/env python3
"""Verificações estruturais do salvar-e-voltar de Áudio: não substituem o build Android."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
ui=(root/'app/src/main/java/br/com/uaicortes/AudioScreen.kt').read_text()
main=(root/'app/src/main/java/br/com/uaicortes/MainActivity.kt').read_text()
labels=(root/'app/src/main/java/br/com/uaicortes/AudioScreenLabels.kt').read_text()
assert 'onSaveAndReturn: (VideoProject) -> Boolean' in ui
assert 'Text("Salvar e voltar ao vídeo"' in ui
assert 'HorizontalDivider()' in ui and '.navigationBarsPadding()' in ui
assert 'editingAudioId != null ->' in ui
assert 'saveReturnError' in ui
assert 'enabled=!exporting' in ui
assert '!onSaveAndReturn(p) ->' in ui
assert 'onSaveAndReturn = { changed ->' in main
assert 'if (write(changed)) { screen = "editor"; true } else false' in main
assert 'AudioScreenLabels.readableVideoClip(clip.title, index)' in ui
assert 'fun readableVideoClip' in labels
# Não substituir o callback Gemini: deve permanecer na tela Áudio para conferência.
assert 'onSaveGeminiToAudio = { changed ->' in main
assert 'message = "Narração Gemini incluída na mixagem.' in ui
print('OK: salvamento explícito, retorno condicional, rascunho protegido, títulos compactos')
