#!/usr/bin/env python3
from pathlib import Path
root = Path(__file__).resolve().parents[1]
src = root / "app/src/main/java/br/com/uaicortes"
read = lambda name: (src / (name + ".kt")).read_text()
client, auto = read("GeminiSubtitleClient"), read("AutoSubtitleScreen")
assert "preparedInput ?: try" in client and "prepared.also { preparedInput = it }" in client
assert "GeminiKeyFailover" not in client
assert 'post("models/$model:generateContent", body, valid)' in client and 'post("interactions", body, valid)' in client
assert 'remote?.let { runCatching { deleteUploaded(it,valid) } }' in client
assert 'geminiCheckpoint.saveSubtitles(mediaFingerprint, usedModel,' in auto
assert 'saved.model == GeminiLiteTranscription.MODEL' in auto
assert "align && usedLite -> original" in auto
assert "cue.wordTimings.isEmpty()" in auto and "styled.copy(animation = SubtitleAnimations.NONE)" in auto
print("OK: fallback leve, uma chave e um upload, horários aproximados com revisão")
