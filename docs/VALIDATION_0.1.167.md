# Uai Cortes 0.1.167 — validação da fase TikTok / Facebook Página

Data: 05/11/2026. Base: fonte 0.1.166, código 167. Nova versão: `0.1.167`, `versionCode=168`.

## Implementação

- Adicionado `SocialExternalApis.kt`: consultas por token OAuth temporário na Meta Graph API (`/me`), TikTok `creator_info/query`, upload Facebook Reels START/binário/FINISH, upload TikTok Direct Post FILE_UPLOAD e consulta de status por `publish_id`.
- Adicionado `SocialExternalDialogs.kt`: vinculação da identidade de Página verificada, testes de token sem publicar, resumo da conta, escolha das opções de privacidade fornecidas pelo TikTok, ajustes de interações e marcação de conteúdo gerado com IA, consentimento específico e progresso por rede.
- `SocialScreens.kt`: oferece ações avançadas apenas para Página e TikTok, sem substituir a alternativa de compartilhamento manual. A Central de APIs tem testes reais sob demanda para tokens fornecidos pelo usuário. Consultar o status da operação TikTok é opcional.
- `SocialStore.kt`: `remotePageId` opcional (mantém esquema 1), estado `REMOTE_SUBMITTED` sem pretensão de publicação concluída, verificação de IDs e gravação `UPLOAD_REVIEW` atômica antes de qualquer POST de publicação. Não reenvia depois de resultado incerto.
- `SocialExternalAndroidTest.kt`: testes instrumentados de persistência, vínculos, migração e proteção contra duplicação. Adicionados contratos `tools/test_social_external_167_contract.py`.
- Novidades, versões, metadados de CI e documentação sincronizados para a 0.1.167.

## Segurança e limitações

- NÃO existe login OAuth automático integrado para Meta/TikTok. O modo avançado aceita um token OAuth com permissões já concedidas, informado pelo usuário APENAS naquela sessão. Não recebe nem grava App Secret; token não é persistido, nem exibido em logs, nem exportado.
- Requer aplicativo autorizado/aprovado nas plataformas e permissão da conta. TikTok: `video.publish`, visibilidade pública condicionada a auditoria do cliente. Meta: token de Página, permissões de publicação e revisão do aplicativo.
- `REMOTE_SUBMITTED` significa apenas que a API declarou recebimento de uma operação. Meta FINISH não garante que o vídeo esteja público; TikTok `publish_id` é identificador da operação, não URL de vídeo.
- Falha após início é incerta: o usuário deve conferir a plataforma antes de iniciar qualquer outra postagem. Não há retomada persistente da sessão de upload Meta/TikTok nem reenvio automático. Limite conservador de 512 MiB para o fluxo avançado.
- Facebook Perfil e Kwai permanecem via compartilhamento do Android. Métricas automáticas do TikTok e da Meta ainda não foram integradas; YouTube mantém seu fluxo anterior.
- Nenhum vídeo, senha ou token foi enviado a serviços durante o desenvolvimento; custos, aprovação, revisão e métricas reais não foram simulados.

## Verificações executadas

1. `python3 tools/check_version_sync.py` — APROVADO (`0.1.167`, código `168`).
2. `python3 tools/validate_source.py` — APROVADO.
3. 63 scripts `tools/test_*.py` — 63 APROVADOS; 0 falhas, execução paralela (registro `/mnt/data/uai167_tests.json`).
4. Parser PSI do Kotlin — 6 arquivos alterados, zero erros sintáticos (não comprova resolução de tipos, Compose, Gradle ou build Android).
5. `tools/package_source.py` — APROVADO: 542 arquivos, CRC e validação da fonte reextraída.

## Pendências

- Build Gradle `:app:assembleDebug`, instrumentação Android, navegação real, permissões/SAF e testes físicos. No ambiente atual não há Gradle instalado nem `gradle-wrapper.jar` para compilação. `sh ./gradlew :app:assembleDebug` foi tentado e retornou explicitamente indisponibilidade do Gradle.
- Testar em Android real com tokens OAuth autorizados por usuários com acesso à API, e confirmar compatibilidade das versões/formatos vigentes da Meta/TikTok.
- Implementar OAuth nativo aprovado e renovação segura dos tokens, além de métricas oficiais, após configuração da infraestrutura de desenvolvedor.

Fontes oficiais de API:
- https://developers.tiktok.com/docs/en/content-posting-api-get-started
- https://developers.tiktok.com/docs/en/content-posting-api-reference-direct-post
- https://www.postman.com/meta/facebook/documentation/r56bjfd/facebook-api
