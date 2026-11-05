# Uai Cortes 0.1.168 — relatório de implementação e validação

Data: 05/11/2026 · versionName **0.1.168** · versionCode **169**.

## Escopo implementado

1. Login oficial de **Facebook Página** por Facebook Android SDK 19.0.0, condicionado a App ID, Client Token e assinatura do aplicativo cadastrada na Meta. A UI solicita separadamente o acesso às Páginas e a permissão de publicação. O botão fica desativado sem a configuração de build.
2. Consulta de `/me/accounts` com autorização do usuário. O usuário escolhe qual Página vincular à ficha local, e apenas o ID público é persistido; credenciais de Página são descartadas ao encerrar o processo/sessão e não entram no JSON do SocialStore ou no backup público.
3. Quando a sessão Meta permite `pages_manage_posts` para a Página vinculada, o diálogo de publicação pode aproveitar o token da sessão sem solicitar colagem manual. Exige confirmação individual, marca `UPLOAD_REVIEW` antes do primeiro POST e não faz retry após resposta incerta. A sessão é conferida novamente antes de iniciar envio. Modo avançado por token temporário da v0.1.167 foi preservado.
4. Central de APIs exibe estado de configuração da Meta, opção de desconectar e os pré-requisitos de TikTok. `TIKTOK_CLIENT_KEY` é opcional, apenas chave pública de desenvolvimento. **Login OAuth TikTok não está implementado:** falta domínio HTTPS verificado, SDK Login Kit configurado e backend seguro para troca de código e refresh de token.
5. Manifesto bloqueia permissões de publicidade do SDK e desliga registro automático de eventos/identificador do anunciante. Versão, notas, documentação, workflow e metadados sincronizados.

## Arquivos novos/modificados principais

- `app/src/main/java/br/com/uaicortes/SocialMetaAuth.kt` (novo)
- `app/src/main/java/br/com/uaicortes/SocialExternalApis.kt`
- `app/src/main/java/br/com/uaicortes/SocialScreens.kt`
- `app/src/main/java/br/com/uaicortes/SocialExternalDialogs.kt`
- `app/src/main/java/br/com/uaicortes/MainActivity.kt`
- `app/src/main/AndroidManifest.xml`
- `app/build.gradle.kts`
- `.github/workflows/android-apk.yml`
- `app/src/main/java/br/com/uaicortes/AppReleaseNotes.kt`
- `tools/test_social_meta_oauth_168.py` (novo)
- `VERSION`, `README.md`, `CHANGELOG.md`, `RELEASE_NOTES.md`, `AGENTS.md`, `app_identity.json`, `github-manager.json`, `tools/package_apks.py`

## Testes executados

- 64/64 scripts `tools/test_*.py`: **aprovados**, incluindo o novo contrato estático de configuração e segurança da Meta.
- `tools/validate_source.py`: **aprovado** (contratos estruturais existentes).
- `tools/check_version_sync.py`: **aprovado** (0.1.168 / versionCode 169).
- Parser Kotlin PSI: **6 arquivos, zero erros sintáticos**. Isso não equivale a compilação nem validação de tipos Android.
- XML do manifesto: **bem formado**.
- Fonte empacotada pelo `tools/package_source.py`, que reextrai e valida o conteúdo; integridade ZIP verificada.

## Não executado / limitações

- `bash ./gradlew --version` falhou: **Gradle 8.11.1 ausente no ambiente**. Não houve build APK, lint Gradle, unit tests JUnit, testes instrumentados, login físico ou upload real. O CI GitHub Actions precisa validar resolução do novo SDK e compatibilidade de tipos/APIs.
- Não existe App ID/Client Token, conta autorizada, aprovações da Meta ou hash de assinatura fornecidos nesta sessão. Não há demonstração de login ou publicação funcional em produção.
- Login Meta é para **Página**, não publica em perfil pessoal. Perfil pessoal e Kwai continuam por compartilhamento Android.
- Sem backend próprio não há troca de OAuth TikTok; o modo de token temporário segue disponível e é separado de uma conexão permanente.
- Nenhuma métrica automática Meta/TikTok foi adicionada nesta versão; YouTube conserva seus caminhos existentes.
- O SDK Meta gerencia internamente o token de usuário; o Uai Cortes não adiciona esse token à persistência da aplicação nem ao ZIP de exportação. Os tokens de Página ficam apenas na memória e são perdidos ao encerrar o processo ou desconectar.

## Configuração no GitHub Actions

Opcionalmente cadastrar `META_APP_ID` e `META_CLIENT_TOKEN` (identificadores de cliente expostos ao APK, **não** App Secret) nos Secrets do projeto, além da assinatura Android existente. Cadastrar o pacote `br.com.uaicortes`, o hash da assinatura real e os escopos solicitados no portal Meta. Verificar aprovação de `pages_show_list`, `pages_read_engagement`, `pages_manage_posts` e permissões reais de Página. `TIKTOK_CLIENT_KEY` é apenas preparo; não habilita OAuth sem backend. **Nunca** enviar `client_secret`/App Secret para o build Android.

## Preservação

`VideoProject` schemaVersion 4, `SocialStore` schema social 1, Editor, Gemini, Wizard, YouTube e cinco APKs Release preservados no código-fonte. Recuperação em aparelhos com dados antigos ainda depende de testes instrumentados.
