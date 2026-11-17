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
assert 'addMenu' not in timeline and 'openTrack(i)' in timeline
assert '"insert_video" -> onAddVideo()' in editor and '"insert_photo" -> onAddImage()' in editor
assert all(route in timeline for route in (
    'TimelineKind.MAIN_VIDEO -> onAddVideo()', 'TimelineKind.MAIN_IMAGE -> onAddImage()',
    'TimelineKind.PIP -> onOpenPiP()',
))
print('OK: atalhos exclusivos do editor, salvar fixo, atalhos de inserção e rotas legadas preservados')
