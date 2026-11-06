#!/usr/bin/env python3
"""Contrato de UI v185: priorizar ações e avisos sem esconder consentimento."""
from pathlib import Path
r=Path(__file__).resolve().parents[1]/'app/src/main/java/br/com/uaicortes'
a=(r/'AutoSubtitleScreen.kt').read_text()
s=(r/'SubtitleSpeakersUi.kt').read_text()
g=(r/'GeminiVoiceReviewUi.kt').read_text()
h=(r/'SubtitleHelpUi.kt').read_text()
assert 'Modelo offline PT-BR pronto' in a and 'Transcrever fala (offline)' in a
assert 'Transcrever com Gemini (online)' in a and 'Sugerir Voz 1, Voz 2' in a
assert 'SubtitleHelpButton("Reconhecimento offline"' in a
assert 'SubtitleHelpButton("Gemini e privacidade"' in a
assert 'SubtitleHelpButton("Revisão de legendas"' in a
assert 'if (results.isNotEmpty())' in a and 'Revisar ${results.size} legendas' in a
assert 'O vídeo não será enviado.' in a
assert 'A operação pode consumir cota ou gerar cobrança' in a
assert 'Detalhes de consumo e privacidade' in a and 'if (usageExpanded)' in a
assert 'GeminiUsageHint("legendas automáticas"' in a
assert 'GeminiTaskCheckpointStore' in a and 'geminiCheckpoint.saveSubtitles' in a
assert 'geminiClient.transcribe(context,project,apiKey,detectSpeakers)' in a
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
