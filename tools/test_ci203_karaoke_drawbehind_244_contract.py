#!/usr/bin/env python3
"""Regride CI 203: origem correta do modificador Compose drawBehind."""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
src = (root / 'app/src/main/java/br/com/uaicortes/SubtitleKaraokeBadge.kt').read_text(encoding='utf-8')
assert 'import androidx.compose.ui.draw.drawBehind' in src, 'drawBehind precisa vir de androidx.compose.ui.draw'
assert 'import androidx.compose.foundation.drawBehind' not in src, 'import inválido causou falha CI 203'
assert 'return drawBehind {' in src and 'drawRoundRect(' in src
preview = (root / 'app/src/main/java/br/com/uaicortes/SubtitleScreen.kt').read_text(encoding='utf-8')
gallery = (root / 'app/src/main/java/br/com/uaicortes/SubtitleStyleGallery.kt').read_text(encoding='utf-8')
assert '.subtitleKaraokeBadge(' in preview and '.subtitleKaraokeBadge(' in gallery
print('OK: CI 203 — import Compose UI correto e tarja rosa integrada em prévia/galeria')
