# Uai Cortes · versão 0.1.190 — Central de Vozes Google

## v0.1.190 · versionCode 191
- Central acessível na Home, no editor Vozes IA e na Narração. Seleção de voz para personagem/fala continua sem solicitar síntese.
- Catálogo JSON local é reutilizado em todas as telas e mantido em falhas, com atualizar manualmente e opção de atualização automática (desativada inicialmente).
- Prévia unificada com a frase fixa: "Olá! Este é o Uai Cortes. Aqui você corta vídeos, edita cenas e cria novas histórias. Fácil, não é? Agora... vamos ouvir esta voz!". Armazenada em `filesDir` por ID de modelo, voz, estilo e frase. Prévia existente reproduz sem API; primeira geração exige confirmação de cota.
- Limites 50/100/250 MB configuráveis, LRU com prioridade para não favoritas, apagar prévias somente por ação explícita; catálogo e favoritos permanecem.
- Vozes IA simplificada: barra Central/Tom/Gerar/Inserir, vozes dos personagens persistem e herança por legenda preservada. Narração reaproveita mesmo catálogo e amostras; sem alterações automáticas em projetos.
- Testes estruturais, Kotlin PSI e testes de identidade de prévia; sem alegar build Android completo sem GitHub Actions.

# Uai Cortes · versão 0.1.189 — Interpretação por personagem e revisão de vozes

## v0.1.189 · versionCode 190
- **Interpretação por personagem:** salva preferências de estilo para cada personagem, herdadas por suas falas. Permite exceções por fala, herança/restauração e interpretação geral persistida, sem apagar vozes já selecionadas.
- **Teste de combinação:** novo botão para gerar uma amostra curta após consentimento explícito sobre consumo de cota Gemini. O WAV de teste vai para cache temporário e não é inserido na timeline nem nos rascunhos.
- **Revisão do lote:** mostra prontas, pendentes, sem voz, áudio incompatível com escolhas atuais, sinais de sincronização e fila incerta; detalhamento por personagem e sem personagem.
- **Geração seletiva:** solicitar falas pendentes de um personagem pelo cartão, ou apenas pendentes do filtro ativo no painel de revisão. Diálogo de confirmação de chamadas permanece obrigatório.
- **Segurança de retomada:** compara voz, modelo e interpretação antes de retomar ou reutilizar WAVs, marcando itens divergentes como STALE sem tentar automaticamente; arquivos existentes intactos.
- **Compatibilidade:** `voiceIaVoices` adiciona campos opcionais `defaultStyle`/`speakerStyles`/`cueStyles`, preservando JSON schemaVersion=4, Gradle e demais fluxos.
- **Validação:** testes estruturais e Kotlin isolado, não substituem build Gradle nem testes físicos Android.

# Uai Cortes · versão 0.1.188 — Voz Gemini por personagem

## v0.1.188 · versionCode 189
- **Personagens:** atribuição local de uma voz Gemini por personagem reutilizando o ID já cadastrado em Legendas, persistida no projeto, com exceção por legenda e opção de herdar/remover. Preferência por voz geral continua consentida.
- **Interface:** faixa horizontal de personagens, filtro e busca compactos, cartões com ícones e cor do personagem, prévia reduzida com botão de ampliar. Lista filtrada não altera a fila total.
- **Fila segura:** itens de voz incompatível passam a STALE; WAVs ficam preservados e não são feitas chamadas automáticas por alteração de personagem.
- **Legendas:** geração automática permite visual clássico (mesmo estilo inicial SRT) ou Karaokê IA; mantém dados temporais, IDs e marcações por palavra. Sem alterar arquivos importados.
- **Compatibilidade:** `voiceIaVoices` opcional em `VideoProject`, `schemaVersion=4`, versionamento, cinco APKs, rascunhos e backups preservados.
- **Pendente:** confirmar compilação e instrumentação Android no CI e testar UI/performance em dispositivos físicos.

# Uai Cortes · versão 0.1.187 — Correção de compilação do CI154

## v0.1.187 · versionCode 188
- **Build AndroidTest:** removido o import incorreto de `assertDoesNotExist` em `HomeAboutPreviewAndroidTest.kt`, que impedia `:app:compileDebugAndroidTestKotlin`. As asserções de ausência continuam ativas.
- **Prevenção de regressão:** verificação de código-fonte para imports incorretos das asserções Compose em testes instrumentados.
- **Versionamento:** sincronizados Gradle, CI, empacotador dos cinco APKs, Sobre/Novidades, manifesto GitHub Manager e documentação. Sem alterações em recursos do editor ou formato dos projetos.
- **Pendente:** confirmar `:app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:lintDebug :app:assembleRelease` no GitHub Actions e testes Android físicos.

# Uai Cortes · versão 0.1.186 — Salvamento e atualizações mais diretos

## v0.1.186 · versionCode 187
- **Sobre:** somente Verificar atualizações como ação principal; removidos os botões Ver novidades e Testar visual da Home do Sobre.
- **Configurações:** removido o cartão independente de Atualizações pelo GitHub. A tela fica vinculada ao Sobre.
- **Atualizador:** não oferece campo para repositório manual; usa a origem pública definida no APK pelo CI. Histórico de Releases limitado a 20 por consulta, com capacidade de leitura de até 8 milhões de caracteres.
- **Atualização no aplicativo:** mantém confirmação, download interno com SHA-256, validação do pacote e abertura do instalador do Android; não há download/instalação silenciosos.
- **Salvar projeto:** diálogo curto e vertical com alternativas claras para não persistir projetos indesejados. Mantido Salvar cópia, restauração e rascunhos temporários.
- **Testes:** contratos de navegação/segurança e casos de resposta excessiva; build completo Android e instalação física ainda pendentes.

- **Legendas automáticas:** removidas descrições duplicadas e títulos de passos sem utilidade, substituídos por estado curto e ajuda `?`; duração e limite de 10 minutos preservados.
- **Gemini:** confirmação curta com informação essencial de consentimento sobre envio de áudio ao Google e risco de uso de cota/cobrança. Detalhes de estimativa, cache e cotas continuam consultáveis sem segunda chamada.
- **Personagens:** diálogo sob medida, botões de seleção compactos e ajuda contextual; associação só muda após toque explícito.
- **Revisão de IA:** o texto explicativo de diarização passou para `?`, sem presumir nomes reais nem mudar as cores exportadas.
- Projetos `schemaVersion=4`, backups, modelo Vosk, Gemini, cinco APKs e funções anteriores preservados. Build Android não testado localmente.

# Uai Cortes · versão 0.1.184 — Interface de Legendas compacta
- Barra superior do player combina Linha do tempo, ajuda, controles, Seguir e Ampliar; remove resumo de horário e mensagem de reprodução redundantes.
- Zoom 1×/2×/4× agora fica em menu junto ao progresso, sem linha exclusiva.
- Filtro de personagem em menu com opção Gerenciar personagens; timeline continua com todas as falas, atenuando as não filtradas. Selecionar bloco fora do filtro restaura visualização de todas as legendas.
- Salvar, Desfazer, Buscar e Mais em cabeçalho compacto; Mais reúne Importar, Exportar, Editar todas e Apagar todas com confirmação. Nova e Automáticas ficam visíveis.
- Toque manual na lista desativa acompanhamento automático e mantém seleção; reativação pelo ícone. Prévia compacta limitada a 190 dp para aumentar área da lista.
- Preservados IDs/horários/cores, projetos legados schemaVersion 4, Vosk/Gemini e CI 5 APKs; build Android e testes físicos pendentes.

# Uai Cortes · versão 0.1.183 — Diarização Gemini revisável
- Gemini 3.5 Transcribe pode solicitar diarização (`mode.diarization_mode=speaker`) junto com marcações por palavra, opt-in e na mesma solicitação de transcrição.
- Parser associa `word_info.speaker` a IDs provisórios únicos por execução, separa legendas na mudança de voz e deixa sem identidade quando a API não identifica; não adivinha nomes.
- Nova revisão permite criar personagens locais por voz, renomear, vincular a personagem existente ou deixar sem atribuição; a confirmação aplica todos os vínculos atomicamente ao rascunho.
- Checkpoint de transcrição preserva os rótulos provisórios para revisão após interrupção; compatibilidade legado `schemaVersion=4` e versionCode 184.
- Identificação infalível e reconhecimento facial NÃO implementados; CI Android e validação com resposta real Gemini pendentes.

# Uai Cortes · versão 0.1.182 — Legendas por personagem e revisão compacta
- Adicionado cadastro local de até 16 personagens com nome/cor, associação manual por legenda, filtros, indicação na lista/timeline e aplicação opcional de cor com confirmação.
- Prévia da tela Legendas inicia compacta e pode ser ampliada; zoom 1×/2×/4×, faixa de legendas com cor por personagem, distinção entre legenda ativa e selecionada e lista com acompanhamento opcional.
- Controles secundários recolhidos em Mais ferramentas para reservar altura para a revisão. Projetos antigos e `schemaVersion=4` preservados; versionCode 183.
- Reconhecimento automático de locutores NÃO incluído; compilação Android e testes de MP4 no aparelho pendentes.

# Uai Cortes · versão 0.1.181 — Legendas com prévia e faixa de vídeo

- Corrigida prévia preta quando não existe legenda selecionada; player Media3 sempre disponível com clipes do projeto.
- Linha do tempo virtualizada passa a exibir faixa de vídeo/foto e faixa de legendas, com cursor e miniaturas sob demanda.
- O vídeo pode ser navegado pela faixa ou barra de progresso; Nova usa o horário atual e mantém limites do projeto.
- Adicionados testes de limites entre clipes e contratos de UI; `versionCode 182`, sem alterar `schemaVersion=4`.

# Uai Cortes · versão 0.1.180 — Correção do instalador Vosk

- Novo `VoskArchiveInstaller` puro Kotlin/JVM valida ZIP completo com CRC, limites de 4.000 entradas e 300 MiB de extração, proteção contra Zip Slip/duplicação, estrutura compatível com modelo português oficial e instalação transacional após validação.
- Mantém modelo já funcional; substitui apenas pasta incompleta após conferir o novo pacote e restaura instalação anterior em caso de interrupção detectada.
- `VoskInstallFeedback` define categorias de erro e mensagens seguras, sem revelar URIs nem detalhes de arquivo. Diagnóstico local recebe código de falha controlado.
- `VoskArchiveInstallerTest` cobre ZIP oficial, pasta extra, raiz, modelo moderno, ZIP inválido/errado/inseguro, limite de expansão e restauração.
- 0.1.180/versionCode 181; `schemaVersion=4` e cinco APKs Release preservados. Gradle/Android real pendente.

# Uai Cortes · versão 0.1.179 — Sessões temporárias e saída confirmada

- `ProjectDraftSession` mantém edições em memória até confirmação. `EditorProjectViewModel` separa `stage`, `commit`, `commitCopy` e `finishWithoutSaving`; novas importações não criam projeto permanente silenciosamente.
- Voltar do editor pergunta Salvar, Salvar cópia (existente), Descartar ou Continuar. Projeto sem mudanças sai sem confirmação.
- `ProjectDraftRecoveryStore` registra sessão em arquivo privado com AtomicFile/sha256 e solicita escolha para recuperação após interrupção; não substitui versão salva modificada.
- Histórico automático somente após confirmação, tela de backup com tamanho/pasta e limpeza explícita de backups órfãos. Versão 0.1.179/versionCode 180, `schemaVersion=4`.
- Sem build Gradle/Android local; validar no CI e dispositivo físico.

# Uai Cortes · versão 0.1.178 — Recuperação de mídias inacessíveis

- Adicionada tela `ProjectMediaRepairScreen` no editor com lista de vídeos, fotos, PiPs, música e vozes inacessíveis, sem registrar URI privada no diagnóstico.
- Seleção por `OpenDocument`: verifica MIME, leitura, duração em relação ao recorte, permissão SAF persistida para vídeo/áudio e cópia privada para foto.
- `ProjectMediaRelink.replace()` troca somente a referência do item por ID e URI original, rejeitando seleções atrasadas; preserva cortes, duração, ordem, legendas, PiP e schema JSON 4.
- Nova rota `media_repair` e testes Kotlin/JUnit de referências e proteção de estado. VersionName 0.1.178 e versionCode 179 sincronizados no Gradle, README, RELEASE_NOTES, sobre/novidades, CI e metadados.
- Testes locais são estruturais/isolados; CI Android e validação física continuam pendentes.

# Uai Cortes · versão 0.1.177 — Correção JUnit do PiP (workflow 147)

- Corrigida a expectativa desatualizada em `OverlayEditorLogicTest.acceptsSmallAndManualPercentages`: após a ampliação do PiP para 150%, **91% é válido**.
- Adicionadas verificações explícitas de 90%, 91%, 100%, 150% e rejeição de 151%, mantendo também a rejeição de 0%.
- Nenhuma alteração no algoritmo de PiP, no exportador Media3, no formato de projetos (`schemaVersion=4`) ou nos arquivos de mídia.
- Workflow 147: compilação Kotlin Debug/Release aprovou; 554 testes unitários foram executados, 1 falhou por contrato antigo; esta correção aguarda nova execução do Gradle em CI.
- Versões do Gradle/CI, workflow, README, RELEASE_NOTES, Sobre/Novidades, metadados e empacotador atualizados para 0.1.177 / versionCode 178.

# Uai Cortes · versão 0.1.176 — Enquadramento livre de fotos no editor

- Adicionado editor visual de foto na faixa principal, com Ajustar/Preencher, zoom, arrasto, centralizar, confirmar/cancelar e seleção preservada.
- Foto mantém quatro novos parâmetros opcionais no JSON (`imageFrameMode`, `imageZoom`, `imagePanX`, `imagePanY`) e é aplicada pela matriz de transformação tanto na prévia como nas sequências Media3 durante a exportação.
- Adicionado cálculo puro de geometria com limites de deslocamento e proteção contra números não finitos. Fotos de projetos legados permanecem em modo Ajustar e zoom 1×.
- Incluídos testes de contrato e JUnit (default legado, preenchimento de aspecto, limites de pan e JSON) e verificação sintática.
- Preservados schemaVersion 4, PiPs, legendas, áudio, transições existentes, cinco APKs Release e integrações de APIs.
- Não há suporte comprovado a crossfade ou virada 3D; compilação e execução no Android ainda são pendentes.

# Uai Cortes · versão 0.1.175 — Editor de tempo, layout e PiP mais flexível

- Corrigido campo de tempo compartilhado: foco não limpa os dígitos; permite seleção e alteração parcial antes de salvar.
- Aumentado limite de escala PiP de 90% para 150%, removida margem obrigatória e mantida proporção; novas opções de enquadramento Ajustar/Preencher e campo `Overlay.fitMode` opcional com default `fit`.
- Prévia e compositor Media3 usam a mesma função de geometria normalizada; colisões entre PiPs continuam exigindo aprovação.
- Ocultadas faixas de timeline sem conteúdo, mantendo o acesso por menu Adicionar e o cursor sincronizado.
- Seis efeitos reais já existentes reorganizados em cartões visuais; os demais preservados em Ver todos, sem reivindicar dissolvência real.
- Acrescentados testes unitários de geometria, bordas, zoom, fallback e persistência, além de teste de contrato visual; schemaVersion=4, cinco APKs Release e integrações existentes mantidas.
- Pendências: fotos da faixa principal em modo livre, dissolvência verdadeiramente sobreposta, compilação Gradle/Android e testes físicos da prévia/exportação.

# Uai Cortes · versão 0.1.174 — Edição gestual direta na timeline

- Encaixe magnético opcional (até 350 ms de um ponto existente), pausa da prévia durante arrasto e alerta quando um recorte descartaria legenda, PiP ou áudio totalmente contido no trecho.
- Adicionadas seleção sem navegação, alças visuais, arrasto horizontal de imagens, vídeos, PiPs, áudio, vozes independentes e legendas, com commit único ao soltar.
- Adicionado zoom 1×/2×/4×/8×, espaço extra após o vídeo para alongar fotos e desfazer/refazer para alterações gestuais.
- `TimelineDirectEdit` centraliza limites: recortes de vídeo e áudio respeitam a fonte e mínimo de 250ms; foto principal permite 1s até 15min; PiP não ultrapassa o projeto e bloqueia novas colisões não aprovadas.
- Campo opcional `Overlay.sourceStartMs` (default 0) persiste o recorte pela esquerda e passa para a prévia e exportação Media3 do vídeo/áudio PiP, preservando schemaVersion 4.
- Ao reordenar clipes principais, elementos totalmente contidos acompanham o clipe, elementos que atravessam bordas mantêm horários e provocam aviso para revisão.
- Mantidos gallery/SAF da 0.1.173, APIs, diagnósticos, funções avançadas, cinco variantes Release. Android/Media3 real permanece pendente.

# Uai Cortes · versão 0.1.173 — Importação de imagens da galeria

- Corrigida a exceção de `image_import` em `StaticImageClip.kt`, identificada no diagnóstico de 05/11/2026 e reproduzida por vídeo: `BitmapFactory.decodeStream` devolve `null` quando `inJustDecodeBounds=true`, sem indicar erro.
- `openInputStream` é verificado separadamente e os limites de pixel/tamanho continuam validados pelas dimensões de `BitmapFactory.Options`.
- Teste Android para JPG/PNG válidos e entrada JPEG corrompida; contrato Python para impedir regresso do operador Elvis sobre `decodeStream`.
- Mantidos schemaVersion=4, 5 APKs, APIs, projetos e todas as mudanças da 0.1.172.

# Uai Cortes · versão 0.1.172 — Linha do tempo unificada, importação e diagnóstico

- Unificadas as pistas do editor com cursor de reprodução comum e faixa visual dedicada a fotos (fotos ainda são clipes principais no render, não overlays extras).
- Atalho Imagem usa DeviceMediaBrowser, o mesmo da seleção de Vídeo e PiP, com alternativa SAF; fotos selecionadas são copiadas para armazenamento privado e lidas via FileProvider.
- Eventos API tipados (serviço/ação/modelo público/HTTP/categoria/latência/consumo informado), sem credenciais nem texto da solicitação; menos eventos de salvamento de projetos no relatório.
- Protegido indicador de progresso de Animar Imagens e ampliada classificação de arquivo ausente/modelo inválido. Correções históricas de Vosk/Media3 permanecem pendentes de confirmação no aparelho.
- Compatibilidade de VideoProject schemaVersion=4, Estúdio, Gemini, redes sociais, backups e cinco APKs mantida.

# Histórico de alterações — Uai Cortes 0.1.171

- **Editor de cortes simplificado:** prévia ocupando o espaço superior, uma régua visual com 12 miniaturas, cursor arrastável e ações grandes Dividir, Aparar, Remover. Ação Pronto retorna ao editor principal; ajuste preciso, mover, excluir clipe e refazer ficam no menu secundário.
- **Recorte visual:** alças roxas escolhem o que manter; alças amarelas escolhem o que remover. A remoção do intervalo pede confirmação, divide a mídia sem regravar o original, remonta a sequência, mantém a transição na borda final e ajusta horários globais de legendas e PiP.
- **Segurança e compatibilidade:** undo/redo de até 15 passos em memória, mínimo de 250 ms por trecho, proibição de apagar o único clipe inteiro e persistência em schemaVersion 4. Áudio vinculado a clipe mantém referências; MP4 final ainda depende de validação real.
- **Regressão:** testes do núcleo e controle visual adicionados. Sintaxe e estrutura verificáveis localmente; CI Gradle e teste no Android são obrigatórios antes de declarar funcionamento aprovado.

# Histórico de alterações — Uai Cortes 0.1.170

- **Correção CI143:** `StaticImageClip.kt` declara `@androidx.annotation.OptIn(UnstableApi::class)` na função `ClipMediaItems.playback`, que chama `MediaItem.Builder.setImageDurationMs`. Corrige especificamente `UnsafeOptInUsageError` encontrado em `:app:lintDebug` do workflow **Gerar APK 143** (0.1.169).
- **Sem mascarar diagnósticos:** lint permanece ativo, sem baseline, supressões globais ou `abortOnError=false`.
- **Compatibilidade:** a função Imagem/Capa 0.1.169, editor, projeto JSON schemaVersion 4, Estúdio, Gemini, Redes Sociais e cinco APKs Release são preservados. Versão 0.1.170, versionCode 171.
- **Validação:** execução local do contrato para opt-in, testes estruturais e sincronização; compilação Gradle/Lint e teste do MP4 no aparelho ainda precisam ser executados na nova versão.

# Histórico de alterações — Uai Cortes 0.1.169

- **Fotos como clipes principais:** no editor, a opção **Imagem** abre o seletor do Android (SAF), valida JPG/PNG/WebP/HEIC/HEIF/BMP, limita arquivo a 24 MiB e imagem a 24 megapixels, permite definir de 1 a 30 segundos e inserir como capa no início, antes/depois do trecho selecionado ou no fim, sem alterar arquivo original.
- **Prévia/edição:** foto na mesma faixa Vídeo dos demais clipes, com rótulo próprio e miniatura; tocar nela abre opções para alterar duração, duplicar, mover ou excluir. Prévia Media3 usa `MediaItem.Builder.setImageDurationMs`; captura de quadro de origem fica desabilitada em foto. Tela Duração usa amostragem de bitmap em IO.
- **Exportação real via Media3 Transformer:** imagem via `EditedMediaItem.Builder.setFrameRate(30)` com duração configurada, mantidos efeitos de vídeo e áudio de fundo. Extrator e reconhecimento de fala ignoram trechos de foto sem áudio, mas preservam a duração no deslocamento das legendas. **Falta testar MP4 e sincronismo em Android real; testes estruturais não substituem build**.
- **Sincronização:** ao inserir/resimensionar uma foto, os elementos com tempo absoluto posteriores (legendas com tempo por palavra, PiPs e áudio extra global) avançam junto com os clipes. Trilhas de áudio vinculadas a ID de clipe seguem o deslocamento automaticamente. Música global mantém início no zero. Elementos cujo intervalo cruza o ponto de inserção podem exigir revisão manual.
- **Compatibilidade:** `Clip.isImage` e `imageMimeType` opcionais no JSON (defaults para vídeo), schemaVersion 4 mantido. Preservar projetos legados, recursos do Estúdio, Gemini, Redes Sociais, cinco APKs Release e versionCode 170. Fotos dependem da permissão de leitura SAF ainda válida; arquivo movido/excluído fora do app precisará ser relocalizado.

# Histórico de alterações — Uai Cortes 0.1.168

- **Meta oficial, sem App Secret:** cliente Facebook Login SDK 19.0.0, autorização pelo Android sob toque, consulta `/me/accounts` e vinculação visual da Página cadastrada. Os IDs são públicos e persistidos; tokens de Página ficam apenas em memória e a sessão do usuário é gerida pelo SDK. A ação "Desconectar Meta" revoga a sessão local do SDK, sem excluir as fichas do Uai Cortes.
- **Publicação com sessão Meta:** autorização separada de `pages_manage_posts` para publicar; se a permissão existir e a Página estiver autorizada na sessão, dispensa colagem do token temporário no diálogo, ainda exigindo confirmação e estado UPLOAD_REVIEW antes do primeiro POST. Modo de token manual preservado como alternativa.
- **Configuração da Meta:** `META_APP_ID` e `META_CLIENT_TOKEN` opcionais no workflow, validados como IDs públicos no Gradle. Sem valores, o botão de login fica desabilitado e a publicação assistida continua disponível. App Secret é proibido no APK. Registre os hashes reais da assinatura Android e solicite revisão de `pages_show_list`, `pages_read_engagement`, `pages_manage_posts` nos produtos Meta. Não declarar produção habilitada sem aprovação.
- **TikTok:** central explica que Login Kit exige domínio verificado com HTTPS, certificado de assinatura e um backend próprio para troca de código por tokens, sem armazenar `client_secret` no cliente. A chave pública `TIKTOK_CLIENT_KEY` é opcional e não realiza login sozinha. Modo avançado de token temporário e status continuam disponíveis; OAuth TikTok integrado ainda pendente.
- **Compatibilidade:** não muda schema social 1 nem VideoProject schema 4, Editor, Gemini, Wizard ou o OAuth YouTube. Mantém 5 APKs Release e versionCode 169. A presença do SDK adiciona dependência a validar no CI; build, login, publish e visual Android real não foram executados localmente.

# Histórico de alterações — Uai Cortes 0.1.167

- **Facebook Página:** vínculo de ID público confirmado pela Graph API v26.0 usando um token OAuth de Página inserido apenas para aquela sessão; START → upload binário → FINISH de Reel, cada envio exige consentimento e a conta deve corresponder à Página vinculada. Recebimento da API não garante processamento/público.
- **TikTok:** consulta oficial `creator_info` antes do envio para mostrar identidade, opções de privacidade e restrições de interação; seleção consciente, marcação opcional de conteúdo criado com IA, upload `FILE_UPLOAD` dividido em blocos, e consulta manual do status via `publish_id`.
- **Segurança:** o token OAuth é inserido no modo avançado, não persistido nem exportado; não há App Secret no APK, nem troca automática de tokens ou reenvio silencioso. A conexão não é login OAuth integrado, e o usuário deve obter token autorizado externamente. `UPLOAD_REVIEW` é gravado antes de qualquer POST; falha incerta exige inspeção da plataforma. `REMOTE_SUBMITTED` NÃO é confirmação de publicação pública.
- **Contas e compatibilidade:** perfil pessoal Facebook e Kwai permanecem manuais. `remotePageId` opcional não altera o schema social 1 nem `VideoProject` schemaVersion 4. Migração de cadastros 0.1.165/166, Gemini, Editor, Wizard e YouTube preservados. Metadados, fluxo de cinco APKs Release e versionCode 168 sincronizados.
- **Configuração externa:** Meta exige permissões aprovadas `pages_manage_posts`, `pages_read_engagement`/`pages_show_list`; TikTok exige `video.publish` e auditoria para visibilidade pública. No modo atual, tokens são fornecidos por ação, NÃO existe autenticação persistente nem OAuth integrado Meta/TikTok. Não publicar sem permissões reais.
- **Limites e testes:** nenhum token real, publicação real ou build Android foi validado nesta sessão; o cliente inclui testes de contrato, parser Kotlin, testes instrumentados pendentes e checagens de integridade do pacote. Upload depende de disponibilidade do serviço, formatos aceitos e rede estável; nunca presumir vídeo publicado.

# Histórico de alterações — Uai Cortes 0.1.166

- **YouTube OAuth:** integração opcional com o AuthorizationClient do Google Play services (escopos `youtube.upload` e `youtube.readonly`). Vincular ID público do canal depois de consulta real e confirmação; nenhum token salvo/exportado.
- **Enviar ao YouTube:** botão por publicação preparada, confirmação individual, privacidade Privado/Não listado/Público (privado por padrão), inicialização de sessão recuperável e envio em blocos de 1 MiB. Sem envio automático de múltiplos destinos nem repetição cega após falha.
- **Status e métricas:** registrar `UPLOAD_REVIEW` antes do POST e `CONFIRMED_BY_API` somente quando API confirmar ID de vídeo. Atualizar métricas sob demanda com origem `youtube_api`, sem misturar valores manuais.
- **Compatibilidade:** campos novos opcionais em contas/publicações/métricas; JSON social ainda schema 1, projetos de vídeo `schemaVersion=4` e biblioteca do Estúdio preservados. Meta, TikTok e Kwai mantêm compartilhamento assistido; sem prometer upload automático.
- **Configuração externa necessária:** habilitar a YouTube Data API v3 e cadastrar OAuth Android com package `br.com.uaicortes` + SHA-1 da assinatura realmente usada; Google Play services no aparelho. Requer consentimento da conta e cotas do projeto Google Cloud.
- **Limites:** uploads acontecem com a tela de publicação ativa. Após interromper a sessão ou erro inconclusivo, o app exige verificação manual no YouTube Studio; nesta versão não persiste URI sensível de retomada. Build Android, OAuth real, compatibilidade de API e upload com conta autorizada ainda não foram executados localmente.

# Histórico de alterações — Uai Cortes 0.1.165

- **Redes Sociais:** nova aba no rodapé da Home, de Projetos e de Ajustes com cadastro local independente de TikTok, YouTube, Kwai, Facebook Página e Facebook Perfil. O cadastro não significa que a conta está autenticada.
- **Após exportação:** opção Publicar nas redes para o exportador tradicional e Animar Imagens. Escolha do MP4 pelo SAF, múltiplos destinos, descrição/hashtags comuns, compartilhamento manual individual e histórico persistente. Abrir o seletor não confirma postagem.
- **Histórico e métricas:** estados preparada/compartilhamento aberto/publicação confirmada manualmente; ligação por vídeo e destino; visualizações, curtidas, comentários e compartilhamentos apenas quando informados. Nunca confundir números manuais com dados oficiais.
- **Central de APIs e Contas:** acesso às chaves Gemini existentes, links oficiais de desenvolvedor e exportação/importação SAF de cadastros sem credenciais, URLs ou tokens. Testes de API e uploads diretos dependem de OAuth/permissões oficiais, ainda não implementados.
- **Segurança:** catálogo JSON privado com AtomicFile, preservação de arquivo corrompido, verificação de URLs HTTPS, sem tocar em VideoProject schemaVersion 4 e sem criar serviços/contas falsas. Nenhum envio de vídeo ou chamada de rede automática.
- **Validação:** verificações estruturais/PSI e instrumentados incluídos. Build Android/execução de OAuth e publicação real permanecem pendentes de ambiente e credenciais autorizadas. Detalhes em `docs/VALIDATION_0.1.165.md`.

# Histórico de alterações — Uai Cortes 0.1.164

- **Wizard em cinco etapas:** novo projeto abre primeiro o estilo visual; depois personagens, cenários, montagem dos clipes e exportação para Flow. Cada tela foca uma tarefa, com navegação e autosave.
- **Sugestões imediatas sem API:** alienígena, pessoa, animal, fruta, robô, objeto animado e cenários. Fichas com nome e aparência em destaque; roupa, voz e objetos opcionais.
- **Biblioteca entre histórias:** personagens, cenários e estilos podem ser salvos e reutilizados; imagem opcional da galeria com cópia privada. Exportação ZIP inclui referências importadas.
- **Gemini opcional:** enriquecer individualmente personagem ou cenário após confirmação, com revisão antes de aplicar; sem chamadas pagas automáticas.
- **Cena = clipe:** editor oferece personagens cadastrados e seleção de quem fala; imagem inicial opcional, prompt preparável sem IA. Compatibilidade dos rascunhos v1 e projetos de vídeo schemaVersion=4.
- **Validação:** testes locais estáticos e de integridade não substituem build Android/uso real das APIs; ver `docs/VALIDATION_0.1.164.md`.

# Histórico de alterações — Uai Cortes 0.1.163

- **Cada cena é um clipe:** o fluxo agora permite criar Cena 1, Cena 2 etc. manualmente, sem história obrigatória; cada uma tem ação, fala, câmera, sons, imagem inicial e prompt próprios.
- **Gemini opcional:** pode sugerir história/cenas, aprimorar um único clipe, gerar imagem vertical ou deixar o usuário preparar localmente o prompt em inglês, sem cobrança.
- **Chave compartilhada:** seleção da credencial em Configurações aplica-se às novas chamadas de IA; Estúdio de texto, imagem e listagem de imagens agora respeitam alternância 429 por projetos distintos quando habilitada.
- **Limites:** não repetir 503, timeout, erros de autorização ou respostas incertas; modelo de outro projeto não é automaticamente trocado. Testes Android e chamadas reais seguem pendentes.

# Uai Cortes · versão 0.1.162 (05/11/2026)
- **Estúdio de Cenas mais simples:** apenas a ideia na entrada, com opções de duração/estilo/gênero recolhidas, resumo legível de título/sinopse/roteiro e campos detalhados sob demanda. "Surpreenda-me" fornece sugestão local imediata e explica que não usa Gemini.
- **Cenas inteligentes:** Gemini solicita falas por personagem e duração estimada por cena; cartões mostram cena, personagens, falas, câmera, sons e ações sem longos formulários. Edição estruturada de falas, menu de ferramentas e um botão principal por vez.
- **Progresso visível:** operação ativa aparece junto a indicador indeterminado e controle de cancelamento Gemini, com aviso sobre eventual consumo incerto. Exportação/importação mostram tarefa em curso.
- **Compatibilidade:** preserva rascunhos anteriores, formatos de vídeo `schemaVersion=4`, editor manual, controle de Gemini, credenciais e SAF. Testes de compilação Android e chamadas reais com chave ainda necessários. Consulte `docs/VALIDATION_0.1.162.md`.

# Uai Cortes · versão 0.1.161 (05/11/2026)
- **Estúdio de Cenas:** etapas História → Cenas → Exportar para Flow, com geração de roteiros/cenas por Gemini somente após consentimento e revisão antes de aplicar. Duração, estilo, gênero, roteiro existente e rascunhos recuperáveis.
- **Cenas e continuidade:** fichas persistentes de personagens e cenários, estado inicial/final, reordenação/duplicação/exclusão, edição por seção, geração de roteiro individual, importação de quadro final, visualização de imagens e associação manual de vídeos.
- **Imagem Gemini:** seleção após consulta `models.list` com a chave ativa, solicitação `generateContent` só para IMAGEM, proporção 9:16, confirmação explícita com custo verificável parcial ou aviso de custo desconhecido; importação, salvamento, regeneração e compartilhamento Android/Flow.
- **Exportação e compatibilidade:** ZIP SAF com imagens por cena, prompts UTF-8, roteiro, fichas e ordem; editor manual de Flow e refinamento em lote existentes permanecem em Avançado. `schemaVersion=4` e projetos legados preservados. Chamadas pagas e integração com Flow não testadas sem chave/autorização. Consultar `docs/VALIDATION_0.1.161.md`.

# Uai Cortes · versão 0.1.160 (05/11/2026)
- **Consumo mais compacto:** resumo de chamadas/tokens/429, custos e resultados por função; gráficos e detalhes técnicos recolhidos. Ajuda contextual `?` e uma linha por modelo utilizado, sem mostrar por padrão três modelos inativos.
- **Configuração unificada:** limites RPM/TPM/RPD e preços USD/1M numa janela por projeto/modelo. Estado do plano pago/gratuito escolhido pelo usuário; `0 USD` exige confirmação explícita de gratuidade daquela operação.
- **Referências oficiais datadas:** botão de preencher sugestão para Gemini 3.8 TTS, 3.5 Transcribe e TTS 2.5 compatíveis; dados consultados em 05/11/2026 na documentação de preços, com vigência e controle de frescor; nunca atualizar valores ou assumir plano via API. Modelos não cadastrados exigem valores manuais.
- **Gráficos corrigidos:** a barra de sucesso representa sucessos/tentativas, não volume relativo. Não altera históricos, chaves AES-GCM, cotas, recuperação, lotes, vídeo nem schemaVersion=4. Validação: `docs/VALIDATION_0.1.160.md`.

# Uai Cortes · versão 0.1.159 (05/11/2026)
- **Prompts Flow + Gemini Batch:** integração opcional dentro da tela Flow (projetos de vídeo e conjuntos locais). Seleciona 2–8 cenas não vazias por envio, modelo de TEXTO compatível, chave/projeto existente, confirmação de possível cobrança e consentimento explícito por envio. Contexto e cena textual são enviados; estilo-base, vídeo, áudio, imagem e credenciais NÃO entram nos prompts.
- **Acompanhamento e revisão:** job `batches/...` fica associado ao identificador local do Flow, números das cenas, modelo, projeto, perfil e SHA-256 do estado original; não persiste texto ou resultados do Gemini. Consulta estado manualmente e oferece revisão individual; só aplica se o resultado é válido e a cena/contexto não mudaram. Preserva original quando houve alteração.
- **Evita duplicidade:** checkpoint REVIEW é salvo antes do POST; jamais reenvia automaticamente após erro ou desconexão. Permite listar jobs do perfil e vincular manualmente um identificador remoto a envio incerto com confirmação. Bloqueia o mesmo conjunto de cenas enquanto há job potencialmente ativo.
- **Parsing REST Batch:** corrige leitura do envelope oficial `output.inlinedResponses.inlinedResponses` e reconhece falhas por item; mantém o uso anterior do Batch genérico. Sem geração TTS, upload de mídia, serviço em segundo plano, estimativa de saldo oficial ou aplicação automática.
- **Validação:** ver `docs/VALIDATION_0.1.159.md`. Preservar schemaVersion=4, 5 APKs Release, histórico e dados anteriores.

# Uai Cortes · versão 0.1.158 (05/11/2026)
- **Gemini Batch TEXTO opcional:** ferramenta expansível na página de Google Gemini. Envia de 2 a 8 pedidos curtos independentes, com escolha explícita de modelo de texto e aceite de possível cobrança do plano pago; nenhum job é criado ao abrir a tela. Só usa uma chave/projeto selecionado, sem failover, retry ou processamento local em segundo plano.
- **Gerenciamento seguro de jobs:** registra ANTES do POST um marcador de envio incerto para evitar duplicações em perda de conexão; após sucesso, guarda `batches/...`, projeto, perfil, modelo, estado e quantidade. Consultas são manuais; permite recuperar um job pelo identificador quando foi criado externamente, consultar estado e selecionar/copiar resultados inline. O registro local pode ser ocultado sem cancelar job remoto.
- **Privacidade:** prompts e respostas não são persistidos, apenas metadados privados de jobs (`gemini_batch_local_v1` fora dos backups). Falta de ID remoto exige revisão; nunca reenvia automaticamente. Texto enviado ao Google somente após confirmação; não altera WAVs, timeline, legendas nem MP4.
- **Limites reais:** API Batch tem cotas separadas, é assíncrona, pode levar até 24 h e a redução anunciada de preço depende do modelo/plano; o Uai Cortes não afirma saldo ou custo oficial. Não é Batch TTS e não sintetiza vozes desta tela.
- **Validação:** ver `docs/VALIDATION_0.1.158.md` para testes executados e pendências. Manter schemaVersion 4, cinco APKs Release e ferramentas anteriores.

# Uai Cortes · versão 0.1.157 (05/11/2026)
- **Consumo Gemini:** painel com gráfico compacto por função e tendência diária (até sete dias visíveis), sucesso/erro e latência média REAL de chamadas registradas por TTS, legendas e descrição/Flow. Nenhum tempo é simulado para operações anteriores.
- **Tarifas manuais opcionais:** campos separados de USD/1 milhão tokens de entrada e saída por projeto/modelo; só somar operações com ambos os valores de uso informados pela API. Não é saldo oficial, fatura nem cálculo garantido de custo para modalidade, cache ou nível diferente.
- **Estimativa antes do lote:** após cinco gerações comparáveis com tokens completos e tarifa informada, exibe custo local aproximado e identifica amostras insuficientes. Nunca altera modelo, chave ou cota da API.
- **Privacidade:** sem guardar prompts, WAVs ou chaves no histórico; preços ficam em preferências privadas, fora do backup do projeto; schemaVersion 4, fluxo de exportação e cinco APKs preservados.
- **Validação:** testes Kotlin/JVM + contratos Python e análise sintática. Compilação Android, preços reais e aparência no dispositivo ainda precisam de CI/teste físico.

# Uai Cortes · versão 0.1.156 (05/11/2026)
- **Transcrição Gemini:** snapshot durável de até 1 MiB por projeto, válido por até 7 dias e somente para a mesma mídia/cortes/áudios. Restaurar gera rascunho para revisão, sem aplicar legendas nem refazer a API.
- **Descrição para publicar:** salvar resultado em cache privado e também tentar persistir no projeto; restaurar sem nova chamada apenas após ação do usuário.
- **Interrupções:** status IN_FLIGHT vira REVIEW após reinício; erro/cancelamento após iniciar chamada não causa repetição automática. Dados de áudio, URI, chave e prompt não entram no manifesto; rascunho recuperável contém somente o texto já produzido.
- **Consumo Gemini:** erros classificados por tipo e contagem de sucesso/falhas por operação. Histórico local não é saldo oficial.
- 0.1.156 / versionCode 157, schemaVersion 4, cinco APKs Release no CI mantidos. Compilação Android e testes físicos ainda pendentes.

# Uai Cortes · versão 0.1.155 (05/11/2026)
- **Vozes IA:** fila local persistente para gerar lotes com confirmação de cota, retomada manual e sem serviço em segundo plano. O registro contém IDs, fingerprints e opções de voz, não a chave nem o texto das legendas.
- Cada pedido registra **em andamento** antes da chamada Gemini e **concluído** somente depois do WAV salvo; ao reabrir, pedidos em andamento sem WAV verificado exigem revisão explícita para não repetir cobranças involuntariamente.
- Ações compactas Retomar, Revisar e Descartar fila. Alterações de legenda invalidam as pendências correspondentes. WAVs e timeline permanecem intactos após descartar o planejamento.
- Rascunhos e fila com limite de 500 falas e até 1 MiB por manifesto para suportar vídeos longos. Sem alteração do schemaVersion=4, credenciais, exportação ou limites da API.
- Regressões Kotlin/JVM e Python incluídas. Android/Compose, reinício real de processo e geração Gemini precisam de validação no GitHub Actions/aparelho.

# Uai Cortes · versão 0.1.154 (05/11/2026)
- **Consumo Gemini:** agora e hoje (horário do Pacífico) aparecem separadamente; RPM, TPM de entrada e RPD mantêm significados próprios. Mostrar tempo aproximado até abrir a próxima janela RPM, quando há dados locais suficientes.
- **Vozes IA:** planeja quantas chamadas novas são necessárias, contando WAVs existentes e válidos apenas quando a configuração da fala coincide. Arquivos concluídos permanecem disponíveis para retomar após interrupções.
- **Ritmo RPM interativo:** respeita o limite manual por projeto e modelo ao gerar lotes, aguardando a janela seguinte com opção de cancelar; limite diário local informado é verificado antes da chamada. Sem limites configurados, mantém operação anterior.
- Não consulta saldo oficial; não soma chaves do mesmo projeto nem repete chamadas ambíguas; não altera exportação, credenciais, legendas, mídias ou schemaVersion 4.
- Testes Kotlin/JVM e contratos locais incluídos; compilação Android/fluxo Gemini real dependem de CI e testes físicos.

# Uai Cortes · versão 0.1.153 (05/11/2026)
## v0.1.153 — Vozes Gemini favoritas e progresso real
- Estrela para adicionar/remover vozes favoritas, com filtro e persistência local entre projetos, sem guardar chaves.
- Nome público amigável da voz geral aparece na barra e fica em cache local para reabertura da tela.
- Porcentagem por falas efetivamente concluídas, com spinner individual; nunca exibir progresso interno fictício da API TTS.
- Mantidos legenda somente leitura, cópia de WAV, sem mudança em schemaVersion 4 ou uso de cota.


## CI128 — Notas de publicação corrigidas
- O workflow 128 concluiu Gradle, testes unitários, Lint, assinatura e verificação de cinco APKs v0.1.151, mas o GitHub devolveu HTTP 422 porque RELEASE_NOTES.md tinha mais de 125.000 caracteres.
- `tools/release_notes_for_github.py` publica somente o resumo da versão atual (máximo 80 KiB UTF-8), preservando as notas históricas em `RELEASE_NOTES.md` e `CHANGELOG.md`.
- Release existente recebe atualização das notas além dos APKs, garantindo reexecução idempotente do workflow.
- Não foram alterados geração Gemini, dados de projetos, quotas, editor, conteúdo da mídia nem assinatura.

# Uai Cortes · versão 0.1.151 (05/11/2026)

## 0.1.151 — Central inteligente de consumo e fallback Gemini
- Métricas por operação (narração, legendas, descrição, Flow), modelo e projeto conhecido; médias de tokens totais e entrada somente com 5 ou mais medições reais, sem estimar tokens ausentes como zero.
- Limites opcionais e manuais por projeto/modelo: RPM, TPM de entrada e RPD. Previsão local conservadora, observando o mínimo das dimensões disponíveis, uso apenas neste aparelho e dia do Pacífico; jamais saldo oficial de créditos/custos.
- Painel Gemini mostra modelos, médias e configura limites em diálogo próprio. Em geração, indica média e projeção quando há dados; Vozes IA considera a quantidade de falas.
- Failover de erro 429 continua opt-in e restrito a outros projetos autorizados distintos; pausa temporária passa a ser por projeto e modelo, com proteção legada para pausas antigas. 401/403/erros de rede ambíguos não disparam novas gerações silenciosas.
- Preferências de limites excluídas do backup assim como chaves e histórico; não guarda prompts, mídia ou tokens de acesso. Sem alterar schemaVersion=4.
- Testes JVM e validação Android/Google API necessários antes de publicar e assegurar fidelidade no aparelho.

# Uai Cortes · versão 0.1.150 (05/11/2026)

## 0.1.150 — Corrigir teste JUnit desatualizado (workflow 126)
- O workflow 126 compilou Kotlin Debug/Release e os testes Android, mas `:app:testDebugUnitTest` falhou com 465/466 testes aprovados.
- O teste `invalidResolutionOrFileNameAreRejected` rejeitava indevidamente 480p, disponível desde 0.1.147; agora recusa apenas valores inválidos (-1, 0, 240 e 2160).
- Novo teste JUnit garante aceitação explícita de 360p, 480p, 720p e 1080p. Teste Python cruza as listas da UI, do coordenador e do JUnit.
- Versão, workflow, About/Novidades, release e metadados sincronizados; não houve alteração no motor de exportação ou esquema do projeto.
- Build Android completo, lint, cinco APKs e testes no aparelho ainda pendentes de nova execução CI.

# Uai Cortes · versão 0.1.149 (05/11/2026)

## 0.1.149 — Corrigir compilação após redesenho Gemini (CI125)
- Workflow 125 falhou em `:app:compileDebugKotlin` e `:app:compileReleaseKotlin`: referência `Icons.Default.LockOutline` indisponível em `GeminiSettingsUi.kt:130`.
- Usado o ícone Material `Icons.Default.Lock`, conservando o painel e o sentido de credencial protegida.
- Adicionado teste de regressão para o ícone incompatível e mantidos testes das telas Gemini, confirmação de operações sensíveis e proteção de segredos.
- Versionamento sincronizado para `0.1.149`/`150`; compilação Android e cinco APKs seguem pendentes de execução no CI.

# Uai Cortes · versão 0.1.148 (05/11/2026)

## 0.1.148 — Novo visual de Gemini e gerenciamento de credenciais
- Configuração Gemini separa status de conexão, campo da chave e ferramentas em superfícies compactas com ícones. Preserva a barra única de 6 ações com área de toque 56dp e rolagem em largura insuficiente; confirmar ao copiar, exportar, remover e testar TTS.
- Gerenciador de chaves usa cartões menores com selo ATIVA, máscara e ID de projeto, ações Usar/Editar/Excluir e cadastro/edição em Dialog próprio com rolagem, sem exibir chave persistida. Mantém máximo de 8, exigência de projetos distintos na alternância 429, confirmação e teste sem geração.
- Histórico de Consumo da IA com cabeçalho destacado, cartões de métricas e últimas operações com estados visuais. Contagens continuam limitadas a dados locais da API; valores sem medição não viram zero, custos/créditos permanecem indisponíveis.
- Nenhuma modificação em GeminiCredentialStore, consumo da API, modelos, engine, exportação Media3 ou schemaVersion do projeto. Necessário build e teste visual Android para confirmar ergonomia.

# Uai Cortes · versão 0.1.147 (04/11/2026)

## 0.1.147 — Exportação de vídeo e publicação mais organizadas
- Destino com ícones ampliados e resolução em quatro opções (360p, 480p, 720p, 1080p); perfis H.264 independentes e dimensões verticais pares para reduzir falhas de codificação. O encoder pode ajustar a saída real, que segue conferida no relatório de qualidade.
- Indicador mantém percentual fornecido pelo Transformer/Media3, com aviso de progresso não linear. Fases de preparação e gravação na galeria permanecem indeterminadas, sem inventar porcentagens nem conclusão antecipada.
- Tela final de vídeo salvo mostra capa, nome, dimensões e três ações: Gerar descrição, Gerar com IA e Compartilhar. Entradas manuais, 5 hashtags, copiar, modelo Gemini, aviso de dados enviados e confirmação de custo/cota migrados para popup contextual rolável.
- Mantidos rascunhos, consentimento explícito, compartilhamento opcional de texto válido, abrir vídeo, voltar ao editor, detalhes técnicos e compatibilidade do MP4. Não há upload de áudio/vídeo à Gemini.

# Uai Cortes · versão 0.1.146 (04/11/2026)

## 0.1.146 — Configuração Gemini compacta
- Atalhos Colar, Copiar, Importar, Exportar, Obter e Remover numa barra única de seis itens, com rótulos e ícones acessíveis. Para larguras muito pequenas, habilita rolagem lateral sem cortar botões.
- Campo protegido permanece vazio após salvar; copiar/exportar/remover continuam protegidos por confirmação e mantêm a semântica anterior. Testar TTS e Salvar/Atualizar permanecem separados.
- Privacidade, chaves extras e consumo organizados em cartões com cantos discretos; 24h/7d/30d e métricas reais com possibilidade de expandir modelos e últimas operações.
- Consumo continua local, e 429 não significa automaticamente saldo esgotado. Sem mudanças no esquema dos projetos, nas APIs ou no processamento de áudio.

# Uai Cortes · versão 0.1.145 (04/11/2026)

## 0.1.145 — Seletor de vozes Gemini redesenhado
- Diálogo quase na largura total do dispositivo, com dimensões limitadas também para tablets e altura útil para a lista.
- Filtros distribuídos igualmente em linhas fixas; sem rolagem horizontal ou opção Agudas cortada. Filtro N/D = altura não informada pela API.
- Busca com limpar, contagem e limpar filtros, listagem com cartões compactos e seleção atual marcada. Ordenação numérica natural de vozes enumeradas (2 antes de 10).
- Sem mudanças em geração, áudio, credenciais, legendas, exportação e contratos do projeto.


- **Vozes IA:** assistente verifica fim da legenda, próxima fala e fim do projeto; oferece derivação offline em WAV (até 1,25×, com pré-escuta recomendada), ou regeneração Gemini com orientação aproximada, sem cortes silenciosos nem edição automática das legendas.
- **Catálogo e UI:** pesquisa e filtros combináveis por gênero e altura declarados, nomes amigáveis dos IDs conhecidos, ações compactas; dados originais e WAV anterior preservados.
- **Compatibilidade:** rascunho antigo continua legível, sem mudança no JSON principal/schemaVersion 4; uso de API exige confirmação e a exportação só utiliza WAV ajustado após Inserir/Trocar.

# Uai Cortes · versão 0.1.143 (04/11/2026)

## Vozes IA: barra única de seis ações e seletores organizados

- Ações **Buscar**, **Lite/Flash**, **Voz**, **Humor**, **Gerar N** e **Inserir N** reunidas em uma linha; botões retangulares compactos (40 dp, raio 8 dp), com descrição acessível completa e sem perder os contadores.
- Modelo Lite/Flash agora usa o mesmo estilo de botão; controles dos cartões individuais também ganharam cantos discretos, preservando textos, informações técnicas, prévia e timeline.
- Diálogos de vozes e estilos ordenados alfabeticamente em português; lista interna rolável limitada a 310/320 dp, seleção atual marcada e título compacto. Todas as opções originais permanecem disponíveis.
- Rótulos curtos aparecem somente na interface. Nenhum texto, estilo Gemini, áudio, chave ou duração do projeto é alterado por essa revisão.
- Testes locais e validação estrutural não substituem compilação Gradle nem teste no celular.

# Uai Cortes · versão 0.1.142 (04/11/2026)

## Vozes IA: interface compacta e escolhas gerais

- **Buscar vozes**, seletor de modelo **Lite/Flash** e **Voz geral** reorganizados na mesma barra da ferramenta Vozes IA. Escolher uma voz geral pede confirmação antes de substituir as escolhas individuais e não aciona a API.
- Botão **Humor** junto a **Gerar todas**, com os estilos existentes do Gemini (Humorística, Sarcástica, Neutra etc.) aplicáveis em lote mediante confirmação. Cada legenda continua permitindo escolha individual de voz e estilo.
- Removido **Voz para todas** da linha de geração. Cartões menores, texto recolhido em uma linha, seletores compactos e ações **Gerar/Refazer**, **Ouvir** e **Inserir/Substituir** preservadas.
- Prévia da tela Vozes IA agora reutiliza `InlineVideoPreview`, a geometria adaptativa da tela Legendas, sem editar posição, texto, tamanho ou tempos salvos. Horários em décimos (`00:00.5`) e sinalização em décimos para WAV que ultrapassa a janela original da legenda.
- Novo `VoiceIaDisplayPolicy` com testes JVM para ultrapassagens e arredondamento visual; nova regressão estática do layout e das decisões de confirmação. Áudio tradicional, timeline Vozes, Gemini TTS, schemaVersion 4, Media3 e cinco saídas Release preservados.
- A compilação Android, o fluxo Gemini real e a reprodução/exportação precisam ser confirmados no GitHub Actions e em aparelho. Não há APK gerado nesta entrega de fonte.

# Uai Cortes · versão 0.1.141 (04/11/2026)

## Correção CI118 — teste unitário de Vozes IA

- O GitHub Actions 118 compilou Kotlin Debug/Release e executou 456 testes unitários, mas a etapa `:app:testDebugUnitTest` falhou em um teste: `VoiceIaPolicyTest.insertAndReplaceKeepOnlyOneVoiceWithoutTouchingMusic`, na chamada de `VoiceIaPolicy.merge` (linha 24).
- A geração da URI do WAV usava `android.net.Uri.fromFile()` dentro do código chamado pelo teste JVM. A API do Android não é implementada no mockable `android.jar` usado por testes locais; o método foi substituído por `java.net.URI` padrão, preservando URIs `file:///...` e percent-encoding de espaços e caracteres acentuados.
- Regressão JUnit adicionada para URI de arquivo local sem Android, incluindo nomes com espaços e acentos; testes existentes continuam verificando inserção/substituição de voz e preservação de faixas tradicionais.
- Sem alterações no contrato da Gemini, chave, legendas, motor de exportação ou esquema de projetos. Código-fonte v0.1.141 com `versionCode=142` e workflow sincronizado.
- A execução integral de Gradle, lint, publicação dos cinco APKs e testes reais de reprodução/exportação continuam pendentes do GitHub Actions.

# Uai Cortes · versão 0.1.140 (04/11/2026)

## Correção do workflow 117 — inclusão do GitHub Actions no ZIP

- A entrega do código-fonte v0.1.139 foi empacotada sem o diretório oculto `.github`, deixando o workflow antigo (`VERSION=0.1.138`) no repositório após a atualização. O teste `check_version_sync.py` impediu a compilação ainda na validação inicial.
- O pacote desta versão inclui explicitamente `.github/workflows/android-apk.yml`, agora com `VERSION=0.1.140` e `VERSION_CODE=141`, em sincronia com Gradle, `VERSION`, metadados, Sobre/Novidades e empacotador de APKs.
- O ZIP é criado por `python3 tools/package_source.py --output <arquivo.zip>`, que verifica arquivos indispensáveis, executa `validate_source.py` sobre conteúdo extraído e rejeita ZIPs incompletos. Novo contrato protege a presença do workflow nesse empacotamento.
- Sem alterações no código funcional de Vozes IA, áudio, legendas ou exportação. A compilação Android, os cinco APKs Release e testes em aparelho ainda dependem do próximo GitHub Actions.

# Uai Cortes · versão 0.1.139 (04/11/2026)

## Vozes IA por legenda e trilha de voz separada

- Atalho **Vozes IA** no carrossel do editor, entre Áudio e Transição; tela própria com prévia do vídeo e legendas carregadas automaticamente do projeto, somente leitura.
- Consulta manual do catálogo de vozes Gemini, escolha individual por legenda, estilos de interpretação, modelo, aplicação da mesma voz a todas as falas, geração e escuta de WAV antes da inserção.
- Botões **Gerar**, **Refazer**, **Gerar todas**, **Ouvir**, **Inserir/Substituir** e **Excluir faixa**. Confirmação explícita antes de chamadas com possível consumo de cota; lotes internos de até 12 e cancelamento entre chamadas.
- Rascunhos dos WAVs gravados em armazenamento interno durável, com SHA-256 e impressão digital da legenda, para reabrir sem regenerar falas idênticas. Sem transcrever, enviar mídia nem editar legendas originais.
- **Vozes** aparece como faixa própria no editor com gesto para mover e toque para reabrir Vozes IA. Todas as faixas continuam em `audioTracks` para manter a prévia, a mixagem e o exportador anteriores; metadados opcionais distinguem vozes de faixas tradicionais mantendo JSON `schemaVersion=4`.
- Inserção só ocorre após confirmação, sem recortar WAV; alerta quando áudio ultrapassa a duração da legenda ou sobrepõe outra voz; bloqueia inserção fora do projeto.
- Tela Áudio tradicional continua disponível para música, efeitos, extração e narração contínua/avançada. Recurso de imagens na timeline não foi incluído nesta versão.
- O CI/Gradle e a reprodução/exportação reais no Android ainda precisam ser validados. Sem APK produzido neste ambiente.

# Uai Cortes · versão 0.1.138 (04/11/2026)

## Correção CI115 — teste de karaokê com versionamento dinâmico

- O workflow 115 falhou antes do Gradle em `tools/test_subtitle_karaoke_136_contract.py`: o teste da versão 0.1.136 ainda exigia exatamente `version=='0.1.136' and code==137` quando a versão do aplicativo já era 0.1.137/138.
- O contrato histórico continua verificando os efeitos de karaokê, a prévia e a exportação, mas agora exige somente a versão mínima em que o recurso nasceu (`>= 0.1.136`, `versionCode >= 137`) e chama `check_version_sync.check()` para confirmar sincronização da versão atual.
- Novo contrato `tools/test_ci115_karaoke_version_contract.py` detecta essa regressão e bloqueia outros contratos históricos do mesmo formato com versão fixa.
- Sem alteração no renderizador, no editor de legendas, no Media3, no formato JSON (schemaVersion 4), nas credenciais ou na assinatura. A geração dos cinco APKs ainda requer CI Android.

# Uai Cortes · versão 0.1.137 (04/11/2026)

## Legendas · barra mais compacta e menos repetição

- O comando **Desfazer** saiu da área entre os blocos de ação e a lista de legendas. Agora fica na mesma linha de **Salvar**, **Apagar todas** e **Buscar**, usando ícone de retorno para liberar espaço vertical.
- A informação de tempo selecionado foi reduzida para um único resumo acima da timeline: **Legenda: início – fim**. O tempo duplicado ao lado dos controles do player foi removido.
- A faixa **Linha do tempo** ficou mais próxima da prévia do vídeo, com cabeçalho mais compacto e ajuda menor, melhorando a leitura da tela.
- O comportamento de desfazer continua cobrindo remoção total de legendas e a última edição em lote, sem alterar projetos antigos, posições, efeitos, Media3 ou o formato JSON atual.
- Novo contrato `tools/test_subtitle_actions_137_contract.py` verifica a presença do atalho Desfazer na barra principal e previne o retorno do tempo duplicado na barra do player. Build Android completo e teste manual da ergonomia ainda dependem do próximo GitHub Actions.

# Uai Cortes · versão 0.1.136 (04/11/2026)

## Novos destaques por palavra nas legendas

- Três opções na categoria Texto: **Pulso de cor**, **Palavra forte** e **Sublinhar palavra**. São variantes do karaokê existente, compatíveis com a sincronização manual da v0.1.134 e horários reconhecidos previamente.
- O pulso é uma **animação de cor quantizada**, sem mudar tamanho ou posição dos caracteres. Palavra forte usa negrito e Sublinhar palavra usa sublinhado apenas na palavra ativa. Três novos modelos prontos somam **25 estilos e 22 animações**.
- `SubtitleAnimations.Frame` descreve o destaque em um único lugar. Prévia Compose, miniaturas de estilos e MP4 Media3 aplicam os mesmos intervalos, cores e opções de negrito/sublinhado; a chave do bitmap inclui a fase do pulso e os atributos do `SubtitleCue` para evitar imagem desatualizada.
- Sem horários individuais, o efeito distribui o tempo por palavra apenas como estimativa e **não altera** o projeto automaticamente. `schemaVersion=4`, IDs, textos, áudio, posição e horários já salvos permanecem intactos.
- Novos testes JVM `SubtitleKaraokeAccentsTest` e contrato Python `test_subtitle_karaoke_136_contract.py`; testes locais não substituem build Android, comparação visual de prévia/MP4 e sincronização real com áudio.

# Uai Cortes · versão 0.1.135 (04/11/2026)

## Correção do workflow 113 — testes de presets de legendas

- O teste `SubtitleStylesTest.presetOnlyChangesAppearance` usava `presets.last()`, que antes era o modelo **Etiqueta**. Após a inclusão de 10 presets na v0.1.133, o último passou a ser **Ritmo rosa**, mas as expectativas de cores do teste permaneceram as de Etiqueta.
- O teste agora localiza **Etiqueta** pelo nome e verifica suas propriedades sem depender da ordem da galeria.
- Nova regressão JVM percorre todos os 22 modelos em ordem inversa e verifica que aplicar aparência não altera ID, texto, horários, posição ou tamanho. Novo contrato `tools/test_ci113_subtitle_preset_contract.py` também detecta testes dependentes da ordem.
- **Sem alteração no código de produção:** os 22 estilos, 19 animações, sincronização por palavra, formato JSON v4 e exportador Media3 permanecem intactos.
- No workflow 113, `compileDebugKotlin`, `compileReleaseKotlin` e `compileDebugAndroidTestKotlin` passaram; `:app:testDebugUnitTest` falhou em 1 de 448 testes e interrompeu o build antes de gerar os cinco APKs. A compilação e execução completa da v0.1.135 requer novo GitHub Actions.

## Versão 0.1.134 · versionCode 135 (04/11/2026)
- Adicionado ajuste manual das fronteiras das palavras, com distribuição opcional e marcadamente estimada, seleção e passos de 100 ms.
- Corrigida prévia do karaokê: escalonamento dos horários absolutos ao mostrar demonstração de três segundos, sem inventar reconhecimento de áudio.
- Adicionados testes Kotlin e contrato CI; versionamento e documentação sincronizados, JSON schema 4 e Media3 preservados. Build Android e conferência MP4 ainda pendentes.

## Versão 0.1.133 · versionCode 134 (04/11/2026)
- Corrigida cópia em lote de cores por palavra usando correspondência por palavra e ocorrência; opção desligada por padrão. Grupos e desfazer agora comunicam o conteúdo alterado.
- Corrigido texto fictício da prévia em quadros vazios; pausar/reiniciar/demo versus duração de cada legenda.
- Nove animações e dez modelos de aparência extras, categorizados, com deslocamento horizontal calculado pelo mesmo `SubtitleAnimations.frame` da prévia e do overlay Media3.
- Adicionados testes de regressão Kotlin e estáticos; compilação Android e teste em aparelho ainda pendentes.
- Versão/CI/metadados sincronizados; schema JSON 4, assinatura e cinco APKs Release previstos preservados.

## Versão 0.1.132 · versionCode 133 (04/11/2026)

- **Correção workflow 110:** adicionado o import `androidx.compose.foundation.horizontalScroll` em `GeminiUsageUi.kt`, resolvendo `Unresolved reference horizontalScroll` nas tarefas Debug/Release.
- **Correção Kotlin:** alterado o retorno de `countInput` em `GeminiVideoCaptionClient.kt` para `return@withContext`, evitando `return is prohibited here` na consulta opcional `models.countTokens`.
- **Regressão preventiva:** `tools/test_ci110_gemini_compilation_contract.py` verifica essas duas construções e sua preservação nas próximas versões; executado automaticamente pelo loop de contratos do CI.
- **Preservação:** sem mudanças na contagem de tokens, gerenciamento de chaves, alternância de projetos, histórico Gemini, exportação, `schemaVersion=4`, Media3 1.11.1 ou assinatura dos cinco APKs.
- **Validação:** contratos locais e verificações Kotlin executados; build Android completo da versão corrigida precisa ser repetido no GitHub Actions.

## Versão 0.1.131 · versionCode 132 (04/11/2026)

- **Medição real:** descrição e Flow (`usageMetadata` de `generateContent`), narração e transcrição (`usage` de `interactions`) agora registram, quando informados, entrada, saída, raciocínio, cache e total oficial; total nunca é somado novamente aos subtotais.
- **Painel Gemini:** Configurações > Google Gemini > Consumo da IA, com 24 horas / 7 dias / 30 dias, histórico por projeto informado e modelo, registros sem medição, 429 e limpeza confirmada. Até 500 registros dos últimos 90 dias, somente neste aparelho.
- **Previsão acessível:** avisos breves antes de gerar descrições, Flow, TTS e legendas; para texto, faixa de estimativa local; para áudio, duração informativa, sem inventar tokens ou custos. Após geração, total real apresentado quando fornecido.
- **Contagem mais precisa opcional:** botões `Conferir tokens de entrada` em descrição/Flow usam `models.countTokens` somente sob ação expressa; não gera resposta, não prevê saída e não alterna projetos automaticamente. Erro de consulta não bloqueia geração.
- **Limitações e privacidade:** sem saldo de créditos ou custo estimado sem dados oficiais do plano; API 429 indica limite e não prova saldo esgotado. Histórico não armazena conteúdo, chave, URL, WAV ou resposta bruta. Continua com Media3 1.11.1 e schemaVersion 4.
- **Validação:** testes unitários e contratos adicionados. Não considerar build Android nem uso da API real aprovados até execução no CI e aparelho com chave autorizada.

## Versão 0.1.130 · versionCode 131 (04/11/2026)

- **Múltiplas chaves Gemini:** até oito credenciais AES-GCM/Android Keystore, cada uma com nome, identificador do projeto Google Cloud, seleção de ativa, edição e exclusão confirmada.
- **Alternância opcional:** após erro HTTP 429 RESOURCE_EXHAUSTED, pausa o projeto por dez minutos e tenta, no máximo uma vez por outro projeto identificado e não pausado, sem retentar erro de cobrança, autenticação ou indisponibilidade. Exige consentimento explícito.
- **APIs conectadas:** narração TTS, transcrição de legendas, geração de descrições e refinamento de cenas Flow utilizam o mesmo gerenciador; a operação autorizada pelo usuário não roda em segundo plano.
- **Limites e segurança:** as cotas são por projeto Google Cloud e não por chave; cadastrar duas chaves do mesmo projeto NÃO duplica cotas. O app não cria contas nem projetos e não garante créditos. IDs são informados pelo usuário e não são comprovados automaticamente; usar apenas projetos autorizados, respeitando termos e orçamento. O backup do Android exclui credenciais.
- **Compatibilidade:** chave única anterior preservada; nenhuma modificação nos vídeos, projetos ou sistema Media3. Faltam Gradle/Android CI e teste com projetos reais distintos antes de declarar a alternância operacional.


- **Home compacta:** três cartões azuis uniformes lado a lado com ícones; atalhos renomeados para Novo projeto, Animar fotos e Prompts Flow. Os mesmos callbacks anteriores são mantidos.
- **Projetos recentes:** cabeçalho mais leve, cartão para lista vazia e Ver todos quando houver projetos. Sem alterar persistência nem excluir dados.
- **Prévia de Sobre:** continua compartilhando `HomePrimaryActions` com `preview=true`, impedindo ações na demonstração. Testes instrumentados da Home e novo contrato Python atualizados.
- **Compatibilidade:** versionCode 130, schemaVersion 4, Media3 1.11.1, workflow de cinco APKs e assinatura preservados. Validação estática não substitui build Android nem inspeção visual em aparelhos de tela estreita.

## Versão 0.1.128 · versionCode 129 (04/11/2026)

- **Exportação repaginada:** ícones, grupos de destino/enquadramento/resolução/compressão compactos, botão Salvar fixo e estimativa do tamanho do projeto no rodapé.
- **Ajuda visível:** exemplos desenhados para Mostrar tudo e Preencher, aviso de corte central, guias para legendas e regiões ilustrativas ocupadas por interfaces; guias não exportadas.
- **Prévia opcional com mídia real:** painel expansível usa PlayerView FIT/ZOOM já existente; elementos da composição são aproximados e exigem verificação do MP4 final.
- **Sem mudança do exportador:** preserva Media3 1.11.1, Original padrão, 9:16, cinco APKs Release e schemaVersion 4.
- **Validação:** testes estáticos e de contrato locais; build e comparação visual em dispositivo pendentes.

## Versão 0.1.127 · versionCode 128 (04/11/2026)

- **Correção CI105:** `FlowPromptDraft` deixa de ser `internal` para compatibilidade com a propriedade pública `VideoProject.flowPromptDraft`; o erro do compilador Kotlin `public function exposes its internal parameter type` foi registrado nos logs Debug e Release.
- **Teste preventivo:** `tools/test_ci105_flow_visibility_contract.py` verifica a assinatura pública e a persistência JSON. A execução automática já ocorre no laço de `tools/test_*.py` do GitHub Actions.
- **Sem alterações no editor/exportador:** preservados marca d'água, prompts Flow, media3 1.11.1, schemaVersion 4, assinatura e cinco saídas Release por ABI.
- **Validação:** verificações de código/contratos locais; compilação Android real da versão corrigida e testes físicos permanecem pendentes.

## Versão 0.1.126 · versionCode 127 (04/11/2026)

- **Marca d'água reformulada:** arrastar texto na prévia, nove posições rápidas, botões +/- para tamanho e opacidade (0–100%), guias opcionais, cores, três fontes locais, contorno e sombra. Remover exige confirmação. A posição e aparência seguem o mesmo `SubtitleCue` da prévia/exportação.
- **Modelo de marca reutilizável:** salvar/carregar configurações em preferências locais privadas; nenhum projeto antigo recebe marca automaticamente. Campos opcionais permanecem compatíveis com JSON `schemaVersion=4`.
- **Prompts Flow:** acesso pela tela inicial sem vídeo importado (até 30 conjuntos locais independentes) e pelo editor de vídeo. Estilo-base 3D fixo editável por aparelho, contexto comum e até 12 cenas separadas por conjunto, com modo texto→vídeo ou transformação de referência. Cada cena tem prompt próprio para copiar; configuração 9:16 é feita no Google Flow.
- **Gemini opcional:** apenas após confirmação envia descrição e contexto textual da cena usando a chave local existente; não envia MP4, frames, áudio, URI ou prompt-base. Mostra sugestão para revisão antes de substituir o texto. Não repete chamadas automaticamente.
- **Compatibilidade:** `VideoProject.flowPromptDraft` opcional em projetos e backups JSON, schema 4 preservado. Conjuntos avulsos de Flow e estilos-base permanecem nas preferências do aparelho e não integram backup de vídeos. Media3 1.11.1, 5 APKs Release e demais ferramentas mantidos.
- **Validação:** testes de contrato/estrutura e sintaxe do código; ainda pendentes build Android real, teste visual da marca d'água e chamadas Gemini com chave válida.

## Versão 0.1.125 · versionCode 126 (04/11/2026)

- **Etapa 7 — modelos pessoais de legendas:** a galeria preexistente de 12 estilos foi mantida. Agora é possível salvar uma combinação personalizada de fonte, tamanho, cor, fundo, contorno, sombra, alinhamento, espaçamento, destaque e animação, reutilizável em outros projetos.
- **Biblioteca local:** até 20 modelos com nome único (até 32 caracteres), renomear e excluir com confirmação. Os estilos são armazenados em SharedPreferences privadas com gravação verificada; erros não apagam automaticamente a biblioteca anterior.
- **Aplicação segura:** escolher um modelo altera apenas o rascunho visual; Salvar legenda confirma alterações individuais. O fluxo já existente Editar todas continua aplicando estilo/animação em lote com confirmação e desfazer. Modelos não armazenam nem substituem texto, IDs, tempos, posição ou cores específicas de palavras.
- **Compatibilidade:** não modifica JSON/backup dos projetos (`schemaVersion=4`), Media3 1.11.1, exportador, assinatura, cinco APKs e seleção de redes sociais. A biblioteca é local ao dispositivo, não incluída nos backups de projetos.
- **Testes:** `SubtitleUserPresetTest.kt` e `tools/test_subtitle_user_presets_contract.py` cobrem isolamento de texto, serialização, nomes duplicados e aplicação. O Gradle/Android e a prévia/exportação visual real ainda precisam ser validados em aparelho.

## Versão 0.1.124 · versionCode 125 (04/11/2026)

- **Etapa 6 — perfis sociais opcionais:** Exportar oferece Original, Shorts, Reels e TikTok. Original permanece selecionado por padrão e não modifica o fluxo antigo. Os perfis sociais solicitam resolução fixa 9:16 (720×1280 ou 1080×1920) via `Presentation.createForWidthAndHeight`, preservando MP4 H.264/AAC e o ajuste manual de bitrate.
- **Enquadramento consciente:** Mostrar tudo usa `LAYOUT_SCALE_TO_FIT` (pode gerar barras); Preencher tela usa `LAYOUT_SCALE_TO_FIT_WITH_CROP` (corte central, pode perder conteúdo lateral/superior/inferior). Não distorce a imagem nem realiza rastreamento automático de pessoas. A prévia usa FIT/ZOOM e exibe guias ilustrativas opcionais, não incorporadas ao vídeo.
- **Compressão:** ao escolher perfil social, sugere a taxa Compacta já existente (5 Mb/s em 720p, 10 Mb/s em 1080p); o usuário pode selecionar Alta/Máxima. São parâmetros solicitados ao encoder, não requisitos garantidos de cada rede.
- **Diagnóstico:** `ExportQualityReport` compara largura/altura exatas solicitadas para perfis verticais e mostra a rede selecionada. Preservados PiPs, legendas, marca d'água, áudio, backup, schemaVersion 4 e a exportação sem rede.
- **Testes:** novas regras puras `SocialExportPolicyTest`, contrato `tools/test_social_export_contract.py` e cenários instrumentados de FIT/CROP com fonte 16:9, ainda não executados em Android. Compilação Gradle e fidelidade visual/áudio em aparelho permanecem pendentes.

## Versão 0.1.123 · versionCode 124 (04/11/2026)

- **Etapa 5 — descrição persistente por projeto:** novo `VideoProject.publicationDraft` armazena somente textos, inclusive rascunhos incompletos, em JSON local; exportação/importação do backup preserva automaticamente o campo. Sem migrar schemaVersion 4; projetos antigos leem campo ausente como `null`.
- **Tela "Vídeo salvo":** recupera rascunho anterior, salva alterações automaticamente após 650 ms de inatividade, e sincroniza ao fechar/voltar ao editor. Falhas de armazenamento são apresentadas e impedem fechar com alterações não salvas.
- **Editor → Descrição:** tela independente para revisar, editar, copiar e salvar sem reexportação e sem enviar chamadas Gemini. Cópia continua restrita a descrição + exatamente cinco hashtags distintas, dentro de 100 caracteres, sem emojis.
- **Animar imagens:** ao escolher "Editar com áudio, legendas e PiP", o rascunho digitado é transferido para o projeto criado/atualizado. Ao apenas fechar a exportação avulsa não há projeto ao qual associar o texto.
- Criados `ProjectPublicationDraftTest.kt` e `tools/test_publication_draft_contract.py` para serialização, backups, compatibilidade legada e ligação de UI. Build completo Android e teste no aparelho ainda pendentes.
- Media3 1.11.1, Gemini, arquivos de vídeo, cinco variantes Release e assinatura não foram modificados.

## Versão 0.1.122 · versionCode 123 (04/11/2026)

- **Etapa 4 — descrições com Gemini:** consulta voluntária de modelos via `GET /v1beta/models`, filtrando `supportedGenerationMethods=generateContent` e removendo famílias TTS/live/imagem/embedding. Listagem depende da chave configurada e não realiza geração de conteúdo nem expõe contexto; não garante suporte a JSON até a primeira geração.
- **Modelo selecionável:** campo na tela Vídeo salvo, atualização manual da lista, escolha persistida apenas em preferências privadas locais. Em instalação antiga mantém `gemini-2.5-flash` até o usuário consultar e selecionar outro; nenhuma escolha é imposta sem consultar a chave.
- **JSON estruturado:** `generationConfig.responseJsonSchema` para frase e exatamente cinco hashtags; mantém validação local rígida de 100 caracteres, cinco hashtags distintas e proibição de emojis/emoticons. Respostas inválidas são rejeitadas, sem tentar chamadas adicionais automaticamente.
- **Erros e privacidade:** respostas de rede com tamanho limitado, mensagem contextual por HTTP (401/403, 404, 429, 5xx) sem vazar corpos completos, token, prompts ou chave. Consentimento permanece antes de enviar texto; nunca enviar MP4, quadros, URI ou áudio.
- Incluídos testes `GeminiCaptionModelPolicyTest.kt` e `tools/test_gemini_caption_models_contract.py`. Build Gradle completo, teste com chave real, rede e API Gemini em aparelho **pendentes**; nenhuma cota foi consumida nesta implementação.
- Preservados Media3 1.11.1, schemaVersion 4, cinco APKs Release e assinatura.

## Versão 0.1.121 · versionCode 122 (03/11/2026)

- **Correção do workflow 100:** removido o import top-level `androidx.compose.ui.test.assertExists` em `HomeAboutPreviewAndroidTest.kt`. O método `.assertExists()` permanece nas interações semânticas do Compose; não altera os testes nem a UI.
- Adicionada proteção `tools/test_ci100_compose_import_contract.py`, executada pelo loop de regressão do CI, para impedir a reintrodução desse import inválido nos testes Android.
- Compilação do código principal, os demais avisos de ícones obsoletos e o Media3 1.11.1 não causaram a falha mostrada nos logs. As cinco variantes de Release e a assinatura continuam configuradas, mas ainda exigem um build completo bem-sucedido.
- Sem modificações no exportador, projetos, vídeos, IA, permissões ou `schemaVersion=4`.

## Versão 0.1.120 · versionCode 121 (03/11/2026)

- **Etapa 3 — Media3 estável:** migrados juntos `media3-exoplayer`, `media3-ui`, `media3-transformer`, `media3-effect` e `media3-common` de 1.10.1 para 1.11.1, sem dependências alpha. Mantidos compileSdk/targetSdk 36 e Kotlin/AGP atuais.
- **Medição da velocidade:** tempo real de processamento e tempo de cópia MediaStore medidos separadamente via `SystemClock.elapsedRealtime`. Fator estimado (duração do vídeo / tempo de processamento) aparece apenas quando ambas as medidas são positivas. Não inventar ganho de velocidade.
- **Cópia local mais econômica em chamadas:** `GalleryVideoSaver` usa blocos de 256 KiB em vez de 8 KiB por operação; mesma verificação de bytes, pendência MediaStore e rollback.
- **Cortes rápidos:** não habilitada a otimização experimental indiscriminadamente. O fluxo aplica redimensionamento, bitrate escolhido, áudio, legendas e PiP, frequentemente incompatíveis com trim otimizado. MP4 edit-list também pode reter conteúdo que o usuário pensou ter apagado. É necessário criar futuramente modo de corte simples, elegibilidade restrita e testes em aparelhos antes de usar essa opção.
- **Testes:** contrato Python para versão alinhada, tempos medidos e proteção contra edição experimental; JUnit para exibição de métricas. Compilação completa, lint, smoke Android real e benchmark antes/depois pendentes.
- `schemaVersion=4`, cinco APKs, assinatura persistente, efeitos, legendas, PiP, narração, projetos legados e arquivos originais preservados.

## Versão 0.1.119 · versionCode 120 (03/11/2026)

- **Etapa 2 — fidelidade entre prévia e MP4:** quando há PiP, legendas e marca d'água agora são efeitos da composição inteira, aplicados após o compositor de vídeo (`Composition.Builder.setEffects`), mantendo-os acima das janelas PiP. Sem PiP, permanece o processamento anterior por clipe.
- **Ordem de sobreposição PiP:** normalização da lista de camadas para compensar a regra Media3 (primeira entrada no topo) em relação ao Compose (última camada desenhada no topo), sem alterar a ordem gravada no projeto.
- **Prévia de transições:** zoom cinematográfico, recuo suave e pan usam a mesma curva temporal (`TransitionEffects.motionAt`) na prévia e no `ClipMotionTransitionEffect` do exportador; camadas PiP/legendas permanecem independentes do movimento do vídeo principal.
- **Legendas vazias/sobrepostas:** editor e exportador escolhem a primeira legenda não vazia no instante, evitando que uma legenda em branco esconda outra. Sobreposições válidas ainda requerem inspeção visual.
- **Testes:** JUnit para ordem das camadas, seleção de legenda e curvas de movimento; smoke Android complementar com composição PiP + legenda; verificações estáticas para impedir regressões.
- **Limites:** prévia de efeitos sépia/preto-e-branco e tonalidades ainda é aproximada; compilação Gradle, smoke test Android real e comparação quadro a quadro/áudio continuam pendentes. Não afirmar fidelidade integral sem esses testes.
- `schemaVersion=4`, cinco variantes de APK Release, exportação 720p/1080p, assinatura, imagens, TTS, Gemini e arquivos originais preservados.

## Versão 0.1.118 · versionCode 119 (03/11/2026)

- **Etapa 1 — testes de exportação:** incluído MP4 de teste H.264/AAC de 1,6 segundo (320×180, ~29 KiB) somente nos assets do AndroidTest; fixture real reproduzível e validável por `ffprobe`.
- Incluído `ExportPipelineSmokeAndroidTest`: testa a exportação Media3 com recorte, áudio e legenda, aguarda confirmação de gravação no MediaStore e confere faixas de vídeo/áudio, resolução, duração e abertura do MP4; apaga arquivos temporários e saída de teste ao terminar.
- **CI:** todos os contratos Python `tools/test_*.py` e as verificações de compatibilidade Kotlin/Media3 agora são executados em cada build; `compileDebugAndroidTestKotlin` compila testes instrumentados, mas não os executa.
- **Limite da validação:** testes Python/estrutura executados localmente. Compilação Gradle/Android e execução do teste MP4 em aparelho/emulador ainda pendentes, portanto não afirmar que a exportação real está aprovada.
- Editor, Gemini, MP4 do usuário, schemaVersion=4, chaves e assinatura preservados. Não foi adicionado serviço nem envio de mídia.

## Versão 0.1.117 · versionCode 118 (03/11/2026)

- A confirmação **Vídeo salvo** foi redesenhada: estado compacto, miniatura quando o Android fornecer, nome e qualidade em resumo, pasta de destino, compartilhamento prioritário e opções Abrir/Voltar/Fechar. Informações técnicas ficam recolhidas por padrão.
- Campos **Descrição** e **5 hashtags** editáveis, contador do total (até 100 caracteres com quebra de linha), sem emojis/emoticons, hashtags distintas, validação antes de Copiar ou incluir a legenda no compartilhamento.
- Botão opcional **Gerar descrição com IA** reutiliza a chave Gemini já configurada. Mostra o contexto textual e exige confirmação explícita antes da requisição; envia somente assunto/legendas selecionados, nunca vídeo ou áudio. Não fabrica conteúdo se não houver contexto. Pode consumir cota; sem chave, mantém a edição manual.
- Geração no endpoint `models/gemini-2.5-flash:generateContent` com resposta JSON validada, cinco hashtags exatas e corte conservador da descrição ao limite de 100 caracteres. Erros de rede, cota e formatação informados sem mostrar credenciais.
- Mantido o mesmo diálogo nos fluxos **Exportar vídeo** e **Animar Imagens**, sem alteração de `schemaVersion=4`, MP4, timeline, assinatura ou workflow de cinco APKs.
- **Validação:** contratos locais e testes Kotlin de política de publicação. Ainda pendentes build Android completo, chamadas Gemini reais, teste de miniatura, layout e compartilhamento em aparelhos.

## Versão 0.1.116 · versionCode 117 (03/11/2026)

- Interface **Narração Gemini**: remove dezenas de campos de texto simultâneos e exibe uma única fala editável por vez, com navegação anterior/próxima, preservando os textos e horários originais.
- **Gerar falas/Retomar/Revisar** permanece fixo no rodapé da janela, visível sem percorrer toda a rolagem. Quando desabilitado, explica se falta voz, consulta ao catálogo, texto, espaço disponível ou um lote válido (máximo de 12).
- Importação de legendas paginada em grupos de 12: exibe apenas uma página, com navegação, seleção da página corrente e confirmação manual. Não altera automaticamente os demais trechos.
- **Áudio como referência**: apenas uma fala ativa na análise e nos comandos Ir à legenda/Marcar aqui; Anterior/Próxima substituem os três controles repetidos em cada linha. O processamento continua local e sob demanda.
- Preserva checkpoint SHA-256, cache de WAV, estilos e confirmação antes das requisições ao Gemini. Não altera `schemaVersion=4`.
- **Testes pendentes**: build Android completo, interação real em celular, geração de voz, consumo de cota, análise de MP4 e exportação.

## Versão 0.1.115 · versionCode 116 (03/11/2026)

- Checkpoint privado por projeto para **narração sincronizada**: armazena rascunho de trechos, modelo, voz, estilo, horários corrigidos, metas de duração e caminho/medida/digest SHA-256 de WAVs já gerados. Não registra a chave Gemini e não envia arquivos à rede.
- Após reinício do aplicativo, reabre o lote somente se projeto e horários das legendas ainda forem compatíveis. Cada WAV é verificado contra o digest salvo; ausência, corrupção ou alteração de origem impedem reutilização silenciosa.
- Após cada síntese de um trecho e gravação íntegra do WAV, registra o checkpoint **antes de solicitar o próximo trecho**. Falha/cancelamento pode deixar chamada de API já contabilizada, mas trechos concluídos são reutilizados. Não há novas chamadas automáticas.
- Importador de legendas seleciona até **12 falas por lote**, com botão "Próximas 12"; a ação "Retomar (N)" mostra quantas chamadas faltam e exige autorização. Nenhuma alteração de formato de projeto (`schemaVersion=4`).
- **Pendências de validação:** Gradle/Android completo, morte do processo durante a geração, restauração real de WAVs, transições de lotes e MP4 exportado.

## Versão 0.1.114 · versionCode 115 (03/11/2026)

- Na narração sincronizada, cada WAV preparado tem **Comparar A/B**: ouvir o som original do vídeo na janela correspondente e ouvir o WAV Gemini separadamente. Player local, sem efeito sobre o projeto ou exportação.
- Nova **meta de duração** individual, com ajustes de 250 ms, enviada como orientação opcional de ritmo na regeneração. A API não garante duração exata: sempre comparar com a duração medida do WAV, sem cortar ou fazer time-stretch implícito.
- **Refazer fala** faz uma solicitação Gemini apenas para o segmento escolhido, com confirmação de possível cota. O WAV anterior só deixa de ser usado após novo áudio válido e salvo; falhas preservam os anteriores.
- Diagnóstico de áudio abaixo de 250 ms separado de ultrapassar o fim do projeto, evitando orientação incorreta durante a revisão.
- Mantidas revisão de sobreposição/excesso do vídeo, reutilização das legendas, alinhamento de referência sem upload, schemaVersion 4 e mixagem existente. **Pendências:** compilação Gradle/Android, áudio real da API e teste da prévia/exportação em aparelhos.

## Versão 0.1.113 · versionCode 114 (03/11/2026)

- Referência de áudio agora inclui análise local sob demanda (Android MediaExtractor/MediaCodec) de até 15 segundos em torno da fala selecionada e forma de onda por energia RMS.
- Mostra regiões de atividade sonora e um possível começo próximo da legenda. Música/efeitos também podem produzir picos; não é reconhecimento de fala nem alinhamento por palavra. Sugestões são opcionais e dependem de duas ações explícitas: **Usar sugestão** no rascunho e **Aplicar horários**.
- Processamento limitado em tempo, fora da thread principal, cancelável, sem enviar áudio ao Gemini ou modificar os arquivos originais. Suporta referência do vídeo, faixas existentes e arquivo SAF conforme disponibilidade do decoder.
- `schemaVersion=4` preservado, sem nova transcrição nem geração de voz automática. **Pendente:** compilação Android/CI e testes em aparelhos com mídias reais, formatos variados e vídeos com vários clipes.

## Versão 0.1.112 · versionCode 113 (03/11/2026)

- Na geração por trechos, novo botão **Usar áudio como referência de tempo**: ouvir o vídeo do projeto, uma faixa adicional ou importar áudio local pelo seletor Android.
- Marcação manual dos inícios de falas usando o tempo da referência, com offset ajustável para arquivo externo, revisão e confirmação. Preserva horários/textos das legendas; nenhum processamento de reconhecimento novo ou upload do áudio.
- Os horários de voz confirmados entram na revisão de conflitos e são gravados nas faixas Gemini, mantendo os WAVs já gerados, sem cortar silenciosamente nem mudar `schemaVersion=4`.
- **Limite:** não é alinhamento automático por voz; a API TTS normal recebe apenas texto. Replicação de timbre exige recurso separado, consentimento e autorização; não foi ativada.
- **Pendente:** compilar Android no GitHub Actions e testar referência sonora/Media3 e exportação real em aparelho.

## Versão 0.1.111 · versionCode 112 (03/11/2026)

- **Reutilizar legendas:** modo Sincronizada por trechos ou Contínua. O modo temporal preserva os instantes de cada legenda, permite edição individual sem alterar o texto original e exige confirmação antes das chamadas Gemini.
- Até 12 falas por geração; cada fala usa uma chamada TTS. Em falha/cancelamento, os WAVs já gravados são mantidos e as falas inalteradas não são solicitadas novamente nesta sessão.
- Antes da inserção, revisão mostra a duração real de cada WAV, conflitos com legendas/vozes seguintes e excedentes do fim do vídeo. Excedente final bloqueia a inclusão; sobreposições dentro do vídeo exigem aprovação expressa. Nenhum áudio é cortado automaticamente.
- Faixas sincronizadas são salvas em uma única atualização do projeto após aprovação. Sem mudanças no JSON/schemaVersion 4, nos clipes, legendas ou exportador; a narração contínua permanece disponível e também não é mais truncada silenciosamente.
- **Pendente:** compilação Android completa, teste com Gemini real, sincronização de preview/exportação no aparelho e interrupções do Android.

## Versão 0.1.110 · versionCode 111 (03/11/2026)

- Novo botão fixo **Salvar e voltar ao vídeo** na tela Áudio; persistência é confirmada antes da navegação, impedindo saída quando falha, durante extração ou com corte ainda não aplicado.
- Mixagem com títulos mais legíveis para arquivos de vídeo com nomes numéricos gerados automaticamente, cartões mais compactos e avisos dispensáveis, mantendo volumes e títulos originais intactos no projeto.
- Pré-escuta, edição das faixas, extração e narração Gemini preservadas; esquema do projeto continua versão 4.
- Testes locais e validação de ZIP não substituem build Android nem teste visual/áudio em aparelho.

## Versão 0.1.109 · versionCode 110 (03/11/2026)

- Corrigida a navegação da narração Gemini mostrada no vídeo de teste: confirmar a voz mantém a janela de geração aberta; após consentir com a chamada de IA, uma resposta WAV válida é gravada e adicionada à tela Áudio, sem navegação automática. Falhas de gravação preservam o WAV para nova tentativa sem repetir a síntese.
- Adicionado botão **Ouvir mixagem** no topo da tela Áudio para conferir o som original do vídeo e as faixas sincronizadas. A reprodução não muda o projeto.
- Novo botão de **Volume da prévia** junto ao Play na tela do editor, com ajuste de 0% a 100% para clipes, música, faixas adicionais e áudio dos PiPs. Não altera a mixagem persistida nem o MP4.
- A confirmação de geração esclarece custo/cota e inclusão; removida a solicitação GET desnecessária de validação do modelo, sem repetição automática de chamadas. Preservado schemaVersion 4, cinco APKs por arquitetura e versionCode 110.
- Verificações locais de sintaxe e contratos não substituem compilação Gradle/Android nem chamada Gemini com chave real.

## Versão 0.1.108 · versionCode 109 (03/11/2026)

- A ferramenta **Extrair áudio de vídeo** inicia recolhida, com título e seta para abrir/fechar; mantém origem, pré-escuta, formato e opções Salvar/Salvar e usar. Ao importar um vídeo externo pela galeria, o painel abre para continuar a extração.
- Após gerar uma narração Gemini e confirmar **Adicionar à timeline**, o projeto é persistido antes de voltar ao editor de vídeo. Falha ao salvar mantém o editor de Áudio aberto e informa o problema; não há inclusão silenciosa.
- Confirmar apenas a escolha da voz continua retornando ao editor sem gerar nem adicionar áudio. Salvamento em arquivo externo (Salvar áudio) permanece independente.
- `schemaVersion=4` mantido, versionCode 109, GitHub Manager/Gradle/CI sincronizados. Compilação Android e teste físico pendentes.

## Versão 0.1.107 · versionCode 108 (02/11/2026)

- Seleção de voz agora é invalidada quando pesquisa ou filtro escondem a voz anterior, prevenindo confirmar voz masculina sob filtro Femininas (e vice-versa).
- O RadioButton é acionável, o cartão exibe estado selecionado e a confirmação exige uma voz da lista visível.
- Nova amostra opcional dentro do seletor; respeita a voz marcada, cache por voz/modelo/estilo, consentimento de cota, e não troca a voz confirmada se o usuário cancelar.
- Testes para seleção feminina versus escolha masculina anterior, busca, retorno ao editor, versão e metadados.
- Compilação Gradle/Android e teste de timbre com chave real pendentes.

## Versão 0.1.106 · versionCode 107 (02/11/2026)

- Seletor de vozes enxuto, pesquisa por nome/ID/perfil e filtros de gênero sem adivinhar características não informadas.
- Metadados e descrição original agora ficam recolhidos em Detalhes, reduzindo truncamento e excesso de textos em inglês na lista principal.
- Corrigida a sobreposição visual de diálogos Narração/Escolher voz: uma única janela por vez. Confirmar retorna ao vídeo sem chamadas TTS; cancelar não altera a voz.
- Testes JVM de filtros/resumo e contrato estático de seleção; versionCode 107, metadados GitHub Manager, workflow e documentação sincronizados.
- Gradle Android e teste visual no aparelho ainda pendentes.

## Versão 0.1.105 · versionCode 106 (02/11/2026)

- Corrige imagem pouco nítida em **Animar Imagens**: substitui miniatura quadrada 640 × 640 por decodificação proporcional com rotação/espelhamento EXIF e redimensionamento sem ampliação artificial.
- Painel de prévia maior e opção Ampliar; mantém a composição de fundo escurecido e imagem inteira já adotada pelo MP4.
- Qualidade da prévia em 3 níveis independentes da exportação. Adiciona testes de proporção/amostragem e checagem de regressão no validador.
- `schemaVersion` de projetos e contrato do rascunho Animar Imagens permanecem iguais; compilação Android e teste visual real pendentes.

## Versão 0.1.104 · versionCode 105 (02/11/2026)

- Consulta ao GitHub: mensagens seguras de erro para DNS, timeout, TLS/SSL, conexão, HTTP 401/403/404/429/5xx e resposta inválida. A exceção original não é exposta.
- A tela Atualizações oferece **Abrir Releases no navegador** quando a API não pode ser consultada; consulta automática permanece sem token e requer repositório público.
- Testes de regressão do diagnóstico e fallback; preservada confirmação antes de baixar ou instalar, verificação SHA-256 e assinatura Android.
- Build Android físico e teste com repositório real ainda pendentes.

## 0.1.103 · versionCode 104 (02/11/2026)

- **Navegação:** Confirmar voz → fechar seletor e gerador → editor com vídeo reproduzindo a partir do cursor existente. Não chama TTS nem altera faixas nesse ato.
- **Estado:** voz, texto, modelo, estilos e catálogo retidos durante a sessão de edição do projeto, mesmo ao trocar entre Áudio e editor.
- **Interface:** listagem de vozes com linhas clicáveis alinhadas e marcador de seleção.
- **Segurança/controle:** geração e inserção na timeline continuam exigindo confirmação explícita. Selecionar voz não modifica vídeo.
- **Regressão:** testes unitários de seleção + validação estrutural de navegação e consistência de versão. Build Android pendente.

## 0.1.102 · versionCode 103 (02/11/2026)

- Adicionado painel de Diagnóstico do catálogo de vozes com teste manual, motivo seguro de falha, estágio pt-BR/geral e contagem sem enviar texto ou chave.
- Busca pt-BR vazia ou com HTTP 400/422 tenta catálogo geral; erros 401/403/429 são reportados sem fallback automático. Parser, paginação e reporte testáveis com respostas simuladas.
- Versão sincronizada com Gradle, GitHub Manager, Home/Sobre, Novidades e CI. Teste Android real pendente.

## v0.1.101 — 02/11/2026 (versionCode 102)

- **Correção workflow 85:** dos 343 testes de unidade, `HomeAboutVersionContractTest` falhava por comparar o AppReleaseNotes com 0.1.99/100 fixos enquanto o app estava em 0.1.100/101 e por procurar a prévia da Home somente em `current.features`.
- **Verificação dinâmica:** agora os testes conferem `BuildConfig.VERSION_NAME` e `VERSION_CODE`; a busca por documentação da Home percorre todas as notas retidas (sem pedir que funcionalidades antigas sejam novidades da versão atual).
- **Prevenção:** `tools/test_version_sync.py` e as principais asserções de versão de `tools/validate_source.py` derivam a versão do Gradle. Incluído contrato de teste no validador para prevenir regressão CI85.
- **Sincronização:** v0.1.101/versionCode 102 no Gradle, workflow, AppReleaseNotes, VERSION, GitHub Manager, pacote e notas.
- **Limite:** testes Kotlin isolados e validação estrutural não equivalem a Gradle/Android completo. Reexecutar CI para confirmar 343+ testes, Lint e os cinco APKs Release.

## v0.1.100 — 02/11/2026 (versionCode 101)

- Consulta a Releases do GitHub na abertura do app (inclusive pré-lançamentos publicados pelo workflow), somente quando há repositório configurado via CI ou Configurações. Sem download em segundo plano.
- Configurações → Atualizações pelo GitHub: repositório, verificar agora, changelog, download manual, SHA-256, conferência do APK e instalação explicitamente autorizada pelo usuário e Android.
- Metadados do GitHub Manager, workflow, Gradle, Sobre/Novidades, scripts e validação em sincronia. `schemaVersion=4` preservado.
- Sem Android SDK local, compilação e teste físico pendentes.

## 0.1.99 (versionCode 100) — 2026-11-01
- A tela Sobre oferece prévia somente leitura de elementos reais da Home para conferir alterações visuais sem iniciar ações.
- Uma única fonte de versão é usada na Home e em Sobre; teste UI instrumentado cobre a comparação e ausência de efeitos colaterais na prévia.
- Metadados reconhecidos pelo GitHub Manager (`github-manager.json`, `app_identity.json`, `VERSION`) agora acompanham o versionamento do Android e são checados automaticamente.

## v0.1.98 (versionCode 99) · 01/11/2026

- **Correção do workflow Gerar APK Uai Cortes 83:** o ZIP da v0.1.97 estava incompleto e não incluía `app/build.gradle.kts`, `build.gradle.kts` e `settings.gradle.kts`. A etapa de validação parava com `FileNotFoundError` antes de compilar Kotlin/Android.
- **Empacotamento corrigido:** os três scripts Gradle retornaram ao pacote de código-fonte. Incluído `tools/package_source.py` para gerar futuros ZIPs, verificar todos os arquivos críticos e validar o conteúdo após extração temporária.
- **Preservação funcional:** importação de legendas com confirmação, escolha de voz, cache, estilos e editor permanecem sem mudanças de funcionamento; `schemaVersion=4` mantido.
- **Versionamento:** `versionName=0.1.98`, `versionCode=99`, workflows e nomenclatura dos cinco APKs sincronizados.
- **Limite da verificação:** o pacote pode ser validado estruturalmente; a execução de `:app:testDebugUnitTest :app:lintDebug :app:assembleRelease` ainda depende de ambiente Android/CI, assim como testes em dispositivo.

## v0.1.97 (versionCode 98) · 01/11/2026

- **Áudio → Gerar por IA → Reutilizar legendas**: permite reaproveitar exclusivamente os textos de legendas já salvas no projeto, independentemente da origem offline, Gemini ou manual. Nenhuma nova transcrição, upload ou chamada à API é feita ao importar.
- **Revisão obrigatória**: lista os trechos com horários, permite marcar/desmarcar, mostra quantidade e caracteres. Texto acima de 4.000 caracteres bloqueia a importação, sem cortes silenciosos. A confirmação informa se substituirá o texto editável atual; legendas originais permanecem intactas.
- **Voz manual**: abrir narração não busca catálogo automaticamente; “Consultar” faz a consulta após ação explícita. A lista não escolhe automaticamente a primeira voz; escolher voz exige “Confirmar voz”. Gerar áudio conserva a confirmação de possível consumo da API.
- **Confirmação da timeline**: adicionar o WAV gerado abre diálogo com horário inicial, duração e eventual corte no fim do projeto. A faixa só é incluída depois de confirmar; mídia/legendas originais não são modificadas.
- **Limites**: nesta fase o texto é agrupado em um rascunho de narração; geração e sincronização por trecho/múltiplos falantes continuam planejadas, não implementadas. `schemaVersion=4`; versão `0.1.97`, `versionCode=98`, cinco APKs no CI. Build e teste Android real pendentes.

## v0.1.96 (versionCode 97) · 01/11/2026

- Narração Gemini: 16 estilos de interpretação, mantendo os cinco anteriores, com rótulos em português e instruções em `speech_metadata.style`.
- Ajustes avançados opcionais: ritmo, expressividade emocional e altura/entonação em cinco níveis cada, restauráveis aos valores naturais. Não prometem m/s, Hz ou intensidade acústica exata.
- Uma instrução composta é usada tanto na amostra quanto na geração definitiva; cache considera a instrução completa. Presets antigos com ajustes naturais mantêm a mesma chave de cache da v0.1.95.
- Preserva texto narrado literalmente, autorização explícita antes de solicitar serviço, vozes/IDs, histórico de amostras, edição offline e `schemaVersion=4`.
- Compilação completa Android e validação da fidelidade dos estilos no aparelho ainda pendentes.

## v0.1.95 (versionCode 96) · 01/11/2026

- Catálogo Gemini: nomes traduzidos para expressões conhecidas, preservando nome original e ID técnico; características só aparecem quando fornecidas pela API. Busca cobre tradução, nome, ID e metadados.
- Ouvir amostra: lê primeiro do cache interno, com chave derivada criptograficamente da conta, modelo, voz, estilo e frase fixa. A primeira síntese continua exigindo confirmação e pode consumir cota; reutilizações não fazem nova chamada.
- Cache WAV privado: validação de formato, limite de 24 amostras / 12 MiB, expiração após 30 dias sem uso e limpeza automática possível pelo Android. Amostras não são anexadas aos projetos.
- A API de catálogo não fornece `sample_audio` para vozes predefinidas; não é apropriado assumir acesso direto às prévias existentes no site do Google.
- Não modifica estilos de narração nem a reprodução de legendas; `schemaVersion=4` mantido.

## v0.1.94 (versionCode 95) · 01/11/2026

- **Legendas — reprodução contínua:** selecionando uma legenda, a prévia começa naquele instante, mas o Play não pausa mais em `endMs` da legenda. O Media3 continua pelos clipes seguintes até `STATE_ENDED` ou pausa manual. Ao reproduzir novamente após o fim, começa no início do projeto.
- **Legenda no tempo real:** enquanto reproduz, as legendas exibidas acompanham o relógio da timeline, incluindo legendas seguintes, lacunas sem texto e sobreposições; a seleção de edição permanece independente. Ao pausar, a legenda selecionada volta a ser editável; o botão Reiniciar legenda volta ao início da seleção.
- **Áudio — atalhos sem duplicação:** a primeira seção agora contém Escolher áudio e Gerar por IA lado a lado. Extrair áudio de vídeo mantém sua seção separada e suas opções de origem/formato/salvamento.
- **Compatibilidade:** sem mudanças na API Gemini, vozes, cotas, exportação, mídia ou schemaVersion 4. A v0.1.95 fica reservada para melhorias de catálogo/prévias.
- **Validação:** teste novo para intervalos consecutivos, lacunas, sobreposições e fim. Build Android, lint e ensaio de reprodução real devem ser confirmados via CI/aparelho antes de anunciar APK.

## v0.1.93 (versionCode 94) · 01/11/2026

- **Prévia fixa:** o exemplo animado das legendas fica fora da área rolável dos diálogos Editar legenda e Editar todas; permanece visível enquanto navega pelas opções. A prévia principal da tela de legendas já era fixa acima da lista.
- **Formatação compacta:** negrito, itálico e tamanho ficam em uma única barra; botões de −/+ alteram 2 sp por toque, respeitando 12–48 sp. Presets animados horizontais passam de 158 para 142 dp de largura e 64 dp de quadro.
- **Opções sob demanda:** cores por palavra, posição (horizontal/vertical) e efeitos avançados (fonte, alinhamento, contorno, sombra e espaçamento) recolhíveis. Nenhuma função foi removida.
- **Editar todas:** caixas de seleção verticais substituídas por três chips finitos Estilo/Animação/Posição, com a mesma confirmação e preservação dos textos e tempos.
- **Segurança e compatibilidade:** nenhum campo persistido ou motor de legendas foi alterado; mantém schemaVersion 4. Não mexe em áudio, cache nem na API Gemini.
- **Pendente:** Gradle/Android Lint e validação visual em aparelho; testes locais não confirmam pixels nem exportação MP4.

## v0.1.92 (versionCode 93) · 01/11/2026

- **CI 77:** `:app:lintDebug` reprovou por `Range` em `SubtitleAudioCache.kt:57`, pois `Cursor.getColumnIndex()` pode devolver `-1`.
- **Correção:** o índice de data de modificação passa por verificação `>= 0`, com tratamento de coluna `NULL`, antes da leitura com `Cursor.getLong()`. A ausência de metadados continua usando `-1L`, sem invalidar o cache ou mascarar a falha.
- **Regressão:** `tools/validate_source.py` verifica a condição segura e a ausência de acesso `getLong(getColumnIndex(...))`. Sem desativar Android Lint.
- **Versão:** Gradle, CI dos cinco APKs Release, empacotamento, Sobre/Novidades, README e documentação sincronizados em 0.1.92/93. A v0.1.91 compilou Debug/Release, passou pelos testes unitários e gerou `assembleRelease` nos logs; a publicação foi bloqueada por `lintDebug`.
- **Pendente:** executar novamente CI para confirmar o Lint e publicar os APKs. Nenhum teste físico de legendas/exportação foi efetuado nesta correção.

## v0.1.91 (versionCode 92) · 01/11/2026

- Opções visuais de legendas organizadas em rolagem horizontal finita, inclusive cores, fontes, alinhamentos e animações. Galeria de 12 cartões compactos animados.
- Escolha de modelo também define uma animação real; adicionados Bounce, Pulsar e Zoom reverso à prévia e exportação baseadas em SubtitleAnimations.frame.
- Novas transcrições Vosk/Gemini usam tamanho inicial de 20 sp em lugar de 30 sp. Controle 12–30 sp na revisão antes de aplicar; legendas antigas inalteradas.
- Áudio WAV reutilizável em cache privado para chamadas Gemini equivalentes, com hash de clipes/cortes, verificação de comprimento, retenção máxima de 3 horas e limpeza limitada. Nova chamada de API continua necessária.
- Ajuda de posicionamento movida para ícone na linha do tempo; menos texto persistente na tela.
- Build Gradle/Android e testes de exportação e API em dispositivo pendentes.

## v0.1.90 (versionCode 91) · 01/11/2026

- Corrigido o teste de presets: o deslocamento de sombra personalizado é preservado quando um modelo não define sombra; modelos com sombra explícita continuam aplicando seu próprio valor.
- Barra da tela Legendas em grade 2×3 com cantos 8 dp; importar SRT, exportar SRT/TXT (SAF) e apagar todas as legendas após confirmação, com opção Desfazer.
- Gemini ASR opcional pela chave protegida já salva nas Configurações: extrai WAV temporário da ordem de clipes/cortes, pede consentimento antes do envio, usa Interactions API Gemini 3.5 Transcribe com tempos por palavra e permite revisar antes de aplicar. Requer internet e pode consumir cota/cobrança.
- Limite de 10 min; o modo Vosk offline segue independente, e projetos existentes continuam com schemaVersion=4.
- Verificar compilação/limites de API, sincronismo e exportação no Android físico antes de declarar funcionamento confirmado.

## [0.1.89] - 2026-11-01

- Nova galeria visual com 12 estilos de legendas e recursos de personalização de cores por palavra.
- Anotações de tempo por palavra do Vosk preservadas no JSON; karaokê usa tempos observados e mantém fallback para legendas sem tempos individuais.
- Texto itálico e cores de palavra compartilhadas entre prévia e renderização MP4.
- Corrigido o destino do botão Voltar da ferramenta Animar Imagens, com proteção de rascunhos não salvos.
- Próximo versionCode = 90; testes Gradle e dispositivos ainda pendentes.

## [0.1.88] - 2026-11-01

- Salvamento de projetos: histórico de cópias locais automáticas com intervalos de 15 s, 30 s, 1, 2 ou 5 minutos, sem duplicar conteúdo e com retenção de 3/5/10 versões.
- Configurações > Salvamento e segurança: escolher frequência, quantidade de cópias, pasta SAF externa, exportar/importar JSON íntegro SHA-256 e confirmar restauração.
- O cadastro do projeto continua salvo a cada edição; cópias históricas são uma proteção adicional. Ao restaurar, o app tenta salvar o estado atual antes de substituí-lo.
- Indicador de mídias não acessíveis ao abrir um projeto. Backup JSON não copia vídeos/fotos/músicas; o MP4 exportado permanece em Filmes/UaiCortes, não Downloads.
- Não foi acrescentado backup em nuvem nem alteração ao esquema JSON 4. A recuperação após limpar dados/desinstalar requer arquivo exportado ou pasta externa acessível.

## v0.1.87 (versionCode 88) · 01/11/2026

- **Animar Imagens:** cinco padrões de câmera (automático, aproximar, afastar, panorâmica horizontal e vertical) aplicados pela mesma função de quadro à prévia e ao Media3; mantendo a opção `auto` em rascunhos antigos.
- **Interface de Animar Imagens:** configurações organizadas em painel inferior rolável; reprodução guiada por tempo real de quadros; toque para inspecionar foto na sequência; cabeçalho “Rascunho” diferenciado de “Gerar MP4”.
- **Prévia:** fundo da própria foto ampliado e escurecido, foto principal sem corte, e transformação sobre toda a composição; ainda é prévia aproximada, sujeita a diferença em transições e hardware.
- **Rascunhos e exportação:** novo campo opcional `motion` no JSON sem perder a leitura dos rascunhos antigos; mantém renderização H.264 offline, duração e formato, sem alterar projetos de vídeo ou `schemaVersion=4`.
- **Validação:** código e testes locais; renderização e comparação visual no Android físico permanecem pendentes.

## v0.1.86 (versionCode 87) · 01/11/2026

- **Correção do workflow Gerar APK 72:** `:app:lintDebug` reprovou com 2 erros `UnsafeOptInUsageError` no `VideoEffects.kt`, ao acessar `TextOverlay.TEXT_SIZE_PIXELS` nos spans de contorno e sombra.
- **Correção localizada:** `@androidx.annotation.OptIn(UnstableApi::class)` nas duas classes, preservando o dimensionamento e estilo das legendas e sem suprimir inspeções do Android Lint.
- **Prevenção de regressão:** verificação estrutural de ambos os opt-ins e da importação correta, com teste específico; metadados de versão e workflow sincronizados.
- **CI 72 confirmado nos logs:** compilação Debug/Release e `:app:assembleRelease` executadas; bloqueio no Lint impediu a publicação dos cinco APKs.
- **Pendente:** repetir o workflow integral de Gradle (`testDebugUnitTest`, `lintDebug`, `assembleRelease`) e verificar geração/publicação dos APKs.

## v0.1.85 (versionCode 86) · 01/11/2026

- **Correção do workflow Gerar APK 71:** `WhatsNewReleaseNotesTest.reorderingOrChangingACaptionChangesTheFingerprint` falhava porque a versão 0.1.84 contém uma única novidade e a lista invertida é igual à original (288 de 289 testes passaram).
- **Teste corrigido, regra de produto preservada:** reordenação agora é verificada com duas novidades artificiais apenas no teste; um novo teste garante que uma novidade isolada tem a mesma impressão digital quando invertida, mas mudanças no texto alteram a impressão digital. Sem adicionar itens artificiais na interface.
- **Confirmado no CI 71:** `compileDebugKotlin` e `compileReleaseKotlin` concluíram com avisos não impeditivos. Publicação dos cinco APKs interrompida nos testes; não houve falha de assinatura identificada.
- **Compatibilidade:** nenhum comportamento do exportador, Media3, Gemini, legendas, PiP ou `schemaVersion 4` alterado. Assinatura e workflow mantidos.
- **Pendente:** executar o workflow completo para confirmar todos os testes, lint e geração de cinco APKs na versão 0.1.85.

## v0.1.84 (versionCode 85) · 01/11/2026

- **Correção do workflow Gerar APK 70:** compilação Debug e Release falharam em `ExportEngine.kt` (linhas 177 e 179) porque `start()` era pública e expunha `ExportBitratePreset` e `ExportQualityReport`, que são tipos internos.
- **Ajuste mínimo:** `ExportEngine.start` passou a `internal fun start`; mantém tipos, parâmetros, callbacks, bitrates, resolução e comportamento de exportação sem mudanças.
- **Regressão:** verificador estrutural agora exige visibilidade compatível nos três símbolos. A validação de sintaxe e o teste isolado do compilador Kotlin não substituem o build Android completo.
- **Preservados:** cinco APKs Release via GitHub Actions, Secrets de assinatura, schemaVersion 4, PiP, Gemini, legendas e projetos.
- **Pendente:** executar workflow para confirmar `compileDebugKotlin`, `compileReleaseKotlin`, testes/lint e publicação dos cinco APKs.

## v0.1.83 (versionCode 84) · 01/11/2026

- **Revisão crítica de v0.1.81 e v0.1.82:** confirmados o uso de `Presentation.createForShortSide`, ordem de redimensionamento antes das legendas e propagação de bitrate. O Media3 pode ignorar parâmetros solicitados; sem testes Android, a melhora visual ainda não está comprovada.
- **Correção de espaço:** a reserva do MP4 temporário e da cópia MediaStore deixa de truncar projetos acima de 24 horas. Cálculos em Long agora saturam em vez de transbordar; estimativa MiB satura em `Int.MAX_VALUE` para durações extremas.
- **Mensagens:** falta de espaço indica o perfil real (Compacta/Alta/Máxima), sem afirmar que toda exportação usa Alta.
- **Regressões:** adicionados testes de duração >24h, saturação e equivalência de reserva para um minuto; preservados os demais 15 testes locais de exportação.
- **Pendente:** teste Gradle/JUnit completo, exportação em aparelho com legendas/PiP, medição de taxa e resolução no MP4, desempenho e comparação após publicação.

## v0.1.82 (versionCode 83) · 01/11/2026

- **Três perfis selecionáveis de bitrate de vídeo:** Compacta, Alta (padrão anterior) e Máxima. Valores solicitados em 720p: 5/8/12 Mb/s; em 1080p: 10/18/28 Mb/s. O encoder pode ajustar o valor e maior bitrate não restaura detalhes perdidos no arquivo-fonte.
- **Estimativas e reserva de espaço coerentes com a escolha:** o tamanho esperado por minuto e o teste preventivo de armazenamento agora usam o perfil selecionado, inclusive na prevenção de falta de espaço para MP4 temporário e cópia na galeria.
- **Aviso de reescala da origem:** consulta dimensões dos clipes em segundo plano e avisa quando o lado menor do vídeo de origem é inferior à resolução escolhida. Não apresenta resolução inventada quando o arquivo não fornece metadados.
- **Tamanho previsto para o projeto:** exibe estimativa aproximada de MiB para a duração total e o perfil selecionado, além da referência por minuto.
- **Quadros-chave:** solicita ao codificador H.264 intervalo de 2 segundos, com fallback do Media3; não afirmar GOP obtido sem verificar o MP4.
- **Relatório de exportação:** diferencia bitrate solicitado e medido, identifica o perfil e fornece nota contextual quando a taxa medida ficar abaixo de metade do alvo. Taxa baixa pode refletir cenas simples, não é diagnóstico definitivo de degradação visual.
- **Compatibilidade:** preserva 720p/1080p pelo lado menor, texto renderizado depois de dimensionar, PiP, legendas, áudio, Gemini, Vosk, `schemaVersion=4`, cinco APKs Release, assinatura e Secrets. Não altera o motor de animações de imagens.
- **Validação pendente:** build Android Gradle e teste físico de MP4 nos três perfis, com legendas e PiP; conferir bitrate e intervalos de quadros-chave no arquivo final antes de afirmar entrega do encoder.

## v0.1.81 (versionCode 82) · 01/11/2026

- **Nitidez de legenda no MP4:** reordena os efeitos do Media3 para normalizar o lado menor do clipe (720 ou 1080 px) ANTES de renderizar legendas e marca d’água, evitando ampliar uma camada de texto rasterizada na resolução menor da fonte. Corrige especialmente exportações verticais 9:16: o modo 1080p passa a solicitar 1080×1920, em vez de cerca de 608×1080. O ajuste de tamanho e posições continua usando o mesmo schema 4. A prévia Compose e o MP4 ainda exigem teste visual no aparelho.
- **Contorno/sombra:** usa tamanho tipográfico consistente com o RelativeSizeSpan no renderizador Media3, evitando dupla multiplicação de escala no desenho dos efeitos.
- **Diagnóstico real do arquivo exportado:** resolução, bitrate de vídeo, quadros por segundo médios estimados, tamanho, codec e alerta quando a altura obtida for diferente da solicitada. Valores indisponíveis são informados como tais, sem estimativa falsa; a confirmação só aparece após o MediaStore salvar.
- **Segurança e estabilidade:** não altera Gemini, PiP, áudio, projeto, assinatura, cinco saídas de APK nem o `schemaVersion=4`. Mantém fallback do encoder para compatibilidade com diferentes aparelhos.
- **Verificações pendentes:** compilação Gradle Android, teste de PiP e legendas em MP4 de 720/1080 e comparação com a publicação nas redes sociais. A imagem publicada não permite diagnosticar sozinha qual etapa gerou a perda de nitidez.

## v0.1.80 (versionCode 81) · 31/10/2026

- **Correção do workflow Gerar APK 67:** falha unitária `SubtitleExportSizingTest.longWordNeverExceedsFrameAndNoNaN` causada por escala mínima de 48% para uma palavra que precisava de redução maior. A tarefa `:app:testDebugUnitTest` interrompia a Release após 270/271 aprovações.
- **Ajuste responsivo de legendas:** `SubtitleAutoFit.factor` agora aplica piso de 48% apenas à redução causada pelo comprimento total da frase; uma palavra isolada pode exigir redução adicional para caber dentro da margem horizontal do quadro, tanto no Compose como no exportador Media3.
- **Escala na exportação:** removido piso absoluto de 0,01 em `SubtitleExportSizing.relativeFontSize`, que poderia reintroduzir corte em casos extremos. Textos, tempos, posição das legendas e esquema de projetos permanecem inalterados.
- **Regressão:** dois novos testes JUnit cobrem palavra longa, limite visual de frases e medidas extremas; execução isolada por runner compatível JUnit: 11/11 aprovados, inclusive o teste que falhou no CI. Compilação Android/Gradle completa ainda pendente.
- **Compatibilidade:** sem alterações nos recursos de PiP, Gemini TTS, Vosk, mediação de áudio, exportação, assinatura ou cinco ABIs. `schemaVersion=4` mantido.

## v0.1.79 (versionCode 80) · 31/10/2026

- **Legendas:** novas legendas iniciam em 92% da altura do quadro (margem inferior de aproximadamente 8%); projeto legado mantém posição individual já salva. Atalho Rodapé · 8% e slider vertical no editor de Estilo, aplicáveis também em lote (posição apenas com confirmação explícita).
- **Textos extensos:** autoajuste do tamanho visual proporcional às larguras medidas do texto e da maior palavra, mirando cerca de 3 linhas sem truncar as falas. A mesma regra é usada na prévia Compose e no bitmap exportado Media3, sem modificar o texto original. Conferir fidelidade de tipografia em aparelho.
- **PiP:** slider 0–100% de opacidade por camada (imagem/vídeo), preservado no JSON esquema 4; prévia TextureView/Compose e exportação via compositor Media3 com alfa. Projetos antigos usam 100%.
- **Captura:** tocar na câmera pausa o player ANTES de fixar o tempo de captura; PNG continua sendo do vídeo original sem PiP/legendas. O indicador mostra o instante capturado e o vídeo permanece pausado.
- **Timeline:** rótulos dos vídeos compactos (Clipe 1, 2…), sem alterar títulos reais dos arquivos nem referências persistidas.
- **Compatibilidade:** exportação, assinatura, cinco APKs do workflow, edição offline, Gemini TTS e schemaVersion 4 preservados. Compilação Android e testes visuais/MP4 reais pendentes.

## v0.1.78 (versionCode 79) · 31/10/2026

- **Legendas → Salvar:** persiste o projeto e retorna ao editor de vídeo do mesmo projeto, não à página inicial. Se a gravação falhar, permanece nas legendas e mostra o erro existente.
- **Editar todas:** ação ao lado do SRT, painel global com abas Estilo/Animação, ajustes avançados recolhidos, confirmação de alteração e botão Desfazer (apenas enquanto o lote atual ainda corresponder ao projeto). Preserva textos, IDs e horários.
- **Edição individual:** mantém texto e tempos e reorganiza os controles visuais; sliders com margens e ações no rodapé do diálogo. Prévia simulada do estilo, com animação do conteúdo e sem mudança de mídia.
- **Reconhecimento Vosk:** relatório do estágio de carregamento do modelo (indeterminado), progresso baseado nos timestamps decodificados por clipe (0–98%), organização dos resultados e cronômetro real. Não estima tempo restante nem inventa progresso durante a abertura do modelo.
- **Compatibilidade:** Vosk offline, SAF/SRT, Media3, Gemini TTS, exportação, assinatura e `schemaVersion=4` preservados. Build Android e validação da aparência/performance em dispositivo físico pendentes.

## v0.1.77 (versionCode 78) · 31/10/2026

- O gerenciamento Gemini passou do formulário embutido para Configurações > Google Gemini, com navegação de retorno a Configurações ou Áudio, botões organizados 2×3 e teste/Salvar separados.
- A Narração com IA não expande mais a tela Áudio: diálogo modal adaptável com rolagem interna, fechamento impedido durante geração e seletor de vozes pesquisável por nome/código da API com LazyColumn.
- Mantidos avisos de possível consumo de cota, confirmação, cancelamento, geração WAV, timeline e armazenamento existentes. A descrição original em inglês da voz não ocupa mais o formulário.
- Adicionados testes de pesquisa de vozes e rotas. Não houve migração de projeto; `schemaVersion=4` mantido.
- Verificações locais não substituem compilação Gradle, teste com Gemini real ou exame da aparência em dispositivo físico.

## v0.1.76 (versionCode 77) · 31/10/2026

- Integração opcional do Gemini TTS via `/v1beta/interactions`: geração WAV, modelos Flash/Lite 3.8, vozes consultadas na conta, estilo estruturado e prévia gerada com aviso de cota.
- Nova seção em Configurações com chaves AES-256-GCM no Android Keystore, SAF, pré-visualização mascarada, confirmação de cópia/exportação/remoção e teste de serviço TTS efetivo.
- Faixa Gemini persistida como `AudioTrack` padrão para corte, volume, timeline e composição Media3; schemaVersion 4 preservado. Suporte WAV precisa de validação física na exportação final.
- Tratamento de falhas de rede/cota/autorização, limite de resposta, validação de RIFF, arquivo temporário atômico, cancelamento sem retry automático e mensagens de privacidade.
- Testes unitários da importação, WAV, classificação de erros e serialização, além da validação estrutural; build Android pendente neste ambiente.

## v0.1.75 (versionCode 76) · 31/10/2026

- Adicionada tela Áudio do clipe (importar via SAF, pré-escuta, volume, aplicar/remover). Vínculo opcional por ID e âncora na fonte; prévia e MP4 usam a posição resolvida dinamicamente.
- Preservados schemaVersion=4, trilhas antigas e arquivos originais. Build Android completo pendente.

## v0.1.74 (versionCode 75) · 31/10/2026

- **Tela Áudio redesenhada:** três cartões com Mixagem do projeto, Adicionar áudio e Extrair áudio de vídeo, mantendo seleção SAF, volume original, trilhas, música legada, corte, confirmação de exclusão, pré-escuta e exportação M4A/MP4 AAC.
- **Controles compactos:** ações de origem Projeto inteiro/Clipe atual/Vídeo externo, seleção de galeria e arquivos para vídeo, formatos e salvar/salvar e usar. Rótulos de arquivo resumidos apenas para exibição, sem alterar títulos persistidos.
- **Fluxo real preservado:** ao escolher áudio, o app já o adiciona ao projeto; a tela não mostra botão fictício para aplicá-lo novamente. O seletor de áudio continua SAF, sem copiar URIs transitórias.
- **Compatibilidade:** `schemaVersion=4`, mídias, PiP, legendas, Vosk, captura de quadros e exportação inalterados. Testes Android/CI e aparência no aparelho pendentes.

## v0.1.73 (versionCode 74) · 31/10/2026

- **Velocidade da prévia:** botões − e + percorrem 14 valores entre 0,1× e 6×, com seleção direta ao tocar no valor e restauração para 1×. Apenas prévia; não altera a exportação nem o tempo do projeto.
- **Controles de quadro:** os comandos ±5 s e os quatro botões de ajuste fino foram substituídos por quadro anterior/próximo (busca por tempo aproximado). Eles pausam o player e utilizam FPS nominal do clipe quando disponível, com fallback estimado de 30 FPS.
- **Limitação:** Media3 não garante salto exato entre quadros nem desempenho em 0,1× ou 6× em qualquer aparelho; conferir em testes físicos e com vídeo de FPS variável.
- **Compatibilidade:** botão de câmera PNG, MediaStore, PiP, legendas, formatos de projeto `schemaVersion=4`, Vosk e parâmetros de exportação preservados.
- **Testes:** controles de velocidade e cálculo temporal em JVM, validador e análise sintática; build Android/CI pendente.

## v0.1.72 (versionCode 73) · 31/10/2026

- **Prévia com velocidades extras:** menu compacto em grade, agora de 0,25× até 5× (incluindo 2,5×, 3×, 4× e 5×). A velocidade altera apenas a reprodução, nunca o MP4.
- **Capturar quadro do vídeo:** ícone de câmera ao lado da velocidade extrai o quadro original na posição da timeline, respeitando cortes e limites entre clipes. Salva PNG em `Pictures/UaiCortes` pela galeria do Android 10+.
- **Qualidade e privacidade:** extração local em IO, sem capturar botões da interface; o PNG representa o vídeo de origem, sem PiP, legenda ou transições sobrepostas. Salvar outro quadro não altera o projeto nem os arquivos de vídeo.
- **Falhas seguras:** erro explícito quando a URI perdeu permissão ou o quadro não é extraível; MediaStore apaga entradas parciais; bloqueio de toques repetidos durante a gravação. Em Android 8/9, pasta externa específica do aplicativo é usada.
- **Testes:** novos casos de instante original, cortes, fronteiras, nomes de arquivos e velocidades. Build Android/CI e captura real em aparelho ainda pendentes. `schemaVersion=4` preservado.

## v0.1.71 (versionCode 72) · 31/10/2026

- **Correção de status do Vosk:** a 0.1.70 deixava a última etapa `Descompactando e verificando o modelo...` visível mesmo depois de `installed=true`, devido à mensagem `stage` persistida na tela. Agora o estado transitório é limpo na conclusão e nunca exibido quando o modelo já está instalado.
- **Teste de regressão:** quatro casos JVM distinguem instalação reconhecida, operação em curso, falha e cancelamento. Orientação para testar a transcrição real após instalar.
- **Preservação:** nenhuma alteração no modelo Vosk, arquivos instalados, projetos (`schemaVersion=4`), editor ou exportador. Os três botões continuam na mesma linha quando o modelo não está instalado.
- **Pendente:** confirmação em APK Android e execução real do Vosk nativo; a detecção de arquivos não comprova transcrição funcional.

## v0.1.70 (versionCode 71) · 31/10/2026

- **Causa comprovada pelo diagnóstico, vídeo e ZIP real:** `vosk-model-small-pt-0.3.zip` contém `final.mdl`, `mfcc.conf` e grafos na raiz (layout Vosk legado). O instalador 0.1.69 exigia exclusivamente `am/final.mdl` + `conf/model.conf`, rejeitando o arquivo oficial em `extractAndInstall`.
- **Correção:** `VoskModelLayout` identifica modelos legados e modernos, exigindo modelo acústico, configuração MFCC e grafo não vazios. Os caminhos nativos são preservados, sem alterar estrutura original nem criar configuração artificial. Mesma validação para download interno, importação SAF e detecção de modelo instalado.
- **Interface:** três botões de instalação em uma única linha (Baixar, Navegador, Importar ZIP), com ícones e rótulos compactos. Cancelamento e progresso continuam separados.
- **Testes:** três novos casos de layout; inspeção do arquivo ZIP real enviado; validador estrutural atualizado. Sem build Android completo/teste do Vosk nativo em aparelho nesta entrega.
- **Compatibilidade:** `schemaVersion=4`, dados de projetos, PiP, exportação e reconhecimento offline mantidos.

## v0.1.69 (versionCode 70) · 31/10/2026

- **Vosk:** alternativa ao download interno: abrir o endereço oficial no navegador e importar o ZIP com seletor Android (SAF), sem extração manual.
- **Instalação:** regras de tamanho, verificação de arquivos essenciais, descompactação com bloqueio de caminhos inseguros, limpeza de arquivos temporários e exclusão mútua entre instalações.
- **Progresso:** bytes reais recebidos e percentual quando o servidor informa tamanho; indicador indeterminado quando não informa.
- **Erros:** diferenciar timeout, falha de DNS, HTTP inválido e ZIP incorreto, sem alegar que a falha original no aparelho foi reproduzida.
- **Preservação:** transcrição offline, `schemaVersion=4`, projetos, PiP e qualidade de exportação inalterados.
- **Validação:** parser Kotlin, regras isoladas de progresso e estrutura de projeto; download real, build Android e uso com Vosk dependem de testes no dispositivo/CI.

## v0.1.68 (versionCode 69) · 31/10/2026
- **Novidades na primeira abertura:** uma tela visual apresenta as atualizações e aparece automaticamente somente se houver mudanças ainda não vistas. A confirmação é armazenada localmente por assinatura do conteúdo das notas, não apenas por número de versão.
- **Informações sincronizadas:** `AppReleaseNotes` é a fonte única usada por `Sobre` e `Novidades`, incluindo as atualizações recentes. Removido o texto antigo da tela Sobre e corrigida a exibição do `versionCode`.
- **Acesso manual:** Configurações > Novidades e Sobre > Ver novidades permitem consultar a lista sem forçar nova abertura automática. Ao tocar em Continuar ou Voltar, a leitura é registrada.
- **Regressões:** testes verificam identidade das listas, reapresentação apenas após novidades reais, restauração da rota e retorno correto. `schemaVersion=4`, vídeos, projetos, exportação, PiP e Vosk preservados.
- **Pendente:** build Gradle/Android, teste no aparelho, rotação e conferência visual da tela nos temas claro e AMOLED.

## v0.1.67 (versionCode 68) · 31/10/2026

- Tela inicial: lixeira direta por projeto e ação de excluir todos com confirmação da quantidade completa; exclusão atômica e sem apagar vídeos originais/MP4s exportados.
- Criação de projeto pede nome antes de selecionar os vídeos; o nome persistido é usado como sugestão do MP4 (sanitização de nome inválido) e continua editável em Exportar.
- Galeria: a busca grande virou uma lupa opcional no cabeçalho; aba única Vídeos/Imagens não aparece quando o filtro já fixa esse tipo; filtros de pasta e SAF continuam acessíveis.
- Legendas automáticas: configuração/instalação Vosk transferida para Configurações > Reconhecimento de voz, com progresso numérico percentual, etapas de verificação/extração, cancelamento e mensagens de erro. Link oficial preservado, teste de download físico pendente.
- PiP: seletor de aplicação temporal aos clipes reais do projeto, projeto inteiro ou cursor ao final; vídeos PiP não são estendidos além da duração original sem suporte de loop. Dois campos compactos e uma única barra de intervalo substituem sliders duplicados.
- Testes e validador ampliados. `schemaVersion=4` preservado. Compilação Android e validação em dispositivo pendentes.

## v0.1.66 (versionCode 67) · 30/10/2026

- **Rascunhos seguros do Animar Imagens:** codec separado valida URIs e opções e distingue primeira execução, rascunho íntegro, backup recuperável e cadastro danificado.
- **Backup consistente:** antes de salvar, a última versão JSON válida permanece como backup; se ambas as cópias estiverem danificadas, a gravação é bloqueada até o usuário confirmar um reinício explícito. Não apaga imagens originais.
- **Permissões e fotos indisponíveis:** verificação de leitura local em `Dispatchers.IO` antes da exportação; referências sem acesso permanecem no rascunho, são comunicadas na tela e só são removidas por ação explícita.
- **Testes de regressão:** novos casos para formato anterior, JSON inválido, ordem, preservação do backup e acesso a fotos. Acessos persistentes do SAF continuam solicitados pelo seletor Android.
- `schemaVersion=4` preservado, sem mudanças no Media3, bitrate ou no catálogo de projetos. Build Android completo, testes JUnit Gradle e testes reais de permissão no aparelho ainda pendentes.

## v0.1.65 (versionCode 66) · 30/10/2026

- **Persistência e recuperação de projetos:** o cadastro de projetos agora distingue uma instalação nova de uma chave principal ausente/corrompida. A cópia de segurança é examinada antes de assumir que não há projetos.
- **Escrita protegida:** o backup recebe somente o último JSON previamente validado; IDs em branco ou duplicados não podem ser persistidos. Dados ilegíveis continuam bloqueando gravações destrutivas.
- **Aviso de armazenamento:** o aplicativo informa quando recuperou o backup ou quando ambas as cópias estão ilegíveis. Não apresenta um erro de leitura silenciosamente como se os projetos tivessem sido excluídos.
- **Falhas de salvamento:** a interface informa o erro e não executa automaticamente o comando de voltar para a tela inicial quando a gravação falha; a exclusão também informa erro sem esconder a falha do usuário.
- `schemaVersion=4` preservado. Nenhuma migração de projetos nem alteração nos motores de exportação. Compilação Android completa e testes físicos permanecem pendentes.

## v0.1.64 (versionCode 65) · 30/10/2026

### Refatoração de reprodução e timeline
- Introduzido `TimelinePlaybackCoordinator` com coordenadas globais e locais reutilizáveis.
- Centralizados os caminhos de seek do editor e da prévia, com avanço/retrocesso entre clipes.
- Arrasto do slider protegido contra a atualização periódica que causava saltos no cursor.
- `EditorPlaybackViewModel` retém apenas velocidade da prévia e ID do projeto; player permanece sob ciclo de vida da tela.
- `Player.Listener` notifica imediatamente play/pause; a posição recuperada é aplicada após preparar a fila de mídias.
- Novos testes JVM para bordas e restauração de estado.
- Build Android, CI integral e testes reais pendentes.

## v0.1.63 (versionCode 64) · 30/10/2026
- **Animar Imagens modularizado:** `ImageAnimationTaskViewModel` assume o motor Media3, progresso, cancelamento e confirmação do MP4; a interface só apresenta estados e opções.
- **Reaproveitamento:** mesmo `ExportTaskCoordinator` do exportador principal com tickets de tentativas, fases PREPARING/RENDERING/SAVING e progresso numérico somente quando o Transformer o informa.
- **Concorrência:** instância própria de `ImageAnimationEngine` para cada tentativa, para impedir que a limpeza de um preparo cancelado remova arquivos de outra tentativa. A limpeza durante decodificação é adiada até terminar a operação atual.
- **Rotação:** ViewModel mantém a exportação durante recriação da Activity e as opções visuais são salváveis com valores simples; não recupera trabalho após morte do processo.
- **Compatibilidade:** animação continua offline, PNG temporário sem perdas, H.264 720p/1080p, limite de 80 imagens, formato `schemaVersion=4`, integração com editor e cinco APKs por ABI preservados.
- **Pendente:** build Android Gradle/CI, testes de rotação, cancelamento, salvamento e qualidade MP4 em dispositivo físico. Testes unitários da máquina de estados não provam processamento real.

## v0.1.62 (versionCode 63) · 30/10/2026
- Extraiu estado e motor de exportação da composição para `ExportTaskViewModel` com `viewModelScope`.
- Criou `ExportTaskCoordinator` puro para andamento, sucesso, falha, cancelamento, identificação de tentativa, tempo e progresso.
- Ignora callbacks de tentativas antigas e bloqueia cancelamento após iniciar cópia para a galeria.
- Tornou a cópia `GalleryVideoSaver` indivisível em relação ao cancelamento; limpeza e notificação do resultado no dispatcher principal.
- Testes Kotlin/JVM para fases, cancelamento, progresso não inventado, falhas e callbacks atrasados.
- Preserva schema 4, H.264/AAC, bitrates e demais funções. Compilação Android, lint e testes reais de exportação pendentes.

## v0.1.61 (versionCode 62) · 30/10/2026
- **Refatoração incremental de arquitetura:** `EditorProjectViewModel` passa a manter a lista de projetos, o projeto ativo e a ID de sessão durante a recriação da interface. A `MainActivity` apenas consome snapshots e encaminha comandos.
- **Restauração limitada e segura:** `SavedStateHandle` recebe somente o ID do projeto. Ao recriar o ViewModel após perda do processo, a versão atual do projeto é carregada novamente de `ProjectStore`. Não serializa JSON, mídia, bitmaps ou jobs no Bundle.
- **Operações testáveis:** `ProjectSessionCoordinator` encapsula carregar, salvar, abrir, excluir e anexar clipes. Snapshots são publicados apenas depois que o repositório conclui a operação. Projeto excluído/inexistente volta a ID vazia, sem criar dados fictícios.
- **Compatibilidade:** `schemaVersion=4`, regras PiP, legendas, importação cancelável, qualidade H.264/AAC da 0.1.56 e cinco APKs por ABI preservados.
- **Validação:** testes isolados do coordenador e análise estática/sintática; build Android completo, lint Gradle e testes em aparelho ainda exigem CI. Não afirmar retomada de importação/exportação após morte de processo.

## v0.1.60 (versionCode 61) · 30/10/2026
- **Refatoração do ciclo de vida das importações:** novo `MediaTaskCoordinator`, independente de Activity/Context, que coordena vídeos, áudio e PiP usando o escopo Compose existente. Encerramento da composição cancela a leitura pendente; não é um serviço em segundo plano nem um ViewModel persistente.
- **Cancelar importação:** diálogo da leitura de metadados oferece cancelamento, liberando a interface imediatamente. As leituras no Android continuam cooperativas: operações bloqueantes do sistema podem terminar posteriormente, mas seu resultado cancelado não deve ser aplicado.
- **Troca de destino segura:** ao mudar projeto, tela ou ferramenta, a tarefa antiga é cancelada. Tickets únicos impedem que um `finally` de tarefa cancelada libere outra importação iniciada em seguida, inclusive da mesma categoria.
- **Tratamento de falhas:** cancelamentos não são reportados como erros; falhas reais mantêm diagnóstico e mensagem genérica. As escritas do projeto continuam no dispatcher principal após a leitura de metadados em IO.
- **Compatibilidade:** schemaVersion 4, projetos, Media3 e qualidade solicitada de exportação (720p/8 Mb/s; 1080p/18 Mb/s) inalterados.
- **Verificações:** cinco testes Kotlin/JVM isolados da camada de tarefas passaram, incluindo cancelamento, reentrada e erro real. Testes Gradle/Android completos e validação em aparelho ainda pendentes.

## v0.1.59 (versionCode 60) · 30/10/2026
- **Refatoração de tarefas de mídia:** `MediaImportTasks.kt` concentra as consultas a metadados de vídeo, áudio e PiP em `Dispatchers.IO`; evita que os seletores SAF bloqueiem a thread principal com leitura de duração e nomes.
- **Concorrência e destino:** `MediaTaskGate` permite apenas uma importação ativa; `MediaTaskOrigin` rejeita resultados quando projeto, tela ou ferramenta já mudaram. Áudio e PiP conferem ainda o projeto em memória antes de salvar.
- **Importação inválida:** o PiP não recebe duração fictícia quando os metadados não puderem ser verificados, e não é inserido quando a posição não tem espaço restante no vídeo.
- **Interface mais enxuta:** removidos imports obsoletos de `MainActivity.kt`; o estado de importação apresenta diálogo único e continua restrito ao escopo de corrotinas do Compose.
- **Compatibilidade:** sem mudanças em `schemaVersion=4`, projetos, formatos de mídia, permissões, exportadores ou opções de 720p/1080p e bitrate da v0.1.56.
- **Validação:** testes unitários novos para exclusão mútua e rejeição de resultados tardios, compilação Kotlin/JVM isolada incluindo o adaptador com corrotinas, validação estrutural e integridade do ZIP. Gradle/Android completo, regressão de UI e qualidade real do MP4 continuam pendentes.

## v0.1.58 (versionCode 59) · 30/10/2026
- **Correção do workflow 51:** adicionados em `MainActivity.kt` os imports `kotlinx.coroutines.launch` e `kotlinx.coroutines.CancellationException`, ausentes na importação de vídeos assíncrona da v0.1.57.
- **Compilação:** resolve as referências não encontradas que bloquearam `:app:compileDebugKotlin` e `:app:compileReleaseKotlin`. O erro de uso de `withContext` fora da coroutine era consequência da referência `launch` não resolvida.
- **Prevenção de regressão:** `tools/validate_source.py` verifica a presença única desses imports, `Dispatchers.IO`, `withContext` e a propagação de `CancellationException`.
- **Compatibilidade:** mantidos `schemaVersion=4`, bitrates/qualidade da v0.1.56, importação assíncrona e os controles atuais de projeto, sem mudar interfaces ou exigir novas permissões.
- **Validação:** inspeção dos logs do workflow 51, validação estrutural, sintaxe Kotlin e compilação isolada de API de corrotinas; Gradle/Android completo e testes em aparelho permanecem a confirmar.

## v0.1.57 (versionCode 58) · 30/10/2026
- **Correção do workflow 50:** teste `EditorWorkspaceStateTest.deletedSelectionsAreRevalidatedAgainstPersistedProject` corrigido: dois clipes possuem uma junção de índice **0**, não 1. Testes adicionais cobrem zero, um, dois e três clipes e revalidação após exclusão.
- **Transições robustas:** `TransitionBoundaryPolicy` centraliza o limite de junções; a prévia usa índice validado mesmo quando o projeto muda mantendo o ID. O editor atualiza a seleção na alteração do projeto, não apenas do identificador.
- **Importação responsiva:** metadados de vídeos importados são consultados em `Dispatchers.IO`; a interface mostra progresso indeterminado, impede duas importações concorrentes e descarta resultado atrasado se o projeto ativo mudou.
- **Mídia inválida:** novas importações usam duração verificada (0 quando indisponível), evitando criar clipes fictícios de 1 segundo. Fallback legado de outros fluxos permanece por compatibilidade.
- **Compatibilidade:** `schemaVersion=4` preservado, qualidade de exportação da 0.1.56 mantida, versionName/versionCode, documentação e workflow sincronizados.
- **Validação:** parser Kotlin, verificação estrutural e execução de testes JVM isolados; Gradle/Android completo e reprodução/exportação reais dependem de CI/aparelho.

## v0.1.56 (versionCode 57) · 30/10/2026
- Exportação de projetos: H.264 com bitrate solicitado de 8 Mb/s (720p) ou 18 Mb/s (1080p), AAC 192 kb/s e fallback do codec mantido.
- Qualidade padrão de novos usuários: 1080p; configurações existentes do usuário são preservadas.
- Animação de imagens: PNG temporário sem perdas (antes JPEG 94), mesmo bitrate de H.264, checagem estimada de espaço para MP4 temporário, cópia MediaStore e imagens preparadas.
- Interface de exportação informa bitrate solicitado e espaço estimado (não garantidos); registro técnico inclui bitrate real reportado pelo Media3.
- Resolução de origem não é recuperada por upscale. Pendente: compilação Android, teste no aparelho e comparação visual objetiva com MP4 da 0.1.55.

## v0.1.55 (versionCode 56) · 30/10/2026
- **Operações de projeto reutilizáveis:** `ProjectController` e `ProjectRepository` separam carregar, abrir por ID, salvar, criar/estender e excluir do código de interface. Atualização do estado acontece somente depois de `save/delete` no armazenamento; o backup e o JSON `schemaVersion=4` continuam em `ProjectStore`.
- **PiP desacoplado:** `OverlayOperations` concentra inserção sequencial, limite de quatro camadas, substituição por ID, cálculo de colisões, autorização explícita para sobrepor e remoção/seleção seguinte. A Activity mantém avisos, confirmação e chamadas de gravação.
- **Importações compartilhadas:** `MediaImportController` unifica consultas locais de metadados e pedidos de permissão SAF. `MediaImportRules` trata seleção única de imagens (máximo 80), clipes válidos e faixa de áudio dentro do projeto; entrada pela galeria e documentos utiliza o mesmo fluxo.
- **Testes e limites:** adicionados testes JUnit de projetos, PiP e importação, validação estrutural e compilação Kotlin/JVM isolada de regras. Não houve build Gradle/Android nem teste de exportação ou interface em aparelho.
- Documentação: `docs/ARQUITETURA_REFATORACAO_0.1.55.md` e `docs/VALIDATION_0.1.55.md`.

## v0.1.54 (versionCode 55) · 30/10/2026
- **Continuação da refatoração:** `EditorWorkspaceState` agora utiliza `rememberSaveable` e salva uma representação compacta (rota, seleções e posição), sem incluir JSON, vídeos ou fotos no `SavedStateRegistry`.
- **Projeto por ID:** o projeto ativo é localizado novamente no `ProjectStore` após recriação da Activity; IDs removidos e posições fora da duração são normalizados, sem alterar a estrutura `schemaVersion=4`.
- **Navegação reutilizável:** `EditorNavigation` concentra rotas, ação Voltar e tratamento de restauração sem projeto. O retorno entre ferramentas mantém as mesmas regras da versão anterior.
- **Fluxos de seleção:** origem/ação da galeria, destino de animação, URIs selecionadas de imagens (máximo 80) e vídeo para extração são salvos quando o Android fornece estado de instância.
- **Testes:** novos testes JUnit para snapshot, restauração sem projeto, seleções inválidas e navegação. Testes Gradle/Android e rotação física ainda pendentes; exportação e transcrição em andamento não são retomadas automaticamente.
- Documentação: `docs/ARQUITETURA_REFATORACAO_0.1.54.md` e `docs/VALIDATION_0.1.54.md`.

## v0.1.53 (versionCode 54) · 30/10/2026
- **Refatoração de base:** `MainActivity.kt` reduzida à inicialização/coordenação. Telas do editor, cortes, áudio, PiP, transições, legendas, exportação, marca d'água e timeline extraídas para arquivos Kotlin dedicados, sem mudar os fluxos visuais.
- **Estado compartilhado:** `EditorWorkspaceState` concentra navegação e seleções transitórias de clipes, PiPs, áudio, legendas e posição. Rascunhos e projetos continuam em seus armazenamentos próprios; não foi adicionada restauração automática de processo.
- **Exportação reutilizável:** `GalleryVideoSaver` publica MP4s de ambos os motores no MediaStore e só devolve a URI após concluir a cópia; elimina implementação duplicada e remove o item parcial ao falhar.
- **Segurança dos projetos:** `ProjectCollectionCodec` e `ProjectStore` preservam cópia anterior válida e bloqueiam `save/delete` se o cadastro e a cópia estiverem ilegíveis, evitando substituir dados por lista vazia. JSON de projetos continua `schemaVersion=4`.
- **Verificações:** validador estrutural atualizado e testes unitários acrescentados para codec/recuperação; sintaxe Kotlin inspecionada. Build Android/Gradle, testes unitários completos e verificação de MP4 em aparelho permanecem pendentes.
- Veja `docs/ARQUITETURA_REFATORACAO_0.1.53.md` e `docs/VALIDATION_0.1.53.md`.

## v0.1.52 (versionCode 53) · 30/10/2026
- **Animar Imagens:** controles padronizados e compactos; botão Salvar edição no cabeçalho para persistir fotos e configurações como rascunho local.
- Ao voltar, alterações não salvas geram confirmação; importações e reordenação preservadas. Voltar não permanece desativado após finalização.
- Tela de vídeo salvo reutilizada do editor principal, com Abrir, Compartilhar, Editar com áudio/legendas/PiP e Fechar; MP4 gerado continua sendo adicionado como clipe editável normal.
- Nova regra testável de alterações de rascunho e validação estrutural ampliada. Sem migração dos projetos existentes, sem dependências adicionais e sem envio de mídia à nuvem.
- Build Android e testes físicos ainda pendentes.

## v0.1.51 (versionCode 52) · 30/10/2026
- Animar Imagens: corrigido o NPE do `LinearProgressIndicator` removendo `progress!!` e usando percentual imutável limitado a 0–100%, barra indeterminada quando ausente.
- Prévia com área compacta de 238 dp e proporção preservada, cartões menores, nomes curtos e controles **Ajustes/Animar** lado a lado.
- Alça de arrastar reposiciona a foto durante o gesto e permite rolagem nas bordas, mantendo botões subir/descer/remover e ordem da galeria.
- Identidade Uai Cortes no progresso, mantendo Media3 apenas no código e nos diagnósticos.
- Novo teste unitário do progresso; compilação Android, exportação MP4 e testes de gesto no aparelho ainda pendentes.

## v0.1.50 (versionCode 51) · 30/10/2026
- **Animar Imagens offline** no menu inicial e nas ferramentas do editor: seleção múltipla na galeria, reordenação por arrasto e setas, movimentos de câmera automáticos, prévia e exportação MP4 H.264 com Media3.
- Ajustes opcionais de intensidade, duração (3/5/8s), formato (9:16/16:9/1:1), 720p/1080p e transições (corte/preto/branco/auto). Padrões: 5s, 9:16, 720p, 30 FPS.
- Salvamento no MediaStore, cancelamento, progresso quando disponível, posterior inclusão na timeline com áudio e efeitos; sem rede ou novo modelo.
- Segurança: cópias temporárias, fotos originais preservadas, sem alegação de animação corporal por IA ou paralaxe 2.5D. Veja `docs/ANIMAR_IMAGENS.md`.
- Compatibilidade: projetos antigos e schemaVersion 4 preservados. Compilação e testes de exportação no Android pendentes.

## v0.1.49 (versionCode 50) · 27/10/2026
- **Legendas automáticas offline (Vosk)**: nova tela acessível em Legendas → Auto; tutorial integrado, download opcional do modelo pequeno de português (~31 MB) no site oficial, decodificação de áudio dos vídeos, respeito a cortes e tempo de projeto, prévia/revisão individual antes de aplicar. Limite inicial de 10 minutos por operação; sem upload de áudio. Modelo extraído com proteção contra caminhos inválidos e limite de tamanho. A qualidade do reconhecimento depende da fala e do ruído; compilação e teste em Android ainda pendentes.
- **Transições de movimento**: Zoom cinematográfico, Recuo suave e Deslocamento lateral são efeitos de transformação por quadro Media3, acompanhados de prévia ilustrada. Não são crossfade nem dissolvência real entre clipes.
- **Projetos**: miniatura do primeiro quadro por projeto (com fallback) e confirmação explícita antes de excluir; vídeos originais não são apagados.
- Compatibilidade: schemaVersion 4 preservado; versionCode 50 e release dos cinco APKs pelo workflow. Não declarar APK/ASR testado até build real e teste em aparelho.

## v0.1.48 (versionCode 49) · 27/10/2026
- **Marca d'água em texto:** novo cartão no carrossel inferior do editor, com tela de edição própria, prévia do vídeo, texto até 80 caracteres, três cores, tamanho 12–36 e opacidade de 10% a 100%.
- Nove posições no quadro, usando coordenadas normalizadas do vídeo. **Aplicar** salva no projeto e volta ao editor; **Remover** apaga só a marca, sem tocar no texto das legendas ou nos arquivos originais.
- A marca ocupa a duração inteira do projeto, em camada separada das legendas SRT/animadas. A prévia do editor, a prévia de exportação e o Media3 Transformer receberam a mesma camada; projetos antigos sem `textWatermark` continuam válidos no schemaVersion 4.
- **Legendas automáticas:** estudadas as alternativas offline (whisper.cpp, Vosk) e online, mas transcrição automática ainda não foi implementada nesta versão. Exige decodificação de áudio, marcação temporal, escolha do modelo, revisão e controle de desempenho.
- Testes Kotlin isolados e validação estrutural incluídos. Compilação Android completa e conferência de MP4 em aparelho permanecem pendentes.

## v0.1.47 (versionCode 48) · 28/10/2026
- **Galeria interna:** corrigida a falha `The coroutine scope left the composition`. Corrotinas canceladas por troca de tela/permissão são propagadas e deixam de aparecer como erro de carregamento da mídia.
- Indicador de carregamento não é encerrado por consulta antiga cancelada; troca de permissões limpa o estado de erro. Em falhas reais, exibe **Tentar novamente** e **Procurar no Android**, sem mostrar caminhos de mídia ou detalhes internos.
- Falhas reais de consulta e permissão registradas no Diagnóstico (quando ativado), com mensagens genéricas na interface. O cabeçalho TXT passa a ler a versão real instalada, em vez de exibir sempre 0.1.41. Preservados filtros, miniaturas, seleção múltipla, SAF e projetos existentes.
- Diagnóstico TXT (cujo cabeçalho era fixo em 0.1.41, independentemente da versão instalada) também mostrou `SecurityException` no player do editor; esse incidente é distinto da falha de ciclo de vida da galeria. Pode exigir nova seleção da mídia e autorização no Android.
- Testes unitários de mensagens de erro, regressão estrutural de cancelamento, versionamento sequencial. Build Android e teste real de concessão/revogação de permissões permanecem pendentes.

## v0.1.46 (versionCode 47) · 27/10/2026
- Transições: nove opções, mantendo Corte direto, Fade preto e Fade branco e adicionando Flash branco, Flash preto, Fade azul, Fade dourado, Preto e branco e Sépia. Usam RgbMatrix do Media3 por quadro, sem sobrepor sequências de vídeo.
- Nova seleção de efeitos em faixa horizontal, mini prévia animada a partir de quadros dos dois clipes e duração ajustável para efeitos ativos.
- Escolhas são rascunhos até **Aplicar**. Botão inferior **Aplicar** persiste e retorna ao editor; **Aplicar a todas as junções** replica o efeito/duração nos pares adjacentes, sem alterar clipes ou mídia original.
- Sem migração do schemaVersion 4; testes de matrizes e aplicação em lote adicionados. Exportação e aparência física ainda exigem build CI e teste Android.

## v0.1.45 (versionCode 46) · 27/10/2026
- Exportação em tela cheia ao iniciar “Salvar vídeo”, com etapa Preparando/Processando/Salvando, nome do MP4, qualidade e cronômetro.
- Percentual vem de `Transformer.getProgress` somente quando disponível; indicador indeterminado nos demais estados. Sem ETA fictícia.
- Confirmação de cancelamento antes de descartar o MP4 parcial; o projeto e as mídias originais são preservados. Ao copiar para a galeria, o cancelamento é bloqueado para não interromper a gravação.
- Mantida a confirmação em tela cheia **Vídeo salvo!** somente após finalizar o MediaStore. Exportação não foi validada fisicamente neste ambiente.

## v0.1.44 (versionCode 45) · 27/10/2026
- Galeria interna MediaStore integrada às entradas de mídia visual: Novo projeto, Adicionar vídeo, PiP (adicionar/substituir) e vídeo externo para Extração de áudio.
- Filtros Recentes/Vídeos/Imagens conforme finalidade, busca por nome/pasta, pastas, miniaturas sob demanda, seleção múltipla ordenada para clipes e atualização da biblioteca.
- Permissões específicas por tipo, suporte ao acesso parcial do Android 14+, reavaliação ao retomar e seletor do Android preservado para fontes não indexadas. Sem cópia automática de arquivos grandes.
- Projetos continuam em schemaVersion 4; testes unitários de busca/filtros e validação das rotas de importação.

## v0.1.43 (versionCode 44) · 27/10/2026
- Corrigido erro de compilação do workflow **Gerar APK Uai Cortes 37**: `MainActivity.kt` referenciava `savedName` e `savedVideo` na confirmação de exportação sem declarar o estado no escopo de `ExportScreen`.
- Estados `remember(p.id)` adicionados antes do callback `onSuccess`, com `savedVideo` explicitamente tipado como `Uri?`; a confirmação em tela cheia, Abrir, Compartilhar, Voltar ao editor e Fechar permanecem.
- Validação estrutural ganhou regressão específica para essas declarações e sua ordem; sem mudanças nos projetos, legendas, motor Media3 ou formato do MP4. Compilação Android e teste físico continuam pendentes.

## v0.1.42 (versionCode 43) · 27/10/2026
- Exportação de legendas: `TimedSubtitleOverlay` respeita largura máxima de 90% do quadro, com quebras de linha automáticas e tamanho em proporção à largura real do vídeo, em vez de bitmap horizontal ilimitado. Preserve tamanho, posição, efeitos e tempos originais do projeto.
- `TextOverlay.getBitmap` substituído por StaticLayout com largura controlada e cache por quadro; legenda sem texto continua transparente, protegida contra imagem 0×0.
- Sucesso de exportação em tela cheia, somente após salvar no MediaStore, com Abrir, Compartilhar, Voltar ao editor e Fechar; falhas continuam detalhadas na tela de exportação.
- Não foi possível realizar exportação MP4 em aparelho neste ambiente; confirmar fidelidade da prévia e os efeitos no vídeo físico, vertical e horizontal.

## v0.1.41 (versionCode 42) · 21/10/2026
- Nova seção **Configurações > Diagnóstico**, desligada inicialmente e ativada manualmente, para capturar data e hora, tela ativa, toques aproximados (grade de 5%) e ações de navegação/salvamento.
- Captura falhas não tratadas, erros Media3 dos players, código/causa categorizada da exportação, extração de áudio e falhas ao salvar o relatório; logs internos limitados a 512 KiB com rotação.
- Visualização local, exportação TXT pelo seletor SAF, compartilhamento explícito pelo Android Sharesheet com FileProvider e exclusão com confirmação. Nenhum upload automático.
- Relatórios omitem textos digitados, legenda, nomes e URIs de mídia, IDs de projetos e conteúdo de arquivos; incluem modelo do aparelho/Android quando exportados. Modo não substitui o Logcat integral ou testes reais no dispositivo.
- Sem mudanças no schemaVersion 4, algoritmos de corte e codificação, projetos salvos e segredos. Build Android e testes físicos pendentes.

## v0.1.40 (versionCode 41) · 21/10/2026
- Corrigida provável causa do erro de exportação Media3 `ERROR_CODE_VIDEO_FRAME_PROCESSING_FAILED` com `width and height must be > 0`: `TimedSubtitleOverlay.getText()` retornava string vazia nos intervalos sem legenda e no primeiro frame de `Letra por letra`. O `TextOverlay` do Media3 tenta criar bitmap com largura zero nesse caso.
- A exportação usa caractere NBSP transparente de largura positiva quando nenhum texto deve aparecer, e define alpha 0 no overlay inativo; nenhum caractere adicional é gravado nos textos ou arquivos SRT.
- Acrescentados testes de regressão para legendas vazias, espaços/quebras de linha e texto visível. Mensagem de erro específica para falha de composição de quadro.
- Preservados corte, áudio, PiP, efeitos, projeto JSON schemaVersion 4 e o comportamento das telas. Compilação Android e exportação em aparelho ainda pendentes.

## v0.1.39 (versionCode 40) · 21/10/2026
- Corrigida configuração de duração de itens Media3 Transformer: não informar a duração *recortada* como se fosse a duração original da fonte em vídeos, PiPs e áudios. A biblioteca lê a duração intrínseca das mídias.
- Exportação: valida leitura das URIs locais e limites dos cortes antes do processamento, verificando espaço mínimo temporário; em caso de falha apresenta orientação mais específica, código Media3 e detalhe da causa quando disponível (também Logcat).
- Prévia Exportar: exibe legendas existentes com animações e estilo, camadas PiP e fades; anteriormente reproduzia somente o vídeo bruto, confundindo o usuário.
- A prévia ainda não garante fidelidade de áudio/mixagem; conferir o MP4 real depois do build e os códigos retornados no aparelho. "Asset loader error" sem código/causa não basta para diagnóstico definitivo.
- Mantidos projetos e esquema JSON 4, sem alterar arquivos originais. Compilação Gradle, exportação real no aparelho e conferência de legendas/PiPs pendentes.

## v0.1.38 (versionCode 39) · 21/10/2026
- Legendas: novo botão compacto **Salvar** na mesma faixa de **Nova** e **SRT**. Salva projeto editável e retorna à Home, não exporta MP4.
- Pop-up: efeitos de revelação lado a lado em carrossel circular horizontal (itens virtualizados), sem coluna longa; o efeito ativo mantém seleção.
- Áudio: seção **Mixagem** inicialmente recolhida para evitar controles de volume de todos os vídeos; cada faixa adicional exibe título, volume e edição em uma linha e abre o slider só quando solicitado.
- Exclusão de faixa de áudio agora pede confirmação e preserva o arquivo de origem; ao excluir uma faixa em edição o rascunho é fechado.
- Mantidos formato, volume e recorte reais já existentes; `schemaVersion=4` permanece. Validar UI, build Gradle, prévia e MP4 em aparelho.

## v0.1.37 (versionCode 38) · 21/10/2026
- Tela Legendas: editor em diálogo compacto com prévia ilustrativa de estilo, texto, início/fim lado a lado, botão Estilo, Animação, Excluir e Salvar.
- Rascunho local: alterações no pop-up apenas são persistidas ao tocar em Salvar. Cancelar descarta as mudanças do diálogo.
- Novas legendas manuais e de SRT usam tamanho inicial de 12 sp; projeto legado sem atributo continua sendo lido com 22 sp para preservar aparência histórica.
- Aplicar a todas permite escolher aparência, posição e animação de forma independente, com confirmação antes da gravação; não altera textos, horários nem IDs.
- Animações offline: letra por letra, frase por frase, destaque progressivo de palavras, pop/zoom, fade e deslizar, com tempo compartilhado entre prévia Compose e exportação Media3. A sincronização é temporal aproximada dentro de cada legenda, não acompanha a fala automaticamente.
- Preserva schemaVersion 4; as novas propriedades opcionais de animação usam defaults compatíveis. Build Android, testes em dispositivo e fidelidade exata prévia/MP4 ainda pendentes.

## v0.1.36 (versionCode 37) · 20/10/2026
- Correção de compilação do workflow "Gerar APK Uai Cortes 31": `searchQuery` era acessada no callback de importação SRT antes de ser declarada em `SubtitleScreen`; a declaração do estado foi movida para antes do importador.
- Adicionada verificação estrutural específica para evitar regressão de escopo/ordem no importador SRT. Preservados busca, legendas, prévia adaptativa, exportação e `schemaVersion=4`.
- Logs confirmaram falha em `:app:compileDebugKotlin` e `:app:compileReleaseKotlin` na linha 2926, após a validação estrutural aprovada. Novo build Android, testes e APKs pendentes de execução no GitHub Actions.

## v0.1.35 (versionCode 36) · 20/10/2026
- Prévia Media3 com dimensões proporcionais ao vídeo em Legendas, Cortar e unir, PiP e Exportar. A altura usa limite compatível com a tela, sem barras pretas externas artificiais ou esticar o arquivo.
- Editor principal, PiP e Exportar acompanham a mudança de proporção entre clipes durante a reprodução.
- Legendas: quebra de palavra mitigada medindo a maior palavra e ajustando fonte apenas na prévia; largura e posição do bloco respeitam os limites do quadro real.
- Conteúdo e estilo persistido da legenda não se alteram; schemaVersion 4, comportamento do exportador, SRT e mídia originais preservados.
- Adicionado `AdaptiveVideoLayout.kt` e testes de dimensões/aspect ratio em `AdaptiveVideoLayoutTest.kt`. Compilação Android e verificação de fidelidade no MP4 ainda pendentes.

## v0.1.34 (versionCode 35) · 20/10/2026
- Legendas: busca rápida de texto independente de maiúsculas e acentos, contagem do resultado e indicação clara de busca sem correspondências. A numeração permanece a da lista completa.
- Prévia fixa ganhou botões de navegação para legenda anterior / seguinte, na ordem temporal, com posição do vídeo sincronizada e sem salto circular involuntário.
- Excluir legenda requer confirmação; Cancelar mantém legenda, texto e horários. Após excluir, a seleção avança para um item existente.
- Nova legenda respeita duração do vídeo mesmo no último milissegundo; caso vídeo esteja vazio, o botão de criação é desabilitado.
- Importador SRT rejeita arquivos acima de 250 mil caracteres, evitando importação silenciosamente incompleta.
- Adicionados helper puro `SubtitleListNavigation.kt`, testes JUnit, checks estruturais e documentação. `schemaVersion=4` mantido; compilação Android e testes físicos pendentes.

## v0.1.33 (versionCode 34) · 20/10/2026
- Legendas: prévia do vídeo fixa no topo, com reprodução Media3 restrita ao trecho selecionado e posicionamento por arrasto no próprio quadro.
- Timeline única, horizontal e virtualizada por blocos de 10 segundos. Indicadores utilizam intervalos absolutos, inclusive quando legendas se sobrepõem.
- Lista compacta LazyColumn: só uma legenda expande por vez, com texto, tempos numéricos e ajuste de alças; estilos e posição avançada recolhidos inicialmente.
- Importar SRT, prévia/exportação, IDs e schema 4 continuam compatíveis. Acrescentados testes de janelas temporais.
- Verificações locais de sintaxe/estrutura e lógica; build Android, lint e testes visuais em aparelho pendentes.

## v0.1.32 (versionCode 33) · 20/10/2026
- Cortar e unir: faixa horizontal de trechos com uma miniatura carregada sob demanda por clipe, parte numerada, horários e borda azul na seleção. Toque no cartão para selecionar e rolagem automática até a parte ativa.
- Dividir agora seleciona automaticamente a parte à direita, posicionando o cursor na divisão em milissegundos da fonte. O ponto e ID do trecho ficam gravados nos dados opcionais do projeto, restaurados ao reabrir, sem mudar `schemaVersion=4`.
- Excluir escolhe o trecho vizinho; Desfazer restaura também os dados do último corte; ao dividir, as transições já presentes na fronteira original são preservadas na nova fronteira externa.
- Testes JUnit `TrimTimelineEditTest` para sequência de cortes, seleção, exclusão, transições, JSON legado e pontos inválidos. Compilação Android e inspeção visual em celular continuam pendentes.
- Gradle, workflow, script de cinco APKs, verificador estrutural e tela Sobre sincronizados em 0.1.32 / 33.

## v0.1.31 (versionCode 32) · 20/10/2026
- Ação “Salvar e voltar à Home” retirada de acima das trilhas e convertida em cartão **Salvar** no carrossel de ferramentas do editor.
- Barra inferior com rolagem horizontal cíclica, acessível à esquerda e à direita. Cartões de Cortar, Unir, PiP, Áudio, Transição e Legendas preservados.
- Em Cortar e unir, **Salvar** aparece após Excluir na faixa horizontal rolável, persistindo o corte pendente quando válido e retornando ao início. Erros de intervalo não fazem perder a edição.
- `TrimSaveLogic` criada para tornar a aplicação do corte testável; adicionado `TrimSaveLogicTest` com cenários válidos, inválidos e inalterados.
- `versionName=0.1.31` e `versionCode=32` alinhados ao workflow, script de empacotamento, Sobre e verificador estrutural.
- Sem exportação automática. Gradle/Android CI e validação real de interface ainda pendentes.

## v0.1.30 (versionCode 31) · 19/10/2026
- Corrigido erro real de compilação do workflow 26: `Unresolved reference 'PathJoin'` em `MainActivity.kt:53/2594`. Import e chamada substituídos por `androidx.compose.ui.graphics.StrokeJoin` / `StrokeJoin.Round`.
- Mantidos os efeitos de contorno existentes e todas as funcionalidades da v0.1.29, sem alterar o esquema do projeto.
- Gradle, workflow, script dos cinco APKs, Sobre, verificações estruturais e documentação sincronizados com v0.1.30/versionCode 31.
- Validações locais não substituem `:app:testDebugUnitTest`, `:app:lintDebug` e `:app:assembleRelease`, ainda pendentes em CI.

## v0.1.29 (versionCode 30) · 19/10/2026
- Editor de legendas: sombra de deslocamento diagonal ajustável de 0–6 dp, com opção preta/branca. 0 desativa a sombra.
- Espaçamento adicional entre linhas de 100–160% e alinhamento de parágrafos à esquerda, centralizado ou à direita, preservando projetos antigos.
- Prévia Compose e exportador Media3 atualizados com renderização de sombra por `LineBackgroundSpan`, espaçamento por `LineHeightSpan` e alinhamento por `AlignmentSpan`.
- Persistência JSON por legenda, normalização conservadora de campos e cópia de aparência para todas sem alterar conteúdo, IDs, horários ou coordenadas.
- Testes unitários `SubtitlePresentationTest` incluídos. Build Android completo e comparação visual com MP4 continuam pendentes.

## v0.1.28 (versionCode 29) · 19/10/2026
- Editor de legendas: fonte Moderna, Serifada ou Monoespaçada e contorno ajustável (0–4 dp), preto ou branco.
- Prévia Compose atualizada com traço por baixo do preenchimento e renderização Media3 estendida com `TypefaceSpan` e `LineBackgroundSpan` para contorno por linha.
- Persistência individual em `SubtitleCue`, leitura tolerante a projetos legados e cópia de aparência para todas as legendas sem modificar intervalos e conteúdo.
- Novo `SubtitleTypographyTest`; testes Kotlin/Gradle e comparação visual com MP4 no aparelho ainda pendentes.

## v0.1.27 (versionCode 28) · 19/10/2026
- Adicionados cinco estilos de legenda: Clássico, Destaque, Ciano, Limpo e Etiqueta. A escolha altera apenas a aparência da legenda selecionada.
- Seletor de fundo (sem fundo, suave, escuro ou claro) e controle de negrito, com atualização imediata das prévias no editor e sobre o quadro do vídeo.
- Exportação Media3 usa a cor de fundo individual e a opção de negrito via `SpannableString`/`TextOverlay`, sem alterar tempos, IDs, X/Y ou o arquivo original.
- Botão de copiar aparência para todas as legendas, com confirmação e preservação de texto, posição e intervalos.
- Modelo `SubtitleCue` inclui `backgroundColor` e `bold` opcionais na leitura de JSON, mantendo o fundo escuro e o negrito de projetos anteriores. Schema continua v4.
- Novo `SubtitleStylesTest` com verificações de presets, cópia e leitura JSON legada. Build e renderização real no Android pendentes.

## v0.1.26 (versionCode 27) · 19/10/2026
- Tela principal: blocos individuais na pista de legendas abrem diretamente a legenda selecionada por ID.
- Tela Legendas: faixa individual por texto, inclusive com sobreposição temporal; alça esquerda ajusta início, centro desloca mantendo duração e alça direita ajusta fim.
- Rascunho visual durante o arrasto e persistência apenas ao soltar, preservando texto, X/Y, estilo e JSON legado.
- Limites de projeto e proteção de duração mínima de 100 ms para gestos em trechos normais, respeitando legendas SRT antigas menores que isso.
- Adicionados `SubtitleTimelineEdit` e testes unitários de início, fim, deslocamento, overflow e trechos curtos. Build Android e testes físicos pendentes.

## v0.1.25 (versionCode 26) · 19/10/2026
- Melhorada a precisão de navegação no editor com passos de 100 ms e 1 s, limitados ao intervalo do projeto.
- Legendas posicionadas proporcionalmente ao tamanho do vídeo, sem deslocamentos fixos em dp. Prévia usa a âncora inferior central do exportador.
- Nova miniatura da legenda com arrasto e setas para ajustes de posição de 1%; aplica a posição ao concluir o gesto e mantém sliders existentes.
- Seleção de quadro do clipe correspondente ao tempo da legenda, sem alterar mídia original nem a estrutura JSON.
- Incluídos testes `TimelinePrecisionTest`; compilação Android e fidelidade da exportação ainda pendentes.

## v0.1.24 (versionCode 25) · 19/10/2026
- Timeline principal de áudio agora permite arrastar horizontalmente as faixas extras para alterar o início no projeto, persistindo só ao concluir o gesto.
- Toque em uma faixa seleciona e abre o editor de áudio diretamente nela.
- Editor de áudio com alças de recorte visual: esquerda (ajusta origem e tempo de entrada), centro (move), direita (ajusta fim do arquivo). Prévia dos valores até Salvar corte, preservando Cancelar.
- Lógica `AudioTimelineEdit` com testes Kotlin de limites do arquivo/projeto, duração mínima e conversão pixels/tempo.
- Compatibilidade JSON preservada; áudio legado e exportação não foram modificados nesta versão.
- Verificações locais não substituem Gradle/Android e testes em aparelho.

## v0.1.23 (versionCode 24) · 19/10/2026

- Faixas adicionais com entrada e saída no áudio de origem, posição na timeline, controles numéricos/deslizantes e pré-escuta separada.
- Exportação Media3 e prévia do editor respeitam `sourceStartMs`; projetos antigos permanecem compatíveis com origem em 0 ms.
- Limites de corte conservadores (mínimo de 250 ms) e testes de lógica/serialização adicionados.
- ZIP/validação estrutural: a compilação Android e teste físico desta versão continuam pendentes.

## v0.1.22 (versionCode 23) — 19/10/2026

- Pré-escuta de áudio para clipes individuais e vídeos externos, com reprodução, pausa, reinício e limite de recorte.
- Reprodução temporária isolada da timeline e liberação do player ao sair da tela.
- Pré-escuta de projeto inteiro ainda não implementada. Build Android e testes em aparelho pendentes.

## v0.1.22 (versionCode 23) — 19/10/2026

- Prévia animada de transições usando quadros reais extraídos dos clipes adjacentes, com Fade preto, Fade branco e Corte direto.
- Extração de quadros em IO, sem decodificação contínua ou áudio. A prévia não substitui reprodução/renderização integral.
- Mantidas as cinco variantes de APK e a exportação Media3 existente.
- Build Android e testes em aparelho pendentes.

Correção v0.1.22: o workflow 19 compilou Kotlin Debug/Release, executou testes e lint com sucesso, mas falhou no empacotamento porque tools/package_apks.py ainda esperava 0.1.17/versionCode 17. Constantes sincronizadas para 0.1.22/versionCode 23. Compilação e publicação da nova versão pendentes.

## 0.1.22 (versionCode 23) — 19/10/2026
- Corrigida compilação Kotlin do build 18: chamada de `TimelineRow` para a faixa PiP vazia agora passa explicitamente `onClip`.
- Nenhuma alteração na lógica de exportação ou transições.

Implementação incremental: indicadores de transição clicáveis na timeline, seleção por cartões e botão salvar e voltar à Home. Prévia real de transições ainda pendente.

## 0.1.18 (versionCode 19) — 19/10/2026
- Extração de vídeo externo via SAF e recorte por início/fim.
- Posição X/Y de legendas persistida no projeto e aplicada à exportação.
- Implementação parcial: prévia de transições, indicadores na timeline e menu de ações ainda pendentes.

## 0.1.16 (versionCode 17) — 19/10/2026
- Correção do Lint do build 16: configuração instável do `PlayerView` isolada em função com opt-in AndroidX explícito; removidas chamadas Media3 instáveis de lambdas Compose.
- Configuração de build ABI splits `armeabi-v7a`, `arm64-v8a`, `x86`, `x86_64` e universal, sem criar variantes Debug para distribuição.
- Correção de distribuição duplicada: o workflow não escuta mais tags criadas na publicação nem publica o mesmo APK como Actions Artifact. Uma Release com cinco APKs nomeados.
- Validação automatizada de cinco saídas Release, metadados de ABI, assinatura, pacote, targetSdk, versionName e versionCode. Atualizados docs e tela Sobre.
- APK/Android Lint ainda dependem de execução no GitHub Actions; testes físicos por arquitetura pendentes.

## 0.1.15 (versionCode 16) — 19/10/2026

- Corrigido build 15: `ModalBottomSheet` e `rememberModalBottomSheetState` exigem `@kotlin.OptIn(ExperimentalMaterial3Api::class)` nos componentes da edição principal e PiP.
- Captura explícita de `maxWidth` no `BoxWithConstraints` de `TimelineRow` para evitar erro Kotlin causado pelo `Row` aninhado.
- Metadados, CI e documentação sincronizados. Nenhuma funcionalidade de edição removida.
- Compilação Android completa permanece pendente.

## 0.1.14 (versionCode 15) — 19/10/2026
- Alinhamento unificado das faixas da timeline (rótulo de 60 dp, margem de 12 dp); blocos retangulares.
- Toque em clipe abre Bottom Sheet compacto: cortar, duplicar, mover, excluir com confirmação e volume individual; prévia passa a seguir volume individual.
- Legendas manuais e importação SRT: campo de texto, horários, cor, tamanho e posição; faixa própria e efeito temporal no exportador.
- Transições independentes por par adjacente: corte direto, fade preto, fade branco (0,25 a 1 s); sem falsa dissolvência ou alteração de duração.
- Áudio: faixas independentes, ajuste de volume e remoção, prévia; extração de áudio do projeto ou clipe em M4A ou MP4 contendo AAC, salvando no SAF, opção salvar e acrescentar à timeline.
- Projeto JSON schemaVersion 4 com migração de campos anteriores e testes de SRT, duração e fades.
- Validações locais de sintaxe e cálculos; build Android, exportação múltipla, mixagem, efeitos GPU e aparelho ainda pendentes.

## 0.1.13 (versionCode 14) — 18/10/2026
- Reprodução em loop automático até pausar: REPEAT_MODE_ONE no clipe de Cortar e unir e REPEAT_MODE_ALL na playlist do editor principal.
- Seletor compacto de velocidade em ambas as telas (0,25× a 2×), alterando somente a prévia; vídeo exportado preserva velocidade original.
- PiPs na prévia do editor recebem a mesma velocidade do vídeo principal, para limitar defasagem durante reprodução acelerada ou lenta.
- Mini tutorial de cinco orientações abaixo dos controles Cortar e unir, incluindo o uso das ferramentas e a diferença entre prévia e exportação.
- Testes novos do catálogo de velocidades, documentação e workflow atualizados. Build Android e teste físico pendentes.

## 0.1.12 (versionCode 13) — 18/10/2026
- Cortar e unir simplificado: miniaturas clicáveis diretamente abaixo da prévia, sem marcador duplo sobre as imagens; único controle fino de cursor separado.
- Campo Tempo move o cursor ao digitar números válidos, com a máscara `mm:ss.d` preservada; sem campo “Ir para”/botões adicionais.
- Início/fim editáveis diretamente, botões de marcação sempre clicáveis com explicação quando o fim não pode ser definido (mínimo 250 ms).
- Dividir, Desfazer, mover antes/depois e Excluir agrupados na mesma linha compacta; desfazer de uma operação também cobre divisão, reordenação e exclusão.
- Mantidos quatro PiPs e edição sem modificar os arquivos originais. Pendente build Gradle e teste físico.

## 0.1.11 (versionCode 12) — 18/10/2026
- Editor Cortar e unir com preview ExoPlayer do clipe, controles de reprodução/seek de 1 segundo e miniaturas amostradas em IO.
- Linha do tempo compacta com marcadores RangeSlider para início/fim e cursor independente; ações Marcar início/Marcar fim; edição em rascunho com Aplicar corte, Desfazer última aplicação, Dividir, Reordenar e Excluir.
- Entrada de horários numérica autoformatada `mm:ss.d` com controle de limite, compartilhada pela busca na prévia e editor PiP.
- Mantidos projeto não destrutivo, múltiplos PiPs e compatibilidade com projetos anteriores. Workflow e documentação sincronizados. Build Android/teste em aparelho pendentes.

## 0.1.10 (versionCode 11) — 18/10/2026
- Bottom Sheet sem alça superior, mantendo botão Fechar; menos margens e espaços na tela PiP e no painel inferior.
- Horários tocáveis: campo digitável de busca na prévia e editor principal; início/fim do PiP com valor digitável, validação e barra preservada.
- Novo PiP começa após o último término existente, sem usar o cursor do vídeo; quando o último já alcançou o fim do projeto, exige confirmação para adicionar em período coincidente.
- Conflitos ESPAÇO+TEMPO têm aviso e opções Reposicionar automaticamente, Manter sobreposição ou Cancelar. Permissões de sobreposição ficam registradas por par de IDs no JSON (schema 3), sem liberar conflitos com futuros PiPs.
- Mantidos quatro PiPs, controles separados de áudio e exportação Media3. Compilação Android e testes no aparelho continuam pendentes.

## 0.1.10 (versionCode 11) — 18/10/2026
- Reorganizadas as ferramentas Posição, Tamanho, Duração e Mídia em **uma única linha** com quatro botões quadrados compactos e ícones.
- Os ajustes agora abrem em **ModalBottomSheet** na borda inferior; aparecem sobre os controles, sem empurrar o vídeo. Altura limitada, rolagem interna, tema do app e fechamento por gesto, toque fora ou botão.
- Mantidos os controles completos (9 posições, tamanho 5–90%/manual, duração e mídia), seleção de até quatro PiPs, áudio independente, anticolisão e compatibilidade de projetos.
- Sincronizados Gradle, tela Sobre, workflow de APK, README, AGENTS, release e validações. Compilação Android e testes físicos pendentes.

## 0.1.8 (versionCode 9) — 18/10/2026
- PiPs múltiplos (até 4) em um projeto, com seleção e edição independentes.
- Interface quadrada expansível na própria tela (Posição/Tamanho/Duração/Mídia); removidos os pop-ups de configuração.
- Motor de colisões espacial e temporal com prioridade para o PiP editado e aviso quando não houver espaço livre.
- Linha do tempo dividida em uma faixa por PiP, posição de início e duração corretas.
- Prévia e composição Media3 das até quatro fontes visuais; áudio independente para cada vídeo PiP.
- Modelo JSON `overlays` e `schemaVersion=2`; leitura automática do campo `pip` dos projetos antigos.
- Documentação/workflow atualizados para 0.1.8 e versionCode 9; APK ainda depende de compilação CI.

## 0.1.7 (versionCode 8) — 18/10/2026
- Corrigida compilação Debug/Release relatada no workflow 7: `PrimaryTabRow` do Material 3 requer `@OptIn(ExperimentalMaterial3Api::class)` na `DeviceMediaBrowser`.
- Preservadas biblioteca de mídia PiP, controles de posição/tamanho/duração e áudio individual do PiP.
- Versionamento e publicação APK sincronizados (Gradle, tela Sobre, workflow, README, release, validador).
- Ainda requer validação de build Android completo no GitHub Actions e testes físicos.

## 0.1.6 (versionCode 7) — 18/10/2026
- Menu compacto para PiP: janelas de Posição (9 atalhos), Tamanho (5% a 90%, slider e entrada manual) e Duração (início/fim e faixa).
- Nova biblioteca local de mídia: abas Imagens/Vídeos, filtros por pasta, miniaturas em grade com espaço reduzido, ícone e tempo dos vídeos. Usa MediaStore, permissões Android e alternativa pelo seletor de arquivos.
- Áudio de vídeo PiP em cartão próprio: silenciar/ativar sem perder volume escolhido; o estado é persistido e respeitado na prévia e na exportação.
- Ajustes de geometria para 5%, testes para entrada manual e serialização do estado mudo, documentação e workflow sequenciais.
- Compilação Android e verificação em aparelho pendentes.

## 0.1.5 (versionCode 6) — 18/10/2026
- PiP com visualização do vídeo principal, posição rápida 3×3, exibição proporcional e tempos de início/fim.
- Corrigida a posição central e limites da área de arrasto, guias centrais e prévia reproduzível.
- Adicionados testes Kotlin da geometria e da janela temporal.
- Workflow e documentação atualizados para UaiCortes-v0.1.5.apk.

# Histórico de versões

## 0.1.3 (versionCode 4)
- Opt-in pontual do Media3 UnstableApi no indicador de buffering da prévia, corrigindo falha Lint #3; sem ocultar erros do Lint.

## 0.1.2 (versionCode 3)
- Correção dos imports Media3 no motor de exportação e validador estático.

## 0.1.1 (versionCode 2)
- Atualização do setup Android para remover pacote SDK legado.

## 0.1.0 (versionCode 1)
- Base nativa do Uai Cortes e telas iniciais do editor.
