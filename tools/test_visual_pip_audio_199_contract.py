#!/usr/bin/env python3
"""Contrato local v0.1.199. Complementa, não substitui, compilação/ensaio Android."""
from pathlib import Path

root=Path(__file__).resolve().parents[1]
src=root/'app/src/main/java/br/com/uaicortes'
shapes=(src/'UaiShapes.kt').read_text()
main=(src/'MainActivity.kt').read_text()
pip=(src/'PipScreen.kt').read_text()
audio=(src/'AudioScreen.kt').read_text()
components=(src/'AudioSectionUi.kt').read_text()
exit_ui=(src/'ProjectDraftUi.kt').read_text()
home=(src/'HomeScreens.kt').read_text()

assert 'val button = RoundedCornerShape(8.dp)' in shapes
assert 'val card = RoundedCornerShape(10.dp)' in shapes
assert 'val dialog = RoundedCornerShape(12.dp)' in shapes
assert 'MaterialTheme(colorScheme = palette, shapes = UaiShapes.theme)' in main
assert 'UaiShapes.button' in exit_ui and 'height(64.dp)' in exit_ui
assert 'EditorExitAction("Descartar"' in exit_ui and 'onDecision("save")' in exit_ui
assert 'onDecision("discard")' in exit_ui and 'onContinue()' in exit_ui
assert 'InlineVideoPreview(canvasAspect, maxHeightDp = if (previewExpanded) null else 250f)' in pip
assert 'Modifier.weight(1f).height(58.dp)' in pip
assert 'shape = UaiShapes.sheet' in pip
assert 'PipLayer(' in pip and 'onUpdate = onChange' in pip
assert 'onChange(pip.copy(opacity=value))' in pip and 'onChange(pip.copy(muted = !it))' in pip
assert 'previewIsolated(track.id, track.uri,' in audio
assert 'previewIsolated("music", music.uri,' in audio
assert 'track.sourceStartMs, track.durationMs, track.volume' in audio
assert 'isolatedPlayer.pause(); isolatedPlayer.release()' in audio
assert 'mixPlayer.pause(); mixPlaying = false' in audio
assert 'previewPlayer.pause(); previewPlaying = false' in audio
assert 'trackPlayer.pause(); trackPlaying = false' in audio
assert 'onSaveAndReturn(p)' in audio
assert 'var extractionExpanded by remember(p.id) { mutableStateOf(false) }' in audio
assert 'AudioPanelCard' in audio and 'UaiShapes.card' in components
assert 'UaiShapes.card' in home
print('OK: design tokens, PiP compacto com edição intacta, pré-escuta isolada e salvamento preservados')
