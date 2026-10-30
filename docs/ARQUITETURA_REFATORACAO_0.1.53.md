# Arquitetura de reaproveitamento: Uai Cortes 0.1.53

## Alterações efetivamente implementadas

- `MainActivity.kt`: inicialização, rotas atuais e orquestração de projetos.
- `HomeScreens.kt`: Home, Configurações, Projetos e componentes compartilhados existentes.
- `EditorScreen.kt` + `EditorTimelineUi.kt`: editor e controles da timeline.
- `TrimScreen.kt`, `AudioScreen.kt`, `PipScreen.kt`, `TransitionScreen.kt`, `SubtitleScreen.kt`, `TextWatermarkScreen.kt`: ferramentas especializadas.
- `ExportScreen.kt`, `PreviewMediaUtils.kt`: interface de exportação, player e utilitários de prévia.
- `EditorWorkspaceState.kt`: estado observável transitório de tela e seleção.
- `GalleryVideoSaver.kt`: gravação MediaStore com publicação pendente e rollback.
- `ProjectCollectionCodec` / `ProjectStore`: codec JSON e recuperação por cópia anterior válida.

Os arquivos continuam no mesmo módulo Gradle e pacote Kotlin para reduzir a chance de mudança de APIs e dependências. A arquitetura é modular por arquivos/funcionalidades; não foi introduzido multi-module Gradle ou uma interface genérica de plugins.

## Como reaproveitar em futuras funcionalidades

1. Novas telas devem consumir os modelos `VideoProject`, `Clip`, `SubtitleCue`, `AudioTrack` e classes puras da timeline, sem serialização própria.
2. Geração de vídeos pode ter motores específicos, mas deve usar `GalleryVideoSaver.save(context, file, name)` para publicar o MP4.
3. Estados temporários de seleção/navegação pertencem ao `EditorWorkspaceState`; estado editável de rascunho permanece no fluxo de cada ferramenta.
4. A evolução das imagens dentro da timeline, restauração de processo e motor de efeitos unificado deve acontecer em novas versões, com testes para duração, áudio e fidelidade da prévia.

## Compatibilidade e precauções

- `schemaVersion=4` inalterado; clipes, PiPs, áudio, legendas e marca de texto continuam em JSON compatível.
- `ProjectStore` agora usa `projects_backup` no mesmo SharedPreferences. `load()` pode exibir lista vazia quando ambas as fontes estiverem ilegíveis, mas `save()`/`delete()` recusam alterar os dados; por isso a UI ainda precisa de alerta/recuperação dedicada em versão futura.
- O comportamento anterior da importação com duração de fallback de 1 segundo ainda existe; não foi alterado por exigir avaliação dos fluxos de mídia e teste real.
- Os componentes continuam com estado Compose `remember` em diversas telas. Não há, nesta versão, `ViewModel`, `SavedStateHandle` ou reinício garantido de tarefas de renderização após morte do processo.
- Progresso, MediaStore e preview/render devem ser validados fisicamente. Android 8/9 merece teste específico da escrita MediaStore; não há promessa de suporte completo sem o teste.
