#!/usr/bin/env python3
"""Regressão da UI de Vozes IA: organização compacta e tempos preservados.

Verificação de contrato do código-fonte; build Compose/Android continua necessário.
"""
from pathlib import Path
root = Path(__file__).resolve().parents[1]
ui = (root/'app/src/main/java/br/com/uaicortes/VoiceIaScreen.kt').read_text()
preview = ui[ui.index('private fun VoiceIaVideoPreview('):ui.index('private fun VoiceIaCueTimeline(')]
assert 'InlineVideoPreview(aspect, maxHeightDp = if (previewExpanded) null else 210f)' in preview
assert 'height(264.dp)' not in preview
assert 'SubtitleOverlayOnFrame(selected, editable = false' in preview
assert 'formatPipMs(cue.startMs)' in ui and 'formatPipMs(cue.endMs)' in ui
assert 'formatMs(cue.startMs)' not in ui
assert ui.index('VoiceIaToolbarAction(label = "Central"') < ui.index('if (status.isNotBlank())')
assert 'VoiceIaToolbarAction(label = "Tom"' in ui and 'label = "Gerar $pending"' in ui
assert 'Text("Voz para todas"' not in ui
assert 'pendingVoiceForAll' in ui and 'pendingStyleForAll' in ui
assert 'voiceOverrides.clear()' in ui and 'styleOverrides.clear()' in ui
assert 'VoiceIaDisplayPolicy.overrunMs(' in ui
assert 'maxLines = if (active) 3 else 1' in ui
assert 'shape = RoundedCornerShape(8.dp)' in ui
assert 'api.synthesize(key, usedModel, voiceId, requestStyle, cue.text)' in ui
assert 'editable = false' in ui
policy = (root/'app/src/main/java/br/com/uaicortes/VoiceIaDisplayPolicy.kt').read_text()
assert 'overrunMs(' in policy and 'fun seconds(' in policy
model=(root/'app/src/main/java/br/com/uaicortes/ProjectStore.kt').read_text()
assert '.put("schemaVersion", 4)' in model
print('OK: Voz geral, estilo em lote, cartões compactos, prévia adaptativa, décimos e aviso de duração')
