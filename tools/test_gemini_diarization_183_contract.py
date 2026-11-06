#!/usr/bin/env python3
"""Validação de contrato da diarização Gemini (não substitui API/Android reais)."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
src=root/'app/src/main/java/br/com/uaicortes'
client=(src/'GeminiSubtitleClient.kt').read_text()
parser=(src/'GeminiSubtitleParser.kt').read_text()
policy=(src/'GeminiDiarizationPolicy.kt').read_text()
auto=(src/'AutoSubtitleScreen.kt').read_text()
main=(src/'MainActivity.kt').read_text()
assert 'put("diarization_mode", "speaker")' in client
assert 'timestamp_granularities' in client and 'gemini-3.5-transcribe' in client
assert 'interaction(uri,valid,withSpeakerLabels)' in client
assert 'item.optString("speaker")' in parser and 'word.speakerId != buffer.last().speakerId' in parser
assert 'UUID.randomUUID()' in parser and 'provisionalId(sessionId, raw)' in parser
assert 'spk_[1-8]' in policy and 'SubtitleSpeakerPolicy.MAX_SPEAKERS' in policy
assert 'fun prepare(' in policy and 'project.copy(subtitles' in policy
assert 'GeminiVoiceReviewUi(results' in auto and 'Sugerir Voz' in auto
assert 'GeminiDiarizationPolicy.prepare(project' in auto and 'else onApply(proposal)' in auto
assert 'geminiClient.transcribe(context,project,apiKey,detectSpeakers)' in auto
assert 'onApply = { reviewed ->' in main and 'write(reviewed)' in main
assert (root/'app/src/test/java/br/com/uaicortes/GeminiDiarizationPolicyTest.kt').exists()
assert 'GeminiTaskCheckpointStore' in auto and 'geminiCheckpoint.saveSubtitles' in auto
assert '.put("speakerId", speakerId)' in (src/'ProjectStore.kt').read_text()
assert (src/'GeminiVoiceReviewUi.kt').exists()
print('OK: diarização por mesma chamada, rótulos provisórios, revisão, checkpoint, compatibilidade')
