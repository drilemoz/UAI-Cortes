# Uai Cortes 0.1.54: estado e navegação recuperáveis

## Componentes adicionados ou alterados

- `EditorNavigation.kt`: rotas conhecidas, ação Voltar e bloqueio de telas sem projeto após restauração. Não é uma biblioteca genérica de navegação; usa a navegação existente.
- `EditorWorkspaceState.kt`: `Saver` compacto para rota, seleção do clipe/áudio/legenda/PiP, limite de transição e posição. `reconcile()` verifica IDs e limites contra o `VideoProject` recuperado.
- `MainActivity.kt`: guarda `activeProjectId` por `rememberSaveable`, busca o projeto em `ProjectStore` e usa a regra compartilhada para Voltar. Os controladores de galeria/SAF e a lista de URIs de imagens recebem savers pequenos. Os dados persistentes continuam no armazenamento normal.

## Escopo real da recuperação

O Android restaura o estado *somente quando disponibiliza um Bundle de instância* após recriação normal da Activity. Reabrir manualmente após force-stop, limpar dados ou reiniciar o dispositivo não garante o retorno à tela anterior. O projeto já salvo permanece no `ProjectStore` e pode ser reaberto na lista de projetos. Os rascunhos internos das ferramentas, estados de players, execução do Media3 Transformer e trabalhos em andamento não são retomados automaticamente.

## Segurança e compatibilidade

- Nenhuma migração de projeto ou novo campo de JSON: `schemaVersion=4` permanece intacto.
- Somente strings, posições e URIs de itens selecionados são salvos no estado de instância. Limitam-se imagens a `ImageAnimationPlan.MAX_IMAGES`; nenhum bitmap ou vídeo em memória é serializado.
- Ausência do projeto ou rota desconhecida redireciona à Home para evitar telas sem conteúdo.
- As URIs dependem das permissões do Android: a restauração não amplia permissões expiradas ou revogadas.

## Próximos passos possíveis

Para desacoplar totalmente a `MainActivity`, extrair a coordenação de operações de projetos/overlays para controladores testáveis e avaliar ViewModel para cada recurso somente após compilação e testes reais desta versão.
