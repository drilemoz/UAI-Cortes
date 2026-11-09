#!/usr/bin/env python3
"""Regressão v241: dois motores, referências consentidas, HTTPS e falas protegidas."""
from pathlib import Path
from importlib.util import spec_from_file_location, module_from_spec
import base64
import io
import unittest

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT/'app/src/main/java/br/com/uaicortes'

class VoiceClone241(unittest.TestCase):
    def read(self, name): return (SRC/name).read_text()

    def test_dialog_and_character_binding(self):
        dialog = self.read('VoiceCloneStudioDialog.kt')
        data = self.read('VoiceIaVoiceAssignments.kt')
        ui = self.read('VoiceIaScreen.kt')
        for key in ('VoiceClonePolicy.CHATTERBOX', 'VoiceClonePolicy.COQUI',
            'referencePicker.launch(', 'consent && !working', 'onSave(project.copy(voiceIaVoices = revised))',
            'VoiceCloneClient.synthesize(', 'VoiceCloneAudio.prepare(',
            '"Gerar e inserir esta fala"', 'VoiceCloneStudioDialog('):
            self.assertIn(key, dialog + ui)
        self.assertIn('speakerCloneProfiles', data)
        self.assertIn('cueCloneProfiles', data)
        self.assertIn('"speakerCloneProfiles"', data)
        self.assertIn('"cueCloneProfiles"', data)

    def test_https_only_no_auto_calls_or_raw_secret(self):
        client = self.read('VoiceCloneClient.kt')
        dialog = self.read('VoiceCloneStudioDialog.kt')
        library = self.read('VoiceCloneLibrary.kt')
        self.assertIn('parsed.scheme == "https"',library)
        self.assertIn('connection.instanceFollowRedirects = false',client)
        self.assertIn('connection.setRequestProperty("Authorization", "Bearer $token")',client)
        self.assertNotIn('putString("token"', dialog)
        self.assertIn('"voice_clone_refs_v1"',library)
        self.assertIn('MAX_REFERENCE = 10L * 1024 * 1024',library)
        self.assertIn('out.fd.sync()',library)
        self.assertIn('VoiceCloneAudio.isClone',self.read('VoiceIaExternalAudio.kt'))

    def test_server_rejects_invalid_requests(self):
        import sys
        sys.path.insert(0, str(ROOT/'tools/voice_bridge'))
        import server
        ref = b'RIFF'+bytes(4)+b'WAVE'+bytes(130)
        ok = dict(engine='chatterbox_v3', text='Olá', language='pt',
                  voice_id='00000000-0000-0000-0000-000000000000',
                  reference_wav_b64=base64.b64encode(ref).decode())
        self.assertEqual(server.validate(ok)[0], 'chatterbox_v3')
        for change in (dict(engine='unsafe'), dict(language='en'),
                       dict(text=''), dict(text='x'*801), dict(reference_wav_b64='not-base64')):
            broken = dict(ok); broken.update(change)
            with self.assertRaises(ValueError): server.validate(broken)

if __name__ == '__main__': unittest.main()
