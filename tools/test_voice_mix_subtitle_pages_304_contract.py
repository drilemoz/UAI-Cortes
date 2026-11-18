#!/usr/bin/env python3
"""Regressão estática para páginas, ações explícitas e volume salvo com a voz."""
from pathlib import Path

base = Path(__file__).resolve().parents[1] / 'app/src/main/java/br/com/uaicortes'
read = lambda file: (base / (file + '.kt')).read_text()
s = read('SubtitleScreen')
prefs = read('VoiceActionPreferences')
v = read('VoiceIaScreen')
center = read('GeminiVoiceCenterScreen')
mix = read('GeneratedVoiceMix')
assert 'itemsIndexed(filtered,key=' in s
assert 'SubtitleCompactTimeline(sorted,' in s and 'SubtitleFileExport.render(snapshot, selectedFormat)' in s
assert 'val snapshot = p.subtitles.toList()' in s
for label in ('Importar SRT', 'Exportar legendas', 'Editar todas', 'Estilos por personagem', 'Apagar todas'):
    assert any('SubtitleActionIcon' in line and label in line for line in s.splitlines()), label
assert 'if(deleteAllConfirm) AlertDialog' in s and 'TooltipBox' in s and 'moreSubtitleTools' not in s
assert 'getBoolean(action.key, false)' in prefs and 'Context.MODE_PRIVATE' in prefs
assert 'Não mostrar novamente' in prefs
assert 'if (hideNext) actionPrefs.setSkipping(batchInsertAction, true)' in v
assert 'if (actionPrefs.skips(batchInsertAction)) applyBatchInsert()' in v
assert 'if (actionPrefs.skips(singleInsertAction)) insert(cue, take)' in v
assert 'VoiceIaPolicy.fits(current.audioTracks.firstOrNull' in v
assert 'File(take.filePath).isFile' in v
assert v.count('VoiceIaPolicy.merge(') == 2
assert 'else if (actionPrefs.skips(previewAction)) generateDirectly = true' in center
assert center.index('} else if (blindLabel != null)') < center.index('} else if (actionPrefs.skips(previewAction))')
assert 'val existing = withContext(Dispatchers.IO) { cache.find(id) }' in center
assert 'Confirmar antes de gerar prévia' in center and 'Restaurar confirmações de Vozes IA' in read('GeminiSettingsUi')
assert 'ORIGINAL_GAIN_LIMIT = .10f' in mix and 'clip.volume > ORIGINAL_GAIN_LIMIT' in mix
assert 'voiceStartMs < cursorMs && voiceEndMs > start' in mix and '!clip.isImage' in mix
assert 'GeneratedVoiceMix.lowerOriginal(project.clips, start, take.durationMs)' in read('VoiceIaDraftStore')
assert 'GeneratedVoiceMix.lowerOriginal(current.clips, audio.startMs, audio.durationMs)' in read('VoiceCloneStudioDialog')
assert 'GeneratedVoiceMix' not in read('VoiceIaExternalAudio')
assert 'voiceProcessors(clip.volume, clip.voiceTone)' in read('ExportEngine')
assert '?.volume ?: 1f' in read('EditorScreen')
print('OK: lista de legendas, ícones acessíveis, confirmações opcionais e volume aplicado a clipes afetados')
