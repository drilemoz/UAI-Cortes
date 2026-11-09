#!/usr/bin/env python3
"""Testes estáticos da nova ação Renomear, preservando dados dos projetos."""
from pathlib import Path
r=Path(__file__).resolve().parents[1]
src=r/'app/src/main/java/br/com/uaicortes'
ui=(src/'HomeScreens.kt').read_text()
main=(src/'MainActivity.kt').read_text()
store=(src/'ProjectStore.kt').read_text()
controller=(src/'ProjectOperations.kt').read_text()
session=(src/'ProjectSessionCoordinator.kt').read_text()
viewmodel=(src/'EditorProjectViewModel.kt').read_text()
dialog=(src/'HomeProjectDialogs.kt').read_text()
assert 'Icon(Icons.Default.Edit, "Renomear ${p.title}")' in ui
assert 'projects.take(5).forEach { ProjectItem(it,onOpen,onRename,onDelete) }' in ui
assert 'ProjectItem(it,open,rename,delete)' in ui
assert 'onRename = { pendingProjectRename = it; projectRenameInput = it.title }' in main
assert '{ pendingProjectRename = it; projectRenameInput = it.title }' in main
assert 'RenameProjectDialog(target.title, projectRenameInput,' in main
assert 'projectSession.rename(target.id, projectRenameInput)' in main
assert 'fun rename(id: String, title: String): VideoProject' in store
assert 'current.firstOrNull { it.id == id }' in store
assert 'original.copy(title = normalized, updated = System.currentTimeMillis())' in store
assert 'persist(current.map { if (it.id == id) renamed else it })' in store
assert 'override fun rename(id: String, title: String): VideoProject = store.rename(id, title)' in controller
assert 'repository.rename(id, title)' in controller
assert 'accept(controller.rename(id, title, snapshot.activeId))' in session
assert 'check(!hasPendingChanges)' in viewmodel
assert 'ProjectNaming.validTitle(name)' in dialog and 'normalized != currentTitle' in dialog
assert 'projectSession.rename' in main
print('OK: renomear nas listas, persistência de título isolada, diálogo validado e falha protegida')
