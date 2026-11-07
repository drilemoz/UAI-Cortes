#!/usr/bin/env python3
"""Contrato offline da tela de marca e do gerador Flow. Não simula Android real."""
from pathlib import Path
r=Path(__file__).resolve().parents[1]
p=r/'app/src/main/java/br/com/uaicortes'
get=lambda name:(p/name).read_text()
ui=get('TextWatermarkScreen.kt')
mark=get('TextWatermark.kt')
assert 'SubtitleOverlayOnFrame(watermark, editable = true' in ui
assert 'onPositionCommit = { x, y -> draft = draft.copy(x = x, y = y) }' in ui
assert 'WatermarkStepSlider("Opacidade (%)"' in ui
assert 'WatermarkStepSlider("Tamanho"' in ui
assert 'removeConfirm' in ui and 'guides' in ui
assert 'WatermarkDefaultsStore(context)' in ui
assert 'opacity.coerceIn(0f, 1f)' in mark
assert 'fontFamily = w.fontFamily' in mark and 'shadowOffsetDp = w.shadowOffsetDp' in mark
assert 'flowPromptDraft: FlowPromptDraft? = null' in get('ProjectStore.kt')
assert '.put("flowPromptDraft", flowPromptDraft?.json())' in get('ProjectStore.kt')
assert 'FlowPromptDraft.read(j.optJSONObject("flowPromptDraft"))' in get('ProjectStore.kt')
assert '.put("schemaVersion", 4)' in get('ProjectStore.kt')
assert '"flow_workspace" -> FlowWorkspaceScreen(::back)' in get('MainActivity.kt')
assert '"flow_prompt" -> project?.let' in get('MainActivity.kt')
assert 'Triple("Prompt Flow"' not in get('EditorScreen.kt')
assert 'HomePrimaryActions(onCreate, onAnimate, onFlow = onFlow)' in get('HomeScreens.kt')
assert '"Estúdio de Cenas"' in get('HomeScreens.kt')
assert 'FlowPromptPolicy.compose(style, draft, selected)' in get('FlowPromptScreen.kt')
assert 'GeminiCredentialStore(context).read()' in get('FlowPromptScreen.kt')
assert 'if (consent) AlertDialog' in get('FlowPromptScreen.kt')
assert 'suggestion = result' in get('FlowPromptScreen.kt')
assert 'suspend fun refineFlowScene(' in get('GeminiVideoCaptionClient.kt')
assert 'getSharedPreferences("flow_style_v1", Context.MODE_PRIVATE)' in get('FlowPromptStyleStore.kt')
assert 'getSharedPreferences("flow_projects_v1", Context.MODE_PRIVATE)' in get('FlowWorkspaceStore.kt')
assert 'read() // Não sobrescrever' in get('FlowWorkspaceStore.kt')
assert (r/'app/src/test/java/br/com/uaicortes/FlowPromptWatermarkTest.kt').exists()
print('OK: Flow independente na Home, sem botão no editor; cenas locais, Gemini sob consentimento e marca de texto ajustável')
