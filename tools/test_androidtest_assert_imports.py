#!/usr/bin/env python3
"""Regressão do CI154: métodos de SemanticsNodeInteraction não são imports de topo."""
from pathlib import Path
import re

root = Path(__file__).resolve().parents[1]
tests = sorted((root / 'app/src/androidTest').rglob('*.kt'))
assert tests, 'Nenhum teste Android encontrado'
for path in tests:
    source = path.read_text(encoding='utf-8')
    for member in ('assertDoesNotExist', 'assertExists'):
        assert not re.search(r'^\s*import\s+androidx\.compose\.ui\.test\.'+member+r'\s*$', source, re.M), f'{path}: import indevido de {member}'

home = root/'app/src/androidTest/java/br/com/uaicortes/HomeAboutPreviewAndroidTest.kt'
source = home.read_text(encoding='utf-8')
assert 'rule.onNodeWithText("Ver novidades").assertDoesNotExist()' in source
assert 'rule.onNodeWithText("Testar visual da Home").assertDoesNotExist()' in source
assert 'rule.onNodeWithText(version).assertExists()' in source
print(f'OK: CI154, {len(tests)} AndroidTests sem imports inválidos e asserções Home/Sobre preservadas')
