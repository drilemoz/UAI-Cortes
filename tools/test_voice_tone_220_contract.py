#!/usr/bin/env python3
"""Contratos v220: o teste de compilação Kotlin/Android continua obrigatório no CI."""
from pathlib import Path
r=Path(__file__).resolve().parents[1]
s=r/'app/src/main/java/br/com/uaicortes'
panel=(s/'VoiceTonePanel.kt').read_text()
tone=(s/'VoiceTone.kt').read_text()
play=(s/'AudioScreen.kt').read_text()
export=(s/'ExportEngine.kt').read_text()
model=(s/'ProjectStore.kt').read_text()
prefs=(s/'VoiceToneFavorites.kt').read_text()
assert 'VoiceTone.STUDIO' in panel and 'VoiceTone.STUDIO' in tone
for label in ('Compensar volume', 'Compressão', 'Redução de ruído leve', 'Suavizar S e CH',
              'Equalizador (EQ) avançado', 'Brilho', 'Meus ajustes', 'Salvar ajuste',
              'Usar ajuste', 'Copiar ajuste para outros vídeos'):
    assert label in panel,label
assert 'showCopyDialog' in panel and 'AlertDialog(' in panel
assert 'project.clips.map' in panel and 'if (it.isImage) it else it.copy(voiceTone = tone)' in panel
assert 'project.music' not in panel
assert 'voicePreviewKey.substringBeforeLast' in play and 'player.seekTo(resumeAt' in play
assert 'setAudioProcessors(arrayOf<AudioProcessor>(VoiceToneProcessor(tone)))' in (s/'VoiceToneProcessor.kt').read_text()
assert 'voiceProcessors(clip.volume, clip.voiceTone)' in export
assert 'voiceProcessors(audio.volume, audio.voiceTone)' in export
assert 'else GainProcessor(volume)' in export
assert 'val voiceTone: VoiceTone = VoiceTone()' in model
assert model.count('VoiceTone.read(j.optJSONObject("voiceTone"))')==2
assert '.put("schemaVersion", 4)' in model
for key in ('trebleDb','compression','noiseReduction','deEsser','autoGain'):
    assert f'.put("{key}"' in tone and f'"{key}"' in tone
assert 'softLimit' in tone and 'VoiceBiquad.bandPass' in tone
assert 'noiseAmount' in tone and 'deEssAmount' in tone
assert 'getSharedPreferences(' in prefs and 'Context.MODE_PRIVATE' in prefs
assert 'VoiceTone.read(JSONObject(raw))' in prefs and 'slot in 1..3' in prefs
print('OK: v220 voz por fonte, DSP, prévia A/B, presets locais, vídeo somente, JSON legado')
