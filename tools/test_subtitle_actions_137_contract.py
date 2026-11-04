from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
FILE = ROOT / 'app' / 'src' / 'main' / 'java' / 'br' / 'com' / 'uaicortes' / 'SubtitleScreen.kt'
text = FILE.read_text(encoding='utf-8')

assert 'Icons.Default.Undo' in text, 'Ação de desfazer deve existir na barra principal das legendas.'
assert 'undoAvailable' in text and 'undoLatestChange()' in text, 'Desfazer deve ser centralizado em helper.'
assert 'Text("Desfazer")' not in text, 'Não deve manter botão textual Desfazer ocupando espaço na lista.'
assert 'Legenda: ${formatPipMs(selected.startMs)} – ${formatPipMs(selected.endMs)}' in text, 'Resumo da legenda selecionada deve aparecer acima da timeline.'
assert 'Text("${formatPipMs(cue.startMs)}–${formatPipMs(cue.endMs)}"' not in text, 'Tempo duplicado não deve permanecer na barra do player.'
print('ok')
