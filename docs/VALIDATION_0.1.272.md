# Validação v0.1.272 — ElevenLabs

## Escopo e integrações
- Central Integrações mantém Gemini, Amazon Polly e redes sociais; ElevenLabs tem rota e tela próprias.
- Chave de usuário criptografada via Android Keystore (AES-GCM) com exclusões nas regras de backup; nunca incluída no APK ou projeto.
- GET /v1/user para teste de autorização (requer permissão compatível); GET /v2/voices com paginação, sem síntese.
- POST /v1/text-to-speech/{voice_id} usa modelo eleven_multilingual_v2 e formato mp3_44100_128, apenas após confirmação de possíveis créditos. Sem retry de POST; reprodução local e salvar MP3 via SAF.
- Texto de teste limitado a 500 caracteres, sem armazenamento de histórico de prompts ou informações de conta em diagnósticos.

## Testes locais realizados
- `tools/check_version_sync.py`: versão/CI/docs sincronizados.
- `tools/validate_source.py`: contratos estruturais existentes aprovados.
- `tools/test_elevenlabs_272_contract.py`: contrato da integração aprovado.
- `tools/test_ci128_release_notes_contract.py`: formato da publicação GitHub aprovado.
- Parser PSI Kotlin: 0 erros sintáticos nas fontes novas, nos pontos de integração e no teste unitário.
- Compilação isolada de `ElevenLabsApi.kt` e `ElevenLabsCredentials.kt` com stubs apenas para verificar tipos Kotlin básicos. Não equivale à compilação Android.

## Pendências obrigatórias
- Compilação Gradle Android em CI e testes instrumentados da tela.
- Testes unitários JUnit do cliente contra `org.json` real no ambiente Android/Gradle.
- Teste de rede com API Key pessoal, inclusive permissões restritas, falhas HTTP, paginação e consumo real na ElevenLabs.
- Reprodução e salvamento MP3 no aparelho; consumo e faturamento devem ser confirmados pelo painel da ElevenLabs.
- A integração **não** altera automaticamente o Estúdio de Voz ou narrações de projetos existentes.
