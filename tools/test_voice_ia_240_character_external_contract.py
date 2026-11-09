#!/usr/bin/env python3
"""Contrato v240: identidade vocal, humor independente, importação local e proteção de fila."""
from pathlib import Path
src = Path(__file__).resolve().parents[1] / 'app/src/main/java/br/com/uaicortes'
ui = (src / 'VoiceIaScreen.kt').read_text()
policy = (src / 'VoiceIaVoiceAssignments.kt').read_text()
external = (src / 'VoiceIaExternalAudio.kt').read_text()
style = (src / 'VoiceIaStylePolicy.kt').read_text()
store = (src / 'ProjectStore.kt').read_text()
assert 'if (speaker != null) return speakerVoiceOverrides[speaker.id]' in ui
assert 'voicePickerSpeakerId = speaker.id' in ui
assert 'speakerVoices = updated, cueVoices = if (voice == null)' in policy
assert 'cue.speakerId != null && project.subtitleSpeakers.any' in policy
assert 'VoiceIaStylePolicy.cueStyle(latestProject' in ui
assert 'GeminiNarrationStyling.instruction(styleFor(cue))' in ui
assert 'api.synthesize(key, usedModel, voiceId, requestStyle, cue.text)' in ui
assert 'val chosen = cues.filter { it.id in selectedIds &&' in ui
assert '(it.id == regenerateId || importedFor(it) == null)' in ui
assert 'if (project.subtitleSpeakers.isEmpty()) "Vozes" else "Personagens"' in ui
assert 'if (character == null) IconButton(' in ui
assert 'onClick = { selectedCueId = cue.id; advancedCueId = cue.id }' in ui
assert 'Icon(Icons.Default.Close, "Fechar lembrete de pendências")' in ui
assert 'rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument())' in ui
assert 'externalPicker.launch(arrayOf("audio/*"))' in ui
assert 'VoiceIaExternalAudio.prepare(context, uri, current, cue)' in ui
assert 'if (!runCatching { onSave(updated) }.getOrDefault(false)) {' in ui and 'file.delete()' in ui
assert 'val imported = importedFor(cue)' in ui and 'val importedCount' in ui
assert 'val readyCount = batchPlan.reusable' in ui
assert 'fun importedFor(project: VideoProject, cue: SubtitleCue)' in external
assert 'it.subtitleFingerprint == VoiceIaPolicy.fingerprint(cue)' in external
assert 'source.scheme == "content"' in external
assert 'MAX_BYTES = 64L * 1024 * 1024' in external
assert 'MediaExtractor()' in external and 'MediaFormat.KEY_DURATION' in external
assert 'part.renameTo(file)' in external and 'out.fd.sync()' in external
assert 'StatFs(folder.path).availableBytes' in external
assert 'kind = VoiceIaPolicy.KIND' in external and 'ttsModel = MODEL' in external
assert 'import java.util.UUID' in external and 'filesDir' in external
assert 'saved.cueStyles[cue.id]' in style
assert '.put("schemaVersion", 4)' in store
print('OK: v240 voz fixa do personagem, humor por fala, importação SAF privada e fila segura')
