# Validação Uai Cortes v0.1.152 — CI128

## Evidências do workflow 128
- :app:testDebugUnitTest, :app:compileDebugAndroidTestKotlin, :app:lintDebug e :app:assembleRelease: BUILD SUCCESSFUL.
- Cinco APKs aprovados, assinados e conferidos pelo empacotador (arm32, arm64, x86, x86_64, universal).
- `gh release create` retornou HTTP 422: `body is too long (maximum is 125000 characters)`.

## Correção
- Script gera corpo de publicação apenas da versão atual, com tamanho máximo conservador de 80000 bytes; notas completas continuam no repositório.
- Workflow executa o script após validar os cinco APKs e antes de publicar; `gh release edit` mantém notas consistentes em reexecuções.
- Novo teste cobre UTF-8, versão errada, corpo grande e uso efetivo do script no workflow.
- Compilação Android completa e publicação da 0.1.152 ainda precisam ser verificadas por CI.
