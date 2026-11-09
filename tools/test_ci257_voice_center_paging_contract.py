#!/usr/bin/env python3
"""v0.1.257: regressões da Central Google; checagens estruturais não são build Android."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT/'app/src/main/java/br/com/uaicortes'

class VoiceCenter257(unittest.TestCase):
    def test_pagination_and_global_search(self):
        ui=(SRC/'GeminiVoiceCenterScreen.kt').read_text()
        paging=(SRC/'GeminiVoicePaging.kt').read_text()
        self.assertIn('const val PAGE_SIZE = 50',paging)
        self.assertIn('GeminiVoicePaging.slice(displayed, currentPage)',ui)
        self.assertIn('val totalPages = GeminiVoicePaging.totalPages(displayed.size)',ui)
        self.assertIn('val target = GeminiVoicePresentation.fold(debouncedQuery.trim())',ui)
        self.assertLess(ui.index('val displayed = filteredResult.voices'),ui.index('val visible = GeminiVoicePaging.slice'))
        self.assertIn('state = voiceListState',ui)
        self.assertNotIn('visibleCount += 30',ui)
        self.assertIn('Text("Anterior")',ui)
        self.assertIn('Text("Próxima")',ui)
        self.assertIn('page = 0',ui)
        self.assertIn('delay(180L)',ui)
    def test_filters_and_actions(self):
        ui=(SRC/'GeminiVoiceCenterScreen.kt').read_text()
        for key in ('"Português BR"','"Masculinas"','"Femininas"','"Com prévia"',
                    '"★ Favoritas (','filtersOpen = true','Renomear ${title(voice)}',
                    'Icon(Icons.Default.MoreVert','GeminiVoiceFavorites.toggle(',
                    'cache.find(sampleId(voice))','compareA = voice.id','compareB = voice.id'):
            self.assertIn(key,ui,key)
        self.assertIn('horizontalArrangement = Arrangement.End',ui)
    def test_no_network_per_page_and_safe_diagnostics(self):
        ui=(SRC/'GeminiVoiceCenterScreen.kt').read_text()
        recorder=(SRC/'DiagnosticRecorder.kt').read_text()
        probe=(SRC/'VoiceCenterFrameProbe.kt').read_text()
        self.assertIn('fun voiceCenterPerformance(',recorder)
        self.assertIn('fun voiceCenterFrames(',recorder)
        self.assertIn('DiagnosticRecorder.isEnabled()',probe)
        self.assertIn('frames < 90',probe)
        self.assertIn('2_500L',probe)
        self.assertIn('DiagnosticRecorder.voiceCenterPerformance("filter"',ui)
        self.assertIn('DiagnosticRecorder.voiceCenterPerformance("cache"',ui)
        self.assertIn('DiagnosticRecorder.voiceCenterPerformance("page"',ui)
        self.assertNotIn('GeminiTtsClient',paging:=(SRC/'GeminiVoicePaging.kt').read_text())
        self.assertNotIn('Api',paging)

if __name__=='__main__': unittest.main()
