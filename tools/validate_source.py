#!/usr/bin/env python3
"""Verificações estruturais: não substituem compilação Android ou testes físicos."""
from pathlib import Path
import re
root=Path(__file__).resolve().parents[1]
source=(root/'app/src/main/java/br/com/uaicortes/ExportEngine.kt').read_text()
# O código de interface passou a morar em arquivos por funcionalidade.
ui_files = [
    'MainActivity.kt', 'HomeScreens.kt', 'TextWatermarkScreen.kt', 'EditorScreen.kt',
    'EditorTimelineUi.kt', 'TrimScreen.kt', 'AudioScreen.kt', 'PipScreen.kt',
    'TransitionScreen.kt', 'SubtitleScreen.kt', 'ExportScreen.kt', 'PreviewMediaUtils.kt'
]
ui='\n'.join((root/'app/src/main/java/br/com/uaicortes'/name).read_text() for name in ui_files)
assert all((root/'app/src/main/java/br/com/uaicortes'/name).exists() for name in ui_files)
assert (root/'app/src/main/java/br/com/uaicortes/MainActivity.kt').read_text().count('fun UaiApp()') == 1

model=(root/'app/src/main/java/br/com/uaicortes/ProjectStore.kt').read_text()
placement=(root/'app/src/main/java/br/com/uaicortes/OverlayPlacement.kt').read_text()
browser=(root/'app/src/main/java/br/com/uaicortes/MediaBrowser.kt').read_text()
workflow=(root/'.github/workflows/android-apk.yml').read_text()
gradle=(root/'app/build.gradle.kts').read_text()
assert re.search(r'versionCode\s*=\s*69',gradle)
assert re.search(r'versionName\s*=\s*"0.1.68"',gradle)
assert 'AppReleaseNotes.VERSION_NAME' in (root/'app/src/main/java/br/com/uaicortes/HomeScreens.kt').read_text()
for label in ["VERSION: '0.1.68'", "VERSION_CODE: '69'", 'TAG="v$VERSION"',
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
assert '@OptIn(ExperimentalMaterial3Api::class)' in browser and 'LazyVerticalGrid(' in browser
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
assert 'OverlayTiming.nextStart(' in (root/'app/src/main/java/br/com/uaicortes/OverlayOperations.kt').read_text() and 'pendingSequential' in ui and 'pendingOverlap' in ui
assert 'OverlayPlacement.collisions(' in (root/'app/src/main/java/br/com/uaicortes/OverlayOperations.kt').read_text() and 'overlapWith' in (root/'app/src/main/java/br/com/uaicortes/OverlayOperations.kt').read_text()
assert 'dragHandle = null' in ui
print('OK: 0.1.24 / versionCode 25, PiP sequencial, confirmação de colisão, bottom sheet compacto e CI')

assert 'internal fun TrimScreen(' in ui and 'TrimThumbnails(' in ui and 'RangeSlider(' not in (root/'app/src/main/java/br/com/uaicortes/TrimScreen.kt').read_text()
assert 'TimeDigitsField(' in ui and 'NumericTimeMask' in ui and 'trimLogic' not in ui
assert 'Marcar início' in ui and 'Marcar fim' in ui and 'Aplicar corte' in ui
assert 'NumericTime.format(' in ui and 'TimeDigitsField("Início"' in ui
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
for token in ['ClipSheetAction(', 'TimelineTimedRow(', 'internal fun SubtitleScreen(',
              'internal fun TransitionScreen(', 'AudioExtractionEngine(context)',
              'player.volume = p.clips', 'Remover este clipe?', 'TrackAudioPreview(']:
    assert token in ui, token
assert 'TimedSubtitleOverlay' in fx and 'ClipFadeEffect' in fx
assert 'EditFeatures.parseSrt' in ui and 'setRemoveVideo(true)' in extract
assert 'project.audioTracks.forEach' in source and 'TimedSubtitleOverlay(project.subtitles' in source
print('OK: v0.1.24 funcionalidades estruturais, schema 4, SRT, fades e extração M4A')

# Regressões detectadas no build GitHub Actions 15 (não valida tipos Kotlin).
assert '@kotlin.OptIn(ExperimentalMaterial3Api::class)\n@Composable\ninternal fun PipScreen' in ui
assert '@kotlin.OptIn(ExperimentalMaterial3Api::class)\n@Composable\ninternal fun EditorScreen' in ui
assert 'val timelineWidth = maxWidth' in ui and 'Modifier.width(timelineWidth*fraction)' in ui
assert 'Modifier.width(maxWidth*fraction)' not in ui
print('OK: regressões build 15 — opt-in Material3 e largura da timeline corrigidos')

# Build 16: todas as chamadas Media3 instáveis da prévia ficam isoladas
# em um helper AndroidX @OptIn. Isso evita o lint nas lambdas AndroidView.
assert '@OptIn(UnstableApi::class)\ninternal fun previewPlayer(' in ui
assert ui.count('resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT') == 1
assert ui.count('setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)') == 1
assert 'previewPlayer(it, transparentShutter = true)' in ui
print('OK: build 16 Lint Media3 isolado e 5 saídas ABI/universal Release sem APK duplicado')

assert 'internal fun TransitionFramePreview(' in ui
assert 'TransitionFramePreview(p.clips[safeBoundary]' in ui
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
assert 'internal fun InteractiveAudioTimeline(' in ui
assert 'internal fun AudioTrimGestureTimeline(' in ui
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
assert 'SubtitleOverlayOnFrame(cue, editable = false, playbackMs=currentMs)' in ui
assert 'SubtitlePositionPreview(p,selected,' in ui
assert 'SubtitlePlacement.anchorPixels(' in ui
assert 'SubtitlePlacement.drag(' in ui
assert 'TimelinePrecision.step(' in ui and 'seekGlobal(dest)' in ui
assert 'Modifier.offset(x=((cue.x-.5f)*240).dp' not in ui
assert 'onDragEnd' in ui and 'latestCommit(draftX,draftY)' in ui
assert 'setOverlayFrameAnchor(0f,-1f)' in fx
assert 'fun sourceAt(' in precision and 'fun anchorPixels(' in precision
assert (root/'app/src/test/java/br/com/uaicortes/TimelinePrecisionTest.kt').exists()
print('OK: v0.1.25 / versionCode 26, precisão da timeline e prévia de legendas proporcional')

# v0.1.26: seleção de legenda por ID e arrasto independente de início/centro/fim.
subtitle=(root/'app/src/main/java/br/com/uaicortes/SubtitleTimelineEdit.kt').read_text()
assert 'SubtitleOverviewTimeline(p.subtitles,p.durationMs,onSelectSubtitle,' in ui
assert 'internal fun SubtitleEditDialog(' in ui
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
assert 'SubtitleStyles.presets.forEach' in ui and 'SubtitleStyles.applySelectedToAll(' in ui
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
    'Sombra: ${draft.shadowOffsetDp}', 'Espaçamento:',
    'SubtitleStyles.alignments.forEach', 'Shadow(', 'lineHeight=textLineHeight',
    'textAlign=alignment'
])
assert all(x in fx for x in ['class SubtitleShadowSpan(', 'class SubtitleLineSpacingSpan(',
    'AlignmentSpan.Standard(layoutAlign)', 'SubtitleStyles.ALIGN_LEFT -> left.toFloat()',
    'SubtitleStyles.ALIGN_RIGHT -> right - width', 'active.textAlignment),0,text.length,0)'])
assert all(x in styles for x in ['shadowOffsetDp = source.shadowOffsetDp',
    'lineSpacingPercent = source.lineSpacingPercent', 'textAlignment = source.textAlignment',
    'fun normalizeShadowColor(', 'fun normalizeLineSpacing(', 'fun normalizeAlignment('])
assert (root/'app/src/test/java/br/com/uaicortes/SubtitlePresentationTest.kt').exists()
assert "VERSION = '0.1.68'" in package_script and 'VERSION_CODE = 69' in package_script
assert 'versionCode 69' in gradle and 'isUniversalApk = true' in gradle
print('OK: versão atual 0.1.64 / versionCode 69, compatibilidade das legendas, JSON legado, APKs')

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
assert 'onSaveHome = { saved -> if (write(saved)) screen = "home" }' in ui
assert (root/'app/src/test/java/br/com/uaicortes/TrimSaveLogicTest.kt').exists()
print('OK: versão atual 0.1.64 / versionCode 69, rolagem circular, salvar projeto e corte antes de ir à Home')

# v0.1.34: cortes retornam à metade direita, a faixa mostra partes selecionáveis,
# e um ponto de fonte persiste sem alterar esquema legado.
trim_edit=(root/'app/src/main/java/br/com/uaicortes/TrimTimelineEdit.kt').read_text()
assert 'internal fun TrimSegmentStrip(' in ui and 'LazyRow(state = scroll' in ui
assert 'TrimSegmentFrame(item)' in ui and 'scroll.animateScrollToItem(selectedIndex)' in ui
assert 'TrimTimelineEdit.split(p, clip.id, cursorMs)' in ui
assert 'selected(result.selectedClipId)' in ui
assert 'TrimTimelineEdit.cursorFor(p, clip)' in ui
assert 'lastTrimCutClipId' in model and 'lastTrimCutSourceMs' in model
assert 'lastTrimCutClipId = newId' in trim_edit and 'lastTrimCutSourceMs = sourceMs' in trim_edit
assert 'EditFeatures.repositionAfterDelete(project, remaining)' in trim_edit
assert 'undoLastCut' in ui
assert (root/'app/src/test/java/br/com/uaicortes/TrimTimelineEditTest.kt').exists()
print('OK: versão atual 0.1.64 / versionCode 69, faixa de trechos, seleção e restauração do último corte')

# v0.1.34: uma timeline geral, lista virtualizada e vídeo fixo no topo.
assert 'internal fun SubtitleCompactTimeline(' in ui
assert 'SubtitleCompactTimeline(sorted,p.durationMs,selectedId,' in ui
assert 'internal fun SubtitlePositionPreview(' in ui and 'player=remember(p.id)' in ui
assert 'LazyColumn(state=captionsState' in ui and 'itemsIndexed(filtered,key=' in ui
assert 'SubtitleEditDialog(cue=selected,totalMs=p.durationMs' in ui
assert 'internal fun SubtitleEditDialog(' in ui and 'TimeDigitsField("Início",draft.startMs,totalMs' in ui
assert 'SubtitleTimelineEditor(p.subtitles,p.durationMs' not in ui
assert 'p.subtitles.sortedBy { it.startMs }.forEachIndexed' not in ui
assert (root/'app/src/main/java/br/com/uaicortes/SubtitleCompactLayout.kt').exists()
assert (root/'app/src/test/java/br/com/uaicortes/SubtitleCompactLayoutTest.kt').exists()
print('OK: versão atual 0.1.64 / versionCode 69, timeline unica, video fixo e legendas compactas')

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
print('OK: versão atual 0.1.64 / versionCode 69, busca e navegação, exclusão confirmada e limite SRT')

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
print('OK: v0.1.64 / versionCode 69, proporção da prévia e largura de legenda responsivas')

# Regressão CI 31: o callback de importação SRT usa searchQuery; a declaração
# de estado Compose deve aparecer ANTES do callback para resolver o identificador.
subtitle_screen=ui[ui.index('internal fun SubtitleScreen('):]
state_declaration='var searchQuery by remember(p.id) { mutableStateOf("") }'
importer_declaration='val importer=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument())'
assert subtitle_screen.count(state_declaration)==1, 'searchQuery deve ser declarado uma única vez'
assert subtitle_screen.index(state_declaration)<subtitle_screen.index(importer_declaration), \
    'searchQuery deve existir antes do callback importer (erro de compilação no workflow 31)'
assert 'selectedId=cues.first().id\n                    searchQuery=""' in subtitle_screen
print('OK: regressão workflow 31 — estado da pesquisa declarado antes do callback de importação SRT')

# v0.1.37: pop-up compacto, rascunho isolado e efeitos alinhados com exportador.
animations=(root/'app/src/main/java/br/com/uaicortes/SubtitleAnimations.kt').read_text()
assert 'val color: Int = -1, val sizeSp: Int = 12' in model
assert 'j.optInt("sizeSp", 22)' in model
assert 'val animation: String = SubtitleAnimations.NONE' in model
assert '.put("animation", animation)' in model
assert 'SubtitleAnimations.normalize(j.optString(' in model
assert 'SubtitleStyles.applySelectedToAll(updated,draft,copyOptions)' in ui
assert 'Dialog(onDismissRequest=onDismiss' in ui
assert 'if(confirmAll) AlertDialog(' in ui
assert 'TimeDigitsField("Fim",draft.endMs,totalMs' in ui
assert 'SubtitleAnimations.frame(cue,playbackMs)' in ui
assert 'SubtitleAnimations.frame(it,' in fx
assert 'setScale(frame?.scale' in fx and 'setAlphaScale(if (active == null' in fx
assert 'internal fun SubtitleEditDialog(' in ui and 'onSave(draft,options)' in ui
assert 'fun splitPhrases(' in animations and 'KARAOKE' in animations
assert (root/'app/src/test/java/br/com/uaicortes/SubtitleAnimationsTest.kt').exists()
print('OK: 0.1.52 / 50, editor modal, rascunho, cópia seletiva e animações no Compose/Media3')

# v0.1.39: Salvar/Home em Legendas, efeitos circulares e áudio compacto com confirmação.
assert 'onSaveHome = { if (write(it)) screen = "home" }' in ui
assert 'Text("Salvar",fontSize=11.sp,maxLines=1)' in ui
assert 'internal fun SubtitleScreen(p:VideoProject' in ui and 'onSaveHome: () -> Unit, onAutoSubtitles: () -> Unit)' in ui
assert 'val carouselStart=CircularCarousel.startIndex(choices.size)' in ui
assert 'items(count=Int.MAX_VALUE)' in ui and 'CircularCarousel.optionIndex(index,choices.size)' in ui
assert 'Text("Faixas adicionais (${p.audioTracks.size})"' in ui
assert 'if(volumesExpanded)' in ui and 'if(trackVolumeExpandedId==track.id)' in ui
assert 'Remover faixa de áudio?' in ui and 'pendingAudioDeleteId=track.id' in ui
assert 'if(editingAudioId==id) editingAudioId=null' in ui
print('OK: v0.1.52 / 50, Salvar/Home em legendas, efeitos circulares, mixagem compacta e exclusão confirmada')

assert (root/'app/src/test/java/br/com/uaicortes/CircularCarouselTest.kt').exists()
print('OK: helper de índice circular e teste de limites incluídos')

# v0.1.39: origem Media3 mantém duração própria (não passar duração recortada).
policy=(root/'app/src/main/java/br/com/uaicortes/ExportTimingPolicy.kt').read_text()
assert 'EditedMediaItem.Builder(item).setDurationUs(clip.durationMs' not in source
assert '.setDurationUs(playable * 1000L)' not in source
assert '.setDurationUs(pip.durationMs * 1000L)' not in source
assert 'validateSources(project)' in source and 'checkReadable(clip.uri' in source
saver=(root/'app/src/main/java/br/com/uaicortes/GalleryVideoSaver.kt').read_text()
assert 'GalleryVideoSaver.save(context, file, fileName)' in source
assert 'GalleryVideoSaver.save(' in (root/'app/src/main/java/br/com/uaicortes/ImageAnimationEngine.kt').read_text()
assert 'copied == source.length()' in saver and 'source.isFile && source.length() > 0L' in saver
assert 'IS_PENDING, 0' in saver and 'resolver.delete(target, null, null)' in saver
assert 'describeExportError(exportException)' in source and 'error.errorCode' in source
assert 'fun validClip(' in policy and 'fun projectTime(' in policy
export=ui[ui.index('internal fun ExportScreen('):ui.index('/** Calcula proporção')]
assert 'SubtitleOverlayOnFrame(cue,editable=false,playbackMs=exportPreviewMs)' in export
assert 'PipLayer(pip,exportPreviewMs-pip.startAtMs,previewPlayer.isPlaying,' in export
assert 'TransitionPreviewTint(previous, fadeIn, next, fadeOut,' in export
assert (root/'app/src/test/java/br/com/uaicortes/ExportTimingPolicyTest.kt').exists()
print('OK: v0.1.39, origem real do clipe, mídias validadas, prévia com legendas/PiPs/fade')

# v0.1.52: Media3 TextOverlay nunca recebe texto com largura zero.
# A prova vem do TextOverlay.getBitmap do Media3 (StaticLayout.width -> Bitmap.createBitmap).
safety=(root/'app/src/main/java/br/com/uaicortes/SubtitleOverlayTextSafety.kt').read_text()
assert 'INVISIBLE_GLYPH = "\\u00A0"' in safety or 'INVISIBLE_GLYPH = "\u00A0"' in safety
assert 'fun bitmapSafeText(' in safety and 'text.isNotBlank()' in safety
assert 'SubtitleOverlayTextSafety.bitmapSafeText(rendered)' in fx
assert 'text.setSpan(ForegroundColorSpan(Color.TRANSPARENT)' in fx
assert 'SubtitleOverlayTextSafety.isVisible(frame?.text.orEmpty())' in fx
assert 'return SpannableString("")' not in fx
assert (root/'app/src/test/java/br/com/uaicortes/SubtitleOverlayTextSafetyTest.kt').exists()
print('OK: v0.1.52, regressão de texto vazio/bitmap 0px e transparência no Media3')

# v0.1.52: observabilidade offline opt-in e sem conteúdo de usuário.
diag=(root/'app/src/main/java/br/com/uaicortes/DiagnosticRecorder.kt').read_text()
privacy=(root/'app/src/main/java/br/com/uaicortes/DiagnosticPrivacy.kt').read_text()
dscreen=(root/'app/src/main/java/br/com/uaicortes/DiagnosticScreen.kt').read_text()
manifest=(root/'app/src/main/AndroidManifest.xml').read_text()
assert 'private const val MAX_BYTES = 512 * 1024' in diag
assert 'AtomicBoolean(false)' in diag and 'fun setEnabled(value: Boolean)' in diag
assert 'fun tap(' in diag and 'fun failure(' in diag and 'fun report()' in diag
assert 'DiagnosticPrivacy.touchGrid(' in diag and 'touchGrid(' in privacy
assert 'DiagnosticRecorder.initialize(applicationContext)' in ui
assert 'Modifier.pointerInput(screen)' in ui and 'PointerEventPass.Final' in ui
assert '"diagnostics" -> DiagnosticScreen(' in ui and 'Diagnóstico' in ui
assert 'ActivityResultContracts.CreateDocument("text/plain")' in dscreen
assert 'FileProvider.getUriForFile' in dscreen and 'androidx.core.content.FileProvider' in manifest
assert 'diagnostic_file_paths' in manifest and 'cache-path' in (root/'app/src/main/res/xml/diagnostic_file_paths.xml').read_text()
assert 'DiagnosticRecorder.exception("media3_transformer", error)' in source
assert 'DiagnosticRecorder.exception("media3_player", error)' in ui
assert 'schemaVersion' in model and '.put("schemaVersion", 4)' in model
print('OK: 0.1.52 (51), diagnóstico opt-in, toques, erros, compartilhamento local, sem mudar schema JSON')

# v0.1.52: proteção da área das legendas e confirmação pós-salvamento real.
export_sizing=(root/'app/src/main/java/br/com/uaicortes/SubtitleExportSizing.kt').read_text()
assert 'fun textBoxWidth(' in export_sizing and '0.90f' in export_sizing
assert 'fun relativeFontSize(' in export_sizing
assert 'override fun getBitmap(presentationTimeUs: Long)' in fx
assert 'StaticLayout.Builder.obtain(styled' in fx
assert 'SubtitleExportSizing.textBoxWidth(videoWidth)' in fx
assert 'setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE)' in fx
assert 'RelativeSizeSpan(relativeFontSize)' in fx
assert 'VideoSavedDialog(uri=savedUri' in export
assert 'savedVideo = uri' in (root/'app/src/main/java/br/com/uaicortes/ExportTaskViewModel.kt').read_text() and 'onSuccess = { uri ->' in (root/'app/src/main/java/br/com/uaicortes/ExportTaskViewModel.kt').read_text()
assert all(t in (root/'app/src/main/java/br/com/uaicortes/VideoSavedDialog.kt').read_text() for t in ['Vídeo salvo!', 'Abrir vídeo', 'Compartilhar'])
assert 'Voltar ao editor' in export and 'Filmes/UaiCortes' in (root/'app/src/main/java/br/com/uaicortes/VideoSavedDialog.kt').read_text()
assert (root/'app/src/test/java/br/com/uaicortes/SubtitleExportSizingTest.kt').exists()
print('OK: v0.1.52, legenda limitada ao quadro e confirmação em tela cheia após exportação')

# Regressão workflow 37: o resultado deve existir antes da observação pela tela.
# A partir da v0.1.64, o ViewModel gerencia ambos os estados, não remember local.
export_vm_source=(root/'app/src/main/java/br/com/uaicortes/ExportTaskViewModel.kt').read_text()
assert 'var savedName by mutableStateOf("")' in export_vm_source
assert 'var savedVideo by mutableStateOf<Uri?>(null)' in export_vm_source
assert export_vm_source.index('var savedVideo by') < export_vm_source.index('onSuccess = { uri ->')
assert export_vm_source.count('savedVideo = uri') == 1
assert 'val savedVideo = exportVm.savedVideo' in export
assert 'savedVideo?.let { savedUri ->' in export
print('OK: regressão workflow 37 — savedName/savedVideo declarados antes do callback e diálogo')

# v0.1.52: a galeria MediaStore é o seletor visual de todas as importações de vídeo/imagem.
assert 'openGallery("home", "new")' in ui
assert 'openGallery("editor", "append")' in ui
assert 'openGallery("pip", if (replace) "replace" else "add")' in ui
assert 'openGallery("audio", "audio_extract")' in ui
assert 'allowMultiple = mediaAction == "new" || mediaAction == "append"' in ui
assert 'MediaLibrary.load(context, false)' in browser and 'MediaLibrary.load(context, true)' in browser
assert 'GalleryRules.matches(' in browser and 'Galeria de vídeos' in browser
assert 'READ_MEDIA_VISUAL_USER_SELECTED' in browser
assert 'videoPicker.launch(arrayOf("video/*"))' in ui
assert (root/'app/src/test/java/br/com/uaicortes/MediaGalleryRulesTest.kt').is_file()
print('OK: v0.1.64 / versionCode 69, galeria visual unificada e seletor Android alternativo')

# v0.1.52: interface de exportação em tela cheia, sem porcentagem fictícia.
progress_ui=(root/'app/src/main/java/br/com/uaicortes/ExportProgressUi.kt').read_text()
assert 'Dialog(onDismissRequest = {}, properties = DialogProperties(' in ui
assert 'dismissOnBackPress = false' in ui and 'usePlatformDefaultWidth = false' in ui
assert 'ExportProgressUi.renderPercent(exportPhase,progress)' in ui
assert 'Tempo decorrido: ${ExportProgressUi.elapsedLabel(elapsedSeconds)}' in ui
assert 'ExportProgressUi.canCancel(exportPhase)' in ui
assert 'confirmCancelExport && working' in ui
assert 'savedVideo?.let { savedUri ->' in ui
assert 'onSaving = {' in export_vm_source and 'ExportPhase.SAVING' in (root/'app/src/main/java/br/com/uaicortes/ExportTaskCoordinator.kt').read_text()
assert 'fun progressOrNull(): Int?' in source
assert 'if (t.getProgress(progressHolder) == Transformer.PROGRESS_STATE_AVAILABLE)' in source
assert 'onSaving: () -> Unit = {}' in source and 'onSaving()' in source
assert all(v in progress_ui for v in ['PREPARING','RENDERING','SAVING','renderPercent(', 'canCancel('])
assert (root/'app/src/test/java/br/com/uaicortes/ExportProgressUiTest.kt').exists()
print('OK: 0.1.52 tela de exportação fullscreen, porcentagem real, cancelamento confirmado e salvamento diferenciado')

# v0.1.52: novas transições coloridas e aplicação explícita por junção / todas.
trans_fx=(root/'app/src/main/java/br/com/uaicortes/TransitionEffects.kt').read_text()
trans_batch=(root/'app/src/main/java/br/com/uaicortes/TransitionBatch.kt').read_text()
assert 'class ClipColorTransitionEffect(' in fx
assert 'videoEffects += ClipColorTransitionEffect(' in source
assert 'TransitionEffects.matrixAt(' in fx
assert 'fun composeValues(' in trans_fx and 'fun intensity(' in trans_fx
for kind in ('Flash branco','Flash preto','Fade azul','Fade dourado','Preto e branco','Sépia'):
    assert kind in trans_fx, kind
assert 'fun apply(' in trans_batch and 'if (applyAll) joins' in trans_batch
assert 'TransitionBatch.apply(p,safeBoundary,kind,fadeMs.toLong(),applyAll)' in ui
assert 'Aplicar a todas as junções' in ui and 'Text("Aplicar")' in ui
assert (root/'app/src/test/java/br/com/uaicortes/TransitionEffectsTest.kt').exists()
assert (root/'app/src/test/java/br/com/uaicortes/TransitionBatchTest.kt').exists()
print('OK: v0.1.52 transições RgbMatrix, seleção de rascunho, aplicar e aplicar a todas')

# v0.1.52: regressão - corrotinas canceladas não viram erros da galeria.
gallery = (root / 'app/src/main/java/br/com/uaicortes/MediaBrowser.kt').read_text()
gallery_rules = (root / 'app/src/main/java/br/com/uaicortes/MediaGalleryRules.kt').read_text()
assert 'catch (e: CancellationException)' in gallery
assert gallery.index('catch (e: CancellationException)') < gallery.index('catch (e: Exception)')
assert 'throw e' in gallery[gallery.index('catch (e: CancellationException)'):gallery.index('catch (e: SecurityException)')]
assert 'currentCoroutineContext().isActive' in gallery
assert 'GalleryLoadErrors.message(e)' in gallery and 'Tentar novamente' in gallery and 'Procurar no Android' in gallery
assert 'error = "Não foi possível carregar a galeria: ${e.localizedMessage' not in gallery
assert 'private uri' not in gallery_rules
print('OK: v0.1.52 - cancelamento Compose distinto de erros de galeria, retry e SAF')

# v0.1.52: cabeçalho do TXT deve apresentar versão instalada, não a string histórica 0.1.41.
diag = (root / 'app/src/main/java/br/com/uaicortes/DiagnosticRecorder.kt').read_text()
assert 'Uai Cortes $appVersion | diagnóstico local' in diag
assert 'Uai Cortes 0.1.41 | diagnóstico local' not in diag
print('OK: diagnóstico TXT mostra versão instalada, não valor legado fixo')

# v0.1.52: marca d'água de texto independente das legendas e persistente no projeto.
watermark = (root/'app/src/main/java/br/com/uaicortes/TextWatermark.kt').read_text()
assert 'data class TextWatermark(' in watermark and 'fun toSubtitle(durationMs: Long)' in watermark
assert 'opacity.coerceIn(0.1f, 1f)' in watermark and 'text.trim().take(80)' in watermark
assert 'val textWatermark: TextWatermark? = null' in model
assert '.put("textWatermark", textWatermark?.json())' in model
assert 'j.optJSONObject("textWatermark")?.let(TextWatermark::read)' in model
assert '"watermark" -> project?.let' in ui and 'internal fun TextWatermarkScreen(' in ui
assert "Triple(\"Marca d'água\", Icons.Default.TextFields, \"watermark\")" in ui
assert 'p.textWatermark?.toSubtitle(p.durationMs)' in ui
assert 'project.textWatermark?.toSubtitle(project.durationMs)' in source
assert 'textLayers += TimedSubtitleOverlay(listOf(watermarkCue), globalStartMs)' in source
assert 'OverlayEffect(textLayers)' in source
assert (root/'app/src/test/java/br/com/uaicortes/TextWatermarkTest.kt').exists()
print('OK: v0.1.64 / versionCode 69, marca de texto em prévia/editor/MP4 e JSON legado')

# v0.1.52: transcrição offline real e galeria de projetos segura.
auto=(root/'app/src/main/java/br/com/uaicortes/AutoSubtitleEngine.kt').read_text()
auto_ui=(root/'app/src/main/java/br/com/uaicortes/AutoSubtitleScreen.kt').read_text()
motion=(root/'app/src/main/java/br/com/uaicortes/ClipMotionTransitionEffect.kt').read_text()
assert 'com.alphacephei:vosk-android:0.3.75' in gradle
assert 'android.permission.INTERNET' in (root/'app/src/main/AndroidManifest.xml').read_text()
assert 'MediaExtractor()' in auto and 'MediaCodec.createDecoderByType' in auto
assert 'Recognizer(model, RATE.toFloat())' in auto and 'currentCoroutineContext().ensureActive()' in auto
assert 'vosk-model-small-pt-0.3.zip' in auto and 'MAX_EXTRACTED' in auto
assert 'Modelo de reconhecimento de voz' in auto_ui and 'Passo 2' in auto_ui and 'Passo 3' in auto_ui
assert 'SpeechModelSettingsScreen' in (root/'app/src/main/java/br/com/uaicortes/SpeechModelSettingsScreen.kt').read_text()
assert 'AutoSubtitleScreen(current' in ui and 'onAutoSubtitles' in ui
assert 'pendingProjectDelete' in ui and 'Excluir projeto?' in ui and 'projectSession.delete(target.id)' in ui
assert 'getScaledFrameAtTime' in ui and 'Miniatura do projeto' in ui
assert 'ClipMotionTransitionEffect(' in source and 'MatrixTransformation' in motion
assert 'ZOOM_IN' in (root/'app/src/main/java/br/com/uaicortes/TransitionEffects.kt').read_text()
print('OK: v0.1.52, ASR offline Vosk, tutorial/modelo, movimento em Media3, confirmação e miniaturas')

# v0.1.52: animação offline de imagens, sem modelos/APIs externas.
anim=(root/'app/src/main/java/br/com/uaicortes/AnimateImagesScreen.kt').read_text()
engine=(root/'app/src/main/java/br/com/uaicortes/ImageAnimationEngine.kt').read_text()
plan=(root/'app/src/main/java/br/com/uaicortes/ImageAnimationPlan.kt').read_text()
assert '"animate" -> AnimateImagesScreen' in ui
assert 'animationImages.addAll(animationDraftStore.load()?.images.orEmpty())' in ui
assert 'GalleryScope.IMAGES_ONLY' in browser and 'READ_MEDIA_IMAGES' in browser
assert 'setImageDurationMs(options.durationMs)' in engine and 'setFrameRate(options.fps)' in engine
assert 'AnimatedImageMatrix' in engine and 'MatrixTransformation' in engine
assert 'ImageAnimationPlan.frame' in anim and 'ImageAnimationPlan.frame' in engine
assert 'MediaStore.Video.Media.IS_PENDING' in saver and 'VIDEO_H264' in engine
assert 'Cancelar exportação' in anim and 'onUseInEditor' in anim and 'onOrderChanged' in anim
assert 'MAX_IMAGES = 80' in plan and 'secondsPerImage: Int = 5' in plan
print('OK: v0.1.64 / versionCode 69, animar imagens offline, galeria, preview, MP4 e integração')

# Regressão do NullPointerException do progresso em Animar Imagens.
anim=(root/'app/src/main/java/br/com/uaicortes/AnimateImagesScreen.kt').read_text()
progress_ui=(root/'app/src/main/java/br/com/uaicortes/AnimationProgressUi.kt').read_text()
assert 'progress!!' not in anim
assert 'val fixedProgress=AnimationProgressUi.fraction(progress)' in anim
assert 'LinearProgressIndicator(progress={fixedProgress}' in anim
assert 'height(238.dp)' in anim and 'BoxWithConstraints(' in anim
assert 'detectDragGestures(' in anim and 'listState.scrollBy(' in anim
assert 'Text("Animar",' in anim and 'O Uai Cortes está preparando seu vídeo' in anim
assert 'fun fraction(value:Int?):Float?' in progress_ui
assert (root/'app/src/test/java/br/com/uaicortes/AnimationProgressUiTest.kt').is_file()
print('OK: v0.1.52, barra de progresso segura, preview compacto e arrasto em tempo real')

# v0.1.52: guarda rascunho separado, confirma saída e compartilha sucesso do editor.
draft=(root/'app/src/main/java/br/com/uaicortes/ImageAnimationDraftStore.kt').read_text()
saved_dialog=(root/'app/src/main/java/br/com/uaicortes/VideoSavedDialog.kt').read_text()
assert 'SharedPreferences' not in draft or 'getSharedPreferences' in draft
assert all(t in draft for t in ['fun load()', 'fun save(', 'ImageAnimationDraftCodec.prepareWrite(', 'editor.commit()'])
assert all(t in (root/'app/src/main/java/br/com/uaicortes/ImageAnimationDraftCodec.kt').read_text() for t in ['images', 'secondsPerImage', 'transition'])
assert all(t in anim for t in ['Salvar edição das imagens?', 'confirmExit', 'VideoSavedDialog(', 'onSaveDraft(', 'EditorOptionRow(', 'Text("Salvar"'])
assert 'progress!!' not in anim and 'onUseInEditor(uri)' in anim
assert 'VideoSavedDialog(uri=savedUri' in ui
assert (root/'app/src/test/java/br/com/uaicortes/ImageAnimationDraftRulesTest.kt').is_file()
print('OK: v0.1.64 / versionCode 69, rascunho, voltar, sucesso compartilhado e ajustes padronizados')


# v0.1.56: contratos de modularização e serviços compartilhados.
app_files = root/'app/src/main/java/br/com/uaicortes'
assert len((app_files/'MainActivity.kt').read_text().splitlines()) < 600
assert all('internal fun ' in (app_files/name).read_text() for name in [
    'HomeScreens.kt', 'EditorScreen.kt', 'EditorTimelineUi.kt', 'TrimScreen.kt',
    'AudioScreen.kt', 'PipScreen.kt', 'TransitionScreen.kt', 'SubtitleScreen.kt',
    'ExportScreen.kt', 'PreviewMediaUtils.kt'])
workspace = (app_files/'EditorWorkspaceState.kt').read_text()
assert 'EditorWorkspaceState()' in ui and 'var selectedClip by workspace::selectedClip' in ui
assert 'var screen by workspace::screen' in ui and 'var positionMs by workspace::positionMs' in ui
codec = (app_files/'ProjectStore.kt').read_text()
assert 'object ProjectCollectionCodec' in codec and 'fun recover(primary: String, backup: String?)' in codec
assert '.putString(backupKey, safePrevious)' in codec and '.commit()' in codec
assert 'persist(readForMutation().filterNot' in codec
assert 'GalleryVideoSaver.save(context, file, fileName)' in source
assert 'GalleryVideoSaver.save(' in engine
assert "IS_PENDING, 1" in saver and "IS_PENDING, 0" in saver
assert 'projectCollectionCodecRoundTrip' in (root/'app/src/test/java/br/com/uaicortes/ProjectStoreTest.kt').read_text()
print('OK: v0.1.64 / 65, modularização, estado transitório, MediaStore único e backup fail-closed')

# v0.1.56: restauração conservadora da navegação sem serializar mídia no Bundle.
main=(app_files/'MainActivity.kt').read_text()
nav=(app_files/'EditorNavigation.kt').read_text()
state=(app_files/'EditorWorkspaceState.kt').read_text()
assert 'rememberSaveable(saver = EditorWorkspaceState.Saver)' in main
vm = (app_files/'EditorProjectViewModel.kt').read_text()
session = (app_files/'ProjectSessionCoordinator.kt').read_text()
assert 'val projectSession: EditorProjectViewModel = viewModel()' in main
assert 'val activeProjectId = projectSession.activeProjectId' in main
assert 'val project = projectSession.project' in main
assert 'val projects = projectSession.projects' in main
assert 'state[ACTIVE_ID] = it' in vm
assert 'ProjectController(StoredProjectRepository(store))' in vm
assert all(token in vm for token in ['fun save(', 'fun open(', 'fun delete(', 'fun append('])
assert 'state[ACTIVE_ID]' in vm and 'state[ACTIVE_ID] ?: ""' in vm
assert 'projectController.' not in main
assert 'controller.load(restoredId)' in session
assert (root/'app/src/test/java/br/com/uaicortes/ProjectSessionCoordinatorTest.kt').exists()
assert 'EditorNavigation.back(' in main and 'workspace.reconcile(project, mediaOrigin, mediaAction)' in main
assert 'listSaver<androidx.compose.runtime.snapshots.SnapshotStateList<Uri>, String>' in main
assert 'val Saver: Saver<EditorWorkspaceState, ArrayList<String>>' in state
assert 'fun reconcile(project: VideoProject?' in state and 'snapshotTokens()' in state
assert 'fun restoreRoute(' in nav and 'fun back(' in nav
assert 'VideoProject' not in state.split('snapshotTokens()')[1].split('companion object')[0]
assert (root/'app/src/test/java/br/com/uaicortes/EditorNavigationTest.kt').exists()
assert (root/'app/src/test/java/br/com/uaicortes/EditorWorkspaceStateTest.kt').exists()
print('OK: v0.1.64 / 65, saved state reduzido, projeto por ID, navegação e testes de recuperação')

# v0.1.56: controllers extraídos da atividade e testes de regressão.
operations = (root/'app/src/main/java/br/com/uaicortes/ProjectOperations.kt').read_text()
pip_operations = (root/'app/src/main/java/br/com/uaicortes/OverlayOperations.kt').read_text()
import_operations = (root/'app/src/main/java/br/com/uaicortes/MediaImportOperations.kt').read_text()
main = (root/'app/src/main/java/br/com/uaicortes/MainActivity.kt').read_text()
assert all(v in operations for v in ['class ProjectController(', 'interface ProjectRepository', 'fun delete(', 'fun append('])
assert all(v in pip_operations for v in ['object OverlayOperations', 'fun insert(', 'fun apply(', 'fun remove('])
assert all(v in import_operations for v in ['object MediaImportRules', 'class MediaImportController', 'fun videoClips(', 'fun audio('])
assert all(v in main for v in ['projectSession.save(p)', 'projectSession.append(', 'OverlayOperations.apply(', 'OverlayOperations.insert(', 'mediaTasks.videos('])
assert 'store.save(p)' not in main and 'store.delete(target.id)' not in main
for name in ['ProjectOperationsTest.kt', 'OverlayOperationsTest.kt', 'MediaImportOperationsTest.kt']:
    assert (root/'app/src/test/java/br/com/uaicortes'/name).exists()
print('OK: v0.1.64 / 65, controllers projetos/PiP/importação e testes')

# v0.1.56: o Media3 recebe solicitação explícita de bitrate nos dois exportadores.
quality=(root/'app/src/main/java/br/com/uaicortes/VideoExportQuality.kt').read_text()
animator=(root/'app/src/main/java/br/com/uaicortes/ImageAnimationEngine.kt').read_text()
export_ui=(root/'app/src/main/java/br/com/uaicortes/ExportScreen.kt').read_text()
assert '720 -> 8_000_000' in quality and '1080 -> 18_000_000' in quality
assert '.setRequestedVideoEncoderSettings(' in source and '.setEncoderFactory(encoderFactory)' in source
assert '.setRequestedVideoEncoderSettings(' in animator and '.setEncoderFactory(encoderFactory)' in animator
assert 'Bitmap.CompressFormat.PNG' in animator and 'image-%03d.png' in animator
assert 'estimatedMiBPerMinute(' in export_ui and '1080' in (root/'app/src/main/java/br/com/uaicortes/ProjectStore.kt').read_text()
assert (root/'app/src/test/java/br/com/uaicortes/VideoExportQualityTest.kt').exists()
print('OK: v0.1.56, qualidade explícita 720p/1080p, armazenamento temporário e testes')

assert 'minimumFreeBytes(' in quality and 'minimumFreeBytes(' in source and 'minimumFreeBytes(' in animator
print('OK: reserva preventiva de espaço do vídeo e cópia MediaStore')

# v0.1.57: regressão do workflow 50 e refatoração de importação.
state=(app_files/'EditorWorkspaceState.kt').read_text()
boundary=(app_files/'TransitionBoundaryPolicy.kt').read_text()
transition=(app_files/'TransitionScreen.kt').read_text()
assert 'TransitionBoundaryPolicy.clamp(selectedBoundary, project.clips.size)' in state
assert 'fun clamp(index: Int, clipCount: Int)' in boundary
assert 'TransitionFramePreview(p.clips[safeBoundary],p.clips[safeBoundary+1]' in transition
assert 'LaunchedEffect(project) {' in main
assert 'mediaTasks.videos(uris, persistDocumentAccess)' in main and 'MediaImportTasks(mediaController)' in main
assert 'VideoImportRequest(project?.id.orEmpty()' in main
assert 'verifiedMediaDuration(context, uri)' in import_operations
assert 'assertEquals(0, restored.selectedBoundary)' in (root/'app/src/test/java/br/com/uaicortes/EditorWorkspaceStateTest.kt').read_text()
assert all((root/'app/src/test/java/br/com/uaicortes'/n).exists() for n in [
 'TransitionBoundaryPolicyTest.kt','MediaImportJobPolicyTest.kt'])
print('OK: v0.1.57, junção zero-based (workflow 50) e importação verificada em IO')

# v0.1.64: serviços de metadados em IO; trabalhos canceláveis e vinculados à rota.
assert 'rememberCoroutineScope()' in main and 'MediaTaskCoordinator(importScope,' in main
assert 'catch (cancelled: CancellationException)' not in main  # tratamento fica no coordenador
assert all(s in main for s in ['MediaImportTasks(mediaController)', 'startMediaTask(MediaTaskKind.VIDEO, origin)',
                              'startMediaTask(MediaTaskKind.AUDIO, origin)', 'startMediaTask(MediaTaskKind.OVERLAY, origin)'])
assert 'mediaController.duration(uri)' not in main and 'mediaController.title(uri)' not in main
assert 'if (activeMediaTask != null)' in main and 'Text("Cancelar importação")' in main
assert 'DisposableEffect(mediaTaskCoordinator)' in main
assert 'mediaTaskCoordinator.cancelIfDestinationChanged(activeProjectId, screen, mediaAction)' in main
media_tasks = (app_files/'MediaImportTasks.kt').read_text()
coordinator = (app_files/'MediaTaskCoordinator.kt').read_text()
assert all(s in media_tasks for s in ['withContext(ioDispatcher)', 'fun tryStart(', 'fun matches(',
                                     'suspend fun videos(', 'suspend fun audio(', 'suspend fun overlay(',
                                     'data class MediaTaskTicket', 'fun isActive(ticket: MediaTaskTicket)'])
assert all(s in coordinator for s in ['CoroutineStart.LAZY', 'val ticket = gate.tryStart(kind)',
                                      'if (gate.finish(ticket))', 'previous?.cancel()',
                                      'catch (cancelled: CancellationException)', 'throw cancelled'])
assert (root/'app/src/test/java/br/com/uaicortes/MediaImportTasksTest.kt').exists()
assert (root/'app/src/test/java/br/com/uaicortes/MediaTaskCoordinatorTest.kt').exists()
print('OK: v0.1.64, cancelamento e identidade de tarefa, dispatcher IO, destino protegido')

# v0.1.64: o ViewModel gerencia projetos; mídia/sessão inteira nunca no SavedStateHandle.
vm = (app_files/'EditorProjectViewModel.kt').read_text()
session = (app_files/'ProjectSessionCoordinator.kt').read_text()
assert 'lifecycle-viewmodel-compose:2.9.0' in gradle
assert 'class EditorProjectViewModel(' in vm and 'SavedStateHandle' in vm
assert 'ProjectSessionCoordinator' in vm and 'fun save(' in vm and 'fun delete(' in vm
assert 'state[ACTIVE_ID] = it' in vm and 'state[ACTIVE_ID] ?: ""' in vm
assert 'ProjectStore(application.applicationContext)' in vm
assert 'fun save(project: VideoProject): ProjectSnapshot = accept(controller.save(project))' in session
assert 'fun delete(id: String): ProjectSnapshot = accept(controller.delete(id, snapshot.activeId))' in session
assert 'val project = projectSession.project' in main
assert 'ProjectStore(context)' not in main
assert 'projectController.' not in main
assert (root/'app/src/test/java/br/com/uaicortes/ProjectSessionCoordinatorTest.kt').exists()
print('OK: v0.1.64 ViewModel e sessão de projetos, restaurando ID sem serializar mídia')

# v0.1.64: motor exportação gerenciado pelo ViewModel, sem serializar mídia ou trabalho.
export_screen = (app_files/'ExportScreen.kt').read_text()
export_vm = (app_files/'ExportTaskViewModel.kt').read_text()
export_tasks = (app_files/'ExportTaskCoordinator.kt').read_text()
engine = (app_files/'ExportEngine.kt').read_text()
assert 'val exportVm: ExportTaskViewModel = viewModel()' in export_screen
assert 'exportVm.start(p, quality, name)' in export_screen
assert 'exportVm.cancel()' in export_screen
assert 'DisposableEffect(engine)' not in export_screen
assert 'class ExportTaskViewModel(application: Application)' in export_vm
assert 'viewModelScope.launch' in export_vm and 'override fun onCleared()' in export_vm
assert 'fun setProject(' in export_vm and 'fun dismissSaved()' in export_vm
assert 'class ExportTaskCoordinator' in export_tasks
assert 'current.id != id' in export_tasks and 'snapshot.phase == ExportPhase.SAVING' in export_tasks
assert 'isSavingToGallery = true' in engine and 'if (isSavingToGallery) return' in engine
assert (root/'app/src/test/java/br/com/uaicortes/ExportTaskCoordinatorTest.kt').exists()
print('OK: v0.1.64 ViewModel de exportação, tickets, progresso real, cancelamento e salvamento protegido')

# v0.1.64: Animar Imagens usa o mesmo coordenador da exportação e guarda o
# motor Media3 no ViewModel, sem Jobs/Transformer em remember ou Bundle.
image_vm = (app_files/'ImageAnimationTaskViewModel.kt').read_text()
assert 'val animationVm: ImageAnimationTaskViewModel = viewModel()' in anim
assert 'animationVm.start(images.toList(),options)' in anim
assert 'animationVm.cancel()' in anim
assert 'animationVm.savedVideo?.let' in anim
assert 'ImageAnimationEngine(context.applicationContext)' not in anim
assert 'DisposableEffect(Unit)' not in anim
assert 'private val tasks = ExportTaskCoordinator()' in image_vm
assert 'activeEngine = engine' in image_vm and 'ImageAnimationEngine(' in image_vm
assert 'override fun onCleared()' in image_vm and 'viewModelScope.launch' in image_vm
assert 'tasks.saving(ticket)' in image_vm and 'tasks.success(ticket)' in image_vm
assert 'engine.isSaving' not in anim
assert 'preparing = false' in (app_files/'ImageAnimationEngine.kt').read_text()
assert 'if (!preparing) cleanup()' in (app_files/'ImageAnimationEngine.kt').read_text()
assert (root/'app/src/test/java/br/com/uaicortes/ImageAnimationTaskFlowTest.kt').is_file()
print('OK: v0.1.64 ViewModel Animar Imagens, fases comuns, reinício isolado e cancelamento seguro')

# v0.1.64: timeline centralizada, velocidade pequena no VM, restauro de seek.
playback=(root/'app/src/main/java/br/com/uaicortes/TimelinePlaybackCoordinator.kt').read_text()
playback_vm=(root/'app/src/main/java/br/com/uaicortes/EditorPlaybackViewModel.kt').read_text()
editor=(root/'app/src/main/java/br/com/uaicortes/EditorScreen.kt').read_text()
preview=(root/'app/src/main/java/br/com/uaicortes/PreviewMediaUtils.kt').read_text()
assert all(x in playback for x in ['fun target(', 'fun projectPosition(', 'fun step(', 'TimelineSeekTarget'])
assert all(x in playback_vm for x in ['class EditorPlaybackViewModel', 'SavedStateHandle', 'PreviewPlayback.SPEEDS', 'fun selectProject(', 'fun updateSpeed('])
assert 'TimelinePlaybackCoordinator.target(' in preview and 'TimelinePlaybackCoordinator.projectPosition(' in preview
assert 'val playbackVm: EditorPlaybackViewModel = viewModel()' in editor
assert 'LaunchedEffect(p.id) { playbackVm.selectProject(p.id) }' in editor
assert 'onIsPlayingChanged(playing: Boolean)' in editor
assert 'seekGlobal(TimelinePrecision.step(currentMs, -5000L, p.durationMs))' in editor
assert 'seekGlobal(TimelinePrecision.step(currentMs, 5000L, p.durationMs))' in editor
assert 'onValueChangeFinished = { scrubbing = false }' in editor
assert 'if (!restoringMedia && !scrubbing' in editor
assert 'seekProjectPosition(player, p.clips, latestPosition)' in editor
assert (root/'app/src/test/java/br/com/uaicortes/TimelinePlaybackCoordinatorTest.kt').exists()
assert (root/'app/src/test/java/br/com/uaicortes/EditorPlaybackViewModelTest.kt').exists()
print('OK: v0.1.64 / versionCode 69, timeline global, ViewModel de velocidade, seek/cancelamento de arrasto')

# v0.1.68: recuperação explícita e gravação segura.
codec=(app_files/'ProjectStore.kt').read_text()
main=(app_files/'MainActivity.kt').read_text()
vm=(app_files/'EditorProjectViewModel.kt').read_text()
assert 'data class ProjectCatalogRead(' in codec and 'fun inspect(primary: String?, backup: String?)' in codec
assert 'fun prepareWrite(primary: String?, backup: String?, projects: List<VideoProject>)' in codec
assert 'lastGoodRaw' in codec and 'usedBackup' in codec
assert 'projects.map { it.id }.distinct().size == projects.size' in codec
assert 'lastReadIssue' in codec and 'storageWarning' in vm
assert 'catalogAlertDismissed' in main and 'Atenção ao armazenamento de projetos' in main
assert 'if (write(p)) screen = "home"' in main
assert (root/'app/src/test/java/br/com/uaicortes/ProjectCatalogRecoveryTest.kt').exists()
print('OK: v0.1.68 / versionCode 69, recuperação de backup, aviso e escrita protegida')

# v0.1.68: codec fail-closed, backup do Animar Imagens e permissões de leitura.
codec=(app_files/'ImageAnimationDraftCodec.kt').read_text()
draft=(app_files/'ImageAnimationDraftStore.kt').read_text()
access=(app_files/'ImageDraftMediaAccess.kt').read_text()
anim=(app_files/'AnimateImagesScreen.kt').read_text()
assert all(t in codec for t in ['enum class ImageDraftStatus', 'PRIMARY, BACKUP, DAMAGED', 'fun inspect(', 'fun prepareWrite(', 'fun decode(', 'fun encode('])
assert all(t in draft for t in ['ImageAnimationDraftCodec.prepareWrite(', 'fun resetDamaged()', 'editor.commit()', 'backupKey'])
assert all(t in access for t in ['Dispatchers.IO', 'openFileDescriptor(', 'ensureActive()'])
assert all(t in anim for t in ['inaccessible(', 'unavailableImages', 'ImageDraftStatus.DAMAGED', 'requestDraftReset', 'allImagesReadable'])
assert (root/'app/src/test/java/br/com/uaicortes/ImageAnimationDraftCodecTest.kt').is_file()
assert 'uris.forEach(mediaController::persistReadAccess)' in main
print('OK: v0.1.68 / versionCode 69, backup fail-closed e validação de permissão de fotos')

# v0.1.68: novos fluxos de Home, galeria, nomes, PiP e modelo Vosk.
home=(root/'app/src/main/java/br/com/uaicortes/HomeScreens.kt').read_text()
main=(root/'app/src/main/java/br/com/uaicortes/MainActivity.kt').read_text()
pipui=(root/'app/src/main/java/br/com/uaicortes/PipScreen.kt').read_text()
vosk=(root/'app/src/main/java/br/com/uaicortes/SpeechModelSettingsScreen.kt').read_text()
assert 'onDeleteAll' in home and 'Excluir todos os projetos' in home
assert 'pendingDeleteAll' in main and 'projectSession.deleteAll()' in main
assert 'pendingNewProjectName' in main and 'ProjectNaming.title(newProjectName)' in main
assert 'ProjectNaming.exportName(p.title)' in (root/'app/src/main/java/br/com/uaicortes/ExportScreen.kt').read_text()
assert 'if (showSearch) OutlinedTextField' in browser
assert 'if (scope == GalleryScope.IMAGES_AND_VIDEOS) LazyRow' in browser
assert 'PipTimePresets.clipPeriods' in pipui and 'RangeSlider(' in pipui
assert 'TimeDigitsField("Início"' in pipui and 'compact=true' in pipui
assert 'AutoSubtitleEngine.install' in vosk and '"$it%"' in vosk
assert 'speech_model' in main and 'onManageModel' in main
print('OK: v0.1.68 / versionCode 69, melhorias UI, nome de projeto, PiP e Vosk')

# v0.1.68: Sobre e Novidades compartilham a mesma fonte e fluxo de exibição única.
notes=(app_files/'AppReleaseNotes.kt').read_text()
seen=(app_files/'WhatsNewSeenStore.kt').read_text()
news=(app_files/'WhatsNewScreen.kt').read_text()
home=(app_files/'HomeScreens.kt').read_text()
main=(app_files/'MainActivity.kt').read_text()
nav=(app_files/'EditorNavigation.kt').read_text()
assert all(x in notes for x in ['fun forAbout()', 'fun forWhatsNew()', 'fun fingerprintFor(', 'currentFingerprint', '0.1.68', 'VERSION_CODE = 69'])
assert 'ReleaseHighlights(AppReleaseNotes.forAbout())' in home
assert 'ReleaseHighlights(AppReleaseNotes.forWhatsNew())' in news
assert 'markViewed()' in main and 'WhatsNewScreen(::back)' in main
assert 'if (whatsNewSeen.shouldOpen())' in main and 'screen = "whats_new"' in main
assert '"whats_new"' in nav and '"whats_new"' in home
assert 'putStringSet' in seen and '.commit()' in seen
assert (root/'app/src/test/java/br/com/uaicortes/WhatsNewReleaseNotesTest.kt').exists()
print('OK: v0.1.68 / versionCode 69, tela Novidades uma vez e fonte única com Sobre')
