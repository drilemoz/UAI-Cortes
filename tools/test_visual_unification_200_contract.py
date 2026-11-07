#!/usr/bin/env python3
"""Contratos da padronização visual da v0.1.200 (não substituem Gradle nem teste físico)."""
from pathlib import Path
import re

root = Path(__file__).resolve().parents[1]
src = root/'app/src/main/java/br/com/uaicortes'
shapes = (src/'UaiShapes.kt').read_text()
for token in ('val chip = RoundedCornerShape(6.dp)',
              'val button = RoundedCornerShape(8.dp)',
              'val card = RoundedCornerShape(10.dp)',
              'val dialog = RoundedCornerShape(12.dp)',
              'val sheet = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)'):
    assert token in shapes, token
assert 'MaterialTheme(colorScheme = palette, shapes = UaiShapes.theme)' in (src/'MainActivity.kt').read_text()

required = {
    'FlowStudioScreen.kt': ('shape = UaiShapes.button', 20),
    'FlowBatchUi.kt': ('UaiShapes.button', 6),
    'FlowStudioWizardUi.kt': ('UaiShapes.button', 6),
    'FlowPromptScreen.kt': ('UaiShapes.button', 6),
    'VoiceIaScreen.kt': ('UaiShapes.dialog', 1),
    'GeminiVoiceCenterScreen.kt': ('UaiShapes.card', 1),
    'SubtitleSpeakersUi.kt': ('UaiShapes.dialog', 1),
    'SubtitleVisualEditor.kt': ('UaiShapes.dialog', 1),
    'SubtitleWordTimingEditor.kt': ('UaiShapes.dialog', 1),
    'TrimScreen.kt': ('UaiShapes.sheet', 1),
    'GeminiKeyPoolSettings.kt': ('UaiShapes.dialog', 1),
    'GeminiSettingsUi.kt': ('UaiShapes.card', 2),
    'ExportScreen.kt': ('UaiShapes.card', 3),
    'VideoSavedDialog.kt': ('UaiShapes.dialog', 1),
    'HomeScreens.kt': ('UaiShapes.card', 2),
    'AnimateImagesScreen.kt': ('UaiShapes.sheet', 1),
    'EditorScreen.kt': ('UaiShapes.sheet', 1),
    'QuickVideoToolsScreen.kt': ('UaiShapes.button', 3),
}
for file, (expected, count) in required.items():
    text = (src/file).read_text()
    assert text.count(expected) >= count, (file, expected, count)

# Não alterar os elementos com formato propositalmente próprio.
media_browser = (src/'MediaBrowser.kt').read_text()
assert 'shape = RoundedCornerShape(24.dp)' in media_browser  # medalha circular de ordem da seleção
assert 'RoundedCornerShape(3.dp)' in (src/'VoiceIaScreen.kt').read_text()  # barras/segmentos
assert 'shape = UaiShapes.sheet' in (src/'PipScreen.kt').read_text()
assert 'class QuickVideoSplitPolicy' not in (src/'QuickVideoToolsScreen.kt').read_text()
assert (root/'VERSION').read_text().strip() == '0.1.200'
print('OK: escala, 18 módulos, elementos especiais e versão 0.1.200')
