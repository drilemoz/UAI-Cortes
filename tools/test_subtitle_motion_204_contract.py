#!/usr/bin/env python3
"""Garante integração única de curva temporal Compose/Media3 e compatibilidade de legados."""
from pathlib import Path
r=Path(__file__).resolve().parents[1]
a=r/'app/src/main/java/br/com/uaicortes'
core=(r/'subtitle-core/src/main/kotlin/br/com/uaicortes/subtitles/core/SubtitleMotionPolicy.kt').read_text()
anim=(a/'SubtitleAnimations.kt').read_text()
preview=(a/'SubtitleScreen.kt').read_text()
export=(a/'VideoEffects.kt').read_text()
editor=(a/'SubtitleVisualEditor.kt').read_text()
gallery=(a/'SubtitleStyleGallery.kt').read_text()
assert 'object SubtitleMotionPolicy' in core and 'fun evaluate(' in core
for token in ('SOFT_POP','CINEMATIC','SOFT_FADE','IMPACT','GLIDE'):
    assert 'const val '+token in core
    assert 'SubtitleMotionPolicy.'+token in anim
assert 'SubtitleMotionPolicy.evaluate(cue.animation, elapsed, duration, rate)' in anim
assert 'SubtitleAnimations.frame(cue,playbackMs)' in preview
assert 'SubtitleAnimations.frame(it,' in export
assert 'frame?.slideX ?: 0f' in export and 'setAlphaScale' in export
assert 'animationFrame.slideX * frameSize.width' in preview
assert 'SubtitleAnimations.CINEMATIC' in editor
assert 'SubtitleStudioPresets.recommended' in gallery
for legacy in ('TYPEWRITER','KARAOKE','FADE','POP','RISE','WORD_REVEAL'):
    assert 'const val '+legacy in anim
assert (r/'subtitle-core/src/test/kotlin/br/com/uaicortes/subtitles/core/SubtitleMotionPolicyTest.kt').exists()
print('OK: 0.1.204 curvas suaves isoladas, legados preservados e exportador/prévia compartilhados')
