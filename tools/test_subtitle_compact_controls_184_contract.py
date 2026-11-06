#!/usr/bin/env python3
"""Contratos da barra compacta e filtro sem ocultar faixas (complementam Android)."""
from pathlib import Path
r=Path(__file__).resolve().parents[1]
s=(r/'app/src/main/java/br/com/uaicortes/SubtitleScreen.kt').read_text()
t=(r/'app/src/main/java/br/com/uaicortes/EditorTimelineUi.kt').read_text()
assert 'Text("Linha do tempo"' in s
assert 'onToggleFollow: () -> Unit' in s and 'onToggleCompact: () -> Unit' in s
assert 'Icon(if(followPlayback) Icons.Default.Sync' in s
assert 'Icon(if(compactPreview) Icons.Default.Fullscreen' in s
assert 'onTimelineHelp: () -> Unit' in s
assert 'DropdownMenu(expanded=zoomMenuExpanded' in s
assert 'DropdownMenu(expanded=speakerFilterMenu' in s
assert 'DropdownMenu(expanded=moreSubtitleTools' in s
assert 'DropdownMenu(expanded=autoSubtitlesMenu' in s
assert 'SubtitleCompactTimeline(sorted, p.clips, p.durationMs, selectedId, cursorMs,' in s
assert 'emphasizedIds = filtered.map { it.id }.toSet()' in s
assert 'val emphasized = emphasizedIds == null' in t
assert 'alpha=if(emphasized) 1f else .28f' in t
assert 'if (filtered.none { it.id == id })' in s
assert 'followPlayback=false' in s and 'PointerEventType.Press' in s
assert 'Text("Em reprodução: "' not in s
assert '"Legenda: ${formatPipMs(selected.startMs)}' not in s
assert 'deleteAllConfirm=true' in s and 'DropdownMenuItem(text={Text("Apagar todas")}' in s
assert 'onSaveEditor' in s and 'SubtitleListNavigation.newInterval(p.durationMs,cursorMs)' in s
assert 'schemaVersion", 4' in (r/'app/src/main/java/br/com/uaicortes/ProjectStore.kt').read_text()
print('OK: player compacto, menus acessíveis, timeline integral, filtros visuais e edição preservada')
