#!/usr/bin/env python3
from pathlib import Path
root = Path(__file__).resolve().parents[1]
src = root / 'app/src/main/java/br/com/uaicortes'
read = lambda name: (src / (name + '.kt')).read_text()
sheet = read('TransitionScreen')
preview = read('TransitionVideoPreview')
plan = read('JunctionTransitionPlan')
engine = read('ExportEngine')
assert 'Mais efeitos' not in sheet and 'showMore' not in sheet
assert all(label in sheet for label in ('Corte direto', 'Dissolver', 'Empurrão lateral', 'Fade preto'))
assert preview.index('"Aplicar a todos"') < preview.index('onAudio(!audioOn)')
assert 'TransitionBatch.apply(p, safeBoundary, kind, durationMs, useAll)' in sheet
assert 'onApplyAll(!applyAll)' in preview
assert 'side.copy(sourceStartMs = at, heldFrame = true)' in plan
assert 'JunctionTransitionPlan.holdAudioBackedImages(joins)' in engine
assert 'ExportMp4Inspector.sourceHasAudio(context, Uri.parse(clip.uri))' in engine
assert 'EditedMediaItemSequence.withAudioAndVideoFrom(mainItems)' in engine
assert 'setRemoveAudio(true)' in read('JunctionTransitionRender')
tests = root / 'app/src/test/java/br/com/uaicortes'
for filename in ('SubtitleAnimationsTest','SubtitleKaraokeAccentsTest','SubtitleStudio202Test','SubtitleStudioMotion204Test'):
    code = (tests / (filename + '.kt')).read_text()
    assert 'SubtitleAnimations.SINGLE_WORD' in code
    assert 'assertEquals(28,SubtitleAnimations.choices.size)' not in code
    assert 'assertEquals(9, SubtitleStudioPresets.recommended.size)' not in code
assert (root / 'docs/VALIDATION_0.1.297.md').is_file()
print('OK: quatro transições, controles de som/lote, quadros de apoio e regressões do build 253')
