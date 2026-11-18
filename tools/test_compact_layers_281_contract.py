#!/usr/bin/env python3
"""Checagens de integração. Pixels precisam do teste Android."""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
src = root / 'app/src/main/java/br/com/uaicortes'
read = lambda name: (src / name).read_text(encoding='utf-8')
pip = read('PipScreen.kt')
main, panel = pip.split('    if (panelVisible && pip != null)', 1)
assert 'FilterChip(selected = pip?.id == item.id' in main
assert 'OverlayPlacement.MAX_PIPS' in main and 'Mais opções do PiP selecionado' in main
assert 'Trocar mídia' in main and 'Som do PiP' in main
assert 'Opacidade do PiP' not in main and 'Opacidade do PiP' in panel
assert 'LayerOptionRow(label, value, expanded == key)' in panel
assert 'if (expanded == key)' in panel and 'parsePercentage(manual)' in panel
assert 'PipTimePresets.clipPeriods' in panel and 'RangeSlider(' in panel
assert 'OverlayEditorLogic.quickPreset' in panel
assert 'pointerInput(pip.uri, canvasW, canvasH)' in read('EditorTimelineUi.kt')
mark = read('TextWatermarkScreen.kt')
assert 'LayerPreviewFrame(aspect, Modifier.weight(1f)' in mark and '200f' not in mark
assert 'Posição da prévia' in mark and 'player.play()' in mark
assert 'frameMs >= it.startMs && frameMs < it.endMs' in mark
assert 'Projeto inteiro' in mark and 'draft.copy(clipId = item.id)' in mark
assert 'draft.copy(clipId = null)' in mark
style = read('WatermarkStylePanel.kt')
assert 'watermarkPalette.chunked(5)' in style and 'WatermarkColor.parse(input)' in style
assert 'Text(sample, fontFamily = subtitleFont(family, bold = true)' in style
assert 'value.copy(clipId = null)' in read('WatermarkDefaultsStore.kt')
model = read('TextWatermark.kt')
assert '.put("clipId", w.clipId)' in model
assert 'if (clip.id == target) return cueFor(offset, end)' in model
assert 'takeIf { hasWatermark }' in read('FrameSnapshotProject.kt')
exporter = read('ExportEngine.kt')
assert 'val watermark = project.textWatermark?.toSubtitle(project.clips)' in exporter
assert 'textLayers += TimedSubtitleOverlay(listOf(watermarkCue))' in exporter
assert 'outputLayers += TimedSubtitleOverlay(listOf(watermarkCue))' in exporter
assert (root / 'app/src/androidTest/java/br/com/uaicortes/WatermarkTimingAndroidTest.kt').exists()
print('OK: PiP compacto, paleta e fontes da marca, vínculo de clipe, prévia e exportação')
