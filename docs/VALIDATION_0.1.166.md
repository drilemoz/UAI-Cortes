# Uai Cortes 0.1.166 — YouTube OAuth e publicação direta (fase 2)

**Base:** código-fonte 0.1.165. **Versão:** 0.1.166 / versionCode 167. **Data:** 05/11/2026.

## Implementado no código

1. Adicionada biblioteca Google Play Services AuthorizationClient (22.0.0), autorizando YouTube `youtube.upload` e `youtube.readonly` pelo Android. Tokens são usados somente em memória durante a operação; não há client secret, chave API ou token embutido no projeto.
2. Cadastro YouTube pode vincular um canal após consulta `channels.list(mine=true)` e confirmação explícita do nome/ID. É armazenado apenas o identificador público `UC...`. Antes de cada upload ou sincronização, o cliente confirma que a conta Google autorizada pertence a esse canal.
3. Nova ação de upload direto após preparar a publicação: seleção de privacidade (privado padrão), confirmação específica e protocolo resumable upload por POST + PUT em blocos de 1 MiB, com progresso baseado nos bytes aceitos pelo servidor. **Sem envio em segundo plano.**
4. Antes do POST, tarefa persistida como `UPLOAD_REVIEW`. Somente uma resposta HTTP de sucesso contendo ID válido da API altera para `CONFIRMED_BY_API`. Em falha, morte do processo ou perda de resposta, não há nova tentativa automática; o usuário deve conferir no YouTube Studio.
5. Consulta manual `videos.list(part=statistics)` para vídeos com IDs confirmados, salvando métricas marcadas com origem `youtube_api`. Valores anteriores permanecem identificados como manuais, e dados ausentes não são fabricados.
6. Central de APIs permite testar autorização YouTube consultando nome e ID reais do canal. As demais plataformas continuam com links oficiais e compartilhamento assistido; não há OAuth Meta/TikTok/Kwai.
7. Schema local social permanece 1; `remoteChannelId`, `remoteVideoId`, `metrics.source` são campos opcionais, com defaults compatíveis com dados da 0.1.165. Catálogo salvo via AtomicFile. Projeto de vídeo continua schemaVersion 4; nenhuma alteração no editor ou no Wizard.

## Segurança e limites

- **Antes de testar:** configure um cliente OAuth do tipo Android no Google Cloud com package name `br.com.uaicortes` e SHA-1 do certificado REAL que assina o APK instalado. Ative a YouTube Data API v3, configure a tela de consentimento e eventuais usuários de teste; o aparelho necessita Google Play services compatível.
- O app não cria, testa ou publica na sua conta sem consentimento. A autorização depende da configuração do projeto Google Cloud. Não foi realizada chamada autorizada à API nesta entrega.
- A sessão retomável de upload existe somente durante a operação; **não é persistida criptografada**. Após interrupção, esta versão não oferece retomar a mesma sessão entre reinícios. Isso evita guardar URL sensível, mas exige conferir YouTube Studio antes de qualquer novo envio, para não duplicar publicações.
- Upload restrito a `content://` com tamanho conhecido até 2 GiB. Permissões SAF precisam continuar válidas. O cliente usa HTTPS fixo para a API e valida o host da sessão de upload, mas não testa redes reais neste ambiente.
- Requisições OAuth e APIs podem falhar por escopos, configuração, cotas, ausência de Play Services, privacidade do canal ou restrições de conta. Não presume contas conectadas ou sucesso real.
- Meta Facebook Páginas, TikTok e Kwai **não receberam upload direto** nesta etapa; continuam com compartilhamento do Android. Facebook Perfil permanece manual.

## Arquivos alterados

- **Novos:** `YouTubeSocialApi.kt`, `YouTubeAuthorizationUi.kt`, `YouTubeSocialAndroidTest.kt`, `tools/test_social_youtube_166_contract.py`, `docs/VALIDATION_0.1.166.md`.
- **Modificados:** `SocialStore.kt`, `SocialScreens.kt`, `app/build.gradle.kts`, três testes estáticos anteriores, `AGENTS.md`, `VERSION`, `AppReleaseNotes.kt`, `README.md`, `CHANGELOG.md`, `RELEASE_NOTES.md`, `.github/workflows/android-apk.yml`, `tools/package_apks.py`, `app_identity.json`, `github-manager.json`.

## Testes executados

| Teste | Resultado |
| --- | --- |
| `python3 tools/check_version_sync.py` | APROVADO — 0.1.166 / code 167 |
| `python3 tools/validate_source.py` | APROVADO |
| 62 scripts `tools/test_*.py` em execução paralela | APROVADOS (62/62, exit 0) |
| `tools/test_social_youtube_166_contract.py` | APROVADO |
| PSI Kotlin — 6 arquivos de produção e de teste | APROVADO, 0 erros de sintaxe |
| `tools/release_notes_for_github.py` | APROVADO |
| ZIP fonte / reextração / CRC | Ver resultado gerador de ZIP |

**Não executados:** Gradle/AGP, compilação Kotlin com resolução de símbolos (a análise PSI não cobre isso), Android Lint real, testes Android instrumentados, Google OAuth real, consulta YouTube com conta autorizada, upload de MP4, medição de banda/consumo, ensaio de interrupção/retomada no aparelho. Ainda não há APK compilado nesta entrega. O workflow GitHub Actions mantém as tarefas `:app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:lintDebug :app:assembleRelease` e os cinco APKs exigidos pelo `AGENTS.md`.
