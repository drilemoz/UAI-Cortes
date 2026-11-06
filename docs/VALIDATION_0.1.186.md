# Validação — Uai Cortes 0.1.186 / versionCode 187

## Objetivos
- Dialogo de saída mais curto, sem persistir ao descartar.
- Módulo Atualizações acessível via Sobre; remover repo manual e atalhos redundantes.
- Busca pública de Releases robusta, baixar APK dentro do app só com SHA-256, oferecer instalação Android confirmada.

## Verificações locais
- `python tools/validate_source.py`
- `python tools/check_version_sync.py`
- `python tools/test_github_about_186_contract.py`
- Testes `tools/test_*.py` e Kotlin PSI, registrar resultados reais.
- Testes unitários `GitHubUpdateCoreTest`, `GitHubUpdateDiagnosticsTest` e UI instrumental `HomeAboutPreviewAndroidTest` exigem Gradle.

## Pendências obrigatórias no aparelho
- Abrir Sobre, confirmar que só existe Verificar atualizações, retornar para Configurações e confirmar que o item separado desapareceu.
- Verificar Repositório público configurado automaticamente no build do GitHub Actions e atualização v0.1.187+ com APK universal e SHA-256.
- Autorizar download, testar conexão lenta/interrompida, tamanho/CRC/SHA, certificado e instalação; jamais desinstalar para atualizar.
- Validar projetos novos e salvos: Salvar, Salvar cópia, Descartar, Continuar.
- Validar o comportamento visual em aparelhos pequenos com fontes ampliadas.
- Nenhuma compilação ou instalação física pode ser inferida dos testes locais.
