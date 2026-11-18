#!/usr/bin/env python3
"""Contrato de UI v185: priorizar ações e avisos sem esconder consentimento."""
from pathlib import Path
r=Path(__file__).resolve().parents[1]/'app/src/main/java/br/com/uaicortes'
a=(r/'AutoSubtitleScreen.kt').read_text()
s=(r/'SubtitleSpeakersUi.kt').read_text()
g=(r/'GeminiVoiceReviewUi.kt').read_text()
h=(r/'SubtitleHelpUi.kt').read_text()
panel=(r/'AutoSubtitlePanel.kt').read_text()
assert 'Modelo offline PT-BR pronto' in a and 'Gerar offline' in a
assert 'Enviar e gerar' in a and 'Identificar vozes' in a
assert 'ModalBottomSheet(' in a and 'if (showDetails && open == SubtitleEngine.GEMINI) AlertDialog(' in a
assert 'AutoSubtitleEngineCard(' in a and 'Revisar ${results.size} legendas' in panel
assert 'O vídeo não é enviado.' in a
assert 'Só envia áudio ao Gemini. Se o Transcribe falhar por cota ou disponibilidade, pode tentar Flash-Lite uma vez.' in a
assert 'Ajuda sobre transcrição Gemini' in a
assert 'mutableStateOf(false)' in a.split('var showDetails', 1)[1].splitlines()[0]
assert 'GeminiUsageHint("legendas automáticas"' in a
assert 'GeminiTaskCheckpointStore' in a and 'geminiCheckpoint.saveSubtitles' in a
assert 'geminiClient.transcribe(context, source.project, apiKey, detect, report)' in a
assert 'Dialog(onDismissRequest = dismiss)' in s and 'heightIn(max = 300.dp)' in s
assert 'SubtitleSpeakerPolicy.assign(project, cueId, null)' in s
assert 'SubtitleSpeakerPolicy.assign(project, cueId, candidate.id)' in s
assert 'SubtitleHelpButton("Atribuir personagem"' in s
assert 'SubtitleHelpButton("Revisar vozes sugeridas"' in g
assert 'IconButton(onClick = { expanded = true }' in h
assert 'Passo 2 · Gerar legendas' not in a
assert 'Text("Dica: faça primeiro um teste curto' not in a
assert 'schemaVersion' in (r/'ProjectStore.kt').read_text()
print('OK: informações progressivas, consentimento Gemini claro, seletor de personagens compacto')
