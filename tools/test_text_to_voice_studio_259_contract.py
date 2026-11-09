#!/usr/bin/env python3
"""Contratos da tela Estúdio de Voz 0.1.259; não substitui compilação Android."""
from pathlib import Path
root = Path(__file__).resolve().parents[1]
app = root/'app/src/main/java/br/com/uaicortes'
s = (app/'StandaloneTextToVoiceScreen.kt').read_text()
lib = (app/'StandaloneVoiceLibrary.kt').read_text()
draft = (app/'StandaloneVoiceDraftStore.kt').read_text()
entry = (app/'MainActivity.kt').read_text()
importer = (app/'StandaloneVoiceTimelineImport.kt').read_text()
policy = (app/'StandaloneVoiceTextPolicy.kt').read_text()
assert 'Heading("Estúdio de Voz"' in s
assert 'minLines = 3, maxLines = 8' in s and '.imePadding()' in s
assert 'Gerar áudio' in s and 'if (confirm) AlertDialog(' in s
assert 'GeminiTtsClient(ctx)' in s and 'client.synthesize(' in s
assert 'GeminiSpeechLanguage.choices' in s and 'GeminiNarrationStyling.presets' in s
assert 'Selecionar na Central de Vozes' in s and 'GeminiVoiceAliases.title' in s
assert 'library.save(wave, info)' in s and 'reloadLibrary()' in s
assert 'runCatching { writeInfo(file, info.copy(durationMs = wave.durationMs)) }' in lib
assert 'source.inputStream().use' in lib and '.part' in lib
assert 'fun rename(' in lib and 'fun toggleFavorite(' in lib and 'fun delete(' in lib
assert 'file.name.matches(validName)' in lib and 'file.canonicalFile.parentFile == root.canonicalFile' in lib
assert 'Salvar rascunho' in s and 'Restaurar' in s and 'Apagar rascunho salvo' in s
assert 'standalone_voice_draft_v1.properties' in draft and 'fun clear()' in draft
assert 'durationRangeSeconds' in policy and 'tokenEstimate(text)' in s
assert 'onAddToEditor = StandaloneVoiceTimelineImport.callback(' in entry
assert 'ProjectMediaCopies.copy(' in importer and 'verifiedMediaDuration(' in importer
assert 'volume = 1f' in importer and 'duration > project.durationMs - cursor' in importer
assert 'latest() === active' in importer and 'onAddToEditor != null' in s
assert 'Nenhuma geração é feita automaticamente.' in s
assert 'ActivityResultContracts.CreateDocument("audio/wav")' in s
assert 'client.cancel()' in s and 'Nenhuma nova chamada será feita automaticamente.' in s
assert 'AudioTrack(' not in s and 'VideoProject(' not in s
assert len(entry.splitlines()) < 800
print('OK: Estúdio de Voz, editor, metadados, favoritos, rascunho privado e autorização explícita')
