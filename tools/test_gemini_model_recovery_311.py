#!/usr/bin/env python3
"""Contrato estrutural de integração. Não substitui teste com API real."""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
src = root / 'app/src/main/java/br/com/uaicortes'
read = lambda name: (src / f'{name}.kt').read_text()
core = read('GeminiModelRecovery')
assert 'repeat(2)' in core and 'delay(1000)' in core
assert 'fault.http == 503' in core and 'fault.http == 404 && fault.modelUnavailable' in core
assert 'route == GeminiRecoveryRoute.TRANSCRIBE' in core and 'fault.http in setOf(404, 429, 503)' in core
assert 'original == GeminiModels.FLASH' in core
assert 'GeminiRecoveryRoute.BATCH -> TEXT_LITE.takeIf' in core
assert 'catch (cancel: CancellationException)' in core
assert 'publicModel(attempt.model)' in core
assert 'error.message' not in core and 'stackTrace' not in core

text = read('GeminiVideoCaptionClient')
assert 'if (first && failover != null)' in text and 'transport(effectiveKey)' in text
assert 'generateStudioValidated' in text and 'parse(result)' in text
assert 'actualModel' in read('GeminiSubtitleLipSync')
assert 'allowModelRecovery = journal == null && chosen.size == 1' in read('VoiceIaScreen')
assert '(take.requestedModel ?: take.model) == model' in read('VoiceIaDraftStore')
assert 'allowModelRecovery = !sample' in read('StandaloneTextToVoiceScreen')
assert 'allowModelRecovery = action != "sample"' in read('GeminiNarrationUi')
assert 'storage.accept(pending, remote, submitted.model)' in read('GeminiBatchUi')
assert 'createRequests' in read('GeminiBatchClient') and 'address.endsWith(":batchGenerateContent")' in read('GeminiBatchClient')
for ui in ('SubtitleProofreadDialog', 'VideoSavedDialog', 'SubtitleLipSyncDialog', 'AutoSubtitleScreen',
           'SrtImportReview', 'StandaloneTextToVoiceScreen', 'GeminiNarrationUi', 'GeminiBatchUi',
           'VoiceIaScreen', 'GeminiVoiceCenterScreen'):
    assert 'GeminiRecoveryFeedback(' in read(ui), ui
assert 'ActivityResultContracts.CreateDocument("text/plain")' in read('GeminiRecoveryUi')
assert (root / 'app/src/test/java/br/com/uaicortes/GeminiModelRecoveryTest.kt').is_file()
print('OK: recuperação Gemini por modalidade, proveniência de voz e lote e relatório local')
