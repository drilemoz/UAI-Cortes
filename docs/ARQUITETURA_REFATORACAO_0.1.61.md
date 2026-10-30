# Arquitetura Uai Cortes 0.1.61

`UaiApp()` (UI): navegação, dialogs, estados temporários do editor e launchers SAF.

`EditorProjectViewModel` (ciclo de vida Android): guarda projeto/lista carregados e ID ativo (SavedStateHandle). Possui `ProjectStore` com applicationContext, não guarda Activity nem URI de tarefa em Bundle.

`ProjectSessionCoordinator` (puro): aceita operações através de `ProjectController` e publica snapshots somente após sucesso. Recupera o projeto pelo ID; ID inválida produz seleção vazia.

`ProjectController` -> `StoredProjectRepository` -> `ProjectStore` (`schemaVersion=4`, backup fail-closed).

`EditorWorkspaceState` continua `rememberSaveable` na UI, com ID de seleção, posição e rota. Em mudanças do projeto, é revalidado; não há transcrição ou exportação recuperada automaticamente.

**Etapas futuras**: se o workflow Gradle aprovar, extrair estado dos processos de exportação (somente observação/progresso) para um serviço gerenciado pelo ciclo de vida com cancelamento explícito e propagação dos erros, sem converter um job Media3 em `SavedStateHandle`.
