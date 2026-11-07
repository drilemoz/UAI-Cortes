#!/usr/bin/env python3
"""v212: tela exclusiva de divisão e resultado usa somente saídas publicadas.
Contrato estático: exige build Android, testes instrumentados e teste de vídeo real.
"""
from pathlib import Path
root = Path(__file__).resolve().parents[1]
path = root/'app/src/main/java/br/com/uaicortes'
ui = (path/'QuickVideoToolsScreen.kt').read_text()
flow = (path/'QuickSplitSessionScreen.kt').read_text()
engine = (path/'QuickVideoSplitEngine.kt').read_text()
for token in (
    'private enum class SplitView { SETUP, RUNNING, RESULT }',
    'splitView = SplitView.RUNNING',
    'splitView = SplitView.RESULT',
    'QuickSplitProgressScreen(',
    'QuickSplitResultScreen(',
    'onSaved = { saved -> savedSplitFiles.add(saved) }',
    'progressTotal = parts.size',
    'galleryOpen -> galleryOpen = false',
    'processing -> confirmCancel = true',
    'splitEngine.cancel(); audioEngine.cancel()',
    'Intent(Intent.ACTION_VIEW)',
    'Intent.FLAG_GRANT_READ_URI_PERMISSION',
): assert token in ui, token
assert 'setDataAndType(file.uri, if (file.isAudio) "audio/mp4" else "video/mp4")' in ui
for token in (
    '"Cortando parte $current"',
    '"Salvando parte $current"',
    '"Extraindo áudio da parte $current"',
    '"$done de $total partes concluídas"',
    'LinearProgressIndicator(progress = { done.toFloat() / total }',
    'QuickSplitResultScreen(',
    'items(files, key = { it.uri.toString() })',
    '"Concluído"',
    '"Nova divisão"',
    '"Nenhum arquivo concluído foi registrado nesta tentativa."',
): assert token in flow, token
assert '"${done * 100 / total}%"' not in flow
assert 'data class QuickSplitSavedFile(' in engine
assert 'onSaved: (QuickSplitSavedFile) -> Unit = {}' in engine
assert 'onSaved(QuickSplitSavedFile(part.index, fileName, videoUri, isAudio = false))' in engine
assert 'onSaved(QuickSplitSavedFile(part.index, audioName, destination, isAudio = true))' in engine
assert engine.index('GalleryVideoSaver.save(context, output') < engine.index('onSaved(QuickSplitSavedFile(part.index, fileName')
assert engine.index('QuickSplitAudioStore.finish(context, destination)') < engine.index('onSaved(QuickSplitSavedFile(part.index, audioName')
assert 'IS_PENDING, 1' in engine and 'IS_PENDING, 0' in engine
assert 'if (!isCurrent()) return@post' in engine
assert 'onError(reason, completed)' in engine and 'onDone(completed)' in engine
print('OK: execução em tela cheia e resultados por URI real, parcial/cancelamento seguros')
