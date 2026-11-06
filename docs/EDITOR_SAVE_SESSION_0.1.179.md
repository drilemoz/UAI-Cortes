# Uai Cortes 0.1.179 — Sessões de edição, confirmação e backups

## Modelo de dados

`ProjectDraftSession` mantém uma versão salva de referência (quando existe) e uma versão temporária de trabalho; `EditorProjectViewModel.stage` substitui o antigo salvamento por gesto, sem gravar no `ProjectStore`.

`EditorProjectViewModel.commit` faz o único salvamento permanente da sessão em `ProjectStore`. `commitCopy` cria outro ID de projeto e título `(cópia)`. `finishWithoutSaving` abandona o rascunho e recarrega o catálogo persistido. A lista da Home mostra apenas projetos confirmados. Versão de esquema dos projetos continua 4.

## Navegação

Voltar do editor para a Home apresenta Salvar projeto / Salvar cópia (existente) / Descartar e sair / Continuar editando se o estado estiver alterado. Em um projeto limpo, a saída não precisa de confirmação. O botão Salvar presente no editor continua sendo confirmação explícita.

O botão Voltar das ferramentas retorna ao editor; não confirma nem descarta o conjunto inteiro. O MP4 exportado permanece independente do salvamento do projeto.

## Recuperação de sessões interrompidas

O rascunho privado `files/editor_draft_recovery.json` é escrito com `AtomicFile` e SHA-256 depois de uma pausa curta nas mudanças, ou na ida ao segundo plano. O arquivo é distinto do catálogo e do histórico. Na abertura seguinte, a Home solicita Recuperar edição ou Descartar rascunho. A recuperação verifica a integridade e se a versão salva do projeto não mudou; se mudou, recusa a recuperação para não sobrescrever dados mais recentes. O rascunho é removido após salvar ou descartar.

Não é possível garantir persistência dos últimos milissegundos da sessão caso o processo morra antes da escrita assíncrona. Não é feita cópia dos vídeos ou das músicas originais.

## Backups

`ProjectBackupLifecycle` fica inativo durante uma sessão de edição. Depois de salvar, um snapshot histórico é agendado a partir do projeto confirmado, quando o usuário deixou as cópias automáticas ativas. O catálogo `SharedPreferences/projects` tem sua própria cópia `projects_backup`, gerida pelo `ProjectStore`.

As cópias JSON históricas ficam em `files/project_backups/` e podem ser enviadas para pasta SAF autorizada pelo usuário. Em Salvamento e segurança, o aplicativo mostra contagem de backups, espaço local e quantidade de pastas órfãs. Excluir backups órfãos exige confirmação e atinge somente diretórios internos com nome gerado pelo hash do ID; backups externos, arquivos originais e projetos salvos não são apagados.

**Limitações:** a limpeza de imagens privadas importadas e não utilizadas ainda não foi automatizada para evitar excluir imagens referenciadas por backups antigos. A restauração e o novo fluxo precisam de testes instrumentados Android e testes físicos de perda de processo, rotação, seletor SAF e exportação.
