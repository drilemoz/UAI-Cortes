# Uai Cortes 0.1.231 — Validação da Central de Vozes

Versão anterior 0.1.230; versionCode 232. Módulo independente e seletores do editor reutilizam a Central sem gerar áudio automaticamente. Novos recursos: UI segura para status bar, filtros dinâmicos por metadados, apelidos locais, prévias textuais e comparação A/B.

## Riscos e verificações obrigatórias

- Testar em Android real, tema claro/escuro, dispositivo estreito e fonte ampliada; conferir abertura em tela cheia e em pop-up.
- Verificar se vozes têm timbres percebidos realmente diferentes; não interpretar 107 identificadores como 107 identidades acústicas únicas.
- Confirmar que os filtros refletem só os metadados da API, sem inventar sotaque ou identidade; verificar catálogo geral sem campos extras.
- Conferir preexistentes WAV do texto padrão, novo hash para frase curta/personalizada, persistência de apelidos e favoritos ao atualizar catálogo.
- Medir consumo de tokens e quota real no Google, sem inferir preço a partir de uma amostra.
- CI: executar validação estrutural, testes Python, sintaxe Kotlin, `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:assembleRelease`, cinco APKs e teste físico; não considerar testes estáticos um build completo.

## Escopo mantido

VideoProject schemaVersion 4, assinatura Android, integração YouTube/Meta, GeminiCredentialStore, cache e modelos de voz permanecem preservados. Paginação do catálogo remoto ainda tem limite de 500 itens; o incremento visual de 30 em 30 é local.
