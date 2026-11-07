#!/usr/bin/env python3
"""Contratos estruturais do tratamento de voz (não substituem build e escuta)."""
from pathlib import Path
root = Path(__file__).resolve().parents[1]
src = root / 'app/src/main/java/br/com/uaicortes'
model = (src/'ProjectStore.kt').read_text()
export = (src/'ExportEngine.kt').read_text()
ui = (src/'AudioScreen.kt').read_text()
panel = (src/'VoiceTonePanel.kt').read_text()
fx = (src/'VoiceTone.kt').read_text()
processor = (src/'VoiceToneProcessor.kt').read_text()
assert 'val voiceTone: VoiceTone = VoiceTone()' in model
assert model.count('.put("voiceTone", voiceTone.json())') == 2
assert model.count('VoiceTone.read(j.optJSONObject("voiceTone"))') == 2
assert '.put("schemaVersion", 4)' in model
assert 'voiceProcessors(clip.volume, clip.voiceTone)' in export
assert 'voiceProcessors(audio.volume, audio.voiceTone)' in export
assert 'if (tone.safe().enabled) VoiceToneProcessor' in export
assert 'else GainProcessor(volume)' in export
assert 'clip.volume == 1f && !clip.voiceTone.safe().enabled' in export
assert 'VoiceTonePanel(p, update' in ui and 'voiceTonePreviewPlayer' in ui
for phrase in ('Melhorar voz','Equalizador (EQ) avançado','Encorpada','Original','Tratada',
               'bassDb','bodyDb','clarityDb','voiceToneSources'):
    assert phrase in panel, phrase
assert 'ClipAudioBindings.resolved(project)' in panel
assert 'project.clips.filterNot { it.isImage }' in panel
assert 'project.music' not in panel
assert 'class VoiceToneDsp' in fx and 'VoiceTone.WARM' in fx
assert 'onFlush(streamMetadata: AudioProcessor.StreamMetadata)' in processor
assert 'DefaultAudioSink.Builder(context)' in processor
assert 'setAudioProcessors(arrayOf<AudioProcessor>(VoiceToneProcessor(tone)))' in processor
assert 'VoiceToneTest.kt' in {p.name for p in (root/'app/src/test/java/br/com/uaicortes').iterdir()}
print('OK: tratamento de voz por fonte, EQ, PCM, prévia, JSON legado e exportação')
