#!/usr/bin/env python3
"""Contrato da descrição recuperável. Não substitui build Android nem testes instrumentados."""
from pathlib import Path
root = Path(__file__).resolve().parents[1]
s = root / 'app/src/main/java/br/com/uaicortes'
model = (s/'ProjectStore.kt').read_text()
main = (s/'MainActivity.kt').read_text()
export = (s/'ExportScreen.kt').read_text()
saved = (s/'VideoSavedDialog.kt').read_text()
editor = (s/'EditorScreen.kt').read_text()
publication = (s/'ProjectPublicationScreen.kt').read_text()
nav = (s/'EditorNavigation.kt').read_text()
images = (s/'AnimateImagesScreen.kt').read_text()
assert 'val publicationDraft: VideoPublicationDraft? = null' in model
assert '.put("publicationDraft", publicationDraft?.json())' in model
assert 'VideoPublicationDraft.read(j.optJSONObject("publicationDraft"))' in model
assert '.put("schemaVersion", 4)' in model
assert 'MAX_FIELD_LENGTH = 400' in model
assert 'initialPublicationDraft=p.publicationDraft' in export
assert 'onPublicationDraftSave=onPublicationDraftSave' in export
assert 'projectSession.project?.takeIf { it.id == current.id }' in main
assert 'write(it.copy(publicationDraft = draft))' in main
assert 'LaunchedEffect(uri, description, hashtags)' in saved
assert 'delay(650)' in saved
assert 'if (persistDraft()) onClose()' in saved
assert 'if (persistDraft()) onEdit(currentDraft())' in saved
assert 'onUseInEditor(uri, draft)' in images and 'if (onUseInEditor(uri, draft)) animationVm.dismissSaved()' in images
assert 'publicationDraft=publicationDraft.takeUnless' in main
assert 'Triple("Descrição", Icons.Default.Description, "publication")' in editor
assert '"publication"' in nav and '"publication" ->' not in nav
assert 'ProjectPublicationScreen(current' in main
assert 'VideoCaptionPolicy.isValid(description, hashtags)' in publication
assert 'delay(650)' in publication
assert 'if (persist()) back()' in publication
assert 'MAX_CHARACTERS' in (s/'VideoCaptionPolicy.kt').read_text()
assert (root/'app/src/test/java/br/com/uaicortes/ProjectPublicationDraftTest.kt').exists()
print('OK: rascunho persistente por ID, editor reutilizável, backup/JSON legado, animação e validação de 100 caracteres')
