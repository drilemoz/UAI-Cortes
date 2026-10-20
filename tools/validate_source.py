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
assert re.search(r'versionCode\s*=\s*37',gradle)
assert re.search(r'versionName\s*=\s*"0.1.36"',gradle)
assert 'Versão 0.1.36 (37)' in ui
for label in ["VERSION: '0.1.36'", "VERSION_CODE: '37'", 'TAG="v$VERSION"',
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

# v0.1.25: coordenadas relativas da legenda na prévia, editor visual e busca fina.
precision=(root/'app/src/main/java/br/com/uaicortes/TimelinePrecision.kt').read_text()
assert 'SubtitleOverlayOnFrame(cue, editable = false)' in ui
assert 'SubtitlePositionPreview(p,selected,' in ui
assert 'SubtitlePlacement.anchorPixels(' in ui
assert 'SubtitlePlacement.drag(' in ui
assert 'TimelinePrecision.step(' in ui and 'seekProjectPosition(player,p.clips,dest)' in ui
assert 'Modifier.offset(x=((cue.x-.5f)*240).dp' not in ui
assert 'onDragEnd' in ui and 'latestCommit(draftX,draftY)' in ui
assert 'setOverlayFrameAnchor(0f, -1f)' in fx
assert 'fun sourceAt(' in precision and 'fun anchorPixels(' in precision
assert (root/'app/src/test/java/br/com/uaicortes/TimelinePrecisionTest.kt').exists()
print('OK: v0.1.25 / versionCode 26, precisão da timeline e prévia de legendas proporcional')

# v0.1.26: seleção de legenda por ID e arrasto independente de início/centro/fim.
subtitle=(root/'app/src/main/java/br/com/uaicortes/SubtitleTimelineEdit.kt').read_text()
assert 'SubtitleOverviewTimeline(p.subtitles,p.durationMs,onSelectSubtitle,' in ui
assert 'SubtitleTimelineEditor(listOf(selected),p.durationMs,selectedId,' in ui
assert 'selectedSubtitleId = id; navTo("subtitles")' in ui
assert all(x in ui for x in ['SubtitleTimelineEdit.move(', 'SubtitleTimelineEdit.trimLeft(', 'SubtitleTimelineEdit.trimRight(', 'onFinish=::commitDraft'])
assert all(x in subtitle for x in ['fun move(', 'fun trimLeft(', 'fun trimRight(', 'MIN_DURATION_MS = 100L'])
assert (root/'app/src/test/java/br/com/uaicortes/SubtitleTimelineEditTest.kt').exists()
print('OK: v0.1.26 / versionCode 27, seleção e arrasto temporal de legendas')

# v0.1.27: estilo visual persistente, prévia e exportação alinhadas.
styles=(root/'app/src/main/java/br/com/uaicortes/SubtitleStyles.kt').read_text()
assert 'val backgroundColor: Int = SubtitleStyles.DEFAULT_BACKGROUND' in model
assert '.put("backgroundColor", backgroundColor).put("bold", bold)' in model
assert 'j.optBoolean("bold", true)' in model
assert 'Color(cue.backgroundColor)' in ui and 'if (cue.bold) FontWeight.Bold' in ui
assert 'SubtitleStyles.presets.forEach' in ui and 'SubtitleStyles.applyToAll(' in ui
assert 'BackgroundColorSpan(active.backgroundColor)' in fx and 'StyleSpan(Typeface.BOLD)' in fx
assert (root/'app/src/test/java/br/com/uaicortes/SubtitleStylesTest.kt').exists()
print('OK: v0.1.27 / versionCode 28, estilos das legendas com prévia/exportação e compatibilidade legado')

# v0.1.28: fonte e contorno independentes de cada legenda, com migração opcional.
assert 'val fontFamily: String = SubtitleStyles.FONT_SANS' in model
assert 'val outlineWidth: Int = 0' in model
assert '.put("fontFamily", fontFamily)' in model and '.put("outlineColor", outlineColor)' in model
assert 'SubtitleStyles.normalizeFont(j.optString(' in model
assert 'j.optInt("outlineWidth", 0).coerceIn(0, 4)' in model
assert 'SubtitleStyles.fonts.forEach' in ui and 'Contorno:' in ui
assert 'subtitleFont(cue.fontFamily)' in ui and 'drawStyle=Stroke(' in ui
assert 'TypefaceSpan(family)' in fx and 'SubtitleOutlineSpan(' in fx
assert 'fontFamily = source.fontFamily' in styles and 'outlineWidth = source.outlineWidth' in styles
assert (root/'app/src/test/java/br/com/uaicortes/SubtitleTypographyTest.kt').exists()
print('OK: v0.1.28 / versionCode 29, fontes e contornos, JSON legado e cinco variantes no CI')

# v0.1.29: shadow, multiline leading, paragraph alignment and backward compatibility.
assert 'val shadowOffsetDp: Int = 0' in model and 'val lineSpacingPercent: Int = 100' in model
assert 'val textAlignment: String = SubtitleStyles.ALIGN_CENTER' in model
assert '.put("shadowOffsetDp", shadowOffsetDp)' in model
assert '.put("lineSpacingPercent", lineSpacingPercent)' in model
assert 'SubtitleStyles.normalizeAlignment(j.optString(' in model
assert 'j.optInt("shadowOffsetDp", 0).coerceIn(0, 6)' in model
assert 'SubtitleStyles.normalizeLineSpacing(j.optInt(' in model
assert all(x in ui for x in [
    'Sombra: ${selected.shadowOffsetDp}', 'Espaçamento entre linhas:',
    'SubtitleStyles.alignments.forEach', 'Shadow(', 'lineHeight=textLineHeight',
    'textAlign=alignment', 'Versão 0.1.36 (37)'
])
assert all(x in fx for x in ['class SubtitleShadowSpan(', 'class SubtitleLineSpacingSpan(',
    'AlignmentSpan.Standard(layoutAlign)', 'SubtitleStyles.ALIGN_LEFT -> left.toFloat()',
    'SubtitleStyles.ALIGN_RIGHT -> right - width', 'active.textAlignment),0,text.length,0)'])
assert all(x in styles for x in ['shadowOffsetDp = source.shadowOffsetDp',
    'lineSpacingPercent = source.lineSpacingPercent', 'textAlignment = source.textAlignment',
    'fun normalizeShadowColor(', 'fun normalizeLineSpacing(', 'fun normalizeAlignment('])
assert (root/'app/src/test/java/br/com/uaicortes/SubtitlePresentationTest.kt').exists()
assert "VERSION = '0.1.36'" in package_script and 'VERSION_CODE = 37' in package_script
assert 'versionCode 37' in gradle and 'isUniversalApk = true' in gradle
print('OK: versão atual 0.1.36 / versionCode 37, compatibilidade das legendas, JSON legado, APKs')

# build 26: Compose UI usa StrokeJoin, não PathJoin. Falha anterior em Debug/Release.
assert 'import androidx.compose.ui.graphics.StrokeJoin' in ui
assert 'join=StrokeJoin.Round' in ui
assert 'PathJoin' not in ui
print('OK: build 26 — tipo StrokeJoin.Round existente no Compose, sem PathJoin')

# v0.1.34: salvar acessível no editor e em Cortar e unir, inclusive rascunho válido.
assert 'LazyRow(state = toolsScroll' in ui and 'items(count = Int.MAX_VALUE)' in ui
assert 'Triple("Salvar", Icons.Default.Save, "save")' in ui
assert 'Salvar e voltar à Home' not in ui
assert 'TrimQuickAction(Icons.Default.Save, "Salvar"' in ui
assert 'TrimSaveLogic.applyPending(p, clip.id, begin, finish)' in ui
assert 'onSaveHome = { saved -> write(saved); screen = "home" }' in ui
assert (root/'app/src/test/java/br/com/uaicortes/TrimSaveLogicTest.kt').exists()
print('OK: versão atual 0.1.36 / versionCode 37, rolagem circular, salvar projeto e corte antes de ir à Home')

# v0.1.34: cortes retornam à metade direita, a faixa mostra partes selecionáveis,
# e um ponto de fonte persiste sem alterar esquema legado.
trim_edit=(root/'app/src/main/java/br/com/uaicortes/TrimTimelineEdit.kt').read_text()
assert 'private fun TrimSegmentStrip(' in ui and 'LazyRow(state = scroll' in ui
assert 'TrimSegmentFrame(item)' in ui and 'scroll.animateScrollToItem(selectedIndex)' in ui
assert 'TrimTimelineEdit.split(p, clip.id, cursorMs)' in ui
assert 'selected(result.selectedClipId)' in ui
assert 'TrimTimelineEdit.cursorFor(p, clip)' in ui
assert 'lastTrimCutClipId' in model and 'lastTrimCutSourceMs' in model
assert 'lastTrimCutClipId = newId' in trim_edit and 'lastTrimCutSourceMs = sourceMs' in trim_edit
assert 'EditFeatures.repositionAfterDelete(project, remaining)' in trim_edit
assert 'undoLastCut' in ui
assert (root/'app/src/test/java/br/com/uaicortes/TrimTimelineEditTest.kt').exists()
print('OK: versão atual 0.1.36 / versionCode 37, faixa de trechos, seleção e restauração do último corte')

# v0.1.34: uma timeline geral, lista virtualizada e vídeo fixo no topo.
assert 'private fun SubtitleCompactTimeline(' in ui
assert 'SubtitleCompactTimeline(sorted,p.durationMs,selectedId,' in ui
assert 'private fun SubtitlePositionPreview(' in ui and 'player=remember(p.id)' in ui
assert 'LazyColumn(state=captionsState' in ui and 'itemsIndexed(filtered,key=' in ui
assert 'if(selectedId==cue.id && editorOpen && selected!=null)' in ui
assert 'if(showAdvanced)' in ui and 'SubtitleTimelineEditor(listOf(selected)' in ui
assert 'SubtitleTimelineEditor(p.subtitles,p.durationMs' not in ui
assert 'p.subtitles.sortedBy { it.startMs }.forEachIndexed' not in ui
assert (root/'app/src/main/java/br/com/uaicortes/SubtitleCompactLayout.kt').exists()
assert (root/'app/src/test/java/br/com/uaicortes/SubtitleCompactLayoutTest.kt').exists()
print('OK: versão atual 0.1.36 / versionCode 37, timeline unica, video fixo e legendas compactas')

# v0.1.34: busca, navegação e exclusão segura no editor de legendas.
nav=(root/'app/src/main/java/br/com/uaicortes/SubtitleListNavigation.kt').read_text()
assert 'fun matches(' in nav and 'Normalizer.Form.NFD' in nav
assert 'fun adjacentIndex(' in nav and 'fun newInterval(' in nav
assert 'itemsIndexed(filtered,key=' in ui and 'Buscar texto da legenda' in ui
assert 'hasPrevious=SubtitleListNavigation.adjacentIndex' in ui
assert 'hasNext=SubtitleListNavigation.adjacentIndex' in ui
assert 'pendingDeleteId?.let' in ui and 'Excluir esta legenda?' in ui
assert 'SubtitleListNavigation.readSrtBounded(reader)' in ui
assert 'fun readSrtBounded(' in nav and 'maxCharacters + 1' in nav
assert (root/'app/src/test/java/br/com/uaicortes/SubtitleListNavigationTest.kt').exists()
print('OK: versão atual 0.1.36 / versionCode 37, busca e navegação, exclusão confirmada e limite SRT')

# v0.1.35: prévia se adapta ao aspecto/rotação em todas as telas de vídeo.
layout=(root/'app/src/main/java/br/com/uaicortes/AdaptiveVideoLayout.kt').read_text()
assert 'fun inline(' in layout and 'fun fit(' in layout and 'fun activeClipIndex(' in layout
assert 'InlineVideoPreview(aspect)' in ui
assert 'InlineVideoPreview(videoAspect)' in ui
assert 'InlineVideoPreview(canvasAspect)' in ui
assert 'InlineVideoPreview(exportPreviewAspect)' in ui
assert 'val fit=AdaptiveVideoLayout.fit(' in ui
assert 'previewSubtitleScale(cue,textLimitPx.toInt(),' in ui
assert 'frameWidthDp * .94f' in ui
assert (root/'app/src/test/java/br/com/uaicortes/AdaptiveVideoLayoutTest.kt').exists()
assert 'mediaAspect(context' in ui and 'METADATA_KEY_VIDEO_ROTATION' in ui
print('OK: v0.1.36 / versionCode 37, proporção da prévia e largura de legenda responsivas')

# Regressão CI 31: o callback de importação SRT usa searchQuery; a declaração
# de estado Compose deve aparecer ANTES do callback para resolver o identificador.
subtitle_screen=ui[ui.index('private fun SubtitleScreen('):]
state_declaration='var searchQuery by remember(p.id) { mutableStateOf("") }'
importer_declaration='val importer=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument())'
assert subtitle_screen.count(state_declaration)==1, 'searchQuery deve ser declarado uma única vez'
assert subtitle_screen.index(state_declaration)<subtitle_screen.index(importer_declaration), \
    'searchQuery deve existir antes do callback importer (erro de compilação no workflow 31)'
assert 'selectedId=cues.first().id\n                    searchQuery=""' in subtitle_screen
print('OK: regressão workflow 31 — estado da pesquisa declarado antes do callback de importação SRT')
