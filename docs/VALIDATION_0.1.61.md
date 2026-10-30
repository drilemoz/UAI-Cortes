# Uai Cortes 0.1.61: refatoração de sessão por ViewModel

## Escopo
- `EditorProjectViewModel`: ciclo de vida do cadastro de projetos separado da composição. `SavedStateHandle` armazena **apenas ID**, restaurando o JSON existente via `ProjectStore`.
- `ProjectSessionCoordinator`: operações de salvar/abrir/excluir/clipes anexados e snapshots coerentes, com testes de regressão.
- `MainActivity`: retira `remember` de lista/projeto e dependência direta do `ProjectController`; `EditorWorkspaceState` de navegação permanece em `rememberSaveable`.
- UI de importação, progressos de MP4 e reconhecimento offline ficam com seu ciclo de vida anterior. Sem alterações no JSON schemaVersion=4.

## Verificações locais
- `python3 tools/validate_source.py` (estrutura e metadados).
- Compilação isolada Kotlin/JVM de `ProjectOperations.kt`, `ProjectSessionCoordinator.kt` e testes com modelos de stub.
- Análise sintática Kotlin e verificação CRC do ZIP.

## Testes Android pendentes
- `:app:testDebugUnitTest :app:lintDebug :app:assembleRelease` no GitHub Actions (SDK 36).
- Rotacionar editor após salvar, abrir e excluir projeto; simular recuperação após encerramento do processo; verificar que só o ID é restaurado.
- Validar SAF, importação cancelável, PiP e saída MP4 em dispositivo real. ViewModel não retoma exportações nem leituras de mídia canceladas.
