#!/usr/bin/env python3
"""Regressões estruturais sobre fila interativa Voice IA; sem alegar teste Android real."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT/'app/src/main/java/br/com/uaicortes'

class QueueContract(unittest.TestCase):
    def test_atomic_nonsecret_queue(self):
        s = (SRC/'VoiceIaQueueStore.kt').read_text()
        for needle in ('AtomicFile(', 'projectId', 'requestStyle', 'cueFingerprint',
                       '1_048_576', 'fun clear()', 'fun save(', 'fun load()'):
            self.assertIn(needle, s)
        for forbidden in ('apiKey', 'secretKey', 'cue.text', 'videoUri'):
            self.assertNotIn(forbidden, s)
    def test_nonreplay_on_restart_and_explicit_review(self):
        s = (SRC/'VoiceIaQueuePolicy.kt').read_text()
        for needle in ('item.stage == VoiceIaQueueStage.IN_FLIGHT || item.stage == VoiceIaQueueStage.READY', 'fun retryReviewed(',
                       'fun recover(', 'VoiceIaQueueStage.STALE'):
            self.assertIn(needle, s)
    def test_pre_and_post_network_journal_and_user_action(self):
        s = (SRC/'VoiceIaScreen.kt').read_text()
        for needle in ('VoiceIaQueueStage.IN_FLIGHT', 'api.synthesize(',
                       'withContext(Dispatchers.IO) { store.save(updated) }',
                       'VoiceIaQueueStage.READY', 'resumeQueueConfirmation',
                       'retryQueueConfirmation', 'discardQueueConfirmation'):
            self.assertIn(needle, s)
        self.assertLess(s.index('VoiceIaQueueStage.IN_FLIGHT'), s.index('api.synthesize('))
    def test_documented_consent(self):
        s = (SRC/'VoiceIaScreen.kt').read_text()
        self.assertIn('Chamadas de resultado incerto não serão repetidas', s)
        self.assertIn('Repeti-la pode consumir cota novamente', s)

if __name__ == '__main__':
    unittest.main()
