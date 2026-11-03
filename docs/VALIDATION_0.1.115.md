# Validação — Uai Cortes v0.1.115 / versionCode 116

- Checkpoint por projeto em `filesDir/gemini_narration_sessions` com gravação temporária, `fsync` e rename atômico. Não há credenciais no JSON.
- WAVs em `filesDir/gemini_narrations`: verificar tamanho + SHA-256 na recuperação; rejeitar paths externos e seleção obsoleta após alterações de legenda.
- Estado restaurado apenas offline; gerar/retomar passa pela confirmação manual das chamadas pendentes.
- Até 12 legendas por lote com atalho para selecionar as 12 seguintes.
- Análise PSI Kotlin, testes de contratos/versionamento e ZIP extraído validados localmente.

**Pendências:** build Android/Gradle real, testes instrumentados de persistência após morte do processo, retomada de cota em aparelho, exclusão de dados e exportação real.
