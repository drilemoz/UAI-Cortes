#!/usr/bin/env python3
"""Proteções estruturais das legendas por personagem (sem prometer build Android)."""
from pathlib import Path
r=Path(__file__).resolve().parents[1]
p=(r/'app/src/main/java/br/com/uaicortes/ProjectStore.kt').read_text()
s=(r/'app/src/main/java/br/com/uaicortes/SubtitleScreen.kt').read_text()
t=(r/'app/src/main/java/br/com/uaicortes/EditorTimelineUi.kt').read_text()
policy=(r/'app/src/main/java/br/com/uaicortes/SubtitleSpeakerPolicy.kt').read_text()
manager=(r/'app/src/main/java/br/com/uaicortes/SubtitleSpeakersUi.kt').read_text()
assert 'val subtitleSpeakers: List<SubtitleSpeaker> = emptyList()' in p
assert 'val speakerId: String? = null' in p
assert '.put("speakerId", speakerId)' in p and '.put("subtitleSpeakers", JSONArray()' in p
assert '.put("schemaVersion", 4)' in p
for marker in ['fun add(', 'fun assign(', 'fun rename(', 'fun remove(', 'fun applyColors(', 'fun filter(']:
    assert marker in policy
for marker in ['SubtitleSpeakerPicker(', 'SubtitleSpeakerManager(', 'Aplicar cores', 'Não identificado']:
    assert marker in manager
for marker in ['val activeCue=SubtitlePreviewPolicy.firstAt', 'if (compactPreview)',
               'compactPreview = compactPreview', 'windowMs = zoomWindowMs',
               'speakerFilter', 'followPlayback', 'onToggleFollow', 'moreSubtitleTools']:
    assert marker in s
assert 'speakers.firstOrNull { it.id == cue.speakerId }' in t
assert 'segmentMs' in (r/'app/src/main/java/br/com/uaicortes/SubtitleCompactLayout.kt').read_text() or 'windowMs' in (r/'app/src/main/java/br/com/uaicortes/SubtitleCompactLayout.kt').read_text()
assert (r/'app/src/test/java/br/com/uaicortes/SubtitleSpeakerPolicyTest.kt').exists()
print('OK: personagens locais, atribuição manual, filtro, cor opt-in, prévia e zoom')
