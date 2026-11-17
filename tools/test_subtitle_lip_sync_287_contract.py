#!/usr/bin/env python3
"""Verify entry, explicit confirmation, media scope, guard rails and commit/undo wiring."""
from pathlib import Path
root = Path(__file__).resolve().parents[1]
app = root / "app/src/main/java/br/com/uaicortes"
screen = (app / "SubtitleScreen.kt").read_text()
dialog = (app / "SubtitleLipSyncDialog.kt").read_text()
policy = (app / "SubtitleLipSyncPolicy.kt").read_text()
frames = (app / "SubtitleLipSyncFrames.kt").read_text()
client = (app / "GeminiSubtitleLipSync.kt").read_text()
assert 'Sincronia labial · Gemini' in screen and 'SubtitleLipSyncDialog(p, candidates' in screen
assert 'onClick = ::analyze' in dialog and 'Aplicar selecionadas' in dialog
assert 'changes.orEmpty().filter { it.before.id in accepted }' in dialog
assert 'SubtitleBatchUndo(p.subtitles, revised)' in screen and 'it.before == cue' in screen
assert 'Comparar quadros' in dialog
assert 'image/jpeg' in client and 'inlineData' in client and 'video/mp4' not in client and 'cue.text' not in client
assert 'MediaMetadataRetriever.OPTION_CLOSEST' in frames and '.writeBytes(' not in frames
assert 'val FRAME_STEP_MS = 100L' in policy and 'val MAX_SHIFT_MS = 300L' in policy
assert 'it <= firstWord' in policy and 'it >= lastWord' in policy
assert 'cue.copy(startMs = start, endMs = end)' in policy and 'wordTimings =' not in policy
print('OK: revisão de quadros consentida, sincronização conservadora, karaokê preservado e Desfazer')
