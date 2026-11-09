"""Contratos estaticos da Central de Vozes 0.1.190; nao substituem build Android."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / 'app/src/main/java/br/com/uaicortes'


def source(name):
    return (APP / name).read_text(encoding='utf-8')


class VoiceCenter190Contract(unittest.TestCase):
    def test_central_reused_across_screens(self):
        self.assertIn('"voice_center" -> GeminiVoiceCenterScreen(', source('MainActivity.kt'))
        self.assertIn('Vozes Google', source('HomeScreens.kt'))
        self.assertIn('GeminiVoiceCenterScreen(onBack', source('VoiceIaScreen.kt'))
        self.assertIn('GeminiVoiceCenterScreen(onBack', source('GeminiNarrationUi.kt'))

    def test_local_catalog_not_refetched_by_default(self):
        library = source('GeminiVoiceLibrary.kt')
        center = source('GeminiVoiceCenterScreen.kt')
        self.assertIn('prefs.getBoolean("auto_refresh", false)', library)
        self.assertIn('library.load()', center)
        self.assertIn('if (loaded && autoRefresh) fetchFromGoogle()', center)
        self.assertIn('enabled = !working, onClick = { scope.launch { fetchFromGoogle() } }', center)

    def test_failed_or_empty_fetch_preserves_catalog(self):
        library = source('GeminiVoiceLibrary.kt')
        center = source('GeminiVoiceCenterScreen.kt')
        self.assertIn('require(cleaned.isNotEmpty()', library)
        self.assertIn('catalog.json.part', library)
        self.assertIn('if (fetched.voices.isEmpty())', center)
        self.assertIn('Seu catálogo anterior foi preservado', center)
        self.assertIn('O catálogo salvo continua disponível', center)

    def test_previews_are_durable_without_repeat_api(self):
        cache = source('GeminiVoicePreviewCache.kt')
        center = source('GeminiVoiceCenterScreen.kt')
        self.assertIn('File(appContext.filesDir, "gemini-voice-previews")', cache)
        self.assertNotIn('30L * 24 * 60 * 60', cache)
        self.assertIn('val local = withContext(Dispatchers.IO) { cache.find(sampleId(voice)) }', center)
        self.assertIn('else pendingSample = voice', center)
        self.assertIn('pode consumir cota ou gerar cobrança', center)
        self.assertIn('cache.save(id, wave.bytes, voice.id)', center)
        self.assertIn('library.setLimitMb(amount)', center)

    def test_fixed_phrase_and_cache_identity(self):
        identity = source('GeminiPreviewIdentity.kt')
        self.assertIn('Este é o Uai Cortes.', identity)
        self.assertIn('Fácil, não é?', identity)
        self.assertIn('Agora... vamos ouvir', identity)
        self.assertIn('"gemini-sample-v2"', identity)
        self.assertIn('model, voiceId, styleInstruction, TEXT', identity)
        self.assertNotIn('listOf("gemini-sample-v2", apiKey', identity)

    def test_voice_and_tone_are_separate(self):
        voice_ui = source('VoiceIaScreen.kt')
        self.assertIn('label = if (project.subtitleSpeakers.isEmpty()) "Vozes" else "Personagens"', voice_ui)
        self.assertIn('Icons.Default.Mood, "Alterar tom da fala', voice_ui)
        self.assertIn('voicePickerSpeakerId', voice_ui)
        self.assertIn('speakerVoiceOverrides', voice_ui)
        self.assertIn('stylePickerCueId = cue.id', voice_ui)

    def test_catalog_never_contains_credentials(self):
        library = source('GeminiVoiceLibrary.kt')
        self.assertIn('.put("id", v.id)', library)
        self.assertIn('.put("pitch", v.pitch', library)
        self.assertNotIn('apiKey', library)
        self.assertNotIn('privateKey', library)


if __name__ == '__main__':
    unittest.main()
