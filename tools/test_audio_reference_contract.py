#!/usr/bin/env python3
"""Contrato de áudio de referência local. Não substitui build Android/integração real."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
files=root/'app/src/main/java/br/com/uaicortes'
ui=(files/'GeminiNarrationUi.kt').read_text()
align=(files/'NarrationReferenceAlignment.kt').read_text()
dialog=(files/'AudioReferenceAlignmentUi.kt').read_text()
assert 'val referenceStarts = mutableStateOf<Map<String, Long>>(emptyMap())' in ui
assert 'AudioReferenceAlignmentDialog(project, timedLines, referenceStarts' in ui
assert '"Usar áudio como referência de tempo"' in ui
assert 'NarrationReferenceAlignment.placed(it.segment, referenceStarts)' in ui
assert 'startMs = NarrationReferenceAlignment.start(result.segment, referenceStarts)' in ui
assert 'NarrationReferenceAlignment.validate(segments, referenceStarts, current.durationMs)' in ui
assert 'GeminiAudioFiles.save(ctx, wave)' in ui
assert 'referenceStarts = emptyMap()' in ui
assert 'AudioReferenceAlignmentDialog' in dialog
assert 'ActivityResultContracts.OpenDocument()' in dialog
assert 'verifiedMediaDuration' in dialog
assert 'takePersistableUriPermission' in dialog
assert '"Aplicar horários"' in dialog and '"Cancelar"' in dialog
assert '"Marcar aqui"' in dialog and '"Restaurar horário original"' in dialog
assert 'projectPosition(referencePlayer' in dialog
assert 'sourceToProject' in dialog
assert 'val referenceIssue' in ui and 'referenceIssue != null' in ui
assert 'onApply(NarrationReferenceAlignment.cleaned' in dialog
assert 'fun validate(' in align and 'zipWithNext()' in align
assert 'api.synthesize(key, model, chosenVoice.id' in ui
assert 'v1beta/' not in dialog and 'v1beta/' not in align
assert 'client.files.upload' not in dialog and 'upload' not in dialog.lower()
print('OK: referência local, seleção confirmada, alinhamento manual, projeto preservado, sem upload de áudio')
