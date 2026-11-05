#!/usr/bin/env python3
"""Contratos estáticos do Wizard; não substituem build Android nem testes de API."""
from pathlib import Path
r=Path(__file__).resolve().parents[1]
p=r/'app/src/main/java/br/com/uaicortes'
u=(p/'FlowStudioScreen.kt').read_text()
w=(p/'FlowStudioWizardUi.kt').read_text()
m=(p/'FlowStudioModel.kt').read_text()
l=(p/'StudioReferenceLibrary.kt').read_text()
a=(p/'FlowStudioAi.kt').read_text()
g=(p/'GeminiVideoCaptionClient.kt').read_text()
s=(p/'FlowStudioStorage.kt').read_text()
assert 'Estilo", "Personagens", "Cenários", "Cenas", "Flow' in w
assert 'StudioWizardStepHeader(draft.step' in u
for stage in ['StudioWizardStyle(', 'StudioWizardPeople(', 'StudioWizardPlaces(']: assert stage in u
assert 'wizardVersion", 2' in m and '0 -> 0' in m and '1 -> 3' in m and 'else -> 4' in m
assert 'class StudioReferenceLibrary(' in l and 'AtomicFile(' in l
assert 'openInputStream(uri)' in l and '12_000_000' in l and 'BitmapFactory.decodeByteArray' in l
assert 'studio_person' in g and 'studio_place' in g
assert 'enrichPerson(' in a and 'enrichPlace(' in a
assert 'StudioReferenceSuggestions.characterKinds' in w and 'StudioReferenceSuggestions.places' in w
assert 'onImport = { id -> galleryReference = "person" to id' in u
assert 'onImport = { id -> galleryReference = "place" to id' in u
assert 'Falas por personagem' in u and 'FilterChip(selected = firstLine.speaker == name' in u
assert 'Imagem da galeria salva. Ela não será enviada à API sem confirmação.' in u
assert 'referencias/personagens/' in s and 'referencias/cenarios/' in s
assert 'ready() = title.isNotBlank() && prompt.isNotBlank()' in m
assert 'FlowLegacyPromptScreen(project, onSave' in u
assert f'versionCode = {__import__("check_version_sync").check(r)[1]}' in (r/'app/build.gradle.kts').read_text()
print('OK: Wizard visual 5 etapas, sugestões locais, biblioteca, galeria, Gemini com consentimento, migração.')
