# Uai Cortes 0.1.57 | estados, transições e importação

## Causa da falha no CI 50

`EditorWorkspaceStateTest.kt:62` esperava índice 1 para a última junção de uma lista com **dois clipes**. As junções são indexadas de zero: o único índice válido é 0. O método de normalização estava correto, a asserção estava incorreta. O Gradle compilou Kotlin e parou em 1 de 166 testes.

## Continuação da refatoração

- `TransitionBoundaryPolicy.kt`: regra independente de interface para contar, validar e limitar índices de junção.
- `EditorWorkspaceState.reconcile` usa essa regra; `MainActivity` reage a mudanças no objeto do projeto (e não só no ID).
- `TransitionScreen` faz o acesso aos dois clipes com um índice validado imediatamente antes da prévia e aplicação.
- `MediaImportJobPolicy.kt`: preserva o ID do projeto de origem de cada seleção assíncrona, impedindo que resultados tardios sejam aplicados a outra edição.
- `MainActivity.importVideoClips`: executa consultas ao sistema de arquivos fora da thread principal via `Dispatchers.IO`, apresenta estado de importação sem progresso fictício e bloqueia pedidos duplicados.
- `verifiedMediaDuration`: retorna 0 quando o metadado real não existe ou falha. Usado em novas importações; `mediaDuration` preserva fallback histórico para outros fluxos.

## Compatibilidade e limites

Não houve migração de projetos (schemaVersion 4), alteração em codecs/bitrate da exportação ou nova permissão Android. Os testes isolados não equivalem a `:app:testDebugUnitTest`/lint/build no CI. Cancelamento/reinício de importação após destruição de Activity não é retomado automaticamente.
