#!/usr/bin/env python3
"""Contratos de linha única, paridade visual e diálogos. Não é ensaio Android."""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
src = root / 'app/src/main/java/br/com/uaicortes'
read = lambda name: (src / f'{name}.kt').read_text()

renderer = read('SubtitleSingleLineRenderer')
assert '.setMaxLines(1)' in renderer and 'Layout.getDesiredWidth' in renderer
assert 'SubtitleSingleLinePolicy.display(frame.text)' in renderer
assert 'outline.getSelectionPath' in renderer and 'drawRoundRect' in renderer
assert 'frame.highlighted' in renderer and 'frame.boldActiveWord' in renderer
assert 'SubtitleSingleLineRenderer()' in read('SubtitleStyleGallery')
for name in ('VideoEffects', 'SubtitleSingleLinePreview'):
    assert 'SubtitleSingleLineRenderer()' in read(name)
    assert 'SubtitleSingleLinePolicy.place(' in read(name)
assert 'SubtitlePreviewPolicy.firstAt(cues, playbackMs)' in read('SubtitleLiveClock')
assert 'SubtitlePreviewPolicy.firstAt(p.subtitles, exportPreviewMs)' in read('ExportScreen')
assert 'showAll = true' not in read('SubtitleScreen')
assert 'SubtitleExportDialog(' in read('SubtitleScreen')
dialog = read('SubtitleExportDialog')
assert 'Row(Modifier.fillMaxWidth()' in dialog and dialog.count('FilterChip(selected =') == 2
assert all(action in dialog for action in ('SRT em Downloads', 'Escolher destino', 'Fechar'))
assert 'onChooseDestination(format)' in dialog and 'onSaveSrtToDownloads' in dialog
proofread = read('SubtitleProofreadDialog')
assert 'color = MaterialTheme.colorScheme.errorContainer' in proofread
assert 'onClick = { request(model.alternative) }' in proofread
assert 'request(model)' in proofread
assert (root / 'app/src/test/java/br/com/uaicortes/SubtitleSingleLinePolicyTest.kt').is_file()
assert (root / 'app/src/androidTest/java/br/com/uaicortes/SubtitleSingleLineRendererAndroidTest.kt').is_file()
print('OK: linha única sem alterar dados, desenho compartilhado e diálogos de legenda')
