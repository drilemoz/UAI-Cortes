#!/usr/bin/env python3
"""Regressão do workflow 100: assertExists() é membro de SemanticsNodeInteraction.

Não importar `androidx.compose.ui.test.assertExists`, pois esse símbolo top-level
não existe no Compose UI Test usado pelo projeto.
"""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
tests = sorted((root/'app/src/androidTest/java').rglob('*.kt'))
assert tests, 'Fontes AndroidTest ausentes'
for test in tests:
    source = test.read_text(encoding='utf-8')
    assert 'import androidx.compose.ui.test.assertExists' not in source, f'Import inválido: {test}'
home = root/'app/src/androidTest/java/br/com/uaicortes/HomeAboutPreviewAndroidTest.kt'
text = home.read_text(encoding='utf-8')
assert '.assertExists()' in text, 'O teste Home/Sobre perdeu a verificação de existência'
assert 'import androidx.compose.ui.test.onNodeWithText' in text
assert 'import androidx.compose.ui.test.junit4.createComposeRule' in text
print('OK: CI100 - sem import top-level assertExists inválido; asserts de UI mantidos')
