#!/usr/bin/env python3
"""Contrato estático do layout v0.1.143: não substitui build Compose."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
ui=(root/'app/src/main/java/br/com/uaicortes/VoiceIaScreen.kt').read_text()
bar=ui[ui.index('// Uma única barra compacta:'):ui.index('if (status.isNotBlank())')]
assert bar.count('VoiceIaToolbarAction(')==6, 'Seis controles em uma única Row'
for label in ['label = ""', 'label = "Voz"', 'VoiceIaShortLabels.style(defaultStyle)',
              'label = "Gerar $pending"','label = "Inserir $preparedCount"',
              'model == GeminiModels.LITE']:
    assert label in bar, label
assert 'Voz geral' not in bar.split('VoiceIaToolbarAction(label = "Voz"')[0]
assert 'Button(' not in bar and 'OutlinedButton(' not in bar
assert 'Surface(onClick = onClick, enabled = enabled' in ui
assert 'shape = RoundedCornerShape(8.dp)' in ui
assert 'semantics { contentDescription = accessibilityLabel }' in ui
assert 'Collator.getInstance(Locale.forLanguageTag("pt-BR"))' in ui
assert 'sortedVoices = catalog.sortedWith' in ui
assert 'sortedStyles = GeminiNarrationStyling.presets.sortedWith' in ui
assert 'heightIn(max = 310.dp)' in ui and 'heightIn(max = 245.dp)' in ui
assert 'currentStyle == preset.label' in ui
assert 'VoiceIaShortLabels' in ui
assert 'pendingVoiceForAll' in ui and 'pendingStyleForAll' in ui
assert 'api.synthesize(key, model, voiceId, requestStyle, cue.text)' in ui
assert 'Text("WAV' not in ui or 'VoiceIaDisplayPolicy.overrunMs' in ui
assert 'SubtitleOverlayOnFrame(selected, editable = false' in ui
print('OK: barra única, 6 ações, cantos 8dp, catálogos alfabéticos, listas menores e políticas preservadas')
