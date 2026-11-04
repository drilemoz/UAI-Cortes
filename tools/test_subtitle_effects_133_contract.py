#!/usr/bin/env python3
"""Contratos da etapa 0.1.133; não substituem Android real/Media3."""
from pathlib import Path
r=Path(__file__).resolve().parents[1]
b=r/'app/src/main/java/br/com/uaicortes'
a=(b/'SubtitleAnimations.kt').read_text()
s=(b/'SubtitleStyles.kt').read_text()
u=(b/'SubtitleVisualEditor.kt').read_text()
g=(b/'SubtitleStyleGallery.kt').read_text()
v=(b/'VideoEffects.kt').read_text()
p=(b/'SubtitleScreen.kt').read_text()
for n in ['RISE','DROP','SLIDE_LEFT','SLIDE_RIGHT','WORD_REVEAL','ELASTIC','FLOAT','SWAY','SOFT_SHAKE']:
    assert f'const val {n}' in a
assert 'val groups:' in a and 'val slideX: Float = 0f' in a
assert 'translationX=animationFrame.slideX * frameSize.width' in p
assert 'frame?.slideX ?: 0f' in v
assert 'translationX=sample.slideX' in u and 'translationX=frame.slideX' in g
assert 'val shown=frame.text.ifBlank' not in g and 'if (frame.text.isBlank()) return' in g
assert 'SubtitleAnimations::label' in u and 'animationGroup' in u
assert 'Text("Pausar"' not in u and '"Pausar" else "Reproduzir"' in u
assert '"Reiniciar"' in u and '"Tempo real"' in u
assert 'Copiar cores das mesmas palavras' in u
assert 'wordColors: Boolean = false' in s and 'matchingWordColors(' in s
assert 'wordColors=if (options.wordColors)' in s
assert 'batchUndoMessage=copyOptions.summary(all.size)' in p
assert 'status=batchUndoMessage' in p
assert 'versionCode = 134' in (r/'app/build.gradle.kts').read_text()
print('OK: efeitos novos, seleção, preview, cópia segura, status e release 0.1.133')
