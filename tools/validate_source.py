#!/usr/bin/env python3
"""Verificações estruturais: não substituem compilação Android ou testes físicos."""
from pathlib import Path
import re
root=Path(__file__).resolve().parents[1]
source=(root/'app/src/main/java/br/com/uaicortes/ExportEngine.kt').read_text()
ui=(root/'app/src/main/java/br/com/uaicortes/MainActivity.kt').read_text()
model=(root/'app/src/main/java/br/com/uaicortes/ProjectStore.kt').read_text()
placement=(root/'app/src/main/java/br/com/uaicortes/OverlayPlacement.kt').read_text()
browser=(root/'app/src/main/java/br/com/uaicortes/MediaBrowser.kt').read_text()
workflow=(root/'.github/workflows/android-apk.yml').read_text()
gradle=(root/'app/build.gradle.kts').read_text()
assert re.search(r'versionCode\s*=\s*9',gradle)
assert re.search(r'versionName\s*=\s*"0.1.8"',gradle)
assert 'Versão 0.1.8 (9)' in ui
for label in ['VERSION: \'0.1.8\'','UaiCortes-v0.1.8.apk','TAG="v0.1.8"',"versionCode='9'",'actions/upload-artifact@v4','gh release upload']:
    assert label in workflow, label
assert 'platforms;android-36' in workflow and 'android-actions/setup-android@v4' in workflow
assert 'abortOnError = false' not in gradle
assert 'setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)' in ui
assert '@OptIn(ExperimentalMaterial3Api::class)' in browser and 'PrimaryTabRow(' in browser
assert 'READ_MEDIA_VISUAL_USER_SELECTED' in browser
assert 'MAX_PIPS = 4' in placement and 'intersects' in placement and 'arrange' in placement
assert 'overlays: List<Overlay>' in model and 'optJSONObject("pip")' in model
assert 'schemaVersion' in model and 'JSONArray()' in model
assert 'OverlayTimeline(' in ui and 'p.overlays.forEach' in ui
assert 'AnimatedVisibility(' in ui and 'animateContentSize()' in ui
assert 'PipPopup(' not in ui and 'Dialog(' not in ui
assert 'onChange(pip.copy(scale = ' in ui and 'onChange(pip.copy(muted = ' in ui
assert 'MediaBrowser' in ui and 'insertOverlay(' in ui
assert 'PipCompositor(private val overlays: List<Overlay>)' in source
assert 'setVideoCompositorSettings(PipCompositor(project.overlays.take(4)))' in source
assert 'sequences += mainSequence' in source and 'pip.muted' in source
assert 'import androidx.media3.common.VideoCompositorSettings' in source
assert 'import androidx.media3.common.OverlaySettings' in source
print('OK: 0.1.8 / versionCode 9, PiPs múltiplos, anticolisão, interface expansível, exportação e CI')
