# Uai Cortes 0.1.89 — Plano de validação

## Mudanças
- Preservação de tempos por palavra nas novas transcrições Vosk; texto corrigido na revisão automática ou no editor limpa os horários antigos. Se horários forem inválidos, fallback sem inventar medição.
- Estilos locais e cores individuais independentes para palavras repetidas; serialização opcional mantendo esquema 4.
- Navegação Voltar de Animar Imagens para a tela inicial, com confirmação de saída e prevenção durante exportação.

## Verificações possíveis sem Android
- `python3 tools/validate_source.py`
- Testes JVM quando houver Gradle e dependências: `:app:testDebugUnitTest`
- Análise sintática dos Kotlin e integridade do ZIP.

## Pendências em dispositivo
- Montar legenda com erros de Vosk e conferir destaques em fala, silêncios e cortes.
- Conferir posição, cores, contorno, sombra, itálico e sincronização na prévia e no MP4 exportado (720p/1080p, 9:16/16:9).
- Verificar Voltar com rascunho limpo, modificado e exportação em andamento, tanto partindo da Home quanto do editor.
- Checar performance, aquecimento, orientação e restauração de projetos legados.
