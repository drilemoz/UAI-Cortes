#!/usr/bin/env python3
"""CI215: a função range retorna Pair, cujo segundo elemento é second, não last."""
from pathlib import Path
import re
import unittest

ROOT=Path(__file__).resolve().parents[1]
SRC=ROOT/'app/src/main/java/br/com/uaicortes'

class VoiceCenterRange215(unittest.TestCase):
    def test_helper_returns_pair_and_ui_uses_second(self):
        paging=(SRC/'GeminiVoicePaging.kt').read_text(encoding='utf-8')
        ui=(SRC/'GeminiVoiceCenterScreen.kt').read_text(encoding='utf-8')
        self.assertRegex(paging,r'fun\s+range\(total:\s*Int,\s*requestedPage:\s*Int\):\s*Pair<Int,\s*Int>')
        self.assertIn('val pageRange = GeminiVoicePaging.range(displayed.size, currentPage)',ui)
        self.assertIn('${pageRange.first}–${pageRange.second}',ui)
        self.assertNotRegex(ui,r'pageRange\.last\b')
        self.assertIn('GeminiVoicePaging.slice(displayed, currentPage)',ui)

if __name__=='__main__': unittest.main()
