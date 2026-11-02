# Validação v0.1.101 — Correção CI85

## Falha confirmada

Workflow 85 executou 343 testes de unidade, falhou em 2 métodos de `HomeAboutVersionContractTest` (linhas 8 e 14 da v0.1.100); Gradle parou em `:app:testDebugUnitTest` e não publicou APK.

## Correções

- A versão de app usada pelo teste vem do `BuildConfig` gerado pelo Android Gradle Plugin.
- O teste da Home pesquisa o histórico de novidades retidas, evitando falha ao publicar uma nova versão com outras mudanças.
- `tools/test_version_sync.py` não exige versão codificada manualmente para validar um release; `tools/validate_source.py` consulta a versão no Gradle, inclusive para nomes de APKs e workflow.
- Arquivos de identificação do GitHub Manager sincronizados com 0.1.101, versionCode 102.

## Evidência e pendências

- Os testes executados na estação local são isolados/estruturais e não substituem o Gradle completo. Confirmar no GitHub Actions: `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:assembleRelease`, `tools/package_apks.py` e publicação dos cinco APKs.
- Verificar manutenção da chave de assinatura e instalar o novo APK sobre o existente, sem desinstalar nem apagar projetos.
