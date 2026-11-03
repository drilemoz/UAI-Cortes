#!/usr/bin/env python3
"""Checagem estrutural v0.1.114: A/B, meta aproximada e retry de somente uma fala."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
base=root/'app/src/main/java/br/com/uaicortes'
ui=(base/'GeminiNarrationUi.kt').read_text()
comparison=(base/'GeminiNarrationComparisonUi.kt').read_text()
assist=(base/'NarrationDurationAssist.kt').read_text()
assert 'GeminiNarrationComparisonDialog(latestProject' in ui
assert 'compareCueId == null' in ui
assert '"Comparar A/B"' in ui and '"Original"' in comparison and '"Gemini"' in comparison
assert 'seekProjectPosition(' in comparison and 'projectPosition(' in comparison
assert 'player.pause(); onDismiss()' in comparison and 'DisposableEffect(player)' in comparison
assert '"Refazer fala"' in ui and '"regenerate_one"' in ui
assert 'GeminiAudioFiles.save(ctx, wave)' in ui
assert 'timedFiles = timedFiles.filterNot { it.segment.cueId == segment.cueId } +' in ui
assert ui.index('GeminiAudioFiles.save(ctx, wave)',ui.index('if (action == "regenerate_one")')) < ui.index('timedFiles = timedFiles.filterNot { it.segment.cueId == segment.cueId } +')
assert 'NarrationDurationAssist.instruction(target)' in ui
assert '"Diferença da meta' in ui
assert 'A meta só orienta a próxima síntese; o WAV atual não é modificado.' in ui
assert 'Nunca omita palavras' in assist and 'coerceIn' in assist
assert '"Continuar"' in ui and 'pode consumir cota' in ui
assert (root/'app/src/test/java/br/com/uaicortes/NarrationDurationAssistTest.kt').exists()
assert (root/'docs/VALIDATION_0.1.114.md').exists()
print('OK: v0.1.114 comparação A/B, duração-alvo não destrutiva e regeneração individual confirmada')
