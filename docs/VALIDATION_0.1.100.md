# Validação v0.1.100 — atualizações GitHub Releases

- Criados `GitHubUpdateCore`, `GitHubUpdateManager` e `GitHubUpdateUi` com consulta a Releases (inclusive pré-releases), versão semântica, segurança do download e instalação por confirmação.
- O APK compilado no GitHub Actions recebe `GITHUB_REPOSITORY`; compilação local pode ficar sem configuração.
- Validação estática e testes de políticas em Kotlin devem passar antes do empacotamento.
- Pendências: Gradle Android e Lint completos; teste de rede real em repositório configurado; verificação de certificado em aparelho; instalar APK assinado com a MESMA chave da versão anterior.
- O workflow falha antes de publicar sem os quatro Secrets de assinatura estável. Nunca prometer atualização in-place quando versões anteriores usaram assinatura debug variável.

- Tela Sobre inclui atalho explícito para conferir atualização, sem alterar a prévia de validação da Home.
- Verificação em ON_START com intervalo mínimo de 60 segundos e sem interromper download/instalação; dispensa repetição do mesmo aviso por versão na sessão.
