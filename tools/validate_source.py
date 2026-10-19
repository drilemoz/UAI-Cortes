#!/usr/bin/env python3
"""Verificações estruturais: não substituem compilação Android ou testes físicos."""
from pathlib import Path
import re
root=Path(__file__).resolve().parents[1]
source=(root/'app/src/main/java/br/com/uaicortes/ExportEngine.kt').read_text()
ui=(root/'app/src/main/java/br/com/uaicortes/MainActivity.kt').read_text()
model=(root/'app/src/main/java/br/com/uaicortes/ProjectStore.kt').read_text()
placement=(root/'app/src/main/java/br/com/uaicortes/OverlayPlacement.kt').read_text()
browser=(root/'app/src/main/java/br/com/uaicortes/MediaBrowser.kt').read_text()
workflow=(root/'.github/workflows/android-apk.yml').read_text()
gradle=(root/'app/build.gradle.kts').read_text()
assert re.search(r'versionCode\s*=\s*25',gradle)
assert re.search(r'versionName\s*=\s*"0.1.24"',gradle)
assert 'Versão 0.1.24 (25)' in ui
for label in ["VERSION: '0.1.24'", "VERSION_CODE: '25'", 'TAG="v$VERSION"',
              'gh release upload', 'tools/package_apks.py', 'arm32 arm64 x86 x86_64 universal']:
    assert label in workflow, label
assert 'actions/upload-artifact@v4' not in workflow
assert 'tags: [' not in workflow
assert 'assembleDebug' not in workflow
assert all(v in gradle for v in ['isUniversalApk = true',
                                 '"armeabi-v7a", "arm64-v8a", "x86", "x86_64"',
                                 'isEnable = true'])
package_script=(root/'tools/package_apks.py').read_text()
assert 'output-metadata.json' in package_script and 'apksigner' in package_script
assert 'len(dist_apks) != 5' in package_script
assert 'platforms;android-36' in workflow and 'android-actions/setup-android@v4' in workflow
assert 'abortOnError = false' not in gradle
assert 'setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)' in ui
assert '@OptIn(ExperimentalMaterial3Api::class)' in browser and 'PrimaryTabRow(' in browser
assert 'READ_MEDIA_VISUAL_USER_SELECTED' in browser
assert 'MAX_PIPS = 4' in placement and 'intersects' in placement and 'arrange' in placement
assert 'overlays: List<Overlay>' in model and 'optJSONObject("pip")' in model
assert 'schemaVersion' in model and 'JSONArray()' in model and 'overlapWith' in model
assert 'OverlayTimeline(' in ui and 'p.overlays.forEach' in ui
assert 'ModalBottomSheet(' in ui and 'rememberModalBottomSheetState(skipPartiallyExpanded = true)' in ui
assert 'dragHandle = null' in ui and 'onDismissRequest = { expanded = null }' in ui
assert 'scrimColor = Color.Black.copy(alpha = 0.12f)' in ui
assert 'Modifier.weight(1f).aspectRatio(1f)' in ui
assert 'options.chunked(2)' not in ui and 'AnimatedVisibility(' not in ui
assert 'PipPopup(' not in ui and 'AlertDialog(' in ui
assert 'onChange(pip.copy(scale = ' in ui and 'onChange(pip.copy(muted = ' in ui
assert 'MediaBrowser' in ui and 'insertOverlay(' in ui
assert 'PipCompositor(private val overlays: List<Overlay>)' in source
assert 'setVideoCompositorSettings(PipCompositor(project.overlays.take(4)))' in source
assert 'sequences += mainSequence' in source and 'pip.muted' in source
assert 'import androidx.media3.common.VideoCompositorSettings' in source
assert 'import androidx.media3.common.OverlaySettings' in source
assert 'EditableSeekTime(' in ui and 'TimeDigitsField(' in ui
assert 'OverlayTiming.nextStart(' in ui and 'pendingSequential' in ui and 'pendingOverlap' in ui
assert 'OverlayPlacement.collisions(' in ui and 'overlapWith' in ui
assert 'dragHandle = null' in ui
print('OK: 0.1.24 / versionCode 25, PiP sequencial, confirmação de colisão, bottom sheet compacto e CI')

assert 'private fun TrimScreen(' in ui and 'TrimThumbnails(' in ui and 'RangeSlider(' not in ui
assert 'TimeDigitsField(' in ui and 'NumericTimeMask' in ui and 'trimLogic' not in ui
assert 'Marcar início' in ui and 'Marcar fim' in ui and 'Aplicar corte' in ui
assert 'NumericTime.format(' in ui and 'TimeDigitsField("Começa em"' in ui
print('OK: 0.1.24 / versionCode 25, editor de cortes com prévia, horários com máscara numérica, PiP preservado')

assert 'TimeDigitsField("Tempo", cursorMs, total, autoApply = true' in ui
assert 'TrimQuickAction(Icons.Default.ContentCut' in ui
assert 'TrimQuickAction(Icons.Default.Undo' in ui
assert 'TrimQuickAction(Icons.Default.Delete' in ui
assert 'undoClips' in ui and 'rememberUndo()' in ui
assert 'Para marcar fim, avance o vídeo' in ui
print('OK: v0.1.24 sem barra sobre miniaturas, tempo automático, ações compactas e desfazer de clipes')

assert 'repeatMode = Player.REPEAT_MODE_ONE' in ui
assert 'repeatMode = Player.REPEAT_MODE_ALL' in ui
assert 'PlaybackSpeedButton(playbackSpeed' in ui
assert 'Mini tutorial · Cortar e unir' in ui
assert 'PreviewPlayback.SPEEDS' in ui
assert 'setPlaybackParameters(PlaybackParameters(playbackSpeed))' in ui
assert 'if (cursorMs >= total) player.pause()' not in ui
print('OK: loop automático corte/editor, velocidades de prévia e mini tutorial após as ações')

# Recursos novos e compatibilidade de metadados, sem alegar build Android.
fx=(root/'app/src/main/java/br/com/uaicortes/VideoEffects.kt').read_text()
features=(root/'app/src/main/java/br/com/uaicortes/EditFeatures.kt').read_text()
extract=(root/'app/src/main/java/br/com/uaicortes/AudioExtractionEngine.kt').read_text()
assert '.put("schemaVersion", 4)' in model
for token in ['subtitles: List<SubtitleCue>','transitions: List<ClipTransition>','audioTracks: List<AudioTrack>']:
    assert token in model, token
for token in ['ClipSheetAction(', 'TimelineTimedRow(', 'private fun SubtitleScreen(',
              'private fun TransitionScreen(', 'AudioExtractionEngine(context)',
              'player.volume = p.clips', 'Remover este clipe?', 'TrackAudioPreview(']:
    assert token in ui, token
assert 'TimedSubtitleOverlay' in fx and 'ClipFadeEffect' in fx
assert 'EditFeatures.parseSrt' in ui and 'setRemoveVideo(true)' in extract
assert 'project.audioTracks.forEach' in source and 'TimedSubtitleOverlay(project.subtitles' in source
print('OK: v0.1.24 funcionalidades estruturais, schema 4, SRT, fades e extração M4A')

# Regressões detectadas no build GitHub Actions 15 (não valida tipos Kotlin).
assert '@kotlin.OptIn(ExperimentalMaterial3Api::class)\n@Composable\nprivate fun PipScreen' in ui
assert '@kotlin.OptIn(ExperimentalMaterial3Api::class)\n@Composable\nprivate fun EditorScreen' in ui
assert 'val timelineWidth = maxWidth' in ui and 'Modifier.width(timelineWidth*fraction)' in ui
assert 'Modifier.width(maxWidth*fraction)' not in ui
print('OK: regressões build 15 — opt-in Material3 e largura da timeline corrigidos')

# Build 16: todas as chamadas Media3 instáveis da prévia ficam isoladas
# em um helper AndroidX @OptIn. Isso evita o lint nas lambdas AndroidView.
assert '@OptIn(UnstableApi::class)\nprivate fun previewPlayer(' in ui
assert ui.count('resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT') == 1
assert ui.count('setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)') == 1
assert 'previewPlayer(it, transparentShutter = true)' in ui
print('OK: build 16 Lint Media3 isolado e 5 saídas ABI/universal Release sem APK duplicado')

assert 'private fun TransitionFramePreview(' in ui
assert 'TransitionFramePreview(p.clips[boundary]' in ui
print('OK: prévia animada com quadros reais de transições')

# v0.1.23: recorte de faixas adicionais entre origem, posição e exportação.
logic=(root/'app/src/main/java/br/com/uaicortes/AudioTrackTiming.kt').read_text()
assert 'sourceStartMs: Long = 0L' in model
assert '.put("sourceStartMs", sourceStartMs)' in model
assert 'j.optLong("sourceStartMs", 0L)' in model
assert 'AudioTrackTiming.valid(' in ui and 'Salvar corte' in ui and 'Ouvir corte' in ui
assert 'sourceStartMs=editSourceStart,durationMs=editDuration' in ui
assert 'audio.sourceStartMs' in ui and 'sourceStartMs+(projectMs-startMs)' in ui
assert '.setStartPositionMs(sourceStart)' in source
assert '.setEndPositionMs(sourceStart + playable)' in source
assert 'MIN_TRIM_MS = 250L' in logic and 'fun initialRange(' in logic
print('OK: v0.1.24 recorte de áudio, persistência legado, prévia e exportação com deslocamento de origem')

# v0.1.24: arrasto da faixa na timeline principal e alças de recorte.
gesture=(root/'app/src/main/java/br/com/uaicortes/AudioTimelineEdit.kt').read_text()
assert 'private fun InteractiveAudioTimeline(' in ui
assert 'private fun AudioTrimGestureTimeline(' in ui
assert 'onSelectAudio = { id ->' in ui
assert 'onMoveTrack = { trackId, start ->' in ui
assert 'AudioTimelineEdit.move(' in ui
assert 'AudioTimelineEdit.left(' in ui
assert 'AudioTimelineEdit.right(' in ui
assert 'currentFinish()' in ui and 'AudioTimelineEdit.pixelsToMs(' in ui
assert all(x in gesture for x in ['fun move(', 'fun left(', 'fun right(', 'fun pixelsToMs('])
assert (root/'app/src/test/java/br/com/uaicortes/AudioTimelineEditTest.kt').exists()
print('OK: v0.1.24 / versionCode 25, timeline de áudio arrastável, seleção e alças visuais com limites')
