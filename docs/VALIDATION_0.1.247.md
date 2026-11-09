# Validação da versão 0.1.247 / versionCode 248

## Escopo

- Histórico de amostras Gemini indexadas por voz, independente de texto, modelo e estilo atuais.
- Reprodução sem rede, parada, exportação com Storage Access Framework (WAV) e exclusão individual com confirmação.
- Filtro de qualquer histórico separado do filtro da amostra atual.
- Modelo, frase e estilo capturados antes de solicitar síntese, mantendo hash/identificação consistente.
- Preservação das demais áreas, credenciais, narrativas de projetos, cinco APKs de release e schema do projeto.

## Validações executadas localmente

- `python tools/check_version_sync.py`: **aprovado**, 0.1.247/versionCode 248 coerentes com Gradle, CI, app_identity, github-manager, RELEASE NOTES.
- `python tools/validate_source.py`: **aprovado** após mudanças e metadados.
- `tools/StudioKotlinSyntaxCheck.kt` pelo parser PSI: **447 fontes Kotlin, zero erros sintáticos**. Não representa compilação Android ou verificação de símbolos do Compose.
- 21 testes Python direcionados a Vozes Google, catálogo, filtros, comparação, favoritos, clonagem Coqui/Chatterbox e compatibilidade de fluxos: **21 aprovados, 0 falhas**.
- `tools/test_ci247_google_voice_preview_history_contract.py`: **aprovado** (incluído no grupo de 21).
- AndroidTest `GeminiVoicePreviewHistory247AndroidTest.kt` adicionado, **não executado** neste ambiente (precisa de aparelho/emulador).
- Tentativa da bateria ampla de 139 testes com controle de timeout: **não conclusiva**, limite da execução local atingido; não declarar aprovada.

## Pendências e riscos

- Gradle Android `:app:testDebugUnitTest`, `:app:compileDebugAndroidTestKotlin`, lint, `:app:assembleRelease` e cinco APKs: **não executados aqui**. Necessário GitHub Actions.
- Testar SAF em provedores reais de arquivos, salvamento WAV e retorno do seletor após rotação, áudio sem rede, exclusão seletiva e privacidade.
- Não é possível recuperar automaticamente o texto, modelo e estilo de amostras antigas só pelo hash do cache; nenhuma classificação foi inventada.
- Prévias que não constam do índice legado (sem ID de voz associado) não são atribuídas automaticamente.
- Nenhum modelo de voz foi instalado no aparelho; a síntese Google continua dependente de API e de solicitação confirmada pelo usuário.
