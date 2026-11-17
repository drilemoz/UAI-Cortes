#!/usr/bin/env python3
from pathlib import Path
root = Path(__file__).resolve().parents[1]
src = root / 'app/src/main/java/br/com/uaicortes'
read = lambda name: (src / (name + '.kt')).read_text()
for file in ('AnimateImagesScreen', 'ImageAnimationEngine', 'ImageAnimationDraftStore',
    'FlowStudioScreen', 'FlowWorkspaceScreen', 'FlowPromptScreen', 'GeminiSceneImageClient'):
    assert not (src / (file + '.kt')).exists(), file
for route in ('animate', 'flow_workspace', 'flow_prompt', 'animate_images'):
    assert f'"{route}"' not in read('MainActivity')
    assert f'"{route}"' not in read('EditorNavigation')
assert read('HomeScreens').count('HomeActionTile(') == 7  # declaration plus six visible shortcuts
assert all(x not in read('HomeScreens') for x in ('Animar fotos', 'Estúdio de Cenas'))
assert 'j.optJSONObject("flowPromptDraft")?.toString()' in read('ProjectStore')
assert 'legacySceneDraftJson?.let' in read('ProjectStore')
assert '"image_insert" -> GalleryScope.IMAGES_ONLY' in read('MainActivity')
assert 'onAddImage = mainImagePick' in read('MainActivity')
assert 'ExportScreen(current' in read('MainActivity') and 'GalleryVideoSaver.save' in read('ExportEngine')
assert all(x in read('GeminiVideoCaptionClient') for x in ('countCaptionInput', 'voice_studio_tone', 'voice_studio_text'))
assert all(x not in read('GeminiVideoCaptionClient') for x in ('refineFlowScene', 'countFlowInput', 'studio_scenes'))
for tag in ('flowProject', 'flowRefs'):
    assert f'"{tag}"' in read('GeminiBatchStore')  # legacy batch metadata remains readable
assert (root/'app/src/test/java/br/com/uaicortes/LegacySceneProjectWatermarkTest.kt').is_file()
print('OK: retirada dos dois módulos e preservação de projetos, editor, voz e exportação')
