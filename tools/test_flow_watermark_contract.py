#!/usr/bin/env python3
"""Contrato offline da tela de marca e do gerador Flow. Não simula Android real."""
from pathlib import Path
r=Path(__file__).resolve().parents[1]
p=r/'app/src/main/java/br/com/uaicortes'
get=lambda name:(p/name).read_text()
ui=get('TextWatermarkScreen.kt')
style=get('WatermarkStylePanel.kt')
mark=get('TextWatermark.kt')
assert 'SubtitleOverlayOnFrame(watermark, editable = true' in ui
assert 'onPositionCommit = { x, y -> draft = draft.copy(x = x, y = y) }' in ui
assert 'WatermarkStepSlider("Opacidade (%)"' in style
assert 'WatermarkStepSlider("Tamanho"' in style
assert 'removeConfirm' in ui and 'guides' in ui
assert 'WatermarkDefaultsStore(context)' in ui
assert 'opacity.coerceIn(0f, 1f)' in mark
assert 'fontFamily = w.fontFamily' in mark and 'shadowOffsetDp = w.shadowOffsetDp' in mark
assert '.put("schemaVersion", 4)' in get('ProjectStore.kt')
assert 'legacySceneDraftJson' in get('ProjectStore.kt')
assert (r/'app/src/test/java/br/com/uaicortes/LegacySceneProjectWatermarkTest.kt').exists()
print('OK: marca de texto preservada e compatibilidade com projetos anteriores')
