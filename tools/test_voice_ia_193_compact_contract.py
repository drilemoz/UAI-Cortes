"""UI compacta e lembrete; testes estruturais não substituem Compose/Android."""
from pathlib import Path
import unittest
ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT/'app/src/main/java/br/com/uaicortes'
UI=(SRC/'VoiceIaScreen.kt').read_text()
class Compact193(unittest.TestCase):
 def test_three_icons_and_long_press_actions(self):
  bar=UI[UI.index('''                    val wav = take?.takeIf'''):UI.index('''    advancedCueId?.let''')]
  self.assertEqual(bar.count('IconButton('),3)
  for value in ('"Ouvir fala ', '"Gerar fala ', '"Alterar tom da fala ', '"Trocar voz da fala ', 'onLongClickLabel', 'advancedCueId = cue.id'):
   self.assertIn(value,bar)
  details=UI[UI.index('''    advancedCueId?.let'''):UI.index('''    if (voicePickerCueId != null''')]
  for token in ('pendingInsertId = cue.id','pendingSyncId = cue.id','forceCueId = cue.id','pendingDeleteId = cue.id','restoreOriginal(cue, take)'):
   self.assertIn(token,details)
 def test_global_counter_and_recovery(self):
  for key in ('"$preparedCount/${cues.size} prontas"','"$pending faltando"','showTechnicalDetails = !showTechnicalDetails','showSpeakerSettings = !showSpeakerSettings','requestPendingGeneration()'):
   self.assertIn(key,UI)
  self.assertIn('showPendingReminder = VoiceIaCompactPolicy.shouldRemind(',UI)
  self.assertIn('savedQueue?.unfinished == true',UI)
  self.assertIn('"Depois"',UI)
  self.assertIn('reminderChecked = true',UI)
  self.assertIn('promptGenerate = pendingIds',UI)
  self.assertNotIn('api.synthesize(', UI[UI.index('if (showPendingReminder && !busy)'):UI.index('promptGenerate?.let { ids ->')])
 def test_previous_safety_stays(self):
  for value in ('VoiceIaQueuePolicy.reconcileSelections(', 'VoiceIaQueueStage.REVIEW','GeminiUsageHint("narração"','"Descartar fila"','withContext(Dispatchers.IO) { store.save(updated) }'):
   self.assertIn(value,UI)
if __name__ == '__main__':unittest.main()
