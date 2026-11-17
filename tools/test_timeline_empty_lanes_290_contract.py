#!/usr/bin/env python3
from pathlib import Path
src = Path(__file__).resolve().parents[1] / "app/src/main/java/br/com/uaicortes"
timeline = (src / "UnifiedEditorTimeline.kt").read_text()
editor = (src / "EditorScreen.kt").read_text()
ui = (src / "EditorTimelineUi.kt").read_text()
speed = ui.split("internal fun PlaybackSpeedButton(", 1)[1].split("@Composable", 1)[0]
assert 'laneOrder.filter { it in filled }' in timeline
assert 'it !in filled && it != TimelineKind.MUSIC' in timeline
assert 'listOf(TimelineKind.MUSIC).filter { it !in filled }' in timeline
assert 'orderedLanes(filled).map' in timeline and 'rowHeights.sum()' in timeline
assert 'if (kind !in filled) TimelineEmptyLane(' in timeline
assert 'val currentCommit by rememberUpdatedState(onCommit)' in timeline
assert 'val filled = kinds.filterIndexed { index, _ -> lanes[index].isNotEmpty() }' in timeline
assert 'horizontalScroll(scroll)' in timeline and 'verticalScroll(verticalScrollState)' in timeline
assert 'transitions=if(kind == transitionLane)project.clips' in timeline
assert '"Adicionar PiP"' in timeline and '"Adicionar áudio"' in timeline and '"Abrir Legendas"' in timeline
assert 'Velocidade ${PreviewPlayback.label(speed)}' in speed
assert 'Icons.Default.Remove' not in speed and 'Icons.Default.Add' not in speed
assert 'heightIn(min = 48.dp)' in speed
assert 'Apenas reprodução; não altera o MP4' in speed and 'Restaurar 1×' in speed
assert 'PlaybackSpeedButton(playbackSpeed, onSelect = playbackVm::updateSpeed)' in editor
print("OK: pistas vazias acessíveis e ordenadas, rolagem limitada, seletor compacto")
