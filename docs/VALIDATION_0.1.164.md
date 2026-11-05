# Uai Cortes 0.1.164 — Wizard visual e biblioteca reutilizável

**Base:** código-fonte 0.1.163 fornecido anteriormente no mesmo projeto. **Destino:** 0.1.164 / `versionCode=165`. **Data:** 05/11/2026.

## Funcionalidades implementadas

1. **Wizard de cinco etapas** no Estúdio: **Estilo → Personagens → Cenários → Cenas → Flow**. O nome continua sendo solicitado em **Novo projeto**. Seletores compactos e cartões Material 3 substituem a primeira tela com muitos campos; os parâmetros opcionais ficam recolhidos. Setas inferiores permitem avançar/voltar, e a etapa atual é salva no rascunho local.
2. **Estilo persistente:** três estilos principais (3D, 2D e realista), personalizado e biblioteca de estilos escolhidos explicitamente para reutilizar em outros projetos. A ideia e o roteiro existente podem ser informados opcionalmente.
3. **Sugestões de personagens sem API:** alienígena, pessoa, animal, fruta, robô e objeto animado. As fichas sugeridas já trazem atributos iniciais; o usuário revisa nome e aparência em dois campos e expande roupa, voz, personalidade e objetos apenas se desejar. Adição, edição e exclusão com confirmação.
4. **Sugestões de cenários sem API:** cozinha, praça, nave espacial, floresta, quarto e cidade. Criação, edição e exclusão confirmada, com detalhes extras opcionais.
5. **Biblioteca entre histórias:** novo arquivo JSON privado atômico `flow_studio_library_v1/library.json`, com listas de pessoas, lugares e estilos; imagem opcional copiada da galeria para `flow_studio_library_v1/images/`. Referências importadas para uma nova história usam uma *cópia independente da ficha* com imagem compartilhada, sem deletar a biblioteca quando uma história é removida.
6. **Seleção de imagem opcional e local** via Storage Access Framework (`ACTION_OPEN_DOCUMENT`): JPEG/PNG/WebP, até 12 MB, cabeçalhos e dimensões de imagem verificados, cópia para diretório privado. Sem permissões gerais de armazenamento, sem geração de imagem obrigatória e sem upload ao selecionar a foto. As imagens de personagens/cenários entram no ZIP como `referencias/personagens/<arquivo>` e `referencias/cenarios/<arquivo>`; cópias repetidas do mesmo recurso não duplicam entradas ZIP.
7. **Enriquecimento de fichas com Gemini** exclusivamente após botão, confirmação de modelo/chave/custos não verificáveis, chamada textual e revisão da sugestão. Novas operações `studio_person` e `studio_place` entram no histórico de consumo do Gemini, com o mesmo seletor global de chave e failover opt-in apenas para 429 confirmado. A IA não sobrescreve a ficha enquanto o usuário não aprovar e não envia imagem, arquivo interno, URI ou chave.
8. **Uma cena = um vídeo do Flow** como na 0.1.163. O editor agora oferece chips para escolher personagens cadastrados e a pessoa que fala. A imagem da cena, importada da galeria ou gerada por IA sob confirmação, é opcional; ter prompt pronto basta para copiar/usar no Flow. O editor manual original e seus refinamentos continuam acessíveis em Avançado.
9. **Compatibilidade de rascunhos:** `FlowStudioDraft.schema=1` preservado. Campo novo `wizardVersion=2` mapeia automaticamente etapas antigas `0→0`, `1→3`, `2→4` sem apagar cenas, pessoas, lugares ou mídias; `StudioPerson` e `StudioPlace` ganharam IDs e nomes de imagem opcionais com defaults legados. O esquema dos projetos de vídeo `schemaVersion=4` não foi alterado.

## Arquivos alterados/criados

- **Novo:** `app/src/main/java/br/com/uaicortes/FlowStudioWizardUi.kt` — assistente e cartões das cinco etapas.
- **Novo:** `app/src/main/java/br/com/uaicortes/StudioReferenceLibrary.kt` — catálogo privado com salvamento atômico, importação de imagens e sugestões locais.
- `FlowStudioScreen.kt`, `FlowStudioModel.kt`, `FlowStudioAi.kt`, `FlowStudioStorage.kt`, `GeminiVideoCaptionClient.kt` — integração da interface, IA por ficha, imagens e exportação.
- `app/src/test/java/br/com/uaicortes/FlowStudioModelTest.kt` — novos testes de migração, imagens e sugestões.
- **Novo:** `app/src/androidTest/java/br/com/uaicortes/StudioReferenceLibraryAndroidTest.kt` — teste de persistência e galeria para emulador/aparelho (não executado aqui).
- **Novo:** `tools/test_flow_studio_164_contract.py` e atualização compatível de `tools/test_flow_studio_161_contract.py` e `tools/test_flow_studio_163_contract.py`.
- Versionamento: `VERSION`, `app/build.gradle.kts`, `.github/workflows/android-apk.yml`, `tools/package_apks.py`, `app_identity.json`, `github-manager.json`, `AppReleaseNotes.kt`, `README.md`, `CHANGELOG.md`, `RELEASE_NOTES.md`, `AGENTS.md`.

## Validação efetuada no ambiente de edição

| Verificação | Resultado |
|---|---|
| `python3 tools/check_version_sync.py` | APROVADO — versão 0.1.164, versionCode 165 |
| `python3 tools/validate_source.py` | APROVADO (estrutura; não é Gradle) |
| 60 scripts `tools/test_*.py` | APROVADOS (contratos e regressões estáticas) |
| Análise sintática Kotlin PSI | APROVADA nos 7 arquivos Kotlin de produção examinados e nos 2 arquivos de testes modificados/criados — zero erros sintáticos |
| `./gradlew`/`sh ./gradlew --offline :app:compileDebugKotlin` | NÃO EXECUTOU COMPILAÇÃO — Gradle 8.11.1 não instalado; wrapper deste projeto depende do Gradle instalado |
| JUnit via Gradle, lint, Android instrumentado, UI real e Gemini autorizado | NÃO EXECUTADOS; CI e testes em aparelho necessários |
| CRC/integridade ZIP | Validar no empacotamento final |

## Limitações e cuidados

- **Não existe APK 0.1.164 compilado ou validado nesta entrega.** Type checking Compose, permissões reais Android, integração Gemini com contas e modelos disponíveis, comportamento visual em diversas larguras/fontes, SAF em aparelho e migração em instalação real permanecem pendentes.
- As sugestões locais enriquecem com valores predeterminados e **não consomem API**. O botão **Enriquecer com Gemini** requer chave/modelo compatível, acesso à internet e confirmação; não há chamada paga automática.
- O modelo Gemini do usuário pode responder HTTP 503 apesar de trocar a chave. O aplicativo não tenta outra geração silenciosa para 500/503, timeout ou resultado incerto. A política de alternância por 429 confirmada da versão anterior foi mantida, sem prometer resolver indisponibilidade do provedor.
- Imagens de personagens/cenários podem ajudar na continuidade, mas o Flow não recebe automaticamente o projeto e não há garantia de manter personagens idênticos. Fotos de referências são exportadas somente por ação explícita do usuário.
- A biblioteca global é privada do app e permanece ao remover uma história; ao desinstalar, seu armazenamento local será perdido, salvo backup independente. Os ZIPs exportados contêm referências, mas **não existe importador universal automático de biblioteca a partir de ZIP** nesta versão.
- A tela avançada antiga foi preservada e a exportação/editação manual do vídeo não foi substituída. O novo Wizard não altera `VideoProject.schemaVersion=4`.

## Próximos passos técnicos

Executar o workflow `Gerar APK Uai Cortes` do GitHub Actions (`:app:testDebugUnitTest`, `:app:compileDebugAndroidTestKotlin`, `:app:lintDebug`, `:app:assembleRelease`), corrigir eventuais erros de tipos e testar no Android com pelo menos duas histórias e um personagem/cenário reutilizados, importando JPEG/PNG/WebP e verificando as cinco etapas, a migração da 0.1.163, a recuperação após reinício, os temas e o ZIP.
