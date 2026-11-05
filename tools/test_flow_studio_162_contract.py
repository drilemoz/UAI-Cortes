#!/usr/bin/env python3
"""Contratos de regressão offline da UI orientada por IA (sem APK ou API)."""
from pathlib import Path
p=Path(__file__).resolve().parents[1]
j=p/'app/src/main/java/br/com/uaicortes'
ui=(j/'FlowStudioScreen.kt').read_text()
model=(j/'FlowStudioModel.kt').read_text()
ai=(j/'FlowStudioAi.kt').read_text()
storage=(j/'FlowStudioStorage.kt').read_text()
assert 'Gerar história com Gemini' in ui
assert 'Surpreenda-me · sugestão grátis' in ui
assert 'Ideia sugerida localmente, sem Gemini e sem consumo de API' in ui
assert 'activeRequest?.cancel()' in ui
assert 'Gemini: dividindo história e preparando as cenas' in ui
assert 'Câmera · ${scene.camera}' in ui and 'Sons · ${scene.effects}' in ui
assert 'Falas por personagem' in ui and 'Quem fala?' in ui
assert 'Duração estimada da cena (1–60 s)' in ui
assert 'Câmera, sons, visual e continuidade' in ui
assert 'Editar título e detalhes manualmente' in ui
assert 'StudioSceneTiming.total(draft.scenes)' in ui
assert 'dialogueLines' in model and 'durationSeconds' in model
assert 'StudioSceneTiming.distribute' in ai
assert 'effectiveDialogue()' in storage
assert 'FlowLegacyPromptScreen(project, onSave' in ui
assert 'StudioImagePricing.note' in ui
notes=(j/'AppReleaseNotes.kt').read_text()
assert notes.count('ReleaseSummary("0.1.162"') == 1
assert notes.count('ReleaseSummary("0.1.161"') == 1
assert 'schemaVersion=4'  in (p/'AGENTS.md').read_text()
print('OK: editor compacto, campos estruturados, indicação de progresso/cancelamento, compatibilidade manual.')
