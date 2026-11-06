#!/usr/bin/env python3
"""Teste de contrato de UI/navegação da v0.1.109.
Não substitui testes Android instrumentados nem chamada real ao Gemini.
"""
from pathlib import Path
root = Path(__file__).resolve().parents[1]
base = root / 'app/src/main/java/br/com/uaicortes'
audio = (base/'AudioScreen.kt').read_text()
voice = (base/'GeminiNarrationUi.kt').read_text()
main = (base/'MainActivity.kt').read_text()

# O menu de extração inicia fechado e as ferramentas só existem dentro do painel.
assert 'var extractionExpanded by remember(p.id) { mutableStateOf(false) }' in audio
start = audio.index('Text("Extrair áudio de vídeo"')
head = audio[:start]
assert 'clickable(enabled = !exporting)' in head
assert 'extractionExpanded = !extractionExpanded' in head
assert 'if (extractionExpanded) {' in audio[start:]
expand = audio.index('if (extractionExpanded) {', start)
end = audio.index('} // As opções são exibidas somente após a expansão manual.', expand)
for token in ['"Origem do vídeo"', '"Fonte externa"', '"Formato de áudio"', '"Salvar e usar"']:
    assert token in audio[expand:end], token
assert 'extractionExpanded = true // A seleção externa' in audio
assert 'val previewClip = if (extractionExpanded)' in audio

# Após confirmar geração, o WAV válido entra na tela Áudio, sem navegação automática.
assert 'onSaveGeminiToAudio = { changed ->' in main
callback = main[main.index('onSaveGeminiToAudio = { changed ->'):main.index('onSaveAndReturn = { changed ->')]
assert 'write(changed)' in callback
assert 'screen = "editor"' not in callback
assert 'onSaveToAudio = { changed ->' in audio
assert 'if (onSaveToAudio(changed)) {' in voice
assert 'if (onSaveToAudio(changed)) {' in voice[voice.index('if (addTimelineConfirm) {'):]
assert 'latestProject' in voice and 'latestTimelinePosition' in voice
assert 'fresh.audioTracks.any { it.uri == uri }' in voice  # sem duplicar
assert 'Áudio gerado' in voice
assert 'Gerar e usar' in voice
assert 'Adicionar à mixagem' in voice
assert 'if (!api.checkModel(' not in voice
assert 'previewCache.find(' in voice
# A escolha da voz é separada de síntese; o retorno é para a janela Gemini.
confirm_voice = voice[voice.index('    // Catálogo e prévias centralizados:'):voice.index('    if (reuseDialogOpen && expanded) {')]
assert 'GeminiVoiceCenterScreen(' in confirm_voice
assert 'selectedVoice = chosen' in confirm_voice
assert 'voicePicker = false' in confirm_voice
for bad in ['onReturnToVideo()', 'api.synthesize', 'onSaveToAudio(']:
    assert bad not in confirm_voice
print('OK: extração recolhida, geração consentida, salvamento e retorno à tela Áudio sem duplicação')
