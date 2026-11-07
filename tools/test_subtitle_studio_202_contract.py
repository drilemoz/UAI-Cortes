#!/usr/bin/env python3
from pathlib import Path
r=Path(__file__).resolve().parents[1]
s=r/'app/src/main/java/br/com/uaicortes'
core=r/'subtitle-core/src/main/kotlin/br/com/uaicortes/subtitles/core/SubtitleTextCasePolicy.kt'
assert core.exists()
assert 'include(":app", ":subtitle-core")' in (r/'settings.gradle.kts').read_text()
assert 'implementation(project(":subtitle-core"))' in (r/'app/build.gradle.kts').read_text()
assert 'subtitle-core' in (r/'tools/package_source.py').read_text()
assert 'Locale.forLanguageTag("pt-BR")' in core.read_text()
assert 'val textCase:' in (s/'ProjectStore.kt').read_text()
assert 'put("textCase"' in (s/'ProjectStore.kt').read_text()
assert 'textCase=source.textCase' in (s/'SubtitleStyles.kt').read_text()
assert 'SubtitleStudioPresets.recommended' in (s/'SubtitleStyleGallery.kt').read_text()
assert 'SubtitleTextCasePolicy.display(cue.text, cue.textCase)' in (s/'SubtitleAnimations.kt').read_text()
assert 'active.copy(text=rendered' in (s/'VideoEffects.kt').read_text()
assert 'animationFrame.text' in (s/'SubtitleScreen.kt').read_text()
assert 'if (visual.has(key)) cue.put(key, visual.get(key))' in (s/'SubtitleUserPresetStore.kt').read_text()
print('OK: Subtitle Studio 0.1.202 core and preview/export contract')
