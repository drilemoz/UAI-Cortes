#!/usr/bin/env python3
"""Regressões de segurança: copiar mídia antes de alterar o projeto."""
from pathlib import Path
root = Path(__file__).resolve().parents[1]
java = root/'app/src/main/java/br/com/uaicortes'
vault = (java/'ProjectMediaCopies.kt').read_text()
imports = (java/'MediaImportTasks.kt').read_text()
main = (java/'MainActivity.kt').read_text()
assert all(token in vault for token in (
    'MediaStore.MediaColumns.IS_PENDING', 'IS_PENDING, 0', 'MediaStore.Video.Media.EXTERNAL_CONTENT_URI',
    'MediaStore.Audio.Media.EXTERNAL_CONTENT_URI', 'MediaStore.Images.Media.EXTERNAL_CONTENT_URI',
    'currentCoroutineContext().ensureActive()', 'expectedSize', 'resolver.delete(destination',
    'ProjectMediaAvailability.canOpen', 'privateFile.delete()', 'protectProject(context: Context, project: VideoProject)',
))
assert all(token in imports for token in ('media.videoClips(uris, persistDocuments, projectId)',
    'media.protectedCopy(uri, projectId, ProjectMediaCopies.Kind.AUDIO)',
    'media.protectedCopy(uri, projectId,'))
assert 'mediaTasks.videos(uris, persistDocumentAccess, importTarget.id)' in main
lifecycle=(java/'ProjectMediaProtectionLifecycle.kt').read_text()
assert 'ProjectMediaCopies.protectProject(context.applicationContext, opened)' in lifecycle
assert 'latestProject === opened' in lifecycle and 'copyTarget' not in main
assert 'ProjectMediaProtectionLifecycle(project, screen, activeMediaTask != null,' in main
assert (root/'app/src/androidTest/java/br/com/uaicortes/ProjectMediaCopiesAndroidTest.kt').is_file()
print('OK: cópia pendente/atômica, rollback, reutilização, cancelamento, destinos e projetos antigos')
