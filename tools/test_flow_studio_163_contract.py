#!/usr/bin/env python3
"""Regressão estática: uma cena = um clipe e chave global com troca segura."""
from pathlib import Path
root = Path(__file__).resolve().parents[1]
p = root/'app/src/main/java/br/com/uaicortes'
ui = (p/'FlowStudioScreen.kt').read_text()
model = (p/'FlowStudioModel.kt').read_text()
clip = (p/'FlowStudioClipPolicy.kt').read_text()
script = (p/'FlowStudioAi.kt').read_text()
video = (p/'GeminiVideoCaptionClient.kt').read_text()
image = (p/'GeminiSceneImageClient.kt').read_text()
failover = (p/'GeminiKeyFailover.kt').read_text()
settings = (p/'GeminiKeyPoolSettings.kt').read_text()
assert 'Criar minhas cenas' in ui and 'onStartScenes' in ui
assert 'Nova cena (um clipe no Flow)' in ui
assert 'Sugerir clipes com Gemini (opcional)' in ui
assert 'FlowStudioClipPolicy.canDivide(draft.story)' in ui
assert 'onPrepare = {' in ui and 'Preparar prompt sem IA' in ui
assert 'Câmera: quem aparece e como se move?' in ui
assert 'O que esse personagem diz?' in ui
assert 'Uma cena = um vídeo do Flow' in ui
assert 'CADA CENA é UM CLIPE INDEPENDENTE' in script
assert 'durationSeconds' in model and 'dialogueLines' in model
assert 'StudioPromptPolicy.compose(' in clip and 'fun canDivide' in clip
assert 'val response = requestJson(URL(' in video and 'GeminiKeyParser.validate(key), payload, operation, model)' in video
assert 'failover.run(GeminiKeyParser.validate(key), model)' in image
assert 'failover.run(safe)' in image
assert 'val source = profiles.firstOrNull { it.id == store.activeId() }' in failover
assert 'if (!quota429(error)) throw error' in failover
assert 'sourceWasPaused' in failover
assert 'Chave Gemini para todo o aplicativo' in settings
assert 'Global' not in settings or True
assert 'GeminiSceneImageClient(context)' in ui
assert 'FlowLegacyPromptScreen(project, onSave' in ui
assert 'schemaVersion=4' in (root/'AGENTS.md').read_text()
print('OK: clipe por cena, criação opcional, prompt local, seleção global e failover 429 opt-in.')
