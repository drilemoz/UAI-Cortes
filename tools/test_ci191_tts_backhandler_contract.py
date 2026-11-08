#!/usr/bin/env python3
"""CI191: evita import incorreto de BackHandler fora do namespace Compose.

O teste estático detecta a falha do workflow 191, mas não substitui o Gradle.
"""
from pathlib import Path
root = Path(__file__).resolve().parents[1]
screen = root / 'app/src/main/java/br/com/uaicortes/StandaloneTextToVoiceScreen.kt'
source = screen.read_text(encoding='utf-8')
assert 'import androidx.activity.compose.BackHandler' in source, 'CI191: import Compose BackHandler ausente'
assert 'import androidx.activity.BackHandler' not in source, 'CI191: import BackHandler inválido voltou'
assert source.count('BackHandler(enabled =') == 2, 'CI191: handlers de retorno foram alterados'
for file in (root / 'app/src/main/java').rglob('*.kt'):
    text = file.read_text(encoding='utf-8')
    assert 'import androidx.activity.BackHandler' not in text, f'CI191: import inválido em {file}'
print('OK: CI191, BackHandler do pacote Compose e handlers de retorno preservados')
