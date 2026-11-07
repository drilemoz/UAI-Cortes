#!/usr/bin/env python3
"""v0.1.214: garantias estáticas do fluxo de exclusão voluntária de saída M4A parcial.
Não é teste de ContentResolver/SAF em dispositivo real.
"""
from pathlib import Path
root = Path(__file__).resolve().parents[1]
main = root/'app/src/main/java/br/com/uaicortes'
engine = (main/'AudioExtractionEngine.kt').read_text()
screen = (main/'QuickVideoToolsScreen.kt').read_text()
result = (main/'QuickAudioSessionScreen.kt').read_text()
assert 'incompleteAudioUri = uri' in screen
assert 'extractedAudioUri = uri; incompleteAudioUri = null' in screen
assert 'audioEngine.removeIncompleteDestination(candidate)' in screen
assert 'deleteIncompleteConfirm = true' in screen
assert 'if (deleteIncompleteConfirm) AlertDialog(' in screen
assert screen.index('if (deleteIncompleteConfirm) AlertDialog(') < screen.index('audioEngine.removeIncompleteDestination(candidate)')
assert 'deletingIncomplete -> Unit' in screen
assert 'enabled = !deletingIncomplete' in result
assert 'incompleteUri: Uri?' in result
assert 'if (!success && incompleteUri != null)' in result
assert '"Excluir M4A incompleto"' in result
assert 'private var stoppedCopyWorker: Thread? = null' in engine
assert 'previousWorker.join(5_000)' in engine
assert 'check(!previousWorker.isAlive)' in engine
assert 'DocumentsContract.deleteDocument(resolver, destination)' in engine
assert 'main.post {' in engine
assert 'removeIncompleteDestination(' in engine
# A lógica automática de sucesso/erro/cancelamento não chama exclusão sem toque do usuário.
assert screen.count('audioEngine.removeIncompleteDestination(') == 1
assert engine.index('fun removeIncompleteDestination(') < engine.index('fun start(')
print('OK: v214 exclusão SAF só por ação e confirmação, sem escrita em andamento e falha informada')
