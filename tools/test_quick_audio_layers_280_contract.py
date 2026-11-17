#!/usr/bin/env python3
"""Contratos de integração da v0.1.280. Geometria/tempo também têm JUnit; pixels exigem Android."""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
src = root / 'app/src/main/java/br/com/uaicortes'
read = lambda name: (src / name).read_text(encoding='utf-8')
editor = read('EditorScreen.kt')
quick = read('QuickAudioDialog.kt')
probe, apply = quick.split('        confirmButton = {', 1)
assert 'if (key == "audio") openQuickAudio("")' in editor
assert 'onOpenAudio={openQuickAudio("")}' in editor
assert 'track?.clipId != null || track?.kind == VoiceIaPolicy.KIND' in editor
assert 'OpenDocument()' in probe and 'media.duration(uri)' in probe
assert 'protectedCopy(' not in probe and 'AudioTrack(savedUri' not in probe
assert 'media.protectedCopy(' in apply and 'current.audioTracks + updated' in apply
assert 'window.omittedMs > 0L' in quick and 'quickAudioCoverage(window, project.clips)' in quick
assert 'LayerTool(label, icon' in read('TextWatermarkScreen.kt') and 'LayerTool(label, icon' in read('PipScreen.kt')
assert 'OverlayEditorLogic.quickPreset(row, col, wf, hf, canvasAspect)' in read('PipScreen.kt')
assert 'OverlayEditorLogic.quickPreset' not in read('OverlayPlacement.kt')
assert 'OverlayEditorLogic.clampCenter' in read('ExportEngine.kt')
draft = read('ProjectDraftUi.kt').split('internal fun ConfirmRecoverDraft', 1)[1].split('internal fun InvalidEditorDraft', 1)[0]
actions = draft.split('confirmButton =', 1)[1]
assert 'Row(Modifier.fillMaxWidth()' in actions
assert actions.index('onClick = onDiscard') < actions.index('onClick = onRecover')
assert 'Falas que precisam de ajuste nas legendas' in read('VoiceIaScreen.kt')
tests = root / 'app/src/androidTest'
assert (tests / 'assets/export_pip_detail_280.mp4').stat().st_size > 2000
pip_test = (tests / 'java/br/com/uaicortes/PipExportQualityAndroidTest.kt').read_text()
assert 'verify(720, 1280)' in pip_test and 'verify(1080, 1920)' in pip_test and 'getPixel(' in pip_test
print('OK: áudio contínuo, aplicação explícita, Camadas, recuperação e cobertura de pixels Android')
