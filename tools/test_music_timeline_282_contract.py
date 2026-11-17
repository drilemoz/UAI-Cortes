#!/usr/bin/env python3
"""Contrato estrutural do caminho à faixa separada. Áudio decodificado exige Android."""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
source = root / 'app/src/main/java/br/com/uaicortes'
read = lambda name: (source / name).read_text(encoding='utf-8')
editor = read('EditorScreen.kt')
timeline = read('UnifiedEditorTimeline.kt')
audio = read('QuickAudioDialog.kt')
preview = read('EditorTimelineUi.kt')
export = read('ExportEngine.kt')
model = read('ProjectStore.kt')
policy = read('ExportAudioPolicy.kt')
assert 'Triple("Música", Icons.Default.MusicNote, "music")' in editor
assert '"music" -> openQuickAudio("music")' in editor
assert 'musicMode = true' in editor and 'onOpenMusic={openQuickAudio("music")}' in editor
assert 'MusicAudioPreview(music, p.durationMs' in editor
assert 'listOf(video,images,overlays,audioSpans,listOfNotNull(musicSpan),voiceSpans,subtitles)' in timeline
assert 'TimelineKind.MUSIC -> onOpenMusic()' in timeline
assert 'TimelineKind.MUSIC->onOpenMusic()' in timeline and 'openTrack(i)' in timeline
assert 'fun laneHeightDp(kind: TimelineKind): Int' in timeline and 'private val rulerHeight = 22.dp' in timeline
assert 'detectDragGestures(onDragStart=' in timeline and 'val gripPx=with(density) { 18.dp.toPx() }' in timeline
assert 'current.copy(music = updated)' in audio and 'current.copy(music = null)' in audio
assert 'QuickAudioPlacement.resolve(lengths' in audio and 'media.protectedCopy' in audio
assert 'repeat = legacyLoop' in audio and 'j.optBoolean("repeat", true)' in model
assert 'ExportAudioPolicy.musicWindow(music, projectDurationMs)' in preview
assert 'ExportAudioPolicy.musicWindow(music, project.durationMs, sourceDuration)' in export
assert 'sequence.addGap(window.timelineStartMs * 1000L)' in export and 'sequence.addGap(window.trailingMs * 1000L)' in export
assert 'if (music.loops)' in export and 'setIsLooping(true)' in export
assert 'activeMusic(project) != null' in policy
assert (root / 'app/src/androidTest/java/br/com/uaicortes/MusicExportTimingAndroidTest.kt').is_file()
print('OK: música independente, pop-up, faixa compacta, janela de exportação e compatibilidade')
