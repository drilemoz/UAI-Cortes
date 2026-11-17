#!/usr/bin/env python3
"""Contrato v0.1.279: Camadas (PiP + Marca d'água em abas). Checagem estrutural, não é build Android."""
from pathlib import Path
src = Path(__file__).resolve().parents[1] / 'app/src/main/java/br/com/uaicortes'
rd = lambda n: (src / n).read_text(encoding='utf-8')
main = rd('MainActivity.kt')
assert 'tabs = { LayersTabs("pip") { screen = "watermark" } }' in main
assert 'TextWatermarkScreen(current, ::back, { LayersTabs("watermark") { screen = "pip" } })' in main
assert len(main.splitlines()) < 800, 'MainActivity deve ficar abaixo de 800 linhas'
assert 'tabs: @Composable () -> Unit = {}, onApply' in rd('TextWatermarkScreen.kt')  # lambda final continua onApply
assert 'tabs()' in rd('PipScreen.kt') and 'tabs()' in rd('TextWatermarkScreen.kt')
bar = rd('EditorScreen.kt').split('val actions = remember { listOf(', 1)[1].split(') }', 1)[0]
assert bar.index('"Vozes IA"') < bar.index('"Camadas"') and 'Marca d' not in bar
print('OK: v0.1.279 Camadas com abas PiP e Marca d\'água')
