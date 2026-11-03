#!/usr/bin/env python3
"""Teste de contrato de UI/navegação da v0.1.108.
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

# A gravação do projeto precisa ser bem-sucedida ANTES da navegação.
assert 'onSaveGeminiAndReturn = { changed ->' in main
callback = main[main.index('onSaveGeminiAndReturn = { changed ->'):]
assert callback.index('if (write(changed))') < callback.index('screen = "editor"')
assert 'onSaveAndReturn=onSaveGeminiAndReturn' in audio
segment = voice[voice.index('if (addTimelineConfirm) {'):voice.index('if (confirmation.isNotBlank())')]
assert 'if (onSaveAndReturn(changed)) {' in segment
assert segment.index('if (onSaveAndReturn(changed))') < segment.index('expanded = false')
assert 'Não foi possível salvar o projeto' in segment
assert 'project.audioTracks.none { it.uri == uri }' in segment  # sem duplicar
assert 'player.pause()' in segment
assert 'Após salvar o projeto' in segment
assert 'Salvar e usar no vídeo' in voice and 'Salvar e voltar' in segment

# A escolha da voz permanece separada da geração e inclusão.
confirm_voice = voice.split('Text("Confirmar voz")')[0].rsplit('Button(onClick = {', 1)[-1]
assert 'selectedVoice = chosen' in confirm_voice
assert 'onReturnToVideo()' in confirm_voice
for bad in ['api.synthesize', 'onSaveAndReturn(', 'AudioTrack(']:
    assert bad not in confirm_voice
print('OK: extração recolhida, importação externa, salvamento confirmado e retorno ao editor sem inclusão duplicada')
