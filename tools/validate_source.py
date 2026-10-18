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
changelog = (root / "CHANGELOG.md").read_text()
assert "import androidx.media3.common.OverlaySettings" in source
assert "import androidx.media3.common.VideoCompositorSettings" in source
assert "import androidx.media3.effect.StaticOverlaySettings" in source
assert "import androidx.media3.effect.OverlaySettings" not in source
assert "import androidx.media3.transformer.VideoCompositorSettings" not in source
assert "class PipCompositor" in source and "setVideoCompositorSettings(PipCompositor" in source
assert re.search(r"compileSdk\s*=\s*36", gradle)
assert re.search(r"targetSdk\s*=\s*36", gradle)
assert re.search(r"versionCode\s*=\s*7", gradle)
assert re.search(r'versionName\s*=\s*"0.1.6"', gradle)
assert "Versão 0.1.6 (7)" in about
assert "UaiCortes-v0.1.6.apk" in workflow
assert "TAG=\"v0.1.6\"" in workflow
assert "android-actions/setup-android@v4" in workflow
assert "platforms;android-36" in workflow
assert "v0.1.6" in release and "0.1.6 (versionCode 7)" in changelog
# Regression for GitHub Actions #3: media3 PlayerView#setShowBuffering is @UnstableApi.
# Android Lint requires androidx.annotation.OptIn, not kotlin.OptIn.
assert "import androidx.annotation.OptIn" in about
assert "import androidx.media3.common.util.UnstableApi" in about
assert re.search(r"@OptIn\(UnstableApi::class\)\s*@Composable\s*private fun EditorScreen\(", about)
assert "setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)" in about
assert "abortOnError = false" not in gradle
# Garantir entrega mesmo em push na main: Artifacts e Releases sempre executam.
assert "actions/upload-artifact@v4" in workflow
assert "if-no-files-found: error" in workflow
assert "gh release create" in workflow and "gh release upload" in workflow
assert "gh release view \"$TAG\" --json assets" in workflow
assert "GITHUB_STEP_SUMMARY" in workflow
assert "branches: [main]" in workflow and "workflow_dispatch:" in workflow
assert "if: github.event_name == 'workflow_dispatch'" not in workflow
assert "if: startsWith(github.ref" not in workflow
assert "name: UaiCortes-v0.1.6-apk" in workflow
assert "private fun PipScreen(" in about and "Alinhar posição" in about
assert "Começa em:" in about and "Termina em:" in about
assert "ContentScale.Fit" in about and "ContentScale.Crop" not in about
assert "seekProjectPosition(player, p.clips" in about
assert "OverlayEditorLogic.dimensions(" in about
assert "OverlayEditorLogic.dimensions(" in source
assert "getOutputSize(inputSizes:" in source
assert "editable = false" in about
assert "R.layout.player_preview_texture" in about
assert "names[i]" in about
media_browser = (root / "app/src/main/java/br/com/uaicortes/MediaBrowser.kt").read_text()
manifest = (root / "app/src/main/AndroidManifest.xml").read_text()
project_store = (root / "app/src/main/java/br/com/uaicortes/ProjectStore.kt").read_text()
assert "DeviceMediaBrowser" in media_browser and "LazyVerticalGrid" in media_browser
assert "READ_MEDIA_VISUAL_USER_SELECTED" in media_browser and "READ_MEDIA_VISUAL_USER_SELECTED" in manifest
assert "READ_EXTERNAL_STORAGE" in manifest and "READ_MEDIA_VIDEO" in manifest
assert "loadThumbnail" in media_browser and "MediaStore.Images.Media.EXTERNAL_CONTENT_URI" in media_browser
assert 'PipPopup("Tamanho do PiP"' in about and "parsePercentage" in about
assert 'PipPopup("Tempo na tela"' in about and 'PipPopup("Alinhar posição"' in about
assert "muted" in project_store and "pip.muted" in source and "pip.muted" in about
assert "coerceIn(.05f, .90f)" in about
print("OK: Media3, SDK 36, versão 0.1.6 e publicação obrigatória Artifacts + Releases")
