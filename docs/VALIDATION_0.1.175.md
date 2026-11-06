# Relatório de implementação — Uai Cortes 0.1.175 (versionCode 176)

## Base e compatibilidade

- Base de código-fonte: Uai Cortes 0.1.174 (versionCode 175), inspecionado com `AGENTS.md`.
- Preservado `schemaVersion=4` e campos antigos de projetos/credenciais.
- Integridade dos fluxos Gemini, YouTube, Meta e TikTok não foi alterada nesta fase.

## Implementação

1. **Tempo numérico** (`EditorTimelineUi.kt`): campo Compose com estado `TextFieldValue`; entrar no campo preserva o horário e seleciona os dígitos existentes, permitindo substituir ou editar parcialmente. O mascaramento mantém posicionamento do cursor e valores inválidos não são aplicados.
2. **PiP sem margem obrigatória** (`OverlayEditorLogic.kt`, `PipScreen.kt`, `EditorTimelineUi.kt`, `ExportEngine.kt`, `OverlayOperations.kt`, `OverlayPlacement.kt`, `TimelineDirectEdit.kt`): escala de 5% a 150%, borda tocando extremidades, gestos de zoom e modos Ajustar/Preencher. Enquadramento usa função normalizada compartilhada na prévia, verificação de colisão e exportação. Sobreposição intencional ainda exige confirmação.
3. **Persistência** (`ProjectStore.kt`): campo opcional `Overlay.fitMode` com default seguro `fit`, preservando `sourceStartMs`, `overlapWith`, opacidade e dados de versões anteriores.
4. **Timeline compacta** (`UnifiedEditorTimeline.kt`): mostra apenas linhas com conteúdo (mais a Vídeo base), cursor acompanha altura visível, botão Adicionar dá acesso a seis categorias mesmo quando vazias.
5. **Transições visuais** (`TransitionScreen.kt`): seleção inicial de seis efeitos preexistentes (Corte direto, Zoom cinematográfico, Recuo suave, Deslocamento lateral, Flash branco e Fade preto) com cartões gráficos; os demais preservados em Ver todos. A prévia maior segue utilizando quadros reais dos clipes. **Não implementada** dissolvência real por sobreposição nem virada 3D.

## Testes e resultados

- Validador estrutural `tools/validate_source.py`: **aprovado**.
- Scripts Python de contrato/regressão: **71 de 71 aprovados**, incluindo `test_editor_polish_175_contract.py`.
- Análise de sintaxe Kotlin PSI: **12 arquivos, nenhum erro sintático** (não equivale a build com dependências Compose/Android).
- Núcleo independente Kotlin `OverlayEditorLogic` + `OverlayPlacement`: **10 verificações de escala, margens, preservação de aspecto, porcentagem e colisão aprovadas** com `kotlinc`.
- Testes JUnit acrescentados em `OverlayEditorLogicTest.kt` e `OverlaySourceStartPersistenceTest.kt`; **não executados em Android/Gradle**.
- Build Android completo / Lint / MP4: **não executados**, porque não há Gradle nem SDK Android completo neste ambiente. Requer CI GitHub Actions e teste em aparelho.

## Limitações e próximos testes

- A edição livre de enquadramento de uma **imagem principal** (capa/clip da trilha de Vídeo), com pinça/pan e paridade Media3, **ainda não foi implementada**. Ajustar/Preencher aplica-se a PiP.
- Os seis cartões de transição representam efeitos reais **já existentes**; dissolvência cruzada, wipe/deslize de dois vídeos e virada 3D **não foram implementados** porque exigem composição com sobreposição temporal e testes de renderização.
- Zoom PiP de 100%–150% e modo Preencher precisam ser testados em telefones 9:16 e horizontais, observando GPU, consumo de memória e equivalência prévia vs MP4.
- Validar o comportamento dos teclados Android e seleção de partes do horário (incluindo backspace e confirmações).
- Em projetos com várias sobreposições simultâneas, confirmar a caixa de conflito e verificar que nenhuma posição é alterada silenciosamente.
- Falhas antigas de URI inacessível/Vosk permanecem pendentes de tratamento específico nesta versão.

## Principais arquivos alterados

`EditorTimelineUi.kt`, `OverlayEditorLogic.kt`, `OverlayOperations.kt`, `OverlayPlacement.kt`, `TimelineDirectEdit.kt`, `PipScreen.kt`, `ProjectStore.kt`, `ExportEngine.kt`, `UnifiedEditorTimeline.kt`, `TransitionScreen.kt`, testes `OverlayEditorLogicTest.kt`, `OverlaySourceStartPersistenceTest.kt`, `tools/test_editor_polish_175_contract.py`, `app/build.gradle.kts`, `AppReleaseNotes.kt`, `.github/workflows/android-apk.yml`, `tools/package_apks.py`, README, CHANGELOG, RELEASE_NOTES, AGENTS, VERSION, `app_identity.json`, `github-manager.json`.

## Recomendação de validação no aparelho

1. Na tela PiP: selecione o logotipo, escolha 100% em Ajustar, encoste o topo e confira o enquadramento; repita com 125% e Preencher.
2. Ajuste `Início` e `Fim`, verificando que o valor não some ao tocar e que o teclado não altera automaticamente para zero.
3. Na timeline, compare faixas com apenas vídeo e depois com foto/PiP/áudio/legenda; confira se o cursor acompanha todas as linhas visíveis.
4. Salve, feche e reabra o projeto: escala, modo PiP e tempos devem persistir.
5. Exporte em 720p/1080p e compare o logotipo renderizado ao da prévia.
6. Abra Transições e teste os seis cartões principais com dois clipes, verificando o efeito no arquivo final.
