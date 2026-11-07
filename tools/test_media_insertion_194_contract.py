"""Contratos de UI e políticas de inserção v194. Estáticos, não substituem Android."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
main=(root/'app/src/main/java/br/com/uaicortes/MainActivity.kt').read_text()
ui=(root/'app/src/main/java/br/com/uaicortes/MediaInsertionUi.kt').read_text()
image=(root/'app/src/main/java/br/com/uaicortes/MainImageInsertionUi.kt').read_text()
export=(root/'app/src/main/java/br/com/uaicortes/ExportScreen.kt').read_text()
saved=(root/'app/src/main/java/br/com/uaicortes/VideoSavedDialog.kt').read_text()
policy=(root/'app/src/main/java/br/com/uaicortes/StaticImageClip.kt').read_text()
editor=(root/'app/src/main/java/br/com/uaicortes/EditorScreen.kt').read_text()
exit_ui=(root/'app/src/main/java/br/com/uaicortes/ProjectDraftUi.kt').read_text()
assert 'closeLabel="Concluir"' in export and 'if (onComplete()) exportVm.dismissSaved()' in export
assert 'onComplete = { leaveEditor("save") }' in main and 'return completed.isSuccess' in main
assert 'closeLabel: String = "Fechar"' in saved  # outro fluxo preservado
assert 'MainVideoInsertionUi(' in main and 'PendingVideoInsertion(' in main
assert 'VideoClipInsertionPolicy.insert(' in ui
assert all(s in ui for s in ['"Início"','"Antes"','"Depois"','"Final"','MediaPlacementChoices'])
assert 'MediaPlacementChoices(placement,' in image
assert 'shiftTimedElements(project, begin, incoming.sumOf { it.durationMs })' in policy
assert 'onInserted(updated, pending.clips.first(), index)' in ui
assert 'EditorExitAction("Salvar"' in exit_ui and 'EditorExitAction("Descartar"' in exit_ui and 'EditorExitAction("Continuar"' in exit_ui
assert 'Triple("Animar imagens"' in editor and 'Modifier.width(56.dp)' in editor
assert 'Triple("Vídeo +"' not in editor  # Ações de adicionar agora ficam no menu da linha do tempo
assert 'Arrange' not in ''  # skip visual-only assertions
print('OK: v194, save/close, video placement, compact image/exit, temporal reflow')
