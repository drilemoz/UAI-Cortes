# Uai Cortes v0.1.99 — verificação Home e versão GitHub Manager

## Mudanças
- `HomePrimaryActions`: componente Compose da Home reutilizado como prévia somente leitura em Sobre. Botão de criação e cartão Animar Imagens estão desativados na prévia.
- `AppVersionLabel`: compartilhado por Home e Sobre, baseado em `AppReleaseNotes`.
- `github-manager.json`, `app_identity.json`, `VERSION` na raiz, formatos aceitos pelo GitHub Manager v2.0.137. `app_identity.json` é metadado de distribuição, não muda o `applicationId` ou arquivos de projeto do usuário.
- `tools/check_version_sync.py` falha no CI quando versão, código, identidade ou metadados não coincidem; `tools/test_version_sync.py` testa casos negativos.
- `HomeAboutPreviewAndroidTest` verifica as duas telas e confirma que a prévia não executa comandos.

## Limites
- O teste estático valida contrato e versão, não o comportamento da UI no Android.
- `androidTest` exige dispositivo/emulador para ser executado. No CI existente são executados testes unitários, Lint e assembleRelease; os testes instrumentados ficam pendentes até uso de emulador.
- A prévia mostra os cartões principais da Home, não renderiza a lista de projetos do usuário nem executa navegação.
- No GitHub Manager, o ZIP local deve ser reimportado ou o repositório atualizado na branch consultada para a versão atual aparecer. O app pode manter informação da branch antiga até nova consulta.
