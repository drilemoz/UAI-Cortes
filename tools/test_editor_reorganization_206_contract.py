#!/usr/bin/env python3
"""Acessos fixos e menu de mídias sem ações redundantes."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
editor=(root/'app/src/main/java/br/com/uaicortes/EditorScreen.kt').read_text()
nav=(root/'app/src/main/java/br/com/uaicortes/EditorNavigation.kt').read_text()
timeline=(root/'app/src/main/java/br/com/uaicortes/UnifiedEditorTimeline.kt').read_text()
assert 'Icon(Icons.Default.Save, "Salvar projeto e sair")' in editor
assert 'Reparar arquivos de mídia' in editor and 'Icons.Default.MoreVert' in editor
assert editor.index('Triple("Cortar"') < editor.index('Triple("Transição"')
assert 'Triple("Animar imagens"' not in editor
assert 'Triple("Prompt Flow"' not in editor
assert all(item not in editor for item in (
    'Triple("Salvar",', 'Triple("Vídeo +",', 'Triple("Imagem",', 'Triple("PiP",',
))
assert 'when(index) {0->onAddVideo();1->onAddImage();2->onOpenPiP();' in timeline
assert '"animate" -> if (animationDestination == "append") "editor" else "home"' in nav
print('OK: atalhos exclusivos do editor, salvar fixo, menu Adicionar e rotas legadas preservados')
