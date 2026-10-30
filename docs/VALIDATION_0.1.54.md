# Uai Cortes 0.1.54: validação de código-fonte

## Planejamento de testes de regressão

- Roda `python3 tools/validate_source.py` para versionamento, compatibilidade e contratos atuais.
- Usa parser Kotlin PSI para checagem de sintaxe de produção/testes; **não** substitui compilação de tipos Android/Compose.
- Compilação Kotlin/JVM isolada de `EditorWorkspaceState` e `EditorNavigation` com stubs de modelos/Compose, seguida de asserções básicas de restauração; não é compilação Gradle do aplicativo.
- Adiciona `EditorWorkspaceStateTest` (serialização reduzida, snapshot inválido, projeto removido, seleções antigas) e `EditorNavigationTest` (rotas de retorno, home sem projeto e galeria de criação sem projeto). Execução via JUnit Gradle permanece pendente.

## Pendente de CI/aparelho

1. Gradle `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:assembleRelease` e verificação das cinco variantes.
2. Testar giro de tela e restauração de Activity enquanto editor, legendas, PiP, áudio, galeria de mídia e Animar Imagens estão abertos.
3. Testar exclusão de um projeto por outra operação e restauração sem dados presentes: deve cair na Home e jamais apagar mídias originais.
4. Testar projetos anteriores à 0.1.53, clipes cortados, PiP/legenda selecionados e acesso SAF revogado.
5. Não afirmar retomada de exportação/ASR após matar o processo. Verificar cancelamento/limpeza de temporários manualmente.

ZIP contém código-fonte, não APK.
