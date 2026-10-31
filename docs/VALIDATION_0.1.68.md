# Validação 0.1.68 / versionCode 69

- Base: ZIP oficial 0.1.67, sem mudanças em `schemaVersion=4`.
- `AppReleaseNotes`: dados compartilhados para Sobre e Novidades, com assinaturas SHA-256 calculadas a partir do conteúdo de novidades atuais.
- `WhatsNewSeenStore`: armazenamento local do conjunto de assinaturas já vistas; atualização não abre novamente se o conteúdo for idêntico.
- `WhatsNewScreen`: abertura automática no início, rolagem e botão Continuar; também pode ser aberta por Configurações/Sobre.
- Testes novos: `WhatsNewReleaseNotesTest.kt` (paridade, disparo único, mudança de conteúdo, navegação).
- Pendências: build Android Gradle, JUnit completo, testes em aparelho, rotação e acessibilidade.
