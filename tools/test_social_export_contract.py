#!/usr/bin/env python3
"""Verificações estruturais da saída social, não substituem Media3 em Android."""
from pathlib import Path
root = Path(__file__).resolve().parents[1]
s = root / 'app/src/main/java/br/com/uaicortes'
policy = (s/'SocialExportPreset.kt').read_text()
engine = (s/'ExportEngine.kt').read_text()
vm = (s/'ExportTaskViewModel.kt').read_text()
ui = (s/'ExportScreen.kt').read_text()
report = (s/'ExportQualityReport.kt').read_text()
assert all(k in policy for k in ('ORIGINAL', 'SHORTS', 'REELS', 'TIKTOK'))
assert 'shortSide to vertical' in policy and 'val vertical =' in policy
assert 'shortSide in listOf(360, 480, 720, 1080)' in policy
assert 'ExportBitratePreset.COMPACT' in policy
assert 'FIT("Mostrar tudo")' in policy and 'CENTER_CROP("Preencher tela")' in policy
assert 'Presentation.createForWidthAndHeight(' in engine
assert 'Presentation.createForShortSide(height)' in engine
assert 'Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP' in engine
assert 'Presentation.LAYOUT_SCALE_TO_FIT' in engine
assert 'portraitSize?.first' in engine and 'portraitSize?.second' in engine
assert 'destination = destination, frameMode = frameMode' in vm
assert 'exportVm.start(p,quality,safeName,bitratePreset,destination,frameMode)' in ui
assert 'configureExportPreviewFrame' in ui
assert 'showFrameGuides' in ui
assert 'requestFrameWidth' not in engine
assert 'requestedFrameWidth' in report and 'requestedFrameHeight' in report
assert 'destinationLabel' in report
assert (root/'app/src/test/java/br/com/uaicortes/SocialExportPolicyTest.kt').exists()
assert 'exportSocialPortrait' in (root/'app/src/androidTest/java/br/com/uaicortes/ExportPipelineSmokeAndroidTest.kt').read_text()
print('OK: destinos sociais, 9:16, fit/crop explícitos, prévia, bitrate e diagnóstico')
