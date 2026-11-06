# Uai Cortes 0.1.181 — Validação da prévia de legendas

## Problema

A tela de Legendas exibia um quadro preto quando `p.subtitles` estava vazia porque o player era montado apenas no ramo `selected != null`. A timeline `SubtitleCompactTimeline` desenhava só segmentos de legendas, sem clipes.

## Mudança

- Prévia independente da existência de legendas, alimentada pelo `ClipMediaItems.playback` compartilhado com o editor principal, inclusive fotos.
- Cursor absoluto compartilhado entre reprodução, slider, régua e criação manual de legenda; somente solicitações explícitas de seek reposicionam o player, para evitar loops de polling.
- Faixa de referência dos clipes acima das legendas, com miniaturas por janela visível, sem trocar as durações ou URIs dos projetos.
- Nova legenda criada no cursor, com validação dos limites pelo `SubtitleListNavigation.newInterval` existente.

## Validação

- Testes de cálculo em `SubtitleCompactLayoutTest.kt`, `SubtitleListNavigationTest.kt` e contrato `tools/test_subtitle_video_preview_181_contract.py`.
- Compilação Android e exibição/seek no aparelho ainda pendentes; registrar resultados reais após CI.

## Compatibilidade

`schemaVersion=4`, rascunhos, backups, permissões e cinco variantes APK Release preservados. Não modificar Media3 exportador nem fazer salvamento permanente ao mover o cursor.
