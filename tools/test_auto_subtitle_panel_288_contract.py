#!/usr/bin/env python3
"""Verify wiring and cloud consent. JVM cases cover timing coordinates and draft edits."""
from pathlib import Path
root = Path(__file__).resolve().parents[1]
base = root / 'app/src/main/java/br/com/uaicortes'
read = lambda name: (base / f'{name}.kt').read_text()
ui, panel, speech, words = map(read, ['AutoSubtitleScreen', 'AutoSubtitlePanel',
    'SubtitleSpeechSource', 'SubtitleTextRetiming'])
assert 'Offline · Vosk' in ui and 'Online · Gemini' in ui and 'ModalBottomSheet(' in ui
assert 'SETUP, GENERATING, REVIEW' in ui and 'AlertDialog(' not in ui
assert 'short_cloud_notice' in ui and 'Não mostrar esta explicação novamente' in ui
assert 'Envia áudio ao Gemini' in ui and 'Enviar e gerar' in ui
assert 'geminiCheckpoint.subtitles(persisted)' in ui and 'if (!previousReady)' in ui
assert 'SubtitleSpeechDraft.place(generated, source.offsetMs, actual)' in ui
assert 'startMs = word.startMs + offset, endMs = word.endMs + offset' in speech
assert 'Narração escolhida' in panel and 'Áudio dos clipes' in panel
assert 'if (before == after) return Edit(cue.copy(text = text), false)' in words
assert 'wordTimings = left + middle + right' in words
assert 'SubtitleSpeechDraft.redo(original, placed, range)' in ui and 'retry = true' in ui
assert 'SrtSpeechAlignment.align(listOf(cue), placed' in ui
assert 'embedded = true' in ui and 'Conferir pela boca' in panel
assert 'Enviar trecho e ajustar pelo áudio' in panel and 'Ouvir trecho' in panel and 'Ver trecho' in panel
assert 'GeminiDiarizationPolicy.prepare(base' in ui and 'onApply(proposal.copy(' in ui
print('OK: um painel, envio explícito, recorte, âncoras por palavra, revisão e recuperação')
