# Validação — Uai Cortes v0.1.116 / versionCode 117

- Gerador: um campo por fala, controles Anterior/Próxima, ação de geração fixa após área rolável e motivo do botão desabilitado.
- Importador: páginas limitadas a 12 itens, seleção explícita; o limite de 12 segue validado em `SubtitleNarrationTimeline` tanto na interface quanto antes da API.
- Referência sonora: visualiza apenas a fala selecionada e permite análise sob demanda; sem alterar tempos originais ou fazer requisição automaticamente.
- Checkpoints `GeminiNarrationCheckpoint`/SHA-256, validação de duração e confirmação de gasto do Gemini preservados.
- Verificações: contratos locais, sintaxe PSI Kotlin, versão e ZIP extraído. **Ainda não é prova de build Android real**.

Pendências: Gradle/Android, interação com teclado e rolagem em aparelho, resposta Gemini real e vídeo exportado.
