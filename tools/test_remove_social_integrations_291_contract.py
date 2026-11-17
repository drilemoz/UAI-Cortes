#!/usr/bin/env python3
"""No social SDK/routes/clients, without regressions in editing and local sharing."""
from pathlib import Path
root = Path(__file__).resolve().parents[1]
app = root / 'app/src/main/java/br/com/uaicortes'
read = lambda f: (app / (f + '.kt')).read_text(encoding='utf-8')
prod = '\n'.join(p.read_text(encoding='utf-8') for p in app.glob('*.kt') if p.name != 'AppReleaseNotes.kt')
manifest = (root / 'app/src/main/AndroidManifest.xml').read_text(encoding='utf-8')
gradle = (root / 'app/build.gradle.kts').read_text(encoding='utf-8')
ci = (root / '.github/workflows/android-apk.yml').read_text(encoding='utf-8')
for token in ('SocialDashboardScreen', 'SocialStore', 'onSocialPublish', '"social_publish"',
              '"social_accounts"', 'CloudUpload', 'graph.facebook.com', 'youtube.googleapis.com',
              'www.googleapis.com/upload/youtube', 'open.tiktokapis.com'):
    assert token not in prod, token
assert not any(p.name.startswith(('Social', 'YouTube')) for p in app.glob('*.kt'))
for token in ('facebook_app_id', 'fb_login_protocol_scheme', 'facebook-login', 'com.facebook.',
              'play-services-auth', 'META_APP_ID', 'META_CLIENT_TOKEN', 'TIKTOK_CLIENT_KEY'):
    assert token not in gradle + ci + manifest, token
assert 'android.permission.INTERNET' in manifest and 'FileProvider' in manifest
main = read('MainActivity')
for route in ('"api_center" -> VoiceServicesScreen(', 'GeminiSettingsScreen(::back)',
              'PollySettingsScreen(::back)', 'ElevenLabsSettingsScreen(::back)',
              'StandaloneTextToVoiceScreen(', 'VoiceIaScreen(', 'AutoSubtitleScreen('):
    assert route in main, route
assert 'IA e voz' in read('VoiceServicesScreen') and 'IA e voz' in read('HomeScreens')
assert 'SubtitleLipSyncDialog' in read('SubtitleScreen')
assert 'Intent(Intent.ACTION_SEND)' in read('VideoSavedDialog')
assert 'Intent(Intent.ACTION_VIEW)' in read('VideoSavedDialog')
assert 'publicationDraft' in read('ProjectStore') and 'VideoPublicationDraft.read' in read('ProjectStore')
assert 'ExportDestination.VERTICAL' in read('ExportScreen') and 'ExportDestination.VERTICAL' in read('SubtitleExportPreview')
assert 'VideoExportFormatPolicy.dimensions(' in read('ExportEngine')
print('OK: integração social removida; voz, legendas, dados antigos e exportação local preservados')
