#!/usr/bin/env python3
"""A seleção em áudio/vídeo deve mostrar cursor e exportar somente faixa sonora."""
from pathlib import Path
r=Path(__file__).resolve().parents[1]
src=r/'app/src/main/java/br/com/uaicortes'
ui=(src/'AudioClipEditorScreen.kt').read_text()
export=(src/'AudioClipExportEngine.kt').read_text()
wave=(src/'AudioClipWaveform.kt').read_text()
policy=(src/'AudioClipSelectionPolicy.kt').read_text()
assert 'val pickAudio = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument())' in ui
assert 'val pickVideo = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument())' in ui
assert 'pickVideo.launch(arrayOf("video/*"))' in ui
assert 'pickAudio.launch(arrayOf("audio/*"))' in ui
assert 'sourceIsVideo = fromVideo' in ui
assert 'setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, true)' in ui
assert 'AudioClipWaveformSelection(waveform, durationMs, startMs, endMs, currentMs,' in ui
assert 'detectTapGestures(onTap' in ui and 'detectDragGestures(onDragStart' in ui
assert 'AudioClipSelectionPolicy.seekFromFraction' in ui
assert 'AudioClipSelectionPolicy.playheadFraction' in ui
assert 'drawLine(playheadColor, Offset(head, 0f)' in ui
assert 'val position = player.currentPosition.coerceAtLeast(0L)' in ui
assert 'currentMs = position.coerceIn(startMs, endMs)' in ui
assert 'if (currentMs in startMs until endMs) currentMs else startMs' in ui
assert 'player.seekTo(target); currentMs = target' in ui
assert '"Este vídeo não contém áudio compatível para edição' in ui
assert 'getString(MediaFormat.KEY_MIME)?.startsWith("audio/")' in wave
assert 'setRemoveVideo(true)' in export
assert 'setOf(C.TRACK_TYPE_AUDIO)' in export
assert 'MimeTypes.AUDIO_AAC' in export
assert 'playheadFraction' in policy and 'seekFromFraction' in policy
assert 'AudioClipSelectionPolicy.seekFromFraction' in (r/'app/src/test/java/br/com/uaicortes/AudioClipSelectionPolicyTest.kt').read_text()
print('OK: editor seleciona audio ou video, exibe cursor real e exporta só AAC/M4A')
