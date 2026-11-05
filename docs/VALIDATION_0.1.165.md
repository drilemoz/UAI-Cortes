# Uai Cortes 0.1.165 — Redes Sociais (fase 1)

**Base:** Uai Cortes 0.1.164. **Destino:** 0.1.165 / versionCode 166. **Data:** 05/11/2026.

## Implementado nesta entrega

1. **Aba Redes** no rodapé de Início, Projetos e Configurações, Material 3 e temas existentes.
2. **Cadastro local de destinos** independentes: TikTok, YouTube, Kwai, Facebook Página e Facebook Perfil. Nome e URL HTTPS opcionais; não realiza autenticação nem lê métricas de contas.
3. **Painel de publicações:** estados preparados, compartilhamento aberto sem confirmação, postagem confirmada manualmente. Registro por vídeo e por destino; exibição de métricas **digitadas** manualmente, sem valores inferidos.
4. **Publicação assistida:** após exportação do editor e de Animar Imagens, entrada “Publicar nas redes sociais”. Pode escolher um MP4 pelo seletor SAF ou usar o exportado, selecionar contas e reutilizar descrição e hashtags. Tarefas persistentes sem duplicar o par `conta + URI do vídeo`; compartilhar individualmente pelo seletor Android, com permissão de leitura para o destino e botão de copiar texto. Abrir o chooser **não equivale a publicação**; confirmação manual explícita, sem upload de rede próprio.
5. **Central de APIs e Contas** em Configurações, retornando à tela Gemini existente; links oficiais de desenvolvedores e status correto “OAuth pendente”; nenhum teste falso de conexão. Exportação/importação via SAF de **somente nomes e tipos das contas locais**, sem URLs, tokens, senhas, segredos, histórico ou vídeos.
6. **Persistência:** `AtomicFile` com versão de catálogo independente, validação de IDs, URL HTTPS e URIs `content://`, limite de catálogo e proteção contra sobrescrita de dados corrompidos. `VideoProject` continua no `schemaVersion=4`.
7. **Compatibilidade:** mantidos cinco APKs no workflow GitHub Actions, preferências e credenciais Gemini, Estúdio de Cenas, projetos existentes, editor manual e funcionalidades anteriores.

## Arquivos de implementação

- Criados: `SocialStore.kt`, `SocialScreens.kt`, `SocialStoreAndroidTest.kt`, `tools/test_social_module_165_contract.py`.
- Modificados: `MainActivity.kt`, `HomeScreens.kt`, `EditorNavigation.kt`, `ExportScreen.kt`, `VideoSavedDialog.kt`, `AnimateImagesScreen.kt`, `tools/validate_source.py`, `tools/test_voice_ia_contract.py`, `tools/test_flow_studio_161_contract.py`, `tools/test_flow_studio_164_contract.py`.
- Sincronizados: `VERSION`, `app/build.gradle.kts`, `.github/workflows/android-apk.yml`, `tools/package_apks.py`, `app_identity.json`, `github-manager.json`, `AppReleaseNotes.kt`, `AGENTS.md`, `README.md`, `CHANGELOG.md`, `RELEASE_NOTES.md`.

## Validações executadas

| Verificação | Estado |
| --- | --- |
| `python3 tools/check_version_sync.py` | Passou — versão 0.1.165 / versionCode 166 |
| `python3 tools/validate_source.py` | Passou |
| 61 scripts `tools/test_*.py` (45 iniciais + 16 finais, teste legado de navegação corrigido e reexecutado) | Passaram |
| PSI sintático Kotlin dos 9 arquivos alterados/instrumentados | Passou: 0 erros sintáticos |
| Gerador de notas curtas de GitHub Release | Passou |
| Integridade ZIP/CRC e validação após reextração | Ver resultado de `tools/package_source.py` |

**Não executados:** Gradle/AGP, compilação Kotlin com resolução de símbolos, testes JUnit/Android em aparelho/emulador, teste visual Material 3, permissão SAF em aparelho, chooser de compartilhamento real. O ambiente não dispõe de Gradle instalado nem wrapper JAR, SDK e dependências para um build local confiável. O workflow CI deve compilar as cinco variantes Release e revelar eventuais erros de tipo/lint.

## Limitações e próximas fases

- **Não há OAuth configurado nem API de publicação direta** para TikTok, Facebook ou YouTube. O perfil pessoal do Facebook e o Kwai utilizam compartilhamento manual; um compartilhamento não garante que o app destino publique o vídeo nem preencha a descrição automaticamente.
- **Não há sincronização automática de estatísticas.** Valores vistos no painel são dados manuais, não verificados pela API e sem histórico temporal de snapshots.
- Publicação de vídeo em múltiplos destinos ocorre **sequencialmente pelo compartilhamento do Android**, não como fila automática de upload simultâneo.
- URLs de vídeo salvas por SAF podem perder acesso se o provedor revogar permissão. Reabrir o MP4 na galeria pode ser necessário.
- A integração futura requer OAuth 2.0, URLs de redirecionamento registradas, revisão de apps/permissões (Meta/TikTok), tratamento de quotas, upload retomável, status confirmado pelo servidor, política de repetição segura, métricas reais por conta e autenticação cifrada com Keystore.
- Nenhuma credencial/token de terceiros é solicitada ou exportada nesta versão.

## Como testar no Android

1. Instale a build 0.1.165 depois de compilar; abra a aba **Redes**.
2. Adicione dois destinos para Facebook: Página e Perfil; acrescente TikTok, YouTube, Kwai se quiser. Feche e reabra para conferir persistência.
3. Exporte um vídeo normalmente e toque **Publicar nas redes sociais**; escolha os destinos e preencha descrição/hashtags.
4. Toque **Preparar destinos**, depois **Compartilhar** um de cada vez; confirme se o Android mostra o aplicativo correto no chooser. Verifique que a publicação não fica como confirmada automaticamente.
5. Em **Redes**, marque como publicada manualmente somente após conferir no aplicativo da rede; insira um link e métricas reais caso disponíveis. Reabra para verificar a recuperação.
6. Em **Configurações → Central de APIs e Contas**, exporte a configuração JSON, verifique que não contém tokens nem links privados, importe e confirme que não foram criadas contas duplicadas.
7. Teste tema Claro/Escuro/AMOLED, botão Voltar, compartilhamento cancelado, arquivo removido e ausência de permissões SAF.
