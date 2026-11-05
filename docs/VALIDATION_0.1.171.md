# Validação — Uai Cortes 0.1.171

## Mudanças
- `TrimScreen.kt`: UI visual de edição, controles fixos, undo/redo, menu de ferramentas, saída para o editor.
- `TrimVisualTimeline.kt`: 12 miniaturas decodificadas em IO e gestos para navegar, mudar o trecho a manter/remover.
- `TrimTimelineEdit.kt`: exclusão de intervalo de fonte (sem regravar) e ajuste de tempos absolutos globais de legendas, PiP e áudio não vinculado.
- `MainActivity.kt`: `Pronto` retorna ao editor principal após persistência.
- `TrimVisualRedesignTest.kt`: cobertura de mapeamento temporal, limites, união, transições e exclusões.
- `tools/test_trim_visual_171_contract.py`: contrato de UI e segurança.

## Limites de teste
- Os testes Python e a análise sintática não equivalem à compilação Android.
- Ainda é necessário executar `:app:assembleRelease`, `:app:testDebugUnitTest`, `:app:lintDebug`, testes instrumentados e exportação real no GitHub Actions ou SDK Android configurado.
- Confirmar em aparelho as alças com gestos, uso em telas pequenas, áudio vinculado a clipe e sincronismo prévia/MP4 após cortes.
- Projeto e arquivos originais são mantidos; histórico de desfazer fica somente na sessão de edição.

## Resultados locais de 05/11/2026
- `tools/check_version_sync.py`: aprovado.
- `tools/validate_source.py`: aprovado.
- 67 scripts Python `tools/test_*.py`: 67/67 aprovados.
- Parser PSI Kotlin: 6 arquivos, 0 erros sintáticos.
- `TrimTimelineEdit.kt` compilado isoladamente em Kotlin com stubs; 7 asserções de execução aprovadas.
- `tools/package_source.py`: validação CRC e reextração do ZIP aprovadas.
