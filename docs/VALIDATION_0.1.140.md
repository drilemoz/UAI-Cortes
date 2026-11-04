# Validação v0.1.140 — CI117, ZIP com workflow

## Diagnóstico comprovado pelos logs
- Em 04/11/2026, workflow 117 falhou em `tools/validate_source.py`: `workflow.VERSION` esperado `0.1.139` e encontrado `0.1.138`.
- A versão v0.1.139 foi empacotada SEM `.github/workflows/android-apk.yml`; o workflow antigo do repositório continuou inalterado. Não há indícios nestes logs de erro de compilação em Vozes IA, porque `assembleRelease` não foi executado.

## Solução
- Nova versão 0.1.140/versionCode 141 com workflow sincronizado e incluído no ZIP.
- `tools/package_source.py` usado para produzir o ZIP; valida arquivos obrigatórios e executa validador no ZIP extraído.
- Regressão `tools/test_ci117_source_bundle_contract.py` confere `.github` na seleção para o ZIP e testa o contrato de versionamento.
- Histórico recente da tela Novidades corrigido (v0.1.138 não é v0.1.139).

## Testes esperados
1. `python3 tools/validate_source.py`
2. `for f in tools/test_*.py; do python3 "$f"; done`
3. `python3 tools/ci70_kotlin_visibility_test.py` e `python3 tools/ci72_media3_optin_test.py`
4. `python3 tools/package_source.py --output /mnt/data/UaiCortes-v0.1.140-CI117-Workflow.zip`
5. Compilar, executar testes JUnit/Lint e gerar cinco APKs no GitHub Actions; validar interface e exportação em aparelho real.

## Atenção ao envio
Importar todo o conteúdo do ZIP no repositório, incluindo a pasta oculta `.github`, para substituir o workflow antigo. Enviar só os arquivos visíveis reproduz o erro.
