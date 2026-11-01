# Validação v0.1.96 / versionCode 97 — 01/11/2026

## Escopo
- Base: código completo da v0.1.95, sem migração de projeto (`schemaVersion=4`).
- 16 estilos, incluindo cinco originais, por `speech_metadata.style`; os três controles de cinco níveis são instruções textuais, não parâmetros físicos/numéricos da API.
- `GeminiNarrationStyling` compõe as instruções antes da amostra e da geração definitiva; cache SHA-256 recebe a instrução completa. Ajustes naturais e presets antigos preservam instruções originais e, portanto, identidade da amostra.
- O texto narrado permanece intacto, sem inserir comandos ou tags; voz, serviço, permissões, confirmação de consumo e arquivos WAV seguem como antes.

## Verificações locais
- `python3 tools/validate_source.py`, scripts de regressão e testes locais Kotlin puro (consultar resultado executado).
- Sem execução comprovada do Gradle/Android completo e sem chamada à API com chave real; não confundir verificação estrutural com APK compilado.

## Testes Android/serviço pendentes
1. Conferir a disposição dos 16 presets e o painel recolhível em tela pequena, modo claro/escuro e acessibilidade.
2. Comparar amostras e narração final para os extremos de ritmo, emoção e altura; nem sempre a IA segue a direção perfeitamente.
3. Mudar apenas um controle e confirmar que a amostra antiga não é reutilizada; restaurar ao valor natural e validar cache anterior.
4. Confirmar que o texto falado é exatamente o digitado, especialmente com pontuação e acentos em português brasileiro.
5. Rodar `:app:testDebugUnitTest :app:lintDebug :app:assembleRelease` no CI e verificar cinco APKs assinados da mesma versão.
