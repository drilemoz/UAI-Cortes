#!/usr/bin/env python3
"""Contrato do Texto para Voz v0.1.229. Não substitui build/execução Android."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
code=root/'app/src/main/java/br/com/uaicortes'
home=(code/'HomeScreens.kt').read_text()
main=(code/'MainActivity.kt').read_text()
nav=(code/'EditorNavigation.kt').read_text()
screen=(code/'StandaloneTextToVoiceScreen.kt').read_text()
library=(code/'StandaloneVoiceLibrary.kt').read_text()
policy=(code/'StandaloneVoiceTextPolicy.kt').read_text()
# v0.1.243: atalho mudou de ItemCard para HomeActionTile na faixa rolável.
# Exigir rota do atalho atual sem restaurar o cartão antigo.
assert 'HomeActionTile("Texto para Voz", Icons.Default.GraphicEq, onTextToVoice,' in home
assert 'HomePrimaryActions(onCreate, onAnimate, onFlow = onFlow,' in home
assert 'onVoiceCenter = onVoiceCenter, onTextToVoice = onTextToVoice,' in home
assert 'horizontalScroll(rememberScrollState())' in home
assert 'onTextToVoice = { navTo("text_to_voice") }' in main
assert '"text_to_voice" -> StandaloneTextToVoiceScreen(' in main
assert '"text_to_voice"' in nav and '"text_to_voice" -> "home"' in nav
assert 'GeminiVoiceCenterScreen(' in screen and 'onChoose = { selected -> voice = selected' in screen
assert 'GeminiTtsClient(ctx)' in screen and 'GeminiNarrationStyling.instruction(style)' in screen
assert 'GeminiCredentialStore(ctx)' in screen and 'GeminiLocalPacing(ctx)' in screen
assert 'if (confirm) AlertDialog(' in screen and 'Confirmar e gerar' in screen
assert 'client.synthesize(' in screen and 'client.cancel()' in screen
assert 'ActivityResultContracts.CreateDocument("audio/wav")' in screen
assert 'GeminiUsageLedger(ctx)' in screen and 'tokens informados pelo Gemini' in screen
assert 'tokenEstimate(text)' in screen and '4.000 caracteres' in screen
assert 'standalone_voice' in library and 'gemini_narrations' not in library
assert 'VideoProject(' not in screen and 'AudioTrack(' not in screen
assert 'MAX_CHARS = 4000' in policy and 'text.length <= MAX_CHARS' in policy
assert 'Nenhuma geração é feita automaticamente.' in screen
print('OK: TTS autônomo, navegação, selecção Gemini, cota, SAF, WAV e isolamento do editor')
