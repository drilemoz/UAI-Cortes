#!/usr/bin/env python3
"""Favoritos reutilizados na Central, progresso de TTS permanece honesto."""
from pathlib import Path
src=Path(__file__).resolve().parents[1]/'app/src/main/java/br/com/uaicortes'
ui=(src/'VoiceIaScreen.kt').read_text()
center=(src/'GeminiVoiceCenterScreen.kt').read_text()
fav=(src/'GeminiVoiceFavorites.kt').read_text()
assert 'gemini_voice_favorites' in fav and 'getStringSet("ids"' in fav
assert 'saveNames(names:' in fav and 'names(): Map<String, String>' in fav
assert 'MAX_FAVORITES = 200' in fav and 'fun toggle(ids: Set<String>, id: String)' in fav
assert 'GeminiVoiceFavorites.toggle(favoriteIds, voice.id)' in center
assert 'favorites.save(next)' in center
assert 'GeminiVoicePresentation.label(voice)' in center
assert 'displayVoiceName(voiceFor(cue))' in ui
assert 'VoiceIaBatchProgress.percent(completed, total)' in ui
assert '"Gerando $completed/$total ($batchPercent%)"' in ui
assert 'generatingCueId = cue.id' in ui and 'generatingCueId = null' in ui
assert 'CircularProgressIndicator(Modifier.size(15.dp)' in ui
assert 'if (generationJob != null && busy)' in ui
assert 'api.synthesize(key, usedModel, voiceId, requestStyle, cue.text)' in ui
assert '.put("schemaVersion", 4)' in (src/'ProjectStore.kt').read_text()
print('OK: favoritos da Central e progresso por WAV concluído sem porcentagens fictícias')
