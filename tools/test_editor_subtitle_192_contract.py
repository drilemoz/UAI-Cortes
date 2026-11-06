"""Contracts for the finite editor bar, character appearance and scroll stability.
These inspect source and do not substitute Android runtime tests.
"""
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
SRC=ROOT/'app/src/main/java/br/com/uaicortes'
sub=(SRC/'SubtitleScreen.kt').read_text()
policy=(SRC/'SubtitleSpeakerStyles.kt').read_text()
model=(SRC/'ProjectStore.kt').read_text()
editor=(SRC/'EditorScreen.kt').read_text()
main=(SRC/'MainActivity.kt').read_text()
assert 'items(count = actions.size)' in editor and 'items(count = Int.MAX_VALUE)' not in editor
assert 'p.clips.map { Triple(it.id, it.isImage, it.durationMs) }' in editor
assert 'p.clips.map { Triple(it.id, it.isImage, it.durationMs) }' in sub
assert 'LaunchedEffect(selectedId, searchQuery, speakerFilter)' in sub
assert 'LaunchedEffect(selectedId,searchQuery,filtered)' not in sub
assert 'LaunchedEffect(activeCue?.id, followPlayback)' in sub
assert 'followPlayback=false' in sub
assert 'subtitleSpeakerStyles' in model and 'subtitleStyleOverrides' in model
assert 'SubtitleSpeakerStyles.apply(p,id,draft,options)' in sub
assert 'SubtitleSpeakerStyles.manualEdit(p, selected, draft)' in sub
assert 'onRestoreSpeakerStyle' in sub
assert 'cue.id in project.subtitleStyleOverrides' in policy
assert 'VoiceIaSubtitleGate.hasCues(current)' in main
assert 'VoiceIaSubtitleGateScreen' in main
assert (SRC/'VoiceIaSubtitleGateScreen.kt').exists()
print('OK: finite toolbar, stable subtitle scroll, persistent speaker styles and voice gate')
