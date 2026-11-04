# Uai Cortes · versão 0.1.132 (04/11/2026)


## Correção do workflow 110 · v0.1.132

- Compilação Debug/Release: import `horizontalScroll` restaurado ao painel Gemini e retorno de `countTokens` marcado com `return@withContext`.
- Contrato CI110 previne regressões; histórico, estimativas, failover e proteção das chaves mantidos. Compilação Android ainda requer validação em CI.

## Monitor Gemini de tokens · v0.1.131

O painel Configurações > Google Gemini > Consumo da IA registra somente consumo **informado pela API** e tentativas locais, com consulta por projeto/modelo e opção Limpar histórico. Antes de descrição, Flow, narração ou legendas, mostra aviso simples e previsão limitada. Em descrição e Flow, `countTokens` é opcional por toque. Isso não oferece acesso ao saldo de créditos nem calcula preços de faturamento automaticamente. Não transmite dados de mídia fora das gerações explicitamente autorizadas. Ver `docs/GEMINI_USAGE_MONITORING.md`.

## Recursos da v0.1.130

- **Múltiplas chaves Gemini:** até oito credenciais AES-GCM/Android Keystore, cada uma com nome, identificador do projeto Google Cloud, seleção de ativa, edição e exclusão confirmada.
- **Alternância opcional:** após erro HTTP 429 RESOURCE_EXHAUSTED, pausa o projeto por dez minutos e tenta, no máximo uma vez por outro projeto identificado e não pausado, sem retentar erro de cobrança, autenticação ou indisponibilidade. Exige consentimento explícito.
- **APIs conectadas:** narração TTS, transcrição de legendas, geração de descrições e refinamento de cenas Flow utilizam o mesmo gerenciador; a operação autorizada pelo usuário não roda em segundo plano.
- **Limites e segurança:** as cotas são por projeto Google Cloud e não por chave; cadastrar duas chaves do mesmo projeto NÃO duplica cotas. O app não cria contas nem projetos e não garante créditos. IDs são informados pelo usuário e não são comprovados automaticamente; usar apenas projetos autorizados, respeitando termos e orçamento. O backup do Android exclui credenciais.
- **Compatibilidade:** chave única anterior preservada; nenhuma modificação nos vídeos, projetos ou sistema Media3. Faltam Gradle/Android CI e teste com projetos reais distintos antes de declarar a alternância operacional.


A Home ganhou três cartões azuis compactos, com ícones e funções independentes: **Novo projeto**, **Animar fotos** e **Prompts Flow**. Os três ficam lado a lado, na mesma linha, sem tela intermediária. O cabeçalho foi reduzido e as edições recentes têm um estado vazio explicativo e um atalho Ver todos quando há projetos. A prévia em Sobre continua reutilizando os mesmos componentes, com cliques desabilitados. Não altera o motor de vídeo, armazenamento, schemaVersion=4, assinatura ou as cinco saídas APK do CI. Build Android e validação visual em aparelho ainda pendentes.

## Versão 0.1.128 · versionCode 129 (04/11/2026)

A exportação agora usa controles compactos com ícones e ações rápidas para destino, 9:16, resolução e compressão. A prévia local foi mantida em painel expansível com guias opcionais para legenda e elementos de interface; há exemplos visuais de Mostrar tudo vs Preencher. O resumo estimado e Salvar ficam no rodapé fixo. Sem alterações no motor Media3, projeto JSON, assinatura ou formatos. Ainda exige build Android e teste físico para confirmar a composição visual.


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

- Corrigido o seletor Gemini para não confirmar uma voz previamente marcada quando ela estiver oculta pelo filtro Femininas/Masculinas ou pela pesquisa. Trocar o filtro limpa uma escolha incompatível; somente uma voz visível pode ser confirmada.
- O círculo de seleção (radio) também responde ao toque, assim como a linha, e o cartão indica claramente a escolha.
- Botão **Ouvir voz marcada** no seletor permite escutar a voz escolhida antes de confirmar. Usa cache local quando disponível; caso contrário exige confirmação explícita de possível consumo de cota antes de chamar o Gemini. A prévia não altera a seleção confirmada, nem cria faixa de áudio.
- Confirmar voz mantém o comportamento anterior: retorna ao editor de vídeo e retoma a prévia sem gerar narração automaticamente. A geração da narração e a inclusão na timeline continuam exigindo confirmações independentes.
- Limite de validação: testes Kotlin isolados e checagens estáticas não garantem timbre feminino no áudio real; requer teste com chave Gemini no Android e build no GitHub Actions.

## Versão 0.1.106 · versionCode 107 (02/11/2026)

- O catálogo de vozes do Gemini foi redesenhado: cartões compactos com nome legível, resumo curto em português, campo de busca e filtros Todas/Femininas/Masculinas baseados somente nos metadados recebidos da API.
- Nome original, ID, descrição em inglês e metadados detalhados aparecem somente em **Detalhes**, com expansão sob demanda; outras vozes continuam acessíveis pela rolagem.
- Ao abrir Escolher voz, a janela de narração fica temporariamente oculta, evitando dois diálogos sobrepostos. Cancelar volta à narração sem aplicar escolha; Confirmar voz retorna ao editor e retoma o vídeo sem sintetizar automaticamente.
- Preservados seleção explícita, cache de amostras, reutilização de legendas, geração com confirmação, atualizações pelo GitHub e ferramentas de Animar Imagens. Teste real no Android pendente.

## Versão 0.1.105 · versionCode 106 (02/11/2026)

- **Animar Imagens:** prévia principal agora decodifica a imagem real via MediaStore/SAF, preservando proporção e orientação EXIF, em vez de usar miniatura quadrada solicitada em 640 px.
- Painel principal mais alto especialmente em 9:16; botão **Ampliar** oferece inspeção maior do enquadramento com o mesmo movimento e composição do editor.
- Seletor **Nitidez:** Rápida (720), Equilibrada (1280) e Alta (1920 pixels no lado maior), sem interpolar imagens originais pequenas e sem modificar opções do MP4 exportado.
- Memória limitada por amostragem, leitura assíncrona e bitmap único por foto; testes estruturais não substituem avaliação física de fluidez e nitidez no aparelho.

## Versão 0.1.104 · versionCode 105 (02/11/2026)

- Em **Atualizações pelo GitHub**, falhas antes ocultas agora aparecem com categorias concretas (DNS, rede, SSL, HTTP e JSON). O app preserva a privacidade: não mostra tokens, dados da requisição ou caminhos internos.
- **Abrir Releases no navegador** permite verificar manualmente o acesso ao repositório configurado. Repositórios privados não são consultáveis anonimamente pela API.
- Atualização automática significa apenas **verificação**; download e instalação continuam sob confirmação.

## Versão 0.1.103 · versionCode 104 (02/11/2026)

- Confirmar uma voz no catálogo agora fecha a janela de narração e retorna à prévia do vídeo no editor, retomando a reprodução no cursor existente.
- O rascunho da narração (texto, voz, catálogo, modelo e estilo) permanece em memória enquanto o mesmo projeto estiver aberto; tocar em confirmar voz não gera áudio nem adiciona trilha ao vídeo.
- Lista de vozes usa linhas clicáveis com botão de seleção, evitando cortes de texto da interface anterior. Nenhuma credencial ou áudio é persistido no rascunho.
- Continua necessário voltar em Áudio → Gerar por IA, confirmar a geração e confirmar a inclusão na timeline para ouvir a voz nova no vídeo. Não há cópia de áudio sem confirmação.
- Testes locais e validações do ZIP não substituem build Android nem teste no aparelho.

## Versão 0.1.102 · versionCode 103 (02/11/2026)

- Diagnóstico explícito na janela Narração com IA: consultar/diagnosticar vozes só por toque, indicar filtro pt-BR, fallback e falhas HTTP/SSL/rede sem mostrar a chave ou legendas. Busca geral após filtro vazio ou rejeição HTTP 400/422; erros de autenticação/cota não disparam retry desnecessário.
- Novos testes automatizados do catálogo com respostas simuladas (execução completa no Gradle pendente; 9 verificações Kotlin isoladas passaram). A consulta não sintetiza áudio nem escolhe voz. Requer compilar no GitHub Actions e testar com a chave real para identificar a causa específica no aparelho.

## Versão 0.1.101 · versionCode 102 (02/11/2026)

- Corrige os dois testes do workflow 85: `HomeAboutVersionContractTest` agora compara Sobre/Novidades com `BuildConfig` em vez de número de versão fixo; a documentação da prévia da Home é buscada no histórico, não apenas no release corrente.
- Testes de sincronização de metadados e verificações estáticas passam a aproveitar os valores atuais do Gradle, evitando versões antigas em asserts. Consulta de atualizações pelo GitHub e confirmações permanecem como na v0.1.100.
- Fonte preparada; a compilação completa Android e o teste real de atualização precisam ser executados no GitHub Actions/aparelho.

## Versão 0.1.100 · versionCode 101 (02/11/2026)

- **Atualizações GitHub Releases**: consulta online ao iniciar o aplicativo ou ao voltar para primeiro plano (com intervalo mínimo de um minuto para evitar excesso de chamadas), inclusive pré-releases, sem download automático. Se houver versão mais recente com APK universal, mostra diálogo; em Configurações → Atualizações pelo GitHub há consulta manual, dados da Release e confirmação para baixar e instalar.
- **Origem confiável**: builds feitos no GitHub Actions recebem `GITHUB_REPOSITORY` automaticamente; builds locais podem configurar um repositório público manualmente. Nenhuma chave GitHub é exigida, não há consulta sem repositório configurado.
- **Segurança**: HTTPS, apenas asset universal oficial, SHA-256 da API, versão superior/mesmo pacote e instalação via Android com confirmação. Certificado precisa ser o mesmo do APK anterior. **CI bloqueia Releases sem os quatro Secrets de assinatura persistente** (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`). Sem digest não faz download interno.

## Versão 0.1.99 · versionCode 100 (01/11/2026)

- Tela **Sobre**: teste visual da Home com os **mesmos componentes** de Criar projeto e Animar Imagens, mas ações desabilitadas para não alterar dados. Home e Sobre exibem versão pela mesma fonte `AppReleaseNotes`.
- Identificação para o **GitHub Manager**: `github-manager.json`, `app_identity.json` e `VERSION` na raiz. O validador bloqueia versões divergentes antes da publicação.
- Teste Android instrumentado de Home/Sobre adicionado; depende de emulador/aparelho para execução real.

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

## v0.1.91 — Galeria horizontal, presets animados e cache de áudio

- Opções visuais de legendas organizadas em rolagem horizontal finita, inclusive cores, fontes, alinhamentos e animações. Galeria de 12 cartões compactos animados.
- Escolha de modelo também define uma animação real; adicionados Bounce, Pulsar e Zoom reverso à prévia e exportação baseadas em SubtitleAnimations.frame.
- Novas transcrições Vosk/Gemini usam tamanho inicial de 20 sp em lugar de 30 sp. Controle 12–30 sp na revisão antes de aplicar; legendas antigas inalteradas.
- Áudio WAV reutilizável em cache privado para chamadas Gemini equivalentes, com hash de clipes/cortes, verificação de comprimento, retenção máxima de 3 horas e limpeza limitada. Nova chamada de API continua necessária.
- Ajuda de posicionamento movida para ícone na linha do tempo; menos texto persistente na tela.
- Build Gradle/Android e testes de exportação e API em dispositivo pendentes.

## v0.1.90 — Ferramentas e IA nas legendas

- Corrigida regressão do `SubtitlePresentationTest`: presets sem configuração explícita de sombra preservam o valor personalizado.
- Barra de ferramentas compacta, cantos de 8 dp, exportação SRT/TXT, e exclusão em lote com confirmação e desfazer.
- Reconhecimento Gemini 3.5 Transcribe opt-in, usando chave já cadastrada; extrai somente áudio temporário dos recortes do projeto, recebe horários por palavra, revisão antes de aplicar.
- Modo offline Vosk preservado; sem envio automático, respeitando limites/cota e `schemaVersion=4`.
- Build completo e testes de aparelho pendentes.

## v0.1.89 — Legendas expressivas e navegação

- Galeria de 12 presets com miniaturas, contorno, sombra, fontes nativas, itálico e cores de destaque.
- Escolha de cor por palavra; texto e cores persistem sem rede e sem afetar mídias originais.
- Novas legendas automáticas do Vosk preservam tempos reais das palavras para karaokê; legendas antigas continuam com aproximação temporal.
- Prévia Compose e MP4 Media3 aplicam a mesma regra de seleção de palavra e as cores de cada trecho.
- Voltar em Animar Imagens leva à Home, mesmo quando aberto pelo editor; mudanças não salvas continuam exigindo confirmação.
- `schemaVersion` 4 inalterado. Compilação Android e fidelidade visual aguardam execução no GitHub Actions/dispositivo.

## v0.1.88 — Salvamento e segurança

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

- **Configurações:** Gemini é um item clicável igual às demais categorias. Página dedicada para chave criptografada, seis ações em grade de duas colunas, campo com olho e painel de resultado do teste, sem expor a credencial salva.
- **Editor > Áudio:** botão **Gerar por IA** abre um pop-up com editor de narração, modelo, voz, estilo, geração, prévia, player e adição à timeline. O formulário não ocupa mais a tela de áudio.
- **Vozes:** catálogo retornado pela API pesquisável por nome ou ID, sem exibir descrições longas em inglês na tela principal. Os códigos reais seguem intactos na requisição; a prévia pode consumir cota.
- **Compatibilidade:** backend TTS, SAF, Keystore, Media3, projetos `schemaVersion=4`, workflows e assinatura mantidos. Build Android e inspeção visual em aparelho ainda pendentes.

## v0.1.76 (versionCode 77) · 31/10/2026

- **Gemini TTS opcional:** Configurações inclui chave pessoal criptografada via Android Keystore, importação `.env`/TXT/JSON, exportação confirmada, teste real de geração e link para AI Studio.
- **Áudio > Narração com IA:** consulta vozes via `GET /v1beta/voices`, suporta modelos `gemini-3.8-flash-tts` e `gemini-3.8-flash-lite-tts` em Interactions; estilos enviados como `speech_metadata.style`.
- **Integração local:** valida RIFF/WAV, salva áudio em `files/gemini_narrations`, reproduz e insere na faixa `audioTracks` do projeto preservando `schemaVersion=4`. Exportação do MP4 via Media3 existente; WAV aceito como entrada pelo Media3, mas composição real deve ser confirmada em aparelho.
- **Privacidade:** gerações enviam somente o texto/voz/estilo/modelo; os demais recursos permanecem offline. Não insira chaves de desenvolvedor em APK distribuído: use um backend. Áudios locais não são automaticamente apagados ao excluir trilhas ou projetos.
- **Validação:** verificações estáticas e testes JVM; API com chave real, Gradle/Android e exportação em aparelho ainda pendentes.

## v0.1.75 (versionCode 76) · 31/10/2026

- O menu Editar clipe agora abre Áudio do clipe: importar, ouvir, volume e aplicar; preserva vínculos nos cortes, divisões, duplicações e reordenação.
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

- **Timeline do editor:** criado `TimelinePlaybackCoordinator` independente de Media3/Compose, compartilhando o mapeamento entre tempo global do projeto e posição de cada clipe.
- **Busca consistente:** botões ±5 segundos, ajuste fino, entrada numérica e barra de progresso usam `seekProjectPosition`, inclusive atravessando as fronteiras entre clipes.
- **Arrasto previsível:** o cursor muda imediatamente durante o gesto e a leitura periódica do player não o substitui até o fim do arrasto.
- **Prévia e reprodução:** restaurar posição salva ao preparar mídia após recriação, publicar `isPlaying` pelo listener do Media3 e soltar o player ao sair da tela.
- **Estado pequeno no ViewModel:** `EditorPlaybackViewModel` mantém apenas projeto ativo e velocidade da prévia (0,25×–2×). Não persiste ExoPlayer/arquivos no `SavedStateHandle`, não altera FPS ou velocidade do MP4.
- **Testes:** mapeamento de clipes, limites, overflow e velocidade; build Android/CI e validação no aparelho continuam pendentes.
- **Compatibilidade:** preservados `schemaVersion=4`, opções de exportação 720p/1080p e componentes das versões anteriores.

## v0.1.63 (versionCode 64) · 30/10/2026
- **Animar Imagens modularizado:** `ImageAnimationTaskViewModel` assume o motor Media3, progresso, cancelamento e confirmação do MP4; a interface só apresenta estados e opções.
- **Reaproveitamento:** mesmo `ExportTaskCoordinator` do exportador principal com tickets de tentativas, fases PREPARING/RENDERING/SAVING e progresso numérico somente quando o Transformer o informa.
- **Concorrência:** instância própria de `ImageAnimationEngine` para cada tentativa, para impedir que a limpeza de um preparo cancelado remova arquivos de outra tentativa. A limpeza durante decodificação é adiada até terminar a operação atual.
- **Rotação:** ViewModel mantém a exportação durante recriação da Activity e as opções visuais são salváveis com valores simples; não recupera trabalho após morte do processo.
- **Compatibilidade:** animação continua offline, PNG temporário sem perdas, H.264 720p/1080p, limite de 80 imagens, formato `schemaVersion=4`, integração com editor e cinco APKs por ABI preservados.
- **Pendente:** build Android Gradle/CI, testes de rotação, cancelamento, salvamento e qualidade MP4 em dispositivo físico. Testes unitários da máquina de estados não provam processamento real.

## v0.1.62 (versionCode 63) · 30/10/2026
- **Exportação com ViewModel:** `ExportTaskViewModel` mantém o estado e o motor Media3 durante recriação da tela; a UI apenas exibe progresso e solicita início/cancelamento.
- **Máquina de estados testável:** `ExportTaskCoordinator` controla preparação, renderização e salvamento; tickets por tentativa ignoram callbacks atrasados, e o progresso só aparece quando informado pelo Media3.
- **Salvamento protegido:** cancelamento só é permitido antes de começar a cópia ao MediaStore. O motor conserva o arquivo temporário até a cópia terminar e retorna o resultado na thread principal.
- **Limites:** a exportação não é serviço em segundo plano, não reinicia automaticamente após encerramento do processo e exige validação em Android real.
- **Compatibilidade:** `schemaVersion=4`, parâmetros 720p/1080p, bitrates, recursos e formatos do projeto permanecem inalterados.

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
- Animar Imagens: ajustes unificados com chips compactos, mesma confirmação de salvamento MP4 usada no editor principal e atalho para continuar editando com áudio, narração, legendas, PiP e cortes.
- Novo **Salvar** no topo guarda a ordem das fotos e opções num rascunho local separado dos projetos de vídeo, retornando à Home ou ao editor de origem. Ao reabrir Animar Imagens, o rascunho é recuperado.
- Voltar com alterações não salvas oferece Salvar e voltar / Descartar / Continuar. O botão permanece disponível após exportações; etapa final de gravação bloqueia saída com explicação.
- Ao renderizar, o rascunho é gravado antes de iniciar o Media3. Arquivos de mídia originais não são copiados nem excluídos.
- Os recursos completos de áudio e timeline continuam no editor existente **após gerar o MP4**; o rascunho de fotos não é um MP4 nem um projeto de vídeo. Teste físico pendente.

## v0.1.51 (versionCode 52) · 30/10/2026
- Animar Imagens: corrigido o NPE do `LinearProgressIndicator` removendo `progress!!` e usando percentual imutável limitado a 0–100%, barra indeterminada quando ausente.
- Prévia com área compacta de 238 dp e proporção preservada, cartões menores, nomes curtos e controles **Ajustes/Animar** lado a lado.
- Alça de arrastar reposiciona a foto durante o gesto e permite rolagem nas bordas, mantendo botões subir/descer/remover e ordem da galeria.
- Identidade Uai Cortes no progresso, mantendo Media3 apenas no código e nos diagnósticos.
- Novo teste unitário do progresso; compilação Android, exportação MP4 e testes de gesto no aparelho ainda pendentes.
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

# Uai Cortes 0.1.53

Editor offline Android nativo em Kotlin, Jetpack Compose e Media3.

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


## Correção de build da versão 0.1.36
- O workflow 31 falhou ao compilar Debug e Release: a importação SRT acessava `searchQuery` antes de sua declaração em Compose. A variável foi movida para o escopo anterior ao callback, sem mudança funcional na busca.
- `tools/validate_source.py` agora valida a ordem da declaração e uso no importador SRT, além de verificar os demais recursos.
- Versão sincronizada em Gradle, workflow, empacotamento, tela Sobre, validação e notas de release. Compilação Android após a correção e teste no dispositivo estão pendentes.

## Novidades da versão 0.1.35
- **Prévia com proporção automática:** telas Legendas, Cortar e unir, PiP e Exportar ajustam o quadro ao vídeo vertical, horizontal ou quadrado, sem esticá-lo; altura limitada pela tela. Editor principal também muda a moldura quando a reprodução passa para outro clipe de proporção diferente.
- **Correção das legendas na prévia:** texto respeita a largura real do quadro, reduz a fonte apenas na visualização quando a palavra mais longa não cabe, e mantém a caixa visível. Evita que “Francisco” se parta entre linhas por causa da prévia estreita.
- **Edição preservada:** posição x/y, tamanho escolhido, SRT, tempos e `schemaVersion=4` não mudam. Essa correção não reprocessa o vídeo e não promete identidade de renderização com o MP4 até teste em aparelho.
- **Testes:** novos casos de geometria para retrato, paisagem, quadrado, metadados inválidos e passagem de clipes.
- **Pendente:** compilar no GitHub Actions e comparar prévia/exportação no aparelho.

## Novidades da versão 0.1.34
- **Busca em legendas:** encontre rapidamente textos entre dezenas ou centenas de entradas, sem exigir acentos ou maiúsculas. Mostra total filtrado, permite limpar e mantém a numeração original.
- **Anterior / Próxima:** navegue diretamente entre legendas cronológicas na prévia fixa. Desabilita setas nas extremidades e atualiza seleção e posição do vídeo.
- **Exclusão com confirmação:** exige confirmação antes de remover a legenda selecionada; texto e projeto permanecem intactos ao cancelar.
- **Segurança temporal e importação:** novas legendas não ultrapassam o vídeo; SRT acima de 250 mil caracteres é rejeitado com erro em vez de ser importado parcialmente.
- `schemaVersion=4` preservado. Sem modificação de fontes, vídeo, trilhas de áudio ou processo de exportação.
- **Validação Android:** build CI, comportamento real em aparelho e fidelidade do MP4 continuam pendentes.

## Novidades da versão 0.1.33
- **Legendas compactas:** uma única timeline horizontal com janelas de dez segundos, carregadas sob demanda, substitui as trilhas individuais empilhadas. As posições e durações exibidas usam horários absolutos, incluindo legendas sobrepostas.
- **Prévia fixa no topo:** vídeo Media3/ExoPlayer com reprodução do trecho selecionado e legenda arrastável; ao tocar na legenda, busca seu início sem alterar o MP4 original.
- **Lista enxuta e virtualizada:** número, texto abreviado, horário e ícone de edição. Tocar em uma linha expande somente a legenda ativa, incluindo texto, horários e uma pista de arrasto única para ajustar início, fim e deslocamento.
- **Avançado sob demanda:** fontes, estilos, cores, sombras, contornos, posição fina e copiar aparência para todas continuam disponíveis, mas inicialmente recolhidos.
- Importação SRT, criação e exclusão de legenda, defaults legados, persistência de propriedades e exportação foram preservados. Validação Android completa e teste visual em aparelho ainda pendentes.

## Histórico da versão 0.1.32
- Na tela Cortar e unir, a faixa **Trechos separados** apresenta miniatura, horários e destaque da parte ativa; os cartões rolam horizontalmente e a seleção rola automaticamente até o trecho escolhido.
- **Dividir** seleciona a parte à direita e recupera o cursor no ponto exato da divisão, sem importar novamente o vídeo. Ao reabrir o projeto, o último ponto de divisão permanece disponível.
- Excluir escolhe o trecho vizinho; Desfazer restaura a divisão e o marcador anterior. Transições já configuradas na fronteira de um clipe são preservadas.
- O arquivo de vídeo original não é alterado; salvar o projeto e exportar MP4 continuam separados.
- A lógica tem testes unitários; interface e Gradle Android aguardam validação em dispositivo/CI.

## Histórico da versão 0.1.31
- Editor: ação **Salvar** virou o primeiro cartão na barra inferior de ferramentas. Ao tocar, persiste o projeto local e volta diretamente à tela Home, sem exportar MP4.
- Os cartões da barra inferior rolam horizontalmente, retornando ao primeiro após o último, nas duas direções (lista virtual extensa, iniciada no centro para simular ciclo contínuo). Nenhuma ferramenta foi removida.
- Cortar e unir: sexto cartão **Salvar** após **Excluir** na faixa de ações com rolagem lateral. Salvar confirma também um corte ainda não aplicado, se início/fim forem válidos, e retorna diretamente à Home; um intervalo inválido exibe aviso e não sai da tela.
- A saída **Exportar** continua separada do salvamento do projeto editável. O arquivo original não é modificado.
- Testes estruturais e testes unitários da nova lógica foram adicionados. Compilação Android e verificação visual no aparelho ainda estão pendentes.

## Versões e ferramentas
- `minSdk = 26`, `compileSdk = 36`, `targetSdk = 36`.
- `versionName = 0.1.53`, `versionCode = 54`.
- Kotlin 2.1.21, AGP 8.10.1, Gradle 8.11.1, JDK 17.
- Media3 1.10.1, ExoPlayer e Transformer para composição H.264/AAC.
- O editor não exige conta; somente o download opcional do modelo de legendas automáticas exige internet. A biblioteca PiP solicita acesso às fotos/vídeos do Android, integral ou parcial conforme consentimento; o seletor de documentos continua como alternativa.

## Telas
- Início, Projetos, Editor, Cortar/Unir, PiP, Áudio, Transições (corte direto, fade preto e fade branco), Exportar MP4.
- Configurações, Geral, Tela e Sobre são **telas**, não pop-ups.
- Ícone azul integrado a `mipmap-*`; original também em `design/icone-uai-cortes.png`.

## Ajustes herdados da versão 0.1.10
- Quatro ferramentas na mesma linha, painel inferior sem alça, com margens compactas.
- Toque nos horários (prévia/editor, começo e fim de cada PiP) para informar **mm:ss**, **mm:ss.d** ou **hh:mm:ss**; valores validados dentro do vídeo.
- Novo PiP inicia após o término mais tardio dos PiPs anteriores; se o vídeo acaba, pergunta antes de permitir coincidência temporal.
- Aviso de conflito espacial-temporal: reorganizar, manter sobreposição com confirmação ou cancelar. Aprovação é persistida por PAR de PiPs, não aplicada indiscriminadamente.
- O tempo final do vídeo principal não é editado pelo campo de busca; esse campo move somente o cursor. Para aumentar a duração, edite/adicionar vídeos no projeto.

## Histórico v0.1.12: corte mais direto e controles compactos
- Miniaturas imediatamente abaixo do vídeo, **sem RangeSlider sobre as imagens**. Cada miniatura é tocável para saltar ao ponto aproximado; uma única barra separada permite ajuste fino.
- Removido o campo separado “Ir para”. O campo **Tempo** fica ao lado da reprodução e, a cada valor válido digitado com a máscara `mm:ss.d`, o cursor já salta, sem botão de aplicar.
- Entradas compactas de início/fim também aceitam digitação direta; somente o botão **Aplicar corte** modifica o projeto.
- **Marcar fim não é desativado silenciosamente:** quando o cursor não está após o início por pelo menos 250 ms, um aviso explica como resolver.
- Dividir, Desfazer, Anterior, Próximo e Excluir estão juntos em uma linha com ícones e rótulos pequenos. Dividir produz dois trechos na sequência; setas reordenam clipes quando há mais de um.
- Desfazer recupera a última alteração aplicada aos clipes nesta sessão: aplicar corte, dividir, mover ou excluir. Os arquivos originais permanecem intocados.
- Continua pendente validar compilação Android e testar reprodução, miniaturas e estados de entrada em hardware real.

## Histórico: 0.1.11: Cortar e unir e horários numéricos
- Tela Cortar e unir com prévia real do vídeo selecionado, reprodução/pausa, saltos de 1 s, cursor de reprodução independente, seis miniaturas carregadas sob demanda e linha do tempo única com seleção de início/fim por marcadores.
- Botões Marcar início e Marcar fim usando o ponto atual; editar os horários tocando no campo e digitando apenas números, com máscara automática `mm:ss.d` (exemplo: `00074` -> `00:07.4`). A máscara também vale para navegação da prévia e duração PiP.
- Aplicar corte grava a edição no projeto sem modificar o arquivo de mídia; Desfazer reverte a última aplicação de corte no clipe enquanto a tela permanecer aberta. Divisão, reordenação e exclusão preservadas.
- Miniaturas em baixa resolução e fora da thread principal para reduzir impacto de memória/fluidez. Prévia, edição temporal e exportação no aparelho ainda dependem de testes de CI e uso real.

## Novidades da 0.1.13: reprodução contínua e velocidade da prévia
- Em Cortar e unir, ExoPlayer repete o clipe automaticamente até o usuário pausar (REPEAT_MODE_ONE).
- Na tela principal de edição, a reprodução percorre todos os clipes e reinicia automaticamente (REPEAT_MODE_ALL).
- Botão compacto para velocidade de prévia: 0,25×, 0,5×, 0,75×, 1×, 1,25×, 1,5× e 2×. Não altera a velocidade nem a duração dos arquivos exportados.
- Áudio e vídeo PiP acompanham a velocidade da prévia principal no editor, mantendo ajuste separado do volume.
- Mini tutorial de 5 passos fica abaixo dos botões de edição de corte.
- Build e teste físico continuam pendentes; Media3 pode limitar decodificadores simultâneos no aparelho.

## Novidades acumuladas até 0.1.30
- Até **4 PiPs por projeto**, com vídeos ou imagens independentes, seleção de camada e opção de adicionar, substituir ou remover.
- Posição, escala 5–90%, duração, volume e silêncio independentes. Áudio desligado em um PiP não silencia os outros.
- Quatro botões quadrados e compactos Posição/Tamanho/Duração/Mídia **na mesma linha**. Cada botão abre um painel **ModalBottomSheet** deslizando da borda inferior, por cima dos controles e ações inferiores, sem alterar a altura ou a posição da prévia. Fechamento por gesto de arrastar, toque fora ou botão; altura máxima de 64% da tela, com conteúdo rolável.
- Mantidos os nove atalhos, escala 5–90% com entrada manual, início/fim e substituição de mídia. Os valores atualizam a prévia; o áudio individual continua fora da janela inferior.
- Prévia de até quatro PiPs ativos no instante do vídeo, edição por arrasto/pinça do selecionado, e faixas de tempo individuais no editor.
- Reposicionamento automático determinístico considerando retângulos, margens e **interseção temporal**. A camada editada recebe prioridade; as demais mudam de posição apenas se necessário. Se não houver espaço, bloqueia a mudança com aviso.
- Projeto JSON com `schemaVersion=4`, campo `overlays[]`, IDs persistentes e **migração de `pip` legado** sem descartar projetos antigos.
- `ExportEngine`: compõe as até quatro sequências visuais e trilhas de áudio independentes. **Compatibilidade real com 4 decodificadores, sincronismo e transparência depende de build e teste em aparelho**.
- Biblioteca de mídia MediaStore por tipo/pasta, alternativa SAF, e demais funcionalidades da 0.1.7 preservadas.

## v0.1.30 · Correção de compilação do contorno das legendas
- Corrigido `MainActivity.kt` para utilizar `StrokeJoin.Round`, enum disponível no Jetpack Compose, no lugar de `PathJoin.Round`, que causou `Unresolved reference` nas compilações Debug e Release do workflow 26.
- A geometria e a espessura do contorno continuam as mesmas. A mudança não altera JSON, presets, áudio, timeline, compatibilidade ou efeitos já configurados.
- Compilação da v0.1.30, lint e publicação dos cinco APKs devem ser confirmados pelo GitHub Actions.

## v0.1.29 · Sombras, entrelinhas e alinhamento das legendas
- Controle individual de sombra diagonal sem desfoque, com deslocamento de 0–6 dp (0 desativa) e cor preta ou branca.
- Espaçamento adicional entre linhas em 100–160% e alinhamento de linhas à esquerda, ao centro ou à direita para legendas com múltiplas linhas.
- A prévia Compose e a composição Media3 receberam o novo estilo, preservando o posicionamento normalizado X/Y. O contorno acompanha o alinhamento escolhido.
- Leitura de projetos antigos usa sombra desligada, espaçamento normal e centro. A ação de copiar aparência transfere também os três novos ajustes, sem alterar IDs, texto, horários ou posição.
- Incluídos `SubtitlePresentationTest` e validações de compatibilidade. Conferir a fidelidade visual no Android e no MP4 exportado antes de considerá-la comprovada.

## v0.1.28 · Fontes e contorno de legendas
- Três fontes locais do Android: Moderna (sans-serif), Serifada e Monoespaçada, sem download de arquivos ou permissão adicional.
- Contorno com intensidade de 0–4 dp e cor preta/branca, aplicável individualmente; prévia Compose com traço sobreposto e exportação Media3 por desenho de borda por linha.
- Os estilos podem ser copiados para todas as legendas sem alterar IDs, horários, conteúdo nem posição no vídeo.
- `SubtitleCue` persiste `fontFamily`, `outlineWidth` e `outlineColor` com padrões sem contorno e fonte Moderna para projetos antigos. Schema v4 preservado.
- A renderização exata, especialmente quebra de linha/alinhamento em fontes diferentes, precisa de teste comparativo em APK e MP4 reais.

## v0.1.27 · Estilos e aparência das legendas
- Cinco presets rápidos: Clássico, Destaque, Ciano, Limpo e Etiqueta, com visualização imediata na prévia e na imagem do vídeo.
- Controle individual de cor, tamanho, fundo (sem fundo/suave/escuro/claro) e negrito. Fundo transparente não cria tarja no MP4.
- A ação **Copiar aparência para todas** pede confirmação e muda somente cor/tamanho/fundo/negrito, sem afetar tempos, IDs, texto ou posição.
- `SubtitleCue` grava `backgroundColor`/`bold` no projeto e adota o aspecto clássico para legendas legadas sem esses campos; `schemaVersion=4` inalterado.
- Renderização real do MP4, fontes e contraste em aparelhos ainda requer testes no CI e no Android.

## v0.1.26 · Legendas editáveis na timeline
- Cada bloco da pista Legenda, na timeline principal, abre exatamente a legenda selecionada, sem depender da ordem de importação SRT.
- Na tela Legendas, cada texto tem sua pista individual: alça esquerda altera início, centro move mantendo duração e alça direita altera fim.
- Ajustes temporais aparecem durante o gesto e são gravados no projeto ao soltar; entradas numéricas de início/fim continuam disponíveis.
- Limites impedem ultrapassar início/fim do projeto e respeitam trechos curtos já importados. Os arquivos originais e a estrutura JSON permanecem intactos.
- O funcionamento visual e a exportação ainda precisam ser validados no Android.

## Compilação e APK
1. Envie os arquivos da pasta extraída para a **raiz** do repositório GitHub.
2. Abra **Actions > Gerar APK Uai Cortes > Run workflow** ou envie alterações para `main`.
3. O workflow valida código, roda testes, Android Lint e `assembleRelease` com API 36.
4. Ao concluir, o **GitHub Releases > v0.1.30 > Assets** oferece diretamente cinco arquivos `.apk` individuais, sem um Artifact adicional e sem ZIP de distribuição:
   - `UaiCortes-v0.1.30-arm32.apk` (`armeabi-v7a`)
   - `UaiCortes-v0.1.30-arm64.apk` (`arm64-v8a`)
   - `UaiCortes-v0.1.30-x86.apk` (`x86`)
   - `UaiCortes-v0.1.30-x86_64.apk` (`x86_64`)
   - `UaiCortes-v0.1.30-universal.apk` (qualquer arquitetura suportada)
5. Somente o APK **Release** é publicado. O gatilho por tags `v*` foi removido para impedir segundo build disparado pela tag criada no Release. O GitHub Actions mostra links diretos no resumo.
6. O mesmo `versionCode=31` é usado em todas as variantes; o APK universal não exige conhecer a arquitetura. Apenas um deve ser instalado por vez. O app atual usa majoritariamente código gerenciado e Media3, portanto tamanhos de APK por ABI podem ser muito próximos. **Não presumir que uma variante é menor antes de medir**.
7. O script `tools/package_apks.py` valida os cinco resultados do Gradle por `output-metadata.json`, assinatura, pacote, `versionCode` e SDK; falha se faltar alguma ABI ou houver duplicata. Não escolhe o primeiro APK encontrado.

### Assinatura
Secrets exigidos para assinatura estável: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` e `KEY_PASSWORD`. Não inclua keystore, senhas ou arquivo TXT de Secrets no repositório. Sem Secrets, o script aceita assinatura debug provisória e pode não atualizar versões anteriores.

## Validações e limitações
- Validação estática executada. Testes Kotlin/JUnit, build Android e inspeção em aparelho ainda não foram executados neste ambiente.
- Testes unitários de posicionamento/intervalos adicionados ao projeto para rodar no CI; ainda sem compilação Android local.
- Ainda **não** foi realizado build Android Gradle nesta sessão nem teste em aparelho físico.
- A execução #3 (código 0.1.2) passou em Kotlin/assembleRelease, mas falhou no Lint; a 0.1.3 resolveu opt-in e a 0.1.4 adicionou upload obrigatório em Actions/Releases. O build 7 (0.1.6) falhou especificamente em `MediaBrowser.kt:165` por falta de opt-in Material 3; o build 15 (0.1.14) falhou em `MainActivity.kt` (OptIn Material 3 e escopo de maxWidth); a correção 0.1.15 aguarda nova compilação.
- A prévia de até quatro ExoPlayers de PiP pode apresentar limitação de decodificadores/defasagem em aparelhos reais. A exportação e transparência de imagens PNG também exigem testes.
- Dissolvência real ainda não é suportada. Fade preto/branco foram codificados para prévia/exportador e dependem de teste real. MP3/WAV não estão disponíveis na extração.
- Arquivos escolhidos no seletor do sistema usam URI persistente. Itens escolhidos pela biblioteca MediaStore dependem da permissão de leitura ativa e da existência do arquivo original; retirar a permissão pode tornar o PiP indisponível.

## Histórico
Ver `CHANGELOG.md`, `RELEASE_NOTES.md` e `docs/VALIDATION.md`.

## Novidades da v0.1.14
- Mesma origem horizontal para as faixas Vídeo, PiP, Áudio e Legenda, com cantos discretos em vez de cápsulas. Os clipes recebem menu inferior com Cortar, Duplicar, Mover, Excluir e volume individual, aplicado na prévia e exportação.
- Legendas manuais e importação SRT local. Ajuste de início/fim, texto, cor, tamanho e posição, renderização na prévia e sobreposição temporal no exportador.
- Transições por par de clipes: Corte direto e fade para preto/branco de 250 a 1000 ms. **Não existe dissolvência real** e os fades não reduzem a duração total.
- Extrair áudio de um clipe ou do projeto completo via Media3 (AAC dentro de MP4): salvar extensão M4A ou MP4 pelo seletor SAF e opcionalmente colocar na timeline.
- Áudio extra pode ser salvo em `audioTracks[]`; o campo antigo `music` continua legível. Novo JSON `schemaVersion=4`, preservando as listas de até quatro PiPs.
- **Limites:** não há codificador MP3 ou WAV. Algumas fontes sem faixa de áudio podem falhar na extração, com aviso. GPU, mixagem e combinações de PiPs/transições ainda precisam de build Gradle/CI e teste real.


## Correção da v0.1.15 · build 15

- Corrigido `@kotlin.OptIn(ExperimentalMaterial3Api::class)` nos composables `EditorScreen` e `PipScreen`, usados nos Bottom Sheets. O `@OptIn` AndroidX existente permanece somente para APIs Media3 `UnstableApi`.
- `TimelineRow` captura `maxWidth` em `BoxWithConstraints` antes do `Row` interno, evitando erro de receptor implícito na largura dos clipes.
- Atualizados Gradle, CI, validação de fonte, informações do aplicativo, notas de release e documentação para v0.1.15 / versionCode 16.
- **A validar:** executar Actions build 16 para confirmar compilação Kotlin Debug/Release, lint, APK e comportamentos em aparelho.

## Atualização 0.1.22: build 16 e download por arquitetura
- Logs do build 16: o Kotlin compilou, mas `:app:lintDebug` falhou com `UnsafeOptInUsageError` em chamadas `AspectRatioFrameLayout.RESIZE_MODE_FIT` dentro de lambdas `AndroidView` em `MainActivity.kt` (lint encontrou 3 erros e 39 avisos).
- Chamadas instáveis de Media3 (`resizeMode`, `setShowBuffering`, shutter) centralizadas no helper `previewPlayer`, anotado com `androidx.annotation.OptIn` para deixar explícita a adesão a `UnstableApi`.
- `splits.abi` configurado para quatro ABIs e APK universal. Publicação unificada em Releases, sem Actions Artifacts duplicado ou gatilho por nova tag.
- **Pendente:** compilar e testar 0.1.22 no Actions, confirmar que lint passou e que os cinco APKs instalaram/funcionam nas arquiteturas compatíveis.

## v0.1.22
A tela Transições agora anima uma prévia baseada em quadros reais da junção. Não é reprodução contínua do vídeo nem crossfade.

## v0.1.24 · Recorte e posicionamento das faixas de áudio

- Cada faixa importada/extraída tem ação **Editar corte** na tela Áudio. A entrada e saída do arquivo e a posição inicial na timeline são editadas por controle deslizante e horário numérico.
- Pré-escuta independente do trecho selecionado; **Salvar corte** aplica alterações ou **Cancelar** mantém a faixa anterior. Exige pelo menos 250 ms e impede passar dos limites da fonte/projeto.
- `AudioTrack.sourceStartMs` persiste a posição de entrada no arquivo, por padrão zero em projetos antigos (sem migração destrutiva). Prévia do editor e exportação agora começam na mesma posição da fonte.
- Não inclui waveform, crossfade ou codificação MP3/WAV. Validar compilação e sincronismo de reprodução/exportação no Android.


## v0.1.24 · Timeline de áudio com gestos
- **Editor principal:** faixas extras de áudio são blocos arrastáveis horizontalmente para mover o instante em que começam. A posição é salva no projeto **ao soltar**. Um toque curto abre diretamente a faixa selecionada na tela Áudio.
- **Tela Áudio:** barra visual com **alça esquerda** para adiantar/retroceder o começo do trecho mantendo o término fixo no projeto, **centro** para mover o trecho completo e **alça direita** para ajustar o término da fonte.
- Ajustes gestuais são uma prévia local até o usuário tocar **Salvar corte** na tela Áudio; **Cancelar** abandona essas mudanças. Campos numéricos continuam disponíveis para ajustes finos e trechos muito curtos.
- Os gestos usam os limites da mídia de origem e do projeto (mínimo de 250 ms); não alteram os arquivos de mídia. Prévia e exportação continuam usando `AudioTrack.sourceStartMs`, `durationMs` e `startMs`.
- **Pendente:** execução GitHub Actions (`testDebugUnitTest`, `lintDebug`, `assembleRelease`) e teste físico de gestos, reprodução e MP4.


## v0.1.25 · Posição proporcional das legendas e ajuste fino do cursor
- A prévia do editor agora posiciona legendas proporcionalmente às dimensões **reais do quadro**, usando âncora inferior central como a exportação Media3. O antigo deslocamento fixo em dp foi eliminado.
- Tela Legendas: miniatura do quadro no início da legenda com texto arrastável. A posição é salva **ao soltar**; sliders e setas de 1% completam o ajuste. A miniatura pode estar indisponível se a mídia não for legível.
- Editor principal: passos de ±0,1 segundo e ±1 segundo na timeline para acertar o cursor em períodos curtos; tempos extremos são limitados ao projeto.
- Lógica pura `TimelinePrecision` e `SubtitlePlacement`, com testes de limites e coordenadas. Arquivo original, JSON/schema e comportamento da exportação permanecem intactos.
- Pendente: compilação Android, teste de arrasto em diferentes proporções de vídeo e comparação visual com MP4 real.
