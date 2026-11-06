#!/usr/bin/env python3
from pathlib import Path
r=Path(__file__).resolve().parents[1]
s=r/'app/src/main/java/br/com/uaicortes'
time=(s/'EditorTimelineUi.kt').read_text()
pip=(s/'PipScreen.kt').read_text()
layout=(s/'OverlayEditorLogic.kt').read_text()
timeline=(s/'UnifiedEditorTimeline.kt').read_text()
transitions=(s/'TransitionScreen.kt').read_text()
project=(s/'ProjectStore.kt').read_text()
assert 'digits = "" // uma nova digitação' not in time
assert 'entry.copy(selection=androidx.compose.ui.text.TextRange(0,entry.text.length))' in time
assert 'Typed' not in time or 'onValueChange = { typed ->' in time
assert 'MAX_SCALE = 1.50f' in layout
assert 'it in 5..150' in layout
assert 'OverlayEditorLogic.FILL' in pip and 'Text("Preencher")' in pip
assert '.put("fitMode",OverlayEditorLogic.fitMode(fitMode))' in project
assert 'visibleRows=tracks.indices.filter { it == 0 || lanes[it].isNotEmpty() }' in timeline
assert 'visibleRows.forEach { index ->' in timeline
assert 'DropdownMenu(expanded=addMenu' in timeline
assert 'val featured = listOf(' in transitions and 'showMore' in transitions
assert 'TransitionFramePreview(' in transitions
assert 'schemaVersion", 4' in project
print('OK: entrada não destrutiva, PiP 150%, enquadramento, faixas compactas e transições visuais')
