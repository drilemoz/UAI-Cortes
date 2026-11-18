#!/usr/bin/env python3
"""Contratos do novo catálogo e da alternativa Arquivos, sem simular permissões do Android."""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
src = root / 'app/src/main/java/br/com/uaicortes'
read = lambda file: (src / (file + '.kt')).read_text()
picker = read('DeviceAudioPicker')
manifest = (root / 'app/src/main/AndroidManifest.xml').read_text()
assert 'android.permission.READ_MEDIA_AUDIO' in manifest
assert 'android.permission.READ_EXTERNAL_STORAGE" android:maxSdkVersion="32"' in manifest
assert 'SDK_INT >= 33' in picker and 'Manifest.permission.READ_MEDIA_AUDIO' in picker
assert 'ActivityResultContracts.RequestPermission()' in picker
assert 'MediaStore.getExternalVolumeNames(context)' in picker
assert 'MediaStore.Audio.Media.EXTERNAL_CONTENT_URI' in picker
assert 'ContentUris.withAppendedId(collection' in picker
assert 'withContext(Dispatchers.IO)' in picker and 'cursor.use' in picker
assert 'IS_MUSIC' not in picker
assert 'compareByDescending<DeviceAudio> { it.dateAdded }' in picker
assert 'DeviceAudioSearch.matches' in picker and 'DeviceAudioSearch.isWave' in picker
assert 'LazyColumn' in picker and 'onPick(item.uri)' in picker
assert 'ActivityResultContracts.OpenDocument()' in picker
assert 'arrayOf("audio/*", "video/*")' in picker
assert 'OutlinedButton(onClick = onFiles' in picker
assert 'rememberUpdatedState(onResult)' in picker and 'callback(null)' in picker
assert 'DeviceAudioState.FAILED' in picker and 'nenhum' in picker.lower()

routes = {
    'ClipAudioScreen': ('val picker = rememberDeviceAudioPicker()', 'picker()'),
    'QuickAudioDialog': ('val choose = rememberDeviceAudioPicker()', 'choose()'),
    'AudioClipEditorScreen': ('val pickAudio = rememberDeviceAudioPicker()', 'pickAudio()'),
    'VoiceIaScreen': ('val externalPicker = rememberDeviceAudioPicker()', 'externalPicker()'),
    'VoiceCloneStudioDialog': ('rememberDeviceAudioPicker(wavOnly = true)', 'referencePicker()'),
    'AudioReferenceAlignmentUi': ('rememberDeviceAudioPicker(includeVideoInFiles = true)', 'externalPicker()'),
    'MainActivity': ('val audioPicker = rememberDeviceAudioPicker()', 'audioPicker()'),
    'ProjectMediaRepairScreen': ('rememberDeviceAudioPicker(onResult = replaceMedia)', 'if (target.mimePrefix == "audio/") audioPicker()'),
}
for name, (picker_call, click_call) in routes.items():
    body = read(name)
    assert picker_call in body and click_call in body, name
    assert '.launch(arrayOf("audio/*"' not in body, name
assert 'ProjectMediaCopies.Kind.AUDIO' in read('ClipAudioScreen')
assert 'ProjectMediaCopies.Kind.AUDIO' in read('QuickAudioDialog')
repair = read('MediaRelinkVerification')
assert repair.index('length >= target.minimumSourceEndMs') < repair.index('ProjectMediaCopies.Kind.AUDIO')
assert 'takePersistableUriPermission' in repair
assert (root / 'app/src/test/java/br/com/uaicortes/DeviceAudioSearchTest.kt').exists()
print('OK: seletor de áudio unificado, permissões por versão, arquivos e cópia privada')
