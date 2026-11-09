# Validação v0.1.253 (2026-11-09)

## Falha real no workflow 210

`tools/validate_source.py` parou em `workflow.VERSION`: Gradle esperava 0.1.252, mas o workflow no GitHub ainda tinha 0.1.251. O ZIP distribuído anteriormente não continha a pasta oculta `.github`, apesar de o empacotador oficial já a suportar. O Gradle/Android não foi executado nesse workflow.

## Correção

- Nova versão 0.1.253, versionCode 254, com workflow `.github/workflows/android-apk.yml` incluído no ZIP distribuído.
- Empacotamento exclusivamente por `tools/package_source.py`, que agora verifica a sincronização antes de gerar o arquivo.
- Novo contrato `tools/test_ci210_workflow_zip_contract.py`: gera ZIP real, valida a presença do workflow, versão, extração, CRC e proíbe arquivos de segredo.
- Nenhuma alteração funcional em edição, legendas, exportação, Google Vozes, YouTube ou Facebook.

## Testes

- `tools/check_version_sync.py`: **aprovado**, v0.1.253/versionCode 254 sincronizados entre Gradle, workflow, dados do GitHub Manager, Novidades e empacotador de APK.
- `tools/validate_source.py`: **aprovado**.
- `tools/test_ci210_workflow_zip_contract.py`: **aprovado**. Teste criou, extraiu e validou ZIP completo com `.github/workflows/android-apk.yml` e mesma versão do Gradle.
- `tools/test_ci117_source_bundle_contract.py`: **aprovado**.
- **144/144** testes `tools/test_*.py`: **aprovados** (execução local em processos separados, paralelos, cada teste com timeout de 18 s, sem timeouts).
- `tools/ci70_kotlin_visibility_test.py` e `tools/ci72_media3_optin_test.py`: **aprovados**.
- `AppReleaseNotes.kt`: compilado isoladamente com `kotlinc`, **aprovado**.
- XML do aplicativo: 6 arquivos com parsing válido.
- `tools/release_notes_for_github.py`: notas da v0.1.253 geradas em UTF-8, **aprovado**.
- ZIP final: será gerado pelo empacotador oficial com checagem CRC, arquivos obrigatórios e validação estrutural da cópia extraída.

**Limitações:** compilação Android Gradle/Media3, lint Android, geração dos cinco APKs e testes no aparelho **não executados** localmente. O workflow 210 falhou antes do Gradle; executar novamente o GitHub Actions após importar a pasta oculta `.github` do novo ZIP.
