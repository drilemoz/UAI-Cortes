#!/usr/bin/env python3
"""Regressões de integração; não substitui compilação Android ou renderização real."""
from pathlib import Path
r=Path(__file__).resolve().parents[1]
s=r/'app/src/main/java/br/com/uaicortes'
def read(name):return (s/name).read_text(encoding='utf-8')
a=read('SubtitleAnimations.kt')
p=read('SubtitleStudioPresets.kt')
v=read('VideoEffects.kt')
preview=read('SubtitleScreen.kt')
style=read('SubtitleStyleGallery.kt')
bridge=read('SubtitleKaraokeBadge.kt')
ui=read('AutoSubtitleScreen.kt')
assert 'const val KARAOKE_BADGE = "karaoke_badge"' in a
assert 'KARAOKE_PULSE, KARAOKE_BOLD, KARAOKE_UNDERLINE, KARAOKE_BADGE -> {' in a
assert 'Karaokê rosa do vídeo' in p and '0xFFE81050' in p
assert 'class SubtitleActiveWordBadgeSpan' in v
assert 'text.setSpan(SubtitleActiveWordBadgeSpan' in v
assert 'if(badgeMode) null else frame.highlighted' in v
assert 'SubtitleAnimations.frame(it,' in v
assert 'drawRoundRect' in bridge and 'getBoundingBox' in bridge
assert '.subtitleKaraokeBadge(cue,animationFrame,badgeLayout)' in preview
assert '.subtitleKaraokeBadge(cue,frame,badgeLayout)' in style
assert 'SubtitleAppearanceMode.KARAOKE_ROSA' in ui
assert 'SubtitleAppearanceMode.KARAOKE_ROSA' in preview
assert 'subtitleKaraokeBadge' not in read('SubtitleStyles.kt') # salvo somente efeito, dados legados intactos
assert (r/'app/src/test/java/br/com/uaicortes/SubtitleKaraokeBadge242Test.kt').exists()
print('OK: tarja rosa renderizada na prévia, galeria e exportação; modo automático e teste JVM presentes')
