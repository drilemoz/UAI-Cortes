#!/usr/bin/env python3
"""Contrato para favoritos e percentuais honestos de Vozes IA."""
from pathlib import Path
root = Path(__file__).resolve().parents[1]
ui = (root/'app/src/main/java/br/com/uaicortes/VoiceIaScreen.kt').read_text()
fav = (root/'app/src/main/java/br/com/uaicortes/GeminiVoiceFavorites.kt').read_text()
assert 'gemini_voice_favorites' in fav and 'getStringSet("ids"' in fav
assert 'saveNames(names:' in fav and 'names(): Map<String, String>' in fav
assert 'MAX_FAVORITES = 200' in fav
assert 'fun toggle(ids: Set<String>, id: String)' in fav
assert 'VoiceIaShortLabels.voice(it, it.id)' in ui
assert 'displayVoiceName(defaultVoice)' in ui
assert 'label = if (allSameVoice) globalVoiceLabel else "Voz"' in ui
assert 'Text(displayVoiceName(voiceFor(cue))' in ui
assert 'favoritesOnly = !favoritesOnly' in ui
assert 'favoritesStore.save(next)' in ui
assert 'Icon(if (voice.id in favoriteVoiceIds) Icons.Default.Star' in ui
assert 'sortedWith(compareByDescending<GeminiVoice> { it.id in favoriteVoiceIds })' in ui
assert 'VoiceIaBatchProgress.percent(completed, total)' in ui
assert '"$batchPercent% concluído · $completed/$total falas"' in ui
assert 'generatingCueId = cue.id' in ui and 'generatingCueId = null' in ui
assert 'progresso desta chamada indisponível pela API' in ui
assert 'if (generationJob != null && busy)' in ui # ajuste offline não finge percentuais de TTS
assert 'api.synthesize(key, model, voiceId, requestStyle, cue.text)' in ui
assert '.put("schemaVersion", 4)' in (root/'app/src/main/java/br/com/uaicortes/ProjectStore.kt').read_text()
print('OK: favoritos, nomes persistidos e progresso por WAV concluído sem percentuais falsos')
