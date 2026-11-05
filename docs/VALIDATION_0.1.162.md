# Uai Cortes 0.1.162 — Estúdio de Cenas com revisão guiada

Data: 05/11/2026. Base: `UaiCortes-v0.1.161-Estudio-de-Cenas-IA-Fonte.zip` (fontes extraídas na sessão anterior). `versionName=0.1.162`; `versionCode=163`.

## Diagnóstico da captura enviada

A imagem mostrava formulário completo da história mesmo antes de haver uma proposta, ajustes numerosos na mesma tela e um botão Gerar história desabilitado apesar de haver texto na ideia. O código anterior continha uma lista local de ideias para "Surpreenda-me": o botão não chamava a API Gemini. A variável global `busy` podia desabilitar a geração durante outras tarefas; a indicação da operação ficava fora da área visível. **Não foi possível afirmar apenas pela captura qual requisição estava ativa.**

## Implementado

- **História guiada:** ideia e CTA de geração Gemini em destaque; `Surpreenda-me` escolhe imediatamente sugestão local, sem API nem custo, com feedback explícito. Roteiro colado e configurações de duração/estilo/gênero estão recolhidos sob demanda. Revisão exibe título/sinopse, roteiro expandível e campos editáveis apenas ao pedir. Aprovação antes de dividir permanece obrigatória.
- **Cenas legíveis:** cartões com título, número, duração **estimada**, personagens, falas com atribuição, câmera, efeitos e ações; continuidade detalhada expansível. Um botão principal por estágio (roteiro IA, imagem ou copiar prompt), edição rápida e demais ferramentas em menu, sem remover reordenação, duplicação, exclusão com confirmação, importação/exportação/compartilhamento, recuperação ou modo manual.
- **Edição por personagem:** formulário de cena com título, duração estimada, personagens e ações; falas por personagem podem ser adicionadas/editadas individualmente. Câmera, sons, descrição visual, estados de continuidade e prompt estão sob painel técnico recolhido. Diálogos legados em texto livre permanecem acessíveis; conversão para falas separadas somente dentro de limites seguros, sem truncamento silencioso.
- **IA estruturada:** instrução de divisão pede `dialogueLines[]` com `speaker`/`speech` e `durationSeconds`. O leitor aceita documentos antigos sem esses campos. Se a IA retornar durações válidas e próximas do alvo, as preserva; quando faltam ou são incoerentes, distribui o orçamento como estimativa. **Nenhuma duração é inserida no texto do prompt destinado ao Flow.**
- **Estado e progresso:** identificador claro da tarefa em andamento sob o cabeçalho, spinner e barra indeterminada, cancelamento de chamada Gemini, estados de importação/exportação, erro e feedback de resultado; advertência de possível consumo mesmo após cancelamento. Gemini permanece sob confirmação antes de qualquer geração de texto/imagem; nenhuma geração automática.
- **Compatibilidade:** `FlowStudioDraft` continua em schema 1 com novos campos opcionais em `StudioScene`; `VideoProject`/`ProjectStore` permanecem `schemaVersion=4`. O editor manual e clientes Gemini anteriores não foram reconstruídos. Credenciais/ZIP/SAF e temas atuais preservados em código.

## Arquivos de implementação modificados

- `app/src/main/java/br/com/uaicortes/FlowStudioScreen.kt`
- `app/src/main/java/br/com/uaicortes/FlowStudioModel.kt`
- `app/src/main/java/br/com/uaicortes/FlowStudioAi.kt`
- `app/src/main/java/br/com/uaicortes/FlowStudioStorage.kt`
- `app/src/test/java/br/com/uaicortes/FlowStudioModelTest.kt`
- `tools/test_flow_studio_161_contract.py` (checagem de versão sequencial)
- `tools/test_flow_studio_162_contract.py` (novo teste de contrato da interface)
- `app/src/main/java/br/com/uaicortes/AppReleaseNotes.kt`
- `app/build.gradle.kts`, `.github/workflows/android-apk.yml`, `tools/package_apks.py`, `VERSION`, `app_identity.json`, `github-manager.json`
- `AGENTS.md`, `README.md`, `CHANGELOG.md`, `RELEASE_NOTES.md` e este relatório.

## Validações e limites

### Executadas sem Android
- Verificação de sincronização de versão, metadados e CI (`tools/check_version_sync.py`).
- Testes de contrato da versão anterior e da nova interface (`tools/test_flow_studio_161_contract.py`, `tools/test_flow_studio_162_contract.py`).
- Parser Kotlin PSI para fontes e teste JUnit alterados (apenas **sintaxe**, não tipos/recursos nem Compose).
- `tools/validate_source.py` e testes estáticos adicionais disponíveis.
- Empacotamento fonte oficial (`tools/package_source.py`), com `.github/`, inspeção CRC e validação de reextração.

### Não executadas / não comprovadas
- **Gradle/SDK Android**: distribuição Gradle instalada e SDK necessários não disponíveis neste container; nenhum APK nem testes JUnit instrumentados reais foram produzidos aqui.
- **Gemini real**: nenhuma chave foi lida nem qualquer chamada API autorizada executada; validade do JSON estruturado para modelos da conta e latência precisam de teste com consentimento.
- **Usabilidade no Android**: dimensões de tela, fontes grandes, estado após girar o aparelho, cancelamento da API, seleção SAF, tema claro/escuro e execução no Redmi dependem de APK compilado e teste real.
- Geração de imagem 9:16, referências visuais, importação/exportação ZIP e edição de vídeo continuam sujeitas à validação física anterior; estes componentes não foram reimplementados nesta versão.

### Pontos de atenção
- Duração por cena é **estimativa de planejamento**, não representa um novo parâmetro nem garantia de duração do Google Flow.
- Alterar roteiro manualmente pode deixar o prompt anterior desatualizado; use regeneração de roteiro Gemini sob confirmação ou ajuste o prompt manualmente.
- Se cancelar após a transmissão do pedido, o consumo/cobrança pode ter ocorrido; o app não reenvia silenciosamente.
- Sem projeto editável no Flow via API; exportação dos prompts/imagens continua manual.
