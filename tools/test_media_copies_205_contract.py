#!/usr/bin/env python3
"""Regressão: novas cópias de projeto privadas; mídias antigas preservadas."""
from pathlib import Path
root = Path(__file__).resolve().parents[1]
java = root/'app/src/main/java/br/com/uaicortes'
vault = (java/'ProjectMediaCopies.kt').read_text()
imports = (java/'MediaImportTasks.kt').read_text()
main = (java/'MainActivity.kt').read_text()
assert all(token in vault for token in (
    'FileProvider.getUriForFile', 'project_media_v1/',
    'currentCoroutineContext().ensureActive()', 'expectedSize', 'partial.renameTo(privateFile)',
    'ProjectMediaAvailability.canOpen', 'privateFile.delete()',
    'protectProject(context: Context, project: VideoProject)',
    'MediaStore.MediaColumns.RELATIVE_PATH', 'folder(projectId, kind)',
    'context.filesDir', 'output.fd.sync()',
))
# MediaStore é usado somente para identificar e manter cópias públicas LEGADAS.
# A função copy() não deve inserir arquivos novos na biblioteca de mídia do Android.
copy_body = vault.split('suspend fun copy(context: Context, source: Uri, projectId: String, kind: Kind): Uri', 1)[1]
assert 'resolver.insert(' not in copy_body
assert 'IS_PENDING' not in copy_body
assert all(token in imports for token in ('media.videoClips(uris, persistDocuments, projectId)',
    'media.protectedCopy(uri, projectId, ProjectMediaCopies.Kind.AUDIO)',
    'media.protectedCopy(uri, projectId,'))
assert 'mediaTasks.videos(uris, persistDocumentAccess, importTarget.id)' in main
lifecycle=(java/'ProjectMediaProtectionLifecycle.kt').read_text()
assert 'ProjectMediaCopies.protectProject(context.applicationContext, opened)' in lifecycle
assert 'latestProject === opened' in lifecycle and 'copyTarget' not in main
assert 'ProjectMediaProtectionLifecycle(project, screen, activeMediaTask != null,' in main
assert (root/'app/src/androidTest/java/br/com/uaicortes/ProjectMediaCopiesAndroidTest.kt').is_file()
assert 'name="project_media" path="project_media_v1/"' in (root/'app/src/main/res/xml/diagnostic_file_paths.xml').read_text()
print('OK: cópias privadas, conclusão atômica, rollback, reuso e referências públicas legadas preservadas')
