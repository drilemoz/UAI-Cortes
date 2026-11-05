#!/usr/bin/env python3
"""Contratos offline do Estúdio. Não substituem Gradle/Android nem chamadas autorizadas."""
from pathlib import Path
p=Path(__file__).resolve().parents[1]
j=p/'app/src/main/java/br/com/uaicortes'
source=lambda name:(j/name).read_text()
assert 'FlowLegacyPromptScreen(project, onSave' in source('FlowStudioScreen.kt')
assert 'FlowBatchSection(project.id, draft' in source('FlowPromptScreen.kt')
assert 'class FlowStudioStorage(' in source('FlowStudioStorage.kt')
assert 'AtomicFile(' in source('FlowStudioStorage.kt')
assert 'read() // Não sobrescrever' in source('FlowWorkspaceStore.kt')
assert 'read()\n        require(' in source('FlowStudioStorage.kt')
assert 'ZipOutputStream(' in source('FlowStudioStorage.kt')
assert 'includeLocalUri = false' in source('FlowStudioStorage.kt')
assert 'ActivityResultContracts.OpenDocument()' in source('FlowStudioScreen.kt')
assert 'ActivityResultContracts.CreateDocument("application/zip")' in source('FlowStudioScreen.kt')
assert 'FileProvider.getUriForFile' in source('FlowStudioScreen.kt')
assert 'takePersistableUriPermission' in source('FlowStudioScreen.kt')
assert 'storySuggestion = FlowStudioAi.parseStory' in source('FlowStudioScreen.kt')
assert 'sceneSuggestions = FlowStudioAi.parseScenes' in source('FlowStudioScreen.kt')
assert 'pending = StudioJob("image", i)' in source('FlowStudioScreen.kt')
assert 'StudioImagePricing.note' in source('FlowStudioScreen.kt')
assert 'models.any { it.id == modelName }' in source('FlowStudioScreen.kt')
image=source('GeminiSceneImageClient.kt')
assert 'imageConfig' in image and 'aspectRatio' in image and '"9:16"' in image
assert 'listImageModels' in image and 'generateContent' in image
assert '"studio_image"' in image
assert '/models/veo' not in image.lower() and ':predict' not in image.lower()
assert '"studio_story"' in source('GeminiVideoCaptionClient.kt')
assert '"studio_scenes"' in source('GeminiVideoCaptionClient.kt')
assert '"studio_scene"' in source('GeminiVideoCaptionClient.kt')
assert '.put("schemaVersion", 4)' in source('ProjectStore.kt')
assert f'versionCode = {__import__("check_version_sync").check(p)[1]}' in (p/'app/build.gradle.kts').read_text()
assert (p/'app/src/test/java/br/com/uaicortes/FlowStudioModelTest.kt').is_file()
assert (p/'app/src/androidTest/java/br/com/uaicortes/FlowStudioStorageAndroidTest.kt').is_file()
print('OK: Estúdio 161, preservação do manual, SAF, modelos/imagens Gemini e fontes de teste.')
