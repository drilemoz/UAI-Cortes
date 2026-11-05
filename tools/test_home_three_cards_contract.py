#!/usr/bin/env python3
"""Contrato de interface Home 0.1.129. Não substitui teste Android/Compose."""
from pathlib import Path
root = Path(__file__).resolve().parents[1]
ui = (root / 'app/src/main/java/br/com/uaicortes/HomeScreens.kt').read_text()
test = (root / 'app/src/androidTest/java/br/com/uaicortes/HomeAboutPreviewAndroidTest.kt').read_text()
start = ui.index('@Composable internal fun HomePrimaryActions(')
end = ui.index('@Composable internal fun MainHome(', start)
actions = ui[start:end]
assert actions.count('HomeActionTile(') == 3
assert 'Row(Modifier.fillMaxWidth()' in actions
assert 'Arrangement.spacedBy(8.dp)' in actions
for label, icon, callback in [
    ('Novo projeto', 'Icons.Default.AddCircleOutline', 'onCreate'),
    ('Animar fotos', 'Icons.Default.AutoAwesomeMotion', 'onAnimate'),
    ('Estúdio de Cenas', 'Icons.Default.MovieFilter', 'onFlow'),
]:
    assert f'HomeActionTile("{label}", {icon}, {callback}' in actions
    assert f'onNodeWithText("{label}").performClick()' in test
    assert f'onNodeWithText("{label}").assertIsNotEnabled()' in test
assert actions.count('enabled = !preview') == 3
assert 'modifier = Modifier.weight(1f)' in actions
assert 'color = Blue' in ui and 'modifier.heightIn(min = 110.dp)' in ui
assert 'HomePrimaryActions(onCreate, onAnimate, onFlow = onFlow)' in ui
assert 'HomePrimaryActions(onCreate = {}, onAnimate = {}, preview = true)' in ui
assert 'onTab("projects")' in ui
assert 'Nenhuma edição ainda. Comece em Novo projeto.' in ui
assert 'Nova edição' not in actions
print('OK: três cards azuis e iguais, navegação preservada e prévia Sobre desabilitada')
