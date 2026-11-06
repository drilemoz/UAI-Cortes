#!/usr/bin/env python3
"""Contratos estruturais da v189; complementam testes Kotlin/JVM e CI."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
src=root/'app/src/main/java/br/com/uaicortes'
ui=(src/'VoiceIaScreen.kt').read_text()
style=(src/'VoiceIaStylePolicy.kt').read_text()
saved=(src/'VoiceIaVoiceAssignments.kt').read_text()
queue=(src/'VoiceIaQueuePolicy.kt').read_text()
speakers=(src/'SubtitleSpeakerPolicy.kt').read_text()
review=(src/'VoiceIaBatchReviewPolicy.kt').read_text()
assert all(x in saved for x in ('"defaultStyle"', '"speakerStyles"','"cueStyles"', 'readStyles('))
assert all(x in style for x in ('speakerStyle(', 'cueStyle(', 'fun all(', 'saved.cueStyles[cue.id]', 'saved.speakerStyles::get'))
assert 'speakerStyles = project.voiceIaVoices.speakerStyles - speakerId' in speakers
assert 'VoiceIaStylePolicy.speakerStyle(latestProject' in ui
assert 'VoiceIaStylePolicy.cueStyle(latestProject' in ui
assert 'VoiceIaStylePolicy.all(latestProject' in ui
assert 'pendingSpeakerStyle?.let' in ui
assert 'pendingPreviewSpeakerId?.let' in ui
assert 'GeminiVoicePreviewCache(context)' in ui and 'cache.find(identity)' in ui
assert 'cache.save(identity, sample.bytes, voice)' in ui
assert 'GeminiPreviewIdentity.TEXT' in ui and 'previewFile?.delete()' not in ui
assert 'previewSpeaker(speakerId)' in ui
assert 'pode ter consumido cota' in ui
assert 'Gerar somente pendentes do filtro' in ui
assert 'requestSpeakerBatch(speaker.id)' in ui
assert 'VoiceIaBatchReviewPolicy.review' in ui and 'installedOutdated' in ui
assert 'currentStyles: Map<String, String>' in queue and 'changedStyle' in queue and 'changedModel' in queue
assert 'VoiceIaQueuePolicy.reconcileSelections(previous' in ui
assert 'task.style != GeminiNarrationStyling.instruction(styleFor(cue))' in ui
assert 'val uncertain:' in review
assert 'schemaVersion", 4' in (src/'ProjectStore.kt').read_text()
print('OK: contratos de estilo, consentimento de teste, revisão, filtro e fila v189')
