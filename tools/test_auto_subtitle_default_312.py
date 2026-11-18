#!/usr/bin/env python3
from pathlib import Path
import wave

root = Path(__file__).resolve().parents[1]
src = root / "app/src/main/java/br/com/uaicortes"
read = lambda name: (src / (name + ".kt")).read_text()
settings = read("GeminiTranscriptionSettings")
client = read("GeminiSubtitleClient")
auto = read("AutoSubtitleScreen")
assert "GeminiTranscriptionSettings(store, hasKey)" in read("GeminiSettingsUi")
assert 'getBoolean("default_automatic_subtitles", false)' in settings
assert "preferences.setDefault(it)" in settings and "GeminiRecoveryFeedback(recovery)" in settings
assert 'Text("Enviar amostra")' in settings and "openRawResource(R.raw.gemini_transcribe_probe)" in settings
assert "if (preferGemini && geminiConfigured) SubtitleEngine.GEMINI else null" in auto
assert "Enviar e gerar" in auto and "SubtitleEngine.VOSK" in auto
assert "GeminiTranscriptionRequest.useInline(" in client
assert "GeminiTranscriptionRequest.inline(" in client and "GeminiTranscriptionRequest.file(" in client
probe = root / "app/src/main/res/raw/gemini_transcribe_probe.wav"
with wave.open(str(probe)) as wav:
    assert (wav.getnchannels(), wav.getsampwidth(), wav.getframerate()) == (1, 2, 16000)
    assert 0.5 < wav.getnframes() / wav.getframerate() < 2
assert probe.stat().st_size < 128 * 1024
print("OK: padrão opcional, consentimento e amostra sintética da transcrição")
