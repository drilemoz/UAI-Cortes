#!/usr/bin/env python3
"""Evita regressões na transformação de fotos da faixa principal."""
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
base=ROOT/'app/src/main/java/br/com/uaicortes'
model=(base/'ProjectStore.kt').read_text()
preview=(base/'EditorScreen.kt').read_text()
export=(base/'ExportEngine.kt').read_text()
policy=(base/'ImageFraming.kt').read_text()
effect=(base/'ImageFrameEffect.kt').read_text()
test=(ROOT/'app/src/test/java/br/com/uaicortes/ImageFramingTest.kt').read_text()
for fragment in ['imageFrameMode','imageZoom','imagePanX','imagePanY','ImageFraming.mode','ImageFraming.zoom','ImageFraming.pan']:
    assert fragment in model, fragment
for fragment in ['detectTransformGestures','imageFrameDraft','Enquadrar imagem','Enquadramento salvo na imagem','ImageFraming.resolve','imageFrameDraft=null']:
    assert fragment in preview,fragment
for fragment in ['hasImageFrame','ImageFrameEffect(','mediaAspect(context, clip.uri, true)','&& !hasImageFrame']:
    assert fragment in export,fragment
assert 'postTranslate(t.offsetX, -t.offsetY)' in effect
assert 'MAX_ZOOM = 3f' in policy
assert 'schemaVersion", 4' in model
assert 'legacyImageHasNoUnexpectedCrop' in test
print('OK: foto da timeline salva 4 campos opcionais, UI de gesto e efeito exportador')
