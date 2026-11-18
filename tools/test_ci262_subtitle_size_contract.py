#!/usr/bin/env python3
"""Contrato do teste de tamanho de legenda atualizado após o CI 262."""
from pathlib import Path
root = Path(__file__).resolve().parents[1]
package = 'java/br/com/uaicortes'
tests = (root / 'app/src/test' / package / 'SrtSpeechAlignmentTest.kt').read_text()
appearance = (root / 'app/src/main' / package / 'SubtitleAppearancePolicy.kt').read_text()
srt, pink = tests.split('fun pinkKaraokeUsesItsOwnDefaultAndHonorsExplicitSize()', 1)
srt = srt.split('fun importedSrtUses14AndKeepsSavedSize()', 1)[1]
assert 'assertEquals(14, parsed.sizeSp)' in srt
assert 'assertEquals(24,' in srt
assert 'SubtitleAppearancePolicy.ROSA_SIZE_SP' in pink and 'assertEquals(20, pink.sizeSp)' in pink
assert 'assertEquals(14, override.sizeSp)' in pink
assert 'assertEquals(4, pink.outlineWidth)' in pink and 'assertEquals(1, pink.shadowOffsetDp)' in pink
assert 'const val ROSA_SIZE_SP = 20' in appearance and 'const val DEFAULT_SUBTITLE_SIZE_SP = 14' in appearance
assert 'outlineWidth=4' in appearance and 'shadowOffsetDp=1' in appearance
assert 'newCaptionsUse14WhileSavedSizesAndOverridesRemainAdjustable' not in tests
print('OK: SRT em 14 sp e karaokê rosa de 20 sp verificados por contratos separados')
