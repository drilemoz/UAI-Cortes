#!/usr/bin/env python3
"""Contrato da tela compacta. Não substitui teste Compose/Media3 em Android."""
from pathlib import Path
import re
r=Path(__file__).resolve().parents[1]
s=(r/'app/src/main/java/br/com/uaicortes/ExportScreen.kt').read_text()
e=(r/'app/src/main/java/br/com/uaicortes/ExportEngine.kt').read_text()
p=(r/'app/src/main/java/br/com/uaicortes/SocialExportPreset.kt').read_text()
assert 'CompactExportOption(' in s and 'CompactExportLabel(' in s
assert all(x in s for x in ['"Original"','"Shorts"','"Reels"','"TikTok"','"Mostrar tudo"','"Preencher"'])
assert 'Text("${q}p"' not in s # valores não ficam com bitrate no cartão de resolução
assert 'CompactExportOption("${q}p"' in s
assert 'CompactExportOption("${preset.label} · ${mbps}M"' in s
assert 'var previewExpanded by rememberSaveable' in s
assert 'if(previewExpanded)' in s and 'InlineVideoPreview(previewAspect)' in s
assert 'configureExportPreviewFrame' in s and 'AspectRatioFrameLayout.RESIZE_MODE_ZOOM' in (r/'app/src/main/java/br/com/uaicortes/PreviewMediaUtils.kt').read_text()
assert 'if(destination.isVertical && showFrameGuides)' in s
assert 'guideKind != "UI"' in s and 'guideKind != "TEXT"' in s
assert 'ExportFrameIllustration(false)' in s and 'ExportFrameIllustration(true)' in s
assert 'As margens reais variam' in s and 'Guias não entram no MP4' in s
assert 'if(it) previewExpanded=true' in s
assert s.index('Column(Modifier.weight(1f).verticalScroll') < s.index('Surface(shadowElevation=8.dp')
assert s.index('Surface(shadowElevation=8.dp') < s.index('if(framingHelpExpanded)')
assert 'navigationBarsPadding()' in s and 'Button(onClick={' in s
assert 'exportVm.start(p, quality, safeName, bitratePreset, destination, frameMode)' in s
assert 'ExportBitratePreset.values().forEach' in s
assert 'ExportDestination.ORIGINAL.name' in s and 'VerticalFrameMode.FIT.name' in s
assert 'exportVm.savedVideo' not in s or 'VideoSavedDialog(' in s
assert 'Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP' in e and 'Presentation.LAYOUT_SCALE_TO_FIT' in e
assert 'fun dimensions(' in p and 'fun recommendedBitrate(' in p
from check_version_sync import check
version, code = check(r)
assert re.search(rf'versionCode\s*=\s*{code}\b',(r/'app/build.gradle.kts').read_text())
print('OK: layout compacto, rodapé fixo, prévia local, exemplos, guias e compatibilidade do exportador')
