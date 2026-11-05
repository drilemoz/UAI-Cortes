#!/usr/bin/env python3
"""Contratos de recuperação Gemini (estáticos, não substituem Android real)."""
from pathlib import Path
import unittest

SRC = Path(__file__).resolve().parents[1] / 'app/src/main/java/br/com/uaicortes'

class GeminiRecovery156Contract(unittest.TestCase):
    def test_atomic_private_and_bounded(self):
        store = (SRC / 'GeminiTaskCheckpoint.kt').read_text()
        policy = (SRC / 'GeminiTaskRecoveryPolicy.kt').read_text()
        for marker in ('AtomicFile(', 'filesDir', 'startWrite()', 'finishWrite(', 'failWrite(',
                       'saveSubtitles(', 'saveDescription(', 'fun load(', 'fun clear()'):
            self.assertIn(marker, store)
        for marker in ('MAX_BYTES = 1_048_576', 'RETENTION_MS', 'MessageDigest',
                       'GeminiTaskStage.IN_FLIGHT', 'GeminiTaskStage.REVIEW'):
            self.assertIn(marker, policy)
        for forbidden in ('apiKey', 'accessToken', 'privateKey', 'responseBody', 'videoUri'):
            self.assertNotIn(forbidden, store)

    def test_subtitles_are_stored_then_user_reviews(self):
        screen = (SRC / 'AutoSubtitleScreen.kt').read_text()
        self.assertLess(screen.index('geminiCheckpoint.start('), screen.index('geminiClient.transcribe('))
        self.assertLess(screen.index('geminiClient.transcribe('), screen.index('geminiCheckpoint.saveSubtitles('))
        self.assertIn('geminiCheckpoint.subtitles(persisted)', screen)
        self.assertIn('GeminiTaskStage.REVIEW', screen)
        self.assertIn('Text("Restaurar")', screen)
        self.assertIn('Repetir pode consumir cota novamente', screen)
        self.assertIn('NonCancellable + Dispatchers.IO', screen)

    def test_description_manual_restore_and_project_persistence(self):
        screen = (SRC / 'VideoSavedDialog.kt').read_text()
        self.assertLess(screen.index('descriptionCheckpoint.start('), screen.index('generator.generate('))
        self.assertLess(screen.index('generator.generate('), screen.index('descriptionCheckpoint.saveDescription('))
        self.assertIn('descriptionCheckpoint::description', screen)
        self.assertIn('if (!persistDraft()) failure', screen)
        self.assertIn('savedDescriptionTask = GeminiTaskCheckpoint(', screen)

    def test_error_classification_and_usage_panel(self):
        errors = (SRC / 'GeminiOperationFailure.kt').read_text()
        for marker in ('429 -> GeminiFailureKind.RATE_LIMIT', '401, 403 -> GeminiFailureKind.AUTHORIZATION',
                       '404 -> GeminiFailureKind.MODEL', 'GeminiFailureKind.NETWORK_UNCERTAIN'):
            self.assertIn(marker, errors)
        usage = (SRC / 'GeminiUsageUi.kt').read_text()
        self.assertIn('Consumo por função e classificação de falhas', usage)
        self.assertIn('GeminiOperationFailure.label(', usage)

if __name__ == '__main__':
    unittest.main()
