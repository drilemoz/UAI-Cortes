#!/usr/bin/env python3
"""Regressão estrutural do salvamento explícito (não substitui testes Android)."""
from pathlib import Path
r=Path(__file__).resolve().parents[1]
s=r/'app/src/main/java/br/com/uaicortes'
ui=(s/'MainActivity.kt').read_text()
vm=(s/'EditorProjectViewModel.kt').read_text()
draft=(s/'ProjectDraftSession.kt').read_text()
recovery=(s/'ProjectDraftRecoveryStore.kt').read_text()
dialogs=(s/'ProjectDraftUi.kt').read_text()
backup=(s/'ProjectBackups.kt').read_text()
backups_ui=(s/'ProjectBackupSettingsScreen.kt').read_text()
assert 'projectSession.stage(ClipAudioBindings.pruneDeleted(p))' in ui
assert 'projectSession.save(ClipAudioBindings.pruneDeleted(p))' not in ui
assert 'request.createNew) projectSession.beginNew(' in ui
assert 'projectSession.commit()' in ui and 'projectSession.commitCopy()' in ui
assert 'projectSession.finishWithoutSaving()' in ui
assert 'if (screen == "editor" && destination == "home")' in ui
assert 'pendingExitEditor = true' in ui
assert 'onSaveHome = { if (write(p)) leaveEditor("save") }' in ui
for label in ['EditorExitAction("Salvar"', 'Salvar como cópia',
              'EditorExitAction("Descartar"', 'EditorExitAction("Continuar"']:
    assert label in dialogs, label
for token in ['fun begin(', 'fun stage(', 'fun committed(', 'fun discard()', 'val dirty']:
    assert token in draft
assert 'fun stage(edited: VideoProject)' in vm and 'draft.stage(edited)' in vm
assert 'publish(session.save(edited))' in vm and 'publish(session.save(copy))' in vm
assert 'fun finishWithoutSaving()' in vm and 'publish(session.reload())' in vm
assert 'fun commitCopy()' in vm and 'UUID.randomUUID()' in vm
assert 'ProjectDraftRecoveryLifecycle' in ui
assert 'AtomicFile(' in recovery and 'sha256' in recovery and 'baselineHash' in recovery
assert 'if (generation.get() != version)' in recovery
assert 'fun clear()' in recovery and 'generation.incrementAndGet()' in recovery
assert 'ProjectBackupLifecycle(backupStore, if (projectSession.isEditingDraft) "" else activeProjectId' in ui
assert 'storageUsage' in backup and 'deleteOrphanedSnapshots' in backup
assert 'confirmOrphanCleanup' in backups_ui and 'Limpar backups órfãos' in backups_ui
assert (r/'app/src/test/java/br/com/uaicortes/ProjectDraftSessionTest.kt').exists()
assert 'schemaVersion", 4' in (s/'ProjectStore.kt').read_text()
print('OK: rascunho separado, confirmação de saída, backup seguro, cópia e recuperação com integridade')
