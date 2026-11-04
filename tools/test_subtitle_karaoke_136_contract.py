#!/usr/bin/env python3
"""Regressão estática das três variantes de karaokê (não substitui Android)."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
s=root/'app/src/main/java/br/com/uaicortes'
a=(s/'SubtitleAnimations.kt').read_text()
w=(s/'SubtitleWordEffects.kt').read_text()
ui=(s/'SubtitleScreen.kt').read_text()
g=(s/'SubtitleStyleGallery.kt').read_text()
v=(s/'VideoEffects.kt').read_text()
style=(s/'SubtitleStyles.kt').read_text()
for name in ('KARAOKE_PULSE','KARAOKE_BOLD','KARAOKE_UNDERLINE'):
    assert 'const val '+name in a
    assert 'SubtitleAnimations.'+name in style
assert 'highlightColorOverride' in a and 'blendColor' in w
assert 'frame.highlightColorOverride' in v and 'animationFrame.highlightColorOverride' in ui
assert 'frame.underlineActiveWord' in v and 'animationFrame.underlineActiveWord' in ui
assert 'frame.boldActiveWord' in v and 'animationFrame.boldActiveWord' in ui
assert 'UnderlineSpan()' in v
assert 'active?.hashCode()' in v and 'frame?.highlightColorOverride' in v
assert 'frame.underlineActiveWord' in g and 'frame.boldActiveWord' in g
assert 'SubtitleAnimations.isKaraoke' in (s/'SubtitleVisualEditor.kt').read_text()
assert 'SubtitleAnimations.isKaraoke(cue.animation)' in (s/'SubtitleWordTimingEditor.kt').read_text()
from check_version_sync import check
version, code=check(root)
assert version=='0.1.136' and code==137
print('OK: variantes de karaokê, prévia, exportação, cache e versão')
