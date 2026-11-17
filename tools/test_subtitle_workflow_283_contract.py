#!/usr/bin/env python3
"""Fronteiras de integração do SRT, estilo inicial e exportação de prévia."""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
src = root / 'app/src/main/java/br/com/uaicortes'
read = lambda n: (src / n).read_text(encoding='utf-8')
editor, exporter, display = map(read, ('SubtitleScreen.kt', 'ExportEngine.kt', 'SubtitleExportPreview.kt'))
assert 'DEFAULT_SUBTITLE_SIZE_SP = 14' in read('SubtitleAppearancePolicy.kt')
for name in ['ProjectStore.kt', 'EditFeatures.kt', 'AutoSubtitleEngine.kt', 'GeminiSubtitleParser.kt']:
    assert 'DEFAULT_SUBTITLE_SIZE_SP' in read(name)
assert all(f'Text("{option}")' in editor for option in ('Compacta', 'Ampliada', 'Como vai exportar'))
assert 'exportPreviewAt = cursorMs' in editor and 'SubtitleExportPreviewDialog(p, it)' in editor
assert 'usePlatformDefaultWidth = false' in display and 'player.seekTo(position)' in display
assert 'onPreviewReady' in display and 'file?.delete()' in display
assert exporter.index('if (onPreviewReady != null)') < exporter.index('savedUri = saveToGallery')
assert 'exporter.launch' not in display and 'MediaStore' not in display
assert 'SrtImportReview(p, original' in editor and 'SubtitleBatchUndo(p.subtitles, changed)' in editor
review = read('SrtImportReview.kt')
assert 'Manter horários e importar' in review and 'Enviar áudio e conferir' in review
assert 'AutoSubtitleEngine.transcribe' in review and 'gemini.transcribe' in review
assert 'source.offsetMs' in review and 'window.timelineStartMs' in read('SubtitleSpeechSource.kt')
assert 'proposals[indexed.index]' in read('SrtSpeechAlignment.kt')
assert 'SubtitleCaptionInk.outlineRadius' in editor and 'SubtitleCaptionInk.outlineRadius' in read('VideoEffects.kt')
print('OK: tamanho 14, prévia MP4 privada, verificação de fala e métricas de karaokê')
