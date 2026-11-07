#!/usr/bin/env python3
"""CI169: teste de fontes deve acompanhar a fonte local Cursiva do Subtitle Studio.

Contrato textual complementar; a compilação JUnit real continua obrigatória no CI.
"""
from pathlib import Path
root = Path(__file__).resolve().parents[1]
source = (root/'app/src/main/java/br/com/uaicortes/SubtitleStyles.kt').read_text()
test = (root/'app/src/test/java/br/com/uaicortes/SubtitleTypographyTest.kt').read_text()
preview = (root/'app/src/main/java/br/com/uaicortes/SubtitleScreen.kt').read_text()
assert 'FONT_CURSIVE = "cursive"' in source
assert 'FONT_CURSIVE to "Cursiva"' in source
assert 'value == FONT_CURSIVE' in source
assert 'listOf("sans", "serif", "mono", "cursive")' in test
assert 'assertEquals("cursive", SubtitleStyles.normalizeFont("cursive"))' in test
assert 'fun cursiveTypographySurvivesProjectRoundTrip()' in test
assert 'SubtitleStyles.FONT_CURSIVE -> FontFamily.Cursive' in preview
assert '"invalid/remote-font"' in test
assert '"https://example.com/font.otf"' in test
print('OK: CI169 - fonte Cursiva preservada e coberta no JUnit; fallback de fontes desconhecidas')
