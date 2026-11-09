"""Regressão 0.1.255: renomear vozes Google + TTS pt-BR; sem dependência de API.

Não substitui a compilação Kotlin/Android nem teste real de sotaque.
"""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / 'app/src/main/java/br/com/uaicortes'

def src(name):
    return (APP / name).read_text(encoding='utf-8')

class GoogleVoices255(unittest.TestCase):
    def test_rename_is_visible_and_does_not_change_voice_id(self):
        screen = src('GeminiVoiceCenterScreen.kt')
        aliases = src('GeminiVoiceAliases.kt')
        assert '"Renomear ${title(voice)}"' in screen
        assert 'Text("Renomear voz Google")' in screen
        assert '"Restaurar nome original"' in screen
        assert 'aliasStore.save(voice.id, aliasDraft)' in screen
        assert 'aliases[voice.id]' in aliases
        assert 'GeminiVoicePresentation.label(voice)' in aliases
        assert 'prefs.edit().putString("aliases"' in aliases
        assert 'voice.id = aliasDraft' not in screen

    def test_portuguese_is_explicit_but_optional_for_previews_and_text_to_voice(self):
        screen = src('GeminiVoiceCenterScreen.kt')
        stand = src('StandaloneTextToVoiceScreen.kt')
        core = src('GeminiTtsCore.kt')
        rules = src('GeminiSpeechLanguage.kt')
        assert '"pt-BR"' in rules
        assert '"auto"' in rules
        assert 'GeminiSpeechLanguage.style(style, languageMode)' in core
        assert 'GeminiSpeechLanguage.style(styleInstruction, sampleLanguage)' in screen
        assert 'requestedStyle, requestedText, requestedLanguage)' in screen
        assert 'requestedStyle, requestedText, requestedLanguage)' in stand
        assert 'GeminiSpeechLanguage.choices.forEach' in screen
        assert 'GeminiSpeechLanguage.choices.forEach' in stand
        assert 'Do not translate' in rules

    def test_cache_changes_identity_and_old_audio_remains_accessible(self):
        assert '"gemini-sample-v3"' in src('GeminiPreviewIdentity.kt')
        assert 'cache.listByVoice(selected.id)' in src('GeminiVoiceCenterScreen.kt')
        assert 'cache.find(sampleId(voice))' in src('GeminiVoiceCenterScreen.kt')
        assert 'samplePrefs.language()' in src('GeminiVoiceCenterScreen.kt')

if __name__ == '__main__':
    unittest.main()
