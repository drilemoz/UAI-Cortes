#!/usr/bin/env python3
"""Contrato v0.1.275: sincronia por palavra e ajuste manual. Checagem estrutural, não é build Android."""
from pathlib import Path
src = Path(__file__).resolve().parents[1] / 'app/src/main/java/br/com/uaicortes'
r = (src / 'SubtitleTimingRefiner.kt').read_text(encoding='utf-8')
assert 'fun refineWords' in r and 'fun nearestEdge' in r and 'refineWords(timed, active)' in r
g = (src / 'SubtitleGlobalShift.kt').read_text(encoding='utf-8')
assert 'MAX_MS = 5000L' in g and 'fun allowed' in g
assert 'SubtitleSyncDialog' in (src / 'SubtitleScreen.kt').read_text(encoding='utf-8')
assert 'SubtitleGlobalShift.apply(' in (src / 'SubtitleScreen.kt').read_text(encoding='utf-8')
assert 'SubtitleBatchUndo(p.subtitles,shifted)' in (src / 'SubtitleScreen.kt').read_text(encoding='utf-8')
print('OK: v0.1.275 sincronia por palavra e ajuste manual')
