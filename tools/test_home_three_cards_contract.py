#!/usr/bin/env python3
"""Contrato da faixa rolável com as oito ferramentas da Home, sem simular Android."""
from pathlib import Path
root = Path(__file__).resolve().parents[1]
ui = (root / 'app/src/main/java/br/com/uaicortes/HomeScreens.kt').read_text()
main = (root / 'app/src/main/java/br/com/uaicortes/MainActivity.kt').read_text()
android_test = (root / 'app/src/androidTest/java/br/com/uaicortes/HomeAboutPreviewAndroidTest.kt').read_text()
start = ui.index('@Composable internal fun HomePrimaryActions(')
end = ui.index('@Composable internal fun MainHome(', start)
actions = ui[start:end]
assert actions.count('HomeActionTile(') == 8
assert 'Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())' in actions
assert 'Arrangement.spacedBy(8.dp)' in actions
assert actions.count('Modifier.width(126.dp)') == 8
for label, icon, callback in [
    ('Novo projeto', 'Icons.Default.AddCircleOutline', 'onCreate'),
    ('Animar fotos', 'Icons.Default.AutoAwesomeMotion', 'onAnimate'),
    ('Estúdio de Cenas', 'Icons.Default.MovieFilter', 'onFlow'),
    ('Vozes Google', 'Icons.Default.RecordVoiceOver', 'onVoiceCenter'),
    ('Texto para Voz', 'Icons.Default.GraphicEq', 'onTextToVoice'),
    ('Editar áudio', 'Icons.Default.ContentCut', 'onEditAudio'),
    ('Extrair áudio', 'Icons.Default.AudioFile', 'onQuickAudio'),
    ('Dividir vídeo', 'Icons.Default.ContentCut', 'onQuickSplit'),
]:
    assert f'HomeActionTile("{label}", {icon}, {callback}' in actions
    assert f'"{label}"' in android_test
assert actions.count('enabled = !preview') == 8
assert 'color = Blue' in ui and 'modifier.heightIn(min = 110.dp)' in ui
assert 'HomePrimaryActions(onCreate, onAnimate, onFlow = onFlow,' in ui
assert 'onVoiceCenter = onVoiceCenter, onTextToVoice = onTextToVoice' in ui
assert 'onEditAudio = onEditAudio, onQuickAudio = onQuickAudio, onQuickSplit = onQuickSplit' in ui
assert 'HomeQuickMediaActions(' not in ui  # sem botões duplicados abaixo da faixa
assert 'onQuickSplit = { navTo("quick_split") }' in main
assert 'onQuickAudio = { navTo("quick_audio") }' in main
assert 'onEditAudio = { navTo("edit_audio") }' in main
assert 'onTab("projects")' in ui
assert 'Nenhuma edição ainda. Comece em Novo projeto.' in ui
assert 'Testar visual da Home' not in ui
print('OK: oito cards azuis roláveis, sem atalhos duplicados, navegação original preservada')
