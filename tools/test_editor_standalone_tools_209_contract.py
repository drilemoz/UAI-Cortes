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
    'Cortar', 'Transição', 'Legendas', 'Áudio', 'Vozes IA', 'Camadas'))
# v0.1.279: Marca d'água e PiP ficam em Camadas (abas); o atalho próprio saiu da barra.
assert "Marca d'água" not in bar
# O atalho Descrição foi removido da barra: o texto é editado após salvar o MP4.
assert 'Triple("Descrição", Icons.Default.Description, "publication")' not in bar
# A Home mantém apenas os atalhos ativos.
assert 'HomePrimaryActions(onCreate,' in home
# Sem regressão na edição convencional ou nas cópias de mídia.
assert 'UnifiedEditorTimeline(' in editor
assert 'onAddVideo=onAddVideo,onAddImage=onAddImage' in editor
assert 'Icon(Icons.Default.Save, "Salvar projeto e sair")' in editor
assert '"animate"' not in main and '"flow_workspace"' not in main and '"flow_prompt"' not in main
print('OK: editor e atalhos ativos preservados sem os módulos removidos')
