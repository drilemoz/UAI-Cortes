# Uai Cortes 0.1.163 — Estúdio por clipes e Gemini global

Data da intervenção: 05/11/2026. Base analisada: `UaiCortes-v0.1.162-Estudio-de-Cenas-Inteligente-Fonte.zip`. Nova versão: `versionName=0.1.163`, `versionCode=164`.

## Diagnóstico real

A versão anterior tinha dois comportamentos inconsistentes:

1. `GeminiVideoCaptionClient.generateStudioJson()` usava `requestJsonOnce` diretamente em vez da rota compartilhada `GeminiKeyFailover`. Isso fazia o recurso de história/divisão/roteiro de cena ignorar a alternância opcional configurada nas preferências.
2. `GeminiSceneImageClient.createImage()` e `listImageModels()` usavam a credencial recebida, sem a mesma política de alternância opcional; o Estúdio não se comportava como outros módulos Gemini.

O aviso genérico apresentado na captura pode corresponder a HTTP 408 ou 5xx. **A captura isolada não prova que a causa foi cota, chave inválida ou projeto.** Uma nova chave não resolve necessariamente 503 (indisponibilidade do serviço). Troca automática continua intencionalmente proibida para 503/500/408, timeout de rede e demais resultados incertos: não reenviar geração potencialmente faturável sem confirmação. A política de alternância é **opt-in**, limitada a **HTTP 429 confirmado**, entre credenciais com IDs de projetos Google Cloud distintos, sem repetição do mesmo projeto e com pausa local de 10 minutos para o projeto/modelo. Os IDs são declarados pelo usuário, não verificados automaticamente junto à conta Google.

## Recursos entregues em código

- **Criar projeto → Ideia → Cenas (clipes) → Exportar:** a história completa e sua aprovação deixam de ser obrigatórias para iniciar a construção manual. A ideia curta pode ser usada como ponto de partida pelo Gemini para sugerir cenas, sem obrigar geração prévia de título/sinopse.
- **Uma cena = um clipe individual do Flow:** é possível criar Cena 1 (dois personagens olhando), Cena 2 (um personagem reagindo/falando) e Cena 3 (outro respondendo). Cada cartão possui prompt e imagem inicial próprios. O Gemini recebe a instrução explícita de não confundir cenas com tomadas de um mesmo clipe.
- **Editor reduzido:** inicialmente apresenta somente a ação da cena. Os botões opcionais revelam falas, câmera e sons; título, duração estimada, personagens, descrição visual, estados de continuidade e prompt ficam recolhidos em Mais detalhes. Diálogos antigos continuam aceitos.
- **Criação manual/offline:** `FlowStudioClipPolicy.prepare` monta prompt em inglês com fala/texto em português brasileiro, sem inserir duração ou 9:16, preservando os dados da cena. Gemini pode ajudar com história, sugestões de clipes, refinamento individual e imagem apenas após confirmação de envio.
- **Imagem antes ou depois do prompt:** cartões expõem Importar imagem e Gerar imagem independentemente de já existir prompt; impede solicitação de imagem genérica quando não existe ideia nem descrição da cena.
- **Gemini global:** `GeminiKeyFailover` prioriza a chave atualmente selecionada no armazenamento compartilhado para novas operações, mesmo que a tela tenha capturado outra credencial anteriormente. O Estúdio passa a usar a mesma política em texto e imagem. A seleção automática bem-sucedida também passa a ser a nova chave ativa global.
- **Diagnóstico seguro:** mostra categoria e código HTTP na geração de texto em falha 5xx; o Estúdio informa o nome (não a chave) do perfil ativo, estado de alternância e eventual troca concluída. O diálogo anterior à operação explica que uma segunda tentativa 429 pode consumir cota/gerar cobrança. Nunca exibe chave nem corpo livre de erro do servidor.
- **Compatibilidade:** editor manual e refinamento em lote, exportação SAF ZIP e referências existentes continuam; `VideoProject.schemaVersion=4`, `FlowStudioDraft.schema=1`, imagens, projetos salvos e `GeminiCredentialStore` cifrado preservados. Não cria projeto diretamente no Google Flow e não gera vídeo via API paga.

## Arquivos modificados

**Estúdio e Gemini:**
- `FlowStudioScreen.kt`, `FlowStudioModel.kt`, `FlowStudioAi.kt`, `FlowWorkspaceScreen.kt`
- `GeminiVideoCaptionClient.kt`, `GeminiSceneImageClient.kt`, `GeminiKeyFailover.kt`, `GeminiKeyPoolSettings.kt`
- **Novo:** `FlowStudioClipPolicy.kt`
- `app/src/test/java/br/com/uaicortes/FlowStudioModelTest.kt`
- `tools/test_flow_studio_161_contract.py`, `tools/test_flow_studio_162_contract.py` e **novo** `tools/test_flow_studio_163_contract.py`

**Release e metadados:**
- `VERSION`, `app/build.gradle.kts`, `.github/workflows/android-apk.yml`, `app_identity.json`, `github-manager.json`, `tools/package_apks.py`, `AppReleaseNotes.kt`, `AGENTS.md`, `README.md`, `CHANGELOG.md`, `RELEASE_NOTES.md` e este relatório.

## Testes e validação executados

- `python tools/check_version_sync.py` — **aprovado** (Gradle, versionCode 164, sobre, workflow, JSON e notas sincronizados).
- **59 scripts** `tools/test_*.py` — **aprovados em duas rodadas**, após correção do cabeçalho de `RELEASE_NOTES.md`; o teste de release foi repetido e passou.
- `python tools/validate_source.py` — **aprovado** (validador estrutural local, não é compilação Android).
- Parser Kotlin PSI em 10 arquivos fonte alterados — **0 erros sintáticos** (não verifica tipos Compose, Gradle nem linkagem).
- `kotlinc`/JVM de `GeminiFailoverPolicy` — **11 cenários aprovados** (429 opt-in e negações por projeto/erro).
- Simulação Kotlin/JVM compilando o **arquivo real** `GeminiKeyFailover.kt` com stubs locais de Android/Keystore — **aprovada** nos casos: chave global selecionada sobre referência antiga; alternância em 429; proibição de nova chamada em 503; proibição de alternância entre chaves do mesmo projeto. É simulação, não chamada real à API.
- O script de pacote `tools/package_source.py` confere conteúdo exigido, ZIP CRC, reextração e `validate_source.py` no projeto extraído; resultado descrito ao final desta entrega.

## Pendências e limitações

- **Sem APK e sem build Gradle Android completo** neste ambiente: Gradle e SDK Android não estavam instalados. Os testes JUnit do aplicativo e os testes instrumentados com Keystore, armazenamento, SAF e UI devem ser executados no CI GitHub e em um Android real antes de considerar a versão validada.
- **Sem chamadas autorizadas à API Gemini**: a existência/acesso real aos modelos de cada chave, tarifação, status 429/503 no projeto do usuário, geração de imagem 9:16 e comportamento de failover com credenciais verdadeiras ainda dependem de teste com a conta do usuário.
- Um erro **503 ou 500** não causa alternância automática e pode persistir mesmo ao trocar de chave. Verificar status registrado, serviço/modelo e tentar manualmente depois; não assumir que novas chaves criam novas quotas no mesmo projeto.
- O modelo de texto ou imagem selecionado pode não estar disponível em outra chave/projeto. Uma troca de credencial não muda o modelo automaticamente; `404`/`403` exigem revisão explícita de modelo/acesso.
- Mudanças manuais em ações/falas/câmera podem deixar o prompt já gerado desatualizado; conferir e usar `Preparar prompt sem IA` ou refazer com Gemini antes de copiar.
- As durações são estimativas organizacionais, não são inseridas nos prompts de Flow. Continuidade de personagens/quadros não é garantida. O Flow recebe manualmente prompts/imagens; vídeos são importados manualmente depois.
- Não iniciar geração paga sem confirmação e não tratar erro de rede como prova de ausência de consumo.

## Como configurar a alternância

Em **Configurações → Gemini → Chave Gemini para todo o aplicativo**, selecione a chave que deseja usar em todos os novos pedidos. Para alternância automática, registre pelo menos **duas credenciais de projetos Google Cloud realmente distintos**, com seus IDs preenchidos, e habilite **Troca automática após limite 429** aceitando o aviso. Não basta cadastrar duas chaves do mesmo projeto. A opção permanece desligada se não for ativada pelo usuário. Quando um 429 confirmado provocar uma troca bem-sucedida, a nova chave se tornará ativa para os próximos pedidos do app.
