#!/usr/bin/env python3
"""Impedir retorno de ferramentas independentes à barra de edição."""
from pathlib import Path
r = Path(__file__).resolve().parents[1]
p = r / 'app/src/main/java/br/com/uaicortes'
editor = (p / 'EditorScreen.kt').read_text()
main = (p / 'MainActivity.kt').read_text()
home = (p / 'HomeScreens.kt').read_text()
# A barra horizontal do editor não deve oferecer ferramentas que não editam a timeline.
bar = editor.split('val actions = remember { listOf(', 1)[1].split(') }', 1)[0]
assert '"animate"' not in bar and '"flow_prompt"' not in bar
assert '"Animar imagens"' not in bar and '"Prompt Flow"' not in bar
assert all(f'Triple("{label}"' in bar for label in (
    'Cortar', 'Transição', 'Legendas', 'Áudio', "Marca d'água", 'Vozes IA', 'Descrição'))
# Permanecem na página inicial como funcionalidades independentes.
assert 'HomePrimaryActions(onCreate, onAnimate, onFlow = onFlow)' in home
assert '"flow_workspace" -> FlowWorkspaceScreen(::back)' in main
assert '"animate" -> AnimateImagesScreen(' in main
assert 'onFlow = { navTo("flow_workspace") }' in main
# Sem regressão na edição convencional ou nas cópias de mídia.
assert 'UnifiedEditorTimeline(' in editor
assert 'onAddVideo=onAddVideo,onAddImage=onAddImage' in editor
assert 'Icon(Icons.Default.Save, "Salvar projeto e sair")' in editor
print('OK: Animar imagens e Prompt Flow disponíveis só como ferramentas separadas da Home')
