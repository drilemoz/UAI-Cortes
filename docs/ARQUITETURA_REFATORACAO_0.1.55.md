# Uai Cortes 0.1.55: operações reutilizáveis

## Topologia

`MainActivity/UaiApp` mantém Compose, Launchers SAF, AlertDialogs, navegação e Toast. Não implementa o algoritmo de alocação de PiP nem a montagem de clipes/áudio.

- `ProjectOperations.kt`: `ProjectRepository` isola o armazenamento; `StoredProjectRepository` adapta o `ProjectStore`; `ProjectController` retorna `ProjectSnapshot` depois de persistir, abre projeto pelo ID e compõe novos clipes sem alterar entradas existentes.
- `OverlayOperations.kt`: recebe dimensões de mídia por injeção de função; calcula conflitos e resolve layout sem Context. Resultados tipados `OverlayUpdate` e `OverlayInsertion` distinguem aprovação pendente, falhas e sucesso. Incluir/quitar PiPs mantém até quatro camadas.
- `MediaImportOperations.kt`: `MediaImportRules` valida duração de entrada, deduplica a seleção de imagens, limita quantidade e recorta a duração de áudio ao projeto. `MediaImportController` faz leitura Android de mídia e pede acesso persistível quando o seletor SAF oferece a concessão.

## Compatibilidade

Nenhum campo do JSON de projeto foi alterado, e a versão do schema permanece 4. As telas, diálogos de colisão e exportadores continuam os mesmos. `ImageAnimationEngine` não foi modificado. A refatoração não garante a correção do fallback de duração de mídia para arquivo ilegível, nem o sucesso de permissões SAF após o provedor revogar acesso.

## Próxima etapa sugerida

Migrar gradualmente operações assíncronas e gerenciamento de erros das telas para controladores com escopos de ciclo de vida testáveis. Priorizar antes a compilação Android da 0.1.55 e testes de regressão no dispositivo.
