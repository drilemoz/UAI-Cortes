#!/usr/bin/env python3
"""Validação antecipada para evitar regressões de import e versionamento."""
from pathlib import Path
import re

root = Path(__file__).resolve().parents[1]
source = (root / "app/src/main/java/br/com/uaicortes/ExportEngine.kt").read_text()
gradle = (root / "app/build.gradle.kts").read_text()
workflow = (root / ".github/workflows/android-apk.yml").read_text()
about = (root / "app/src/main/java/br/com/uaicortes/MainActivity.kt").read_text()
release = (root / "RELEASE_NOTES.md").read_text()
assert "import androidx.media3.common.OverlaySettings" in source
assert "import androidx.media3.common.VideoCompositorSettings" in source
assert "import androidx.media3.effect.StaticOverlaySettings" in source
assert "import androidx.media3.effect.OverlaySettings" not in source
assert "import androidx.media3.transformer.VideoCompositorSettings" not in source
assert "class PipCompositor" in source and "setVideoCompositorSettings(PipCompositor" in source
assert re.search(r"compileSdk\s*=\s*36", gradle)
assert re.search(r"targetSdk\s*=\s*36", gradle)
assert re.search(r"versionCode\s*=\s*3", gradle)
assert re.search(r'versionName\s*=\s*"0.1.2"', gradle)
assert "Versão 0.1.2 (3)" in about
assert "UaiCortes-v0.1.2.apk" in workflow
assert "TAG=\"v0.1.2\"" in workflow
assert "android-actions/setup-android@v4" in workflow
assert "platforms;android-36" in workflow
assert "v0.1.2" in release
print("OK: imports Media3, API 36, versionamento e workflow coerentes")
