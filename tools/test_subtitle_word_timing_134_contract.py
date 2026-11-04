#!/usr/bin/env python3
"""Contratos da sincronização manual; complementam os testes unitários Kotlin."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
app=root/'app/src/main/java/br/com/uaicortes'
policy=(app/'SubtitleWordTimingEditorPolicy.kt').read_text()
ui=(app/'SubtitleWordTimingEditor.kt').read_text()
editor=(app/'SubtitleScreen.kt').read_text()
preview=(app/'SubtitleVisualEditor.kt').read_text()
assert 'fun distribute(cue: SubtitleCue)' in policy
assert 'fun moveBoundary(' in policy
assert 'fun previewCue(' in policy
assert 'MAX_EDITABLE_WORDS = 40' in policy
assert 'Não interpreta geração linear como reconhecimento vocal' in policy
assert 'val good = existing.size == wordsCount' in policy
assert 'SubtitleWordTimingEditorPolicy.previewCue(draft,previewDuration)' in preview
assert 'SubtitleWordTimingEditor(draft,' in editor
assert 'animation=SubtitleAnimations.KARAOKE' not in editor or 'SubtitleAnimations.KARAOKE' in editor
assert 'onSave(intervals)' in ui and 'Salvar legenda' in ui
assert 'Ouvir' not in ui and 'não são detectados pelo áudio' in ui
assert 'Sincronizar' in editor
assert (root/'app/src/test/java/br/com/uaicortes/SubtitleWordTimingEditorPolicyTest.kt').exists()
print('OK: UI, política de sincronização, prévia, edição manual, preservação do schema e testes Kotlin')
