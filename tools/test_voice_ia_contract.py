#!/usr/bin/env python3
"""Contrato estático v139; não substitui compilação Gradle ou teste Gemini real."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
src=root/'app/src/main/java/br/com/uaicortes'
editor=(src/'EditorScreen.kt').read_text()
assert 'Triple("Vozes IA", Icons.Default.RecordVoiceOver, "voices")' in editor
assert 'resolved.filter { it.kind==VoiceIaPolicy.KIND }' in (src/'UnifiedEditorTimeline.kt').read_text()
assert 'UnifiedEditorTimeline(p,currentMs' in editor and 'onOpenVoice={onEdit("voices")}' in editor
nav=(src/'EditorNavigation.kt').read_text()
assert '"voices"' in nav and '"gemini_settings" -> if (previous == "audio" || previous == "voices" || previous == "api_center")' in nav
screen=(src/'VoiceIaScreen.kt').read_text()
assert 'project.subtitles.filter' in screen and 'editable = false' in screen
assert 'GeminiVoiceCenterScreen(' in screen
assert 'GeminiVoiceLibrary(context)' in screen
assert 'api.synthesize(key, usedModel, voiceId, requestStyle, cue.text)' in screen
assert 'client.voicesWithDiagnosis(key)' in (src/'GeminiVoiceCenterScreen.kt').read_text()
assert 'promptGenerate' in screen and 'GeminiUsageHint' in screen
assert 'chunked(SubtitleNarrationTimeline.MAX_SEGMENTS)' in screen
assert 'VoiceIaPolicy.merge(current, cue, take)' in screen
assert 'onSave(changed)' in screen and 'onSave(current.copy(audioTracks' in screen
store=(src/'VoiceIaDraftStore.kt').read_text()
assert 'AtomicFile' in store and 'sha256' in store and 'audio.parentFile != audioDir' in store
model=(src/'ProjectStore.kt').read_text()
assert 'val kind: String = "audio"' in model and '.put("kind", kind)' in model
assert '.put("schemaVersion", 4)' in model
print('OK: Vozes IA + atalho, navegação, WAVs, geração com consentimento e timeline separada.')
