#!/usr/bin/env python3
"""Contrato da ferramenta rápida: regras, navegação, isolamento e salvamento seguro."""
from pathlib import Path
root = Path(__file__).resolve().parents[1]
src = root / 'app/src/main/java/br/com/uaicortes'
policy = (src/'QuickVideoSplitPolicy.kt').read_text()
engine = (src/'QuickVideoSplitEngine.kt').read_text()
ui = (src/'QuickVideoToolsScreen.kt').read_text()
home = (src/'HomeScreens.kt').read_text()
main = (src/'MainActivity.kt').read_text()
nav = (src/'EditorNavigation.kt').read_text()
saver = (src/'GalleryVideoSaver.kt').read_text()
for text in ['SHORT_MS = 9_500L', 'TEN_SECONDS_MS = 10_000L', 'MAX_PARTS = 180',
             'minOf(durationMs, start + partLengthMs)', 'fun partName(']:
    assert text in policy, text
assert 'GalleryVideoSaver.save(context, output, fileName, subfolder = "Divididos")' in engine
assert 'QuickSplitAudioStore.createPending' in engine
assert 'QuickSplitAudioStore.discard' in engine and 'IS_PENDING, 0' in engine
assert 'setRemoveAudio(audioMode == "mute")' in engine
assert 'onProgress(index + 1' in engine and 'nextPart(index + 1)' in engine
assert 'QuickVideoToolsScreen(QuickToolMode.SPLIT_VIDEO' in main
assert 'QuickVideoToolsScreen(QuickToolMode.EXTRACT_AUDIO' in main
assert 'HomeQuickMediaActions(onQuickSplit, onQuickAudio)' in home
assert '"quick_split", "quick_audio" -> "home"' in nav
assert 'AudioExtractionEngine(context.applicationContext)' in ui
assert 'ActivityResultContracts.CreateDocument("audio/mp4")' in ui
assert 'Não foi possível ler a duração' in ui
assert 'subfolder.isEmpty() || subfolder == "Divididos"' in saver
assert 'IS_PENDING' in saver
assert 'VideoProject(' not in engine and 'projectSession' not in ui
print('OK: dividir vídeo/áudio na Home, preservação do original, Media3, galeria e cancelamento')
