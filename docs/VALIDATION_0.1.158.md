# Uai Cortes — validação v0.1.158 (05/11/2026)

## Escopo
- `GeminiBatchPolicy`: valida 2–8 prompts de TEXTO, modelo `gemini-*` compatível com gerador textual, tamanho limitado, nome remoto `batches/...` sanitizado e estados sem inferência de sucesso.
- `GeminiBatchClient`: cria jobs inline via `models/{model}:batchGenerateContent`; busca jobs via `/v1beta/batches`, consulta manualmente pelo ID e apresenta até 8 respostas de texto inline. Não usa failover, reenvio automático, chaves por URL, áudio, imagem ou upload de vídeos.
- `GeminiBatchStore`: guarda privadamente somente perfil/projeto/modelo/contagem/nome/status/tempo. Um registro REVIEW é persistido antes do POST, para proteger contra repetição após falha de transporte. Não salva prompt, resposta nem credencial.
- `GeminiBatchUi`: novo módulo expansível em Configurações > Google Gemini, com aceite de possível cobrança, confirmação pré-envio, consulta de jobs locais/remotos e saída apenas em memória da sessão.
- Dados de jobs excluídos de `full-backup-content`, `cloud-backup` e `device-transfer`; schemaVersion 4 e ferramentas existentes preservados.

## Verificações locais
- `python3 tools/check_version_sync.py`: aprovado (versão 0.1.158, versionCode 159).
- `python3 tools/validate_source.py`: aprovado.
- 54/54 scripts `tools/test_*.py`: aprovados, incluindo contrato 158.
- `python3 tools/ci70_kotlin_visibility_test.py` e `tools/ci72_media3_optin_test.py`: aprovados.
- PSI Kotlin dos 7 arquivos Kotlin alterados ou novos: 0 erros sintáticos.
- Compilação JVM isolada com stubs para `GeminiBatchPolicy`, `GeminiBatchStore` e `GeminiBatchClient`: aprovada; testes smoke das regras de IDs/modelos/quantidades: aprovados.

## Limitações e riscos
- Gradle/Android completo, JUnit Android, UI Compose e chamadas reais à API Batch NÃO foram executados neste ambiente: validar GitHub Actions e aparelho físico antes de afirmar funcionamento em produção.
- API Batch suporta modalidades/modelos com restrições próprias. Esta UI implementa **somente TEXTO** em jobs inline pequenos e exige consentimento de custo, sem síntese TTS. Pode não estar disponível em todos os níveis de acesso; cobrança e tempos dependem do Google.
- Falha de conexão durante o POST pode ter gerado um job remoto: registros REVIEW não se repetem automaticamente. Usuário pode consultar jobs via API e vinculá-los manualmente. A aplicação não monitora jobs em segundo plano.
- Resultados inline são exibidos temporariamente; não substituem diretamente legendas, prompts Flow, descrições ou mídia do projeto. Jobs maiores e saída por arquivo JSONL ainda não são suportados.
- Limite de 30 jobs locais (últimos); ocultar remove somente o registro deste aparelho, **não** cancela o job remoto.

## Documentação consultada
- https://ai.google.dev/gemini-api/docs/batch-api
- https://ai.google.dev/gemini-api/docs/rate-limits
- https://ai.google.dev/gemini-api/docs/pricing
