# Validação — Uai Cortes 0.1.184

## Alterações
- SubtitleScreen: compactação de controles, menus, seguimento da lista, posição do zoom, filtro e ações avançadas.
- EditorTimelineUi: timeline continua exibindo cues filtrados/esmaecidos sem excluir ou alterar dados.
- Sem mudança de `schemaVersion=4` ou API do Gemini/Vosk.

## Testes locais
- `tools/test_subtitle_compact_controls_184_contract.py`
- `tools/check_version_sync.py`, `tools/validate_source.py` e regressões `tools/test_*.py`.
- Kotlin PSI para sintaxe.

## Pendências
- Gradle assembleRelease, testDebugUnitTest, lint, testes no celular e avaliação de overflow/gestos nos dispositivos.
- Conferir ação de Salvar sobre rascunho e navegação com projetos legados; testes de contrato não comprovam renderização real.
