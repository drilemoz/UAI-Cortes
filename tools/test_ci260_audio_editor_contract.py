#!/usr/bin/env python3
"""Editor de áudio independente, sem redes, com seleção e M4A via SAF."""
from pathlib import Path
r = Path(__file__).resolve().parents[1]
src = r / 'app/src/main/java/br/com/uaicortes'
home = (src/'HomeScreens.kt').read_text()
main = (src/'MainActivity.kt').read_text()
nav = (src/'EditorNavigation.kt').read_text()
ui = (src/'AudioClipEditorScreen.kt').read_text()
engine = (src/'AudioClipExportEngine.kt').read_text()
wave = (src/'AudioClipWaveform.kt').read_text()
policy = (src/'AudioClipSelectionPolicy.kt').read_text()
assert 'HomeActionTile("Editar áudio", Icons.Default.ContentCut, onEditAudio' in home
assert 'onEditAudio = { navTo("edit_audio") }' in main
assert '"edit_audio" -> AudioClipEditorScreen(::back)' in main
assert '"edit_audio"' in nav
assert 'ActivityResultContracts.OpenDocument()' in ui
assert 'ActivityResultContracts.CreateDocument("audio/mp4")' in ui
assert 'AudioClipWaveformSelection(' in ui and 'detectDragGestures(' in ui
assert 'AudioClipSelectionPolicy.valid(durationMs' in ui
assert 'player.seekTo(startMs)' in ui and 'position >= endMs' in ui
assert 'repeatSelection' in ui and 'BlueButton("Exportar trecho M4A"' in ui
assert 'withContext(Dispatchers.IO)' in wave and 'MediaCodec.createDecoderByType' in wave
assert 'WAVEFORM_BARS = 160' in policy
assert 'setStartPositionMs(startMs)' in engine and 'setEndPositionMs(endMs)' in engine
assert 'setRemoveVideo(true)' in engine and 'MimeTypes.AUDIO_AAC' in engine
assert 'openOutputStream(destination, "w")' in engine and 'removeIncomplete' in engine
assert 'DiagnosticRecorder.action("audio_editor", "export_success")' in ui
assert 'KEYSTORE' not in ui and 'http://' not in ui and 'https://' not in ui
assert (r/'app/src/test/java/br/com/uaicortes/AudioClipSelectionPolicyTest.kt').exists()
print('OK: editor de áudio com onda em streaming, cortes com prévia, M4A e integração Home')
