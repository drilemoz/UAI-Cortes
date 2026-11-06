#!/usr/bin/env python3
"""Novos contratos estáticos de navegação/atribuição, não substituem build Android."""
from pathlib import Path
root = Path(__file__).resolve().parents[1]
src = root / 'app/src/main/java/br/com/uaicortes'
model = (src/'ProjectStore.kt').read_text()
ui = (src/'VoiceIaScreen.kt').read_text()
policy = (src/'VoiceIaVoiceAssignments.kt').read_text()
queue = (src/'VoiceIaQueuePolicy.kt').read_text()
auto = (src/'AutoSubtitleScreen.kt').read_text()
style = (src/'SubtitleAppearancePolicy.kt').read_text()
assert 'voiceIaVoices: VoiceIaVoiceAssignments' in model
assert '.put("voiceIaVoices", voiceIaVoices.json())' in model
assert 'VoiceIaVoiceAssignments.read(j.optJSONObject("voiceIaVoices"))' in model
assert 'schemaVersion", 4' in model
assert 'cueVoices[cue.id]' in policy and 'speakerVoices[it]' in policy
assert 'project.subtitleSpeakers.none { it.id == id }' in policy
assert 'speakerVoiceOverrides[speaker.id]' in ui
assert 'voicePickerSpeakerId = speaker.id' in ui
assert 'onSave(changed)' in ui
assert 'SubtitleSpeakerPolicy.filter(cues, speakerFilter' in ui
assert 'itemsIndexed(filteredCues' in ui
assert 'VoiceIaCueTimeline(cues,' in ui
assert 'val pending = batchPlan.toGenerate' in ui
assert 'pendingSpeakerVoice?.let' in ui
assert 'VoiceIaQueuePolicy.reconcileSelections' in ui
assert 'current != item.voiceId' in queue
assert 'FilterChip(selected = appearanceMode == SubtitleAppearanceMode.CLASSIC_SRT' in auto
assert 'SubtitleAppearancePolicy.generated(it, appearanceMode, generatedSize)' in auto
assert 'wordTimings' not in style  # copy preserva marcações sem reconstruir
assert 'CLASSIC_SRT' in style and 'KARAOKE_IA' in style
print('OK: voz por personagem e legenda, filtro, fila segura e estilo SRT/IA')
