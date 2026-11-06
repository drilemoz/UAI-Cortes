"""Contrato de recuperação segura de fontes sem alterar o schemaVersion 4."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
base=root/'app/src/main/java/br/com/uaicortes'
policy=(base/'ProjectMediaRelink.kt').read_text()
verifier=(base/'MediaRelinkVerification.kt').read_text()
screen=(base/'ProjectMediaRepairScreen.kt').read_text()
activity=(base/'MainActivity.kt').read_text()
editor=(base/'EditorScreen.kt').read_text()
assert 'project.clips.map' in policy and 'project.overlays.map' in policy
assert 'project.audioTracks.map' in policy and 'project.music?.copy' in policy
assert 'it.originalUri == target.originalUri' in policy
assert 'length >= target.minimumSourceEndMs' in verifier
assert 'takePersistableUriPermission' in verifier
assert 'ImageClipImport.copyIntoProject' in verifier
assert 'ActivityResultContracts.OpenDocument' in screen
assert 'ProjectMediaAvailability.canOpen' in screen
assert 'onRepairMedia = { navTo("media_repair") }' in activity
assert 'projectSession.project != p' in activity
assert '"media_repair"' in (base/'EditorNavigation.kt').read_text()
assert 'onRepairMedia:()->Unit' in editor
assert '"schemaVersion", 4' in (base/'ProjectStore.kt').read_text()
print('OK: recuperação não destrutiva, validação de duração, SAF e rota do editor')
