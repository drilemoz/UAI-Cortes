# Uai Cortes v0.1.106 — Seletor compacto de vozes

## Escopo
- A janela de seleção substitui temporariamente a janela de narração, sem sobreposição dupla de diálogos.
- Busca mantém nome traduzido, original, descrição, ID e metadados como critérios; os dados longos só aparecem em **Detalhes**.
- Filtros Todas/Femininas/Masculinas usam apenas `gender` informado pela API, sem inferir gênero para nomes.
- Seleção não chama API; botão Confirmar voz retorna ao editor, preservando rascunho, e não gera áudio ou altera faixas.
- Cancelar e Voltar descartam apenas seleção temporária; nenhuma alteração de schemaVersion.

## Verificações
- `python3 tools/validate_source.py`
- `python3 tools/test_voice_picker_contract.py`
- `python3 tools/test_voice_return_contract.py`
- `python3 tools/test_version_sync.py`
- Testes unitários `GeminiVoiceCompactPickerTest.kt` via Gradle no CI.
- Revisão sintática dos arquivos Kotlin modificados; teste isolado do núcleo de busca e metadados.
- Empacotador com validação do ZIP extraído: `python3 tools/package_source.py --output /.../UaiCortes-v0.1.106.zip`.

## Pendências
- Executar build, lint e testes do Android no GitHub Actions; testar visual e teclado no aparelho.
- Conferir nomes longos, lista de 107 vozes, seleção, filtro, voltar, confirmações e reprodução do editor com perfil de RAM reduzido.
