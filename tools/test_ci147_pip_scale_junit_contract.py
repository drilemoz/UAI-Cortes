#!/usr/bin/env python3
"""CI147: impedir teste antigo de rejeitar 91% na escala PiP (5%–150%)."""
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]
unit = (ROOT / 'app/src/test/java/br/com/uaicortes/OverlayEditorLogicTest.kt').read_text(encoding='utf-8')
logic = (ROOT / 'app/src/main/java/br/com/uaicortes/OverlayEditorLogic.kt').read_text(encoding='utf-8')
assert 'const val MIN_SCALE = 0.05f' in logic
assert 'const val MAX_SCALE = 1.50f' in logic
assert 'it in 5..150' in logic
for value in ['"90"', '"91"', '"100%"', '"150"']:
    assert f'OverlayEditorLogic.parsePercentage({value})' in unit, value
assert 'assertEquals(null, OverlayEditorLogic.parsePercentage("91"))' not in unit
assert 'assertEquals(null, OverlayEditorLogic.parsePercentage("151"))' in unit
print('OK: CI147 - teste JUnit aceita 5..150%, sem rejeitar 91% e sem liberar 151%')
