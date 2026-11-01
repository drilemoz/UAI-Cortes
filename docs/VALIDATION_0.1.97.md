# Validação v0.1.97 / versionCode 98 — 01/11/2026

## Escopo

- Origem: ZIP v0.1.96, código e metadados preservados.
- `SubtitleNarrationReuse`: ordenar legendas pelo horário, ignorar cues vazios/inválidos e compor texto selecionado localmente. Limite de 4000 caracteres, sem truncagem.
- Janela Gerar por IA: abrir lista de legendas por ação do usuário; pré-visualizar, marcar/desmarcar, confirmar substituir rascunho; sem chamar ASR ou API de síntese durante a importação.
- Lista de vozes: abrir sem consulta automática; consulta é feita por toque; confirmação explícita da voz.
- Inserção de narração na timeline: confirmação antes de inserir; preservação de legendas e áudio existentes.
- Dados: sem novas propriedades no `VideoProject`; `schemaVersion=4`, sem migração; cinco APKs Release previstos no CI.

## Testes e limitações

- `SubtitleNarrationReuseTest.kt`: ordem temporal, seleção parcial, entrada vazia, IDs duplicados, caracteres acima do limite; compilação e testes Kotlin/JVM locais.
- `tools/validate_source.py`: verificações estruturais de políticas de interação, versionamento e regressões.
- PSI Kotlin: validação de sintaxe dos arquivos Kotlin modificados.
- Não foram executados build completo Android, Lint Android nem testes UI ou autenticação real Gemini. Validar `:app:testDebugUnitTest :app:lintDebug :app:assembleRelease` no GitHub Actions, depois testar as confirmações e reprodução em dispositivo.
