#!/usr/bin/env python3
"""v215: grupos sociais e tempo manual. Testes estáticos não substituem build Android."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
src=root/'app/src/main/java/br/com/uaicortes'
ui=(src/'QuickVideoToolsScreen.kt').read_text()
policy=(src/'QuickVideoSplitPolicy.kt').read_text()
engine=(src/'QuickVideoSplitEngine.kt').read_text()
for token in [
    'enum class DurationGroup { SECONDS, MINUTES, CUSTOM }',
    'QuickDurationPreset("9,5 s"', 'QuickDurationPreset("10 s"',
    'QuickDurationPreset("30 s"', 'QuickDurationPreset("60 s"',
    'QuickDurationPreset("1 min"', 'QuickDurationPreset("1 min 05 s"',
    'QuickDurationPreset("2 min"', 'QuickDurationPreset("2 min 59 s"',
    'QuickDurationPreset("3 min"', 'QuickDurationPreset("4 min"',
    'QuickDurationPreset("8 min"',
    'customHours.toIntOrNull()', 'customMinutes.toIntOrNull()',
    'customSeconds.toIntOrNull()',
    'durationGroup == DurationGroup.CUSTOM && customMs == null',
    'val effectivePartDurationMs =',
    'QuickVideoSplitPolicy.parts(sourceDurationMs, effectivePartDurationMs)',
    'parts.isNotEmpty()',
    'QuickVideoSplitPolicy.timeLabel(part.startMs)',
    'QuickVideoSplitPolicy.timeLabel(part.endMs)',
    'options.chunked(2)',
    'maxWidth < 330.dp || LocalDensity.current.fontScale >= 1.3f',
    'QuickDurationGroupChip("Personalizado"',
    'não garante monetização ou alcance',
    'Esta divisão gerará ${parts.size} arquivos de vídeo',
    'splitEngine.start(Uri.parse(sourceText), sourceName, parts, audioMode,',
]: assert token in ui, token
for token in ['MAX_CUSTOM_MS = 86_399_000L', 'MAX_PARTS = 180',
              'partLengthMs % 1_000L == 0L', 'fun customDurationMs(',
              'fun timeLabel(', 'val count = 1L + (durationMs - 1L) / partLengthMs',
              'require(count <= MAX_PARTS)']:
    assert token in policy, token
assert 'GalleryVideoSaver.save(context, output, fileName,' in engine
assert 'onSaved(QuickSplitSavedFile(' in engine
assert 'VideoProject(' not in ui
print('OK: presets sociais, personalizado estrito, preview, acessibilidade e limites preservados')
