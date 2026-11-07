# Uai Cortes · versão 0.1.219 — Voz natural e encorpada (versionCode 220)
## v0.1.219
- Em Áudio, nova opção Melhorar voz: presets Natural/Encorpada/Clara, ajuste de intensidade e equalizador avançado de 3 controles. A prévia permite comparar a faixa original com o processamento.
- Equalização suave, compressor e proteção de pico aplicados somente à fonte selecionada durante a exportação. Gravações originais ficam intactas. Totalmente offline e sem novas dependências externas.
- Compatibilidade com projetos antigos preservada; RNNoise e separação de voz/música não estão incluídos. Build Android e teste de qualidade auditiva ainda precisam ser confirmados no dispositivo.

# Uai Cortes · versão 0.1.218 — Compatibilidade do CI (versionCode 219)
## v0.1.218
- Corrigido teste de regressão que impedia o GitHub Actions de chegar à compilação após a reorganização da Central de Integrações.
- Publicação YouTube, autenticação Google e telas da versão anterior foram preservadas. Ainda é necessário validar a compilação e os testes no Android.

# Uai Cortes · versão 0.1.217 — Central de Integrações (versionCode 218)
## v0.1.217
- Encontre Google Gemini, YouTube, Facebook, TikTok e Kwai em uma lista mais curta, com ajuda por serviço.
- Saiba quando precisa configurar uma API, autorizar conta ou usar compartilhamento manual. Controles técnicos ficam dentro dos detalhes.
- Página Redes mais direta, com publicação em destaque e opções de conexão e desempenho recolhíveis.
- Não altera credenciais nem permite acesso automático em serviços que exigem análise e autorização. Compilação Gradle e teste físico pendentes.

# Uai Cortes · versão 0.1.216 — revisão da divisão (versionCode 217)
## v0.1.216
- Melhorias de segurança ao cancelar cortes durante o salvamento de MP4, com tentativa de limpeza de arquivo pendente.
- Segundos e Minutos agora mantêm a última opção escolhida; campos personalizados se adaptam a telas pequenas.
- As opções de duração anteriores continuam disponíveis; a compilação Android e testes com vídeos reais seguem pendentes.

# Uai Cortes · versão 0.1.215 — novas durações de divisão (versionCode 216)
## v0.1.215 · Cortes adequados a diferentes redes sociais
- Divida vídeos em 9,5 s, 10 s, 30 s, 60 s, 1 min, 1min05s, 2 min, 2min59s, 3 min, 4 min ou 8 min; ou escolha horas, minutos e segundos.
- Prévia mostra as partes e intervalos reais; o limite de 180 partes continua valendo. A interface explica que duração não garante monetização.
- Mídias originais, extração de áudio, acompanhamento em tela cheia e resultados anteriores preservados. Exige teste real de processamento no Android.

# Uai Cortes · versão 0.1.214 — Revisão de extração de áudio (versionCode 215)
## v0.1.214 · Mais controle sobre M4As incompletos
- Após erro ou cancelamento em Extrair áudio, o resultado pode apresentar ação para excluir o M4A incompleto com confirmação; não há exclusão automática.
- A exclusão espera a operação de escrita terminar e informa quando o sistema ou provedor não autoriza remover o documento.
- Tela cheia, áudio M4A, divisão de vídeo e fontes originais preservados. Compilação e teste Android físico pendentes.

# Uai Cortes · versão 0.1.213 — Extração de áudio em tela cheia (versionCode 214)
## v0.1.213 · Processamento e resultado visíveis
- Depois de escolher o destino do M4A, uma tela dedicada mostra as etapas reais de extração e salvamento e permite cancelar com confirmação.
- Uma tela de resultados identifica o M4A concluído, com ação de abrir/ouvir, além de Concluído e Nova extração.
- Em cancelamento ou falha, o app não exibe arquivos incompletos como concluídos. Vídeos originais, edição e divisão em partes permanecem inalterados.
- A funcionalidade requer validação em Android real e compilação CI antes de distribuir APK.

# Uai Cortes · versão 0.1.212 — Dividir vídeo em tela cheia (versionCode 213)
## v0.1.212 · Acompanhamento e resultado de cada corte
- Ao iniciar Dividir vídeo, abra uma tela exclusiva de acompanhamento, com a parte atual, fase do processamento e quantas partes já terminaram.
- Ao concluir, a tela Resultado lista os MP4/M4A publicados, com abertura individual e localização das pastas. Em erro ou cancelamento, as partes concluídas continuam acessíveis.
- Sem mudança em Media3, 9,5/10 s, seleção de áudio, original, projetos, assinatura ou cinco variantes Release. Executar CI e testes em aparelho antes de publicar APK.

# Uai Cortes · versão 0.1.211 — Dividir vídeo em qualquer tela (versionCode 212)
## v0.1.211 · Dividir vídeo adaptado a telas pequenas
- Opções de duração agora se organizam verticalmente em telas estreitas e quando o tamanho da fonte é ampliado.
- Seleção dos cartões mais clara para leitores de tela e melhor leitura das mensagens de processamento.
- As durações, o motor de corte e a proteção dos arquivos originais foram preservados. Build e testes em Android ainda pendentes.

# Uai Cortes · versão 0.1.210 — edição rápida visual mais clara (versionCode 211)
## v0.1.210 · Dividir vídeo mais simples de usar
- Nova organização por etapas para escolher o vídeo, definir a duração e decidir o áudio.
- Cartões de seleção com estados claros, prévia de partes e botão fixo de execução com destino informado.
- O vídeo original continua preservado; durações, exportação, cancelamento e extração M4A permanecem com o comportamento anterior.
- A compilação Android e os testes em dispositivos ainda precisam ser executados no CI.

# Uai Cortes · versão 0.1.209 — barra de edição limpa (versionCode 210)

- **Animar imagens** e **Prompt Flow** foram removidos do menu horizontal do editor, porque seus módulos são independentes da linha do tempo.
- Ambos permanecem disponíveis na Home: **Animar fotos** e **Estúdio de Cenas**.
- Ferramentas de edição, mídias e projetos preservados. Os contratos locais foram atualizados; aguardam-se Gradle/JUnit/Lint e testes Android reais.

# Uai Cortes · versão 0.1.208 — correção de CI172 (versionCode 209)

- Corrigido o teste estrutural que procurava o botão Imagem onde ele não existe mais; a opção continua em **Linha do tempo > Adicionar**.
- Preservadas as cópias automáticas de mídias, a barra de ferramentas reorganizada e as telas Sobre/Novidades da 0.1.207.
- Este é um pacote de código-fonte, não cinco APKs compilados. Um novo CI deve executar JUnit, Lint e compilações Release antes de publicação.

# Uai Cortes · versão 0.1.207 — editor e proteção dos projetos (versionCode 208)

- **0.1.205 (206):** mídia importada com cópia independente e validada, vídeos em Filmes, áudios em Música, PiPs de imagem em Imagens; proteção de fontes antigas na abertura do editor.
- **0.1.206 (207):** Salvar no topo, Reparar mídias em Mais, Animar imagens em destaque; inserções de mídia mantidas na timeline.
- **0.1.207 (208):** Sobre compacto (um destaque, desenvolvedor, Pix, link GitHub) e Novidades no topo das Configurações, sem alterar o reconhecimento e a confirmação de novidades após atualização.
- Não modifica schemaVersion 4 nem apaga originais; não inclui APK compilado. Executar testes CI/Android antes de publicar Release.

# Uai Cortes · versão 0.1.204 — Subtitle Studio: animações suaves (versionCode 205)
- Cinco efeitos de legenda aprimorados, com animações suaves de entrada e saída e velocidade ajustável.
- Os estilos principais do Subtitle Studio usam novos movimentos; legado, personalizações e projetos antigos preservados.
- Cálculo de movimento isolado no `:subtitle-core` e compartilhado por prévia Compose e exportação MP4, sem nova dependência externa.
- Build Android, Lint, instrumentação e comparação real de exportação ainda exigem GitHub Actions e aparelho.

# Uai Cortes · versão 0.1.203 — Correção CI169: teste da fonte Cursiva (versionCode 204)
- Corrigido o contrato JUnit de tipografia do Subtitle Studio para reconhecer a quarta fonte Cursiva sem remover essa opção do aplicativo.
- Teste adicional confirma que o estilo Cursiva sobrevive ao salvamento e leitura do projeto; URL remota desconhecida continua usando fonte segura.
- Mantidas as funções da 0.1.202, o modelo schemaVersion 4, assinatura e cinco variantes Release. Build/JUnit/Lint completos necessitam nova execução no CI.

# Uai Cortes · versão 0.1.202 — Subtitle Studio (versionCode 203)
- Galeria de 8 estilos de legenda escolhidos; modelos anteriores continuam acessíveis sem perder projetos salvos.
- Maiúsculas e minúsculas como formatação visual de legenda, mantendo texto e timings do arquivo original.
- Efeitos favoritos compactos, opções antigas recolhidas e fonte cursiva nativa.
- Núcleo `:subtitle-core` separado, com testes de formatação pt-BR, usado por prévia e MP4.
- Compilação Android e Lint completos precisam de nova execução no GitHub Actions; sem afirmar resultado de APK.

# Uai Cortes · versão 0.1.201 — Novidades e atualizações padronizadas (versionCode 202)
- A abertura de Novidades apresenta somente o que mudou nesta versão, deixando o histórico para consulta opcional.
- Sobre mostra últimas três versões e permite abrir o histórico completo sob demanda.
- Tela Atualizações mais compacta, com progresso, velocidade local de download em MiB/s, bytes recebidos e previsão aproximada quando disponível.
- Mantidas validação SHA-256, instalação com autorização, dados de projetos, compatibilidade e os cinco APKs previstos no CI. Compilação Android e testes em aparelho pendentes.

# Uai Cortes · versão 0.1.200 — Padronização visual ampliada (versionCode 201)
- Botões, cartões e diálogos do Estúdio Flow, Vozes IA, Legendas e configurações Gemini seguem a mesma geometria discreta.
- Cortes, inserção/edição, exportação e Home receberam ajustes de formato sem alterar ações, importações, projetos ou arquivos.
- Mantidos PiP, pré-escuta de áudio, Central de Vozes, divisão em 9,5/10 segundos, cinco variantes de APK e compatibilidade de projetos. Testes Android completos ainda pendentes.

## 0.1.199 (versionCode 200)
- Visual mais consistente: cartões e botões retangulares de cantos leves, incluindo a janela Salvar edição.
- PiP: prévia compacta com opção de ampliar, ferramentas menores e mais espaço para editar.
- Áudio: ouça faixas importadas e música diretamente, sem tocar a mixagem toda; extração avançada continua disponível.
- Mantidas as cinco variantes APK, os projetos anteriores e o funcionamento offline. Compilação completa e teste físico pendentes.

# Uai Cortes · versão 0.1.199 — Galeria integrada

- A seleção de vídeo nas ferramentas **Dividir vídeo** e **Extrair áudio** agora usa a galeria interna do Uai Cortes, com miniaturas, pastas, filtro e busca, em vez de abrir primeiro o gerenciador de arquivos do Android.
- O botão de pasta na galeria ainda permite escolher pelo gerenciador de arquivos (SAF). O botão Voltar retorna à ferramenta sem iniciar processamento.
- Criar projeto e Inserir imagem continuam usando o mesmo navegador compartilhado; preservados os vídeos originais, o cache e as ferramentas da versão anterior.
- v0.1.198, versionCode 199. Build e Android Lint completos aguardam execução no GitHub Actions.

# Uai Cortes · versão 0.1.197 — Correção do Lint da extração rápida

- Corrigido uso de anotação experimental Media3 em `AudioExtractionEngine`: a implementação assume sua própria instabilidade e não obriga a tela rápida a opt-in.
- Mantidas as ferramentas de dividir MP4 em 9,5/10 segundos, extrair M4A, recursos de edição e vozes Gemini.
- Adicionado teste de regressão; versionCode **198**. O build Android e o Lint completos devem ser reexecutados no GitHub Actions.

# Uai Cortes · versão 0.1.196 — Dividir vídeo e extrair áudio

- Divisão rápida em MP4s de **9,5 ou 10 segundos**, pelo menu inicial, sem projeto.
- Salve vídeos com ou sem áudio; opcionalmente exporte também M4As por parte (Android 10+).
- Atalho independente para extrair um M4A completo com seletor de destino.
- Progresso e cancelamento; saídas concluídas preservadas; fontes não modificadas.
- versionCode **197**; compilação Android e verificação da precisão real dos cortes pendentes.

# Uai Cortes · versão 0.1.195 — Correção CI161

- Corrigido erro de compilação Kotlin `Unresolved reference 'height'` em `ProjectDraftUi.kt`, detectado pelo workflow 161, sem alterar o diálogo de saída do editor.
- Adicionada verificação de regressão para o import Compose obrigatório.
- Mantidas exportação, inserção de mídias, legendas, Central de Vozes, integração Gemini e formatos de projeto.
- v0.1.195/versionCode 196: cinco APKs Release previstos. A compilação Android completa permanece pendente até nova execução do CI.

# Uai Cortes · versão 0.1.194 — Exportação e Inserção

- O botão **Concluir** depois do vídeo salvo grava o projeto e retorna à página inicial; não fecha o projeto se o salvamento falhar.
- A saída do editor oferece Salvar, Descartar e Continuar na mesma linha com ícones. Salvar cópia continua disponível.
- **Vídeo +** substitui o nome impreciso Unir e pergunta em qual posição inserir os novos clipes. A inserção ajusta elementos posteriores da timeline, mantendo vídeos originais.
- **Inserir imagem** tem quatro opções horizontais compactas, com ícones e rótulos. A barra de ferramentas está mais próxima, com rolagem finita.
- v0.1.194/versionCode 195, cinco APKs Release previstos. Compilação Android e validação no aparelho pendentes.

# Uai Cortes · versão 0.1.193 — Vozes IA Compacta

- Legendas com três ícones: ouvir/gerar, tom e voz; ações avançadas por toque prolongado.
- Progresso de WAVs prontos e falas faltantes com botões de lote Gerar e Inserir; detalhes da fila e personagens recolhidos.
- Ao reabrir Vozes IA depois de iniciar um trabalho, o aplicativo oferece continuar se houver falas pendentes. Não há chamada automática ao Gemini e a fila salva não é sobrescrita.
- VersionCode 194, cinco APKs Release e formatos anteriores preservados; Gradle/Android real pendente.

# Uai Cortes · versão 0.1.192 — Estilos por Personagem e Editor

- Escolha um personagem em **Legendas > Mais > Estilos por personagem** e aplique o visual às falas dele, preservando exceções individuais e permitindo restaurá-las ao padrão.
- Ao salvar uma legenda, a lista mantém a posição; seguir a reprodução permanece opcional.
- Vozes IA orienta criar/importar legendas quando o projeto não contém nenhuma fala válida. A Central de Vozes não é bloqueada.
- Barra inferior Salvar/Cortar/Unir/... sem repetição infinita, e atualização da playlist de mídia em mudanças de duração de imagens.
- Código-fonte v0.1.192/versionCode 193; build no CI e verificação física dos eventuais saltos do player continuam pendentes.

# Uai Cortes · versão 0.1.191 — Correção dos testes CI157

- Corrigida a expectativa do teste de identidade da prévia que bloqueou o workflow 157 (613 testes aprovados, 1 falha).
- A Central de Vozes mantém o cache reutilizável entre chaves Gemini diferentes, desde que modelo, voz, interpretação e frase sejam os mesmos.
- Sem alterações nos recursos de edição, geração, personagens ou narração.
- Código-fonte v0.1.191/versionCode 192. Novo build Android no GitHub Actions e teste físico ainda necessários.

# Uai Cortes · versão 0.1.190 — Central de Vozes

- Abra **Vozes Google** pela Home ou selecione pela própria tela Vozes IA/Narração. Não é mais necessário consultar o catálogo a cada acesso.
- Vozes do Google ficam salvas no aparelho e continuam pesquisáveis sem conexão; recarga é manual por padrão. Em caso de erro de atualização, o catálogo anterior é preservado.
- Ouça a frase-padrão com pausas e pontuação. Prévia já salva reproduz offline; uma nova geração exige consentimento e pode consumir cota Gemini.
- Controle espaço de prévias e apague apenas as amostras, preservando projetos, favoritos e catálogo.
- Voice IA conserva a voz principal por personagem e facilita mudar só a interpretação por fala.
- Código-fonte v0.1.190/versionCode 191. Build Gradle, teste com chave Gemini e validação no aparelho continuam pendentes.

# Uai Cortes · versão 0.1.189 — Interpretação por personagem

- Configure voz e interpretação Gemini em cada personagem de Legendas, com exceções por fala e herança de estilo geral. Preferências são salvas no rascunho do projeto.
- Teste a combinação de voz e interpretação com uma amostra curta após confirmação de que uma chamada Gemini pode consumir cota. Esse áudio de teste não entra na timeline.
- Revise o lote por estado das falas e por personagem antes de gerar. Gere somente as falas pendentes de um personagem ou do filtro atual.
- Reutilização e retomada da fila verificam voz, modelo e interpretação. WAVs preparados e legendas permanecem preservados.
- Código-fonte v0.1.189/versionCode 190; cinco variantes de APK no CI. Build Android e validação em aparelho ainda pendentes.

# Uai Cortes · versão 0.1.188 — Vozes por personagem

- Vozes IA: toque no personagem para escolher uma voz Gemini e aplicá-la a todas as falas dele, sem precisar configurar legenda por legenda. Exceções individuais continuam possíveis e são mantidas no rascunho do projeto.
- Lista compacta com cores de personagens, pesquisa por texto, filtro de personagens, ícones acessíveis e prévia menor que pode ser ampliada.
- Legendas automáticas: opção **Clássica (SRT)** ou **Karaokê IA**, com tamanho ajustável; texto e horários por palavra permanecem preservados.
- Fila recuperada de síntese verifica a voz esperada antes de retomar; alterações não enviam chamadas Gemini nem descartam áudios antigos.
- Código-fonte versão 0.1.188/versionCode 189; cinco APKs no CI e schemaVersion 4 mantidos. Compilação Android e teste em aparelho ainda pendentes.

# Uai Cortes · versão 0.1.187 — Correção do build Android

- Corrigida a referência não resolvida `assertDoesNotExist` na compilação dos testes instrumentados da tela Home/Sobre (`CI154`).
- Mantidas as verificações da interface e as funcionalidades da 0.1.186, incluindo salvar projeto e atualização pelo GitHub.
- Versão e `versionCode` atualizados para `0.1.187` e `188`, com cinco APKs Release previstos no workflow.
- **Pendente:** executar novamente o GitHub Actions, a compilação AndroidTest, os testes, o Lint e a geração dos APKs.

# Uai Cortes · versão 0.1.186 — Atualização no aplicativo e saída simplificada

- **Atualizações em Sobre:** acesso único visível por Verificar atualizações, sem configurar dono/nome nem abrir o navegador como etapa principal.
- **Download interno seguro:** encontra a Release mais recente, consulta SHA-256, verifica APK e pede instalação pelo Android após sua confirmação. Não modifica os projetos automaticamente.
- **Resposta do GitHub:** lote de 20 Releases, limite de resposta ampliado e erro específico quando excedido. Isso reduz, mas não garante eliminar, falhas de rede/API.
- **Salvar este projeto?:** botões em coluna; Descartar não exclui as mídias originais. Salvar cópia permanece para projetos existentes.
- **Pendente:** Gradle, Lint, teste do GitHub real e teste de atualização assinada no aparelho.

- **Legendas automáticas:** modelo, duração e transcrição offline/Gemini estão fáceis de encontrar. Toque em `?` para as explicações completas.
- **Confirmação Gemini:** o resumo do envio de áudio e de possíveis custos fica visível. Toque em **Detalhes de consumo e privacidade** para estimativas técnicas e informações de cache.
- **Quem está falando?:** a janela de escolha ocupa apenas o espaço necessário; mantém personagem não identificado, cores e acesso ao gerenciamento.
- Os nomes não são reconhecidos automaticamente; qualquer rótulo sugerido pelo Gemini precisa de revisão. Nenhuma chamada de IA é feita sem confirmação.
- Antes de publicar o APK, executar testes Gradle, Lint e interface no Android.

# Uai Cortes · versão 0.1.184 — Legendas com mais espaço
- A barra do player agora inclui Linha do tempo à esquerda e os botões de Acompanhar e Ampliar à direita.
- Zoom passa para menu suspenso ao lado da barra de progresso. A prévia compacta fica mais baixa, liberando espaço para a lista.
- Personagens e filtros ficam em um único menu; todas as legendas continuam visíveis nas faixas, mas as não filtradas ficam mais discretas.
- Importar, Exportar, Editar todas e Apagar todas ficam no menu Mais, com proteção contra exclusão involuntária; Nova e Automáticas seguem acessíveis.
- O áudio e os arquivos originais não são alterados. Vosk e Gemini continuam como antes. Validar build e layout no Android antes de considerar comprovado.

# Uai Cortes · versão 0.1.183 — Identificação sugerida de vozes
- Ao usar **Gemini IA** em Legendas automáticas, ative ou desative a sugestão de locutores antes de confirmar o envio do áudio.
- Na revisão, cada Voz 1/Voz 2 pode criar um personagem, vincular a um já existente ou continuar sem identificação. Nomes e cores exportadas não são inferidos nem aplicados automaticamente.
- Uma única chamada pode gerar legenda e rótulos; resultados sem identificação permanecem sem personagem. Vosk offline permanece local e não possui diarização nesta versão.
- Compatibilidade com projetos antigos preservada. Código-fonte validado localmente; compilar Android e testar API real em aparelho antes de afirmar funcionamento final.

# Uai Cortes · versão 0.1.182 — Revisão de legendas e personagens
- Cadastre personagens manualmente, marque quem está falando em cada legenda e filtre as falas por personagem.
- Prévia compacta com botão de ampliar, zoom na linha do tempo e destaque separado da legenda em reprodução.
- Atribuir personagens não altera as cores do vídeo automaticamente: existe confirmação específica para aplicar as cores às legendas.
- Projetos antigos preservados; identificação automática de vozes e compilação/testes Android reais ainda pendentes.

# Uai Cortes · versão 0.1.181 — Vídeo disponível na edição de legendas

**Correção principal:** a tela Legendas mostra e reproduz os clipes do projeto mesmo com zero legendas. Use a faixa visual dos vídeos ou a barra de progresso para escolher o tempo e toque em Nova. As legendas aparecem em faixa separada, sincronizadas com a prévia.

**Compatibilidade:** cortes, imagens, rascunhos e exportação anteriores preservados. A correção precisa ser compilada no GitHub Actions e validada em aparelho real; não foi gerado APK local.

# Uai Cortes · versão 0.1.180 — Importação de modelos Vosk mais confiável

- Em **Configurações → Reconhecimento de voz**, o instalador aceita o ZIP oficial português, inclusive quando veio com uma pasta externa adicional.
- Rejeita pacotes incompletos, corrompidos, com caminhos inseguros ou maiores que o permitido, com indicação da causa; download por navegador e importação SAF permanecem disponíveis.
- Um modelo funcional não é substituído durante tentativas de instalação. A extração só é aplicada após a validação completa; a recuperação de instalação anterior cobre interrupções específicas.
- O diagnóstico grava códigos controlados, sem expor caminhos nem conteúdo do arquivo. A voz continua local/offline após instalação.
- Compilação no GitHub Actions, instalação de ZIP real e uso efetivo do Vosk em Android continuam pendentes de validação.

# Uai Cortes · versão 0.1.179 — Controle sobre o salvamento dos projetos

- Ao voltar da edição, escolher **Salvar projeto**, **Salvar cópia** (em projeto existente), **Descartar e sair** ou **Continuar editando**. As mudanças não são cadastradas como projeto salvo até sua confirmação.
- Em caso de interrupção, permite recuperar um rascunho local separado, sem sobrescrever versões salvas alteradas posteriormente. Mídias originais não são duplicadas.
- **Salvamento e segurança:** tamanho e pasta privada dos backups, contagem de cópias órfãs e botão de limpeza com confirmação. Backups históricos só são gerados a partir de projetos confirmados.
- Limitações: rascunho de recuperação assíncrono pode não conter os últimos instantes antes de falha; imagens privadas descartadas não são removidas automaticamente para evitar perdas em referências de backups.
- Testes de contrato e sintaxe não substituem o build/Lint Android e o teste real de Voltar/recuperar/cópia; `schemaVersion=4` preservado.

# Uai Cortes · versão 0.1.178 — Reparação de arquivos de mídia

- **Reparar mídias:** abra o editor e toque no ícone de pasta. Para arquivos que não abrem, escolha o original usando Arquivos do Android. O app valida formato e duração e reconecta a referência.
- Fotos recebem cópia privada; vídeos e áudios exigem concessão de leitura persistente. Cortes, legendas, sobreposições e horários são preservados.
- Esta atualização não recupera arquivos apagados nem identifica automaticamente a mesma gravação por seu conteúdo. Nenhuma mídia original é apagada.
- **Pendente:** executar build Android, Lint, seleção SAF e exportação de vídeo após reparo em aparelho.

# Uai Cortes · versão 0.1.177 — Correção de testes da escala PiP

- Corrigido o teste que impedia o workflow 147 de avançar após a ampliação do PiP de 90% para 150%.
- Escalas de 91%, 100% e 150% permanecem permitidas; 151% continua inválido. Nenhuma mudança na interface, nos vídeos ou nos projetos.
- Build Kotlin anterior aprovado, mas a suíte JUnit/Android precisa ser executada de novo no GitHub Actions antes de declarar release validada.

# Uai Cortes · versão 0.1.176 — Enquadre suas fotos na linha do tempo

- Toque na foto na linha do tempo e escolha **Enquadrar imagem**.
- Ajuste ou preencha o quadro, arraste a imagem para reposicionar e use o gesto de pinça ou controle de zoom de 100% a 300%.
- **Salvar** registra o enquadramento daquela foto; **Cancelar** mantém o anterior. Imagens antigas continuam sem recorte extra.
- O Media3 recebe a mesma transformação na exportação; qualidade e fidelidade entre tela/MP4 precisam ser validadas no Android.
- Transições com sobreposição real de dois clipes ainda não foram implementadas, pois o Media3 Transformer 1.11.1 não as oferece diretamente.

# Uai Cortes · versão 0.1.175 — Mais controle, menos toques

- **Horários:** toque em Início ou Fim para editar o número existente sem perder o valor.
- **PiP:** use Ajustar ou Preencher, alinhamento sem bordas e escala de até 150% para logos e imagens, mantendo a opção anterior por padrão.
- **Timeline:** só aparecem as faixas ocupadas; Adicionar abre os tipos ausentes.
- **Transições:** seis cartões visuais principais e opção de revelar todos os efeitos originais.
- **Limitações:** não inclui dissolvência real entre clipes nem transformação livre das fotos de fundo; validar zoom PiP na prévia e no MP4 em aparelho.

# Uai Cortes · versão 0.1.174 — Linha do tempo com ajuste direto

- Encaixe magnético ativável/desativável e pausa da reprodução durante o arrasto.
- **Selecione:** tocar num clipe agora seleciona; botão Editar abre as ferramentas completas se necessário.
- **Mova e apare:** arrastar o centro muda posição/ordem, puxar as alças modifica começo/fim; controles exibem zoom e Desfazer/Refazer.
- **Regras de mídia:** vídeo/áudio limitados à duração do arquivo, fotos podem ficar mais tempo, PiP de vídeo usa a entrada correta após recortar.
- **Preservação:** original intacto, schemaVersion=4 e projetos antigos compatíveis pelo campo opcional de offset de PiP.
- **Verificação necessária:** build/Lint no GitHub Actions e testes físicos da timeline, de mídia e do MP4.

# Uai Cortes · versão 0.1.173 — Fotos importáveis novamente

- Corrige o aviso indevido “Não foi possível ler a imagem escolhida” após selecionar uma foto na galeria integrada.
- Mantém capa no início, duração de 1 a 30 segundos, galeria integrada de Vídeo/PiP e mídia original intacta.
- Testes de regressão preparados. Build Android, teste físico de importação e exportação de MP4 continuam pendentes.

# Uai Cortes · versão 0.1.172 — Timeline, mídia local e erros HTTP

- **Editor:** escala temporal única, pistas com ícones para vídeo, imagem, PiP, áudio, vozes e legendas; cursor alinhado à reprodução e navegação ao tocar/arrastar na régua.
- **Foto/capa:** escolha pela galeria integrada; botão de arquivos via SAF, confirmação da duração/posição e cópia local durável antes de inserir. Não remove nem modifica a foto original.
- **Diagnóstico de APIs:** registra status HTTP, categoria e tempo de operação para Gemini e APIs de publicação, com tokens contabilizados apenas quando declarados oficialmente. Não grava chaves, tokens, prompts ou URLs.
- **Estabilidade:** progresso protegido contra estados nulos durante geração e classificação mais específica de arquivos indisponíveis/ZIP de modelo.
- **Validação pendente:** compilação Gradle Android, Lint, teste de imagens/Media3, navegação real e chamadas autorizadas.

# Uai Cortes · versão 0.1.171 — Cortar e unir visual

- **Editor de cortes simplificado:** prévia ocupando o espaço superior, uma régua visual com 12 miniaturas, cursor arrastável e ações grandes Dividir, Aparar, Remover. Ação Pronto retorna ao editor principal; ajuste preciso, mover, excluir clipe e refazer ficam no menu secundário.
- **Recorte visual:** alças roxas escolhem o que manter; alças amarelas escolhem o que remover. A remoção do intervalo pede confirmação, divide a mídia sem regravar o original, remonta a sequência, mantém a transição na borda final e ajusta horários globais de legendas e PiP.
- **Segurança e compatibilidade:** undo/redo de até 15 passos em memória, mínimo de 250 ms por trecho, proibição de apagar o único clipe inteiro e persistência em schemaVersion 4. Áudio vinculado a clipe mantém referências; MP4 final ainda depende de validação real.
- **Regressão:** testes do núcleo e controle visual adicionados. Sintaxe e estrutura verificáveis localmente; CI Gradle e teste no Android são obrigatórios antes de declarar funcionamento aprovado.

# Uai Cortes · versão 0.1.170 — CI143, opt-in Media3 de imagens

- **Correção CI143:** `StaticImageClip.kt` declara `@androidx.annotation.OptIn(UnstableApi::class)` na função `ClipMediaItems.playback`, que chama `MediaItem.Builder.setImageDurationMs`. Corrige especificamente `UnsafeOptInUsageError` encontrado em `:app:lintDebug` do workflow **Gerar APK 143** (0.1.169).
- **Sem mascarar diagnósticos:** lint permanece ativo, sem baseline, supressões globais ou `abortOnError=false`.
- **Compatibilidade:** a função Imagem/Capa 0.1.169, editor, projeto JSON schemaVersion 4, Estúdio, Gemini, Redes Sociais e cinco APKs Release são preservados. Versão 0.1.170, versionCode 171.
- **Validação:** execução local do contrato para opt-in, testes estruturais e sincronização; compilação Gradle/Lint e teste do MP4 no aparelho ainda precisam ser executados na nova versão.

# Uai Cortes · versão 0.1.169 — Fotos e capa na timeline principal

- **Fotos como clipes principais:** no editor, a opção **Imagem** abre o seletor do Android (SAF), valida JPG/PNG/WebP/HEIC/HEIF/BMP, limita arquivo a 24 MiB e imagem a 24 megapixels, permite definir de 1 a 30 segundos e inserir como capa no início, antes/depois do trecho selecionado ou no fim, sem alterar arquivo original.
- **Prévia/edição:** foto na mesma faixa Vídeo dos demais clipes, com rótulo próprio e miniatura; tocar nela abre opções para alterar duração, duplicar, mover ou excluir. Prévia Media3 usa `MediaItem.Builder.setImageDurationMs`; captura de quadro de origem fica desabilitada em foto. Tela Duração usa amostragem de bitmap em IO.
- **Exportação real via Media3 Transformer:** imagem via `EditedMediaItem.Builder.setFrameRate(30)` com duração configurada, mantidos efeitos de vídeo e áudio de fundo. Extrator e reconhecimento de fala ignoram trechos de foto sem áudio, mas preservam a duração no deslocamento das legendas. **Falta testar MP4 e sincronismo em Android real; testes estruturais não substituem build**.
- **Sincronização:** ao inserir/resimensionar uma foto, os elementos com tempo absoluto posteriores (legendas com tempo por palavra, PiPs e áudio extra global) avançam junto com os clipes. Trilhas de áudio vinculadas a ID de clipe seguem o deslocamento automaticamente. Música global mantém início no zero. Elementos cujo intervalo cruza o ponto de inserção podem exigir revisão manual.
- **Compatibilidade:** `Clip.isImage` e `imageMimeType` opcionais no JSON (defaults para vídeo), schemaVersion 4 mantido. Preservar projetos legados, recursos do Estúdio, Gemini, Redes Sociais, cinco APKs Release e versionCode 170. Fotos dependem da permissão de leitura SAF ainda válida; arquivo movido/excluído fora do app precisará ser relocalizado.

# Uai Cortes · versão 0.1.168 — Meta OAuth e preparação TikTok

- **Meta oficial, sem App Secret:** cliente Facebook Login SDK 19.0.0, autorização pelo Android sob toque, consulta `/me/accounts` e vinculação visual da Página cadastrada. Os IDs são públicos e persistidos; tokens de Página ficam apenas em memória e a sessão do usuário é gerida pelo SDK. A ação "Desconectar Meta" revoga a sessão local do SDK, sem excluir as fichas do Uai Cortes.
- **Publicação com sessão Meta:** autorização separada de `pages_manage_posts` para publicar; se a permissão existir e a Página estiver autorizada na sessão, dispensa colagem do token temporário no diálogo, ainda exigindo confirmação e estado UPLOAD_REVIEW antes do primeiro POST. Modo de token manual preservado como alternativa.
- **Configuração da Meta:** `META_APP_ID` e `META_CLIENT_TOKEN` opcionais no workflow, validados como IDs públicos no Gradle. Sem valores, o botão de login fica desabilitado e a publicação assistida continua disponível. App Secret é proibido no APK. Registre os hashes reais da assinatura Android e solicite revisão de `pages_show_list`, `pages_read_engagement`, `pages_manage_posts` nos produtos Meta. Não declarar produção habilitada sem aprovação.
- **TikTok:** central explica que Login Kit exige domínio verificado com HTTPS, certificado de assinatura e um backend próprio para troca de código por tokens, sem armazenar `client_secret` no cliente. A chave pública `TIKTOK_CLIENT_KEY` é opcional e não realiza login sozinha. Modo avançado de token temporário e status continuam disponíveis; OAuth TikTok integrado ainda pendente.
- **Compatibilidade:** não muda schema social 1 nem VideoProject schema 4, Editor, Gemini, Wizard ou o OAuth YouTube. Mantém 5 APKs Release e versionCode 169. A presença do SDK adiciona dependência a validar no CI; build, login, publish e visual Android real não foram executados localmente.

# Uai Cortes · versão 0.1.167 — TikTok e Facebook Página (fase 3)

- **Facebook Página:** vínculo de ID público confirmado pela Graph API v26.0 usando um token OAuth de Página inserido apenas para aquela sessão; START → upload binário → FINISH de Reel, cada envio exige consentimento e a conta deve corresponder à Página vinculada. Recebimento da API não garante processamento/público.
- **TikTok:** consulta oficial `creator_info` antes do envio para mostrar identidade, opções de privacidade e restrições de interação; seleção consciente, marcação opcional de conteúdo criado com IA, upload `FILE_UPLOAD` dividido em blocos, e consulta manual do status via `publish_id`.
- **Segurança:** o token OAuth é inserido no modo avançado, não persistido nem exportado; não há App Secret no APK, nem troca automática de tokens ou reenvio silencioso. A conexão não é login OAuth integrado, e o usuário deve obter token autorizado externamente. `UPLOAD_REVIEW` é gravado antes de qualquer POST; falha incerta exige inspeção da plataforma. `REMOTE_SUBMITTED` NÃO é confirmação de publicação pública.
- **Contas e compatibilidade:** perfil pessoal Facebook e Kwai permanecem manuais. `remotePageId` opcional não altera o schema social 1 nem `VideoProject` schemaVersion 4. Migração de cadastros 0.1.165/166, Gemini, Editor, Wizard e YouTube preservados. Metadados, fluxo de cinco APKs Release e versionCode 168 sincronizados.
- **Configuração externa:** Meta exige permissões aprovadas `pages_manage_posts`, `pages_read_engagement`/`pages_show_list`; TikTok exige `video.publish` e auditoria para visibilidade pública. No modo atual, tokens são fornecidos por ação, NÃO existe autenticação persistente nem OAuth integrado Meta/TikTok. Não publicar sem permissões reais.
- **Limites e testes:** nenhum token real, publicação real ou build Android foi validado nesta sessão; o cliente inclui testes de contrato, parser Kotlin, testes instrumentados pendentes e checagens de integridade do pacote. Upload depende de disponibilidade do serviço, formatos aceitos e rede estável; nunca presumir vídeo publicado.

# Uai Cortes · versão 0.1.166 — YouTube OAuth e publicação direta (fase 2)

- **YouTube OAuth:** integração opcional com o AuthorizationClient do Google Play services (escopos `youtube.upload` e `youtube.readonly`). Vincular ID público do canal depois de consulta real e confirmação; nenhum token salvo/exportado.
- **Enviar ao YouTube:** botão por publicação preparada, confirmação individual, privacidade Privado/Não listado/Público (privado por padrão), inicialização de sessão recuperável e envio em blocos de 1 MiB. Sem envio automático de múltiplos destinos nem repetição cega após falha.
- **Status e métricas:** registrar `UPLOAD_REVIEW` antes do POST e `CONFIRMED_BY_API` somente quando API confirmar ID de vídeo. Atualizar métricas sob demanda com origem `youtube_api`, sem misturar valores manuais.
- **Compatibilidade:** campos novos opcionais em contas/publicações/métricas; JSON social ainda schema 1, projetos de vídeo `schemaVersion=4` e biblioteca do Estúdio preservados. Meta, TikTok e Kwai mantêm compartilhamento assistido; sem prometer upload automático.
- **Configuração externa necessária:** habilitar a YouTube Data API v3 e cadastrar OAuth Android com package `br.com.uaicortes` + SHA-1 da assinatura realmente usada; Google Play services no aparelho. Requer consentimento da conta e cotas do projeto Google Cloud.
- **Limites:** uploads acontecem com a tela de publicação ativa. Após interromper a sessão ou erro inconclusivo, o app exige verificação manual no YouTube Studio; nesta versão não persiste URI sensível de retomada. Build Android, OAuth real, compatibilidade de API e upload com conta autorizada ainda não foram executados localmente.

# Uai Cortes · versão 0.1.165 — Redes Sociais (fase 1)

- **Redes Sociais:** nova aba no rodapé da Home, de Projetos e de Ajustes com cadastro local independente de TikTok, YouTube, Kwai, Facebook Página e Facebook Perfil. O cadastro não significa que a conta está autenticada.
- **Após exportação:** opção Publicar nas redes para o exportador tradicional e Animar Imagens. Escolha do MP4 pelo SAF, múltiplos destinos, descrição/hashtags comuns, compartilhamento manual individual e histórico persistente. Abrir o seletor não confirma postagem.
- **Histórico e métricas:** estados preparada/compartilhamento aberto/publicação confirmada manualmente; ligação por vídeo e destino; visualizações, curtidas, comentários e compartilhamentos apenas quando informados. Nunca confundir números manuais com dados oficiais.
- **Central de APIs e Contas:** acesso às chaves Gemini existentes, links oficiais de desenvolvedor e exportação/importação SAF de cadastros sem credenciais, URLs ou tokens. Testes de API e uploads diretos dependem de OAuth/permissões oficiais, ainda não implementados.
- **Segurança:** catálogo JSON privado com AtomicFile, preservação de arquivo corrompido, verificação de URLs HTTPS, sem tocar em VideoProject schemaVersion 4 e sem criar serviços/contas falsas. Nenhum envio de vídeo ou chamada de rede automática.
- **Validação:** verificações estruturais/PSI e instrumentados incluídos. Build Android/execução de OAuth e publicação real permanecem pendentes de ambiente e credenciais autorizadas. Detalhes em `docs/VALIDATION_0.1.165.md`.

# Uai Cortes · versão 0.1.164 — Wizard visual de criação

- **Wizard em cinco etapas:** novo projeto abre primeiro o estilo visual; depois personagens, cenários, montagem dos clipes e exportação para Flow. Cada tela foca uma tarefa, com navegação e autosave.
- **Sugestões imediatas sem API:** alienígena, pessoa, animal, fruta, robô, objeto animado e cenários. Fichas com nome e aparência em destaque; roupa, voz e objetos opcionais.
- **Biblioteca entre histórias:** personagens, cenários e estilos podem ser salvos e reutilizados; imagem opcional da galeria com cópia privada. Exportação ZIP inclui referências importadas.
- **Gemini opcional:** enriquecer individualmente personagem ou cenário após confirmação, com revisão antes de aplicar; sem chamadas pagas automáticas.
- **Cena = clipe:** editor oferece personagens cadastrados e seleção de quem fala; imagem inicial opcional, prompt preparável sem IA. Compatibilidade dos rascunhos v1 e projetos de vídeo schemaVersion=4.
- **Validação:** testes locais estáticos e de integridade não substituem build Android/uso real das APIs; ver `docs/VALIDATION_0.1.164.md`.

# Uai Cortes · versão 0.1.163 — Estúdio em clipes e Gemini global

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
- Recuperação manual de legendas Gemini geradas e ainda não aplicadas após fechar/reabrir a tela. Rascunhos privados ficam até 7 dias, com tamanho máximo de 1 MiB.
- Recuperação de descrição e hashtags geradas, mesmo sem salvar o projeto, quando o vídeo/contexto/modelo coincidem; nenhuma regeneração automática.
- Falhas de cota, autorização, modelo e rede classificadas. O painel Gemini resume operações concluídas e reprovadas.
- Nenhuma mudança nos dados de chave, áudio, exportação ou esquema de projetos. A validação Android real é pendente.

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
## 0.1.153 — Favoritos e progresso em Vozes IA
- Favoritas no catálogo Gemini com estrela e filtro, salvas localmente.
- Nome da voz escolhida na barra, inclusive após reabrir o app quando já consultada.
- Progresso percentual das falas concluídas e indicador indeterminado por chamada TTS.
- Sem alterações nos limites Gemini, projetos, legendas, exportação ou assinatura.


## 0.1.152 — Correção de publicação CI128
- Evita HTTP 422 por exceder o limite de tamanho das notas no GitHub Releases.
- Publica um resumo da versão atual, mantendo o histórico completo no código-fonte.
- Mantém a validação de assinatura, os cinco APKs por arquitetura e a revisão das notas em reexecuções do workflow.
- O workflow 128 comprovou build Android completo, testes, Lint e cinco APKs da 0.1.151; a publicação da 0.1.152 ainda precisa ser testada no GitHub Actions.

# Uai Cortes · versão 0.1.151 (05/11/2026)

## 0.1.151 — Estimativas de consumo Gemini
- Consumo por função e modelo com média de tokens baseada em pelo menos cinco gerações medidas.
- Campo de limites RPM, TPM de entrada e RPD por projeto, para estimar disponibilidade local sem afirmar saldo oficial.
- Previsão antes de gerar, incluindo quantidade de vozes.
- Troca autorizada após HTTP 429 agora pausa o modelo afetado, sem bloquear modelos de outras ferramentas.
- IDs dos projetos Google Cloud precisam ser configurados nas chaves, e limites devem ser copiados do AI Studio.
- Compilação Android e teste no aparelho continuam pendentes.

# Uai Cortes · versão 0.1.150 (05/11/2026)
**CI126 — correção de regressão na suíte JUnit:** resolução 480p deve ser aceita pela exportação. Corrigido teste antigo que a tratava como inválida; mantidos testes de 360/480/720/1080 e rejeição de resoluções não suportadas. Exportação, voz Gemini, consumo e segurança das chaves não foram alterados. O workflow 126 teve 1 falha em 466 testes, após compilação Kotlin Debug/Release aprovada. v0.1.150 / versionCode 151.

# Uai Cortes · versão 0.1.149 (05/11/2026)

## 0.1.149 — Correção da compilação Gemini (CI125)
- Corrige símbolo de ícone não reconhecido ao compilar a nova tela Gemini.
- Preserva a interface compacta, as chaves protegidas, o gerenciamento de múltiplas chaves e as ações com confirmação.
- Correção de código-fonte; execução completa dos testes Android e publicação de APKs ainda pendentes.

# Uai Cortes · versão 0.1.148 (05/11/2026)

## 0.1.148 — Gerenciamento Gemini renovado
- Status visual da credencial e botões menores para Colar, Copiar, Importar, Exportar, Obter e Remover.
- Múltiplas chaves exibidas em cartões; adicionar ou editar agora abre uma janela em vez de ocupar a tela inteira.
- Painel de uso com métricas, histórico e aviso de limites em seções claras.
- As operações sensíveis continuam protegidas por confirmação, e o histórico não presume saldo de créditos.
- A experiência em aparelho ainda precisa ser testada após build GitHub Actions.

# Uai Cortes · versão 0.1.147 (04/11/2026)

## 0.1.147 — Exportar e compartilhar
- 360p, 480p, 720p ou 1080p com controles maiores e estimativas de tamanho ajustadas à qualidade.
- Exportação apresenta apenas o progresso informado pelo Media3, explicando que a velocidade de renderização pode variar.
- Após salvar: três botões (Gerar descrição, Gerar com IA, Compartilhar). Editor de texto/hashtags e configurações Gemini abrem em pop-up com confirmação de API.
- Uso real no aparelho, duração da renderização e saída MP4 ainda precisam de validação Android.

# Uai Cortes · versão 0.1.146 (04/11/2026)

## 0.1.146 — Visual da tela Google Gemini
- Comandos da chave numa barra única, com menos rolagem e caixas compactas.
- Privacidade, várias chaves e consumo IA em painéis expansíveis; três indicadores e detalhes sob demanda.
- Segurança e confirmações das chaves mantidas. Validar em aparelho, TalkBack e com fonte ampliada; build Android ainda pendente.

# Uai Cortes · versão 0.1.145 (04/11/2026)

## 0.1.145 — Janela de vozes aprimorada
- Busca e filtros mais acessíveis, seleção atual destacada, lista rolável aproveitando a altura da tela.
- Filtros de gênero e timbre sem elementos cortados nas laterais.
- O catálogo continua vindo da API; nenhuma nova chamada é feita ao apenas filtrar/escolher.


- Novo ajuste assistido: detectar sobreposição, gerar WAV local derivado para pequenos desvios, preservar o original e exigir escuta antes de inserir. Para grandes diferenças, regenerar com ritmo orientado após confirmação de consumo Gemini.
- Busca e filtros de vozes (feminina/masculina; grave/média/aguda) usando dados reais, botões curtos e nomes amigáveis para padrões reconhecidos.
- Testar voz/tempo no aparelho e exportação MP4 antes de declarar fidelidade audiovisual.

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
- Legendas: sincronização manual do destaque por palavra, com distribuição inicial opcional, ajuste das fronteiras, hora digitável e prévia ilustrativa.
- Correção da prévia de karaokê quando os horários são absolutos; preserva intervalos de ASR, projetos antigos e exportação já existente.
- Testar compilação Gradle e vídeo real; nenhuma compatibilidade com áudio foi presumida sem teste em aparelho.

## Versão 0.1.133 · versionCode 134 (04/11/2026)
- Legendas: edição em lote mais clara, com opção explícita de copiar cores apenas para palavras correspondentes.
- Mais opções visuais: agora 22 modelos prontos e 19 tipos de animação no catálogo, organizados por categoria.
- Prévia controlável: pausa, reinício e visualização no tempo real da legenda; sem texto substituto enganoso.
- Preservados projetos existentes, animações antigas e exportação local. Testar APK e MP4 antes de confirmar fidelidade visual.

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


- Tela inicial reformulada: **Novo projeto**, **Animar fotos** e **Prompts Flow** em três cartões iguais de cor azul e ícones, numa só fileira.
- Cabeçalho compacto e seção Edições recentes com estado vazio melhor explicado e acesso Ver todos quando aplicável.
- Prévia da Home em Sobre usa os mesmos componentes com ações desabilitadas; navegação e armazenamento de projetos preservados.
- Atualizados testes, versionamento e metadados. Android/Gradle e acessibilidade com fonte ampliada dependem de teste em aparelho.

## Versão 0.1.128 · versionCode 129 (04/11/2026)

- Nova tela compacta de exportação com ícones, seletores menores, compressão sem bitrate duplicado nos cartões de resolução e botão Salvar fixo.
- Ajuda visual com dois exemplos de enquadramento; opções de guias de legendas e interface somente na prévia.
- Prévia expansível conserva PlayerView local, FIT/ZOOM e camadas do projeto; requer validação Android e comparação do MP4 antes de declarar fidelidade.
- Formatos MP4, versão Media3 e dados dos projetos permanecem inalterados.

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

# Uai Cortes v0.1.111 — Narração Gemini por trechos sincronizados

- **Reutilizar legendas:** modo Sincronizada por trechos ou Contínua. O modo temporal preserva os instantes de cada legenda, permite edição individual sem alterar o texto original e exige confirmação antes das chamadas Gemini.
- Até 12 falas por geração; cada fala usa uma chamada TTS. Em falha/cancelamento, os WAVs já gravados são mantidos e as falas inalteradas não são solicitadas novamente nesta sessão.
- Antes da inserção, revisão mostra a duração real de cada WAV, conflitos com legendas/vozes seguintes e excedentes do fim do vídeo. Excedente final bloqueia a inclusão; sobreposições dentro do vídeo exigem aprovação expressa. Nenhum áudio é cortado automaticamente.
- Faixas sincronizadas são salvas em uma única atualização do projeto após aprovação. Sem mudanças no JSON/schemaVersion 4, nos clipes, legendas ou exportador; a narração contínua permanece disponível e também não é mais truncada silenciosamente.
- **Pendente:** compilação Android completa, teste com Gemini real, sincronização de preview/exportação no aparelho e interrupções do Android.

# Uai Cortes v0.1.110 — Salvar e voltar ao vídeo + ajustes visuais de Áudio

- Botão de salvamento explícito e fixo no rodapé da tela Áudio, com retorno ao editor somente se a persistência do projeto funcionar; corte pendente impede saída até ser aplicado ou cancelado.
- Rótulos de vídeos de nomes automáticos agora ficam compreensíveis na mixagem, sem alterar títulos gravados. Cartões têm espaçamento menor e mensagens podem ser descartadas.
- Não altera níveis de áudio, prévia, ExportEngine ou API Gemini. Esquema do projeto 4 preservado, versão Android e GitHub Manager sincronizados.
- Compilação Android e teste físico da interface ainda pendentes.

# Uai Cortes v0.1.109 — Gemini na mixagem e volume de prévia

- Corrigida a navegação da narração Gemini mostrada no vídeo de teste: confirmar a voz mantém a janela de geração aberta; após consentir com a chamada de IA, uma resposta WAV válida é gravada e adicionada à tela Áudio, sem navegação automática. Falhas de gravação preservam o WAV para nova tentativa sem repetir a síntese.
- Adicionado botão **Ouvir mixagem** no topo da tela Áudio para conferir o som original do vídeo e as faixas sincronizadas. A reprodução não muda o projeto.
- Novo botão de **Volume da prévia** junto ao Play na tela do editor, com ajuste de 0% a 100% para clipes, música, faixas adicionais e áudio dos PiPs. Não altera a mixagem persistida nem o MP4.
- A confirmação de geração esclarece custo/cota e inclusão; removida a solicitação GET desnecessária de validação do modelo, sem repetição automática de chamadas. Preservado schemaVersion 4, cinco APKs por arquitetura e versionCode 110.
- Verificações locais de sintaxe e contratos não substituem compilação Gradle/Android nem chamada Gemini com chave real.

# Uai Cortes v0.1.108 — Extração recolhida e retorno ao editor

- A ferramenta **Extrair áudio de vídeo** inicia recolhida, com título e seta para abrir/fechar; mantém origem, pré-escuta, formato e opções Salvar/Salvar e usar. Ao importar um vídeo externo pela galeria, o painel abre para continuar a extração.
- Após gerar uma narração Gemini e confirmar **Adicionar à timeline**, o projeto é persistido antes de voltar ao editor de vídeo. Falha ao salvar mantém o editor de Áudio aberto e informa o problema; não há inclusão silenciosa.
- Confirmar apenas a escolha da voz continua retornando ao editor sem gerar nem adicionar áudio. Salvamento em arquivo externo (Salvar áudio) permanece independente.
- `schemaVersion=4` mantido, versionCode 109, GitHub Manager/Gradle/CI sincronizados. Compilação Android e teste físico pendentes.

# Uai Cortes v0.1.107 — Seleção feminina e teste de voz

- Corrige o caso em que o filtro Femininas mostrava somente vozes femininas, mas uma voz masculina marcada anteriormente ainda poderia ser confirmada.
- Tocar no círculo agora marca a voz explicitamente; a seleção fica visível no cartão.
- Botão Ouvir voz marcada permite testar a voz antes de confirmar. Se não houver amostra local, a síntese exige autorização e pode consumir cota.
- Escolher uma voz retorna ao editor de vídeo sem gerar narração; a geração e inclusão de áudio permanecem sob confirmação.
- GitHub Manager e versionamento sincronizados em 0.1.107 / versionCode 108. Testar no APK real.

# Uai Cortes v0.1.106 — Seleção de vozes simplificada

- Lista compacta: nome em português quando conhecido, características oficiais resumidas e opções Todas/Femininas/Masculinas.
- Use Detalhes para consultar nome original da API, ID, descrição e perfil completos, sem poluir a tela de escolha.
- Seleção e narração não aparecem mais em dois diálogos simultâneos. Confirmar voz volta ao vídeo; cancelar retorna ao gerador, sem alterar a voz anterior.
- Reutilização de legendas e confirmações antes de gerar áudio permanecem intactas.
- Versionamento e GitHub Manager sincronizados em 0.1.106 / versionCode 107. Build Android e testes no aparelho pendentes.

# Uai Cortes v0.1.105 — Prévia nítida no Animar Imagens

- Prévia de fotos usa a mídia original proporcionada, com correção de orientação EXIF; não depende mais de uma miniatura de proporção quadrada.
- Área de prévia maior para vídeos verticais e botão **Ampliar** para conferir os detalhes.
- Três níveis de nitidez ajustam apenas a prévia (Rápida, Equilibrada, Alta); o vídeo final mantém a configuração de resolução já escolhida em Personalizar.
- Limite de decodificação conservador para aparelhos com pouca RAM. O teste no Android é necessário para confirmar o resultado visual.

# Uai Cortes v0.1.104 — Diagnóstico da consulta de atualizações GitHub

- O verificador de Releases agora distingue erros de DNS, conexão, tempo limite, TLS/SSL, acesso HTTP 401/403/404/429 e respostas inválidas.
- A opção **Abrir Releases no navegador** permite verificar o endereço do GitHub quando a API falha. Não envia credenciais e não baixa APK automaticamente.
- Por segurança, o aplicativo não mostra mensagens internas de exceção, tokens, chaves ou URLs de requisição no diagnóstico.
- A consulta exige um repositório público para funcionar sem token; não é possível deduzir somente da captura se o repo está privado ou houve falha local de rede.
- Testes unitários adicionais para diagnóstico; assinatura, SHA-256, projeto e confirmadores preservados.
- Versão 0.1.104 / versionCode 105. Compilação Android completa e teste em aparelho ainda pendentes.

# Uai Cortes v0.1.103 — Retorno ao vídeo ao escolher voz

- Escolha uma voz em Áudio → Gerar por IA → Voz. Ao tocar em **Confirmar voz**, o app fecha as janelas e volta para a prévia do vídeo no editor, retomando a reprodução no ponto mantido na timeline.
- O texto e a voz escolhida continuam no rascunho do mesmo projeto: para gerar narração com essa voz, reabra Gerar por IA e confirme a geração. A voz **não** substitui o áudio original apenas por ser selecionada.
- O seletor ganhou linhas mais legíveis com marcador de seleção; Cancelar/Fechar não muda a voz nem dispara a API.
- Versão 0.1.103 / versionCode 104 e GitHub Manager sincronizados. Build Android completo e verificação no aparelho pendentes.

# Uai Cortes v0.1.102 — Diagnóstico do catálogo Gemini

- Em Áudio → Gerar por IA → Voz, use **Consultar** ou abra **Diagnóstico do catálogo** e toque **Executar teste novamente**. O painel informa se o filtro `pt-BR` retornou vozes, se houve fallback geral e erros de acesso de forma sanitizada.
- A consulta não envia legendas, não sintetiza áudio, não seleciona a voz e não consome cota de geração TTS; a chamada de listagem pode continuar sujeita a limites do serviço.
- Adicionados testes offline de consulta, erro HTTP, resposta vazia, paginação, deduplicação e privacidade (9 verificações Kotlin isoladas passaram; Gradle pendente). Assinatura/5 APKs e esquema de projeto preservados. Build Android e teste com chave real pendentes.

# Uai Cortes v0.1.101 — Correção dos testes de versões (workflow 85)

- `HomeAboutVersionContractTest` usa `BuildConfig.VERSION_NAME` e `BuildConfig.VERSION_CODE` para validar Sobre/Novidades sem dados fixos antigos.
- Teste da mudança da Home em Sobre passa a pesquisar novidades em todas as versões retidas, mantendo a exigência de documentação mas sem falhar quando a funcionalidade já foi lançada anteriormente.
- Verificador de código-fonte e teste de versão sincronizada usam os valores do Gradle, reduzindo regressões. Arquivos GitHub Manager, workflow, app e pacote sincronizados em **0.1.101 / versionCode 102**.
- Mantidas as atualizações via GitHub e a política de instalação/download somente com confirmação. Requer rerun do GitHub Actions e teste em aparelho antes de afirmar que APK foi validado.

# Uai Cortes v0.1.100 — Atualizações online pelo GitHub

- Ao abrir o Uai Cortes, verifica se há Release mais recente no repositório público configurado. Inclui pré-releases; não usa `releases/latest`.
- Uma nova versão é anunciada em diálogo sem download automático. Configurações → Atualizações pelo GitHub permite verificar, ler novidades, baixar e instalar com confirmações.
- O APK universal é conferido com tamanho, SHA-256 (quando publicado) e identificador/versão do Android. Se a Release não tiver digest, somente abre a página oficial para download manual.
- Builds no GitHub Actions gravam o identificador do repositório no APK; compilação local pode exigir configurar manualmente. Atualizações só instalam por cima com assinatura consistente. O workflow exige os quatro Secrets de assinatura e bloqueia publicação sem eles.
- Versionamento: v0.1.100, versionCode 101. Requer teste no GitHub Actions e em aparelho Android antes de considerar a instalação validada.

# Uai Cortes v0.1.99 — Teste da Home e versão no GitHub Manager

- Em **Sobre**, toque em **Testar visual da Home** para conferir os cartões da Home reutilizados, com ações desativadas.
- A versão exibida na Home e em Sobre vem do mesmo registro `AppReleaseNotes`.
- Arquivos `github-manager.json`, `app_identity.json` e `VERSION` passam a acompanhar a versão do APK. Divergências interrompem a validação no CI.
- Teste Android instrumentado adicionado, ainda pendente de execução em emulador ou aparelho.

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

## v0.1.89 (versionCode 90) · 01/11/2026

- **Legendas:** 12 modelos visuais com miniaturas, contornos, sombras e estilos; destaque de cores por palavra, paleta do karaokê e itálico.
- **Sincronização:** legendas automáticas guardam os horários individuais obtidos do Vosk; prévia e exportação usam o mesmo índice de palavra e cores.
- **Navegação:** botão Voltar de Animar Imagens leva à tela inicial, independentemente da origem, com salvamento ou descarte explícito quando houver rascunho alterado.
- **Compatibilidade:** projetos antigos preservados (`schemaVersion=4`); testes em APK real pendentes.

## v0.1.88 (versionCode 89) · 01/11/2026

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

- Configurações > Google Gemini agora abre um menu próprio, com ações em duas colunas e visual mais compacto.
- Áudio > Gerar por IA abre uma janela de narração, com busca pelas vozes e códigos reais do Gemini.
- Áudio gerado continua com reprodução, salvamento e inserção na timeline; manutenção dos recursos offline.
- Ainda requer compilação Android e teste visual no aparelho antes de considerar o APK validado.

## v0.1.76 (versionCode 77) · 31/10/2026

- Configurações > Google Gemini: configure sua chave pessoal, importe/exporte, teste acesso real e remova com confirmação.
- Editor > Áudio > Narração com IA: escolha modelo/voz, estilo, gere, escute, salve WAV e adicione à timeline do projeto.
- Geração na internet com avisos de possível cobrança; áudios armazenados localmente continuam acessíveis offline.
- Compatibilidade: projetos schemaVersion=4, editor e Media3 existentes. Build Android, teste com chave real e MP4 final ainda não executados.

## v0.1.75 (versionCode 76) · 31/10/2026

- Áudio do clipe: toque em um trecho e escolha Adicionar áudio somente a este clipe. Importe uma narração, escute, configure o volume e aplique. O áudio segue o clipe durante alterações da timeline.
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

**Editor, reprodução e timeline.** Avançar/voltar 5 segundos agora considera a duração total do projeto, inclusive cruzando clipes. A barra de progresso acompanha o dedo durante a busca e não deve sofrer correções do player enquanto está sendo arrastada. A posição salva é retomada ao preparar a prévia e a velocidade de visualização passa a ser gerenciada pelo ViewModel. Os recursos existentes e o formato dos projetos foram preservados. Compilação Android completa e testes em aparelho continuam pendentes.

## v0.1.63 (versionCode 64) · 30/10/2026
- **Animar Imagens modularizado:** `ImageAnimationTaskViewModel` assume o motor Media3, progresso, cancelamento e confirmação do MP4; a interface só apresenta estados e opções.
- **Reaproveitamento:** mesmo `ExportTaskCoordinator` do exportador principal com tickets de tentativas, fases PREPARING/RENDERING/SAVING e progresso numérico somente quando o Transformer o informa.
- **Concorrência:** instância própria de `ImageAnimationEngine` para cada tentativa, para impedir que a limpeza de um preparo cancelado remova arquivos de outra tentativa. A limpeza durante decodificação é adiada até terminar a operação atual.
- **Rotação:** ViewModel mantém a exportação durante recriação da Activity e as opções visuais são salváveis com valores simples; não recupera trabalho após morte do processo.
- **Compatibilidade:** animação continua offline, PNG temporário sem perdas, H.264 720p/1080p, limite de 80 imagens, formato `schemaVersion=4`, integração com editor e cinco APKs por ABI preservados.
- **Pendente:** build Android Gradle/CI, testes de rotação, cancelamento, salvamento e qualidade MP4 em dispositivo físico. Testes unitários da máquina de estados não provam processamento real.

## v0.1.62 (versionCode 63) · 30/10/2026
- Exportação mais robusta durante mudanças de orientação: estado e progresso agora pertencem ao ViewModel.
- Melhor controle de cancelamento e etapas da exportação; resultados antigos não se misturam com novas tentativas.
- O salvamento final do MP4 na galeria é protegido contra cancelamentos que interrompam a cópia.
- Mantidos 720p/1080p, qualidade solicitada, exportação offline e compatibilidade dos projetos.
- Build Android completo e teste em aparelho permanecem pendentes.

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
- Padronização dos ajustes de Animar Imagens e botão Salvar edição no cabeçalho.
- Rascunho local persistente (fotos, sequência e opções) com confirmação ao sair e recuperação ao reabrir.
- Confirmação de vídeo salvo compartilhada com a exportação do editor: Abrir, Compartilhar, Editar na timeline, Fechar.
- O editor continua sendo o local das ferramentas de música, áudio, legendas, PiP e cortes, após a geração do MP4.
- Corrigido bloqueio de Voltar depois de salvar; a finalização MediaStore permanece protegida.
- Código-fonte validado estruturalmente; compilação Android e teste real no dispositivo pendentes.

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
- Correção do build 26: `PathJoin` (símbolo inexistente no Compose usado pelo projeto) substituído por `StrokeJoin.Round` no contorno das legendas, mantendo a mesma intenção visual.
- Preservados os controles de estilo, sombra, entrelinhas e alinhamento da v0.1.29.
- Cinco variantes Release (arm32, arm64, x86, x86_64 e universal) permanecem configuradas para GitHub Releases.
- Compilação Kotlin, lint, empacotamento e publicação da v0.1.30 ainda dependem de nova execução do workflow.

## v0.1.29 (versionCode 30) · 19/10/2026
- Novos controles de sombra deslocada (0–6 dp, preta/branca), espaçamento entre linhas (100–160%) e alinhamento à esquerda, centro ou direita.
- Legendas antigas mantêm sombra desligada, espaçamento padrão e alinhamento centralizado.
- Prévia e Media3 recebem os novos efeitos, com necessidade de teste real em Android e comparação com vídeo exportado para comprovar fidelidade.
- Cinco APKs Release por ABI + universal continuam configurados para GitHub Releases; execução de CI pendente.

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

## Correção v0.1.22 (versionCode 23)
- Corrige o erro `No value passed for parameter onClip` em `MainActivity.kt:825` encontrado no build 18.
- Compilação Android e teste físico ainda pendentes.

Implementação incremental: indicadores de transição clicáveis na timeline, seleção por cartões e botão salvar e voltar à Home. Prévia real de transições ainda pendente.

# Uai Cortes v0.1.22 · versionCode 23

## Correção do build 16
- Resolve uso de API `UnstableApi` do Media3 dentro das lambdas `AndroidView` que fazia `:app:lintDebug` falhar. Configurações de vídeo centralizadas em função com `@androidx.annotation.OptIn`.
- Mantém editor, quatro PiPs, legendas, cortes, áudio, fades e efeitos já implementados.

## Um Release com cinco APKs para baixar
- `UaiCortes-v0.1.22-arm32.apk` (ARM 32-bit, `armeabi-v7a`)
- `UaiCortes-v0.1.22-arm64.apk` (ARM 64-bit, `arm64-v8a`)
- `UaiCortes-v0.1.22-x86.apk` (Intel x86 32-bit)
- `UaiCortes-v0.1.22-x86_64.apk` (Intel x86 64-bit)
- `UaiCortes-v0.1.22-universal.apk` (universal, indicado quando não souber a ABI)

O CI publica **somente os cinco APKs Release** como arquivos diretos em GitHub Releases, não repete o pacote no Actions Artifacts e não reexecuta automaticamente por causa de tag criada pelo Release.
Todas as variantes usam o mesmo versionCode e a mesma assinatura do workflow. Utilize apenas uma por instalação.

## Limitações e validação
- Android Lint e compilação completa ainda precisam ser confirmados no próximo GitHub Actions.
- As variantes por ABI podem ser semelhantes em tamanho quando o aplicativo não embarca bibliotecas nativas específicas.
- Fades, exportação de múltiplos PiPs, SRT e extração AAC ainda precisam de validação em aparelhos reais. Dissolvência, MP3 e WAV não estão disponíveis.
