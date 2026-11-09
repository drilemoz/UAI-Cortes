#!/usr/bin/env python3
"""Contratos de regressão v147, complementares ao teste Android real."""
from pathlib import Path
r=Path(__file__).resolve().parents[1]
src=r/'app/src/main/java/br/com/uaicortes'
s=(src/'ExportScreen.kt').read_text()
assert 'listOf(360,480,720,1080).forEach' in s
assert 'Modifier.weight(1f).height(62.dp)' in s
assert 'ExportProgressUi.progressExplanation(exportPhase)' in s
assert 'ExportProgressUi.renderPercent(exportPhase,progress)' in s
assert 'exportVm.start(p,quality,safeName,bitratePreset,destination,frameMode)' in s
c=(src/'ExportTaskCoordinator.kt').read_text()
assert 'quality in listOf(360, 480, 720, 1080)' in c
q=(src/'VideoExportQuality.kt').read_text()
assert all(f'{n} -> when (preset)' in q for n in (360,480,720,1080))
d=(src/'SocialExportPreset.kt').read_text()
assert 'shortSide in listOf(360, 480, 720, 1080)' in d
assert 'val vertical = ' in d
p=(src/'ExportProgressUi.kt').read_text()
assert 'if (phase == ExportPhase.RENDERING) percent?' in p
assert 'Progresso real do vídeo' in p
v=(src/'VideoSavedDialog.kt').read_text()
assert 'if (showCaptionDialog)' in v and 'if (showAiOptions)' in v
assert all(f'Text("{label}"' in v for label in ['Descrição','Gerar com IA','Compartilhar'])
assert 'showGeminiConsent' in v and 'GeminiCredentialStore(context).read()' in v
assert 'persistDraft()' in v
assert 'Intent.EXTRA_TEXT' in v and 'Intent.EXTRA_STREAM' in v
assert 'VideoCaptionPolicy.isValid(description, hashtags)' in v
print('OK: resoluções, progresso real, popup de publicação e consentimento Gemini')
